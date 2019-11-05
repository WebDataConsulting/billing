package com.sapienter.jbilling.server.notification;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.Context;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

/**
 *  Notification async message listener
 *
 * @author Panche Isajeski
 * @since 08-Mar-2014
 */
public class NotificationMDB implements MessageListener {

    private final FormatLogger LOG = new FormatLogger(NotificationMDB.class);

    public void onMessage(Message message) {

        try {
            LOG.debug("Received a notification message: ");
            // use a session bean to make sure the processing is done in
            // a transaction
            ObjectMessage objectMessage = (ObjectMessage) message;
            INotificationSessionBean notificationSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
            MessageDTO messageDTO = (MessageDTO) objectMessage.getObject();
            Integer userId = objectMessage.getIntProperty("userId");

            notificationSession.asyncNotify(userId, messageDTO);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

    }
}
