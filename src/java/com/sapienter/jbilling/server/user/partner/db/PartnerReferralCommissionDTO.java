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

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@TableGenerator(
        name="partner_referral_commission_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="partner_referral_commission",
        allocationSize=10
)
@Table(name="partner_referral_commission")
public class PartnerReferralCommissionDTO {
    private int id;
    private PartnerDTO referral;
    private PartnerDTO referrer;
    private Date startDate;
    private Date endDate;
    private BigDecimal percentage;

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="partner_referral_commission_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="referral_id", nullable=false)
    public PartnerDTO getReferral () {
        return referral;
    }

    public void setReferral (PartnerDTO partner) {
        this.referral = partner;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="referrer_id", nullable=false)
    public PartnerDTO getReferrer () {
        return referrer;
    }

    public void setReferrer (PartnerDTO partner) {
        this.referrer = partner;
    }

    @Column(name="start_date", length=13)
    public Date getStartDate () {
        return startDate;
    }

    public void setStartDate (Date startDate) {
        this.startDate = startDate;
    }

    @Column(name="end_date", length=13)
    public Date getEndDate () {
        return endDate;
    }

    public void setEndDate (Date endDate) {
        this.endDate = endDate;
    }

    @Column(name="percentage", nullable=false, precision=17, scale=17)
    public BigDecimal getPercentage () {
        return percentage;
    }

    public void setPercentage (BigDecimal percentage) {
        this.percentage = percentage;
    }

}
