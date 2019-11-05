/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */

package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.account.AccountTypeBL;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.item.event.ItemDeletedEvent;
import com.sapienter.jbilling.server.item.event.ItemUpdatedEvent;
import com.sapienter.jbilling.server.item.event.NewItemEvent;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pricing.PricingContext;
import com.sapienter.jbilling.server.pricing.tasks.IPricing;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springmodules.cache.CachingModel;
import org.springmodules.cache.FlushingModel;
import org.springmodules.cache.provider.CacheProviderFacade;

import java.math.BigDecimal;
import java.util.*;

;

public class ItemBL {

    private static final FormatLogger LOG = new FormatLogger(ItemBL.class);

    private ItemDAS itemDas = null;
    private ItemDTO item = null;
    private EventLogger eLogger = null;
    private String priceCurrencySymbol = null;
    private List<PricingField> pricingFields = null;
    private CacheProviderFacade cache;
    private FlushingModel flushModel;
    private CachingModel cacheModel;
    
    public ItemBL(Integer itemId)
            throws SessionInternalError {
        try {
            init();
            set(itemId);
        } catch (Exception e) {
            throw new SessionInternalError("Setting item", ItemBL.class, e);
        }
    }

    public ItemBL() {
        init();
    }

    public ItemBL(ItemDTO item) {
        this.item = item;
        init();
    }

    public void set(Integer itemId) {
        item = itemDas.find(itemId);
    }

    private void init() {
        eLogger = EventLogger.getInstance();
        itemDas = new ItemDAS();
        cache = (CacheProviderFacade) Context.getBean(Context.Name.CACHE);
        cacheModel = (CachingModel) Context.getBean(
        Context.Name.CACHE_MODEL_ITEM_PRICE);
        flushModel = (FlushingModel) Context.getBean(
        Context.Name.CACHE_FLUSH_MODEL_ITEM_PRICE);
    }

    public ItemDTO getEntity() {
        return item;
    }

    public Integer create(ItemDTO dto, Integer languageId) {
        EntityBL entity = new EntityBL(dto.getEntityId());
        if (languageId == null) {
            languageId = entity.getEntity().getLanguageId();
        }

        if (dto.getHasDecimals() != null) {
            dto.setHasDecimals(dto.getHasDecimals());
        } else {
            dto.setHasDecimals(0);
        }

        if (dto.getAssetManagementEnabled() != null) {
            dto.setAssetManagementEnabled(dto.getAssetManagementEnabled());
        } else {
            dto.setAssetManagementEnabled(0);
        }

        validateUniqueProductCode(dto, dto.getChildEntitiesIds(), true);

        dto.setDeleted(0);

        //add the orderline meta fields
        if(dto.getOrderLineMetaFields().size() > 0) {
            validateProductOrderLinesMetaFields(dto.getOrderLineMetaFields());

            Set<MetaField> orderLineMetaFieldDtos = dto.getOrderLineMetaFields();
            dto.setOrderLineMetaFields(new HashSet<MetaField>());
            MetaFieldBL metaFieldBL = new MetaFieldBL();
            for(MetaField metaField : orderLineMetaFieldDtos) {
                dto.addOrderLineMetaField(metaFieldBL.create(metaField));
            }
        }

        dto.updateMetaFieldsWithValidation(dto.getEntityId(),null, dto);
      
        item = itemDas.save(dto);

        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription(), languageId);
        }
        updateTypes(dto);
        updateExcludedTypes(dto);
        updateAccountTypes(dto);
        
        //triggering processing of event for parent company
        NewItemEvent newItemEvent = new NewItemEvent(item);
    	EventManager.process(newItemEvent);
    	
        // triggering process for child companies
        for(Integer id : dto.getChildEntitiesIds()) {
    		newItemEvent.setEntityId(id);
    		EventManager.process(newItemEvent);
    	}

        updateCurrencies(dto);
        
        // trigger internal event
        EventManager.process(new NewItemEvent(item));

        return item.getId();
    }

    public Integer create(ItemDTO dto, Integer languageId, boolean isPlan) {
        EntityBL entity = new EntityBL(dto.getEntityId());
        if (languageId == null) {
            languageId = entity.getEntity().getLanguageId();
        }

        if (dto.getHasDecimals() != null) {
            dto.setHasDecimals(dto.getHasDecimals());
        } else {
            dto.setHasDecimals(0);
        }

        dto.setDeleted(0);

        item = itemDas.save(dto);

        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription(), languageId);
        }
        updateTypes(dto);
        updateExcludedTypes(dto);

        // trigger internal event
        EventManager.process(new NewItemEvent(item));

        return item.getId();
    }

    public void update(Integer executorId, ItemDTO dto, Integer languageId)  {
        update(executorId, dto, languageId, false);
    }

    public void update(Integer executorId, ItemDTO dto, Integer languageId, boolean isPlan)  {
        eLogger.audit(executorId, null, ServerConstants.TABLE_ITEM, item.getId(),
                EventLogger.MODULE_ITEM_MAINTENANCE,
                EventLogger.ROW_UPDATED, null, null, null);

        //validate unique product code
        validateUniqueProductCode(dto, dto.getChildEntitiesIds(), false);

        validateProductOrderLinesMetaFields(dto.getOrderLineMetaFields());

        item.setNumber(dto.getNumber());
        item.setGlCode(dto.getGlCode());
        item.setPriceManual(dto.getPriceManual());
        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription(), languageId);
        }
        item.setPercentage(dto.getPercentage());
        item.setHasDecimals(dto.getHasDecimals());
        item.setAssetManagementEnabled(dto.getAssetManagementEnabled());
        item.setStandardAvailability(dto.isStandardAvailability());

        item.setStandardPartnerPercentage(dto.getStandardPartnerPercentage());
        item.setMasterPartnerPercentage(dto.getMasterPartnerPercentage());

        updateTypes(dto);
        updateExcludedTypes(dto);
        updateAccountTypes(dto);
        

        item.setGlobal(dto.isGlobal());
        item.setEntities(dto.getEntities());
        if(dto.getEntity() != null) {
        	item.setEntity(dto.getEntity());

        if (item.getPercentage() == null) {
            // update price currencies
            updateCurrencies(dto);
        } else {
            // percentage items shouldn't have a price, remove all old prices
            for (ItemPriceDTO  price : item.getItemPrices()) {
                new ItemPriceDAS().delete(price);
            }
            item.getItemPrices().clear();
        }
        //clear meta fields, in case we have different child entities than the ones assigned before we do not want there meta fields
        item.getMetaFields().clear();

	       	if (dto.isGlobal()) {
	       		for (CompanyDTO company : new CompanyDAS().findChildEntities(dto.getEntityId())) {
	       			item.updateMetaFieldsWithValidation(company.getId(), null, dto);
	       		}	
	        } else {
	       		for (Integer entityId : dto.getChildEntitiesIds()) {
	       			item.updateMetaFieldsWithValidation(entityId, null, dto);
	       		}
	       	}

        Collection<Integer> unusedMetaFieldIds = updateProductOrderLineMetaFields(dto.getOrderLineMetaFields());

        item.setActiveSince(dto.getActiveSince());
        item.setActiveUntil(dto.getActiveUntil());
        item.setReservationDuration(dto.getReservationDuration());
        
        itemDas.save(item);

        // trigger internal event
        EventManager.process(new ItemUpdatedEvent(item));
        }
    }

    private void updateCurrencies(ItemDTO dto) {
        LOG.debug("updating prices. prices %s price = %s", (dto.getPrices() != null),
                   dto.getPrice());
        ItemPriceDAS itemPriceDas = new ItemPriceDAS();
        // may be there's just one simple price
        if (dto.getPrices() == null) {
            if (dto.getPrice() != null) {
                List prices = new ArrayList();
                // get the defualt currency of the entity
                CurrencyDTO currency = new CurrencyDAS().findNow(
                        dto.getCurrencyId());
                if (currency == null) {
                    EntityBL entity = new EntityBL(dto.getEntityId());
                    currency = entity.getEntity().getCurrency();
                }
                ItemPriceDTO price = new ItemPriceDTO(null, dto, dto.getPrice(),
                        currency);
                prices.add(price);
                dto.setPrices(prices);
            } else {
                LOG.warn("updatedCurrencies was called, but this " +
                        "item has no price");
                return;
            }
        }

        // a call to clear() would simply set item_price.entity_id = null
        // instead of removing the row
        for (int f = 0; f < dto.getPrices().size(); f++) {
            ItemPriceDTO price = (ItemPriceDTO) dto.getPrices().get(f);
            ItemPriceDTO priceRow = null;

            priceRow = itemPriceDas.find(dto.getId(),
                        price.getCurrencyId());
            if (price.getPrice() != null) {
                if (priceRow != null) {
                    // if there one there already, update it
                    priceRow.setPrice(price.getPrice());

                } else {
                    // nothing there, create one
                    ItemPriceDTO itemPrice= new ItemPriceDTO(null, item,
                            price.getPrice(), price.getCurrency());
		            item.addItemPrice(itemPrice);
		        }
		    } else {
		        // this price should be removed if it is there
		        if (priceRow != null) {
		        	new ItemPriceDAS().delete(priceRow);
		        }
		        item.getItemPrices().clear();
		    }
		}
        invalidateCache();
		// invalidate item/price cache
	}
    
    private void updateTypes(ItemDTO dto)
            {
        // update the types relationship
        Collection types = item.getItemTypes();
        types.clear();
        ItemTypeBL typeBl = new ItemTypeBL();
        // TODO verify that all the categories belong to the same
        // order_line_type_id
        for (int f=0; f < dto.getTypes().length; f++) {
            typeBl.set(dto.getTypes()[f]);
            types.add(typeBl.getEntity());
        }
    }

    private void updateExcludedTypes(ItemDTO dto) {
        item.getExcludedTypes().clear();

        ItemTypeBL itemType = new ItemTypeBL();
        for (Integer typeId : dto.getExcludedTypeIds()) {
            itemType.set(typeId);
            item.addExcludedType(itemType.getEntity());
        }
    }
    
	private void updateAccountTypes(ItemDTO dto) {
		item.getAccountTypeAvailability().clear();
		if (!dto.isStandardAvailability()) {
			AccountTypeBL accountTypeBL = new AccountTypeBL();
			for (Integer accountTypeId : dto.getAccountTypeIds()) {
				accountTypeBL.setAccountType(accountTypeId);
				item.addAccountTypeAvailability(accountTypeBL.getAccountType());
			}
		}
	}

    public void delete(Integer executorId) {
        //check if there are assets linked to the item
        int assets = new AssetBL().countAssetsForItem(item.getId());
        if(assets > 0) {
            throw new SessionInternalError("Unable to delete item. There are linked assets.",
                    new String[] {"validation.item.no.delete.assets.linked"});
        }
        int dependencyCount = new ItemDependencyDAS().countByDependentItem(item.getId());
        if (dependencyCount > 0) {
            throw new SessionInternalError("Unable to delete item. Its use in other product as dependency.",
                    new String[]{"validation.item.dependency.exist"});
        }
        item.setDeleted(1);

        item.setTypes(new Integer[0]);
        
        eLogger.audit(executorId, null, ServerConstants.TABLE_ITEM, item.getId(),
                EventLogger.MODULE_ITEM_MAINTENANCE,
                EventLogger.ROW_DELETED, null, null, null);

        // trigger internal event
        ItemDeletedEvent itemDeletedEvent = new ItemDeletedEvent(item);
        EventManager.process(itemDeletedEvent);
        for (Integer entityId : item.getChildEntitiesIds()) {
        	itemDeletedEvent.setEntityId(entityId);
        	EventManager.process(itemDeletedEvent);
        }

        itemDas.flush();
        itemDas.clear();
    }

    public boolean validateDecimals( Integer hasDecimals ){
        if( hasDecimals == 0 ){
            if(new OrderLineDAS().findLinesWithDecimals(item.getId()) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the basic price for an item and currency, without including purchase quantity or
     * the users current usage in the pricing calculation.
     *
     * This method does not execute any pricing plug-ins and does not use quantity or usage
     * values for {@link PriceModelDTO#applyTo(com.sapienter.jbilling.server.order.db.OrderDTO, java.math.BigDecimal, com.sapienter.jbilling.server.item.tasks.PricingResult, java.util.List, com.sapienter.jbilling.server.order.Usage, boolean, java.util.Date)}
     * price calculations.
     *
     * @param date
     * @param item item to price
     * @param currencyId currency id of requested price
     * @return The price in the requested currency
     */
    public BigDecimal getPriceByCurrency(Date date, ItemDTO item, Integer userId, Integer currencyId)  {
    	UserBL user = new UserBL(userId);
    	return getPriceByCurrency(item, user.getEntity().getEntity().getId(), currencyId, date);
    }
    
    public BigDecimal getPriceByCurrency(ItemDTO item, Integer entityId, Integer currencyId)  {
    	return getPriceByCurrency(item, entityId, currencyId, new Date());
    }
    	
    public BigDecimal getPriceByCurrency(ItemDTO item, Integer entityId, Integer currencyId, Date pricingDate) {
        BigDecimal retValue = null;

        if ( null == pricingDate ) { 
        	pricingDate = new Date();
        }
        
        // get the item's defualt price
        int prices = 0;
        BigDecimal aPrice = null;
        Integer aCurrency = null;
        // may be the item has a price in this currency
        for (Iterator it = item.getItemPrices().iterator(); it.hasNext(); ) {
            prices++;
            ItemPriceDTO price = (ItemPriceDTO) it.next();
            if (price.getCurrencyId().equals(currencyId) && price.getPrice().compareTo(new BigDecimal(0.0))!=0) {
                // it is there!
                retValue = price.getPrice();
                break;
            } else {
                // the pivot has priority, for a more accurate conversion
            	
                if ( price.getPrice() != null && price.getPrice().compareTo(new BigDecimal(0.0)) != 0 && (aCurrency == null) ) {
                    aPrice = price.getPrice();
                    aCurrency = price.getCurrencyId();
                }
            }
        }
	    if (prices > 0 && (retValue == null || retValue.compareTo(new BigDecimal(0.0))==0)) {
	        // there are prices defined, but not for the currency required
	        try {
	            CurrencyBL currencyBL = new CurrencyBL();
	            retValue = currencyBL.convert(aCurrency, currencyId, aPrice, pricingDate, entityId);
	        } catch (Exception e) {
	            throw new SessionInternalError(e);
	        }
	    } else {
	        if (retValue == null) {
	            return BigDecimal.ZERO;
	        }
	    }
	    retValue = retValue.setScale(5, BigDecimal.ROUND_HALF_UP);
	    return retValue;
	}

    public BigDecimal getPrice(Integer userId, BigDecimal quantity, Integer entityId) throws SessionInternalError {
        UserBL user = new UserBL(userId);
        return getPrice(userId, user.getCurrencyId(), quantity, entityId, null, null, false, null);
    }

    public BigDecimal getPrice(Integer userId, Integer currencyId, BigDecimal quantity, Integer entityId) throws SessionInternalError {
         return getPrice(userId, currencyId, quantity, entityId, null, null, false, null);
    }

    /**
     * Will find the right price considering the user's special prices and which
     * currencies had been entered in the prices table.
     *
     *
     * @param userId user id
     * @param currencyId currency id
     * @param entityId entity id
     * @param order order being created or edited, maybe used for additional pricing calculations    @return The price in the requested currency. It always returns a price,
     * */
    public BigDecimal getPrice(Integer userId, Integer currencyId, BigDecimal quantity, Integer entityId, OrderDTO order, OrderLineDTO orderLine,
                               boolean singlePurchase, Date eventDate) throws SessionInternalError {

        if (currencyId == null || entityId == null) {
            throw new SessionInternalError("Can't get a price with null parameters. currencyId = " + currencyId +
                    " entityId = " + entityId);
        }

        CurrencyBL currencyBL;
        try {
            currencyBL = new CurrencyBL(currencyId);
            priceCurrencySymbol = currencyBL.getEntity().getSymbol();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        Date pricingDate = ( null != eventDate ? eventDate : ( null != order ? order.getPricingDate() : null ));

        // set price model company id
        item.setPriceModelCompanyId(entityId);
        // default "simple" price
        BigDecimal price = getPriceByCurrency(item, entityId, currencyId, (order != null ? order.getPricingDate() : null));

        PricingContext pricingContext = new PricingContext(
                                                new UserDAS().find(userId),
                                                new CurrencyDAS().find(currencyId),
                                                eventDate, item, quantity, pricingFields);

        // run a plug-in with external logic (rules), if available
        try {
            PluggableTaskManager<IPricing> taskManager
                    = new PluggableTaskManager<IPricing>(entityId, ServerConstants.PLUGGABLE_TASK_ITEM_PRICING);
            IPricing myTask = taskManager.getNextClass();

            while(myTask != null) {
                price = myTask.getPrice(pricingContext, price, order, orderLine, singlePurchase);
                myTask = taskManager.getNextClass();
            }
        } catch (Exception e) {
            throw new SessionInternalError("Item pricing task error", ItemBL.class, e);
        }

        return price;
    }

    /**
     * Will find the right price considering the user's special prices, 
     * currencies that have been entered in the prices table 
     * and the mediation order Event Date.
     *
     * Useful for calls by the Mediation process
     * 
     * @param userId user id
     * @param currencyId currency id
     * @param entityId entity id
     * @param quantity quantity
     * @param eventDate the date on which the order/event must be created
     * @return The price in the requested currency. It always returns a price,
     * otherwise an exception for lack of pricing for an item
     */
    public BigDecimal getPriceByEventDate(Integer userId, Integer currencyId, BigDecimal quantity, Integer entityId, Date eventDate)
            throws SessionInternalError {

        if (currencyId == null || entityId == null) {
            throw new SessionInternalError("Can't get a price with null parameters. "
                    + "currencyId = " + currencyId
                    + " entityId = " + entityId);
        }

        CurrencyBL currencyBL;
        try {
            currencyBL = new CurrencyBL(currencyId);
            priceCurrencySymbol = currencyBL.getEntity().getSymbol();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        //price will be retrieved/converted based on exchange rate on the eventDate
        BigDecimal price = getPriceByCurrency(eventDate, item, userId, currencyId);
        LOG.debug("Got default 'simple' price for order pricing date %s as %s", eventDate, price);

        PricingContext pricingContext = new PricingContext(
                new UserDAS().find(userId),
                new CurrencyDAS().find(currencyId),
                eventDate, item, quantity, pricingFields);

        // run a plug-in with external logic (rules), if available
        try {
            PluggableTaskManager<IPricing> taskManager
                    = new PluggableTaskManager<IPricing>(entityId, ServerConstants.PLUGGABLE_TASK_ITEM_PRICING);
            IPricing myTask = taskManager.getNextClass();

            while(myTask != null) {
                price = myTask.getPrice(pricingContext, price, null, null, false);
                myTask = taskManager.getNextClass();
            }
        } catch (Exception e) {
            throw new SessionInternalError("Item pricing task error", ItemBL.class, e);
        }

        return price;
    }
        
    /**
     * Returns an ItemDTO constructed for the given language and entity, priced for the
     * given user and currency.
     *
     *
     * @param languageId id of the users language
     * @param userId id of the user purchasing the item
     * @param entityId id of the entity
     * @param currencyId id of the currency   @return item dto
     * @throws SessionInternalError if an internal exception occurs processing request
     */
    public ItemDTO getDTO(Integer languageId, Integer userId, Integer entityId, Integer currencyId)
        throws SessionInternalError {
        return getDTO(languageId, userId, entityId, currencyId, BigDecimal.ONE, null, null, false, null);
    }


    /**
     * Returns an ItemDTO constructed for the given language and entity, priced for the
     * given user, currency and the amount being purchased.
     *
     * @param languageId id of the users language
     * @param userId id of the user purchasing the item
     * @param entityId id of the entity
     * @param currencyId id of the currency
     * @param quantity quantity being purchased
     * @return item dto
     * @throws SessionInternalError if an internal exception occurs processing request
     */
    public ItemDTO getDTO(Integer languageId, Integer userId, Integer entityId, Integer currencyId, BigDecimal quantity)
        throws SessionInternalError {
        return getDTO(languageId, userId, entityId, currencyId, quantity, null, null, false, null);
    }
    
    /**
     * Returns an ItemDTO constructed for the given language and entity, priced for the
     * given user, currency and the amount being purchased.
     *
     * If an order is given, then the order quantities will impact the price calculations
     * for item prices that include usage.
     *
     * @param languageId id of the users language
     * @param userId id of the user purchasing the item
     * @param entityId id of the entity
     * @param currencyId id of the currency
     * @param quantity quantity being purchased
     * @param order order that this item is to be added to. may be null if no order operation.
     * @return item dto
     * @throws SessionInternalError if an internal exception occurs processing request
     */
    public ItemDTO getDTO(Integer languageId, Integer userId, Integer entityId, Integer currencyId, BigDecimal quantity,
                          OrderDTO order) throws SessionInternalError {

    	return getDTO(languageId, userId, entityId, currencyId, quantity, order, null, false, null);
    }


    /**
     * Returns an ItemDTO constructed for the given language and entity, priced for the
     * given user, currency and the amount being purchased.
     *
     * If an order is given, then the order quantities will impact the price calculations
     * for item prices that include usage.
     *
     *
     *
     *
     * @param languageId id of the users language
     * @param userId id of the user purchasing the item
     * @param entityId id of the entity
     * @param currencyId id of the currency
     * @param quantity quantity being purchased
     * @param order order that this item is to be added to. may be null if no order operation.
     * @return item dto
     * @throws SessionInternalError if an internal exception occurs processing request
     */
    public ItemDTO getDTO(Integer languageId, Integer userId, Integer entityId, Integer currencyId, BigDecimal quantity,
                          OrderDTO order, OrderLineDTO orderLine, boolean singlePurchase, Date eventDate) throws SessionInternalError {

        ItemDTO dto = new ItemDTO(
            item.getId(),
            item.getInternalNumber(),
            item.getGlCode(),
            item.getEntity(),
            item.getDescription(languageId),
            item.getDeleted(),
            item.getPriceManual(),
            currencyId,
            item.getPrice(),
            item.getPercentage(),
            null, // to be set right after
            item.getHasDecimals(),
            item.getAssetManagementEnabled(),item.isPercentage());
        
        // if item is also attached to some child entities
        dto.setEntities(item.getEntities());

        dto.setGlobal(item.isGlobal());
        // add all the prices for each currency
        // if this is a percenteage, we still need an array with empty prices
        dto.setPrices(findPrices(entityId, languageId));

        
        // calculate a true price using the pricing plug-in, pricing takes into
        // account plans, special prices and the quantity of the item being purchased
        if (currencyId != null) {
            dto.setPrice(getPrice(userId, currencyId, quantity, entityId, order, orderLine, singlePurchase, eventDate));

            if (item.isPercentage()) {
                dto.setPercentage(dto.getPrice());
                dto.setIsPercentage(true);
            } else {
                dto.setPercentage(null);
                dto.setIsPercentage(false);
            }
        }

        // set the types
        Integer types[] = new Integer[item.getItemTypes().size()];
        int n = 0;
        for (ItemTypeDTO type : item.getItemTypes()) {
            types[n++] = type.getId();
            dto.setOrderLineTypeId(type.getOrderLineTypeId());
        }
        dto.setTypes(types);

        // set excluded types
        Integer excludedTypes[] = new Integer[item.getExcludedTypes().size()];
        int i = 0;
        for (ItemTypeDTO type : item.getExcludedTypes()) {
            excludedTypes[i++] = type.getId();
        }
        dto.setExcludedTypeIds(excludedTypes);

        // set account types
        dto.setStandardAvailability(item.isStandardAvailability());
        Integer accountTypes[] = new Integer[item.getAccountTypeAvailability().size()];
        int j = 0;
		for (AccountTypeDTO type : item.getAccountTypeAvailability()) {
			accountTypes[j++] = type.getId();
		}
		dto.setAccountTypeIds(accountTypes);
        dto.setMetaFields(item.getMetaFields());
        dto.setDependencies(item.getDependencies());
        dto.setOrderLineMetaFields(item.getOrderLineMetaFields());

        dto.setActiveSince(item.getActiveSince());
        dto.setActiveUntil(item.getActiveUntil());
        
        dto.setStandardPartnerPercentage(item.getStandardPartnerPercentage());
        dto.setMasterPartnerPercentage(item.getMasterPartnerPercentage());
        dto.setReservationDuration(Util.convertFromMsToMinutes(item.getReservationDuration()));

        LOG.debug("Got item: %s , price: %s", dto.getId(), dto.getPrice());

        return dto;
    }

    public static final ItemDTO getDTO(ItemDTOEx other) {
        ItemDTO retValue = new ItemDTO();
        CompanyDAS companyDAS = new CompanyDAS();
        
        if (other.getId() != null) {
            retValue.setId(other.getId());
        }

        if(other.getEntityId() != null) {
        	retValue.setEntity(companyDAS.find(other.getEntityId()));
        }
        
        retValue.setNumber(other.getNumber());
        retValue.setGlCode(other.getGlCode());
        retValue.setDeleted(other.getDeleted());
        retValue.setHasDecimals(other.getHasDecimals());
        retValue.setDescription(other.getDescription());
        retValue.setTypes(other.getTypes());
        retValue.setExcludedTypeIds(other.getExcludedTypes());
        retValue.setStandardAvailability(other.isStandardAvailability());
        retValue.setAccountTypeIds(other.getAccountTypes());
        retValue.setPromoCode(other.getPromoCode());
        retValue.setCurrencyId(other.getCurrencyId());
        retValue.setPrice(other.getPriceAsDecimal());
        retValue.setOrderLineTypeId(other.getOrderLineTypeId());

        retValue.setAssetManagementEnabled(other.getAssetManagementEnabled());

        retValue.setDependencies(ItemDependencyBL.toDto(other.getDependencies(), retValue));

        retValue.setEntities(AssetBL.convertToCompanyDTO(other.getEntities()));
        retValue.setGlobal(other.isGlobal());
        retValue.setPriceManual(other.getPriceManual());
        
        /*if(retValue.getEntityId() != null) {
        	MetaFieldBL.fillMetaFieldsFromWS(retValue.getEntityId(), retValue, other.getMetaFieldsMap().get(retValue.getEntityId()));
        }*/
        
        List otherPrices = other.getPrices();
        if (otherPrices != null) {
        List prices = new ArrayList(otherPrices.size());
        for (int i = 0; i < otherPrices.size(); i++) {
	        ItemPriceDTO itemPrice = new ItemPriceDTO();
	        ItemPriceDTOEx otherPrice = (ItemPriceDTOEx) otherPrices.get(i);
	        itemPrice.setId(otherPrice.getId());
	        itemPrice.setCurrency(new CurrencyDAS().find(otherPrice.getCurrencyId()));
	        itemPrice.setPrice(otherPrice.getPriceAsDecimal());
	        itemPrice.setName(otherPrice.getName());
	        itemPrice.setPriceForm(otherPrice.getPriceForm());
	        prices.add(itemPrice);
        }
        	retValue.setPrices(prices);
        }

       	if(retValue.isGlobal()){
            if(retValue.getEntity() != null) {
                List<Integer> allEntities = new CompanyDAS().getChildEntitiesIds(other.getEntityId());
                allEntities.add(other.getEntityId());

                for(Integer id: allEntities){
                    MetaFieldBL.fillMetaFieldsFromWS(id, retValue, other.getMetaFieldsMap().get(id));
                }

       			if (other.getOrderLineMetaFields() != null) {
       	            for (MetaFieldWS metaField : other.getOrderLineMetaFields()) {
       	                retValue.addOrderLineMetaField(MetaFieldBL.getDTO(metaField,retValue.getEntityId()));    
       	            }
       	        }
       		}
       	} else {
       		for(Integer id : other.getEntities()) {
       			MetaFieldBL.fillMetaFieldsFromWS(id, retValue, other.getMetaFieldsMap().get(id));
       		}
        }
        
       	if (other.getOrderLineMetaFields() != null) {
            for (MetaFieldWS metaField : other.getOrderLineMetaFields()) {
                retValue.addOrderLineMetaField(MetaFieldBL.getDTO(metaField,retValue.getEntityId()) );
            }
        }
       	
       	// convert PriceModelWS to PriceModelDTO
       	CompanyDTO priceModelCompany = other.getPriceModelCompanyId() != null ? companyDAS.find(other.getPriceModelCompanyId()) : null;
        retValue.setPriceModelCompanyId(other.getPriceModelCompanyId());

        retValue.setMasterPartnerPercentage(other.getMasterPartnerPercentageAsDecimal());
        retValue.setStandardPartnerPercentage(other.getStandardPartnerPercentageAsDecimal());
        
        // #7514 - Plans Enhancement
        retValue.setActiveSince(other.getActiveSince());
        retValue.setActiveUntil(other.getActiveUntil());

        /* #10256 - Asset Reservation */
//        We always need to save asset value as it's not nullable field in database.
        if (other.getReservationDuration() == null || other.getReservationDuration() == 0) {
//            If other.getReservationDuration() does not have any value, we should add default value saved in Preference.
            retValue.setReservationDuration(Util.convertFromMinutesToMs(Integer.parseInt(PreferenceBL.getPreferenceValue(other.getEntityId(), CommonConstants.PREFERENCE_ASSET_RESERVATION_DURATION))));
        } else {
            retValue.setReservationDuration(Util.convertFromMinutesToMs(other.getReservationDuration()));
        }
        
        return retValue;
    }

    public ItemDTOEx getWS(ItemDTO other) {
    	if (other == null) {
            other = item;
        }
    	
    	return getItemDTOEx(other);
    }
    
    public static ItemDTOEx getItemDTOEx(ItemDTO other){
    	
        ItemDTOEx retValue = new ItemDTOEx();
        retValue.setId(other.getId());
        
        if(other.getEntity() != null) {
        	retValue.setEntityId(other.getEntity().getId());
        }

        if(other.getEntities() != null) {
        	retValue.setEntities(other.getChildEntitiesIds());
        }
        
        retValue.setNumber(other.getInternalNumber());
        retValue.setGlCode(other.getGlCode());
        retValue.setDeleted(other.getDeleted());
        retValue.setHasDecimals(other.getHasDecimals());
        retValue.setDescription(other.getDescription());
        retValue.setTypes(other.getTypes());
        retValue.setExcludedTypes(other.getExcludedTypeIds());
        retValue.setAccountTypes(other.getAccountTypeIds());
        retValue.setStandardAvailability(other.isStandardAvailability());
        retValue.setPromoCode(other.getPromoCode());
        retValue.setCurrencyId(other.getCurrencyId());
        retValue.setPrice(other.getPrice());
        retValue.setOrderLineTypeId(other.getOrderLineTypeId());

        retValue.setAssetManagementEnabled(other.getAssetManagementEnabled());
        retValue.setOrderLineMetaFields(new MetaFieldWS[other.getOrderLineMetaFields().size()]);
        int index = 0;
        for(MetaField metaField :  other.getOrderLineMetaFields()) {
            retValue.getOrderLineMetaFields()[index] = MetaFieldBL.getWS(metaField);
            index++;
        }

        retValue.setDependencies(ItemDependencyBL.toWs(other.getDependencies()));
        
        retValue.setGlobal(other.isGlobal());
        
        // Get meta field values of all the entities and then set them
        // set each entity's meta fields in map also
        MetaFieldValueWS[] metaFields = null;


        if(other.isGlobal()) {
            List<Integer> companyIds = new CompanyDAS().getChildEntitiesIds(other.getEntityId());
            companyIds.add(other.getEntityId());
        	for (int entityId :companyIds) {
        		MetaFieldValueWS[] childMetaFields = MetaFieldBL.convertMetaFieldsToWS(entityId, other);
        		retValue.getMetaFieldsMap().put(entityId, childMetaFields);
        		
        		metaFields = (MetaFieldValueWS[]) ArrayUtils.addAll(metaFields, childMetaFields);
        	}
        } else {
        	for (int entityId : other.getChildEntitiesIds()) {
        		MetaFieldValueWS[] childMetaFields = MetaFieldBL.convertMetaFieldsToWS(entityId, other);
        		retValue.getMetaFieldsMap().put(entityId, childMetaFields);
        		
        		metaFields = (MetaFieldValueWS[]) ArrayUtils.addAll(metaFields, childMetaFields);
        	}
        }
        retValue.setMetaFields(metaFields);

        retValue.setStandardPartnerPercentage(other.getStandardPartnerPercentage());
        retValue.setMasterPartnerPercentage(other.getMasterPartnerPercentage());
        
        retValue.setActiveSince(other.getActiveSince());
        retValue.setActiveUntil(other.getActiveUntil());
        retValue.setReservationDuration(other.getReservationDuration());
        
        retValue.setPrices(other.getPrices());
        retValue.setPriceManual(other.getPriceManual());

        return retValue;
    }

    /**
     * @return
     */
    public String getPriceCurrencySymbol() {
        return priceCurrencySymbol;
    }

    /**
     * Returns all items for the given entity.
     * @param entityId
     * The id of the entity.
     * @return an array of all items
     */
    public ItemDTOEx[] getAllItems(Integer entityId) {
    	EntityBL entityBL = new EntityBL(entityId);
        CompanyDTO entity = entityBL.getEntity();
        Collection<ItemDTO> itemEntities = itemDas.findByEntityId(entityId);
        ItemDTOEx[] items = new ItemDTOEx[itemEntities.size()];

        // iterate through returned item entities, converting them into a DTO
        int index = 0;
        for (ItemDTO item : itemEntities) {
		    set(item.getId());
            items[index++] = getWS(getDTO(entity.getLanguageId(), null, entityId, entity.getCurrencyId()));
        }

        return items;
    }

    /**
     * Returns all items for the given item type (category) id. If no results
     * are found an empty array is returned.
     *
     * @see ItemDAS#findAllByItemType(Integer)
     *
     * @param itemTypeId item type (category) id
     * @param entityId	company id of which price will be used
     * @return array of found items, empty if none found
     */
    public static ItemDTOEx[] getAllItemsByType(Integer itemTypeId, Integer entityId) {
        List<ItemDTO> results = new ItemDAS().findAllByItemType(itemTypeId);
        ItemDTOEx[] items = new ItemDTOEx[results.size()];

        int index = 0;
        for (ItemDTO item : results) {
        	// set caller company id of item to get price model
        	item.setPriceModelCompanyId(entityId);
        	items[index++] = getItemDTOEx(item);
        }

        return items;
    }

    public void setPricingFields(List<PricingField> fields) {
        pricingFields = fields;
    }

    public List<PricingField> getPricingFields(){
        return pricingFields;
    }


    public void validateUniqueProductCode(ItemDTO dto, Set<Integer> entities, boolean isNew){
        LOG.debug("Validating product code : "+dto.getNumber());
        if(forceUniqueProductCode(entities) && !isUniqueProductCode(dto, entities, isNew)){
            throw new SessionInternalError("Product Number Is A Duplicate", new String[] {
                    "ItemDTOEx,number,validation.duplicate.error"
            });
        }
    }

    public boolean forceUniqueProductCode(Set<Integer> entities){
        int preferenceUniqueProductCode = 0;
        boolean value = false;
        try {
        	for (Integer entityId : entities) {
        		preferenceUniqueProductCode =
        				PreferenceBL.getPreferenceValueAsIntegerOrZero(
        						entityId, ServerConstants.PREFERENCE_UNIQUE_PRODUCT_CODE);
        		if (1 == preferenceUniqueProductCode) {
        			value = true;
        		}
        	}
        } catch (EmptyResultDataAccessException e) {
            // default will be used
        }

        return value;
    }

    public static boolean isUniqueProductCode(ItemDTO item, Integer entityId, boolean isNew) {
        Long productCodeUsageCount = new ItemDAS().findProductCountByInternalNumber(item.getInternalNumber(), entityId, isNew, item.getId());
        if(productCodeUsageCount == 0) {
            LOG.debug("Its a unique product code ");
            return true;
        }
        LOG.debug("Its a duplicate product code");
        return false;
    }

    public static boolean isUniqueProductCode(ItemDTO item, Set<Integer> entities, boolean isNew) {
    	for (Integer entityId : entities) {
    		Long productCodeUsageCount = new ItemDAS().findProductCountByInternalNumber(item.getInternalNumber(), entityId, isNew, item.getId());
    		if(productCodeUsageCount != 0) {
                LOG.debug("Its a duplicate product code ");
                return false;
            }
    	}
        LOG.debug("Its a unique product code");
        return false;
    }

    private void validateProductOrderLinesMetaFields(Collection<MetaField> newMetaFields) throws SessionInternalError {
        Collection<MetaField> currentMetaFields = item != null && item.getId() > 0 ? item.getOrderLineMetaFields() : new LinkedList<MetaField>();
        MetaFieldBL.validateMetaFieldsChanges(newMetaFields, currentMetaFields);
    }

    /**
     * Save new metafields, update existed meta fields, return ID of meta fields to remove
     * @param newMetaFields collection of entered metafileds
     * @return collection of IDs of metafields to be removed
     */
    private Collection<Integer> updateProductOrderLineMetaFields(Collection<MetaField> newMetaFields) {
        return MetaFieldBL.updateMetaFieldsCollection(newMetaFields, item.getOrderLineMetaFields());
    }

    /**
     * This method removes MetaFields, that no longer used by product. No validation is performed
     * Cal this method after removing links to MetaField from other entitties in DB
     * @param unusedMetaFieldIds ids of metafields for remove
     */
    private void deleteUnusedProductOrderLineMetaFields(Collection<Integer> unusedMetaFieldIds) {
        MetaFieldBL metaFieldBL = new MetaFieldBL();
        //delete metafields not linked to the product anymore
        for(Integer id : unusedMetaFieldIds) {
            metaFieldBL.delete(id);
        }
    }

    /**
     * Merge properties from dto metafield to persisted one
     * @param destination persisted metafield
     * @param source dto metafield with updated properties
     */
    private void mergeBasicProperties(MetaField destination, MetaField source) {
       destination.setName(source.getName());
       destination.setPrimary(source.getPrimary());
       destination.setValidationRule(source.getValidationRule());
       destination.setDataType(source.getDataType());
       destination.setDefaultValue(source.getDefaultValue());
       destination.setDisabled(source.isDisabled());
       destination.setMandatory(source.isMandatory());
       destination.setDisplayOrder(source.getDisplayOrder());
       destination.setFieldUsage(source.getFieldUsage());
   }

    /**
     * Calculates all the parents and childs of a given id
     * 
     * @param entityId
     * @return
     */
    public List<Integer> getParentAndChildIds(Integer entityId) {
    	 Integer parentId = getRootEntityId(entityId);
         List<Integer> entities = new ArrayList<Integer>(0);
         
         entities.add(parentId);
         entities.addAll(findChilds(parentId));
         
         return entities;
    }
    
    private List<Integer> findChilds(Integer parentId) {
    	List<Integer> entities = new ArrayList<Integer>();
    	
    	List<CompanyDTO> childs = new CompanyDAS().findChildEntities(parentId);
    	for(CompanyDTO child : childs) {
    		entities.add(child.getId());
    		entities.addAll(findChilds(child.getId()));
    	}
    	
    	return entities;
    }
    
    private Integer getRootEntityId(Integer entityId) {
    	CompanyDTO company = new CompanyDAS().find(entityId);
    	if(company.getParent() == null) {
    		return entityId;
    	} else {
    		return getRootEntityId(company.getParent().getId());
    	}
    }
    
    
    /**
     * Get all items by given company
     */
    public List<ItemDTOEx> getAllItemsByEntity(Integer entityId) {
        EntityBL entityBL = new EntityBL(entityId);
        CompanyDTO entity = entityBL.getEntity();
        CompanyDAS das = new CompanyDAS();

        boolean isRoot = das.isRoot(entityId);
        List<Integer> allCompanies = das.getChildEntitiesIds(entityId);
        allCompanies.add(entityId);

        List<ItemDTO> items = itemDas.findItems(entityId, allCompanies, isRoot);

        List<ItemDTOEx> ws = new ArrayList<ItemDTOEx>();
        for (ItemDTO item : items) {
            set(item.getId());
            ws.add(getWS(getDTO(entity.getLanguageId(), null, entityId, entity.getCurrencyId())));
        }

        return ws;
    }    
    
    /**
     * This method will try to find a currency id for this item. It will
     * give priority to the entity's default currency, otherwise anyone
     * will do.
     * @return
     */
    private List findPrices(Integer entityId, Integer languageId) {
        List retValue = new ArrayList();

        // go over all the curencies of this entity
        for (CurrencyDTO currency: item.getEntity().getCurrencies()) {
            ItemPriceDTO price = new ItemPriceDTO();
            price.setCurrency(currency);
            price.setName(currency.getDescription(languageId));
            // se if there's a price in this currency
            ItemPriceDTO priceRow = new ItemPriceDAS().find(
                item.getId(),currency.getId());
            if (priceRow != null) {
                price.setPrice(priceRow.getPrice());
                price.setPriceForm(price.getPrice().toString());
            }
            retValue.add(price);
        }

        return retValue;
    }
    
    public void invalidateCache() {
    	cache.flushCache(flushModel);
	}

}
