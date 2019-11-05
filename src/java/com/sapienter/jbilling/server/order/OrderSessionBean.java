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

package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.client.util.ClientConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.order.validator.OrderHierarchyValidator;
import com.sapienter.jbilling.server.util.ServerConstants;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 *
 * This is the session facade for the orders in general. It is a statless
 * bean that provides services not directly linked to a particular operation
 *
 * @author emilc
 **/
@Transactional( propagation = Propagation.REQUIRED )
public class OrderSessionBean implements IOrderSessionBean {

    private static final FormatLogger LOG = new FormatLogger(OrderSessionBean.class);

    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public void reviewNotifications(Date today)
            throws SessionInternalError {

        try {
            OrderBL order = new OrderBL();
            order.reviewNotifications(today);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public OrderDTO getOrder(Integer orderId) throws SessionInternalError {
        try {
            OrderDAS das = new OrderDAS();
            OrderDTO order = das.find(orderId);
            order.touch();
            return order;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public OrderDTO getOrderEx(Integer orderId, Integer languageId)
            throws SessionInternalError {
        try {
            OrderDAS das = new OrderDAS();
            OrderDTO order = das.find(orderId);
            order.addExtraFields(languageId);
            order.touch();
            das.detach(order);
            Collections.sort(order.getLines(), new OrderLineComparator());
            //LOG.debug("returning order " + order);
            return order;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public OrderDTO setStatus(Integer orderId, Integer statusId,
            Integer executorId, Integer languageId)
            throws SessionInternalError {
        try {
            OrderBL order = new OrderBL(orderId);
            order.setStatus(executorId, statusId);
            OrderDTO dto = order.getDTO();
            dto.addExtraFields(languageId);
            dto.touch();
            return dto;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

     public void delete(Integer id, Integer executorId)
            throws SessionInternalError {
        try {
            // now get the order
            OrderBL bl = new OrderBL(id);
            bl.delete(executorId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

    }

    public OrderPeriodDTO[] getPeriods(Integer entityId, Integer languageId)
            throws SessionInternalError {
        try {
            // now get the order
            OrderBL bl = new OrderBL();
            return bl.getPeriods(entityId, languageId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public OrderPeriodDTO getPeriod(Integer languageId, Integer id)
            throws SessionInternalError {
        try {
            // now get the order
            OrderBL bl = new OrderBL();
            OrderPeriodDTO dto =  bl.getPeriod(languageId, id);
            dto.touch();

            return dto;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void setPeriods(Integer languageId, OrderPeriodDTO[] periods)
            throws SessionInternalError {
        OrderBL bl = new OrderBL();
        bl.updatePeriods(languageId, periods);
    }

    public void addPeriod(Integer entityId, Integer languageId)
            throws SessionInternalError {
        try {
            // now get the order
            OrderBL bl = new OrderBL();
            bl.addPeriod(entityId, languageId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public Boolean deletePeriod(Integer periodId)
            throws SessionInternalError {
        try {
            // now get the order
            OrderBL bl = new OrderBL();
            return Boolean.valueOf(bl.deletePeriod(periodId));
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public OrderDTO addItem(Integer itemID, BigDecimal quantity, OrderDTO order, Integer languageId, Integer userId,
                            Integer entityId) throws SessionInternalError, ItemDecimalsException {

        LOG.debug("Adding item %s q: %s", itemID, quantity);

        OrderBL bl = new OrderBL(order);
        bl.addItem(itemID, quantity, languageId, userId, entityId, order.getCurrencyId(), new Date());
        return order;
    }

    public OrderDTO addItem(Integer itemID, Integer quantity, OrderDTO order, Integer languageId, Integer userId,
                            Integer entityId) throws SessionInternalError, ItemDecimalsException {

        return addItem(itemID, new BigDecimal(quantity), order, languageId, userId, entityId);
    }

    public OrderDTO recalculate(OrderDTO modifiedOrder, Integer entityId)
            throws ItemDecimalsException {

        OrderBL bl = new OrderBL();
        bl.set(modifiedOrder);
        bl.recalculate(entityId);
        return bl.getDTO();
    }

    public Integer createUpdate(Integer entityId, Integer executorId,Integer languageId,
            OrderDTO order,  Collection<OrderChangeDTO> orderChanges, Collection<Integer> deletedChanges) throws SessionInternalError {
        Integer retValue = null;
        try {
            OrderDTO rootOrder = OrderHelper.findRootOrderIfPossible(order);
            // linked set to preserve hierarchy order in collection, from root to child
            LinkedHashSet<OrderDTO> ordersForUpdate = OrderHelper.findOrdersInHierarchyFromRootToChild(rootOrder);

            OrderDTO persistedOrder = null;
            for (OrderDTO updatedOrder : ordersForUpdate) {
                if (updatedOrder.getId() != null) {
                    persistedOrder = new OrderBL(updatedOrder.getId()).getDTO();
                    break;
                }
            }
            List<Integer> ordersForDelete = new LinkedList<Integer>();
            if (persistedOrder != null) {
                for (OrderDTO existedOrder : OrderHelper.findOrdersInHierarchyFromRootToChild(OrderHelper.findRootOrderIfPossible(persistedOrder))) {
                    boolean found = false;
                    for (OrderDTO updatedOrder : ordersForUpdate) {
                        if (existedOrder.getId().equals(updatedOrder.getId())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // add in reverse order: from child to parent
                        ordersForDelete.add(0, existedOrder.getId());
                    }
                }
            }

            Date onDate = com.sapienter.jbilling.common.Util.truncateDate(new Date());
            if (persistedOrder != null) {
                // evict orders hierarchy to update as transient entities
                new OrderDAS().detachOrdersHierarchy(persistedOrder);
            }
            OrderDTO targetOrder = OrderBL.updateOrdersFromDto(persistedOrder, rootOrder);
	        Map<OrderLineDTO, OrderChangeDTO> appliedChanges =
			        OrderChangeBL.applyChangesToOrderHierarchy(targetOrder, orderChanges, onDate, true, entityId);
            // validate final hierarchy
            OrderHierarchyValidator hierarchyValidator = new OrderHierarchyValidator();
            hierarchyValidator.buildHierarchy(targetOrder);
            String error = hierarchyValidator.validate();
            if (error != null) {
                throw new SessionInternalError("Incorrect orders hierarchy: " + error, new String[]{error});
            }

            // linked set to preserve hierarchy order in collection, from root to child
            ordersForUpdate = OrderHelper.findOrdersInHierarchyFromRootToChild(targetOrder);
            // update from root order to child orders
            for (OrderDTO updatedOrder : ordersForUpdate) {
                if (updatedOrder.getId() == null) {
                    OrderBL bl = new OrderBL();
                    List<PricingField> pricingFields = updatedOrder.getPricingFields();
                    bl.processLines(updatedOrder, languageId, entityId, updatedOrder.getBaseUserByUserId().getId(),
                            updatedOrder.getCurrencyId(),
                            updatedOrder.getPricingFields() != null ? PricingField.setPricingFieldsValue(pricingFields.toArray(new PricingField[pricingFields.size()])) : null);
                    retValue = bl.createSingleOrder(entityId, executorId, updatedOrder, appliedChanges);
                    updatedOrder.setId(retValue);
                } else {
                    recalculateAndUpdateOrder(updatedOrder, languageId, entityId, executorId, appliedChanges);
                }
            }

            for (Integer orderForDeleteId : ordersForDelete) {
                OrderDTO orderForDelete = new OrderDAS().find(orderForDeleteId);
                if (orderForDelete.getDeleted() > 0) continue;
                // soft delete of order: delete only if hierarchy will not have errors
                String err = hierarchyValidator.deleteOrder(orderForDelete.getId());
                if (err == null) {
                    err = hierarchyValidator.validate();
                    if (err == null) {
                        OrderBL bl = new OrderBL();
                        bl.setForUpdate(orderForDelete.getId());
                        bl.delete(executorId);
                    } else {
                        // add order back to hierarchy in validator
                        hierarchyValidator.updateOrdersInfo(Arrays.asList(orderForDelete));
                    }
                }
            }
            OrderChangeBL orderChangeBL = new OrderChangeBL();
            //synchronize order changes with database state
            orderChangeBL.updateOrderChanges(entityId, orderChanges, deletedChanges, onDate);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return order.getId();
    }

    public Long getCountWithDecimals(Integer itemId)
            throws SessionInternalError {
        try {
            return new OrderLineDAS().findLinesWithDecimals(itemId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * Apply order changes group on given date. This method will select order changes from db and try to apply them to orders.
     * If success changes of orders will be saved. Otherwise orderChanges will be marked as APPLY_ERROR with appropriate error message
     * @param orderChangeIdsForHierarchy Order change ids for orders hierarchy
     * @param onDate application date
     * @param entityId target entity id
     * @throws SessionInternalError Exception is thrown if error was found during changes apply
     */
    public void applyChangesToOrders(Collection<Integer> orderChangeIdsForHierarchy, Date onDate, Integer entityId)
            throws SessionInternalError {
        List<OrderChangeDTO> orderChanges = new LinkedList<OrderChangeDTO>();
        Set<OrderDTO> ordersForUpdate = new HashSet<OrderDTO>();
        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();
        // select order changes by ids from db, add to list only applicable on given date
        for (Integer changeId : orderChangeIdsForHierarchy) {
            OrderChangeDTO change = orderChangeDAS.find(changeId);
            if (change.getStatus().getId() == ClientConstants.ORDER_CHANGE_STATUS_PENDING
                    && OrderChangeBL.isApplicable(change, onDate)) {
                orderChanges.add(change);
                ordersForUpdate.add(change.getOrder());
            }
        }

        applyOrderChangesToOrders(orderChanges, ordersForUpdate, onDate, entityId, false);
    }

    public void applyOrderChangesToOrders(Collection<OrderChangeDTO> orderChanges, Collection<OrderDTO> ordersForUpdate, Date onDate, Integer entityId,
                                          boolean throwOnError)
            throws SessionInternalError {

        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();

        if (orderChanges.isEmpty() || ordersForUpdate.isEmpty()) return;

        OrderDTO rootOrder = OrderHelper.findRootOrderIfPossible(ordersForUpdate.iterator().next());
        if (rootOrder == null) return;

        // make input entities transient
        new OrderDAS().detachOrdersHierarchy(rootOrder);
        for (OrderChangeDTO change : orderChanges) {
            change.touch();
            orderChangeDAS.detach(change);
            Set<AssetDTO> assets = new HashSet<AssetDTO>();
            for (AssetDTO persistedAsset : change.getAssets()) {
                assets.add(new AssetDTO(persistedAsset));
            }
            change.setAssets(assets);
        }

        OrderChangeBL orderChangeBL = new OrderChangeBL();

        Map<OrderLineDTO, OrderChangeDTO> appliedChanges =
		        OrderChangeBL.applyChangesToOrderHierarchy(rootOrder, orderChanges, onDate, throwOnError, entityId);
        // validate final hierarchy
        OrderHierarchyValidator hierarchyValidator = new OrderHierarchyValidator();
        hierarchyValidator.buildHierarchy(rootOrder);
        String error = hierarchyValidator.validate();
        if (error != null) {
            throw new SessionInternalError("Error in final orders hierarchy after changes apply: " + error, new String[]{error});
        } else {
            LinkedHashSet<OrderDTO> updatedOrders = OrderHelper.findOrdersInHierarchyFromRootToChild(rootOrder);
            updatedOrders.retainAll(ordersForUpdate);
            // find only really changed order, recalculate and update them
            for (OrderDTO order : updatedOrders) {
                boolean reallyUpdated = false;
                for (OrderChangeDTO change : orderChanges) {
                    if (change.getOrder().getId().equals(order.getId())
                            && change.getStatus().getId() != ClientConstants.ORDER_CHANGE_STATUS_APPLY_ERROR
                            && change.getStatus().getId() != ClientConstants.ORDER_CHANGE_STATUS_PENDING) {
                        reallyUpdated = true;
                        break;
                    }
                }
                if (reallyUpdated) {
                    recalculateAndUpdateOrder(
		                    order, ServerConstants.LANGUAGE_ENGLISH_ID,
		                    entityId, null, appliedChanges);
                }
            }
            //synchronize order changes with database state
            orderChangeBL.updateOrderChanges(entityId, orderChanges, new HashSet<Integer>(), onDate);
        }

    }

    /**
     * Log the error during changes apply to orderChange objects
     * @param entityId Entity id
     * @param orderChangeIds Target orderChange Ids
     * @param onDate Changes Application date
     * @param errorCode Error Code
     * @param errorMessage Error Message
     */
    public void markOrderChangesAsApplyError(Integer entityId, Collection<Integer> orderChangeIds, Date onDate, String errorCode, String errorMessage) {
        List<OrderChangeDTO> orderChanges = new LinkedList<OrderChangeDTO>();
        OrderChangeDAS orderChangeDAS = new OrderChangeDAS();
        for (Integer changeId : orderChangeIds) {
            orderChanges.add(orderChangeDAS.find(changeId));
        }
        new OrderChangeBL().updateOrderChangesAsApplyError(entityId, orderChanges, onDate, errorCode, errorMessage);
    }

    /**
     * Recalculate order and update persisted one
     * @param updatedOrder Input order dto
     * @param languageId Language Id
     * @param entityId Entity Id
     * @param executorId Executor Id
     */
    private void recalculateAndUpdateOrder(
		    OrderDTO updatedOrder, Integer languageId, Integer entityId,
		    Integer executorId, Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {
        // start by locking the order
        OrderBL oldOrder = new OrderBL();
        oldOrder.setForUpdate(updatedOrder.getId());
        OrderBL orderBL = new OrderBL();
        // see if the related items should provide info
        List<PricingField> pricingFields = updatedOrder.getPricingFields();
        orderBL.processLines(updatedOrder, languageId, entityId, updatedOrder.getBaseUserByUserId().getId(),
                updatedOrder.getCurrency().getId(),
                updatedOrder.getPricingFields() != null ? PricingField.setPricingFieldsValue(pricingFields.toArray(new PricingField[pricingFields.size()])) : null);

        // recalculate
        orderBL.set(updatedOrder);
        orderBL.recalculate(entityId);

        // update
        oldOrder.update(executorId, updatedOrder, appliedChanges);
    }

}
