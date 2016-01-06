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

import com.sapienter.jbilling.common.FormatLogger;
;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Usage represents a single customers usage of an item, or an item type over a
 * set date range (usually aligned with the customer's billing period).
 * 
 * @author Brian Cowdery
 * @since 16-08-2010
 */
public class Usage implements Serializable {
    private static FormatLogger LOG = new FormatLogger(Usage.class);

    private Integer userId;
    private Integer itemId;
    private Integer itemTypeId;
    private BigDecimal quantity;
    private BigDecimal amount;

    private BigDecimal currentQuantity;
    private BigDecimal currentAmount;
    
    private BigDecimal currentLineQuantity;

    private Date startDate;
    private Date endDate;
    
    private BigDecimal freeUsageQuantity;
    
    public Usage() {
    }

    public Usage(Integer userId, Integer itemId, Integer itemTypeId, BigDecimal quantity, BigDecimal amount,
                 BigDecimal currentQuantity, BigDecimal currentAmount, Date startDate, Date endDate) {
        this.userId = userId;
        this.itemId = itemId;
        this.itemTypeId = itemTypeId;
        this.quantity = quantity;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentQuantity = currentQuantity;
        this.currentAmount = currentAmount;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getItemTypeId() {
        return itemTypeId;
    }

    public void setItemTypeId(Integer itemTypeId) {
        this.itemTypeId = itemTypeId;
    }

    /**
     * The total quantity, or "number of units" purchased
     * over the period.
     *
     * @return number of units purchased
     */
    public BigDecimal getQuantity() {
        return (quantity != null ? quantity : BigDecimal.ZERO);
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void addQuantity(BigDecimal quantity) {
        if (quantity != null) setQuantity(getQuantity().add(quantity));
    }

    public void subtractQuantity(BigDecimal quantity) {
        if (quantity != null) setQuantity(getQuantity().subtract(quantity));
    }
            
    /**
     * The total dollar amount of usage purchased over the period.
     *
     * @return total amount of usage in dollars
     */
    public BigDecimal getAmount() {
        return (amount != null ? amount : BigDecimal.ZERO);
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void addAmount(BigDecimal amount) {
        if (amount != null) setAmount(getAmount().add(amount));
    }

    public void subractAmount(BigDecimal amount) {
        if (amount != null) setAmount(getAmount().subtract(amount));
    }

    /**
     * Quantity purchased over the working order
     * @return Local item quantity
     */
    public BigDecimal getCurrentQuantity() {
        return (currentQuantity != null ? currentQuantity : BigDecimal.ZERO);
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public void addCurrentQuantity(BigDecimal currentQuantity) {
        if (currentQuantity != null) {
            setCurrentQuantity(getCurrentQuantity().add(currentQuantity));
        }
    }

    public void subtractCurrentQuantity(BigDecimal currentQuantity) {
        if (currentQuantity != null)
            setCurrentQuantity(getCurrentQuantity().subtract(currentQuantity));
    }
    
    public BigDecimal getCurrentLineQuantity() {
        return (currentLineQuantity != null ? currentLineQuantity : BigDecimal.ZERO);
    }

    public void setCurrentLineQuantity(BigDecimal currentLineQuantity) {
        this.currentLineQuantity = currentLineQuantity;
    }

    /**
     * Amount of usage purchased over the working order
     *
     * @return local item amount
     */
    public BigDecimal getCurrentAmount() {
        return (currentAmount != null ? currentAmount : BigDecimal.ZERO);
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }

    public void addCurrentAmount(BigDecimal currentAmount) {
        if (currentAmount != null) {
            setCurrentAmount(getCurrentAmount().add(currentAmount));
        }
    }

    public void subtractCurrentAmount(BigDecimal currentAmount) {
        if (currentAmount != null)
            setCurrentAmount(getCurrentAmount().subtract(currentAmount));
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
    
    public BigDecimal getFreeUsageQuantity() {
        return (freeUsageQuantity != null ? freeUsageQuantity : BigDecimal.ZERO);
    }

    public void setFreeUsageQuantity(BigDecimal freeUsageQuantity) {
        this.freeUsageQuantity = freeUsageQuantity;
    }
    
    @Override
    public String toString() {
        return "Usage{"
                + "itemId=" + itemId
                + ", itemTypeId=" + itemTypeId
                + ", quantity=" + getQuantity()
                + ", amount=" + getAmount()
                + ", startDate=" + startDate
                + ", endDate=" + endDate
                + ", freeUsageQuantity=" + freeUsageQuantity
                + '}';
    }
}
