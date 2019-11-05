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
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import org.apache.commons.lang.StringUtils;

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
    
	public static final ParameterDescription PARAMETER_HOLIDAY_FILE = new ParameterDescription(
			PARAM_HOLIDAY_FILE, false, ParameterDescription.Type.STR);

	public static final ParameterDescription PARAMETER_DATE_FORMAT = new ParameterDescription(
			PARAM_DATE_FORMAT, false, ParameterDescription.Type.STR);

	// initializer for pluggable params
	{
		descriptions.add(PARAMETER_HOLIDAY_FILE);
		descriptions.add(PARAMETER_DATE_FORMAT);
	}

    private BusinessDays businessDays;

    private BusinessDays getBusinessDaysHelper() {
        if (businessDays == null) {
            String dateFormat = getParameter(PARAM_DATE_FORMAT, "yyyy-MM-dd");
            String holidayFile = getParameter(PARAM_HOLIDAY_FILE, "");

            if (StringUtils.isNotEmpty(holidayFile)) {
                holidayFile = Util.getBaseDir() + File.separator + holidayFile;
                businessDays= new BusinessDays(new File(holidayFile), DateTimeFormat.forPattern(dateFormat));
            } else {
            	businessDays = new BusinessDays();
            }
        }

        return businessDays;
    }

    @Override
    public boolean isAgeingRequired(UserDTO user, InvoiceDTO overdueInvoice, Integer stepDays, Date today) {

        Date invoiceDueDate = Util.truncateDate(overdueInvoice.getDueDate());
        Date expiryDate = getBusinessDaysHelper().addBusinessDays(invoiceDueDate, stepDays);

        // last status change + step days as week days
        if (expiryDate.equals(today) || expiryDate.before(today)) {
            LOG.debug("User status has expired (last change %s + %s days is before today %s)", invoiceDueDate,
                       stepDays, today);
            return true;
        }

        LOG.debug("User does not need to be aged (last change %s + %s days is after today %s)", invoiceDueDate,
                   stepDays, today);
        return false;
    }
}
