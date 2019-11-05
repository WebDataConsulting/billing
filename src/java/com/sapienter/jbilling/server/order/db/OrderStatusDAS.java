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
package com.sapienter.jbilling.server.order.db;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class OrderStatusDAS extends AbstractDAS<OrderStatusDTO> {
    private static final FormatLogger LOG = new FormatLogger(OrderStatusDAS.class);

    public OrderStatusDTO createOrderStatus (OrderStatusDTO orderStatus) {
        return save(orderStatus);
    }

    public OrderStatusWS findOrderStatusById (Integer orderStatusId) {
        return OrderStatusBL.getOrderStatusWS(find(orderStatusId));
    }

    public int findByOrderStatusFlag (OrderStatusFlag orderStatusFlag, Integer entityId) {

        return getSession()
                .createCriteria(OrderStatusDTO.class)
                .add(Restrictions.eq("orderStatusFlag", orderStatusFlag))
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", entityId))
                .list()
                .size();
    }

    @SuppressWarnings("unchecked")
    public List<OrderStatusDTO> findAllByOrderStatusFlag (OrderStatusFlag orderStatusFlag, Integer entityId) {

        return getSession()
                .createCriteria(OrderStatusDTO.class)
                .add(Restrictions.eq("orderStatusFlag", orderStatusFlag))
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", entityId))
                .addOrder(Order.asc("id"))
                .list();
    }

    public int getDefaultOrderStatusId (OrderStatusFlag flag, Integer entityId) {
        Criteria criteria = getSession()
                .createCriteria(OrderStatusDTO.class)
                .add(Restrictions.eq("orderStatusFlag", flag))
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", entityId))
                .addOrder(Order.asc("id"))
                .setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<OrderStatusDTO> list = criteria.list();
        LOG.debug("Order Status Dto == %s", list.get(0));
        OrderStatusDTO orderStatusDTO = list.get(0);
        LOG.debug("Order Status Dto Id == %s", orderStatusDTO.getId());
        return orderStatusDTO.getId();
    }

    @SuppressWarnings("unchecked")
    public List<OrderStatusDTO> findAll (Integer companyId) {
        return getSession()
                .createCriteria(OrderStatusDTO.class)
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", companyId))
                .list();
    }

}
