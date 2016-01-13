package com.sapienter.jbilling.server.user.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.event.DynamicBalanceChangeEvent;
import com.sapienter.jbilling.server.util.Context;

import java.math.BigDecimal;

/**
 * <code>CreditLimitationNotificationTask</code> checks for credit limitation 1 and 2 for
 * any customer according to its balance value and generates dedicated notification if
 * balance value is reduced from the defined limitations.
 *
 * @author Maeis Gharibjanian
 * @since 02-09-2013
 */
public class CreditLimitationNotificationTask extends PluggableTask implements
        IInternalEventsTask {

	private static final FormatLogger LOG = new FormatLogger(CreditLimitationNotificationTask.class);

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
            DynamicBalanceChangeEvent.class
    };

    /**
     * Checks if new balance value is less than or equal the credit limitation 1 (2)
     * then notifies related notification to customer.
     *
     * @param event The {@code DynamicBalanceChangeEvent} event is passed the method.
     * @throws PluggableTaskException
     */
    @Override
    public void process(Event event) throws PluggableTaskException {
		LOG.debug("Dynamic Balance Change. Checking for Credit Limit Notifications.");

        if (event instanceof DynamicBalanceChangeEvent) {

            DynamicBalanceChangeEvent dbce = (DynamicBalanceChangeEvent) event;

            UserDTO user = new UserDAS().find(dbce.getUserId());
            CustomerDTO customer = user.getCustomer();

            BigDecimal availableMoney = null != customer.getCreditLimit() ?
                    customer.getCreditLimit().add(dbce.getNewBalance()):
                    dbce.getNewBalance();

	        LOG.debug("Checking for Credit Limit 1 Notify, Available Money: %s, Credit Limit 1: %s",
			        availableMoney, null != customer.getCreditNotificationLimit1() ? customer.getCreditNotificationLimit1() : "null");
            // Notify credit limitation 1
            if (customer.getCreditNotificationLimit1() != null &&
                    availableMoney.compareTo(customer.getCreditNotificationLimit1()) < 0) {

                LOG.debug("Balance below/equal-to creditNotificationLimit1.");

                sendNotification(dbce, customer, MessageDTO.TYPE_BAL_BELOW_CREDIT_LIMIT_1);
            }

	        LOG.debug("Checking for Credit Limit 2 Notify, Available Money: %s, Credit Limit 2: %s",
			        availableMoney, null != customer.getCreditNotificationLimit2() ? customer.getCreditNotificationLimit2() : "null");
            // Notify credit limitation 2
            if (customer.getCreditNotificationLimit2() != null &&
                    availableMoney.compareTo(customer.getCreditNotificationLimit2()) < 0) {

                LOG.debug("Balance below/equal-to creditNotificationLimit2.");

                sendNotification(dbce, customer, MessageDTO.TYPE_BAL_BELOW_CREDIT_LIMIT_2);
            }
        }
    }

    /**
     * Specifies to fire current task class with subscribed events.
     * @return array of subscribed events.
     */
    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    // send notification business by NotificationBL and NotificationSessionBean
    private void sendNotification(DynamicBalanceChangeEvent dynamicBalanceChangeEvent, CustomerDTO customerDTO, Integer messageType) {

        NotificationBL notificationBL = new NotificationBL();

        try {

            BigDecimal creditNotificationLimit = null;
            if (MessageDTO.TYPE_BAL_BELOW_CREDIT_LIMIT_1.equals(messageType)) {

                creditNotificationLimit = customerDTO.getCreditNotificationLimit1();

            } else if (MessageDTO.TYPE_BAL_BELOW_CREDIT_LIMIT_2.equals(messageType)) {

                creditNotificationLimit = customerDTO.getCreditNotificationLimit2();
            }
            // prepare message
            MessageDTO belowCreditNotificationMessage = notificationBL.getCreditLimitationMessage(
                    messageType,
                    dynamicBalanceChangeEvent.getEntityId(), dynamicBalanceChangeEvent.getUserId(), creditNotificationLimit,
                    dynamicBalanceChangeEvent.getNewBalance());

            // sending an email.
            INotificationSessionBean notificationSess = (INotificationSessionBean) Context
                    .getBean(Context.Name.NOTIFICATION_SESSION);

            notificationSess.notify(dynamicBalanceChangeEvent.getUserId(), belowCreditNotificationMessage);

        } catch (Exception e) {
            LOG.warn("Cant send credit limit notification email.");
            // do not throw task exception if email not sent.
            // critical activity like create Order should not fail if the email
            // could not be sent
        }
        return;
    }
}
