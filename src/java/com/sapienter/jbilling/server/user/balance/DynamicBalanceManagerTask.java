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

package com.sapienter.jbilling.server.user.balance;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.event.*;
import com.sapienter.jbilling.server.payment.event.PaymentDeletedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.event.DynamicBalanceChangeEvent;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import org.hibernate.StaleObjectStateException;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.util.Date;



/**
 * @author emilc
 */
public class DynamicBalanceManagerTask extends PluggableTask implements IInternalEventsTask {
    private static final FormatLogger LOG = new FormatLogger(DynamicBalanceManagerTask.class);
    private static final int TRANSACTION_RETRIES = 10;

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[]{
            PaymentSuccessfulEvent.class,
            OrderDeletedEvent.class,
            NewOrderEvent.class,
            PaymentDeletedEvent.class,
            OrderAddedOnInvoiceEvent.class,
            NewQuantityEvent.class,
            NewPriceEvent.class,
            ProcessTaxLineOnInvoiceEvent.class,
    };

    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    public void process (Event event) throws PluggableTaskException {

        PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);

        try {

            //try to commit transaction with retry
            Exception exception = null;

            int numAttempts = 0;
            do {
                numAttempts++;
                try {
                    TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));

                    updateDynamicBalance(event.getEntityId(), determineUserId(event), determineAmount(event));

                    transaction.flush();

                    return;

                } catch (Exception ex) {
                    if (ex instanceof HibernateOptimisticLockingFailureException ||
                            ex instanceof StaleObjectStateException) {
                        new UserDAS().clear();
                        exception = ex;
                        //transactionManager.rollback(transaction);
                        LOG.debug("Could not commit transaction.", ex);
                        //wait 100 milliseconds
                        Thread.sleep(100);
                    } else {
                        throw new PluggableTaskException(ex);
                    }
                }
                LOG.debug("Updating customer's dynamic balance retry %d", numAttempts);
            } while (numAttempts <= TRANSACTION_RETRIES);
            LOG.error("Failed update customer's dynamic balance after %d retries", TRANSACTION_RETRIES);
            throw exception;
        } catch (Exception e) {
            LOG.error("An exception ocurred.", e);
            throw new PluggableTaskException(e);
        }
    }

    private BigDecimal determineAmount (Event event) {
        if (event instanceof PaymentSuccessfulEvent) {
            PaymentSuccessfulEvent payment = (PaymentSuccessfulEvent) event;
            BigDecimal retVal = payment.getPayment().getAmount();
            if (payment.getPayment().getIsRefund() > 0) {
                retVal = retVal.negate();
            }
            return convertAmountToUsersCurrency(retVal, payment.getPayment().getCurrency().getId(),
                    payment.getPayment().getUserId(), payment.getPayment().getPaymentDate(), payment.getEntityId());

        } else if (event instanceof OrderDeletedEvent) {
            OrderDeletedEvent order = (OrderDeletedEvent) event;
            if (order.getOrder().isOneTime()) {
                return order.getOrder().getTotal();
            } else {
                return BigDecimal.ZERO;
            }

        } else if (event instanceof NewOrderEvent) {
            NewOrderEvent order = (NewOrderEvent) event;
            if (order.getOrder().isOneTime()) {
                return order.getOrder().getTotal().multiply(new BigDecimal(-1));
            } else {
                return BigDecimal.ZERO;
            }

        } else if (event instanceof PaymentDeletedEvent) {
            PaymentDeletedEvent payment = (PaymentDeletedEvent) event;

            if (!CommonConstants.PAYMENT_RESULT_SUCCESSFUL.equals(payment.getPayment().getResultId())
                    && !CommonConstants.PAYMENT_RESULT_ENTERED.equals(payment.getPayment().getResultId())) {
                LOG.debug("A non-successful payment deletion must not affect dynamic balance.");
                return BigDecimal.ZERO;
            }

            BigDecimal retVal = payment.getPayment().getAmount().negate();
            return convertAmountToUsersCurrency(retVal, payment.getPayment().getCurrency().getId(),
                    payment.getPayment().getBaseUser().getId(), payment.getPayment().getPaymentDate(), payment.getEntityId());


        } else if (event instanceof OrderAddedOnInvoiceEvent) {

            OrderAddedOnInvoiceEvent orderOnInvoiceEvent = (OrderAddedOnInvoiceEvent) event;
            OrderAddedOnInvoiceEvent order = (OrderAddedOnInvoiceEvent) event;
            if (order.getOrder().isRecurring()) {
                return orderOnInvoiceEvent.getTotalInvoiced().multiply(new BigDecimal(-1));
            } else {
                return BigDecimal.ZERO;
            }

        } else if (event instanceof ProcessTaxLineOnInvoiceEvent) {

        	ProcessTaxLineOnInvoiceEvent taxLineOnInvoiceEvent = (ProcessTaxLineOnInvoiceEvent) event;
            return taxLineOnInvoiceEvent.getTaxLineAmount().multiply(new BigDecimal(-1));
            
        } else if (event instanceof NewQuantityEvent) {
            NewQuantityEvent nq = (NewQuantityEvent) event;

            if (new OrderDAS().find(nq.getOrderId()).isOneTime()) {
                BigDecimal newTotal, oldTotal;
                // new order line, or old one updated?
                if (nq.getNewOrderLine() == null) {
                    // new
                    oldTotal = BigDecimal.ZERO;
                    newTotal = nq.getOrderLine().getAmount();
                    if (nq.getNewQuantity().compareTo(BigDecimal.ZERO) == 0) {
                        // it is a delete
                        newTotal = newTotal.multiply(new BigDecimal(-1));
                    }
                } else {
                    // old
                    oldTotal = null != nq.getOrderLine() ? nq.getOrderLine().getAmount() : BigDecimal.ZERO;
                    newTotal = nq.getNewOrderLine().getAmount();
                }
                return newTotal.subtract(oldTotal).multiply(new BigDecimal(-1));

            } else {
                return BigDecimal.ZERO;
            }
        } else if (event instanceof NewPriceEvent) {
            BigDecimal newAmount, oldAmount;
            NewPriceEvent npe = (NewPriceEvent) event;
            if (new OrderDAS().find(npe.getOrderId()).isOneTime()) {
                oldAmount = npe.getOldAmount();
                newAmount = npe.getNewAmount();
                return newAmount.subtract(oldAmount).negate();
            } else {
                return BigDecimal.ZERO;
            }
        } else {
            LOG.error("Can not determine amount for event " + event);
            return null;
        }
    }

    private BigDecimal convertAmountToUsersCurrency (BigDecimal amount, Integer amountCurrencyId, Integer userId, Date date, Integer entityId) {
        //no need to convert zeros
        if (null != amount && !(amount.compareTo(BigDecimal.ZERO) == 0)) {
            //non-zero return value, must be converted if
            Integer userCurrencyId = new UserDAS().find(userId).getCurrencyId();
            if (amountCurrencyId != userCurrencyId) {
                //convert to user's currency
                LOG.debug("Converting amount to User's specific currency");
                amount = new CurrencyBL().convert(amountCurrencyId, userCurrencyId, amount, date, entityId);
            }
        }
        return amount;
    }

    private int determineUserId (Event event) {
        if (event instanceof PaymentSuccessfulEvent) {
            PaymentSuccessfulEvent payment = (PaymentSuccessfulEvent) event;
            return payment.getPayment().getUserId();
        } else if (event instanceof OrderDeletedEvent) {
            OrderDeletedEvent order = (OrderDeletedEvent) event;
            return order.getOrder().getBaseUserByUserId().getId();
        } else if (event instanceof NewOrderEvent) {
            NewOrderEvent order = (NewOrderEvent) event;
            return order.getOrder().getBaseUserByUserId().getId();
        } else if (event instanceof PaymentDeletedEvent) {
            PaymentDeletedEvent payment = (PaymentDeletedEvent) event;
            return payment.getPayment().getBaseUser().getId();
        } else if (event instanceof OrderAddedOnInvoiceEvent) {
            OrderAddedOnInvoiceEvent order = (OrderAddedOnInvoiceEvent) event;
            return order.getOrder().getBaseUserByUserId().getId();
        } else if (event instanceof ProcessTaxLineOnInvoiceEvent) {
        	ProcessTaxLineOnInvoiceEvent taxLineEvent = (ProcessTaxLineOnInvoiceEvent) event;
            return taxLineEvent.getUserId();
        } else if (event instanceof NewQuantityEvent) {
            NewQuantityEvent nq = (NewQuantityEvent) event;
            return new OrderDAS().find(nq.getOrderId()).getBaseUserByUserId().getId();
        } else if (event instanceof NewPriceEvent) {
            NewPriceEvent nq = (NewPriceEvent) event;
            return new OrderDAS().find(nq.getOrderId()).getBaseUserByUserId().getId();
        } else {
            LOG.error("Can not determine user for event " + event);
            return 0;
        }
    }

    private void updateDynamicBalance (Integer entityId, Integer userId, BigDecimal amount) {
        UserDTO user = new UserDAS().findNow(userId);
        CustomerDTO customer = user.getCustomer();

        // get the parent customer that pays, if it exists
        if (customer != null) {
            while (customer.getParent() != null
                    && (customer.getInvoiceChild() == null || customer.getInvoiceChild() == 0)) {
                customer = customer.getParent(); // go up one level
            }
        }

        // fail fast condition, no dynamic balance or ammount is zero
        if (customer == null || BigDecimal.ZERO.compareTo(amount) == 0) {
            LOG.debug("Nothing to update");
            return;
        }

        LOG.debug("Updating dynamic balance for " + amount);

        BigDecimal balance = (customer.getDynamicBalance() == null ? BigDecimal.ZERO : customer.getDynamicBalance());

        // register the event, before the balance is changed
        new EventLogger().auditBySystem(entityId,
                customer.getBaseUser().getId(),
                ServerConstants.TABLE_CUSTOMER,
                user.getCustomer().getId(),
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.DYNAMIC_BALANCE_CHANGE,
                null,
                balance.toString(),
                null);

        customer.setDynamicBalance(balance.add(amount));

        if (!balance.equals(customer.getDynamicBalance())) {
            DynamicBalanceChangeEvent event = new DynamicBalanceChangeEvent(user.getEntity().getId(),
                    user.getUserId(),
                    customer.getDynamicBalance(), // new
                    balance);                     // old
            EventManager.process(event);
        }
    }
}
