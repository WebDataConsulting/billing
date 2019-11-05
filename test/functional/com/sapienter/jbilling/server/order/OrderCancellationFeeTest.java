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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetTransitionDTOEx;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.JBillingTestUtils;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.user.*;

import java.util.Hashtable;

import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.JBillingTestUtils;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import junit.framework.TestCase;
import com.sapienter.jbilling.server.util.JBillingTestUtils;

import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import org.testng.annotations.*;

import junit.framework.TestCase;
import org.joda.time.DateMidnight;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import com.sapienter.jbilling.server.util.CreateObjectUtil;

/**
 * @author Bilal Nasir
 */
@Test(groups = {"web-services", "order"})
public class OrderCancellationFeeTest {

    private final static int ORDER_CANCELLATION_TASK_ID = 113;
    private static final Integer PRANCING_PONY = Integer.valueOf(1);
    private static Integer ORDER_CHANGE_STATUS_APPLY_ID;
    private static Integer ORDER_PERIOD_MONTHLY;

    private JbillingAPI api;

    @BeforeClass
    public void initializeTests() throws IOException, JbillingAPIException {

        if(null == api){
            api = JbillingAPIFactory.getAPI();
        }

        ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeApplyStatus(api);
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);

    }

    @AfterClass
    public void cleanUp(){

        if(null != ORDER_CHANGE_STATUS_APPLY_ID){
            ORDER_CHANGE_STATUS_APPLY_ID = null;
        }
        if(null != ORDER_PERIOD_MONTHLY){
            ORDER_PERIOD_MONTHLY = null;
        }
        if(null != api){
            api = null;
        }

    }


    /**
     * IMPORTANT: This test may fail if new tasks are created before upgrade-3.4 is executed, as it
     * uses a fixed value for the TASK_ID. Please check the id for the OrderCancellationTask task.
     * @throws Exception
     */
    @Test // 2014-11-04 Igor Poteryaev. same plugin cretion conflict with test009RefundAndCancelFee
    public void test001CreateUpdateOrder() throws Exception {

        String description = "Test Dependencies " + new Date().getTime();

        // Create parent penalty category
        ItemTypeWS penaltyCategory = new ItemTypeWS();
        penaltyCategory.setDescription("Parent fees");
        penaltyCategory.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_PENALTY);
        penaltyCategory.setEntityId(PRANCING_PONY);

        // Persist
        Integer penaltyParentCategoryId = api.createItemCategory(penaltyCategory);

        // Create child fee category
        ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription(description);
        itemType.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        itemType.setParentItemTypeId(penaltyParentCategoryId);

        System.out.println("Creating item category '" + description + "'...");
        Integer itemTypeId = api.createItemCategory(itemType);

        ItemDTOEx newItem = new ItemDTOEx();

        List<InternationalDescriptionWS> descriptions = new java.util.ArrayList<InternationalDescriptionWS>();
        InternationalDescriptionWS enDesc = new InternationalDescriptionWS(ServerConstants.LANGUAGE_ENGLISH_ID, "Fee Product");
        descriptions.add(enDesc);

        newItem.setDescriptions(descriptions);
        newItem.setPriceManual(0);
        newItem.setPrices(CreateObjectUtil.setItemPrice(new BigDecimal(1), new DateMidnight(1970, 1, 1).toDate(), PRANCING_PONY, Integer.valueOf(1)));
        newItem.setNumber("FP");
        newItem.setTypes(new Integer[]{itemTypeId});

        System.out.println("Creating item ..." + newItem);
        Integer ret = api.createItem(newItem);

        ItemDTOEx newItem1 = new ItemDTOEx();

        List<InternationalDescriptionWS> descriptions1 = new java.util.ArrayList<InternationalDescriptionWS>();
        InternationalDescriptionWS enDesc1 = new InternationalDescriptionWS(ServerConstants.LANGUAGE_ENGLISH_ID, "Cool Phone");
        descriptions1.add(enDesc1);

        newItem1.setDescriptions(descriptions);
        newItem1.setPriceManual(0);
        newItem1.setPrices(CreateObjectUtil.setItemPrice(new BigDecimal(25), new DateMidnight(1970, 1, 1).toDate(), PRANCING_PONY, Integer.valueOf(1)));
        newItem1.setNumber("CP");
        newItem1.setTypes(new Integer[]{itemTypeId});

        System.out.println("Creating item ..." + newItem1);
        Integer ret1 = api.createItem(newItem1);

        PluggableTaskWS plugin = new PluggableTaskWS();
        Map<String, String> parameters = new Hashtable<String, String>();
        parameters.put("fee_item_id", "" + ret);
        plugin.setParameters((Hashtable) parameters);
        plugin.setProcessingOrder(16);
        plugin.setTypeId(ORDER_CANCELLATION_TASK_ID);

        Integer pluginId = api.createPlugin(plugin);

        UserWS newUser = com.sapienter.jbilling.server.user.WSTest.createUser(true, true, null, ServerConstants.PRIMARY_CURRENCY_ID, false);

        System.out.println("Creating new user ...");
        // do the creation
        Integer userId = api.createUser(newUser);
        System.out.println("user created");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2013, 1, 1);

        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
        order.setPeriod(ORDER_PERIOD_MONTHLY);
        order.setCurrencyId(ServerConstants.PRIMARY_CURRENCY_ID);
        order.setActiveSince(cal.getTime());
        order.setCancellationFee(10);
        order.setCancellationFeeType("ZERO");
        order.setCancellationMinimumPeriod(6);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(ret1);
        line.setQuantity(1);
        line.setPrice(new BigDecimal("25.00"));
        line.setAmount(new BigDecimal("25.00"));

        order.setOrderLines(new OrderLineWS[]{line});

        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        System.out.println("order created...");

        OrderWS saveOrder = api.getOrder(orderId);
        cal.clear();
        cal.set(2013, 3, 31);
        saveOrder.setActiveUntil(cal.getTime());

        System.out.println("calling updateOrder method");
        api.updateOrder(saveOrder, null);
        System.out.println("saved updated order");

        OrderWS latestOrder = api.getLatestOrder(userId);
        assertEquals("Total", new BigDecimal("10.00"), latestOrder.getTotalAsDecimal());

        cal.clear();
        cal.set(2013, 1, 1);

        //cancellation fee percentage without maximum fee
        OrderWS order2 = new OrderWS();
        order2.setUserId(userId);
        order2.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
        order2.setPeriod(ORDER_PERIOD_MONTHLY);
        order2.setCurrencyId(ServerConstants.PRIMARY_CURRENCY_ID);
        order2.setActiveSince(cal.getTime());
        order2.setCancellationFeePercentage(10);
        order2.setCancellationFeeType("PERCENTAGE");
        order2.setCancellationMinimumPeriod(6);

        OrderLineWS line2 = new OrderLineWS();
        line2.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line2.setDescription("Order line");
        line2.setItemId(ret1);
        line2.setQuantity(1);
        line2.setPrice(new BigDecimal("25.00"));
        line2.setAmount(new BigDecimal("25.00"));

        order2.setOrderLines(new OrderLineWS[]{line2});

        Integer orderId2 = api.createOrder(order2, OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID));

        cal.clear();
        cal.set(2013, 3, 31);
        OrderWS saveOrder2 = api.getOrder(orderId2);
        saveOrder2.setActiveUntil(cal.getTime());

        System.out.println("calling updateOrder method");
        api.updateOrder(saveOrder2, null);
        System.out.println("saved updated order");

        OrderWS latestOrder2 = api.getLatestOrder(userId);
        assertEquals("Total", new BigDecimal("7.67"), latestOrder2.getTotalAsDecimal().setScale(2));

        cal.clear();
        cal.set(2013, 1, 1);
        //cancellation fee percentage with maximum fee
        OrderWS order3 = new OrderWS();
        order3.setUserId(userId);
        order3.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
        order3.setPeriod(ORDER_PERIOD_MONTHLY);
        order3.setCurrencyId(ServerConstants.PRIMARY_CURRENCY_ID);
        order3.setActiveSince(cal.getTime());
        order3.setCancellationFeePercentage(10);
        order3.setCancellationFeeType("PERCENTAGE");
        order3.setCancellationMaximumFee(4);
        order3.setCancellationMinimumPeriod(6);

        OrderLineWS line3 = new OrderLineWS();
        line3.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line3.setDescription("Order line");
        line3.setItemId(ret1);
        line3.setQuantity(1);
        line3.setPrice(new BigDecimal("25.00"));
        line3.setAmount(new BigDecimal("25.00"));

        order3.setOrderLines(new OrderLineWS[]{line3});

        Integer orderId3 = api.createOrder(order3, OrderChangeBL.buildFromOrder(order3, ORDER_CHANGE_STATUS_APPLY_ID));

        cal.clear();
        cal.set(2013, 3, 31);
        OrderWS saveOrder3 = api.getOrder(orderId3);
        saveOrder3.setActiveUntil(cal.getTime());

        System.out.println("calling updateOrder method");
        api.updateOrder(saveOrder3, null);
        System.out.println("saved updated order");

        OrderWS latestOrder3 = api.getLatestOrder(userId);
        assertEquals("Total", new BigDecimal("4.00"), latestOrder3.getTotalAsDecimal());

        api.deleteOrder(latestOrder3.getId());
        api.deleteOrder(orderId3);
        api.deleteOrder(latestOrder2.getId());
        api.deleteOrder(orderId2);
        api.deleteOrder(latestOrder.getId());
        api.deleteOrder(orderId);
        api.deleteUser(userId);
        api.deletePlugin(pluginId);
        api.deleteItem(ret);
        api.deleteItem(ret1);
        api.deleteItemCategory(itemTypeId);
        api.deleteItemCategory(penaltyParentCategoryId);
    }

    private static Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
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
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(ServerConstants.LANGUAGE_ENGLISH_ID, "OCF:MONTHLY")));
        return api.createOrderPeriod(monthly);
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
            newStatus.addDescription(new InternationalDescriptionWS(ServerConstants.LANGUAGE_ENGLISH_ID, "status1"));
            return api.createOrderChangeStatus(newStatus);
        }
    }

}