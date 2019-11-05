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

import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDAS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.*;

import java.util.List;

public class CustomerDAS extends AbstractDAS<CustomerDTO> {
    public CustomerDTO create() {
        CustomerDTO newCustomer = new CustomerDTO();
        newCustomer.setInvoiceDeliveryMethod(new InvoiceDeliveryMethodDAS()
                .find(ServerConstants.D_METHOD_EMAIL));
        newCustomer.setExcludeAging(0);
        return save(newCustomer);
    }

    public Integer getCustomerId(Integer userId){
        Criteria criteria = getSession().createCriteria(CustomerDTO.class);
        criteria.add(Restrictions.eq("baseUser.id", userId));
        criteria.setProjection(Projections.id());
        return (Integer) criteria.uniqueResult();
    }

    public List<Integer> getCustomerAccountInfoTypeIds(Integer customerId){
        DetachedCriteria atCriteria = DetachedCriteria.forClass(CustomerDTO.class);
        atCriteria.add(Restrictions.idEq(customerId));
        atCriteria.setProjection(Projections.property("accountType.id"));
        atCriteria.addOrder(Order.asc("id"));

        Criteria criteria = getSession().createCriteria(AccountInformationTypeDTO.class);
        criteria.setProjection(Projections.id());
        criteria.add(Subqueries.propertyEq("accountType.id", atCriteria));

        return criteria.list();
    }

}
