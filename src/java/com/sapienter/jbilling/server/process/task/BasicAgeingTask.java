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

package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.user.event.AgeingNotificationEvent;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.ScrollableResults;
import org.springframework.dao.EmptyResultDataAccessException;

import java.sql.SQLException;
import java.util.*;

;

/**
 * BasicAgeingTask
 *
 * @author Brian Cowdery
 * @since 28/04/11
 */
public class BasicAgeingTask extends PluggableTask implements IAgeingTask {

    private static final FormatLogger LOG = new FormatLogger(BasicAgeingTask.class);
    private final EventLogger eLogger = EventLogger.getInstance();

    /**
     *  Get all users that are about to be aged
     *
     * @param entityId
     * @param ageingDate
     * @return all users eligible for ageing
     */
    public ScrollableResults findUsersToAge(Integer entityId, Date ageingDate) {
        LOG.debug("Reviewing users for entity %s ...", entityId);
        return new UserDAS().findUserIdsWithUnpaidInvoicesForAgeing(entityId);
    }

    /**
     * Review user for the given day, and age if it has an outstanding invoices over
     * the set number of days for an ageing step.
     *
     * @param steps ageing steps
     * @param today today's date
     * @param executorId executor id
     */
    public List<InvoiceDTO> reviewUser(Integer entityId, Set<AgeingEntityStepDTO> steps, Integer userId, Date today, Integer executorId) {
        LOG.debug("Reviewing user for ageing %s ...", userId);

        UserDAS userDas = new UserDAS();
        UserDTO user = userDas.find(userId);

        InvoiceDAS invoiceDas = new InvoiceDAS();
        LOG.debug("Reviewing invoices for user %s", user.getId());

        List<InvoiceDTO> userOverdueInvoices = new ArrayList<InvoiceDTO>();
        for (InvoiceDTO invoice : invoiceDas.findProccesableByUser(user)) {
        	if (!invoice.getDueDate().after(today)) {
	            AgeingEntityStepDTO ageingEntityStepDTO = ageUser(steps, user, invoice, today, executorId);
	            if (ageingEntityStepDTO != null) {
	                userOverdueInvoices.add(invoice);
	            }
        	}
        }
        return userOverdueInvoices;
    }

    /**
     * Moves a user one step forward in the ageing process.The
     * user will only be moved if enough days have passed from their overdue invoice due date
     * Return the next ageing step
     *
     * @param steps ageing steps
     * @param user user to age
     * @param today today's date
     * @return the resulting ageing step for the user after ageing
     */
    public AgeingEntityStepDTO ageUser(Set<AgeingEntityStepDTO> steps, UserDTO user, InvoiceDTO unpaidInvoice, Date today, Integer executorId) {
        if (!InvoiceBL.isInvoiceBalanceEnoughToAge(unpaidInvoice, user.getEntity().getId())) {
            LOG.debug("Wants to age user: %s but invoice balance is not enough to age: %s", user.getId(), unpaidInvoice.getId());
            return null;
        }
    	
        LOG.debug("Ageing user %s for unpaid invoice: %s", user.getId(), unpaidInvoice.getId());
        UserStatusDTO nextStatus = null;
        AgeingEntityStepDTO ageingStep = null;

        List<AgeingEntityStepDTO> ageingSteps = new LinkedList<AgeingEntityStepDTO>(steps);
        Date todayTruncated = Util.truncateDate(today);

        for (AgeingEntityStepDTO step : ageingSteps) {
            // run this step

            if (isAgeingRequired(user, unpaidInvoice, step.getDays(), todayTruncated)) {
                // possible multiple runs at a day, check status
                if (!isUserAlreadyPassAgeingStep(user.getStatus(), step, ageingSteps)) {
                    ageingStep = step;
                    nextStatus = step.getUserStatus();
                    LOG.debug("User: %s needs to be aged to '%s'", user.getId(), getStatusDescription(nextStatus));

                    //only 1 step per day
                    break;
                }
            }
        
        }

        // set status
        if (nextStatus != null) {
            setUserStatus(user, nextStatus, today, null);

        } else {
            LOG.debug("Next status of user %s  is null, no further ageing steps are available.", user.getId());
            eLogger.warning(user.getEntity().getId(),
                            user.getUserId(),
                            user.getUserId(),
                            EventLogger.MODULE_USER_MAINTENANCE,
                            EventLogger.NO_FURTHER_STEP,
                            ServerConstants.TABLE_BASE_USER);
        }

        return ageingStep;
    }

    /**
     * Returns true if the user requires ageing.
     *
     * @param user user being reviewed
     * @param overdueInvoice overdue invoice initiating the ageing
     * @param stepDays current ageing step days of the user
     * @param today today's date
     * @return true if user requires ageing, false if not
     */
    public boolean isAgeingRequired(UserDTO user, InvoiceDTO overdueInvoice, Integer stepDays, Date today) {

        Date invoiceDueDate = Util.truncateDate(overdueInvoice.getDueDate());
        Date statusExpirationDate = DateUtils.addDays(invoiceDueDate, stepDays);

        if (statusExpirationDate.equals(today) || statusExpirationDate.before(today)) {
            LOG.debug("User %s status has expired (last change %s plus %s days is before today %s)", user.getId(), invoiceDueDate, stepDays, today);
            return true;
        }

        LOG.debug("User %s does not need to be aged (last change %s plus %s days is after today %s)", user.getId(), invoiceDueDate, stepDays, today);
        return false;
    }

    /**
     * Removes a user from the ageing process (makes them active), ONLY if they do not
     * still have overdue invoices.
     *
     * @param user user to make active
     * @param excludedInvoiceId invoice id to ignore when determining if the user CAN be made active
     * @param executorId executor id
     */
    public void removeUser(UserDTO user, Integer excludedInvoiceId, Integer executorId) {
        Date now = new Date();

        // validate that the user actually needs a status change
        if (UserDTOEx.STATUS_ACTIVE.equals(user.getStatus().getId())) {
            LOG.debug("User %s is already active, no need to remove from ageing.", user.getId());
            return;
        }

        // validate that the user does not still have overdue invoices
        try {
            if (new InvoiceBL().isUserWithOverdueInvoices(user.getUserId(), now, excludedInvoiceId)) {
                LOG.debug("User %s still has overdue invoices, cannot remove from ageing.", user.getId());
                return;
            }
        } catch (SQLException e) {
            LOG.error("Exception occurred checking for overdue invoices.", e);
            return;
        }

        // make the status change.
        LOG.debug("Removing user %s from ageing (making active).", user.getUserId());
        UserStatusDTO status = new UserStatusDAS().find(UserDTOEx.STATUS_ACTIVE);
        setUserStatus(user, status, now, null);
    }

    /**
     * Sets the user status to the given "aged" status. If the user status is already set to the aged status
     * no changes will be made. This method also performs an HTTP callback and sends a notification
     * message when a status change is made.
     *
     * If the user becomes suspended and can no longer log-in to the system, all of their active orders will
     * be automatically suspended.
     *
     * If the user WAS suspended and becomes active (and can now log-in to the system), any automatically
     * suspended orders will be re-activated.
     *
     * @param user user
     * @param status status to set
     * @param today today's date
     * @param executorId executor id
     */
    public boolean setUserStatus(UserDTO user, UserStatusDTO status, Date today, Integer executorId) {
        // only set status if the new "aged" status is different from the users current status
        if (status.getId() == user.getStatus().getId()) {
            return false;
        }

        AgeingEntityStepDTO nextAgeingStep = status.getAgeingEntityStep();

        if (executorId != null) {
            // this came from the gui
            eLogger.audit(executorId,
                          user.getId(),
                          ServerConstants.TABLE_BASE_USER,
                          user.getId(),
                          EventLogger.MODULE_USER_MAINTENANCE,
                          EventLogger.STATUS_CHANGE,
                          user.getStatus().getId(), null, null);
        } else {
            // this is from a process, no executor involved
            eLogger.auditBySystem(user.getCompany().getId(),
                                  user.getId(),
                                  ServerConstants.TABLE_BASE_USER,
                                  user.getId(),
                                  EventLogger.MODULE_USER_MAINTENANCE,
                                  EventLogger.STATUS_CHANGE,
                                  user.getStatus().getId(), null, null);
        }

        // make the change
        UserStatusDTO oldStatus = user.getStatus();

        user.setUserStatus(status);
        user.setLastStatusChange(today);

        // perform callbacks and notifications
        performAgeingCallback(user, oldStatus, status);
        sendAgeingNotification(user, oldStatus, status);

        // status changed from active to suspended
        // suspend customer orders
        if (nextAgeingStep != null && nextAgeingStep.getSuspend() == 1) {
            LOG.debug("Suspending orders for user %s", user.getUserId());

            OrderBL orderBL = new OrderBL();
            ScrollableResults orders = new OrderDAS().findByUser_Status(user.getId(), OrderStatusFlag.INVOICE);

            while (orders.next()) {
                OrderDTO order = (OrderDTO) orders.get()[0];
                orderBL.set(order);
                orderBL.setStatus(executorId, new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.SUSPENDED_AGEING, order.getUser().getCompany().getId()));
            }

            orders.close();
        } else {
            // status changed from suspended to active
            // re-active suspended customer orders
            if (nextAgeingStep == null && status.getId() == UserDTOEx.STATUS_ACTIVE
                    && oldStatus.getAgeingEntityStep() != null && oldStatus.getAgeingEntityStep().getSuspend() == 1) {
                LOG.debug("Activating orders for user %s", user.getUserId());
                // user out of ageing, activate suspended orders
                OrderBL orderBL = new OrderBL();
                ScrollableResults orders = new OrderDAS().findByUser_Status(user.getId(), OrderStatusFlag.SUSPENDED_AGEING);

                while (orders.next()) {
                    OrderDTO order = (OrderDTO) orders.get()[0];
                    orderBL.set(order);
                    orderBL.setStatus(executorId, new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.INVOICE, order.getUser().getCompany().getId()));
                }

                orders.close();
            }
        }
        // emit NewUserStatusEvent
        NewUserStatusEvent event = new NewUserStatusEvent(user.getCompany().getId(), user.getId(), oldStatus.getId(), status.getId());
        EventManager.process(event);
        return true;
    }

    protected boolean performAgeingCallback(UserDTO user, UserStatusDTO oldStatus, UserStatusDTO newStatus) {
        String url = null;
        try {
            url = PreferenceBL.getPreferenceValue(user.getEntity().getId(), ServerConstants.PREFERENCE_URL_CALLBACK);

        } catch (EmptyResultDataAccessException e) {
            /* ignore, no callback preference configured */
        }

        if (url != null && url.length() > 0) {
            try {
                LOG.debug("Performing ageing HTTP callback for URL: %s", url);

                // cook the parameters to be sent
                NameValuePair[] data = new NameValuePair[6];
                data[0] = new NameValuePair("cmd", "ageing_update");
                data[1] = new NameValuePair("user_id", String.valueOf(user.getId()));
                data[2] = new NameValuePair("login_name", user.getUserName());
                data[3] = new NameValuePair("from_status", String.valueOf(oldStatus.getId()));
                data[4] = new NameValuePair("to_status", String.valueOf(newStatus.getId()));
                data[5] = new NameValuePair("can_login", String.valueOf(newStatus.getCanLogin()));

                // make the call
                HttpClient client = new HttpClient();
                client.setConnectionTimeout(30000);
                PostMethod post = new PostMethod(url);
                post.setRequestBody(data);
                client.executeMethod(post);

            } catch (Exception e) {
                LOG.error("Exception occurred posting ageing HTTP callback for URL: %s", url, e);
                return false;
            }
        }
        return true;
    }

    protected boolean sendAgeingNotification(UserDTO user, UserStatusDTO oldStatus, UserStatusDTO newStatus) {

        AgeingEntityStepDTO nextStep = newStatus.getAgeingEntityStep();

        if (nextStep == null || nextStep.getSendNotification() == 1) {
            LOG.debug("Sending notification to user %s during ageing/reactivating", user.getUserId());
            // process the ageing notification event to find and send the notification message for the ageing step
            try {
                EventManager.process(new AgeingNotificationEvent(user.getEntity().getId(),
                        user.getLanguage().getId(),
                        (nextStep != null && newStatus != null) ? newStatus.getId() : null,
                        user.getId()));

            } catch (Exception exception) {
                LOG.warn("Cannot send notification on ageing: %s", user.getId());
            }
        }

        return true;
    }

    private boolean isUserAlreadyPassAgeingStep(UserStatusDTO userStatus, AgeingEntityStepDTO step, List<AgeingEntityStepDTO> orderedSteps) {

        AgeingEntityStepDTO currentStep = userStatus.getAgeingEntityStep();
        if (step == null && currentStep == null) {
            return true; // same status
        } else if (currentStep == null) { // now user is active and does not take part in ageing process
            return false;
        } else if (step == null) {
            return false; //now user ageing, but should be active
        } else {
            int currentIndex = orderedSteps.indexOf(currentStep);
            int nextIndex = orderedSteps.indexOf(step);
            return nextIndex <= currentIndex;
        }
    }

    /**
     * Null safe convenience method to return the status description.
     *
     * @param status user status
     * @return description
     */
    private String getStatusDescription(UserStatusDTO status) {
        if (status != null) {
            AgeingEntityStepDTO step = status.getAgeingEntityStep();
            return step != null ? step.getDescription() : null;
        }
        return null;
    }
}
