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
package com.sapienter.jbilling.server.user.db;


import com.sapienter.jbilling.server.order.db.OrderDTO;

import javax.persistence.*;

/**
 * Represents a link of a UserCodeDTO to Order in jBilling.
 */
@Entity
@DiscriminatorValue("ORDER")
public class UserCodeOrderLinkDTO extends UserCodeLinkDTO {

    private OrderDTO order;

    public UserCodeOrderLinkDTO() {

    }

    public UserCodeOrderLinkDTO(UserCodeDTO userCode, OrderDTO order) {
        super(userCode);
        this.order = order;
    }


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_id")
    public OrderDTO getOrder() {
        return order;
    }

    public void setOrder(OrderDTO order) {
        this.order = order;
    }
}
