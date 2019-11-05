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

package com.sapienter.jbilling.server.payment;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.payment.blacklist.CsvProcessor;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.event.PaymentFailedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentLinkedToInvoiceEvent;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.payment.event.ProcessPaymentEvent;
import com.sapienter.jbilling.server.process.AgeingBL;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.TransactionInfoUtil;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

;

/**
 *
 * This is the session facade for the payments in general. It is a statless
 * bean that provides services not directly linked to a particular operation
 *
 * @author emilc
 */
public class PaymentSessionBean implements IPaymentSessionBean {

    private final FormatLogger LOG = new FormatLogger(PaymentSessionBean.class);

   /**
    * This method goes over all the over due invoices for a given entity and
    * generates a payment record for each of them.
    */
    @Transactional( propagation = Propagation.REQUIRED )
    public void processPayments(Integer entityId) throws SessionInternalError {
        try {
            entityId.intValue(); // just to avoid the warning ;)
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /** 
    * This is meant to be called from the billing process, where the information
    * about how the payment is going to be done is not known. This method will
    * call a pluggable task that finds this information (usually a cc) before
    * calling the realtime processing.
    * Later, this will have to be changed for some file creation with all the
    * payment information to be sent in a batch mode to the processor at the 
    * end of the billing process. 
    * This is called only if the user being process has as a preference to 
    * process the payment with billing process, meaning that a payment has
    * to be created and processed real-time.
    * @return If the payment was not successful for any reason, null, 
    * otherwise the payment method used for the payment
    */
    @Transactional( propagation = Propagation.REQUIRED )
    public Integer generatePayment(InvoiceDTO invoice) 
            throws SessionInternalError {
        
        LOG.debug("Generating payment for invoice %s", invoice.getId());
        // go fetch the entity for this invoice
        Integer userId = invoice.getBaseUser().getUserId();
        UserDAS userDas = new UserDAS();
        Integer entityId = userDas.find(userId).getCompany().getId();
        Integer retValue = null;
        // create the dto with the information of the payment to create
        try {
            // get this payment information. Now we only expect one pl.tsk
            // to get the info, I don't see how more could help
            PaymentDTOEx dto = PaymentBL.findPaymentInstrument(entityId,
                    invoice.getBaseUser().getUserId());
            
            boolean noInstrument = false;
            if (dto == null) {
                noInstrument = true;
                dto = new PaymentDTOEx();
            }

            dto.setIsRefund(new Integer(0)); //it is not a refund
            dto.setUserId(userId);
            dto.setAmount(invoice.getBalance());
            dto.setCurrency(new CurrencyDAS().find(invoice.getCurrency().getId()));
            dto.setAttempt(new Integer(invoice.getPaymentAttempts() + 1));
            // when the payment is generated by the system (instead of
            // entered manually by a user), the payment date is sysdate
            dto.setPaymentDate(Calendar.getInstance().getTime());

            LOG.debug("Prepared payment %s", dto);
            // it could be that the user doesn't have a payment 
            // instrument (cc) in the db, or that is invalid (expired).
            if (!noInstrument) {
                Integer result = processAndUpdateInvoice(dto, invoice.getId(), null);
                LOG.debug("After processing. Result= %s", result);
                if (result != null && result.equals(ServerConstants.RESULT_OK)) {
                    retValue = dto.getInstrument().getPaymentMethod().getId();
                }
            } else {
                // audit that this guy was about to get a payment
                EventLogger logger = new EventLogger();
                logger.auditBySystem(entityId, userId, ServerConstants.TABLE_BASE_USER, userId,
                        EventLogger.MODULE_PAYMENT_MAINTENANCE, EventLogger.PAYMENT_INSTRUMENT_NOT_FOUND,
                        null, null, null);
                // update the invoice attempts
                invoice.setPaymentAttempts(dto.getAttempt() == null ? 
                        new Integer(1) : dto.getAttempt());
                // treat this as a failed payment
                PaymentFailedEvent event = new PaymentFailedEvent(entityId, dto);
                EventManager.process(event);
            }
            
        } catch (Exception e) {
            LOG.fatal("Problems generating payment.", e);
            throw new SessionInternalError(
                "Problems generating payment.");
        } 
        
        LOG.debug("Done. Returning:%s", retValue);
        return retValue;
    }
    
    /**
     * This method soft deletes a payment
     * 
     * @param paymentId
     * @throws SessionInternalError
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public void deletePayment(Integer paymentId) throws SessionInternalError {

        try {
            PaymentBL bl = new PaymentBL(paymentId);
            bl.delete();

        } catch (Exception e) {
            LOG.warn("Problem deleteing payment.", e);
            throw new SessionInternalError("Problem deleteing payment");
        }
    }
    
    
    /**
     * It creates the payment record, makes the calls to the authorization
     * processor and updates the invoice if successful.
     *
     * @param dto
     * @param invoiceId
     * @throws SessionInternalError
     */
    public Integer processAndUpdateInvoice(PaymentDTOEx dto, 
            Integer invoiceId, Integer executorUserId) throws SessionInternalError {

        PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
        DefaultTransactionDefinition transactionDefinition =
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);

        //only these IDs are shared between the
        //first and the second transaction
        Integer result = null;
        Integer paymentId = null;

        TransactionStatus transaction = null;

        /******************************* FIRST TRANSACTION SCOPE **************************************
         *  The first transaction scope is responsible for doing online processing if needed and
         *  creation of a payment record in database. Apart from the payment record in this first
         *  transaction few other records can be created, such as notification messages.
         *********************************************************************************************/

        LOG.debug("2-Transaction status before first transaction: %s",
                TransactionInfoUtil.getTransactionStatus(true));
        try {

            transactionDefinition.setName("2-processAndUpdateInvoice-create-payment-transaction-" + System.nanoTime());
            transaction = transactionManager.getTransaction(transactionDefinition);
            LOG.debug("2-Transaction info for payment: %s", TransactionInfoUtil.getTransactionStatus(true));

            InvoiceDTO invoice = new InvoiceBL(invoiceId).getEntity();

            if (invoice.getIsReview().compareTo(1) == 0) {
                LOG.debug("2-Invoice is a review invoice, can not process payment against it.");
                throw new SessionInternalError("Invoice is a review invoice, can not process payment against it.");
            }

            PaymentBL bl = new PaymentBL();
            Integer entityId = invoice.getBaseUser().getEntity().getId();
            
            // set the attempt
            if (dto.getIsRefund() == 0) {
                // take the attempt from the invoice
                dto.setAttempt(new Integer(invoice.getPaymentAttempts() + 1));
            } else { // is a refund
                dto.setAttempt(new Integer(1));
            } 
                
            // payment notifications require some fields from the related
            // invoice
            dto.getInvoiceIds().add(invoice.getId());
                
            // process the payment (will create the db record as well, if
            // there is any actual processing). Do not process negative
            // payments (from negative invoices), unless allowed.
            if (dto.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                result = bl.processPayment(entityId, dto, executorUserId);
                paymentId = bl.getEntity().getId(); //remember the payment id
            } else {
                // only process if negative payments are allowed
                int preferenceAllowNegativePayments = 0;
                try {
                    preferenceAllowNegativePayments =
                    	PreferenceBL.getPreferenceValueAsIntegerOrZero(
                    		entityId, ServerConstants.PREFERENCE_ALLOW_NEGATIVE_PAYMENTS);
                    
                } catch (EmptyResultDataAccessException fe) { 
                    // use default
                }
                if (preferenceAllowNegativePayments == 1) {
                    LOG.warn("Processing payment with negative amount %s",
                            dto.getAmount());
                    result = bl.processPayment(entityId, dto, executorUserId);
                    paymentId = bl.getEntity().getId(); //remember the payment id
                } else {
                    LOG.warn("Skiping payment processing. Payment with negative amount %s", dto.getAmount());
                }
            }

            // while still in the first transaction scope
            // update the payment record
            if (null != result) {
                bl.getEntity().setPaymentResult(new PaymentResultDAS().find(result));
            }
            LOG.debug("Processed payment with result: %s", result);

            //the commit will flush the hibernate session
            transactionManager.commit(transaction);

            //after successful commit we are removing only that invoice entity from
            //hibernate session to force the second transaction to reload the entity
            //from database and load a fresh version of that record. This reduces
            //the chances for optimistic locking exception on invoice.
            SessionFactory sessionFactory = Context.getBean(Context.Name.HIBERNATE_SESSION);
            //clear 2nd level cache
            if(sessionFactory.getCache().containsEntity(InvoiceDTO.class, invoice.getId())){
                LOG.debug("2-Removing invoiceDTO[%s] entity from 2nd level cache", invoice.getId());
                sessionFactory.getCache().evictEntity(InvoiceDTO.class, invoice.getId());
            }

        } catch (SessionInternalError sie) {
            LOG.error("2-Session Internal Error in transaction block.", sie);
            if(!transaction.isCompleted()){
                LOG.debug("Transaction not completed, initiate rollback");
                transactionManager.rollback(transaction);
            }
            throw sie;
        } catch(TransactionException te) {
            LOG.error("2-Transaction exception occurred.", te);
            throw new SessionInternalError(te);
        } catch (Exception e) {
            LOG.error("2-An exception occurred.", e);
            if(!transaction.isCompleted()){
                LOG.debug("Transaction not completed, initiate rollback");
                transactionManager.rollback(transaction);
            }
            throw new SessionInternalError(e);
        }


        /******************************* SECOND TRANSACTION SCOPE ********************************
        * The second transaction's job is to apply the payment to the invoice. The problem here is
        * that the invoice could also be updated by different thread/transactions so here we risk
        * optimistic locking exception. Because of this we try to repeat the applying of payment to
        * invoice a number of times.
        *****************************************************************************************/
        try {
            //try to commit transaction with retry
            Exception exception = null;
            int numAttempts = 0;
            do {
                numAttempts++;
                try {
                    if (result != null) {
                        LOG.debug("3-Apply payment[%s] to invoice[%s]. Before transaction start: %s",
                                TransactionInfoUtil.getTransactionStatus(true), paymentId, invoiceId);
                        transactionDefinition.setName("3-processAndUpdateInvoice-apply-payment-to-invoice-transaction-" +
                                System.nanoTime());
                        transaction = transactionManager.getTransaction(transactionDefinition);
                        LOG.debug("3-Apply payment[%s] to invoice[%s]. After transaction start: %s",
                                TransactionInfoUtil.getTransactionStatus(true), paymentId, invoiceId);

                        PaymentBL paymentBL = new PaymentBL(paymentId);

                        // update the dto with the created id
                        dto.setId(paymentBL.getEntity().getId());
                        // the balance will be the same as the amount
                        // if the payment failed, it won't be applied to the invoice
                        // so the amount will be ignored
                        dto.setBalance(dto.getAmount());

                        // Note: I could use the return of the last call to fetch another
                        // dto with a different cc number to retry the payment

                        //reload invoice data
                        InvoiceDTO invoice = new InvoiceBL(invoiceId).getEntity();
                        // get all the invoice's fields updated with this payment
                        BigDecimal paid = applyPayment(dto, invoice, result.equals(ServerConstants.RESULT_OK));

                        if (dto.getIsRefund() == 0) {
                            // Update the link between invoice and payment. This part of
                            // the code should not cause optimistic locking exception
                            // since it is only an insert against a table.
                            paymentBL.createMap(invoice, paid);
                        }

                        LOG.debug("3-Attempting to commit transaction: %s",
                                TransactionInfoUtil.getTransactionStatus(true));
                        transactionManager.commit(transaction);
                        LOG.debug("3-Transaction committed: %s",
                                TransactionInfoUtil.getTransactionStatus(true));

                    }
                    return result;

                //catches exceptions for which a retry is wanted
                } catch (HibernateOptimisticLockingFailureException ex) {
                    exception = ex;
                    LOG.error("31. Could not commit transaction.", ex);
                    //wait 100 milliseconds
                    Thread.sleep(100);
                } catch (StaleObjectStateException ex){
                    exception = ex;
                    LOG.error("32. Could not commit transaction.", ex);
                    //wait 100 milliseconds
                    Thread.sleep(100);
                } catch (DeadlockLoserDataAccessException ex){
                    exception = ex;
                    LOG.error("33. Could not commit transaction.", ex);
                    //wait 100 milliseconds
                    Thread.sleep(100);
                }

                LOG.debug("3-Applying payment to invoice retry: %s", numAttempts);
            } while (numAttempts <= 10); //retry 10 times

            LOG.debug("3. Failed to apply payment[%s] to invoice[%s], propagating exception", paymentId, invoiceId);
            throw exception;
        } catch (SessionInternalError sie){
            if(!transaction.isCompleted()){
                LOG.debug("Transaction not completed, initiate rollback");
                transactionManager.rollback(transaction);
            }
            LOG.error("3-Session Internal Error in transaction block.", sie);
            throw sie;
        } catch (TransactionException te){
            LOG.error("3-Transaction exception occurred.", te);
            throw new SessionInternalError(te);
        } catch (Exception e) {
            LOG.error("3-An exception occurred.", e);
            if(!transaction.isCompleted()){
                LOG.debug("Transaction not completed, initiate rollback");
                transactionManager.rollback(transaction);
            }
            throw new SessionInternalError(e);
        }
    }

    /**
     * This is called from the client to process real-time a payment, usually cc. 
     * 
     * @param dto
     * @param invoiceId
     * @throws SessionInternalError
     */
    public Integer processAndUpdateInvoice(PaymentDTOEx dto, 
            Integer invoiceId, Integer entityId, Integer executorUserId) throws SessionInternalError {

        PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
        DefaultTransactionDefinition transactionDefinition =
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionDefinition.setName("1-processAndUpdateInvoice1-outer");
        TransactionStatus transaction = transactionManager.getTransaction(transactionDefinition);
        LOG.debug("Transaction after: %s", TransactionInfoUtil.getTransactionStatus(true));

        try {
        	//for refunds, invoiceIds must be populated
        	if (dto.getIsRefund() == 1) {
        		PaymentBL linkedPayment = new PaymentBL(dto.getPayment().getId());

        		if (!linkedPayment.getEntity().getInvoicesMap().isEmpty()) {
        			LOG.debug("The refunds linked payment has some paid Invoices.");
                    for (PaymentInvoiceMapDTO entry : linkedPayment.getEntity().getInvoicesMap()) {
                        dto.getPayment().getInvoiceIds().add(entry.getInvoiceEntity().getId());
                    }
        		}
        	}

            if (dto.getIsRefund() == 0 && invoiceId != null) {
                InvoiceBL bl = new InvoiceBL(invoiceId);
                List<Integer> inv = new ArrayList<Integer>();
                inv.add(invoiceId);
                dto.setInvoiceIds(inv);
                transactionManager.commit(transaction);
                return processAndUpdateInvoice(dto, bl.getEntity().getId(), executorUserId);

            } else {
            	
            	if (dto.getIsRefund() == 1
                        && dto.getPayment() != null ) {
            	    /* && !dto.getPayment().getInvoiceIds().isEmpty()){*/
                    /*InvoiceBL bl = new InvoiceBL((Integer) dto.getPayment().getInvoiceIds().get(0));
                    return processAndUpdateInvoice(dto, bl.getEntity(), executorUserId);*/
                    LOG.debug("We changed the rules, you can't refund the Payment amount linked to Invoice. So no need to involve invoices in Refunds.");
                } 
                // without an invoice, it's just creating the payment row
                // and calling the processor
                LOG.info("The payment may be a refund and its linked payment has no invoices connected to it");
                
                PaymentBL bl = new PaymentBL();
                Integer result = bl.processPayment(entityId, dto, executorUserId);

                //try to commit transaction with retry
                Exception exception = null;
                int numAttempts = 0;
                do {
                    numAttempts++;
                    TransactionStatus innerTransaction = transactionManager.getTransaction(
                            new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));
                    try {
                        if (result != null) {
                            bl.getEntity().setPaymentResult(new PaymentResultDAS().find(result));
                        }

                        if (result != null && result.equals(ServerConstants.RESULT_OK)) {

                            LOG.debug("A successful Refund. But original payment not linked to Invoices.");
                            //Therefore, will not be auto-applied to other Invoices 
                            //The refund then in-fact must reduce the Payment Balance it is linked to.
                            if (dto.getIsRefund() == 1) {
                                LOG.debug("Linked payment balance after refund application %s", bl.getEntity().getPayment().getBalance());
                            } else {
                                // if the configured, pay any unpaid invoices
                                ConfigurationBL config = new ConfigurationBL(entityId);
                                if (config.getEntity().getAutoPaymentApplication() == 1) {
                                    bl.automaticPaymentApplication();
                                }
                            }
                        }
                        transactionManager.commit(innerTransaction);
                        LOG.debug("Attempting to commit transaction.");
                        transactionManager.commit(transaction);
                        LOG.debug("Transaction commited.");
                        return result;
                    } catch (Exception ex) {
                        if(!innerTransaction.isCompleted()) {
                            LOG.debug("Transaction not completed, initiate rollback");
                            transactionManager.rollback(innerTransaction);
                        }
                        exception = ex;
                        LOG.error("Could not commit transaction.", ex);
                        //wait 100 milliseconds
                        Thread.sleep(100);
                    }
                }
                while (numAttempts <= 10); //retry 10 times
                throw exception;
            }
        } catch (Exception e) {
            LOG.error("An exception occurred.", e);
            if (!transaction.isCompleted()) {
                LOG.debug("Transaction not completed, initiate rollback");
                transactionManager.rollback(transaction);
            }
            throw new SessionInternalError(e);
        }
    }
    
    /**
     * This is called from the client to apply an existing payment to
     * an invoice.
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public void applyPayment(Integer paymentId, Integer invoiceId) {
        LOG.debug("Applying payment %s to invoice %s", paymentId, invoiceId);
        if (paymentId == null || invoiceId == null) {
            LOG.warn("Got null parameters to apply a payment");
            return;
        }

        try {
            PaymentBL payment = new PaymentBL(paymentId);
            InvoiceBL invoice = new InvoiceBL(invoiceId);
            
            BigDecimal paid = applyPayment(payment.getDTO(), invoice.getEntity(), true);
            
            // link it with the invoice
            payment.createMap(invoice.getEntity(), paid);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

    }
    /**
     * Applys a payment to an invoice, updating the invoices fields with
     * this payment.
     * @param payment
     * @param invoice
     * @param success
     * @throws SessionInternalError
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public BigDecimal applyPayment(PaymentDTO payment, InvoiceDTO invoice, boolean success) throws SQLException {
        BigDecimal totalPaid = BigDecimal.ZERO;

        if (invoice != null) {
            // set the attempt of the invoice
            LOG.debug("applying payment to invoice %s", invoice.getId());
            if (payment.getIsRefund() == 0) {
                //invoice can't take nulls. Default to 1 if so.
                invoice.setPaymentAttempts(payment.getAttempt() == null ? new Integer(1) : payment.getAttempt());
            }

            if (success) {
                // update the invoice's balance if applicable
                BigDecimal balance = invoice.getBalance();
                // get current invoice balance
                LOG.debug("current invoice balance is %s", balance);
                if (balance != null) {
                    boolean balanceSign = (balance.compareTo(BigDecimal.ZERO) < 0) ? false : true;
                    LOG.debug("balance sign is %s", balanceSign);
                    BigDecimal newBalance = null;
                    if (payment.getIsRefund() == 0) {
                        LOG.debug("payment is a normal payment");
                        newBalance = balance.subtract(payment.getBalance());
                        LOG.debug("new balance is %s",newBalance);
                        // I need the payment record to update its balance
                        if (payment.getId() == 0) {
                            throw new SessionInternalError("The ID of the payment to has to be present in the DTO");
                        }
                        PaymentBL paymentBL = new PaymentBL(payment.getId());
                        BigDecimal paymentBalance = payment.getBalance().subtract(balance);
                        LOG.debug("payment balance is %s",paymentBalance);
                        // payment balance cannot be negative, must be at least zero
                        if (BigDecimal.ZERO.compareTo(paymentBalance) > 0) {
                            LOG.debug("setting the paymentBalance which was %s to ZERO", paymentBalance);
                            paymentBalance = BigDecimal.ZERO;
                        }

                        totalPaid = payment.getBalance().subtract(paymentBalance);

                        paymentBL.getEntity().setBalance(paymentBalance);
                        payment.setBalance(paymentBalance);
                        
                        // only level the balance if the original balance wasn't negative
                        if (newBalance.compareTo(ServerConstants.BIGDECIMAL_ONE_CENT) < 0 && balanceSign) {
                            LOG.debug("new balance is %s and BIGDECIMAL_ONE_CENT is %s and balance sign is %s", newBalance, ServerConstants.BIGDECIMAL_ONE_CENT, balanceSign);
                            LOG.debug("setting the new balance to ZERO");
                            // the payment balance was greater than the invoice's
                            newBalance = BigDecimal.ZERO;
                        }
                        
                        invoice.setBalance(newBalance);
                        LOG.debug("Set invoice balance to: %s", invoice.getBalance());
                                            
                        if (BigDecimal.ZERO.compareTo(newBalance) == 0) {
                            // update the to_process flag if the balance is 0
                            invoice.setToProcess(new Integer(0));
                        } else {
                            // a refund might make this invoice payabale again
                            invoice.setToProcess(new Integer(1));
                        }

                    } else { // refunds add to the invoice                        
                    	LOG.debug("Refunds do not add to Invoice anymore. You cannot refund a Payment that is linked to an Invoce. First un-link it.");
                    }
                        
                } else {
                    // with no balance, we assume the the invoice got all paid
                    LOG.debug("The balance of the invoice is %s",balance);
                    invoice.setToProcess(new Integer(0));
                }

                // if the user is in the ageing process, she should be out
                if (invoice.getToProcess().equals(new Integer(0))) {
                    AgeingBL ageing = new AgeingBL();
                    ageing.out(invoice.getBaseUser(), invoice.getId());
                }
            }
        }
        if (!totalPaid.equals(BigDecimal.ZERO)) {
            //Fire the event if the payment actually pays something.
            Integer entityId;
            if(payment.getBaseUser() == null){
                PaymentDTOEx paymentDTOEx = (PaymentDTOEx) payment;
                entityId = new UserDAS().find(paymentDTOEx.getUserId()).getEntity().getId();
            }else {
                entityId = payment.getBaseUser().getEntity().getId();
            }

            PaymentLinkedToInvoiceEvent event = new PaymentLinkedToInvoiceEvent(entityId,
                                                                                new PaymentDTOEx(payment),
                                                                                invoice,
                                                                                totalPaid);
            EventManager.process(event);
        }
        return totalPaid;
    }

    /**
     * This method is called from the client, when a payment needs only to 
     * be applyed without realtime authorization by a processor
     * Finds this invoice entity, creates the payment record and calls the 
     * apply payment  
     * Id does suport invoiceId = null because it is possible to get a payment
     * that is not paying a specific invoice, a deposit for prepaid models.
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public Integer applyPayment(PaymentDTOEx payment, Integer invoiceId, Integer executorUserId)  
            throws SessionInternalError {
        LOG.debug("PaymentDTOEx contains %s",payment);
        try {
            // create the payment record
            PaymentBL paymentBl = new PaymentBL();
            PaymentInformationBL piBl = new PaymentInformationBL();
            
            if(payment.getPaymentInstruments().size() > 0) {
            	payment.setInstrument(payment.getPaymentInstruments().iterator().next());
            	// set the payment method
            	payment.getInstrument().setPaymentMethod(new PaymentMethodDAS().find(piBl.getPaymentMethodForPaymentMethodType(payment.getInstrument())));
            }
            
            // set the attempt to an initial value, if the invoice is there,
            // it's going to be updated
            payment.setAttempt(new Integer(1));
            // a payment that is applied, has always the same result
            payment.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_ENTERED));
            payment.setBalance(payment.getAmount());
            paymentBl.create(payment, executorUserId);
            // this is necessary for the caller to get the Id of the
            // payment just created
            payment.setId(paymentBl.getEntity().getId());
            
            boolean wasPaymentApplied= false;
            
            if (payment.getIsRefund() == 0) { // normal payment
                if (invoiceId != null) {
                    // find the invoice
                    InvoiceBL invoiceBl = new InvoiceBL(invoiceId);
                    // set the attmpts from the invoice
                    payment.setAttempt(new Integer(invoiceBl.getEntity().getPaymentAttempts() + 1));
                    // apply the payment to the invoice
                    BigDecimal paid = applyPayment(payment, invoiceBl.getEntity(), true);
                    // link it with the invoice
                    paymentBl.createMap(invoiceBl.getEntity(), paid);
                    
                    //payment was applied successfully
                    wasPaymentApplied= true;
                } else {
                    // this payment was done without an explicit invoice
                    // We'll try to link it to invoices with balances then provided automatic payment is set
                	Integer userId= payment.getUserId();
                	UserDTO userDTO= new UserDAS().find(userId);
                	if (null != userDTO) {
	                	ConfigurationBL config = new ConfigurationBL(userDTO.getCompany().getId());
	                    if (config.getEntity().getAutoPaymentApplication() == 1) {
	                        wasPaymentApplied= paymentBl.automaticPaymentApplication();
	                    }
                	}
                }
                // let know about this payment with an event
                PaymentSuccessfulEvent event = new PaymentSuccessfulEvent(
                        paymentBl.getEntity().getBaseUser().getEntity().getId(),payment);
                EventManager.process(event);
            } else {
                LOG.debug("payment is linked to payment %s and may be linked with invoice", payment.getPayment());
                
                // fetch the linked payment from database
                PaymentDTO linkedPayment = new PaymentBL(payment.getPayment().getId()).getEntity();
                
                if ( null != linkedPayment ) {
	                /*
	                 * Since payment is not linked to any invoice now, 
	                 * we must subtract the payment balance with that of the refund payment value
                	*/
	                linkedPayment.setBalance(linkedPayment.getBalance().subtract(payment.getAmount()));
                    wasPaymentApplied= true;
                }
                else {
                    LOG.debug("This refund is not linked with any payment which is wrong");
                    //maybe throw exception
                }
            }
            
            //should we notify the customer of this payment
            if (wasPaymentApplied) {
                //this notification prevents multiple notifications sent for each application of the payment to an Invoice
                LOG.debug("Invoking Payment notification for the Payment Entered since it was applied to atleast 1 Invoice.");
                if (payment.getInvoiceIds().isEmpty() && invoiceId != null){
                    paymentBl.sendNotification(payment, new UserDAS().find(payment.getUserId()).getCompany().getId(), true,invoiceId);
                } else {
                	paymentBl.sendNotification(payment, new UserDAS().find(payment.getUserId()).getCompany().getId(), true);
                }
            }
            return paymentBl.getEntity().getId();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }  

    @Transactional( propagation = Propagation.REQUIRED )
    public PaymentDTOEx getPayment(Integer id, Integer languageId) 
            throws SessionInternalError {
        try {
            PaymentBL bl = new PaymentBL(id);
            return bl.getDTOEx(languageId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Transactional( propagation = Propagation.REQUIRED )
    public boolean isMethodAccepted(Integer entityId, 
            Integer paymentMethodId) 
            throws SessionInternalError {
        if (paymentMethodId == null) {
            // if this is a credit card and it has not been
            // identified by the first digit, the method will be null
            return false;
        }
        try {
            PaymentBL bl = new PaymentBL();
            
            return bl.isMethodAccepted(entityId, paymentMethodId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    } 
    
    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public Boolean processPaypalPayment(Integer invoiceId, String entityEmail,
            BigDecimal amount, String currency, Integer paramUserId, String userEmail) 
            throws SessionInternalError {
        
        if (userEmail == null && invoiceId == null && paramUserId == null) {
            LOG.debug("Too much null, returned");
            return false;
        }
        try {
            boolean ret = false;
            InvoiceBL invoice = null;
            Integer entityId = null;
            Integer userId = null;
            CurrencyBL curr = null;
            if (invoiceId != null) {
                invoice = new InvoiceBL(invoiceId);
                entityId = invoice.getEntity().getBaseUser().getEntity().getId();
                userId = invoice.getEntity().getBaseUser().getUserId();
                curr = new CurrencyBL(
                        invoice.getEntity().getCurrency().getId());
            } else {
                UserBL user = new UserBL();
                // identify the user some other way
                if (paramUserId != null) {
                    // easy
                    userId = paramUserId;
                } else {
                    // find a user by the email address
                    userId = user.getByEmail(userEmail);
                    if (userId == null) {
                        LOG.debug("Could not find a user for email %s", userEmail);
                        return false;
                    }
                }
                user = new UserBL(userId);
                entityId = user.getEntityId(userId);
                curr = new CurrencyBL(user.getCurrencyId());
            }
            
            // validate the entity
            String paypalAccount = PreferenceBL.getPreferenceValue(entityId, ServerConstants.PREFERENCE_PAYPAL_ACCOUNT);
            if (paypalAccount != null && paypalAccount.equals(entityEmail)) {
                // now the currency
                if (curr.getEntity().getCode().equals(currency)) {
                    // all good, make the payment
                    PaymentDTOEx payment = new PaymentDTOEx();
                    payment.setAmount(amount);
                    payment.setPaymentMethod(new PaymentMethodDAS().find(ServerConstants.PAYMENT_METHOD_PAYPAL));
                    payment.setUserId(userId);
                    payment.setCurrency(curr.getEntity());
                    payment.setCreateDatetime(Calendar.getInstance().getTime());
                    payment.setPaymentDate(Calendar.getInstance().getTime());
                    payment.setIsRefund(new Integer(0));
                    applyPayment(payment, invoiceId, null);
                    ret = true;
                    
                    // notify the customer that the payment was received
                    NotificationBL notif = new NotificationBL();
                    MessageDTO message = notif.getPaymentMessage(entityId, 
                            payment, true);
                    INotificationSessionBean notificationSess = 
                            (INotificationSessionBean) Context.getBean(
                            Context.Name.NOTIFICATION_SESSION);
                    notificationSess.notify(payment.getUserId(), message);
                    
                    // link to unpaid invoices
                    // TODO avoid sending two emails
                    PaymentBL bl = new PaymentBL(payment);
                    bl.automaticPaymentApplication();
                } else {
                    LOG.debug("wrong currency %s", currency);
                }
            } else {
                LOG.debug("wrong entity paypal account %s %s", paypalAccount, entityEmail);
            }
            
            return Boolean.valueOf(ret);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void doPaymentRetry(Integer userId, List<InvoiceDTO> overdueInvoices)
            throws SessionInternalError {
        try {
            UserBL userLoader = new UserBL(userId);
            UserDTO user = userLoader.getEntity();
            UserStatusDTO status = user.getUserStatus();
            if (status == null) {
                return;
            }

            AgeingEntityStepDTO nextStep = status.getAgeingEntityStep();
            // preform payment retry
            if (nextStep != null && nextStep.getRetryPayment() == 1) {
                LOG.debug("Retrying payment for user %s based on the user status", userId);
                // post the need of a payment for all unpaid invoices for this user
                for (InvoiceDTO invoice : overdueInvoices) {
                    ProcessPaymentEvent event = new ProcessPaymentEvent(invoice.getId(),
                            null, null, user.getEntity().getId());
                    EventManager.process(event);
                }
            }

        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }
    
    /** 
     * Clients with the right priviliges can update payments with result
     * 'entered' that are not linked to an invoice
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public void update(Integer executorId, PaymentDTOEx dto) 
            throws SessionInternalError, EmptyResultDataAccessException {
        if (dto.getId() == 0) {
            throw new SessionInternalError("ID missing in payment to update");
        }
        
        LOG.debug("updateting payment %s", dto.getId());
        PaymentBL bl = new PaymentBL(dto.getId());
        if (new Integer(bl.getEntity().getPaymentResult().getId()).equals(ServerConstants.RESULT_ENTERED)) {
                
        } else {
            throw new SessionInternalError("Payment update only available" +
                    " for entered payments");
        }
            
        bl.update(executorId, dto);
    }
    
    /** 
     * Removes a payment-invoice link
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public void removeInvoiceLink(Integer mapId) {
        PaymentBL payment = new PaymentBL();
        payment.removeInvoiceLink(mapId);
    }

    /** 
     * Processes the blacklist CSV file specified by filePath.
     * It will either add to or replace the existing uploaded 
     * blacklist for the given entity (company). Returns the number
     * of new blacklist entries created.
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public int processCsvBlacklist(String filePath, boolean replace, 
            Integer entityId) throws CsvProcessor.ParseException {
        CsvProcessor processor = new CsvProcessor();
        return processor.process(filePath, replace, entityId);
    }

    /**
     * Saves legacy payment information on jBilling related tables.
     *
     * @param paymentDTOEx The instance of payment information.
     * @throws SessionInternalError
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public Integer saveLegacyPayment(PaymentDTOEx paymentDTOEx) throws SessionInternalError {
        PaymentBL paymentBL = new PaymentBL();
        paymentBL.create(paymentDTOEx, null);
	    return Integer.valueOf(paymentBL.getDTO().getId());
    }

}
