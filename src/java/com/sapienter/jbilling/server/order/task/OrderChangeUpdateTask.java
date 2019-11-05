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

package com.sapienter.jbilling.server.order.task;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sapienter.jbilling.common.FormatLogger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;

/**
 * This scheduled task will apply order changes to orders if possible.
 * On error all changes will be switched to ERROR state.
 * Each hierarchy changes group is applied in separate DB transaction
 *
 * @author Alexander Aksenov
 * @since 29.07.13
 */
public class OrderChangeUpdateTask extends AbstractCronTask {

    private static final FormatLogger log = new FormatLogger(OrderChangeUpdateTask.class);
    
    public String getTaskName() {
        return "order change update task , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        if(Util.getSysPropBooleanTrue(ServerConstants.PROPERTY_RUN_ORDER_UPDATE)) {
        	_init(context);
	        IOrderSessionBean orderSessionBean = (IOrderSessionBean) Context.getBean(Context.Name.ORDER_SESSION);
	        Date onDate = Util.truncateDate(new Date());
	
	        List<Map<String, Object>> orderChangesSearchResult = new OrderChangeDAS().findApplicableChangesForGrouping(getEntityId(), onDate);
	        List<Collection<Integer>> changeGroups = findOrderChangesGrouped(orderChangesSearchResult);
	        for (Collection<Integer> group : changeGroups) {
		            try {
		                orderSessionBean.applyChangesToOrders(group, onDate, getEntityId());
		            } catch (SessionInternalError ex) {
		                log.error("Unexpected error during changes apply", ex);
		                String errorMessage = ex.getErrorMessages() != null && ex.getErrorMessages().length > 0 ? ex.getErrorMessages()[0] : null;
		                orderSessionBean.markOrderChangesAsApplyError(getEntityId(), group, onDate, null, errorMessage);
		            } catch (Exception ex) {
		                log.error("Error during changes apply to hierarchy", ex);
		                orderSessionBean.markOrderChangesAsApplyError(getEntityId(), group, onDate, null, null);
		            } 
	        }
        } else {
        	log.warn("Failed to trigger OrderChangeUpdate process at %s", context.getFireTime()
                    + ", another process is already running.");
        }
    }

    /**
     * Group order changes by orders hierarchy. Order changes for orders' tree (hierarchy) should be processed together
     * @param orderChangesSearchResult List of prepared for grouping order changes data in format
     *          {changeId: <order_change_id>, orderId: <order_change_order_id>, parentOrderId: <parent_for_change_order>, 
     *          grandParentOrderId: <grand_parent_for_change_order>}
     *          parentOrderId and grandParentOrderId should be null if change order is the root order in hierarchy
     * @return List of order change id groups
     */
    protected List<Collection<Integer>> findOrderChangesGrouped(List<Map<String, Object>> orderChangesSearchResult) {
        Map<Integer, String> ordersHierarchyMap = new HashMap<Integer, String>();
        Map<String, Set<Integer>> hierarchyToChangesMap = new HashMap<String, Set<Integer>>();
        for (Map<String, Object> record : orderChangesSearchResult) {
            Integer changeId = (Integer) record.get("changeId");
            Integer orderId = (Integer) record.get("orderId");
            Integer parentOrderId = (Integer) record.get("parentOrderId");
            Integer grandParentOrderId = (Integer) record.get("grandParentOrderId");
            String key = findOrCreateHierarchyKey(ordersHierarchyMap, orderId, parentOrderId, grandParentOrderId);

            if (!hierarchyToChangesMap.containsKey(key)) {
                hierarchyToChangesMap.put(key, new HashSet<Integer>());
            }
            hierarchyToChangesMap.get(key).add(changeId);
        }
        return new LinkedList<Collection<Integer>>(hierarchyToChangesMap.values());
    }

    /**
     * Find key for given order in hierarchyMap
     * @param hierarchyMap Map of orderIds to hierarchy tree key
     * @param orderId target order id
     * @param parentOrderId parent order id for target order
     * @param gransParentOrderId grand parent order id for target order
     * @return key for hierarchy tree if found
     */
    private String findHierarchyKey(Map<Integer, String> hierarchyMap, Integer orderId, Integer parentOrderId, Integer gransParentOrderId) {
        String key = hierarchyMap.get(orderId);
        if (key == null && parentOrderId != null) {
            key = hierarchyMap.get(parentOrderId);
        }
        if (key == null && gransParentOrderId != null) {
            key = hierarchyMap.get(gransParentOrderId);
        }
        return key;
    }

    /**
     * Find or generate hierarchy tree key for given order. Fill hierarchy map if key was generated
     * @param hierarchyMap Map of orderIds to hierarchy tree key
     * @param orderId target order id
     * @param parentOrderId parent order id for target order
     * @param gransParentOrderId grand parent order id for target order
     * @return key (GUID) for given orde
     */
    private String findOrCreateHierarchyKey(Map<Integer, String> hierarchyMap, Integer orderId, Integer parentOrderId, Integer gransParentOrderId) {
        String key = findHierarchyKey(hierarchyMap, orderId, parentOrderId, gransParentOrderId);
        if (key == null) {
            key = UUID.randomUUID().toString();
            hierarchyMap.put(orderId, key);
        }
        if (parentOrderId != null) {
            hierarchyMap.put(parentOrderId, key);
        }
        if (gransParentOrderId != null) {
            hierarchyMap.put(gransParentOrderId, key);
        }
        return key;
    }
}
