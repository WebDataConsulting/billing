package com.sapienter.jbilling.server.discounts;

import java.math.BigDecimal;
import java.util.Calendar;

import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import org.joda.time.DateMidnight;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.JBillingTestUtils;

@Test(groups = { "billing-and-discounts", "discounts" })
public class RateOrderDiscountTest extends BaseDiscountApiTest {

	private static final Integer ENTITY_ID = 1;
    private static final Integer CURRENCY_ID = 1;
    private static final Integer ORDER_PERIOD_ONCE = 1;
	private static final BigDecimal DISCOUNT_RATE  = new BigDecimal(5);

	private OrderWS order;
	private UserWS customer;
	private DiscountWS amountBasedDiscount;
	private DiscountWS percentageBasedDiscount;
	
	{
		try {
			System.out.println("========= Initialization Block");
			api = JbillingAPIFactory.getAPI();
			
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	@BeforeMethod
    protected void setUp() {
        
        System.out.println("========= setUp");
        
        //create User
        this.customer = CreateObjectUtil.createCustomer(
		        CURRENCY_USD, "testRateOrderApi-New-"+random, "newPa$$word1",
		        LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
          		CreateObjectUtil.createCustomerContact("test@gmail.com"));
        Integer customerId = api.createUser(this.customer);
        this.customer.setUserId(customerId);
        assertNotNull("Customer/User ID should not be null", customerId);
        
        // create order and line for plan's item

        this.order = CreateObjectUtil.createOrderObject(this.customer.getUserId(), CURRENCY_ID, 
        		Constants.ORDER_BILLING_PRE_PAID, ORDER_PERIOD_ONCE, new DateMidnight(2012, 6, 1).toDate());
        assertNotNull("Order should not be null", this.order);
        
    }

	/**
	 * This test case is for testing rate order api to check if discounted order lines
	 * and the order are rated properly. The fields to check for would be: 
	 * 1. orderLine.adjustedPrice
	 * 2. order.adjustedTotal
	 * 
	 * Also the test case checks if order lines are created for given set of discount lines on an order.
	 */
	@Test
	public void testRateOrderApiDiscountedProducts() {
	
		System.out.println("========= testRateOrderApiDiscountedProducts");
		
		this.amountBasedDiscount = createAmountBasedDiscount(120);
		this.percentageBasedDiscount = createPercentageBasedDiscount(120);
		
		ItemDTOEx testItem1 = CreateObjectUtil.createItem(TEST_ENTITY_ID, BigDecimal.TEN, CURRENCY_USD, TEST_ITEM_CATEGORY, "Test Item 1");
		Integer testItem1Id = api.createItem(testItem1);
		ItemDTOEx testItem2 = CreateObjectUtil.createItem(TEST_ENTITY_ID, BigDecimal.TEN.add(BigDecimal.ONE), CURRENCY_USD, TEST_ITEM_CATEGORY, "Test Item 2");
		Integer testItem2Id = api.createItem(testItem2);
		
		System.out.println("Item Ids 1 & 2 " + testItem1Id + " " + testItem2Id);
		
		// create order and lines with items
        OrderWS testOrder = CreateObjectUtil.createOrderObject(this.customer.getUserId(), CURRENCY_USD,
        		Constants.ORDER_BILLING_PRE_PAID, ONE_TIME_ORDER_PERIOD, new DateMidnight(2012, 6, 1).toDate());
        OrderLineWS lines[] = new OrderLineWS[2];

        testOrder = CreateObjectUtil.addLine(testOrder, 1, Constants.ORDER_LINE_TYPE_ITEM, 
        				testItem1Id, BigDecimal.TEN, "Test Item 1 Order line");
        
        testOrder = CreateObjectUtil.addLine(testOrder, 1, Constants.ORDER_LINE_TYPE_ITEM, 
						testItem2Id, BigDecimal.TEN.add(BigDecimal.ONE), "Test Item 2 Order line");
        
        assertNotNull("Order should not be null", testOrder);
        
        // lets first rate our test order without discounts
        OrderWS ratedTestOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        
        BigDecimal linesTotal = BigDecimal.ZERO;
        
        for (OrderLineWS line : ratedTestOrder.getOrderLines()) {
        	System.out.println("========== line amount: " + line.getAmount());
        	System.out.println("========== line adjusted price: " + line.getAdjustedPrice());
        	
        	assertNull("Adjusted Price for the line should be null as there is no line discount.", line.getAdjustedPrice());
        	assertNotNull("Line Amount should be not null after rating an order.", line.getAmount());
        
        	linesTotal = linesTotal.add(line.getAmountAsDecimal());
        }
        
        System.out.println("========== Order Total: " + ratedTestOrder.getTotal());
        System.out.println("========== Order Adjusted Total: " + ratedTestOrder.getAdjustedTotal());
        
        assertNull("Adjusted Total for the order should be null as there is no order level discount.", ratedTestOrder.getAdjustedTotal());
        assertNotNull("Order Total should be not null post rating.", ratedTestOrder.getTotal());
        assertEquals(linesTotal, ratedTestOrder.getTotalAsDecimal()); 
        
        // apply discounts on order lines - both percentage and amount
        
        // Amount based Discount applied at Item level
 		DiscountLineWS amountBasedItemLevel = new DiscountLineWS();
 		amountBasedItemLevel.setDiscountId(this.amountBasedDiscount.getId());
 		amountBasedItemLevel.setOrderId(ratedTestOrder.getId());
 		amountBasedItemLevel.setItemId(testOrder.getOrderLines()[0].getItemId());
 		amountBasedItemLevel.setDescription("Test Amount based Discount applied at Item level");
   		
   		// Percentage based Discount applied at Item level
   		DiscountLineWS percentageBasedItemLevel = new DiscountLineWS();
   		percentageBasedItemLevel.setDiscountId(this.percentageBasedDiscount.getId());
   		percentageBasedItemLevel.setOrderId(ratedTestOrder.getId());
   		percentageBasedItemLevel.setItemId(testOrder.getOrderLines()[1].getItemId());
   		percentageBasedItemLevel.setDescription("Test Percentage based Discount applied at Item level");
         
 		// Create discount lines array and attach to the test order
   		DiscountLineWS []discountLines = new DiscountLineWS[2];
   		discountLines[0] = amountBasedItemLevel;
   		discountLines[1] = percentageBasedItemLevel;
        
   		testOrder.setDiscountLines(discountLines);
        
        // rate order again post discounts
   		testOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, ORDER_CHANGE_STATUS_APPLY_ID));
   		
        // check adjusted prices on lines
   		BigDecimal discountedLinesTotal = BigDecimal.ZERO;
   		
   		for (OrderLineWS line : testOrder.getOrderLines()) {
   			
        	System.out.println("========== line amount: " + line.getAmount());
        	System.out.println("========== line adjusted price: " + line.getAdjustedPrice());
        	
        	assertNotNull("Line Amount should be not null after rating an order.", line.getAmount());
        	
        	if (line.getTypeId().intValue() == Constants.ORDER_LINE_TYPE_DISCOUNT) {
        		
        		System.out.println("========== ORDER_LINE_TYPE_DISCOUNT");
        		// adjusted price should be null for discount type order line
        		assertNull("Adjusted Price for the line should be null as this is discount line.", line.getAdjustedPrice());
        		
        		// the line amount for discount type order line should be negative
        		assertTrue(line.getAmountAsDecimal().compareTo(BigDecimal.ZERO) < 0);
        		
        	} else if (line.getTypeId().intValue() == Constants.ORDER_LINE_TYPE_ITEM) {
        		
        		System.out.println("========== ORDER_LINE_TYPE_ITEM");
        		
        		// adjusted price should be not null and positive/zero (but not negative)
        		assertNotNull("Adjusted Price for the line should be not null as there is line discount.", line.getAdjustedPrice());
        		assertTrue(line.getAdjustedPriceAsDecimal().compareTo(BigDecimal.ZERO) >= 0);
        	}
        
        	discountedLinesTotal = discountedLinesTotal.add(line.getAmountAsDecimal());
        }
   		
   		// check if adjusted price on line 1 = line amount - discount
   		OrderLineWS line1Ws = findOrderLineWithItem(testOrder.getOrderLines(), testItem1Id);
   		OrderLineWS line2Ws = findOrderLineWithItem(testOrder.getOrderLines(), testItem2Id);   	
   		   		
   		JBillingTestUtils.assertEquals("Line1: ", line1Ws.getAmountAsDecimal().subtract(DISCOUNT_RATE), line1Ws.getAdjustedPriceAsDecimal());
        assertEquals((line2Ws.getAmountAsDecimal().
        				subtract((DISCOUNT_RATE.multiply(line2Ws.getAmountAsDecimal().divide(new BigDecimal(100)))))).
        				compareTo(line2Ws.getAdjustedPriceAsDecimal()), 0);
   		
   		System.out.println("========== Order Total: " + testOrder.getTotal());
        System.out.println("========== Order Adjusted Total: " + testOrder.getAdjustedTotal());
        
        assertNotNull("Adjusted Total for the order should be not null as there are product level discounts.", testOrder.getAdjustedTotal());
        assertNotNull("Order Total should be not null post rating.", testOrder.getTotal());

        // check adjusted total on order is less than order total
   		assertTrue(testOrder.getAdjustedTotalAsDecimal().compareTo(testOrder.getTotalAsDecimal()) < 0);
   		
   		// calculate the expected adjusted total by subtracting amount based discount from order total
   		BigDecimal line1DiscountAmount = DISCOUNT_RATE;	// directly the amount based discount amount
   		BigDecimal line2DiscountAmount = DISCOUNT_RATE.multiply(line2Ws.getAmountAsDecimal().divide(new BigDecimal(100))); // percentage discount
   		BigDecimal expectedAdjustedTotal = testOrder.getTotalAsDecimal().subtract((line1DiscountAmount.add(line2DiscountAmount)));
   		
        assertEquals(expectedAdjustedTotal.compareTo(testOrder.getAdjustedTotalAsDecimal()), 0);

		//cleanup
		api.deleteItem(testItem2Id);
		api.deleteItem(testItem1Id);
	}
	
	/**
	 * This test case applies amount and percentage based discounts at order level
	 * and rates the order to verify if the order's adjusted total has been updated correctly.
	 */
	@Test
	public void testRateOrderApiDiscountedOrder() {
        
		System.out.println("========= testRateOrderApiDiscountedOrder");
		
		this.amountBasedDiscount = createAmountBasedDiscount(121);
		this.percentageBasedDiscount = createPercentageBasedDiscount(121);
		
		ItemDTOEx testItem1 = CreateObjectUtil.createItem(TEST_ENTITY_ID, BigDecimal.TEN, CURRENCY_USD, TEST_ITEM_CATEGORY, "Test Item 1");
		Integer testItem1Id = api.createItem(testItem1);
		ItemDTOEx testItem2 = CreateObjectUtil.createItem(TEST_ENTITY_ID, BigDecimal.TEN.add(BigDecimal.ONE), CURRENCY_USD, TEST_ITEM_CATEGORY, "Test Item 2");
		Integer testItem2Id = api.createItem(testItem2);
		
		// create order and lines with items
        OrderWS testOrder = CreateObjectUtil.createOrderObject(this.customer.getUserId(), CURRENCY_USD,
        		Constants.ORDER_BILLING_PRE_PAID, ONE_TIME_ORDER_PERIOD, new DateMidnight(2012, 6, 1).toDate());
        OrderLineWS lines[] = new OrderLineWS[2];

        testOrder = CreateObjectUtil.addLine(testOrder, 1, Constants.ORDER_LINE_TYPE_ITEM, 
        				testItem1Id, BigDecimal.TEN, "Test Item 1 Order line");
        
        testOrder = CreateObjectUtil.addLine(testOrder, 1, Constants.ORDER_LINE_TYPE_ITEM, 
						testItem2Id, BigDecimal.TEN.add(BigDecimal.ONE), "Test Item 2 Order line");
        
        assertNotNull("Order should not be null", testOrder);
		
        // Amount based Discount applied at Order level
  		DiscountLineWS amountBasedOrderLevel = new DiscountLineWS();
  		amountBasedOrderLevel.setDiscountId(this.amountBasedDiscount.getId());
  		amountBasedOrderLevel.setOrderId(testOrder.getId());
  		amountBasedOrderLevel.setDescription("Test Amount based Discount applied at Order level");
    		
		// Percentage based Discount applied at Order level
		DiscountLineWS percentageBasedOrderLevel = new DiscountLineWS();
		percentageBasedOrderLevel.setDiscountId(this.percentageBasedDiscount.getId());
		percentageBasedOrderLevel.setOrderId(testOrder.getId());
		percentageBasedOrderLevel.setDescription("Test Percentage based Discount applied at Order level");
        
		DiscountLineWS []orderDiscountLines = new DiscountLineWS[2];
		orderDiscountLines[0] = amountBasedOrderLevel;
		orderDiscountLines[1] = percentageBasedOrderLevel;
        
		System.out.println("Amount Based Discount @ " + amountBasedDiscount.getRateAsDecimal());
		System.out.println("Percentage Based Discount @ " + percentageBasedDiscount.getRateAsDecimal());
		
   		testOrder.setDiscountLines(orderDiscountLines);
        
        // rate order again post discounts
   		testOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, ORDER_CHANGE_STATUS_APPLY_ID));
   		
   		//discount lines to contain discount amounts
        for (DiscountLineWS discountLIne: testOrder.getDiscountLines()) {
            System.out.println(discountLIne.getDiscountAmountAsDecimal());
            assertNotNull("Discount Amount must be set for Order Level Discount lines.", 
                    discountLIne.getDiscountAmount());
            if (discountLIne.getDiscountId() == amountBasedDiscount.getId() ) 
                assertEquals("Discount Amount set right", discountLIne.getDiscountAmountAsDecimal(), new BigDecimal("5.0").negate());
        }
        // check adjusted total on order
   		System.out.println("====== Adjusted Total: " + testOrder.getAdjustedTotal());
   		System.out.println("====== Total: " + testOrder.getTotal());
   		
   		assertNotNull("Adjusted total on order should be not null", testOrder.getAdjustedTotal());
   		assertNotNull("Order Total should be not null", testOrder.getTotal());
   		
   		assertTrue(testOrder.getAdjustedTotalAsDecimal().compareTo(testOrder.getTotalAsDecimal()) < 0);
   		
   		// calculate the expected adjusted total by subtracting amount based discount from order total
   		BigDecimal expectedAdjustedTotal = testOrder.getTotalAsDecimal().subtract(DISCOUNT_RATE);
   		
   		// now deduct the percentage based discount
   		expectedAdjustedTotal = expectedAdjustedTotal.
   			subtract(testOrder.getTotalAsDecimal().multiply(DISCOUNT_RATE.divide(new BigDecimal(100))));
   		
        assertEquals(expectedAdjustedTotal.compareTo(testOrder.getAdjustedTotalAsDecimal()), 0);

		//cleanup
		api.deleteItem(testItem2Id);
		api.deleteItem(testItem1Id);
	}
	
	private DiscountWS createAmountBasedDiscount(Integer callCounter) {
		Calendar startOfThisMonth = Calendar.getInstance();
		startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);
				
		Calendar oneYearLater = Calendar.getInstance();
		oneYearLater.set(oneYearLater.get(Calendar.YEAR) + 1, oneYearLater.get(Calendar.MONTH), oneYearLater.get(Calendar.DAY_OF_MONTH));
		
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DISC-AMT-310-" + callCounter);
		discountWs.setDescription("Flat Discount (Code 310-" + callCounter + ") of $5");
		discountWs.setStartDate(startOfThisMonth.getTime());
		discountWs.setEndDate(oneYearLater.getTime());
		discountWs.setRate(DISCOUNT_RATE);
		discountWs.setType(DiscountStrategyType.ONE_TIME_AMOUNT.name());
		discountWs.setEntityId(TEST_ENTITY_ID);
		
		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}
	
	private DiscountWS createPercentageBasedDiscount(Integer callCounter) {
		Calendar startOfThisMonth = Calendar.getInstance();
		startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);
				
		Calendar oneYearLater = Calendar.getInstance();
		oneYearLater.set(oneYearLater.get(Calendar.YEAR) + 1, oneYearLater.get(Calendar.MONTH), oneYearLater.get(Calendar.DAY_OF_MONTH));
		
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DISC-PERCENT-310-" + callCounter);
		discountWs.setDescription("Discount (Code 310-" + callCounter + ") of 5%");
		discountWs.setStartDate(startOfThisMonth.getTime());
		discountWs.setEndDate(oneYearLater.getTime());
		discountWs.setRate(DISCOUNT_RATE);
		discountWs.setType(DiscountStrategyType.ONE_TIME_PERCENTAGE.name());
		
		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}
	
	@AfterMethod
    protected void tearDown() throws Exception {
		System.out.println("========= tearDown");
        
        try{
	        if (this.amountBasedDiscount != null && this.amountBasedDiscount.getId() > 0) {
	        	api.deleteDiscount(this.amountBasedDiscount.getId());
	        }
	        
	        if (this.percentageBasedDiscount != null && this.percentageBasedDiscount.getId() > 0) {
	        	api.deleteDiscount(this.percentageBasedDiscount.getId());
	        }
        } catch (Exception e) {
        	// do nothing
        	System.out.println(e);
        }
        
        if (this.customer != null) {
        	api.deleteUser(this.customer.getUserId());
        }
    }

    private OrderLineWS findOrderLineWithItem(OrderLineWS[] lines, Integer itemId) {
        for (OrderLineWS line : lines) {
            if (line.getItemId().equals(itemId)) return line;
        }
        return null;
    }
}