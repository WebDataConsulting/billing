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

package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.security.WSSecured;

import java.io.Serializable;
import java.util.Date;

/**
 * MediationConfigurationWS
 *
 * @author Brian Cowdery
 * @since 21-10-2010
 */
public class MediationConfigurationWS implements WSSecured, Serializable {

    private Integer id;
    private Integer entityId;
    private Integer pluggableTaskId;
    private String name;
    private Integer orderValue;
    private Date createDatetime;
    private Integer versionNum;

    public MediationConfigurationWS() {
    }

    public MediationConfigurationWS(MediationConfiguration dto) {
        this.id = dto.getId();
        this.entityId = dto.getEntityId();
        this.pluggableTaskId = dto.getPluggableTask().getId();
        this.name = dto.getName();
        this.orderValue = dto.getOrderValue();
        this.createDatetime = dto.getCreateDatetime();
        this.versionNum= dto.getVersionNum();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public Integer getPluggableTaskId() {
        return pluggableTaskId;
    }

    public void setPluggableTaskId(Integer pluggableTaskId) {
        this.pluggableTaskId = pluggableTaskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOrderValue() {
        return orderValue;
    }

    public void setOrderValue(Integer orderValue) {
        this.orderValue = orderValue;
    }

    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    public Integer getOwningEntityId() {
        return getEntityId();
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningEntityId()}
     * @return null
     */
    public Integer getOwningUserId() {
        return null;
    }
    
    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @Override
    public String toString() {
        return "MediationConfigurationWS{"
               + "id=" + id
               + ", entityId=" + entityId
               + ", pluggableTaskId=" + pluggableTaskId
               + ", name='" + name + '\''
               + ", orderValue=" + orderValue
               + ", createDatetime=" + createDatetime
               + ", versionNum=" + versionNum
               + '}';
    }
}
