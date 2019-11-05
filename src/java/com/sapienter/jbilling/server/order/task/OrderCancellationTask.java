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
package com.sapienter.jbilling.server.order.task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineTypeDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.event.NewActiveUntilEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.ServerConstants;

/**
 * Order Cancellation Task
 *
 * parameters:
 *      fee_item_id = id of the fee product that is applied when the order is cancelled
 *
 * @author Bilal Nasir
 */
public class OrderCancellationTask extends PluggableTask implements IInternalEventsTask {
    private static final FormatLogger LOG = new FormatLogger(OrderCancellationTask.class);

    public static final ParameterDescription FEE_ITEM_ID = new ParameterDescription("fee_item_id", true, ParameterDescription.Type.STR);

    {	
        descriptions.add(FEE_ITEM_ID);
	}

    private enum EventType {
        NEW_ACTIVE_UNTIL_EVENT
	}

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
        NewActiveUntilEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
	}

    private Date newActiveUntil = null;
    private Date oldActiveUntil = null;
    private OrderDTO order = null;
    private Integer entityId = null;
    private Integer fee_item_id = null;

    public void process(Event event) throws PluggableTaskException {

        EventType eventType = null;

        // validate the type of the event
        if (event instanceof NewActiveUntilEvent) {
            NewActiveUntilEvent myEvent = (NewActiveUntilEvent) event;
            order = new OrderDAS().find(myEvent.getOrderId());
            Date previousActiveUntil = myEvent.getOldActiveUntil();
            if(oldActiveUntil == null){
			    Calendar aCalendar = Calendar.getInstance();  
                aCalendar.setTime(order.getActiveSince());  
                aCalendar.add(Calendar.MONTH, order.getCancellationMinimumPeriod());
                previousActiveUntil = aCalendar.getTime();
            }
            // if the new active until is later than the old one
            // or the new one is null don't process
            if (myEvent.getNewActiveUntil() == null
            	    || (myEvent.getNewActiveUntil().after(previousActiveUntil))) {
			           LOG.debug("New active until is not earlier than old one. Skipping cancellation fees. Order id %s", myEvent.getOrderId());
		        return;
			}
            setOldActiveUntil(previousActiveUntil);
            
            eventType = EventType.NEW_ACTIVE_UNTIL_EVENT;

        } else {
            throw new SessionInternalError("Can't process anything but a new active until event");
        }

        LOG.debug("Processing event %s for cancellation fee", event);

        if (event != null && eventType == EventType.NEW_ACTIVE_UNTIL_EVENT) {
            NewActiveUntilEvent myEvent = (NewActiveUntilEvent) event;
            setNewActiveUntil(myEvent.getNewActiveUntil());
        }

        entityId = event.getEntityId();
        validateParameters();

        // apply the fee
        applyFee(fee_item_id);
    }

    private void validateParameters() {
        try {
    	    fee_item_id = Integer.parseInt(parameters.get(FEE_ITEM_ID.getName()));
        } catch (NumberFormatException e) {
    	    LOG.error("Invalid paramters, they should be integers", e);
            throw new SessionInternalError("Invalid parameters for Cancellation fee plug-in. They should be integers", e);
        }

        if (new ItemDAS().findNow(fee_item_id) == null) {
    	    String message = "Invalid parameters, fee_item_id"+ fee_item_id + " does not exist.";
    	    LOG.error(message);
    	    throw new SessionInternalError(message);
        }

        LOG.debug("Parameters set to cancel =  fee = %s", fee_item_id);
    }

	/**
	 * apply the order cancellation fee on the order whose activeUntil changed
	 * @param itemId id of the item which will be used as fee
	 */
     private void applyFee(Integer itemId) {
        ResourceBundle bundle;
        UserBL userBL;
        try {
            userBL = new UserBL(order.getBaseUserByUserId().getId());
            bundle = ResourceBundle.getBundle("entityNotifications", userBL.getLocale());
        } catch (Exception e) {
            throw new SessionInternalError("Error when doing credit", RefundOnCancelTask.class, e);
        }

        // now create a new order for the fee:
        // - one time
        // - item from the parameter * number of periods being cancelled
        OrderDTO feeOrder = new OrderDTO();
        feeOrder.setMetaFields(order.getMetaFields()); // get the metafields from the original order
        feeOrder.setBaseUserByUserId(order.getBaseUserByUserId());
        feeOrder.setCurrency(order.getCurrency());
        feeOrder.setNotes("This order was automatically created because the main subscription was cancelled before the minimum required period.");
        feeOrder.setOrderPeriod(new OrderPeriodDAS().find(ServerConstants.ORDER_PERIOD_ONCE));

        Integer fee = null ;
        BigDecimal feePercent = null ;
        BigDecimal calculatedFeeByMonth = null;
        if(order.getCancellationFeeType().equals("ZERO")){
            fee = order.getCancellationFee();
        }else if(order.getCancellationFeeType().equals("PERCENTAGE")){


            BigDecimal periods;

            if (oldActiveUntil == null) {
                periods = new BigDecimal(1);
                LOG.info("Old active until not present. Period will be 1.");
            } else {
                long totalMills = oldActiveUntil.getTime() - newActiveUntil.getTime();
                BigDecimal periodMills = new BigDecimal(30)
                .multiply(new BigDecimal(24))
                .multiply(new BigDecimal(60))
                .multiply(new BigDecimal(60))
                .multiply(new BigDecimal(1000));

                periods = new BigDecimal(totalMills).divide(periodMills, ServerConstants.BIGDECIMAL_SCALE, ServerConstants.BIGDECIMAL_ROUND);
            }
            calculatedFeeByMonth = order.getTotal().multiply(periods).setScale(2, BigDecimal.ROUND_UP);
            if(order.getCancellationMaximumFee() != null){
                if(calculatedFeeByMonth.intValue()<order.getCancellationMaximumFee()){
                    feePercent = (calculatedFeeByMonth.multiply(new BigDecimal(order.getCancellationFeePercentage()))).divide(new BigDecimal(100));
                }
                else{
                    feePercent = new BigDecimal(order.getCancellationMaximumFee());
                }
            }else{
                 feePercent = (calculatedFeeByMonth.multiply(new BigDecimal(order.getCancellationFeePercentage()))).divide(new BigDecimal(100));
            }
        }

        // now the line
        ItemDTO feeItem = new ItemDAS().find(itemId);
        OrderLineDTO feeLine = new OrderLineDTO();
        feeLine.setDeleted(0);
        feeLine.setDescription(feeItem.getDescription(userBL.getEntity().getLanguageIdField()));
        feeLine.setItem(feeItem);
        feeLine.setOrderLineType(new OrderLineTypeDAS().find(ServerConstants.ORDER_LINE_TYPE_ITEM));
        feeLine.setPurchaseOrder(feeOrder);
        if(order.getCancellationFeeType().equals("ZERO")){
            feeLine.setAmount(new BigDecimal(fee).setScale(2, BigDecimal.ROUND_UP));
            feeLine.setPrice(new BigDecimal(fee).setScale(2, BigDecimal.ROUND_UP));
        }else if(order.getCancellationFeeType().equals("PERCENTAGE")){
            feeLine.setAmount(feePercent.setScale(2, BigDecimal.ROUND_UP));
            feeLine.setPrice(feePercent.setScale(2, BigDecimal.ROUND_UP));
        }
        feeOrder.addLine(feeLine);
        feeLine.setQuantity(1);

        OrderBL orderBL = new OrderBL(feeOrder);
        try {
            orderBL.recalculate(entityId);
        } catch (ItemDecimalsException e) {
            throw new SessionInternalError(e);
        }

        Integer feeOrderId = orderBL.create(entityId, null, feeOrder);
        LOG.debug("New fee order created: %s for cancel of %s", feeOrderId, order.getId());
    }

    public Date getNewActiveUntil() {
        return newActiveUntil;
    }

    public void setNewActiveUntil(Date activeSince) {
        this.newActiveUntil = activeSince;
    }

    public Date getOldActiveUntil() {
        return oldActiveUntil;
    }

    public void setOldActiveUntil(Date activeUntil) {
        this.oldActiveUntil = activeUntil;
    }

    public Integer getFee_item_id() {
        return fee_item_id;
    }

    public void setFee_item_id(Integer fee_item_id) {
        this.fee_item_id = fee_item_id;
    }
}
