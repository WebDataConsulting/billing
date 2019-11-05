package in.webdata.ws

import java.math.BigDecimal;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.lang.Object;

import junit.framework.TestCase;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.OrderLineWS
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.ValidatePurchaseWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.joda.time.DateMidnight;
import spock.lang.Specification

public class WSTestOrderSpec  extends Specification {

	def  Integer GANDALF_USER_ID = 2;

	def "testCreateUpdateDelete"() {

		setup:

		JbillingAPI api = JbillingAPIFactory.getAPI();
		int i;

		OrderWS newOrder = new OrderWS();
		newOrder.setUserId(GANDALF_USER_ID);
		newOrder.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		newOrder.setPeriod(new Integer(1)); // once
		newOrder.setCurrencyId(new Integer(1));
		// notes can only be 200 long... but longer should not fail
		newOrder.setNotes("At the same time the British Crown began bestowing land grants in Nova Scotia on favored subjects to encourage settlement and trade with the mother country. In June 1764, for instance, the Boards of Trade requested the King make massive land grants to such Royal favorites as Thomas Pownall, Richard Oswald, Humphry Bradstreet, John Wentworth, Thomas Thoroton[10] and Lincoln's Inn barrister Levett Blackborne.[11] Two years later, in 1766, at a gathering at the home of Levett Blackborne, an adviser to the Duke of Rutland, Oswald and his friend James Grant were released from their Nova Scotia properties so they could concentrate on their grants in British East Florida.");
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2008, 9, 3);
		newOrder.setCycleStarts(cal.getTime());

		// now add some lines
		OrderLineWS []lines = new OrderLineWS[3];
		OrderLineWS line;

		line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("Fist line");
		line.setItemId(new Integer(1));
		lines[0] = line;

		// this is an item line
		line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setItemId(new Integer(2));
		// take the description from the item
		line.setUseItem(new Boolean(true));
		lines[1] = line;

		// this is an item line
		line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setItemId(new Integer(3));
		line.setUseItem(new Boolean(true));
		lines[2] = line;

		newOrder.setOrderLines(lines);

		println("Creating order ...");
		Integer ret = api.createOrderAndInvoice(newOrder);

		expect:

		null		!=		 ret;

		when:

		// create another one so we can test get by period.
		ret = api.createOrderAndInvoice(newOrder);
		println("Created invoice " + ret);
		InvoiceWS newInvoice = api.getInvoiceWS(ret);
		ret = newInvoice.getOrders()[0]; // this is the order that was also created



		api.getOrder(new Integer(5));

		then:

		println("Order 5 belongs to entity 2");

		thrown(Exception)

		when:

		System.out.println("Getting created order " + ret);
		OrderWS retOrder = api.getOrder(ret);
		//System.out.println("Got:" + retOrder);

		then:

		retOrder.getBillingTypeId()		==         newOrder.getBillingTypeId();

		retOrder.getPeriod()		==		newOrder.getPeriod();

		retOrder.getCycleStarts().getTime()			==		newOrder.getCycleStarts().getTime();

		/*
		 * get order line. The new order should include a new discount
		 * order line that comes from the rules.
		 */
		// try getting one that doesn't belong to us

		when:
		println("Getting bad order line");

		api.getOrderLine(new Integer(6));
		then:

		thrown(Exception);
		println("Order line 6 belongs to entity 6");

		println("Getting created order line");

		// make sure that item 2 has a special price



		for (OrderLineWS item2line: retOrder.getOrderLines()) {
			if (item2line.getItemId() == 2) {
				"30.00"		==		 item2line.getPrice();
				break;
			}
		}

		when:

		boolean found = false;
		OrderLineWS retOrderLine = null;
		OrderLineWS normalOrderLine = null;
		Integer lineId = null;

		then:
		for (i = 0; i < retOrder.getOrderLines().length; i++) {
			lineId = retOrder.getOrderLines()[i].getId();
			retOrderLine = api.getOrderLine(lineId);
			if (retOrderLine.getItemId().equals(new Integer(14))) {
				retOrderLine.getItemId()	==		new Integer(14);
				"-5.50"		==		 retOrderLine.getAmount();
				found = true;
			} else {
				normalOrderLine = retOrderLine;
				if (found) break;
			}
		}

		true		==		 found;


		when:

		retOrderLine = normalOrderLine; // use a normal one, not the percentage
		retOrderLine.setQuantity(new Integer(99));
		lineId = retOrderLine.getId();

		println("Updating bad order line");
		retOrderLine.setId(new Integer(6));
		api.updateOrderLine(retOrderLine);

		then:

		println("Order line 6 belongs to entity 301");
		thrown(Exception)


		when:
		retOrderLine.setId(lineId);
		println("Update order line " + lineId);
		api.updateOrderLine(retOrderLine);
		retOrderLine = api.getOrderLine(retOrderLine.getId());
		retOrderLine.setQuantity(new Integer(0));


		then:

		"0.00"		==		 retOrderLine.getQuantity();
		//delete a line through updating with quantity = 0
		println("Delete order line");
		api.updateOrderLine(retOrderLine);
		int totalLines = retOrder.getOrderLines().length;

		pause(2000); // pause while provisioning status is being updated

		when:
		retOrder = api.getOrder(retOrder.getId());
		// the order has to have one less line now

		then:
		totalLines		==		(retOrder.getOrderLines().length + 1);

		/*
		 * Update
		 */
		// now update the created order

		when:

		cal.clear();

		cal.set(2003, 9, 29, 0, 0, 0);

		retOrder.setActiveSince(cal.getTime());
		retOrder.getOrderLines()[1].setDescription("Modified description");
		retOrder.getOrderLines()[1].setQuantity(new Integer(2));
		retOrder.setStatusId(new Integer(2));
		// also update the next billable day
		retOrder.setNextBillableDay(cal.getTime());
		println("Updating order...");
		api.updateOrder(retOrder);

		// try to update an order of another entity

		println("Updating bad order...");

		retOrder.setId(new Integer(5));

		api.updateOrder(retOrder);


		then:

		println("Order 5 belongs to entity 2");

		thrown(Exception)

		// and ask for it to verify the modification

		when:

		println("Getting updated order ");

		retOrder = api.getOrder(ret);

		null		!=		retOrder;
		true		==		(retOrder.getActiveSince().compareTo(cal.getTime()) == 0);

		new Integer(2)	==	 retOrder.getStatusId();

		"Modified description"		==			retOrder.getOrderLines()[1].getDescription();

		then:

		"2.00"		==		 retOrder.getOrderLines()[1].getQuantity();
		cal.getTimeInMillis()		==		retOrder.getNextBillableDay().getTime();
		for (i = 0; i < retOrder.getOrderLines().length; i++) {
			retOrderLine = retOrder.getOrderLines()[i];
			if (retOrderLine.getItemId().equals(new Integer(14))) {
				// the is one less line for 15
				// but one extra item for 30
				// difference is 15 and 10% of that is 1.5  thus 5.5 + 1.5 = 7
				"-7.00"		==		 retOrderLine.getAmount();
				break;
			}
		}

		false		==		(i == retOrder.getOrderLines().length);

		/*
		 * Get latest
		 */

		when:


		println("Getting latest");

		OrderWS lastOrder = api.getLatestOrder(new Integer(2));

		then:

		null		!=		lastOrder;

		ret			==		 lastOrder.getId();
		// now one for an invalid user

		when:

		System.out.println("Getting latest invalid");

		retOrder = api.getLatestOrder(new Integer(13));

		then:
		thrown(Exception)
		println("User 13 belongs to entity 2");

		/*
		 * Get last
		 */
		when:

		println("Getting last 5 ... ");
		Integer	[]list = api.getLastOrders(new Integer(2), new Integer(5));

		then:

		println(list);
		//>>>>>>>>			null		!=		 list.size;
		true		==		((list.length <= 5) && (list.length > 0));

		// the first in the list is the last one created
		//            retOrder = api.getOrder(new Integer(list[0]));
		//            ret			==		 retOrder.getId();


		// try to get the orders of my neighbor
		when:

		println("Getting last 5 - invalid");
		api.getOrder(new Integer(5));

		then:
		thrown(Exception)
		println("User 13 belongs to entity 2");


		//            /*
		//             * Delete
		//             */
		//
		when:

		println("Deleteing order " + ret);

		api.deleteOrder(ret);
		// try to delete from my neightbor

		api.deleteOrder(new Integer(5));

		then:

		thrown(Exception)
		println("Order 5 belongs to entity 2");

		when:
		// try to get the deleted order
		println("Getting deleted order ");
		retOrder = api.getOrder(ret);

		then:

		1		==		 retOrder.getDeleted();

		/*
		 * Get by user and period
		 */
		when:

		println("Getting orders by period for invalid user " + ret);
		// try to get from my neightbor


		api.getOrderByPeriod(new Integer(13), new Integer(1));

		then:
		thrown(Exception)
		println("User 13 belongs to entity 2");
		//
		//            // now from a valid user
		when:

		println("Getting orders by period ");
		Integer []orders = api.getOrderByPeriod(new Integer(2), new Integer(1));

		println("Got total orders " + orders.length +" first is " + orders[0]);

		/*
		 * Create an order with pre-authorization
		 */
		println("Create an order with pre-authorization" + ret);
		PaymentAuthorizationDTOEx auth = (PaymentAuthorizationDTOEx)api.createOrderPreAuthorize(newOrder);

		then:

		null		!=		 auth;
		// the test processor should always approve gandalf
		new Boolean(true)		==		 auth.getResult();
		println("Order pre-authorized. Approval code = " + auth.getApprovalCode());
		// check the last one is a new one
		pause(2000); // pause while provisioning status is being updated

		when:

		println("Getting latest");
		retOrder = api.getLatestOrder(new Integer(2));
		println("Order created with ID = " + retOrder.getId());

		then:

		//>>>>>>>>.			   retOrder.getId()		==		 lastOrder.getId();

		println("Deleteing order " + retOrder.getId());

		when:

		println("Deleteing order " + retOrder.getId());

		api.deleteOrder(retOrder.getId());

		then:

		println("Exception caught");
		//	thrown(Exception)

	}

	def "testcreateOrderAndInvoiceAutoCreatesAnInvoice"()  {

		when:

		final int USER_ID = GANDALF_USER_ID;

		InvoiceWS before = callGetLatestInvoice(USER_ID);

		then:

		(before == null || before.getId()) != null;

		when:

		OrderWS order = createMockOrder(USER_ID, 3, new BigDecimal("42.00"));

		Integer invoiceId = callcreateOrderAndInvoice(order);

		InvoiceWS afterNormalOrder = callGetLatestInvoice(USER_ID);

		then:

		null		!=		invoiceId;

		null		!=		afterNormalOrder;
		null		!=		afterNormalOrder.getId();

		when:
		before != null

		then:
		false		==		afterNormalOrder.getId().equals(before.getId());


		//even if empty
		when:
		OrderWS emptyOrder = createMockOrder(USER_ID, 0, new BigDecimal("123.00"));
		Integer emptyOrderId = callcreateOrderAndInvoice(emptyOrder);

		then:

		null		!=		emptyOrderId;

		InvoiceWS afterEmptyOrder = callGetLatestInvoice(USER_ID);
		null		!=		afterEmptyOrder.getId();
		null		!=		afterEmptyOrder;
		false		==		afterNormalOrder.getId().equals(afterEmptyOrder.getId());
	}

	def "testCreateNotActiveOrderDoesNotCreateInvoices"()  {

		when:

		final int USER_ID = GANDALF_USER_ID;

		InvoiceWS before = callGetLatestInvoice(USER_ID);

		OrderWS orderWS = createMockOrder(USER_ID, 2, new BigDecimal("234.00"));

		orderWS.setActiveSince(weeksFromToday(1));

		JbillingAPI api = JbillingAPIFactory.getAPI();

		Integer orderId = api.createOrder(orderWS);

		InvoiceWS after = callGetLatestInvoice(USER_ID);

		then:
		null		!=		orderId;



		when:
		before == null

		then:

		null		!=		after;

		when:
		before != null

		then:
		before.getId()		==		after.getId();

	}


	def "testCreatedOrderIsCorrect"()  {

		when:

		def USER_ID = GANDALF_USER_ID;

		def LINES = 2;

		OrderWS requestOrder = createMockOrder(USER_ID, LINES, new BigDecimal("567.00"));

		then:

		println("Into the then Block.");
		LINES		==		 requestOrder.getOrderLines().length;

		Integer orderId = callcreateOrderAndInvoice(requestOrder);

		null		!=		orderId;

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		OrderWS resultOrder = api.getOrder(orderId);

		then:

		null		!=		resultOrder;

		orderId		==		 resultOrder.getId();

		LINES		==		 resultOrder.getOrderLines().length;

		when:

		HashMap<String, OrderLineWS> actualByDescription = new HashMap<String, OrderLineWS>();

		then:
		for (OrderLineWS next : resultOrder.getOrderLines()){


			null		!=		next.getId();
			null		!=		next.getDescription();
			actualByDescription.put(next.getDescription(), next);
		}

		when:

		println("Getting the nexRequest")

		then:

		for (int i = 0; i < LINES; i++){
			OrderLineWS nextRequested = requestOrder.getOrderLines()[i];
			OrderLineWS nextActual = actualByDescription.remove(nextRequested.getDescription());

			null		!=		nextActual;

			nextRequested.getDescription()		==		nextActual.getDescription();
			nextRequested.getAmountAsDecimal()		==		nextActual.getAmountAsDecimal();
			nextRequested.getQuantityAsDecimal()	==		 nextActual.getQuantityAsDecimal();
			nextRequested.getQuantityAsDecimal()	== nextActual.getQuantityAsDecimal();
		}
	}

	def "testAutoCreatedInvoiceIsCorrect"() {


		when:
		int USER_ID = GANDALF_USER_ID;
		int LINES = 2;

		// it is critical to make sure that this invoice can not be composed by
		// previous payments
		// so, make the price unusual
		BigDecimal PRICE = new BigDecimal("687654.29");

		OrderWS orderWS = createMockOrder(USER_ID, LINES, PRICE);
		Integer orderId = callcreateOrderAndInvoice(orderWS);
		InvoiceWS invoice = callGetLatestInvoice(USER_ID);

		//new Integer[] {orderId}

		def ar	=	new Integer[1];

		ar[0]	=	orderId;


		then:

		null		!=		invoice.getOrders();


		true		==		 Arrays.equals(ar, invoice.getOrders());

		null		!=		invoice.getInvoiceLines();

		LINES		==		 invoice.getInvoiceLines().length;

		//>>>>>    	null		==	invoice.getPayments();
		Integer.valueOf(0)		==		 invoice.getPaymentAttempts();

		null		!=		invoice.getBalance();

		PRICE.multiply(new BigDecimal(LINES))		==		 invoice.getBalanceAsDecimal();
	}

	def "testAutoCreatedInvoiceIsPayable"()  {

		when:

		final int USER_ID = GANDALF_USER_ID;
		callcreateOrderAndInvoice(createMockOrder(USER_ID, 1, new BigDecimal("789.00")));
		InvoiceWS invoice = callGetLatestInvoice(USER_ID);

		then:

		null		!=		invoice;
		null		!=		invoice.getId();
		1		==		 invoice.getToProcess().intValue();
		true		==		BigDecimal.ZERO.compareTo(invoice.getBalanceAsDecimal()) < 0;

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();
		PaymentAuthorizationDTOEx auth = api.payInvoice(invoice.getId());

		then:

		null		!=		auth;
		true		==		 auth.getResult().booleanValue();
		"The transaction has been approved"		==		auth.getResponseMessage();

		// payment date should not be null (bug fix)
		null		!=		api.getLatestPayment(USER_ID).getPaymentDate();

		// now the invoice should be shown as paid
		when:

		invoice = callGetLatestInvoice(USER_ID);

		then:

		null		!=		invoice;
		null		!=		invoice.getId();
		0			==			 invoice.getToProcess().intValue();
		true		==		(BigDecimal.ZERO.compareTo(invoice.getBalanceAsDecimal()) == 0);

	}

	def "testEmptyInvoiceIsNotPayable"() {

		when:
		final int USER_ID = GANDALF_USER_ID;
		callcreateOrderAndInvoice(createMockOrder(USER_ID, 0, new BigDecimal("890.00")));
		InvoiceWS invoice = callGetLatestInvoice(USER_ID);

		then:

		null		!=		invoice;
		null		!=		invoice.getId();

		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();
		PaymentAuthorizationDTOEx auth = api.payInvoice(invoice.getId());

		then:
		null		==		auth;
	}

	private Date weeksFromToday(int weekNumber) {
		Calendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.WEEK_OF_YEAR, weekNumber);
		return calendar.getTime();
	}

	private InvoiceWS callGetLatestInvoice(int userId) throws Exception {
		JbillingAPI api = JbillingAPIFactory.getAPI();
		return api.getLatestInvoice(userId);
	}



	private Integer callcreateOrderAndInvoice(OrderWS order) throws Exception {
		JbillingAPI api = JbillingAPIFactory.getAPI();
		InvoiceWS invoice = api.getInvoiceWS(api.createOrderAndInvoice(order));
		return invoice.getOrders()[0];
	}



	def OrderWS createMockOrder(int userId, int orderLinesCount, BigDecimal linePrice) {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(1); // once
		order.setCurrencyId(1);

		ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(orderLinesCount);
		for (int i = 0; i < orderLinesCount; i++){
			OrderLineWS nextLine = new OrderLineWS();
			nextLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
			nextLine.setDescription("Order line: " + i);
			nextLine.setItemId(i + 1);
			nextLine.setQuantity(1);
			nextLine.setPrice(linePrice);
			nextLine.setAmount(nextLine.getQuantityAsDecimal().multiply(linePrice));

			lines.add(nextLine);
		}
		order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
		return order;
	}

	private void assertEmptyArray(Object[] array){
		// CXF returns null for empty array
		//assertNotNull(array);
		if (array != null) {
			0		==		array.length;
		}
	}

	def "testUpdateLines"() {


		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();
		Integer orderId = new Integer(15);
		OrderWS order = api.getOrder(orderId);
		int initialCount = order.getOrderLines().length;
		println("Got order with " + initialCount + " lines");

		// let's add a line
		OrderLineWS line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setItemId(new Integer(14));
		line.setUseItem(new Boolean(true));

		ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>();
		Collections.addAll(lines, order.getOrderLines());
		lines.add(line);
		OrderLineWS[] aLines = new OrderLineWS[lines.size()];
		lines.toArray(aLines);
		order.setOrderLines(aLines);

		// call the update
		println("Adding one order line");
		api.updateOrder(order);

		// let's see if my new line is there
		order = api.getOrder(orderId);
		println("Got updated order with " + order.getOrderLines().length + " lines");

		then:
		initialCount + 1		==			order.getOrderLines().length;

		// and again
		when:

		initialCount = order.getOrderLines().length;
		lines = new ArrayList<OrderLineWS>();
		Collections.addAll(lines, order.getOrderLines());
		line.setItemId(1); // to add another line, you need a different item
		lines.add(line);
		aLines = new OrderLineWS[lines.size()];
		println("lines now " + aLines.length);
		lines.toArray(aLines);
		order.setOrderLines(aLines);

		// call the update
		println("Adding another order line");
		api.updateOrder(order);

		// let's see if my new line is there
		order = api.getOrder(orderId);
		println("Got updated order with " + order.getOrderLines().length + " lines");

		then:

		initialCount + 1		==		order.getOrderLines().length;


	}

	def "testRecreate"() {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();
		// the the latest
		OrderWS order = api.getLatestOrder(GANDALF_USER_ID);
		// use it to create another one
		Integer newOrder = api.createOrder(order);

		then:

		true		==		order.getId().compareTo(newOrder) < 0;
		// clean up
		api.deleteOrder(newOrder);

	}

	def "testRefundAndCancelFee"() {


		when:

		final Integer USER_ID = 1000;

		// create an order an order for testing
		JbillingAPI api = JbillingAPIFactory.getAPI();

		OrderWS newOrder = new OrderWS();
		newOrder.setUserId(USER_ID);
		newOrder.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		newOrder.setPeriod(2);
		newOrder.setCurrencyId(new Integer(1));

		// now add some lines
		OrderLineWS []lines = new OrderLineWS[2];
		OrderLineWS line;

		// 5 lemonades - 1 per day monthly pass
		line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(5));
		line.setItemId(new Integer(1));
		line.setUseItem(new Boolean(true));
		lines[0] = line;

		// 5 coffees
		line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(5));
		line.setItemId(new Integer(3));
		line.setUseItem(new Boolean(true));
		lines[1] = line;

		newOrder.setOrderLines(lines);

		// create the first order and invoice it
		println("Creating order ...");
		Integer orderId = api.createOrderAndInvoice(newOrder);

		then:

		null		!=		orderId;

		// update the quantities of the order (-2 lemonades, -3 coffees)
		when:

		println("Updating quantities of order ...");

		OrderWS order = api.getLatestOrder(USER_ID);

		then:

		2		==		 order.getOrderLines().length;

		when:

		OrderLineWS orderLine = order.getOrderLines()[0];
		orderLine.setQuantity(3);
		orderLine = order.getOrderLines()[1];
		orderLine.setQuantity(2);
		api.updateOrder(order);

		// get last 3 orders and check what's on them (2 refunds and a fee)
		println("Getting last 3 orders ...");
		Integer[] list = api.getLastOrders(new Integer(USER_ID), new Integer(3));

		then:

		null		!=		list;

		// order 1 - coffee refund
		when:

		order = api.getOrder(list[0]);
		orderLine = order.getOrderLines()[0];

		then:
		1		==		 order.getOrderLines().length;

		new Integer(3)		==	 orderLine.getItemId();

		"-3.00"		==		 orderLine.getQuantity();
		"15.00"		==		 orderLine.getPrice();
		"-45.00"		==		 orderLine.getAmount();

		// order 3 - cancel fee for lemonade (see the rule in CancelFees.drl)

		when:

		order = api.getOrder(list[1]);

		then:

		1		==		 order.getOrderLines().length;

		when:

		orderLine = order.getOrderLines()[0];

		then:

		new Integer(24)		==		 orderLine.getItemId();
		"2.00"			==			 orderLine.getQuantity();
		"5.00"		==		 orderLine.getPrice();
		"10.00"		==			orderLine.getAmount();

		// order 2 - lemonade refund
		when:

		order = api.getOrder(list[2]);

		then:
		1		==		 order.getOrderLines().length;

		when:

		orderLine = order.getOrderLines()[0];

		then:

		new Integer(1)		==		 orderLine.getItemId();

		"-2.00"			==		orderLine.getQuantity();
		"10.00"			==		 orderLine.getPrice();
		"-20.00"		==		 orderLine.getAmount();

		// create a new order like the first one

		when:

		println("Creating order ...");
		// to test period calculation of fees in CancellationFeeRulesTask
		newOrder.setActiveUntil(weeksFromToday(12));
		orderId = api.createOrderAndInvoice(newOrder);

		then:

		null		!=		orderId;

		// set active until earlier than invoice date
		when:
		order = api.getLatestOrder(USER_ID);
		order.setActiveUntil(weeksFromToday(2));
		api.updateOrder(order);

		// get last 2 orders and check what's on them (a full refund and a fee)


		println("Getting last 2 orders ...");
		list = api.getLastOrders(new Integer(USER_ID), new Integer(3));

		then:

		null		!=		list;
		// order 1 - full refund
		when:


		order = api.getOrder(list[0]);


		then:
		2		==		 order.getOrderLines().length;


		when:

		orderLine = order.getOrderLines()[0];

		then:
		new Integer(1)		==		 orderLine.getItemId();

		"-5.00"			==		 orderLine.getQuantity();

		"10.00"			==		 orderLine.getPrice();
		"-50.00"		==		 orderLine.getAmount();

		when:

		orderLine = order.getOrderLines()[1];

		then:

		new Integer(3)		==		 orderLine.getItemId();
		"-5.00"				==		 orderLine.getQuantity();
		"15.00"				==	 	orderLine.getPrice();
		"-75.00"			==		 orderLine.getAmount();

		// order 2 - cancel fee for lemonades (see the rule in CancelFees.drl)
		when:

		order = api.getOrder(list[1]);

		then:

		1			==		 order.getOrderLines().length;

		when:
		orderLine = order.getOrderLines()[0];

		then:

		new Integer(24)		==		 orderLine.getItemId();
		// 2 periods cancelled (2 periods * 5 fee quantity)
		"10.00"			==		orderLine.getQuantity();

		"5.00"			==		 orderLine.getPrice();
		"50.00"			==		 orderLine.getAmount();

		// remove invoices
		when:
		list = api.getLastInvoices(new Integer(USER_ID), new Integer(2));
		api.deleteInvoice(list[0]);
		api.deleteInvoice(list[1]);
		// remove orders
		list = api.getLastOrders(new Integer(USER_ID), new Integer(7));

		then:
		for (int i = 0; i < list.length; i++) {
			api.deleteOrder(list[i]);
		}
	}

	def "testDefaultCycleStart"() {

		when:
		final Integer USER_ID = 1000;

		// create an order for testing
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// create a main subscription (current) order
		OrderWS mainOrder = createMockOrder(USER_ID, 1, new BigDecimal("10.00"));
		mainOrder.setPeriod(2);
		mainOrder.setIsCurrent(1);
		mainOrder.setCycleStarts(new Date());
		println("Creating main subscription order ...");
		Integer mainOrderId = api.createOrder(mainOrder);

		then:

		null		!=		mainOrderId;

		// create another order and see if cycle starts was set
		when:
		OrderWS testOrder = createMockOrder(USER_ID, 1, new BigDecimal("20.00"));
		testOrder.setPeriod(2);

		println("Creating test order ...");
		Integer testOrderId = api.createOrder(testOrder);

		then:
		null		!=		testOrderId;

		// check cycle starts dates are the same
		when:

		mainOrder = api.getOrder(mainOrderId);
		testOrder = api.getOrder(testOrderId);

		then:

		mainOrder.getCycleStarts()		==	testOrder.getCycleStarts();

		// create another order with cycle starts set to check it isn't
		// overwritten
		when:

		api.deleteOrder(testOrderId);
		testOrder = createMockOrder(USER_ID, 1, new BigDecimal("30.00"));
		testOrder.setPeriod(2);
		testOrder.setCycleStarts(weeksFromToday(1));
		println("Creating test order ...");
		testOrderId = api.createOrder(testOrder);

		then:

		null		!=		testOrderId;

		// check cycle starts dates aren't the same
		when:

		testOrder = api.getOrder(testOrderId);

		then:
		false			==		mainOrder.getCycleStarts().equals(testOrder.getCycleStarts());

		// create another order with isCurrent not null
		when:

		api.deleteOrder(testOrderId);
		testOrder = createMockOrder(USER_ID, 1, new BigDecimal("40.00"));
		testOrder.setPeriod(2);
		testOrder.setIsCurrent(0);


		println("Creating test order ...");
		testOrderId = api.createOrder(testOrder);

		then:

		null		!=		testOrderId;

		// check that cycle starts wasn't set (is null)
		when:

		testOrder = api.getOrder(testOrderId);

		then:

		null		==		testOrder.getCycleStarts();

		// remove orders
		api.deleteOrder(mainOrderId);
		api.deleteOrder(testOrderId);

	}

	def "testPlan"() {

		when:

		final Integer USER_ID = 1000;

		// create an order for testing
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// create an order with the plan item
		OrderWS mainOrder = createMockOrder(USER_ID, 1, new BigDecimal("10.00"));
		mainOrder.setPeriod(2);
		mainOrder.getOrderLines()[0].setItemId(250);
		mainOrder.getOrderLines()[0].setUseItem(true);
		println("Creating plan order ...");
		Integer mainOrderId = api.createOrder(mainOrder);

		then:

		null		!=		mainOrderId;

		// take the last two orders
		when:
		Integer []orders = api.getLastOrders(USER_ID, 2);
		// setup
		OrderWS order = api.getOrder(orders[1]);

		then:

		1		==		 order.getOrderLines().length;
		251		==		 order.getOrderLines()[0].getItemId().intValue();
		1		==		 order.getPeriod().intValue();

		// subscription
		when:

		order = api.getOrder(orders[0]);

		then:
		1		==		 order.getOrderLines().length;
		1		==		 order.getOrderLines()[0].getItemId().intValue();
		2		==		 order.getPeriod().intValue();

		// clean up
		api.deleteOrder(orders[0]);
		api.deleteOrder(orders[1]);

	}

	//    // Tests InternalEventsRulesTask plug-in.
	//    // See also InternalEventsRulesTask520.drl.
	def "testInternalEventsRulesTask"() {

		when:

		final Integer USER_ID = 1010;

		JbillingAPI api = JbillingAPIFactory.getAPI();

		// create order with 2 lines (item ids 1 & 2) and invoice
		OrderWS order = createMockOrder(USER_ID, 2, new BigDecimal("5.00"));
		order.setNotes("Change me.");
		Integer invoiceId = api.createOrderAndInvoice(order);

		// get back created order
		InvoiceWS invoice = api.getInvoiceWS(invoiceId);
		Integer orderId = invoice.getOrders()[0];
		order = api.getOrder(orderId);

		// check order was modified

		then:
		"Modified by rules created by generateRules API method."		==		order.getNotes();

		when:

		OrderLineWS[] orderLines = order.getOrderLines();

		then:

		2		==		 orderLines.length;
		new Integer(1)		==		orderLines[0].getItemId();

		// double check the invoice lines
		when:

		InvoiceLineDTO[] invoiceLines = invoice.getInvoiceLines();

		then:

		2		==		 invoiceLines.length;
		new Integer(2)		==		invoiceLines[0].getItemId();

		// clean up
		api.deleteInvoice(invoiceId);
		api.deleteOrder(orderId);

	}

	def "testCurrentOrder"() {

		setup:

		final Integer USER_ID = GANDALF_USER_ID;
		final Integer NO_MAIN_SUB_USER_ID = 1010;

		JbillingAPI api = JbillingAPIFactory.getAPI();

		/*
		 * Test update current order without pricing fields.
		 */

		// current order before modification
		OrderWS currentOrderBefore = api.getCurrentOrder(USER_ID,new Date());
		// CXF returns null for empty arrays
		when:
		currentOrderBefore.getOrderLines() != null

		then:
		1		==			currentOrderBefore.getOrderLines().length;


		// add a single line

		when:

		OrderLineWS newLine = new OrderLineWS();
		newLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		newLine.setItemId(new Integer(1));
		newLine.setQuantity(new BigDecimal("22.00"));
		// take the price and description from the item
		newLine.setUseItem(new Boolean(true));

		def ar		=	new OrderLineWS[1];
		ar[0]		=	newLine;
		// update the current order

		OrderWS currentOrderAfter = api.updateCurrentOrder(USER_ID,ar, null, new Date(),
				"Event from WS");

		// asserts

		then:

		currentOrderBefore.getId()		==		 currentOrderAfter.getId();
		1								==	 currentOrderAfter.getOrderLines().length;

		when:
		OrderLineWS createdLine = currentOrderAfter.getOrderLines()[0];

		then:
		newLine.getItemId()			==			  createdLine.getItemId();
		22.00						==								newLine.getQuantity()
		"10.00"						==			 createdLine.getPrice();
		"220.00"					==			 createdLine.getAmount();


		/*
		 * Test update current order with pricing fields.
		 */

		// A pricing rule. See PricingRules.drl, rule 'PricingField test1'.

		when:

		PricingField pf = new PricingField("newPrice", new BigDecimal("5.0"));
		newLine.setQuantity(1);

		def ar6		=	new PricingField[1];
		ar6[0]		=	pf;
		currentOrderAfter = api.updateCurrentOrder(USER_ID,ar,ar6,new Date(), "Event from WS");

		// asserts

		then:
		1		==		 currentOrderAfter.getOrderLines().length;

		when:
		createdLine = currentOrderAfter.getOrderLines()[0];

		then:
		newLine.getItemId()		==		 createdLine.getItemId();
		"23.00"				==		 createdLine.getQuantity();
		"10.00"				==	 createdLine.getPrice();

		// Note that because of the rule, the result should be
		// 225.0, not 230.0.
		"225.00"			==		 createdLine.getAmount();


		/*
		 * Test update current order with pricing fields and no
		 * order lines. RulesMediationTask should create them.
		 */

		// Call info pricing fields. See Mediation.drl, rule 'line creation'
		when:

		PricingField duration = new PricingField("duration", 5); // 5 min
		PricingField dst = new PricingField("dst", "12345678");

		def ar3		=	new PricingField[2];
		ar[0]		=	pf;
		ar[1]		=	dst;
		currentOrderAfter = api.updateCurrentOrder(USER_ID, null,ar3, new Date(),"Event from WS");

		// asserts

		then:

		2		==		 currentOrderAfter.getOrderLines().length;

		when:

		createdLine = currentOrderAfter.getOrderLines()[0];


		then:
		newLine.getItemId()		==			createdLine.getItemId;
		"23.00"		==		 createdLine.getQuantity();
		"10.00"		==		 createdLine.getPrice();
		"225.00"	==		 createdLine.getAmount();

		// 'newPrice' pricing field, $5 * 5 units = 25
		when:

		createdLine = currentOrderAfter.getOrderLines()[1];

		then:
		"5.00"		==		 createdLine.getQuantity();
		"5.00"		==		 createdLine.getPrice();
		new BigDecimal("25")		==		 new BigDecimal(createdLine.getAmount()); // not priced

		/*
		 * Events that go into an order already invoiced, should update the
		 * current order for the next cycle
		 */

		// fool the system making the current order finished (don't do this at home)

		when:
		println("Making current order 'finished'");
		currentOrderAfter.setStatusId(2); // this means finished
		api.updateOrder(currentOrderAfter);

		then:
		2		==			 api.getOrder(currentOrderAfter.getId()).getStatusId().intValue();
		// make that current order an invoice
		/*
		 Integer invoiceId = api.createInvoice(USER_ID, false)[0];
		 System.out.println("current order generated invoice " + invoiceId);
		 */
		// now send again that last event

		when:

		println("Sending event again");
		def ar4		=	new PricingField[3];
		ar[0]		=	pf;
		ar[1]		=	duration;
		ar[2]		=	dst;
		OrderWS currentOrderNext = api.updateCurrentOrder(USER_ID, null,ar4, new Date(),"Same event from WS");

		then:

		null		!=		currentOrderNext;
		false		==		currentOrderNext.getId().equals(currentOrderAfter.getId());
		new DateMidnight(currentOrderAfter.getActiveSince().getTime()).plusMonths(1)	==
				new DateMidnight(currentOrderNext.getActiveSince().getTime());

		/*
		 * No main subscription order tests.
		 */

		// User with no main subscription order should return
		// null when trying to get a current order.
		"null current order"		==		 api.getCurrentOrder(NO_MAIN_SUB_USER_ID, new Date());

		// An exception should be thrown

		when:

		api.updateCurrentOrder(NO_MAIN_SUB_USER_ID,ar, null, new Date(),"Event from WS");

		then:

		thrown(Exception);
		println("User with no main subscription order should throw an " +
				"exception");

		/*
		 * Security tests
		 */
		when:
		api.getCurrentOrder(13, new Date());


		then:

		thrown(Exception);
		println("User 13 belongs to entity 2");

		when:

		def ar5		=	new PricingField[1];
		ar[0]		=	pf;
		api.updateCurrentOrder(13, ar,ar5, new Date(), "Event from WS");


		then:

		thrown(Exception);
		println("User 13 belongs to entity 2");


		// cleanup
		api.deleteOrder(currentOrderAfter.getId());
		api.deleteOrder(currentOrderNext.getId());

	}

	def "testIsUserSubscribedTo"()  {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		// Test a non-existing user first, result should be 0
		BigDecimal result = api.isUserSubscribedTo(Integer.valueOf(999), Integer.valueOf(999));

		then:

		BigDecimal.ZERO		==		 result;

		// Test the result given by a known existing user (
		// in PostgreSQL test db)
		when:

		result = api.isUserSubscribedTo(Integer.valueOf(2), Integer.valueOf(2));

		then:
		new BigDecimal("1")		==		 result;

		// Test another user
		when:

		result = api.isUserSubscribedTo(Integer.valueOf(73), Integer.valueOf(1));
		then:

		new BigDecimal("89")		==		 result;
	}

	def "testGetUserItemsByCategory"()  {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		// Test a non-existing user first, result should be 0
		Integer[] result = api.getUserItemsByCategory(Integer.valueOf(999),Integer.valueOf(999));
		then:
		null		==		result;

		// Test the result given by a known existing user
		// (it has items 2 and 3 on category 1
		// in PostgreSQL test db)

		when:
		result = api.getUserItemsByCategory(Integer.valueOf(2),
				Integer.valueOf(1));

		then:

		2		==		 result.length;
		Integer.valueOf(2)		==		 result[0];
		Integer.valueOf(3)		==	 result[1];

		// Test another user (has items 1 and 2 on cat. 1)
		when:

		result = api.getUserItemsByCategory(Integer.valueOf(73),
				Integer.valueOf(1));
		then:

		2		==		 result.length;
		Integer.valueOf(1)		==		 result[0];
		Integer.valueOf(2)		==		 result[1];
	}

	void pause(long t) {
		System.out.println("pausing for " + t + " ms...");
		Thread.sleep(t);

	}

	def "testMainOrder"() {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		// note: for some reason, calling api.getUsersByCreditCard("1152") returns three users
		// but after calling updateUser, it reutrns 4 because Gandalf is included.
		// why is not picking him up before? What is updateUser doing that then the CC shows up?
		// get gandalf's orders
		Integer []orders = api.getLastOrders(GANDALF_USER_ID, 100);
		// now get the user
		UserWS user = api.getUserWS(GANDALF_USER_ID);
		Integer mainOrder = user.getMainOrderId();
		println("Gandalf's main order = " + mainOrder);
		user.setMainOrderId(orders[orders.length - 1]);
		println("Gandalf's new main order = " + user.getMainOrderId());
		// update the user (so new main order)
		user.setPassword(null);
		api.updateUser(user);
		// validate that the user does have the new main order

		then:

		orders[orders.length - 1]		==		api.getUserWS(GANDALF_USER_ID).getMainOrderId();

		// set to null: does not work
		/*
		 user.setMainOrderId(null);
		 api.updateUser(user);
		 // validate that the user does have the new main order
		 assertNull("User does should not have main order after setting to null",
		 api.getUserWS(GANDALF_USER_ID).getMainOrderId());
		 */


		// update the user (restore main order)
		when:

		user.setMainOrderId(mainOrder);
		api.updateUser(user);

		then:

		mainOrder		==		api.getUserWS(GANDALF_USER_ID).getMainOrderId();
	}

	def "testOrderLineDescriptionLanguage"() {

		when:

		final Integer USER_ID = 10750; // french speaker

		JbillingAPI api = JbillingAPIFactory.getAPI();

		// create order
		OrderWS order = new OrderWS();
		order.setUserId(USER_ID);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(1); // once
		order.setCurrencyId(1);

		OrderLineWS line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setItemId(1);
		line.setQuantity(1);
		line.setUseItem(true);

		def ar		=	new OrderLineWS[1];
		ar[0]		=	line;
		order.setOrderLines(ar);

		// create order and invoice

		Integer invoiceId = api.createOrderAndInvoice(order);

		// check invoice line
		InvoiceWS invoice = api.getInvoiceWS(invoiceId);

		then:
		1		==		invoice.getInvoiceLines().length;

		InvoiceLineDTO invoiceLine = invoice.getInvoiceLines()[0];

		"French Lemonade"		==		invoiceLine.getDescription();

		// clean up
		api.deleteInvoice(invoiceId);
		api.deleteOrder(invoice.getOrders()[0]);

	}

	def "testItemSwappingRules"() {


		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();
		// add items to a user subscribed to 1
		System.out.println("Testing item swapping - included in plan");
		OrderWS order = createMockOrder(1070, 1, new BigDecimal("1.00"));
		order.getOrderLines()[0].setItemId(2600); // the generic lemonade
		order.getOrderLines()[0].setUseItem(true);

		int orderId = api.createOrder(order);
		order = api.getOrder(orderId);

		then:

		1		==			 order.getOrderLines().length;
		2601	==			 order.getOrderLines()[0].getItemId().intValue();

		// cleanup

		when:

		api.deleteOrder(orderId);

		// now a guy without the plan (user 33)
		System.out.println("Testing item swapping - NOT included in plan");
		order = createMockOrder(33, 1, new BigDecimal("1.00"));
		order.getOrderLines()[0].setItemId(2600); // the generic lemonade
		order.getOrderLines()[0].setUseItem(true);

		orderId = api.createOrder(order);
		order = api.getOrder(orderId);


		then:

		1		==		 order.getOrderLines().length;
		2602	==		order.getOrderLines()[0].getItemId().intValue();

		// cleanup
		api.deleteOrder(orderId);
	}


	def "testRateCard"() {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		println("Testing Rate Card");

		// user for tests
		UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);
		Integer userId = user.getUserId();
		// create main subscription order
		Integer mainOrderId = com.sapienter.jbilling.server.user.WSTest.createMainSubscriptionOrder(userId, 1);
		// update to credit limit
		user.setBalanceType(Constants.BALANCE_CREDIT_LIMIT);
		user.setCreditLimit(new BigDecimal("100.0"));
		api.updateUser(user);


		/* updateCurrentOrder */
		// should be priced at 0.33 (see row 548)

		PricingField[] pf = [
			new PricingField("dst", "55999"),
			new PricingField("duration", 1)
		];

		OrderWS currentOrder = api.updateCurrentOrder(userId,null, pf, new Date(), "Event from WS");

		then:

		println("got it.")
		1		==			 currentOrder.getOrderLines().length;
		OrderLineWS line = currentOrder.getOrderLines()[0];

		2800			==		 line.getItemId().intValue();
		"1.00"			==		line.getQuantity();
		new BigDecimal("0.33")		==		line.getAmountAsDecimal();

		// check dynamic balance
		when:

		user = api.getUserWS(userId);

		then:
		new BigDecimal("0.33")		==		user.getDynamicBalanceAsDecimal();

		// should be priced at 0.08 (see row 1753)
		when:

		println("Hello Rohit Sir.")
		pf[0].setStrValue("55000");
		currentOrder = api.updateCurrentOrder(userId,null, pf, new Date(), "Event from WS");

		line = currentOrder.getOrderLines()[0];

		then:

		println("Hello Rohit Sir 2")

		1			==			currentOrder.getOrderLines().length;

		2800		==			 line.getItemId().intValue();
		"2.00"			==			 line.getQuantity();
		// 0.33 + 0.08 = 0.41
		new BigDecimal("0.41")		==			line.getAmountAsDecimal();
		//
		// check dynamic balance

		when:
		user = api.getUserWS(userId);

		then:
		new BigDecimal("0.41")		==			user.getDynamicBalanceAsDecimal();


		/* getItem */
		// should be priced at 0.42 (see row 1731)
		when:

		pf[0].setStrValue("212222");
		ItemDTOEx item = api.getItem(2800, userId, pf);

		then:
		new BigDecimal("0.42")		==		 item.getPrice();


		/* rateOrder */
		when:

		OrderWS newOrder = createMockOrder(userId, 0, new BigDecimal("10.0"));

		// createMockOrder(...) doesn't add the line items we need for this test - do it by hand
		OrderLineWS newLine = new OrderLineWS();
		newLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		newLine.setDescription("New Order Line");
		newLine.setItemId(2800);
		newLine.setQuantity(10);
		newLine.setPrice((String) null);
		newLine.setAmount((String) null);
		newLine.setUseItem(true);

		List<OrderLineWS> lines = new ArrayList<OrderLineWS>();
		lines.add(newLine);

		newOrder.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
		newOrder.setPricingFields(PricingField.setPricingFieldsValue(pf));

		OrderWS order = api.rateOrder(newOrder);

		then:

		1		==		 currentOrder.getOrderLines().length;

		when:

		line = order.getOrderLines()[0];

		then:
		2800		==		 line.getItemId().intValue();
		"10.00"		==		 line.getQuantity();
		// 0.42 * 10 = 4.2
		new BigDecimal("4.2")		==		 line.getAmountAsDecimal();

		/* validatePurchase */
		// should be priced at 0.47 (see row 498)

		// current balance: 100 - 0.41 = 99.59
		// quantity available expected: 99.59 / 0.47

		when:
		pf[0].setStrValue("187630");
		ValidatePurchaseWS result = api.validatePurchase(userId,null, pf);

		then:

		Boolean.valueOf(true)		==		result.getSuccess();
		Boolean.valueOf(true)		==		result.getAuthorized();
		new BigDecimal("211.8936170213")	==		result.getQuantityAsDecimal();

		// check current order wasn't updated

		when:

		currentOrder = api.getOrder(currentOrder.getId());
		line = currentOrder.getOrderLines()[0];


		then:
		1				==		 currentOrder.getOrderLines().length;



		2800			==		 line.getItemId().intValue();
		"2.00"			==		 line.getQuantity();
		new BigDecimal("0.41")		==			line.getAmountAsDecimal();


		// clean up
		api.deleteUser(userId);


	}

	// Tests VelocityRulesGeneratorTask plug-in.
	// See also rules-generator-template-integration-test.vm and
	// rules-generator-config.xml. Overwrites
	// InternalEventsRulesTask520.pkg.
	def "testVelocityRulesGeneratorTaskTest"() {

		when:
		final Integer USER_ID = 1010;

		JbillingAPI api = JbillingAPIFactory.getAPI();

		// updates rules
		String xml =
				"<bundles> " +
				"<bundle> " +
				"<original-product> " +
				"<name>DR-01</name> " +
				"</original-product> " +
				"<replacement-product> " +
				"<name>DR--02</name> " +
				"</replacement-product> " +
				"<replacement-product> " +
				"<name>DR-03</name> " +
				"</replacement-product> " +
				"</bundle> " +
				"<bundle> " +
				"<original-product> " +
				"<name>CALL-LD-GEN</name> " +
				"</original-product> " +
				"<replacement-product> " +
				"<name>CALL-LD</name> " +
				"</replacement-product> " +
				"<replacement-product> " +
				"<name>CALL-LD-INCLUDE</name> " +
				"</replacement-product> " +
				"</bundle> " +
				"</bundles>";

		api.generateRules(xml);

		// wait for packages to be rescanned
		pause(5500);

		// create order with 1 line and invoice
		OrderWS order = createMockOrder(USER_ID, 1, new BigDecimal("5.00"));
		order.setNotes("Change me.");
		// set order line item to 'DR-01' (itemId:2600)
		order.getOrderLines()[0].setItemId(2600);
		Integer invoiceId = api.createOrderAndInvoice(order);

		// get back created order
		InvoiceWS invoice = api.getInvoiceWS(invoiceId);
		Integer orderId = invoice.getOrders()[0];
		order = api.getOrder(orderId);

		// check order was modified
		then:

		"Modified by rules created by generateRules API method."		==		order.getNotes();

		// Bundling rules don't seem to be working for the
		// internal event rules plug-in setup.
		/*
		 OrderLineWS[] orderLines = order.getOrderLines();
		 assertEquals("2 order lines", 2, orderLines.length);
		 // check the item's internalNumbers
		 Arrays.sort(orderLines, new Comparator<OrderLineWS>() {
		 public int compare(OrderLineWS line1, OrderLineWS line2) {
		 return line1.getId() - line2.getId();
		 }
		 });
		 assertEquals("DR--02 line created", "DR--02",
		 orderLines[0].getItemDto().getNumber());
		 assertEquals("DR-03 line created", "DR-03",
		 orderLines[1].getItemDto().getNumber());
		 */

		// clean up
		api.deleteInvoice(invoiceId);
		api.deleteOrder(orderId);
	}
}