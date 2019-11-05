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

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.payment.IExternalCreditCardStorage;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Brian Cowdery
 * @since 20-10-2009
 */
public class PaymentWorldPayExternalTask extends PaymentWorldPayBaseTask implements IExternalCreditCardStorage {
    private static final FormatLogger LOG = new FormatLogger(PaymentWorldPayExternalTask.class);

    @Override
    String getProcessorName() { return "WorldPay"; }    

    public boolean process(PaymentDTOEx payment) throws PluggableTaskException {
        LOG.debug("Payment processing for %s gateway", getProcessorName());

        if (payment.getPayoutId() != null) return true;

        /*  build a ReAuthorize request if the payment instrument has a gateway key to
            be used, otherwise create a new Sale transaction using the raw CC data.

            if the payment amount is negative or refund is set, do a Credit transaction.
         */
        PaymentInformationBL piBl = new PaymentInformationBL();
        prepareExternalPayment(payment);
        SvcType transaction = (BigDecimal.ZERO.compareTo(payment.getAmount()) > 0 || payment.getIsRefund() != 0
                               ? SvcType.REFUND_CREDIT
                               : (piBl.useGatewayKey(payment.getInstrument())
                                  ? SvcType.RE_AUTHORIZE
                                  : SvcType.SALE));

        // process
        LOG.debug("creating %s payment transaction", transaction);
        Result result = doProcess(payment, transaction, null);

        // update the stored external gateway key
        if (CommonConstants.RESULT_OK.equals(payment.getResultId()))
            updateGatewayKey(payment);

        return result.shouldCallOtherProcessors();
    }

    public void failure(Integer userId, Integer retry) {
        // not supported
    }

    public boolean preAuth(PaymentDTOEx payment) throws PluggableTaskException {
        LOG.debug("Pre-authorization processing for %s gateway", getProcessorName());
        prepareExternalPayment(payment);
        return doProcess(payment, SvcType.AUTHORIZE, null).shouldCallOtherProcessors();
    }

    public boolean confirmPreAuth(PaymentAuthorizationDTO auth, PaymentDTOEx payment)
            throws PluggableTaskException {

        LOG.debug("Confirming pre-authorization for %s gateway", getProcessorName());

        if (!getProcessorName().equals(auth.getProcessor())) {
            /*  let the processor be called and fail, so the caller can do something
                about it: probably re-call this payment task as a new "process()" run */
            LOG.warn("The processor of the pre-auth is not %s, is %s", getProcessorName(), auth.getProcessor());
        }

        PaymentInformationDTO card = payment.getInstrument();
        if (card == null) {
            throw new PluggableTaskException("Credit card is required, capturing payment: " + payment.getId());
        }

        if (!isApplicable(payment)) {
            LOG.error("This payment can not be captured %s", payment);
            return true;
        }

        // process
        prepareExternalPayment(payment);
        Result result = doProcess(payment, SvcType.SETTLE, auth);

        // update the stored external gateway key
        if (CommonConstants.RESULT_OK.equals(payment.getResultId()))
            updateGatewayKey(payment);

        return result.shouldCallOtherProcessors();
    }

    /**
     * Prepares a given payment to be processed using an external storage gateway key instead of
     * the raw credit card number. If the associated credit card has been obscured it will be
     * replaced with the users stored credit card from the database, which contains all the relevant
     * external storage data.
     *
     * New or un-obscured credit cards will be left as is.
     *
     * @param payment payment to prepare for processing from external storage
     */
    public void prepareExternalPayment(PaymentDTOEx payment) {
    	
        if (new PaymentInformationBL().useGatewayKey(payment.getInstrument())) {
            LOG.debug("credit card is obscured, retrieving from database to use external store.");
            payment.setInstrument(payment.getInstrument());
        } else {
            LOG.debug("new credit card or previously un-obscured, using as is.");
        }
    }

    /**
     * Updates the gateway key of the credit card associated with this payment. RBS WorldPay
     * implements the gateway key a per-transaction ORDER_ID that is returned as part of the
     * payment response.
     *
     * @param payment successful payment containing the credit card to update.
     *  */
    public void updateGatewayKey(PaymentDTOEx payment) {
        PaymentAuthorizationDTO auth = payment.getAuthorization();
        PaymentInformationBL piBl = new PaymentInformationBL();
        // update the gateway key with the returned RBS WorldPay ORDER_ID
        PaymentInformationDTO card = payment.getInstrument();
        piBl.updateStringMetaField(card, auth.getTransactionId(), MetaFieldType.GATEWAY_KEY);

        // obscure new credit card numbers
        if (!CommonConstants.PAYMENT_METHOD_GATEWAY_KEY.equals(card.getPaymentMethod().getId()))
            piBl.obscureCreditCardNumber(card);
    }    

    /**
     * {@inheritDoc}
     *
     * Creates a payment of zero dollars and returns the RBC WorldPay ORDER_ID as the gateway
     * key to be stored for future transactions.
     */
    public String storeCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
        UserDTO user;
        if (contact != null) {
            UserBL bl = new UserBL(contact.getUserId());
            user = bl.getEntity();
            // do not load card from db, use the one that has been provided it should be a fresh fetch 
        } else if (instrument != null && instrument.getUser() != null) {
            user = instrument.getUser();
        } else {
            LOG.error("Could not determine user id for external credit card storage");
            return null;
        }

        // new contact that has not had a credit card created yet
        if (instrument == null) {
            LOG.warn("No credit card to store externally.");
            return null;
        }

        /*  Note, don't use PaymentBL.findPaymentInstrument() as the given creditCard is still being
            processed at the time that this event is being handled, and will not be found.

            PaymentBL()#create() will cause a stack overflow as it will attempt to update the credit card,
            emitting another NewCreditCardEvent which is then handled by this method and repeated.
         */
        PaymentDTO paymentInfo = new PaymentDTO();
        paymentInfo.setBaseUser(user);
        paymentInfo.setCurrency(user.getCurrency());
        paymentInfo.setAmount(BigDecimal.ZERO);
        paymentInfo.setCreditCard(instrument);
        paymentInfo.setPaymentMethod(new PaymentMethodDAS().find(Util.getPaymentMethod(new PaymentInformationBL().getStringMetaFieldByType(instrument, MetaFieldType.PAYMENT_CARD_NUMBER))));
        paymentInfo.setIsRefund(0);
        paymentInfo.setIsPreauth(0);
        paymentInfo.setDeleted(0);
        paymentInfo.setAttempt(1);
        paymentInfo.setPaymentDate(new Date());
        paymentInfo.setCreateDatetime(new Date());

        PaymentDTOEx payment = new PaymentDTOEx(new PaymentDAS().save(paymentInfo));

        try {
            doProcess(payment, SvcType.SALE, null);
        } catch (PluggableTaskException e) {
            LOG.error("Could not process external storage payment", e);
            return null;
        }        

        // if result is OK, return authorization transaction id as the gateway key
        return CommonConstants.RESULT_OK.equals(payment.getResultId())
               ? payment.getAuthorization().getTransactionId()
               : null;

    }

    /**
     * 
     */
    public String deleteCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
        //noop
        return null;
    }
    
    @Override // implements abstract method
    public NVPList buildRequest(PaymentDTOEx payment, SvcType transaction) throws PluggableTaskException {
        NVPList request = new NVPList();

        request.add(PARAMETER_MERCHANT_ID, getMerchantId());
        request.add(PARAMETER_STORE_ID, getStoreId());
        request.add(PARAMETER_TERMINAL_ID, getTerminalId());
        request.add(PARAMETER_SELLER_ID, getSellerId());
        request.add(PARAMETER_PASSWORD, getPassword());

        request.add(WorldPayParams.General.AMOUNT, formatDollarAmount(payment.getAmount()));
        request.add(WorldPayParams.General.SVC_TYPE, transaction.getCode());

        PaymentInformationDTO card = payment.getInstrument();

        /*  Sale transactions do not support the use of the ORDER_ID gateway key. After an initial sale
            transaction RBS WorldPay will have a record of our transactions for reference - so all
            other transaction types are safe for use with the stored gateway key.
         */
        PaymentInformationBL piBl = new PaymentInformationBL();
        if (SvcType.SALE.equals(transaction)
                && CommonConstants.PAYMENT_METHOD_GATEWAY_KEY.equals(Util.getPaymentMethod(piBl.getStringMetaFieldByType(card, MetaFieldType.PAYMENT_CARD_NUMBER)))) {
            throw new PluggableTaskException("Cannot process a SALE transaction with an obscured credit card!");
        }
        
        if (piBl.useGatewayKey(card)) {
            request.add(WorldPayParams.ReAuthorize.ORDER_ID, piBl.getStringMetaFieldByType(card, MetaFieldType.GATEWAY_KEY));

        } else {
            ContactBL contact = new ContactBL();
            contact.set(payment.getUserId());

            request.add(WorldPayParams.General.STREET_ADDRESS, contact.getEntity().getAddress1());
            request.add(WorldPayParams.General.CITY, contact.getEntity().getCity());
            request.add(WorldPayParams.General.STATE, contact.getEntity().getStateProvince());
            request.add(WorldPayParams.General.ZIP, contact.getEntity().getPostalCode());

            request.add(WorldPayParams.General.FIRST_NAME, contact.getEntity().getFirstName());
            request.add(WorldPayParams.General.LAST_NAME, contact.getEntity().getLastName());
            request.add(WorldPayParams.General.COUNTRY, contact.getEntity().getCountryCode());


            request.add(WorldPayParams.CreditCard.CARD_NUMBER, piBl.getStringMetaFieldByType(card, MetaFieldType.PAYMENT_CARD_NUMBER));
            request.add(WorldPayParams.CreditCard.EXPIRATION_DATE, EXPIRATION_DATE_FORMAT.print(piBl.getDateMetaFieldByType(card, MetaFieldType.DATE).getTime()));
        }

        return request;
    }
}
