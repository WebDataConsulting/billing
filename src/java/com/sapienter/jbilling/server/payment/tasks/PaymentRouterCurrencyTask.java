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

package com.sapienter.jbilling.server.payment.tasks;

import java.util.HashMap;
import java.util.Map;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.pluggableTask.PaymentTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

/**
 * Routes payments to other processor plug-ins based on currency. 
 * To configure the routing, set the parameter name to the currency 
 * code and the parameter value to the processor plug-in id.
 */
public class PaymentRouterCurrencyTask extends AbstractPaymentRouterTask {
    private static final FormatLogger LOG = new FormatLogger(
            PaymentRouterCurrencyTask.class);

    @Override
    protected PaymentTask selectDelegate(PaymentDTOEx paymentInfo)
            throws PluggableTaskException {
        String currencyCode = paymentInfo.getCurrency().getCode();
        Integer selectedTaskId = null;

        try {
            // try to get the task id for this currency
            selectedTaskId = intValueOf(parameters.get(currencyCode));
        } catch (NumberFormatException e) {
            throw new PluggableTaskException("Invalid task id for currency " +
                    "code: " + currencyCode);
        }
        if (selectedTaskId == null) {
            LOG.warn("Could not find processor for %s", parameters.get(currencyCode));
            return null;
        }

        LOG.debug("Delegating to task id %s", selectedTaskId);
        PaymentTask selectedTask = instantiateTask(selectedTaskId);

        return selectedTask;
    }

    @Override
    public Map<String, String> getAsyncParameters(InvoiceDTO invoice) 
            throws PluggableTaskException {
        String currencyCode = invoice.getCurrency().getCode();
        Map<String, String> parameters = new HashMap<String, String>(1);
        parameters.put("currency", currencyCode);
        return parameters;
    }
}
