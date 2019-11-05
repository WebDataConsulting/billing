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

/*
 * Created on Dec 15, 2004
 *
 */
package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.db.OrderDTO;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author Emil
 *
 */
public class OrderPeriodAnticipateTask extends BasicOrderPeriodTask {
    

    private static final FormatLogger LOG = new FormatLogger(OrderPeriodAnticipateTask.class);
    
    public Date calculateEnd(OrderDTO order, Date processDate,
            int maxPeriods, Date periodStarts) 
            throws TaskException {

        if (order.isOneTime()) {
            return null;
        }

        viewLimit = getViewLimit(order.getUser().getId(), processDate);

        if (order.getAnticipatePeriods() != null &&
                order.getAnticipatePeriods().intValue() > 0) {
            try {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(viewLimit);
                // now add the months that this order is getting anticipated
                cal.add(GregorianCalendar.MONTH,
                        order.getAnticipatePeriods().intValue());
                LOG.debug("Ant periods: %s view limit: %s extended %s", order.getAnticipatePeriods(),
                          viewLimit, cal.getTime());
                viewLimit = cal.getTime();
            } catch (Exception e) {
                throw new TaskException(e);
            } 
        } 
        
        return super.calculateEnd(order, processDate, maxPeriods, periodStarts);
    }
}
