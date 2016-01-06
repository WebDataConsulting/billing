package jbilling

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.order.OrderChangeWS
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.util.IMigrationServicesSessionBean

import javax.jws.WebMethod
import javax.jws.WebResult

/*
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

/**
 * Created by marcomanzi on 3/18/14.
 * This services will be used from the migration tools.
 */
class MigrationService implements IMigrationServicesSessionBean {

    def IMigrationServicesSessionBean migrationServicesSession

    static transactional = true

    static expose = ['cxfjax']

    /**
    * Retrieve all customer metafield for the entityId and the accountTypeId given
    * @param entityId
    * @param accountType
    * @return
    */
    @WebResult
    @WebMethod
    def List<MetaFieldValueWS> retrieveMetafieldForCustomer(Integer entityId, Integer accountTypeId) {
        return migrationServicesSession.retrieveMetafieldForCustomer(entityId, accountTypeId)
    }
    /**
     * Retrieve the accountType Id with the description given (accountType)
     * @param entityId
     * @param accountTypeDescription
     * @return
     */
    @WebResult
    @WebMethod
    def Integer getAccountTypeIdByDescription(Integer entityId, String accountTypeDescription) {
        return migrationServicesSession.getAccountTypeIdByDescription(entityId, accountTypeDescription)
    }

    /**
    * Retrieve a User with the metafield with Name in input and value in input
    * @param entityId
    * @param metaFieldName
    * @param metaFieldValue
    * @return UserWS
    */
    @WebResult
    @WebMethod
    UserWS retrieveUserWSByMetaField(Integer entityId, String metaFieldName, String metaFieldValue) {
        return migrationServicesSession.retrieveUserWSByMetaField(entityId, metaFieldName, metaFieldValue)
    }

    /*
    * Retrieve a User with the externalUserId Like the tmsUserId
    * @param tmsUserId
    * @return UserWS
    */
    @WebResult
    @WebMethod
    UserWS findUserBy(Integer tmsUserId) {
        return migrationServicesSession.findUserBy(tmsUserId)
    }

    @WebResult
    @WebMethod
    @Override
    Integer createChildOrderForUser(OrderWS order, OrderChangeWS[] orderChanges, Integer userId) {
        return migrationServicesSession.createChildOrderForUser(order, orderChanges, userId)
    }

    @WebResult
    @WebMethod
    @Override
    Integer findParentOrder(Integer childOrderId) {
        return migrationServicesSession.findParentOrder(childOrderId)
    }

    @WebResult
    @WebMethod
    void updateAssetTransitionWithDatesFor(int assetId, int orderLineId, Date startDate, Date endDate) {
        migrationServicesSession.updateAssetTransitionWithDatesFor(assetId, orderLineId, startDate, endDate)
    }
}
