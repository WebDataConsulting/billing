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

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@TableGenerator(
        name="invoice_commission_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="invoice_commission",
        allocationSize=10
)
@Table(name="invoice_commission")
public class InvoiceCommissionDTO {
    private int id;
    private CommissionProcessRunDTO commissionProcessRun;
    private InvoiceDTO invoice;
    private PartnerDTO partner;
    private BigDecimal standardAmount = BigDecimal.ZERO;
    private BigDecimal masterAmount = BigDecimal.ZERO;
    private BigDecimal exceptionAmount = BigDecimal.ZERO;
    private BigDecimal referralAmount = BigDecimal.ZERO;
    private PartnerDTO referralPartner;
    private CommissionDTO commission;


    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="invoice_commission_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="partner_id", nullable=false)
    public PartnerDTO getPartner () {
        return partner;
    }

    public void setPartner (PartnerDTO partner) {
        this.partner = partner;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    public InvoiceDTO getInvoice () {
        return invoice;
    }

    public void setInvoice (InvoiceDTO invoice) {
        this.invoice = invoice;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_process_run_id")
    public CommissionProcessRunDTO getCommissionProcessRun () {
        return commissionProcessRun;
    }

    public void setCommissionProcessRun (CommissionProcessRunDTO commissionProcessRun) {
        this.commissionProcessRun = commissionProcessRun;
    }

    @Column(name="standard_amount", nullable=false, precision=17, scale=17)
    public BigDecimal getStandardAmount () {
        return standardAmount;
    }

    public void setStandardAmount (BigDecimal standardAmount) {
        this.standardAmount = standardAmount;
    }

    @Column(name="master_amount", nullable=false, precision=17, scale=17)
    public BigDecimal getMasterAmount () {
        return masterAmount;
    }

    public void setMasterAmount (BigDecimal masterAmount) {
        this.masterAmount = masterAmount;
    }

    @Column(name="exception_amount", nullable=false, precision=17, scale=17)
    public BigDecimal getExceptionAmount () {
        return exceptionAmount;
    }

    public void setExceptionAmount (BigDecimal exceptionAmount) {
        this.exceptionAmount = exceptionAmount;
    }

    @Column(name="referral_amount", nullable=false, precision=17, scale=17)
    public BigDecimal getReferralAmount () {
        return referralAmount;
    }

    public void setReferralAmount (BigDecimal referralAmount) {
        this.referralAmount = referralAmount;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="referral_partner_id", nullable=true)
    public PartnerDTO getReferralPartner () {
        return referralPartner;
    }

    public void setReferralPartner (PartnerDTO referralPartner) {
        this.referralPartner = referralPartner;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id")
    public CommissionDTO getCommission () {
        return commission;
    }

    public void setCommission (CommissionDTO commission) {
        this.commission = commission;
    }
}
