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

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class PaymentCommissionDAS extends AbstractDAS<PaymentCommissionDTO> {

    public List<PaymentCommissionDTO> findByInvoiceId(Integer invoiceId) {
        Criteria criteria = getSession().createCriteria(PaymentCommissionDTO.class)
                .add(Restrictions.eq("invoice.id", invoiceId))
                .addOrder(Order.asc("id"));

        return criteria.list();
    }

    public List<Integer> findInvoiceIdsByPartner(PartnerDTO partner) {
        Criteria criteria = getSession().createCriteria(PaymentCommissionDTO.class)
                .createAlias("invoice", "_invoice")
                .createAlias("_invoice.baseUser", "_baseUser")
                .createAlias("_baseUser.customer", "_customer")
                .add(Restrictions.eq("_customer.partner", partner))
                .setProjection(Property.forName("_invoice.id"));
        return criteria.list();
    }

    public List<PaymentCommissionDTO> findByPartner(PartnerDTO partner) {
        Criteria criteria = getSession().createCriteria(PaymentCommissionDTO.class)
                .createAlias("invoice", "_invoice")
                .createAlias("_invoice.baseUser", "_baseUser")
                .createAlias("_baseUser.customer", "_customer")
                .add(Restrictions.eq("_customer.partner", partner));
        return criteria.list();
    }

}
