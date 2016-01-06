package com.sapienter.jbilling.server.util;/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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


import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.AssetTransitionBL;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;

import grails.plugin.springsecurity.SpringSecurityService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Created by marcomanzi on 3/17/14.
 */
@Transactional( propagation = Propagation.REQUIRED )
public class MigrationServicesSessionSpringBean implements IMigrationServicesSessionBean{

    private SpringSecurityService springSecurityService;
    private IWebServicesSessionBean webServicesSessionBean;

    public SpringSecurityService getSpringSecurityService() {
        if (springSecurityService == null)
            this.springSecurityService = Context.getBean(Context.Name.SPRING_SECURITY_SERVICE);
        return springSecurityService;
    }

    public void setSpringSecurityService(SpringSecurityService springSecurityService) {
        this.springSecurityService = springSecurityService;
    }

    public void setWebServicesSessionBean(IWebServicesSessionBean webServicesSessionBean) {
        this.webServicesSessionBean = webServicesSessionBean;
    }


    @Override
    public Integer getAccountTypeIdByDescription(Integer entityId, String accountType) {
        List<AccountTypeDTO> accountForEntity = new AccountTypeDAS().findAll(entityId);
        for (AccountTypeDTO accountTypeDTO: accountForEntity) {
            if (accountTypeDTO.getDescription().equals(accountType)) {
                return accountTypeDTO.getId();
            }
        }
        return 0;
    }

    @Override
    public List<MetaFieldValueWS> retrieveMetafieldForCustomer(Integer entityId, Integer accountTypeId) {
        List<EntityType> entities =  new ArrayList<EntityType>();
        entities.add(EntityType.CUSTOMER);
        List<MetaField> availableFieldsList = MetaFieldBL.getAvailableFieldsList(entityId, entities.toArray(new EntityType[1]));
        availableFieldsList.addAll(availableFieldsList);

        List<MetaFieldValueWS> metaFieldValueWS = new ArrayList<MetaFieldValueWS>();
        for (MetaField metaField: availableFieldsList) {
            MetaFieldValueWS metaFieldValue =MetaFieldBL.getWS(metaField.createValue());
            metaFieldValueWS.add(metaFieldValue);
        }
        Map<Integer, List<MetaField>> availableAccountTypeFieldsMap = MetaFieldBL.getAvailableAccountTypeFieldsMap(accountTypeId);
        for (Integer groupId : availableAccountTypeFieldsMap.keySet()) {
            for (MetaField metaField: availableAccountTypeFieldsMap.get(groupId)) {
                MetaFieldValueWS metaFieldValue = MetaFieldBL.getWS(metaField.createValue());
                metaFieldValue.setGroupId(groupId);
                metaFieldValueWS.add(metaFieldValue);
            }
        }

        return metaFieldValueWS;
    }

    public UserWS retrieveUserWSByMetaField(Integer entityId, String metaFieldName, String metaFieldValue) {
        List<AccountTypeDTO> accountsForEntity = new AccountTypeDAS().findAll(entityId);
        List<MetaFieldValueWS> metaFieldValueWS = new ArrayList<MetaFieldValueWS>();
        for (AccountTypeDTO accountType: accountsForEntity) {
            metaFieldValueWS.addAll(retrieveMetafieldForCustomer(entityId, accountType.getId()));
        }
        return new UserBL(new UserDAS().findByMetaFieldNameAndValue(entityId, metaFieldName, metaFieldValue)).getUserWS();
    }

    /**
     * Returns the company ID of the authenticated user account making the web service call.
     *
     * @return caller company ID
     */
    @Transactional(readOnly=true)
    public Integer getCallerCompanyId() {
        CompanyUserDetails details = (CompanyUserDetails) getSpringSecurityService().getPrincipal();
        return details.getCompanyId();
    }

    private void createOrderLine(OrderDTO orderDTO, ItemDTO product) {
        OrderLineDTO orderLineDTO = new OrderLineDTO();
        orderLineDTO.setItem(product);
        orderLineDTO.setDescription(product.getDescription());
        orderLineDTO.setMediated(true);
        orderLineDTO.setEditable(false);
        orderLineDTO.setPurchaseOrder(orderDTO);
        orderLineDTO.setOrderLineType(new OrderLineTypeDAS().find(Constants.ORDER_LINE_TYPE_ITEM));
        OrderLineDTO savedOrderline = new OrderLineDAS().save(orderLineDTO);
        OrderDTO savedOrder = new OrderDAS().save(orderDTO);
    }

    private void setupActiveSinceActiveUntilOnOrder(OrderDTO orderDTO, Date date) {
        if (orderDTO.getActiveSince() == null) {
            orderDTO.setActiveSince(date);
            Calendar c = Calendar.getInstance();
            c.setTime(orderDTO.getActiveSince());
            c.add(Calendar.DATE, 1);
            orderDTO.setActiveUntil(c.getTime());
        }
        if (orderDTO.getActiveUntil() != null) {
            if (orderDTO.getActiveSince().after(date)) {
                orderDTO.setActiveSince(date);
            }
            if (orderDTO.getActiveUntil().before(date)) {
                orderDTO.setActiveUntil(date);
            }
        }
    }

    @Override
    public UserWS findUserBy(Integer clientUserId) {
        /**
         * This method need to be customized for each customer, users imported by another system will probably have a
         * external ID that need to be used to understand where the user imported is in jBilling after the migration
         */
        throw new RuntimeException("Method to implement for each customer");
    }

    @Override
    @Transactional()
    public Integer createChildOrderForUser(OrderWS order, OrderChangeWS[] orderChanges, Integer userId) throws SessionInternalError {
        OrderWS parentOrder = null;
        try {
            parentOrder = webServicesSessionBean.getLatestOrder(userId);
            fixOrderWithParentOrder(order, parentOrder);

            if (parentOrder.getParentOrder() != null) {
                parentOrder =  parentOrder.getParentOrder();
            }
        } catch (SessionInternalError sessionInternalError) {
            throw new SessionInternalError("Can't find the subscription for userId:" + userId);
        }

        int orderCreated = webServicesSessionBean.createUpdateOrder(order, orderChanges);
        OrderWS orderSaved = webServicesSessionBean.getOrder(orderCreated);
        setOrderAsParentChild(parentOrder.getId(), orderSaved.getId());
        return orderSaved.getId();
    }

    private void fixOrderWithParentOrder(OrderWS order, OrderWS parentOrder) {
        order.setCancellationMinimumPeriod(parentOrder.getCancellationMinimumPeriod());
        order.setNextBillableDay(parentOrder.getNextBillableDay());
        if (parentOrder.getActiveUntil() != null && order.getActiveUntil() == null) {
            order.setActiveUntil(parentOrder.getActiveUntil());
            checkOrderDatesAreRight(order);
        }
        if (order.getActiveSince().before(parentOrder.getActiveSince())) {
            order.setActiveSince(parentOrder.getActiveSince());
            checkOrderDatesAreRight(order);
        }
        if (order.getActiveUntil().after(parentOrder.getActiveUntil())) {
            order.setActiveUntil(parentOrder.getActiveUntil());
            checkOrderDatesAreRight(order);
        }
    }

    private void checkOrderDatesAreRight(OrderWS order) {
        if (!order.getActiveUntil().after(order.getActiveSince())) {
            Calendar c = Calendar.getInstance();
            c.setTime(order.getActiveSince());
            c.add(Calendar.DATE, 1);
            order.setActiveUntil(c.getTime());
        }
        if (!order.getActiveSince().before(order.getActiveUntil())) {
            Calendar c = Calendar.getInstance();
            c.setTime(order.getActiveUntil());
            c.add(Calendar.DATE, -1);
            order.setActiveSince(c.getTime());
        }
    }

    @Override
    @Transactional()
    public Integer findParentOrder(Integer childId) {
        OrderDTO childOrder = new OrderDAS().find(childId);
        if (childOrder != null && childOrder.getParentOrder() != null) {
            return new OrderDAS().find(childId).getParentOrder().getId();
        }
        return null;
    }

    private void setOrderAsParentChild(Integer parentId, Integer childId) {
        OrderDAS das = new OrderDAS();
        OrderDTO childDto = das.find(childId);
        OrderDTO parentDto = das.find(parentId);
        childDto.setParentOrder(parentDto);
        parentDto.getChildOrders().add(childDto);
        das.save(parentDto);
        das.save(childDto);
    }

}
