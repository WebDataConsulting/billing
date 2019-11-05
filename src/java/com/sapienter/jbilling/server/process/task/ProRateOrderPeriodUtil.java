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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.ServerConstants;

public class ProRateOrderPeriodUtil {

    public static Date calculateCycleStarts (OrderDTO order, Date periodStart) {

        Date retValue = null;
        List<Integer> results = new OrderProcessDAS().findActiveInvoicesForOrder(order.getId());
        Date nextBillableDayFromOrderChanges = order.calcNextBillableDayFromChanges();

        if (!results.isEmpty() && nextBillableDayFromOrderChanges != null) {
            retValue = nextBillableDayFromOrderChanges;
            if (order.getUser().getCustomer().getMainSubscription() != null) {
                retValue = calcCycleStartDateFromMainSubscription(nextBillableDayFromOrderChanges, periodStart, order
                        .getUser()
                        .getCustomer()
                        .getMainSubscription());
            }
        } else if (order.getUser().getCustomer().getMainSubscription() != null) {
            MainSubscriptionDTO mainSubscription = order.getUser().getCustomer().getMainSubscription();
            for (OrderLineDTO line : order.getLines()) {
                for (OrderChangeDTO change : line.getOrderChanges()) {
                    Date nextBillableDayFromChange = calcCycleStartDateFromMainSubscription(
                            change.getNextBillableDate() == null ? change.getStartDate() : change.getNextBillableDate(),
                            periodStart, mainSubscription);
                    if ((retValue == null) || nextBillableDayFromChange.before(retValue)) {
                        retValue = nextBillableDayFromChange;
                    }
                }
            }
            if (retValue == null) {
                retValue = calcCycleStartDateFromMainSubscription(
                        order.getActiveSince() != null ? order.getActiveSince() : order.getCreateDate(), periodStart,
                        mainSubscription);
            }
        } else {
            retValue = periodStart;
        }
        return Util.truncateDate(retValue);
    }

    private static Date calcCycleStartDateFromMainSubscription (Date activeSince, Date periodStart,
            MainSubscriptionDTO mainSubscription) {
        Date calculatedValue = null;
        Calendar cal = new GregorianCalendar();

        Integer nextInvoiceDaysOfPeriod = mainSubscription.getNextInvoiceDayOfPeriod();
        Integer mainSubscriptionPeriodUnit = mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();
        Integer mainSubscriptionPeriodValue = mainSubscription.getSubscriptionPeriod().getValue();

        cal.setTime(activeSince);
        if (ServerConstants.PERIOD_UNIT_WEEK.equals(mainSubscriptionPeriodUnit)) {
            cal.set(Calendar.DAY_OF_WEEK, nextInvoiceDaysOfPeriod);
        } else if (ServerConstants.PERIOD_UNIT_SEMI_MONTHLY.equals(mainSubscriptionPeriodUnit)) {
            Date expectedStartDate = CalendarUtils.findNearestTargetDateInPastForSemiMonthly(cal,
                    nextInvoiceDaysOfPeriod);
            cal.setTime(expectedStartDate);
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }

        if (ServerConstants.PERIOD_UNIT_MONTH.equals(mainSubscriptionPeriodUnit)) {
            // consider end of month case
            if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) <= nextInvoiceDaysOfPeriod
                    && ServerConstants.PERIOD_UNIT_MONTH.equals(mainSubscriptionPeriodUnit)) {
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            } else {
                cal.add(Calendar.DATE, nextInvoiceDaysOfPeriod - 1);
            }
        }

        if (!ServerConstants.PERIOD_UNIT_SEMI_MONTHLY.equals(mainSubscriptionPeriodUnit)) {
            calculatedValue = CalendarUtils.findNearestTargetDateInPast(cal.getTime(), periodStart,
                    nextInvoiceDaysOfPeriod, mainSubscriptionPeriodUnit, mainSubscriptionPeriodValue);
        } else {
            calculatedValue = cal.getTime();
        }

        return calculatedValue;
    }
}
