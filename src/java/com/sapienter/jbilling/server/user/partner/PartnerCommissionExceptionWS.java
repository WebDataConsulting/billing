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
import java.util.Date;

/**
 * This class overrides the item's commission percentage for a particular partner
 * for a given period of time.
 */
public class PartnerCommissionExceptionWS implements WSSecured, Serializable {
    private int id;
    private Integer partnerId;
    private Date startDate;
    private Date endDate;
    private String percentage;
    private Integer itemId;
    private Integer userId;

    public PartnerCommissionExceptionWS() {
    }

    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    public Integer getPartnerId () {
        return partnerId;
    }

    public void setPartnerId (Integer partnerId) {
        this.partnerId = partnerId;
    }

    public Date getStartDate () {
        return startDate;
    }

    public void setStartDate (Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate () {
        return endDate;
    }

    public void setEndDate (Date endDate) {
        this.endDate = endDate;
    }

    public String getPercentage() {
        return percentage;
    }

    public BigDecimal getPercentageAsDecimal() {
        return Util.string2decimal(percentage);
    }

    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = (percentage != null ? percentage.toString() : null);
    }

    public Integer getItemId () {
        return itemId;
    }

    public void setItemId (Integer itemId) {
        this.itemId = itemId;
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningUserId()}
     *
     * @return null
     */
    public Integer getOwningEntityId () {
        return null;
    }

    public Integer getOwningUserId () {
       return userId;
    }

    public void setOwningUserId(Integer userId){
    	this.userId=userId;
    }
    
    @Override
    public String toString() {
        return "PartnerCommissionException{"
               + "id=" + id
               + ", partnerId=" + partnerId
               + ", startDate=" + startDate
               + ", endDate=" + endDate
               + ", percentage=" + percentage
               + ", itemId=" + itemId
               + '}';
    }

}
