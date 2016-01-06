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
package com.sapienter.jbilling.server.item.tasks;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderHelper;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Record;
import org.hibernate.StaleObjectStateException;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

;


public class BasicItemManager implements IItemPurchaseManager {

    private static final FormatLogger LOG = new FormatLogger(BasicItemManager.class);
    private static final int TRANSACTION_RETRIES = 10;
    private ItemDTO item = null;
    private OrderLineDTO latestLine = null;

    protected FormatLogger getLog() { return LOG; }

    public void addItem(Integer itemId, BigDecimal quantity, Integer languageId, Integer userId, Integer entityId,
                        Integer currencyId, OrderDTO order, List<Record> records,
                        List<OrderLineDTO> lines, boolean singlePurchase, String sipUri, Date eventDate) throws TaskException {

        LOG.debug("Adding %s of item %s to order %s", quantity, itemId, order);

        ItemBL item = new ItemBL(itemId);

        // validate decimal quantity
        if (quantity.remainder(Constants.BIGDECIMAL_ONE).compareTo(BigDecimal.ZERO) > 0) {
            if (item.getEntity().getHasDecimals().equals(0)) {
                latestLine = null;
                throw new ItemDecimalsException("Item " + itemId + " does not allow decimal quantities.");
            }
        }

        // build the order line
        OrderLineDTO newLine = getOrderLine(itemId, languageId, userId, currencyId, quantity, entityId, order,
                                            records, lines, singlePurchase, sipUri, eventDate);

        // check if line already exists on the order & update
        OrderLineDTO oldLine = order.getLine(itemId);
        if (oldLine != null && lines != null) {
            OrderLineDTO updatedOldLine = OrderHelper.findOrderLineWithId(lines, oldLine.getId());
            if (updatedOldLine != null && updatedOldLine.getDeleted() == 1) {
                oldLine = null;
            }
        }
        if (oldLine == null) {
            addNewLine(order, newLine);
        } else {
            updateExistingLine(order, newLine, oldLine);
        }
    }

    /**
     * Add a new line to the order
     *
     * @param order order
     * @param newLine new line to add
     */
    protected void addNewLine(OrderDTO order, OrderLineDTO newLine) {
        LOG.debug("Adding new line to order: %s", newLine);

        newLine.setPurchaseOrder(order);
        order.getLines().add(newLine);

        this.latestLine = newLine;
    }

    /**
     * Update an existing line on the order with the quantity and dollar amount of the new line.
     *
     * @param order order
     * @param newLine new order line
     * @param oldLine existing order line to be updated
     */
    protected void updateExistingLine(OrderDTO order, OrderLineDTO newLine, OrderLineDTO oldLine) throws TaskException{
        //todo: update order line assets
        PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);

        try {

            //try to commit transaction with retry
            Exception exception = null;

            int numAttempts = 0;
            do {
                numAttempts++;
                try {
                    TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));

                        oldLine = new OrderLineDAS().findNow(oldLine.getId());

                        LOG.debug("Updating existing order with line quantity & amount: %s", newLine);

                        BigDecimal quantity = oldLine.getQuantity().add(newLine.getQuantity());
                        oldLine.setQuantity(quantity);

                        BigDecimal amount = oldLine.getAmount().add(newLine.getAmount());
                        oldLine.setAmount(amount);

                        this.latestLine = oldLine;

                    transaction.flush();

                    return;

                } catch (Exception ex) {
                    if (ex instanceof HibernateOptimisticLockingFailureException ||
                            ex instanceof StaleObjectStateException) {
                        new OrderLineDAS().clear();
                        exception = ex;
                        //transactionManager.rollback(transaction);
                        LOG.debug("Could not commit transaction.", ex);
                        //wait 100 milliseconds
                        Thread.sleep(100);
                    } else {
                        throw new PluggableTaskException(ex);
                    }
                }
                LOG.debug("Updating order line retry %d", numAttempts);
            } while (numAttempts <= TRANSACTION_RETRIES);
            LOG.error("Failed to update order line after %d retries", TRANSACTION_RETRIES);
            throw exception;
        } catch (Exception e) {
            LOG.error("An exception ocurred.", e);
            throw new TaskException(e);
        }
    }

    /**
     * Builds a new order line for the given item, currency and user. The item will be priced according
     * to the quantity purchased, the order it is being added to and the user's own prices.
     *
     * @see ItemBL#getDTO(Integer, Integer, Integer, Integer, java.math.BigDecimal, com.sapienter.jbilling.server.order.db.OrderDTO)
     *
     * @param itemId item id
     * @param languageId language id
     * @param userId user id
     * @param currencyId currency id
     * @param quantity quantity being purchased
     * @param entityId entity id
     * @param order order the line will be added to
     * @return new order line
     */
    protected OrderLineDTO getOrderLine(Integer itemId, Integer languageId, Integer userId, Integer currencyId,
                                        BigDecimal quantity, Integer entityId, OrderDTO order, List<Record> records,
                                        List<OrderLineDTO> lines, boolean singlePurchase, String sipUri, Date eventDate) {

        // item BL with pricing fields
        ItemBL itemBl = new ItemBL(itemId);
        if (records != null) {
            List<PricingField> fields = new ArrayList<PricingField>();
            for (Record record : records) {
                PricingField.addAll(fields, record.getFields());
            }

            LOG.debug("Including %d field(s) for pricing.", fields.size());
            itemBl.setPricingFields(fields);
        }

        // get the item with the price populated for the quantity being purchased
        this.item = itemBl.getDTO(languageId, userId, entityId, currencyId, quantity, order, null, singlePurchase, eventDate);
        LOG.debug("Item %s priced as %s", itemId, item.getPrice());

        // build the order line
        OrderLineDTO line = new OrderLineDTO();
        line.setItem(item);
        line.setQuantity(quantity != null ? quantity : BigDecimal.ZERO);

        // set line price
        line.setPrice( null != item.getPrice() ? item.getPrice() : BigDecimal.ZERO );

        // calculate total line dollar amount
        if ( item.isPercentage() ) {
            line.setAmount(item.getPercentage());
        } else {
	        if ( null != line.getPrice() && null != line.getQuantity() ) {
	            line.setAmount(line.getPrice().multiply(line.getQuantity()));
	        } else {
	            line.setAmount(BigDecimal.ZERO);
	        }
        }    

        // round dollar amount
        if ( null != line.getAmount())
            line.setAmount(line.getAmount().setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND));

        line.setDeleted(0);
        line.setTypeId(item.getOrderLineTypeId());
        line.setEditable(OrderBL.lookUpEditable(item.getOrderLineTypeId()));
        line.setDefaults();
        	

        LOG.debug("Built new order line: %s", line);

        return line;
    }
    //todo: temp fix for NonUniqueObjectException, rewrite
    private AssetDTO findAssetEntityOrDto(Integer assetId, OrderDTO order) {
        AssetBL assetBL = new AssetBL();
        AssetDTO entity = assetBL.find(assetId);
        // return dto for new order
        if (order == null || order.getId() == null || order.getId() <= 0) {
            return new AssetDTO(entity);
        } else {
            // compare current order with ref in hibernate context
            OrderDAS orderDAS = new OrderDAS();
            if (orderDAS.find(order.getId()) == order) {
                return entity; // return entity for persisted order
            } else {
                return new AssetDTO(entity);
            }
        }
    }

    public ItemDTO getItem() {
        return item;
    }

    public OrderLineDTO getLatestLine() {
        return latestLine;
    }
}
