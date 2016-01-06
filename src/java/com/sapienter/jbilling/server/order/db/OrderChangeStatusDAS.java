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

import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.db.AbstractGenericStatusDAS;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springmodules.cache.CachingModel;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Alexander Aksenov
 * @since 05.07.13
 */
public class OrderChangeStatusDAS extends AbstractGenericStatusDAS<OrderChangeStatusDTO> {

    public OrderChangeStatusDAS() {
        super();
        cacheModel = (CachingModel) Context.getBean(Context.Name.CACHE_MODEL_RW);
    }

    @SuppressWarnings("unchecked")
    public List<OrderChangeStatusDTO> findOrderChangeStatuses(Integer entityId) {
        Criteria criteria = getSession().createCriteria(OrderChangeStatusDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.or(Restrictions.eq("company.id", entityId), Restrictions.isNull("company")))
                .addOrder(Order.asc("order")); //todo: cacheable
        return criteria.list();
    }

    public OrderChangeStatusDTO findApplyStatus(int entityId) {
        for (OrderChangeStatusDTO dto : findOrderChangeStatuses(entityId)) {
            if (dto.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return dto;
            }
        }
        return null;
    }

    /**
     * Select user order change statuses for entity ordered by 'order' field.
     * System statuses are not included in the result
     * @param entityId Target entity id
     * @return list of user orderChangeStatuses
     */
    public List<OrderChangeStatusDTO> findUserOrderChangeStatusesOrdered(Integer entityId) {
        List<OrderChangeStatusDTO> result = new LinkedList<OrderChangeStatusDTO>();
        for (OrderChangeStatusDTO dto : findOrderChangeStatuses(entityId)) {
            if (dto.getId() != Constants.ORDER_CHANGE_STATUS_APPLY_ERROR &&
                    dto.getId() != Constants.ORDER_CHANGE_STATUS_PENDING) {
                result.add(dto);
            }
        }
        return result;
    }
}
