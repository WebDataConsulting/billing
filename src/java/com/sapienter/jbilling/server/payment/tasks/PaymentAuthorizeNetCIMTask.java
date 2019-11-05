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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.util.Util;

import org.joda.time.format.DateTimeFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.payment.IExternalCreditCardStorage;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.pluggableTask.PaymentTask;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.ServerConstants;

public class PaymentAuthorizeNetCIMTask extends PaymentTaskWithTimeout
    implements PaymentTask, IExternalCreditCardStorage {

    private static final FormatLogger LOG = new FormatLogger(PaymentAuthorizeNetCIMTask.class);

    // pluggable task parameters names
    public static final ParameterDescription PARAMETER_NAME =
            new ParameterDescription("login", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_KEY =
            new ParameterDescription("transaction_key", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_TEST_MODE =
            new ParameterDescription("test", false, ParameterDescription.Type.STR); // true or false

    public static final ParameterDescription PARAMETER_CUSTOMER_EMAIL =
                new ParameterDescription("customer_email", false, ParameterDescription.Type.STR); // true or false

    /**
     * Validation mode allows you to generate a test transaction at the time you create a customer profile. In Test
     * Mode, only field validation is performed. In Live Mode, a transaction is generated and submitted to the processor
     * with the amount of $0.00 or $0.01. If successful, the transaction is immediately voided. Visa transactions are
     * being switched from $0.01 to $0.00 for all processors. All other credit card types use $0.01. We recommend you
     * consult your Merchant Account Provider before switching to Zero Dollar Authorizations for Visa, because you may
     * be subject to fees For Visa transactions using $0.00, the billTo address and billTo zip fields are required.
     */
    public static final ParameterDescription PARAMETER_VALIDATION_MODE =
            new ParameterDescription("validation_mode", false, ParameterDescription.Type.STR);  // none/testMode/liveMode

    //initializer for pluggable params
    {
        descriptions.add(PARAMETER_NAME);
        descriptions.add(PARAMETER_KEY);
        descriptions.add(PARAMETER_TEST_MODE);
        descriptions.add(PARAMETER_CUSTOMER_EMAIL);
        descriptions.add(PARAMETER_VALIDATION_MODE);
    }

    private String getProcessorName() {
        return "Authorize.Net CIM";
    }

    public boolean process(PaymentDTOEx info) throws PluggableTaskException {
        return doProcess(info, "profileTransAuthCapture", null);
    }

    public boolean preAuth(PaymentDTOEx info) throws PluggableTaskException {
        return doProcess(info, "profileTransAuthOnly", null);
    }

    public boolean confirmPreAuth(PaymentAuthorizationDTO auth, PaymentDTOEx info) throws PluggableTaskException {
        return doProcess(info, "profileTransCaptureOnly", auth.getApprovalCode());
    }

    public void failure(Integer userId, Integer retry) { /* noop */ }

    private boolean doProcess(PaymentDTOEx info, String txType, String approvalCode) throws PluggableTaskException {
        PaymentInformationBL piBl = new PaymentInformationBL();
    	int method = -1; /* 1 cc , 2 ach*/

        if (piBl.isCreditCard(info.getInstrument())) {
            method = 1;
        }
        if (piBl.isACH(info.getInstrument())) {
            method = 2;
        }

        if ( ServerConstants.PAYMENT_METHOD_ACH.equals(info.getInstrument().getPaymentMethod().getId() ) ) {
            method = 2;
            if (piBl.isACH(info.getInstrument())) {
                LOG.error("Can't process payment without a ACH Details");
                throw new PluggableTaskException("Payment Method ACH but ACH Info not present in payment");
            }
        } else {
            method=1;
        }

        if (!piBl.isCreditCard(info.getInstrument()) &&
            !piBl.isACH(info.getInstrument())) {
            LOG.error("Can't process without a credit card or ach");
            throw new PluggableTaskException("Credit card/ACH not present in payment");
        }

//        if (info.getCreditCard() != null &&
//          info.getAch() != null) {
//            LOG.warn("Both cc and ach are present");
//            method = 2; // default to ach (cheaper)
//        }

        if (isCreditCardStored(info, method == 1)) {
            LOG.debug("credit card is obscured, retrieving from database to use external store.");
            if (method == 1) {
            	// load only if its saved in database
            	if(info.getInstrument().getId() != null) {
            		info.setInstrument(new PaymentInformationDAS().find(info.getInstrument().getId()));
            	}
            }
        } else {
            /*  Credit cards being used for one time payments do not need to be saved in the CIM
               as they do not represent the customers primary card.

               Process using the next payment processor in the chain. This should be configured
               as the PaymentAuthorizeNetTask to process normal credit cards through Authorize.net
            */
            LOG.debug("One time payment credit card (not obscured!) or ACH Gateway Key Not available, process using the next PaymentTask.");
            info.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_UNAVAILABLE));
            return true;
        }

        String gatewayKey = piBl.getStringMetaFieldByType(info.getInstrument(), MetaFieldType.GATEWAY_KEY);
        AuthorizeNetCIMApi api = createApi();
        CustomerProfileData profile = CustomerProfileData.buildFromGatewayKey(gatewayKey);
        PaymentAuthorizationDTO paymentDTO = api.performTransaction(info.getAmount(),
                                                                    profile,
                                                                    txType, approvalCode);

        if (paymentDTO.getCode1().equals("1")) {
            info.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_OK));
            info.setAuthorization(paymentDTO);
            PaymentAuthorizationBL bl = new PaymentAuthorizationBL();
            bl.create(paymentDTO, info.getId());
            return false;
        } else {
            info.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_FAIL));
            info.setAuthorization(paymentDTO);
            PaymentAuthorizationBL bl = new PaymentAuthorizationBL();
            bl.create(paymentDTO, info.getId());
            return false;
        }
    }

    public String deleteCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
    	PaymentInformationBL piBl = new PaymentInformationBL();
        //credit card or ach was passed as null
        if (!piBl.isCreditCard(instrument) && !piBl.isACH(instrument)) {
            LOG.warn("No credit card/Ach details to store externally.");
            return null;
        }

        String gatewayKey= piBl.getStringMetaFieldByType(instrument, MetaFieldType.GATEWAY_KEY);
        
        try {
            deletePaymentProfile(instrument, createApi(), gatewayKey);
        } catch (PluggableTaskException e) {
            LOG.debug("Could not process delete event.");
            return null;
        }

        return gatewayKey;
    }

    public String storeCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
    	PaymentInformationBL piBl = new PaymentInformationBL();
        // new contact that has not had a credit card created yet
    	if (!piBl.isCreditCard(instrument) && !piBl.isACH(instrument)) {
            LOG.warn("No credit card/Ach details to store externally.");
            return null;
        }

        LOG.debug("Storing credit card/ach details info within %s gateway", getProcessorName());

        // fetch contact info if missing
        if (contact == null) {
            UserDTO user = null;
            if (instrument.getUser() != null) {
                user = instrument.getUser();
            }
            
            if (user != null) {
                ContactBL bl = new ContactBL();
                bl.set(user.getId());
                contact = bl.getEntity();
                contact.setBaseUser(user);
            }
        }

        // user does not have contact info
        if (contact == null) {
            LOG.error("Could not determine contact info for external credit card storage");
            return null;
        }

        try {
            CustomerProfileData profile = createOrUpdateProfile(contact, instrument, createApi());
            String gatewayKey = profile.toGatewayKey();
            LOG.debug("Obtained card reference number during external storage: %s", gatewayKey);
            return gatewayKey;
        } catch (PluggableTaskException e) {
            LOG.debug("Could not process external storage payment", e);
            throw new SessionInternalError(e);
        }
    }

    private AuthorizeNetCIMApi createApi() throws PluggableTaskException {
        return new AuthorizeNetCIMApi(ensureGetParameter(PARAMETER_NAME.getName()),
                                      ensureGetParameter(PARAMETER_KEY.getName()),
                                      getOptionalParameter(PARAMETER_VALIDATION_MODE.getName(), "none"),
                                      Boolean.valueOf(getOptionalParameter(PARAMETER_TEST_MODE.getName(), "false")),
                                      getTimeoutSeconds());
    }

    private CustomerProfileData createOrUpdateProfile(ContactDTO contact, PaymentInformationDTO instrument,
                                                      AuthorizeNetCIMApi api) throws PluggableTaskException {
    	PaymentInformationBL piBl = new PaymentInformationBL();
    	String gatewayKey = piBl.getStringMetaFieldByType(instrument, MetaFieldType.GATEWAY_KEY);
        if ( gatewayKey == null) {
            try {
                return api.createCustomerProfile(CustomerProfileData.buildFromContactAndCreditCardOrACH(contact, instrument, userCustomerEmail()), userCustomerEmail());
            } catch (DublicateProfileRecordException e) {
                return updateProfile(contact, instrument, e.getProfileId(), api);
            }
        } else {
            CustomerProfileData customerProfile = CustomerProfileData.buildFromGatewayKey(gatewayKey);
            return updateProfile(contact, instrument, customerProfile.getCustomerProfileId(), api);
        }
    }

    private CustomerProfileData updateProfile(ContactDTO contact, PaymentInformationDTO instrument, String profileID,
                                              AuthorizeNetCIMApi api) throws PluggableTaskException {

        CustomerProfileData customerProfile = api.getCustomerProfile(profileID, userCustomerEmail());
        customerProfile.fillWith(contact, instrument, userCustomerEmail());
        api.updateCustomerProfile(customerProfile, userCustomerEmail());

        return customerProfile;
    }

    private void deletePaymentProfile(PaymentInformationDTO instrument, AuthorizeNetCIMApi api, String gatewayKey) throws PluggableTaskException {
        try {
            CustomerProfileData customerProfile= CustomerProfileData.buildFromGatewayKey(gatewayKey);
            api.deletePaymentProfile(customerProfile);
        } catch (Exception e) {
            throw new PluggableTaskException("Could not process delete Payment Profile.");
        }
    }

    private static boolean isCreditCardStored(PaymentDTOEx payment, boolean bUseCreditCard) {
        LOG.debug("IsCreditCardStored called, bUseCreditCard = %s, instrument = %s", bUseCreditCard, payment.getInstrument());
        return new PaymentInformationBL().useGatewayKey(payment.getInstrument());
    }

    private boolean userCustomerEmail(){
        return Boolean.valueOf(getOptionalParameter(PARAMETER_CUSTOMER_EMAIL.getName(), "false"));
    }
}


class DublicateProfileRecordException extends Exception {

    private static final long serialVersionUID = 1L;
    private final String profileId;

    DublicateProfileRecordException(String profileId, String errorMessage) {
        super(errorMessage);
        this.profileId = profileId;
    }

    public String getProfileId() {
        return profileId;
    }
}

class CustomerProfileData {

    public static final int CREDIT_CARD = 1;
    public static final int BANK_ACCOUNT = 2;

    private static final String GATEWAY_KEY_DELIMITER = "/";

    private String customerProfileId;
    private String email;
    private String merchantCustomerId;

    private String customerPaymentProfileId;
    private String firstName;
    private String lastName;
    private String company;
    private String address;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String phoneNumber;
    private String faxNumber;
    private String creditCardNumber;
    private String creditCardExpirationDate;
    private String creditCardCode;
    // Added for ACH support
    private int paymentType;
    private String accountType;
    private String routingNumber;
    private String accountNumber;
    private String accountName;
    private String bankName;

    CustomerProfileData(String customerProfileId, String customerPaymentProfileId) {
        this.customerProfileId = customerProfileId;
        this.customerPaymentProfileId = customerPaymentProfileId;
    }

    CustomerProfileData() {
    }

    public String getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(String customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public String getCustomerPaymentProfileId() {
        return customerPaymentProfileId;
    }

    public void setCustomerPaymentProfileId(String customerPaymentProfileId) {
        this.customerPaymentProfileId = customerPaymentProfileId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMerchantCustomerId () {
        return merchantCustomerId;
    }

    public void setMerchantCustomerId (String merchantCustomerId) {
        this.merchantCustomerId = merchantCustomerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip () {
        return zip;
    }

    public void setZip (String zip) {
        this.zip = zip;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFaxNumber() {
        return faxNumber;
    }

    public void setFaxNumber(String faxNumber) {
        this.faxNumber = faxNumber;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public String getCreditCardExpirationDate() {
        return creditCardExpirationDate;
    }

    public void setCreditCardExpirationDate(String creditCardExpirationDate) {
        this.creditCardExpirationDate = creditCardExpirationDate;
    }

    public String getCreditCardCode() {
        return creditCardCode;
    }

    public void setCreditCardCode(String creditCardCode) {
        this.creditCardCode = creditCardCode;
    }

    public String toGatewayKey() {
        return customerProfileId + GATEWAY_KEY_DELIMITER + customerPaymentProfileId;
    }

    public int getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(int paymentType) {
        this.paymentType = paymentType;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getRoutingNumber() {
        return routingNumber;
    }

    public void setRoutingNumber(String routingNumber) {
        this.routingNumber = routingNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    
    public void fillWith(ContactDTO contact, PaymentInformationDTO instrument, boolean customerEmail) {
    	PaymentInformationBL piBl = new PaymentInformationBL();

    	if (contact != null) {
            if(customerEmail){
                setEmail(contact.getEmail());
            }else{
                setMerchantCustomerId(contact.getBaseUser().getEntity() + "-" + contact.getBaseUser().getId());
            }
            setFirstName(contact.getFirstName());
            setLastName(contact.getLastName());
            setCompany(contact.getOrganizationName());
            setAddress(contact.getAddress1());
            setCity(contact.getCity());
            setState(contact.getStateProvince());
            setZip(contact.getPostalCode());
            setCountry(contact.getCountryCode());
            setPhoneNumber(contact.getPhoneNumber());
            setFaxNumber(contact.getFaxNumber());
        }

        if (piBl.isACH(instrument)) {
            setAccountType(piBl.getStringMetaFieldByType(instrument, MetaFieldType.BANK_ROUTING_NUMBER));
            setRoutingNumber(piBl.getStringMetaFieldByType(instrument, MetaFieldType.BANK_ROUTING_NUMBER));
            setAccountNumber(piBl.getStringMetaFieldByType(instrument, MetaFieldType.BANK_ACCOUNT_NUMBER));
            setAccountName(piBl.getStringMetaFieldByType(instrument, MetaFieldType.TITLE));
            setBankName(piBl.getStringMetaFieldByType(instrument, MetaFieldType.BANK_NAME));
            setPaymentType(CustomerProfileData.BANK_ACCOUNT);
        }
        else if (piBl.isCreditCard(instrument)) {
        	String paymentCardNumber = piBl.getStringMetaFieldByType(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
            if (!instrument.isNumberObsucred(paymentCardNumber)) {
                setCreditCardNumber(paymentCardNumber);
                setCreditCardExpirationDate(DateTimeFormat.forPattern("yyyy-MM").print(piBl.getDateMetaFieldByType(instrument, MetaFieldType.DATE).getTime()));
            }

            setPaymentType(CustomerProfileData.CREDIT_CARD);
        }
    }

    public static CustomerProfileData buildFromGatewayKey(String gatewayKey) {
        int delimiterPosition = gatewayKey.indexOf(GATEWAY_KEY_DELIMITER);
        String customerProfileId = gatewayKey.substring(0, delimiterPosition);
        String paymentProfileId = gatewayKey.substring(delimiterPosition + GATEWAY_KEY_DELIMITER.length(),
                                                       gatewayKey.length());

        return new CustomerProfileData(customerProfileId, paymentProfileId);
    }

    public static CustomerProfileData buildFromContactAndCreditCardOrACH(ContactDTO contact, PaymentInformationDTO instrument, boolean customerEmail) {
        CustomerProfileData customerProfileData = new CustomerProfileData();
        customerProfileData.fillWith(contact, instrument, customerEmail);
        
        return customerProfileData;
    }
}

class AuthorizeNetCIMApi {
    private static final FormatLogger LOG = new FormatLogger(AuthorizeNetCIMApi.class);

    private static final String DUBLICATE_PROFILE_ID_PREFIX = "ID ";
    private static final String DUPLICATE_PROFILE_ERROR_CODE = "E00039";

    // Authorize.Net Web Service Resources
    private static final String AUTHNET_XML_TEST_URL = "https://apitest.authorize.net/xml/v1/request.api";
    private static final String AUTHNET_XML_PROD_URL = "https://api.authorize.net/xml/v1/request.api";
    private static final String AUTHNET_XML_NAMESPACE = "AnetApi/xml/v1/schema/AnetApiSchema.xsd";

    private final String loginID;
    private final String transactionKey;
    private final String validationMode;
    private final boolean testMode;
    private final int timeout;

    AuthorizeNetCIMApi(String loginID, String transactionKey, String validationMode, boolean testMode, int timeout) {
        this.loginID = loginID;
        this.transactionKey = transactionKey;
        this.validationMode = validationMode;
        this.testMode = testMode;
        this.timeout = timeout;
    }

    public PaymentAuthorizationDTO performTransaction(BigDecimal amount, CustomerProfileData customerProfile,
                                                      String txType, String approvalCode)
        throws PluggableTaskException {

        String XML = buildCustomerProfileTransactionRequest(amount, customerProfile, txType, approvalCode);
        String HTTPResponse = sendViaXML(XML);

        return parseCustomerProfileTransactionResponse(HTTPResponse);
    }

    public CustomerProfileData createCustomerProfile(CustomerProfileData customerProfile, boolean customerEmail)
        throws DublicateProfileRecordException, PluggableTaskException {

        String XML = buildCreateCustomerProfileRequest(customerProfile, customerEmail);
        String HTTPResponse = sendViaXML(XML);

        return parseCreateCustomerProfileResponse(HTTPResponse);
    }

    public CustomerProfileData getCustomerProfile(String customerProfileID, boolean customerEmail)
        throws PluggableTaskException {

        String XML = buildGetCustomerProfileRequest(customerProfileID);
        String HTTPResponse = sendViaXML(XML);

        return parseGetCustomerProfileResponse(HTTPResponse, customerEmail);
    }

    public void updateCustomerProfile(CustomerProfileData customerProfile, boolean customerEmail) throws PluggableTaskException {
        String XML = buildUpdateCustomerProfileRequest(customerProfile, customerEmail);
        String HTTPResponse = sendViaXML(XML);
        parseSimpleResponse(HTTPResponse, "updateCustomerProfile");

        if ( null == customerProfile.getCustomerPaymentProfileId()) {
            XML= buildCreateCustomerPaymentProfileRequest(customerProfile);
        } else {
            XML = buildUpdateCustomerPaymentProfileRequest(customerProfile);
        }
        HTTPResponse = sendViaXML(XML);
        if ( null == customerProfile.getCustomerPaymentProfileId()) {
            try {
                String customerPaymentProfileId= parseCreateCustomerPaymentProfileResponse(HTTPResponse);
                customerProfile.setCustomerPaymentProfileId(customerPaymentProfileId);
            } catch (DublicateProfileRecordException e) {
                LOG.warn("Error creating a Payment Profile for this customer in absence of any. %s",
                         customerProfile.getCustomerProfileId());
                throw new PluggableTaskException(e);
            }
        }
        parseSimpleResponse(HTTPResponse, "updateCustomerPaymentProfile");
    }

    public void deletePaymentProfile(CustomerProfileData customerProfile) throws PluggableTaskException {
        String XML = buildDeleteCustomerPaymentProfileRequest(customerProfile);
        String HTTPResponse = sendViaXML(XML);
        parseSimpleResponse(HTTPResponse, "deleteCustomerPaymentProfileRequest");
    }

    private void buildTag(StringBuffer xml, String name, String value) {
        xml.append(String.format("<%s>%s</%s>", name, Util.escapeStringForXmlFormat(value), name));
    }

    private void buildTagIfNotEmpty(StringBuffer xml, String name, String value) {
        if (value != null) {
            buildTag(xml, name, value);
        }
    }

    private void beginTag(StringBuffer xml, String name) {
        xml.append(String.format("<%s>", name));
    }

    private void endTag(StringBuffer xml, String name) {
        xml.append(String.format("</%s>", name));
    }

    private String getMerchantAuthenticationXML() throws PluggableTaskException {
        StringBuffer xml = new StringBuffer();

        beginTag(xml, "merchantAuthentication");
        buildTag(xml, "name", loginID);
        buildTag(xml, "transactionKey", transactionKey);
        endTag(xml, "merchantAuthentication");

        return xml.toString();
    }

    private String buildDeleteCustomerPaymentProfileRequest(CustomerProfileData customerProfileData)
        throws PluggableTaskException {
        StringBuffer XML = new StringBuffer();
        XML.append("<deleteCustomerPaymentProfileRequest xmlns=\"" + AUTHNET_XML_NAMESPACE + "\">");
        XML.append(getMerchantAuthenticationXML());
        buildTag(XML, "customerProfileId", customerProfileData.getCustomerProfileId());
        buildTag(XML, "customerPaymentProfileId", customerProfileData.getCustomerPaymentProfileId());
        endTag(XML, "deleteCustomerPaymentProfileRequest");
        return XML.toString();
    }

    private String buildCreateCustomerProfileRequest(CustomerProfileData customerProfileData, boolean customerEmail)
        throws PluggableTaskException {

        StringBuffer XML = new StringBuffer();
        XML.append("<createCustomerProfileRequest xmlns=\"" + AUTHNET_XML_NAMESPACE + "\">");
        XML.append(getMerchantAuthenticationXML());

        beginTag(XML, "profile");
        if(customerEmail){
            buildTag(XML, "email", customerProfileData.getEmail());
        }else{
            buildTag(XML, "merchantCustomerId", customerProfileData.getMerchantCustomerId());
        }

        beginTag(XML, "paymentProfiles");

        beginTag(XML, "billTo");
        buildTag(XML, "firstName", customerProfileData.getFirstName());
        buildTag(XML, "lastName", customerProfileData.getLastName());
        buildTag(XML, "company", customerProfileData.getCompany());
        buildTag(XML, "address", customerProfileData.getAddress());
        buildTag(XML, "city", customerProfileData.getCity());
        buildTag(XML, "state", customerProfileData.getState());
        buildTag(XML, "zip", customerProfileData.getZip());
        buildTag(XML, "country", customerProfileData.getCountry());
        buildTag(XML, "phoneNumber", customerProfileData.getPhoneNumber());
        buildTag(XML, "faxNumber", customerProfileData.getFaxNumber());
        endTag(XML, "billTo");

        beginTag(XML, "payment");
        if (customerProfileData.getPaymentType() == CustomerProfileData.CREDIT_CARD) {
            beginTag(XML, "creditCard");
            buildTag(XML, "cardNumber", customerProfileData.getCreditCardNumber());
            buildTag(XML, "expirationDate", customerProfileData.getCreditCardExpirationDate());
            buildTagIfNotEmpty(XML, "cardCode", customerProfileData.getCreditCardCode());
            endTag(XML, "creditCard");
        } else if (customerProfileData.getPaymentType() == CustomerProfileData.BANK_ACCOUNT) {
            beginTag(XML, "bankAccount");
            buildTag(XML, "accountType", customerProfileData.getAccountType());
            buildTag(XML, "routingNumber", customerProfileData.getRoutingNumber());
            buildTag(XML, "accountNumber", customerProfileData.getAccountNumber());
            buildTag(XML, "nameOnAccount", customerProfileData.getAccountName());
            buildTag(XML, "bankName", customerProfileData.getBankName());
            endTag(XML, "bankAccount");
        }
        endTag(XML, "payment");

        endTag(XML, "paymentProfiles");
        endTag(XML, "profile");
        buildTag(XML, "validationMode", validationMode);
        endTag(XML, "createCustomerProfileRequest");

        return XML.toString();
    }

    private String buildUpdateCustomerProfileRequest(CustomerProfileData customerProfileData, boolean customerEmail)
        throws PluggableTaskException {

        StringBuffer XML = new StringBuffer();
        XML.append("<updateCustomerProfileRequest xmlns=\"" + AUTHNET_XML_NAMESPACE + "\">");
        XML.append(getMerchantAuthenticationXML());

        beginTag(XML, "profile");

        if(customerEmail){
            buildTag(XML, "email", customerProfileData.getEmail());
        }else {
            buildTag(XML, "merchantCustomerId", customerProfileData.getMerchantCustomerId());
        }
        buildTag(XML, "customerProfileId", customerProfileData.getCustomerProfileId());
        endTag(XML, "profile");

        endTag(XML, "updateCustomerProfileRequest");

        return XML.toString();
    }

    private String buildCreateCustomerPaymentProfileRequest(CustomerProfileData customerProfileData)
    throws PluggableTaskException {

    StringBuffer XML = new StringBuffer();
    XML.append("<createCustomerPaymentProfileRequest xmlns=\"" + AUTHNET_XML_NAMESPACE + "\">");
    XML.append(getMerchantAuthenticationXML());

    buildTag(XML, "customerProfileId", customerProfileData.getCustomerProfileId());

    beginTag(XML, "paymentProfile");

    beginTag(XML, "billTo");
    buildTag(XML, "firstName", customerProfileData.getFirstName());
    buildTag(XML, "lastName", customerProfileData.getLastName());
    buildTag(XML, "company", customerProfileData.getCompany());
    buildTag(XML, "address", customerProfileData.getAddress());
    buildTag(XML, "city", customerProfileData.getCity());
    buildTag(XML, "state", customerProfileData.getState());
    buildTag(XML, "zip", customerProfileData.getZip());
    buildTag(XML, "country", customerProfileData.getCountry());
    buildTag(XML, "phoneNumber", customerProfileData.getPhoneNumber());
    buildTag(XML, "faxNumber", customerProfileData.getFaxNumber());
    endTag(XML, "billTo");

    beginTag(XML, "payment");
    if (customerProfileData.getPaymentType() == CustomerProfileData.CREDIT_CARD) {
        beginTag(XML, "creditCard");
        buildTag(XML, "cardNumber", customerProfileData.getCreditCardNumber());
        buildTag(XML, "expirationDate", customerProfileData.getCreditCardExpirationDate());
        buildTagIfNotEmpty(XML, "cardCode", customerProfileData.getCreditCardCode());
        endTag(XML, "creditCard");
    } else if (customerProfileData.getPaymentType() == CustomerProfileData.BANK_ACCOUNT) {
        beginTag(XML, "bankAccount");
        buildTag(XML, "accountType", customerProfileData.getAccountType());
        buildTag(XML, "routingNumber", customerProfileData.getRoutingNumber());
        buildTag(XML, "accountNumber", customerProfileData.getAccountNumber());
        buildTag(XML, "nameOnAccount", customerProfileData.getAccountName());
        buildTag(XML, "bankName", customerProfileData.getBankName());
        endTag(XML, "bankAccount");
    }
    endTag(XML, "payment");
    endTag(XML, "paymentProfile");
    endTag(XML, "createCustomerPaymentProfileRequest");

    return XML.toString();
}

    private String buildUpdateCustomerPaymentProfileRequest(CustomerProfileData customerProfileData)
        throws PluggableTaskException {

        StringBuffer XML = new StringBuffer();
        XML.append("<updateCustomerPaymentProfileRequest xmlns=\"" + AUTHNET_XML_NAMESPACE + "\">");
        XML.append(getMerchantAuthenticationXML());

        buildTag(XML, "customerProfileId", customerProfileData.getCustomerProfileId());

        beginTag(XML, "paymentProfile");

        beginTag(XML, "billTo");
        buildTag(XML, "firstName", customerProfileData.getFirstName());
        buildTag(XML, "lastName", customerProfileData.getLastName());
        buildTag(XML, "company", customerProfileData.getCompany());
        buildTag(XML, "address", customerProfileData.getAddress());
        buildTag(XML, "city", customerProfileData.getCity());
        buildTag(XML, "state", customerProfileData.getState());
        buildTag(XML, "zip", customerProfileData.getZip());
        buildTag(XML, "country", customerProfileData.getCountry());
        buildTag(XML, "phoneNumber", customerProfileData.getPhoneNumber());
        buildTag(XML, "faxNumber", customerProfileData.getFaxNumber());
        endTag(XML, "billTo");

        beginTag(XML, "payment");
        if (customerProfileData.getPaymentType() == CustomerProfileData.CREDIT_CARD) {
            beginTag(XML, "creditCard");
            buildTag(XML, "cardNumber", customerProfileData.getCreditCardNumber());
            buildTag(XML, "expirationDate", customerProfileData.getCreditCardExpirationDate());
            buildTagIfNotEmpty(XML, "cardCode", customerProfileData.getCreditCardCode());
            endTag(XML, "creditCard");
        } else if (customerProfileData.getPaymentType() == CustomerProfileData.BANK_ACCOUNT) {
            beginTag(XML, "bankAccount");
            buildTag(XML, "accountType", customerProfileData.getAccountType());
            buildTag(XML, "routingNumber", customerProfileData.getRoutingNumber());
            buildTag(XML, "accountNumber", customerProfileData.getAccountNumber());
            buildTag(XML, "nameOnAccount", customerProfileData.getAccountName());
            buildTag(XML, "bankName", customerProfileData.getBankName());
            endTag(XML, "bankAccount");
        }
        endTag(XML, "payment");

        buildTag(XML, "customerPaymentProfileId", customerProfileData.getCustomerPaymentProfileId());
        endTag(XML, "paymentProfile");

        endTag(XML, "updateCustomerPaymentProfileRequest");

        return XML.toString();
    }

    private String buildGetCustomerProfileRequest(String customerProfileId) throws PluggableTaskException {

        StringBuffer XML = new StringBuffer();
        XML.append("<getCustomerProfileRequest xmlns=\"" + AUTHNET_XML_NAMESPACE + "\">");
        XML.append(getMerchantAuthenticationXML());
        buildTag(XML, "customerProfileId", customerProfileId);
        endTag(XML, "getCustomerProfileRequest");

        return XML.toString();
    }

    private String buildCustomerProfileTransactionRequest(BigDecimal amount, CustomerProfileData customerProfile,
                                                          String transactionType, String approvalCode)
        throws PluggableTaskException {

        StringBuffer XML = new StringBuffer();

        XML.append("<createCustomerProfileTransactionRequest xmlns=\"" + AUTHNET_XML_NAMESPACE + "\">");
        XML.append(getMerchantAuthenticationXML());

        beginTag(XML, "transaction");
        beginTag(XML, transactionType);
        buildTag(XML, "amount", amount.toString());
        buildTag(XML, "customerProfileId", customerProfile.getCustomerProfileId());
        buildTag(XML, "customerPaymentProfileId", customerProfile.getCustomerPaymentProfileId());

        if ("profileTransCaptureOnly".equals(transactionType)) {
            buildTag(XML, "approvalCode", approvalCode);
        }

        endTag(XML, transactionType);
        endTag(XML, "transaction");
        XML.append("<extraOptions><![CDATA[x_delim_char=|&x_encap_char=]]></extraOptions>");
        endTag(XML, "createCustomerProfileTransactionRequest");

        return XML.toString();
    }

    /**
     * Sends the request to the Authorize.Net payment processor
     *
     * @param data String The HTTP POST formatted as a GET string
     * @return String
     * @throws PluggableTaskException when error occured.
     */
    private String sendViaXML(String data) throws PluggableTaskException {
        int ch;
        StringBuffer responseText = new StringBuffer();
        String XML = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + data;

        try {
            // Set up the connection
            URL url = testMode ? new URL(AUTHNET_XML_TEST_URL) : new URL(AUTHNET_XML_PROD_URL);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("CONTENT-TYPE", "application/xml");
            conn.setConnectTimeout(timeout * 1000);
            conn.setDoOutput(true);

            LOG.debug("Sending request: %s", XML);

            // Send the request
            OutputStream ostream = conn.getOutputStream();
            ostream.write(XML.getBytes());
            ostream.close();

            // Get the response
            InputStream istream = conn.getInputStream();
            while ((ch = istream.read()) != -1)
                responseText.append((char) ch);
            istream.close();
            responseText.replace(0, 3, ""); // KLUDGE: Strips erroneous chars from response stream.

            LOG.debug("Authorize.Net response: %s", responseText);

            return responseText.toString();
        } catch (Exception e) {

            LOG.error(e);
            throw new PluggableTaskException(e);
        }
    }

    private PaymentAuthorizationDTO parseCustomerProfileTransactionResponse(String HTTPResponse)
        throws PluggableTaskException {

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource inStream = new InputSource();
            inStream.setCharacterStream(new StringReader(HTTPResponse));
            Document doc = builder.parse(inStream);
            doc.getDocumentElement().normalize();
            Element rootElement = doc.getDocumentElement();
            NodeList nodeLst = rootElement.getChildNodes();
            NodeList messagesNodeLst = nodeLst.item(0).getChildNodes();
            NodeList resultCodeNodeLst = messagesNodeLst.item(0).getChildNodes();
            String resultCode = resultCodeNodeLst.item(0).getNodeValue();
            NodeList messageNodeLst = messagesNodeLst.item(1).getChildNodes();
            NodeList codeNodeLst = messageNodeLst.item(0).getChildNodes();
            String code = codeNodeLst.item(0).getNodeValue();
            NodeList textNodeLst = messageNodeLst.item(1).getChildNodes();
            String text = textNodeLst.item(0).getNodeValue();

            PaymentAuthorizationDTO paymentDTO = new PaymentAuthorizationDTO();

            // check for errors
            if (!resultCode.equals("Ok")) {
                paymentDTO.setCode1(resultCode);
                paymentDTO.setCode2(code);
                paymentDTO.setResponseMessage(text);
                paymentDTO.setProcessor("PaymentAuthorizeNetCIMTask");

                return paymentDTO;
            }
            /**
             * If the response was ok the direct response node gets parsed and
             * PaymentAuthorizationDTO gets updated with the parsed values
             */
            NodeList directResponseNodeLst = nodeLst.item(1).getChildNodes();
            String response = directResponseNodeLst.item(0).getNodeValue();
            String[] responseList = response.split("\\|", -2);
            paymentDTO.setApprovalCode(responseList[4]);
            paymentDTO.setAvs(responseList[5]);
            paymentDTO.setProcessor("PaymentAuthorizeNetCIMTask");
            paymentDTO.setCode1(responseList[0]);
            paymentDTO.setCode2(responseList[1]);
            paymentDTO.setCode3(responseList[2]);
            paymentDTO.setResponseMessage(responseList[3]);
            paymentDTO.setTransactionId(responseList[6]);
            paymentDTO.setMD5(responseList[37]);
            paymentDTO.setCreateDate(Calendar.getInstance().getTime());

            return paymentDTO;
        } catch (Exception e) {

            LOG.error(e);
            throw new PluggableTaskException(e);
        }
    }

    private CustomerProfileData parseCreateCustomerProfileResponse(String HTTPResponse)
        throws PluggableTaskException, DublicateProfileRecordException {

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource inStream = new InputSource();
            inStream.setCharacterStream(new StringReader(HTTPResponse));
            Document doc = builder.parse(inStream);
            doc.getDocumentElement().normalize();
            Element rootElement = doc.getDocumentElement();
            NodeList nodeLst = rootElement.getChildNodes();
            NodeList messagesNodeLst = nodeLst.item(0).getChildNodes();
            NodeList resultCodeNodeLst = messagesNodeLst.item(0).getChildNodes();
            String resultCode = resultCodeNodeLst.item(0).getNodeValue();
            NodeList messageNodeLst = messagesNodeLst.item(1).getChildNodes();
            NodeList codeNodeLst = messageNodeLst.item(0).getChildNodes();
            String code = codeNodeLst.item(0).getNodeValue();
            NodeList textNodeLst = messageNodeLst.item(1).getChildNodes();
            String text = textNodeLst.item(0).getNodeValue();

            // check for errors
            if (!resultCode.equals("Ok")) {
                String errorMessage = String.format(
                    "Authorize.Net createCustomerProfile error: %s (code1: %s, code2: %s)",
                    text, resultCode, code);

                if (DUPLICATE_PROFILE_ERROR_CODE.equals(code)) {
                    throwDuplicateProfileError(errorMessage, text);
                }

                throw new PluggableTaskException(errorMessage);
            }

            /**
             * If the response was ok the direct response node gets parsed and
             * PaymentAuthorizationDTO gets updated with the parsed values
             */
            NodeList customerProfileIdNodeLst = nodeLst.item(1).getChildNodes();
            String customerProfileId = customerProfileIdNodeLst.item(0).getNodeValue();
            NodeList customerPaymentProfileIdListNodeLst = nodeLst.item(2).getChildNodes();
            NodeList numericStringNodeLst = customerPaymentProfileIdListNodeLst.item(0).getChildNodes();
            String customerPaymentProfileId = numericStringNodeLst.item(0).getNodeValue();

            return new CustomerProfileData(customerProfileId, customerPaymentProfileId);
        } catch (DublicateProfileRecordException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e);
            throw new PluggableTaskException(e);
        }
    }

    /**
     * Check for errors in the messages node of the getCustomerProfileResponse xml file.
     * @param messagesNode
     * @throws PluggableTaskException throw the exception if the resultCode node is different from "ok".
     */
    private void parseGetCustomerProfileResponseNodeMessages(Node messagesNode)
            throws PluggableTaskException {
        Node currentNode = null;
        String resultCode = null;
        String code = null;
        String text = null;

        for (int i = 0; i < messagesNode.getChildNodes().getLength(); i++) {

            currentNode = messagesNode.getChildNodes().item(i);
            if ("resultCode".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>messages>result Node
                resultCode = currentNode.getTextContent();
            } else if ("message".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>messages>message Node
                Node messageChildNode = null;
                for (int j = 0; j < currentNode.getChildNodes().getLength(); j++) {
                    messageChildNode = currentNode.getChildNodes().item(j);
                    if ("code".equalsIgnoreCase(messageChildNode.getNodeName())) {
                        // getCustomerProfileResponse>messages>message>code Node
                        code = messageChildNode.getTextContent();
                    } else if ("text".equalsIgnoreCase(messageChildNode.getNodeName().toLowerCase())) {
                        // getCustomerProfileResponse>message>message>text Node
                        text = messageChildNode.getTextContent();
                    }
                }
            }

        }

        // check for errors
        if (resultCode != null && !"ok".equalsIgnoreCase(resultCode.trim())) {
            String errorMessage = String.format("Authorize.Net getCustomerProfile error: %s (code1: %s, code2: %s)",
                    text, resultCode, code);

            throw new PluggableTaskException(errorMessage);
        }
    }

    /**
     * Parse the profile node of getCustomerProfileResponse xml file and fill the data in the CustomerProfileData object.
     * @param profileNode
     * @param customerProfileData
     */
    private void parseGetCustomerProfileResponseNodeProfile(Node profileNode, CustomerProfileData customerProfileData) {
        Node currentNode = null;
        for (int i = 0; i < profileNode.getChildNodes().getLength(); i++) {

            currentNode = profileNode.getChildNodes().item(i);
            if ("merchantCustomerId".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>merchantCustomerId
                customerProfileData.setMerchantCustomerId(currentNode.getTextContent());
            } else if ("customerProfileId".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>customerProfileId
                customerProfileData.setCustomerProfileId(currentNode.getTextContent());
            } else if ("email".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>customerProfileId
                customerProfileData.setEmail(currentNode.getTextContent());
            } else if ("paymentProfiles".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>customerProfileId
                parseGetCustomerProfileResponseNodePaymentProfiles(currentNode, customerProfileData);
            }

        }
    }

    /**
     * Parse the paymentProfiles node of getCustomerProfileResponse xml file and fill the data in the CustomerProfileData object.
     * @param paymentProfilesNode
     * @param customerProfileData
     */
    private void parseGetCustomerProfileResponseNodePaymentProfiles(Node paymentProfilesNode,
                                                                    CustomerProfileData customerProfileData) {
        Node currentNode = null;
        for (int i = 0; i < paymentProfilesNode.getChildNodes().getLength(); i++) {

            currentNode = paymentProfilesNode.getChildNodes().item(i);
            if ("customerPaymentProfileId".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>customerPaymentProfileId
                customerProfileData.setCustomerPaymentProfileId(currentNode.getTextContent());
            } else if ("billTo".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>billTo
                parseGetCustomerProfileResponseNodeBillTo(currentNode, customerProfileData);
            } else if ("payment".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>payment
                Node paymentChildNode = null;
                for (int j = 0; j < currentNode.getChildNodes().getLength(); j++) {

                    paymentChildNode = currentNode.getChildNodes().item(j);
                    if ("creditCard".equalsIgnoreCase(paymentChildNode.getNodeName())) {
                        // getCustomerProfileResponse>profile>paymentProfiles>payment>creditCard
                        parseGetCustomerProfileResponseNodeCreditCard(paymentChildNode, customerProfileData);
                    } else if("bankAccount".equalsIgnoreCase(paymentChildNode.getNodeName())) {
                        // getCustomerProfileResponse>profile>paymentProfiles>payment>bankAccount
                        parseGetCustomerProfileResponseNodeBankAccount(paymentChildNode, customerProfileData);
                    }

                }
            }

        }
    }

    /**
     * Parse the billTo node of getCustomerProfileResponse xml file and fill the data in the CustomerProfileData object.
     * @param billToNode
     * @param customerProfileData
     */
    private void parseGetCustomerProfileResponseNodeBillTo(Node billToNode,
                                                           CustomerProfileData customerProfileData) {
        Node currentNode = null;
        for (int i = 0; i < billToNode.getChildNodes().getLength(); i++) {

            currentNode = billToNode.getChildNodes().item(i);
            if ("firstName".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>billTo>firstName
                customerProfileData.setFirstName(currentNode.getTextContent());
            } else if ("lastName".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>billTo>lastName
                customerProfileData.setLastName(currentNode.getTextContent());
            } else if ("company".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>billTo>company
                customerProfileData.setCompany(currentNode.getTextContent());
            } else if ("address".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>billTo>address
                customerProfileData.setAddress(currentNode.getTextContent());
            } else if ("city".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>billTo>city
                customerProfileData.setCity(currentNode.getTextContent());
            } else if ("state".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>billTo>state
                customerProfileData.setState(currentNode.getTextContent());
            } else if ("zip".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>billTo>zip
                customerProfileData.setZip(currentNode.getTextContent());
            } else if ("country".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>billTo>country
                customerProfileData.setCountry(currentNode.getTextContent());
            } else if ("phoneNumber".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>billTo>phoneNumber
                customerProfileData.setPhoneNumber(currentNode.getTextContent());
            } else if ("faxNumber".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>billTo>faxNumber
                customerProfileData.setFaxNumber(currentNode.getTextContent());
            }
        }
    }

    /**
     * Parse the creditCard node of getCustomerProfileResponse xml file and fill the data in the CustomerProfileData object.
     * @param creditCardNode
     * @param customerProfileData
     */
    private void parseGetCustomerProfileResponseNodeCreditCard(Node creditCardNode,
                                                               CustomerProfileData customerProfileData) {
        Node currentNode = null;
        for (int i = 0; i < creditCardNode.getChildNodes().getLength(); i++) {

            currentNode = creditCardNode.getChildNodes().item(i);
            if ("cardNumber".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>payment>creditCard>cardNumber
                customerProfileData.setCreditCardNumber(currentNode.getTextContent());
            } else if ("expirationDate".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>payment>creditCard>expirationDate
                customerProfileData.setCreditCardExpirationDate(currentNode.getTextContent());
            }

        }
    }

    /**
     * Parse the bankAccount node of getCustomerProfileResponse xml file and fill the data in the CustomerProfileData object.
     * @param bankAccountNode
     * @param customerProfileData
     */
    private void parseGetCustomerProfileResponseNodeBankAccount(Node bankAccountNode,
                                                                CustomerProfileData customerProfileData) {
        Node currentNode = null;
        for (int i = 0; i < bankAccountNode.getChildNodes().getLength(); i++) {

            currentNode = bankAccountNode.getChildNodes().item(i);
            if ("accountType".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>payment>bankAccount>accountType
                customerProfileData.setCountry(currentNode.getTextContent());
            } else if ("routingNumber".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>payment>bankAccount>routingNumber
                customerProfileData.setCountry(currentNode.getTextContent());
            } else if ("accountNumber".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>payment>bankAccount>accountNumber
                customerProfileData.setCountry(currentNode.getTextContent());
            } else if ("nameOnAccount".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>payment>bankAccount>nameOnAccount
                customerProfileData.setCountry(currentNode.getTextContent());
            } else if ("bankName".equalsIgnoreCase(currentNode.getNodeName())) {
                // getCustomerProfileResponse>profile>paymentProfiles>payment>bankAccount>bankName
                customerProfileData.setCountry(currentNode.getTextContent());
            }
        }

    }

    private CustomerProfileData parseGetCustomerProfileResponse(String HTTPResponse, boolean customerEmail) throws PluggableTaskException {
        CustomerProfileData customerProfile = null;
        try {
        	
        	LOG.debug("getCustomerProfileResponse \n%s", HTTPResponse);

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource inStream = new InputSource();
            inStream.setCharacterStream(new StringReader(HTTPResponse));
            Document doc = builder.parse(inStream);
            doc.getDocumentElement().normalize();
            Element rootElement = doc.getDocumentElement();

            Node currentNode = null;
            Node messagesNode = null;
            Node profileNode = null;

            for (int i = 0; i < rootElement.getChildNodes().getLength(); i++) {
                currentNode = rootElement.getChildNodes().item(i);
                if (currentNode.getNodeName().equalsIgnoreCase("messages")) {
                    // getCustomerProfileResponse>messages
                    messagesNode = currentNode;
                } else if (currentNode.getNodeName().equalsIgnoreCase("profile")) {
                    // getCustomerProfileResponse>profile
                    profileNode = currentNode;
                }
            }

            parseGetCustomerProfileResponseNodeMessages(messagesNode);
            customerProfile = new CustomerProfileData();
            parseGetCustomerProfileResponseNodeProfile(currentNode, customerProfile);

        } catch (Exception e) {
            LOG.error(e);
            throw new PluggableTaskException(e);
        }
        return customerProfile;
    }

    private void parseSimpleResponse(String HTTPResponse, String request) throws PluggableTaskException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource inStream = new InputSource();
            inStream.setCharacterStream(new StringReader(HTTPResponse));
            Document doc = builder.parse(inStream);
            doc.getDocumentElement().normalize();
            Element rootElement = doc.getDocumentElement();
            NodeList nodeLst = rootElement.getChildNodes();
            NodeList messagesNodeLst = nodeLst.item(0).getChildNodes();
            NodeList resultCodeNodeLst = messagesNodeLst.item(0).getChildNodes();
            String resultCode = resultCodeNodeLst.item(0).getNodeValue();
            NodeList messageNodeLst = messagesNodeLst.item(1).getChildNodes();
            NodeList codeNodeLst = messageNodeLst.item(0).getChildNodes();
            String code = codeNodeLst.item(0).getNodeValue();
            NodeList textNodeLst = messageNodeLst.item(1).getChildNodes();
            String text = textNodeLst.item(0).getNodeValue();

            // check for errors
            if (!resultCode.equals("Ok")) {
                String errorMessage = String.format("Authorize.Net %s error: %s (code1: %s, code2: %s)",
                                                    request, text, resultCode, code);

                throw new PluggableTaskException(errorMessage);
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new PluggableTaskException(e);
        }
    }

    private String parseCreateCustomerPaymentProfileResponse(String HTTPResponse)
        throws PluggableTaskException, DublicateProfileRecordException {

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource inStream = new InputSource();
            inStream.setCharacterStream(new StringReader(HTTPResponse));
            Document doc = builder.parse(inStream);
            doc.getDocumentElement().normalize();
            Element rootElement = doc.getDocumentElement();
            NodeList nodeLst = rootElement.getChildNodes();
            NodeList messagesNodeLst = nodeLst.item(0).getChildNodes();
            NodeList resultCodeNodeLst = messagesNodeLst.item(0).getChildNodes();
            String resultCode = resultCodeNodeLst.item(0).getNodeValue();
            NodeList messageNodeLst = messagesNodeLst.item(1).getChildNodes();
            NodeList codeNodeLst = messageNodeLst.item(0).getChildNodes();
            String code = codeNodeLst.item(0).getNodeValue();
            NodeList textNodeLst = messageNodeLst.item(1).getChildNodes();
            String text = textNodeLst.item(0).getNodeValue();

            // check for errors
            if (!resultCode.equals("Ok")) {
                String errorMessage = String.format(
                    "Authorize.Net createCustomerPaymentProfile error: %s (code1: %s, code2: %s)",
                    text, resultCode, code);

                if (DUPLICATE_PROFILE_ERROR_CODE.equals(code)) {
                    throwDuplicateProfileError(errorMessage, text);
                }
                throw new PluggableTaskException(errorMessage);
            }

            /**
             * If the response was ok the direct response node gets parsed and
             * a new Customer Payment Profile ID gets returned with the parsed values
             */
            NodeList customerPaymentProfileIdNodeLst = nodeLst.item(1).getChildNodes();
            String customerPaymentProfileId = customerPaymentProfileIdNodeLst.item(0).getNodeValue();

            return customerPaymentProfileId;
        } catch (DublicateProfileRecordException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e);
            throw new PluggableTaskException(e);
        }
    }

    private static void throwDuplicateProfileError(String errorMessage, String serverErrorMessage)
        throws DublicateProfileRecordException {

        int idStart = serverErrorMessage.indexOf(DUBLICATE_PROFILE_ID_PREFIX) + DUBLICATE_PROFILE_ID_PREFIX.length();
        int idEnd = serverErrorMessage.indexOf(' ', idStart);
        String profileId = serverErrorMessage.substring(idStart, idEnd);

        throw new DublicateProfileRecordException(profileId, errorMessage);
    }

    /*public static void main(String args[]) throws Exception {
        AuthorizeNetCIMApi api= new AuthorizeNetCIMApi("", "",
                "none",true, 10);
        String XML = api.buildGetCustomerProfileRequest("");
        System.out.println("REQUEST\n" + XML);
        String HTTPResponse = api.sendViaXML(XML);
        System.out.println("RESPONSE\n"+HTTPResponse);
    }*/

}
