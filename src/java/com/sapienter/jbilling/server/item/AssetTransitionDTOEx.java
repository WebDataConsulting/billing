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
package com.sapienter.jbilling.server.item;

import java.io.Serializable;
import java.util.Date;

/**
 * Version of AssetTransitionDTO safe for external communication.
 *
 * @author Gerhard
 * @since 15/04/13
 * @see com.sapienter.jbilling.server.item.db.AssetTransitionDTO
 */
public class AssetTransitionDTOEx implements Serializable {

    private Date createDatetime;
    private Integer previousStatusId;
    private Integer newStatusId;
    private Integer assignedToId;
    private Integer userId;
    private Date startDate;
    private Date endDate;
    private Integer orderLineId;

    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    public Integer getPreviousStatusId() {
        return previousStatusId;
    }

    public void setPreviousStatusId(Integer previousStatusId) {
        this.previousStatusId = previousStatusId;
    }

    public Integer getNewStatusId() {
        return newStatusId;
    }

    public void setNewStatusId(Integer newStatusIs) {
        this.newStatusId = newStatusIs;
    }

    public Integer getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(Integer assignedToId) {
        this.assignedToId = assignedToId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Integer getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(Integer orderLineId) {
        this.orderLineId = orderLineId;
    }
}