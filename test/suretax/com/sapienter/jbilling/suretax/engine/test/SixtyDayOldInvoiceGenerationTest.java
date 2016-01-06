package com.sapienter.jbilling.suretax.engine.test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.joda.time.DateMidnight;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.PreferenceTypeWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.CreateObjectUtil;

public class SixtyDayOldInvoiceGenerationTest extends DefaultTest {
	private static final Integer SYSTEM_CURRENCY_ID = 1;
	private static final Integer TEST_ITEM_TYPE_ID = 2201;

	public void setUp() throws Exception {
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSixtyDayOldInvoiceGeneration() throws Exception {
		resetBillingConfiguration();

		UserWS user = null;
		ItemDTOEx item = null;
		OrderWS order = null;
		Integer itemId = null;
		Integer orderId = null;
		Integer customerId = setupTestCaseForSelfBilling(user, item, order,
				itemId, orderId);

		// api.triggerBilling(new Date());
		api.triggerBillingAsync(new DateMidnight(2006, 11, 26).toDate());
		System.out.println("Billing Process Started ....");

		// continually check the process status until the API says that the
		// billing process is no longer running.
		ProcessStatusWS runningStatus = null;
		while (api.isBillingProcessRunning()) {
			runningStatus = api.getBillingProcessStatus();
			Thread.sleep(5000);
		}

		System.out.println("Billing Process Completed ....");

		// Test Case 1 Checking ...
		InvoiceWS[] invoices = api.getAllInvoicesForUser(customerId);
		InvoiceWS invoice = invoices != null && invoices.length > 0 ? invoices[0]
				: null;
		assertNotNull("Invoice was not generated", invoice);
		assertEquals(7, invoice.getInvoiceLines().length);
		// Check if self - billing test case passes.
		assertEquals("Customer Id  & Invoice User Id are not same.",
				customerId, invoice.getUserId());
		try {
			api.deleteInvoice(invoice.getId());
		} catch (Exception e) {
			System.out.println("Exception while deleting invoice: " + invoice.getId());
		}
		
		//TODO: discuss with Suretax and confirm if the invoice older than 60 days would get cancelled or not
		/*
		 	InvoiceWS invoiceFromDb = null;
			try {
				invoiceFromDb = api.getInvoiceWS(invoice.getId());
			} catch (Exception e) {
				System.out.println("Exception while Fecthing invoice: " + invoice.getId());
			} 
		 */
		//assertNull("Invoice should have been deleted", invoiceFromDb);

	}

	// Test Case 1 Setup
	private Integer setupTestCaseForSelfBilling(UserWS user1, ItemDTOEx item1,
			OrderWS order1, Integer itemId1, Integer orderId1)
			throws JbillingAPIException, IOException {

		// create User
		System.out.println("Creating user");
		user1 = SureTaxCompositionTaskTest.createUser(System
				.currentTimeMillis());
		System.out.println("Created user");
		Integer customerId1 = user1.getUserId();
		assertNotNull("Customer/User ID should not be null", customerId1);

		// create item
		BigDecimal itemPrice = new BigDecimal("1.0");
		item1 = new ItemDTOEx();
		item1.setCurrencyId(SYSTEM_CURRENCY_ID);
		item1.setPrice(itemPrice);
		item1.setDescription("Test Item for Bill To User");
		item1.setEntityId(1);
		item1.setNumber("Number1");
		item1.setTypes(new Integer[] { TEST_ITEM_TYPE_ID });

		itemId1 = api.createItem(item1);
		ItemDTOEx item = api.getItem(itemId1, null, null);
		MetaFieldValueWS[] metaFields = new MetaFieldValueWS[1];
		MetaFieldValueWS transTypeMetaField = new MetaFieldValueWS();
		transTypeMetaField.setStringValue("010101");
		transTypeMetaField.setFieldName("Transaction Type Code");
		metaFields[0] = transTypeMetaField;
		item.setMetaFields(metaFields);
		api.updateItem(item);
		assertNotNull(item);
		// create order active since February 01, 2011
		order1 = CreateObjectUtil.createOrderObject(customerId1,
				SYSTEM_CURRENCY_ID, Constants.ORDER_BILLING_PRE_PAID, 1,
				new DateMidnight(2006, 10, 26).toDate());

		OrderLineWS line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setItemId(itemId1);
		line.setUseItem(true);
		line.setQuantity(1);
		order1.setOrderLines(new OrderLineWS[] { line });

		orderId1 = api.createOrder(order1);
		assertNotNull("Order Id cannot be null.", orderId1);

		return customerId1;
	}

	/**
	 * Resets the billing configuration to the default state found in a fresh
	 * load of the testing 'jbilling_test.sql' file.
	 * 
	 * @throws Exception
	 *             possible api exception
	 */
	private void resetBillingConfiguration() throws Exception {
		JbillingAPI api = JbillingAPIFactory.getAPI();

		BillingProcessConfigurationWS config = api
				.getBillingProcessConfiguration();
		config.setNextRunDate(new DateMidnight(2006, 11, 26).toDate());
		config.setGenerateReport(0);
		config.setDaysForReport(3);
		config.setRetries(0);
		config.setDaysForRetry(1);
		config.setDueDateValue(1);
		config.setDueDateUnitId(PeriodUnitDTO.MONTH);

		config.setOnlyRecurring(0);
		config.setInvoiceDateProcess(0);
		config.setMaximumPeriods(1);

		api.createUpdateBillingProcessConfiguration(config);

		// reset continuous invoice date
		PreferenceWS continuousDate = new PreferenceWS(new PreferenceTypeWS(
				Constants.PREFERENCE_CONTINUOUS_DATE), null);
		api.updatePreference(continuousDate);

	}
	
	@Override
	protected Hashtable<String, String> getOptionalParametersForSuretaxPlugin() {
		return null;
	}

	@Override
	protected Hashtable<String, String> getOptionalParametersForSuretaxDeletePlugin() {
		return null;
	}
}
