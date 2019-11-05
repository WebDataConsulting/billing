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

package com.sapienter.jbilling.server.pluggableTask;

import java.text.ParseException;
import java.util.Date;

import com.sapienter.jbilling.common.SessionInternalError;


import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.ServerConstants;

/**
 * This creates payment dto. It now only goes and fetches the credit card
 * of the given user. It doesn't need to initialize the rest of the payment
 * information (amount, etc), only the info for the payment processor,
 * usually cc info but it could be electronic cheque, etc...
 * This task should consider that the user is a partner and is being paid
 * (like a refund) and therefore fetch some other information, as getting
 * paid with a cc seems not to be the norm.
 * @author Emil
 */
public class BasicPaymentInfoTask
        extends PluggableTask implements PaymentInfoTask {

    private static final FormatLogger LOG = new FormatLogger(BasicPaymentInfoTask.class);
    
    /**
     * Gets all the payment instruments of user and after verifying puts them in PaymentDTOEx
     */
    public PaymentDTOEx getPaymentInfo(Integer userId)
            throws TaskException {
        PaymentDTOEx retValue = new PaymentDTOEx();
        PaymentInformationBL paymentInfoBL = new PaymentInformationBL();
        
        try {
        	UserBL userBL = new UserBL(userId);
        	for(PaymentInformationDTO paymentInformation : userBL.getEntity().getPaymentInstruments()) {
        		LOG.debug("Payment instrument %s", paymentInformation.getId());
        		// If its a payment/credit card
        		
        		if(paymentInfoBL.isCreditCard(paymentInformation)) {
        			processCreditCard(retValue, paymentInformation);
        		} else if(paymentInfoBL.isACH(paymentInformation)) {
        			processACH(retValue, paymentInformation);
        		} else if(paymentInfoBL.isCheque(paymentInformation)){
        			processCheque(retValue, paymentInformation);
        		}
        	}
        }catch (Exception e) {
            throw new TaskException(e);
        }
        
        if (retValue.getPaymentInstruments().size() == 0) {
            LOG.debug("Could not find payment instrument for user %s", userId);
            return null;
        }
        
        return retValue;
    }

    /**
     * Processes the payment instrument that is a credit card and if its a valid credit card then adds it to payment instruments
     * 
     * @param dto	PaymentDTOEx
     * @param paymentInstrument	PaymentInformationDTO
     * @param piBl	PaymentInformationBL
     * @throws ParseException
     */
    protected void processCreditCard(PaymentDTOEx dto, PaymentInformationDTO paymentInstrument) throws ParseException {
    	PaymentInformationBL piBl = new PaymentInformationBL();
    	// TODO: some fields like gateway key that were being process in old implementation have been removed at all
    	String cardNumber = piBl.getStringMetaFieldByType(paymentInstrument, MetaFieldType.PAYMENT_CARD_NUMBER);
    	Date ccExpiryDate = piBl.getDateMetaFieldByType(paymentInstrument, MetaFieldType.DATE);
        if (cardNumber == null || ccExpiryDate == null) {
            throw new SessionInternalError("Payment Card information not found for customer :" + paymentInstrument.getUser().getId(),
                    new String[] { "PaymentWS,creditCard,validation.payment.card.data.not.found," +paymentInstrument.getUser().getId()});
        }
    	LOG.debug("Expiry date is: %s", ccExpiryDate);
    	if(piBl.validateCreditCard(ccExpiryDate, cardNumber)) {
    		LOG.debug("Card is valid");
    		PaymentInformationDTO paymentInformation = paymentInstrument.getDTO();
    		paymentInformation.setPaymentMethod(new PaymentMethodDAS().find(piBl.getPaymentMethod(cardNumber)));
    		
    		dto.getPaymentInstruments().add(paymentInformation);
    	}
    }
    
    /**
     * Process the payment instrument if its ach
     * @param dto	PaymentDTOEx
     * @param paymentInstrument	PaymentInformationDTO
     */
    protected void processACH(PaymentDTOEx dto, PaymentInformationDTO paymentInstrument) {
    	// TODO: some fields like gateway key that were being process in old implementation have been removed at all
    	PaymentInformationDTO paymentInformation = paymentInstrument.getDTO();
		paymentInformation.setPaymentMethod(new PaymentMethodDAS().find(ServerConstants.PAYMENT_METHOD_ACH));
		
		dto.getPaymentInstruments().add(paymentInformation);
    }
    
    /**
     * Process the payment instrument if its neither ach nor credit card
     * 
     * @param dto
     * @param paymentInstrument
     */
    protected void processCheque(PaymentDTOEx dto, PaymentInformationDTO paymentInstrument) {
    	PaymentInformationDTO paymentInformation = paymentInstrument.getDTO();
    	paymentInformation.setPaymentMethod(new PaymentMethodDAS().find(ServerConstants.PAYMENT_METHOD_CHEQUE));
    	
		dto.getPaymentInstruments().add(paymentInformation);
    }
}
