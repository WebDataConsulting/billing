package com.sapienter.jbilling.server.discounts;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.discount.DiscountBL;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import org.joda.time.DateMidnight;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.*;

@Test(groups = {"billing-and-discounts", "discounts"})
public class PeriodBasedDiscountOrderTest extends BaseDiscountApiTest {

	@Test
	public void testPeriodBasedDiscountOrder() throws Exception {
		// run the billing as 1st of next month
		Date fixedDate = new GregorianCalendar(2011, 10, 5).getTime();
		today.setTime(fixedDate);
		today.set(Calendar.DAY_OF_MONTH, 1);

		//create User
		UserWS customer = CreateObjectUtil.createCustomer(
				CURRENCY_USD, "Test-User-316-1-" + random, "Admin123@", LANGUAGE_US,
				CUSTOMER_MAIN_ROLE, true, CUSTOMER_ACTIVE, null,
				CreateObjectUtil.createCustomerContact("frodo@shire.com"));

		Integer customerId = api.createUser(customer);

		customer = api.getUserWS(customerId);
		// update the user main subscription and next invoice date to match with the date of bill run
		MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
		mainSubscription.setPeriodId(THREE_MONTHLY_ORDER_PERIOD); //Quarterly
		mainSubscription.setNextInvoiceDayOfPeriod(today.get(Calendar.DAY_OF_MONTH));
		customer.setMainSubscription(mainSubscription);
		customer.setNextInvoiceDate(today.getTime());
		api.updateUser(customer);

		Integer itemId1 = createItem("Item3.316 Description", "300", "IT-0003.316", TEST_ITEM_CATEGORY);

		Calendar activeSince = Calendar.getInstance();
		activeSince.setTime(today.getTime());
		//Set the active since date as 1st of current month
		activeSince.set(Calendar.DAY_OF_MONTH, 1);

		OrderWS order = getOrder(1, activeSince.getTime(), itemId1);
		order.setUserId(customerId);

		DiscountWS discount = createPeriodBasedAmountDiscount(1, null, "1");
		order.setDiscountLines(createDiscountLines(order, discount));
		order.setProrateFlag(Boolean.TRUE);    // Enable prorating on the main order, so discount gets prorated as well.
		OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
		//because this order is now prorate and backdated we will set
		//start date on changes to be the same as active since. Otherwise
		//we will not see any charge to the invoice from this order
		for (OrderChangeWS change : changes) {change.setStartDate(order.getActiveSince());}
		Integer orderId = api.createOrder(order, changes);

		OrderWS mainOrder = api.getOrder(orderId);
		assertNotNull("mainOrder is null.", mainOrder);
		OrderWS[] linkedOrders = mainOrder.getChildOrders();
		assertNotNull("linkedOrders is null.", linkedOrders);

		OrderWS discountOrderWs = null;
		for (OrderWS orderWS : linkedOrders) {
			if (!orderWS.getId().equals(orderId)) {
				discountOrderWs = orderWS;
				break;
			}
		}

		assertEquals("Main Order's Period & Discount Order's period is same.", mainOrder.getPeriod(), discountOrderWs.getPeriod());
		assertEquals("Main Order's Active Since Date & Discount Order's Active Since Date is same.", mainOrder.getActiveSince(), discountOrderWs.getActiveSince());
		assertEquals("Discount Order's Primary Order Id is not equal to Main Order's Id.", mainOrder.getId(), discountOrderWs.getParentOrder().getId());

		OrderLineWS discountOrderLines[] = discountOrderWs.getOrderLines();
		OrderLineWS discountOrderLine = discountOrderLines[0];

		assertEquals("Periodic Discount Order's line Type is not ORDER_LINE_TYPE_DISCOUNT.",
				discountOrderLine.getTypeId().intValue(), Constants.ORDER_LINE_TYPE_DISCOUNT);

		// Periodic discount Order Line should not have any Item set
		assertNull("Discount Order Line's Item is not null.", discountOrderLine.getItemId());

		// check discount order active until date
		DiscountWS discountWs = api.getDiscountWS(mainOrder.getDiscountLines()[0].getDiscountId());
		// no option but to hard code period unit here. If we take from discountWs, we get the order period
		// but there is no API presently available which will get the order period based on order period id.
		Integer periodUnit = 1; // we know discount we have applied is monthly.
		Integer periodValue = DiscountBL.getPeriodValue(discountWs);
		Calendar expectedDiscountOrderUntil = Calendar.getInstance();
		expectedDiscountOrderUntil.setTime(discountOrderWs.getActiveSince());
		expectedDiscountOrderUntil.add(MapPeriodToCalendar.map(periodUnit), periodValue);
		assertTrue("Discount Order Active Until Date is incorrect and does not match the expected value.",
				discountOrderWs.getActiveUntil().equals(expectedDiscountOrderUntil.getTime()));
		if (mainOrder.getActiveUntil() != null) {
			assertTrue("Discount Order Active Until Date is beyond main order active until date which is incorrect.",
					discountOrderWs.getActiveUntil().before(mainOrder.getActiveUntil()) ||
							discountOrderWs.getActiveUntil().equals(mainOrder.getActiveUntil()));
		}
		if (discountWs.getEndDate() != null) {
			assertTrue("Discount Order Active Until Date is beyond discount end date which is incorrect.",
					discountOrderWs.getActiveUntil().before(discountWs.getEndDate()) ||
							discountOrderWs.getActiveUntil().equals(discountWs.getEndDate()));
		}

		// run trigger with the run date as of 1 month back
		today.setTime(fixedDate);
		today.add(Calendar.DAY_OF_MONTH, -4);

		//instead of running the entire billing process we use this
		//API method to only generate invoices for one customer
		Integer[] invoiceIds = api.createInvoiceWithDate(
				customerId,
				new DateMidnight(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH)).toDate(),
				PeriodUnitDTO.MONTH,
				Integer.valueOf(1),
				false);
		assertTrue("More than one invoice should be generated", invoiceIds.length > 0);

		InvoiceWS[] invoices = api.getAllInvoicesForUser(mainOrder.getUserId());
		InvoiceWS invoice1 = invoices != null && invoices.length > 0 ? invoices[0] : null;
		assertNotNull("Invoice was not generated", invoice1);

		// make sure the invoice amount for discount order is prorated.
		Calendar discountOrderPeriodEndCal = Calendar.getInstance();
		discountOrderPeriodEndCal.setTime(discountOrderWs.getActiveSince());
		discountOrderPeriodEndCal.add(MapPeriodToCalendar.map(Constants.PERIOD_UNIT_MONTH), 3);    // Add 3 months as its a quarterly order being discounted
		Integer discountOrderPeriodInDays = daysBetween(discountOrderWs.getActiveSince(), discountOrderPeriodEndCal.getTime());
		BigDecimal total = discountOrderWs.getTotalAsDecimal().setScale(2, RoundingMode.HALF_UP);
		BigDecimal oneDay = total.divide(new BigDecimal(discountOrderPeriodInDays), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
		Integer discountOrderBillingPeriodInDays = daysBetween(discountOrderWs.getActiveSince(), discountOrderWs.getActiveUntil());
		BigDecimal proratedTotal = oneDay.multiply(new BigDecimal(discountOrderBillingPeriodInDays)).setScale(2, RoundingMode.HALF_UP);

		System.out.println("Discount Order discountOrderPeriodInDays: " + discountOrderPeriodInDays);
		System.out.println("Discount Order oneDay: " + oneDay);
		System.out.println("Discount Order discountOrderBillingPeriodInDays: " + discountOrderBillingPeriodInDays);
		System.out.println("Discount Order Prorated Total: " + proratedTotal);

		assertTrue("Discount Order Invoice Amount is not prorated.", invoice1.getTotalAsDecimal().subtract(new BigDecimal(mainOrder.getTotal())).compareTo(TEN.negate()) > 0);
		assertEquals("Prorated amount and Invoice amount for discount order are not equal.", proratedTotal, invoice1.getTotalAsDecimal().subtract(new BigDecimal(mainOrder.getTotal())));

		// check invoice description contains "period from" and "to" words appended in BasicCompositionTask
		InvoiceLineDTO invoiceLine = invoice1.getInvoiceLines()[0];
		String lineDescription = invoiceLine.getDescription();
		assertNotNull("Discount Order Invoice Line Description is null.", lineDescription);
		assertTrue("Discount Order Invoice Line Description does not contain from and to dates.", lineDescription.contains("Period from") && lineDescription.contains("to"));

		// make sure the invoice line description for periodic discount order contains period from and to dates
		String periodFrom = lineDescription.substring(lineDescription.indexOf("Period from") + 12, lineDescription.indexOf("to") - 1);
		String periodTo = lineDescription.substring(lineDescription.indexOf("to") + 2);
		periodFrom = periodFrom.trim();
		periodTo = periodTo.trim();
		DateTimeFormatter df = DateTimeFormat.forPattern("MM/dd/yyyy");
		Date periodFromDate = df.parseDateTime(periodFrom).toDate();
		Date periodToDate = df.parseDateTime(periodTo).toDate();
		System.out.println("periodFromDate: " + periodFromDate);
		System.out.println("periodToDate: " + periodToDate);
		assertTrue("Period From Date is greater than/equal to Period To Date.", periodFromDate.before(periodToDate));

		//cleanup
		for(InvoiceWS invoice : invoices) api.deleteInvoice(invoice.getId());
		api.deleteOrder(discountOrderWs.getId());
		api.deleteOrder(orderId);
		api.deleteItem(itemId1);
		api.deleteUser(customerId);
	}

	/**
	 * Test case to check that discount with start date greater than order active since
	 * date should not get applied on order and should throw exception from createOrder api.
	 */
	@Test
	public void testFuturePeriodBasedDiscountOrder() {

		//create User
		UserWS customer = CreateObjectUtil.createCustomer(
				CURRENCY_USD, "Test-User-316-2-" + random, "Admin123@", LANGUAGE_US,
				CUSTOMER_MAIN_ROLE, true, CUSTOMER_ACTIVE, null,
				CreateObjectUtil.createCustomerContact("frodo@shire.com"));
		Integer customerId = api.createUser(customer);

		Integer itemId1 = createItem("Item2.316 Description", "300", "IT-0002.316", TEST_ITEM_CATEGORY);

		Calendar activeSince = Calendar.getInstance();
		activeSince.add(Calendar.DAY_OF_MONTH, -10);
		OrderWS order = getOrder(2, activeSince.getTime(), itemId1);
		order.setUserId(customerId);

		Calendar futureDiscountStartDate = Calendar.getInstance();
		futureDiscountStartDate.add(Calendar.DAY_OF_MONTH, 10);
		DiscountWS discount = createPeriodBasedAmountDiscount(2, futureDiscountStartDate.getTime(), "1");

		order.setDiscountLines(createDiscountLines(order, discount));

		try {
			api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
			fail("createOrder API should have raised exception as future discount is applied to order");
		} catch (Exception ex) {
			assertTrue("Exception is not of type SessionInternalError, some other error: " + ex, ex instanceof SessionInternalError);
		}

		//cleanup
		api.deleteDiscount(discount.getId());
		api.deleteItem(itemId1);
		api.deleteUser(customerId);
	}

	/**
	 * An Order Active Since in past (1/1/2012), add a Periodic Discount (Start Date: 1/1/2011)
	 * Period Unit Monthly, Period Value 1, Order creation should be successful (Order backdating),
	 * Invoices should have correct values for both the main Order and the Discount pro-rated Order
	 */
	@Test
	public void testBackdatedDiscountOrder() {

		Date fixedDate = new GregorianCalendar(2011, 7, 5).getTime();
		// reset the billing configuration to 4 days back
		today.setTime(fixedDate);
		today.add(Calendar.MONTH, -1);
		today.add(Calendar.DAY_OF_MONTH, -4);

		//create User
		UserWS newCustomer = CreateObjectUtil.createCustomer(
				CURRENCY_USD, "Test-User-316-3-" + random, "P@ssword1",
				LANGUAGE_US, CUSTOMER_MAIN_ROLE, true, CUSTOMER_ACTIVE, null,
				CreateObjectUtil.createCustomerContact("frodo@shire.com"));
		Integer customerId = api.createUser(newCustomer);

		UserWS customer = api.getUserWS(customerId);
		customer.setPassword(null);
		// update the user main subscription and next invoice date to match with the date of billing run below
		GregorianCalendar gCal = new GregorianCalendar();
		gCal.setTime(today.getTime());
		MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
		mainSubscription.setPeriodId(MONTHLY_ORDER_PERIOD); //monthly
		mainSubscription.setNextInvoiceDayOfPeriod(gCal.get(Calendar.DAY_OF_MONTH));
		customer.setMainSubscription(mainSubscription);
		customer.setNextInvoiceDate(gCal.getTime());
		api.updateUser(customer);

		// update the user once again this time for setting back the invoice date to date of billing run below
		customer = api.getUserWS(customer.getId());
		customer.setPassword(null);
		customer.setNextInvoiceDate(gCal.getTime());
		api.updateUser(customer);

		// create item
		Integer itemId1 = createItem("Item1.316 Description", "300", "IT-0001.316", TEST_ITEM_CATEGORY);

		// setting active since to 1 year before
		Calendar activeSince = Calendar.getInstance();
		today.setTime(fixedDate);
		activeSince.set(Calendar.YEAR, today.get(Calendar.YEAR) - 1);
		activeSince.set(Calendar.MONTH, today.get(Calendar.MONTH));    // current month, but 1 year back as year is set to -1 in the above line.
		activeSince.set(Calendar.DAY_OF_MONTH, 1); // lets take first of the month a year before.
		OrderWS order = getOrder(3, activeSince.getTime(), itemId1);
		order.setUserId(customerId);

		Calendar backDatedDiscountStartDate = Calendar.getInstance();
		today.setTime(new Date());
		today.set(Calendar.YEAR, 2011);
		backDatedDiscountStartDate.set(Calendar.YEAR, today.get(Calendar.YEAR) - 2);
		backDatedDiscountStartDate.set(Calendar.MONTH, today.get(Calendar.MONTH));    // current month, but 2 years back as -2 in above line for year.
		backDatedDiscountStartDate.set(Calendar.DAY_OF_MONTH, 1); // lets take first of the month 2 years back
		DiscountWS discount = createPeriodBasedAmountDiscount(3, backDatedDiscountStartDate.getTime(), "18"); // 18 months period value for discount

		// attaching discount line with discount start date of 2 years before
		order.setDiscountLines(createDiscountLines(order, discount));

		System.out.println("Order= " + order);
		System.out.println("Order change Ws=" + OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

		OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
		for (OrderChangeWS change : orderChanges) {
			change.setStartDate(order.getActiveSince());
		}

		Integer orderId = api.createOrder(order, orderChanges);
		System.out.println("Order Id==" + orderId);
		OrderWS mainOrder = api.getOrder(orderId);
		assertNotNull("mainOrder is null.", mainOrder);
		List<OrderWS> linkedOrders = Arrays.asList(api.getLinkedOrders(orderId));
		assertNotNull("linkedOrders is null.", linkedOrders);
		OrderWS discountOrderWs = linkedOrders.get(0);

		//instead of running the entire billing process we use this
		//API method to only generate invoices for one customer
		Integer[] invoiceIds = api.createInvoiceWithDate(
				customerId,
				new DateMidnight(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH)).toDate(),
				PeriodUnitDTO.MONTH,
				Integer.valueOf(1),
				false);
		assertTrue("More than one invoice should be generated", invoiceIds.length > 0);

		InvoiceWS[] invoices = api.getAllInvoicesForUser(mainOrder.getUserId());

		for (InvoiceWS invoiceX : invoices) {
			System.out.println("********* invoiceX : " + invoiceX.getTotal());
		}

		InvoiceWS invoice1 = invoices != null && invoices.length > 0 ? invoices[0] : null;
		assertNotNull("Invoice was not generated", invoice1);

		// make sure the invoice amount matches total amount on main order minus the discount amount
		Integer delegatedInvoiceId = invoice1.getDelegatedInvoiceId();
		if (delegatedInvoiceId != null) {
			InvoiceWS delegatedInvoice = api.getInvoiceWS(delegatedInvoiceId);
			BigDecimal invoice1Total = mainOrder.getTotalAsDecimal().add(discountOrderWs.getTotalAsDecimal());
			assertEquals("Actual and Expected Invoice amounts are not equal.", invoice1Total, delegatedInvoice.getTotalAsDecimal());
		} else {
			BigDecimal invoice1Total = mainOrder.getTotalAsDecimal().add(discountOrderWs.getTotalAsDecimal());
			assertEquals("Actual and Expected Invoice amounts are not equal.", invoice1Total, invoice1.getTotalAsDecimal());
		}

		//cleanup
		if (null != delegatedInvoiceId) api.deleteInvoice(delegatedInvoiceId);
		for (InvoiceWS invoice : invoices) {
			api.deleteInvoice(invoice.getId());
		}
		for (OrderWS discountOrder : linkedOrders) {
			api.deleteOrder(discountOrder.getId());
		}
		api.deleteOrder(orderId);
//		api.deleteDiscount(discount.getId());
		api.deleteItem(itemId1);
		api.deleteUser(customerId);
	}

	private OrderWS getOrder(int counter, Date activeSince, Integer itemId1) {
		// need an order for it
		OrderWS newOrder = new OrderWS();
		newOrder.setUserId(Integer.valueOf(-1)); // it does not matter, the user will be created
		newOrder.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		newOrder.setPeriod(THREE_MONTHLY_ORDER_PERIOD); // quarterly
		newOrder.setCurrencyId(CURRENCY_USD);
		newOrder.setActiveSince(activeSince);
		System.out.println("Order Active Since Date: " + activeSince);

		// now add some lines
		OrderLineWS lines[] = new OrderLineWS[1];
		OrderLineWS line;

		line = new OrderLineWS();
		line.setPrice(new BigDecimal("100.00"));
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(Integer.valueOf(1));
		line.setAmount(new BigDecimal("100.00"));
		line.setDescription("Order Line " + counter);
		line.setItemId(itemId1);
		lines[0] = line;

		newOrder.setOrderLines(lines);

		return newOrder;
	}

	private DiscountLineWS[] createDiscountLines(OrderWS order, DiscountWS discount) {

		// Period based Amount Discount applied at Order level
		DiscountLineWS periodBasedAmountOrderLevel = new DiscountLineWS();
		periodBasedAmountOrderLevel.setDiscountId(discount.getId());
		periodBasedAmountOrderLevel.setOrderId(order.getId());
		periodBasedAmountOrderLevel.setDescription(discount.getDescription() + " Discount On Order Level");

		DiscountLineWS discountLines[] = new DiscountLineWS[1];
		discountLines[0] = periodBasedAmountOrderLevel;        // Period Based Amount Discount applied on Order level

		// return discount lines
		return discountLines;
	}

	private DiscountWS createPeriodBasedAmountDiscount(Integer callCounter, Date discountStartDate, String periodValue) {
		Calendar startOfThisMonth = Calendar.getInstance();
		startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);

		Calendar afterOneMonth = Calendar.getInstance();
		afterOneMonth.setTime(startOfThisMonth.getTime());
		afterOneMonth.add(Calendar.MONTH, 1);

		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DS-PRAM-316-" + random + callCounter);
		discountWs.setDescription("Discount (Code 316-" + random + callCounter + ") Period 1 Month off $1");
		discountWs.setRate(TEN);
		discountWs.setType(DiscountStrategyType.RECURRING_PERIODBASED.name());

		SortedMap<String, String> attributes = new TreeMap<String, String>();
		attributes.put("periodUnit", "2");    // period unit month
		attributes.put("periodValue", periodValue);
		attributes.put("isPercentage", "0");    // Consider rate as amount
		discountWs.setAttributes(attributes);

		if (discountStartDate != null) {
			discountWs.setStartDate(discountStartDate);
		}

		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}

	private int daysBetween(Date start, Date end) {
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(start);

		Calendar endCal = Calendar.getInstance();
		endCal.setTime(end);
		int daysBetween = Days.daysBetween(
				new DateMidnight(startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH) + 1, startCal.get(Calendar.DAY_OF_MONTH)),
				new DateMidnight(endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH) + 1, endCal.get(Calendar.DAY_OF_MONTH))).
				getDays();
		System.out.println("daysBetween: " + daysBetween);
		return daysBetween;
	}
}
