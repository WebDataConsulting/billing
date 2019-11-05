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

import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.util.ServerConstants;


;
import org.hibernate.*;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class UserDAS extends AbstractDAS<UserDTO> {
    private static final FormatLogger LOG = new FormatLogger(UserDAS.class);

     private static final String findInStatusSQL =
         "SELECT a " +
         "  FROM UserDTO a " +
         " WHERE a.userStatus.id = :status " +
         "   AND a.company.id = :entity " +
         "   AND a.deleted = 0" ;

     private static final String findNotInStatusSQL =
         "SELECT a " +
         "  FROM UserDTO a " +
         " WHERE a.userStatus.id <> :status " +
         "   AND a.company.id = :entity " +
         "   AND a.deleted = 0";

     private static final String findAgeingSQL =
         "SELECT a " +
         "  FROM UserDTO a " +
         " WHERE a.userStatus.id > " + UserDTOEx.STATUS_ACTIVE +
         "   AND a.customer.excludeAging = 0 " +
         "   AND a.company.id = :entity " +
         "   AND a.deleted = 0";

     private static final String CURRENCY_USAGE_FOR_ENTITY_SQL =
             "SELECT count(*) " +
             "  FROM UserDTO a " +
             " WHERE a.currency.id = :currency " +
             "	  AND a.company.id = :entity "+
             "   AND a.deleted = 0";
     
     private static final String FIND_CHILD_LIST_SQL =
    	        "SELECT u.id " +
    	        "FROM UserDTO u " +
    	        "WHERE u.deleted=0 and u.customer.parent.baseUser.id = :parentId";


	public List<Integer> findChildList(Integer userId) {
		Query query = getSession().createQuery(FIND_CHILD_LIST_SQL);
		query.setParameter("parentId", userId);
		
		return query.list();
	}

     public Long findUserCountByCurrencyAndEntity(Integer currencyId, Integer entityId){
         Query query = getSession().createQuery(CURRENCY_USAGE_FOR_ENTITY_SQL);
         query.setParameter("currency", currencyId);
         query.setParameter("entity", entityId);

         return (Long) query.uniqueResult();
     }

    private static final String findCurrencySQL =
          "SELECT count(*) " +
          "  FROM UserDTO a " +
          " WHERE a.currency.id = :currency "+
          "   AND a.deleted = 0";

    public UserDTO findRoot(String username) {
        if (username == null || username.length() == 0) {
            LOG.error("can not find an empty root: %s", username);
            return null;
        }
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(UserDTO.class)
            .add(Restrictions.eq("userName", username))
            .add(Restrictions.eq("deleted", 0))
            .createAlias("roles", "r")
                .add(Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_ROOT));

        criteria.setCacheable(true); // it will be called over an over again

        return (UserDTO) criteria.uniqueResult();
    }

    public UserDTO findWebServicesRoot(String username) {
        if (username == null || username.length() == 0) {
            LOG.error("can not find an empty root: %s", username);
            return null;
        }
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(UserDTO.class)
            .add(Restrictions.eq("userName", username))
            .add(Restrictions.eq("deleted", 0))
            .createAlias("roles", "r")
            .add(Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_ROOT));

        criteria.setCacheable(true); // it will be called over an over again

        return (UserDTO) criteria.uniqueResult();
    }

    public UserDTO findByUserId(Integer userId, Integer entityId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("id", userId))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("company", "e")
                .add(Restrictions.eq("e.id", entityId))
                .add(Restrictions.eq("e.deleted", 0));

        return (UserDTO) criteria.uniqueResult();
    }

    public UserDTO findByUserName(String username, Integer entityId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("userName", username).ignoreCase())
                .add(Restrictions.eq("deleted", 0))
                .createAlias("company", "e")
                    .add(Restrictions.eq("e.id", entityId))
                    .add(Restrictions.eq("e.deleted", 0));

        return (UserDTO) criteria.uniqueResult();
    }

    public List<UserDTO> findByEmail(String email, Integer entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("deleted", 0)) 
                .createAlias("company", "e")
                .add(Restrictions.eq("e.id", entityId))
                .createAlias("contact", "c")
                .add(Restrictions.eq("c.email", email).ignoreCase());

        return criteria.list();
    }

    public List<UserDTO> findInStatus(Integer entityId, Integer statusId) {
        Query query = getSession().createQuery(findInStatusSQL);
        query.setParameter("entity", entityId);
        query.setParameter("status", statusId);
        return query.list();
    }

    public List<UserDTO> findNotInStatus(Integer entityId, Integer statusId) {
        Query query = getSession().createQuery(findNotInStatusSQL);
        query.setParameter("entity", entityId);
        query.setParameter("status", statusId);
        return query.list();
    }

    public List<UserDTO> findAgeing(Integer entityId) {
        Query query = getSession().createQuery(findAgeingSQL);
        query.setParameter("entity", entityId);
        return query.list();
    }

    public boolean exists(Integer userId, Integer entityId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.idEq(userId))
                .createAlias("company", "company")
                .add(Restrictions.eq("company.id", entityId))
                .setProjection(Projections.rowCount());

        return (criteria.uniqueResult() != null && ((Long) criteria.uniqueResult()) > 0);
    }

    public Long findUserCountByCurrency(Integer currencyId){
        Query query = getSession().createQuery(findCurrencySQL);
        query.setParameter("currency", currencyId);
        return (Long) query.uniqueResult();
    }

    public List<UserDTO> findAdminUsers(Integer entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("roles", "r")
                .add(Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_ROOT));

        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public ScrollableResults findUserIdsWithUnpaidInvoicesForAgeing(Integer entityId) {
        DetachedCriteria query = DetachedCriteria.forClass(UserDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("customer", "customer", CriteriaSpecification.INNER_JOIN)
                .add(Restrictions.eq("customer.excludeAging", 0))
                .createAlias("invoices", "invoice", CriteriaSpecification.INNER_JOIN)  // only with invoices
                .add(Restrictions.eq("invoice.isReview", 0))
                .add(Restrictions.eq("invoice.deleted", 0))
                .createAlias("invoice.invoiceStatus", "status", CriteriaSpecification.INNER_JOIN)
                .add(Restrictions.ne("status.id", ServerConstants.INVOICE_STATUS_PAID))
                .setProjection(Projections.distinct(Projections.property("id")));
        if (entityId != null) {
            query.add(Restrictions.eq("company.id", entityId));
        }
        // added order to get all ids in ascending order
        query.addOrder(Order.asc("id"));

        Criteria criteria = query.getExecutableCriteria(getSession());
        return criteria.scroll();
    }

    public List<UserDTO> findByMetaFieldValueIds(Integer entityId, List<Integer> valueIds){
        Criteria criteria = getSession().createCriteria(UserDTO.class, "user");
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.createAlias("user.customer", "customer");
        criteria.createAlias("customer.metaFields", "values");
        criteria.add(Restrictions.in("values.id", valueIds));
        return criteria.list();
    }
    
    public List<UserDTO> findByAitMetaFieldValueIds(Integer entityId, List<Integer> valueIds){
        Criteria criteria = getSession().createCriteria(UserDTO.class, "user");
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.createAlias("user.customer", "customer");
        criteria.createAlias("customer.customerAccountInfoTypeMetaFields", "cmfs");
        criteria.createAlias("cmfs.metaFieldValue", "value");
        criteria.add(Restrictions.in("value.id", valueIds));
        return criteria.list();
    }

    public UserDTO findByMetaFieldNameAndValue(Integer entityId, String metaFieldName, String metaFieldValue){
        Criteria criteria = getSession().createCriteria(UserDTO.class, "user");
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.createAlias("user.customer", "customer");
        criteria.createAlias("customer.customerAccountInfoTypeMetaFields", "cmfs");
        criteria.createAlias("cmfs.metaFieldValue", "value");
        criteria.createAlias("value.field", "metaField");
        criteria.add(Restrictions.sqlRestriction("string_value =  ?", metaFieldValue, Hibernate.STRING));
        criteria.add(Restrictions.eq("metaField.name", metaFieldName));
        return (UserDTO) criteria.uniqueResult();
    }

    /**
     * Returns the entity ID for the user. Executes really
     * fast and does not use any joins.
     */
    public Integer getUserCompanyId(Integer userId){
        SQLQuery query = getSession().createSQLQuery("select entity_id from base_user where id = :userId");
        query.setParameter("userId", userId);
        return (Integer) query.uniqueResult();
    }
    
    public boolean hasSubscriptionProduct(Integer userId) {
    	DetachedCriteria dc = DetachedCriteria.forClass(CustomerDTO.class).
    							createAlias("parent", "parent").
    							createAlias("parent.baseUser", "parentUser").
    			 				add(Restrictions.eq("parentUser.id", userId)).
    			 				createAlias("baseUser", "baseUser").
    			 				setProjection(Projections.property("baseUser.id"));
    	
 		Criteria c = getSession().createCriteria(OrderDTO.class).
 				     			add(Restrictions.eq("deleted", 0)).
 				     			createAlias("baseUserByUserId","user").
 				     			add(Property.forName("user.id").in(dc)).
 				     			
 				 				createAlias("lines","lines").
 				 				createAlias("lines.item", "item").
 				 				createAlias("item.itemTypes", "types").
 				 				add(Restrictions.eq("types.orderLineTypeId", ServerConstants.ORDER_LINE_TYPE_SUBSCRIPTION)).
 				 				setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
 		
 		return c.list().size() > 0;
    }
    
    public boolean isSubscriptionAccount(Integer userId) {
        Criteria c = getSession().createCriteria(OrderDTO.class).
                add(Restrictions.eq("deleted", 0)).
                createAlias("baseUserByUserId", "user").
                add(Restrictions.eq("user.id", userId)).

                createAlias("lines", "lines").
                createAlias("lines.item", "item").
                createAlias("item.itemTypes", "types").
                add(Restrictions.eq("types.orderLineTypeId", ServerConstants.ORDER_LINE_TYPE_SUBSCRIPTION)).
                setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        return c.list().size() > 0;
    }
    
    public void saveUserWithNewPasswordScheme(Integer userId, String newPassword, Integer newScheme, Integer entityId){
    	String hql = "Update UserDTO u set password = :password, encryptionScheme = :newScheme where id = :id and company.id = :entityId";
    	Query query = getSession().createQuery(hql).setString("password", newPassword).setInteger("newScheme", newScheme)
    			.setInteger("id", userId).setInteger("entityId", entityId);
    	query.executeUpdate();
    }

    public List<UserDTO> findUsersInActiveSince(Date activityThresholdDate, Integer entityId) {
        if (null == activityThresholdDate) {
            LOG.error("can not find users on empty date %s for entity id %s",activityThresholdDate, entityId );
            return null;
        }
        // Get a list of users that have not logged in since before the provided date
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.or(Restrictions.and(Restrictions.isNotNull("lastLoginDate"), Restrictions.le("lastLoginDate", activityThresholdDate)), Restrictions.and(Restrictions.isNull("lastLoginDate"), Restrictions.le("createDatetime", activityThresholdDate))))
                .add(Restrictions.eq("entity_id", entityId))
                .add(Restrictions.eq("deleted", 0));

        return criteria.list();
    }

}
