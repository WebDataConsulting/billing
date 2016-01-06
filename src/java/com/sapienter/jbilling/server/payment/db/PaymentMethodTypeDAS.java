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
package com.sapienter.jbilling.server.payment.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;


import org.hibernate.Query;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * 
 * @author khobab
 *
 */
public class PaymentMethodTypeDAS extends AbstractDAS<PaymentMethodTypeDTO> {
	
	public PaymentMethodTypeDTO getPaymentMethodTypeByTemplate(String templateName, Integer entity) {
		// I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(PaymentMethodTypeDTO.class)
        		.createAlias("entity", "e")
                 	.add(Restrictions.eq("e.id", entity))
                .createAlias("paymentMethodTemplate", "pmt")
                    .add(Restrictions.eq("pmt.templateName", templateName));
        return findFirst(criteria);
	}
	
	@SuppressWarnings("unchecked")
    public PaymentMethodTypeDTO findFirst(Query query) {
        query.setFirstResult(0).setMaxResults(1);
        return (PaymentMethodTypeDTO) query.uniqueResult();
    }
	
	public Integer countInstrumentsAttached(Integer paymentMethodId) {
		List list = getSession().createCriteria(PaymentInformationDTO.class)
			.add(Restrictions.eq("paymentMethodType.id", paymentMethodId))
			.list();
		
		if(list != null) {
			return list.size();
		}
		return 0;
	}

    public List<PaymentMethodTypeDTO> findByMethodName(String methodName, Integer entityId) {
        Criteria criteria = getSession().createCriteria(PaymentMethodTypeDTO.class)
                .add(Restrictions.eq("methodName", methodName).ignoreCase())
                .createAlias("entity", "e")
                .add(Restrictions.eq("e.id", entityId))
                .add(Restrictions.eq("e.deleted", 0));

        return (List<PaymentMethodTypeDTO>) criteria.list();
    }
    public List<PaymentMethodTypeDTO> findByAllAccountType(Integer entityId) {
        Criteria criteria = getSession().createCriteria(PaymentMethodTypeDTO.class)
                .add(Restrictions.eq("allAccountType", true))
                .createAlias("entity", "e")
                .add(Restrictions.eq("e.id", entityId))
                .add(Restrictions.eq("e.deleted", 0));

        return (List<PaymentMethodTypeDTO>) criteria.list();
    }
}
