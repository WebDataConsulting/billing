/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.process.task;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.pluggableTask.BasicOrderPeriodTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.PreferenceBL;
import org.springframework.dao.EmptyResultDataAccessException;

public class ProRateOrderPeriodTask extends BasicOrderPeriodTask {

    protected Date viewLimit = null;

    //private static final FormatLogger LOG = Logger
        //  .getLogger(ProRateOrderPeriodTask.class);

    public ProRateOrderPeriodTask() {
        viewLimit = null;
    }

    /**
     * This methods takes and order and calculates the end date that is going to
     * be covered cosidering the starting date and the dates of this process.
     * 
     * @param order
     * @param processDate
     * @param maxPeriods
     * @param periodStart
     * @return
     * @throws SessionInternalError
     */
    public Date calculateEnd(OrderDTO order, Date processDate, int maxPeriods,
            Date periodStart) throws TaskException {

        // verify that the pro-rating preference is present
        int preferenceUseProRating = 0;
        try {
            preferenceUseProRating = 
            	PreferenceBL.getPreferenceValueAsIntegerOrZero(
            		order.getUser().getEntity().getId(), ServerConstants.PREFERENCE_USE_PRO_RATING);
        } catch (EmptyResultDataAccessException e1) {
            // the defaults are fine
        }
        if (preferenceUseProRating == 0) {
            throw new TaskException(
                    "This plug-in is only for companies with pro-rating enabled.");
        }

        return super.calculateEnd(order, processDate, maxPeriods,
                calculateCycleStarts(order, periodStart));

    }

    private Date calculateCycleStarts(OrderDTO order, Date periodStart) {
        Date retValue = null;
        List<Integer> results = new OrderProcessDAS().findActiveInvoicesForOrder(order.getId());
        if ( !results.isEmpty() && order.getNextBillableDay() != null) {
            retValue = order.getNextBillableDay();
        } else if (order.getUser().getCustomer().getMainSubscription() != null) {
        	Calendar cal = new GregorianCalendar();
        	MainSubscriptionDTO mainSubscription = order.getUser().getCustomer().getMainSubscription();
            Integer nextInvoiceDaysOfPeriod = mainSubscription.getNextInvoiceDayOfPeriod();
            Integer mainSubscriptionPeriodUnit = mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();
            Integer mainSubscriptionPeriodValue = mainSubscription.getSubscriptionPeriod().getValue();

            cal.setTime(order.getActiveSince() != null ? order.getActiveSince() : order.getCreateDate());
            
    		cal.set(Calendar.DAY_OF_MONTH, 1);

            // consider end of month case
            if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) <= nextInvoiceDaysOfPeriod &&
                    ServerConstants.PERIOD_UNIT_MONTH.equals(mainSubscriptionPeriodUnit)) {
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            } else {
                cal.add(Calendar.DATE, nextInvoiceDaysOfPeriod - 1);
            }


    		return CalendarUtils.findNearestTargetDateInPast(cal.getTime(),
                    periodStart,
                    nextInvoiceDaysOfPeriod,
                    mainSubscriptionPeriodUnit,
                    mainSubscriptionPeriodValue);
    		
        } else {
            retValue = periodStart;
        }

        return Util.truncateDate(retValue);
    }

}
