package com.sapienter.jbilling.server.item.event;

import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * Event gets fired when as asset is created.
 *
 * @author Gerhard
 * @since 24/04/13
 */
public class AssetCreatedEvent extends AbstractAssetEvent {

    public AssetCreatedEvent(Integer entityId, AssetDTO asset, UserDTO assignedTo, Integer user) {
        super(entityId, asset, assignedTo, user);
    }

    @Override
    public String getName() {
        return "Asset Updated event";
    }
}
