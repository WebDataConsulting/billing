/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.sapienter.jbilling.common.CommonConstants.ORDER_CHANGE_STATUS_APPLY_ERROR;
import static com.sapienter.jbilling.common.CommonConstants.ORDER_CHANGE_STATUS_PENDING;
import static com.sapienter.jbilling.server.util.ServerConstants.LANGUAGE_ENGLISH_ID;

/**
 * @author Alexander Aksenov
 * @since 05.07.13
 */
public class OrderChangeStatusBL {

    private static final FormatLogger LOG = new FormatLogger(OrderChangeStatusBL.class);

    public static OrderChangeStatusDTO createOrderChangeStatus(OrderChangeStatusDTO dto, int entityId) {
        if (dto != null) {
            OrderChangeStatusDAS das = new OrderChangeStatusDAS();
            dto.setId(das.findNextStatusId());
            dto.setCompany(new CompanyDAS().find(entityId));
            OrderChangeStatusDTO status = das.save(dto);
            changeApplyToOrderIfNeeded(status, entityId);
            return status;
        }
        return null;
    }

    public static void updateOrderChangeStatus(OrderChangeStatusDTO dto, int entityId) {
        // predefined statuses can't be updated
        if (dto.getId() == ORDER_CHANGE_STATUS_APPLY_ERROR ||
                dto.getId() == ORDER_CHANGE_STATUS_PENDING) {
            return;
        }
        OrderChangeStatusDAS das = new OrderChangeStatusDAS();
        OrderChangeStatusDTO statusDTO = das.find(dto.getId());
        statusDTO.setApplyToOrder(dto.getApplyToOrder());
        statusDTO.setOrder(dto.getOrder());

//        OrderDAS orderDAS = new OrderDAS();
//        OrderDTO order = orderDAS.find(dto.getOrder());
//        Integer oldStatus = null;
//
//        if (order != null) {
//            oldStatus = order.getOrderStatus().getId();
//        }

        OrderChangeStatusDTO status = das.save(statusDTO);
        changeApplyToOrderIfNeeded(status, entityId);
        das.flush();
        das.detach(status);

//        OrderChangeStatusTransitionEvent event = new OrderChangeStatusTransitionEvent(entityId, order, oldStatus, status.getId());
//        EventManager.process(event);
//
//        LOG.debug("OrExternalProvisioning: generated OrderChangeStatusTransitionEvent for order change status update: %s", dto.getOrder());
    }

    public static void deleteOrderChangeStatus(Integer id, int entityId) {
        OrderChangeStatusDAS das = new OrderChangeStatusDAS();
        OrderChangeStatusDTO statusDTO = das.find(id);
        // predefined statuses can't be deleted
        if (statusDTO.getId() == ORDER_CHANGE_STATUS_APPLY_ERROR ||
                statusDTO.getId() == ORDER_CHANGE_STATUS_PENDING) {
            return;
        }
        boolean applyToOrderWasDeleted = statusDTO.getApplyToOrder().equals(ApplyToOrder.YES);
        statusDTO.setDeleted(1);
        OrderChangeStatusDTO status = das.save(statusDTO);
        das.flush();
        das.detach(status);
        if (applyToOrderWasDeleted) {
            updateApplyToOrderAfterDelete(entityId);
        }
    }

    /**
     * Set one orderChangeStatus as ApplyToOrder.YES if needed after delete of orderChangeStatus.
     * If at least one orderChangeStatus exists for entity, first one by 'order' field will be selected as 'ApplyToOrder.YES' status
     * @param entityId target entity id
     */
    private static void updateApplyToOrderAfterDelete(int entityId) {
        OrderChangeStatusDAS das = new OrderChangeStatusDAS();
        List<OrderChangeStatusDTO> allStatuses = das.findUserOrderChangeStatusesOrdered(entityId);
        if (!allStatuses.isEmpty()) {
            OrderChangeStatusDTO firstStatus = allStatuses.iterator().next();
            firstStatus.setApplyToOrder(ApplyToOrder.YES);
            das.save(firstStatus);
        }
    }

    /**
     * Reset previous 'ApplyToOrder.YES' status to 'ApplyToOrder.NO' if new updated status became 'ApplyToOrder.YES'
     * @param updatedStatus updated OrderChangeStatus
     * @param entityId target entity id
     */
    private static void changeApplyToOrderIfNeeded(OrderChangeStatusDTO updatedStatus, int entityId) {
        OrderChangeStatusDAS das = new OrderChangeStatusDAS();
        List<OrderChangeStatusDTO> allStatuses = das.findUserOrderChangeStatusesOrdered(entityId);
        if (updatedStatus.getApplyToOrder().equals(ApplyToOrder.YES)) {
            for (OrderChangeStatusDTO status : allStatuses) {
                if (status.getId() != updatedStatus.getId() &&
                        status.getApplyToOrder().equals(ApplyToOrder.YES)) {
                    status.setApplyToOrder(ApplyToOrder.NO);
                    das.save(status);
                    return;
                }
            }
        } else if (allStatuses.size() == 1) {
            OrderChangeStatusDTO singleStatus = allStatuses.iterator().next();
            singleStatus.setApplyToOrder(ApplyToOrder.YES);
            das.save(singleStatus);
        }
    }


    public static OrderChangeStatusWS getWS(OrderChangeStatusDTO dto) {
    	
    	 OrderChangeStatusWS ws = new OrderChangeStatusWS();
    	
    	 ws.setId(dto.getId());
         ws.setOrder(dto.getOrder());
         ws.setDeleted(dto.getDeleted());
         ws.setEntityId(dto.getCompany() != null ? dto.getCompany().getId() : null);
         ws.setApplyToOrder(dto.getApplyToOrder());
         ws.setName(dto.getDescription(), LANGUAGE_ENGLISH_ID);
        return ws;
    }

    public static final OrderChangeStatusDTO getDTO(OrderChangeStatusWS ws) {
        OrderChangeStatusDTO dto = new OrderChangeStatusDTO();
        if (ws.getId() != null) {
            dto.setId(ws.getId());
        }
        dto.setApplyToOrder(ws.getApplyToOrder());
        dto.setDeleted(ws.getDeleted());
        dto.setOrder(ws.getOrder());
        return dto;
    }
    /**
     * Check that the description of orderChangeStatus is unique within given language
     * @param entityId Target entity id
     * @param orderChangeStatusId OrderChangeStatus Id owner of description
     * @param languageId Target language id
     * @param description Target description
     * @return True if description is unique. False otherwise
     */
    public static boolean isDescriptionUnique(Integer entityId, Integer orderChangeStatusId, Integer languageId, String description) {
        List<OrderChangeStatusDTO> orderChangeStatuses = new OrderChangeStatusDAS().findOrderChangeStatuses(entityId);
        Set<String> descriptions = new HashSet<String>();
        for (OrderChangeStatusDTO statusDTO : orderChangeStatuses) {
            if (orderChangeStatusId == null || !orderChangeStatusId.equals(statusDTO.getId())) {
                descriptions.add(statusDTO.getDescription(languageId));
            }
        }
        return !descriptions.contains(description);
    }
}
