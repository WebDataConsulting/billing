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

package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.validator.DateBetween;
import com.sapienter.jbilling.server.order.validator.DateRange;
import com.sapienter.jbilling.server.security.WSSecured;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author Emil
 */

@DateRange(start = "activeSince", end = "activeUntil", message = "validation.activeUntil.before.activeSince")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlRootElement
public class OrderWS implements WSSecured, Serializable {

    private static final long serialVersionUID = 20130704L;

    private Integer id;
    private Integer statusId;
    @NotNull(message = "validation.error.null.user.id")
    private Integer userId = null;
    @NotNull(message = "validation.error.null.currency")
    private Integer currencyId = null;
    @NotNull(message = "validation.error.null.billing.type")
    private Integer billingTypeId;
    @NotNull(message = "validation.error.null.period")
    private Integer period = null;
    private Date createDate;
    private Integer createdBy;
    @NotNull(message = "validation.error.null.activeSince")
    @DateBetween(start = "01/01/1901", end = "12/31/9999")
    private Date activeSince;
    private Date activeUntil;
    private Date nextBillableDay;
    private int deleted;
    private Integer notify;
    private Date lastNotified;
    private Integer notificationStep;
    private Integer dueDateUnitId;
    private Integer dueDateValue;
    private Integer dfFm;
    private Integer anticipatePeriods;
    private Integer ownInvoice;
    private String notes;
    private Integer notesInInvoice;
    @Valid
    private OrderLineWS orderLines[] = new OrderLineWS[0];
    private DiscountLineWS discountLines[] = null;
    private String pricingFields = null;
    private InvoiceWS[] generatedInvoices= null;
    @Valid
    private MetaFieldValueWS[] metaFields;
    @Valid
    private OrderWS parentOrder;
    @Valid
    private OrderWS[] childOrders;

    private String userCode;

    //Verifone - get PlanBundledItems with adjustedPrices
    private OrderLineWS[] planBundleItems = null;

    /**
     * Verifone - AdjustedTotal after applying order level discount.
     */
    private String adjustedTotal;
    
    // balances
    private String total;

    // textual descriptions
    private String statusStr = null;
    private String timeUnitStr = null;
    private String periodStr = null;
    private String billingTypeStr = null;
    private Boolean prorateFlag = Boolean.FALSE;
    private boolean isDisable= false;
    private Integer customerBillingCycleUnit;
    private Integer customerBillingCycleValue;
    private String proratingOption;

    // optlock (not necessary)
    private Integer versionNum;
    private String cancellationFeeType;
    private Integer cancellationFee;
    private Integer cancellationFeePercentage;
    private Integer cancellationMaximumFee;
    private Integer cancellationMinimumPeriod;

    private String objectId;
    private String freeUsageQuantity;
    
	public String getObjectId() {
        return objectId;
    }

    @XmlAttribute @XmlID
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    private Integer primaryOrderId; 
    private OrderStatusWS orderStatusWS;

    public OrderWS() {
        objectId = UUID.randomUUID().toString();
    }

    public OrderWS(Integer id, Integer billingTypeId, Integer notify, Date activeSince, Date activeUntil,
                   Date createDate, Date nextBillableDay, Integer createdBy,Integer statusId, OrderStatusWS orderStatusWS, Integer deleted,
                   Integer currencyId, Date lastNotified, Integer notifStep, Integer dueDateUnitId, Integer dueDateValue,
                   Integer anticipatePeriods, Integer dfFm, String notes, Integer notesInInvoice, Integer ownInvoice,
                   Integer period, Integer userId, Integer version,Boolean prorateFlag) {
        setId(id);
        setBillingTypeId(billingTypeId);
        setNotify(notify);
        setActiveSince(activeSince);
        setActiveUntil(activeUntil);
        setAnticipatePeriods(anticipatePeriods);
        setCreateDate(createDate);
        setStatusId(statusId);
        setNextBillableDay(nextBillableDay);
        setCreatedBy(createdBy);
        setOrderStatusWS(orderStatusWS);
        setDeleted(deleted.shortValue());
        setCurrencyId(currencyId);
        setLastNotified(lastNotified);
        setNotificationStep(notifStep);
        setDueDateUnitId(dueDateUnitId);
        setDueDateValue(dueDateValue);
        setDfFm(dfFm);
        setNotes(notes);
        setNotesInInvoice(notesInInvoice);
        setOwnInvoice(ownInvoice);
        setPeriod(period);
        setUserId(userId);
        setVersionNum(version);
        objectId = UUID.randomUUID().toString();
        setProrateFlag(prorateFlag);
    }

    
    public OrderStatusWS getOrderStatusWS() {
		return orderStatusWS;
	}

	public void setOrderStatusWS(OrderStatusWS orderStatusWS) {
		this.orderStatusWS = orderStatusWS;
	}

	public InvoiceWS[] getGeneratedInvoices() {
		return generatedInvoices;
	}

	public void setGeneratedInvoices(InvoiceWS[] generatedInvoices) {
		this.generatedInvoices = generatedInvoices;
	}

	public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    public Integer getBillingTypeId() {
        return billingTypeId;
    }

    public void setBillingTypeId(Integer billingTypeId) {
        this.billingTypeId = billingTypeId;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Date getActiveSince() {
        return activeSince;
    }

    public void setActiveSince(Date activeSince) {
        this.activeSince = activeSince;
    }

    public Date getActiveUntil() {
        return activeUntil;
    }

    public void setActiveUntil(Date activeUntil) {
        this.activeUntil = activeUntil;
    }

    public Date getNextBillableDay() {
        return nextBillableDay;
    }

    public void setNextBillableDay(Date nextBillableDay) {
        this.nextBillableDay = nextBillableDay;
    }

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    public Integer getNotify() {
        return notify;
    }

    public void setNotify(Integer notify) {
        this.notify = notify;
    }

    public Date getLastNotified() {
        return lastNotified;
    }

    public void setLastNotified(Date lastNotified) {
        this.lastNotified = lastNotified;
    }

    public Integer getNotificationStep() {
        return notificationStep;
    }

    public void setNotificationStep(Integer notificationStep) {
        this.notificationStep = notificationStep;
    }

    public Integer getDueDateUnitId() {
        return dueDateUnitId;
    }

    public void setDueDateUnitId(Integer dueDateUnitId) {
        this.dueDateUnitId = dueDateUnitId;
    }

    public Integer getDueDateValue() {
        return dueDateValue;
    }

    public void setDueDateValue(Integer dueDateValue) {
        this.dueDateValue = dueDateValue;
    }

    public Integer getDfFm() {
        return dfFm;
    }

    public void setDfFm(Integer dfFm) {
        this.dfFm = dfFm;
    }

    public Integer getAnticipatePeriods() {
        return anticipatePeriods;
    }

    public void setAnticipatePeriods(Integer anticipatePeriods) {
        this.anticipatePeriods = anticipatePeriods;
    }

    public Integer getOwnInvoice() {
        return ownInvoice;
    }

    public void setOwnInvoice(Integer ownInvoice) {
        this.ownInvoice = ownInvoice;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getNotesInInvoice() {
        return notesInInvoice;
    }

    public void setNotesInInvoice(Integer notesInInvoice) {
        this.notesInInvoice = notesInInvoice;
    }

    public OrderLineWS[] getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(OrderLineWS[] orderLines) {
        this.orderLines = orderLines;
    }
    
    public DiscountLineWS[] getDiscountLines() {
        return discountLines;
    }

    public void setDiscountLines(DiscountLineWS[] discountLines) {
        this.discountLines = discountLines;
    }
    
    public boolean hasDiscountLines() {
    	return this.getDiscountLines() != null && this.getDiscountLines().length > 0;
    }

    public String getPricingFields() {
        return pricingFields;
    }

    public void setPricingFields(String pricingFields) {
        this.pricingFields = pricingFields;
    }

    public String getTotal() {
        return total;
    }

    @XmlTransient
    public BigDecimal getTotalAsDecimal() {
        return Util.string2decimal(total);
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public void setTotal(BigDecimal total) {
        this.total = (total != null ? total.toString() : null);
    }

    public String getStatusStr() {
        return statusStr;
    }

    public void setStatusStr(String statusStr) {
        this.statusStr = statusStr;
    }

    public String getTimeUnitStr() {
        return timeUnitStr;
    }

    public void setTimeUnitStr(String timeUnitStr) {
        this.timeUnitStr = timeUnitStr;
    }

    public String getPeriodStr() {
        return periodStr;
    }

    public void setPeriodStr(String periodStr) {
        this.periodStr = periodStr;
    }

    public String getBillingTypeStr() {
        return billingTypeStr;
    }

    public void setBillingTypeStr(String billingTypeStr) {
        this.billingTypeStr = billingTypeStr;
    }

    public Boolean getProrateFlag() {
		return prorateFlag;
	}

	public void setProrateFlag(Boolean prorateFlag) {
		this.prorateFlag = prorateFlag;
	}

	public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    public Integer getPrimaryOrderId() {
		return primaryOrderId;
	}

	public void setPrimaryOrderId(Integer primaryOrderId) {
		this.primaryOrderId = primaryOrderId;
	}

    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    public OrderWS getParentOrder() {
        return parentOrder;
    }

    @XmlTransient
    public void setParentOrder(OrderWS parentOrder) {
        this.parentOrder = parentOrder;
    }

    /**
     * Used only in java <-> XML mapping to solve child order xml deserialization problem 
     * whn parent order not found
     */
    private String parentOrderId;

    @XmlAttribute(name = "parentOrderId")
    public String getParentOrderId () {
        return parentOrderId;
    }

    public void setParentOrderId (String orderId) {
        parentOrderId = orderId;
    }

    /**
     * magic method used by JAX-RS before marshalling java instance to xml
     * 
     * @param marshaller
     */
    @SuppressWarnings("unused")
    private void beforeMarshal(final Marshaller marshaller) {
        if (parentOrder != null) {
            setParentOrderId (parentOrder.getObjectId());
        }
    }

    /**
     * magic method used by JAX-RS before unmarshalling xml to java instance
     * 
     * @param marshaller
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (final Unmarshaller u, final Object parent) {
        if (parent instanceof OrderWS) {
            this.parentOrder = (OrderWS)parent;
        }
    }

    public OrderWS[] getChildOrders() {
        return childOrders;
    }

    @XmlElement(name="childOrder")
    public void setChildOrders(OrderWS[] childOrders) {
        this.childOrders = childOrders;
    }

    /**
     * Returns true if any line has an asset linked to it
     * @return
     */
    public boolean hasLinkedAssets() {
        for(OrderLineWS line : orderLines) {
            if(line.hasLinkedAssets()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningUserId()}
     * @return null
     */
    @XmlTransient
    public Integer getOwningEntityId() {
        return null;
    }

    @XmlTransient
    public Integer getOwningUserId() {
        return getUserId();
    }

    /**
     * Orders array, with only one OrderLineWS per bundled Item.
     * The adjustedPrice is a property of the LineWS object
     * @return the bundledItems 
     */
    public OrderLineWS[] getPlanBundledItems() {
        return planBundleItems;
    }
    
    /**
     * @param bundledItems the bundledItems to set
     */
    public void setPlanBundledItems(OrderLineWS[] bundledItems) {
        this.planBundleItems = bundledItems;
    }
    
    public String getAdjustedTotal() {
		return adjustedTotal;
	}
    
    public BigDecimal getAdjustedTotalAsDecimal() {
		return Util.string2decimal(adjustedTotal);
	}

	public void setAdjustedTotal(String adjustedTotal) {
		this.adjustedTotal = adjustedTotal;
	}
	
	public void setAdjustedTotal(BigDecimal adjustedTotal) {
		this.adjustedTotal = (adjustedTotal != null ? adjustedTotal.toString() : null);
	}

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("OrderWS");
        sb.append("{id=").append(id);
        sb.append(", userId=").append(userId);
        sb.append(", currencyId=").append(currencyId);
        sb.append(", activeUntil=").append(activeUntil);
        sb.append(", activeSince=").append(activeSince);
        sb.append(", statusStr='").append(statusStr).append('\'');
        sb.append(", periodStr='").append(periodStr).append('\'');
        sb.append(", periodId=").append(period);
        sb.append(", billingTypeStr='").append(billingTypeStr).append('\'');

        sb.append(", lines=");
        if (getOrderLines() != null) {
            sb.append(Arrays.toString(getOrderLines()));
        } else {
            sb.append("[]");
        }
        
        sb.append(", discountLines=");
        if (getDiscountLines() != null) {
            sb.append(Arrays.toString(getDiscountLines()));
        } else {
            sb.append("[]");
        }

        sb.append('}');
        sb.append(", parentOrderId=").append(parentOrder != null ? parentOrder.getId() : "null" ).append(',');
        sb.append(" childOrderIds:[");
        if (getChildOrders() != null) {
            for (OrderWS childOrder: getChildOrders()) {
                sb.append( null != childOrder ? childOrder.getId() : null).append("-");
            }
        }
        sb.append("]");

        sb.append(", userCode=").append(userCode);

        return sb.toString();
    }

	public String getCancellationFeeType() {
        return cancellationFeeType;
    }

    public void setCancellationFeeType(String cancellationFeeType) {
        this.cancellationFeeType = cancellationFeeType;
    }

    public Integer getCancellationFeePercentage() {
        return cancellationFeePercentage;
    }

    public void setCancellationFeePercentage(Integer cancellationFeePercentage) {
        this.cancellationFeePercentage = cancellationFeePercentage;
    }

    public Integer getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(Integer cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    public Integer getCancellationMaximumFee() {
        return cancellationMaximumFee;
    }

    public void setCancellationMaximumFee(Integer cancellationMaximumFee) {
        this.cancellationMaximumFee = cancellationMaximumFee;
    }

    public Integer getCancellationMinimumPeriod() {
        return cancellationMinimumPeriod;
    }

    public void setCancellationMinimumPeriod(Integer cancellationMinimumPeriod) {
        this.cancellationMinimumPeriod = cancellationMinimumPeriod;
    }
    
    public String getFreeUsageQuantity() {
		return null != freeUsageQuantity && !freeUsageQuantity.isEmpty() ? freeUsageQuantity : "0";
	}

	public void setFreeUsageQuantity(String freeUsageQuantity) {
		this.freeUsageQuantity = freeUsageQuantity;
	}
	
	public boolean isDisable() {
		return isDisable;
	}

	public void setDisable(boolean isDisable) {
		this.isDisable = isDisable;
	}

	public Integer getCustomerBillingCycleUnit() {
		return customerBillingCycleUnit;
	}

	public Integer getCustomerBillingCycleValue() {
		return customerBillingCycleValue;
	}

	public void setCustomerBillingCycleUnit(Integer customerBillingCycleUnit) {
		this.customerBillingCycleUnit = customerBillingCycleUnit;
	}

	public void setCustomerBillingCycleValue(Integer customerBillingCycleValue) {
		this.customerBillingCycleValue = customerBillingCycleValue;
	}

	public String getProratingOption() {
		return proratingOption;
	}

	public void setProratingOption(String proratingOption) {
		this.proratingOption = proratingOption;
	}
}
