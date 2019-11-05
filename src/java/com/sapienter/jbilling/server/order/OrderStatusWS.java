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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author Maruthi
 */

public class OrderStatusWS implements WSSecured, Serializable {

    private static final long serialVersionUID = 20140605L;

	private Integer id;
    private OrderStatusFlag orderStatusFlag;
    private Integer userId = null;
    private CompanyWS entity;
    @Size(min = 1, max = 50,message = "validation.error.size,1,50")
    private String description;
    private List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>(1);
   
    
    public OrderStatusWS() {
    }

    public OrderStatusWS(Integer id, CompanyWS entity, OrderStatusFlag orderStatusFlag, String description) {
        setId(id);
        setEntity(entity);
        setOrderStatusFlag(orderStatusFlag);
        setDescription(description);
    }

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public OrderStatusFlag getOrderStatusFlag() {
		return orderStatusFlag;
	}

	public void setOrderStatusFlag(OrderStatusFlag orderStatusFlag) {
		this.orderStatusFlag = orderStatusFlag;
	}

	public CompanyWS getEntity() {
		return entity;
	}

	public void setEntity(CompanyWS entity) {
		this.entity = entity;
	}

	public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("OrderStatusWS");
        sb.append("{id=").append(id);
        sb.append(", userId=").append(userId);
        sb.append(", orderStatusFlag=").append(orderStatusFlag);
        sb.append(", entity=").append(entity);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningUserId()}
     * @return null
     */
    public Integer getOwningEntityId() {
        return null;
    }

    public Integer getOwningUserId() {
        return getUserId();
    }
}
