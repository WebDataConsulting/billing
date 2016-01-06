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
package com.sapienter.jbilling.server.process.db;

import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import org.hibernate.Criteria;
import org.hibernate.Query;

import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class AgeingEntityStepDAS extends AbstractDAS<AgeingEntityStepDTO> {

    public void create(Integer entityId, String description,
                       Integer languageId, int days,
                       int sendNotification, int retryPayment, int suspend) {

        UserStatusDTO userStatus = new UserStatusDTO();
        userStatus.setCanLogin(1);
        userStatus = new UserStatusDAS().save(userStatus);
        userStatus.setDescription(description, languageId);

        AgeingEntityStepDTO ageing = new AgeingEntityStepDTO();
        ageing.setCompany(new CompanyDAS().find(entityId));
        ageing.setUserStatus(userStatus);

        ageing.setDays(days);
        ageing.setSendNotification(sendNotification);
        ageing.setRetryPayment(retryPayment);
        ageing.setSuspend(suspend);
        ageing.setDescription(description, languageId);

        save(ageing);
    }

    @SuppressWarnings("unchecked")
    public List<AgeingEntityStepDTO> findAgeingStepsForEntity(Integer entityId) {
        Criteria criteria = getSession().createCriteria(AgeingEntityStepDTO.class)
                .add(Restrictions.eq("company.id", entityId))
                .addOrder(Order.asc("days"));
        return criteria.list();
    }

    public boolean isAgeingStepInUse(Integer ageingStepId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .createAlias("userStatus", "status", CriteriaSpecification.INNER_JOIN)
                .createAlias("status.ageingEntityStep", "ageingStep", CriteriaSpecification.INNER_JOIN)
                .add(Restrictions.eq("ageingStep.id", ageingStepId))
                .setProjection(Projections.count("id"));
        return (Long) criteria.list().get(0) > 0;
    }
}
