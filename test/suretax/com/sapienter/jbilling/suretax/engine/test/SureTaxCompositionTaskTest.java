package com.sapienter.jbilling.suretax.engine.test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.CreateObjectUtil;

public class SureTaxCompositionTaskTest extends DefaultTest {
	private static final Integer LONG_DISTANCE_CALL_ITEM_ID = 2801; // taxable
																	// item
	private static final Integer MONTHLY_PERIOD = 2;

	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();
	}

	public static UserWS createUser(long millis) throws JbillingAPIException,
			IOException {
		return createUser(millis, "80301");
	}

	public static UserWS createUser(long millis, String postalCode)
			throws JbillingAPIException, IOException {
		// create a user for testing

		UserWS user = new UserWS();
		user.setUserName("invoice-suretax-" + millis);
		user.setPassword("P@ssword1");
		user.setLanguageId(1);
		user.setCurrencyId(1);
		user.setMainRoleId(5);
		user.setStatusId(UserDTOEx.STATUS_ACTIVE);

		ContactWS contact = new ContactWS();
		contact.setEmail("gurdev.parmar@gmail.com");
		contact.setFirstName("Gurdev");
		contact.setLastName("Parmar");
		if (postalCode != null) {
			contact.setPostalCode(postalCode);
		}
		user.setContact(contact);

		user.setUserId(JbillingAPIFactory.getAPI().createUser(user)); // create
																		// user
		return user;
	}

	public void testInvoiceGenerationFor2OrMoreOrders() throws IOException,
			JbillingAPIException {

		int currentTimeMillis = (int) System.currentTimeMillis();
		if (currentTimeMillis < 0) {
			currentTimeMillis = currentTimeMillis * -1;
		}
		UserWS user = createUser(currentTimeMillis);
		assertNotNull("customer created", user.getUserId());

		// Create two items now
		// First item: Long distance call with $1 rate.

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
		// new ItemDTOEx(null, ""+currentTimeMillis, glCode, entity,
		// description, deleted, currencyId, price, percentage, orderLineTypeId,
		// hasDecimals)

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
				new Date(), PeriodUnitDTO.DAY, 45, false);

		assertEquals("1 invoice generated", 1, invoiceIds.length);

		InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
		// InvoiceWS invoice = invoices != null && invoices.length > 0 ?
		// invoices[0]
		// : null;
		assertNotNull("Generated invoice was null", invoice);
		assertEquals("Customer Id  & Invoice User Id are not same.",
				new Integer(user.getUserId()), new Integer(invoice.getUserId()));
		assertEquals(8, invoice.getInvoiceLines().length);
		System.out.println("Number of invoice lines:"
				+ invoice.getInvoiceLines().length);
	}

	public void testGenerateInvoiceFromPlanOrder() throws JbillingAPIException,
			IOException {
		int currentTimeMillis = (int) System.currentTimeMillis();
		if (currentTimeMillis < 0) { //
			currentTimeMillis = currentTimeMillis * -1;
		}

		// Create a plan

		MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
		metaFieldValueWS.setFieldName("Transaction Type Code");
		metaFieldValueWS.setStringValue("010101");
		MetaFieldValueWS[] metaFieldValueWSs = new MetaFieldValueWS[1];
		metaFieldValueWSs[0] = metaFieldValueWS;

		// create User
		UserWS customer = createUser(currentTimeMillis);

		Integer customerId = customer.getUserId();
		assertNotNull("Customer/User ID should not be null", customerId);

		// create order
		Integer plansItemId = plan.getItemId();
		OrderWS planItemBasedOrder = new OrderWS(); //
		// planItemBasedOrder.set
		planItemBasedOrder = getUserSubscriptionToPlan(new Date(),
				BigDecimal.TEN, customerId, 1, 1, plansItemId);
		// [3/15/2013 8:27:27 PM] Amol Ashok Gadre:
		Integer orderId = api.createOrder(planItemBasedOrder);
		assertNotNull("Order Id cannot be null.", orderId);

		// get order //
		planItemBasedOrder = api.getOrder(orderId);
		assertNotNull("Order must not be null.", planItemBasedOrder);

		Integer[] invoiceIds = api.createInvoiceWithDate(customerId,
				new Date(), PeriodUnitDTO.DAY, 45, false); // //
		// InvoiceWS[] invoices = api.getAllInvoicesForUser(customerId); //
		assertEquals("1 invoice generated", 1, invoiceIds.length); // //
		InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]); // // InvoiceWS
		// invoice = invoices != null && invoices.length > 0 ? invoices[0]
		// : null; //
		assertNotNull("Generated invoice was null", invoice);
		assertEquals(7, invoice.getInvoiceLines().length);

	}

	public void testNonTaxPercentageItem() throws JbillingAPIException,
			IOException {
		// create User
		int currentTimeMillis = (int) System.currentTimeMillis();
		if (currentTimeMillis < 0) {
			currentTimeMillis = currentTimeMillis * -1;
		}
		UserWS user = createUser(currentTimeMillis);
		int customerId = user.getUserId();
		assertNotNull("Customer/User should not be null", user);

		Integer itemId = CreateObjectUtil.createItem("Long distance Calls",
				"15.0", System.currentTimeMillis() + "", "2201", api);
		ItemDTOEx item1 = api.getItem(itemId, null, null);
		MetaFieldValueWS[] metaFields = new MetaFieldValueWS[1];
		MetaFieldValueWS transTypeMetaField = new MetaFieldValueWS();
		transTypeMetaField.setStringValue("010101");
		transTypeMetaField.setFieldName("Transaction Type Code");
		metaFields[0] = transTypeMetaField;
		item1.setMetaFields(metaFields);
		api.updateItem(item1);

		ItemDTOEx percentageItem = CreateObjectUtil.createPercentageItem(1,
				new BigDecimal(10), 1, 2201, "A Percentage Item");
		percentageItem.setId(api.createItem(percentageItem));
		percentageItem.setMetaFields(metaFields);
		percentageItem.setHasDecimals(0);
		api.updateItem(percentageItem);
		Calendar cal = Calendar.getInstance();
		OrderWS order = CreateObjectUtil.createOrderObject(customerId, 1,
				Constants.ORDER_BILLING_POST_PAID, 1, cal.getTime());

		// OrderLineWS orderLineWS = new OrderLineWS(id, itemId, description,
		// amount, quantity, price, create, deleted, newTypeId, editable,
		// orderId, useItem, version, provisioningStatusId,
		// provisioningRequestId)
		CreateObjectUtil.addLine(order, 10, Constants.ORDER_LINE_TYPE_ITEM,
				itemId, new BigDecimal(1.5), "Long Distance Call-intra state");
		CreateObjectUtil.addLine(order, 1, Constants.ORDER_LINE_TYPE_ITEM,
				percentageItem.getId(), new BigDecimal(10),
				"Long Distance Call-inter state");
		order.setDueDateUnitId(PeriodUnitDTO.DAY);
		order.setDueDateValue(0);// order due
		System.out.println("Creaating order");

		order.setId(api.createOrder(order)); // create order
		order = api.getOrder(order.getId());
		assertNotNull("order created", order.getId());

		Integer[] invoiceIds = api.createInvoiceWithDate(customerId,
				new Date(), PeriodUnitDTO.DAY, 45, false);
		// getAllInvoicesForUser(user.getUserId());
		assertEquals("1 invoice generated", 1, invoiceIds.length);

		InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
		assertNotNull("Generated invoice was null", invoice);
		assertEquals("Customer Id  & Invoice User Id are not same.",
				new Integer(user.getUserId()), new Integer(invoice.getUserId()));
		assertEquals(8, invoice.getInvoiceLines().length);
		System.out.println("Invoice:" + invoice);
		System.out.println("number of invoice lines:"
				+ invoice.getInvoiceLines().length);
	}

	private OrderWS getUserSubscriptionToPlan(Date since, BigDecimal cost,
			Integer userId, Integer billingType, Integer orderPeriodID,
			Integer plansItemId) {

		System.out.println("Got plan Item Id as " + plansItemId);
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(billingType);
		order.setPeriod(orderPeriodID);
		order.setCurrencyId(1);
		order.setActiveSince(since);

		// MetaFieldValueWS metaField1 = new MetaFieldValueWS();
		// metaField1.setFieldName("Bill To User");
		// metaField1.setValue("0"); // The bill to user will be self in this
		// case.
		// order.setMetaFields(new MetaFieldValueWS[] { metaField1 });

		OrderLineWS line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(1);
		line.setDescription("Order line for plan subscription");
		line.setItemId(plansItemId);
		line.setUseItem(true);

		order.setOrderLines(new OrderLineWS[] { line });

		System.out.println("User subscription...");
		return order;
	}

	@Override
	protected Hashtable<String, String> getOptionalParametersForSuretaxPlugin() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Hashtable<String, String> getOptionalParametersForSuretaxDeletePlugin() {
		// TODO Auto-generated method stub
		return null;
	}
}
