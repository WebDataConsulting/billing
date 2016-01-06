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


import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.security.WSSecured;

import java.io.Serializable;
import java.math.BigDecimal;

public class CommissionWS implements WSSecured, Serializable {
    private int id;
    
    private String amount;
    private String type;
    private Integer partnerId;
    private Integer commissionProcessRunId;
    private Integer currencyId;
    private Integer owningEntityId;

    public CommissionWS () {
    }

   
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    public String getAmount () {
        return amount;
    }

    public BigDecimal getAmountAsDecimal() {
        return Util.string2decimal(amount);
    }

    public void setAmount (String amount) {
        this.amount = amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = (amount != null ? amount.toString() : null);
    }

    public String getType () {
        return type;
    }

    public void setType (String type) {
        this.type = type;
    }

    public Integer getPartnerId () {
        return partnerId;
    }

    public void setPartnerId (Integer partnerId) {
        this.partnerId = partnerId;

    }

    public Integer getCommissionProcessRunId () {
        return commissionProcessRunId;
    }

    public void setCommissionProcessRunId (Integer commissionProcessRunId) {
        this.commissionProcessRunId = commissionProcessRunId;
    }

    @Override
    public String toString () {
        return "CommissionWS{"
                + "id=" + id
                + ", amount=" + amount
                + ", type=" + type
                + ", partnerId=" + partnerId
                + ", commissionProcessRunId=" + commissionProcessRunId
                + ", currencyId=" + currencyId
                + '}';

    }

    public Integer getOwningEntityId () {
       return owningEntityId;
    }
    
    public void setOwningEntityId(Integer owningEntityId){
    	this.owningEntityId = owningEntityId;
    }

    public Integer getOwningUserId () {
        return null;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    
}
