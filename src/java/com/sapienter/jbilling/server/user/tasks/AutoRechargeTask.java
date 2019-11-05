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
package com.sapienter.jbilling.server.user.tasks;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.payment.IPaymentSessionBean;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentSessionBean;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.event.DynamicBalanceChangeEvent;
import com.sapienter.jbilling.server.util.PreferenceBL;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.dao.EmptyResultDataAccessException;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;



/**
 * Automatic payment task designed to "top up" a customers pre-paid balance with a user
 * configured amount whenever the balance drops below a company wide threshold (configured
 * as a preference).
 *
 * This task subscribes to the {@link DynamicBalanceChangeEvent} which is fired whenever
 * the customers balance changes.
 *
 * @see com.sapienter.jbilling.server.user.balance.DynamicBalanceManagerTask
 *
 * @author Brian Cowdery
 * @since  10-14-2009
 */
public class AutoRechargeTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(AutoRechargeTask.class);

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
        DynamicBalanceChangeEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    public void process(Event event) throws PluggableTaskException {
        if (!(event instanceof DynamicBalanceChangeEvent)) {
            throw new PluggableTaskException("Cannot process event " + event);
        }

        DynamicBalanceChangeEvent balanceEvent = (DynamicBalanceChangeEvent) event;
        UserDTO user = new UserBL(balanceEvent.getUserId()).getDto();
        CustomerDTO customer = user.getCustomer();
        // get the parent customer that pays, if it exists
        if (customer != null) {
            while (customer.getParent() != null
                    && (customer.getInvoiceChild() == null || customer.getInvoiceChild() == 0)) {
                customer = customer.getParent(); // go up one level
                user = customer.getBaseUser();
            }
        }

        LOG.debug("Processing %s", event);

        if (!isEventProcessable(balanceEvent.getNewBalance(), user, customer)) {
            LOG.debug("Conditions not met, no recharge");
            return;
        }

        PaymentDTOEx payment = null;
        try {
            payment = PaymentBL.findPaymentInstrument(event.getEntityId(), user.getId());
        } catch (TaskException e) {
            throw new PluggableTaskException(e);
        }

        if (payment != null) {
            payment.setIsRefund(0);
            payment.setAttempt(1);
            payment.setAmount(customer.getAutoRecharge());
            payment.setCurrency(user.getCurrency());
            payment.setUserId(user.getId());
            payment.setPaymentDate(new Date());

            LOG.debug("Making automatic payment of $%s for user %s", payment.getAmount(), payment.getUserId());

            // can't use the managed bean, a new transaction will cause the CustomerDTO to get an
            // optimistic lock: this transaction and the new payment one both changing the same customer.dynamic_balance
            IPaymentSessionBean paymentSession = new PaymentSessionBean();

            BigDecimal currMthlyAmnt= customer.getCurrentMonthlyAmount();

            if (customer.getCurrentMonthlyAmount() == null) {
                customer.setCurrentMonthlyAmount(customer.getAutoRecharge());
            } else {
                customer.setCurrentMonthlyAmount(customer.getCurrentMonthlyAmount().add(customer.getAutoRecharge()));
            }

            Integer result = paymentSession.processAndUpdateInvoice(payment,
                                                                    null,
                                                                    balanceEvent.getEntityId(),
                                                                    user.getUserId());

            if(result.equals(CommonConstants.PAYMENT_RESULT_FAILED) || result.equals(CommonConstants.PAYMENT_RESULT_PROCESSOR_UNAVAILABLE)) {
		        //if payment failed, revert back to original value
                customer.setCurrentMonthlyAmount(currMthlyAmnt);
	        }

            LOG.debug("Payment created with result: %s", result);
        } else {
            LOG.debug("No payment instrument, no recharge");
        }
    }

    /**
     * Returns true if the auto-recharge criteria has been met and this event can be processed.
     *
     * @param newBalance new dynamic balance of the user
     * @param user user to validate
     * @param customer customer to validate
     * @return true if event can be processed, false if not.
     * @throws PluggableTaskException
     */
    private boolean isEventProcessable(BigDecimal newBalance, UserDTO user, CustomerDTO customer) {
        if (customer == null || customer.getAutoRecharge().compareTo(BigDecimal.ZERO) <= 0) {
            LOG.debug("Not a customer, or auto recharge value <= 0");
            return false;
        }

        BigDecimal threshold = getAutoRechargeThreshold(user);
        if (threshold == null ) {             
        	LOG.debug("Company or customer does not have a recharge preference.");
        	return false;
        }

        LOG.debug("Threshold = %s, New Balance= %s", threshold, newBalance);

        //check monthly limit
        if(customer.getMonthlyLimit() != null && customer.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0){
            if(customer.getCurrentMonth() == null || !DateUtils.truncatedEquals(new Date(), customer.getCurrentMonth(), Calendar.MONTH)){
                customer.setCurrentMonth(new Date());
                customer.setCurrentMonthlyAmount(BigDecimal.ZERO);
            }
            if (customer.getCurrentMonthlyAmount().add(customer.getAutoRecharge()).compareTo(customer.getMonthlyLimit()) > 0){
                return false;
            }
        }
        LOG.debug("Customer Recharge Amt: %s Credit Limit: %s", customer.getAutoRecharge(), customer.getCreditLimit());
        if (threshold.compareTo(newBalance.add(customer.getCreditLimit())) > 0) {
        } else {
            LOG.debug("threshold not reached yet.");
            return false;
        }
        return true;
    }

    /**
     * Returns the set auto-recharge threshold for the given user, or null
     * if the user or company does not have a configured threshold.
     *
     * @param user UserDTO
     * @return auto-recharge threshold or null if not set
     */
    private BigDecimal getAutoRechargeThreshold(UserDTO user) {

        if(user.getCustomer().getRechargeThreshold() != null &&
                user.getCustomer().getRechargeThreshold().compareTo(BigDecimal.ZERO) >= 0){
            return user.getCustomer().getRechargeThreshold();
        }

        try {
            return PreferenceBL.getPreferenceValueAsDecimal(
                            user.getEntity().getId(), CommonConstants.PREFERENCE_AUTO_RECHARGE_THRESHOLD);
        } catch (EmptyResultDataAccessException e) {
            return null; // no threshold set
        }
    }

}
