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

package com.sapienter.jbilling.server.item.event;

import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * Base event used for those linked to an asset.
 *
 * @author Gerhard
 * @since 15/04/13
 */
public abstract class AbstractAssetEvent implements Event {

    private Integer entityId;
    private AssetDTO asset;
    /** user the asset is assigned to */
    private UserDTO assignedTo;
    /** user who made the change */
    private Integer user;

    protected AbstractAssetEvent(Integer entityId, AssetDTO asset, UserDTO assignedTo, Integer user) {
        this.entityId = entityId;
        this.asset = asset;
        this.assignedTo = assignedTo;
        this.user = user;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public AssetDTO getAsset() {
        return asset;
    }

    public UserDTO getAssignedTo() {
        return assignedTo;
    }

    public Integer getUser() {
        return user;
    }

    public AssetStatusDTO getAssetStatus() {
        return asset.getAssetStatus();
    }

    @Override
    public String toString() {
        return "AbstractAssetEvent{" +
                "entityId=" + entityId +
                ", name=" + getName() +
                ", asset=" + asset +
                ", assignedTo=" + (assignedTo != null ? assignedTo.getId() : null) +
                ", user=" + user +
                '}';
    }
}
