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

package com.sapienter.jbilling.server.user.partner;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.sapienter.jbilling.common.Util;

/**
 * PartnerPayoutWS
 *
 * @author Brian Cowdery
 * @since 25-10-2010
 */
public class PartnerPayoutWS implements Serializable {

    private int id;
    private Integer partnerId;
    private Integer paymentId;
    private Date startingDate;
    private Date endingDate;
    private String paymentsAmount;
    private String refundsAmount;
    private String balanceLeft;

    public PartnerPayoutWS() {
    }

   
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Integer partnerId) {
        this.partnerId = partnerId;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public Date getStartingDate() {
        return startingDate;
    }

    public void setStartingDate(Date startingDate) {
        this.startingDate = startingDate;
    }

    public Date getEndingDate() {
        return endingDate;
    }

    public void setEndingDate(Date endingDate) {
        this.endingDate = endingDate;
    }

    public String getPaymentsAmount() {
        return paymentsAmount;
    }

    public BigDecimal getPaymentsAmountAsDecimal() {
        return Util.string2decimal(paymentsAmount);
    }

    public void setPaymentsAmount(String paymentsAmount) {
        this.paymentsAmount = paymentsAmount;
    }

    public void setPaymentsAmount(BigDecimal paymentsAmount) {
        this.paymentsAmount = (paymentsAmount != null ? paymentsAmount.toString() : null);
    }

    public String getRefundsAmount() {
        return refundsAmount;
    }

    public BigDecimal getRefundsAmountAsDecimal() {
        return Util.string2decimal(refundsAmount);
    }

    public void setRefundsAmount(String refundsAmount) {
        this.refundsAmount = refundsAmount;
    }

    public void setRefundsAmount(BigDecimal refundsAmount) {
        this.refundsAmount = (refundsAmount != null ? refundsAmount.toString() : null);
    }

    public String getBalanceLeft() {
        return balanceLeft;
    }

    public BigDecimal getBalanceLeftAsDecimal() {
        return Util.string2decimal(balanceLeft);
    }

    public void setBalanceLeft(String balanceLeft) {
        this.balanceLeft = balanceLeft;
    }

    public void setBalanceLeft(BigDecimal balanceLeft) {
        this.balanceLeft = (balanceLeft != null ? balanceLeft.toString() : null);
    }

    @Override
    public String toString() {
        return "PartnerPayoutWS{"
               + "id=" + id
               + ", partnerId=" + partnerId
               + ", paymentId=" + paymentId
               + ", startingDate=" + startingDate
               + ", endingDate=" + endingDate
               + ", paymentsAmount=" + paymentsAmount
               + ", refundsAmount=" + refundsAmount
               + ", balanceLeft=" + balanceLeft
               + '}';
    }
}

