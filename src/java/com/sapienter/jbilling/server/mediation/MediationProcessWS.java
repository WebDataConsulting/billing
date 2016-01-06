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

import com.sapienter.jbilling.server.mediation.db.MediationOrderMap;
import com.sapienter.jbilling.server.mediation.db.MediationProcess;
import com.sapienter.jbilling.server.mediation.db.MediationRecordDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MediationProcessWS
 *
 * @author Brian Cowdery
 * @since 21-10-2010
 */
public class MediationProcessWS implements Serializable {

    private Integer id;
    private Integer configurationId;
    private Date startDatetime;
    private Date endDatetime;
    private Integer ordersAffected;   
    private List<Integer> orderIds;
    private List<Integer> recordIds;

    public MediationProcessWS() {
    }

    public MediationProcessWS(MediationProcess dto) {
        this.id = dto.getId();
        this.configurationId = dto.getConfiguration() != null ? dto.getConfiguration().getId() : null;
        this.startDatetime = dto.getStartDatetime();
        this.endDatetime = dto.getEndDatetime();
        this.ordersAffected = dto.getOrdersAffected();

        // order ID's
        this.orderIds = new ArrayList<Integer>(dto.getOrderMap().size());
        for (MediationOrderMap map : dto.getOrderMap())
            this.orderIds.add(map.getOrderId());

        // mediation record ID's
        this.recordIds = new ArrayList<Integer>(dto.getRecords().size());
        for (MediationRecordDTO record : dto.getRecords())
            this.recordIds.add(record.getId());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(Integer configurationId) {
        this.configurationId = configurationId;
    }

    public Date getStartDatetime() {
        return startDatetime;
    }

    public void setStartDatetime(Date startDatetime) {
        this.startDatetime = startDatetime;
    }

    public Date getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(Date endDatetime) {
        this.endDatetime = endDatetime;
    }

    public Integer getOrdersAffected() {
        return ordersAffected;
    }

    public void setOrdersAffected(Integer ordersAffected) {
        this.ordersAffected = ordersAffected;
    }

    public List<Integer> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<Integer> orderIds) {
        this.orderIds = orderIds;
    }

    public List<Integer> getRecordIds() {
        return recordIds;
    }

    public void setRecordIds(List<Integer> recordIds) {
        this.recordIds = recordIds;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MediationProcessWS{");
        sb.append("id=").append(id);
        sb.append(", configurationId=").append(configurationId);
        sb.append(", startDatetime=").append(startDatetime);
        sb.append(", endDatetime=").append(endDatetime);
        sb.append(", ordersAffected=").append(ordersAffected);
        sb.append(", orderIds=").append(orderIds);
        sb.append(", recordIds=").append(recordIds);
        sb.append('}');
        return sb.toString();
    }
}
