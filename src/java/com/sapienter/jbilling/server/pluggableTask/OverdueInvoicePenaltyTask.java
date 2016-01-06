/**
 * 
 */
package com.sapienter.jbilling.server.pluggableTask;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;

;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderBillingTypeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.AboutToGenerateInvoices;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;

/**
 * The OverdueInvoicePenaltyTask is similar to the BasicPenaltyTask
 * The purpose is similar to create a Penalty order but in this case the 
 * it is not a one-time penalty order but a recurring Penalty Order that stays 
 * as long as that particular Invoice is overdue.
 * 
 * This task is powerful because it performs this action just before the Billing process collects orders. 
 * Thus it is different from BasicPenaltyTask, which is actually dependent on the Ageing Process to 
 * initiate action. Any overdue Invoice cannot be missed because this task performs its action just before the Billing 
 * Process collects orders.
 * 
 * It uses the same plugin parameters as the BasicPenaltyTask, the actual values of the params
 * may differ to acheive a different result or Penalty Description.
 * 
 * Once, the offending Invoice has been paid, the related Order should be set to have
 * an activeUntil date value of the date on which it has been fully paid.
 *
 * For this penalty to be pro-rated, a DynamicBalanceManagerTask must be configured.
 * 
 * @author Vikas Bodani
 * @since 05-Jun-2012
 *
 */
public class OverdueInvoicePenaltyTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(OverdueInvoicePenaltyTask.class);
    
    public static final ParameterDescription PARAMETER_ITEM =
        	new ParameterDescription("penalty_item_id", true, ParameterDescription.Type.STR);

        //initializer for pluggable params
        {
        	descriptions.add(PARAMETER_ITEM);
    	}

        private Integer itemId;

        @SuppressWarnings("unchecked")
        private static final Class<Event> events[] = new Class[] {
                AboutToGenerateInvoices.class
        };

        public Class<Event>[] getSubscribedEvents() { return events; }

        /**
         * Returns the configured penalty item id to be added to any overdue invoices.
         *
         * fixme: user configured penalty item id always comes through as a String
         *
         * @return item id
         * @throws PluggableTaskException if the parameter is not an integer
         */
        public Integer getPenaltyItemId() throws PluggableTaskException {
            if (itemId == null) {
                try {
                    itemId = Integer.parseInt((String) parameters.get(PARAMETER_ITEM.getName()));
                } catch (NumberFormatException e) {
                    throw new PluggableTaskException("Configured penalty item id must be an integer!", e);
                }
            }
            return itemId;
        }

        /**
         * @see IInternalEventsTask#process(com.sapienter.jbilling.server.system.event.Event)
         *
         * @param event event to process
         * @throws PluggableTaskException
         */
        public void process(Event event) throws PluggableTaskException {
            if (!(event instanceof AboutToGenerateInvoices))
                throw new PluggableTaskException("Cannot process event " + event);

            AboutToGenerateInvoices invEvent = (AboutToGenerateInvoices) event;

            LOG.debug("Processing event: "
                    + "user id: "
                    + invEvent.getUserId());

            // find all unpaid, overdue invoices or invoices paid after due date, for this user and add the penalty item excluding
            // carried invoices as the remaining balance will already have been applied to the new invoice.
            InvoiceDAS invoiceDAS = new InvoiceDAS();
            Collection<InvoiceDTO> unpaidInvoices=invoiceDAS.findProccesableByUser(UserBL.getUserEntity(invEvent.getUserId())); 
            List<Integer> latePaid = new InvoiceDAS().findLatePaidInvoicesForUser(invEvent.getUserId());

            LOG.debug("Found un-paid invoices " + unpaidInvoices);
            LOG.debug("Found invoice ids " + latePaid);
            
            // quit if the user has no overdue invoices.
            if (unpaidInvoices.isEmpty() && latePaid.isEmpty()) {
                LOG.error("Cannot apply a penalty to a user that does not have an overdue invoice!");
                return;
            }

            for (Integer invoiceID: latePaid) {
            	unpaidInvoices.add(invoiceDAS.find(invoiceID));
            }
            
            ItemBL item;
            try {
                item = new ItemBL(getPenaltyItemId());
                LOG.debug("Penalty item " + getPenaltyItemId());
            } catch (SessionInternalError e) {
                throw new PluggableTaskException("Cannot find configured penalty item: " + getPenaltyItemId(), e);
            } catch (Exception e) {
                throw new PluggableTaskException(e);
            }

            for (InvoiceDTO invoice: unpaidInvoices) {
            	// Calculate the penalty fee. If the fee is zero (check the item cost) then
                // no penalty should be applied to this invoice.
                BigDecimal fee = calculatePenaltyFee(invoice, item);
                LOG.debug("Calculated penalty item fee: " + fee.toString());

                if (fee.compareTo(BigDecimal.ZERO) <= 0)
                    return;

                // create the order
                Integer orderId= createPenaltyOrder(invoice, item, fee);
                LOG.debug("Created penalty Order " + orderId);
            }
        }
        
    /** 
     * Create a task specific Order for the given Invoice having given Item
     * @param invoice Invoice for which to create Penalty order
     * @param item Penalty Item (Percentage or Flat)
     * @return orderId 
     */
    public Integer createPenaltyOrder(InvoiceDTO invoice, ItemBL item, BigDecimal fee) throws PluggableTaskException {
        OrderDTO summary = new OrderDTO();
        OrderPeriodDTO orderPeriod = new OrderPeriodDAS().findRecurringPeriod(invoice.getBaseUser().getEntity().getId());
        if (orderPeriod == null) {
            LOG.error("No period different than One-Time was found.");
            return null;
        }
        summary.setOrderPeriod(orderPeriod);

        OrderBillingTypeDTO type = new OrderBillingTypeDTO();
        type.setId(Constants.ORDER_BILLING_PRE_PAID);
        summary.setOrderBillingType(type);
        //penalty applicable since the date Invoice got created.
        summary.setActiveSince(invoice.getDueDate()); 
        LOG.debug("Order active since " + summary.getActiveSince());
        summary.setCurrency(invoice.getCurrency());

        //if invoice is paid after due date, this order must have an active until date
        if (Constants.INVOICE_STATUS_PAID.equals(invoice.getInvoiceStatus().getId())) {
        	LOG.debug("Invoice " + invoice.getId() + " has been paid.");
        	summary.setActiveUntil(invoice.getPaymentMap().iterator().next().getCreateDatetime());
        }
        
        
        UserDTO user = new UserDTO();
        user.setId(invoice.getBaseUser().getId());
        summary.setBaseUserByUserId(user);

        // now add the item to the po
        Integer languageId = invoice.getBaseUser().getLanguageIdField();
        String description = item.getEntity().getDescription(languageId) + " as Overdue Penalty for Invoice Number " + invoice.getPublicNumber();

        OrderLineDTO line = new OrderLineDTO();
        line.setAmount(fee);
        line.setPrice(fee);
        line.setDescription(description);
        line.setItemId(getPenaltyItemId());
        line.setTypeId(Constants.ORDER_LINE_TYPE_PENALTY);
        line.setCreateDatetime(new Date());
        line.setDeleted(0);
        line.setUseItem(false);
        line.setQuantity(1);
        summary.getLines().add(line);

        // create the db record
        OrderBL order = new OrderBL();
        order.set(summary);
        return order.create(invoice.getBaseUser().getEntity().getId(), null, summary);
    }

    /**
     * Returns a calculated penalty fee for the users current owing balance and
     * the configured penalty item.
     *
     * @param invoice overdue invoice
     * @param item penalty item
     * @return value of the penalty item (penalty fee)
     */
    public BigDecimal calculatePenaltyFee(InvoiceDTO invoice, ItemBL item) {
        // use the user's current balance as the base for our fee calculations
        BigDecimal base = new UserBL().getBalance(invoice.getUserId());

        // if the item price is a percentage of the balance
        if (item.getEntity().getPercentage()!=null) {
            base = base.divide(new BigDecimal("100"), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
            base = base.multiply(item.getEntity().getPrice());
            return base;

        } else if (base.compareTo(BigDecimal.ZERO) > 0) {
            // price for a single penalty item.
            return item.getPrice(invoice.getBaseUser().getId(),
                                          invoice.getCurrency().getId(),
                                          BigDecimal.ONE,
                                          invoice.getBaseUser().getEntity().getId());
        } else {
            return BigDecimal.ZERO;
        }
    }

    
}
