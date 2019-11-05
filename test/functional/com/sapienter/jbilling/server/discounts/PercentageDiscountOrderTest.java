package com.sapienter.jbilling.server.discounts;

import java.math.BigDecimal;
import java.util.*;

import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;

import org.joda.time.DateMidnight;

import org.testng.annotations.Test;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.PreferenceTypeWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.CreateObjectUtil;


@Test(groups = { "billing-and-discounts", "discounts" })
public class PercentageDiscountOrderTest extends BaseDiscountApiTest {
	
	@Test
	public void testPercentageDiscountOrderTotal() {
		//create User
        UserWS user = CreateObjectUtil.createCustomer(
		        CURRENCY_USD, "testPercentageDiscountItemOrder.310",
                "newPa$$word1", LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
                CreateObjectUtil.createCustomerContact("test@gmail.com"));

        Integer customerId = api.createUser(user);
        System.out.println("customerId : "+customerId);
        assertNotNull("Customer/User ID should not be null", customerId);
        // create item
        Integer itemId = createItem("Item1.417 Description", "400", "IT-0001.417", TEST_ITEM_CATEGORY);

        System.out.println("itemId : "+itemId);
        assertNotNull("Item ID should not be null", itemId);

        // create order object
        OrderWS mainOrder = CreateObjectUtil.createOrderObject(
		        customerId, CURRENCY_USD, ServerConstants.ORDER_BILLING_PRE_PAID,
		        MONTHLY_ORDER_PERIOD, new DateMidnight(2013, 01, 21).toDate());

        mainOrder.setUserId(customerId);
        // set discount lines with percentage discount at order level
		DiscountWS discount = createPercentageDiscount(1, null);
        mainOrder.setDiscountLines(createDiscountLinesOnOrder(mainOrder, discount));
        mainOrder.setOrderLines(new OrderLineWS[0]);
        mainOrder = CreateObjectUtil.addLine(
                mainOrder,
                new Integer(1),
                ServerConstants.ORDER_LINE_TYPE_ITEM,
                itemId,
                new BigDecimal("100.00"),
                "Order Line 1"
                );
        // call api to create order
        Integer orderId = api.createOrder(mainOrder, OrderChangeBL.buildFromOrder(mainOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        mainOrder = api.getOrder(orderId);
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

        BigDecimal expectedDiscountAmount = TEN.negate().divide(new BigDecimal(100)).multiply(mainOrder.getTotalAsDecimal());
        assertEquals("Discount Order line Amount not equal to Discount Amount.", expectedDiscountAmount, discountOrderWS.getOrderLines()[0].getAmountAsDecimal());
        assertEquals("Discount amount is not equal to Discount Order Total", expectedDiscountAmount, discountOrderWS.getTotalAsDecimal());

		//cleanup
		api.deleteOrder(discountOrderWS.getId());
		api.deleteItem(itemId);
		api.deleteUser(customerId);

	}
	
	@Test
	public void testPercentageItemLevelDiscount() throws Exception {
		//create User
        UserWS user = CreateObjectUtil.createCustomer(
		        CURRENCY_USD, "testPercentageDiscountItem.310", "newPa$$word1",
		        LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
		        CreateObjectUtil.createCustomerContact("test@gmail.com"));

		Integer customerId = api.createUser(user);
        System.out.println("customerId : "+customerId);
        assertNotNull("Customer/User ID should not be null", customerId);
        // create 2 items
        Integer itemId1 = createItem("Item1.518 Description", "500", "IT-0001.518", TEST_ITEM_CATEGORY);
        Integer itemId2 = createItem("Item2.518 Description", "500", "IT-0002.518", TEST_ITEM_CATEGORY);

        System.out.println("itemId1 : "+itemId1);
        assertNotNull("Item ID 1 should not be null", itemId1);

        System.out.println("itemId2 : "+itemId2);
        assertNotNull("Item ID 2 should not be null", itemId2);
        // create order object
        OrderWS mainOrder = CreateObjectUtil.createOrderObject(
		        customerId, CURRENCY_USD, ServerConstants.ORDER_BILLING_PRE_PAID,
		        MONTHLY_ORDER_PERIOD, new DateMidnight(2013, 01, 21).toDate());

        mainOrder.setUserId(customerId);
        // set discount lines with percentage discount at item level (only on item 1)
		DiscountWS discount = createPercentageDiscount(10, null);
        mainOrder.setDiscountLines(createDiscountLinesOnOrder(mainOrder, discount, itemId1, "Item Level"));
        mainOrder.setOrderLines(new OrderLineWS[0]);
        mainOrder = CreateObjectUtil.addLine(
                mainOrder,
                new Integer(1),
                ServerConstants.ORDER_LINE_TYPE_ITEM,
                itemId1,
                new BigDecimal("100.00"),
                "Order Line 1"
                );
        mainOrder = CreateObjectUtil.addLine(
                mainOrder,
                new Integer(1),
                ServerConstants.ORDER_LINE_TYPE_ITEM,
                itemId2,
                new BigDecimal("100.00"),
                "Order Line 2"
                );
        // call api to create order
        Integer orderId = api.createOrder(mainOrder, OrderChangeBL.buildFromOrder(mainOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        mainOrder = api.getOrder(orderId);
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
        // GET THE discountable amount as matching order line's amount (match by item id).
        BigDecimal discountableAmount = BigDecimal.ZERO;
        for (OrderLineWS line : mainOrder.getOrderLines()) {
            if (line.getItemId().intValue() == itemId1.intValue()) {
                discountableAmount = line.getAmountAsDecimal();
                break;
            }
        }
        BigDecimal expectedDiscountAmount = TEN.negate().divide(new BigDecimal(100)).multiply(discountableAmount);
        assertEquals("Discount Order line Amount not equal to Discount Amount.", expectedDiscountAmount, discountOrderWS.getOrderLines()[0].getAmountAsDecimal());
        assertEquals("Discount amount is not equal to Discount Order Total", expectedDiscountAmount, discountOrderWS.getTotalAsDecimal());

		//cleanup
		api.deleteOrder(discountOrderWS.getId());
//		api.deleteDiscount(discount.getId());
		api.deleteItem(itemId1);
		api.deleteItem(itemId2);
		api.deleteUser(customerId);
	}

	/**
	 * An Order Active Since in past (1/1/2012), add Percentage Discount (Start Date: 1/1/2011) 
	 * Order creation should be successful (Order backdating), 
	 * Invoices should have correct values for both the main Order and the Discount One-time Order
	 */
	@Test
	public void testBackdatedDiscountOrder() throws Exception {

        Date fixedDate = new GregorianCalendar(2011, 7, 5).getTime();
		// reset the billing configuration to 3 months 2 days back
		today.setTime(fixedDate);
		today.add(Calendar.MONTH, -3);

		//create User
        UserWS customer = CreateObjectUtil.createCustomer(
		        CURRENCY_USD, "testPercentageDiscountItemOrder.311."+random, "newPa$$word1",
		        LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
		        CreateObjectUtil.createCustomerContact("test@gmail.com"));

        Integer customerId = api.createUser(customer);
        System.out.println("customerId : "+customerId);
        assertNotNull("Customer/User ID should not be null", customerId);
        
        customer = api.getUserWS(customerId);
		customer.setPassword(null);
        // update the user main subscription and next invoice date to match with the date of billing run below
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(today.getTime());
        MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
    	mainSubscription.setPeriodId(MONTHLY_ORDER_PERIOD); //monthly
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
        Integer itemId = createItem("Item1.418 Description", "401", "IT-0001.418", TEST_ITEM_CATEGORY);
        
        System.out.println("itemId : "+itemId);
        assertNotNull("Item ID should not be null", itemId);
        
        // create order object
        // setting active since to 1 year before
        Calendar activeSince = Calendar.getInstance();
        today.setTime(fixedDate);
        activeSince.set(Calendar.YEAR, today.get(Calendar.YEAR)-1);
        activeSince.set(Calendar.MONTH, today.get(Calendar.MONTH));	// current month, but 1 year back as year is set to -1 in the above line. 
        activeSince.set(Calendar.DAY_OF_MONTH, 1); // lets take first of the month a year before.
        OrderWS mainOrder = CreateObjectUtil.createOrderObject(customerId, CURRENCY_USD,
        		ServerConstants.ORDER_BILLING_PRE_PAID, MONTHLY_ORDER_PERIOD, activeSince.getTime());
        mainOrder.setUserId(customerId);

		//create discount
		Calendar backDatedDiscountStartDate = Calendar.getInstance();
		backDatedDiscountStartDate.set(Calendar.YEAR, today.get(Calendar.YEAR)-2);
		backDatedDiscountStartDate.set(Calendar.MONTH, today.get(Calendar.MONTH));	// current month, but 2 years back as -2 in above line for year.
		backDatedDiscountStartDate.set(Calendar.DAY_OF_MONTH, 1); // lets take first of the month 2 years back
		DiscountWS discount = createPercentageDiscount(2, backDatedDiscountStartDate.getTime());

		// set discount lines with percentage discount at order level
		mainOrder.setDiscountLines(createBackDatedDiscountLinesOnOrder(mainOrder, discount));
        mainOrder.setOrderLines(new OrderLineWS[0]);
        mainOrder = CreateObjectUtil.addLine(
        		mainOrder,
        		Integer.valueOf(1),
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
        BigDecimal expectedDiscountAmount = TEN.negate().divide(new BigDecimal(100)).multiply(mainOrder.getTotalAsDecimal());
        assertEquals("Discount Order line Amount not equal to Discount Amount.", expectedDiscountAmount, discountOrderWS.getOrderLines()[0].getAmountAsDecimal());
        assertEquals("Discount amount is not equal to Discount Order Total", expectedDiscountAmount, discountOrderWS.getTotalAsDecimal());

		//instead of running the entire billing process we use this
		//API method to only generate invoices for one customer
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
        Arrays.parallelSort(invoices, (InvoiceWS inv1, InvoiceWS inv2) -> inv1.getId() - inv2.getId());

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
//		api.deleteDiscount(discount.getId());
		api.deleteItem(itemId);
		api.deleteUser(customerId);
	}
	
	private DiscountLineWS[] createBackDatedDiscountLinesOnOrder(OrderWS order, DiscountWS discount) {
		
		// Percentage Discount applied at Order level
		DiscountLineWS percentageDiscountOrderLevel = new DiscountLineWS();

	    percentageDiscountOrderLevel.setDiscountId(discount.getId());
		percentageDiscountOrderLevel.setOrderId(order.getId());
		percentageDiscountOrderLevel.setDescription(discount.getDescription() + " Discount On Order Level");
		
		DiscountLineWS discountLines[] = new DiscountLineWS[1];
		discountLines[0] = percentageDiscountOrderLevel;
		
		// return discount lines
		return discountLines;
	}
	

	private DiscountLineWS[] createDiscountLinesOnOrder(OrderWS order, DiscountWS discount) {
		
		// Percentage based Discount applied at Order level
		DiscountLineWS percentageDiscountOrderLevel = new DiscountLineWS();
		percentageDiscountOrderLevel.setDiscountId(discount.getId());
		percentageDiscountOrderLevel.setOrderId(order.getId());
		percentageDiscountOrderLevel.setDescription(discount.getDescription() + " Discount On Order Level");
		
		DiscountLineWS discountLines[] = new DiscountLineWS[1];
		discountLines[0] = percentageDiscountOrderLevel;		// Percentage Discount applied on Order level
		
		// return discount lines
		return discountLines;
	}
	
	/** Percentage based Discount applied at Item Or Plan Item level.
	 * Pass the discountLevel param as "Item Level" Or "Plan Item Level".	
	 */
	private DiscountLineWS[] createDiscountLinesOnOrder(
			OrderWS order, DiscountWS discount, Integer itemId,
			String discountLevel) {
		
		DiscountLineWS discountLine = new DiscountLineWS();
		discountLine.setDiscountId(discount.getId());
		discountLine.setOrderId(order.getId());
		if ("Item Level".equalsIgnoreCase(discountLevel)) {
			discountLine.setItemId(itemId);
		} 
		discountLine.setDescription(discount.getDescription() + " Discount " + discountLevel);
		
		DiscountLineWS discountLines[] = new DiscountLineWS[1];
		discountLines[0] = discountLine;
		
		// return discount lines
		return discountLines;
	}
	
	private DiscountWS createPercentageDiscount(Integer callCounter, Date discountStartDate) {
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DSC-PRC-" + random + callCounter);
		discountWs.setDescription("Discount-" + random + callCounter + " %" + TEN);
		discountWs.setRate(TEN);	// 10% Discount Rate
		discountWs.setType(DiscountStrategyType.ONE_TIME_PERCENTAGE.name());
		
		if (discountStartDate != null) {
			discountWs.setStartDate(discountStartDate);
		}
		
		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}

}