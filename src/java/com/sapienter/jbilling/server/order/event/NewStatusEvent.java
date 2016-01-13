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

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.system.event.Event;

public class NewStatusEvent implements Event {

    private static final FormatLogger LOG = new FormatLogger(NewStatusEvent.class);
    private Integer entityId;
    private Integer userId;
    private Integer orderId;
    private Integer orderType;
    private Integer oldStatusId;
    private Integer newStatusId;
    private Integer executorId;

    public NewStatusEvent(Integer orderId, Integer oldStatusId, Integer newStatusId, Integer executorId) {
        this(orderId, oldStatusId, newStatusId);
        this.executorId = executorId;
    }

    public NewStatusEvent(Integer orderId, Integer oldStatusId, Integer newStatusId) {
        try {
            OrderBL order = new OrderBL(orderId);
            
            this.entityId = order.getDTO().getUser().getEntity().getId();
            this.userId = order.getDTO().getUser().getUserId();
            this.orderType = order.getDTO().getOrderPeriod().getId();
            this.oldStatusId = oldStatusId;
            this.newStatusId = newStatusId;
        } catch (Exception e) {
            LOG.error("Handling order in event", e);
        } 
        this.orderId = orderId;
    }
    
    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "New status";
    }

    public String toString() {
        return getName();
    }
    public Integer getOrderId() {
        return orderId;
    }
    public Integer getUserId() {
        return userId;
    }

    public Integer getOrderType() {
        return orderType;
    }

    public Integer getNewStatusId() {
        return newStatusId;
    }

    public Integer getOldStatusId() {
        return oldStatusId;
    }

    public Integer getExecutorId() {
        return executorId;
    }
}
