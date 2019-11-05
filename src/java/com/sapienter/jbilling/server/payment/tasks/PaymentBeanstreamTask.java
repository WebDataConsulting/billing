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
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.pluggableTask.PaymentTask;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.util.ServerConstants;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

;

public class PaymentBeanstreamTask extends PaymentTaskWithTimeout implements
        PaymentTask {

    // Required parameters
    private static final String PARAMATER_MERCHANT_ID = "merchant_id";

    private static final String PARAMATER_USERNAME = "username";

    private static final String PARAMATER_PASSWORD = "password";

    // Optional parameters
    private static final String PARAMATER_CAV_ENABLED = "cav_enabled";

    private static final String PARAMATER_CAV_PASSCODE = "cav_passcode";

    private static final String PARAMATER_CAV_VERSION = "cav_version";

    private static final String PARAMATER_VBV_ENABLED = "vbv_enabled";

    private static final String PARAMATER_SC_ENABLED = "sc_enabled";

    private static final String BeanstreamURL = "https://www.beanstream.com/scripts/process_transaction.asp";

    private static final FormatLogger LOG = new FormatLogger(PaymentBeanstreamTask.class);

    public boolean process(PaymentDTOEx paymentInfo)
            throws PluggableTaskException {
    	PaymentInformationBL piBl = new PaymentInformationBL();
        try {

            if (!piBl.isCreditCard(paymentInfo.getInstrument())) {

                String error = "Credit card not present in payment";
                LOG.error(error);
                throw new TaskException(error);
            }

            if (piBl.isACH(paymentInfo.getInstrument())) {

                String error = "ACH not supported by Beanstream processing API";
                LOG.error(error);
                throw new TaskException(error);
            }

            if (BigDecimal.ZERO.compareTo(paymentInfo.getAmount()) > 0 && paymentInfo.getIsRefund() == 0) {
                String error = "Credits not linked to a previous transaction " +
                        " (refund) not supported by Beanstream processing API";
                LOG.error(error);
                // note: refunds haven't actually been coded for, either
                throw new TaskException(error);
            }

            String POSTString = this.getPOSTString(paymentInfo, "P", null);
            String HTTPResponse = doPost(POSTString, paymentInfo);
            PaymentAuthorizationDTO paymentDTO = new BeanstreamResponseDTO()
                    .parseResponse(HTTPResponse);

            if (paymentDTO.getCode1().equals("1")) {

                paymentInfo.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_OK));
                paymentInfo.setAuthorization(paymentDTO);
                PaymentAuthorizationBL bl = new PaymentAuthorizationBL();
                bl.create(paymentDTO, paymentInfo.getId());
                return false;
            } else {

                paymentInfo.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_FAIL));
                paymentInfo.setAuthorization(paymentDTO);
                PaymentAuthorizationBL bl = new PaymentAuthorizationBL();
                bl.create(paymentDTO, paymentInfo.getId());
                return false;
            }
        } catch (Exception e) {

            LOG.error(e);
            paymentInfo.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_UNAVAILABLE));
            return true;
        }
    }

    public boolean preAuth(PaymentDTOEx paymentInfo)
            throws PluggableTaskException {

        try {
            String POSTString = this.getPOSTString(paymentInfo, "PA", null);
            String HTTPResponse = doPost(POSTString, paymentInfo);
            PaymentAuthorizationDTO paymentDTO = new BeanstreamResponseDTO()
                    .parseResponse(HTTPResponse);

            if (paymentDTO.getCode1().equals("1")) {

                paymentInfo.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_OK));
                paymentInfo.setAuthorization(paymentDTO);
                PaymentAuthorizationBL bl = new PaymentAuthorizationBL();
                bl.create(paymentDTO, paymentInfo.getId());
                return false;
            } else {

                paymentInfo.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_FAIL));
                paymentInfo.setAuthorization(paymentDTO);
                PaymentAuthorizationBL bl = new PaymentAuthorizationBL();
                bl.create(paymentDTO, paymentInfo.getId());
                return false;
            }
        } catch (Exception e) {

            paymentInfo.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_UNAVAILABLE));
            return true;
        }
    }

    public boolean confirmPreAuth(PaymentAuthorizationDTO paymentDTO,
            PaymentDTOEx paymentInfo) throws PluggableTaskException {

        try {
            String POSTString = this.getPOSTString(paymentInfo, "PAC",
                    paymentDTO.getTransactionId());
            String HTTPResponse = doPost(POSTString, paymentInfo);
            PaymentAuthorizationDTO newPaymentDTO = new BeanstreamResponseDTO()
                    .parseResponse(HTTPResponse);

            if (newPaymentDTO.getCode1().equals("1")) {

                paymentInfo.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_OK));
                paymentInfo.setAuthorization(newPaymentDTO);
                PaymentAuthorizationBL bl = new PaymentAuthorizationBL();
                bl.create(newPaymentDTO, paymentInfo.getId());
                return false;
            } else {

                paymentInfo.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_FAIL));
                paymentInfo.setAuthorization(newPaymentDTO);
                PaymentAuthorizationBL bl = new PaymentAuthorizationBL();
                bl.create(newPaymentDTO, paymentInfo.getId());
                return false;
            }
        } catch (Exception e) {

            paymentInfo.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_UNAVAILABLE));
            return true;
        }
    }

    public void failure(Integer userId, Integer retry) {
    }

    /**
     * Returns a string suitable for passing to Beanstream processing API
     * (taking into account optional configuration parameters)
     * 
     * @param paymentInfo
     * @return String
     * @throws PluggableTaskException
     */
    private String getPOSTString(PaymentDTOEx paymentInfo, String paymentType,
            String transactionId) throws PluggableTaskException {

        String merchantId = ensureGetParameter(PARAMATER_MERCHANT_ID);
        String username = ensureGetParameter(PARAMATER_USERNAME);
        String password = ensureGetParameter(PARAMATER_PASSWORD);

        String cav_enabled = getOptionalParameter(PARAMATER_CAV_ENABLED, "0");
        String cav_passcode = getOptionalParameter(PARAMATER_CAV_PASSCODE, null);
        String cav_version = getOptionalParameter(PARAMATER_CAV_VERSION, null);
        String vbv_enabled = getOptionalParameter(PARAMATER_VBV_ENABLED, null);
        String sc_enabled = getOptionalParameter(PARAMATER_SC_ENABLED, null);

        if (merchantId.length() != 9) {

            String error = "Invalid merchant_id for Beanstream payment processor";
            LOG.error(error);
            throw new PluggableTaskException(error);
        }
        	
        PaymentInformationBL piBl = new PaymentInformationBL();
        try {
            ContactBL contact = new ContactBL();
            contact.set(paymentInfo.getUserId());

            Calendar cal = Calendar.getInstance();
            cal.setTime(piBl.getDateMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.DATE));

            StringBuffer postVars = new StringBuffer("requestType=BACKEND&");
            postVars.append("merchant_id=").append(merchantId).append("&");
            postVars.append("username=").append(username).append("&");
            postVars.append("trnCardOwner=").append(piBl.getStringMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.TITLE)).append("&");
            postVars.append("trnCardNumber=").append(piBl.getStringMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.PAYMENT_CARD_NUMBER)).append("&");
            postVars.append("trnExpMonth=").append((cal.get(Calendar.MONTH) < 10) ? ("0" + cal
                    .get(Calendar.MONTH)) : cal.get(Calendar.MONTH)).append("&");
            postVars.append("trnExpYear=").append((Integer.toString(cal.get(Calendar.YEAR)))
                    .substring(2, 4)).append("&");
            postVars.append("trnOrderNumber=").append(paymentInfo.getId()).append("&");
            postVars.append("trnAmount=").append(paymentInfo.getAmount()).append("&");
            postVars.append("trnType=").append(paymentType).append("&");
            postVars.append("trnId=").append(getString(transactionId));
            postVars.append("ordName=").append(contact.getEntity().getFirstName()).append(" ").append(contact
                    .getEntity().getLastName()).append("&");
            postVars.append("ordEmailAddress=").append(getString(contact.getEntity().getEmail())).append("&");
            postVars.append("ordPhoneNumber=").append(getString(contact.getEntity().getPhoneNumber())).append("&");
            postVars.append("ordAddress1=").append(getString(contact.getEntity().getAddress1())).append("&");
            postVars.append("ordAddress2=").append(getString(contact.getEntity().getAddress2())).append("&");
            postVars.append("ordCity=").append(getString(contact.getEntity().getCity())).append("&");
            postVars.append("ordProvince=").append(getString(contact.getEntity().getStateProvince())).append("&");
            postVars.append("ordPostalCode=").append(getString(contact.getEntity().getPostalCode())).append("&");
            postVars.append("ordCountry=").append(getString(contact.getEntity().getCountryCode())).append("&");
            postVars.append("cavEnabled=").append(cav_enabled).append("&");
            postVars.append((cav_passcode != null) ? ("cavPassCode="
                    + cav_passcode + "&") : "");
            postVars.append("cavServiceVersion=").append((cav_version != null) ? cav_version : 0).append("&");
            postVars.append("vbvEnabled=").append((vbv_enabled != null && vbv_enabled.equals("true")) ? 1
                    : 0).append("&");
            postVars.append("scEnabled=").append((sc_enabled != null && sc_enabled.equals("true")) ? 1
                    : 0);

            //TODO: check this log also check values of postVars
            String maskedCCNumber = postVars.toString().replaceAll("trnCardNumber=[^&]*", "trnCardNumber=*****");
            LOG.debug("HTTP POST vars going to beanstream:  %s", maskedCCNumber);

            postVars.append("&").append("password=").append(password);

            return postVars.toString();
        } catch (Exception e) {

            throw new PluggableTaskException(e);
        }
    }

    /**
     * Performs an HTTPS POST request to the Beanstream payment processor
     * 
     * @param postVars
     *            String The HTTP POST formatted as a GET string
     * @return String
     * @throws PluggableTaskException
     */
    private String doPost(String postVars, PaymentDTOEx paymentInfo)
            throws PluggableTaskException {

        int ch;
        StringBuffer responseText = new StringBuffer();

        try {
            // Set the location of the Beanstream payment gateway
            URL url = new URL(BeanstreamURL);

            // Open the connection
            URLConnection conn = url.openConnection();

            // Set the connection timeout
            conn.setConnectTimeout(getTimeoutSeconds() * 1000);

            // Set the DoOutput flag to true because we intend
            // to use the URL connection for output
            conn.setDoOutput(true);

            // Send the transaction via HTTPS POST
            OutputStream ostream = conn.getOutputStream();
            ostream.write(postVars.getBytes());
            ostream.close();

            // Get the response from Beanstream
            InputStream istream = conn.getInputStream();
            while ((ch = istream.read()) != -1)
                responseText.append((char) ch);

            istream.close();
            LOG.debug("Beanstream responseText: %s", responseText);
            return responseText.toString();
        } catch (Exception e) {

            LOG.error(e);
            throw new PluggableTaskException(e);
        }
    }

    class BeanstreamResponseDTO {

        private String trnApproved;

        private String trnId;

        private String messageId;

        private String messageText;

        private String trnOrderNumber;

        private String authCode;

        private String hCode;

        private String errorType;

        private String errorFields;

        private String responseType;

        private String trnAmount;

        private String trnDate;

        private String avsProcessed;

        private String avsId;

        private String avsResult;

        private String avsAddrMatch;

        private String avsPostalMatch;

        private String avsMessage;

        private String rspCodeCav;

        private String rspCodeAdd2;

        private String rspCodeCredit1;

        private String rspCodeCredit2;

        private String rspCodeCredit3;

        private String rspCodeCredit4;

        private String rspCodeAddr1;

        private String rspCodeAddr2;

        private String rspCodeAddr3;

        private String rspCodeAddr4;

        private String rspCodeDob;

        private String rspCustomerDec;

        private String trnType;

        private String paymentMethod;

        private String ref1;

        private String ref2;

        private String ref3;

        private String ref4;

        private String ref5;

        public BeanstreamResponseDTO() {
        }

        public PaymentAuthorizationDTO parseResponse(String responseText)
                throws PluggableTaskException {

            try {
                BeanstreamResponseDTO responseDTO = new BeanstreamResponseDTO();
                String[] reply = responseText.split("&");

                for (int i = 0; i < reply.length; i++) {

                    String[] pair = reply[i].split("=");
                    Field field = responseDTO.getClass().getDeclaredField(
                            pair[0]);
                    field.set(responseDTO, (pair.length == 1) ? null : pair[1]);
                }

                PaymentAuthorizationDTO paymentDTO = new PaymentAuthorizationDTO();

                paymentDTO.setTransactionId(responseDTO.trnId);
                paymentDTO.setProcessor("Beanstream");
                paymentDTO.setApprovalCode(responseDTO.authCode);
                paymentDTO.setAvs(responseDTO.avsResult);
                paymentDTO.setMD5(responseDTO.hCode);
                // paymentDTO.setCardCode( ??? );
                paymentDTO.setCreateDate(Calendar.getInstance().getTime());
                paymentDTO.setResponseMessage(java.net.URLDecoder.decode(
                        responseDTO.messageText, "UTF-8"));
                paymentDTO.setCode1(responseDTO.trnApproved);

                return paymentDTO;
            } catch (Exception e) {
                throw new PluggableTaskException(e);
            }
        }
    }
}
