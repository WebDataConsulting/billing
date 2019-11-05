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
package com.sapienter.jbilling.server.util.db;

import java.util.List;

import com.sapienter.jbilling.common.FormatLogger;
import org.hibernate.*;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Subqueries;

import com.sapienter.jbilling.server.util.ServerConstants;

public class PreferenceDAS extends AbstractDAS<PreferenceDTO> {

	private static final FormatLogger LOG = new FormatLogger(PreferenceDAS.class);

    private static final String findByType_Row =
        "SELECT a " + 
        "  FROM PreferenceDTO a " + 
        " WHERE a.preferenceType.id = :typeId " +
        "   AND a.foreignId = :foreignId " +
        "   AND a.jbillingTable.name = :tableName ";

    public PreferenceDTO findByType_Row(Integer typeId,Integer foreignId,String tableName) {
        Query query = getSession().createQuery(findByType_Row);
        query.setParameter("typeId", typeId);
        query.setParameter("foreignId", foreignId);
        query.setParameter("tableName", tableName);
        query.setCacheable(true);
        return (PreferenceDTO) query.uniqueResult();
    }

    /**
     * This method is used for caching preferences. The criteria query fetches
     * all preference types from preference_type table with an outer join on preference table.
     * DetachedCriteria is used to select from jbilling_table based on entity table name.
     * 3 fields are selected using Projections: preferenceType.id, preference.value, preferenceType.defaultValue
     * Method is called from PreferenceBL, and while caching into a map, preferenceType.id becomes the map key,
     * if preference.value is null, then preferenceType.defaultValue is used as map value.
     * @param entityId
     * @return an Object[] containing preference type id and preference values (both value set for entity and default)
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getPreferencesByEntity(Integer entityId) {
        
    	DetachedCriteria subCriteria = DetachedCriteria.forClass(JbillingTable.class, "jbillingTable");
    	subCriteria.setProjection(Projections.property("jbillingTable.id"));
    	subCriteria.add(Restrictions.eq("jbillingTable.name", ServerConstants.TABLE_ENTITY));
    	
    	Criteria criteria = getSession().createCriteria(PreferenceTypeDTO.class, "preferenceTypeDto");
    	
    	criteria.createAlias("preferenceTypeDto.preferences", 
			"preference", 
			CriteriaSpecification.LEFT_JOIN, 
			Restrictions.and(
				Restrictions.eq("preference.foreignId", entityId), 
				Subqueries.propertyEq("preference.jbillingTable.id", subCriteria)
			)
		);
    	
    	criteria.setProjection(Projections.projectionList()
    			.add(Projections.property("preferenceTypeDto.id"))
    			.add(Projections.property("preference.value"))
    			.add(Projections.property("preferenceTypeDto.defaultValue")));
    	    	
    	return criteria.list();
    }

	/**
	 * Single object that we will be using to hold lock against.
	 */
	private static final Object LOCK = new Object();

	private static final String PREF_VALUE_HQL =
			"  FROM PreferenceDTO p" +
			" WHERE p.preferenceType.id = :typeId " +
			"   AND p.foreignId = :foreignId ";

	private static final String PREF_VALUE_UPDATE_HQL =
			"  UPDATE PreferenceDTO p" +
					" SET p.value = :value " +
					" WHERE p.preferenceType.id = :typeId " +
					"   AND p.foreignId = :foreignId ";

	/**
	 * This method return the current value of invoice number and increment
	 * it to next invoice number. During read it gets lock over the LOCK
	 * object until record updates. When record updated successfully
	 * then release the lock so that can be read by other thread.
	 */
	public Integer getPreferenceAndIncrement(Integer entityId, Integer typeId) {
		Integer value = null;

		synchronized (LOCK) {
			StatelessSession session = null;
			Transaction tx = null;
			try {
				session = getSessionFactory().openStatelessSession();
				tx = session.beginTransaction();

				Query query = session.createQuery(PREF_VALUE_HQL);
				query.setParameter("typeId", typeId);
				query.setParameter("foreignId", entityId);
	//			query.setLockMode("p", LockMode.UPGRADE);//no pessimistic locking for now
				PreferenceDTO preferenceDTO = (PreferenceDTO) query.uniqueResult();

				//If no record is set then next invoice will be start from 1.
				value = Integer.valueOf(1);
				if (preferenceDTO == null) {
					//stop the generation of the invoice because an invoice number can not be generated
					//here the code does not assume that the invoice numbers should start from 1.
					//also there is a technical difficulty to do this insert in stateless session
					throw new IllegalStateException("The preference for next invoice number must be set for all companies");

				} else if (preferenceDTO.getValue() != null) {
					//the preference existed so just increment and update
					value = preferenceDTO.getIntValue();
					preferenceDTO.setValue(value + 1);

					Query updateQuery = session.createQuery(PREF_VALUE_UPDATE_HQL);
					updateQuery.setParameter("value", preferenceDTO.getValue());
					updateQuery.setParameter("typeId", typeId);
					updateQuery.setParameter("foreignId", entityId);
					updateQuery.executeUpdate();
				}

				//if this explodes for some reason
				tx.commit();
				session.close();
			} catch (RuntimeException e) {
				LOG.debug("Generation of invoice number failed.", e);
				//no matter the exception, try doing the clean up and propagate the exception
				try {
					if (null != tx) {if (tx.isActive() && !tx.wasRolledBack()) tx.rollback();}
					if (null != session) {session.close();}
				} catch (Exception ex) {
					//swallow the attempt to do a clean up
				}
				throw e;
			}
		}
		return value;
    }

}
