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
package com.sapienter.jbilling.server.mediation.db;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
/*
 * Only needed because Order is not JPA, but an entity. Once that table is
 * migrated to JPA, this class will not be necessary (but the table stays)
 */
@Entity
@IdClass(MapPK.class)
@Table(name = "mediation_order_map")
// no cache, this is only a temp class until the order is JPAed
public class MediationOrderMap implements Serializable {

    @Id
    private Integer mediationProcessId;
    
    @Id
    private Integer orderId;

    public Integer getMediationProcessId() {
        return mediationProcessId;
    }

    public void setMediationProcessId(Integer mediationProcessId) {
        this.mediationProcessId = mediationProcessId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

}
