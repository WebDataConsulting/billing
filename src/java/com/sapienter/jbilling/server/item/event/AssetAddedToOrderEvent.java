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
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * Event gets fired when an asset is added to an order.
 *
 * @author Gerhard
 * @since 15/04/13
 */
public class AssetAddedToOrderEvent extends AbstractAssetEvent {

    public AssetAddedToOrderEvent(Integer entityId, AssetDTO asset, UserDTO assignedTo, Integer user) {
        super(entityId, asset, assignedTo, user);
    }

    @Override
    public String getName() {
        return "Asset Added to Order event";
    }
}
