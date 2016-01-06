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

import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeTypeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;

import java.util.*;

/**
 * @author: Alexander Aksenov
 * @since: 21.02.14
 */
public class OrderChangeTypeBL {

    private OrderChangeTypeDAS orderChangeTypeDAS = new OrderChangeTypeDAS();

    public static OrderChangeTypeWS getWS(OrderChangeTypeDTO dto) {
    	
    	OrderChangeTypeWS ws = new OrderChangeTypeWS();
    	ws.setId(dto.getId());
        ws.setEntityId(dto.getEntity() != null ? dto.getEntity().getId() : null);
        ws.setDefaultType(dto.isDefaultType());
        ws.setAllowOrderStatusChange(dto.isAllowOrderStatusChange());
        ws.setName(dto.getName());
        if (dto.getItemTypes() != null) {
            for (ItemTypeDTO itemType : dto.getItemTypes()) {
                ws.getItemTypes().add(itemType.getId());
            }
        }
        if (dto.getOrderChangeTypeMetaFields() != null) {
            Collections.addAll(ws.getOrderChangeTypeMetaFields(), MetaFieldBL.convertMetaFieldsToWS(dto.getOrderChangeTypeMetaFields()));
        }
        return ws;
    }

    public static OrderChangeTypeDTO getDTO(OrderChangeTypeWS ws, Integer entityId) {
        OrderChangeTypeDTO dto = new OrderChangeTypeDTO();
        dto.setId(ws.getId());
        dto.setDefaultType(ws.isDefaultType());
        dto.setAllowOrderStatusChange(ws.isAllowOrderStatusChange());
        dto.setName(ws.getName());
        if (ws.getItemTypes() != null) {
            ItemTypeDAS itemTypeDAS = new ItemTypeDAS();
            for (Integer itemTypeId : ws.getItemTypes()) {
                dto.getItemTypes().add(itemTypeDAS.find(itemTypeId));
            }
        }
        dto.setEntity(new CompanyDAS().find(entityId));
        if (ws.getOrderChangeTypeMetaFields() != null) {
            dto.setOrderChangeTypeMetaFields(MetaFieldBL.convertMetaFieldsToDTO(ws.getOrderChangeTypeMetaFields(), entityId));
        }
        return dto;
    }

    public Integer createUpdateOrderChangeType(OrderChangeTypeDTO dto, Integer entityId) {
        OrderChangeTypeDTO toPersistDto = null;
        if (dto.isDefaultType()) {
            dto.getItemTypes().clear();
        }
        if (dto.getId() != null) {
            toPersistDto = orderChangeTypeDAS.find(dto.getId());
            // Default order change type can't be updated
            if (toPersistDto != null && toPersistDto.getId().equals(Constants.ORDER_CHANGE_TYPE_DEFAULT)) {
                return null;
            }
        }
        Set<MetaField> currentMetaFields = new HashSet<MetaField>();
        if (toPersistDto != null && toPersistDto.getOrderChangeTypeMetaFields() != null) {
            currentMetaFields = toPersistDto.getOrderChangeTypeMetaFields();
        }
        //update the orderChange meta fields
        MetaFieldBL.validateMetaFieldsChanges(dto.getOrderChangeTypeMetaFields(), currentMetaFields);

        Collection<Integer> unusedMetaFieldIds = MetaFieldBL.updateMetaFieldsCollection(dto.getOrderChangeTypeMetaFields(), currentMetaFields);
        deleteUnusedOrderChangeMetaFields(unusedMetaFieldIds);

        if (toPersistDto != null) {
            toPersistDto.setEntity(new CompanyDAS().find(entityId));
            toPersistDto.setAllowOrderStatusChange(dto.isAllowOrderStatusChange());
            toPersistDto.setDefaultType(dto.isDefaultType());
            toPersistDto.getItemTypes().clear();
            toPersistDto.getItemTypes().addAll(dto.getItemTypes());
            toPersistDto.setName(dto.getName());
        } else {
            toPersistDto = dto;
        }
        // set actual collection of meta fields
        toPersistDto.setOrderChangeTypeMetaFields(currentMetaFields);

        toPersistDto = orderChangeTypeDAS.save(toPersistDto);
        return toPersistDto.getId();
    }

    public void delete(Integer orderChangeTypeId, Integer callerCompanyId) {
        OrderChangeTypeDTO dto = orderChangeTypeDAS.find(orderChangeTypeId);
        if (dto != null && dto.getEntity().getId() == callerCompanyId) {
            orderChangeTypeDAS.delete(dto);
        }
    }
    /**
     * This method removes MetaFields, that no longer used by order change. No validation is performed
     * Call this method after removing links to MetaField from other entities in DB
     * @param unusedMetaFieldIds ids of metafields for remove
     */
    private void deleteUnusedOrderChangeMetaFields(Collection<Integer> unusedMetaFieldIds) {
        MetaFieldBL metaFieldBL = new MetaFieldBL();
        //delete metafields not linked to the product anymore
        for(Integer id : unusedMetaFieldIds) {
            metaFieldBL.delete(id);
        }
    }
}
