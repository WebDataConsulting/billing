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
package com.sapienter.jbilling.server.order.db;

import java.math.BigDecimal;
import java.util.*;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.DateType;
import org.joda.time.DateMidnight;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class OrderDAS extends AbstractDAS<OrderDTO> {

    /**
     * Returns the newest active order for the given user id and period.
     *
     * @param userId user id
     * @param period period
     * @return newest active order for user and period.
     */
    @SuppressWarnings("unchecked")
    public OrderDTO findByUserAndPeriod(Integer userId, OrderPeriodDTO period, Date activeSince) {
	//we should strip time information
	activeSince= new DateMidnight(activeSince).toDate();

        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.eq("orderPeriod", period))
                //cast as date to compare only the date part, ignore time
                .add(Restrictions.sqlRestriction("cast(active_since as date) = ?", activeSince, DateType.INSTANCE))
                .addOrder(Order.asc("id"))
                .setMaxResults(1);

        return findFirst(criteria);
    }

    /**
     * Returns the newest active order for the given user id and period and parent.
     *
     * @param userId user id
     * @param period period
     * @param parentOrder parentOrder
     * @return newest active order for user and period and parentOrder.
     */
    @SuppressWarnings("unchecked")
    public OrderDTO findByUserAndPeriodAndParentOrder(Integer userId, OrderPeriodDTO period, OrderDTO parentOrder) {
    	 Criteria criteria = getSession().createCriteria(OrderDTO.class)
                 .createAlias("orderStatus", "s")
                 .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                 .add(Restrictions.eq("deleted", 0))
                 .createAlias("baseUserByUserId", "u")
                 .add(Restrictions.eq("u.id", userId))
                 .add(Restrictions.eq("orderPeriod", period))
                  .add(Restrictions.eq("parentOrder", parentOrder))
                 .addOrder(Order.asc("id"))
                 .setMaxResults(1);
    	 
    	 return findFirst(criteria);       	 
    }

    /**
     * Returns the list of linked orders by given Primary Order Id
     *
     * @param primaryOrderId
     * @return List<OrderDTO> - List of linked orders for the given Primary Order
     */
    @SuppressWarnings("unchecked")
    public List<OrderDTO> findByPrimaryOrderId(Integer primaryOrderId) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("parentOrder.id", primaryOrderId))
                .addOrder(Order.asc("id"));

        return criteria.list();
    }

    /**
     * Returns an order by id and that is deleted or not depending on the isDeleted parameter.
     *
     * @param orderId   Id of the order to find.
     * @param isDeleted <b>true</b> if we want to find a deleted order and <b>false</b> if we want a not deleted order.
     * @return Order retrieved by id and deleted true/false.
     */
    public OrderDTO findByIdAndIsDeleted(Integer orderId, boolean isDeleted) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("id", orderId))
                .add(Restrictions.eq("deleted", isDeleted ? 1 : 0))
                .setMaxResults(1);

        return (OrderDTO) criteria.uniqueResult();
    }

    public List<OrderDTO> findOrdersByUserPaged(Integer userId, int maxResults, int offset) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .addOrder(Order.desc("id"))
                .setMaxResults(maxResults)
                .setFirstResult(offset)
                .setComment("findOrdersByUserPaged " + userId + " " + maxResults);
        return criteria.list();
    }

    /**
     * Returns the oldest active order for the given user id that contains an item
     * with the given id and period different to once.
     *
     * @param userId user id
     * @param itemId item id
     * @return newest active order for user and period.
     */
    @SuppressWarnings("unchecked")
    public OrderDTO findRecurringOrder(Integer userId, Integer itemId) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.ne("p.id", ServerConstants.ORDER_PERIOD_ONCE))
                .createAlias("lines", "l")
                .createAlias("l.item", "i")
                .add(Restrictions.eq("i.id", itemId))
                .addOrder(Order.desc("id"))
                .setMaxResults(1);

        return findFirst(criteria);
    }

    public OrderProcessDTO findProcessByEndDate(Integer id, Date myDate) {
        return (OrderProcessDTO) getSession().createFilter(find(id).getOrderProcesses(),
                "where this.periodEnd = :endDate").setDate("endDate",
                        Util.truncateDate(myDate)).uniqueResult();

    }

    /**
     * Finds active recurring orders for a given user
     * @param userId
     * @return
     */
    public List<OrderDTO> findByUserSubscriptions(Integer userId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
		        .add(Restrictions.eq("deleted", 0))
		        .createAlias("baseUserByUserId", "u")
		        	.add(Restrictions.eq("u.id", userId))
	        	.createAlias("orderPeriod", "p")
		        	.add(Restrictions.ne("p.id", ServerConstants.ORDER_PERIOD_ONCE))
	        	.createAlias("orderStatus", "s");

        Criterion ORDER_ACTIVE = Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE);

        Criterion ORDER_FINISHED = Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.FINISHED);
        Criterion UNTIL_FUTURE = Restrictions.gt("activeUntil", new Date());
        LogicalExpression FINISH_IN_FUTURE= Restrictions.and(ORDER_FINISHED, UNTIL_FUTURE);

        LogicalExpression orderActiveOrEndsLater= Restrictions.or(ORDER_ACTIVE, FINISH_IN_FUTURE);

        // Criteria or condition
        criteria.add(orderActiveOrEndsLater);

        return criteria.list();
    }

    /**
     * Finds all active orders for a given user
     * @param userId
     * @return
     */
    public Object findEarliestActiveOrder(Integer userId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                    .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                    .add(Restrictions.eq("u.id", userId))
                .addOrder(Order.asc("nextBillableDay"));

        return findFirst(criteria);
    }

    /**
     * Returns a scrollable result set of orders with a specific status belonging to a user.
     *
     * You MUST close the result set after iterating through the results to close the database
     * connection and discard the cursor!
     *
     * <code>
     *     ScrollableResults orders = new OrderDAS().findByUser_Status(123, 1);
     *     // do something
     *     orders.close();
     * </code>
     *
     * @param userId user ID
     * @param statusId order status to include
     * @return scrollable results for found orders.
     */
    public ScrollableResults findByUser_Status(Integer userId,OrderStatusFlag status) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                    .add(Restrictions.eq("u.id", userId))
                .createAlias("orderStatus", "s")
                	.add(Restrictions.eq("s.orderStatusFlag", status))
                .createAlias("orderPeriod", "p")
                	.addOrder(Order.desc("p.id"));

        return criteria.scroll();
    }

    // used for the web services call to get the latest X orders
    public List<Integer> findIdsByUserLatestFirst(Integer userId, int maxResults) {
        return findIdsByUserLatestFirst(userId, maxResults, 0);
    }

    // used for the web services call to get the latest X orders with offset
    public List<Integer> findIdsByUserLatestFirst(Integer userId, int maxResults, int offset) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                    .add(Restrictions.eq("u.id", userId))
                .setProjection(Projections.id())
                .addOrder(Order.desc("id"))
                .setMaxResults(maxResults)
                .setFirstResult(offset)
                .setComment("findIdsByUserLatestFirst " + userId + " " + maxResults + " " + offset);
        return criteria.list();
    }

    public List<Integer> findIdsByUserAndDate(Integer userId, Date since, Date until) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.ge("createDate", since))
                .add(Restrictions.lt("createDate", until))
                .setProjection(Projections.id())
                .addOrder(Order.desc("id"))
                .setComment("findIdsByUserAndDate " + userId + " " + since + " " + until);
        return criteria.list();
    }


    // used for the web services call to get the latest X orders that contain an item of a type id
    @SuppressWarnings("unchecked")
    public List<Integer> findIdsByUserAndItemTypeLatestFirst(Integer userId, Integer itemTypeId, int maxResults) {
        // I'm a HQL guy, not Criteria
        String hql =
            "select distinct(orderObj.id)" +
            " from OrderDTO orderObj" +
            " inner join orderObj.lines line" +
            " inner join line.item.itemTypes itemType" +
            " where itemType.id = :typeId" +
            "   and orderObj.baseUserByUserId.id = :userId" +
            "   and orderObj.deleted = 0" +
            " order by orderObj.id desc";
        List<Integer> data = getSession()
                                .createQuery(hql)
                                .setParameter("userId", userId)
                                .setParameter("typeId", itemTypeId)
                                .setMaxResults(maxResults)
                                .list();
        return data;
    }

    /**
     * @author othman
     * @return list of active orders
     */
    public List<OrderDTO> findToActivateOrders() {
        Date today = Util.truncateDate(new Date());
        Criteria criteria = getSession().createCriteria(OrderDTO.class);

        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.or(Expression.le("activeSince", today),
                Expression.isNull("activeSince")));
        criteria.add(Restrictions.or(Expression.gt("activeUntil", today),
                Expression.isNull("activeUntil")));

        return criteria.list();
    }

    /**
     * @author othman
     * @return list of inactive orders
     */
    public List<OrderDTO> findToDeActiveOrders() {
        Date today = Util.truncateDate(new Date());
        Criteria criteria = getSession().createCriteria(OrderDTO.class);

        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.or(Expression.gt("activeSince", today),
                Expression.le("activeUntil", today)));

        return criteria.list();
    }

    public BigDecimal findIsUserSubscribedTo(Integer userId, Integer itemId) {
        String hql =
                "select sum(l.quantity) " +
                "from OrderDTO o " +
                "inner join o.lines l " +
                "where l.item.id = :itemId and " +
                "o.baseUserByUserId.id = :userId and " +
                "o.orderPeriod.id != :periodVal and " +
                "o.orderStatus.orderStatusFlag = :status and " +
                "o.deleted = 0 and " +
                "l.deleted = 0";

        BigDecimal result = (BigDecimal) getSession()
                .createQuery(hql)
                .setInteger("userId", userId)
                .setInteger("itemId", itemId)
                .setInteger("periodVal", ServerConstants.ORDER_PERIOD_ONCE)
                .setInteger("status", OrderStatusFlag.INVOICE.ordinal())
                .uniqueResult();

        return (result == null ? BigDecimal.ZERO : result);
    }

    public Integer[] findUserItemsByCategory(Integer userId,
            Integer categoryId) {

        Integer[] result = null;

        final String hql =
                "select distinct(i.id) " +
                "from OrderDTO o " +
                "inner join o.lines l " +
                "inner join l.item i " +
                "inner join i.itemTypes t " +
                "where t.id = :catId and " +
                "o.baseUserByUserId.id = :userId and " +
                "o.orderPeriod.id != :periodVal and " +
                "o.deleted = 0 and " +
                "l.deleted = 0";
        List qRes = getSession()
                .createQuery(hql)
                .setInteger("userId", userId)
                .setInteger("catId", categoryId)
                .setInteger("periodVal", ServerConstants.ORDER_PERIOD_ONCE)
                .list();
        if (qRes != null && qRes.size() > 0) {
            result = (Integer[]) qRes.toArray(new Integer[qRes.size()]);
        }
        return result;
    }

    private static final String FIND_ONETIMERS_BY_DATE_HQL =
            "select o " +
                    "  from OrderDTO o " +
                    " where o.baseUserByUserId.id = :userId " +
                    "   and o.orderPeriod.id = :periodId " +
                    "   and cast(activeSince as date) = :activeSince " +
                    "   and deleted = 0";

    @SuppressWarnings("unchecked")
    public List<OrderDTO> findOneTimersByDate(Integer userId, Date activeSince) {
        Query query = getSession().createQuery(FIND_ONETIMERS_BY_DATE_HQL)
                .setInteger("userId", userId)
                .setInteger("periodId", ServerConstants.ORDER_PERIOD_ONCE)
                .setDate("activeSince", activeSince);

        return query.list();
    }

    /**
     * Find orders by user ID and order notes.
     *
     * @param userId user id
     * @param notes order notes to match
     * @return list of found orders, empty if none
     */
    @SuppressWarnings("unchecked")
    public List<OrderDTO> findByNotes(Integer userId, String notes) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("notes", notes))
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("baseUserByUserId.id", userId));

        return criteria.list();
    }

    /**
     * Find orders by user ID and where notes are like the given string. This method
     * can accept wildcard characters '%' for matching.
     *
     * @param userId user id
     * @param like string to match against order notes
     * @return list of found orders, empty if none
     */
    @SuppressWarnings("unchecked")
    public List<OrderDTO> findByNotesLike(Integer userId, String like) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.like("notes", like))
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("baseUserByUserId.id", userId));

        return criteria.list();
    }

    /**
     * Returns the latest active order for the given user id
     * that was created as an Invoice Overdue Penalty.
     *
     * @param userId user id
     * @param invoiceDto invoice
     * @return penalty order for invoice invoice
     */
    @SuppressWarnings("unchecked")
    public List<OrderDTO>  findPenaltyOrderForInvoice(InvoiceDTO invoice) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", invoice.getUserId()))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.ne("p.id", ServerConstants.ORDER_PERIOD_ONCE))
                //.add(Restrictions.eq("activeSince", invoice.getDueDate()))
                .createAlias("lines", "l")
                .add(Restrictions.eq("l.orderLineType.id", ServerConstants.ORDER_LINE_TYPE_PENALTY))
                .add(Restrictions.ilike("l.description", "Overdue Penalty for Invoice Number ", MatchMode.ANYWHERE))
                .addOrder(Order.desc("id"));
        //.setMaxResults(1);

        return criteria.list();
    }
    
    private static final String CURRENCY_USAGE_FOR_ENTITY_SQL =
    		" select count(*) " +
    		" from OrderDTO dto " +
    		" where dto.orderStatus.orderStatusFlag = :status " +
    		" and dto.currency.id = :currencyId " +
    		" and dto.baseUserByUserId.company.id = :entityId ";
    
    public Long findOrderCountByCurrencyAndEntity(Integer currencyId, Integer entityId ) {
    	Query query = getSession().createQuery(CURRENCY_USAGE_FOR_ENTITY_SQL)
    		  .setParameter("status", OrderStatusFlag.INVOICE)
    		  .setParameter("currencyId", currencyId)
    		  .setParameter("entityId", entityId);
    	return (Long) query.uniqueResult();
    }
    
    public List<OrderDTO> findOrdersByUserAndResellerOrder(Integer userId, Integer resellerOrder) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("resellerOrder", resellerOrder))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .addOrder(Order.desc("id"));
        return criteria.list();
    }
    
    private static final String COUNT_ORDER_BY_ITEM =
    		" select count(*) " +
    		" from OrderDTO dto " +
    		" join dto.lines as l where l.item.id = :itemId " +
    		" and dto.baseUserByUserId.company.parent != null" +
    		" and dto.deleted = 0";
    
    public Long findOrdersOfChildsByItem(Integer itemId) {
    	Query query = getSession().createQuery(COUNT_ORDER_BY_ITEM)
      		  .setParameter("itemId", itemId);
      	return (Long) query.uniqueResult();
    }

    /**
     * Detach all orders in hierarchy from hibernate context.
     * Touch all fields for orders and lines before actual evict persisted entity from hibernate context
     * @param persistedOrder target order for detach
     */
    public void detachOrdersHierarchy(OrderDTO persistedOrder) {
        Set<OrderDTO> processed = new HashSet<OrderDTO>();
        persistedOrder.touch();
        detachOrdersHierarchy(persistedOrder, processed);
    }
    
    private void detachOrdersHierarchy(OrderDTO order, Set<OrderDTO> processedOrders) {
        if (processedOrders.contains(order)) return;
        detach(order);
        processedOrders.add(order);
        if (order.getParentOrder() != null) {
            detachOrdersHierarchy(order.getParentOrder(), processedOrders);
        }
        for (OrderDTO child : order.getChildOrders()) {
            detachOrdersHierarchy(child, processedOrders);
        }
    }

    public boolean orderHasStatus(Integer statusId,Integer entity)
    {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("orderStatus", "o")
                .add(Restrictions.eq("o.id", statusId))
                .add(Restrictions.eq("o.entity.id", entity));
        List<OrderDTO> orderDTOs;
        orderDTOs = criteria.list();
        return !(orderDTOs.isEmpty());
    }

    /**
     * Verfies if sub account of a user has active orders containing a specific product
     *  
     * @param userId
     * @param itemId
     * @param activeSince
     * @param activeUntil
     * @return
     */
    public boolean isSubscribed(Integer userId, Integer itemId, Date activeSince, Date activeUntil) {
    	DetachedCriteria dc = DetachedCriteria.forClass(CustomerDTO.class).
    							createAlias("parent", "parent").
    							createAlias("parent.baseUser", "parentUser").
    			 				add(Restrictions.eq("parentUser.id", userId)).
    			 				createAlias("baseUser", "baseUser").
    			 				setProjection(Projections.property("baseUser.id"));
    	
    	Disjunction dis1 = Restrictions.disjunction();
    	     	if(activeUntil != null) {
    	     		dis1.add(Restrictions.le("activeSince", activeUntil));
    	     	}
    	 		
    	Disjunction dis2 = Restrictions.disjunction();
    	 		dis2.add(Restrictions.isNull("activeUntil"));
    	 		dis2.add(Restrictions.ge("activeUntil", activeSince));
    	 		
 		Criteria c = getSession().createCriteria(OrderDTO.class).
 				     			add(Restrictions.eq("deleted", 0)).
 				     			createAlias("baseUserByUserId","user").
 				     			add(Property.forName("user.id").in(dc)).
 				     			add(Restrictions.conjunction().
 				     					add(dis1).
 				     					add(dis2)).
 				 				createAlias("lines","lines").
 				 				createAlias("lines.item", "item").
 				 				add(Restrictions.eq("item.id", itemId)).
 				 				setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
 		
 		return c.list().size() > 0;
    }
}
