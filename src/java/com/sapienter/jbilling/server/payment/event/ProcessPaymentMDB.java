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
package com.sapienter.jbilling.server.payment.event;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.util.Context;

/*
 * The configuration needs to be done specifically for each installation/scenario
 * using the file jbilling-jms.xml
 */
public class ProcessPaymentMDB implements MessageListener {
    
    private final FormatLogger LOG = new FormatLogger(ProcessPaymentMDB.class);

    public void onMessage(Message message) {
        try {
            LOG.debug("Processing message. Processor %s entity %s by %s", message.getStringProperty("processor"), 
                       message.getIntProperty("entityId"), this.hashCode());
            MapMessage myMessage = (MapMessage) message;
            
            // use a session bean to make sure the processing is done in one transaction
            IBillingProcessSessionBean process = (IBillingProcessSessionBean) 
                    Context.getBean(Context.Name.BILLING_PROCESS_SESSION);

            String type = message.getStringProperty("type"); 
            if (type.equals("payment")) {
                LOG.debug("Now processing asynch payment:  processId: %s runId: %s invoiceId: %s",
                          myMessage.getInt("processId"),
                          myMessage.getInt("runId"),
                          myMessage.getInt("invoiceId"));
                Integer invoiceId = (myMessage.getInt("invoiceId") == -1) ? null : myMessage.getInt("invoiceId");
                if (invoiceId != null) {
                    // lock it
                    new InvoiceDAS().findForUpdate(invoiceId);
                }
                process.processPayment(
                        (myMessage.getInt("processId") == -1) ? null : myMessage.getInt("processId"),
                        (myMessage.getInt("runId") == -1) ? null : myMessage.getInt("runId"),
                        invoiceId);
                LOG.debug("Done");
            } else if (type.equals("ender")) {
                process.endPayments(myMessage.getInt("runId"));
            } else {
                LOG.error("Can not process message of type %s", type);
            }
        } catch (Exception e) {
            LOG.error("Generating payment", e);
        }
    }

}
