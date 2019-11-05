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

import java.util.Collections;
import java.util.List;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;

import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

import java.util.ArrayList;

public class PaymentDTOEx extends PaymentDTO {

    private Integer userId = null;
    private String method = null;
    private List<Integer> invoiceIds = null;
    private List paymentMaps = null;
    private PaymentDTOEx payment = null; // for refunds
    private String resultStr = null;
    private Integer payoutId = null;

    // now we only support one of these
    private PaymentAuthorizationDTO authorization = null; // useful in refuds
    // instruments using which user specifc, not linked to payments
    private List<PaymentInformationDTO> paymentInstruments = new ArrayList<PaymentInformationDTO>(0);
    
    // current instrument with which this payment will be process
    private PaymentInformationDTO instrument = null;

    public PaymentDTOEx(PaymentDTO dto) {
        if (dto.getBaseUser() != null)
            userId = dto.getBaseUser().getId();

        setId(dto.getId());
        setCurrency(dto.getCurrency());
        setAmount(dto.getAmount());
        setBalance(dto.getBalance());
        setAttempt(dto.getAttempt());
        setCreditCard(dto.getCreditCard());
        
        setDeleted(dto.getDeleted());
        setIsPreauth(dto.getIsPreauth());
        setIsRefund(dto.getIsRefund());

        setPaymentDate(dto.getPaymentDate());
        setCreateDatetime(dto.getCreateDatetime());
        setUpdateDatetime(dto.getUpdateDatetime());

        if (dto.getPaymentMethod() != null) {
            setPaymentMethod(dto.getPaymentMethod());
        }

        if (dto.getPaymentResult() != null) {
            setPaymentResult(dto.getPaymentResult());
        }
        setPaymentPeriod(dto.getPaymentPeriod());
        setPaymentNotes(dto.getPaymentNotes());
        setMetaFields(dto.getMetaFields());
        
        //for refunds
        setPayment(dto.getPayment());
        
        // payment instruments
        if(dto.getPaymentInstrumentsInfo() != null) {
        	setPaymentInstrumentsInfo(dto.getPaymentInstrumentsInfo());
        }
        
        invoiceIds = new ArrayList<Integer>();
        paymentMaps = new ArrayList();
    }

    public PaymentDTOEx(PaymentWS dto) {

        setId(dto.getId());
        setAmount(dto.getAmountAsDecimal());
        setAttempt(dto.getAttempt());
        setBalance(dto.getBalanceAsDecimal());
        setCreateDatetime(dto.getCreateDatetime());
        setCurrency(new CurrencyDTO(dto.getCurrencyId()));
        setDeleted(dto.getDeleted());
        setIsPreauth(dto.getIsPreauth());
        setIsRefund(dto.getIsRefund());
        setPaymentDate(dto.getPaymentDate());
        setUpdateDatetime(dto.getUpdateDatetime());
        setPaymentPeriod(dto.getPaymentPeriod());
        setPaymentNotes(dto.getPaymentNotes());

        if (dto.getMethodId() != null)
            setPaymentMethod(new PaymentMethodDTO(dto.getMethodId()));

        if (dto.getResultId() != null)
            setPaymentResult(new PaymentResultDAS().find(dto.getResultId()));

        userId = dto.getUserId();

        method = dto.getMethod();

        Integer entityId = new UserBL().getEntityId(userId);
        // set payment instruments
        for(PaymentInformationWS paymentInstrument : dto.getPaymentInstruments()) {
    		this.getPaymentInstruments().add(new PaymentInformationDTO(paymentInstrument, entityId));
        }
        
        invoiceIds = new ArrayList<Integer>();
        paymentMaps = new ArrayList();

        if (dto.getInvoiceIds() != null) {
            Collections.addAll(invoiceIds, dto.getInvoiceIds());
        }

        if (dto.getPaymentId() != null) {
            payment = new PaymentDTOEx();
            payment.setId(dto.getPaymentId());
        } else {
            payment = null;
        }

        authorization = new PaymentAuthorizationDAS().find(dto.getAuthorizationId());
        MetaFieldBL.fillMetaFieldsFromWS(entityId, 
        		this, dto.getMetaFields());
    }
    
    /**
     *
     */
    public PaymentDTOEx() {
        super();
        invoiceIds = new ArrayList<Integer>();
        paymentMaps = new ArrayList();
    }

    /**
     * @param id
     * @param amount
     * @param createDateTime
     * @param attempt
     * @param deleted
     * @param methodId
     */
//    public PaymentDTOEx(Integer id, BigDecimal amount, Date createDateTime,
//            Date updateDateTime,
//            Date paymentDate, Integer attempt, Integer deleted,
//            Integer methodId, Integer resultId, Integer isRefund,
//            Integer isPreauth, Integer currencyId, BigDecimal balance) {
//        super(id, amount, balance, createDateTime, updateDateTime,
//                paymentDate, attempt, deleted, methodId, resultId, isRefund,
//                isPreauth, currencyId, null, null);
//        invoiceIds = new ArrayList<Integer>();
//        paymentMaps = new ArrayList();
//    }

    /**
     * @param otherValue
     */
//    public PaymentDTOEx(PaymentDTO otherValue) {
//        super(otherValue);
//        invoiceIds = new ArrayList<Integer>();
//        paymentMaps = new ArrayList();
//    }

    public boolean validate() {
        boolean retValue = true;

        // check some mandatory fields
        if (getPaymentMethod() == null || getPaymentResult() == null) {
            retValue = false;
        }

        return retValue;
    }
    
    public String toString() {

        StringBuffer maps = new StringBuffer();
        if (paymentMaps != null) {
            for (int f = 0; f < paymentMaps.size(); f++) {
                maps.append(paymentMaps.get(f).toString());
                maps.append(" - ");
            }
        }

        return super.toString() + " payment maps:" + maps.toString() + "payment for refund "+ payment;
    }
    /**
     * @return
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * @param integer
     */
    public void setUserId(Integer integer) {
        userId = integer;
    }

    /**
     * @return
     */
    public String getMethod() {
        return method;
    }

    /**
     * @param string
     */
    public void setMethod(String string) {
        method = string;
    }


    /**
     * @return
     */
    public List<Integer> getInvoiceIds() {
        return invoiceIds;
    }

    /**
     * @param vector
     */
    public void setInvoiceIds(List vector) {
        invoiceIds = vector;
    }

    /**
     * @return
     */
    public PaymentDTOEx getPayment() {
        return payment;
    }

    /**
     * @param ex
     */
    public void setPayment(PaymentDTOEx ex) {
        payment = ex;
    }

    /**
     * @return
     */
    public PaymentAuthorizationDTO getAuthorization() {
        new FormatLogger(PaymentDTOEx.class).debug("Returning " +
                authorization + " for payemnt " + getId());
        return authorization;
    }

    /**
     * @param authorizationDTO
     */
    public void setAuthorization(PaymentAuthorizationDTO authorizationDTO) {
        authorization = authorizationDTO;
    }

    /**
     * @return
     */
    public String getResultStr() {
        return resultStr;
    }

    /**
     * @param resultStr
     */
    public void setResultStr(String resultStr) {
        this.resultStr = resultStr;
    }

    /**
     * @return
     */
    public Integer getPayoutId() {
        return payoutId;
    }

    /**
     * @param payoutId
     */
    public void setPayoutId(Integer payoutId) {
        this.payoutId = payoutId;
    }

    public List getPaymentMaps() {
        new FormatLogger(PaymentDTOEx.class).debug("Returning " +
                paymentMaps.size() + " elements in the map");
        return paymentMaps;
    }

    public void addPaymentMap(PaymentInvoiceMapDTOEx map) {
        new FormatLogger(PaymentDTOEx.class).debug("Adding map to the vector ");
        paymentMaps.add(map);
    }

	public List<PaymentInformationDTO> getPaymentInstruments() {
		return paymentInstruments;
	}

	public void setPaymentInstruments(List<PaymentInformationDTO> paymentInstruments) {
		this.paymentInstruments = paymentInstruments;
	}

	public PaymentInformationDTO getInstrument() {
		return instrument;
	}

	public void setInstrument(PaymentInformationDTO instrument) {
		this.instrument = instrument;
	}
}
