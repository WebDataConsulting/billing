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

import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;

import javax.persistence.*;
import java.util.Date;

@Entity
@TableGenerator(
        name="partner_commission_proc_config_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="partner_commission_proc_config",
        allocationSize=10
)
@Table(name="partner_commission_proc_config")
public class CommissionProcessConfigurationDTO {
    private int id;
    private CompanyDTO entity;
    private Date nextRunDate;
    private PeriodUnitDTO periodUnit;
    private int periodValue;

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="partner_commission_proc_config_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @Column(name="next_run_date", length=13)
    public Date getNextRunDate () {
        return nextRunDate;
    }

    public void setNextRunDate (Date nextRunDate) {
        this.nextRunDate = nextRunDate;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_unit_id")
    public PeriodUnitDTO getPeriodUnit () {
        return periodUnit;
    }

    public void setPeriodUnit (PeriodUnitDTO periodUnit) {
        this.periodUnit = periodUnit;
    }

    @Column(name = "period_value")
    public int getPeriodValue () {
        return periodValue;
    }

    public void setPeriodValue (int periodValue) {
        this.periodValue = periodValue;
    }
}
