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

package com.sapienter.jbilling.server.process;

import com.sapienter.jbilling.server.order.OrderProcessWS;
import com.sapienter.jbilling.server.security.WSSecured;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * BillingProcessWS
 *
 * @author Brian Cowdery
 * @since 25-10-2010
 */
public class BillingProcessWS implements WSSecured, Serializable {
    
    // PaperInvoiceBatchDTO excluded from WS

    private Integer id;
    private Integer entityId;
    private Integer periodUnitId;
    private Integer periodValue;
    private Date billingDate;
    private Date billingDateEnd;
    private Integer isReview;
    private Integer retries;
    private Integer retriesToDo;
    private List<Integer> invoiceIds = new ArrayList<Integer>(0);
    private List<OrderProcessWS> orderProcesses = new ArrayList<OrderProcessWS>(0);
    private List<ProcessRunWS> processRuns = new ArrayList<ProcessRunWS>(0);

    // todo: extensions of ProcessRunDTO and ProcessRunTotalDTO, may not be necessary.
    // List<BillingProcessRunDTOEx> runs
    // BillingProcessRunDTOEx grandTotal

    public BillingProcessWS() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public Integer getPeriodUnitId() {
        return periodUnitId;
    }

    public void setPeriodUnitId(Integer periodUnitId) {
        this.periodUnitId = periodUnitId;
    }

    public Integer getPeriodValue() {
        return periodValue;
    }

    public void setPeriodValue(Integer periodValue) {
        this.periodValue = periodValue;
    }

    public Date getBillingDate() {
        return billingDate;
    }

    public void setBillingDate(Date billingDate) {
        this.billingDate = billingDate;
    }

    public Date getBillingDateEnd() {
        return billingDateEnd;
    }

    public void setBillingDateEnd(Date billingDateEnd) {
        this.billingDateEnd = billingDateEnd;
    }

    public Integer getReview() {
        return isReview;
    }

    public void setReview(Integer review) {
        isReview = review;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public Integer getRetriesToDo() {
        return retriesToDo;
    }

    public void setRetriesToDo(Integer retriesToDo) {
        this.retriesToDo = retriesToDo;
    }

    public List<Integer> getInvoiceIds() {
        return invoiceIds;
    }

    public void setInvoiceIds(List<Integer> invoiceIds) {
        this.invoiceIds = invoiceIds;
    }

    public List<OrderProcessWS> getOrderProcesses() {
        return orderProcesses;
    }

    public void setOrderProcesses(List<OrderProcessWS> orderProcesses) {
        this.orderProcesses = orderProcesses;
    }

    public List<ProcessRunWS> getProcessRuns() {
        return processRuns;
    }

    public void setProcessRuns(List<ProcessRunWS> processRuns) {
        this.processRuns = processRuns;
    }

    public Integer getOwningEntityId() {
        return getEntityId();
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningEntityId()}
     * @return null
     */
    public Integer getOwningUserId() {
        return null;
    }

    @Override
    public String toString() {
        return "BillingProcessWS{"
               + "id=" + id
               + ", entityId=" + entityId
               + ", periodUnitId=" + periodUnitId
               + ", periodValue=" + periodValue
               + ", billingDate=" + billingDate
               + ", billingDateEnd=" + billingDateEnd
               + ", isReview=" + isReview
               + ", retries=" + retries
               + ", retriesToDo=" + retriesToDo
               + ", invoiceIds=" + (invoiceIds != null ? invoiceIds.size() : null)
               + ", orderProcesses=" + (orderProcesses != null ? orderProcesses.size() : null)
               + ", processRuns=" + (processRuns != null ? processRuns.size() : null)
               + '}';
    }
}
