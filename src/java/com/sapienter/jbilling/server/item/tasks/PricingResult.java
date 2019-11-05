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

package com.sapienter.jbilling.server.item.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.rule.Result;

import java.math.BigDecimal;

/**
 * @author emilc
 */
public class PricingResult extends Result {

    private static final FormatLogger LOG = new FormatLogger(PricingResult.class);

    private final Integer itemId;
    private final Integer userId;
    private final Integer currencyId;
    private BigDecimal price;
    private BigDecimal quantity;
    private long pricingFieldsResultId;
    private boolean perCurrencyRateCard;
    private BigDecimal freeUsageQuantity;
    private boolean isChained;
    private boolean isPercentage;

    public PricingResult(Integer itemId, Integer userId, Integer currencyId) {
        this.itemId = itemId;
        this.userId = userId;
        this.currencyId = currencyId;
        this.perCurrencyRateCard = false;
    }

    public PricingResult(Integer itemId, BigDecimal quantity, Integer userId, Integer currencyId) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.userId = userId;
        this.currencyId = currencyId;
        this.perCurrencyRateCard = false;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public Integer getUserId() {
        return userId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        LOG.debug("Setting price. Result fields id %s item %s price %s", pricingFieldsResultId, itemId, price);
        this.price = price;
    }

    public void setPrice(String price) {
        setPrice(new BigDecimal(price));
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public long getPricingFieldsResultId() {
        return pricingFieldsResultId;
    }

    public void setPricingFieldsResultId(long pricingFieldsResultId) {
        this.pricingFieldsResultId = pricingFieldsResultId;
    }
    
    public BigDecimal getFreeUsageQuantity() {
        return (freeUsageQuantity != null ? freeUsageQuantity : BigDecimal.ZERO);
    }

    public boolean isPerCurrencyRateCard() {
        return perCurrencyRateCard;
    }

    public void setPerCurrencyRateCard(boolean perCurrencyRateCard) {
        this.perCurrencyRateCard = perCurrencyRateCard;
    }

    public void setFreeUsageQuantity(BigDecimal freeUsageQuantity) {
        this.freeUsageQuantity = freeUsageQuantity;
    }
    
    public boolean isChained() {
		return isChained;
	}
    
	public void setIsChained(boolean isChained) {
		this.isChained = isChained;
	}

	public boolean isPercentage() {
		return isPercentage;
	}

	public void setIsPercentage(boolean isPercentage) {
		this.isPercentage = isPercentage;
	}
	
	public String toString() {
        return  "PricingResult:" +
                "itemId=" + itemId + " " +
                "userId=" + userId + " " +
                "isPercentage=" + isPercentage + " " +
                "currencyId=" + currencyId + " " +
                "price=" + price + " " +
                "pricing fields result id=" + pricingFieldsResultId + " " +
                super.toString();
    }


}
