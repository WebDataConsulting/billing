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

package com.sapienter.jbilling.server.order;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;


import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springmodules.cache.CachingModel;
import org.springmodules.cache.FlushingModel;
import org.springmodules.cache.provider.CacheProviderFacade;

public class CurrentOrder {
    private static final FormatLogger LOG = new FormatLogger(CurrentOrder.class);

    private final EventLogger eLogger = EventLogger.getInstance();

    private final Date eventDate;
    private final Integer userId;
    private final UserBL user;

    // current order
    private OrderBL orderBl = null;

    // cache management
    private CacheProviderFacade cache;
    private CachingModel cacheModel;
    private FlushingModel flushModel;

    public CurrentOrder(Integer userId, Date eventDate) {
        if (userId == null) throw new IllegalArgumentException("Parameter userId cannot be null!");
        if (eventDate == null) throw new IllegalArgumentException("Parameter eventDate cannot be null!");

        this.userId = userId;
        this.eventDate = eventDate;
        this.user = new UserBL(userId);

        cache = (CacheProviderFacade) Context.getBean(Context.Name.CACHE);
        cacheModel = (CachingModel) Context.getBean(Context.Name.CACHE_MODEL_RW);
        flushModel = (FlushingModel) Context.getBean(Context.Name.CACHE_FLUSH_MODEL_RW);

        LOG.debug("Current order constructed with user %s event date %s", userId, eventDate);
    }
    
    /**
     * Returns the ID of a one-time order, where to add an event.
     *
     *
     * @return order ID of the current order
     */
    public Integer getCurrent() {

        // find in the cache
        String cacheKey = userId.toString() + Util.truncateDate(eventDate);
        Integer currentOrder = (Integer) cache.getFromCache(cacheKey, cacheModel);
        LOG.debug("Retrieved from cache '%s', order id: %s", cacheKey, currentOrder);

        // a hit is only a hit if the order is still active and is not deleted. Sometimes when the order gets deleted
        // it wouldn't be removed from the cache.
        OrderDTO cachedOrder = new OrderDAS().findByIdAndIsDeleted(currentOrder, false);
        if (cachedOrder != null && OrderStatusFlag.INVOICE.equals(cachedOrder.getOrderStatus().getOrderStatusFlag())) {
            LOG.debug("Cache hit for %s", currentOrder);
            return currentOrder;
        }

        MainSubscriptionDTO mainSubscription = user.getEntity().getCustomer().getMainSubscription();
        Integer entityId = null;
        Integer currencyId = null;
        if (mainSubscription == null) {
            return null;
        }

        // find user entity & currency
        try {
            entityId = user.getEntity().getCompany().getId();
            currencyId = user.getEntity().getCurrency().getId();
        } catch (Exception e) {
            throw new SessionInternalError("Error looking for user entity of currency", CurrentOrder.class, e);
        }
        
        // if main subscription preference is not set 
        // do not use the main subscription
        if (!isMainSubscriptionUsed(entityId)) {
        	return null;
        }

        boolean orderFound = false;
	    if (orderBl == null) {orderBl = new OrderBL();}

	    /* Previous implementation was going in future until an open one-time order is
	     * found or until a period where an open one-time order did not exist and can be
	     * created. This logic is not valid anymore but the implementation logic in
	     * date calculation was preserved*/
	    final Date newOrderDate = calculateDate(0, mainSubscription);
	    LOG.debug("Calculated one timer date: %s", newOrderDate);

        if (newOrderDate == null) {
            // this is an error, there isn't a good date give
            // the event date and the main subscription order
            LOG.error("Could not calculate order date for event. Event date is before the order active since date.");
            return null;
        }

        // now that the date is calculated, let's see if there is
        // a one-time order for that date that is still open
	    boolean somePresent = false;
	    try {
		    List<OrderDTO> rows = new OrderDAS().findOneTimersByDate(userId, newOrderDate);
		    LOG.debug("Found %s one-time orders for new order date: %s", rows.size(), newOrderDate);

		    //TODO (VCA): the following code does not discriminate between any one time order
		    //and one time order that collects traffic. It is unlikely but it could happen
		    //that a user creates a one time order on a specific date and then this order
		    //is picked up by this code as the current order. We need to fix this scenario.
		    for (OrderDTO oneTime : rows) {
			    somePresent = true;
			    orderBl.set(oneTime.getId());//init so we can check the order status
			    if (orderBl.getEntity().getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
				    LOG.debug("Found one timer %s but status is finished", oneTime.getId());
			    } else {
				    LOG.debug("Found existing one-time order");
				    orderFound = true;
				    break;
			    }
		    }
	    } catch (Exception e) {
		    throw new SessionInternalError("Error looking for one time orders", CurrentOrder.class, e);
	    }

        if (somePresent && !orderFound) {
	        LOG.debug("One time orders (current) were present were found for the given date but with FINISHED status");
            eLogger.auditBySystem(entityId, userId,
                                  ServerConstants.TABLE_PUCHASE_ORDER,
		                          orderBl.getEntity().getId(),
                                  null,
                                  EventLogger.CURRENT_ORDER_FINISHED,
                                  null, null, null);

        }

	    if (!orderFound) {
            // there aren't any one-time orders for this date that are 'open'. Create one.
            Integer newOrderId = create(newOrderDate, currencyId, entityId);
            LOG.debug("Created new one-time order, Order ID: %s", newOrderId);
        }

        currentOrder = orderBl.getEntity().getId();

        LOG.debug("Caching order %s with key '%s'", currentOrder, cacheKey);
        cache.putInCache(cacheKey, cacheModel, currentOrder);

        LOG.debug("Returning %s", currentOrder);
        return currentOrder;
    }
    
    /**
     * Assumes that main subscription already exists for the customer
     * @param futurePeriods date for N periods into the future
     * @param mainSubscription Customer main subscription
     * @return calculated period date for N future periods
     */
    private Date calculateDate(int futurePeriods, MainSubscriptionDTO mainSubscription) {
    	
        GregorianCalendar cal = new GregorianCalendar();

        LOG.debug("To begin with eventDate is %s", eventDate);

        // calculate the event date with the added future periods
        // default cal to actual event date
        Date actualEventDate = eventDate;
        cal.setTime(actualEventDate);
        
        for (int f = 0; f < futurePeriods; f++) {
        	if (CalendarUtils.isSemiMonthlyPeriod(mainSubscription.getSubscriptionPeriod().getPeriodUnit())) {
        		cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
        	} else {
        		cal.add(MapPeriodToCalendar.map(mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId()), 
                                            mainSubscription.getSubscriptionPeriod().getValue());
        	}
        }
        // set actual event date based on future periods
        actualEventDate = cal.getTime();
        
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.DATE, mainSubscription.getNextInvoiceDayOfPeriod() - 1);

        while (cal.getTime().after(actualEventDate)) {
        	cal.add(MapPeriodToCalendar.map(mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId()), 
            		-mainSubscription.getSubscriptionPeriod().getValue());
        }

        LOG.debug("After period adjustment, the date arrived for current order is %s", cal.getTime());

        return cal.getTime();
    }

    private boolean isMainSubscriptionUsed(Integer entityId) {
        int preferenceUseCurrentOrder = 0;
        try {
            preferenceUseCurrentOrder = 
            	PreferenceBL.getPreferenceValueAsIntegerOrZero(
            		entityId, ServerConstants.PREFERENCE_USE_CURRENT_ORDER);
        } catch (EmptyResultDataAccessException e) {
            // default preference will be used
            }
        
        return preferenceUseCurrentOrder != 0;
	}

	/**
     * Creates a new one-time order for the given active since date.
     * @param activeSince active since date
     * @param currencyId currency of order
     * @param entityId company id of order
     * @return new order
     */
    public Integer create(Date activeSince, Integer currencyId, Integer entityId) {
        OrderDTO currentOrder = new OrderDTO();
        currentOrder.setCurrency(new CurrencyDTO(currencyId));

        // notes
        try {
            EntityBL entity = new EntityBL(entityId);
            ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", entity.getLocale());
            currentOrder.setNotes(bundle.getString("order.current.notes"));
        } catch (Exception e) {
            throw new SessionInternalError("Error setting the new order notes", CurrentOrder.class, e);
        } 

        currentOrder.setActiveSince(activeSince);
        
        // create the order
        if (orderBl == null) {
            orderBl = new OrderBL();
        }

	    orderBl.set(currentOrder);
	    orderBl.addRelationships(userId, ServerConstants.ORDER_PERIOD_ONCE, currencyId);

        return orderBl.create(entityId, null, currentOrder);
    }
}
