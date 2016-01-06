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
package com.sapienter.jbilling.server.item.tasks;

import com.sapienter.jbilling.server.item.AssetTransitionBL;
import com.sapienter.jbilling.server.item.db.AssetTransitionDTO;
import com.sapienter.jbilling.server.item.event.AbstractAssetEvent;
import com.sapienter.jbilling.server.item.event.AssetCreatedEvent;
import com.sapienter.jbilling.server.item.event.AssetUpdatedEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.UserDTO;

import java.util.Date;

/**
 * Listens for {@link AssetUpdatedEvent} events.
 * The task will create an {@link AssetTransitionDTO} if the status of the asset changed.
 * @author Gerhard
 * @since 15/04/13
 */
public class AssetUpdatedTask extends PluggableTask implements IInternalEventsTask {

    private static final Class<Event> events[] = new Class[] {
        AssetUpdatedEvent.class,
        AssetCreatedEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        AssetTransitionBL transitionBL = new AssetTransitionBL();
        if(event instanceof AssetUpdatedEvent) {
            AssetUpdatedEvent assetUpdatedEvent = (AssetUpdatedEvent)event;
            if(assetUpdatedEvent.hasStatusChanged()) {
                transitionBL.create(convert(assetUpdatedEvent));
            }
        } else {
            AssetCreatedEvent assetCreatedEvent = (AssetCreatedEvent)event;
            transitionBL.create(convert(assetCreatedEvent));
        }
    }

    /**
     * Converts the AbstractAssetEvent to an AssetTransitionDTO
     * @param event
     * @return
     */
    private AssetTransitionDTO convert(AbstractAssetEvent event) {
        AssetTransitionDTO dto = new AssetTransitionDTO();
        dto.setAsset(event.getAsset());
        dto.setAssignedTo(event.getAssignedTo());
        dto.setNewStatus(event.getAssetStatus());
        dto.setCreateDatetime(new Date());

        if(event.getUser() != null) {
            dto.setUser(new UserDTO(event.getUser()));
        }

        if(event instanceof AssetUpdatedEvent) {
            dto.setPreviousStatus(((AssetUpdatedEvent)event).getOldStatus());
        }
        return dto;
    }
}
