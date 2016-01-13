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
import com.sapienter.jbilling.server.invoice.InvoiceIdComparator;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.payment.db.*;
import com.sapienter.jbilling.server.payment.event.AbstractPaymentEvent;
import com.sapienter.jbilling.server.payment.event.PaymentDeletedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentUnlinkedFromInvoiceEvent;
import com.sapienter.jbilling.server.payment.tasks.PaymentFilterTask;
import com.sapienter.jbilling.server.pluggableTask.PaymentInfoTask;
import com.sapienter.jbilling.server.pluggableTask.PaymentTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerPayout;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import org.springframework.dao.EmptyResultDataAccessException;

import javax.persistence.EntityNotFoundException;
import javax.sql.rowset.CachedRowSet;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

;

public class PaymentBL extends ResultList implements PaymentSQL {

    private static final FormatLogger LOG = new FormatLogger(PaymentBL.class);

    private PaymentDAS paymentDas = null;
    private PaymentMethodDAS methodDas = null;
    private PaymentInvoiceMapDAS mapDas = null;
    private PaymentDTO payment = null;
    private EventLogger eLogger = null;
    
    private PaymentInformationDAS piDas = null;
    public PaymentBL(Integer paymentId) {
        init();
        set(paymentId);
    }

    /**
     * Validates that a refund payment must be linked to a payment and the amount of the refund payment should
     * be equal to the linked payment. Return true if valid.
     * @param refundPayment
     * @return boolean
     */
    public static synchronized Boolean validateRefund(PaymentWS refundPayment) {
        if(refundPayment.getPaymentId() == null) {
            LOG.debug("There is no linked payment with this refund");
            return false;
        }
        // fetch the linked payment from database
        PaymentDTO linkedPayment =  new PaymentBL(refundPayment.getPaymentId()).getEntity();
        /*if(linkedPayment.getAmount().compareTo(refundPayment.getAmountAsDecimal()) !=0 ) {
            LOG.debug("The linked payment amount is different than the refund value amount");
            return false;
        }*/
        
        BigDecimal refundableBalance= linkedPayment.getBalance(); 
        LOG.debug("The payment %d selected can been refunded for %s", linkedPayment.getId(), refundableBalance);
		if (refundPayment.getAmountAsDecimal().compareTo(refundableBalance) > 0) {
			LOG.debug("Cannot refund more than the refundableBalance");
			return false;
		} else if (BigDecimal.ZERO.compareTo(refundableBalance) >= 0) {
            LOG.debug("Cannot refund a zero or negative refundable balance");
            return false;
        } else if (BigDecimal.ZERO.compareTo(refundPayment.getAmountAsDecimal()) >= 0) {
            LOG.debug("Cannot refund a zero or negative refund amount");
            return false;
		}
        return true;
    }

    public PaymentBL() {
        init();
    }

    public PaymentBL(PaymentDTO payment) {
        init();
        this.payment = payment;
    }

    public void set(PaymentDTO payment) {
        this.payment = payment;
    }

    private void init() {
        try {
            eLogger = EventLogger.getInstance();
            
            paymentDas = new PaymentDAS();

            methodDas = new PaymentMethodDAS();

            mapDas = new PaymentInvoiceMapDAS();
            
            piDas = new PaymentInformationDAS();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public PaymentDTO getEntity() {
        return payment;
    }

    public PaymentDAS getHome() {
        return paymentDas;
    }

    public String getMethodDescription(PaymentMethodDTO method, Integer languageId) {
        // load directly from the DB, otherwise proxies get in the way
    	LOG.debug("Loading description for method: " + method);
        return new PaymentMethodDAS().find(method.getId()).getDescription(languageId);
    }

    public void set(Integer id) {
        payment = paymentDas.find(id);
    }

    public void create(PaymentDTOEx dto, Integer executorUserId) {
        // create the record
    	// The method column here is null now because method of payment will be decided after the payment processing
        payment = paymentDas.create(dto.getAmount(), null,
                dto.getUserId(), dto.getAttempt(), dto.getPaymentResult(), dto.getCurrency());
        // if the payment result is set and its Constants.RESULT_ENTERED then payment method should be set 
        // and may bepayment instrument info record should be created
        if(dto.getPaymentResult() != null && dto.getPaymentResult().getId() == Constants.RESULT_ENTERED) {
        	if(dto.getInstrument() != null && dto.getPaymentInstruments().size() > 0) {
        		payment.setPaymentMethod(dto.getInstrument().getPaymentMethod());
        		payment.getPaymentInstrumentsInfo().add(new PaymentInstrumentInfoDTO(payment, 
        				dto.getPaymentResult(),dto.getInstrument().getPaymentMethod(), dto.getInstrument().getSaveableDTO()));
        	}

        	if(dto.getPaymentMethod() != null) {
        		payment.setPaymentMethod(dto.getPaymentMethod());
        	}
        }
        
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setBalance(dto.getBalance());
               
        // may be this is a refund
        if (dto.getIsRefund() == 1) {
            payment.setIsRefund(new Integer(1));
            
            // refund balance is always set to ZERO
            payment.setBalance(BigDecimal.ZERO);
            LOG.debug("dto of paymentDTOEX contains %s", dto);
            if (dto.getPayment() != null) {
                LOG.debug("Refund is linked to some payment %s", dto.getPayment());
                // this refund is link to a payment
                PaymentBL linkedPayment = new PaymentBL(dto.getPayment().getId());
                payment.setPayment(linkedPayment.getEntity());
            }
        }

        // preauth payments
        if (dto.getIsPreauth() != null && dto.getIsPreauth().intValue() == 1) {
            payment.setIsPreauth(1);
        }

        // the payment period length this payment was expected to last
        if (dto.getPaymentPeriod() != null){
            payment.setPaymentPeriod(dto.getPaymentPeriod());

        }
        // the notes related to this payment
        if (dto.getPaymentNotes() != null){
            payment.setPaymentNotes(dto.getPaymentNotes());
        }

        // meta fields
        payment.updateMetaFieldsWithValidation(
        		new UserBL().getEntityId(dto.getUserId()), null, dto);

        dto.setId(payment.getId());
        dto.setCurrency(payment.getCurrency());

        //Update real amount from PaymentDTO into PaymentDTOEx
        dto.setAmount(payment.getAmount());
        dto.setBalance(payment.getAmount());
//        dto.setPublicNumber(payment.getPublicNumber());

        dto.setPayment(payment.getPayment());
        paymentDas.save(payment);
        // add a log row for convenience
        UserDAS user = new UserDAS();
        
        if ( null != executorUserId ) {
            eLogger.audit(executorUserId, dto.getUserId(), Constants.TABLE_PAYMENT, dto.getId(),
                    EventLogger.MODULE_PAYMENT_MAINTENANCE,
                    EventLogger.ROW_CREATED, null, null, null);
        } else {
            eLogger.auditBySystem(user.find(dto.getUserId()).getCompany().getId(),
                    dto.getUserId(), Constants.TABLE_PAYMENT, dto.getId(),
                    EventLogger.MODULE_PAYMENT_MAINTENANCE,
                    EventLogger.ROW_CREATED, null, null, null);
        }

    }

    void createMap(InvoiceDTO invoice, BigDecimal amount) {
        BigDecimal realAmount;
        if (new Integer(payment.getPaymentResult().getId()).equals(Constants.RESULT_FAIL) || new Integer(payment.getPaymentResult().getId()).equals(Constants.RESULT_UNAVAILABLE)) {
            realAmount = BigDecimal.ZERO;
        } else {
            realAmount = amount;
        }
        mapDas.create(invoice, payment, realAmount);
    }

    /**
     * Updates a payment record, including related cheque or credit card
     * records. Only valid for entered payments not linked to an invoice.
     *
     * @param dto
     *            The DTO with all the information of the new payment record.
     */
    public void update(Integer executorId, PaymentDTOEx dto)
            throws SessionInternalError {
        // the payment should've been already set when constructing this
        // object
        if (payment == null) {
            throw new EmptyResultDataAccessException("Payment to update not set", 1);
        }

        // we better log this, so this change can be traced
        eLogger.audit(executorId, payment.getBaseUser().getId(),
                Constants.TABLE_PAYMENT, payment.getId(),
                EventLogger.MODULE_PAYMENT_MAINTENANCE,
                EventLogger.ROW_UPDATED, null, payment.getAmount().toString(),
                null);

        // start with the payment's own fields
        payment.setUpdateDatetime(Calendar.getInstance().getTime());
        payment.setAmount(dto.getAmount());
        // since the payment can't be linked to an invoice, the balance
        // has to be equal to the total of the payment
        payment.setBalance(dto.getAmount());
        payment.setPaymentDate(dto.getPaymentDate());

        // the payment period length this payment was expected to last
        if (dto.getPaymentPeriod() != null){
            payment.setPaymentPeriod(dto.getPaymentPeriod());

        }
        // the notes related to this payment
        if (dto.getPaymentNotes() != null){
            payment.setPaymentNotes(dto.getPaymentNotes());
        }

        payment.updateMetaFieldsWithValidation(
        		new UserBL().getEntityId(dto.getUserId()), null, dto);
    }

    /**
     * Goes through the payment pluggable tasks, and calls them with the payment
     * information to get the payment processed. If a call fails because of the
     * availability of the processor, it will try with the next task. Otherwise
     * it will return the result of the process (approved or declined).
     *
     * @return the constant of the result allowing for the caller to attempt it
     *         again with different payment information (like another cc number)
     */
    public Integer processPayment(Integer entityId, PaymentDTOEx info, Integer executorUserId)
            throws SessionInternalError {
        Integer retValue = null;
        try {
            PluggableTaskManager taskManager = new PluggableTaskManager(entityId, Constants.PLUGGABLE_TASK_PAYMENT);
            PaymentTask task = (PaymentTask) taskManager.getNextClass();

            if (task == null) {
                LOG.warn("No payment pluggable tasks configured for entity %s", entityId);
                return null;
            }

            create(info, executorUserId);
            boolean processorUnavailable = true;
            while (task != null && processorUnavailable) {
                // see if this user has pre-auths
            	PaymentInformationBL piBl = new PaymentInformationBL();
                PaymentAuthorizationBL authBL = new PaymentAuthorizationBL();
                PaymentAuthorizationDTO auth = null;
                LOG.debug("Total instruments for processing are : " + info.getPaymentInstruments().size());
                Iterator<PaymentInformationDTO> iterator = info.getPaymentInstruments().iterator();
                while(iterator.hasNext()) {
                	PaymentInformationDTO instrument = iterator.next();
                	// check if the instrument was blacklisted by filter if yes, then get the next ones
                	while(instrument.isBlacklisted() && iterator.hasNext()) {
                		instrument = iterator.next();
                	}
                	// if all the list is exhausted but we are still on black listed then move on to next processor
                	if(instrument.isBlacklisted())
                		break;
                	
                	auth = authBL.getPreAuthorization(info.getUserId());
                	
                	PaymentInformationDTO newInstrument;
                	// load fresh instrument if its a saved one or refresh its relations if its a new one
                	if(instrument.getId() != null) {
                		newInstrument = piDas.find(instrument.getId());
                		newInstrument.setPaymentMethod(methodDas.find(piBl.getPaymentMethodForPaymentMethodType(newInstrument)));
                	} else {
                		newInstrument = instrument.getDTO();
                		refreshRequiredRelations(newInstrument);
                	}
                	info.setInstrument(newInstrument);
                	LOG.debug("Processing payment with instrument : " + newInstrument);
                	
                	if (auth != null) {
	                    processorUnavailable = task.confirmPreAuth(auth, info);
	                    if (!processorUnavailable) {
	                        if (new Integer(info.getPaymentResult().getId()).equals(Constants.RESULT_FAIL)) {
	                            processorUnavailable = task.process(info);
	                        }
	                        // in any case, don't use this preAuth again
	                        authBL.markAsUsed(info);
	                    }
	                } else {
	                    processorUnavailable = task.process(info);
	                }
                
                	// if this is an output of filter when filter does not fail, no need to save it
                	if(Constants.RESULT_NULL != info.getPaymentResult().getId()) {
		                // create a payment instrument to link to payment information object
		            	// create an information record of this payment method to link this instrument to payment
                		// obscure card number if its credit card
                		PaymentInformationDTO saveable = newInstrument.getSaveableDTO();
                		piBl.obscureCreditCardNumber(saveable);
		            	payment.getPaymentInstrumentsInfo().add(new PaymentInstrumentInfoDTO(payment, info.getPaymentResult(),newInstrument.getPaymentMethod(), saveable));
                	}
            	
	            	
	                // allow the pluggable task to do something if the payment
	                // failed (like notification, suspension, etc ... )
	                if (!processorUnavailable && new Integer(info.getPaymentResult().getId()).equals(Constants.RESULT_FAIL)) {
	                    task.failure(info.getUserId(), info.getAttempt());
	                    
	                    // if the processor was a filter then we need to eliminate the given method as it was not a success while filtering
		                if(task instanceof PaymentFilterTask) {
		                	instrument.setBlacklisted(true);
		                }
	                }
	                // trigger an event
	                AbstractPaymentEvent event = AbstractPaymentEvent.forPaymentResult(entityId, info);
	
	                if (event != null) {
	                    EventManager.process(event);
	                }
	                LOG.debug("Status of payment processor : %s", processorUnavailable);
	                
	                // if the processor has processed payment successfully then no need to iterate over
	                if(Constants.RESULT_OK.equals(info.getPaymentResult().getId())) {
	                	processorUnavailable = false;
	                	break;
	                }
                }

                // get the next task
                LOG.debug("Getting next task, processorUnavailable : %s", processorUnavailable);
                task = (PaymentTask) taskManager.getNextClass();
            }

            // set last payment method for which given result will be displayed
            payment.setPaymentMethod(info.getInstrument().getPaymentMethod());
            // if after all the tasks, the processor in unavailable,
            // return that
            LOG.debug("Payment result is: " + info.getPaymentResult().getId());
            if (processorUnavailable || info.getPaymentResult().getId() == Constants.RESULT_NULL) {
                retValue = Constants.RESULT_UNAVAILABLE;
            } else {
                retValue = info.getPaymentResult().getId();
            }
            LOG.debug("Payment result is after: " + retValue);
            payment.setPaymentResult(new PaymentResultDAS().find(retValue));
            // the balance of the payment depends on the result
            if (retValue.equals(Constants.RESULT_OK) || retValue.equals(Constants.RESULT_ENTERED)) {
            	//#3788 - Partial Refunds Adjustment to Linked Payment
            	if (payment.getIsRefund() == 0) {
					payment.setBalance(payment.getAmount());
				} else {// Payment is a Refund
					// fetch the linked payment from database
					PaymentDTO linkedPayment = new PaymentBL(payment
							.getPayment().getId()).getEntity();
					if (null != linkedPayment) {
						/* Since payment is not linked to any invoice now, we
						 * must subtract the payment balance with that of the
						 * refund payment value */
						linkedPayment.setBalance(linkedPayment.getBalance()
								.subtract(payment.getAmount()));
					} else {
						LOG.debug("This refund is not linked with any payment which is wrong");
						// maybe throw exception
					}
				}
            } else {
                payment.setBalance(BigDecimal.ZERO);
            }
        } catch (SessionInternalError e) {
            throw e;
        } catch (Exception e) {
            LOG.fatal("Problems handling payment task.", e);
            throw new SessionInternalError("Problems handling payment task.");
        }

        //send notification of all result types: OK, Entered, Failed
        LOG.debug("Sending notification to customer with retValue ****** "+retValue);
        sendNotification(info, entityId);

        return retValue;
    }

    public PaymentDTO getDTO() {
        PaymentDTO dto = new PaymentDTO(payment.getId(), payment.getAmount(), payment.getBalance(), payment.getCreateDatetime(), payment.getUpdateDatetime(), payment.getPaymentDate(), payment.getAttempt(), payment.getDeleted(),
                payment.getPaymentMethod(), payment.getPaymentResult(), payment.getIsRefund(), payment.getIsPreauth(), payment.getCurrency(), payment.getBaseUser());
        dto.setMetaFields(new LinkedList<MetaFieldValue>(payment.getMetaFields()));
        //for refunds
        dto.setPayment(payment.getPayment());
        return dto;
    }

    public PaymentDTOEx getDTOEx(Integer language) {
        PaymentDTOEx dto = new PaymentDTOEx(getDTO());
        dto.setUserId(payment.getBaseUser().getUserId());
        // now add all the invoices that were paid by this payment
        Iterator it = payment.getInvoicesMap().iterator();
        while (it.hasNext()) {
            PaymentInvoiceMapDTO map = (PaymentInvoiceMapDTO) it.next();
            dto.getInvoiceIds().add(map.getInvoiceEntity().getId());

            dto.addPaymentMap(getMapDTO(map.getId()));
        }

        // payment method (international)
        PaymentMethodDTO method = payment.getPaymentMethod();
        dto.setMethod(method.getDescription(language));

        // refund fields if applicable
        dto.setIsRefund(payment.getIsRefund());
        if (payment.getPayment() != null && payment.getId() != payment.getPayment().getId()) {
            PaymentBL linkedPayment = new PaymentBL(payment.getPayment().getId());
            //#1890 - linking it to a payment
            dto.setPayment(linkedPayment.getDTOEx(language));
        }

        // the first authorization if any
        if (!payment.getPaymentAuthorizations().isEmpty()) {
            PaymentAuthorizationBL authBL = new PaymentAuthorizationBL(
                    (PaymentAuthorizationDTO) payment.getPaymentAuthorizations().iterator().next());
            dto.setAuthorization(authBL.getDTO());
        }

        // the result in string mode (international)
        if (payment.getPaymentResult() != null) {
            PaymentResultDTO result = payment.getPaymentResult();
            dto.setResultStr(result.getDescription(language));
        }

        // to which payout this payment has been included
        if (payment.getPartnerPayouts().size() > 0) {
            dto.setPayoutId(((PartnerPayout) payment.getPartnerPayouts().toArray()[0]).getId());
        }

        // the payment period length this payment was expected to last
        if (payment.getPaymentPeriod() != null){
            dto.setPaymentPeriod(payment.getPaymentPeriod());

        }
        // the notes related to this payment
        if (payment.getPaymentNotes() != null){
            dto.setPaymentNotes(payment.getPaymentNotes());
        }

        //payment instruments info result
        dto.setPaymentInstrumentsInfo(payment.getPaymentInstrumentsInfo());

        return dto;
    }

    public static synchronized PaymentWS getWS(PaymentDTOEx dto) {
        PaymentWS ws = new PaymentWS();
        ws.setId(dto.getId());
	    ws.setUserId(dto.getUserId());
        ws.setAmount(dto.getAmount());
        ws.setAttempt(dto.getAttempt());
        ws.setBalance(dto.getBalance());
        ws.setCreateDatetime(dto.getCreateDatetime());
        ws.setDeleted(dto.getDeleted());
        ws.setIsPreauth(dto.getIsPreauth());
        ws.setIsRefund(dto.getIsRefund());
        ws.setPaymentDate(dto.getPaymentDate());
        ws.setUpdateDatetime(dto.getUpdateDatetime());
        ws.setPaymentNotes(dto.getPaymentNotes());
        ws.setPaymentPeriod(dto.getPaymentPeriod());

        ws.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(
        		new UserBL().getEntityId(dto.getUserId()), dto));

        if (dto.getCurrency() != null)
            ws.setCurrencyId(dto.getCurrency().getId());
        
        if (dto.getPaymentResult() != null)
            ws.setResultId(dto.getPaymentResult().getId());

        ws.setUserId(dto.getUserId());
        ws.setMethod(dto.getMethod());

        if (dto.getAuthorization() != null) {
            com.sapienter.jbilling.server.entity.PaymentAuthorizationDTO authDTO = new com.sapienter.jbilling.server.entity.PaymentAuthorizationDTO();
            authDTO.setAVS(dto.getAuthorization().getAvs());
            authDTO.setApprovalCode(dto.getAuthorization().getApprovalCode());
            authDTO.setCardCode(dto.getAuthorization().getCardCode());
            authDTO.setCode1(dto.getAuthorization().getCode1());
            authDTO.setCode2(dto.getAuthorization().getCode2());
            authDTO.setCode3(dto.getAuthorization().getCode3());
            authDTO.setCreateDate(dto.getAuthorization().getCreateDate());
            authDTO.setId(dto.getAuthorization().getId());
            authDTO.setMD5(dto.getAuthorization().getMD5());
            authDTO.setProcessor(dto.getAuthorization().getProcessor());
            authDTO.setResponseMessage(dto.getAuthorization().getResponseMessage());
            authDTO.setTransactionId(dto.getAuthorization().getTransactionId());

            ws.setAuthorization(authDTO);
        } else {
            ws.setAuthorization(null);
        }

        Integer invoiceIds[] = new Integer[dto.getInvoiceIds().size()];

        for (int f = 0; f < dto.getInvoiceIds().size(); f++) {
            invoiceIds[f] = (Integer) dto.getInvoiceIds().get(f);
        }
        ws.setInvoiceIds(invoiceIds);

        if (dto.getPayment() != null) {
            ws.setPaymentId(dto.getPayment().getId());
        } else {
            ws.setPaymentId(null);
        }
        
        // set payment specific instruments, payment instruments are linked through the PaymentInstrumentInfo
        LOG.debug("Payment instruments info are: " + dto.getPaymentInstrumentsInfo());
        if(dto.getPaymentInstrumentsInfo() != null && dto.getPaymentInstrumentsInfo().size() > 0) {
	        for(PaymentInstrumentInfoDTO paymentInstrument : dto.getPaymentInstrumentsInfo()) {
	    		ws.getPaymentInstruments().add(PaymentInformationBL.getWS(paymentInstrument.getPaymentInformation()));
	        }
        }
        
        // set user payment instruments of user if this call is coming from findPaymentInstruments
        for(PaymentInformationDTO paymentInstrument : dto.getPaymentInstruments()) {
    		ws.getUserPaymentInstruments().add(PaymentInformationBL.getWS(paymentInstrument));
        }
        
        if(dto.getPaymentMethod() != null) {
        	ws.setMethodId(dto.getPaymentMethod().getId());
        }
        return ws;
    }

    public CachedRowSet getList(Integer entityID, Integer languageId,
            Integer userRole, Integer userId, boolean isRefund)
            throws SQLException, Exception {

        // the first variable specifies if this is a normal payment or
        // a refund list
        if (userRole.equals(Constants.TYPE_ROOT) || userRole.equals(Constants.TYPE_CLERK)) {
            prepareStatement(PaymentSQL.rootClerkList);
            cachedResults.setInt(1, isRefund ? 1 : 0);
            cachedResults.setInt(2, entityID.intValue());
            cachedResults.setInt(3, languageId.intValue());
        } else if (userRole.equals(Constants.TYPE_PARTNER)) {
            prepareStatement(PaymentSQL.partnerList);
            cachedResults.setInt(1, isRefund ? 1 : 0);
            cachedResults.setInt(2, entityID.intValue());
            cachedResults.setInt(3, userId.intValue());
            cachedResults.setInt(4, languageId.intValue());
        } else if (userRole.equals(Constants.TYPE_CUSTOMER)) {
            prepareStatement(PaymentSQL.customerList);
            cachedResults.setInt(1, isRefund ? 1 : 0);
            cachedResults.setInt(2, userId.intValue());
            cachedResults.setInt(3, languageId.intValue());
        } else {
            throw new Exception("The payments list for the type " + userRole + " is not supported");
        }

        execute();
        conn.close();
        return cachedResults;
    }

    /**
     * Validates the deletion of a payment
     * @param
     * @return  boolean
     */
    public boolean ifRefunded() {
		LOG.debug("Checking if The payment id %d is refunded. ", payment.getId());
		return new PaymentDAS().isRefundedPartiallyOrFully(payment.getId());
    }

    /**
     * Does the actual work of deleting the payment
     *
     * @throws SessionInternalError
     */
    public void delete() throws SessionInternalError {

        try {

            LOG.debug("Deleting payment %s", payment.getId());
            Integer entityId = payment.getBaseUser().getEntity().getId();
            EventManager.process(new PaymentDeletedEvent(entityId, payment));

            payment.setUpdateDatetime(Calendar.getInstance().getTime());
            payment.setDeleted(new Integer(1));

            eLogger.auditBySystem(entityId, payment.getBaseUser().getId(),
                    Constants.TABLE_PAYMENT, payment.getId(),
                    EventLogger.MODULE_PAYMENT_MAINTENANCE,
                    EventLogger.ROW_DELETED, null, null, null);

        } catch (Exception e) {
            LOG.warn("Problem deleteing payment.", e);
            throw new SessionInternalError("Problem deleteing payment.");
        }
    }

    /*
     * This is the list of payment that are refundable. It shows when entering a
     * refund.
     */
    public CachedRowSet getRefundableList(Integer languageId, Integer userId)
            throws SQLException, Exception {
        prepareStatement(PaymentSQL.refundableList);
        cachedResults.setInt(1, 0); // is not a refund
        cachedResults.setInt(2, userId.intValue());
        cachedResults.setInt(3, languageId.intValue());
        execute();
        conn.close();
        return cachedResults;
    }

    public boolean isMethodAccepted(Integer entityId, Integer paymentMethodId) {

        boolean retValue = false;

        PaymentMethodDTO method = methodDas.find(paymentMethodId);

        for (Iterator it = method.getEntities().iterator(); it.hasNext();) {
            if (((CompanyDTO) it.next()).getId() == entityId) {
                retValue = true;
                break;
            }
        }
        return retValue;
    }

    public static synchronized PaymentDTOEx findPaymentInstrument(Integer entityId,
            Integer userId) throws PluggableTaskException,
            SessionInternalError, TaskException {

        PluggableTaskManager taskManager = new PluggableTaskManager(entityId,
                Constants.PLUGGABLE_TASK_PAYMENT_INFO);
        PaymentInfoTask task = (PaymentInfoTask) taskManager.getNextClass();

        if (task == null) {
            // at least there has to be one task configurated !
            new FormatLogger(PaymentBL.class).fatal(
                    "No payment info pluggable" + "tasks configurated for entity " + entityId);
            throw new SessionInternalError("No payment info pluggable" + "tasks configurated for entity " + entityId);
        }

        // get this payment information. Now we only expect one pl.tsk
        // to get the info, I don't see how more could help
        return task.getPaymentInfo(userId);

    }

    /*public static synchronized boolean validate(PaymentWS dto) {
        boolean retValue = true;

        if (dto.getAmount() == null || dto.getMethodId() == null || dto.getIsRefund() == 0 || dto.getResultId() == null || dto.getUserId() == null || (dto.getCheque() == null && dto.getCreditCard() == null)) {
            retValue = false;
        } else if (dto.getCreditCard() != null) {
            PaymentDTOEx ex = new PaymentDTOEx(dto);
            retValue = CreditCardBL.validate(ex.getCreditCard());
        } else if (dto.getCheque() != null) {
            PaymentDTOEx ex = new PaymentDTOEx(dto);
        }
        MetaFieldBL.validateMetaFields(new UserBL().getEntityId(dto.getUserId()), 
        		EntityType.PAYMENT, dto.getMetaFields());

        return retValue;
    }*/

    public Integer getLatest(Integer userId) throws SessionInternalError {
        Integer retValue = null;
        try {
            prepareStatement(PaymentSQL.getLatest);
            cachedResults.setInt(1, userId);
            cachedResults.setInt(2, userId);
            execute();
            if (cachedResults.next()) {
                int value = cachedResults.getInt(1);
                if (!cachedResults.wasNull()) {
                    retValue = new Integer(value);
                }
            }
            cachedResults.close();
            conn.close();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return retValue;
    }

    public Integer[] getManyWS(Integer userId, Integer limit, Integer offset,
            Integer languageId) {
        List<Integer> result = new PaymentDAS().findIdsByUserLatestFirst(
                userId, limit, offset);
        return result.toArray(new Integer[result.size()]);

    }

    public Integer[] getListIdsByDate(Integer userId, Date since, Date until) {

        // add a day to include the until date
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(until);
        cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
        until = cal.getTime();

        List<Integer> result = new PaymentDAS().findIdsByUserAndDate(userId, since, until);
        return result.toArray(new Integer[result.size()]);
    }

    private List<PaymentDTO> getPaymentsWithBalance(Integer userId) {
        // this will usually return 0 or 1 records, rearly a few more
        List<PaymentDTO> paymentsList = null;
        Collection payments = paymentDas.findWithBalance(userId);

        if (payments != null) {
            paymentsList = new ArrayList<>(payments); // needed for the
            // sort
            Collections.sort(paymentsList, new PaymentEntityComparator().reversed());
        } else {
            paymentsList = new ArrayList<>(); // empty
        }

        return paymentsList;
    }

    /**
     * Given an invoice, the system will look for any payment with a balance
     * and get the invoice paid with this payment.
     */
    public boolean automaticPaymentApplication(InvoiceDTO invoice)
            throws SQLException {
        boolean appliedAtAll= false;
        
        List<PaymentDTO> payments = getPaymentsWithBalance(invoice.getBaseUser().getUserId());

        //Bug fix 9970 - The older Payment's balance should be used to pay the 
        //last invoice before new Payment's balance is used
        Collections.sort(payments, (o1, o2) -> o1.getCreateDatetime().compareTo(o2.getCreateDatetime()));
        
        for (int f = 0; f < payments.size() && invoice.getBalance().compareTo(BigDecimal.ZERO) > 0; f++) {
            payment= payments.get(f);
            if (new Integer(payment.getPaymentResult().getId()).equals(Constants.RESULT_FAIL) || new Integer(payment.getPaymentResult().getId()).equals(Constants.RESULT_UNAVAILABLE)) {
                continue;
            }
            if ( applyPaymentToInvoice(invoice) ) {
                appliedAtAll= true;
            }
        }
        
        return appliedAtAll;
    }

    /**
     * Give an payment (already set in this object), it will look for any
     * invoices with a balance and get them paid, starting wiht the oldest.
     */
    public boolean automaticPaymentApplication() throws SQLException {
        boolean appliedAtAll= false;
        if (BigDecimal.ZERO.compareTo(payment.getBalance()) >= 0) {
            return false; // negative payment, skip
        }

        Collection<InvoiceDTO> invoiceCollection = new InvoiceDAS().findWithBalanceByUser(payment.getBaseUser());

        // sort from oldest to newest
        List<InvoiceDTO> invoices = new ArrayList<InvoiceDTO>(invoiceCollection);
        Collections.sort(invoices, new InvoiceIdComparator());

        for (InvoiceDTO invoice : invoices) {
            // negative balances don't need paying
            if (BigDecimal.ZERO.compareTo(invoice.getBalance()) > 0) {
                continue;
            }

            //apply and set
            if ( applyPaymentToInvoice(invoice) ) {
                appliedAtAll= true;
            }
            
            if (BigDecimal.ZERO.compareTo(payment.getBalance()) >= 0) {
                break; // no payment balance remaining
            }
        }
        return appliedAtAll;
    }

    private boolean applyPaymentToInvoice(InvoiceDTO invoice) throws SQLException {
        // this is not actually getting de Ex, so it is faster
        PaymentDTOEx dto = new PaymentDTOEx(getDTO());

        // not pretty, but the methods are there
        IPaymentSessionBean psb = (IPaymentSessionBean) Context.getBean(
            Context.Name.PAYMENT_SESSION);
        // make the link between the payment and the invoice
        BigDecimal paidAmount = psb.applyPayment(dto, invoice, true);
        createMap(invoice, paidAmount);
        dto.getInvoiceIds().add(invoice.getId());
        
        // notify the customer
        dto.setUserId(invoice.getBaseUser().getUserId()); // needed for the
        // notification
        // the notification only understands ok or not, if the payment is
        // entered
        // it has to show as ok
        dto.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_OK));
        //sendNotification(dto, payment.getBaseUser().getEntity().getId());

        return true;
    }

    /**
     * sends an notification with a payment
     */
    public void sendNotification(PaymentDTOEx info, Integer entityId) {
        sendNotification(info, entityId, 
                new Integer(info.getPaymentResult().getId()).equals(Constants.RESULT_OK));
    }

    public void sendNotification(PaymentDTOEx info, Integer entityId, boolean success) {
        try {
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getPaymentMessage(entityId, info, success);

            INotificationSessionBean notificationSess =
                    (INotificationSessionBean) Context.getBean(
                    Context.Name.NOTIFICATION_SESSION);
            notificationSess.notify(info.getUserId(), message);
        } catch (NotificationNotFoundException e1) {
            // won't send anyting because the entity didn't specify the
            // notification
            LOG.warn("Can not notify a customer about a payment " +
                    "beacuse the entity lacks the notification. " +
                    "entity = %s", entityId);
        }
    }

    // for sending payment successful notification when invoice is not linked with payment.
    public void sendNotification(PaymentDTOEx info, Integer entityId, boolean success,Integer invoiceId) {
        try {
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getPaymentMessage(entityId, info, success);

            INotificationSessionBean notificationSess =
                    (INotificationSessionBean) Context.getBean(
                            Context.Name.NOTIFICATION_SESSION);

            InvoiceBL invoice = new InvoiceBL(invoiceId);
            UserBL user = new UserBL(invoice.getEntity().getUserId());
            message.addParameter("invoice_number", invoice.getEntity()
                    .getPublicNumber());
            message.addParameter("invoice", invoice.getEntity());
            message.addParameter("method", info.getInstrument().getPaymentMethod().getDescription(user.getEntity().getLanguage().getId()));
            notificationSess.notify(info.getUserId(), message);
        } catch (NotificationNotFoundException e1) {
            // won't send anyting because the entity didn't specify the
            // notification
            LOG.warn("Can not notify a customer about a payment " +
                    "beacuse the entity lacks the notification. " +
                    "entity = " + entityId);

        }
    }

    /*
     * The payment doesn't have to be set. It adjusts the balances of both the
     * payment and the invoice and deletes the map row.
     */
    public void removeInvoiceLink(Integer mapId) {
        try {
            // declare variables
            InvoiceDTO invoice;

            // find the map
            PaymentInvoiceMapDTO map = mapDas.find(mapId);
                // start returning the money to the payment's balance
                BigDecimal amount = map.getAmount();
                payment = map.getPayment();
                amount = amount.add(payment.getBalance());
                payment.setBalance(amount);

                // the balace of the invoice also increases
                invoice = map.getInvoiceEntity();
                amount = map.getAmount().add(invoice.getBalance());
                invoice.setBalance(amount);

                // this invoice probably has to be paid now
                if (InvoiceBL.isInvoiceBalanceEnoughToAge(invoice, invoice.getBaseUser().getEntity().getId())) {
                    invoice.setToProcess(1);
                }

            // log that this was deleted, otherwise there will be no trace
            eLogger.info(invoice.getBaseUser().getEntity().getId(),
                    payment.getBaseUser().getId(), mapId,
                    EventLogger.MODULE_PAYMENT_MAINTENANCE,
                    EventLogger.ROW_DELETED,
                    Constants.TABLE_PAYMENT_INVOICE_MAP);

            // get rid of the map all together
            mapDas.delete(map);

        } catch (EntityNotFoundException enfe) {
            LOG.error("Exception removing payment-invoice link: EntityNotFoundException", enfe);
        } catch (Exception e) {
            LOG.error("Exception removing payment-invoice link", e);
            throw new SessionInternalError(e);
        }
    }

    /**
     * This method removes the link between this payment and the
     * <i>invoiceId</i> of the Invoice
     * @param invoiceId Invoice Id to be unlinked from this payment
     */
    public boolean unLinkFromInvoice(Integer invoiceId) {

    	InvoiceDTO invoice= new InvoiceDAS().find(invoiceId);
		Iterator<PaymentInvoiceMapDTO> it = invoice.getPaymentMap().iterator();
		boolean bSucceeded= false;
        while (it.hasNext()) {
            PaymentInvoiceMapDTO map = it.next();
            if (this.payment.getId() == map.getPayment().getId()) {
	            this.removeInvoiceLink(map.getId());
	            invoice.getPaymentMap().remove(map);
                payment.getInvoicesMap().remove(map);
	            bSucceeded=true;

                //fire event
                PaymentUnlinkedFromInvoiceEvent event = new PaymentUnlinkedFromInvoiceEvent(
                        payment.getBaseUser().getEntity().getId(),
                        new PaymentDTOEx(payment),
                        invoice,
                        map.getAmount());
                EventManager.process(event);

	            map= null;
	            break;
            }
        }
        return bSucceeded;
    }

    public PaymentInvoiceMapDTOEx getMapDTO(Integer mapId) {
        // find the map
        PaymentInvoiceMapDTO map = mapDas.find(mapId);
        PaymentInvoiceMapDTOEx dto = new PaymentInvoiceMapDTOEx(map.getId(), map.getAmount(), map.getCreateDatetime());
        dto.setPaymentId(map.getPayment().getId());
        dto.setInvoiceId(map.getInvoiceEntity().getId());
        dto.setCurrencyId(map.getPayment().getCurrency().getId());
        return dto;
    }

    public List<PaymentDTO> findUserPaymentsPaged(Integer entityId, Integer userId, Integer limit, Integer offset) {

        return new PaymentDAS().findPaymentsByUserPaged(userId, limit, offset);
    }

    private void refreshRequiredRelations(PaymentInformationDTO newInstrument) {
    	if(newInstrument.getPaymentMethod() != null) {
    		newInstrument.setPaymentMethod(methodDas.find(newInstrument.getPaymentMethod().getId()));
    	}
    	
    	if(newInstrument.getPaymentMethodType() != null) {
    		newInstrument.setPaymentMethodType(new PaymentMethodTypeDAS().find(newInstrument.getPaymentMethodType().getId()));
    	}
    }
}
