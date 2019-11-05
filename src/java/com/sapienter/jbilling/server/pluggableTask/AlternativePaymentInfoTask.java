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

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.ServerConstants;

;

/**
 * Created with IntelliJ IDEA.
 *
 *  This will check the preferred payment method by default
 *  If preferred method is not available it will check the next available payment method info
 *
 * @author Panche.Isajeski
 * @since 17/05/12
 */
public class AlternativePaymentInfoTask extends BasicPaymentInfoTask {

    private static final FormatLogger LOG = new FormatLogger(AlternativePaymentInfoTask.class);

    @Override
    public PaymentDTOEx getPaymentInfo(Integer userId) throws TaskException {
        PaymentDTOEx retValue = new PaymentDTOEx();
        try {
            UserBL userBL = new UserBL(userId);
            
            for(PaymentInformationDTO paymentInformation : userBL.getEntity().getPaymentInstruments()) {
        		// If its a payment/credit card
        		if(paymentInformation.getPaymentMethodType().getMethodName().equalsIgnoreCase(CommonConstants.PAYMENT_CARD)) {
        			processCreditCard(retValue, paymentInformation);
        		} else if(paymentInformation.getPaymentMethodType().getMethodName().equalsIgnoreCase(CommonConstants.ACH)) {
        			processACH(retValue, paymentInformation);
        		} else if(paymentInformation.getPaymentMethodType().getMethodName().equalsIgnoreCase(CommonConstants.CHEQUE)){
        			processCheque(retValue, paymentInformation);
        		}
        		
        		if(retValue.getPaymentInstruments().size()>0) {
        			return retValue;
        		}
        	}
        } catch (Exception e) {
            throw new TaskException(e);
        }
        if (retValue == null) {
            LOG.debug("Could not find payment instrument for user %s", userId);
        }
        return null;
    }
}
