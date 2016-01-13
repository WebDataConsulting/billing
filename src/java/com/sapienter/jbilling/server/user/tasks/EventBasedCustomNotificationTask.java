/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
You may download the latest source from webdataconsulting.github.io.

 */

package com.sapienter.jbilling.server.user.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.order.event.NewOrderEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.event.NewContactEvent;
import com.sapienter.jbilling.server.util.Context;



/**
 * Event custom notification task.
 *
 * @author: Panche.Isajeski
 * @since: 12/07/12
 */
public class EventBasedCustomNotificationTask extends PluggableTask implements IInternalEventsTask {

    public static final ParameterDescription PARAMETER_NEW_CONTACT_CUSTOM_NOTIFICATION_ID =
            new ParameterDescription("new_contact_notification_id", false, ParameterDescription.Type.INT);
    public static final ParameterDescription PARAMETER_NEW_ORDER_CUSTOM_NOTIFICATION_ID =
            new ParameterDescription("new_order_notification_id", false, ParameterDescription.Type.INT);
    //initializer for pluggable params
    // add as many event - notification parameters
    {
        descriptions.add(PARAMETER_NEW_CONTACT_CUSTOM_NOTIFICATION_ID);
        descriptions.add(PARAMETER_NEW_ORDER_CUSTOM_NOTIFICATION_ID);
    }
    private static final FormatLogger LOG = new FormatLogger(EventBasedCustomNotificationTask.class);
    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
        NewContactEvent.class,
        NewOrderEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
    	
        INotificationSessionBean notificationSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
        if (event instanceof NewContactEvent) {
            fireNewContactEventNotification((NewContactEvent) event, notificationSession);
        }
        if (event instanceof NewOrderEvent) {
            fireNewOrderEventNotification((NewOrderEvent) event, notificationSession);
        }
    }

    private boolean fireNewOrderEventNotification(NewOrderEvent newOrderEvent, INotificationSessionBean notificationSession) {
        if (parameters.get(PARAMETER_NEW_ORDER_CUSTOM_NOTIFICATION_ID.getName()) == null || newOrderEvent.getOrder() == null) {
            return false;
        }

        Integer notificationMessageTypeId = Integer.parseInt((String) parameters
                .get(PARAMETER_NEW_ORDER_CUSTOM_NOTIFICATION_ID.getName()));

        MessageDTO message = null;
        Integer userId = newOrderEvent.getOrder().getUserId();

        try {
            UserBL userBL = new UserBL(userId);
            message = new NotificationBL().getCustomNotificationMessage(
                    notificationMessageTypeId,
                    newOrderEvent.getEntityId(),
                    userId,
                    userBL.getLanguage());

        } catch (NotificationNotFoundException e) {
            LOG.debug(String.format("Custom notification id: %s does not exist for the user id %s ",
                    notificationMessageTypeId, userId));
        }

        if (message == null) {
            return false;
        }

        LOG.debug(String.format("Notifying user: %s for a new contact event", userId));
        notificationSession.notify(userId, message);
        return true;

    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private boolean fireNewContactEventNotification(NewContactEvent newContactEvent,
                                                    INotificationSessionBean notificationSession) {
        if (parameters.get(PARAMETER_NEW_CONTACT_CUSTOM_NOTIFICATION_ID.getName()) == null || newContactEvent.getContactDto()  == null) {
            return false;
        }
        Integer notificationMessageTypeId = Integer.parseInt((String) parameters
                .get(PARAMETER_NEW_CONTACT_CUSTOM_NOTIFICATION_ID.getName()));

        MessageDTO message = null;
        Integer userId = newContactEvent.getContactDto().getUserId();

        try {
            UserBL userBL = new UserBL(userId);
            message = new NotificationBL().getCustomNotificationMessage(
                    notificationMessageTypeId,
                    newContactEvent.getEntityId(),
                    userId,
                    userBL.getLanguage());

        } catch (NotificationNotFoundException e) {
            LOG.debug(String.format("Custom notification id: %s does not exist for the user id %s ",
                    notificationMessageTypeId, userId));
        }

        if (message == null) {
            return false;
        }

        LOG.debug(String.format("Notifying user: %s for a new contact event", userId));
        notificationSession.notify(userId, message);
        return true;
    }
    
}
