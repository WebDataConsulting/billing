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
package com.sapienter.jbilling.server.order.event;

import java.math.BigDecimal;

import com.sapienter.jbilling.server.system.event.Event;

/**
 *
 * @author Deepak Pande
 * 
 */
public class NewPriceEvent implements Event {

    private final Integer entityId;
    private final BigDecimal newPrice;
    private final BigDecimal oldPrice;
    private final BigDecimal newAmount;
    private final BigDecimal oldAmount;
    private final Integer orderLineId;
    private final Integer orderId;
    
    public NewPriceEvent(Integer entityId, BigDecimal oldPrice, BigDecimal newPrice,
                         BigDecimal oldAmount, BigDecimal newAmount, Integer orderId,
                         Integer orderLineId) {
        this.entityId = entityId;
        this.newPrice = newPrice;
        this.oldPrice = oldPrice;
        this.newAmount = newAmount;
        this.oldAmount = oldAmount;
        this.orderLineId = orderLineId;
        this.orderId = orderId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public BigDecimal getOldPrice() {
        return oldPrice;
    }

    public BigDecimal getNewAmount() {
        return newAmount;
    }

    public BigDecimal getOldAmount() {
        return oldAmount;
    }

    public Integer getOrderLineId() {
        return orderLineId;
    }

    public Integer getOrderId() {
        return orderId;
    }
    
    public String getName() {
        return "New Price Event - entity " + entityId;
    }
    
	@Override
	public String toString() {
		return String
				.format("NewPriceEvent [entityId=%s, newPrice=%s, oldPrice=%s, oldAmount=%s, newAmount=%s, orderLineId=%s, orderId=%s]",
						entityId, newPrice, oldPrice, oldAmount, newAmount, orderLineId, orderId);
	}

}
