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
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.order.TimePeriod;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;

import java.math.BigDecimal;

/**
 * This plug-in calculates taxes for invoice.
 *
 * Plug-in parameters:
 *
 *      penalty_after_days: (required) Number of days beyond which the penalty becomes applicable
 *
 * @author Vikas Bodani
 * @since 28-Jul-2011
 *
 */
public class PaymentTermPenaltyTask extends AbstractChargeTask {

    private static final FormatLogger LOG = new FormatLogger(PaymentTermPenaltyTask.class);

    // Plug-in Parameters
    // Mandatory parameters
    protected static final ParameterDescription PARAM_PENALTY_AFTER_DAYS = new ParameterDescription("penalty_after_days", true, ParameterDescription.Type.STR);

    private Integer penaltyAfterDays=null;

    //initializer for pluggable params
    {
        descriptions.add(PARAM_PENALTY_AFTER_DAYS);
    }

    /**
     *
     */
    protected BigDecimal calculateAndApplyTax(NewInvoiceContext invoice, Integer userId) {

        LOG.debug("calculateAndApplyTax");

        BigDecimal invoiceAmountSum= super.calculateAndApplyTax(invoice, userId);

        this.invoiceLineTypeId= ServerConstants.INVOICE_LINE_TYPE_PENALTY;

        return invoiceAmountSum;
    }

    /**
     * Custom logic to determine if the tax should be applied to this user's invoice
     * @param userId The user_id of the Invoice
     * @return
     */
    protected boolean isTaxCalculationNeeded(NewInvoiceContext invoice, Integer userId) {
        LOG.debug("isTaxCalculationNeeded");
        //Invoice Due Date period
        TimePeriod timePeriod= invoice.getDueDatePeriod();
        LOG.debug("Invoice Due Days Time Period= %s", timePeriod);
        //convert period unit to days (Days, Weeks, Months, Years to days)
        int periodToDays= MapPeriodToCalendar.periodToDays(timePeriod.getUnitId());
        //Period unit value
        int periodValue= timePeriod.getValue();
        //product of the above two to get total number of days
        Integer totalDays= periodValue * periodToDays;
        LOG.debug("Total Invoice Due days= %s", totalDays);
        return (totalDays.compareTo(penaltyAfterDays) > 0 );
    }

    /**
     * Set the current set of plugin params
     */
    protected void setPluginParameters()  throws TaskException {
        LOG.debug("setPluginParameters()");
        super.setPluginParameters();
        try {
            //mandatory
            String paramValue = getParameter(PARAM_PENALTY_AFTER_DAYS.getName(), "");
            if (paramValue == null || "".equals(paramValue.trim())) {
                throw new TaskException("Penalty After Days field is not defined!");
            }
            penaltyAfterDays= Integer.valueOf(paramValue);
            LOG.debug("Parameter Penalty After Days is set.");
        } catch (NumberFormatException e) {
            LOG.error("Incorrect plugin configuration", e);
            throw new TaskException(e);
        }
    }

}
