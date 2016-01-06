/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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
 * Created by Fernando G. Morales on 10/24/14.
 */
public class AssetReservationWS implements Serializable {

    private Integer id;
    private Integer userId;
    private Integer creatorId;
    private Integer assetId;
    private Date startDate;
    private Date endDate;

    public AssetReservationWS() {

    }

    public AssetReservationWS(Integer userId, Integer assetId, Integer creatorId, Date startDate, Date endDate) {
        this.userId = userId;
        this.creatorId = creatorId;
        this.assetId = assetId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public Integer getAssetId() {
        return assetId;
    }

    public void setAssetId(Integer assetId) {
        this.assetId = assetId;
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

    @Override
    public String toString() {
        return "AssetReservationWS{" +
                "id=" + id +
                ", userId=" + userId +
                ", creatorId=" + creatorId +
                ", assetId=" + assetId +
                ", startDate='" + startDate +
                ", endDate='" + endDate +
                '}';
    }

}
