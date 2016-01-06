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
package com.sapienter.jbilling.server.order.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * @author: Alexander Aksenov
 * @since: 21.02.14
 */
public class OrderChangeTypeDAS extends AbstractDAS<OrderChangeTypeDTO> {

    @SuppressWarnings("unchecked")
    public List<OrderChangeTypeDTO> findOrderChangeTypes(Integer entityId) {
        Criteria criteria = getSession().createCriteria(OrderChangeTypeDTO.class)
                .add(Restrictions.or(
                        Restrictions.eq("entity.id", entityId),
                        Restrictions.isNull("entity.id")
                 ))
                .addOrder(Order.asc("id"));
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public OrderChangeTypeDTO findOrderChangeTypeByName(String name, Integer entityId) {
        Criteria criteria = getSession().createCriteria(OrderChangeTypeDTO.class)
                .add(Restrictions.eq("entity.id", entityId))
                .add(Restrictions.eq("name", name));
        List<OrderChangeTypeDTO> results = criteria.list();
        // change types are unique by name within entity
        return results != null && !results.isEmpty() ? results.get(0) : null;
    }

    @SuppressWarnings("unchecked")
    public boolean isOrderChangeTypeInUse(Integer orderChangeTypeId) {
        Criteria criteria = getSession().createCriteria(OrderChangeDTO.class)
                .add(Restrictions.eq("orderChangeType.id", orderChangeTypeId))
                .setProjection(Projections.rowCount());
        List<Long> count = criteria.list();
        return count != null && !count.isEmpty() && count.get(0) > 0;
    }
}
