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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.SQLQuery;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class CompanyDAS extends AbstractDAS<CompanyDTO> {
    
    @SuppressWarnings({"unchecked", "deprecation"})
	public List<CompanyDTO> findEntities() {
        return getSession().createCriteria(CompanyDTO.class).list();
    }
    
    @SuppressWarnings({"unchecked", "deprecation"})
	public List<CompanyDTO> findChildEntities(Integer parentId) {
    	return getSession().createCriteria(CompanyDTO.class)
                .add(Restrictions.eq("parent.id", parentId)).list();
    }

    public CompanyDTO findRootFromSource(Integer companyId) {
        CompanyDTO current= findNow(companyId);
        Integer parentId = null;
        if (null != current ) {
            if (null != current.getParent()) {
                parentId = current.getParent().getId();
            } else {
                parentId = current.getId();
            }
        }
        return null != parentId ? findNow(parentId) : null;
    }

    public List<CompanyDTO> getHierachyEntities(Integer entityId) {
        List<CompanyDTO> allEntities = new ArrayList<>();
        CompanyDTO current= findNow(entityId);
        if (null != current ) {

            Integer parentId= null;
            if ( null != current.getParent() ) {
                parentId= current.getParent().getId();
            } else {
                parentId= current.getId();
                allEntities.add(current);
            }

            allEntities.addAll(getChildEntitiesIds(parentId).stream().map(this::findNow).collect(Collectors.toList()));
        }
        return allEntities;
    }
    
    @SuppressWarnings({"unchecked", "deprecation"})
	public List<Integer> getChildEntitiesIds(Integer parentId) {
    	return getSession().createCriteria(CompanyDTO.class)
                .add(Restrictions.eq("parent.id", parentId))
                .setProjection(Projections.id())
                .list();
    }

    public Integer getParentCompanyId(Integer entityId) {
        SQLQuery query = getSession().createSQLQuery(
                "select parent_id from entity where id = :entityId");
        query.setParameter("entityId", entityId);
        return (Integer) query.uniqueResult();
    }
    
    public boolean isRoot(Integer entityId){
    	CompanyDTO entity = find(entityId);
    	
    	if(entity == null) {
    		return false;
    	}
    	
    	if(entity.getParent() == null){
    		return true;
    	}
    	
    	// if it has some child entities then its root
    	List<CompanyDTO> childs = findChildEntities(entityId);
    	if(childs != null && childs.size() > 0) {
    		return true;
    	}
    	// this entity is consistent to be a non root
    	return false;
    }

    private Criteria _allHierarchyEntities (CompanyDTO entity) {
        CompanyDTO parentEntity = entity.getParent();
        @SuppressWarnings("deprecation")
        Criteria searchCriteria = getSession().createCriteria(CompanyDTO.class);
        Criterion itself = Restrictions.eq("id", entity.getId());
        Criterion findByParent = Restrictions.eq("parent.id",
                (parentEntity == null) ? entity.getId() : parentEntity.getId());
        return searchCriteria
                .add(Restrictions.or(itself, findByParent));
    }

    @SuppressWarnings("unchecked")
    public List<CompanyDTO> findAllHierarchyEntities (Integer entityId) {
        CompanyDTO entity = find(entityId);
        if(entity == null) {
            return Collections.emptyList();
        }
        return _allHierarchyEntities(entity)
                .list();
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findAllHierarchyEntitiesIds (Integer entityId) {
        CompanyDTO entity = find(entityId);
        if(entity == null) {
            return Collections.emptyList();
        }
        return _allHierarchyEntities(entity)
                .setProjection(Projections.id())
                .list();
    }

    public List<Integer> findAllCurrentAndChildEntities( Integer entityId) {
        CompanyDTO entity = find(entityId);
        if(entity == null) {
            return Collections.emptyList();
        }
        List<Integer> retVal= getChildEntitiesIds(entityId);
        retVal.add(entityId);

        return retVal;
    }

}
