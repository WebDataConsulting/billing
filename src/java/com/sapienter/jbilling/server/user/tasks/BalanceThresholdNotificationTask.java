/**
 * 
 */
package com.sapienter.jbilling.server.user.tasks;


import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.event.DynamicBalanceChangeEvent;
import com.sapienter.jbilling.server.util.Context;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;


/**
 * @author Vikas Bodani
 * @since 07-Oct-2011
 *
 */
public class BalanceThresholdNotificationTask extends PluggableTask implements
        IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(BalanceThresholdNotificationTask.class);

    //mandatory to specify the customer threshold field id
    protected final static ParameterDescription PARAM_THRESHOLD_CCF_ID =
            new ParameterDescription("Dynamic Balance Threshold", true, ParameterDescription.Type.STR);
    
    private Integer thresholdCCFId = null;
    
    //initializer for pluggable params
    {
        descriptions.add(PARAM_THRESHOLD_CCF_ID);
    }
    
    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] { 
        DynamicBalanceChangeEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    /** 
     * Process the task.
     */
    public void process(Event event) throws PluggableTaskException {
        
        if (event instanceof DynamicBalanceChangeEvent) {
        
            DynamicBalanceChangeEvent dbce= (DynamicBalanceChangeEvent) event;

            UserDTO user = UserBL.getUserEntity(dbce.getUserId());

            BigDecimal creditLimit = user.getCustomer().getCreditLimit();
            creditLimit = null != creditLimit ? creditLimit : BigDecimal.ZERO;

            BigDecimal thresholdAmt = retrieveUserThreshold(user);

            LOG.debug("New Dynamic Balance is %s. Credit Limit is %s. Threshold amount is %s",
                    dbce.getNewBalance().toString(),
                    creditLimit.toString(),
                    thresholdAmt.toString());

            if ( dbce.getNewBalance().add(creditLimit).compareTo(thresholdAmt) > 0 ) {
                LOG.debug("Balance+CreditLimit above threshold.");
                //do nothing
            } else {
                LOG.debug("Balance+CreditLimit below/equal-to threshold.");
                notifyByEmail(dbce, thresholdAmt);
            }
            
        }

    }

    /**
     * Returns the threshold amount for the user. If the user does not have
     * defined threshold amount that this method will return ZERO
     *
     * @param user
     * @return calculated threshold amount for the user
     */
    private BigDecimal retrieveUserThreshold(UserDTO user) throws PluggableTaskException {

        thresholdCCFId = getParameter(PARAM_THRESHOLD_CCF_ID.getName(), 0);

        //The threshold amount is stored as a user's meta
        //field provided by the plugin parameter
        if (user.getCustomer() != null && user.getCustomer().getMetaFields() != null) {
            MetaField metaField = new MetaFieldDAS().findNow(thresholdCCFId);
            if (metaField != null) {
                MetaFieldValue metaFieldValue = user.getCustomer().getMetaField(metaField.getName());
                if (metaFieldValue != null && metaFieldValue.getValue() != null &&
                        StringUtils.isNotBlank(metaFieldValue.getValue().toString())) {
                    return new BigDecimal(metaFieldValue.getValue().toString());
                }
            }
        }

        return BigDecimal.ZERO;
    }

    /**
     * This method invokes the NotificationBL class to send the email.
     * 
     * @param event
     * @param thresholdAmt
     */
    private void notifyByEmail(DynamicBalanceChangeEvent event,
            BigDecimal thresholdAmt) {

        NotificationBL notif = new NotificationBL();

        try {

            // prepare message
            MessageDTO belowThresholdMessage = notif.getBelowThresholdMessage(
                    event.getEntityId(), event.getUserId(), thresholdAmt,
                    event.getNewBalance());

            // sending an email.
            INotificationSessionBean notificationSess = (INotificationSessionBean) Context
                    .getBean(Context.Name.NOTIFICATION_SESSION);

            notificationSess.notify(event.getUserId(), belowThresholdMessage);

        } catch (Exception e) {
            LOG.warn("Cant send threshold notification email.");
            // do not throw task exception if email not sent.
            // critical activity like create Order should not fail if the email
            // could not be sent
        }
        return;
    }
}
