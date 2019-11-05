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

public class CommissionProcessRunWS implements WSSecured, Serializable {
    private int id;
    private Date runDate;
    private Date periodStart;
    private Date periodEnd;
    private Integer entityId;

    public CommissionProcessRunWS () {
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

    public Date getRunDate () {
        return runDate;
    }

    public void setRunDate (Date runDate) {
        this.runDate = runDate;
    }

    public Date getPeriodStart () {
        return periodStart;
    }

    public void setPeriodStart (Date periodStart) {
        this.periodStart = periodStart;
    }

    public Date getPeriodEnd () {
        return periodEnd;
    }

    public void setPeriodEnd (Date periodEnd) {
        this.periodEnd = periodEnd;
    }

    @Override
    public String toString () {
        return "CommissionWS{"
                + "id=" + id
                + ", entityId=" + entityId
                + ", runDate=" + runDate
                + ", periodStart=" + periodStart
                + ", periodEnd=" + periodEnd
                + '}';

    }

    public Integer getOwningEntityId () {
        return entityId;
    }

    public Integer getOwningUserId () {
        return null;
    }
}
