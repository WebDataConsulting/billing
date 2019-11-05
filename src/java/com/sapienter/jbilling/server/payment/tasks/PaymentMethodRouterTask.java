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

import java.util.ArrayList;
import java.util.List;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.pluggableTask.PaymentTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

/**
 * Routes payments to a suitable gateway according to the payment method
 * registered for the customer (either Credit Card or ACH). This task
 * determines the appropriate payment processor task to which payment
 * should be routed according to the payment type registered in the
 * payment information or customer record.
 * 
 * @author emirc
 */
public class PaymentMethodRouterTask extends AbstractPaymentRouterTask {

    private static final FormatLogger LOG = new FormatLogger(
            PaymentMethodRouterTask.class);
    
    private static final ParameterDescription CREDIT_CARD_DELEGATE = 
    	new ParameterDescription("cc_payment_task", false, ParameterDescription.Type.STR);
    private static final ParameterDescription ACH_DELEGATE = 
    	new ParameterDescription("ach_payment_task", false, ParameterDescription.Type.STR);
    
    //initializer for pluggable params
    { 
    	descriptions.add(CREDIT_CARD_DELEGATE);
        descriptions.add(ACH_DELEGATE);
    }

    
    @Override
    public void initializeParamters(PluggableTaskDTO task)
            throws PluggableTaskException {
        super.initializeParamters(task);
        LOG.debug("Delegate task for credit card payments: %s", 
                parameters.get(CREDIT_CARD_DELEGATE));
        LOG.debug("Delegate task for ACH payments: %s", 
                parameters.get(ACH_DELEGATE));
    }
    
    @Override
    protected PaymentTask selectDelegate(PaymentDTOEx paymentInfo)
            throws PluggableTaskException {
        
        Integer selectedTaskId = null;
        PaymentInformationBL piBl = new PaymentInformationBL();
        if (piBl.isCreditCard(paymentInfo.getInstrument())) {
            // Credit card data is present in payment record
            selectedTaskId = new Integer((String)parameters.get(CREDIT_CARD_DELEGATE));
            LOG.debug("Delegating to credit card payment processor");
        } else if (piBl.isACH(paymentInfo.getInstrument())) {
            // ACH data is present in payment record
            selectedTaskId = new Integer((String)parameters.get(ACH_DELEGATE));
            LOG.debug("Delegating to ACH payment processor");
        }
        
        if (selectedTaskId == null) {
            LOG.warn("Payment data unavailable, unable to route payment");
            return null;
        }
        LOG.debug("Delegating to task id %s", selectedTaskId);
        PaymentTask selectedTask = instantiateTask(selectedTaskId);
        return selectedTask;
    }

}
