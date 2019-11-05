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

package com.sapienter.jbilling.server.notification;

import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Context;

import org.apache.commons.lang.StringEscapeUtils;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.notification.db.NotificationMessageArchDAS;
import com.sapienter.jbilling.server.notification.db.NotificationMessageArchDTO;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.pluggableTask.NotificationTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.ServerConstants;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.*;
import java.util.List;

@Transactional( propagation = Propagation.REQUIRED )
public class NotificationSessionBean implements INotificationSessionBean {

    private static final FormatLogger LOG = new FormatLogger(NotificationSessionBean.class);


    /**
     * Sends an email with the invoice to a customer.
     * This is used to manually send an email invoice from the GUI
     * @param invoiceId
     * @return
    */
    public Boolean emailInvoice(Integer invoiceId)
            throws SessionInternalError {
        Boolean retValue;
        try {
            InvoiceBL invoice = new InvoiceBL(invoiceId);
            if(invoice.getEntity().isReviewInvoice()) {
                throw new SessionInternalError("Can not send Email as invoice is in review status",
                        new String[]{"InvoiceWS,review.status,invoice.prompt.failure.email.invoice.review.status,"
                                +invoice.getEntity().getId()});
            }
            UserBL user = new UserBL(invoice.getEntity().getBaseUser());
            Integer entityId = user.getEntity().getEntity().getId();
            Integer languageId = user.getEntity().getLanguageIdField();
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getInvoiceEmailMessage(entityId,
                    languageId, invoice.getEntity());
            retValue = notify(user.getEntity(), message);

        } catch (NotificationNotFoundException e) {
            retValue = Boolean.FALSE;
        }

        return retValue;
    }

    /**
     * Sends an email with the Payment information to the customer.
     * This is used to manually send an email Payment notification from the GUI (Show Payments)
     * @param paymentId
     * @return
    */
    public Boolean emailPayment(Integer paymentId)
            throws SessionInternalError {
        Boolean retValue;
        try {
            PaymentBL payment = new PaymentBL(paymentId);
            UserBL user = new UserBL(payment.getEntity().getBaseUser());
            Integer entityId = user.getEntity().getEntity().getId();
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getPaymentMessage(entityId,
                    payment.getDTOEx(user.getEntity().getLanguageIdField()),
                    payment.getEntity().getPaymentResult().getId());
            retValue = notify(user.getEntity(), message);
        } catch (NotificationNotFoundException e) {
            retValue = Boolean.FALSE;
        } catch (Exception e) {
        	LOG.error("Error creating/sending Payment Notification: \n%s", e.getMessage());
            retValue = Boolean.FALSE;
        } 
        
        return retValue;
    }

    public void notify(Integer userId, MessageDTO message)
            throws SessionInternalError {
    	LOG.debug("Entering notify()");

        try {
            UserBL user = new UserBL(userId);
            notify(user.getEntity(), message);
        } catch (Exception e) {
            throw new SessionInternalError("Problems getting user entity" +
                    " for id " + userId + "." + e.getMessage());
        }
    }

    public void asyncNotify(Integer userId, MessageDTO message)
            throws SessionInternalError {
        LOG.debug("Entering notify()");

        try {
            UserBL user = new UserBL(userId);
            asyncNotify(user.getEntity(), message);
        } catch (Exception e) {
            throw new SessionInternalError("Problems getting user entity" +
                    " for id " + userId + "." + e.getMessage());
        }
    }

    /**
     * Sends a notification to a user. Returns true if no exceptions were
     * thrown, otherwise false. This return value could be considered
     * as if this message was sent or not for most notifications (emails).
     */
    public Boolean asyncNotify(UserDTO user, MessageDTO message)
            throws SessionInternalError {
        LOG.debug("Entering notify()");
        Boolean retValue = Boolean.TRUE;
        try {
            // verify that the message is good
            if (message.validate() == false) {
                throw new SessionInternalError("Invalid message");
            }
            // parse this message contents with the parameters
            MessageSection sections[] = message.getContent();
            for (int f=0; f < sections.length; f++) {
                MessageSection section = sections[f];
                section.setContent(NotificationBL.parseParameters(
                        section.getContent(), message.getParameters()));
            }
            retValue = sendMessageWithNotification(user, message, retValue, sections);

        } catch (Exception e) {
            LOG.error("Exception in notify", e);
            throw new SessionInternalError(e);
        }

        return retValue;
    }

   /**
    * Sends a notification to a user asynchronously by posting the notification messages to a JMS queue.
    * It also post a notification messages for the other users that needs to be notified (admin, parents, partner)
    */
    public Boolean notify(UserDTO user, MessageDTO message) throws SessionInternalError {
        try {
            // verify that the message is good
            if (message.validate() == false) {
                throw new SessionInternalError("Invalid message");
            }
            // parse this message contents with the parameters
            MessageSection sections[] = message.getContent();
            for (int f=0; f < sections.length; f++) {
                MessageSection section = sections[f];
                section.setContent(NotificationBL.parseParameters(
                		StringEscapeUtils.unescapeJava(section.getContent()), message.getParameters()));
            }
            LOG.debug("Entering notify()");
            postNotificationMessage(user, message);

            if(isChecked(message.getNotifyAdmin())){
                for (UserDTO admin : new UserDAS().findAdminUsers(user.getEntity().getId())) {
                    postNotificationMessage(admin, message);
                }
            }
            if(isChecked(message.getNotifyPartner())){
                if (user.getPartner() != null) {
                    postNotificationMessage(user.getPartner().getUser(), message);
                }
            }
            CustomerDTO userParent = null;
            if(isChecked(message.getNotifyParent())){
                CustomerDTO customer = user.getCustomer();
                if (customer != null) {
                    userParent = customer.getParent();
                    if (userParent != null) {
                        postNotificationMessage(userParent.getBaseUser(), message);
                    }
                }
            }
            if(isChecked(message.getNotifyAllParents()) && userParent != null){
                CustomerDTO parentOfParent = userParent.getParent();
                while(parentOfParent!=null) {
                    postNotificationMessage(parentOfParent.getBaseUser(), message);
                    parentOfParent = parentOfParent.getParent();
                }
            }

        } catch (Exception e) {
            LOG.error("Exception in notify", e);
            throw new SessionInternalError(e);
        }

        return true;
    }

    private void postNotificationMessage(final UserDTO user, final MessageDTO message) {

        LOG.debug("Entering notify with message");

        JmsTemplate jmsTemplate = (JmsTemplate) Context.getBean(
                Context.Name.JMS_TEMPLATE);
        jmsTemplate.setSessionTransacted(true);

        Destination destination = (Destination) Context.getBean(
                Context.Name.NOTIFICATIONS_DESTINATION);

        jmsTemplate.send(destination, new MessageCreator() {

            public Message createMessage(Session session)
                    throws JMSException {
                ObjectMessage objectMessage = session.createObjectMessage();
                objectMessage.setObject(message);
                objectMessage.setIntProperty("userId", user.getId());
                return objectMessage;
            }
        });
    }

    private Boolean sendMessageWithNotification(UserDTO user, MessageDTO message, Boolean retValue,
                                                MessageSection[] sections)
            throws com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException {
        // now do the delivery with the pluggable tasks
        PluggableTaskManager<NotificationTask> taskManager =
            new PluggableTaskManager<NotificationTask>( user.getEntity().getId(),
                ServerConstants.PLUGGABLE_TASK_NOTIFICATION);

        NotificationTask task = taskManager.getNextClass();
        NotificationMessageArchDAS messageHome = new NotificationMessageArchDAS();
        boolean delivered = false;

        // deliver the message to the first task it can process by medium type(s)
        // continue to the next notification task only if the message was not delivered by the previous
        while (!delivered && executeTask(task, message.getMediumTypes())) {
            NotificationMessageArchDTO messageRecord = messageHome.create(message.getTypeId(), sections, user);
            try {
                delivered = deliverNotification("user", user, message, task);

            } catch (TaskException e) {
                messageRecord.setResultMessage(Util.truncateString(
                        e.getMessage(), 200));
                LOG.error(e);
            }
            task = taskManager.getNextClass();
        }
        return delivered;
    }

    private boolean executeTask(NotificationTask task, List<NotificationMediumType> mediumTypes) {
        if (task == null) return false;
        else if (mediumTypes.size() == NotificationMediumType.values().length) return true;
        else {
            boolean oneMediumIsHandledFromTask = false;
            for (NotificationMediumType mediumType: mediumTypes) {
                oneMediumIsHandledFromTask =
                        oneMediumIsHandledFromTask ||
                        task.mediumHandled().contains(mediumType);
            }
            return oneMediumIsHandledFromTask;
        }
    }

    private boolean isChecked(Integer checkboxValue) {
        return Integer.valueOf(1).equals(checkboxValue);
    }

    private boolean deliverNotification(String userDescription, UserDTO user, MessageDTO message, NotificationTask task) throws TaskException {
        LOG.debug("Sending notification to %s : %s", userDescription, user.getUserName());
        return task.deliver(user, message);
    }

    public MessageDTO getDTO(Integer typeId, Integer languageId,
            Integer entityId) throws SessionInternalError {
        try {
            NotificationBL notif = new NotificationBL();
            MessageDTO retValue = null;
            int plugInSections = notif.getSections(entityId);
            notif.set(typeId, languageId, entityId);
            if (notif.getEntity() != null) {
                retValue = notif.getDTO();
            } else {
                retValue = new MessageDTO();
                retValue.setTypeId(typeId);
                retValue.setLanguageId(languageId);
                MessageSection sections[] =
                        new MessageSection[plugInSections];
                for (int f = 0; f < sections.length; f++) {
                    sections[f] = new MessageSection(new Integer(f + 1), "");
                }
                retValue.setContent(sections);
            }

            if (retValue.getContent().length < plugInSections) {
                // pad any missing sections, due to changes to a new plug-in with more sections
                for (int f = retValue.getContent().length ; f < plugInSections; f++) {
                    retValue.addSection(new MessageSection(new Integer(f + 1), ""));
                }
            } else if (retValue.getContent().length > plugInSections) {
                // remove excess sections
                retValue.setContentSize(plugInSections);
            }


            return retValue;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public Integer createUpdate(MessageDTO dto,
            Integer entityId) throws SessionInternalError {
        try {
            NotificationBL notif = new NotificationBL();

            return notif.createUpdate(entityId, dto);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public String getEmails(Integer entityId, String separator)
            throws SessionInternalError {
        try {
            NotificationBL notif = new NotificationBL();

            return notif.getEmails(separator, entityId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }
}
