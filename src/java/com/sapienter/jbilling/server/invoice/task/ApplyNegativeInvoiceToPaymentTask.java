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
package com.sapienter.jbilling.server.invoice.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.payment.IPaymentSessionBean;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;


import java.math.BigDecimal;
import java.util.Date;

public class ApplyNegativeInvoiceToPaymentTask extends PluggableTask
        implements IInternalEventsTask {

    private static final FormatLogger LOG =  new FormatLogger(ApplyNegativeInvoiceToPaymentTask.class);

    private static final Class<Event> events[] = new Class[]{
            InvoicesGeneratedEvent.class
    };

    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    public void process (Event event) throws PluggableTaskException {

        if (event instanceof InvoicesGeneratedEvent) {
            InvoicesGeneratedEvent instantiatedEvent = (InvoicesGeneratedEvent) event;

            for(Integer invoiceId: instantiatedEvent.getInvoiceIds()){
                fixNegativeInvoice(invoiceId);
            }

        } else {
            throw new PluggableTaskException("Unknown event: " +
                    event.getClass());
        }
    }

    private void fixNegativeInvoice(Integer invoiceId){
        InvoiceDTO invoiceDTO = new InvoiceBL(invoiceId).getEntity();

        if (invoiceDTO.getTotal().compareTo(BigDecimal.ZERO) < 0 && invoiceDTO.getIsReview().equals(0)) {
            PaymentDTOEx creditPayment = new PaymentDTOEx();
            creditPayment.setIsRefund(0);
            creditPayment.setAmount(invoiceDTO.getTotal().negate());
            creditPayment.setCurrency(new CurrencyDAS().find(invoiceDTO.getCurrency().getId()));
            creditPayment.setPaymentDate(new Date());
            creditPayment.setPaymentMethod(new PaymentMethodDTO(ServerConstants.PAYMENT_METHOD_CREDIT));
            creditPayment.setUserId(invoiceDTO.getUserId());
            IPaymentSessionBean paymentSessionBean = Context.getBean(Context.Name.PAYMENT_SESSION);
            paymentSessionBean.applyPayment(creditPayment, null, invoiceDTO.getUserId());
            invoiceDTO.setTotal(BigDecimal.ZERO);
            invoiceDTO.setBalance(BigDecimal.ZERO);
        }
    }
}
