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
package com.sapienter.jbilling.server.user.partner.db;

import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class CommissionProcessRunDAS extends AbstractDAS<CommissionProcessRunDTO>{

    public CommissionProcessRunDTO findLatestByDate(CompanyDTO entity) {
        Criteria criteria = getSession().createCriteria(CommissionProcessRunDTO.class);
        criteria.add(Restrictions.eq("entity", entity));
        criteria.addOrder(Order.desc("periodEnd"));
        return findFirst(criteria);
    }

    public CommissionProcessRunDTO findLatest(CompanyDTO entity) {
        Criteria criteria = getSession().createCriteria(CommissionProcessRunDTO.class);
        criteria.add(Restrictions.eq("entity", entity));
        criteria.addOrder(Order.desc("id"));
        return findFirst(criteria);
    }

    public List<CommissionProcessRunDTO> findAllByEntity(CompanyDTO entity){
        Criteria criteria = getSession().createCriteria(CommissionProcessRunDTO.class);
        criteria.add(Restrictions.eq("entity", entity));

        return criteria.list();
    }
}
