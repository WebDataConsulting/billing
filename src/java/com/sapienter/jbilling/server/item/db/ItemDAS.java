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

import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.*;

import java.util.List;

public class ItemDAS extends AbstractDAS<ItemDTO> {

    /**
     * Returns a list of all items for the given item type (category) id.
     * If no results are found an empty list will be returned.
     *
     * @param itemTypeId item type id
     * @return list of items, empty if none found
     */
    @SuppressWarnings("unchecked")
    public List<ItemDTO> findAllByItemType(Integer itemTypeId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .createAlias("itemTypes", "type")
                .add(Restrictions.eq("type.id", itemTypeId))
                .add(Restrictions.eq("deleted", 0))
                .addOrder(Order.desc("id"));

        return criteria.list();
    }

    /**
     * Returns a list of all items with item type (category) who's
     * description matches the given prefix.
     *
     * @param prefix prefix to check
     * @return list of items, empty if none found
     */
    @SuppressWarnings("unchecked")
    public List<ItemDTO> findItemsByCategoryPrefix(String prefix) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .createAlias("itemTypes", "type")
                .add(Restrictions.like("type.description", prefix + "%"));

        return criteria.list();
    }    

    public List<ItemDTO> findItemsByInternalNumber(String internalNumber) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("internalNumber", internalNumber));

        return criteria.list();
    }

    public ItemDTO findItemByInternalNumber(String internalNumber, Integer entityId) {

        Integer rootCompanyId = new CompanyDAS().getParentCompanyId(entityId);
        rootCompanyId = rootCompanyId!=null?rootCompanyId:entityId;
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("internalNumber", internalNumber))
                .createAlias("entities", "entities", CriteriaSpecification.LEFT_JOIN)
                .add(Restrictions.disjunction()
                        .add(Restrictions.conjunction().add(Restrictions.eq("entity.id", rootCompanyId)).add(Restrictions.eq("global", true)))
                        .add(Restrictions.eq("entities.id", entityId)))
                .add(Restrictions.eq("deleted", 0));

        return (ItemDTO)criteria.uniqueResult();
    }

    private static final String CURRENCY_USAGE_FOR_ENTITY_SQL =
            "select count(*) from " +
            " item i, " +
            " item_price ipt " +
            " where " +
            "     ipt.item_id = i.id " +
            " and ipt.currency_id = :currencyId " +
            " and i.entity_id = :entityId " +
            " and i.deleted = 0 ";

    public Long findProductCountByCurrencyAndEntity(Integer currencyId, Integer entityId ) {
        Query sqlQuery = getSession().createSQLQuery(CURRENCY_USAGE_FOR_ENTITY_SQL);
        sqlQuery.setParameter("currencyId", currencyId);
        sqlQuery.setParameter("entityId", entityId);
        Number count = (Number) sqlQuery.uniqueResult();
        return Long.valueOf(null == count ? 0L : count.longValue());
    }

    public Long findProductCountByInternalNumber(String internalNumber, Integer entityId, boolean isNew, Integer id) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
        		.createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
                .add(Restrictions.eq("internalNumber", internalNumber))
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("entities.id", entityId));

        if(!isNew)
            criteria.add(Restrictions.ne("id", id));

        return (Long) criteria.setProjection(Projections.rowCount()).uniqueResult();
    }
    
    public List<ItemDTO> findByEntityId(Integer entityId) {
    	Criteria criteria = getSession().createCriteria(ItemDTO.class)
        		.createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
        		.add(Restrictions.eq("entities.id", entityId))
        		.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }
    
    /**
     * Get all items for the given company its childs and global categories
     */
    public List<ItemDTO> findItems(Integer entity, List<Integer> entities, boolean isRoot) {
        Criteria criteria = getSession().createCriteria(ItemDTO.class)
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

    private static final String PRODUCT_VISIBLE_TO_PARENT_COMPANY_SQL =
            "select count(*) from item i " +
                    " left join item_entity_map ie on ie.item_id = i.id where " +
                    " i.id = :itemId and " +
                    " (ie.entity_id = :entityId or i.entity_id = :entityId) and" +
                    " i.deleted = 0";
    
    private static final String PRODUCT_AVAILABLE_TO_PARENT_COMPANY_SQL =
            "select count(*) from item i " +
                    " left join item_entity_map ie on ie.item_id = i.id where " +
                    " i.id = :itemId and " +
                    " (ie.entity_id = :entityId) and" +
                    " i.deleted = 0";

    private static final String PRODUCT_VISIBLE_TO_CHILD_HIERARCHY_SQL =
            "select count(*) from item i "+
                "left outer join item_entity_map icem "+
                "on i.id = icem.item_id "+
                "where i.id = :itemId "+
                "and  i.deleted = 0 "+
                "and  (i.entity_id = :childCompanyId or " +
                " icem.entity_id = :childCompanyId or " +
                "((icem.entity_id = :parentCompanyId or icem.entity_id is null) and " +
                "i.global = true));";

    public boolean isProductVisibleToCompany(Integer itemId, Integer entityId, Integer parentId) {
        if (null == parentId) {
            //this means that the entityId is root so the
            //product must be defined for that company
            SQLQuery query = getSession().createSQLQuery(PRODUCT_VISIBLE_TO_PARENT_COMPANY_SQL);
            query.setParameter("itemId", itemId);
            query.setParameter("entityId", entityId);
            Number count = (Number) query.uniqueResult();
            return null != count && count.longValue() > 0;
        } else {
            //check if the product is visible to either the parent or the child company
            SQLQuery query = getSession().createSQLQuery(PRODUCT_VISIBLE_TO_CHILD_HIERARCHY_SQL);
            query.setParameter("itemId", itemId);
            query.setParameter("parentCompanyId", parentId);
            query.setParameter("childCompanyId", entityId);
            Number count = (Number) query.uniqueResult();
            return null != count && count.longValue() > 0;
        }
    }

    public boolean isProductAvailableToCompany(Integer itemId, Integer entityId) {

        Integer rootCompanyId = new CompanyDAS().getParentCompanyId(entityId);
        rootCompanyId = rootCompanyId != null ? rootCompanyId : entityId;
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("id", itemId))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("entities", "entities", CriteriaSpecification.LEFT_JOIN)
                .add(Restrictions.or(
                    Restrictions.eq("global", true),
                    Restrictions.eq("entities.id", entityId))
                ).add(Restrictions.or(
                    Restrictions.eq("entity.id", rootCompanyId),
                    Restrictions.eq("entity.id", entityId))
                );

        List result = criteria.list();
        return result != null && result.size() > 0;
    }
    
	/**
	 * Returns true if the customer is subscribed to plan founded by the given
	 * item id.
	 * 
	 * @param userId
	 *            user id of the customer
	 * @param itemId
	 *            plan id
	 * @return true if customer is subscribed to the plan, false if not.
	 */
	public boolean isSubscribedByItem(Integer userId, Integer itemId) {
		String queryIsSubscriberByItem = ""
				+ "SELECT * FROM order_line ol, plan p, base_user u, purchase_order po, order_status os "
				+ "WHERE p.item_id = ol.item_id and po.id = ol.order_id and u.id = po.user_id and po.status_id = os.id "
				+ "and p.id = (SELECT id FROM plan WHERE item_id = :item_id) "
				+ "and u.id = :user_id " + "and ol.deleted = 0 "
				+ "and po.deleted = 0 " + "and po.period_id <> 1 "
				+ "and os.order_status_flag <> 1";
		Query query = getSession().createSQLQuery(queryIsSubscriberByItem)
				.setParameter("user_id", userId)
				.setParameter("item_id", itemId);
		List result = query.list();
		return result != null && result.size() > 0;
	}

}
