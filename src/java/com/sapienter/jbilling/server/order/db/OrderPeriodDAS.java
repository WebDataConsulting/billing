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
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import java.util.List;

public class OrderPeriodDAS extends AbstractDAS<OrderPeriodDTO> {
	
	public OrderPeriodDTO findOrderPeriod(Integer entityId, Integer value, Integer unitId) {
		
        final String hql = "select p from OrderPeriodDTO p where " +
        		"p.company.id=:entityId and p.periodUnit.id=:unitId and p.value=:value";

        Query query = getSession().createQuery(hql);
        query.setParameter("entityId", entityId);
        query.setParameter("unitId", unitId);
        query.setParameter("value", value);

        return (OrderPeriodDTO) query.uniqueResult();
		
	}


    /**
     * Returns any orderPeriod distinct to 'ONCE'
     *
     * @return a period
     */
    @SuppressWarnings("unchecked")
    public OrderPeriodDTO findRecurringPeriod(Integer entityId) {
        Criteria criteria = getSession().createCriteria(OrderPeriodDTO.class)
                .add(Restrictions.ne("id", ServerConstants.ORDER_PERIOD_ONCE))
                .add(Restrictions.eq("company.id", entityId))
                .setMaxResults(1);

        return findFirst(criteria);
    }

    /**
     * Returns list of order periods defined for company(entity)
     *
     * @param entityId - the entity id
     */
    public List<OrderPeriodDTO> getOrderPeriods(Integer entityId) {
        Criteria criteria = getSession().createCriteria(OrderPeriodDTO.class);
        criteria.add(Restrictions.eq("company.id", entityId));
        return criteria.list();
    }
}
