package in.webdata.ws

import spock.lang.Specification


import spock.lang.Shared

import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.order.OrderLineWS
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.util.api.JbillingAPI
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory
import java.lang.Integer;
import com.sapienter.jbilling.server.util.Constants;

public class WSTestinvoiceSpec extends Specification {


	@Shared  Integer []invoices;

	@Shared  JbillingAPI api;

	def setupSpec() {

		api = JbillingAPIFactory.getAPI();
	}

	def "testGet"() {

		setup:

		//JbillingAPI api = JbillingAPIFactory.getAPI();

		when:
		System.out.println("Getting invalid invoice");
		api.getInvoiceWS(75);

		then:
		thrown(Exception);

		when:
		System.out.println("Getting invoice");
		InvoiceWS retInvoice = api.getInvoiceWS(15);

		then:

		null			!=		retInvoice;
		retInvoice.getId()		==		 new Integer(15);
		System.out.println("Got = " + retInvoice);

		// latest
		// first, from a guy that is not mine
		when:
		api.getLatestInvoice(13);

		then:
		thrown(Exception);

		when:
		System.out.println("Getting latest invoice");
		retInvoice = api.getLatestInvoice(2);

		then:
		null			!=		retInvoice;
		retInvoice.getUserId()		==	 new Integer(2);
		System.out.println("Got = " + retInvoice);
		Integer lastInvoice = retInvoice.getId();

		// List of last
		// first, from a guy that is not mine

		when:
		api.getLastInvoices(13, 5);

		then:
		thrown(Exception);

		when:
		System.out.println("Getting last 5 invoices");

		invoices = api.getLastInvoices(2, 5);

		println(api.getLastInvoices(2, 5))
		println("Done");

		then:

		println("I am in when");

		null		!=		invoices;

		println("I am There");

		when:

		println("I am in when");
		retInvoice = api.getInvoiceWS(invoices[0]);

		then:

		new Integer(2)		==		 retInvoice.getUserId();

		println("Got = " + invoices.length + " invoices");

		for (int f = 0; f < invoices.length; f++) {
			println(" Invoice " + (f + 1) + invoices[f]);
		}

		when:
		// now I want just the two latest

		println("Getting last 2 invoices");

		println(api.getLastInvoices(2, 2))

		invoices = api.getLastInvoices(2, 2);

		retInvoice = api.getInvoiceWS(invoices[0]);


		then:


		null			!=		invoices;

		new Integer(2)	== retInvoice.getUserId();

		lastInvoice		==		retInvoice.getId();

		2				==		 invoices.length;

		when:

		System.out.println("Getting by date (empty)");

		Integer []invoices2 = api.getInvoicesByDate("2000-01-01", "2005-01-01");

		then:

		invoices2 != null

		true		==		(invoices2.length == 0);

		when:

		System.out.println("Getting by date");

		invoices2 = api.getInvoicesByDate("2006-01-01", "2007-01-01");

		null		!=		invoices2;

		then:

		false		==	(invoices2.length == 0);

		when:

		System.out.println("Got array " + invoices2.length + " getting " + invoices2[0]);

		retInvoice = api.getInvoiceWS(invoices2[0]);

		then:

		null	!=		retInvoice;

		System.out.println("Got invoice " + retInvoice);

		System.out.println("Done!");

	}

	def "testDelete"() {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		Integer invoiceId = new Integer(1);

		println(invoiceId)

		then:

		println(api.getInvoiceWS(2))
		null		!=		api.getInvoiceWS(invoices[invoiceId]);


		when:
		api.deleteInvoice(invoiceId);
		api.getInvoiceWS(invoiceId);
		then:
		thrown(Exception);
		println("Invoice should not have been deleted");
		// try to delete an invoice that is not mine

		when:
		api.deleteInvoice(new Integer(75));

		then:
		thrown(Exception);
		println("Not my invoice. It should not have been deleted");


	}


	def "testCreateInvoice"() {

		setup:

		final Integer USER_ID = 10730; // user has no orders

		JbillingAPI api = JbillingAPIFactory.getAPI();

		// setup order
		OrderWS order = new OrderWS();
		order.setUserId(USER_ID);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(1); // once
		order.setCurrencyId(1);

		OrderLineWS line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setDescription("Order line");
		line.setItemId(1);
		line.setQuantity(1);
		line.setPrice(new BigDecimal("10.00"));
		line.setAmount(new BigDecimal("10.00"));

		def ar = new OrderLineWS[1];
		ar[0]  = line;
		order.setOrderLines(ar);

		order.setOrderLines(ar);

		/*
		 * Test invoicing of one-time and recurring orders
		 */

		// create 1st order
		Integer orderId1 = api.createOrder(order);

		// create 2nd order
		line.setPrice(new BigDecimal("20.00"));

		line.setAmount(new BigDecimal("20.00"));

		Integer orderId2 = api.createOrder(order);

		// create invoice
		Integer[] invoices = api.createInvoice(USER_ID, false);

		InvoiceWS invoice = api.getInvoiceWS(invoices[0]);

		expect:

		1			==		 invoices.length;

		null		==		invoice.getDelegatedInvoiceId();


		BigDecimal.ZERO		==		 invoice.getCarriedBalanceAsDecimal();

		Integer[] invoicedOrderIds = invoice.getOrders();

		2		==		 invoicedOrderIds.length;

		Arrays.sort(invoicedOrderIds);

		orderId1		==		 invoicedOrderIds[0];

		orderId2		==		 invoicedOrderIds[1];

		//println("\n\n>>>>>>>>>>>>>>>>"+ invoice.getTotalAsDecimal()+"<<<<<<<<<<<<<<<<<<\n\n");

		new BigDecimal("30.00")		==		 invoice.getTotalAsDecimal();

		when:

		api.deleteInvoice(invoices[0]);

		api.deleteOrder(orderId1);

		api.deleteOrder(orderId2);

		/*
		 * Test only recurring order can generate invoice.
		 */

		// one-time order
		line.setPrice(new BigDecimal("2.00"));

		line.setAmount(new BigDecimal("2.00"));

		orderId1 = api.createOrder(order);

		// try to create invoice, but none should be returned
		invoices = api.createInvoice(USER_ID, true);

		then:

		println("Parameters Intialized");
		// Note: CXF returns null for empty array

		when:

		invoices != null

		then:
		0		==		 invoices.length;

		// recurring order

		when:

		order.setPeriod(2); // monthly

		line.setPrice(new BigDecimal("3.00"));

		line.setAmount(new BigDecimal("3.00"));

		orderId2 = api.createOrder(order);

		// create invoice
		invoices = api.createInvoice(USER_ID, true);

		then:

		1		==		 invoices.length;

		//*This test	invoice == api.getInvoiceWS(invoices[0]);


		invoicedOrderIds == invoice.getOrders();

		3		==		 invoicedOrderIds.length;

		Arrays.sort(invoicedOrderIds);

		//println("\n\n>>>>>>>>>>>>>>>>"+invoicedOrderIds[0]+"<<<<<<<<<<<<<<<<<<\n\n");

		orderId1		==		 invoicedOrderIds[0];

		orderId2		==		 invoicedOrderIds[1];

		new BigDecimal("5.00")		==		 invoice.getTotalAsDecimal();

		// clean up

		when:

		api.deleteInvoice(invoices[0]);

		api.deleteOrder(orderId1);

		api.deleteOrder(orderId2);

		then:

		println("Invoice and Orders are deleted Sucessfully")

	}

	def "testCreateInvoiceFromOrder"()  {

		setup:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		final Integer USER_ID = 10730; // user has no orders

		// setup orders
		OrderWS order = new OrderWS();
		order.setUserId(USER_ID);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(1); // once
		order.setCurrencyId(1);

		OrderLineWS line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setDescription("Order line");
		line.setItemId(1);
		line.setQuantity(1);
		line.setPrice(new BigDecimal("10.00"));
		line.setAmount(new BigDecimal("10.00"));

		def ar = new OrderLineWS[1];

		ar[0]  = line;

		order.setOrderLines(ar);

		// create orders
		Integer orderId1 = api.createOrder(order);
		Integer orderId2 = api.createOrder(order);

		// generate invoice using first order
		Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

		Integer[] invoiceIds = api.getLastInvoices(USER_ID, 2);

		InvoiceWS invoice = api.getInvoiceWS(invoiceId);

		invoiceIds = api.getLastInvoices(USER_ID, 2);

		invoice = api.getInvoiceWS(invoiceId);

		expect:

		null		!=		orderId1;
		null		!=		orderId2;
		null		!=		invoiceId;



		2		==		 invoiceIds.length;



		new BigDecimal("10.00")		==		 invoice.getTotalAsDecimal();

		1							== invoice.getOrders().length;

		orderId1					==	invoice.getOrders()[0];

		// add second order to invoice
		Integer invoiceId2 = api.createInvoiceFromOrder(orderId2, invoiceId);
		invoiceId					==	 invoiceId2;


		1		== invoiceIds.length;


		new BigDecimal("20.00")		== invoice.getTotalAsDecimal();
		2								== invoice.getOrders().length;

		// cleanup
		when:

		api.deleteInvoice(invoiceId);
		api.deleteOrder(orderId1);
		api.deleteOrder(orderId2);

		then:
		println("Invoice Deleted Sucessfully.")
	}

	def "testCreateInvoiceSecurity"() {

		setup:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		when:
		api.createInvoice(13, false);

		then:
		thrown(Exception);
		println("User 13 belongs to entity 2");

	}


	/**
	 //	 * Tests that when a past due invoice is processed it will generate a new invoice for the
	 //	 * current period that contains all previously un-paid balances as the carried balance.
	 //	 *
	 //	 * Invoices that have been carried still show the original balance for reporting/paper-trail
	 //	 * purposes, but will not be re-processed by the system as part of the normal billing process.
	 //	 *
	 //	 * @throws Exception
	 //	 */
	def "testCreateWithCarryOver"() {

		setup:

		final Integer USER_ID = 10743;          // user has one past-due invoice to be carried forward
		final Integer OVERDUE_INVOICE_ID = 70;  // holds a $20 balance

		JbillingAPI api = JbillingAPIFactory.getAPI();


		// new order witha  single line item
		OrderWS order = new OrderWS();
		order.setUserId(USER_ID);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(1); // once
		order.setCurrencyId(1);

		OrderLineWS line = new OrderLineWS();
		def ar = new OrderLineWS[1];
		ar[0]  = line;
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setDescription("Order line");
		line.setItemId(1);
		line.setQuantity(1);
		line.setPrice(new BigDecimal("10.00"));
		line.setAmount(new BigDecimal("10.00"));

		order.setOrderLines(ar);

		// create order
		Integer orderId = api.createOrder(order);

		// create invoice
		Integer invoiceId = api.createInvoice(USER_ID, false)[0];

		// validate that the overdue invoice has been carried forward to the newly created invoice
		InvoiceWS overdue = api.getInvoiceWS(OVERDUE_INVOICE_ID);

		InvoiceWS invoice = api.getInvoiceWS(invoiceId);

		expect:

		Constants.INVOICE_STATUS_UNPAID_AND_CARRIED		==		 overdue.getStatusId();

		0		==		  overdue.getToProcess().intValue();

		new  BigDecimal("20.00")		==		  overdue.getBalanceAsDecimal();

		invoiceId		==		  overdue.getDelegatedInvoiceId();

		// validate that the newly created invoice contains the carried balance


		new BigDecimal("10.00")		==		 invoice.getBalanceAsDecimal();

		overdue.getBalanceAsDecimal()		==		  invoice.getCarriedBalanceAsDecimal();

		new BigDecimal("30.00")		==		  invoice.getTotalAsDecimal();
	}

	def "testGetUserInvoicesByDate"() {

		setup:

		final Integer USER_ID = 2; // user has some invoices
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// invoice dates: 2006-07-26
		// select the week
		Integer[] result = api.getUserInvoicesByDate(USER_ID, "2006-07-23", "2006-07-29");


		// note: invoice 1 gets deleted

		expect:

		3		==		  result.length;

		4		==		  result[0].intValue();

		3		==		  result[1].intValue();

		2		==		  result[2].intValue();

		// test since date inclusive

		when:

		result = api.getUserInvoicesByDate(USER_ID, "2006-07-26", "2006-07-29");

		then:

		3		==		result.length;

		4		==		 result[0].intValue();

		3		==		result[1].intValue();

		2		==		 result[2].intValue();

		// test until date inclusive
		when:

		result = api.getUserInvoicesByDate(USER_ID, "2006-07-23", "2006-07-26");

		then:

		3		==		 result.length;

		4		==		  result[0].intValue();

		3		==		  result[1].intValue();

		2		==		  result[2].intValue();

		// test date with no invoices
		when:

		result = api.getUserInvoicesByDate(USER_ID, "2005-07-23", "2005-07-29");
		// Note: CXF returns null for empty array

		result != null

		then:
		0		==		 result.length;

	}

	def "testGetTotalAsDecimal"() {

		setup:

		List<Integer> invoiceIds = new ArrayList<Integer>();

		List<Integer> orderIds = new ArrayList<Integer>();

		JbillingAPI api = null;

		final Integer USER_ID = 10730; // user has no orders
		api = JbillingAPIFactory.getAPI();

		// test BigDecimal behavior

		expect:

		false		==		(new BigDecimal("1.1").equals(new BigDecimal("1.10")));
		true		==		(new BigDecimal("1.1").compareTo(new BigDecimal("1.10")) == 0);

		// with items 2 and 3 10% discount should apply

		when:

		orderIds.add(api.createOrder(com.sapienter.jbilling.server.order.WSTest.createMockOrder(USER_ID, 3, new BigDecimal("0.32"))));

		orderIds.add(api.createOrder(com.sapienter.jbilling.server.order.WSTest.createMockOrder(USER_ID, 3, new BigDecimal("0.32"))));

		invoiceIds.addAll(Arrays.asList(api.createInvoice(USER_ID, false)));

		InvoiceWS invoice = api.getInvoiceWS(invoiceIds.get(0));


		then:

		1		==		invoiceIds.size();


		new BigDecimal("1.728")		==		 invoice.getTotalAsDecimal();

		println(invoice.getTotal());

		"1.73"		==		 invoice.getTotal();



		when:

		for (Integer integer : invoiceIds) {
			api.deleteInvoice(integer);
		}
		println("Successfully deleted invoices: " + invoiceIds.size());
		for (Integer integer : orderIds) {
			api.deleteOrder(integer);
		}

		then:

		println("Successfully deleted orders: " + orderIds.size());

	}

	def "testGetPaperInvoicePDF"() {

		setup:

		final Integer USER_ID = 2; // user has invoices

		JbillingAPI api = JbillingAPIFactory.getAPI();

		Integer[] invoiceIds = api.getLastInvoices(USER_ID, 1);

		byte[] pdf = api.getPaperInvoicePDF(invoiceIds[0]);

		expect:

		1		==		 invoiceIds.length;

		true	==		pdf.length > 0;
	}

	def assertEquals(BigDecimal expected, BigDecimal actual) {
		assertEquals(null, expected, actual);
	}

	def assertEquals(String message, BigDecimal expected, BigDecimal actual) {
		if (expected == null && actual == null) {
			return;
		}
		expected.compareTo(actual) == 0;
	}
}

