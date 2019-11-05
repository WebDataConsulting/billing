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

package com.sapienter.jbilling.server.process;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.PreferenceTypeWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import junit.framework.TestCase;

import org.joda.time.DateMidnight;
import org.testng.annotations.Test;

import java.util.Date;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * WSTest to test billing process and invoice generation API calls
 *
 * @author Brian Cowdery
 * @since Oct-21-2011
 */
@Test(groups = { "web-services", "process" })
public class WSTest {
	
    private static final Integer ORDER_PERIOD_PLUGIN_ID = 6;
    private static final Integer BASIC_ORDER_PERIOD_PLUGIN_TYPE_ID = 7; // BasicOrderPeriodTAsk
    private static final Integer PRO_RATE_ORDER_PERIOD_PLUGIN_TYPE_ID = 37; // ProRateOrderPeriodTask
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;

    @Test
    public void testPostPaidGejbilnerateInvoice() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        resetBillingConfiguration(); // make sure we're using the same config/dates

        UserWS user = new UserWS();
        user.setUserName("invoice-test-01-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(5);
        user.setAccountTypeId(Integer.valueOf(1));
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField01 = new MetaFieldValueWS();
        metaField01.setFieldName("contact.email");
        metaField01.setValue(user.getUserName() + "@test.com");
        metaField01.setGroupId(1);

        MetaFieldValueWS metaField02 = new MetaFieldValueWS();
        metaField02.setFieldName("contact.first.name");
        metaField02.setValue("Invoice Test");
        metaField02.setGroupId(1);

        MetaFieldValueWS metaField03 = new MetaFieldValueWS();
        metaField03.setFieldName("contact.last.name");
        metaField03.setValue("Post-paid with due date");
        metaField03.setGroupId(1);

        user.setMetaFields(new MetaFieldValueWS[]{
                metaField01,
                metaField02,
                metaField03
        });

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());

        OrderWS order = new OrderWS();
    	order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
        order.setPeriod(2); // monthly
        order.setCurrencyId(1);
        order.setActiveSince(new DateMidnight(2011, 1, 1).toDate()); // active since January 01, 2011

        // create an order to be invoiced
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(2602); // lemonade
        line.setUseItem(true);
        line.setQuantity(1);
        order.setOrderLines(new OrderLineWS[] { line });

        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        order.setId(api.createOrder(order, orderChanges)); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());


        // generate the invoice with a target billing & due date, "use process date for invoice" is off so
        // order date will be used when calculating invoice due date:
        //
        //      post-paid order starts on January 01, 2010
        //      invoiced on February 01, 2011
        //      45 day due date
        //
        // post paid == invoice after 1 period, use order start date + 1 month
        // January 01, 2011 + 1 month + 45 days = March 18, 2011
        Date billingDate = new DateMidnight(2011, 2, 1).toDate();
        Integer[] invoiceIds = api.createInvoiceWithDate(user.getUserId(), billingDate, PeriodUnitDTO.DAY, 45, false);
        assertEquals("1 invoice generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
        assertEquals("due date 45 days from order start date", new DateMidnight(2011, 3, 18).toDate(), invoice.getDueDate());

        // cleanup
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(order.getId());
        api.deleteUser(user.getUserId());
    }

    @Test
    public void testPrePaidGenerateInvoicePostdated() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        resetBillingConfiguration(); // make sure we're using the same config/dates

        UserWS user = new UserWS();
        user.setUserName("invoice-test-02-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(5);
        user.setAccountTypeId(Integer.valueOf(1));
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField01 = new MetaFieldValueWS();
        metaField01.setFieldName("contact.email");
        metaField01.setValue(user.getUserName() + "@test.com");
        metaField01.setGroupId(1);

        MetaFieldValueWS metaField02 = new MetaFieldValueWS();
        metaField02.setFieldName("contact.first.name");
        metaField02.setValue("Invoice Test");
        metaField02.setGroupId(1);

        MetaFieldValueWS metaField03 = new MetaFieldValueWS();
        metaField03.setFieldName("contact.last.name");
        metaField03.setValue("Pre-paid (past) with due date");
        metaField03.setGroupId(1);

        user.setMetaFields(new MetaFieldValueWS[]{
                metaField01,
                metaField02,
                metaField03
        });

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());

        OrderWS order = new OrderWS();
    	order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(2); // monthly
        order.setCurrencyId(1);
        order.setActiveSince(new DateMidnight(2010, 12, 1).toDate()); // active since December 01, 2010

        // create an order to be invoiced
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(2602); // lemonade
        line.setUseItem(true);
        line.setQuantity(1);
        order.setOrderLines(new OrderLineWS[] { line });

        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        order.setId(api.createOrder(order, orderChanges)); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());


        // generate the invoice with a target billing & due date, "use process date for invoice" is off so
        // order date will be used when calculating invoice due date:
        //
        //      pre-paid order starts on December 01, 2010
        //      invoiced on January 01, 2011
        //      45 day due date
        //
        // pre paid == invoice immediately, use order start date
        // December 01, 2010 + 45 = January 15, 2011
        Date billingDate = new DateMidnight(2011, 1, 1).toDate();
        Integer[] invoiceIds = api.createInvoiceWithDate(user.getUserId(), billingDate, PeriodUnitDTO.DAY, 45, false);
        assertEquals("1 invoice generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
        assertEquals("due date 45 days from order start date", new DateMidnight(2011, 2, 15).toDate(), invoice.getDueDate());

        // cleanup
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(order.getId());
        api.deleteUser(user.getUserId());
    }

    @Test
    public void testPrePaidGenerateInvoice() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        resetBillingConfiguration(); // make sure we're using the same config/dates

        UserWS user = new UserWS();
        user.setUserName("invoice-test-03-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(5);
        user.setAccountTypeId(Integer.valueOf(1));
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField01 = new MetaFieldValueWS();
        metaField01.setFieldName("contact.email");
        metaField01.setValue(user.getUserName() + "@test.com");
        metaField01.setGroupId(1);

        MetaFieldValueWS metaField02 = new MetaFieldValueWS();
        metaField02.setFieldName("contact.first.name");
        metaField02.setValue("Invoice Test");
        metaField02.setGroupId(1);

        MetaFieldValueWS metaField03 = new MetaFieldValueWS();
        metaField03.setFieldName("contact.last.name");
        metaField03.setValue("Pre-paid with due date");
        metaField03.setGroupId(1);

        user.setMetaFields(new MetaFieldValueWS[]{
                metaField01,
                metaField02,
                metaField03
        });

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());

        OrderWS order = new OrderWS();
    	order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(2); // monthly
        order.setCurrencyId(1);
        order.setActiveSince(new DateMidnight(2011, 1, 1).toDate()); // active since January 01, 2011

        // create an order to be invoiced
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(2602); // lemonade
        line.setUseItem(true);
        line.setQuantity(1);
        order.setOrderLines(new OrderLineWS[] { line });

        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        order.setId(api.createOrder(order, orderChanges)); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());


        // generate the invoice with a target billing & due date, "use process date for invoice" is off so
        // order date will be used when calculating invoice due date:
        //
        //      order starts on January 01, 2011
        //      invoiced on January 01, 2011
        //      45 day due date
        //
        // pre paid == invoice immediately, use order start date
        // January 01, 2011 + 45 = February 15, 2011
        Date billingDate = new DateMidnight(2011, 1, 1).toDate();
        Integer[] invoiceIds = api.createInvoiceWithDate(user.getUserId(), billingDate, PeriodUnitDTO.DAY, 45, false);
        assertEquals("1 invoice generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
        assertEquals("due date 45 days from order start date", new DateMidnight(2011, 2, 15).toDate(), invoice.getDueDate());

        // cleanup
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(order.getId());
        api.deleteUser(user.getUserId());
    }

    /**
     *  Test generation of multiple invoices for multiple billing periods
     *
     *  Covers bug #3889
     *
     * @throws Exception
     */
    @Test
    public void testMultiPeriodGenerateInvoice() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        resetBillingConfiguration(); // make sure we're using the same config/dates

        UserWS user = new UserWS();
        user.setUserName("invoice-test-14-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(5);
        user.setAccountTypeId(Integer.valueOf(1));
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        //set main subscription to 15 monthly
        MainSubscriptionWS mainSubscriptionWS = new MainSubscriptionWS(2, 15);
        user.setMainSubscription(mainSubscriptionWS);

        MetaFieldValueWS metaField01 = new MetaFieldValueWS();
        metaField01.setFieldName("contact.email");
        metaField01.setValue(user.getUserName() + "@test.com");
        metaField01.setGroupId(1);

        MetaFieldValueWS metaField02 = new MetaFieldValueWS();
        metaField02.setFieldName("contact.first.name");
        metaField02.setValue("Multi period invoice test");
        metaField02.setGroupId(1);

        MetaFieldValueWS metaField03 = new MetaFieldValueWS();
        metaField03.setFieldName("contact.last.name");
        metaField03.setValue("Pre-paid with due date");
        metaField03.setGroupId(1);

        user.setMetaFields(new MetaFieldValueWS[]{
                metaField01,
                metaField02,
                metaField03
        });

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());

        OrderWS order = new OrderWS();
        order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(2); // monthly
        order.setCurrencyId(1);
        order.setActiveSince(new DateMidnight(2011, 8, 1).toDate()); // active since August 01, 2011
        order.setActiveUntil(new DateMidnight(2011, 9, 1).toDate()); // active until September 01, 2011

        // create an order to be invoiced
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(2602); // lemonade
        line.setUseItem(true);
        line.setQuantity(2);
        order.setOrderLines(new OrderLineWS[] { line });

        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
            change.setEndDate(order.getActiveUntil());
        }

        order.setId(api.createOrder(order, orderChanges)); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());

        // generate the invoice with a target billing date
        //
        //      order starts on August 01, 2011
        //      billing date on July 15, 2011
        //      customer main subscription is 15 monthly
        //
        Date billingDate = new DateMidnight(2011, 7, 15).toDate();
        Integer[] invoiceIds = api.createInvoiceWithDate(user.getUserId(), billingDate, PeriodUnitDTO.DAY, 30, false);
        assertEquals("1 invoice generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
        assertEquals("Invoice line should be filled with the Lemonade product", 1, invoice.getInvoiceLines().length);

        // test the second scenario
        OrderWS secondOrder = new OrderWS();
        secondOrder.setUserId(user.getUserId());
        secondOrder.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        secondOrder.setPeriod(2); // monthly
        secondOrder.setCurrencyId(1);
        secondOrder.setActiveSince(new DateMidnight(2011, 9, 1).toDate()); // active since September 01, 2011

        // create an order to be invoiced
        OrderLineWS secondLine = new OrderLineWS();
        secondLine.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        secondLine.setItemId(2602); // lemonade
        secondLine.setUseItem(true);
        secondLine.setQuantity(5);
        secondOrder.setOrderLines(new OrderLineWS[] { secondLine });

        OrderChangeWS[] secondOrderChanges = OrderChangeBL.buildFromOrder(secondOrder, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: secondOrderChanges) {
            change.setStartDate(secondOrder.getActiveSince());
        }

        secondOrder.setId(api.createOrder(secondOrder, secondOrderChanges)); // create order
        secondOrder = api.getOrder(secondOrder.getId());
        assertNotNull("second order created", secondOrder.getId());

        // generate the invoice with a target billing date
        //
        //      order starts on September 01, 2011
        //      billing date on August 15, 2011
        //      customer main subscription is 15 monthly
        //
        billingDate = new DateMidnight(2011, 8, 15).toDate();
        invoiceIds = api.createInvoiceWithDate(user.getUserId(), billingDate, PeriodUnitDTO.DAY, 30, false);
        assertEquals("1 invoice generated", 1, invoiceIds.length);

        InvoiceWS secondInvoice = api.getInvoiceWS(invoiceIds[0]);
        assertEquals("Invoice line should be filled with the Lemonade product and delegated invoice", 2, secondInvoice.getInvoiceLines().length);

        // cleanup
        api.deleteInvoice(secondInvoice.getId());
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(order.getId());
        api.deleteOrder(secondOrder.getId());
        api.deleteUser(user.getUserId());
    }

    /**
     * Test generation of an invoice with a specific target due date when the customer
     * has multiple orders to be invoiced.
     *
     * This test case covers bug #1487
     *
     * @throws Exception possible api exception
     */
    @Test
    public void testMultiOrderGenerateInvoice() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        resetBillingConfiguration(); // make sure we're using the same config/dates

        UserWS user = new UserWS();
        user.setUserName("invoice-test-04-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(5);
        user.setAccountTypeId(Integer.valueOf(1));
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField01 = new MetaFieldValueWS();
        metaField01.setFieldName("contact.email");
        metaField01.setValue(user.getUserName() + "@test.com");
        metaField01.setGroupId(1);

        MetaFieldValueWS metaField02 = new MetaFieldValueWS();
        metaField02.setFieldName("contact.first.name");
        metaField02.setValue("Invoice Test");
        metaField02.setGroupId(1);

        MetaFieldValueWS metaField03 = new MetaFieldValueWS();
        metaField03.setFieldName("contact.last.name");
        metaField03.setValue("Multi-order with due date");
        metaField03.setGroupId(1);

        user.setMetaFields(new MetaFieldValueWS[]{
                metaField01,
                metaField02,
                metaField03
        });

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());

        // create orders

        // order 1
        OrderWS order = new OrderWS();
    	order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(2); // monthly
        order.setCurrencyId(1);
        order.setActiveSince(new DateMidnight(2011, 1, 1).toDate()); // active since January 01, 2011

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(2602); // lemonade
        line.setUseItem(true);
        line.setQuantity(1);
        order.setOrderLines(new OrderLineWS[] { line });

        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        order.setId(api.createOrder(order, orderChanges)); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());

        // order 2
        OrderWS order2 = new OrderWS();
    	order2.setUserId(user.getUserId());
        order2.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order2.setPeriod(2); // monthly
        order2.setCurrencyId(1);
        order2.setActiveSince(new DateMidnight(2011, 1, 1).toDate()); // active since January 01, 2011

        OrderLineWS line2 = new OrderLineWS();
        line2.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line2.setItemId(2602); // lemonade
        line2.setUseItem(true);
        line2.setQuantity(2);
        order2.setOrderLines(new OrderLineWS[] {line2});

        OrderChangeWS[] order2Changes = OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: order2Changes) {
            change.setStartDate(order2.getActiveSince());
        }

        order2.setId(api.createOrder(order2, order2Changes)); // create order
        order2 = api.getOrder(order2.getId());
        assertNotNull("order created", order2.getId());

        // order 2
        OrderWS order3 = new OrderWS();
    	order3.setUserId(user.getUserId());
        order3.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order3.setPeriod(2); // monthly
        order3.setCurrencyId(1);
        order3.setActiveSince(new DateMidnight(2011, 1, 1).toDate()); // active since January 01, 2011
        //order3.setCycleStarts(new DateMidnight(2011, 1, 1).toDate()); // cycle starts January 01, 2011

        OrderLineWS line3 = new OrderLineWS();
        line3.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line3.setItemId(2602); // lemonade
        line3.setUseItem(true);
        line3.setQuantity(3);
        order3.setOrderLines(new OrderLineWS[] {line3});

        OrderChangeWS[] order3Changes = OrderChangeBL.buildFromOrder(order3, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: order3Changes) {
            change.setStartDate(order3.getActiveSince());
        }

        order3.setId(api.createOrder(order3, order3Changes)); // create order
        order3 = api.getOrder(order3.getId());
        assertNotNull("order created", order3.getId());


        // generate the invoice with a target billing & due date, "use process date for invoice" is off so
        // order date will be used when calculating invoice due date:
        //
        //      all three orders start on January 01, 2011
        //      invoiced on January 01, 2011
        //      45 day due date
        //
        // pre paid == invoice immediately, use order start date
        // January 01, 2011 + 45 = February 15, 2011
        Date billingDate = new DateMidnight(2011, 1, 1).toDate();
        Integer[] invoiceIds = api.createInvoiceWithDate(user.getUserId(), billingDate, PeriodUnitDTO.DAY, 45, true);
        assertEquals("1 invoice generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
        assertEquals("due date 45 days from order start date", new DateMidnight(2011, 2, 15).toDate(), invoice.getDueDate());

        // cleanup
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(order.getId());
        api.deleteOrder(order2.getId());
        api.deleteOrder(order3.getId());
        api.deleteUser(user.getUserId());
    }

    @Test
    public void testGenerateInvoicePartialPeriod() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        resetBillingConfiguration(); // make sure we're using the same config/dates

        UserWS user = new UserWS();
        user.setUserName("invoice-test-05-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(5);
        user.setAccountTypeId(Integer.valueOf(1));
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField01 = new MetaFieldValueWS();
        metaField01.setFieldName("contact.email");
        metaField01.setValue(user.getUserName() + "@test.com");
        metaField01.setGroupId(1);

        MetaFieldValueWS metaField02 = new MetaFieldValueWS();
        metaField02.setFieldName("contact.first.name");
        metaField02.setValue("Invoice Test");
        metaField02.setGroupId(1);

        MetaFieldValueWS metaField03 = new MetaFieldValueWS();
        metaField03.setFieldName("contact.last.name");
        metaField03.setValue("Partial period (pro-rating)");
        metaField03.setGroupId(1);

        user.setMetaFields(new MetaFieldValueWS[]{
                metaField01,
                metaField02,
                metaField03
        });

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());

        OrderWS order = new OrderWS();
    	order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(2); // monthly
        order.setCurrencyId(1);
        order.setActiveSince(new DateMidnight(2011, 1, 20).toDate()); // active since January 20, 2011
        order.setProrateFlag(true);

        // create an order to be invoiced
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(2602); // lemonade
        line.setUseItem(true);
        line.setQuantity(1);
        order.setOrderLines(new OrderLineWS[] { line });

        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        order.setId(api.createOrder(order, orderChanges)); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());


        // generate the invoice with a target billing & due date, "use process date for invoice" is off so
        // order date will be used when calculating invoice due date:
        //
        //      order starts on January 20, 2011
        //      invoiced on January 01, 2011
        //      45 day due date
        //
        // pre paid == invoice immediately, use order start date
        // January 20, 2011 + 45 = March 6, 2011
        Date billingDate = new DateMidnight(2011, 1, 1).toDate();
        Integer[] invoiceIds = api.createInvoiceWithDate(user.getUserId(), billingDate, PeriodUnitDTO.DAY, 45, false);
        assertEquals("1 invoice generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
        assertEquals("due date 45 days from order start date", new DateMidnight(2011, 2, 15).toDate(), invoice.getDueDate());

        // verify that order is pro-rated.
        //
        //      order cycle start on January 20, 2011
        //      invoiced on January 1, 2011
        //
        // expected pro rating between 20th -> 31st of January
        assertEquals("1 invoice line", 1, invoice.getInvoiceLines().length);
        InvoiceLineDTO invoiceLine = invoice.getInvoiceLines()[0];
        assertEquals("invoice is pro-rated", "Lemonade  Period from 01/20/2011 to 01/31/2011", invoiceLine.getDescription());

        // cleanup
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(order.getId());
        api.deleteUser(user.getUserId());
    }

    @Test
    public void testGenerateInvoiceMultiplePartialPeriod() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        resetBillingConfiguration(); // make sure we're using the same config/dates

        UserWS user = new UserWS();
        user.setUserName("invoice-test-06-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(5);
        user.setAccountTypeId(Integer.valueOf(1));
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);
        user.setMainSubscription(com.sapienter.jbilling.server.user.WSTest.createUserMainSubscription());

        MetaFieldValueWS metaField01 = new MetaFieldValueWS();
        metaField01.setFieldName("contact.email");
        metaField01.setValue(user.getUserName() + "@test.com");
        metaField01.setGroupId(1);

        MetaFieldValueWS metaField02 = new MetaFieldValueWS();
        metaField02.setFieldName("contact.first.name");
        metaField02.setValue("Invoice Test");
        metaField02.setGroupId(1);

        MetaFieldValueWS metaField03 = new MetaFieldValueWS();
        metaField03.setFieldName("contact.last.name");
        metaField03.setValue("Multi-order pro-rating");
        metaField03.setGroupId(1);

        user.setMetaFields(new MetaFieldValueWS[]{
                metaField01,
                metaField02,
                metaField03
        });

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());
        
        user = api.getUserWS(user.getUserId());
        user.setNextInvoiceDate(new DateMidnight(2011, 2, 1).toDate());
        api.updateUser(user);

        // order 1
        // this order has a "next invoice date" in the future and WILL NOT BE INVOICED.
        OrderWS order1 = new OrderWS();
    	order1.setUserId(user.getUserId());
        order1.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order1.setPeriod(2); // monthly
        order1.setCurrencyId(1);
        order1.setActiveSince(new DateMidnight(2011, 2, 1).toDate());     // active since February 01, 2011
        order1.setNextBillableDay(new DateMidnight(2011, 3, 1).toDate()); // next invoice March 01, 2011
        order1.setProrateFlag(true);

        // create an order to be invoiced
        OrderLineWS line1 = new OrderLineWS();
        line1.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line1.setItemId(2602); // lemonade
        line1.setUseItem(true);
        line1.setQuantity(1);
        order1.setOrderLines(new OrderLineWS[] {line1});

        OrderChangeWS[] order1Changes = OrderChangeBL.buildFromOrder(order1, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: order1Changes) {
            change.setStartDate(order1.getActiveSince());
        }

        order1.setId(api.createOrder(order1, order1Changes)); // create order
        order1 = api.getOrder(order1.getId());
        assertNotNull("order created", order1.getId());


        // order 2
        OrderWS order2 = new OrderWS();
    	order2.setUserId(user.getUserId());
        order2.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order2.setPeriod(2); // monthly
        order2.setCurrencyId(1);
        order2.setActiveSince(new DateMidnight(2011, 2, 10).toDate());    // active since February 10, 2011
        order2.setActiveUntil(new DateMidnight(2011, 2, 20).toDate());    // active until February 20, 2011
        //order2.setCycleStarts(new DateMidnight(2011, 2, 1).toDate());     // cycle starts February 01, 2011
        order2.setProrateFlag(true);

        // create an order to be invoiced
        OrderLineWS line2 = new OrderLineWS();
        line2.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line2.setItemId(2602); // lemonade
        line2.setUseItem(true);
        line2.setQuantity(1);
        order2.setOrderLines(new OrderLineWS[] {line2});

        OrderChangeWS[] order2Changes = OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: order2Changes) {
            change.setStartDate(order2.getActiveSince());
        }

        order2.setId(api.createOrder(order2, order2Changes)); // create order
        order2 = api.getOrder(order2.getId());
        assertNotNull("order created", order2.getId());


        // order 3
        OrderWS order3 = new OrderWS();
    	order3.setUserId(user.getUserId());
        order3.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order3.setPeriod(2); // monthly
        order3.setCurrencyId(1);
        order3.setActiveSince(new DateMidnight(2011, 2, 20).toDate());    // active since February 10, 2011
        order3.setProrateFlag(true);

        // create an order to be invoiced
        OrderLineWS line3 = new OrderLineWS();
        line3.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line3.setItemId(2602); // lemonade
        line3.setUseItem(true);
        line3.setQuantity(1);
        order3.setOrderLines(new OrderLineWS[] {line3});

        OrderChangeWS[] order3Changes = OrderChangeBL.buildFromOrder(order3, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: order3Changes) {
            change.setStartDate(order3.getActiveSince());
        }

        order3.setId(api.createOrder(order3, order3Changes)); // create order
        order3 = api.getOrder(order3.getId());
        assertNotNull("order created", order3.getId());


        // order 4
        OrderWS order4 = new OrderWS();
    	order4.setUserId(user.getUserId());
        order4.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order4.setPeriod(2); // monthly
        order4.setCurrencyId(1);
        order4.setActiveSince(new DateMidnight(2011, 2, 10).toDate());    // active since February 10, 2011
        order4.setProrateFlag(true);

        // create an order to be invoiced
        OrderLineWS line4 = new OrderLineWS();
        line4.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line4.setItemId(2602); // lemonade
        line4.setUseItem(true);
        line4.setQuantity(1);
        order4.setOrderLines(new OrderLineWS[] {line4});

        OrderChangeWS[] order4Changes = OrderChangeBL.buildFromOrder(order4, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: order4Changes) {
            change.setStartDate(order4.getActiveSince());
        }

        order4.setId(api.createOrder(order4, order4Changes)); // create order
        order4 = api.getOrder(order4.getId());
        assertNotNull("order created", order4.getId());


        // generate the invoice with a target billing & due date, "use process date for invoice" is off so
        // order date will be used when calculating invoice due date:
        //
        //      As per new Billing Process changes Invoice date set as billing run date 
        //      Billing Run date on February 01, 2011
        //      invoiced on February 01, 2011
        //      45 day due date
        //
        // pre paid == invoice immediately, Billing run date
        // Billing run date + 45 = due date
        // February 1, 2011 + 45 = March 18, 2011
        Date billingDate = new DateMidnight(2011, 2, 1).toDate();
        Integer[] invoiceIds = api.createInvoiceWithDate(user.getUserId(), billingDate, PeriodUnitDTO.DAY, 45, false);
        assertEquals("1 invoice generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
        assertEquals("due date 45 days from order start date", new DateMidnight(2011, 3, 18).toDate(), invoice.getDueDate());
        assertEquals("3 orders included", 3, invoice.getOrders().length);
        assertEquals("3 invoice line", 3, invoice.getInvoiceLines().length);

        // verify that the March 3rd order was not invoiced
        for (Integer orderId : invoice.getOrders()) {
            if (orderId.equals(order1.getId()))
                fail("Order 1 should not be invoiced until March 1, 2011");
        }

        // verify pro-rating periods
        boolean period10to28 = false;
        boolean period20to28 = false;
        boolean period10to19 = false;

        for (InvoiceLineDTO invoiceLine : invoice.getInvoiceLines()) {
            if ("Lemonade  Period from 02/10/2011 to 02/28/2011".equals(invoiceLine.getDescription()))
                period10to28 = true;

            if ("Lemonade  Period from 02/20/2011 to 02/28/2011".equals(invoiceLine.getDescription()))
                period20to28 = true;

            if ("Lemonade  Period from 02/10/2011 to 02/19/2011".equals(invoiceLine.getDescription()))
                period10to19 = true;
        }

        assertTrue("Pro-rated between Feb 10 -> 28", period10to28);
        assertTrue("Pro-rated between Feb 20 -> 28", period20to28);
        assertTrue("Pro-rated between Feb 10 -> 19", period10to19);

        // cleanup
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(order4.getId());
        api.deleteUser(user.getUserId());
    }

    /**
     * Resets the billing configuration to the default state found in a fresh
     * load of the testing 'jbilling_test.sql' file.
     *
     * @throws Exception possible api exception
     */
    private void resetBillingConfiguration() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        config.setNextRunDate(new DateMidnight(2006, 10, 26).toDate());
        config.setGenerateReport(1);
        config.setDaysForReport(3);
        config.setRetries(0);
        config.setDaysForRetry(1);
        config.setDueDateValue(1);
        config.setDueDateUnitId(PeriodUnitDTO.MONTH);

        config.setOnlyRecurring(1);
        config.setInvoiceDateProcess(0);
        config.setMaximumPeriods(1);

        api.createUpdateBillingProcessConfiguration(config);

        // reset continuous invoice date
        PreferenceWS continuousDate = new PreferenceWS(new PreferenceTypeWS(ServerConstants.PREFERENCE_CONTINUOUS_DATE), null);
        api.updatePreference(continuousDate);

    }

}
