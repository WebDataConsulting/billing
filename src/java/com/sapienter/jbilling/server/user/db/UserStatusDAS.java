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
package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.io.Serializable;
import java.util.List;

public class UserStatusDAS extends AbstractDAS<UserStatusDTO> {


    public UserStatusDTO findByValueIfSingle(Integer statusValue) {
        if (statusValue == null) {
            return null;
        }
        List<UserStatusDTO> statuses = findByCriteria(Restrictions.eq("statusValue", statusValue));
        if (statuses.size() == 1) {
            return statuses.get(0);
        } else {
            // search by ID is needed for unique
            return null;
        }
    }

    public List<UserStatusDTO> findByEntityId(Integer entityId) {
        Criteria crit = getSession().createCriteria(UserStatusDTO.class)
                .createAlias("ageingEntityStep", "aes", CriteriaSpecification.LEFT_JOIN);

        Disjunction disjunction = Restrictions.disjunction();
        disjunction.add(Restrictions.idEq(UserDTOEx.STATUS_ACTIVE));
        disjunction.add(Restrictions.eq("aes.company.id", entityId));

        crit.add(disjunction);
        crit.addOrder(Order.asc("id"));

        return crit.list();
    }

    @Override
    public UserStatusDTO findNow(Serializable statusId) {
        return find(statusId);
    }
}
