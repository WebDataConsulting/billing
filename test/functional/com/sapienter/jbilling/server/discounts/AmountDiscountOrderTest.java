package com.sapienter.jbilling.server.discounts;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import org.joda.time.DateMidnight;
import org.testng.annotations.Test;
import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;

import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.PreferenceTypeWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.CreateObjectUtil;

@Test(groups = { "billing-and-discounts", "discounts" })
public class AmountDiscountOrderTest extends BaseDiscountApiTest {

	@Test
	public void testAmountDiscountOrderTotal() {
		//create User
        UserWS customer = CreateObjectUtil.createCustomer(
		        CURRENCY_USD, "testAmountBasedDiscountItemOrder.910." + random, "newPa$$word1",
		        LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE,
		        null, CreateObjectUtil.createCustomerContact("test@gmail.com"));

		Integer customerId = api.createUser(customer);
        System.out.println("customerId : "+customerId);
        assertNotNull("Customer/User ID should not be null", customerId);

        // create item
		Integer itemId = createItem("Item1.317 Description", "300", "IT-0001.317", TEST_ITEM_CATEGORY);

        System.out.println("itemId : "+itemId);
        assertNotNull("Item ID should not be null", itemId);

        // create order object
        OrderWS mainOrder = CreateObjectUtil.createOrderObject(
		        customerId, CURRENCY_USD, ServerConstants.ORDER_BILLING_PRE_PAID,
		        MONTHLY_ORDER_PERIOD, new DateMidnight(2010, 01, 21).toDate());

        mainOrder.setUserId(customerId);
        // set discount lines with one amount discount at order level
        mainOrder.setDiscountLines(createDiscountLinesOnOrder(mainOrder));
        mainOrder.setOrderLines(new OrderLineWS[0]);
        mainOrder = CreateObjectUtil.addLine(
                mainOrder,
                Integer.valueOf(1),
                ServerConstants.ORDER_LINE_TYPE_ITEM,
                itemId,
                new BigDecimal("100.00"),
                "Order Line 1"
                );

        // call api to create order
		Integer orderId = api.createOrder(mainOrder, OrderChangeBL.buildFromOrder(mainOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("mainOrder is null.", orderId);
        // fetch the discount order from linked orders

        OrderWS primaryOrder= api.getOrder(orderId);
        OrderWS[] linkedOrders = primaryOrder.getChildOrders();

        System.out.println("Primary Order ID is  " + primaryOrder.getId());
        System.out.println("No. of linked orders: " + linkedOrders.length);

        assertNotNull("linkedOrders is null.", linkedOrders);
        assertTrue("No. of linkedOrders is not equal to 1", linkedOrders.length == 1);

        OrderWS discountOrderWS = null;
        for (OrderWS orderWS : linkedOrders) {
            if (orderWS.getId() != orderId) {
                discountOrderWS = orderWS;
                break;
            }
        }

        // various asserts to test discount order and its order line
        assertNotNull("Discount Order is null", discountOrderWS);
        assertTrue("Discount Order Period is not One Time.", discountOrderWS.getPeriod().intValue() == 1);
        assertTrue("No. of lines on Discount Order not equal to One", discountOrderWS.getOrderLines().length == 1);
        assertTrue("Discount Order line Type not Discount Line Type.", discountOrderWS.getOrderLines()[0].getTypeId().intValue() == ServerConstants.ORDER_LINE_TYPE_DISCOUNT);
        assertNull("Discount Order line item is not null", discountOrderWS.getOrderLines()[0].getItemId());
        assertTrue("Discount Order line Amount not equal to Discount Amount.", discountOrderWS.getOrderLines()[0].getAmountAsDecimal().compareTo(TEN.negate()) == 0 );
        assertEquals("Discount amount is not equal to Discount Order Total", TEN.negate(), discountOrderWS.getTotalAsDecimal());

		//cleanup
		api.deleteOrder(discountOrderWS.getId());
		api.deleteItem(itemId);
		api.deleteUser(customerId);
	}
	
	/**
	 * An Order Active Since in past (1/1/2012), add a Amount Discount (Start Date: 1/1/2011) 
	 * Order creation should be successful (Order backdating), 
	 * Invoices should have correct values for both the main Order and the Discount One-time Order
	 */
	@Test
	public void testBackdatedDiscountOrder() {
		
        Date fixedDate = new GregorianCalendar(2011, 7, 5).getTime();
		// reset the billing configuration to 4 months 3 days back
		today.setTime(fixedDate);
		today.add(Calendar.MONTH, -4);

		//create User
        UserWS customer = CreateObjectUtil.createCustomer(
		        CURRENCY_USD, "testAmountBasedDiscountItemOrder.311." + random,
		        "newPa$$word1", LANGUAGE_US, CUSTOMER_MAIN_ROLE, false,
		        CUSTOMER_ACTIVE, null, null);

        Integer customerId = api.createUser(customer);
        System.out.println("customerId : "+customerId);
        assertNotNull("Customer/User ID should not be null", customerId);
        
        customer = api.getUserWS(customerId);
		customer.setPassword(null);
        // update the user main subscription and next invoice date to match with the date of billing run below
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(today.getTime());
        MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
    	mainSubscription.setPeriodId(MONTHLY_ORDER_PERIOD);
    	mainSubscription.setNextInvoiceDayOfPeriod(gcal.get(Calendar.DAY_OF_MONTH));
    	customer.setMainSubscription(mainSubscription);
    	customer.setNextInvoiceDate(gcal.getTime());
    	api.updateUser(customer);

    	// update the user once again this time for setting back the invoice date to date of billing run below
    	customer = api.getUserWS(customer.getId());
		customer.setPassword(null);
    	customer.setNextInvoiceDate(gcal.getTime());
    	api.updateUser(customer);
        
        // create item
        Integer itemId = createItem("Item1.318 Description", "301", "IT-0001.318", TEST_ITEM_CATEGORY);
        
        System.out.println("itemId : "+itemId);
        assertNotNull("Item ID should not be null", itemId);
        
        // create order object
        // setting active since to 1 year before
        Calendar activeSince = Calendar.getInstance();
        today.setTime(fixedDate);
        activeSince.set(Calendar.YEAR, today.get(Calendar.YEAR)-1);
        activeSince.set(Calendar.MONTH, today.get(Calendar.MONTH));	// current month, but 1 year back as year is set to -1 in the above line. 
        activeSince.set(Calendar.DAY_OF_MONTH, 1); // lets take first of the month a year before.
        OrderWS mainOrder = CreateObjectUtil.createOrderObject(
		        customerId, CURRENCY_USD,  ServerConstants.ORDER_BILLING_PRE_PAID,
		        MONTHLY_ORDER_PERIOD, activeSince.getTime());
        
        mainOrder.setUserId(customerId);
        // set discount lines with one amount discount at order level
        mainOrder.setDiscountLines(createBackDatedDiscountLinesOnOrder(mainOrder));
        mainOrder.setOrderLines(new OrderLineWS[0]);
        mainOrder = CreateObjectUtil.addLine(
        		mainOrder,
        		new Integer(1),
        		ServerConstants.ORDER_LINE_TYPE_ITEM,
        		itemId,
        		new BigDecimal("100.00"),
        		"Backdated Order Line 1"
        		);

        OrderChangeWS[] mainOrderChanges = OrderChangeBL.buildFromOrder(mainOrder, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: mainOrderChanges) {
            change.setStartDate(mainOrder.getActiveSince());
        }

        Integer orderId = api.createOrder(mainOrder, mainOrderChanges);
       	mainOrder = api.getOrder(orderId);
        assertNotNull("mainOrder is null.", mainOrder);
        // fetch the discount order from linked orders

        OrderWS primaryOrder = api.getOrder(orderId);
        OrderWS[] linkedOrders = primaryOrder.getChildOrders();
        
        System.out.println("Primary Order ID is  " + primaryOrder.getId());
        System.out.println("No. of linked orders: " + linkedOrders.length);
        
        assertNotNull("linkedOrders is null.", linkedOrders);
        assertTrue("No. of linkedOrders is not equal to 1", linkedOrders.length == 1);
                
        OrderWS discountOrderWS = null;
        for (OrderWS orderWS : linkedOrders) {
        	if (orderWS.getId() != orderId) {
        		discountOrderWS = orderWS;
        		break;
        	}
        }
        
        // various asserts to test discount order and its order line
        assertNotNull("Discount Order is null", discountOrderWS);
        assertTrue("Discount Order Period is not One Time.", discountOrderWS.getPeriod().intValue() == 1);
        assertTrue("No. of lines on Discount Order not equal to One", discountOrderWS.getOrderLines().length == 1);
        assertTrue("Discount Order line Type not Discount Line Type.", discountOrderWS.getOrderLines()[0].getTypeId().intValue() == ServerConstants.ORDER_LINE_TYPE_DISCOUNT);
        assertNull("Discount Order line item is not null", discountOrderWS.getOrderLines()[0].getItemId());
        assertTrue("Discount Order line Amount not equal to Discount Amount.", discountOrderWS.getOrderLines()[0].getAmountAsDecimal().compareTo(TEN.negate()) == 0 );
        assertEquals("Discount amount is not equal to Discount Order Total", TEN.negate(), discountOrderWS.getTotalAsDecimal());


		Integer[] invoiceIds = api.createInvoiceWithDate(
				customerId,
				new DateMidnight(today.get(Calendar.YEAR), today.get(Calendar.MONTH)+1, today.get(Calendar.DAY_OF_MONTH)).toDate(),
				PeriodUnitDTO.MONTH,
				Integer.valueOf(1),
				false);
		assertTrue("More than one invoice should be generated", invoiceIds.length > 0);

        InvoiceWS[] invoices = api.getAllInvoicesForUser(mainOrder.getUserId());
        InvoiceWS invoice1 = invoices != null && invoices.length > 0 ? invoices[0] : null;
        assertNotNull("Invoice was not generated", invoice1);
        
        Arrays.parallelSort(invoices, (InvoiceWS inv1, InvoiceWS inv2) -> inv1.getId() - inv2.getId() );
        
        for (InvoiceWS invoiceWs : invoices) {
        	// make sure the invoice amount matches total amount on main order minus the discount amount
            BigDecimal carriedBalance = invoiceWs.getCarriedBalanceAsDecimal();
            if (carriedBalance !=null && carriedBalance.compareTo(BigDecimal.ZERO) > 0) {
            	BigDecimal invoiceWsTotal = carriedBalance.add(mainOrder.getTotalAsDecimal());
                assertEquals("Actual and Expected Invoice amounts are not equal.", invoiceWsTotal, invoiceWs.getTotalAsDecimal());
            } else {
            	// because amount discount is one-time discount, subtracting it from main order total only first time for the expected invoice amount.
            	BigDecimal invoiceWsTotal = mainOrder.getTotalAsDecimal().add(discountOrderWS.getTotalAsDecimal());
                assertEquals("Actual and Expected Invoice amounts are not equal.", invoiceWsTotal, invoiceWs.getTotalAsDecimal());	
            }
        }

		//cleanup
		for (InvoiceWS invoice : invoices) {
			api.deleteInvoice(invoice.getId());
		}
		api.deleteOrder(discountOrderWS.getId());
		api.deleteItem(itemId);
		api.deleteUser(customerId);
	}
	
	private DiscountLineWS[] createBackDatedDiscountLinesOnOrder(OrderWS order) {
		
		// Amount Discount applied at Order level
		DiscountLineWS amountDiscountOrderLevel = new DiscountLineWS();
		Calendar backDatedDiscountStartDate = Calendar.getInstance();
		backDatedDiscountStartDate.set(Calendar.YEAR, today.get(Calendar.YEAR)-2);
		backDatedDiscountStartDate.set(Calendar.MONTH, today.get(Calendar.MONTH));	// current month, but 2 years back as -2 in above line for year.
		backDatedDiscountStartDate.set(Calendar.DAY_OF_MONTH, 1); // lets take first of the month 2 years back
		DiscountWS discountWs = createAmountDiscount(2, backDatedDiscountStartDate.getTime()); 
		amountDiscountOrderLevel.setDiscountId(discountWs.getId());
		amountDiscountOrderLevel.setOrderId(order.getId());
		amountDiscountOrderLevel.setDescription(discountWs.getDescription() + " " + random  +" Discount On Order Level");
		
		DiscountLineWS discountLines[] = new DiscountLineWS[1];
		discountLines[0] = amountDiscountOrderLevel;		// Period Based Amount Discount applied on Order level
		
		// return discount lines
		return discountLines;
	}


	private DiscountLineWS[] createDiscountLinesOnOrder(OrderWS order) {

		// Amount based Discount applied at Order level
		DiscountLineWS amountBasedDiscountOrderLevel = new DiscountLineWS();
		DiscountWS discountWs = createAmountDiscount(1, null);
		amountBasedDiscountOrderLevel.setDiscountId(discountWs.getId());
		amountBasedDiscountOrderLevel.setOrderId(order.getId());
		amountBasedDiscountOrderLevel.setDescription(discountWs.getDescription() + random + "-" + " Discount On Order Level");
		
		DiscountLineWS discountLines[] = new DiscountLineWS[1];
		discountLines[0] = amountBasedDiscountOrderLevel;		// Period Based Amount Discount applied on Order level
		
		// return discount lines
		return discountLines;
	}
	
	private DiscountWS createAmountDiscount(Integer callCounter, Date discountStartDate) {
		Calendar startOfThisMonth = Calendar.getInstance();
		startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);
				
		Calendar afterOneMonth = Calendar.getInstance();
		afterOneMonth.setTime(startOfThisMonth.getTime());
		afterOneMonth.add(Calendar.MONTH, 1);
		
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DISC-AMOUNT-" + random + "-" + callCounter);
		discountWs.setDescription("Discount-" + random + "-" + callCounter + " Amount $" + TEN);
		discountWs.setRate(TEN);
		discountWs.setType(DiscountStrategyType.ONE_TIME_AMOUNT.name());
		
		if (discountStartDate != null) {
			discountWs.setStartDate(discountStartDate);
		}
		
		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}

}