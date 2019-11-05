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
package com.sapienter.jbilling.server.item.db;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Date;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.*;

public class ItemTypeDAS extends AbstractDAS<ItemTypeDTO> {


    /**
     * Returns true if the given item type ID is in use.
     *
     * @param typeId type id
     * @return true if in use, false if not
     */
    public boolean isInUse(Integer typeId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("id", typeId))
                .createAlias("items", "item")
                .add(Restrictions.eq("item.deleted", 0)) // item type contains non-deleted items
                .setProjection(Projections.count("item.id"));

        criteria.setComment("ItemTypeDTO.isInUse");

        return (criteria.uniqueResult() != null && ((Long) criteria.uniqueResult()) > 0);
    }

    /**
     * Find all ItemTypes linked to products which are linked to the ItemType identified by typeId
     *
     * @param typeId  ItemType id
     * @return list of ItemType ids
     */
    public List<Integer> findAllTypesLinkedThroughProduct(Integer typeId) {
        Query query = getSession().createQuery("SELECT distinct t.id FROM ItemTypeDTO t " +
                "JOIN t.items it " +
                "WHERE it.id IN " +
                "( SELECT it2.id FROM ItemDTO it2 " +
                " JOIN it2.itemTypes its2 " +
                " WHERE its2.id = :the_id)");
        query.setInteger("the_id", typeId);

        return (query.list());
    }

    /**
     * Returns the internal category for plan subscription items.
     *
     * @param entityId entity id
     * @return plans internal category
     */
    public ItemTypeDTO getCreateInternalPlansType(Integer entityId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass());
        criteria.add(Restrictions.eq("entity.id", entityId))
                .add(Restrictions.eq("description", ServerConstants.PLANS_INTERNAL_CATEGORY_NAME))
                .add(Restrictions.eq("internal", true));

        return (ItemTypeDTO) criteria.uniqueResult();
    }

    /**
     * Returns the category that has the specified description.The search is done case insensitive, for example "calls"
     * and "Calls" are considered the same.
     * @param description Description.
     * @return The category that matches the description or null if no category was found.
     */
    public ItemTypeDTO findByDescription(Integer entityId, String description) {
        Criteria criteria = getSession().createCriteria(ItemTypeDTO.class)
        		.createAlias("entities", "childs", CriteriaSpecification.LEFT_JOIN)
        		.add(Restrictions.eq("childs.id", entityId))
        		.add(Restrictions.eq("description", description).ignoreCase())
        		.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return (ItemTypeDTO) criteria.uniqueResult();
    }

    /**
     * Method added to fix #7890 - Duplicate Global categories are possible.
     * Returns a global category that has the specified description.
     * @param description Description.
     * @return The category that matches the description or null if no category was found.
     */
    public ItemTypeDTO findByGlobalDescription(Integer entityId, String description) {
    	 Criteria criteria = getSession().createCriteria(ItemTypeDTO.class)
    			.createAlias("entity", "e")
         		.add(Restrictions.eq("global", true))
         		.add(Restrictions.eq("e.id", entityId))
         		.add(Restrictions.eq("description", description).ignoreCase())
         		.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
         return (ItemTypeDTO) criteria.uniqueResult();
    }
    
    /**
     * 	Find By entity
     * 
     * @param entityId
     * @return returns all the categories per given entity
     */
	public List<ItemTypeDTO> findByEntityId(Integer entityId) {
		
        Criteria criteria = getSession().createCriteria(ItemTypeDTO.class)
        		.createAlias("entities", "entities", CriteriaSpecification.LEFT_JOIN)
        		.add(Restrictions.eq("entities.id", entityId))
        		.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
	}

    /**
     * Returns immediate children categories for given parent category.
     *
     * @param itemTypeId - parent category id
     * @return returns all the children categories for the given parent category
     */
    public List<ItemTypeDTO> getChildItemCategories(Integer itemTypeId){

        Criteria criteria = getSession().createCriteria(ItemTypeDTO.class)
                .add(Restrictions.eq("parent.id", itemTypeId));

        return criteria.list();
    }

    public List<ItemTypeDTO> getItemCategoriesByPartner(String partner, boolean parentCategoriesOnly) {

        Criteria usedCriteria = createMetaFieldCriteria(partner).getExecutableCriteria(getSession());
        usedCriteria.setProjection(Projections.rowCount());
        boolean used = ((Long) usedCriteria.uniqueResult()) > 0;//avoiding empty in clause

        if (used) {

            DetachedCriteria itemCriteria = DetachedCriteria.forClass(ItemDTO.class);
            itemCriteria.createAlias("metaFields", "fields", CriteriaSpecification.LEFT_JOIN);
            itemCriteria.add(Subqueries.propertyIn("fields.id", createMetaFieldCriteria(partner)));
            itemCriteria.setProjection(Projections.id());

            Criteria itemTypeCriteria = getSession().createCriteria(getPersistentClass(), "p");
            itemTypeCriteria.createAlias("items", "itms", CriteriaSpecification.LEFT_JOIN);
            itemTypeCriteria.add(Restrictions.eq("itms.deleted", 0));
            itemTypeCriteria.add(Subqueries.propertyIn("itms.id", itemCriteria));
            itemTypeCriteria.addOrder(Order.desc("id"));
            itemTypeCriteria.setResultTransformer(CriteriaSpecification.ROOT_ENTITY);

            if (parentCategoriesOnly) {
                itemTypeCriteria.add(Restrictions.isNull("parent"));

//                in case we need to include all parents not just root parents
//                DetachedCriteria existCriteria = DetachedCriteria.forClass(ItemTypeDTO.class, "ip");
//                existCriteria.setProjection(Projections.id());
//                existCriteria.add(Property.forName("ip.parent.id").eqProperty("p.id"));
//                itemTypeCriteria.add(Subqueries.exists(existCriteria));
            }

            return itemTypeCriteria.list();
        } else {
            return new LinkedList<ItemTypeDTO>();
        }
    }

    private DetachedCriteria createMetaFieldCriteria(String partner) {
        DetachedCriteria metaFieldNameCriteria = DetachedCriteria.forClass(MetaField.class);
        metaFieldNameCriteria.add(Restrictions.eq("name", "ccf.partner"));
        metaFieldNameCriteria.add(Restrictions.eq("entityType", EntityType.PRODUCT));
        metaFieldNameCriteria.add(Restrictions.eq("dataType", DataType.ENUMERATION));
        metaFieldNameCriteria.setProjection(Projections.id());

        DetachedCriteria metaFieldValueCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue");
        metaFieldValueCriteria.setProjection(Projections.property("id"));
        metaFieldValueCriteria.add(Subqueries.propertyEq("field.id", metaFieldNameCriteria));
        metaFieldValueCriteria.add(Restrictions.eq("stringValue.value", partner).ignoreCase());
        return metaFieldValueCriteria;
	}

    /**
     * Find the ItemType which has asset management enabled for the given itemId.
     *
     * @param itemId
     * @return
     */
    public ItemTypeDTO findItemTypeWithAssetManagementForItem(int itemId) {
        Criteria itemTypeCriteria = getSession().createCriteria(getPersistentClass(), "p");
        itemTypeCriteria.createAlias("items", "itms");
        itemTypeCriteria.add(Restrictions.eq("itms.id", itemId));
        itemTypeCriteria.add(Restrictions.eq("p.allowAssetManagement", 1));

        return (ItemTypeDTO) itemTypeCriteria.uniqueResult();
    }
    
    /**
     * Get all item categories for the given company its childs and global categories
     */
    public List<ItemTypeDTO> findItemCategories(Integer entity, List<Integer> entities, boolean isRoot) {

        Criteria criteria = getSession().createCriteria(ItemTypeDTO.class)
                .createAlias("entities", "entities", CriteriaSpecification.LEFT_JOIN);

        Disjunction dis = Restrictions.disjunction();
        dis.add(Restrictions.eq("global", true));
        dis.add(Restrictions.in("entities.id", entities));
        if (isRoot) {
            dis.add(Restrictions.eq("entities.parent.id", entity));
        }
        criteria.add(dis)
                .addOrder(Order.asc("id"))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return criteria.list();
    }

    public Boolean isAssociatedToActiveOrder(Integer userId, Integer itemTypeId, Date activeSince, Date activeUntil) {
		Disjunction dis1 = Restrictions.disjunction();
    	if(activeUntil != null) {
    		dis1.add(Restrictions.le("activeSince", activeUntil));
    	}
		
		Disjunction dis2 = Restrictions.disjunction();
		dis2.add(Restrictions.isNull("activeUntil"));
		dis2.add(Restrictions.ge("activeUntil", activeSince));
    	    					
    	Criteria c = getSession().createCriteria(OrderDTO.class).
    			add(Restrictions.eq("deleted", 0)).
    			add(Restrictions.conjunction().
    					add(dis1).
    					add(dis2)).
				createAlias("baseUserByUserId","baseUserByUserId").
				createAlias("lines","lines").
				createAlias("lines.item", "item").
				createAlias("item.itemTypes","types").
				add(Restrictions.eq("types.id", itemTypeId)).
				add(Restrictions.eq("baseUserByUserId.id", userId)).
				setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
    	
    	return c.list().size() > 0;
	}

	/**
	 * Returns ItemTypeDTO with given ID that belongs or is accessible to a given company.
	 * If parentEntityID is not null that the ItemTypeDTO is declared accessible to entityId
	 * if it also belongs to parent company and is declared as global or maybe accessible to
	 * the child company.
	 *
	 * @param itemTypeId - the ID of the ItemTypeDTO
	 * @param entityId - an ID of the calling company (child or parent)
	 * @param parentEntityId - the ID of the parent company of the child company
	 */
	public ItemTypeDTO getById(Integer itemTypeId, Integer entityId, Integer parentEntityId) {

		if(null == itemTypeId || null == entityId) {
			throw new IllegalArgumentException("Arguments itemTypeId and entityId can not be null");
		}

		Criteria criteria = getSession().createCriteria(ItemTypeDTO.class);
		criteria.createAlias("entities", "entities", CriteriaSpecification.LEFT_JOIN);
		criteria.add(Restrictions.idEq(itemTypeId));

		if(null != parentEntityId){
			Conjunction con1 = Restrictions.conjunction();
			con1.add(Restrictions.eq("entity.id", parentEntityId));
			con1.add(Restrictions.eq("global", true));

			Conjunction con2 = Restrictions.conjunction();
			con2.add(Restrictions.eq("entity.id", parentEntityId));
			con2.add(Restrictions.in("entities.id", new Integer[] {entityId}));

			criteria.add(Restrictions.disjunction()
					.add(Restrictions.eq("entity.id", entityId))
					.add(con1)
					.add(con2));
		} else {
			criteria.add(Restrictions.eq("entity.id", entityId));
		}

		return (ItemTypeDTO) criteria.uniqueResult();
	}

}
