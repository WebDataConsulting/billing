package com.sapienter.jbilling.server.user.event;

import com.sapienter.jbilling.server.system.event.Event;

/**
 * Ageing notification event triggered during the ageing notification process
 *
 * @author Panche Isajeski
 * @since 12/12/12
 */
public class AgeingNotificationEvent implements Event {

    private final Integer languageId;
    private final Integer entityId;
    private final Integer userStatusId;
    private final Integer userId;


    public AgeingNotificationEvent(Integer entityId, Integer languageId,
                                   Integer userStatusId, Integer userId) {

        this.entityId = entityId;
        this.languageId = languageId;
        this.userStatusId = userStatusId;
        this.userId = userId;
    }

    @Override
    public String getName() {
        return "User ageing notification";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public Integer getUserStatusId() {
        return userStatusId;
    }

    public Integer getUserId() {
        return userId;
    }
}
