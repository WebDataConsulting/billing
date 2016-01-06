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


import com.sapienter.jbilling.server.security.WSSecured;

import java.io.Serializable;
import java.util.Date;

public class CommissionProcessConfigurationWS implements WSSecured, Serializable {
    private int id;
    private Integer entityId;
    private Date nextRunDate;
    private Integer periodUnitId;
    private Integer periodValue;

    public CommissionProcessConfigurationWS () {
    }

    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    public Integer getEntityId () {
        return entityId;
    }

    public void setEntityId (Integer entityId) {
        this.entityId = entityId;
    }

    public Date getNextRunDate () {
        return nextRunDate;
    }

    public void setNextRunDate (Date nextRunDate) {
        this.nextRunDate = nextRunDate;
    }

    public Integer getPeriodUnitId () {
        return periodUnitId;
    }

    public void setPeriodUnitId (Integer periodUnitId) {
        this.periodUnitId = periodUnitId;
    }

    public Integer getPeriodValue () {
        return periodValue;
    }

    public void setPeriodValue (Integer periodValue) {
        this.periodValue = periodValue;
    }

    

    @Override
    public String toString () {
        return "CommissionWS{"
                + "id=" + id
                + ", entityId=" + entityId
                + ", nextRunDate=" + nextRunDate
                + ", periodUnitId=" + periodUnitId
                + '}';

    }

    public Integer getOwningEntityId () {
        return entityId;
    }

    public Integer getOwningUserId () {
        return null;
    }
}
