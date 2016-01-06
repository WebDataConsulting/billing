package com.sapienter.jbilling.server.discounts;

import java.math.BigDecimal;
import java.util.*;

import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import org.testng.annotations.Test;
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
import com.sapienter.jbilling.server.util.CreateObjectUtil;

@Test(groups = { "billing-and-discounts", "discounts" })
public class CreateDiscountSubOrderTest extends BaseDiscountApiTest {
	
	@Test
	public void testDiscountSubOrderCreation() {
        Integer userId = createUser();

		Integer itemId1 = createItem("Item1.310 Description", "310.34", "IT-0001.310", TEST_ITEM_CATEGORY);
		Integer itemId2 = createItem("Item2.310 Description", "310.22", "IT-0002.310", TEST_ITEM_CATEGORY);
        OrderWS order = buildOrder(itemId1, itemId2); //with two order lines
        order.setUserId(userId);

		//create discounts and discount lines

		// Amount based Discount applied on Order level
		DiscountWS discountOne = createAmountBasedDiscount(1);
		DiscountLineWS amountBasedOrderLevel = createDiscountLine(order, discountOne, null);

		// Percentage based Discount applied at Order level
		DiscountWS discountTwo = createPercentageBasedDiscount(1);
		DiscountLineWS percentageBasedOrderLevel = createDiscountLine(order, discountTwo, null);

		// Period based Amount Discount applied at Order level
		DiscountWS discountThree = createPeriodBasedAmountDiscount(1);
		DiscountLineWS periodBasedAmountOrderLevel = createDiscountLine(order, discountThree, null);

		// Period based Percentage Discount at Order level
		DiscountWS discountFour = createPeriodBasedPercentageDiscount(1);
		DiscountLineWS periodBasedPercentageOrderLevel = createDiscountLine(order, discountFour, null);

		// Amount based Discount at Item level
		DiscountWS discountFive = createAmountBasedDiscount(2);
		Integer itemId = order.getOrderLines()[0].getItemId();
		DiscountLineWS amountBasedItemLevel = createDiscountLine(order, discountFive, itemId);

		//amountBasedItemLevel.setOrderLineAmount("100");
		//>>>>>>> vf_rate-order-api

		// Percentage based Discount at Item level
		DiscountWS discountSix = createPercentageBasedDiscount(2);
		itemId = order.getOrderLines()[0].getItemId();
		DiscountLineWS percentageBasedItemLevel = createDiscountLine(order, discountSix, itemId);

		//percentageBasedItemLevel.setOrderLineAmount("100");
		//>>>>>>> vf_rate-order-api

		// Period based Amount Discount applied at Item level
		DiscountWS discountSeven = createPeriodBasedAmountDiscount(2);
		itemId = order.getOrderLines()[1].getItemId();//apply at second line
		DiscountLineWS periodBasedAmountItemLevel = createDiscountLine(order, discountSeven, itemId);

		//periodBasedAmountItemLevel.setOrderLineAmount("100");
		//>>>>>>> vf_rate-order-api

		// Period based Percentage Discount at Item level
		DiscountWS discountEight = createPeriodBasedPercentageDiscount(2);
		itemId = order.getOrderLines()[1].getItemId();
		DiscountLineWS periodBasedPercentageItemLevel = createDiscountLine(order, discountEight, itemId);

		//periodBasedPercentageItemLevel.setOrderLineAmount("100");
		//>>>>>>> vf_rate-order-api

		DiscountLineWS discountLines[] = new DiscountLineWS[8];
		discountLines[0] = amountBasedOrderLevel;			// Amount Based Discount applied on Order level
		discountLines[1] = percentageBasedOrderLevel;		// Percentage Based Discount applied on Order level
		discountLines[2] = periodBasedAmountOrderLevel;		// Period Based Amount Discount applied on Order level
		discountLines[3] = periodBasedPercentageOrderLevel;	// Period Based Percentage Discount applied on Order level

		discountLines[4] = amountBasedItemLevel;			// Amount Based Discount applied on Item level (or line level or product level)
		discountLines[5] = percentageBasedItemLevel;		// Percentage Based Discount applied on Item level
		discountLines[6] = periodBasedAmountItemLevel;		// Period Based Amount Discount applied on Item level
		discountLines[7] = periodBasedPercentageItemLevel;	// Period Based Percentage Discount applied on Item level

		//create order
		order.setDiscountLines(discountLines);
        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        System.out.println("Order Created " + orderId);
        assertNotNull(orderId);

		List<OrderWS> discountOrders = getDiscountOrders(orderId);
		assertTrue("Should have more that one discount order", 1 <= discountOrders.size());

        // check that order line amount for discounts is negative
		checkDiscountOrderLineAmounts(discountOrders);

		//cleanup
		for(OrderWS discountOrder : discountOrders) api.deleteOrder(discountOrder.getId());
//		api.deleteOrder(orderId);
//		api.deleteDiscount(discountEight.getId());
//		api.deleteDiscount(discountSeven.getId());
//		api.deleteDiscount(discountSix.getId());
//		api.deleteDiscount(discountFive.getId());
//		api.deleteDiscount(discountFour.getId());
//		api.deleteDiscount(discountThree.getId());
//		api.deleteDiscount(discountTwo.getId());
//		api.deleteDiscount(discountOne.getId());
		api.deleteItem(itemId1);
		api.deleteItem(itemId2);
		api.deleteUser(userId);

		System.out.println("Done testDiscountSubOrderCreation");
	}
	
	/**
	 * Checks that discount orders lines have amount less than zero.
	 */
	private void checkDiscountOrderLineAmounts(List<OrderWS> orders) {
		for (OrderWS linkedOrderWs : orders) {
			for (OrderLineWS orderLine : linkedOrderWs.getOrderLines()) {
				if (orderLine.getTypeId().equals(Constants.ORDER_LINE_TYPE_DISCOUNT)) {
					assertTrue("Order Line Type is ORDER_LINE_TYPE_DISCOUNT and Order Line Amount is positive or zero.",
							orderLine.getAmountAsDecimal().compareTo(BigDecimal.ZERO) < 0);
				} else {
					assertTrue("Order Line Type is not ORDER_LINE_TYPE_DISCOUNT and Order Line Amount is negative.",
							orderLine.getAmountAsDecimal().compareTo(BigDecimal.ZERO) >= 0);
				}
			}
		}
	}

	/**
	 * Returns the actual total discount applied by the main order id.
	 */
	private BigDecimal getActualTotalDiscountAmount(Integer mainOrderId) {
		List<OrderWS> linkedOrders = Arrays.asList(api.getLinkedOrders(mainOrderId));
        assertNotNull("linkedOrders is null.", linkedOrders);
        BigDecimal actualTotalDiscountAmount = BigDecimal.ZERO;
        for (OrderWS linkedOrderWs : linkedOrders) {
    		for (OrderLineWS orderLine : linkedOrderWs.getOrderLines()) {
    			if (orderLine.getAmountAsDecimal().compareTo(BigDecimal.ZERO) < 0) {
    				// this is a discount suborder, lets take line amount and add up
    				actualTotalDiscountAmount.add(orderLine.getAmountAsDecimal());
    			}
    		}
        }
        return actualTotalDiscountAmount;
	}
	
	private Integer createUser() {
		UserWS newUser = new UserWS();
        newUser.setUserName("Discount-User-310-"+random);
        newUser.setPassword("Admin123@");
        newUser.setLanguageId(LANGUAGE_US);
        newUser.setMainRoleId(CUSTOMER_MAIN_ROLE);
        newUser.setIsParent(Boolean.TRUE);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCompanyName("Verifone");
        
        newUser.setAccountTypeId(Integer.valueOf(1));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor


        //contact info
        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(1);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("Frodo");
        metaField4.setGroupId(1);

        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("Baggins");
        metaField5.setGroupId(1);

        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4,
                metaField5
        });

        // add a contact
        ContactWS contact = new ContactWS();
        contact.setEmail("frodo@shire.com");
        newUser.setContact(contact);
        
        return api.createUser(newUser);
	}
	
	private DiscountWS createAmountBasedDiscount(Integer callCounter) {
		Calendar startOfThisMonth = Calendar.getInstance();
		startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);
				
		Calendar oneYearLater = Calendar.getInstance();
		oneYearLater.set(oneYearLater.get(Calendar.YEAR) + 1, oneYearLater.get(Calendar.MONTH), oneYearLater.get(Calendar.DAY_OF_MONTH));
		
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DS-AM-310-" + random + callCounter);
		discountWs.setDescription("Flat Discount (Code 310-" + random + callCounter + ") of $1");
		discountWs.setStartDate(startOfThisMonth.getTime());
		discountWs.setEndDate(oneYearLater.getTime());
		discountWs.setRate(BigDecimal.ONE);
		discountWs.setType(DiscountStrategyType.ONE_TIME_AMOUNT.name());
		
		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}
	
	private DiscountWS createPercentageBasedDiscount(Integer callCounter) {
		Calendar startOfThisMonth = Calendar.getInstance();
		startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);
				
		Calendar oneYearLater = Calendar.getInstance();
		oneYearLater.set(oneYearLater.get(Calendar.YEAR) + 1, oneYearLater.get(Calendar.MONTH), oneYearLater.get(Calendar.DAY_OF_MONTH));
		
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DS-PRC-310-" + random + callCounter);
		System.out.println(discountWs.getCode());
		discountWs.setDescription("Discount (Code 310-" + random + callCounter + ") of 1%");
		discountWs.setStartDate(startOfThisMonth.getTime());
		discountWs.setEndDate(oneYearLater.getTime());
		discountWs.setRate(BigDecimal.ONE);
		discountWs.setType(DiscountStrategyType.ONE_TIME_PERCENTAGE.name());
		
		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}
	
	private DiscountWS createPeriodBasedPercentageDiscount(Integer callCounter) {
		Calendar startOfThisMonth = Calendar.getInstance();
		startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);
				
		Calendar oneYearLater = Calendar.getInstance();
		oneYearLater.set(oneYearLater.get(Calendar.YEAR) + 1, oneYearLater.get(Calendar.MONTH), oneYearLater.get(Calendar.DAY_OF_MONTH));
		
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DS-PRPC-310-" + random + callCounter);
		discountWs.setDescription("Discount (Code 310-" + random + callCounter + ") Period 3 Months off 1%");
		discountWs.setStartDate(startOfThisMonth.getTime());
		discountWs.setEndDate(oneYearLater.getTime());
		discountWs.setRate(BigDecimal.ONE);
		discountWs.setType(DiscountStrategyType.RECURRING_PERIODBASED.name());
		
		SortedMap<String, String> attributes = new TreeMap<String, String>();
		attributes.put("periodUnit", "1");		// period unit month
		attributes.put("periodValue", "3");		// 3 months
		attributes.put("isPercentage", "1");	// consider rate as percentage
		discountWs.setAttributes(attributes);
		
		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}
	
	private DiscountWS createPeriodBasedAmountDiscount(Integer callCounter) {
		Calendar startOfThisMonth = Calendar.getInstance();
		startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);
				
		Calendar oneYearLater = Calendar.getInstance();
		oneYearLater.set(oneYearLater.get(Calendar.YEAR) + 1, oneYearLater.get(Calendar.MONTH), oneYearLater.get(Calendar.DAY_OF_MONTH));
		
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DS-PRAM-310-" + random + callCounter);
		discountWs.setDescription("Discount (Code 310-" + random + callCounter + ") Period 1 Month off $30");
		discountWs.setStartDate(startOfThisMonth.getTime());
		discountWs.setEndDate(oneYearLater.getTime());
		discountWs.setRate(BigDecimal.ONE);
		discountWs.setType(DiscountStrategyType.RECURRING_PERIODBASED.name());
		
		SortedMap<String, String> attributes = new TreeMap<String, String>();
		attributes.put("periodUnit", "1");		// period unit month
		attributes.put("periodValue", "1");		// 1 month
		attributes.put("isPercentage", "0");	// Consider rate as amount
		discountWs.setAttributes(attributes);
		
		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}
	
	private DiscountLineWS createDiscountLine(OrderWS order, DiscountWS discount, Integer itemId){
		DiscountLineWS discountLine = new DiscountLineWS();
		discountLine.setDiscountId(discount.getId());
		discountLine.setOrderId(order.getId());
		discountLine.setDescription(discount.getDescription() + " Discount on Order Level:" + random);
		discountLine.setItemId(itemId);
		return discountLine;
	}

	private OrderWS buildOrder(Integer itemId1, Integer itemId2) {
		
        // need an order for it
        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(new Integer(-1)); // it does not matter, the user will be created
        newOrder.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        newOrder.setPeriod(THREE_MONTHLY_ORDER_PERIOD); // quarterly
        newOrder.setCurrencyId(CURRENCY_USD);
        newOrder.setActiveSince(new Date());

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[2];
        OrderLineWS line;

        line = new OrderLineWS();
        line.setPrice(new BigDecimal("100.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(Integer.valueOf(1));
        line.setAmount(new BigDecimal("100.00"));
        line.setDescription("First line");
        line.setItemId(itemId1);
        lines[0] = line;

        line = new OrderLineWS();
        line.setPrice(new BigDecimal("100.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(Integer.valueOf(1));
        line.setAmount(new BigDecimal("100.00"));
        line.setDescription("Second line");
        line.setItemId(itemId2);
        lines[1] = line;

        newOrder.setOrderLines(lines);

        return newOrder;
    }
	
	private OrderWS getUserSubscriptionToPlan(
			Date since, BigDecimal cost, Integer userId,
			Integer billingType, Integer orderPeriodID,
			Integer plansItemId, Integer planQuantity) {

        System.out.println("Got plan Item Id as "+plansItemId);
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(billingType);
        order.setPeriod(orderPeriodID);
        order.setCurrencyId(CURRENCY_USD);
        order.setActiveSince(since);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(planQuantity);
        line.setDescription("Order line for plan subscription");
        line.setItemId(plansItemId);
        line.setUseItem(true);

        order.setOrderLines(new OrderLineWS[]{line});

        System.out.println("User subscription...");
        return order;
    }
}
