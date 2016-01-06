/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.order.event;

import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * This event is triggered AFTER an orderChange is successfully applied to an order.
 * Allows for the order to be modified before saving new changes in order.
 *
 * Important: event is triggered for any successful application of change to order.
 * It is not guaranteed that applied changes will take effect in DB
 * (ex, changes can be applied for non-persisted object for rate order only)
 *
 * @author: Alexander Aksenov
 * @since: 27.02.14
 */
public class OrderChangeAppliedEvent implements Event {

    private final Integer entityId;
    private final OrderChangeDTO orderChangeDTO;

    public OrderChangeAppliedEvent(Integer entityId, OrderChangeDTO orderChangeDTO) {
        this.entityId = entityId;
        this.orderChangeDTO = orderChangeDTO;
    }

    @Override
    public String getName() {
        return "Order Change Applied Event - entity " + entityId;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public OrderChangeDTO getOrderChangeDTO() {
        return orderChangeDTO;
    }
}
