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

import org.hibernate.Criteria;
import org.hibernate.Query;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

/**
 * 
 * @author khobab
 *
 */
public class PaymentInformationDAS extends AbstractDAS<PaymentInformationDTO> {
	
	private final static String IS_CREDIT_CARD_SQL = 
		"select p.id from payment_information p " +
			" inner join payment_information_meta_fields_map pimf on p.id = pimf.payment_information_id " +
			" inner join meta_field_value mf on pimf.meta_field_value_id = mf.id " +
			" inner join meta_field_name mfn  on (mf.meta_field_name_id = mfn.id and mfn.field_usage = 'PAYMENT_CARD_NUMBER') " +
			" where p.id = :instrument";
	
	/**
	 * creates payment instrument without meta fields
	 * 
	 * @param dto	PaymentInformationDTO
	 * @return	PaymentInformationDTO
	 */
	public PaymentInformationDTO create(PaymentInformationDTO dto, Integer entityId){
		PaymentInformationDTO saved = new PaymentInformationDTO(dto.getProcessingOrder(), dto.getUser(), dto.getPaymentMethodType());
		saved.updatePaymentMethodMetaFieldsWithValidation(entityId, dto);
		return save(saved);
	}
	
	public boolean isCreditCard(Integer instrument) {
        Query sqlQuery = getSession().createSQLQuery(IS_CREDIT_CARD_SQL);
        sqlQuery.setParameter("instrument", instrument);
        sqlQuery.setMaxResults(1);
        Number count = (Number) sqlQuery.uniqueResult();
        return Integer.valueOf(null == count ? 0 : count.intValue()) > 0;
	}

	public Long findByAccountTypeAndPaymentMethodType(Integer accountTypeId, Integer paymentMethodTypeId) {
		Criteria criteria = getSession().createCriteria(PaymentInformationDTO.class)
				.createAlias("user", "user")
				.createAlias("user.customer", "customer")
				.createAlias("customer.accountType", "accountType")
				.add(Restrictions.eq("accountType.id", accountTypeId))
				.add(Restrictions.eq("paymentMethodType.id", paymentMethodTypeId))
				.setProjection(Projections.rowCount());
		return (Long)criteria.uniqueResult();
	}
}
