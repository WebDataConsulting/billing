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

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.payment.event.PaymentLinkedToInvoiceEvent;
import com.sapienter.jbilling.server.payment.event.PaymentUnlinkedFromInvoiceEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.partner.db.PaymentCommissionDAS;
import com.sapienter.jbilling.server.user.partner.db.PaymentCommissionDTO;


import java.math.BigDecimal;

public class GeneratePaymentCommissionTask extends PluggableTask
        implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(GeneratePaymentCommissionTask.class);

    private static final Class<Event> events[] = new Class[]{
            PaymentLinkedToInvoiceEvent.class,
            PaymentUnlinkedFromInvoiceEvent.class
    };

    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    public void process (Event event) throws PluggableTaskException {

        if (event instanceof PaymentLinkedToInvoiceEvent) {
            PaymentLinkedToInvoiceEvent instantiatedEvent = (PaymentLinkedToInvoiceEvent) event;
            createPaymentCommission(instantiatedEvent.getInvoice(), instantiatedEvent.getTotalPaid());
        } else if (event instanceof PaymentUnlinkedFromInvoiceEvent) {
            PaymentUnlinkedFromInvoiceEvent instantiatedEvent = (PaymentUnlinkedFromInvoiceEvent) event;
            createPaymentCommission(instantiatedEvent.getInvoice(), instantiatedEvent.getTotalPaid());
        } else {
            throw new PluggableTaskException("Unknown event: " +
                    event.getClass());
        }
    }

    private void createPaymentCommission(InvoiceDTO invoice, BigDecimal amount){
        if(invoice.getBaseUser().getCustomer().getPartner() != null){
            PaymentCommissionDTO paymentCommission = new PaymentCommissionDTO();
            paymentCommission.setInvoice(invoice);
            paymentCommission.setPaymentAmount(amount);

            new PaymentCommissionDAS().save(paymentCommission);
        }
    }
}
