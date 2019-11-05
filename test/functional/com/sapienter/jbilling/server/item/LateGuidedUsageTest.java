package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.annotations.Test;
import java.math.BigDecimal;
import java.util.*;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.junit.Assert.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Created by vivekmaster146 on 28/8/14.
 */

@Test(groups = {"rating-late-usage", "late-usage"})
public class LateGuidedUsageTest {

	private static final Integer DUMMY_MEDIATION_CFG_ID = 50;
    private static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static final Integer ONE_TIME_PERIOD = 1;
    private static final Integer MONTHLY_PERIOD = 2;
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyyMMdd-HHmmss");

    private static Integer PRICING_ITEM_ID= null;
    private static Integer PRICING_PLAN_ID= null;

    private static JbillingAPI api= null;

    @Test
    public void testCreateSetupData() throws Exception {

        //Setup -
        api = JbillingAPIFactory.getAPI();

        //1. Create an item "PRICING_ITEM", set price to 10.00

        ItemDTOEx newItem1 = getItem("Pricing Item", "PRICING_ITEM");
        newItem1.setPrice(new BigDecimal("10.00"));
        PRICING_ITEM_ID= api.createItem(newItem1);
        assertNotNull ("A non-null item should get created", PRICING_ITEM_ID);

        newItem1= api.getItem(PRICING_ITEM_ID, null, null);
        assertEquals("Item price is 10.00", new BigDecimal("10.0000000000"), newItem1.getPriceAsDecimal());

    }

   

/*    @Test
    public void checkLateUsageforMediation() throws Exception {
        final Integer LEMONADE = 2602;
        JbillingAPI api = null;
        try {
            api = JbillingAPIFactory.getAPI();
        } catch (JbillingAPIException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ItemDTOEx newItem1 = getItem("TEST PLAN2", "TP-02");
        newItem1.setId(api.createItem(newItem1));
        PlanWS plan = getPlan("TEST PLAN2", newItem1.getId());
        PlanItemWS planItem = new PlanItemWS();
        planItem.setItemId(LEMONADE);
        planItem.getModels().put(CommonConstants.EPOCH_DATE,
                new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.10"), 1));

        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setPeriodId(CommonConstants.ORDER_PERIOD_ONCE);
        bundle.setTargetCustomer(PlanItemBundleWS.TARGET_SELF);
        bundle.setQuantity(new BigDecimal("0"));

        planItem.setBundle(bundle);
        plan.addPlanItem(planItem);

        plan.setId(api.createPlan(plan));
        UserWS customer = CreateObjectUtil.createCustomer(1, "CustomerPriceTest2",
                "AAaa$$11", 1, 5, false, 1, null,
                CreateObjectUtil.createCustomerContact("test@gmail.com"));
        Integer userId = api.createUser(customer);
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(CommonConstants.ORDER_BILLING_POST_PAID);
        order.setPeriod(2);
        order.setCurrencyId(1);
        Calendar cal = Calendar.getInstance();
        cal.set(2014, Calendar.JULY, 1); //Year, month and day of month
        Date activeSince = cal.getTime();
        cal.set(2014, Calendar.JULY, 30); //Year, month and day of month
        Date activeUntil = cal.getTime();
        order.setActiveSince(activeSince);
        order.setActiveUntil(activeUntil);
        // subscribe to plan item
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(CommonConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId( newItem1.getId());
        line.setUseItem(true);
        line.setQuantity(1);
        order.setOrderLines(new OrderLineWS[] { line });

        api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));


// Second Order
        ItemDTOEx newItem2 = getItem("TEST PLAN3", "TP-03");
        newItem2.setId(api.createItem(newItem2));
        PlanWS planB = getPlan("TEST PLAN3", newItem2.getId());
        PlanItemWS planItemB = new PlanItemWS();
        planItemB.setItemId(LEMONADE);
        planItemB.getModels().put(CommonConstants.EPOCH_DATE,
                new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.50"), 1));

        PlanItemBundleWS bundle2 = new PlanItemBundleWS();
        bundle2.setPeriodId(CommonConstants.ORDER_PERIOD_ONCE);
        bundle2.setTargetCustomer(PlanItemBundleWS.TARGET_SELF);
        bundle2.setQuantity(new BigDecimal("0"));

        planItemB.setBundle(bundle2);
        planB.addPlanItem(planItemB);
        planB.setId(api.createPlan(planB));
        OrderWS secondOrder = new OrderWS();
        secondOrder.setUserId(userId);
        secondOrder.setBillingTypeId(CommonConstants.ORDER_BILLING_POST_PAID);
        secondOrder.setPeriod(2);
        secondOrder.setCurrencyId(1);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2014, Calendar.AUGUST, 1); //Year, month and day of month
        Date activeSince2 = cal2.getTime();
        cal2.set(2014, Calendar.AUGUST, 30); //Year, month and day of month
        Date activeUntil2 = cal2.getTime();
        secondOrder.setActiveSince(activeSince2);
        secondOrder.setActiveUntil(activeUntil2);
        // subscribe to plan item
        OrderLineWS line2 = new OrderLineWS();
        line2.setTypeId(CommonConstants.ORDER_LINE_TYPE_ITEM);
        line2.setItemId( newItem2.getId());
        line2.setUseItem(true);
        line2.setQuantity(1);
        secondOrder.setOrderLines(new OrderLineWS[] { line2 });
        api.createOrder(secondOrder, OrderChangeBL.buildFromOrder(secondOrder, ORDER_CHANGE_STATUS_APPLY_ID));

        final Integer DUMMY_MEDIATION_CFG_ID = 50;
        System.out.println("TEST OUT: Mediation CDR Record processing for July");

        //trigger to create processId
        Integer firstProcessId = api.triggerMediationByConfiguration(DUMMY_MEDIATION_CFG_ID);
        waitForMediationProcess(api);

        OrderWS processedOrder = api.processJMRData(firstProcessId, "WS06-" + String.valueOf(System.currentTimeMillis()), userId, 1, new DateTime(2014, 7, 5, 11, 40, 11, 00).toDate(),
                "Phone call to 4501231234", 2602, "10", "accountcode:1:string:06,src:1:string:6041231234,dst:1:string:4501231234,dcontext:1:string:jb-test-ctx,clid:1:string:Test user <1234>,channel:1:string:IAX2/0282119604-13,dstchannel:1:string:SIP/8315-b791bcc0,lastapp:1:string:Dial,lastdata:1:string:dial data,start:1:date:1194259211000,answer:1:date:1193913616000,end:1:date:1193913911000,duration:1:integer:500,billsec:1:integer:295,disposition:1:string:ANSWERED,amaflags:1:string:3,userfield:1:string:testUserName-1189624498100");

        assertEquals("Price should be .10, but it is: " + processedOrder.getOrderLines()[0].getPriceAsDecimal(),new BigDecimal(".50"),
                processedOrder.getOrderLines()[0].getPriceAsDecimal());
        api.deleteUser(userId);
        api.deletePlan(plan.getId());
        api.deletePlan(planB.getId());

    }*/

   

   

    @Test
    public void checkLateUsage() {

        System.out.println("TEST OUT: LateGuidedUsageTest.checkLateUsage");

        final Integer LEMONADE = 2602;
        try {
            api = JbillingAPIFactory.getAPI();

            UserWS customer = CreateObjectUtil.createCustomer(1, "CustomerPriceTest5 " + System.currentTimeMillis(),
                    "AAaa$$11", 1, 5, false, 1, null,
                    CreateObjectUtil.createCustomerContact("test@gmail.com"));
            Integer userId = api.createUser(customer);
            List<Integer> userIds = new LinkedList<Integer>();
            userIds.add(userId);
            OrderWS order = new OrderWS();
            order.setUserId(userId);
            order.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
            order.setPeriod(ServerConstants.ORDER_PERIOD_ONCE);
            order.setCurrencyId(1);
            Calendar cal = Calendar.getInstance();
            cal.set(2014, Calendar.JULY, 15);
            Date activeSince = cal.getTime();
            order.setActiveSince(activeSince);
            cal.set(2014, Calendar.JULY, 1);
            Date startDate = cal.getTime();
            cal.set(2014, Calendar.JULY, 30);
            Date endDate = cal.getTime();
            OrderLineWS line = new OrderLineWS();
            line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
            line.setItemId(LEMONADE);
            line.setUseItem(true);
            line.setQuantity(10);
            order.setOrderLines(new OrderLineWS[]{line});
            api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
            Usage usage = api.getItemUsage(null, LEMONADE, userId, userIds, startDate, endDate);
            assertEquals("Used Quantity should be 10", new BigDecimal(10), usage.getQuantity());
            api.deleteUser(userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	/**
	 *The following test was create to cover the the issue in #12609
	 */
	/*@Test
	public void checkCustomerPriceInMediation12609() throws Exception {

		api = JbillingAPIFactory.getAPI();
		String random = String.valueOf(System.currentTimeMillis() % 10000);

		//create a customer, 1-Monthly
		UserWS user = CreateObjectUtil.createCustomer(
				CommonConstants.PRIMARY_CURRENCY_ID, "CustomerPriceTest-" + random, "Admin123@",
				CommonConstants.LANGUAGE_ENGLISH_ID, CommonConstants.TYPE_CUSTOMER, false, 1, null,
				CreateObjectUtil.createCustomerContact("customer_price_" + random +"@gmail.com"));

		user.setId(api.createUser(user));

		//create an item
		ItemDTOEx item = getItem("LGU:TEST:"+random, "LGU:TEST:"+random);
		item.setPrice(new BigDecimal("1.00"));
		item.setId(api.createItem(item));

		PricingField[] fields = {
				new PricingField("accountcode", "LGUWS-" + random + "_1"),
				new PricingField("src", "6041231234"),
				new PricingField("dst", "4501231234"),
				new PricingField("dcontext", "jb-test-ctx"),
				new PricingField("clid", "Gandalf <1234>"),
				new PricingField("channel", "IAX2/0282119604-13"),
				new PricingField("dstchannel", "SIP/8315-b791bcc0"),
				new PricingField("lastapp", "Dial"),
				new PricingField("lastdata", "dial data"),
				new PricingField("start", FORMATTER.parseDateTime("20140101-114011").toDate()),
				new PricingField("answer", FORMATTER.parseDateTime("20140101-114016").toDate()),
				new PricingField("end", FORMATTER.parseDateTime("20140101-114511").toDate()),
				new PricingField("duration", 100),
				new PricingField("billsec", 150),
				new PricingField("disposition", "ANSWERED"),
				new PricingField("amaflags", "3"),
				new PricingField("itemId", item.getNumber()),
				new PricingField("userfield", user.getUserName())
		};

		Integer processId = api.processCDR(DUMMY_MEDIATION_CFG_ID, "accountcode", fields);
		assertNotNull("Process not created", processId);

		//retrieve order and check amount
		Date firstCdrDate = new DateTime(2014, 1, 1, 11, 40).toDate();
		OrderWS firstCdrOrder = api.getCurrentOrder(user.getId(), firstCdrDate);
		assertNotNull("1. Current order was not created", firstCdrOrder.getId());
		//quantity 100 x 1$ = 100$
		assertEquals("The order total is not correct", new BigDecimal("100.00"), firstCdrOrder.getTotalAsDecimal());

		
		//change the cdr to be fall into a date range where the customer has specific price
		List<PricingField> fieldsAsList = Arrays.asList(fields);
		PricingField.find(fieldsAsList, "accountcode").setStrValue("LGUWS-" + random + "_2");
		PricingField.find(fieldsAsList, "start").setDateValue(FORMATTER.parseDateTime("20140215-114011").toDate());

		//now mediate the new cdr, which has only different event date
		processId = api.processCDR(DUMMY_MEDIATION_CFG_ID, "accountcode", fields);
		assertNotNull("Process not created", processId);

		//check the validity of the current order used in the second cdr
		Date secondCdrDate = new DateTime(2014, 2, 15, 11, 40).toDate();
		OrderWS secondCdrOrder = api.getCurrentOrder(user.getId(), secondCdrDate);
		assertNotNull("2. Current order was not created", secondCdrOrder.getId());
		assertNotEquals("The current order should not be the same", firstCdrOrder.getId().intValue(), secondCdrOrder.getId().intValue());
		//quantity 100 x 2$ = 200$
		assertEquals("The order total is not correct", new BigDecimal("200.00"), secondCdrOrder.getTotalAsDecimal());

		//Here we are going to pretend that billing process ran on 01-FEB and made the first order finished
		OrderStatusWS finished = api.findOrderStatusById(api.getDefaultOrderStatusId(OrderStatusFlag.FINISHED, api.getCallerCompanyId()));
		firstCdrOrder.setOrderStatusWS(finished);
		api.updateOrder(firstCdrOrder, null);
		assertEquals("now current order has to be finished",
				finished.getId().intValue(), api.getOrder(firstCdrOrder.getId()).getOrderStatusWS().getId().intValue());

		//now create a cdr that is for the january period. In this case this is backdated cdr
		PricingField.find(fieldsAsList, "accountcode").setStrValue("LGUWS-" + random + "_3");
		PricingField.find(fieldsAsList, "start").setDateValue(FORMATTER.parseDateTime("20140128-114011").toDate());
		processId = api.processCDR(DUMMY_MEDIATION_CFG_ID, "accountcode", fields);
		assertNotNull("Process not created", processId);

		Date lateCdrDate = new DateTime(2014, 1, 28, 11, 40).toDate();
		OrderWS lateCdrOrder = api.getCurrentOrder(user.getId(), lateCdrDate);
		assertNotNull("3. Current order was not created", lateCdrOrder.getId());
		assertNotEquals("This late cdr order is the same as the first cdr order", firstCdrOrder.getId(), lateCdrOrder.getId());
		assertEquals("Both order do not have the same active since", firstCdrOrder.getActiveSince(), lateCdrOrder.getActiveSince());
		//the price for January should apply, quantity 100 x 1$ = 100$
		assertEquals("The order total is not correct", new BigDecimal("100.00"), lateCdrOrder.getTotalAsDecimal());

		//cleanup
		api.deleteOrder(lateCdrOrder.getId());
		api.deleteOrder(secondCdrOrder.getId());
		api.deleteCustomerPrice(user.getId(), customerPrice.getId());
		api.deleteItem(item.getId());
		api.deleteUser(user.getId());
	}*/

    private OrderWS subscribeUserToPricingPlan(Integer userId, Integer pricingPlanItemID) {

        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
        order.setPeriod(MONTHLY_PERIOD);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(pricingPlanItemID);
        line.setUseItem(true);
        line.setQuantity(1);
        order.setOrderLines(new OrderLineWS[] { line });

        return order;
    }

    private ItemDTOEx getItem(String description, String number) {
        ItemDTOEx newItem = new ItemDTOEx();
        newItem.setDescription(description);
        newItem.setPriceModelCompanyId(1);
        newItem.setPrice(new BigDecimal("5.00"));
        newItem.setNumber(number);
        Integer types[] = new Integer[1];
        types[0] = new Integer(1);
        newItem.setTypes(types);
        return newItem;
    }

}