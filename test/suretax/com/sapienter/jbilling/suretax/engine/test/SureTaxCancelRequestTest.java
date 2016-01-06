package com.sapienter.jbilling.suretax.engine.test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import junit.framework.TestCase;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.task.SureTaxCompositionTask;
import com.sapienter.jbilling.server.process.task.SuretaxDeleteInvoiceTask;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.CreateObjectUtil;

public class SureTaxCancelRequestTest extends DefaultTest {

	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFailureOnInvoiceDeletion() {
		int currentTimeMillis = (int) System.currentTimeMillis();
		if (currentTimeMillis < 0) {
			currentTimeMillis = currentTimeMillis * -1;
		}
		// create a user for testing
		UserWS user = new UserWS();
		user.setUserName("invoice-suretax-" + new Date().getTime());
		user.setPassword("P@ssword1");
		user.setLanguageId(1);
		user.setCurrencyId(1);
		user.setMainRoleId(5);
		user.setStatusId(UserDTOEx.STATUS_ACTIVE);

		ContactWS contact = new ContactWS();
		contact.setEmail("gurdev.parmar@gmail.com");
		contact.setFirstName("Gurdev");
		contact.setLastName("Parmar");
		contact.setPostalCode("80301");
		user.setContact(contact);

		user.setUserId(api.createUser(user)); // create user
		assertNotNull("customer created", user.getUserId());

		// Create two items now
		// First item: Long distance call with $1.5 rate.
		Integer itemId1 = CreateObjectUtil.createItem(
				"Long Distance Call intra-state", "1.5",
				currentTimeMillis + "", "2201", api);
		ItemDTOEx item1 = api.getItem(itemId1, null, null);
		MetaFieldValueWS[] metaFields = new MetaFieldValueWS[1];
		MetaFieldValueWS transTypeMetaField = new MetaFieldValueWS();
		transTypeMetaField.setStringValue("010101");
		transTypeMetaField.setFieldName("Transaction Type Code");
		metaFields[0] = transTypeMetaField;
		item1.setMetaFields(metaFields);
		api.updateItem(item1);
		Integer itemId2 = CreateObjectUtil.createItem(
				"Long Distance Call inter-state", "2.5", currentTimeMillis + 1
						+ "", "2201", api);
		ItemDTOEx item2 = api.getItem(itemId2, null, null);
		item2.setMetaFields(metaFields);
		api.updateItem(item2);
		// purchase order with taxable items
		Calendar cal = Calendar.getInstance();
		// I want to set the active since to 07 June 2012 , so the billing
		// process sees it and invoices it
		// set the calendar to 06/07
		cal.set(2010, 5, 7);
		OrderWS order = CreateObjectUtil.createOrderObject(user.getUserId(), 1,
				Constants.ORDER_BILLING_POST_PAID, 1, cal.getTime());

		CreateObjectUtil.addLine(order, 10, Constants.ORDER_LINE_TYPE_ITEM,
				itemId1, new BigDecimal(1.5), "Long Distance Call-intra state");
		CreateObjectUtil.addLine(order, 10, Constants.ORDER_LINE_TYPE_ITEM,
				itemId2, new BigDecimal(2.5), "Long Distance Call-inter state");
		order.setDueDateUnitId(PeriodUnitDTO.DAY);
		order.setDueDateValue(0);// order due

		order.setId(api.createOrder(order)); // create order
		order = api.getOrder(order.getId());
		assertNotNull("order created", order.getId());

		Integer[] invoiceIds = api.createInvoiceWithDate(user.getUserId(),
				new Date(), PeriodUnitDTO.DAY, 45, false); // getAllInvoicesForUser(user.getUserId());
		assertEquals("1 invoice generated", 1, invoiceIds.length);

		InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);

		assertNotNull("Generated invoice was null", invoice);
		assertEquals("Customer Id  & Invoice User Id are not same.",
				new Integer(user.getUserId()), new Integer(invoice.getUserId()));
		assertEquals(8, invoice.getInvoiceLines().length);

		try {
			api.deleteInvoice(invoice.getId());
			System.out.println("Deleted Invoice " + invoiceIds[0]);
		} catch (Exception e) {
			if (e.getMessage().contains("Invalid Validation Key")) {
				// Expected exception.Ignore
				System.out.println("Invalid Validation Key, Expected exception.Ignore");
			} else {
				System.out.println("Unexpected exception. Can't Ignore.");
				throw new RuntimeException(e);
			}
		}
		invoice = null;

		invoice = api.getInvoiceWS(invoiceIds[0]);
		System.out.println("Invoice with id " + invoiceIds[0]
				+ " was not deleted.");
		assertNotNull("Generated invoice must not be deleted.", invoice);

	}

	@Override
	protected Hashtable<String, String> getOptionalParametersForSuretaxPlugin() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Hashtable<String, String> getOptionalParametersForSuretaxDeletePlugin() {
		// Pass a wrong validation_key as the option parameter
		Hashtable<String, String> optionalParameters = new Hashtable<String, String>();
		optionalParameters.put(SuretaxDeleteInvoiceTask.VALIDATION_KEY, "Wrong Validation Key");
		return optionalParameters;
	}
}
