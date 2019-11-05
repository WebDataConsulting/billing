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
package com.sapienter.jbilling.server.payment.blacklist.db;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS;
import com.sapienter.jbilling.server.util.db.AbstractDAS;


public class BlacklistDAS extends AbstractDAS<BlacklistDTO> {

    private static final FormatLogger LOG = new FormatLogger(BlacklistDAS.class);
    
    public List<BlacklistDTO> findByEntity(Integer entityId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(BlacklistDTO.class)
                .createAlias("company", "c")
                    .add(Restrictions.eq("c.id", entityId));

        return criteria.list();
    }

    public List<BlacklistDTO> findByEntityType(Integer entityId, Integer type) {
        Criteria criteria = getSession().createCriteria(BlacklistDTO.class)
                .createAlias("company", "c")
                    .add(Restrictions.eq("c.id", entityId))
                .add(Restrictions.eq("type", type));

        return criteria.list();    
    }

    public List<BlacklistDTO> findByEntitySource(Integer entityId, Integer source) {
        Criteria criteria = getSession().createCriteria(BlacklistDTO.class)
                .createAlias("company", "c")
                    .add(Restrictions.eq("c.id", entityId))
                .add(Restrictions.eq("source", source));

        return criteria.list();
    }

    public List<BlacklistDTO> findByUser(Integer userId) {
        Criteria criteria = getSession().createCriteria(BlacklistDTO.class)
                .createAlias("user", "u")
                    .add(Restrictions.eq("u.id", userId));

        return criteria.list();
    }

    public List<BlacklistDTO> findByUserType(Integer userId, Integer type) {
        Criteria criteria = getSession().createCriteria(BlacklistDTO.class)
                .createAlias("user", "u")
                    .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.eq("type", type));

        return criteria.list();
    }

    // blacklist filter specific queries

    public List<BlacklistDTO> filterByName(Integer entityId, String firstName,
            String lastName) {
        Criteria criteria = getSession().createCriteria(BlacklistDTO.class)
                .createAlias("company", "c")
                    .add(Restrictions.eq("c.id", entityId))
                .add(Restrictions.eq("type", BlacklistDTO.TYPE_NAME))
                .createAlias("contact", "ct")
                    .add(equals("ct.firstName", firstName))
                    .add(equals("ct.lastName", lastName));

        return criteria.list();
    }

    public List<BlacklistDTO> filterByAddress(Integer entityId, String address1,
            String address2, String city, String stateProvince, 
            String postalCode, String countryCode) {
        Criteria criteria = getSession().createCriteria(BlacklistDTO.class)
                .createAlias("company", "c")
                    .add(Restrictions.eq("c.id", entityId))
                .add(Restrictions.eq("type", BlacklistDTO.TYPE_ADDRESS))
                .createAlias("contact", "ct")
                    .add(equals("ct.address1", address1))
                    .add(equals("ct.address2", address2))
                    .add(equals("ct.city", city))
                    .add(equals("ct.stateProvince", stateProvince))
                    .add(equals("ct.postalCode", postalCode))
                    .add(equals("ct.countryCode", countryCode));

        return criteria.list();
    }

    public List<BlacklistDTO> filterByPhone(Integer entityId, 
            Integer phoneCountryCode, Integer phoneAreaCode, String phoneNumber) {
        Criteria criteria = getSession().createCriteria(BlacklistDTO.class)
                .createAlias("company", "c")
                    .add(Restrictions.eq("c.id", entityId))
                .add(Restrictions.eq("type", BlacklistDTO.TYPE_PHONE_NUMBER))
                .createAlias("contact", "ct")
                    .add(equals("ct.phoneCountryCode", phoneCountryCode))
                    .add(equals("ct.phoneAreaCode", phoneAreaCode))
                    .add(equals("ct.phoneNumber", phoneNumber));

        return criteria.list();
    }

    private final String queryFilterByCCNumber = "select bl.id from blacklist bl " +
					" inner join payment_information p on p.id = bl.credit_card_id " + 
					" inner join payment_information_meta_fields_map pimf on p.id = pimf.payment_information_id " +
					" inner join meta_field_value mf on pimf.meta_field_value_id = mf.id " + 
					" inner join meta_field_name mfn  on (mf.meta_field_name_id = mfn.id and mfn.field_usage = 'PAYMENT_CARD_NUMBER' )" + 
						" where mf.string_value in (:rawNumbers) and bl.entity_id = :companyId";

    public List<BlacklistDTO> filterByCcNumbers(Integer entityId, 
            Collection<String> rawNumbers) {
    	Query query = getSession().createSQLQuery(queryFilterByCCNumber)
    			.setParameterList("rawNumbers", rawNumbers)
    			.setParameter("companyId", entityId);
    	return query.list();
    }

    public List<BlacklistDTO> filterByIpAddress(Integer entityId, String ipAddress, Integer ccfId) {

        // don't try and filter if there's no IP address to lookup
        if (StringUtils.isBlank(ipAddress)) {
            return Collections.emptyList();
        }

        Criteria criteria = getSession().createCriteria(BlacklistDTO.class)
                .createAlias("company", "c")
                    .add(Restrictions.eq("c.id", entityId))
                .add(Restrictions.eq("type", BlacklistDTO.TYPE_IP_ADDRESS));
        
         Criteria secondCriteria = criteria.createCriteria("metaFieldValue", "fieldValue", CriteriaSpecification.LEFT_JOIN)
                    .add(Restrictions.eq("fieldValue.field.id", ccfId))
                    .add(Restrictions.sqlRestriction("{alias}.string_value = ?", ipAddress, Hibernate.STRING));

        return secondCriteria.list();
    }

    /**
     * Considers comparing nulls as equal. Useful for some filters,
     * such as address, where not all fields may have a value.
     */
    private Criterion equals(String propertyName, Object value) {
        if (value != null) {
            return Restrictions.eq(propertyName, value);
        }
        return Restrictions.isNull(propertyName);
    }

    public int deleteSource(Integer entityId, Integer source) {
        /*
        List<BlacklistDTO> deleteList = findByEntitySource(entityId, source);

        for (BlacklistDTO entry : deleteList) {
            delete(entry);
        }

        return deleteList.size();
        */

        // should be faster than above, but hql doesn't do cascading deletes :(
    	String hql = "DELETE FROM MetaFieldValue v WHERE v.id IN (" +
    			"SELECT val.id FROM PaymentInformationDTO p inner join p.metaFields val " +
                "WHERE p.id IN (" +
                "SELECT bl.creditCard.id FROM BlacklistDTO bl" + 
                "WHERE bl.company.id = :company AND bl.source = :source))";
        Query query = getSession().createQuery(hql);
        query.setParameter("company", entityId);
        query.setParameter("source", source);
        query.executeUpdate();
    	
    	hql = "DELETE FROM PaymentInformationDTO WHERE id IN (" +
                "SELECT creditCard.id FROM BlacklistDTO " + 
                "WHERE company.id = :company AND source = :source)";
        query = getSession().createQuery(hql);
        query.setParameter("company", entityId);
        query.setParameter("source", source);
        query.executeUpdate();
    	
        hql = "DELETE FROM MetaFieldValue WHERE id IN (" +
                "SELECT creditCard.id FROM BlacklistDTO " + 
                "WHERE company.id = :company AND source = :source)";
        query = getSession().createQuery(hql);
        query.setParameter("company", entityId);
        query.setParameter("source", source);
        query.executeUpdate();
        
        hql = "DELETE FROM MetaFieldValue WHERE id IN (" +
                "SELECT metaFieldValue.id FROM BlacklistDTO " +
                "WHERE company.id = :company AND source = :source)";
        query = getSession().createQuery(hql);
        query.setParameter("company", entityId);
        query.setParameter("source", source);
        query.executeUpdate();

        hql = "DELETE FROM ContactDTO WHERE id IN (" +
                "SELECT contact.id FROM BlacklistDTO " + 
                "WHERE company.id = :company AND source = :source)";
        query = getSession().createQuery(hql);
        query.setParameter("company", entityId);
        query.setParameter("source", source);
        query.executeUpdate();

        hql = "DELETE FROM BlacklistDTO " +
                "WHERE company.id = :company AND source = :source";
        query = getSession().createQuery(hql);
        query.setParameter("company", entityId);
        query.setParameter("source", source);
        int result = query.executeUpdate();

        return result;
    }
}
