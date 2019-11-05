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

import com.sapienter.jbilling.server.order.validator.DateBetween;
import com.sapienter.jbilling.server.util.ServerConstants;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;


/**
 * BillingProcessConfigurationWS
 *
 * @author Brian Cowdery
 * @since 21-10-2010
 */
public class BillingProcessConfigurationWS implements Serializable {

    private int id;
    private Integer periodUnitId;
    private Integer entityId;
    @DateBetween(start = "01/01/1901", end = "12/31/9999")
    @NotNull(message = "validation.error.is.required")
    private Date nextRunDate;
    private Integer generateReport;
    private Integer retries;
    private Integer daysForRetry;
    private Integer daysForReport;
    private int reviewStatus;
    private int dueDateUnitId;
    private int dueDateValue;
    private Integer dfFm;
    private Integer onlyRecurring;
    private Integer invoiceDateProcess;
    @Min(value = 1, message = "validation.error.min,1")
    private int maximumPeriods;
    private int autoPaymentApplication;
    private boolean lastDayOfMonth = false;
    private String proratingType;
    
    public BillingProcessConfigurationWS() {
    }

	public BillingProcessConfigurationWS(BillingProcessConfigurationWS ws) {
		this.id = ws.getId();
		this.periodUnitId = ws.getPeriodUnitId();
		this.entityId = ws.getEntityId();
		this.nextRunDate = ws.getNextRunDate();
		this.generateReport = ws.getGenerateReport();
		this.retries = ws.getRetries();
		this.daysForRetry = ws.getDaysForRetry();
		this.daysForReport = ws.getDaysForReport();
		this.reviewStatus = ws.getReviewStatus();
		this.dueDateUnitId = ws.getDueDateUnitId();
		this.dueDateValue = ws.getDueDateValue();
		this.dfFm = ws.getDfFm();
		this.onlyRecurring = ws.getOnlyRecurring();
		this.invoiceDateProcess = ws.getInvoiceDateProcess();
		this.maximumPeriods = ws.getMaximumPeriods();
		this.autoPaymentApplication = ws.getAutoPaymentApplication();
		this.lastDayOfMonth = ws.isLastDayOfMonth();
		this.proratingType = ws.getProratingType();
	}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public Integer getPeriodUnitId() {
        return periodUnitId;
    }

    public void setPeriodUnitId(Integer periodUnitId) {
        this.periodUnitId = periodUnitId;
    }
    
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public Date getNextRunDate() {
        return nextRunDate;
    }

    public void setNextRunDate(Date nextRunDate) {
        this.nextRunDate = nextRunDate;
    }

    public Integer getGenerateReport() {
        return generateReport;
    }

    public void setGenerateReport(Integer generateReport) {
        this.generateReport = generateReport;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public Integer getDaysForRetry() {
        return daysForRetry;
    }

    public void setDaysForRetry(Integer daysForRetry) {
        this.daysForRetry = daysForRetry;
    }

    public Integer getDaysForReport() {
        return daysForReport;
    }

    public void setDaysForReport(Integer daysForReport) {
        this.daysForReport = daysForReport;
    }

    public int getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(int reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public int getDueDateUnitId() {
        return dueDateUnitId;
    }

    public void setDueDateUnitId(int dueDateUnitId) {
        this.dueDateUnitId = dueDateUnitId;
    }

    public int getDueDateValue() {
        return dueDateValue;
    }

    public void setDueDateValue(int dueDateValue) {
        this.dueDateValue = dueDateValue;
    }

    public Integer getDfFm() {
        return dfFm;
    }

    public void setDfFm(Integer dfFm) {
        this.dfFm = dfFm;
    }

    public Integer getOnlyRecurring() {
        return onlyRecurring;
    }

    public void setOnlyRecurring(Integer onlyRecurring) {
        this.onlyRecurring = onlyRecurring;
    }

    public Integer getInvoiceDateProcess() {
        return invoiceDateProcess;
    }

    public void setInvoiceDateProcess(Integer invoiceDateProcess) {
        this.invoiceDateProcess = invoiceDateProcess;
    }
    
    public int getMaximumPeriods() {
        return maximumPeriods;
    }

    public void setMaximumPeriods(int maximumPeriods) {
        this.maximumPeriods = maximumPeriods;
    }

    public int getAutoPaymentApplication() {
        return autoPaymentApplication;
    }

    public void setAutoPaymentApplication(int autoPaymentApplication) {
        this.autoPaymentApplication = autoPaymentApplication;
    }

    public boolean isLastDayOfMonth() {
		return lastDayOfMonth;
	}

	public void setLastDayOfMonth(boolean lastDayOfMonth) {
		this.lastDayOfMonth = lastDayOfMonth;
	}

	public String getProratingType() {
		return proratingType;
	}

	public void setProratingType(String proratingType) {
		this.proratingType = proratingType;
	}

	@Override
    public String toString() {
        return "BillingProcessConfigurationWS{"
               + "id=" + id
               + ", entityId=" + entityId
               + ", nextRunDate=" + nextRunDate
               + ", generateReport=" + generateReport
               + ", retries=" + retries
               + ", daysForRetry=" + daysForRetry
               + ", daysForReport=" + daysForReport
               + ", reviewStatus=" + reviewStatus
               + ", dueDateUnitId=" + dueDateUnitId
               + ", dueDateValue=" + dueDateValue
               + ", dfFm=" + dfFm
               + ", onlyRecurring=" + onlyRecurring
               + ", invoiceDateProcess=" + invoiceDateProcess
               + ", maximumPeriods=" + maximumPeriods
               + ", autoPaymentApplication=" + autoPaymentApplication
               + ", periodUnitId=" + periodUnitId
               + ", lastDayOfMonth=" + lastDayOfMonth
               + ", proratingType=" + proratingType
               + '}';
    }
}
