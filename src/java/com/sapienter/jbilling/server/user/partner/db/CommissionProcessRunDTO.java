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
package com.sapienter.jbilling.server.user.partner.db;

import com.sapienter.jbilling.server.user.db.CompanyDTO;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@TableGenerator(
        name="partner_commission_process_run_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="partner_commission_process_run",
        allocationSize=10
)
@Table(name="partner_commission_process_run")
public class CommissionProcessRunDTO {
    private int id;
    private Date runDate;
    private Date periodStart;
    private Date periodEnd;
    private List<CommissionDTO> commissions;
    private CompanyDTO entity;

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="partner_commission_process_run_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @Column(name="run_date", length=13)
    public Date getRunDate () {
        return runDate;
    }

    public void setRunDate (Date runDate) {
        this.runDate = runDate;
    }

    @Column(name="period_start", length=13)
    public Date getPeriodStart () {
        return periodStart;
    }

    public void setPeriodStart (Date periodStart) {
        this.periodStart = periodStart;
    }

    @Column(name="period_end", length=13)
    public Date getPeriodEnd () {
        return periodEnd;
    }

    public void setPeriodEnd (Date periodEnd) {
        this.periodEnd = periodEnd;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "commissionProcessRun")
    public List<CommissionDTO> getCommissions () {
        return commissions;
    }

    public void setCommissions (List<CommissionDTO> commissions) {
        this.commissions = commissions;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity () {
        return entity;
    }

    public void setEntity (CompanyDTO entity) {
        this.entity = entity;
    }
}
