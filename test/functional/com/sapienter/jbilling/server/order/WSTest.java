/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */

/*
 * Created on Dec 18, 2003
 *
 */
package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.*;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.order.validator.OrderHierarchyValidator;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.*;
import com.sapienter.jbilling.server.util.*;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.ValidatePurchaseWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.accountType.builder.AccountTypeBuilder;
import com.sapienter.jbilling.test.Asserts;
//import static com.sapienter.jbilling.test.Asserts.*;


import org.joda.time.DateMidnight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.format.DateTimeFormat;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

//import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.*;

/**
 * @author Emil
 */
@Test(groups = { "web-services", "order" })
public class WSTest {

    // Final constants
    private static final Integer GANDALF_USER_ID = 2;
    private static final int ALLOWED_ASSET_MANAGEMENT_3_CATEGORY = 32;
    private static final int ALLOWED_ASSET_MANAGEMENT_ASSET_STATUS_AVAILABLE = 103;

    private static final Integer PRANCING_PONY_ENTITY_ID = 1;
    private static final Integer MORDOR_ENTITY_ID = 2;
    private static final Integer RESELLER_ENTITY_ID = 3;
    private static final int ORDER_CANCELLATION_TASK_ID = 113;
    private static final String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
    private static final String CC_MF_NUMBER = "cc.number";
    private static final String CC_MF_EXPIRY_DATE = "cc.expiry.date";
    private static final String CC_MF_TYPE = "cc.type";
    private static final int CC_PM_ID = 1;
    private static final Integer LANGUAGE_FR = Integer.valueOf(2);
    private static final Integer PP_RESELLER_USER = Integer.valueOf(10802);

    // Prancing pony
    private static Integer PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID = null;
    private static Integer PRANCING_PONY_ACCOUNT_TYPE_ID = Integer.valueOf(1);
    private static Integer PRANCING_PONY_USER_ID;
    private static Integer PRANCING_PONY_SPARE_USER_ID;
    private static Integer PRANCING_PONY_CATEGORY_ID;
    private static Integer ORDER_PERIOD_MONTHLY;
    private static Integer ASSET_STATUS_DEFAULT;
    private static Integer ASSET_STATUS_AVAILABLE;
    private static Integer ASSET_STATUS_NOT_AVAILABLE;
    private static Integer ASSET_STATUS_ORDER_SAVED;
    // Mordor
    private static Integer MORDOR_ORDER_STATUS_INVOICE = null;
    private static Integer MORDOR_ORDER_CHANGE_STATUS_APPLY_ID = null;
    private static Integer MORDOR_ACCOUNT_TYPE_ID = null;
    private static Integer MORDOR_USER_ID;
    private static Integer MORDOR_CATEGORY_ID;
    // Reseller
    private static Integer RESELLER_ORDER_CHANGE_STATUS_APPLY_ID = null;
    private static Integer RESELLER_ACCOUNT_TYPE_ID= null;
    private static Integer RESELLER_USER_ID;
    private static Integer RESELLER_CATEGORY_ID;
    // Api
    private static JbillingAPI api;
    private static JbillingAPI mordorApi;
    private static JbillingAPI resellerApi;

    private static void sortOrderLines(OrderWS order) {
        Arrays.sort(order.getOrderLines(), new Comparator<OrderLineWS>() {
            public int compare(OrderLineWS orderLine1, OrderLineWS orderLine2) {
                return orderLine1.getDescription().compareTo(orderLine2.getDescription());
            }
        });
    }

    private static void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertTrue(expected + " != " + actual, expected.setScale(2, RoundingMode.HALF_UP).compareTo(actual.setScale(2, RoundingMode.HALF_UP)) == 0);
    }

    public static OrderChangePlanItemWS buildPlanChangeFromItem(ItemDTOEx item) {
        OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
        orderChangePlanItem.setAssetIds(new int[0]);
        orderChangePlanItem.setItemId(item.getId());
        orderChangePlanItem.setId(0);
        orderChangePlanItem.setOptlock(0);
        orderChangePlanItem.setDescription(item.getDescription());
        return orderChangePlanItem;
    }

    public static OrderChangeWS buildFromItem(ItemDTOEx item, OrderWS order, Integer statusId) {
        OrderChangeWS ws = new OrderChangeWS();
        ws.setOptLock(1);
        ws.setOrderChangeTypeId(com.sapienter.jbilling.common.Constants.ORDER_CHANGE_TYPE_DEFAULT);
        ws.setUserAssignedStatusId(statusId);
        ws.setStartDate(Util.truncateDate(new Date()));
        ws.setOrderWS(order);

        ws.setUseItem(1);

        ws.setDescription(item.getDescription());
        ws.setItemId(item.getId());
        ws.setPrice(item.getPriceAsDecimal());
        ws.setQuantity("1");

        MetaFieldWS[] metaFields = item.getOrderLineMetaFields();
        List<MetaFieldValueWS> values = new ArrayList<MetaFieldValueWS>();
        if(metaFields != null) for(MetaFieldWS mf : metaFields) {
            MetaFieldValueWS value = MetaFieldBL.createValue(mf,"");
            value.setFieldName(mf.getName());
            values.add(value);
        }
        ws.setMetaFields(values.toArray(new MetaFieldValueWS[values.size()]));
        return ws;
    }
    
    @BeforeTest
    public void initializeTests() throws Exception {

        // Prancing Pony entities
        api = JbillingAPIFactory.getAPI();
        PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeApplyStatus(api);
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
        PRANCING_PONY_USER_ID = createUser(true, null, Constants.PRIMARY_CURRENCY_ID, true, api, PRANCING_PONY_ACCOUNT_TYPE_ID).getId();
        PRANCING_PONY_SPARE_USER_ID = createUser(true, null, Constants.PRIMARY_CURRENCY_ID, true, api, PRANCING_PONY_ACCOUNT_TYPE_ID).getId();
        PRANCING_PONY_CATEGORY_ID = createItemCategory(api);
        initializeAssetsStatuses(api);

        // Mordor entities
        mordorApi = JbillingAPIFactory.getAPI("apiClientMordor");
        MORDOR_ORDER_STATUS_INVOICE = getOrCreateOrderStatusInvoice(mordorApi);
        MORDOR_ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeApplyStatus(mordorApi);
        MORDOR_ACCOUNT_TYPE_ID = new AccountTypeBuilder().create(mordorApi, true).getId();
        MORDOR_USER_ID = createSimpleUser(MORDOR_ACCOUNT_TYPE_ID, null, Constants.PRIMARY_CURRENCY_ID, true, mordorApi).getId();
        MORDOR_CATEGORY_ID = createItemCategory(mordorApi);

        // Reseller entities
        resellerApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CHILD_CLIENT.getName());
        RESELLER_ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeApplyStatus(resellerApi);
        RESELLER_ACCOUNT_TYPE_ID = new AccountTypeBuilder().create(resellerApi, true).getId();
        RESELLER_USER_ID = createSimpleUser(RESELLER_ACCOUNT_TYPE_ID, null, Constants.PRIMARY_CURRENCY_ID, true, resellerApi).getId();
        RESELLER_CATEGORY_ID = createItemCategory(resellerApi);
        
    }

    @AfterTest
    public void cleanUp(){

        // Reseller entities
        if(null != RESELLER_ORDER_CHANGE_STATUS_APPLY_ID){
            RESELLER_ORDER_CHANGE_STATUS_APPLY_ID = null;
        }
        if(null != RESELLER_ACCOUNT_TYPE_ID){
            try {
                resellerApi.deleteAccountType(RESELLER_ACCOUNT_TYPE_ID);
            } catch (Exception e){
                fail(String.format("Error deleting reseller account type %d.\n %s", RESELLER_ACCOUNT_TYPE_ID, e.getMessage()));
            } finally {
                RESELLER_ACCOUNT_TYPE_ID = null;
            }
        }
        if(null != RESELLER_CATEGORY_ID){
            try {
                resellerApi.deleteItemCategory(RESELLER_CATEGORY_ID);
            } catch (Exception e){
                fail(String.format("Error deleting reseller category %d.\n %s", RESELLER_CATEGORY_ID, e.getMessage()));
            } finally {
                RESELLER_CATEGORY_ID = null;
            }
        }
        if(null != RESELLER_USER_ID){
            try{
                resellerApi.deleteUser(RESELLER_USER_ID);
            } catch (Exception e){
                fail(String.format("Error deleting reseller user %d.\n %s", RESELLER_USER_ID, e.getMessage()));
            } finally {
                RESELLER_USER_ID = null;
            }
        }
        if(null != resellerApi){
            resellerApi = null;
        }

        // Mordor entities

        if(null != MORDOR_ORDER_STATUS_INVOICE){
            MORDOR_ORDER_STATUS_INVOICE = null;
        }

        if(null != MORDOR_ORDER_CHANGE_STATUS_APPLY_ID){
            MORDOR_ORDER_CHANGE_STATUS_APPLY_ID = null;
        }
        if(null != MORDOR_ACCOUNT_TYPE_ID){
            try{
                mordorApi.deleteAccountType(MORDOR_ACCOUNT_TYPE_ID);
            } catch (Exception e){
                fail(String.format("Error deleting mordor account type %d.\n %s", MORDOR_ACCOUNT_TYPE_ID, e.getMessage()));
            } finally {
                MORDOR_ACCOUNT_TYPE_ID = null;
            }
        }
        
        if(null != MORDOR_CATEGORY_ID){
            try{
                mordorApi.deleteItemCategory(MORDOR_CATEGORY_ID);
            } catch (Exception e){
                fail(String.format("Error deleting mordor category %d.\n %s", MORDOR_CATEGORY_ID, e.getMessage()));
            } finally {
                MORDOR_CATEGORY_ID = null;
            }
        }
        
        if(null != MORDOR_USER_ID){
            try{
                mordorApi.deleteUser(MORDOR_USER_ID);
            } catch (Exception e){
                fail(String.format("Error deleting mordor user %d.\n %s", MORDOR_USER_ID, e.getMessage()));
            } finally {
                MORDOR_USER_ID = null;
            }
        }
        if(null != mordorApi){
            mordorApi = null;
        }

        // Prancing entities
        if(null != PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID){
            PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID = null;
        }

        if(null != PRANCING_PONY_CATEGORY_ID){
            try {
                api.deleteItemCategory(PRANCING_PONY_CATEGORY_ID);
            } catch (Exception e){
                fail(String.format("Error deleting category %d.\n %s", PRANCING_PONY_CATEGORY_ID, e.getMessage()));
            } finally {
                PRANCING_PONY_CATEGORY_ID = null;
            }
        }

        if(null != PRANCING_PONY_SPARE_USER_ID){
            try {
                api.deleteUser(PRANCING_PONY_SPARE_USER_ID);
            } catch (Exception e){
                fail(String.format("Error deleting user %d.\n %s", PRANCING_PONY_SPARE_USER_ID, e.getMessage()));
            } finally {
                PRANCING_PONY_SPARE_USER_ID = null;
            }
        }

        if(null != PRANCING_PONY_USER_ID){
            try {
                api.deleteUser(PRANCING_PONY_USER_ID);
            } catch (Exception e){
                fail(String.format("Error deleting user %d.\n %s", PRANCING_PONY_USER_ID, e.getMessage()));
            } finally {
                PRANCING_PONY_USER_ID = null;
            }
        }

        if(null != api){
            api = null;
        }
    }

    @Test
    public void test001GetOrderPeriods() {
        OrderPeriodWS []orderPeriods = api.getOrderPeriods();
        assertNotNull("There should be orders periods fetched!!", orderPeriods);
        Integer periodsInitCount = orderPeriods.length;
        //creating new orderPeriod
        OrderPeriodWS period = new OrderPeriodWS();
        period.setPeriodUnitId(PeriodUnitDTO.DAY);
        period.setValue(1);
        InternationalDescriptionWS internationalDescription = new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "Daily Period");
        period.getDescriptions().add(internationalDescription);
        api.updateOrCreateOrderPeriod(period);
        orderPeriods = api.getOrderPeriods();
        assertNotNull("No Order Periods found", orderPeriods);
        assertEquals("Order periods count should increase!", Integer.valueOf(periodsInitCount + 1), Integer.valueOf(orderPeriods.length));
        for (OrderPeriodWS orderPeriodWs : orderPeriods) {
        	System.out.println("Order Period periodUnitId: " + orderPeriodWs.getPeriodUnitId() + ", Value: " + orderPeriodWs.getValue());
            // Delete order periods created in this test only
            if(orderPeriodWs.getDescription(Constants.LANGUAGE_ENGLISH_ID).getContent().equals(internationalDescription.getContent())){
                internationalDescription.setDeleted(true);
                api.deleteOrderPeriod(orderPeriodWs.getId());
            }
        }
        orderPeriods = api.getOrderPeriods();
        assertEquals("Order periods count should be same as initial!", Integer.valueOf(periodsInitCount), Integer.valueOf(orderPeriods.length));
    }

    @Test
    public void test002CreateUpdateDelete() {
        int i;

        // Create
        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);

        // Create Items for order lines
        // First Item
        ItemDTOEx firstItem = createProduct(2, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer firstItemId = api.createItem(firstItem);

        // Second Item
        ItemDTOEx secondItem = createProduct(2, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer secondItemId = api.createItem(secondItem);

        // Third Item
        ItemDTOEx thirdItem = createProduct(2, new BigDecimal("2.0"), "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer thirdItemId = api.createItem(thirdItem);

        // Add Lines
        OrderLineWS lines[] = new OrderLineWS[3];
        // Set line price
        lines[0] = buildOrderLine(firstItemId, 1, BigDecimal.TEN);
        // Use item price
        lines[1] = createOrderLine(secondItemId, 1, null);
        // Use item price
        lines[2] = createOrderLine(thirdItemId, 1, null);

        newOrder.setOrderLines(lines);

        System.out.println("Creating order ... " + newOrder);
        Integer invoiceId_1 = api.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        InvoiceWS invoice_1 = api.getInvoiceWS(invoiceId_1);
        Integer orderId_1 = invoice_1.getOrders()[0];

        assertNotNull("The order was not created", orderId_1);

        // create another one so we can test get by period.
        Integer invoiceId = api.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        System.out.println("Created invoice " + invoiceId);

        InvoiceWS newInvoice = api.getInvoiceWS(invoiceId);
        Integer orderId = newInvoice.getOrders()[0]; // this is the order that was also created

        // Create item and order in entity 2
        // Item
        ItemDTOEx mordorTestItem = createProduct(1, BigDecimal.ONE, "Mordor Product", false);
        mordorTestItem.setEntityId(MORDOR_ENTITY_ID);
        // Persist
        Integer mordorItemId = mordorApi.createItem(mordorTestItem);

        // Order
        OrderWS mordorOrder = buildOrder(MORDOR_USER_ID, Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_PERIOD_ONCE);
        // Line
        OrderLineWS mordorOrderLine = createOrderLine(mordorItemId, Integer.valueOf(1), null);
        mordorOrder.setOrderLines(new OrderLineWS[]{mordorOrderLine});
        // Persist
        Integer mordorOrderId = mordorApi.createOrder(mordorOrder, OrderChangeBL.buildFromOrder(mordorOrder, MORDOR_ORDER_CHANGE_STATUS_APPLY_ID));

        // Get

        //try getting one that doesn't belong to us
        try {
            api.getOrder(mordorOrderId);
            fail(String.format("Order %d belongs to entity 2!! Can not access foreign orders!!", mordorOrderId));
        } catch (SecurityException se){
            assertTrue("Invalid error message!!", se.getMessage().contains("Unauthorized access to entity 2"));
        }

        //verify the created order
        System.out.println("Getting created order " + orderId);
        OrderWS retOrder = api.getOrder(orderId);

        assertEquals("created order billing type", retOrder.getBillingTypeId(), newOrder.getBillingTypeId());
        assertEquals("created order billing period", retOrder.getPeriod(), newOrder.getPeriod());

        // cleanup
        api.deleteInvoice(invoiceId);

        mordorOrder = mordorApi.getOrder(mordorOrderId);
        assertEquals("More than one order line in mordor order!!", Integer.valueOf(1), Integer.valueOf(mordorOrder.getOrderLines().length));
        Integer mordorOrderLineId = mordorOrder.getOrderLines()[0].getId();

        // try getting one that doesn't belong to us
        System.out.println("Getting bad order line");
        try{
            api.getOrderLine(mordorOrderLineId);
            fail(String.format("Order line %d belongs to entity 2", mordorOrderLineId));
        } catch (SecurityException se){
            assertTrue("Invalid error message!!", se.getMessage().contains("Unauthorized access to entity 2"));
        }


        // get order line. The new order should include a new discount
        // order line that comes from the rules.
        System.out.println("Getting created order line");

        // make sure that item 2 has a special price
        for (OrderLineWS item2line: retOrder.getOrderLines()) {
            if (item2line.getItemId().intValue() == secondItemId.intValue()) {
                com.sapienter.jbilling.test.Asserts.assertEquals("Special price for Item 2", new BigDecimal("30.00"), item2line.getPriceAsDecimal().setScale(2));
                break;
            }
        }

        boolean found = false;
        OrderLineWS retOrderLine = null;
        OrderLineWS normalOrderLine = null;
        Integer lineId = null;
        for (i = 0; i < retOrder.getOrderLines().length; i++) {
            lineId = retOrder.getOrderLines()[i].getId();
            retOrderLine = api.getOrderLine(lineId);
            normalOrderLine = retOrderLine;
        }


        // Update the order line

        retOrderLine = normalOrderLine; // use a normal one, not the percentage
        retOrderLine.setQuantity(new Integer(99));

        System.out.println("Updating bad order line");
        retOrderLine.setOrderId(mordorOrderId);
        try{
            api.updateOrderLine(retOrderLine);
            fail(String.format("Can not use order line in entity 2 order!!"));
        } catch (SecurityException se){
            assertTrue("Invalid error message!!", se.getMessage().contains("Unauthorized access to entity 2"));
        }

        retOrderLine.setOrderId(orderId);

        System.out.println("Update order line " + lineId);
        api.updateOrderLine(retOrderLine);
        retOrderLine = api.getOrderLine(retOrderLine.getId());
        com.sapienter.jbilling.test.Asserts.assertEquals("updated quantity", new BigDecimal("99.00"), retOrderLine.getQuantityAsDecimal().setScale(2));

        //delete a line through updating with quantity = 0
        System.out.println("Delete order line");
        retOrderLine.setQuantity(new Integer(0));
        api.updateOrderLine(retOrderLine);
        int totalLines = retOrder.getOrderLines().length;
        pause(2000); // pause while provisioning status is being updated
        retOrder = api.getOrder(orderId);

        // the order has to have one less line now
        assertEquals("order should have one less line", totalLines, retOrder.getOrderLines().length + 1);


        // Update

        // now update the created order
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, 9, 29, 0, 0, 0);
        retOrder.setActiveSince(cal.getTime());
        //OrderStatusFlag FINISHED = 2
        OrderStatusWS ORDER_STATUS_FINISHED= api.findOrderStatusById(api.getDefaultOrderStatusId(OrderStatusFlag.FINISHED, api.getCallerCompanyId()));

        retOrder.setOrderStatusWS( ORDER_STATUS_FINISHED );

        OrderLineWS orderLine = retOrder.getOrderLines()[0];
        orderLine.setUseItem(false);
        orderLine.setDescription("Modified description");

        OrderChangeWS orderChange = OrderChangeBL.buildFromLine(orderLine, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange.setQuantity(BigDecimal.valueOf(2).subtract(orderLine.getQuantityAsDecimal().setScale(2)));

        int orderLineid = orderLine.getId();

        System.out.println("Updating order...");
        api.updateOrder(retOrder, new OrderChangeWS[]{orderChange});

        // try to update an order of another entity
        System.out.println("Updating bad order...");
        retOrder.setId(mordorOrderId);
        try {
            api.updateOrder(retOrder, null);
            fail(String.format("Order %d belongs to entity 2", mordorOrderId));
        } catch (SecurityException se){
            assertTrue("Invalid error message!!", se.getMessage().contains("Unauthorized access to entity 2"));
        }

        // and ask for it to verify the modification
        System.out.println("Getting updated order ");
        retOrder = api.getOrder(orderId);

        assertNotNull("Didn't get updated order", retOrder);
        assertTrue("Active since", retOrder.getActiveSince().compareTo(cal.getTime()) == 0);
        assertEquals("Status id", ORDER_STATUS_FINISHED.getId(), retOrder.getOrderStatusWS().getId()); //OrderStatusFlag FINISHED = 2
        for (OrderLineWS updatedLine: retOrder.getOrderLines()) {
        	if (updatedLine.getId() == orderLineid) {
        		assertEquals("Modified line description", "Modified description", updatedLine.getDescription());
                assertEquals("Modified quantity", new BigDecimal("2.00"), updatedLine.getQuantityAsDecimal().setScale(2));
                orderLineid = 0;
                break;
        	}
        }

        assertEquals("Order Line updated was not found", 0, orderLineid);

        // Get latest

        System.out.println("Getting latest");
        OrderWS lastOrder = api.getLatestOrder(PRANCING_PONY_USER_ID);
        assertNotNull("Didn't get any latest order", lastOrder);
        assertEquals("Latest id", orderId, lastOrder.getId());

        // now one for an invalid user
        System.out.println("Getting latest invalid");
        try{
            retOrder = api.getLatestOrder(MORDOR_USER_ID);
            fail(String.format("User %d belongs to entity 2", MORDOR_USER_ID));
        } catch (SecurityException se){
            assertTrue("Invalid error message!!", se.getMessage().contains("Unauthorized access to entity 2"));
        }

        // Get last

        System.out.println("Getting last 5 ... ");
        Integer[] list = api.getLastOrders(PRANCING_PONY_USER_ID, new Integer(5));
        assertNotNull("Missing list", list);
        assertTrue("No more than five", list.length <= 5 && list.length > 0);

        // the first in the list is the last one created
        retOrder = api.getOrder(new Integer(list[0]));
        assertEquals("Latest id " + Arrays.toString(list), orderId, retOrder.getId());


        // try to get the orders of my neighbor
        System.out.println("Getting last 5 - invalid");
        try{
            api.getLastOrders(MORDOR_USER_ID, new Integer(5));
            fail(String.format("User %d belongs to entity 2", MORDOR_USER_ID));
        } catch (SecurityException se){
            assertTrue("Invalid error message!!", se.getMessage().contains("Unauthorized access to entity 2"));
        }

        // Delete

        System.out.println("Deleteing order " + orderId);
        api.deleteOrder(orderId);

        // try to delete from my neightbor
        try {
            api.deleteOrder(mordorOrderId);
            fail(String.format("Order %d belongs to entity 2", mordorOrderId));
        } catch (SecurityException se){
            assertTrue("Invalid error message!!", se.getMessage().contains("Unauthorized access to entity 2"));
        }

        // try to get the deleted order
        System.out.println("Getting deleted order ");
        retOrder = api.getOrder(orderId);
        assertEquals("Order " + orderId + " should have been deleted", 1, retOrder.getDeleted());

        // Get by user and period

        System.out.println("Getting orders by period for invalid user " + orderId);

        // try to get from my neightbor
        try{
            api.getOrderByPeriod(MORDOR_USER_ID, Constants.ORDER_PERIOD_ONCE);
            fail(String.format("User %d belongs to entity 2", MORDOR_USER_ID));
        } catch (SecurityException se){
            assertTrue("Invalid error message!!", se.getMessage().contains("Unauthorized access to entity 2"));
        }

        // now from a valid user
        System.out.println("Getting orders by period ");
        Integer orders[] = api.getOrderByPeriod(PRANCING_PONY_USER_ID, Constants.ORDER_PERIOD_ONCE);
        System.out.println("Got total orders " + orders.length + " first is " + orders[0]);


        // Create an order with pre-authorization

        System.out.println("Create an order with pre-authorization" + orderId);
        PaymentAuthorizationDTOEx auth = api.createOrderPreAuthorize(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Missing list", auth);

        // the test processor should always approve
        assertEquals("Result is ok", new Boolean(true), auth.getResult());
        System.out.println("Order pre-authorized. Approval code = " + auth.getApprovalCode());

        // check the last one is a new one
        pause(2000); // pause while provisioning status is being updated
        System.out.println("Getting latest");
        retOrder = api.getLatestOrder(PRANCING_PONY_USER_ID);
        System.out.println("Order created with ID = " + retOrder.getId());
        assertNotSame("New order is there", retOrder.getId(), lastOrder.getId());

        // cleanup
        System.out.println("Cleaning invoice " + invoiceId_1);
        api.deleteInvoice(invoiceId_1);
        // delete this order
        System.out.println("Deleteing order " + retOrder.getId());
        api.deleteOrder(retOrder.getId());
        System.out.println("Cleaning order " + orderId_1);
        api.deleteOrder(orderId_1);

        api.deleteItem(thirdItemId);
        api.deleteItem(secondItemId);
        api.deleteItem(firstItemId);

        mordorApi.deleteOrder(mordorOrderId);
        mordorApi.deleteItem(mordorItemId);
    }

    @Test
    public void test003CreateOrderAndInvoiceAutoCreatesAnInvoice() {

        InvoiceWS before = api.getLatestInvoice(PRANCING_PONY_USER_ID);
        assertNull(before);

        // Create
        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);

        // Create Items for order lines
        // First Item
        ItemDTOEx firstItem = createProduct(2, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer firstItemId = api.createItem(firstItem);

        // Second Item
        ItemDTOEx secondItem = createProduct(2, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer secondItemId = api.createItem(secondItem);

        // Third Item
        ItemDTOEx thirdItem = createProduct(2, new BigDecimal("2.0"), "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer thirdItemId = api.createItem(thirdItem);

        // Add Lines
        OrderLineWS lines[] = new OrderLineWS[3];
        // Set line price
        lines[0] = buildOrderLine(firstItemId, 1, BigDecimal.TEN);
        // Use item price
        lines[1] = buildOrderLine(secondItemId, 1, null);
        // Use item price
        lines[2] = buildOrderLine(thirdItemId, 1, null);

        newOrder.setOrderLines(lines);

        System.out.println("Creating order ... " + newOrder);
        Integer invoiceId = api.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull(invoiceId);
        System.out.println("Created invoice " + invoiceId);

        InvoiceWS afterNormalOrder = api.getLatestInvoice(PRANCING_PONY_USER_ID);
        assertNotNull("createOrderAndInvoice should create invoice", afterNormalOrder);
        assertNotNull("invoice without id", afterNormalOrder.getId());

        // This doesn't make sense
//        if (before != null){
//            assertFalse("createOrderAndInvoice should create the most recent invoice", afterNormalOrder.getId().equals(before.getId()));
//        }

        // cannot create order without lines
        
        OrderWS emptyOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        emptyOrder.setOrderLines(new OrderLineWS[0]);
        try {
        	System.out.println("Empty order: " + emptyOrder);
            callCreateOrderAndInvoice(emptyOrder);
            fail("Empty order should fail validation.");
        } catch (SessionInternalError e) {
            assertTrue("Got expected validation exception", true);
        }

        // cleanup
        api.deleteInvoice(invoiceId);
        api.deleteOrder(api.getLatestOrder(PRANCING_PONY_USER_ID).getId());
        api.deleteItem(thirdItemId);
        api.deleteItem(secondItemId);
        api.deleteItem(firstItemId);
    }

    @Test
    public void test004CreateNotActiveOrderDoesNotCreateInvoices() {

        InvoiceWS before = api.getLatestInvoice(PRANCING_PONY_USER_ID);
        assertNull(before);

        // Create
        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        newOrder.setActiveSince(weeksFromToday(1));

        // Create Items for order lines
        // First Item
        ItemDTOEx firstItem = createProduct(3, null, "Product".concat(String.valueOf(System.currentTimeMillis())) ,false);
        Integer firstItemId = api.createItem(firstItem);

        // Second Item
        ItemDTOEx secondItem = createProduct(3, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer secondItemId = api.createItem(secondItem);

        // Third Item
        ItemDTOEx thirdItem = createProduct(3, new BigDecimal("2.0"), "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer thirdItemId = api.createItem(thirdItem);

        // Add Lines
        OrderLineWS lines[] = new OrderLineWS[3];
        // Set line price
        lines[0] = buildOrderLine(firstItemId, 1, BigDecimal.TEN);
        // Use item price
        lines[1] = buildOrderLine(secondItemId, 1, null);
        // Use item price
        lines[2] = buildOrderLine(thirdItemId, 1, null);

        newOrder.setOrderLines(lines);

        System.out.println("Creating order ... " + newOrder);

	    OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
		for(OrderChangeWS change : changes) change.setStartDate(newOrder.getActiveSince());

        Integer orderId = api.createOrder(newOrder, changes);
        assertNotNull(orderId);

        InvoiceWS after = api.getLatestInvoice(PRANCING_PONY_USER_ID);

        if (before == null){
            assertNull("Not yet active order -- no new invoices expected", after);
        } else {
            assertEquals("Not yet active order -- no new invoices expected", before.getId(), after.getId());
        }

        // cleanup
        api.deleteOrder(orderId);
        api.deleteItem(thirdItemId);
        api.deleteItem(secondItemId);
        api.deleteItem(firstItemId);
    }

    @Test
    public void test005CreatedOrderIsCorrect() {

        final int LINES = 2;

        // Create
        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);

        // Create Items for order lines
        // First Item
        ItemDTOEx firstItem = createProduct(4, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer firstItemId = api.createItem(firstItem);

        // Second Item
        ItemDTOEx secondItem = createProduct(4, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer secondItemId = api.createItem(secondItem);

        // Add Lines
        OrderLineWS lines[] = new OrderLineWS[LINES];
        // Set line price
        lines[0] = buildOrderLine(firstItemId, 1, BigDecimal.TEN);
        // Use item price
        lines[1] = buildOrderLine(secondItemId, 1, null);

        newOrder.setOrderLines(lines);

        Integer invoiceId = api.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull(invoiceId);
        InvoiceWS createdInvoice = api.getInvoiceWS(invoiceId);

        OrderWS resultOrder = api.getOrder(createdInvoice.getOrders()[0]);
        assertNotNull(resultOrder);
        assertEquals(LINES, resultOrder.getOrderLines().length);
        pause(2000);
        // cleanup
        api.deleteInvoice(invoiceId);
        api.deleteOrder(resultOrder.getId());

        api.deleteItem(secondItemId);
        api.deleteItem(firstItemId);
    }

    @Test
    public void test006AutoCreatedInvoiceIsCorrect() {

        final int LINES = 1;

        // it is critical to make sure that this invoice can not be composed by
        // previous payments
        // so, make the price unusual
        final BigDecimal PRICE = new BigDecimal("687654.29");

        // Create
        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);

        // Create Items for order lines
        // First Item
        ItemDTOEx firstItem = createProduct(5, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer firstItemId = api.createItem(firstItem);

        // Add Lines
        OrderLineWS lines[] = new OrderLineWS[LINES];
        // Set line price
        lines[0] = buildOrderLine(firstItemId, 1, PRICE);

        newOrder.setOrderLines(lines);

        Integer invoiceId = api.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        Integer orderId = api.getLatestOrder(PRANCING_PONY_USER_ID).getId();
        InvoiceWS invoice = api.getLatestInvoice(PRANCING_PONY_USER_ID);
        assertNotNull(invoice.getOrders());
        assertEquals(invoice.getOrders()[0], orderId);

        assertNotNull(invoice.getInvoiceLines());
        assertEquals(LINES, invoice.getInvoiceLines().length);

        assertEmptyArray(invoice.getPayments());
        assertEquals(Integer.valueOf(0), invoice.getPaymentAttempts());

        assertNotNull(invoice.getBalance());
        assertBigDecimalEquals(PRICE.multiply(new BigDecimal(LINES)), invoice.getBalanceAsDecimal().setScale(2));

        // cleanup
        api.deleteInvoice(invoiceId);
        api.deleteOrder(orderId);

        api.deleteItem(firstItemId);
    }

    @Test
    public void test007AutoCreatedInvoiceIsPayable() {

        final BigDecimal PRICE = new BigDecimal("789.00");

        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);

        // Create Items for order lines
        // First Item
        ItemDTOEx firstItem = createProduct(6, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer firstItemId = api.createItem(firstItem);

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[1];

        lines[0] = buildOrderLine(firstItemId, Integer.valueOf(1), PRICE);

        newOrder.setOrderLines(lines);

        Integer invoiceId = api.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        Integer orderId = api.getLatestOrder(PRANCING_PONY_USER_ID).getId();
        InvoiceWS invoice = api.getLatestInvoice(PRANCING_PONY_USER_ID);
        assertNotNull(invoice);
        assertNotNull(invoice.getId());
        assertEquals("new invoice is not paid", 1, invoice.getToProcess().intValue());
        assertTrue("new invoice with a balance", BigDecimal.ZERO.compareTo(invoice.getBalanceAsDecimal()) < 0);

        PaymentAuthorizationDTOEx auth = api.payInvoice(invoice.getId());
        assertNotNull(auth);
        assertEquals("Payment result OK", true, auth.getResult().booleanValue());
        assertEquals("Processor code", "The transaction has been approved", auth.getResponseMessage());

        // payment date should not be null (bug fix)
        assertNotNull("Payment date not null", api.getLatestPayment(PRANCING_PONY_USER_ID).getPaymentDate());

        // now the invoice should be shown as paid
        invoice = api.getLatestInvoice(PRANCING_PONY_USER_ID);
        assertNotNull(invoice);
        assertNotNull(invoice.getId());
        assertEquals("new invoice is now paid", 0, invoice.getToProcess().intValue());
        assertTrue("new invoice without a balance", BigDecimal.ZERO.compareTo(invoice.getBalanceAsDecimal()) == 0);

        // cleanup
        api.deleteInvoice(invoiceId);
        api.deleteOrder(orderId);
        api.deleteItem(firstItemId);
    }

    @Test
    public void test008UpdateLines() {

        final BigDecimal PRICE = new BigDecimal("789.00");

        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);

        // Create Items for order lines
        // First Item
        ItemDTOEx firstItem = createProduct(7, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer firstItemId = api.createItem(firstItem);

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[1];

        lines[0] = buildOrderLine(firstItemId, Integer.valueOf(1), PRICE);

        newOrder.setOrderLines(lines);

        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        OrderWS order = api.getOrder(orderId);
        
        int initialCount = order.getOrderLines().length;
        System.out.println("Got order with " + initialCount + " lines");

        // let's add a line
        // this is an item line
        ItemDTOEx secondItem = createProduct(7, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer secondItemId = api.createItem(secondItem);

        OrderLineWS linesArray[] = new OrderLineWS[2];
        java.lang.System.arraycopy(lines, 0, linesArray, 0, 1);
        linesArray[1] = buildOrderLine(secondItemId, Integer.valueOf(1), null);
        OrderChangeWS orderChange = OrderChangeBL.buildFromLine(linesArray[1], order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        order.setOrderLines(linesArray);

        // call the update
        System.out.println("Adding one order line: " + order);
        api.updateOrder(order, new OrderChangeWS[]{orderChange});

        // let's see if my new line is there
        order = api.getOrder(orderId);
        System.out.println("Got updated order with " + order.getOrderLines().length + " lines");
        assertEquals("One more line should be there", initialCount + 1, order.getOrderLines().length);

        // and again
        initialCount = order.getOrderLines().length;

        // this is an item line
        ItemDTOEx thirdItem = createProduct(7, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer thirdItemId = api.createItem(thirdItem);

        OrderLineWS newLinesArray[] = new OrderLineWS[3];
        java.lang.System.arraycopy(linesArray, 0, newLinesArray, 0, 2);
        newLinesArray[2] = buildOrderLine(thirdItemId, Integer.valueOf(1), null);

        orderChange = OrderChangeBL.buildFromLine(newLinesArray[2], order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        order.setOrderLines(newLinesArray);

        System.out.println("lines now " + order.getOrderLines().length);

        // call the update
        System.out.println("Adding another order line");
        api.updateOrder(order, new OrderChangeWS[]{orderChange});

        // let's see if my new line is there
        order = api.getOrder(orderId);
        System.out.println("Got updated order with " + order.getOrderLines().length + " lines");
        assertEquals("One more line should be there", initialCount + 1, order.getOrderLines().length);

        // cleanUp
        api.deleteOrder(orderId);
        api.deleteItem(thirdItemId);
        api.deleteItem(secondItemId);
        api.deleteItem(firstItemId);

    }

    /**
     * TODO Please explain the test case here in brief.
     * Question: Why is this test case important or required?
     */
    @Test
    public void test009Recreate() {

        final BigDecimal PRICE = new BigDecimal("789.00");

        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[1];

        // Create Items for order lines
        // First Item
        ItemDTOEx firstItem = createProduct(7, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer firstItemId = api.createItem(firstItem);
        lines[0] = buildOrderLine(firstItemId, Integer.valueOf(1), PRICE);

        newOrder.setOrderLines(lines);

        OrderChangeWS[] changes= OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: changes) 	change.setStartDate(newOrder.getActiveSince());
        Integer orderId = api.createOrder(newOrder, changes);
        OrderWS order = api.getLatestOrder(PRANCING_PONY_USER_ID);
	    assertEquals("The order ids should match", orderId, order.getId());
        order.setId(null);
        order.setParentOrder(null);
        order.setChildOrders(null);
        order.setProvisioningCommands(null);
        
        // use it to create another one
        changes= OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: changes)	change.setStartDate(order.getActiveSince());
        Integer newOrderId = api.createOrder(order, changes);
        assertTrue("New order newer than original", orderId.compareTo(newOrderId) < 0);

        // clean up
        api.deleteOrder(newOrderId);
        api.deleteOrder(orderId);
        api.deleteItem(firstItemId);
    }

    @Test
    public void test010RefundAndCancelFee() {

        Calendar activeSinceDate = Calendar.getInstance();
        activeSinceDate.set(Calendar.DAY_OF_MONTH, 1);

        // Create Test Lemonade Product
        ItemDTOEx testLemonade = createProduct(9, BigDecimal.TEN, "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        Integer testLemonadeId = api.createItem(testLemonade);

        // Create Test Coffee Product
        ItemDTOEx testCoffee = createProduct(9, new BigDecimal("15.0"), "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        Integer testCoffeeId = api.createItem(testCoffee);

        PluggableTaskWS plugin = new PluggableTaskWS();
        Map<String, String> parameters = new Hashtable<String, String>();
        parameters.put("fee_item_id", "" + testLemonadeId);
        plugin.setParameters((Hashtable) parameters);
        plugin.setProcessingOrder(113);
        plugin.setTypeId(ORDER_CANCELLATION_TASK_ID);

        Integer pluginId = api.createPlugin(plugin);

        OrderWS newOrder = buildOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_PRE_PAID, ORDER_PERIOD_MONTHLY);
        newOrder.setActiveSince(activeSinceDate.getTime());
        newOrder.setCancellationFee(5);
        newOrder.setCancellationFeeType("ZERO");
        newOrder.setCancellationMinimumPeriod(6);

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[2];

        // 5 lemonades 
        lines[0] = buildOrderLine(testLemonadeId, Integer.valueOf(5), null);

        // 5 coffees
        lines[1] = buildOrderLine(testCoffeeId, Integer.valueOf(5), null);

        newOrder.setOrderLines(lines);

        // create the first order and invoice it
        System.out.println("Creating order ...");
		Integer invoiceId = api.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
		assertNotNull("The invoice was not created", invoiceId);

        // update the quantities of the order (-2 lemonades, -3 coffees)
        System.out.println("Updating quantities of order ...");
        OrderWS order = api.getLatestOrder(PRANCING_PONY_USER_ID);

        assertEquals("No. of order lines", 2, order.getOrderLines().length);

        OrderLineWS orderLine = findOrderLineWithItem(order.getOrderLines(), testLemonadeId);
        OrderChangeWS change1 = OrderChangeBL.buildFromLine(orderLine, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        change1.setQuantity(BigDecimal.valueOf(3).subtract(orderLine.getQuantityAsDecimal()));
        orderLine = findOrderLineWithItem(order.getOrderLines(), testCoffeeId);
        OrderChangeWS change2 = OrderChangeBL.buildFromLine(orderLine, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        change2.setQuantity(BigDecimal.valueOf(2).subtract(orderLine.getQuantityAsDecimal()));

        api.updateOrder(order, new OrderChangeWS[]{change1, change2});

        // get last 3 orders and check what's on them (2 refunds and a fee)
        System.out.println("Getting last 3 orders ...");
        Integer[] list = api.getLastOrders(new Integer(PRANCING_PONY_USER_ID), new Integer(3));
        assertNotNull("Missing list", list);

        List<OrderWS> orders = new LinkedList<OrderWS>();
        for (Integer id : list) {
            orders.add(api.getOrder(id));
        }

        // order 1 - coffee refund
        order = findOrderWithItem(orders, testCoffeeId, 1);
        assertEquals("No. of order lines", 1, order.getOrderLines().length);
        orderLine = order.getOrderLines()[0];
        assertEquals("Item Id", testCoffeeId, orderLine.getItemId());
        com.sapienter.jbilling.test.Asserts.assertEquals("Quantity", new BigDecimal("-3.00"), orderLine.getQuantityAsDecimal().setScale(2));
        com.sapienter.jbilling.test.Asserts.assertEquals("Price", new BigDecimal("15.00"), orderLine.getPriceAsDecimal().setScale(2));
        com.sapienter.jbilling.test.Asserts.assertEquals("Amount", new BigDecimal("-45.00"), orderLine.getAmountAsDecimal().setScale(2));

        // order 3 - cancel fee for lemonade (see the rule in CancelFees.drl)
        order = findOrderWithItem(orders, testLemonadeId, 1);
        assertEquals("No. of order lines", 1, order.getOrderLines().length);
        orderLine = order.getOrderLines()[0];
        assertEquals("Item Id", testLemonadeId, orderLine.getItemId());
        com.sapienter.jbilling.test.Asserts.assertEquals("Quantity", new BigDecimal("-2.00"), orderLine.getQuantityAsDecimal().setScale(2));
        com.sapienter.jbilling.test.Asserts.assertEquals("Price", new BigDecimal("10.00"), orderLine.getPriceAsDecimal().setScale(2));
        com.sapienter.jbilling.test.Asserts.assertEquals("Amount", new BigDecimal("-20.00"), orderLine.getAmountAsDecimal().setScale(2));

        // order 2 - lemonade refund
        order = findOrderWithItem(orders, testLemonadeId, 2);
        assertEquals("No. of order lines", 2, order.getOrderLines().length);
        orderLine = findOrderLineWithItem(order.getOrderLines(), testLemonadeId);
        assertEquals("Item Id", testLemonadeId, orderLine.getItemId());
        com.sapienter.jbilling.test.Asserts.assertEquals("Quantity", new BigDecimal("3.00"), orderLine.getQuantityAsDecimal().setScale(2));
        com.sapienter.jbilling.test.Asserts.assertEquals("Price", new BigDecimal("10.00"), orderLine.getPriceAsDecimal().setScale(2));
        com.sapienter.jbilling.test.Asserts.assertEquals("Amount", new BigDecimal("30.00"), orderLine.getAmountAsDecimal().setScale(2));

        // create a new order like the first one
        System.out.println("Creating order ...");
        // to test period calculation of fees in CancellationFeeRulesTask
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(newOrder.getActiveSince().getTime());
        calendar.add(Calendar.WEEK_OF_YEAR, 12);
        newOrder.setActiveUntil(calendar.getTime());
        invoiceId = api.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("The invoice was not created", invoiceId);

	    pause(200);

        // set active until earlier than invoice date
        order = api.getLatestOrder(PRANCING_PONY_USER_ID);
		Calendar activeUntilDate = Calendar.getInstance();
		activeUntilDate.set(Calendar.DAY_OF_MONTH, 8);
		order.setActiveUntil(activeUntilDate.getTime());
        api.updateOrder(order, null);

        // get last 2 orders and check what's on them (a full refund and a fee)
        System.out.println("Getting last 2 orders ...");
        list = api.getLastOrders(PRANCING_PONY_USER_ID, Integer.valueOf(2));
        assertNotNull("Missing list", list);

        int flag = 0;
        for (int i = 0; i <= 1; i++) {
            order = api.getOrder(list[i]);
            // order - ZERO cancel fee; see OrderCancellationTask
            if (order.getOrderLines().length == 1 && order.getOrderLines()[0].getItemId().equals(testLemonadeId)) {
                assertEquals("No. of order lines", 1, order.getOrderLines().length);
                orderLine = order.getOrderLines()[0];
                // 2 periods cancelled (2 periods * 5 fee quantity)
                com.sapienter.jbilling.test.Asserts.assertEquals("Quantity", new BigDecimal("1.00"), orderLine.getQuantityAsDecimal().setScale(2));
                com.sapienter.jbilling.test.Asserts.assertEquals("Price", new BigDecimal("5.00"), orderLine.getPriceAsDecimal().setScale(2));
                com.sapienter.jbilling.test.Asserts.assertEquals("Amount", new BigDecimal("5.00"), orderLine.getAmountAsDecimal().setScale(2));
                flag++;
            } else {
                // order - full refund
                assertEquals("No. of order lines", 2, order.getOrderLines().length);
                for (OrderLineWS oLine : order.getOrderLines()) {
                    if (oLine.getItemId().equals(testLemonadeId)) {
                        com.sapienter.jbilling.test.Asserts.assertEquals("Quantity", new BigDecimal("-5.00"), oLine.getQuantityAsDecimal().setScale(2));
                        com.sapienter.jbilling.test.Asserts.assertEquals("Price", new BigDecimal("10.00"), oLine.getPriceAsDecimal().setScale(2));
                        com.sapienter.jbilling.test.Asserts.assertEquals("Amount", new BigDecimal("-50.00"), oLine.getAmountAsDecimal().setScale(2));
                        flag++;
                    }
                    if (oLine.getItemId().equals(testCoffeeId)) {
                        com.sapienter.jbilling.test.Asserts.assertEquals("Quantity", new BigDecimal("-5.00"), oLine.getQuantityAsDecimal().setScale(2));
                        com.sapienter.jbilling.test.Asserts.assertEquals("Price", new BigDecimal("15.00"), oLine.getPriceAsDecimal().setScale(2));
                        com.sapienter.jbilling.test.Asserts.assertEquals("Amount", new BigDecimal("-75.00"), oLine.getAmountAsDecimal().setScale(2));
                        flag++;
                    }
                }
            }
        }

        //Both refunds should be in single order,while seperate cancel fee order
        assertEquals("Did not find all the expected lines for cancel and refund: ", 3, flag);

        // remove invoices
        list = api.getLastInvoices(PRANCING_PONY_USER_ID, Integer.valueOf(2));
        api.deleteInvoice(list[0]);
        api.deleteInvoice(list[1]);
        // remove orders
        list = api.getLastOrders(PRANCING_PONY_USER_ID, Integer.valueOf(7));
        for (int i = 0; i < list.length; i++) {
            api.deleteOrder(list[i]);
        }

        api.deletePlugin(pluginId);
        api.deleteItem(testLemonadeId);
        api.deleteItem(testCoffeeId);
    }

    @Test
    public void test011Plan_nestedPlans() {

        /*
        Create Roaming Test Plan (monthly) which has the following product:
            - Roaming Item (bundled quantity = 0, period = monthly)
         */

        // Create Roaming Item
        ItemDTOEx roamingItem = createProduct(10, new BigDecimal("3.5"), "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        // Persist
        Integer roamingItemId = api.createItem(roamingItem);

        // Create Bundle (bundled quantity=0, period = monthly)
        PlanItemWS bundledRoamingItem = createPlanItem(roamingItemId, BigDecimal.ZERO, ORDER_PERIOD_MONTHLY);

        // Create nested plan monthly
        PlanWS roamingPlan = createPlan(10, "RP", BigDecimal.TEN, Arrays.asList(bundledRoamingItem), api);
        Integer roamingPlanSubscriptionItemId = roamingPlan.getPlanSubscriptionItemId();

        /*
        Create Tariff Test Plan (monthly) which has the following products:
            - Phone (bundled quantity = 1, period = once)
            - Roaming Plan (bundled quantity = 1, period = monthly)
         */

        // Create Phone number item
        ItemDTOEx phoneItem = createProduct(10, new BigDecimal("150.0"), "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        // Persist
        Integer phoneItemId = api.createItem(phoneItem);


        // Bundle items into plan
        // Phone (once)
        PlanItemWS phonePlanItem = createPlanItem(phoneItemId, BigDecimal.ONE, Constants.ORDER_PERIOD_ONCE);
        // Roaming Plan (monthly)
        PlanItemWS roamingPlanItem = createPlanItem(roamingPlanSubscriptionItemId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY);

        List<PlanItemWS> bundledItems = new ArrayList<PlanItemWS>();
        bundledItems.add(phonePlanItem);
        bundledItems.add(roamingPlanItem);

        // Create Plan with two items
        PlanWS tariffPlan = createPlan(10, "TP", BigDecimal.TEN, bundledItems, api);
        Integer tariffPlanSubscriptionItemId = tariffPlan.getPlanSubscriptionItemId();

        // create an order with the Tariff Test Plan
        OrderWS mainOrder = createPlanOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_POST_PAID, ORDER_PERIOD_MONTHLY, tariffPlanSubscriptionItemId);
        System.out.println("Creating plan order ...");
        Integer mainOrderId = api.createOrder(mainOrder, OrderChangeBL.buildFromOrder(mainOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("The order was not created", mainOrderId);

        // take the last order
        Integer orders[] = api.getLastOrders(PRANCING_PONY_USER_ID, Integer.valueOf(1));
        System.out.println("the last order is " + Arrays.toString(orders));

        // order with the plan
        OrderWS parentOrder = api.getOrder(orders[0]);
        assertEquals("subscription order should contain one Tariff Test Plan only!!", 1, parentOrder.getOrderLines().length);
        assertEquals("subscription item id do not match", tariffPlanSubscriptionItemId, parentOrder.getOrderLines()[0].getItemId());
        assertEquals("subscription order monthly", ORDER_PERIOD_MONTHLY, parentOrder.getPeriod());


        OrderWS[] childOrders = parentOrder.getChildOrders();
        assertEquals("The parental order must have 2 children", 2, childOrders.length);

        // one time child order
        OrderWS oneTimeOrder = null;
        if (childOrders[0].getPeriod().equals(Constants.ORDER_PERIOD_ONCE)) {
            oneTimeOrder = childOrders[0];
        } else if (childOrders[1].getPeriod().equals(Constants.ORDER_PERIOD_ONCE)) {
            oneTimeOrder = childOrders[1];
        } else {
            fail("There must be a one-time child order");
        }

        assertEquals("One time order should have one order line!", 1, oneTimeOrder.getOrderLines().length);
        assertEquals("One time order should contain item " + phoneItemId, phoneItemId, oneTimeOrder.getOrderLines()[0].getItemId());

        // order with the monthly subscription item
        OrderWS monthlySubscriptionOrder = null;
        if (childOrders[0].getPeriod() == ORDER_PERIOD_MONTHLY) {
            monthlySubscriptionOrder = childOrders[0];
        } else if (childOrders[1].getPeriod() == ORDER_PERIOD_MONTHLY) {
            monthlySubscriptionOrder = childOrders[1];
        } else {
            fail("There must be a monthly child order");
        }

        assertEquals("Monthly order should have one order line!", 1, monthlySubscriptionOrder.getOrderLines().length);
        assertEquals("Monthly order should contain nested plan id " + roamingPlanSubscriptionItemId, roamingPlan.getPlanSubscriptionItemId(), monthlySubscriptionOrder.getOrderLines()[0].getItemId());

        // clean up first delete the child order
        api.deleteOrder(monthlySubscriptionOrder.getId());
        api.deleteOrder(oneTimeOrder.getId());
        api.deleteOrder(mainOrderId);
        api.deletePlan(roamingPlan.getId());
        api.deleteItem(roamingPlanSubscriptionItemId);
        api.deleteItem(roamingItemId);
        api.deletePlan(tariffPlan.getId());
        api.deleteItem(tariffPlanSubscriptionItemId);
        api.deleteItem(phoneItemId);
    }

    @Test
    public void test012Plan_nestedPlans_2() {


//        I'm creating a (nested) Gold service plan which has the following products:
//                - SMS Service (bundled quantity=1, period = monthly)
//                - GPRS Service (bundled quantity=1, period = monthly)
//                - SMS to NA (bundled quantity=1, period = monthly)

        ItemDTOEx smsServiceItem = new ItemDTOEx();
        smsServiceItem.setDescription("SMS Service");
        smsServiceItem.setEntityId(PRANCING_PONY_ENTITY_ID);
        smsServiceItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});
        smsServiceItem.setPrice("1");
        smsServiceItem.setNumber("SMS");
        Integer smsServiceItemId = api.createItem(smsServiceItem);

        ItemDTOEx gprsServiceItem = new ItemDTOEx();
        gprsServiceItem.setDescription("GPRS Service");
        gprsServiceItem.setEntityId(PRANCING_PONY_ENTITY_ID);
        gprsServiceItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});
        gprsServiceItem.setPrice("1");
        gprsServiceItem.setNumber("GPRS");
        Integer gprsServiceItemId = api.createItem(gprsServiceItem);

        ItemDTOEx smsToNaItem = new ItemDTOEx();
        smsToNaItem.setDescription("SMS to NA");
        smsToNaItem.setEntityId(PRANCING_PONY_ENTITY_ID);
        smsToNaItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});
        smsToNaItem.setPrice("1");
        smsToNaItem.setNumber("SMSNA");
        Integer smsToNaItemId = api.createItem(smsToNaItem);

        ItemDTOEx goldServiceItem = new ItemDTOEx();
        goldServiceItem.setDescription("Gold Service Plan");
        goldServiceItem.setEntityId(PRANCING_PONY_ENTITY_ID);
        goldServiceItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});
        goldServiceItem.setPrice("1");
        goldServiceItem.setNumber("GSP");
        Integer goldServiceItemId = api.createItem(goldServiceItem);

        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, Constants.PRIMARY_CURRENCY_ID);
        SortedMap<Date, PriceModelWS> models = new TreeMap<Date, PriceModelWS>();
        models.put(Constants.EPOCH_DATE, priceModel);

        PlanItemBundleWS bundle1 = new PlanItemBundleWS();
        bundle1.setPeriodId(ORDER_PERIOD_MONTHLY);
        bundle1.setQuantity(BigDecimal.ONE);
        PlanItemWS pi1 = new PlanItemWS();
        pi1.setItemId(smsServiceItemId);
        pi1.setPrecedence(-1);
        pi1.setModels(models);
        pi1.setBundle(bundle1);

        PlanItemBundleWS bundle2 = new PlanItemBundleWS();
        bundle2.setPeriodId(ORDER_PERIOD_MONTHLY);
        bundle2.setQuantity(BigDecimal.ONE);
        PlanItemWS pi2 = new PlanItemWS();
        pi2.setItemId(gprsServiceItemId);
        pi2.setPrecedence(-1);
        pi2.setModels(models);
        pi2.setBundle(bundle2);

        PlanItemBundleWS bundle3 = new PlanItemBundleWS();
        bundle3.setPeriodId(ORDER_PERIOD_MONTHLY);
        bundle3.setQuantity(BigDecimal.ONE);
        PlanItemWS pi3 = new PlanItemWS();
        pi3.setItemId(smsToNaItemId);
        pi3.setPrecedence(-1);
        pi3.setModels(models);
        pi3.setBundle(bundle3);

        PlanWS goldServicePlan = new PlanWS();
        goldServicePlan.setItemId(goldServiceItemId);
        goldServicePlan.setDescription("Gold Service Plan");
        goldServicePlan.setPeriodId(ORDER_PERIOD_MONTHLY);
        goldServicePlan.addPlanItem(pi1);
        goldServicePlan.addPlanItem(pi2);
        goldServicePlan.addPlanItem(pi3);
        Integer goldServicePlanId = api.createPlan(goldServicePlan);

//        I'm trying to create a main plan: Basic Tariff plan which includes the following:
//                - Connection Fee product (bundled quantity=1, period = one-time)
//                - Gold service plan (bundled quantity=1, period = monthly)
//                - International call to North America (bundled quantity=0, period = monthly)


        ItemDTOEx cfItem = new ItemDTOEx();
        cfItem.setDescription("Connection Fee");
        cfItem.setEntityId(PRANCING_PONY_ENTITY_ID);
        cfItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});
        cfItem.setPrice("1");
        cfItem.setNumber("CF");
        Integer cfItemId = api.createItem(cfItem);

        ItemDTOEx naCallsItem = new ItemDTOEx();
        naCallsItem.setDescription("International call to North America");
        naCallsItem.setEntityId(PRANCING_PONY_ENTITY_ID);
        naCallsItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});
        naCallsItem.setPrice("1");
        naCallsItem.setNumber("NA");
        Integer naCallsItemId = api.createItem(naCallsItem);

        ItemDTOEx basicTariffItem = new ItemDTOEx();
        basicTariffItem.setDescription("Basic Tariff plan");
        basicTariffItem.setEntityId(PRANCING_PONY_ENTITY_ID);
        basicTariffItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});
        basicTariffItem.setPrice("1");
        basicTariffItem.setNumber("BTP");
        Integer basicTariffItemId = api.createItem(basicTariffItem);

        PlanItemBundleWS bundle4 = new PlanItemBundleWS();
        bundle4.setPeriodId(Constants.ORDER_PERIOD_ONCE);
        bundle4.setQuantity(BigDecimal.ONE);
        PlanItemWS pi4 = new PlanItemWS();
        pi4.setItemId(cfItemId);
        pi4.setPrecedence(-1);
        pi4.setModels(models);
        pi4.setBundle(bundle4);

        PlanItemBundleWS bundle5 = new PlanItemBundleWS();
        bundle5.setPeriodId(ORDER_PERIOD_MONTHLY);
        bundle5.setQuantity(BigDecimal.ONE);
        PlanItemWS pi5 = new PlanItemWS();
        pi5.setItemId(goldServiceItemId);
        pi5.setPrecedence(-1);
        pi5.setModels(models);
        pi5.setBundle(bundle5);

        PlanItemBundleWS bundle6 = new PlanItemBundleWS();
        bundle6.setPeriodId(ORDER_PERIOD_MONTHLY);
        bundle6.setQuantity(BigDecimal.ZERO);
        PlanItemWS pi6 = new PlanItemWS();
        pi6.setItemId(naCallsItemId);
        pi6.setPrecedence(-1);
        pi6.setModels(models);
        pi6.setBundle(bundle6);

        PlanWS basicPlan = new PlanWS();
        basicPlan.setItemId(basicTariffItemId);
        basicPlan.setDescription("Basic Tariff plan");
        basicPlan.setPeriodId(ORDER_PERIOD_MONTHLY);
        basicPlan.addPlanItem(pi4);
        basicPlan.addPlanItem(pi5);
        basicPlan.addPlanItem(pi6);
        Integer basicPlanPlanId = api.createPlan(basicPlan);

        // I'm trying to create an monthly recurring order for a customer with this plan
        OrderWS order = new OrderWS();
        order.setUserId(PRANCING_PONY_USER_ID);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(ORDER_PERIOD_MONTHLY);
        order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        order.setActiveSince(new Date());
        OrderLineWS orderLine = new OrderLineWS();
        orderLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        orderLine.setDescription("Order line: " + orderLine.hashCode());
        orderLine.setUseItem(true);
        orderLine.setItemId(basicTariffItemId);
        orderLine.setQuantity(1);
        orderLine.setPrice(BigDecimal.ONE);
        orderLine.setAmount(BigDecimal.ONE);
        order.setOrderLines(new OrderLineWS[] { orderLine });

        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull(orderId);

        order = api.getOrder(orderId);
        assertEquals("parent order should be monthly", ORDER_PERIOD_MONTHLY, order.getPeriod());

        sortOrderLines(order);
        OrderLineWS[] parentOrderOrderLines = order.getOrderLines();
        assertEquals("Order lines should contain only one order line!!", Integer.valueOf(1), Integer.valueOf(parentOrderOrderLines.length));
        assertEquals("Order line should contain basic tariff plan item id", basicTariffItemId, parentOrderOrderLines[0].getItemId());

        assertEquals(2, order.getChildOrders().length);
        OrderWS[] childOrders = order.getChildOrders();
        Arrays.sort(childOrders, new Comparator<OrderWS>() {
            @Override
            public int compare(OrderWS o1, OrderWS o2) {
                return o1.getPeriod().compareTo(o2.getPeriod());
            }
        });

        OrderWS childOrder1 = childOrders[0];
        assertEquals("order should be one-time", Constants.ORDER_PERIOD_ONCE, childOrder1.getPeriod());
        OrderLineWS[] orderLines1 = childOrder1.getOrderLines();
        assertEquals("there should be 1 order line", 1, orderLines1.length);

        assertEquals("CF", orderLines1[0].getProductCode());
        assertEquals("Connection Fee", orderLines1[0].getDescription());
        assertEquals(cfItemId, orderLines1[0].getItemId());


        OrderWS childOrder2 = childOrders[1];
        assertEquals("order should be monthly", ORDER_PERIOD_MONTHLY, childOrder2.getPeriod());
        sortOrderLines(childOrder2);
        OrderLineWS[] orderLines2 = childOrder2.getOrderLines();
        assertEquals("there should be 4 order lines", 4, orderLines2.length);

        Arrays.sort(orderLines2, new Comparator<OrderLineWS>() {
            @Override
            public int compare(OrderLineWS ol1, OrderLineWS ol2) {
                return ol1.getProductCode().compareTo(ol2.getProductCode());
            }
        });
        assertEquals("GPRS", orderLines2[0].getProductCode());
        assertEquals(gprsServiceItemId, orderLines2[0].getItemId());
        assertEquals("GSP", orderLines2[1].getProductCode());
        assertEquals(goldServiceItemId, orderLines2[1].getItemId());
        assertEquals("SMS", orderLines2[2].getProductCode());
        assertEquals(smsServiceItemId, orderLines2[2].getItemId());
        assertEquals("SMSNA", orderLines2[3].getProductCode());
        assertEquals(smsToNaItemId, orderLines2[3].getItemId());

        api.deleteOrder(childOrder1.getId());
        api.deleteOrder(childOrder2.getId());
        api.deleteOrder(order.getId());
        api.deletePlan(basicPlanPlanId);
        api.deleteItem(basicTariffItemId);
        api.deletePlan(goldServicePlanId);
        api.deleteItem(goldServiceItemId);
        api.deleteItem(naCallsItemId);
        api.deleteItem(cfItemId);
        api.deleteItem(smsToNaItemId);
        api.deleteItem(gprsServiceItemId);
        api.deleteItem(smsServiceItemId);
    }

    @Test
    public void test013Plan_percentageOrders() {

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2008, 9, 3);

        DiscountWS discount = new DiscountWS();
        discount.setEntityId(PRANCING_PONY_ENTITY_ID);
        String code = UUID.randomUUID().toString();
        discount.setCode(code.substring(code.length() - 10));
        Date yesterday = new Date(cal.getTimeInMillis() - TimeUnit.DAYS.toMillis(1));
        discount.setStartDate(yesterday);
        Date tomorrow = new Date(cal.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));
        discount.setEndDate(tomorrow);
        discount.setDescription(code + " Elf discount 10%");
        discount.setRate(BigDecimal.TEN);
        discount.setType(DiscountStrategyType.ONE_TIME_PERCENTAGE.toString());

        Integer discountId = api.createOrUpdateDiscount(discount);

        /*
        Create Long Distance Plan A - fixed rate (monthly) which has the following product:
            - Long Distance Call (bundled quantity = 0, period = once)
         */

        // Create Long Distance Call Item
        ItemDTOEx longDistanceCall = createProduct(10, new BigDecimal("0.2"), "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        // Persist
        Integer longDistanceCallItemId = api.createItem(longDistanceCall);

        // Bundle into future Long Distance Plan A and B
        PlanItemWS longDistanceBundle = createPlanItem(longDistanceCallItemId, BigDecimal.ZERO, Constants.ORDER_PERIOD_ONCE);

        // Create Long Distance Plan A
        PlanWS longDistancePlanA = createPlan(10, "PA", new BigDecimal("25.0"), Arrays.asList(longDistanceBundle), api);
        Integer longDistancePlanASubscriptionItemId = longDistancePlanA.getPlanSubscriptionItemId();

        /*
        Create Long Distance Plan B - fixed rate (monthly) which has the following product:
            - Long Distance Call (bundled quantity = 0, period = once)
         */

        // Create Long Distance Plan B
        PlanWS longDistancePlanB = createPlan(10, "PB", new BigDecimal("40.0"), Arrays.asList(longDistanceBundle), api);
        Integer longDistancePlanBSubscriptionItemId = longDistancePlanB.getPlanSubscriptionItemId();

        /*
        Create Percentage Line Test Plan (monthly) which has the following product:
            - Local Call (bundled quantity = 1, period = all orders)
            - Long Distance Plan A (bundled quantity = 1, period = once)
            - Long Distance Plan B (bundled quantity = 1, period = monthly)
         */

        // Create Long Distance Call Item
        ItemDTOEx localCall = createProduct(10, new BigDecimal("3.5"), "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        // Persist
        Integer localCallItemId = api.createItem(localCall);

        // Bundle Local Call Item into future Percentage Line Test Plan
        PlanItemWS localCallBundle = createPlanItem(localCallItemId, BigDecimal.ONE, Constants.ORDER_PERIOD_ALL_ORDERS);

        // Bundle Long Distance Plan A into future Percentage Line Test Plan
        PlanItemWS longDistancePlanABundle = createPlanItem(longDistancePlanASubscriptionItemId, BigDecimal.ONE, Constants.ORDER_PERIOD_ONCE);

        // Bundle Long Distance Plan B into future Percentage Line Test Plan
        PlanItemWS longDistancePlanBBundle = createPlanItem(longDistancePlanBSubscriptionItemId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY);

        List<PlanItemWS> bundledItems = new ArrayList<PlanItemWS>();
        bundledItems.add(localCallBundle);
        bundledItems.add(longDistancePlanABundle);
        bundledItems.add(longDistancePlanBBundle);

        // Create Percentage Line Test Plan
        PlanWS percentageLineTestPlan = createPlan(10, "PP", BigDecimal.ZERO, bundledItems, api);
        Integer percentageLineTestPlanSubscriptionItemId = percentageLineTestPlan.getPlanSubscriptionItemId();

        OrderWS newOrder = createPlanOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_POST_PAID, ORDER_PERIOD_MONTHLY, percentageLineTestPlanSubscriptionItemId);
        DiscountLineWS discountLine = new DiscountLineWS();
        discountLine.setDescription("Elf discount 10%");
        discountLine.setDiscountId(discountId);
        newOrder.setDiscountLines(new DiscountLineWS[] { discountLine });

        System.out.println("Creating percentage plan order ...");
        Integer newOrderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("The order was not created", newOrderId);

        // take the last three orders
        Integer percentageOrders[] = api.getLastOrders(PRANCING_PONY_USER_ID, Integer.valueOf(4));

        List<OrderWS> createdOrders = new LinkedList<OrderWS>(Arrays.asList(api.getOrder(percentageOrders[0]),
                        api.getOrder(percentageOrders[1]), api.getOrder(percentageOrders[2]),
                        api.getOrder(percentageOrders[3])
        ));
        OrderWS parent =  findOrderWithItem(createdOrders, percentageLineTestPlanSubscriptionItemId, 2);
        if (parent != null) {
            createdOrders.remove(parent);
        }
        OrderWS setUpOrder = findOrderWithItem(createdOrders, longDistancePlanASubscriptionItemId, 2);
        if (setUpOrder != null) {
            createdOrders.remove(setUpOrder);
        }
        OrderWS subscription =  findOrderWithItem(createdOrders, longDistancePlanBSubscriptionItemId, 2);
        if (subscription != null) {
            createdOrders.remove(subscription);
        }
        OrderWS discountOrder =  createdOrders.get(0);

        assertEquals("System generated Discount Order", discountOrder.getNotes());

        assertEquals("There should be 2 lines in the order.", 2, setUpOrder.getOrderLines().length);
        System.out.println("Checking that the Monthly item is in this order.");
        assertEquals("This should be the item with id " + longDistancePlanASubscriptionItemId, longDistancePlanASubscriptionItemId, setUpOrder.getOrderLines()[0].getItemId());
        assertEquals("This should be the All Orders item.", localCallItemId, setUpOrder.getOrderLines()[1].getItemId());
        assertEquals("Period should be One-Time", Constants.ORDER_PERIOD_ONCE, setUpOrder.getPeriod());

        sortOrderLines(subscription);
        assertEquals("There should be 2 lines in the order.", 2, subscription.getOrderLines().length);
        assertEquals("This should be the item with id " + longDistancePlanBSubscriptionItemId, longDistancePlanBSubscriptionItemId, subscription.getOrderLines()[0].getItemId());
        assertEquals("This should be the All Orders.", localCallItemId, subscription.getOrderLines()[1].getItemId());
        assertEquals("Period should be Monthly", ORDER_PERIOD_MONTHLY, subscription.getPeriod());

        // #8931 - The items with "bundle period = all orders" will be added to all orders.
        assertNotNull("There should be a parent order", parent);
        assertEquals("There should be 2 lines in the order.", 2, parent.getOrderLines().length);

        sortOrderLines(parent);
        assertEquals("This should be the plan with id " + percentageLineTestPlanSubscriptionItemId, percentageLineTestPlanSubscriptionItemId, parent.getOrderLines()[0].getItemId());
        assertEquals("This should be the plan with id " + localCallItemId, localCallItemId, parent.getOrderLines()[1].getItemId());
        assertEquals("Period should be Monthly", ORDER_PERIOD_MONTHLY, parent.getPeriod());

        /* Previous behaviour, made the Order to contain 3 lines, but now, the order should have
         * had 2 lines.
        System.out.println("Checking that the All Orders item is in this order.");
        assertEquals("This should be the Discount.", 14, parent.getOrderLines()[0].getItemId().intValue());
        assertEquals("This should be the All Orders item.", 2602, parent.getOrderLines()[1].getItemId().intValue());

        */
        // clean up first delete the child order
        api.deleteOrder(discountOrder.getId());
        api.deleteOrder(setUpOrder.getId());
        api.deleteOrder(subscription.getId());
        api.deleteOrder(parent.getId());
        api.deletePlan(percentageLineTestPlan.getId());
        api.deleteItem(percentageLineTestPlanSubscriptionItemId);
        api.deleteItem(localCallItemId);
        api.deletePlan(longDistancePlanB.getId());
        api.deleteItem(longDistancePlanBSubscriptionItemId);
        api.deletePlan(longDistancePlanA.getId());
        api.deleteItem(longDistancePlanASubscriptionItemId);
        api.deleteItem(longDistanceCallItemId);
    }

    @Test
    public void test014Plan_bug4491() {

        /*
        Create Long Distance Plan A - fixed rate (monthly) which has the following product:
            - Long Distance Call (bundled quantity = 0, period = once)
         */

        // Create Long Distance Call Item
        ItemDTOEx longDistanceCall = createProduct(10, new BigDecimal("0.2"), "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        // Persist
        Integer longDistanceCallItemId = api.createItem(longDistanceCall);

        // Bundle into future Long Distance Plan A and B
        PlanItemWS longDistanceBundle = createPlanItem(longDistanceCallItemId, BigDecimal.ZERO, Constants.ORDER_PERIOD_ONCE);

        // Create Long Distance Plan A
        PlanWS longDistancePlanA = createPlan(10, "Plan A", BigDecimal.TEN, Arrays.asList(longDistanceBundle), api);
        Integer longDistancePlanASubscriptionItemId = longDistancePlanA.getPlanSubscriptionItemId();

        /*
        Create Cool Plan (monthly) which has the following product:
            - Local Call (bundled quantity = 1, period = once)
            - Long Distance Plan A (bundled quantity = 1, period = once)
         */

        // Create Long Distance Call Item
        ItemDTOEx localCall = createProduct(10, new BigDecimal("20"), "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        // Persist
        Integer localCallItemId = api.createItem(localCall);

        // Bundle Local Call Item into future Cool Plan
        PlanItemWS localCallBundle = createPlanItem(localCallItemId, BigDecimal.ONE, Constants.ORDER_PERIOD_ONCE);
        localCallBundle.addModel(CommonConstants.EPOCH_DATE, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("20.0"), Constants.PRIMARY_CURRENCY_ID));

        // Bundle Long Distance Plan A into future Cool Plan
        PlanItemWS longDistancePlanABundle = createPlanItem(longDistancePlanASubscriptionItemId, BigDecimal.ONE, Constants.ORDER_PERIOD_ONCE);
        longDistancePlanABundle.addModel(CommonConstants.EPOCH_DATE, new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.TEN, Constants.PRIMARY_CURRENCY_ID));

        List<PlanItemWS> bundledItems = new ArrayList<PlanItemWS>();
        bundledItems.add(localCallBundle);
        bundledItems.add(longDistancePlanABundle);

        // Create Cool Plan
        PlanWS coolPlan = createPlan(10, "Cool Plan", new BigDecimal("99.99"), bundledItems, api);
        Integer coolPlanSubscriptionItemId = coolPlan.getPlanSubscriptionItemId();


        // Test Bug fix #4491 - When updating plan order the bundled item orders are not updated correctly.
        OrderWS order = createPlanOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_POST_PAID, ORDER_PERIOD_MONTHLY, coolPlanSubscriptionItemId);
        System.out.println("Creating plan order ...");
        Integer mainOrderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("The order was not created", mainOrderId);

        // take the last 3 orders
        Integer[] orderIds = api.getLastOrders(PRANCING_PONY_USER_ID, Integer.valueOf(3));
        List<OrderWS> createdOrders = new LinkedList<OrderWS>();
        createdOrders.add(api.getOrder(orderIds[0]));
        createdOrders.add(api.getOrder(orderIds[1]));
        createdOrders.add(api.getOrder(orderIds[2]));


        OrderWS monthlyOrder = findOrderWithItem(createdOrders, coolPlanSubscriptionItemId, 1);
        OrderWS firstOneTimeOrder = findOrderWithItem(createdOrders, localCallItemId, 1);
        OrderWS oneTimeOrder = findOrderWithItem(createdOrders, longDistancePlanASubscriptionItemId, 1);

        System.out.println("Checking the Monthly order");
        assertNotNull("Monthly order not found", monthlyOrder);
        assertEquals("There should be 1 line in the order.", 1, monthlyOrder.getOrderLines().length);
        assertEquals("This should be the plan with id " + coolPlanSubscriptionItemId, coolPlanSubscriptionItemId, monthlyOrder.getOrderLines()[0].getItemId());
        assertBigDecimalEquals(BigDecimal.ONE, monthlyOrder.getOrderLines()[0].getQuantityAsDecimal());
        assertBigDecimalEquals(new BigDecimal("99.99"), monthlyOrder.getOrderLines()[0].getAmountAsDecimal());
        assertEquals(ORDER_PERIOD_MONTHLY, monthlyOrder.getPeriod()); // Monthly

        assertNotNull("SecondMonthlyOrder order not found", firstOneTimeOrder);
        assertEquals("There should be 1 line in the order.", 1, firstOneTimeOrder.getOrderLines().length);
        assertEquals("This should be the item with id " + localCallItemId, localCallItemId, firstOneTimeOrder.getOrderLines()[0].getItemId());
        assertBigDecimalEquals(BigDecimal.ONE, firstOneTimeOrder.getOrderLines()[0].getQuantityAsDecimal());
        assertBigDecimalEquals(new BigDecimal("20.00"), firstOneTimeOrder.getOrderLines()[0].getAmountAsDecimal());
        assertEquals(Constants.ORDER_PERIOD_ONCE, firstOneTimeOrder.getPeriod()); // One time because bundle is one time

        assertNotNull("OneTimeOrder order not found", oneTimeOrder);
        assertEquals("There should be 1 line in the order.", 1, oneTimeOrder.getOrderLines().length);
        System.out.println("Checking the One time order");
        assertEquals("This should be the item with id " + longDistancePlanASubscriptionItemId, longDistancePlanASubscriptionItemId, oneTimeOrder.getOrderLines()[0].getItemId());
        assertBigDecimalEquals(BigDecimal.ONE, oneTimeOrder.getOrderLines()[0].getQuantityAsDecimal());
        assertBigDecimalEquals(new BigDecimal("10.00"), oneTimeOrder.getOrderLines()[0].getAmountAsDecimal());
        assertEquals(Constants.ORDER_PERIOD_ONCE, oneTimeOrder.getPeriod()); // One time


        // Change the plan's quantity to 3.
        OrderLineWS orderLine = monthlyOrder.getOrderLines()[0];
        OrderChangeWS orderChange = OrderChangeBL.buildFromLine(orderLine, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange.setQuantity(new BigDecimal("3.0").subtract(orderLine.getQuantityAsDecimal()));
        api.updateOrder(monthlyOrder, new OrderChangeWS[]{orderChange});

        monthlyOrder = api.getOrder(monthlyOrder.getId());
        System.out.println("Checking the Monthly order after updating the quantity to 3.");
        assertEquals("This should be the plan with id " + coolPlanSubscriptionItemId, coolPlanSubscriptionItemId, monthlyOrder.getOrderLines()[0].getItemId());
        assertBigDecimalEquals(new BigDecimal("3"), monthlyOrder.getOrderLines()[0].getQuantityAsDecimal());
        assertBigDecimalEquals(new BigDecimal("299.97"), monthlyOrder.getOrderLines()[0].getAmountAsDecimal());
        assertEquals(Integer.valueOf(ORDER_PERIOD_MONTHLY), monthlyOrder.getPeriod()); // Monthly

        firstOneTimeOrder = api.getOrder(firstOneTimeOrder.getId());
        assertEquals("This should be the item with id " + localCallItemId, localCallItemId, firstOneTimeOrder.getOrderLines()[0].getItemId());
        assertBigDecimalEquals(new BigDecimal("3"), firstOneTimeOrder.getOrderLines()[0].getQuantityAsDecimal());
        assertBigDecimalEquals(new BigDecimal("60.00"), firstOneTimeOrder.getOrderLines()[0].getAmountAsDecimal());


        // Change the plan's quantity to 2.
        orderLine = monthlyOrder.getOrderLines()[0];
        orderChange = OrderChangeBL.buildFromLine(orderLine, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange.setQuantity(new BigDecimal("2.0").subtract(orderLine.getQuantityAsDecimal()));
        api.updateOrder(monthlyOrder, new OrderChangeWS[]{orderChange});

        monthlyOrder = api.getOrder(monthlyOrder.getId());
        System.out.println("Checking the Monthly order after updating the quantity to 2.");
        assertEquals("This should be the plan with id " + coolPlanSubscriptionItemId, coolPlanSubscriptionItemId, monthlyOrder.getOrderLines()[0].getItemId());
        assertBigDecimalEquals(new BigDecimal("2"), monthlyOrder.getOrderLines()[0].getQuantityAsDecimal());
        assertBigDecimalEquals(new BigDecimal("199.98"), monthlyOrder.getOrderLines()[0].getAmountAsDecimal());
        assertEquals(Integer.valueOf(ORDER_PERIOD_MONTHLY), monthlyOrder.getPeriod()); // Monthly

        firstOneTimeOrder = api.getOrder(firstOneTimeOrder.getId());

        System.out.println("Checking the One time order after updating the quantity to 2.");
        assertEquals("This should be the item with id " + localCallItemId, localCallItemId, firstOneTimeOrder.getOrderLines()[0].getItemId());
        assertBigDecimalEquals(new BigDecimal("2"), firstOneTimeOrder.getOrderLines()[0].getQuantityAsDecimal());
        assertBigDecimalEquals(new BigDecimal("40.00"), firstOneTimeOrder.getOrderLines()[0].getAmountAsDecimal());

        // clean up first delete the child order
        api.deleteOrder(oneTimeOrder.getId());
        api.deleteOrder(firstOneTimeOrder.getId());
        api.deleteOrder(monthlyOrder.getId());
        api.deletePlan(coolPlan.getId());
        api.deleteItem(coolPlanSubscriptionItemId);
        api.deleteItem(localCallItemId);
        api.deletePlan(longDistancePlanA.getId());
        api.deleteItem(longDistancePlanASubscriptionItemId);
        api.deleteItem(longDistanceCallItemId);
    }

    @Test
    public void test015CurrentOrder() {


        // Create Test Item
        ItemDTOEx testItem = createProduct(12, BigDecimal.TEN, "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        // Persist
        Integer testItemId = api.createItem(testItem);

        //
        // Test update current order without pricing fields.
        //

        // current order before modification
        OrderWS currentOrderBefore = api.getCurrentOrder(PRANCING_PONY_USER_ID, new Date());
        System.out.println("currentOrderBefore::"+currentOrderBefore);
        // CXF returns null for empty arrays
        if (currentOrderBefore.getOrderLines() != null) {
            assertEquals("No order lines.", 0, currentOrderBefore.getOrderLines().length);
        }

        // add a single line
        OrderLineWS newLine = buildOrderLine(testItemId, Integer.valueOf(22), null, null);

        // update the current order
        OrderWS currentOrderAfter = api.updateCurrentOrder(PRANCING_PONY_USER_ID,
                                                           new OrderLineWS[] { newLine }, // adding a new order line
                                                           null,
                                                           new Date(),
                                                           "Event from WS");
        System.out.println("currentOrderAfter1::"+currentOrderAfter);
        // asserts
        assertEquals("Order ids", currentOrderBefore.getId(), currentOrderAfter.getId());
        assertEquals("1 new order line", 1, currentOrderAfter.getOrderLines().length);
        System.out.println("running..........1");
        OrderLineWS createdLine = currentOrderAfter.getOrderLines()[0];
        assertEquals("Order line item ids", newLine.getItemId(),  createdLine.getItemId());
        assertEquals("Order line quantities", newLine.getQuantityAsDecimal(), createdLine.getQuantityAsDecimal());
        com.sapienter.jbilling.test.Asserts.assertEquals("Order line price", new BigDecimal("10.00"), createdLine.getPriceAsDecimal().setScale(2));
        com.sapienter.jbilling.test.Asserts.assertEquals("Order line total", new BigDecimal("220.00"), createdLine.getAmountAsDecimal().setScale(2));

        //
        // Test update current order with pricing fields and no
        // order lines. Mediation should create them.
        //

        // Call info pricing fields. See ExampleMediationTask
        PricingField duration = new PricingField("duration", 5); // 5 min
        PricingField disposition = new PricingField("disposition", "ANSWERED");
        PricingField dst = new PricingField("dst", "12345678");
        currentOrderAfter = api.updateCurrentOrder(PRANCING_PONY_USER_ID,
                                                   null,
                                                   new PricingField[] { duration, disposition, dst },
                                                   new Date(),
                                                   "Event from WS");

        // asserts
        assertEquals("2 order line", 2, currentOrderAfter.getOrderLines().length);

        // this is the same line from the previous call
        createdLine = currentOrderAfter.getOrderLines()[0];
        assertEquals("Order line ids", newLine.getItemId(), createdLine.getItemId());
        com.sapienter.jbilling.test.Asserts.assertEquals("Order line quantities", new BigDecimal("22.00"), createdLine.getQuantityAsDecimal());
        com.sapienter.jbilling.test.Asserts.assertEquals("Order line price", new BigDecimal("10.00"), createdLine.getPriceAsDecimal());
        com.sapienter.jbilling.test.Asserts.assertEquals("Order line total", new BigDecimal("220.00"), createdLine.getAmountAsDecimal());

        // 'newPrice' pricing field, $5 * 5 units = 25
        createdLine = currentOrderAfter.getOrderLines()[1];
        com.sapienter.jbilling.test.Asserts.assertEquals("Order line quantities", new BigDecimal("5.00"), createdLine.getQuantityAsDecimal());
        com.sapienter.jbilling.test.Asserts.assertEquals("Order line price", new BigDecimal("5.00"), createdLine.getPriceAsDecimal());
        com.sapienter.jbilling.test.Asserts.assertEquals("Order line amount", new BigDecimal("25.00"), createdLine.getAmountAsDecimal());

        //
        // Events that go into an order already invoiced, should update the
        // current order for the next cycle
        //

        // fool the system making the current order finished (don't do this at home)
	    System.out.println("Making current order 'FINISHED'");
	    //OrderStatusFlag FINISHED = 2
	    OrderStatusWS ORDER_STATUS_FINISHED= api.findOrderStatusById(api.getDefaultOrderStatusId(OrderStatusFlag.FINISHED, api.getCallerCompanyId()));
	    currentOrderAfter.setOrderStatusWS(ORDER_STATUS_FINISHED);
	    api.updateOrder(currentOrderAfter, null);
	    assertEquals("now current order has to be finished",
			    ORDER_STATUS_FINISHED.getId().intValue(), api.getOrder(currentOrderAfter.getId()).getOrderStatusWS().getId().intValue());


	    // now send again that last event
	    System.out.println("Sending event again");
	    OrderWS currentOrderNext = api.updateCurrentOrder(PRANCING_PONY_USER_ID,
			    null,
			    new PricingField[] { duration, disposition, dst },
			    new Date(),
			    "Same event from WS");

	    assertNotNull("Current order for next cycle should be provided", currentOrderNext);
	    assertFalse("Current order for next cycle can't be the same as the previous one",
			    currentOrderNext.getId().equals(currentOrderAfter.getId()));


        // make that current order an invoice
	    Integer invoiceIds[] = api.createInvoice(PRANCING_PONY_USER_ID, false);
	    assertNotNull("Invoices not generates", invoiceIds);
	    assertTrue("Invoices not generates", invoiceIds.length > 0);

        Integer invoiceId = invoiceIds[0];
        System.out.println("current order generated invoice " + invoiceId);

        //
        // Security tests
        //

        try {
            api.getCurrentOrder(MORDOR_USER_ID, new Date()); // returns null, not a real test
            fail(String.format("User %d belongs to child entity %d", MORDOR_USER_ID, MORDOR_ENTITY_ID));
        } catch (Exception e) { }

        try {
            api.updateCurrentOrder(MORDOR_USER_ID,
                                   new OrderLineWS[] { newLine },
                                   new PricingField[] { },
                                   new Date(),
                                   "Event from WS");

            fail(String.format("User %d belongs to child entity %d", MORDOR_USER_ID, MORDOR_ENTITY_ID));
        } catch (Exception e) { }

        // cleanup
        api.deleteInvoice(invoiceId);
        api.deleteOrder(currentOrderAfter.getId());
        api.deleteItem(testItemId);
    }

    @Test
    public void test016IsUserSubscribedTo() {

        // Test a non-existing user first, result should be 0
        String result = api.isUserSubscribedTo(999999, 999999);
        assertEquals(BigDecimal.ZERO, new BigDecimal(result));

        final BigDecimal PRICE = new BigDecimal("789.00");

        OrderWS newOrder = buildOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_PERIOD_ALL_ORDERS);

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[1];

        // Create Items for order lines
        // First Item
        ItemDTOEx firstItem = createProduct(15, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer firstItemId = api.createItem(firstItem);
        lines[0] = buildOrderLine(firstItemId, Integer.valueOf(1), PRICE);

        newOrder.setOrderLines(lines);

        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        OrderWS order = api.getLatestOrder(PRANCING_PONY_USER_ID);

        result = api.isUserSubscribedTo(PRANCING_PONY_USER_ID, firstItemId);
        assertEquals(BigDecimal.ONE.setScale(2), new BigDecimal(result).setScale(2));

        api.deleteOrder(orderId);
        api.deleteItem(firstItemId);

    }

    @Test
    public void test017GetUserItemsByCategory() {

        // Test a non-existing user first, result should be 0
        Integer[] result = api.getUserItemsByCategory(999999, 999999);
        assertNull(result);

        final BigDecimal PRICE = new BigDecimal("789.00");

        OrderWS newOrder = buildOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_PERIOD_ALL_ORDERS);

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[2];

        // Create Items for order lines
        // First Item
        ItemDTOEx firstItem = createProduct(16, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer firstItemId = api.createItem(firstItem);

        lines[0] = buildOrderLine(firstItemId, Integer.valueOf(1), PRICE);

        // let's add a line
        // this is an item line
        ItemDTOEx secondItem = createProduct(16, null, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer secondItemId = api.createItem(secondItem);

        // take the description from the item
        lines[1] = buildOrderLine(secondItemId, Integer.valueOf(1), null);

        newOrder.setOrderLines(lines);

        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        OrderWS order = api.getLatestOrder(PRANCING_PONY_USER_ID);
        result = api.getUserItemsByCategory(PRANCING_PONY_USER_ID, PRANCING_PONY_CATEGORY_ID);
        Arrays.sort(result);
        assertEquals(2, result.length);
        assertEquals(firstItemId, result[0]);
        assertEquals(secondItemId, result[1]);

        // cleanUp
        api.deleteOrder(orderId);
        api.deleteItem(secondItemId);
        api.deleteItem(firstItemId);

    }

    @Test
    public void test018OrderLineDescriptionLanguage() {

        // Modify user to be french user
        UserWS frenchUser = api.getUserWS(PRANCING_PONY_SPARE_USER_ID);
        frenchUser.setLanguageId(LANGUAGE_FR);
        frenchUser.setPassword(null);
        api.updateUser(frenchUser);

        // Create Test Item with french description
        ItemDTOEx testItem = createProduct(17, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>();
        InternationalDescriptionWS enDesc = new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "itemDescription-en");
        InternationalDescriptionWS frDesc = new InternationalDescriptionWS(LANGUAGE_FR, "itemDescription-fr");
        descriptions.add(enDesc);
        descriptions.add(frDesc);
        testItem.setDescriptions(descriptions);
        // Persist
        Integer testItemId = api.createItem(testItem);

        // create order
        OrderWS order = createPlanOrder(PRANCING_PONY_SPARE_USER_ID, Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_PERIOD_ONCE, testItemId);

        // create order and invoice
        Integer invoiceId = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        // check invoice line
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Number of invoice lines", 1,
                     invoice.getInvoiceLines().length);

        InvoiceLineDTO invoiceLine = invoice.getInvoiceLines()[0];
        assertEquals("French description",
                     "itemDescription-fr",
                     invoiceLine.getDescription());

        // back user to initial state
        frenchUser = api.getUserWS(PRANCING_PONY_SPARE_USER_ID);
        frenchUser.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        frenchUser.setPassword(null);
        api.updateUser(frenchUser);

        // clean up
        api.deleteInvoice(invoiceId);
        api.deleteOrder(invoice.getOrders()[0]);
        api.deleteItem(testItemId);
    }

    @Test
    public void test019GraduatedPlanItems() {

        // Create Item that is going to be included in two test plans
        ItemDTOEx includedInPlansItem = createProduct(18, BigDecimal.ZERO, "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        // Persist
        Integer includedInPlansItemId = api.createItem(includedInPlansItem);

        /*
        Create Test Plan (monthly) which has the following products:
            - Included in plans item(graduated rate = 3.5, included qty = 1) (bundled quantity = 0, period = once)
         */

        // Price model for included product in plan
        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.GRADUATED.name(), new BigDecimal("3.5"), Constants.PRIMARY_CURRENCY_ID);
        priceModel.addAttribute("included", "1");

        // Create plan bundle
        PlanItemWS itemBundle = createPlanItem(includedInPlansItemId, BigDecimal.ZERO, Constants.ORDER_PERIOD_ONCE);
        itemBundle.addModel(CommonConstants.EPOCH_DATE, priceModel);

        // Create and persist plan
        PlanWS testPlan = createPlan(18, "TP1", BigDecimal.TEN, Arrays.asList(itemBundle), api);
        Integer testPlanSubscriptionId = testPlan.getPlanSubscriptionItemId();

        /*
        Create Test Plan 2 (monthly) which has the following products:
            - Test Plan (metered rate = 10) (bundled quantity = 0, period = once)
         */

        priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.TEN, Constants.PRIMARY_CURRENCY_ID);

        // Create plan bundle
        PlanItemWS planBundle = createPlanItem(testPlanSubscriptionId, BigDecimal.ZERO, Constants.ORDER_PERIOD_ONCE);
        planBundle.addModel(CommonConstants.EPOCH_DATE, priceModel);

        // Create and persist plan
        PlanWS testPlan2 = createPlan(18, "TP2", new BigDecimal("20.0"), Arrays.asList(planBundle), api);
        Integer testPlanSubscriptionId2 = testPlan2.getPlanSubscriptionItemId();

        // Create Monthly order including these two plans
        OrderWS plansOrder = createPlanOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_PRE_PAID, ORDER_PERIOD_MONTHLY, testPlanSubscriptionId, testPlanSubscriptionId2);
        Integer planOrderId = api.createOrder(plansOrder, OrderChangeBL.buildFromOrder(plansOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        // add items to a user subscribed to Test Plan
        System.out.println("Testing item swapping - included in plan");
        OrderWS order = createPlanOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_PERIOD_ONCE, includedInPlansItemId);
        order.getOrderLines()[0].setQuantity(new BigDecimal("100")); // doesn't exceed included plan quantity, priced at $0

        int orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        order = api.getOrder(orderId);

        assertEquals("Order should have one line", 1, order.getOrderLines().length);
        assertEquals("Order should have the included in plan line", includedInPlansItemId, order.getOrderLines()[0].getItemId());

        // cleanup
        api.deleteOrder(orderId);

        // now a guy without the plan

        System.out.println("Testing item swapping - NOT included in plan");
        order = createPlanOrder(PRANCING_PONY_SPARE_USER_ID, Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_PERIOD_ONCE, includedInPlansItemId);
        order.getOrderLines()[0].setQuantity(new BigDecimal("100")); // full quantity priced at $0.30/unit

        orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        order = api.getOrder(orderId);

        assertEquals("Order should have one line", 1, order.getOrderLines().length);
        assertEquals("Order should have the priced item line", includedInPlansItemId, order.getOrderLines()[0].getItemId());

        // cleanup
        api.deleteOrder(orderId);
        api.deleteOrder(planOrderId);
        api.deletePlan(testPlan2.getId());
        api.deleteItem(testPlanSubscriptionId2);
        api.deletePlan(testPlan.getId());
        api.deleteItem(testPlanSubscriptionId);
        api.deleteItem(includedInPlansItemId);
    }

    @Test
    public void test019RateCard() throws Exception {
        System.out.println("#test019RateCard");
        //JbillingAPI api = JbillingAPIFactory.getAPI();

        System.out.println("Testing Rate Card");

        // user for tests
        UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);
        Integer userId = user.getUserId();

        // update to credit limit
        user.setCreditLimit(new BigDecimal("100.0"));
        user.setMainSubscription(com.sapienter.jbilling.server.user.WSTest.createUserMainSubscription());
        user.setPassword(null);
        api.updateUser(user);



        //    updateCurrentOrder


        // should be priced at 0.33 (see row 548)
        PricingField[] pf = {
                new PricingField("dst", "55999"),
                new PricingField("duration", 1),
                new PricingField("disposition", "ANSWERED")
        };

        OrderWS currentOrder = api.updateCurrentOrder(userId, null, pf, new Date(), "Event from WS");

        assertEquals("1 order line", 1, currentOrder.getOrderLines().length);
        OrderLineWS line = currentOrder.getOrderLines()[0];
        assertEquals("order line itemId", 2800, line.getItemId().intValue());
        com.sapienter.jbilling.test.Asserts.assertEquals("order line quantity", new BigDecimal("1.00"), line.getQuantityAsDecimal());
        com.sapienter.jbilling.test.Asserts.assertEquals("order line total", new BigDecimal("0.33"), line.getAmountAsDecimal());

        // check dynamic balance
        user = api.getUserWS(userId);
        com.sapienter.jbilling.test.Asserts.assertEquals("dynamic balance", new BigDecimal("-0.33"), user.getDynamicBalanceAsDecimal());

        // should be priced at 0.08 (see row 1753)
        pf[0].setStrValue("55000");
        currentOrder = api.updateCurrentOrder(userId,null, pf, new Date(), "Event from WS");

        assertEquals("1 order line", 1, currentOrder.getOrderLines().length);
        line = currentOrder.getOrderLines()[0];
        assertEquals("order line itemId", 2800, line.getItemId().intValue());
        com.sapienter.jbilling.test.Asserts.assertEquals("order line quantity", new BigDecimal("2.00"), line.getQuantityAsDecimal());

        // 0.33 + 0.08 = 0.41
        com.sapienter.jbilling.test.Asserts.assertEquals("order line total", new BigDecimal("0.41"), line.getAmountAsDecimal());

        // check dynamic balance
        user = api.getUserWS(userId);
        com.sapienter.jbilling.test.Asserts.assertEquals("dynamic balance", new BigDecimal("-0.41"), user.getDynamicBalanceAsDecimal());



        //    getItem


        // should be priced at 0.42 (see row 1731)
        pf[0].setStrValue("212222");
        ItemDTOEx item = api.getItem(2800, userId, pf);
        com.sapienter.jbilling.test.Asserts.assertEquals("price", new BigDecimal("0.42"), item.getPriceAsDecimal());



        //    rateOrder


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

        OrderWS order = api.rateOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        assertEquals("1 order line", 1, currentOrder.getOrderLines().length);
        line = order.getOrderLines()[0];
        assertEquals("order line itemId", 2800, line.getItemId().intValue());
        com.sapienter.jbilling.test.Asserts.assertEquals("order line quantity", new BigDecimal("10.00"), line.getQuantityAsDecimal());

        // 0.42 * 10 = 4.2
        com.sapienter.jbilling.test.Asserts.assertEquals("order line total", new BigDecimal("4.20"), line.getAmountAsDecimal());



        //     validatePurchase


        // should be priced at 0.47 (see row 498)
        pf[0].setStrValue("187630");

        // current balance: 100 - 0.41 = 99.59
        // quantity available expected: 99.59 / 0.47
        ValidatePurchaseWS result = api.validatePurchase(userId, null, pf);
        assertEquals("validate purchase success", Boolean.valueOf(true), result.getSuccess());
        assertEquals("validate purchase authorized", Boolean.valueOf(true), result.getAuthorized());
        com.sapienter.jbilling.test.Asserts.assertEquals("validate purchase quantity", new BigDecimal("211.89"), result.getQuantityAsDecimal());

        // check current order wasn't updated
        currentOrder = api.getOrder(currentOrder.getId());
        assertEquals("1 order line", 1, currentOrder.getOrderLines().length);
        line = currentOrder.getOrderLines()[0];
        assertEquals("order line itemId", 2800, line.getItemId().intValue());
        com.sapienter.jbilling.test.Asserts.assertEquals("order line quantity", new BigDecimal("2.00"), line.getQuantityAsDecimal());
        com.sapienter.jbilling.test.Asserts.assertEquals("order line total", new BigDecimal("0.41"), line.getAmountAsDecimal());

        // clean up
        api.deleteUser(userId);
    }

    @Test
    public void test020CreateUpdateDeleteAsset() {

        ItemDTOEx firstItem = createProduct(20, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), true);
        Integer firstItemId = api.createItem(firstItem);
        String firstAssetIdentifier = "Asset020-First".concat(String.valueOf(System.currentTimeMillis()));
        String secondAssetIdentifier = "Asset020-Second".concat(String.valueOf(System.currentTimeMillis()));
        String thirdAssetIdentifier = "Asset020-Third".concat(String.valueOf(System.currentTimeMillis()));
        Integer ASSET_1 = api.createAsset(getAssetWS(firstAssetIdentifier, ASSET_STATUS_DEFAULT, firstItemId));
        Integer ASSET_2 = api.createAsset(getAssetWS(secondAssetIdentifier, ASSET_STATUS_DEFAULT, firstItemId));
        Integer ASSET_3 = api.createAsset(getAssetWS(thirdAssetIdentifier, ASSET_STATUS_DEFAULT, firstItemId));

        // Create

        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[1];
        // Line with asset
        lines[0] = buildOrderLine(firstItemId, Integer.valueOf(1), BigDecimal.TEN, ASSET_1);

        newOrder.setOrderLines(lines);

        System.out.println("Creating order ... " + newOrder);

        // create order
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        System.out.println("Created order " + orderId);


        //check that we can not change the status of the asset
        AssetWS savedAsset = api.getAsset(ASSET_1);
        savedAsset.setAssetStatusId(ASSET_STATUS_DEFAULT);
        try {
            api.updateAsset(savedAsset);
            fail("Exception expected");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "AssetWS,assetStatus,asset.validation.status.change.fromordersaved");
        }

        // get

        OrderWS retOrder = api.getOrder(orderId);

        assertEquals("Must have 1 order line", 1, retOrder.getOrderLines().length);

        OrderLineWS line = retOrder.getOrderLines()[0];
        assertEquals("Must have 1 asset", 1, line.getAssetIds().length);
        assertEquals("Check asset id", ASSET_1, line.getAssetIds()[0]);

        //check that the asset is assigned and transition made
        checkAssetStatus(api, ASSET_1, ASSET_STATUS_ORDER_SAVED, line.getId(), PRANCING_PONY_USER_ID);

        //change the asset
        line.setAssetIds(new Integer[] {ASSET_2});

        System.out.println("Update order line " + line.getId());
        api.updateOrderLine(line);

        //check that we have the asset
        line = api.getOrderLine( line.getId());
        assertEquals("Must have 1 asset", 1, line.getAssetIds().length);
        assertEquals("Check asset id", ASSET_2, line.getAssetIds()[0]);

        //check that the new asset is assigned
        checkAssetStatus(api, ASSET_2, ASSET_STATUS_ORDER_SAVED, line.getId(), PRANCING_PONY_USER_ID);

        //check that the old asset is unassigned
        checkAssetStatus(api, ASSET_1, ASSET_STATUS_DEFAULT, null, null);

        //delete a line through updating with quantity = 0
        System.out.println("Delete order line");
        line.setQuantity(new Integer(0));
        api.updateOrderLine(line);

        //asset must be unassigned
        checkAssetStatus(api, ASSET_2, ASSET_STATUS_DEFAULT, null, null);

        // Update

        // now add a new lines
        lines = new OrderLineWS[2];
        line = createOrderLineForAsset(2, ASSET_1, ASSET_2);

        lines[0] = buildOrderLine(firstItemId, Integer.valueOf(2), BigDecimal.TEN, ASSET_1, ASSET_2);
        OrderChangeWS change1 = OrderChangeBL.buildFromLine(lines[0], retOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        lines[1] = buildOrderLine(firstItemId, Integer.valueOf(1), BigDecimal.TEN, ASSET_3);
        OrderChangeWS change2 = OrderChangeBL.buildFromLine(lines[1], retOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        retOrder.setOrderLines(lines);

        System.out.println("Update line with multiple assets and lines");
        api.updateOrder(retOrder, new OrderChangeWS[]{change1, change2});

        // and ask for it to verify the modification
        System.out.println("Getting updated order ");
        retOrder = api.getOrder(orderId);

        assertNotNull("Didn't get updated order", retOrder);
        assertEquals("Check has 2 order lines", 2, retOrder.getOrderLines().length);

        //check that we have the assets assigned
        for (OrderLineWS updatedLine: retOrder.getOrderLines()) {
            if(updatedLine.getAssetIds().length == 1) {
                assertEquals("Asset id should be equal.",ASSET_3, updatedLine.getAssetIds()[0]);
                //check that the new asset is assigned
                checkAssetStatus(api, ASSET_3, ASSET_STATUS_ORDER_SAVED, updatedLine.getId(), PRANCING_PONY_USER_ID);
            } else {
                assertEquals(2, updatedLine.getAssetIds().length);
                checkAssetStatus(api, ASSET_1, ASSET_STATUS_ORDER_SAVED, updatedLine.getId(), PRANCING_PONY_USER_ID);
                checkAssetStatus(api, ASSET_2, ASSET_STATUS_ORDER_SAVED, updatedLine.getId(), PRANCING_PONY_USER_ID);
            }
        }

        List<OrderChangeWS> orderChanges = new LinkedList<OrderChangeWS>();
        //unassign some assets
        Integer updatedLineId = null;
        for (OrderLineWS updatedLine: retOrder.getOrderLines()) {
            if(updatedLine.getAssetIds().length == 1) {
                OrderChangeWS change = OrderChangeBL.buildFromLine(updatedLine, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
                change.setQuantity(BigDecimal.ZERO.subtract(updatedLine.getQuantityAsDecimal()));
                change.setAssetIds(new Integer[0]);
                orderChanges.add(change);
            } else {
                OrderChangeWS change = OrderChangeBL.buildFromLine(updatedLine, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
                change.setQuantity(BigDecimal.ONE.subtract(updatedLine.getQuantityAsDecimal()));
                change.setAssetIds(new Integer[]{ASSET_1});
                orderChanges.add(change);
                updatedLineId = updatedLine.getId();
            }
        }

        System.out.println("Unassigning some assets");
        System.out.println("Updating order...");
        api.updateOrder(retOrder, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]));

        System.out.println("Getting latest");
        OrderWS lastOrder = api.getLatestOrder(PRANCING_PONY_USER_ID);
        assertNotNull("Didn't get any latest order", lastOrder);
        assertEquals("Latest id", orderId, lastOrder.getId());

        checkAssetStatus(api, ASSET_1, ASSET_STATUS_ORDER_SAVED, updatedLineId, PRANCING_PONY_USER_ID);
        checkAssetStatus(api, ASSET_2, ASSET_STATUS_DEFAULT, null, null);
        checkAssetStatus(api, ASSET_3, ASSET_STATUS_DEFAULT, null, null);

        // Delete
        System.out.println("Deleteing order " + orderId);
        api.deleteOrder(retOrder.getId());

        checkAssetStatus(api, ASSET_1, ASSET_STATUS_DEFAULT, null, null);

        api.deleteAsset(ASSET_3);
        api.deleteAsset(ASSET_2);
        api.deleteAsset(ASSET_1);
        api.deleteItem(firstItemId);

    }

    private OrderLineWS createOrderLineForAsset(Integer quantity, Integer... assetId) {
        OrderLineWS line;
        line = new OrderLineWS();
        line.setPrice(new BigDecimal("10.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(quantity);
        line.setAmount(new BigDecimal("10.00"));
        line.setDescription("Fist line");
        line.setItemId(1250);
        line.setAssetIds(assetId);
        return line;
    }

    @Test
    public void test021AssetValidationOnCreate() {
        // Create
        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        createUpdateAssetTest(newOrder, true, 21);
    }

    @Test
    public void test022AssetValidationOnUpdate() {

        ItemDTOEx dummyItem = createProduct(22, BigDecimal.ONE, "Dummy", false);
        Integer dummyItemId = api.createItem(dummyItem);

        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS dummyLine = createOrderLine(22, dummyItemId, "Test dummy item");
        newOrder.setOrderLines(new OrderLineWS[]{dummyLine});

        System.out.println("Creating order ... " + newOrder);
        Integer id = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        createUpdateAssetTest(api.getOrder(id), false, 22);

        api.deleteOrder(id);
        api.deleteItem(dummyItemId);
    }

    private void createUpdateAssetTest(OrderWS order, boolean create, Integer testNumber) {

        ItemDTOEx firstItem = createProduct(testNumber, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), true);
        Integer firstItemId = api.createItem(firstItem);

        String firstAssetIdentifier = "Asset"+testNumber+"-First".concat(String.valueOf(System.currentTimeMillis()));
        String secondAssetIdentifier = "Asset"+testNumber+"-Second".concat(String.valueOf(System.currentTimeMillis()));
        String thirdAssetIdentifier = "Asset"+testNumber+"-Third".concat(String.valueOf(System.currentTimeMillis()));
        Integer ASSET_1 = api.createAsset(getAssetWS(firstAssetIdentifier, ASSET_STATUS_DEFAULT, firstItemId));
        Integer ASSET_2 = api.createAsset(getAssetWS(secondAssetIdentifier, ASSET_STATUS_DEFAULT, firstItemId));
        Integer ASSET_3 = api.createAsset(getAssetWS(thirdAssetIdentifier, ASSET_STATUS_NOT_AVAILABLE, firstItemId));

        OrderLineWS line = buildOrderLine(firstItemId, Integer.valueOf(1), BigDecimal.TEN, ASSET_1);
        order.setOrderLines(new OrderLineWS[]{line});

        // Test wrong quantity

        System.out.println("Wrong quantity");
        try {
            line.setQuantity(2);
            if(create) {
                api.createOrder(order, OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            } else {
                OrderChangeWS orderChange = OrderChangeBL.buildFromLine(line, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
                api.updateOrder(order, new OrderChangeWS[]{orderChange});
            }
            fail("Wrong quantity");
        } catch (SessionInternalError error) {
//            error.printStackTrace();
//            JBillingTestUtils.assertContainsError(error, "OrderLineWS,assetIds,validation.assets.unequal.to.quantity");
        }

       // Test item without asset management

        System.out.println("Item without asset management");

        ItemDTOEx secondItem = createProduct(testNumber, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer secondItemId = api.createItem(secondItem);
        try {
            // Item without asset management and not a plan
            line.setItemId(secondItemId);
            line.setQuantity(1);
            if(create) {
                api.createOrder(order, OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            } else {
                OrderChangeWS orderChange = OrderChangeBL.buildFromLine(line, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
                api.updateOrder(order, new OrderChangeWS[]{orderChange});
            }
            fail("Item without asset management");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "OrderLineWS,assetIds,validation.assets.but.no.assetmanagement");
        }

        line.setItemId(firstItemId);

        //  order with asset not available

        System.out.println("Order with asset not available");
        line.setAssetIds(new Integer[] {ASSET_3});

        try {
            if(create) {
                api.createOrder(order, OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            } else {
                OrderChangeWS orderChange = OrderChangeBL.buildFromLine(line, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
                api.updateOrder(order, new OrderChangeWS[]{orderChange});
            }
            fail("Asset is unavailable");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "OrderLineWS,assetIds,validation.asset.status.unavailable");
        }

        api.deleteAsset(ASSET_3);
        api.deleteAsset(ASSET_2);
        api.deleteAsset(ASSET_1);
        api.deleteItem(secondItemId);
        api.deleteItem(firstItemId);

    }

    @Test
    public void test023MoveAssetBetweenLinesOnUpdate() {

        ItemDTOEx firstItem = createProduct(23, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), true);
        Integer firstItemId = api.createItem(firstItem);
        String firstAssetIdentifier = "Asset23-First".concat(String.valueOf(System.currentTimeMillis()));
        String secondAssetIdentifier = "Asset23-Second".concat(String.valueOf(System.currentTimeMillis()));
        Integer ASSET_1 = api.createAsset(getAssetWS(firstAssetIdentifier, ASSET_STATUS_DEFAULT, firstItemId));
        Integer ASSET_2 = api.createAsset(getAssetWS(secondAssetIdentifier, ASSET_STATUS_DEFAULT, firstItemId));

        OrderWS order = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);

        // now add some lines
        OrderLineWS line = buildOrderLine(firstItemId, Integer.valueOf(1), BigDecimal.TEN, ASSET_1);

        order.setOrderLines(new OrderLineWS[]{line});

        System.out.println("Create order");
        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        int assetTransitionCnt = api.getAssetTransitions(ASSET_1).length;

        order = api.getOrder(orderId);

        OrderLineWS[] lines = new OrderLineWS[2];
        line = order.getOrderLines()[0];
        line.setAssetIds(new Integer[]{ASSET_2});
        lines[0] = line;
        int lineId = lines[0].getId();
        OrderChangeWS change1 = OrderChangeBL.buildFromLine(line, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        line = buildOrderLine(firstItemId, Integer.valueOf(1), new BigDecimal("12.0"), ASSET_1);

        lines[1] = line;
        OrderChangeWS change2 = OrderChangeBL.buildFromLine(line, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        System.out.println("Moving asset between lines");
        order.setOrderLines(lines);
        api.updateOrder(order, new OrderChangeWS[]{change1, change2});
        order = api.getOrder(orderId);

        lines = order.getOrderLines();
        assertEquals(2, lines.length);
        for(OrderLineWS lineWS : lines){
            if(lineWS.getId() == lineId) {
                assertEquals(ASSET_2, lineWS.getAssetIds()[0]);
            } else {
                assertEquals(ASSET_1, lineWS.getAssetIds()[0]);
            }
        }

        AssetTransitionDTOEx[] transitionDTOExs = api.getAssetTransitions(ASSET_1);
        //The Asset_1 has 2 more transiction, one because it was freed from the line, and another one when was attached to the new Line
        assertEquals(assetTransitionCnt+2, transitionDTOExs.length);

        //cleanup
        api.deleteOrder(orderId);
        api.deleteAsset(ASSET_2);
        api.deleteAsset(ASSET_1);
        api.deleteItem(firstItemId);
    }

    @Test
    public void test024CreateAssetGroupWithAssetInOrder() {

        ItemDTOEx firstItem = createProduct(24, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), true);
        Integer firstItemId = api.createItem(firstItem);

        String firstAssetIdentifier = "Asset24-First".concat(String.valueOf(System.currentTimeMillis()));
        Integer ASSET_1 = api.createAsset(getAssetWS(firstAssetIdentifier, ASSET_STATUS_DEFAULT, firstItemId));

        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);

        // now add some lines
        OrderLineWS line = buildOrderLine(firstItemId, Integer.valueOf(1), BigDecimal.TEN, ASSET_1);

        newOrder.setOrderLines(new OrderLineWS[]{line});

        System.out.println("Creating order ... " + newOrder);

        // create order
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        System.out.println("Created order " + orderId);

        pause(5000);

        AssetWS asset = getAssetWS("Group1", ASSET_STATUS_DEFAULT, firstItemId);
        asset.setIdentifier("Group1");
        asset.setContainedAssetIds(new Integer[]{ASSET_1});
        try {
            api.createAsset(asset);
            fail("Exception expected");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "AssetWS,containedAssets,asset.validation.order.linked,"+firstAssetIdentifier);
        } finally {
            //cleanup
            api.deleteOrder(orderId);
            api.deleteAsset(ASSET_1);
            api.deleteItem(firstItemId);
        }

    }

    @Test
    public void test025CreateEditablePlanOrderStory() {


        // SMS Product
        ItemDTOEx smsItem = createProduct(20, new BigDecimal("0.1"), "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        // Persist
        Integer smsItemId = api.createItem(smsItem);

        // Create a plan item with sms product
        PlanItemWS smsPlanItem = createPlanItem(smsItemId, new BigDecimal("100"), ORDER_PERIOD_MONTHLY);
        // Use product price model
        smsPlanItem.addModel(Constants.EPOCH_DATE, smsItem.getDefaultPrice());

        // User Story 1
        /*
        Create Local Mobile Plan (monthly) which has the following products:
            - Local Call (metered rate = 1) (bundled quantity = 100, period = monthly)
         */

        // Create Local Call Item
        ItemDTOEx localCallItem = createProduct(20, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        // Persist
        Integer localCallItemId = api.createItem(localCallItem);

        // Add Local Call Item to a plan bundle
        PlanItemWS localCallBundle = createPlanItem(localCallItemId, new BigDecimal("100"), ORDER_PERIOD_MONTHLY);
        // Use product price model
        localCallBundle.addModel(CommonConstants.EPOCH_DATE, localCallItem.getDefaultPrice());

        // Create Local Mobile Plan
        PlanWS localMobilePlan = createPlan(20, "LMP", new BigDecimal("25"), Arrays.asList(localCallBundle), api);
        Integer localMobilePlanSubscriptionId = localMobilePlan.getPlanSubscriptionItemId();

        // Add sms plan item
        PlanWS planWS = api.getPlanWS(localMobilePlan.getId());
        planWS.addPlanItem(smsPlanItem);
        planWS.setEditable(1);

        api.updatePlan(planWS);

        PlanWS fetchedPlan = api.getPlanWS(localMobilePlan.getId());
        assertEquals(planWS.getEditable(), fetchedPlan.getEditable());
        assertEquals(planWS.getPlanItems().size(), fetchedPlan.getPlanItems().size());

        // User Story 2
        /*
        Create International Mobile Plan (monthly) which has the following products:
            - International Call (metered rate = 5) (bundled quantity = 10, period = monthly)
         */

        // Create Local Call Item
        ItemDTOEx internationalCallItem = createProduct(20, new BigDecimal("5"), "Product".concat(String.valueOf(System.currentTimeMillis())),   false);
        // Persist
        Integer internationalCallItemId = api.createItem(internationalCallItem);

        // Add International Call Item to a plan bundle
        PlanItemWS internationalCallBundle = createPlanItem(internationalCallItemId, new BigDecimal("10"), ORDER_PERIOD_MONTHLY);
        // Use product price model
        internationalCallBundle.addModel(CommonConstants.EPOCH_DATE, internationalCallItem.getDefaultPrice());

        // Create International Mobile Plan
        PlanWS internationalMobilePlan = createPlan(20, "IMP", new BigDecimal("50"), Arrays.asList(internationalCallBundle), api);
        Integer internationalMobilePlanSubscriptionId = internationalMobilePlan.getPlanSubscriptionItemId();


        PlanWS fetchedPlan2 = api.getPlanWS(internationalMobilePlan.getId());
        fetchedPlan2.addPlanItem(smsPlanItem);

        api.updatePlan(fetchedPlan2);

        fetchedPlan2 = api.getPlanWS(internationalMobilePlan.getId());

        System.out.println("Creating new Order");

        OrderWS newOrder = buildOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_POST_PAID, ORDER_PERIOD_MONTHLY);

        // now add some lines
        OrderLineWS[] lines = new OrderLineWS[fetchedPlan2.getPlanItems().size()];
        int indexCounter = 0;
        for(PlanItemWS tmpPlanItemWS: fetchedPlan2.getPlanItems()) {
            OrderLineWS orderLineWS = new OrderLineWS();
            ItemDTOEx tmpItem = api.getItem(tmpPlanItemWS.getItemId(), PRANCING_PONY_USER_ID, null);
            orderLineWS.setPrice(tmpItem.getPrice());
            orderLineWS.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
            orderLineWS.setQuantity(new BigDecimal(tmpPlanItemWS.getBundle() != null?tmpPlanItemWS.getBundle().getQuantity():"0"));
            orderLineWS.setAmount(tmpPlanItemWS.getModel().getRateAsDecimal());
            orderLineWS.setDescription(tmpItem.getDescription());
            orderLineWS.setItemId(tmpItem.getId());

            lines[indexCounter] = orderLineWS;
            indexCounter++;
        }

        newOrder.setOrderLines(lines);

        System.out.println("Creating order ... " + newOrder);
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("The order was not created ", orderId);

        OrderWS orderWS = api.getOrder(orderId);

        for(OrderLineWS tmpOrderLineWS: orderWS.getOrderLines()) {
            assertNotNull(tmpOrderLineWS.getPrice());
            assertNotNull(tmpOrderLineWS.getQuantity());
            assertNotNull(tmpOrderLineWS.getAmount());
            assertNotNull(tmpOrderLineWS.getDescription());
        }

        api.deleteOrder(orderId);

        // User story 3
        OrderWS newOrder3 = buildOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_POST_PAID, ORDER_PERIOD_MONTHLY);

//        PlanWS planWS3 = api.getPlanWS(4); // Why do we need this?? It is not used..

        // dummy values to be overwrite
        OrderLineWS orderLineWS3 = buildOrderLine(smsItemId, Integer.valueOf(1), new BigDecimal("30"));

        newOrder3.setOrderLines(new OrderLineWS[] {orderLineWS3});

        System.out.println("Creating order ... " + newOrder3);
        Integer orderId3 = api.createOrder(newOrder3, OrderChangeBL.buildFromOrder(newOrder3, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("The order was not created ", orderId3);

        OrderWS orderWS3 = api.getOrder(orderId3);
        for(OrderLineWS tmpOrderLineWS: orderWS3.getOrderLines()) {
            assertNotNull(tmpOrderLineWS.getPrice());
            assertNotNull(tmpOrderLineWS.getQuantity());
            assertNotNull(tmpOrderLineWS.getAmount());
            assertNotNull(tmpOrderLineWS.getDescription());
        }

        api.deleteOrder(orderId3);
        System.out.println("Order deleted: " + orderId3);

        api.deletePlan(internationalMobilePlan.getId());
        api.deleteItem(internationalMobilePlanSubscriptionId);
        api.deleteItem(internationalCallItemId);
        api.deletePlan(localMobilePlan.getId());
        api.deleteItem(localMobilePlanSubscriptionId);
        api.deleteItem(localCallItemId);
        api.deleteItem(smsItemId);
    }

    @Test
    public void test26ProductDependenciesValidation() {


        ItemDTOEx productB = createProduct(21, BigDecimal.ONE, "ProductB".concat(String.valueOf(System.currentTimeMillis())),   false);
        System.out.println("Creating item ..." + productB);
        Integer retB = api.createItem(productB);
        assertNotNull("Product B should be created", retB);

        ItemDTOEx productA = createProduct(21, BigDecimal.ONE, "ProductA".concat(String.valueOf(System.currentTimeMillis())),   false);
        setDependency(productA, retB, 2, 3);

        System.out.println("Creating item ..." + productA);
        Integer retA = api.createItem(productA);
        assertNotNull("Product A should be created", retA);

        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line1 = createOrderLine(21, retA, "A");
        lines[0] = line1;
        newOrder.setOrderLines(lines);

        System.out.println("Creating order ... " + newOrder);
        Integer retOrder1;
        try {
            retOrder1 = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", retOrder1));
        } catch (SessionInternalError ex) {
            assertEquals("Incorrect error", OrderHierarchyValidator.ERR_PRODUCT_MANDATORY_DEPENDENCY_NOT_MEET, ex.getErrorMessages()[0]);// do nothing
        }
        OrderLineWS line2 = createOrderLine(21, retB, "B");
        lines = new OrderLineWS[2];
        lines[0] = line1;
        lines[1] = line2;
        newOrder.setOrderLines(lines);

        System.out.println("Dependent lines not linked ... " + newOrder);
        try {
            retOrder1 = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", retOrder1));
        } catch (SessionInternalError ex) {
            assertEquals("Incorrect error", OrderHierarchyValidator.ERR_PRODUCT_MANDATORY_DEPENDENCY_NOT_MEET, ex.getErrorMessages()[0]);// do nothing
        }

        line2.setParentLine(line1);
        line1.setChildLines(new OrderLineWS[]{line2});
        System.out.println("Creating order ... " + newOrder);

        System.out.println("Min quantity not met ... " + newOrder);
        try {
            retOrder1 = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", retOrder1));
        } catch (SessionInternalError ex) {
            assertEquals("Incorrect error", OrderHierarchyValidator.ERR_PRODUCT_MANDATORY_DEPENDENCY_NOT_MEET, ex.getErrorMessages()[0]);// do nothing
        }

        line2.setQuantity(5);
        System.out.println("Quantitiy exceeded ... " + newOrder);
        try {
            retOrder1 = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", retOrder1));
        } catch (SessionInternalError ex) {
            assertEquals("Incorrect error", OrderHierarchyValidator.ERR_PRODUCT_MANDATORY_DEPENDENCY_NOT_MEET, ex.getErrorMessages()[0]);// do nothing
        }

        line2.setQuantity(2);
        retOrder1 = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Order should be created now", retOrder1);
        OrderWS order = api.getOrder(retOrder1);

        assertNotNull("Order should be presented", order);
        assertEquals("Order has incorrect order lines size", 2, order.getOrderLines().length);
        assertNotNull("Order line 2 should have line 1 as parent", order.getOrderLines()[1].getParentLine());
        assertEquals("Order line 1 should have line 2 as child", 1, order.getOrderLines()[0].getChildLines().length);
        assertEquals("Order line 1 should have line 2 as child", order.getOrderLines()[0].getChildLines()[0], order.getOrderLines()[1]);

        api.deleteOrder(retOrder1);

	    //TODO: this here removes the dependency by hand,
	    // but should be managed automatically by delete
	    ItemDTOEx itemA = api.getItem(retA, null, null);
	    itemA.setDependencies(null);
	    api.updateItem(itemA);

		//now delete the items
        api.deleteItem(retB);
        api.deleteItem(retA);
    }

    private void setDependency(ItemDTOEx product, Integer itemId, Integer min, Integer max) {
        ItemDependencyDTOEx dep1 = new ItemDependencyDTOEx();
        dep1.setDependentId(itemId);
        dep1.setMinimum(min);
        dep1.setMaximum(max);
        dep1.setType(ItemDependencyType.ITEM);
        ItemDependencyDTOEx[] deps = product.getDependencies();
        ItemDependencyDTOEx[] newDepts = new ItemDependencyDTOEx[deps == null ? 1 : (deps.length+1)];
        int idx = 0;
        if(deps != null) {
            for(ItemDependencyDTOEx d : deps) {
                newDepts[idx++] = d;
            }
        }
        newDepts[idx] = dep1;
        product.setDependencies(newDepts);
    }

    @Test
    public void test27ProductDependenciesWithSuborderValidation() {

        ItemDTOEx productB = createProduct(22, BigDecimal.ONE, "ProductB".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + productB);
        Integer retB = api.createItem(productB);
        assertNotNull("Product B should be created", retB);

        ItemDTOEx productA = createProduct(22, BigDecimal.ONE, "ProductA".concat(String.valueOf(System.currentTimeMillis())), false);
        setDependency(productA, retB, 1, null);
        System.out.println("Creating item ..." + productA);
        Integer retA = api.createItem(productA);
        assertNotNull("Product A should be created", retA);

        OrderWS parentOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line1 = createOrderLine(22, retA, "A");
        lines[0] = line1;
        parentOrder.setOrderLines(lines);

        OrderWS childOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line2 = createOrderLine(22, retB, "B");
        lines[0] = line2;
        childOrder.setOrderLines(lines);

        parentOrder.setChildOrders(new OrderWS[]{childOrder});
        childOrder.setParentOrder(parentOrder);

        System.out.println("Creating order ... " + parentOrder);
        Integer retOrder1 = null;
        try{
            retOrder1 = api.createOrder(parentOrder, OrderChangeBL.buildFromOrder(parentOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", retOrder1));
        } catch (SessionInternalError sie){
            System.out.println("Error creating order!\n" + sie.getMessage());
        }

        line2.setParentLine(line1);
        line1.setChildLines(new OrderLineWS[]{line2});
        System.out.println("Creating order ... " + parentOrder);
        retOrder1 = api.createOrder(parentOrder, OrderChangeBL.buildFromOrder(parentOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Order should be created now", retOrder1);
        OrderWS order = api.getOrder(retOrder1);

        assertNotNull("Order should be presented", order);
        assertEquals("Order has incorrect order lines size", 1, order.getOrderLines().length);
        assertEquals("Child order is not found", 1, order.getChildOrders().length);
        assertEquals("Order has incorrect order lines size", 1, order.getChildOrders()[0].getOrderLines().length);
        assertEquals("Incorrect parent order in child order", order, order.getChildOrders()[0].getParentOrder());
        assertNotNull("Order line 2 should have line 1 as parent", order.getChildOrders()[0].getOrderLines()[0].getParentLine());
        assertEquals("Order line 1 should have line 2 as child", 1, order.getOrderLines()[0].getChildLines().length);
        assertEquals("Order line 1 should have line 2 as child", order.getOrderLines()[0].getChildLines()[0], order.getChildOrders()[0].getOrderLines()[0]);

        // Remove product dependencies
        productA = api.getItem(retA, PRANCING_PONY_USER_ID, null);
        productA.setDependencies(null);
        api.updateItem(productA);

        // Remove order hierarchies
        childOrder = order.getChildOrders()[0];
        Integer childOrderId = childOrder.getId();
        childOrder.setParentOrder(null);
        childOrder.getOrderLines()[0].setParentLine(null);
        order.setChildOrders(null);
        order.getOrderLines()[0].setChildLines(null);

        api.updateOrder(order, OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        api.deleteOrder(childOrderId);
        api.deleteOrder(retOrder1);
        api.deleteItem(retA);
        api.deleteItem(retB);
    }

    @Test
    public void test28ProductDependenciesWithThreeLevelHierarchyValidation() {

        ItemDTOEx productB = createProduct(23, BigDecimal.ONE, "ProductB".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + productB);
        Integer retB = api.createItem(productB);
        assertNotNull("Product B should be created", retB);

        ItemDTOEx productA = createProduct(23, BigDecimal.ONE, "ProductA".concat(String.valueOf(System.currentTimeMillis())), false);
        setDependency(productA, retB, 1, null);
        System.out.println("Creating item ..." + productA);
        Integer retA = api.createItem(productA);
        assertNotNull("Product A should be created", retA);

        ItemDTOEx productC = createProduct(23, BigDecimal.ONE, "ProductC".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + productC);
        Integer retC = api.createItem(productC);
        assertNotNull("Product B should be created", retC);

        OrderWS parentOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line1 = createOrderLine(23, retA, "A");
        lines[0] = line1;
        parentOrder.setOrderLines(lines);

        OrderWS secondLevelOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS lineSecondLevel = createOrderLine(23, retC, "dummy");
        lines[0] = lineSecondLevel;
        secondLevelOrder.setOrderLines(lines);

        OrderWS childOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line2 = createOrderLine(23, retB, "B");
        lines[0] = line2;
        childOrder.setOrderLines(lines);

        parentOrder.setChildOrders(new OrderWS[]{secondLevelOrder});
        secondLevelOrder.setParentOrder(parentOrder);
        secondLevelOrder.setChildOrders(new OrderWS[]{childOrder});
        childOrder.setParentOrder(secondLevelOrder);

        System.out.println("Creating order ... " + parentOrder);
        Integer retOrder1;
        try{
            retOrder1 = api.createOrder(parentOrder, OrderChangeBL.buildFromOrder(parentOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", retOrder1));
        } catch (SessionInternalError sie){
            System.out.println("Error creating order!\n" + sie.getMessage());
        }

        line2.setParentLine(line1);
        line1.setChildLines(new OrderLineWS[]{line2});
        System.out.println("Creating order ... " + parentOrder);
        retOrder1 = api.createOrder(parentOrder, OrderChangeBL.buildFromOrder(parentOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Order should be created now", retOrder1);
        OrderWS order = api.getOrder(retOrder1);

        assertNotNull("Order should be presented", order);
        assertEquals("Order has incorrect order lines size", 1, order.getOrderLines().length);
        assertEquals("Second level order is not found", 1, order.getChildOrders().length);
        assertEquals("Child order is not found", 1, order.getChildOrders()[0].getChildOrders().length);

        // Clear product dependencies
        productA = api.getItem(retA, PRANCING_PONY_USER_ID, null);
        productA.setDependencies(null);
        api.updateItem(productA);

        // Remove order hierarchies
        secondLevelOrder = order.getChildOrders()[0];
        Integer secondLevelOrderId = secondLevelOrder.getId();
        childOrder = secondLevelOrder.getChildOrders()[0];

        // Child order
        childOrder.setParentOrder(null);
        Integer childOrderId = childOrder.getId();
        childOrder.getOrderLines()[0].setParentLine(null);

        // Second level order
        secondLevelOrder.setChildOrders(null);
        secondLevelOrder.getOrderLines()[0].setChildLines(null);

        // Parent order
        order.setChildOrders(null);
        order.getOrderLines()[0].setChildLines(null);

        api.deleteOrder(childOrderId);
        api.deleteOrder(secondLevelOrderId);
        api.deleteOrder(retOrder1);
        api.deleteItem(retC);
        api.deleteItem(retA);
        api.deleteItem(retB);

    }

    @Test
    public void test29ProductDependenciesInverseHierarchyValidation() {

        ItemDTOEx firstItem = createProduct(24, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + firstItem);
        Integer firstItemId = api.createItem(firstItem);
        assertNotNull("First Product should be created", firstItemId);

        ItemDTOEx secondItem = createProduct(24, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        setDependency(secondItem, firstItemId, 1, null);
        System.out.println("Creating item ..." + secondItem);
        Integer secondItemId = api.createItem(secondItem);
        assertNotNull("Second Product should be created", secondItemId);

        OrderWS orderA = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line1 = buildOrderLine(secondItemId, Integer.valueOf(1), null);
        lines[0] = line1;
        orderA.setOrderLines(lines);

        OrderWS orderB = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line2 = buildOrderLine(firstItemId, Integer.valueOf(1), null);
        lines[0] = line2;
        orderB.setOrderLines(lines);

        orderB.setChildOrders(new OrderWS[]{orderA});
        orderA.setParentOrder(orderB);

        System.out.println("Creating order ... " + orderA);
        Integer retOrder1;
        try{
            retOrder1 = api.createOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", retOrder1));
        } catch (SessionInternalError sie){
            System.out.println("Error creating order!!\n" + sie.getMessage());
        }

        line2.setParentLine(line1);
        line1.setChildLines(new OrderLineWS[]{line2});
        System.out.println("Creating order ... " + orderA);
        try{
            retOrder1 = api.createOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", retOrder1));
        } catch (SessionInternalError sie){
            System.out.println("Error creating order!!\n" + sie.getMessage());
        } finally {
	        //TODO: this here removes the dependency by hand,
	        // but should be managed automatically by delete
			secondItem = api.getItem(secondItemId, null, null);
	        secondItem.setDependencies(null);
	        api.updateItem(secondItem);

			//delete items
            api.deleteItem(secondItemId);
            api.deleteItem(firstItemId);
        }
    }

    @Test
    public void test30OrderCycleValidation() {

        ItemDTOEx firstItem = createProduct(25, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + firstItem);
        Integer firstItemId = api.createItem(firstItem);
        assertNotNull("First Product should be created", firstItemId);

        ItemDTOEx secondItem = createProduct(25, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + secondItem);
        Integer secondItemId = api.createItem(secondItem);
        assertNotNull("Second Product should be created", secondItemId);

        OrderWS orderA = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line1 = buildOrderLine(secondItemId, Integer.valueOf(1), null);
        lines[0] = line1;
        orderA.setOrderLines(lines);

        OrderWS orderB = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line2 = buildOrderLine(firstItemId, Integer.valueOf(1), null);
        lines[0] = line2;
        orderB.setOrderLines(lines);

        orderB.setChildOrders(new OrderWS[]{orderA});
        orderA.setParentOrder(orderB);
        orderA.setChildOrders(new OrderWS[]{orderB});
        orderB.setParentOrder(orderA);

        System.out.println("Creating order ... " + orderA);
        Integer retOrder1;
        try{
            retOrder1 = api.createOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", retOrder1));
        } catch (SessionInternalError sie){
            System.out.println("Error creating order!!\n" + sie.getMessage());
        } finally {
            api.deleteItem(secondItemId);
            api.deleteItem(firstItemId);
        }
    }

    @Test
    public void test31HierarchyActiveSinceValidation() {

        ItemDTOEx firstItem = createProduct(26, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + firstItem);
        Integer firstItemId = api.createItem(firstItem);
        assertNotNull("First Product should be created", firstItemId);

        OrderWS orderA = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line1 = buildOrderLine(firstItemId, Integer.valueOf(1), null);
        lines[0] = line1;
        orderA.setOrderLines(lines);
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, 2008);
        cal.set(Calendar.MONTH, Calendar.MAY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        orderA.setActiveSince(cal.getTime());

        OrderWS orderB = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line2 = buildOrderLine(firstItemId, Integer.valueOf(1), null);
        lines[0] = line2;
        orderB.setOrderLines(lines);
        cal.set(Calendar.MONTH, Calendar.APRIL);
        orderB.setActiveSince(cal.getTime());

        orderA.setChildOrders(new OrderWS[]{orderB});
        orderB.setParentOrder(orderA);

        System.out.println("Creating order ... " + orderA);
        Integer retOrder1;
        try{
            retOrder1 = api.createOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", retOrder1));
        } catch (SessionInternalError sie){
            System.out.println("Error creating order!\n" + sie.getMessage());
        } finally {
            api.deleteItem(firstItemId);
        }
    }

    @Test
    public void test32HierarchyActiveUntilValidation() {

        ItemDTOEx firstItem = createProduct(27, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + firstItem);
        Integer firstItemId = api.createItem(firstItem);
        assertNotNull("First Product should be created", firstItemId);

        OrderWS orderA = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line1 = buildOrderLine(firstItemId, Integer.valueOf(1), null);
        lines[0] = line1;
        orderA.setOrderLines(lines);
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, 2008);
        cal.set(Calendar.MONTH, Calendar.MAY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        orderA.setActiveSince(cal.getTime());
        cal.set(Calendar.MONTH, Calendar.JUNE);
        orderA.setActiveUntil(cal.getTime());

        OrderWS orderB = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line2 = buildOrderLine(firstItemId, Integer.valueOf(1), null);
        lines[0] = line2;
        orderB.setOrderLines(lines);
        cal.set(Calendar.MONTH, Calendar.MAY);
        orderB.setActiveSince(cal.getTime());
        cal.set(Calendar.MONTH, Calendar.AUGUST);
        orderB.setActiveUntil(cal.getTime());

        orderA.setChildOrders(new OrderWS[]{orderB});
        orderB.setParentOrder(orderA);

        System.out.println("Creating order ... " + orderA);
        Integer retOrder1;
        try{
            retOrder1 = api.createOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", retOrder1));
        } catch (SessionInternalError sie){
            System.out.println("Error creating order!!!\n" + sie.getMessage());
        } finally {
            api.deleteItem(firstItemId);
        }
    }

    @Test
    public void test33HierarchyObjectsEquality() {
        JbillingAPI[] apiClients = new JbillingAPI[] {api, mordorApi};
        for (JbillingAPI clientApi : apiClients) {

            final Integer PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeApplyStatus(clientApi);
            Integer userId = clientApi.getCallerCompanyId().equals(PRANCING_PONY_ENTITY_ID) ? PRANCING_PONY_USER_ID : MORDOR_USER_ID;

            ItemDTOEx firstItem = createProduct(28, BigDecimal.ONE, "Product1",  false);
            firstItem.setEntityId(clientApi.getCallerCompanyId());
            firstItem.setEntities(Arrays.asList(clientApi.getCallerCompanyId()));
            System.out.println("Creating item ..." + firstItem);
            Integer firstItemId = clientApi.createItem(firstItem);
            assertNotNull("First Product should be created", firstItemId);

            OrderWS orderA = buildOneTimePostPaidOrder(userId);
            OrderLineWS lines[] = new OrderLineWS[1];
            OrderLineWS line1 = buildOrderLine(firstItemId, Integer.valueOf(1), BigDecimal.ONE);
            lines[0] = line1;
            orderA.setOrderLines(lines);

            OrderWS orderB = buildOneTimePostPaidOrder(userId);
            lines = new OrderLineWS[1];
            OrderLineWS line2 = buildOrderLine(firstItemId, Integer.valueOf(1), BigDecimal.ONE);
            lines[0] = line2;
            orderB.setOrderLines(lines);

            OrderWS orderC = buildOneTimePostPaidOrder(userId);
            lines = new OrderLineWS[3];
            String line3Description = firstItem.getDescription()+"3rd Order, first line";
            OrderLineWS line3 = buildOrderLine(firstItemId, Integer.valueOf(1), BigDecimal.ONE);
            line3.setDescription(line3Description);
            lines[0] = line3;
            String line4Description = firstItem.getDescription()+"3rd Order, 2nd line";
            OrderLineWS line4 = buildOrderLine(firstItemId, Integer.valueOf(1), BigDecimal.ONE);
            line4.setDescription(line4Description);
            lines[1] = line4;
            String line5Description = firstItem.getDescription()+"3rd Order, third line";
            OrderLineWS line5 = buildOrderLine(firstItemId, Integer.valueOf(1), null);
            line5.setDescription(line5Description);
            lines[2] = line5;
            orderC.setOrderLines(lines);

            line1.setChildLines(new OrderLineWS[]{line2, line4});
            line2.setParentLine(line1);
            line4.setParentLine(line1);

            line2.setChildLines(new OrderLineWS[]{line3});
            line3.setParentLine(line2);

            line3.setChildLines(new OrderLineWS[]{line5});
            line5.setParentLine(line3);

            orderA.setChildOrders(new OrderWS[]{orderB});
            orderB.setParentOrder(orderA);
            orderB.setChildOrders(new OrderWS[]{orderC});
            orderC.setParentOrder(orderB);

            System.out.println("Creating order ... " + orderA);

            Integer retOrder1 = clientApi.createOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            assertNotNull("Orders should be created", retOrder1);

            orderA = clientApi.getOrder(retOrder1);
            assertNotNull("Orders should be created", retOrder1);
            assertEquals("Second level order is not found", 1, orderA.getChildOrders().length);
            orderB = orderA.getChildOrders()[0];
            assertEquals("Third level order is not found", 1, orderB.getChildOrders().length);
            orderC = orderB.getChildOrders()[0];
            line1 = orderA.getOrderLines()[0];
            line2 = orderB.getOrderLines()[0];
            line3 = findOrderLineWithDescription(orderC.getOrderLines(), line3Description);
            line4 = findOrderLineWithDescription(orderC.getOrderLines(), line4Description);
            line5 = findOrderLineWithDescription(orderC.getOrderLines(), line5Description);

            assertEquals(" Incorrect objects equality: orders", orderA, orderB.getParentOrder());
            assertEquals(" Incorrect objects equality: orders", orderB, orderC.getParentOrder());

            assertEquals(" Incorrect objects equality: order lines", line1, line2.getParentLine());
            assertEquals(" Incorrect objects equality: order lines", line1, line4.getParentLine());
            assertEquals(" Incorrect objects equality: order lines", line2, line3.getParentLine());
            assertEquals(" Incorrect objects equality: order lines", line3, line5.getParentLine());

            //todo: fix
//            orderC = service.getOrder(orderC.getId());
//            assertNotNull("Order from middle of hierarchy should be retrieved", orderC);

            // Remove order hierarchies
            orderB = orderA.getChildOrders()[0];
            Integer orderBId = orderB.getId();
            orderC = orderB.getChildOrders()[0];

            // Order C
            orderC.setParentOrder(null);
            Integer orderCId = orderC.getId();
            orderC.getOrderLines()[0].setParentLine(null);
            orderC.getOrderLines()[1].setParentLine(null);
            orderC.getOrderLines()[2].setParentLine(null);

            // Order B
            orderB.setChildOrders(null);
            orderB.getOrderLines()[0].setChildLines(null);

            // Order A
            orderA.setChildOrders(null);
            orderA.getOrderLines()[0].setChildLines(null);

            clientApi.updateOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            clientApi.updateOrder(orderB, OrderChangeBL.buildFromOrder(orderB, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            clientApi.updateOrder(orderC, OrderChangeBL.buildFromOrder(orderC, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

            clientApi.deleteOrder(orderCId);
            clientApi.deleteOrder(orderBId);
            clientApi.deleteOrder(retOrder1);
            clientApi.deleteItem(firstItemId);
        }
    }

    @Test
    public void test034OrderDeleteFromHierarchy() {

        ItemDTOEx firstItem = createProduct(29, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + firstItem);
        Integer firstItemId = api.createItem(firstItem);
        assertNotNull("First Product should be created", firstItemId);

        ItemDTOEx secondItem = createProduct(25,BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + secondItem);
        Integer secondItemId = api.createItem(secondItem);
        assertNotNull("Second Product should be created", secondItemId);

        ItemDTOEx thirdItem = createProduct(25, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + thirdItem);
        Integer thirdItemId = api.createItem(thirdItem);
        assertNotNull("Third Product should be created", thirdItemId);

        OrderWS orderA = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line1 = buildOrderLine(secondItemId, Integer.valueOf(1), null);
        lines[0] = line1;
        orderA.setOrderLines(lines);

        OrderWS orderB = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line2 = buildOrderLine(firstItemId, Integer.valueOf(1), null);
        lines[0] = line2;
        orderB.setOrderLines(lines);

        OrderWS orderC = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line3 = buildOrderLine(thirdItemId, Integer.valueOf(1), null);
        lines[0] = line3;
        orderC.setOrderLines(lines);

        orderA.setChildOrders(new OrderWS[]{orderB, orderC});
        orderB.setParentOrder(orderA);
        orderC.setParentOrder(orderA);
        line2.setParentLine(line1);
        line1.setChildLines(new OrderLineWS[]{line2});

        System.out.println("Creating order ... " + orderA);
        Integer retOrder1 = api.createOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Orders should be created", retOrder1);

        OrderWS order1 = api.getOrder(retOrder1);
        assertEquals("Incorrect count of Order A child orders", 2, order1.getChildOrders().length);

        Integer orderBId = order1.getOrderLines()[0].getChildLines()[0].getOrderId();
        Integer orderCId = order1.getChildOrders()[0].getId();
        if (orderCId.equals(orderBId)) {
            orderCId = order1.getChildOrders()[1].getId();
        }

        try {
            api.deleteOrder(orderBId);
        } catch (SessionInternalError ex) {
            assertEquals("Incorrect error", OrderHierarchyValidator.ERR_PRODUCT_MANDATORY_DEPENDENCY_NOT_MEET, ex.getErrorMessages()[0]);// do nothing
        }

        api.deleteOrder(orderCId);

        order1 = api.getOrder(retOrder1);
        OrderWS orderCWS = order1.getChildOrders()[0];
        if (orderCWS != null && !orderCWS.getId().equals(orderCId)) {
            orderCWS = order1.getChildOrders()[1];
        }
        assertTrue("Order C should be deleted", orderCWS.getDeleted() == 1);

        api.deleteOrder(orderBId);

        order1 = api.getOrder(retOrder1);
        OrderWS orderBWS = order1.getChildOrders()[0];
        if (orderBWS != null && !orderBWS.getId().equals(orderBId)) {
            orderBWS = order1.getChildOrders()[1];
        }
        assertTrue("Order B should be deleted", orderBWS.getDeleted() == 1);

        api.deleteOrder(retOrder1);

        api.deleteItem(thirdItemId);
        api.deleteItem(secondItemId);
        api.deleteItem(firstItemId);

    }
	
    @Test
    public void test035UpdateOrderHierarchy() {

        ItemDTOEx secondItem = createProduct(25, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + secondItem);
        Integer secondItemId = api.createItem(secondItem);
        assertNotNull("Second Product should be created", secondItemId);

        ItemDTOEx firstItem = createProduct(29, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + firstItem);
        Integer firstItemId = api.createItem(firstItem);
        assertNotNull("First Product should be created", firstItemId);

        ItemDTOEx thirdItem = createProduct(25, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + thirdItem);
        Integer thirdItemId = api.createItem(thirdItem);
        assertNotNull("Third Product should be created", thirdItemId);

        OrderWS orderA = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line1 = buildOrderLine(firstItemId, Integer.valueOf(1), null);
        lines[0] = line1;
        orderA.setOrderLines(lines);

        OrderWS orderB = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line2 = buildOrderLine(secondItemId, Integer.valueOf(1), null);
        lines[0] = line2;
        orderB.setOrderLines(lines);

        OrderWS orderC = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line3 = buildOrderLine(thirdItemId, Integer.valueOf(1), null);
        lines[0] = line3;
        orderC.setOrderLines(lines);

        orderA.setChildOrders(new OrderWS[]{orderB, orderC});
        orderB.setParentOrder(orderA);
        orderC.setParentOrder(orderA);
        line2.setParentLine(line1);
        line1.setChildLines(new OrderLineWS[]{line2});


        System.out.println("Creating order ... " + orderA);
        Integer retOrder1 = api.createOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Orders should be created", retOrder1);

        orderA = api.getOrder(retOrder1);
        assertEquals("Incorrect count of Order A child orders", 2, orderA.getChildOrders().length);

        Integer orderBId = orderA.getOrderLines()[0].getChildLines()[0].getOrderId();
        orderC = orderA.getChildOrders()[0];
        if (orderC.getId().equals(orderBId)) {
            orderB = orderA.getChildOrders()[0];
            orderC = orderA.getChildOrders()[1];
        } else {
            orderB = orderA.getChildOrders()[1];
        }
        // change orders hierarchy first
        orderA.setChildOrders(new OrderWS[]{orderB});
        orderC.setParentOrder(orderB);
        orderB.setChildOrders(new OrderWS[]{orderC});
        api.updateOrder(orderC, null);
        orderA = api.getOrder(retOrder1);
        assertEquals("Incorrect hierarchy for root order", 1, orderA.getChildOrders().length);
        orderB = orderA.getChildOrders()[0];
        assertEquals("Incorrect hierarchy for second level order", 1, orderB.getChildOrders().length);
        orderC = orderB.getChildOrders()[0];
        assertEquals("Incorrect parent for second level order", orderA, orderB.getParentOrder());
        assertEquals("Incorrect parent for third level order", orderB, orderC.getParentOrder());

        // change order lines hierarchy and save single order
        line3 = orderC.getOrderLines()[0];
        line3.setParentLine(orderB.getOrderLines()[0]);
        orderC.setParentOrder(null);
        orderC.setNotes("Updated notes for order 30 C");
        OrderChangeWS orderChange = OrderChangeBL.buildFromLine(line3, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        api.updateOrder(orderC, new OrderChangeWS[]{orderChange});
        orderA = api.getOrder(retOrder1);
        assertEquals("Incorrect hierarchy for root order", 1, orderA.getChildOrders().length);
        orderB = orderA.getChildOrders()[0];
        assertEquals("Incorrect hierarchy for second level order", 1, orderB.getChildOrders().length);
        orderC = orderB.getChildOrders()[0];
        assertEquals("Incorrect parent for second level order", orderA, orderB.getParentOrder());
        assertEquals("Incorrect parent for third level order", orderB, orderC.getParentOrder());
        // validate order lines hierarchy
        line1 = orderA.getOrderLines()[0];
        line2 = orderB.getOrderLines()[0];
        line3 = orderC.getOrderLines()[0];
        assertEquals("Child lines was not updated for root order lines", 1, line1.getChildLines().length);
        assertEquals("Incorrect root order line child", line2, line1.getChildLines()[0]);
        assertEquals("Incorrect parent line for second order line", line1, line2.getParentLine());
        assertEquals("Incorrect child line for second order line", line3, line2.getChildLines()[0]);
        assertEquals("Incorrect parent line for third order line", line2, line3.getParentLine());
        // validate updated field
        assertEquals("Order field was not updated", "Updated notes for order 30 C", orderC.getNotes());

        // try to break product dependencies
        line2.setItemId(thirdItemId);
        orderChange = OrderChangeBL.buildFromLine(line2, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        api.updateOrder(orderA, new OrderChangeWS[]{orderChange});
        orderA = api.getOrder(retOrder1);
        // try to change product C to product B in line 3 - this is possible
        orderB = orderA.getChildOrders()[0];
        orderC = orderB.getChildOrders()[0];
        line3 = orderC.getOrderLines()[0];
        line3.setItemId(secondItemId);
        orderA.setNotes("Updated notes order 30 A");
        orderB.setNotes("Updated notes order 30 B");
        orderChange = OrderChangeBL.buildFromLine(line3, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        api.updateOrder(orderA, new OrderChangeWS[]{orderChange});
        orderA = api.getOrder(retOrder1);
        orderB = orderA.getChildOrders()[0];
        orderC = orderB.getChildOrders()[0];
        line1 = orderA.getOrderLines()[0];
        line2 = orderB.getOrderLines()[0];
        line3 = orderC.getOrderLines()[0];
        // change hierarchy root, but first with cycle in hierarchy
        orderA.setParentOrder(orderB);
        orderB.setChildOrders(new OrderWS[]{orderA});

        orderC.setParentOrder(orderA);
        orderA.setChildOrders(new OrderWS[]{orderC});

        line2.setParentLine(null);
        line2.setChildLines(new OrderLineWS[0]);
        line1.setParentLine(null);
        line1.setChildLines(new OrderLineWS[]{line3});
        line3.setParentLine(line1);
        line3.setChildLines(new OrderLineWS[0]);
        try{
            api.updateOrder(orderA, new OrderChangeWS[]{
                    OrderChangeBL.buildFromLine(line1, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID),
                    OrderChangeBL.buildFromLine(line2, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID),
                    OrderChangeBL.buildFromLine(line3, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID)
            });
        } catch (SessionInternalError sie){
            assertEquals("Incorrect error", OrderHierarchyValidator.ERR_CYCLES_IN_HIERARCHY, sie.getErrorMessages()[0]);// do nothing
        }
        // remove cycle
        orderB.setParentOrder(null);
        api.updateOrder(orderA, new OrderChangeWS[]{
                    OrderChangeBL.buildFromLine(line1, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID),
                    OrderChangeBL.buildFromLine(line2, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID),
                    OrderChangeBL.buildFromLine(line3, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID)
        });
        orderA = api.getOrder(retOrder1);
        orderB = orderA.getParentOrder();
        orderC = orderA.getChildOrders()[0];
        assertNotNull("Root should be changed", orderB);
        assertEquals("Incorrect parent order for order 1", orderBId, orderB.getId());
        assertNull("Incorrect parent order line for order B", orderB.getOrderLines()[0].getParentLine());
        assertEquals("Incorrect parent order line for order C", orderA.getOrderLines()[0], orderC.getOrderLines()[0].getParentLine());

        // Clear order hierarchies
        // Order C
        Integer orderCId = orderC.getId();
        clearOrderHierarchy(orderC);

        // Order A
        clearOrderHierarchy(orderA);

        // Order B
        orderBId = orderB.getId();
        clearOrderHierarchy(orderB);

        api.updateOrder(orderB, OrderChangeBL.buildFromOrder(orderB, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        api.updateOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        api.updateOrder(orderC, OrderChangeBL.buildFromOrder(orderC, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        api.deleteOrder(orderCId);
        api.deleteOrder(retOrder1);
        api.deleteOrder(orderBId);

        api.deleteItem(secondItemId);
        api.deleteItem(firstItemId);
        api.deleteItem(thirdItemId);
    }

    @Test
    public void test036CreateUpdateOrder() {

        ItemDTOEx secondItem = createProduct(31, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + secondItem);
        Integer secondItemId = api.createItem(secondItem);
        assertNotNull("Second Product should be created", secondItemId);

        ItemDTOEx firstItem = createProduct(31, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + firstItem);
        Integer firstItemId = api.createItem(firstItem);
        assertNotNull("First Product should be created", firstItemId);

        ItemDTOEx thirdItem = createProduct(31, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + thirdItem);
        Integer thirdItemId = api.createItem(thirdItem);
        assertNotNull("Third Product should be created", thirdItemId);

        OrderWS orderA = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line1 = buildOrderLine(firstItemId, Integer.valueOf(1), null);
        lines[0] = line1;
        orderA.setOrderLines(lines);

        OrderWS orderB = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line2 = buildOrderLine(secondItemId, Integer.valueOf(1), null);
        lines[0] = line2;
        orderB.setOrderLines(lines);

        OrderWS orderC = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line3 = buildOrderLine(thirdItemId, Integer.valueOf(1), null);
        lines[0] = line3;
        orderC.setOrderLines(lines);

        orderA.setChildOrders(new OrderWS[]{orderB, orderC});
        orderB.setParentOrder(orderA);
        orderC.setParentOrder(orderA);
        line2.setParentLine(line1);
        line1.setChildLines(new OrderLineWS[]{line2});
        OrderChangeWS change1 = OrderChangeBL.buildFromLine(line1, orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        OrderChangeWS change2 = OrderChangeBL.buildFromLine(line2, orderB, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        OrderChangeWS change3 = OrderChangeBL.buildFromLine(line3, orderC, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        change2.setParentOrderChange(change1);
        change3.setParentOrderChange(change1);

        System.out.println("Creating order ... " + orderA);
        Integer retOrder1 = api.createUpdateOrder(orderA, new OrderChangeWS[]{change1, change2, change3});
        assertNotNull("Orders should be created", retOrder1);

        orderA = api.getOrder(retOrder1);
        assertEquals("Incorrect count of Order A child orders", 2, orderA.getChildOrders().length);

        orderC = orderA.getChildOrders()[0];
        if (orderC.getNotes().contains("2")) {
            orderB = orderA.getChildOrders()[0];
            orderC = orderA.getChildOrders()[1];
        } else {
            orderB = orderA.getChildOrders()[1];
        }
        // change orders hierarchy and fields
        orderA.setChildOrders(new OrderWS[]{orderB});
        orderC.setParentOrder(orderB);
        orderB.setChildOrders(new OrderWS[]{orderC});
        orderA.setNotes("Updated notes order 31 A");
        api.createUpdateOrder(orderC, new OrderChangeWS[0]);
        orderA = api.getOrder(retOrder1);
        assertEquals("Incorrect hierarchy for root order", 1, orderA.getChildOrders().length);
        orderB = orderA.getChildOrders()[0];
        assertEquals("Incorrect hierarchy for second level order", 1, orderB.getChildOrders().length);
        orderC = orderB.getChildOrders()[0];
        assertEquals("Incorrect parent for second level order", orderA, orderB.getParentOrder());
        assertEquals("Incorrect parent for third level order", orderB, orderC.getParentOrder());
        assertEquals("Order fields was not updated", "Updated notes order 31 A", orderA.getNotes());
        // delete order C from hierarchy
        orderB.setChildOrders(new OrderWS[0]);
        api.createUpdateOrder(orderA,  new OrderChangeWS[0]);
        orderA = api.getOrder(retOrder1);
        assertEquals("Incorrect hierarchy for root order", 1, orderA.getChildOrders().length);
        orderB = orderA.getChildOrders()[0];
        assertEquals("Incorrect hierarchy for second level order", 1, orderB.getChildOrders().length);
        assertTrue("Order C should be deleted", orderB.getChildOrders()[0].getDeleted() > 0);
        Integer orderCId = orderB.getChildOrders()[0].getId();
        // add new order 'C1'
        orderC.setId(null);
        orderC.setNotes("Order 31 C1");
        orderB.setChildOrders(new OrderWS[]{orderC});
        orderC.setParentOrder(orderB);
        orderC.getOrderLines()[0].setId(0);
        orderC.getOrderLines()[0].setOrderId(null);
        OrderChangeWS orderChange = OrderChangeBL.buildFromLine(orderC.getOrderLines()[0], orderC, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        api.createUpdateOrder(orderA, new OrderChangeWS[]{orderChange});
        orderA = api.getOrder(retOrder1);
        assertEquals("Incorrect hierarchy for root order", 1, orderA.getChildOrders().length);
        orderB = orderA.getChildOrders()[0];
        assertEquals("Incorrect hierarchy for second level order", 2, orderB.getChildOrders().length);
        orderC = orderB.getChildOrders()[0];
        if (orderC.getDeleted() > 0) {
            orderC = orderB.getChildOrders()[1];
        }
        Integer orderC1Id = orderC.getId();
        assertEquals("Incorrect order C1 notes", "Order 31 C1", orderC.getNotes());
        assertEquals("Incorrect lines count for order C1", 1, orderC.getOrderLines().length);
        // change product for order B - dependency of A should be not meet
        orderB.getOrderLines()[0].setItemId(thirdItemId);
        orderB.setChildOrders(new OrderWS[]{orderC});
        orderChange = OrderChangeBL.buildFromLine(orderB.getOrderLines()[0], null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        try {
            api.createUpdateOrder(orderB, new OrderChangeWS[]{orderChange});
        } catch (SessionInternalError ex) {
            assertEquals("Incorrect error", OrderHierarchyValidator.ERR_PRODUCT_MANDATORY_DEPENDENCY_NOT_MEET, ex.getErrorMessages()[0]);
        } finally {

            clearOrderHierarchy(orderC);
            clearOrderHierarchy(orderB);
            clearOrderHierarchy(orderA);

            api.updateOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

            api.deleteOrder(orderC1Id);
            api.deleteOrder(orderCId);
            api.deleteOrder(orderB.getId());
            api.deleteOrder(retOrder1);

            api.deleteItem(thirdItemId);
            api.deleteItem(secondItemId);
            api.deleteItem(firstItemId);
        }
    }

    @Test
    public void test037CreateUpdateOrder() {

        ItemDTOEx secondItem = createProduct(32, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + secondItem);
        Integer secondItemId = api.createItem(secondItem);
        assertNotNull("Second Product should be created", secondItemId);

        ItemDTOEx firstItem = createProduct(32, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + firstItem);
        Integer firstItemId = api.createItem(firstItem);
        assertNotNull("First Product should be created", firstItemId);

        ItemDTOEx thirdItem = createProduct(32, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + thirdItem);
        Integer thirdItemId = api.createItem(thirdItem);
        assertNotNull("Third Product should be created", thirdItemId);

        OrderWS orderA = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line1 = buildOrderLine(firstItemId, Integer.valueOf(1), null);
        lines[0] = line1;
        orderA.setOrderLines(lines);

        OrderWS orderB = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        lines = new OrderLineWS[1];
        OrderLineWS line2 = buildOrderLine(secondItemId, Integer.valueOf(1), null);
        lines[0] = line2;
        orderB.setOrderLines(lines);

        orderA.setChildOrders(new OrderWS[]{orderB});
        orderB.setParentOrder(orderA);
        line2.setParentLine(line1);
        line1.setChildLines(new OrderLineWS[]{line2});

        System.out.println("Creating order ... " + orderA);
        OrderChangeWS change1 = OrderChangeBL.buildFromLine(line1, orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        OrderChangeWS change2 = OrderChangeBL.buildFromLine(line2, orderB, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        change2.setParentOrderChange(change1);

        Integer retOrder1 = api.createUpdateOrder(orderA, new OrderChangeWS[]{change1, change2});
        assertNotNull("Orders should be created", retOrder1);

        orderA = api.getOrder(retOrder1);

        orderB = orderA.getChildOrders()[0];
        line1 = orderA.getOrderLines()[0];
        line2 = orderB.getOrderLines()[0];

        OrderLineWS line3 = buildOrderLine(thirdItemId, Integer.valueOf(1), null);
        line2.setDeleted(1);
        lines = new OrderLineWS[2];
        lines[0] = line2;
        lines[1] = line3;
        orderB.setOrderLines(lines);
        change1 = OrderChangeBL.buildFromLine(line2, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        change2 = OrderChangeBL.buildFromLine(line3, orderB, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        OrderWS orderC = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS line4 = buildOrderLine(secondItemId, Integer.valueOf(1), null);
        lines = new OrderLineWS[1];
        lines[0] = line4;
        orderC.setOrderLines(lines);

        orderC.setParentOrder(orderA);
        orderA.setChildOrders(new OrderWS[]{orderB, orderC});
        line1.setChildLines(new OrderLineWS[]{line2, line4});
        line4.setParentLine(line1);
        OrderChangeWS change3 = OrderChangeBL.buildFromLine(line4, orderC, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        api.createUpdateOrder(orderA, new OrderChangeWS[]{change1, change2, change3});
        orderA = api.getOrder(retOrder1);
        assertEquals("Incorrect children count for orderA", 2, orderA.getChildOrders().length);
        for (OrderWS order : orderA.getChildOrders()) {
            if (order.getId().equals(orderB.getId())) {
                orderB = order;
            } else {
                orderC = order;
            }
        }
        assertEquals("Incorrect lines count for OrderB", 1, orderB.getOrderLines().length);
        assertEquals("Incorrect parent order for orderB", orderA.getId(), orderB.getParentOrder().getId());
        assertEquals("Incorrect lines count for OrderC", 1, orderC.getOrderLines().length);
        assertEquals("Incorrect parent order for OrderC", orderA.getId(), orderC.getParentOrder().getId());

        clearOrderHierarchy(orderC);
        clearOrderHierarchy(orderB);
        clearOrderHierarchy(orderA);

        api.updateOrder(orderA, OrderChangeBL.buildFromOrder(orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        api.deleteOrder(orderC.getId());
        api.deleteOrder(orderB.getId());
        api.deleteOrder(orderA.getId());

        api.deleteItem(thirdItemId);
        api.deleteItem(secondItemId);
        api.deleteItem(firstItemId);
    }

    @Test
	public void test038CreateChildOrderInvoiceAutoCreateOrderForParent() {


        ItemDTOEx newItem = new ItemDTOEx();
        newItem.setDescription("An item for Reseller Category.".concat(String.valueOf(System.currentTimeMillis())));
        newItem.setPrice(new BigDecimal("30"));
        newItem.setNumber("RP-1".concat(String.valueOf(System.currentTimeMillis())));

        newItem.setTypes(new Integer[]{RESELLER_CATEGORY_ID});
        Integer itemId = resellerApi.createItem(newItem);

        //create order and invoice
        OrderWS newOrder = buildOneTimePostPaidOrder(RESELLER_USER_ID);

        // now add some lines
        OrderLineWS line = buildOrderLine(itemId, Integer.valueOf(1), null);
        newOrder.setOrderLines(new OrderLineWS[]{line});

        System.out.println("Creating order...  " + newOrder);
        Integer invoiceId=null;
        try{
            OrderChangeWS[] orderWs2= OrderChangeBL.buildFromOrder(newOrder, RESELLER_ORDER_CHANGE_STATUS_APPLY_ID);
            System.out.println("orderWs2 ***********" +orderWs2);
            invoiceId = resellerApi.createOrderAndInvoice(newOrder, orderWs2);
            System.out.println("invoice id ***********" +invoiceId);
        } catch (Exception e) {
            fail("Invoice Id is null " +e.getMessage());
        }

        InvoiceWS invoice = resellerApi.getInvoiceWS(invoiceId);
        Integer orderId = invoice.getOrders()[0];

        //get invoice of Reseller Customer
        // ToDo change this line
        OrderWS resellerOrder = api.getLatestOrder(PP_RESELLER_USER);

        //check if created order has same amount as reseller order
        assertNotNull("Order for reseller should exist", resellerOrder);
        assertEquals(invoice.getBalance(), resellerOrder.getTotal());

        resellerApi.deleteInvoice(invoiceId);
        resellerApi.deleteOrder(orderId);

        resellerOrder = resellerApi.getLatestOrder(RESELLER_USER_ID);
        System.out.println("Reseller Order is: " + resellerOrder);
        if (resellerOrder != null) {
            assertNotSame("Order for reseller should not be the one deleted", orderId, resellerOrder.getId());
            resellerApi.deleteOrder(resellerOrder.getId());
        }
        // tear down

        resellerApi.deleteItem(itemId);
	}

    @Test
	public void test039AutoCreateOrderForParent_DifferentialPricing() {

        ItemDTOEx newItem = new ItemDTOEx();
        newItem.setDescription("An item for Reseller Category.");
        newItem.setNumber("RP-1".concat(String.valueOf(System.currentTimeMillis())));

        PriceModelWS parentEntityPrice = new PriceModelWS(PriceModelStrategy.GRADUATED.name(), new BigDecimal("10.00"), Constants.PRIMARY_CURRENCY_ID);
        parentEntityPrice.addAttribute("included", "0");  // 0 units included
        newItem.setPriceModelCompanyId(PRANCING_PONY_ENTITY_ID);

        newItem.addDefaultPrice(CommonConstants.EPOCH_DATE, parentEntityPrice);
        newItem.setTypes(new Integer[]{RESELLER_CATEGORY_ID});
        Integer itemId = api.createItem(newItem);

        // set child entity price
        ItemDTOEx updated = api.getItem(itemId, RESELLER_USER_ID, null);

        PriceModelWS childEntityPrice = new PriceModelWS(PriceModelStrategy.GRADUATED.name(), new BigDecimal("5.00"), Constants.PRIMARY_CURRENCY_ID);
        childEntityPrice.addAttribute("included", "0");  // 0 units included

        SortedMap<Date, PriceModelWS> prices = new TreeMap<Date, PriceModelWS>();
        prices.put(CommonConstants.EPOCH_DATE, childEntityPrice);

        updated.setDefaultPrices(prices);

        updated.setPriceModelCompanyId(RESELLER_ENTITY_ID);

        api.updateItem(updated);

        //create order and invoice
        OrderWS newOrder = buildOneTimePostPaidOrder(RESELLER_USER_ID);
        newOrder.setActiveSince(new Date());

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line = PricingTestHelper.buildOrderLine(itemId, 1);

        lines[0] = line;
        newOrder.setOrderLines(lines);

        System.out.println("Creating order ... " + newOrder);
        Integer invoiceId = resellerApi.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        InvoiceWS invoice = resellerApi.getInvoiceWS(invoiceId);
        Integer orderId = invoice.getOrders()[0];

        //get invoice of Reseller Customer
        OrderWS resellerOrder = api.getLatestOrder(PP_RESELLER_USER);

        assertNotNull("Order for reseller should exist", resellerOrder);

        // invoice should have balance 5
        assertBigDecimalEquals(new BigDecimal("5.00"), invoice.getBalanceAsDecimal());
        //reseller order must be created at reseller entity's price
        assertBigDecimalEquals(new BigDecimal("10.00"), resellerOrder.getTotalAsDecimal());

        resellerApi.deleteInvoice(invoiceId);
        resellerApi.deleteOrder(orderId);

        resellerOrder = api.getLatestOrder(PP_RESELLER_USER);
        System.out.println("Reseller Order is: " + resellerOrder);
        if (resellerOrder != null) {
            assertNotSame("Order for reseller should not be the one deleted", orderId, resellerOrder.getId());
            api.deleteOrder(resellerOrder.getId());
        }
        // tear down

        api.deleteItem(itemId);
	}

    @Test
    public void test40CreateUpdateApplyOrderChanges() {

        ItemDTOEx productA = createProduct(40, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + productA);
        Integer retA = api.createItem(productA);
        assertNotNull("Product A should be created", retA);

        ItemDTOEx productC = createProduct(40, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + productC);
        Integer retC = api.createItem(productC);
        assertNotNull("Product C should be created", retC);

        ItemDTOEx productB = createProduct(40, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        setDependency(productB, retC, 1, 5);
        System.out.println("Creating item ..." + productB);
        Integer retB = api.createItem(productB);
        assertNotNull("Product B should be created", retB);

        OrderWS orderA = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        OrderLineWS line1 = buildOrderLine(retA, Integer.valueOf(1), BigDecimal.ONE);
        orderA.setOrderLines(new OrderLineWS[]{line1});
        OrderChangeWS orderChange = OrderChangeBL.buildFromLine(line1, orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        Integer orderAId = api.createUpdateOrder(orderA, new OrderChangeWS[]{orderChange});
        assertNotNull("Order should be created", orderAId);
        orderA = api.getOrder(orderAId);
        assertEquals("Incorrect lines count for order", 1, orderA.getOrderLines().length);
        OrderLineWS line = orderA.getOrderLines()[0];
        assertEquals("Incorrect order line description", line1.getDescription(), line.getDescription());
        assertEquals("Incorrect order line quantity", line1.getQuantityAsDecimal().intValue(), line.getQuantityAsDecimal().intValue());

        // update line own fields
        line.setDescription(line1.getDescription() + " updated");
        line.setQuantity(330); // try to update quantity without change - should fail
        api.createUpdateOrder(orderA, new OrderChangeWS[0]);
        orderA = api.getOrder(orderAId);
        line = orderA.getOrderLines()[0];
        assertEquals("Description was not updated", line1.getDescription() + " updated", line.getDescription());
        assertEquals("Quantity should not be updated without change", line1.getQuantityAsDecimal().intValue(), line.getQuantityAsDecimal().intValue());

        // update line fields via order change
        line.setPrice(BigDecimal.valueOf(10));
        orderChange = OrderChangeBL.buildFromLine(line, null, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange.setQuantity(BigDecimal.valueOf(10).subtract(line.getQuantityAsDecimal()));
        api.createUpdateOrder(orderA, new OrderChangeWS[]{orderChange});
        orderA = api.getOrder(orderAId);
        line = orderA.getOrderLines()[0];
        assertEquals("Quantity should be updated", BigDecimal.valueOf(10).intValue(), line.getQuantityAsDecimal().intValue());
        assertEquals("Price should be updated", BigDecimal.valueOf(10).intValue(), line.getPriceAsDecimal().intValue());

        // try to update line fields on back date
        orderChange.setStartDate(new Date(new Date().getTime() - 1000L * 60 * 60 * 24 * 5));
        try {
            api.createUpdateOrder(orderA, new OrderChangeWS[]{orderChange});
        } catch (SessionInternalError ex) {
            assertEquals("Incorrect error", "OrderChangeWS,startDate,validation.error.incorrect.start.date", ex.getErrorMessages()[0]);
        }

        // try to create unapplicable change
        OrderChangeStatusWS notApplyStatus = new OrderChangeStatusWS();
        notApplyStatus.setApplyToOrder(ApplyToOrder.NO);
        notApplyStatus.setDeleted(0);
        notApplyStatus.setOrder(orderAId);
        notApplyStatus.addDescription(
                new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "NotApplyStatus_" + new Date().getTime())
        );
        List<OrderChangeWS> allOrderChanges = Arrays.asList(api.getOrderChanges(orderAId));
        int changesCount = allOrderChanges.size();
        Integer statusId = api.createOrderChangeStatus(notApplyStatus);
        orderA = api.getOrder(orderAId);
        line = orderA.getOrderLines()[0];
        line.setPrice(BigDecimal.valueOf(110));
        orderChange = OrderChangeBL.buildFromLine(line, null, statusId);
        api.createUpdateOrder(orderA, new OrderChangeWS[]{orderChange});
        orderA = api.getOrder(orderAId);
        line = orderA.getOrderLines()[0];
        assertEquals("Price should not be changed", BigDecimal.valueOf(10).intValue(), line.getPriceAsDecimal().intValue());
        allOrderChanges = Arrays.asList(api.getOrderChanges(orderAId));
        assertEquals("Order change should be stored without apply", changesCount + 1, allOrderChanges.size());
        OrderChangeWS lastChange = null;
        for (OrderChangeWS tmpChange : allOrderChanges) {
            if (tmpChange.getUserAssignedStatusId().equals(statusId)) {
                lastChange = tmpChange;
                break;
            }
        }
        assertNotNull("Order change should be stored and retrieved", lastChange);
        assertNull("Order change application date should be null", lastChange.getApplicationDate());
        lastChange.setDelete(1);
        api.createUpdateOrder(orderA,  new OrderChangeWS[]{lastChange});
        allOrderChanges = Arrays.asList(api.getOrderChanges(orderAId));
        assertEquals("Order change should be deleted", changesCount, allOrderChanges.size());
        // clear statuses
        api.deleteOrderChangeStatus(statusId);

        // try to create order lines via changes
        OrderLineWS line2 = buildOrderLine(retB, Integer.valueOf(1), BigDecimal.ONE);
        OrderLineWS line3 = buildOrderLine(retC, Integer.valueOf(1), BigDecimal.ONE);
        OrderChangeWS change2 = OrderChangeBL.buildFromLine(line2, orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        OrderChangeWS change3 = OrderChangeBL.buildFromLine(line3, orderA, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        change3.setParentOrderChange(change2);

        api.createUpdateOrder(orderA, new OrderChangeWS[] {change2, change3});
        orderA = api.getOrder(orderAId);
        assertEquals("Incorrect lines count", 3, orderA.getOrderLines().length);
        line1 = findOrderLineWithItem(orderA.getOrderLines(), retA);
        line2 = findOrderLineWithItem(orderA.getOrderLines(), retB);
        line3 = findOrderLineWithItem(orderA.getOrderLines(), retC);
        assertEquals("Line 3 should have line 2 as parent", line2, line3.getParentLine());

        // create empty child order with order change to future date
        OrderWS orderB = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        orderB.setParentOrder(orderA);
        orderA.setChildOrders(new OrderWS[]{orderB});
        OrderLineWS line4 = buildOrderLine(retC, Integer.valueOf(1), BigDecimal.ONE);
        line4.setParentLine(line2);
        line2.setChildLines(new OrderLineWS[]{line4});
        OrderChangeWS change4 = OrderChangeBL.buildFromLine(line4, orderB, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        change4.setStartDate(new Date(new Date().getTime() + 1000L * 60 * 60 * 24 * 10));

        Integer orderBId = api.createUpdateOrder(orderB, new OrderChangeWS[]{change4});
        assertNotNull("Order B should be created", orderBId);
        orderB = api.getOrder(orderBId);
        assertEquals("Incorrect parent order", orderAId, orderB.getParentOrder() != null ? orderB.getParentOrder().getId() : null);
        assertEquals("Order should not have lines", 0, orderB.getOrderLines().length);

        // apply change4 right now
        allOrderChanges = Arrays.asList(api.getOrderChanges(orderBId));
        assertEquals("Should be 1 order change for order B", 1, allOrderChanges.size());
        change4 = allOrderChanges.get(0);
        assertEquals("Order change status should be pending", Constants.ORDER_CHANGE_STATUS_PENDING, change4.getStatusId());
        assertNull("Order change application date should be null", change4.getApplicationDate());
        change4.setStartDate(Util.truncateDate(new Date()));
        api.createUpdateOrder(orderB, new OrderChangeWS[]{change4});
        orderB = api.getOrder(orderBId);
        assertEquals("Order should have lines now", 1, orderB.getOrderLines().length);
        line4 = orderB.getOrderLines()[0];
        assertEquals("Incorrect parent line", (Integer) line2.getId(), line4.getParentLine() != null ? line4.getParentLine().getId() : null);
        allOrderChanges = Arrays.asList(api.getOrderChanges(orderBId));
        assertEquals("Should be 1 order change for order B", 1, allOrderChanges.size());
        change4 = allOrderChanges.get(0);
        assertEquals("Order change status should user status", change4.getUserAssignedStatusId(), change4.getStatusId());
        assertNotNull("Application date should be filled", change4.getApplicationDate());

        api.deleteOrder(orderBId);
        api.deleteOrder(orderAId);
	    //TODO: this here removes the dependency by hand,
	    // but should be managed automatically by delete
		productB = api.getItem(retB, null, null);
	    productB.setDependencies(null);
	    api.updateItem(productB);

	    //delete items
        api.deleteItem(retC);
        api.deleteItem(retB);
        api.deleteItem(retA);
    }

    @Test
    public void test041OrderChangeWithMetaFields() {

        ItemDTOEx newItem = new ItemDTOEx();
        newItem.setDescription("OrderLineMetaFields test");
        newItem.setPrice(new BigDecimal("29.5"));
        newItem.setNumber("OM-100");
        newItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});

        MetaFieldWS metaField = new MetaFieldWS();
        metaField.setDataType(DataType.STRING);
        metaField.setDisabled(false);
        metaField.setDisplayOrder(1);
        metaField.setEntityId(PRANCING_PONY_ENTITY_ID);
        metaField.setEntityType(EntityType.ORDER_LINE);
        metaField.setMandatory(false);
        metaField.setPrimary(false);
        metaField.setName("Item OM-100 orderLinesMetaField_1");
        newItem.setOrderLineMetaFields(new MetaFieldWS[]{metaField});

        System.out.println("Creating item ..." + newItem);
        Integer itemId = api.createItem(newItem);
        assertNotNull("The item was not created", itemId);
        newItem.setId(itemId);


        OrderWS newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);

        OrderChangeWS orderChange = buildFromItem(newItem, newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange.setStartDate(newOrder.getActiveSince());
        orderChange.getMetaFields()[0].setValue("str-val-1");

        //RATE ORDER
        System.out.println("Rating order: " + newOrder);
        OrderWS orderWS = api.rateOrder(newOrder, new OrderChangeWS[]{orderChange});
        assertEquals("One order line expected", 1, orderWS.getOrderLines().length);
        OrderLineWS line = orderWS.getOrderLines()[0];

        assertEquals("One meta field expected", 1, line.getMetaFields().length);
        MetaFieldValueWS mfVal = line.getMetaFields()[0];

        assertEquals("str-val-1", mfVal.getValue());
        assertEquals(metaField.getName(), mfVal.getFieldName());


        //CREATE ORDER
        System.out.println("Creating order");
        Integer orderId = api.createOrder(newOrder, new OrderChangeWS[]{orderChange});

        System.out.println("Loading order " + orderId);
        orderWS = api.getOrder(orderId);
        assertNotNull(String.format("order id %s should not be null", orderId), orderWS);
        line = orderWS.getOrderLines()[0];

        assertEquals("One meta field expected", 1, line.getMetaFields().length);
        mfVal = line.getMetaFields()[0];

        assertEquals("str-val-1", mfVal.getValue());
        assertEquals(metaField.getName(), mfVal.getFieldName());

        //UPDATE META FIELD OF ORDER LINE
        System.out.println("Update meta field of order line");
        mfVal.setValue("str-val-2");
        api.updateOrder(orderWS, new OrderChangeWS[]{});

        orderWS = api.getOrder(orderId);

        line = orderWS.getOrderLines()[0];

        assertEquals("One meta field expected", 1, line.getMetaFields().length);
        mfVal = line.getMetaFields()[0];

        assertEquals("str-val-2", mfVal.getValue());
        assertEquals(metaField.getName(), mfVal.getFieldName());

        api.deleteOrder(orderId);

        //CREATE ORDER 2

        // Create New Test Item
        ItemDTOEx testItem = createProduct(41, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        // Persist
        Integer testItemId = api.createItem(testItem);

        newOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        System.out.println("Creating order 2");
        orderChange = new OrderChangeWS();
        orderChange.setOrderWS(newOrder);
        orderChange.setPrice("10.00");
        orderChange.setQuantity("1");
        orderChange.setUserAssignedStatusId(PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange.setUseItem(1);
        orderChange.setItemId(testItemId);
        orderChange.setDescription("Item 1");
        orderChange.setStartDate(Util.truncateDate(new Date()));
	    orderChange.setOrderChangeTypeId(com.sapienter.jbilling.common.Constants.ORDER_CHANGE_TYPE_DEFAULT);

        orderId = api.createOrder(newOrder, new OrderChangeWS[]{orderChange});

        System.out.println("Loading order");
        orderWS = api.getOrder(orderId);

        orderChange = buildFromItem(newItem, newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange.getMetaFields()[0].setValue("str-val-1");
        orderChange.setOrderId(orderId);

        //ORDER CHANGE WITH UPDATE
        System.out.println("Order Change with Update");
        api.updateOrder(orderWS, new OrderChangeWS[]{orderChange});

        orderWS = api.getOrder(orderId);

        line = orderWS.getOrderLines()[0];
        if(testItemId.intValue() == line.getItemId()) {
           line = orderWS.getOrderLines()[1];
        }

        assertEquals("One meta field expected", 1, line.getMetaFields().length);
        mfVal = null;
        if("str-val-1".equals(line.getMetaFields()[0].getValue())) {
            mfVal = line.getMetaFields()[0];
        } else {
            mfVal = line.getMetaFields()[1];
        }

        assertEquals("str-val-1", mfVal.getValue());
        assertEquals(metaField.getName(), mfVal.getFieldName());

        //GET ORDER CHANGES
        System.out.println("Get order changes");
        List<OrderChangeWS> changes = Arrays.asList(api.getOrderChanges(orderId));
        assertEquals(2, changes.size());
        orderChange = changes.get(0);
        if(testItemId.intValue() == orderChange.getItemId()) {
            orderChange = changes.get(1);
        }
        assertEquals(1, orderChange.getMetaFields().length);

        mfVal = line.getMetaFields()[0];

        assertEquals("str-val-1", mfVal.getValue());
        assertEquals(metaField.getName(), mfVal.getFieldName());

        //REMOVE THE META FIELD FROM THE PRODUCT
        newItem = api.getItem(itemId, null, null);
        newItem.setOrderLineMetaFields(new MetaFieldWS[]{});
        api.updateItem(newItem);

        changes = Arrays.asList(api.getOrderChanges(orderId));
        assertEquals(2, changes.size());
        orderChange = changes.get(0);
        if(testItemId.intValue() == orderChange.getItemId()) {
            orderChange = changes.get(1);
        }
        assertEquals(0, orderChange.getMetaFields().length);

        api.deleteOrder(orderId);
        api.deleteItem(testItemId);
        api.deleteItem(itemId);
    }

    @Test
    public void test042CreateOrderForUsageProductInFuture() {

        ItemDTOEx firstItem = createProduct(42, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + firstItem);
        Integer firstItemId = api.createItem(firstItem);
        assertNotNull("Second Product should be created", firstItemId);

        System.out.println("Create first order");
        OrderWS order = buildOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_POST_PAID, ORDER_PERIOD_MONTHLY);
        order.setOrderStatusWS( api.findOrderStatusById( api.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, api.getCallerCompanyId()) ));
        //set the active since 3 months into the future
        order.setActiveSince(new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 30 * 3)));

        OrderChangeWS ws = new OrderChangeWS();
        ws.setOptLock(1);
        ws.setUserAssignedStatusId(PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        ws.setStartDate(Util.truncateDate(new Date()));
        ws.setOrderWS(order);
        ws.setOrderChangeTypeId(com.sapienter.jbilling.common.Constants.ORDER_CHANGE_TYPE_DEFAULT);
        ws.setUseItem(1);

        ws.setDescription("Description".concat(String.valueOf(System.currentTimeMillis())));
        ws.setItemId(firstItemId);
        ws.setPrice("2.00");
        ws.setQuantity("10");
        ws.setUseItem(1);

        int orderId = api.createOrder(order, new OrderChangeWS[] {ws});

        api.deleteOrder(orderId);
        api.deleteItem(firstItemId);
    }

    @Test
    public void test043UpdateOrderSecurityTest() {

        // Create Test item
        ItemDTOEx firstItem = createProduct(43, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + firstItem);
        Integer firstItemId = api.createItem(firstItem);
        assertNotNull("First Product should be created", firstItemId);

        // Create second test item
        ItemDTOEx secondItem = createProduct(43, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        secondItem.setEntityId(MORDOR_ENTITY_ID);
        System.out.println("Creating item ..." + secondItem);
        Integer secondItemId = mordorApi.createItem(secondItem);
        assertNotNull("Second Product should be created", secondItemId);

        // Create Test Order
        OrderWS testOrder = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        // Add order lines
        OrderLineWS orderLine = buildOrderLine(firstItemId, Integer.valueOf(1), null);
        testOrder.setOrderLines(new OrderLineWS[]{orderLine});

        // Persist
        Integer orderId = api.createOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        // Create test order in mordor
        OrderWS testOrderMordor = buildOneTimePostPaidOrder(MORDOR_USER_ID);
        // Add order lines
        orderLine = buildOrderLine(secondItemId, Integer.valueOf(1), null);
        testOrderMordor.setOrderLines(new OrderLineWS[]{orderLine});

        // Persist
        Integer orderMordorId = mordorApi.createOrder(testOrderMordor, OrderChangeBL.buildFromOrder(testOrderMordor, MORDOR_ORDER_CHANGE_STATUS_APPLY_ID));

        testOrder = api.getOrder(orderId); //get order of current entity
        // try to update an order of another entity
        // order with id which belongs to another entity
        System.out.println("Updating bad order...");
        testOrder.setId(orderMordorId);
        try {
            api.updateOrder(testOrder, null);
            fail("Should throw a exception!!");
        } catch (SecurityException se){
            System.out.println("Exception thrown!!\n" + se.getMessage());

        } finally {
            api.deleteOrder(orderId);
            mordorApi.deleteOrder(orderMordorId);
            api.deleteItem(firstItemId);
            mordorApi.deleteItem(secondItemId);
        }
    }

    @Test
    public void test044SaveLegacyOrder() {

        ItemDTOEx firstItem = createProduct(35, BigDecimal.ONE, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        System.out.println("Creating item ..." + firstItem);
        Integer firstItemId = api.createItem(firstItem);
        assertNotNull("Second Product should be created", firstItemId);

        OrderWS order = buildOneTimePostPaidOrder(PRANCING_PONY_USER_ID);
        order.setNotes("Just a test note.");

        OrderLineWS line = buildOrderLine(firstItemId, Integer.valueOf(1), BigDecimal.ONE);

        order.setOrderLines(new OrderLineWS[]{line});

        Integer orderId = api.saveLegacyOrder(order);

        assertNotNull(orderId);

        OrderWS orderWS = api.getOrder(orderId);

        assertNotNull(orderWS);

        assertEquals(order.getUserId(), orderWS.getUserId());
        assertEquals(order.getBillingTypeId(), orderWS.getBillingTypeId());
        assertEquals(order.getPeriod(), orderWS.getPeriod());
        assertEquals(order.getCurrencyId(), orderWS.getCurrencyId());
        assertEquals(order.getNotes(), orderWS.getNotes());

        OrderLineWS orderLineWS = orderWS.getOrderLines()[0];
        assertNotNull(orderLineWS);
        assertEquals(line.getPriceAsDecimal().setScale(2), orderLineWS.getPriceAsDecimal().setScale(2));
        assertEquals(line.getTypeId(), orderLineWS.getTypeId());
        assertEquals(line.getQuantityAsDecimal().setScale(2), orderLineWS.getQuantityAsDecimal().setScale(2));
        assertEquals(line.getAmountAsDecimal().setScale(2), orderLineWS.getAmountAsDecimal().setScale(2));
        assertEquals(line.getDescription(), orderLineWS.getDescription());
        assertEquals(line.getItemId(), orderLineWS.getItemId());

        api.deleteOrder(orderId);
        api.deleteItem(firstItemId);

    }

    @Test
    public void test045OrderUserCodes() {
        userCodeTest(true);
        //userCodeTest(false);
    }

	@Test
    public void test039OrderPlanSwap() throws Exception {
        System.out.println("test039OrderPlanSwap");

        String uuid = "_" + new Date().getTime();
        ItemDTOEx smsActivation = createProduct(39, new BigDecimal(39), "SMS Service Activation" + uuid, false);
        Integer smsActivationId = api.createItem(smsActivation);
        smsActivation.setId(smsActivationId);

        ItemDTOEx gprsActivation = createProduct(39, new BigDecimal(39), "GPRS Service Activation" + uuid, false);
        Integer gprsActivationId = api.createItem(gprsActivation);
        gprsActivation.setId(gprsActivationId);

        ItemDTOEx gggActivation = createProduct(39, new BigDecimal(39), "3G Service Activation" + uuid, false);
        Integer gggActivationId = api.createItem(gggActivation);
        gggActivation.setId(gggActivationId);

        ItemDTOEx smsToAmerica = createProduct(39, new BigDecimal(39), "SMS to North America" + uuid, false);
        Integer smsToAmericaId = api.createItem(smsToAmerica);
        smsToAmerica.setId(smsToAmericaId);

        List<PlanItemWS> planItems = new LinkedList<PlanItemWS>();
        planItems.add(createPlanItem(smsActivationId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY));
        planItems.add(createPlanItem(gprsActivationId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY));
        planItems.add(createPlanItem(smsToAmericaId, BigDecimal.ZERO, ORDER_PERIOD_MONTHLY));
        PlanWS goldPlan = createPlan(39, "Gold plan", BigDecimal.TEN, planItems, api);

        ItemDTOEx goldPlanItem = api.getItem(goldPlan.getItemId(), GANDALF_USER_ID, new PricingField[]{});


        OrderWS order = createOrder(39, "Order with plan");
        order.setPeriod(ORDER_PERIOD_MONTHLY);
        List<OrderChangeWS> changes = new LinkedList<OrderChangeWS>();
        OrderChangeWS planItemChange = buildFromItem(goldPlanItem, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        OrderChangePlanItemWS smsActivationChange = buildPlanChangeFromItem(smsActivation);
        OrderChangePlanItemWS gprsActivationChange = buildPlanChangeFromItem(gprsActivation);
        OrderChangePlanItemWS smsToAmericaChange = buildPlanChangeFromItem(smsToAmerica);

        planItemChange.setOrderChangePlanItems(new OrderChangePlanItemWS[]{smsActivationChange, gprsActivationChange, smsToAmericaChange});
        changes.add(planItemChange);

        Integer orderId = api.createUpdateOrder(order, changes.toArray(new OrderChangeWS[changes.size()]));
        assertNotNull("Order should be created", orderId);
        order = api.getOrder(orderId);

        assertEquals("Incorrect count of Order lines", 3, order.getOrderLines().length);
        assertEquals("Incorrect order status", Constants.DEFAULT_ORDER_INVOICE_STATUS_ID, order.getOrderStatusWS().getId());
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), goldPlanItem.getId()));
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), smsActivation.getId()));
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), gprsActivation.getId()));

        //create another plan and subscribe to it
        planItems = new LinkedList<PlanItemWS>();
        planItems.add(createPlanItem(smsActivationId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY));
        planItems.add(createPlanItem(gggActivationId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY));
        planItems.add(createPlanItem(smsToAmericaId, BigDecimal.ZERO, ORDER_PERIOD_MONTHLY));
        PlanWS titaniumPlan = createPlan(39, "Titanium plan", new BigDecimal("20"), planItems, api);

        ItemDTOEx titaniumPlanItem = api.getItem(titaniumPlan.getItemId(), GANDALF_USER_ID, new PricingField[]{});


        OrderChangeWS[] changesToSwap = api.calculateSwapPlanChanges(order, goldPlanItem.getId(), titaniumPlanItem.getId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
        assertNotNull("Swap changes should be calculated", changesToSwap);
        assertEquals("Swap changes count is incorrect for DEFAULT method", 3, changesToSwap.length);
        OrderChangeWS changeForPlan = findOrderChangeWithItem(changesToSwap, titaniumPlanItem.getId());
        assertEquals("OrderChangePlanItemWS should be found", 1, changeForPlan.getOrderChangePlanItems().length);

        api.createUpdateOrder(order, changesToSwap);
        order = api.getOrder(orderId);

        assertEquals("Incorrect count of Order lines after swap", 3, order.getOrderLines().length);
        assertEquals("Incorrect order status", Constants.DEFAULT_ORDER_INVOICE_STATUS_ID, order.getOrderStatusWS().getId());
        assertNull("Line for item Gold plan should not be found", findOrderLineWithItem(order.getOrderLines(), goldPlanItem.getId()));
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), titaniumPlanItem.getId()));
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), smsActivation.getId()));
        assertNull("Line for item GPGS activation should not be found", findOrderLineWithItem(order.getOrderLines(), gprsActivation.getId()));
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), gggActivation.getId()));

        OrderChangeWS[] changesToSwap2 = api.calculateSwapPlanChanges(order, titaniumPlanItem.getId(), goldPlanItem.getId(), SwapMethod.DEFAULT, Util.truncateDate(new Date()));
        assertNotNull("Swap changes should be calculated", changesToSwap2);
        assertEquals("Swap changes count is incorrect for DEFAULT method", 4, changesToSwap2.length);
        changeForPlan = findOrderChangeWithItem(changesToSwap2, goldPlanItem.getId());
        assertEquals("OrderChangePlanItemWS should be found", 2, changeForPlan.getOrderChangePlanItems().length);


        api.createUpdateOrder(order, changesToSwap2);
        order = api.getOrder(orderId);

        assertEquals("Incorrect count of Order lines after swap", 3, order.getOrderLines().length);
        assertEquals("Incorrect order status", Constants.DEFAULT_ORDER_INVOICE_STATUS_ID, order.getOrderStatusWS().getId());
        assertNotNull("Line for item Gold plan should be found now", findOrderLineWithItem(order.getOrderLines(), goldPlanItem.getId()));
        assertNull("Line for item should not be found", findOrderLineWithItem(order.getOrderLines(), titaniumPlanItem.getId()));
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), smsActivation.getId()));
        assertNotNull("Line for item GPGS activation should be found", findOrderLineWithItem(order.getOrderLines(), gprsActivation.getId()));
        assertNull("Line for item should not be found", findOrderLineWithItem(order.getOrderLines(), gggActivation.getId()));

		//cleanup
		api.deleteOrder(orderId);
		api.deleteItem(titaniumPlan.getItemId());
		api.deleteItem(goldPlan.getItemId());
		api.deleteItem(smsToAmerica.getId());
		api.deleteItem(gggActivation.getId());
		api.deleteItem(gprsActivation.getId());
		api.deleteItem(smsActivation.getId());
    }

	@Test
    public void test40ApplyOrderChangeWithMetaFieldsFromType() throws Exception {
        System.out.println("test40ApplyOrderChangeWithMetaFieldsFromType");

        String uuid = "_" + new Date().getTime();
        ItemDTOEx smsidn = createProduct(40, new BigDecimal(40), "SMSISDN" + uuid, true);
        Integer smsidnId = api.createItem(smsidn);
        smsidn.setId(smsidnId);

        AssetWS smsidnAsset1 = createAsset(smsidnId, "389767890943", ALLOWED_ASSET_MANAGEMENT_ASSET_STATUS_AVAILABLE, api);
        AssetWS smsidnAsset2 = createAsset(smsidnId, "38977890987", ALLOWED_ASSET_MANAGEMENT_ASSET_STATUS_AVAILABLE, api);

        OrderChangeTypeWS numberTransferChangeType = createOrderChangeType("Number Transfer", Arrays.asList("donor ICC"), ALLOWED_ASSET_MANAGEMENT_3_CATEGORY, false, api);
        String metaFieldName = numberTransferChangeType.getOrderChangeTypeMetaFields().iterator().next().getName();

        OrderWS order = createOrder(40, "Order for change type meta fields");
        OrderChangeWS change = buildFromItem(smsidn, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        change.setAssetIds(new Integer[] {smsidnAsset1.getId()});
        change.setOrderChangeTypeId(numberTransferChangeType.getId());
        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName(metaFieldName);
        metaField1.setValue("Test metaField DONOR ICC");
        change.setMetaFields(new MetaFieldValueWS[]{metaField1});

        Integer orderId = api.createUpdateOrder(order, new OrderChangeWS[]{change});
        assertNotNull("Order should be created", orderId);
        order = api.getOrder(orderId);

        assertEquals("Incorrect count of Order lines", 1, order.getOrderLines().length);
        assertEquals("Incorrect order status", Constants.DEFAULT_ORDER_INVOICE_STATUS_ID, order.getOrderStatusWS().getId());
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), smsidn.getId()));
        assertEquals("Incorrect asset", smsidnAsset1.getId(), findOrderLineWithItem(order.getOrderLines(), smsidnId).getAssetIds()[0]);

        List<OrderChangeWS> orderChanges = Arrays.asList(api.getOrderChanges(orderId));
        assertNotNull("Order change should be created", orderChanges);
        assertEquals("Incorrect order changes count for order", 1, orderChanges.size());
        assertEquals("Meta fields should be presented", 1, orderChanges.get(0).getMetaFields().length);
        assertEquals("Incorrect meta field name", metaFieldName, orderChanges.get(0).getMetaFields()[0].getFieldName());
        assertEquals("Incorrect meta field value", metaField1.getValue(), orderChanges.get(0).getMetaFields()[0].getValue());

        OrderChangeTypeWS swapMsisdnChangeType = createOrderChangeType("Swap MSISDN", Arrays.asList("Swap note"), ALLOWED_ASSET_MANAGEMENT_3_CATEGORY, false, api);
        metaFieldName = swapMsisdnChangeType.getOrderChangeTypeMetaFields().iterator().next().getName();

        change = OrderChangeBL.buildFromLine(order.getOrderLines()[0], order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        change.setQuantity(BigDecimal.ZERO);
        change.setAssetIds(new Integer[] {smsidnAsset2.getId()});
        change.setOrderChangeTypeId(swapMsisdnChangeType.getId());
        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName(metaFieldName);
        metaField2.setValue("Test metaField note");
        change.setMetaFields(new MetaFieldValueWS[]{metaField2});

        api.createUpdateOrder(order, new OrderChangeWS[]{change});
        order = api.getOrder(orderId);

        assertEquals("Incorrect count of Order lines", 1, order.getOrderLines().length);
        assertEquals("Incorrect order status", Constants.DEFAULT_ORDER_INVOICE_STATUS_ID, order.getOrderStatusWS().getId());
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), smsidn.getId()));
        // The assertion is commented below as getAssets method in OrderLineDTO returns a set of multiple assets so the order of
        // the asset ID's is not preserved. The order of the assetIds() can vary randomly

     //   assertEquals("Incorrect asset after change", smsidnAsset2.getId(), findOrderLineWithItem(order.getOrderLines(), smsidnId).getAssetIds()[0]);

        orderChanges = Arrays.asList(api.getOrderChanges(orderId));
        assertNotNull("Order change should be created", orderChanges);
        assertEquals("Incorrect order changes count for order", 2, orderChanges.size());
        assertEquals("Meta fields should be presented", 1, orderChanges.get(0).getMetaFields().length);
        assertEquals("Meta fields should be presented", 1, orderChanges.get(1).getMetaFields().length);

	    //cleanup
	    api.deleteOrder(orderId);
	    api.deleteAsset(smsidnAsset1.getId());
	    api.deleteAsset(smsidnAsset2.getId());
	    api.deleteItem(smsidnId);
    }

	@Test
    public void test41ChangeOrderStatusViaOrderChange() throws Exception {
        System.out.println("test41ChangeOrderStatusViaOrderChange");

        ///JbillingAPI api = JbillingAPIFactory.getAPI();

        ItemDTOEx isdn = createProduct(40, new BigDecimal(40), "SMSISDN".concat(String.valueOf(System.currentTimeMillis())), true);
        Integer isdnId = api.createItem(isdn);
        isdn.setId(isdnId);

        AssetWS isdnAsset1 = createAsset(isdnId, "389445670912", ALLOWED_ASSET_MANAGEMENT_ASSET_STATUS_AVAILABLE, api);
        AssetWS isdnAsset2 = createAsset(isdnId, "389218974567", ALLOWED_ASSET_MANAGEMENT_ASSET_STATUS_AVAILABLE, api);

        OrderChangeTypeWS newNumberActivationChangeType = createOrderChangeType("New number activation", new LinkedList<String>(), null, true, api);

        OrderWS order = createOrder(41, "Order for status change via order change");
        OrderStatusWS orderStatusWS = new OrderStatusWS();
        orderStatusWS.setId(Constants.DEFAULT_ORDER_INVOICE_STATUS_ID); //OrderStatusFlag INVOICE = 1
        order.setOrderStatusWS(orderStatusWS);
        OrderChangeWS change = buildFromItem(isdn, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        change.setAssetIds(new Integer[]{isdnAsset1.getId()});
        change.setOrderChangeTypeId(newNumberActivationChangeType.getId());
        change.setOrderStatusIdToApply(api.findOrderStatusById(Integer.valueOf(3)).getId());

        Integer orderId = api.createUpdateOrder(order, new OrderChangeWS[]{change});
        assertNotNull("Order should be created", orderId);
        order = api.getOrder(orderId);

        assertEquals("Incorrect count of Order lines", 1, order.getOrderLines().length);
        assertEquals("Order status should be changed during order change apply", Constants.DEFAULT_ORDER_NOT_INVOICE_STATUS_ID, order.getOrderStatusWS().getId());
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), isdn.getId()));
        assertEquals("Incorrect asset", isdnAsset1.getId(), findOrderLineWithItem(order.getOrderLines(), isdnId).getAssetIds()[0]);

        OrderChangeTypeWS serviceUnbarringChangeType = createOrderChangeType("Service Unbarring", new LinkedList<String>(), null, true, api);

        change = OrderChangeBL.buildFromLine(order.getOrderLines()[0], order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        change.setQuantity(BigDecimal.ZERO);
        change.setAssetIds(new Integer[] {isdnAsset2.getId()});
        change.setOrderChangeTypeId(serviceUnbarringChangeType.getId());
        change.setOrderStatusIdToApply(api.findOrderStatusById(Integer.valueOf(1)).getId());

        api.createUpdateOrder(order, new OrderChangeWS[]{change});
        order = api.getOrder(orderId);

        assertEquals("Incorrect count of Order lines", 1, order.getOrderLines().length);
        assertEquals("Order status should be reverted to ACTIVE", Constants.DEFAULT_ORDER_INVOICE_STATUS_ID, order.getOrderStatusWS().getId());
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), isdn.getId()));
        // The assertion is commented below as getAssets method in OrderLineDTO returns a set of multiple assets so the order of
        // the asset ID's is not preserved. The order of the assetIds() can vary randomly
        // assertEquals("Incorrect asset after change", isdnAsset2.getId(), findOrderLineWithItem(order.getOrderLines(), isdnId).getAssetIds()[0]);

		//cleanup
		api.deleteOrder(orderId);
		api.deleteAsset(isdnAsset1.getId());
		api.deleteAsset(isdnAsset2.getId());
		api.deleteItem(isdn.getId());
    }

    private void userCodeTest(boolean useCreateUpdate) {

        UserWS user = createUser(true, null, Constants.PRIMARY_CURRENCY_ID, true, api, PRANCING_PONY_ACCOUNT_TYPE_ID);
        String uc1 = user.getUserName() + "00002";
        String uc2 = user.getUserName() + "00003";

        UserCodeWS uc = new UserCodeWS();
        uc.setIdentifier(uc1);
        uc.setTypeDescription("ProgramDesc");
        uc.setType("ProgramType");
        uc.setExternalReference("translationId");
        uc.setValidFrom(new Date());
        uc.setUserId(user.getUserId());
        api.createUserCode(uc);

        uc.setIdentifier(uc2);
        api.createUserCode(uc);

        // Create New Test Item
        ItemDTOEx testItem = CreateObjectUtil.createItem(PRANCING_PONY_ENTITY_ID, BigDecimal.ONE, Constants.PRIMARY_CURRENCY_ID, PRANCING_PONY_CATEGORY_ID, "Test Product");
        // Persist
        Integer testItemId = api.createItem(testItem);


        OrderWS order = buildOneTimePostPaidOrder(user.getId());
        order.setUserCode("aaaaa");

        OrderChangeWS orderChange = new OrderChangeWS();
        orderChange.setOrderChangeTypeId(1);
        orderChange.setOrderWS(order);
        orderChange.setPrice("10.00");
        orderChange.setQuantity("1");
        orderChange.setUserAssignedStatusId(PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange.setUseItem(1);
        orderChange.setItemId(testItemId);
        orderChange.setDescription("Item 1");
        orderChange.setStartDate(Util.truncateDate(new Date()));

        try {
            if(useCreateUpdate) {
                api.createUpdateOrder(order, new OrderChangeWS[] {orderChange});
            } else {
                api.createOrder(order, new OrderChangeWS[] {orderChange});
            }
        } catch (SessionInternalError e) {
            Asserts.assertContainsError(e, "OrderWS,userCode,validation.error.userCode.not.exist,aaaaa");
        }

        order.setUserCode(uc1);
        int orderId = 0;
        if(useCreateUpdate) {
            orderId = api.createUpdateOrder(order, new OrderChangeWS[] {orderChange});
        } else {
            orderId = api.createOrder(order, new OrderChangeWS[]{orderChange});
        }

        order = api.getOrder(orderId);
        assertEquals(uc1, order.getUserCode());

        Integer[] ids = api.getOrdersLinkedToUser(user.getUserId());
        assertEquals(1, ids.length);
        assertEquals(orderId, ids[0].intValue());

        ids = api.getOrdersByUserCode(uc1);
        assertEquals(1, ids.length);
        assertEquals(orderId, ids[0].intValue());

        order.setUserCode(uc2);
        if(useCreateUpdate) {
            api.createUpdateOrder(order, new OrderChangeWS[] {});
        } else {
            api.updateOrder(order, new OrderChangeWS[]{});
        }

        order = api.getOrder(orderId);
        assertEquals(uc2, order.getUserCode());

        api.deleteOrder(orderId);
        api.deleteItem(testItemId);
        api.deleteUser(user.getId());
    }

    @Test
    public void test040CreateSubscriptionOrder() throws Exception {
        System.out.println("#test040CreateSubscriptionOrder");

	    String random = String.valueOf(System.currentTimeMillis() % 1000);

        //create a new user
        UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);

        // create a subscription category
        ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription("Subscription Category:"+random);
        itemType.setOrderLineTypeId(5);

        try {
        	api.createItemCategory(itemType);
        	fail("Item category must not have been created, Subscription category must allow asset management");
        } catch (Exception e) {
        }

        itemType.setAllowAssetManagement(1);

        addDefaultStatusesToCategory(itemType);

        System.out.println("Creating item category ...");
        Integer itemTypeId = api.createItemCategory(itemType);
        assertNotNull(itemTypeId);
        System.out.println("Subscription category created");

        // create subscription product
        ItemDTOEx item = createProduct(40, BigDecimal.ONE, "40:"+random, true);
        item.setTypes(new Integer[]{itemTypeId});
        Integer itemId = api.createItem(item);

        // create an asset for product
        AssetWS asset1 = createAsset(itemId, "742442112:"+random, ALLOWED_ASSET_MANAGEMENT_ASSET_STATUS_AVAILABLE, api);

        // Create

        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(user.getId());
        newOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        newOrder.setPeriod(Constants.ORDER_PERIOD_ONCE);
        newOrder.setCurrencyId(new Integer(1));


        newOrder.setActiveSince(new Date());

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[1];

        // this is an item line
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_SUBSCRIPTION);
        line.setQuantity(new Integer(1));
        line.setItemId(itemId);
        // take the description from the item
        line.setUseItem(new Boolean(true));
        line.setAssetIds(new Integer[] {asset1.getId()});
        lines[0] = line;

        newOrder.setOrderLines(lines);
        OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        System.out.println("Creating order ... " + newOrder);
        Integer orderId = api.createOrder(newOrder, changes);
        assertNull("The order was created", orderId);

        // verify results
        UserWS parent = api.getUserWS(user.getId());
        assertTrue("User should have allowed subaccounts", parent.getIsParent());
        assertNotNull("User children can not be null", parent.getChildIds());
        assertTrue("User must have only 1 subaccounts", parent.getChildIds().length == 1);

        // get the sub account order
        OrderWS childOrder = api.getLatestOrder(parent.getChildIds()[0]);
        assertNotNull("Order must exist", childOrder);
        assertTrue("Order must have only one order line", childOrder.getOrderLines().length == 1);
        assertTrue("Order line must be of type subscription", childOrder.getOrderLines()[0].getTypeId().intValue() == Constants.ORDER_LINE_TYPE_SUBSCRIPTION);
        assertTrue("Order line have different item", childOrder.getOrderLines()[0].getItemId().intValue() == itemId.intValue());

        // try to get parent order
        assertNull("Order for parent must not exist", api.getLatestOrder(parent.getId()));

        // try to create order with same subscription item again
        try {
        	api.createOrder(newOrder, changes);
        	fail("Order must not have been created with same subscription item");
        } catch(Exception e) {
        }

        // cleanup
	    api.deleteOrder(childOrder.getId());
	    api.deleteAsset(asset1.getId());
        api.deleteItem(itemId);
        api.deleteItemCategory(itemTypeId);
        api.deleteUser(parent.getChildIds()[0]);
        api.deleteUser(parent.getId());
    }

    @Test
    public void test041CreateSubscriptionAccountAndOrderCall() throws Exception {
        System.out.println("#test041CreateSubscriptionAccountAndOrderCall");

	    String random = String.valueOf(System.currentTimeMillis() % 1000);

        //create a new user
        UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);

        // create a subscription category
        ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription("Subscription Category 5:" + random);
        itemType.setOrderLineTypeId(5);

        itemType.setAllowAssetManagement(1);

        addDefaultStatusesToCategory(itemType);

        System.out.println("Creating item category ...");
        Integer itemTypeId = api.createItemCategory(itemType);
        assertNotNull(itemTypeId);
        System.out.println("Subscription category created");

        // create subscription product
        ItemDTOEx item = createProduct(40, BigDecimal.ONE, "41:"+random, true);
        item.setTypes(new Integer[]{itemTypeId});
        Integer itemId = api.createItem(item);

        AssetWS asset2 = createAsset(itemId, "742442121:"+random, ALLOWED_ASSET_MANAGEMENT_ASSET_STATUS_AVAILABLE, api);

        // Create

        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(user.getId());
        newOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        newOrder.setPeriod(Constants.ORDER_PERIOD_ONCE);
        newOrder.setCurrencyId(new Integer(1));


        newOrder.setActiveSince(new Date());

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[1];

        // this is an item line
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_SUBSCRIPTION);
        line.setQuantity(new Integer(1));
        line.setItemId(itemId);
        // take the description from the item
        line.setUseItem(new Boolean(true));
        line.setAssetIds(new Integer[] {asset2.getId()});
        lines[0] = line;

        newOrder.setOrderLines(lines);
        OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        System.out.println("Creating subscription account and order ... ");

        List<OrderChangeWS> changesList = new ArrayList<OrderChangeWS>(Arrays.asList(changes));

        Integer[] orderIds = api.createSubscriptionAccountAndOrder(user.getId(), newOrder, true, changesList);
        assertNotNull("The order should have been created", orderIds);
        assertNotNull("There should only be one order left", orderIds.length == 1);

        UserWS parent = api.getUserWS(user.getId());

        // cleanup
	    for(Integer orderId : orderIds){
            for(InvoiceWS invoiceWS:api.getOrder(orderId).getGeneratedInvoices()){
                api.deleteInvoice(invoiceWS.getId());
            }
            api.deleteOrder(orderId);
        }
		api.deleteAsset(asset2.getId());
		api.deleteItem(itemId);
		api.deleteItemCategory(itemTypeId);
		api.deleteUser(parent.getChildIds()[0]);
		api.deleteUser(parent.getId());
    }

    private void addDefaultStatusesToCategory(ItemTypeWS itemType) {
        AssetStatusDTOEx status = new AssetStatusDTOEx();
        status.setDescription("Default");
        status.setIsAvailable(1);
        status.setIsDefault(1);
        status.setIsOrderSaved(0);
        itemType.getAssetStatuses().add(status);

        status = new AssetStatusDTOEx();
        status.setDescription("Order Saved");
        status.setIsAvailable(0);
        status.setIsDefault(0);
        status.setIsOrderSaved(1);
        itemType.getAssetStatuses().add(status);

        status = new AssetStatusDTOEx();
        status.setDescription("Order Reserved");
        status.setIsAvailable(0);
        status.setIsDefault(0);
        status.setIsOrderSaved(0);
        itemType.getAssetStatuses().add(status);
    }

    @Test
    public void test046CreateOrderWithPlanViaPlanItemChanges() {

        ItemDTOEx smsActivation = createProduct(46, BigDecimal.ONE, "SMS Service Activation", false);
        Integer smsActivationId = api.createItem(smsActivation);
        smsActivation.setId(smsActivationId);

        ItemDTOEx gprsActivation = createProduct(46, BigDecimal.ONE, "GPRS Service Activation", false);
        Integer gprsActivationId = api.createItem(gprsActivation);
        gprsActivation.setId(gprsActivationId);

        ItemDTOEx gggActivation = createProduct(46, BigDecimal.ONE, "3G Service Activation", false);
        Integer gggActivationId = api.createItem(gggActivation);
        gggActivation.setId(gggActivationId);

        ItemDTOEx smsToAmerica = createProduct(46, BigDecimal.ONE, "SMS to North America", false);
        Integer smsToAmericaId = api.createItem(smsToAmerica);
        smsToAmerica.setId(smsToAmericaId);

        List<PlanItemWS> planItems = new LinkedList<PlanItemWS>();
        planItems.add(createPlanItem(smsActivationId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY));
        planItems.add(createPlanItem(gprsActivationId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY));
        planItems.add(createPlanItem(smsToAmericaId, BigDecimal.ZERO, ORDER_PERIOD_MONTHLY));
        PlanWS goldPlan = createPlan(46, "Gold plan".concat(String.valueOf(System.currentTimeMillis())), BigDecimal.TEN, planItems, api);

        ItemDTOEx goldPlanItem = api.getItem(goldPlan.getItemId(), PRANCING_PONY_USER_ID, new PricingField[]{});

        OrderWS order = buildOrder(PRANCING_PONY_USER_ID, Constants.ORDER_BILLING_POST_PAID, ORDER_PERIOD_MONTHLY);
        List<OrderChangeWS> changes = new LinkedList<OrderChangeWS>();
        OrderChangeWS planItemChange = buildFromItem(goldPlanItem, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        OrderChangePlanItemWS smsActivationPlanItemChange = buildPlanChangeFromItem(smsActivation);
        OrderChangePlanItemWS gprsActivationPlanItemChange = buildPlanChangeFromItem(gprsActivation);
        OrderChangePlanItemWS smsToAmericaPlanItemChange = buildPlanChangeFromItem(smsToAmerica);

        planItemChange.setOrderChangePlanItems(new OrderChangePlanItemWS[] {smsActivationPlanItemChange});
        // edit description to non-standard
        smsActivationPlanItemChange.setDescription("Test description to sms activation plan change");
        // edit change date to prevent apply
        planItemChange.setStartDate(Util.truncateDate(new Date(new Date().getTime() + 1000 * 60 * 60 * 24 * 2L)));

        changes.add(planItemChange);

        Integer orderId = api.createUpdateOrder(order, changes.toArray(new OrderChangeWS[changes.size()]));
        assertNotNull("Order should be created", orderId);
        order = api.getOrder(orderId);

        assertEquals("Incorrect count of Order lines: should not be created", 0, order.getOrderLines().length);

        List<OrderChangeWS> persistedChanges = Arrays.asList(api.getOrderChanges(orderId));
        assertEquals("Change should be persisted", 1, persistedChanges.size());
        assertEquals("Change plan items should be persisted", 1, persistedChanges.get(0).getOrderChangePlanItems().length);
        planItemChange = persistedChanges.get(0);
        List<OrderChangePlanItemWS> planItemChanges = new LinkedList<OrderChangePlanItemWS>();
        planItemChanges.add(planItemChange.getOrderChangePlanItems()[0]);
        smsToAmericaPlanItemChange.setDescription("Description 2 test for sma to America");
        planItemChanges.add(smsToAmericaPlanItemChange);
        planItemChange.setOrderChangePlanItems(planItemChanges.toArray(new OrderChangePlanItemWS[planItemChanges.size()]));
        // set date to apply change
        planItemChange.setStartDate(Util.truncateDate(new Date()));
        planItemChange.setStatusId(PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        api.createUpdateOrder(order, new OrderChangeWS[]{planItemChange});

        order = api.getOrder(orderId);
        assertEquals("Incorrect count of Order lines: should be created now", 3, order.getOrderLines().length);

        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), goldPlanItem.getId()));
        assertNotNull("Line for item not found", findOrderLineWithItem(order.getOrderLines(), smsActivation.getId()));
        assertNotNull("Line for item not found, but was in plan, without change", findOrderLineWithItem(order.getOrderLines(), gprsActivation.getId()));
        assertNull("Line for item should not be found (ZERO bundle)", findOrderLineWithItem(order.getOrderLines(), smsToAmerica.getId()));

        assertEquals("Description of line should be from planItemOrderChange", smsActivationPlanItemChange.getDescription(), findOrderLineWithItem(order.getOrderLines(), smsActivation.getId()).getDescription());

        api.deleteOrder(orderId);
        api.deletePlan(goldPlan.getId());
        api.deleteItem(goldPlanItem.getId());
        api.deleteItem(gggActivationId);
        api.deleteItem(gprsActivationId);
        api.deleteItem(smsActivationId);
        api.deleteItem(smsToAmericaId);

    }

    private AssetWS getAssetWS() {
        AssetWS asset = new AssetWS();
        asset.setEntityId(1);
        asset.setIdentifier("ASSET1");
        asset.setItemId(1250);
        asset.setNotes("NOTE1");
        asset.setAssetStatusId(101);
        asset.setDeleted(0);
        MetaFieldValueWS mf = new MetaFieldValueWS();
        mf.setFieldName("Tax Exemption Code");
        mf.setDataType(DataType.LIST);
        mf.setListValue(new String[] {"01", "02"});
        asset.setMetaFields(new MetaFieldValueWS[]{mf});
        return asset;
    }

    private void checkAssetStatus(JbillingAPI api, Integer assetId, Integer expectedStatus, Integer orderLineId, Integer assignedTo) {
        AssetWS asset;
        AssetTransitionDTOEx[] transitionDTOExs;
        asset = api.getAsset(assetId) ;
        assertEquals("Asset status is [" + asset.getAssetStatusId() + "] expected [" + expectedStatus + "]", expectedStatus, asset.getAssetStatusId());
        transitionDTOExs = api.getAssetTransitions(assetId);
       assertEquals("Check asset status", expectedStatus, transitionDTOExs[0].getNewStatusId());
        if(assignedTo == null) assertNull("Assigned To is null", transitionDTOExs[0].getAssignedToId());
        else assertEquals("Assigned To is ["+transitionDTOExs[0].getAssignedToId() +"] expected ["+assignedTo+"]", assignedTo, transitionDTOExs[0].getAssignedToId());
    }

    private Date weeksFromToday(int weekNumber) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.WEEK_OF_YEAR, weekNumber);
        return calendar.getTime();
    }

    private void pause(long t) {
        System.out.println("pausing for " + t + " ms...");
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
        }
    }
	
    /**
     * Creates an order and invoices it, returning the ID of the new invoice and populating
     * the given order with the ID of the new order.
     *
     * @param order order to create and set ID
     * @return invoice id
     */

    private Integer callCreateOrderAndInvoice(OrderWS order) {

        Integer invoiceId = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        order.setId(invoice.getOrders()[0]);

        System.out.println("Created order " + order.getId() + " and invoice " + invoice.getId());

        return invoice.getId();
    }
	

    public static OrderWS createMockOrder(int userId, int orderLinesCount, BigDecimal linePrice) {
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(1); // once
        order.setCurrencyId(1);
        order.setActiveSince(new Date());
        order.setProrateFlag(Boolean.FALSE);

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

    public static OrderWS createPlanOrder(Integer userId, Integer billingTypeId, Integer periodId, Integer... planSubscriptionIds){
        OrderWS order = buildOrder(userId, billingTypeId, periodId);
        ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(planSubscriptionIds.length);
        for (Integer itemId : planSubscriptionIds){
            lines.add(buildOrderLine(itemId, 1, null));
        }
        order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));

        return order;
    }

    private void assertEmptyArray(Object[] array){
        if (array != null) {
            assertEquals("Empty array expected: " + Arrays.toString(array), 0, array.length);
        }
    }

	public static UserWS createUser(boolean goodCC, Integer parentId, Integer currencyId, boolean doCreate, JbillingAPI api, Integer accountTypeId) {

        // Create - This passes the password validation routine.

        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName("testUserName-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("P@ssword1");
        newUser.setAccountTypeId(accountTypeId);
        newUser.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        newUser.setMainRoleId(Constants.TYPE_CUSTOMER);
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setInvoiceChild(Boolean.FALSE);
        newUser.setMainSubscription(new MainSubscriptionWS(ORDER_PERIOD_MONTHLY, 1));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue("test" + System.currentTimeMillis() + "@test.com");
        metaField1.setGroupId(accountTypeId);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Pricing Test");
        metaField2.setGroupId(accountTypeId);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue(newUser.getUserName());
        metaField3.setGroupId(accountTypeId);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
		metaField4.setFieldName("ccf.payment_processor");
		metaField4.setValue("FAKE_2"); // the plug-in parameter of the processor

        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4
        });

        System.out.println("Creating credit card");
		String ccName = "Frodo Baggins";
		String ccNumber = "4111111111111152";
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		PaymentInformationWS newCC = createCreditCard(ccName, ccNumber,
				expiry.getTime());
		newUser.getPaymentInstruments().add(newCC);

        if (doCreate) {
            System.out.println("Creating user ...");
            newUser = api.getUserWS(api.createUser(newUser));
        }

        return newUser;
    }

    private UserWS createSimpleUser(Integer accountTypeId, Integer parentId, Integer currencyId, boolean doCreate, JbillingAPI api){
        // Create - This passes the password validation routine.

        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName("testUserName-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("P@ssword1");
        newUser.setAccountTypeId(accountTypeId);
        newUser.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        newUser.setMainRoleId(Constants.TYPE_CUSTOMER);
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setInvoiceChild(Boolean.FALSE);

        if (doCreate) {
            System.out.println("Creating user ...");
            newUser = api.getUserWS(api.createUser(newUser));
        }

        return newUser;
    }

    private ItemDTOEx createProduct(int testNumber, BigDecimal price, String productNumber, boolean assetsManagementEnabled) {
        ItemDTOEx product = CreateObjectUtil.createItem(
		        PRANCING_PONY_ENTITY_ID, price, Constants.PRIMARY_CURRENCY_ID, PRANCING_PONY_CATEGORY_ID,
		        trimToLength("OrderWS " + testNumber + "-" + productNumber, 35));
        product.setNumber(trimToLength("OrderWS " + testNumber + "-" + productNumber, 50));
        product.setAssetManagementEnabled(assetsManagementEnabled ? 1 : 0);
        return product;
    }

    public static OrderWS buildOrder(Integer userId, Integer billingTypeId, Integer periodId){
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(billingTypeId);
        order.setPeriod(periodId);
        order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2008, 9, 3);
        order.setActiveSince(cal.getTime());
        // notes can only be 200 long... but longer should not fail
        order.setNotes("At the same time the British Crown began bestowing land grants in Nova Scotia on favored subjects to encourage settlement and trade with the mother country. In June 1764, for instance, the Boards of Trade requested the King make massive land grants to such Royal favorites as Thomas Pownall, Richard Oswald, Humphry Bradstreet, John Wentworth, Thomas Thoroton[10] and Lincoln's Inn barrister Levett Blackborne.[11] Two years later, in 1766, at a gathering at the home of Levett Blackborne, an adviser to the Duke of Rutland, Oswald and his friend James Grant were released from their Nova Scotia properties so they could concentrate on their grants in British East Florida.");

        return order;
    }

    public static OrderWS buildOneTimePostPaidOrder(Integer userId){
        return buildOrder(userId, Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_PERIOD_ONCE);
    }

    public static OrderLineWS buildOrderLine(Integer itemId, Integer quantity, BigDecimal price, Integer... assetsId) {
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(quantity);
        line.setDescription(String.format("Line for product %d", itemId));
        line.setItemId(itemId);
        if(null != assetsId){
            line.setAssetIds(assetsId);
        }
        if(null == price){
            line.setUseItem(Boolean.TRUE);
        } else {
            line.setPrice(price);
            line.setAmount(price.multiply(new BigDecimal(quantity)));
        }
        return line;
    }
    
    private OrderWS createOrder(int testNumber, String orderDescription) {
    	OrderWS order = new OrderWS();
    	order.setUserId(GANDALF_USER_ID);
    	order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
    	order.setPeriod(Constants.ORDER_PERIOD_ONCE);
    	order.setCurrencyId(1);
    	order.setNotes("test " + testNumber + " order " + orderDescription);
    	Calendar cal = Calendar.getInstance();
    	cal.clear();
    	cal.set(2008, 9, 3);
    	order.setActiveSince(cal.getTime());
    	return order;
    }

    private OrderLineWS createOrderLine(int testNumber, Integer itemId, String productDescription) {
        OrderLineWS line = new OrderLineWS();
        line.setPrice(new BigDecimal(testNumber));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(1);
        line.setAmount(new BigDecimal(testNumber));
        line.setDescription("Line for product " + productDescription);
        line.setItemId(itemId);
        return line;
    }

    private OrderLineWS findOrderLineWithDescription(OrderLineWS[] lines, String description) {
        for (OrderLineWS line : lines) {
            if (description.contains(line.getDescription())) return line;
        }
        return null;
    }

    private OrderLineWS findOrderLineWithItem(OrderLineWS[] lines, Integer itemId) {
        System.out.println("Searching lines for item..." + itemId);
        for (OrderLineWS line : lines) {
            if (line != null && itemId.equals(line.getItemId())) {
                System.out.println("Item found on line " + line.getId());
                return line;
            }
        }
        System.out.println("Item not found");
        return null;
    }

    private OrderWS findOrderWithItem(List<OrderWS> orders, Integer itemId, Integer linesCount) {
        System.out.println("Searching orders for item..." + itemId);
        for (OrderWS order : orders) {
            System.out.println("Order has lines " + order.getOrderLines().length + ", expected: " + linesCount);
            if (order.getOrderLines().length == linesCount) {
                if (findOrderLineWithItem(order.getOrderLines(), itemId) != null) {
                    System.out.println("Item found on order " + order.getId());
                    return order;
                }
            }
        }
        System.out.println("Item not found");
        return null;
    }

    private OrderChangeWS findOrderChangeWithItem(OrderChangeWS[] orderChanges, Integer itemId) {
        for (OrderChangeWS change : orderChanges) {
            if (change.getItemId().equals(itemId)) {
                return change;
            }
        }
        return null;
    }

    private AssetWS createAsset(Integer itemId,
                                String identifier,
                                Integer statusId,
                                JbillingAPI api) {
        String uuid = "_" + new Date().getTime();
        AssetWS asset = new AssetWS();
        asset.setItemId(itemId);
        asset.setEntityId(PRANCING_PONY_ENTITY_ID);
        asset.setIdentifier(identifier + uuid);
        asset.setNotes("NOTE1");
        asset.setAssetStatusId(statusId);
        asset.setDeleted(0);
        int id = api.createAsset(asset);
        return api.getAsset(id);
    }

    private OrderChangeTypeWS createOrderChangeType(String name,
                                                    List<String> metaFieldNames,
                                                    Integer itemTypeId,
                                                    boolean isAllowOrderStatusChange,
                                                    JbillingAPI api) {
        OrderChangeTypeWS type = new OrderChangeTypeWS();
        String uuid =  "_" + new Date().getTime();
        type.setName(name + uuid);
        if (itemTypeId == null) {
            type.setDefaultType(true);
        } else {
            type.setItemTypes(Arrays.asList(itemTypeId));
        }
        if (metaFieldNames != null) {
            type.setOrderChangeTypeMetaFields(new HashSet<MetaFieldWS>());
            for (String metaFieldName : metaFieldNames) {
                MetaFieldWS field = new MetaFieldWS();
                field.setName(metaFieldName + uuid);
                field.setEntityType(EntityType.ORDER_CHANGE);
                field.setDataType(DataType.STRING);
                type.getOrderChangeTypeMetaFields().add(field);
            }
        }
        type.setAllowOrderStatusChange(isAllowOrderStatusChange);
        Integer id = api.createUpdateOrderChangeType(type);
        return api.getOrderChangeTypeById(id);
    }

    private PlanWS createPlan(int testNumber,
                              String planName,
                              BigDecimal price,
                              List<PlanItemWS> planBundleItems,
                              JbillingAPI api) {
        ItemDTOEx planItem = createProduct(testNumber, price, planName, false);
        planItem.setId(api.createItem(planItem));
        PlanWS plan = new PlanWS();
        plan.setEditable(0);
        plan.setPeriodId(ORDER_PERIOD_MONTHLY);
        plan.setItemId(planItem.getId());
        plan.setPlanItems(planBundleItems);
        Integer planId = api.createPlan(plan);
        return api.getPlanWS(planId);
    }

    private PlanItemWS createPlanItem(Integer itemId,
                                      BigDecimal quantity, Integer periodId) {
        PlanItemWS planItemWS = new PlanItemWS();
        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setPeriodId(periodId);
        bundle.setQuantity(quantity);
        planItemWS.setItemId(itemId);
        planItemWS.setBundle(bundle);
        planItemWS.addModel(CommonConstants.EPOCH_DATE,
                new PriceModelWS(PriceModelStrategy.ZERO.name(), BigDecimal.ZERO, Constants.PRIMARY_CURRENCY_ID));
                //new PriceModelWS(PriceModelStrategy.ZERO.name(), BigDecimal.ONE, Constants.PRIMARY_CURRENCY_ID));
        return planItemWS;
    }

    private String trimToLength(String value, int length) {
        if (value == null || value.length() < length) return value;
        return value.substring(0, length);
    }

    private Integer createItemCategory(JbillingAPI api){
    	ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription("category"+Short.toString((short)System.currentTimeMillis()));
        itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        itemType.setAllowAssetManagement(1);
        itemType.setAssetStatuses(createAssetStatusForCategory());
        return api.createItemCategory(itemType);
    }

    private Set<AssetStatusDTOEx> createAssetStatusForCategory(){
    	Set<AssetStatusDTOEx> assetStatuses = new HashSet<AssetStatusDTOEx>();
    	AssetStatusDTOEx addToOrderStatus = new AssetStatusDTOEx();
    	addToOrderStatus.setDescription("AddToOrderStatus");
    	addToOrderStatus.setIsAvailable(0);
    	addToOrderStatus.setIsDefault(0);
    	addToOrderStatus.setIsInternal(0);
    	addToOrderStatus.setIsOrderSaved(1);
    	assetStatuses.add(addToOrderStatus);

    	AssetStatusDTOEx available = new AssetStatusDTOEx();
    	available.setDescription("Available");
    	available.setIsAvailable(1);
    	available.setIsDefault(1);
    	available.setIsInternal(0);
    	available.setIsOrderSaved(0);
    	assetStatuses.add(available);

    	AssetStatusDTOEx notAvailable = new AssetStatusDTOEx();
    	notAvailable.setDescription("NotAvailable");
    	notAvailable.setIsAvailable(0);
    	notAvailable.setIsDefault(0);
    	notAvailable.setIsInternal(0);
    	notAvailable.setIsOrderSaved(0);
    	assetStatuses.add(notAvailable);

    	return assetStatuses;
    }

    public static PaymentInformationWS createCreditCard(String cardHolderName,
			String cardNumber, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(CC_PM_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_GATEWAY_KEY);

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true,
				DataType.STRING, 1, cardHolderName);
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.STRING, 2,
				cardNumber);
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
                DataType.STRING, 3, DateTimeFormat.forPattern(
                        Constants.CC_DATE_FORMAT).print(date.getTime()));
		// have to pass meta field card type for it to be set
		addMetaField(metaFields, CC_MF_TYPE, true, false,
				DataType.INTEGER, 4, new Integer(0));
		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}

    private static void addMetaField(List<MetaFieldValueWS> metaFields,
			String fieldName, boolean disabled, boolean mandatory,
			DataType dataType, Integer displayOrder, Object value) {
		MetaFieldValueWS ws = new MetaFieldValueWS();
		ws.setFieldName(fieldName);
		ws.setDisabled(disabled);
		ws.setMandatory(mandatory);
		ws.setDataType(dataType);
		ws.setDisplayOrder(displayOrder);
		ws.setValue(value);
		metaFields.add(ws);
	}
    public static Integer getOrCreateOrderChangeApplyStatus(JbillingAPI api){
    	OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
    	Integer statusId = null;
    	for(OrderChangeStatusWS orderChangeStatus : list){
    		if(orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)){
    			statusId = orderChangeStatus.getId();
    			break;
    		}
    	}
    	if(statusId != null){
    		return statusId;
    	}else{
    		OrderChangeStatusWS newStatus = new OrderChangeStatusWS();
    		newStatus.setApplyToOrder(ApplyToOrder.YES);
    		newStatus.setDeleted(0);
    		newStatus.setOrder(1);
    		newStatus.addDescription(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "status1"));
    		return api.createOrderChangeStatus(newStatus);
    	}
    }

    private AssetWS getAssetWS(String identifier, Integer status, Integer itemId) {
        AssetWS asset = new AssetWS();
        asset.setEntityId(1);
        asset.setIdentifier(identifier);
        asset.setItemId(itemId);
        asset.setNotes("NOTE1");
        asset.setAssetStatusId(status);
        asset.setDeleted(0);
        return asset;
    }

    private void initializeAssetsStatuses(JbillingAPI api){

        ItemTypeWS[] categories = api.getAllItemCategoriesByEntityId(api.getCallerCompanyId());
        for(ItemTypeWS type : categories) {
            if (type.getId().equals(PRANCING_PONY_CATEGORY_ID)) {
                for(AssetStatusDTOEx status : type.getAssetStatuses()){
                    if(status.getIsDefault() == 1){
                        ASSET_STATUS_DEFAULT= status.getId();
                    }
                    if(status.getIsOrderSaved() == 1){
                        ASSET_STATUS_ORDER_SAVED = status.getId();
                    }
                    if(status.getIsAvailable() == 1){
                        ASSET_STATUS_AVAILABLE = status.getId();
                    }
                    if(status.getIsAvailable() == 0 && status.getIsDefault() == 0 && status.getIsOrderSaved() == 0 && status.getIsInternal() == 0){
                        ASSET_STATUS_NOT_AVAILABLE = status.getId();
                    }
                }
                break;
            }
        }

    }

    private void clearOrderHierarchy(OrderWS order){

        if(null == order){
            fail("Can not operate on null order!!!");
        }

        // Clear parent if any
        if(null != order.getParentOrder()){
            order.setParentOrder(null);
        }

        // Clear child lines if any
        if(null != order.getChildOrders() && Integer.valueOf(0) < order.getChildOrders().length){
            order.setChildOrders(new OrderWS[0]);
        }

        if(null != order.getOrderLines() && Integer.valueOf(0) < order.getOrderLines().length){
            for (OrderLineWS line : order.getOrderLines()){
                if(null != line.getParentLine()){
                    line.setParentLine(null);
                }
                if(null != line.getChildLines() && Integer.valueOf(0) < line.getChildLines().length ){
                    line.setChildLines(new OrderLineWS[0]);
                }
            }
        }
    }

    public static Integer getOrCreateOrderStatusInvoice(JbillingAPI api){

        Integer entityId = api.getCallerCompanyId();
        Integer orderStatusId = null;
        try {
            orderStatusId = api.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, entityId);
            return orderStatusId;
        } catch (SessionInternalError sie){

            OrderStatusWS newStatus = new OrderStatusWS();
            newStatus.setEntity(api.getCompany());
            newStatus.setOrderStatusFlag(OrderStatusFlag.INVOICE);
            List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>(1);
            descriptions.add(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "Active"));
            newStatus.setDescriptions(descriptions);
            newStatus.setDescription("Active");

            return api.createUpdateOrderStatus(newStatus);
        }
    }

    private Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for(OrderPeriodWS period : periods){
            if(1 == period.getValue() &&
                    PeriodUnitDTO.MONTH == period.getPeriodUnitId()){
                return period.getId();
            }
        }
        //there is no monthly order period so create one
        OrderPeriodWS monthly = new OrderPeriodWS();
        monthly.setEntityId(api.getCallerCompanyId());
        monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);//monthly
        monthly.setValue(1);
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "ORD:MONTHLY")));
        return api.createOrderPeriod(monthly);
    }

}
