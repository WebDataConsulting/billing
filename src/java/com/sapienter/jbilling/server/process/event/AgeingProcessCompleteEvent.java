package com.sapienter.jbilling.server.process.event;

import com.sapienter.jbilling.server.system.event.Event;

/**
 * @author Alexander Aksenov
 * @since 20.09.11
 */
public class AgeingProcessCompleteEvent implements Event {

    private final Integer entityId;

    public AgeingProcessCompleteEvent(Integer entityId) {
        this.entityId = entityId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "Ageing Process Complete Event";
    }

    public String toString() {
        return getName() + " - entity " + entityId;
    }
}
