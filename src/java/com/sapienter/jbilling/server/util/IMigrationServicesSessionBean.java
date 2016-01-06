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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.UserDTO;

import java.util.Date;
import java.util.List;

/**
 * Created by marcomanzi on 3/17/14.
 */
public interface IMigrationServicesSessionBean {

    /**
     * Retrieve all customer metafield for the entityId and the accountTypeId given
     * @param entityId
     * @param accountTypeId
     * @return
     */
    List<MetaFieldValueWS> retrieveMetafieldForCustomer(Integer entityId, Integer accountTypeId);

    /**
     * Retrieve the accountType Id with the description given (accountType)
     * @param entityId
     * @param accountTypeDescription
     * @return
     */
    Integer getAccountTypeIdByDescription(Integer entityId, String accountTypeDescription);

    /**
     * Retrieve a User with the metafield with Name in input and value in input
     * @param entityId
     * @param metaFieldName
     * @param metaFieldValue
     * @return UserWS
     */
    UserWS retrieveUserWSByMetaField(Integer entityId, String metaFieldName, String metaFieldValue);


    UserWS findUserBy(Integer tmsUserId);

    /**
     * Create a order by taking the main order from the user (it is used for migration, actually it create the only order that don't have any child)
     * @param order
     * @param orderChanges
     * @param userId
     * @throws SessionInternalError
     * @return
     */
    Integer createChildOrderForUser(OrderWS order, OrderChangeWS[] orderChanges, Integer userId) throws SessionInternalError;

    Integer findParentOrder(Integer childOrderId);

}
