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

package com.sapienter.jbilling.server.payment.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.ContactBL;

import java.math.BigDecimal;

/**
 * A pluggable PaymentTask that uses RBS WorldPay gateway for credit card
 * transactions.
 * 
 * The following parameters must be configured for this payment task to work: -
 * "URL" WordlPay gateway server url "StoreId" Store ID assigned by RBS WorldPay
 * "MerchantID" Merchant ID assigned by RBS WorldPay "TerminalId" Terminal ID
 * assigned by RBS WorldPay "SellerId" optional Store SellerId associated with
 * the StoreId. The SellerId is mandatory if the security flag is turned on for
 * the store. "Password" optional Password associated with the SellerId. The
 * Password is mandatory if the security flag is turned on for the store.
 * 
 * timeout_sec - number of seconds for timeout (in inheritance from
 * PaymentTaskWithTimeout)
 * 
 * @author othman
 */

public class PaymentWorldPayTask extends PaymentWorldPayBaseTask {
    private static final FormatLogger LOG = new FormatLogger(PaymentWorldPayTask.class);

    @Override
    String getProcessorName() { return "WorldPay"; }

    public boolean process(PaymentDTOEx payment) throws PluggableTaskException {
        LOG.debug("Payment processing for %s gateway", getProcessorName());

        if (payment.getPayoutId() != null) return true;

        SvcType transaction = SvcType.SALE;                      
        if (BigDecimal.ZERO.compareTo(payment.getAmount()) > 0 || (payment.getIsRefund() != 0)) {
            LOG.debug("Doing a refund using credit card transaction");
            transaction = SvcType.REFUND_CREDIT;            
        }

        boolean result;
        try {
            result = doProcess(payment, transaction, null).shouldCallOtherProcessors();
            LOG.debug("Processing result is %s, return value of process is %s",
                      payment.getPaymentResult().getId(), result);
            
        } catch (Exception e) {
            LOG.error("Exception", e);
            throw new PluggableTaskException(e);
        }
        return result;
    }

    public void failure(Integer userId, Integer retry) {
        // not supported
    }

    public boolean preAuth(PaymentDTOEx payment) throws PluggableTaskException {
        LOG.debug("Pre-authorization processing for %s gateway", getProcessorName());
        return doProcess(payment, SvcType.AUTHORIZE, null).shouldCallOtherProcessors();
    }

    public boolean confirmPreAuth(PaymentAuthorizationDTO auth, PaymentDTOEx payment)
            throws PluggableTaskException {
    	PaymentInformationBL piBl = new PaymentInformationBL();
        LOG.debug("Confirming pre-authorization for %s gateway", getProcessorName());

        if (!getProcessorName() .equals(auth.getProcessor())) {
            /*  let the processor be called and fail, so the caller can do something
                about it: probably re-call this payment task as a new "process()" run */
            LOG.warn("The processor of the pre-auth is not %s, is %s", getProcessorName(), auth.getProcessor());
        }

        PaymentInformationDTO card = payment.getInstrument();
        if (card == null || !piBl.isCreditCard(card)) {
            throw new PluggableTaskException("Credit card is required capturing" + " payment: " + payment.getId());
        }

        if (!isApplicable(payment)) {
            LOG.error("This payment can not be captured %s", payment);
            return true;
        }

        return doProcess(payment, SvcType.SETTLE, auth).shouldCallOtherProcessors();
    }

    @Override // implements abstract method
    public NVPList buildRequest(PaymentDTOEx payment, SvcType transaction) throws PluggableTaskException {
        NVPList request = new NVPList();

        request.add(PARAMETER_MERCHANT_ID, getMerchantId());
        request.add(PARAMETER_STORE_ID, getStoreId());
        request.add(PARAMETER_TERMINAL_ID, getTerminalId());
        request.add(PARAMETER_SELLER_ID, getSellerId());
        request.add(PARAMETER_PASSWORD, getPassword());
    
        ContactBL contact = new ContactBL();
        contact.set(payment.getUserId());

        request.add(WorldPayParams.General.STREET_ADDRESS, contact.getEntity().getAddress1());
        request.add(WorldPayParams.General.CITY, contact.getEntity().getCity());
        request.add(WorldPayParams.General.STATE, contact.getEntity().getStateProvince());
        request.add(WorldPayParams.General.ZIP, contact.getEntity().getPostalCode());

        request.add(WorldPayParams.General.FIRST_NAME, contact.getEntity().getFirstName());
        request.add(WorldPayParams.General.LAST_NAME, contact.getEntity().getLastName());
        request.add(WorldPayParams.General.COUNTRY, contact.getEntity().getCountryCode());

        request.add(WorldPayParams.General.AMOUNT, formatDollarAmount(payment.getAmount()));
        request.add(WorldPayParams.General.SVC_TYPE, transaction.getCode());
        
        PaymentInformationBL piBl = new PaymentInformationBL();
        PaymentInformationDTO card = payment.getInstrument();
        request.add(WorldPayParams.CreditCard.CARD_NUMBER, piBl.getStringMetaFieldByType(card, MetaFieldType.PAYMENT_CARD_NUMBER));
        request.add(WorldPayParams.CreditCard.EXPIRATION_DATE, EXPIRATION_DATE_FORMAT.print(piBl.getDateMetaFieldByType(card, MetaFieldType.DATE).getTime()));
        
        return request;
    }

}
