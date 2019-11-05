package com.sapienter.jbilling.server.user.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.event.AgeingNotificationEvent;
import com.sapienter.jbilling.server.util.Context;
;

/**
 * Provides a mapping between the ageing step(user status Id) and a custom notification message id
 * <p>
 * Sends the notification mapped to the ageing step when the event is triggered during the ageing process
 *
 * @author Panche Isajeski
 * @since 12/12/12
 */
public class UserAgeingNotificationTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(UserAgeingNotificationTask.class);

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
            AgeingNotificationEvent.class
    };

    // notification message for user reactivation
    public static final ParameterDescription PARAMETER_REACTIVATE_NOTIFICATION_ID =
            new ParameterDescription("0", false, ParameterDescription.Type.INT);

    {
        descriptions.add(PARAMETER_REACTIVATE_NOTIFICATION_ID);
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {

        if (event instanceof AgeingNotificationEvent) {

            AgeingNotificationEvent ageingNotificationEvent = (AgeingNotificationEvent) event;
            Integer entityId = ageingNotificationEvent.getEntityId();
            Integer userStatusId = ageingNotificationEvent.getUserStatusId();
            Integer userId = ageingNotificationEvent.getUserId();
            Integer languageId = ageingNotificationEvent.getLanguageId();

            try {

                Integer ageingNotificationId = getNotificationIdForUserStatus(userStatusId, userId);
                if (ageingNotificationId == null) {
                    LOG.warn("User notification message mapping not found for the user status: %s", userStatusId);
                    return;
                }
                // get the correct ageing message
                MessageDTO message = new NotificationBL().getAgeingMessage(
                        entityId,
                        languageId,
                        ageingNotificationId,
                        userId);

                // notify the user with the notification message
                INotificationSessionBean notification = (INotificationSessionBean) Context.getBean(
                        Context.Name.NOTIFICATION_SESSION);
                notification.notify(userId, message);

            } catch (NotificationNotFoundException e) {
                LOG.warn("Failed to send ageing notification. Entity %s does not have an ageing message configured for user status id '%s'.", ageingNotificationEvent.getEntityId(),
                          ageingNotificationEvent.getUserStatusId());
            }
        }

    }

    /**
     *  Retrieves the notification message based on the ageing step(user status) and user
     *  <p>
     *  If no userStatusId is provided(null) consider the user reactivate notification
     *
     * @param userStatusId
     * @param userId
     * @return notification message id; null if no message is applicable
     */
    protected Integer getNotificationIdForUserStatus(Integer userStatusId, Integer userId) {

        // if no user status (ageing step) provided consider the notification for user re-activation
        // if there is user status, retrieve from the parameters
        String ageingNotificationStr = userStatusId == null ?
                parameters.get(PARAMETER_REACTIVATE_NOTIFICATION_ID.getName()) :
                parameters.get(String.valueOf(userStatusId));

        if (ageingNotificationStr != null) {
            try {
                return Integer.valueOf(ageingNotificationStr);
            } catch (NumberFormatException e) {
                LOG.warn("Cannot parse attribute value '%s' as an integer.", ageingNotificationStr);
            }
        }

        return null;
    }
}
