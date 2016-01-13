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
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.process.BusinessDays;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.joda.time.format.DateTimeFormat;

import java.io.File;
import java.util.Date;

/**
 * BusinessDayAgeingTask
 *
 * @author Brian Cowdery
 * @since 29/04/11
 */
public class BusinessDayAgeingTask extends BasicAgeingTask {
    private static final FormatLogger LOG = new FormatLogger(BusinessDayAgeingTask.class);

    private static final String PARAM_HOLIDAY_FILE = "holiday_file";
    private static final String PARAM_DATE_FORMAT = "date_format";

    private BusinessDays businessDays;

    private BusinessDays getBusinessDaysHelper() {
        if (businessDays == null) {
            String dateFormat = getParameter(PARAM_DATE_FORMAT, "yyyy-MM-dd");
            String holidayFile = getParameter(PARAM_HOLIDAY_FILE, (String) null);

            if (holidayFile != null) {
                holidayFile = Util.getSysProp("base_dir") + File.separator + holidayFile;
            }

            businessDays = new BusinessDays(new File(holidayFile), DateTimeFormat.forPattern(dateFormat));
        }

        return businessDays;
    }

    @Override
    public boolean isAgeingRequired(UserDTO user, InvoiceDTO overdueInvoice, Integer stepDays, Date today) {

        Date invoiceDueDate = Util.truncateDate(overdueInvoice.getDueDate());
        Date expiryDate = getBusinessDaysHelper().addBusinessDays(invoiceDueDate, stepDays);

        // last status change + step days as week days
        if (expiryDate.equals(today) || expiryDate.before(today)) {
            LOG.debug("User status has expired (last change " + invoiceDueDate + " + "
                      + stepDays + " days is before today " + today + ")");
            return true;
        }

        LOG.debug("User does not need to be aged (last change " + invoiceDueDate + " + "
                  + stepDays + " days is after today " + today + ")");
        return false;
    }
}
