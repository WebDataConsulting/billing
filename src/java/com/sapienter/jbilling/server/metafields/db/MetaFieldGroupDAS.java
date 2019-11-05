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
package com.sapienter.jbilling.server.metafields.db;

import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;


/**
 * @author Oleg Baskakov
 * @since 18-Apr-2013
 */

public class MetaFieldGroupDAS extends AbstractDAS<MetaFieldGroup> {

	
    @SuppressWarnings("unchecked")
	public List<MetaFieldGroup> getAvailableFieldGroups(Integer entityId, EntityType entityType){
		        DetachedCriteria query = DetachedCriteria.forClass(MetaFieldGroup.class);
		        CompanyDTO company = new CompanyDTO(entityId);
		        query.add(Restrictions.eq("entity", company));
		        query.add(Restrictions.eq("entityType", entityType));
		        query.add(Restrictions.eq("class", MetaFieldGroup.class));
		        query.addOrder(Order.asc("displayOrder"));
		        return (List<MetaFieldGroup>)getHibernateTemplate().findByCriteria(query);
	}
    
    @SuppressWarnings("unchecked")
    public MetaFieldGroup getGroupByName(Integer entityId, EntityType entityType, String name) {
    	
    	if(name==null||name.trim().length()==0){
    		return null;
    	}
        DetachedCriteria query = DetachedCriteria.forClass(MetaFieldGroup.class);
        query.add(Restrictions.eq("entity.id", entityId));
        query.add(Restrictions.eq("entityType", entityType));
        query.add(Restrictions.eq("description", name));
        query.add(Restrictions.eq("class", MetaFieldGroup.class));
        List<MetaFieldGroup> fields = (List<MetaFieldGroup>)getHibernateTemplate().findByCriteria(query);
        return !fields.isEmpty() ? fields.get(0) : null;
    }

}
