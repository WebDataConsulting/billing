package com.sapienter.jbilling.server.discount;

import com.sapienter.jbilling.common.Util;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;


public class DiscountLineWS implements Serializable {

    private Integer id;
    @NotNull(message = "validation.error.null.discount")
    private Integer discountId;
    private Integer orderId;
    private Integer itemId;
    private Integer discountOrderLineId;

    private String orderLineAmount;        // this line amount will be used for product level discounts
    private String description;            // discount line description to be used in invoice

    // Starts from 1. Identifies the sorting order on UI and
    // used for removing discountline from conversation.order
    private Integer discountLineIndex;
    private String lineLevelDetails;
    private String discountAmount;

    public DiscountLineWS() {

    }

    public DiscountLineWS(Integer id, Integer discountId, Integer orderId, Integer itemId) {
        setId(id);
        setDiscountId(discountId);
        setOrderId(orderId);
        setItemId(itemId);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDiscountId() {
        return discountId;
    }

    public void setDiscountId(Integer discountId) {
        this.discountId = discountId;
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

    public Integer getDiscountOrderLineId() {
        return discountOrderLineId;
    }

    public void setDiscountOrderLineId(Integer discountOrderLineId) {
        this.discountOrderLineId = discountOrderLineId;
    }

    public String getOrderLineAmount() {
        return orderLineAmount;
    }

    public void setOrderLineAmount(String orderLineAmount) {
        this.orderLineAmount = orderLineAmount;
    }

    public BigDecimal getOrderLineAmountAsDecimal() {
        return Util.string2decimal(orderLineAmount);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDiscountLineIndex() {
        return discountLineIndex;
    }

    public void setDiscountLineIndex(Integer discountLineIndex) {
        this.discountLineIndex = discountLineIndex;
    }

    public String getLineLevelDetails() {
        return lineLevelDetails;
    }

    public void setLineLevelDetails(String lineLevelDetails) {
        this.lineLevelDetails = lineLevelDetails;
    }

    public boolean hasItem() {
        return getItemId() != null && getItemId() > 0;
    }

    public boolean isProductLevelDiscount() {
        return (hasItem());
    }

    public boolean isOrderLevelDiscount() {
        return (!hasItem());
    }

    /**
     * @return the discountAmount
     */
    public String getDiscountAmount() {
        return discountAmount;
    }

    /**
     * @param discountAmount the discountAmount to set
     */
    public void setDiscountAmount(String discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getDiscountAmountAsDecimal() {
        return Util.string2decimal(discountAmount);
    }

    @Override
    public String toString() {
        return String
                .format("DiscountLineWS [id=%s, discountId=%s, orderId=%s, itemId=%s, discountOrderLineId=%s, orderLineAmount=%s, description=%s, discountLineIndex=%s, lineLevelDetails=%s, discountAmount=%s]",
                        id, discountId, orderId, itemId,
                        discountOrderLineId, orderLineAmount, description,
                        discountLineIndex, lineLevelDetails, discountAmount);
    }

}
