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
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.InvoiceDeletedEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;

public class DeleteResellerOrderTask extends PluggableTask
        implements IInternalEventsTask {

    private static final FormatLogger logger = new FormatLogger(ApplyNegativeInvoiceToPaymentTask.class);

    @SuppressWarnings("unchecked")
	private static final Class<Event> events[] = new Class[]{
            InvoiceDeletedEvent.class
    };
    
    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

	@Override
	public void process(Event event) throws PluggableTaskException {
		logger.debug("Entering DeleteInvoiceOfReseller process - event: " + event);
		
		InvoiceDeletedEvent deletedEvent = (InvoiceDeletedEvent) event;

		Integer entityId = deletedEvent.getEntityId();
		Integer orderId = deletedEvent.getInvoice().getOrderProcesses().iterator().next().getPurchaseOrder().getId();
		
		CompanyDAS companyDAS = new CompanyDAS();
		CompanyDTO company = companyDAS.find(entityId);
		
		boolean resellerExists = company.getReseller() != null;
		
		if(!resellerExists) {
			logger.debug("No reseller customer for entity: " + entityId);
			return;
		}
		logger.debug("Reseller Exists for company");
		
		deleteResellerOrdersAndInvoices(company.getReseller().getId(), orderId);
	}

    /**
     * Finds orders and invoices of reseller for given order and deletes them
     *
     * @param userId
     * @param orderId
     */
	private void deleteResellerOrdersAndInvoices(Integer userId, Integer orderId) {
		
		OrderBL orderBL = new OrderBL();
		InvoiceBL invoiceBL = new InvoiceBL();
		
		OrderDAS orderDAS = new OrderDAS();
		InvoiceDAS invoiceDAS = new InvoiceDAS();
		
		for (OrderDTO order : orderDAS.findOrdersByUserAndResellerOrder(userId, orderId)) {
			for (InvoiceDTO invoice : invoiceDAS.findInvoicesByOrder(order.getId())) {
				invoiceBL.set(invoice);
				invoiceBL.delete(userId);
			}
			orderBL.set(order);
			orderBL.delete(userId);
		}
	}
}
