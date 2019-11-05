package com.sapienter.jbilling.server.process.task;

;

import com.sapienter.jbilling.client.suretax.SuretaxClient;
import com.sapienter.jbilling.client.suretax.request.SuretaxCancelRequest;
import com.sapienter.jbilling.client.suretax.response.SuretaxCancelResponse;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.SuretaxTransactionLogDAS;
import com.sapienter.jbilling.server.invoice.db.SuretaxTransactionLogDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type;
import com.sapienter.jbilling.server.process.event.BeforeInvoiceDeleteEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;

public class SuretaxDeleteInvoiceTask extends PluggableTask implements
		IInternalEventsTask {
	private static final FormatLogger LOG = new FormatLogger(
			SuretaxDeleteInvoiceTask.class);
	public static final String CLIENT_NUMBER = "client_number";
	public static final String VALIDATION_KEY = "validation_key";
	public static final String SURETAX_DELETE_REQUEST_URL = "Suretax Delete Request Url";

	public SuretaxDeleteInvoiceTask() {
		descriptions.add(new ParameterDescription(SURETAX_DELETE_REQUEST_URL, true, Type.STR));
		descriptions.add(new ParameterDescription(CLIENT_NUMBER, true, Type.STR));
		descriptions.add(new ParameterDescription(VALIDATION_KEY, true, Type.STR));
	}

	@Override
	public void process(Event event) throws PluggableTaskException {
		InvoiceDTO deletingInvoice = ((BeforeInvoiceDeleteEvent) event).getInvoice();
		LOG.debug("Processing BeforeInvoiceDeleteEvent for invoice number: %s",
				  deletingInvoice.getNumber());

		Integer transId = (Integer) deletingInvoice.getMetaField(
				SureTaxCompositionTask.SURETAX_TRANS_ID_META_FIELD_NAME)
				.getValue();
		if (transId == null || transId < 1) {
			// There was no trans id saved for this invoice. Raise exception
			LOG.debug("transId was null. Therefore not sending any cancel request to Suretax.");
			throw new PluggableTaskException("No transId found, cannot proceed to call Suretax.");
		} else {
			SuretaxCancelRequest cancelRequest = new SuretaxCancelRequest();
			cancelRequest.setClientNumber(getParameter(CLIENT_NUMBER, ""));
			cancelRequest.setValidationKey(getParameter(VALIDATION_KEY, ""));
			SuretaxTransactionLogDAS suretaxTransactionLogDAS = new SuretaxTransactionLogDAS();
			SuretaxTransactionLogDTO suretaxTransactionLogDTO = suretaxTransactionLogDAS
					.findByResponseTransId(transId);
			cancelRequest.setClientTracking(suretaxTransactionLogDTO
					.getTransactionId());
			cancelRequest.setTransId(suretaxTransactionLogDTO
					.getResponseTransactionId() + "");
			String suretaxDeleteRequestUrl = getParameter(
					SURETAX_DELETE_REQUEST_URL, "");
			SuretaxCancelResponse cancelResponse = new SuretaxClient()
					.getCancelResponse(cancelRequest, suretaxDeleteRequestUrl);
			if (!cancelResponse.getSuccessful().equals("Y")) {
				throw new PluggableTaskException(
						"Deletion of suretax cancel request failed due to:"
								+ cancelResponse.getHeaderMessage());
			}
		}
	}

	@Override
	public Class<Event>[] getSubscribedEvents() {
		Class<Event>[] result = new Class[] { BeforeInvoiceDeleteEvent.class };
		return result;
	}
}
