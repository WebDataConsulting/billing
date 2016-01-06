/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.order;

import static com.sapienter.jbilling.common.CommonConstants.INTEGER_TRUE;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.util.api.validation.CreateValidationGroup;
import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;

/**
 * @author Alexander Aksenov
 * @since 09.07.13
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class OrderChangeWS implements Serializable {

    private static final long serialVersionUID = 5904281478067075499L;

    private Integer id;
    private Integer orderId;
    private Integer orderLineId;
    private Integer itemId;
    private Integer parentOrderChangeId;
    private Integer parentOrderLineId;

    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number", groups = {CreateValidationGroup.class, UpdateValidationGroup.class} )
    private String quantity;
    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number", groups = {CreateValidationGroup.class, UpdateValidationGroup.class} )
    private String price;
    private String description;
    private Integer useItem;
    private Date startDate;
    private Date applicationDate;
    private Integer[] assetIds;
    private Integer userAssignedStatusId;
    private String userAssignedStatus;
    private Integer statusId;
    private String status;
    private String errorMessage;
    private String errorCodes;
    private Integer optLock;
    private Integer delete = 0; // flag is this change should be removed

    private Integer orderStatusIdToApply;
    @NotNull(message = "validation.error.notnull")
    private Integer orderChangeTypeId;
    private String type;

    @Valid
    private MetaFieldValueWS[] metaFields;

    private Integer appliedManually;
    private Integer removal;

    private Date nextBillableDate;
    private Date endDate;

    // dependencies without ids should be in that fields
    // fill them only if appropriate id field is null
    @XmlIDREF
    @XmlAttribute(name = "orderRef")
    private OrderWS orderWS;
    @XmlIDREF
    @XmlAttribute(name = "parentChangeRef")
    private OrderChangeWS parentOrderChange;
    @XmlAttribute @XmlID
    private String objectId;
    private boolean isPercentage =false;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public OrderChangeWS() {
        objectId = UUID.randomUUID().toString();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getParentOrderChangeId() {
        return parentOrderChangeId;
    }

    public void setParentOrderChangeId(Integer parentOrderChangeId) {
        this.parentOrderChangeId = parentOrderChangeId;
    }

    public Integer getParentOrderLineId() {
        return parentOrderLineId;
    }

    public void setParentOrderLineId(Integer parentOrderLineId) {
        this.parentOrderLineId = parentOrderLineId;
    }

    public OrderWS getOrderWS() {
        return orderWS;
    }

    public void setOrderWS(OrderWS orderWS) {
        if (orderWS == null) {
            this.orderWS = null;
        } else if (orderWS.getId() != null && orderWS.getId() > 0) {
            this.orderWS = orderWS;
            this.orderId = orderWS.getId();
        } else {
            this.orderWS = orderWS;
            this.orderId = null;
        }
    }

    public OrderChangeWS getParentOrderChange() {
        return parentOrderChange;
    }

    public void setParentOrderChange(OrderChangeWS parentOrderChange) {
        if (parentOrderChange == null) {
            this.parentOrderChange = null;
        } else if (parentOrderChange.getId() != null && parentOrderChange.getId() > 0) {
            this.parentOrderChange = null;
            this.parentOrderChangeId = parentOrderChange.getId();
        } else {
            this.parentOrderChange = parentOrderChange;
            this.parentOrderChangeId = null;
        }
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getQuantityAsDecimal() {
        return Util.string2decimal(quantity);
    }

    public void setQuantityAsDecimal(BigDecimal quantity) {
        setQuantity(quantity);
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = (quantity != null ? quantity.toPlainString() : null);
    }

    public String getPrice() {
        return price;
    }

    public BigDecimal getPriceAsDecimal() {
        return Util.string2decimal(price);
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setPriceAsDecimal(BigDecimal price) {
        setPrice(price);
    }

    public void setPrice(BigDecimal price) {
        this.price = (price != null ? price.toPlainString() : null);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUseItem() {
        return useItem;
    }

    public void setUseItem(Integer useItem) {
        this.useItem = useItem;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(Date applicationDate) {
        this.applicationDate = applicationDate;
    }

    public Integer[] getAssetIds() {
        return assetIds;
    }

    public void setAssetIds(Integer[] assetIds) {
        this.assetIds = assetIds;
    }

    public Integer getUserAssignedStatusId() {
        return userAssignedStatusId;
    }

    public void setUserAssignedStatusId(Integer userAssignedStatusId) {
        this.userAssignedStatusId = userAssignedStatusId;
    }

    public String getUserAssignedStatus() {
        return userAssignedStatus;
    }

    public void setUserAssignedStatus(String userAssignedStatus) {
        this.userAssignedStatus = userAssignedStatus;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(Integer orderLineId) {
        this.orderLineId = orderLineId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessageForLocalization() {
        if (this.errorMessage == null) return null;
        StringTokenizer str = new StringTokenizer(this.errorMessage, ",");
        String result = null;
        while (str.hasMoreTokens()) {
            result = str.nextToken();
        }
        return result;
    }

    public String getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(String errorCodes) {
        this.errorCodes = errorCodes;
    }

    public Integer getDelete() {
        return delete;
    }

    public void setDelete(Integer delete) {
        this.delete = delete;
    }

    public Integer getOptLock() {
        return optLock;
    }

    public void setOptLock(Integer optLock) {
        this.optLock = optLock;
    }

    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    public Integer getOrderStatusIdToApply() {
        return orderStatusIdToApply;
    }

    public void setOrderStatusIdToApply(Integer orderStatusIdToApply) {
        this.orderStatusIdToApply = orderStatusIdToApply;
    }

    public Integer getOrderChangeTypeId() {
        return orderChangeTypeId;
    }

    public void setOrderChangeTypeId(Integer orderChangeTypeId) {
        this.orderChangeTypeId = orderChangeTypeId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getAppliedManually () {
        return appliedManually;
    }

    public void setAppliedManually (Integer appliedManually) {
        this.appliedManually = appliedManually;
    }

    public boolean isAppliedManually () {
        return INTEGER_TRUE.equals(appliedManually);
    }

    public Integer getRemoval () {
        return removal;
    }

    public void setRemoval (Integer removal) {
        this.removal = removal;
    }

    public boolean isRemoval () {
        return INTEGER_TRUE.equals(removal);
    }


    public Date getNextBillableDate () {
        return nextBillableDate;
    }

    public void setNextBillableDate (Date nextBillableDate) {
        this.nextBillableDate = nextBillableDate;
    }

    public Date getEndDate () {
        return endDate;
    }

    public void setEndDate (Date endDate) {
        this.endDate = endDate;
    }

    public boolean isAppliedSuccessfully() {
        return this.statusId != null &&
                !Constants.ORDER_CHANGE_STATUS_PENDING.equals(this.statusId) &&
                !Constants.ORDER_CHANGE_STATUS_APPLY_ERROR.equals(this.statusId);
    }

    public boolean isPercentage() {
		return isPercentage;
	}

	public void setPercentage(boolean isPercentage) {
		this.isPercentage = isPercentage;
	}
    
    public String toString() {
        return "OrderChangeWS{" +
                "id=" + id +
                ",orderId=" + orderId +
                ",orderLineId=" + orderLineId +
                ",itemId=" + itemId +
                ",parentOrderChangeId=" + parentOrderChangeId +
                ",parentOrderLineId=" + parentOrderLineId +
                ",delta quantity=" + quantity +
                ",statusId=" + statusId +
                ",isPercentage=" + isPercentage +
                ",userAssignedStatusId=" + userAssignedStatusId +
                ",isRemoval=" + removal +
                ",isAppliedManually=" + appliedManually +
                ",nextBillableDate=" + nextBillableDate +
                ",endDate=" + endDate +
                ",price=" + price +
                ",metaFields=" + ((metaFields == null) ? "null" : Arrays.asList(metaFields)) +
                "}";
    }

}
