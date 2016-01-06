package com.sapienter.jbilling.server.user.event;

import com.sapienter.jbilling.server.system.event.Event;

public class UserCreatedEvent implements Event {
    Integer userId;
    Integer entityId;
    Integer executorId;

    public UserCreatedEvent(Integer userId, Integer entityId, Integer executorId) {
        this.userId = userId;
        this.entityId = entityId;
        this.executorId = executorId;
    }

    public Integer getUserId() {
        return this.userId;
    }

    public String getName() {
        return "User created event.";
    }

    public Integer getEntityId() {
        return this.entityId;
    }

    public Integer getExecutorId() {
        return this.executorId;
    }
}
