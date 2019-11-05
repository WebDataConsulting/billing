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

package com.sapienter.jbilling.server.invoice;

import java.math.BigDecimal;
import java.util.*;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.joda.time.DateMidnight;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import com.sapienter.jbilling.server.util.CreateObjectUtil;

/**
 * @author Emil
 */
@Test(groups = { "web-services", "invoice" })
public class WSTest {

	private static Integer PRANCING_PONY_BASIC_ACCOUNT_TYPE = Integer.valueOf(1);
	private static Integer MORDOR_BASIC_ACCOUNT_TYPE = Integer.valueOf(2);

	private static JbillingAPI api = null;
	private static JbillingAPI mordorApi = null;
	private static int ORDER_CHANGE_STATUS_APPLY_ID;
	private static int ORDER_PERIOD_MONTHLY_ID;
	private static int ORDER_PERIOD_ONE_TIME_ID = 1;

	@BeforeClass
	public void setupClass() throws Exception {
		api = JbillingAPIFactory.getAPI();
		mordorApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_MORDOR.name());
		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api).intValue();
		ORDER_PERIOD_MONTHLY_ID = getOrCreateMonthlyOrderPeriod(api).intValue();
	}

    @Test(enabled = false)
    public void test001Get() {
        System.out.println("#test001Get");

	    //create data in Mordor
	    UserWS user = buildUser(MORDOR_BASIC_ACCOUNT_TYPE);
	    user.setId(mordorApi.createUser(user));

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(mordorApi.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), mordorApi.getCallerCompanyId());
	    item.setId(mordorApi.createItem(item));

	    InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
	    invoice.setId(mordorApi.saveLegacyInvoice(invoice));

        // get
        // try getting one that doesn't belong to us
        try {
            System.out.println("Getting invalid invoice");
            api.getInvoiceWS(invoice.getId());
            fail("Invoice belongs to entity 2");
        } catch (Exception e) {
        }

	    // latest
	    // first, from a guy that is not mine
	    try {
		    api.getLatestInvoice(user.getId());
		    fail("User belongs to entity 2");
	    } catch (Exception e) {
	    }

	    // List of last
	    // first, from a guy that is not mine
	    try {
		    api.getLastInvoices(user.getId(), 5);
		    fail("User belongs to entity 2");
	    } catch (Exception e) {
	    }

	    //delete data from Mordor
	    mordorApi.deleteInvoice(invoice.getId());
	    mordorApi.deleteItem(item.getId());
	    mordorApi.deleteItemCategory(itemType.getId());
	    mordorApi.deleteUser(user.getId());

	    System.out.println("Done with Mordor");

	    //setup data in Prancing Pony
	    user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));

	    itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    item = buildItem(itemType.getId(), mordorApi.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    invoice = buildInvoice(user.getId(), item.getId());
	    invoice.setCreateDateTime(newDate(2006, 7, 26));
	    invoice.setId(api.saveLegacyInvoice(invoice));

	    InvoiceWS invoice2 = buildInvoice(user.getId(), item.getId());
	    invoice2.setCreateDateTime(newDate(2006, 8, 26));
	    invoice2.setId(api.saveLegacyInvoice(invoice2));

	    //start testing Prancing Pony
        System.out.println("Getting invoice");
        InvoiceWS retInvoice = api.getInvoiceWS(invoice.getId());
        assertNotNull("invoice not returned", retInvoice);
        assertEquals("invoice id", retInvoice.getId(), new Integer(invoice.getId()));
        System.out.println("Got Invoice With Id= " + retInvoice.getId());

        System.out.println("Getting latest invoice");
        retInvoice = api.getLatestInvoice(user.getId());
        assertNotNull("invoice not returned", retInvoice);
        assertEquals("invoice's user id", user.getId(), retInvoice.getUserId().intValue());
        System.out.println("Got = " + retInvoice);
        Integer lastInvoice = retInvoice.getId();

        System.out.println("Getting last 5 invoices");
        Integer invoices[] = api.getLastInvoices(user.getId(), 5);
        assertNotNull("invoice not returned", invoices);

        retInvoice = api.getInvoiceWS(invoices[0]);
        assertEquals("invoice's user id", user.getId(), retInvoice.getUserId().intValue());
        System.out.println("Got = " + invoices.length + " invoices");
        for (int f = 0; f < invoices.length; f++) {
            System.out.println(" Invoice " + (f + 1) + invoices[f]);
        }

        // now I want just the two latest
        System.out.println("Getting last 2 invoices");
        invoices = api.getLastInvoices(user.getId(), 2);
        assertNotNull("invoice not returned", invoices);
        retInvoice = api.getInvoiceWS(invoices[0]);
        assertEquals("invoice's user id", user.getId(), retInvoice.getUserId().intValue());
        assertEquals("invoice's has to be latest", lastInvoice, retInvoice.getId());
        assertEquals("there should be only 2", 2, invoices.length);

        // get some by date
        System.out.println("Getting by date (empty)");
        Integer invoices2[] = api.getInvoicesByDate("2000-01-01", "2005-01-01");
        // CXF returns null instead of empty arrays
        // assertNotNull("invoice not returned", invoices2);
        if (invoices2 != null) {
            assertTrue("array not empty", invoices2.length == 0);
        }

        System.out.println("Getting by date");
        invoices2 = api.getInvoicesByDate("2006-01-01", "2007-01-01");
        assertNotNull("invoice not returned", invoices2);
        assertFalse("array not empty", invoices2.length == 0);
        System.out.println("Got array with size:" + invoices2.length);
        retInvoice = api.getInvoiceWS(invoices2[0]);
        assertNotNull("invoice not there", retInvoice);
        System.out.println("Got invoice " + retInvoice);

	    //cleanup
	    api.deleteInvoice(invoice.getId());
	    api.deleteInvoice(invoice2.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());

        System.out.println("Done!");
    }

    @Test
    public void test002Delete() {
        System.out.println("#test002Delete");

	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
		invoice.setId(api.saveLegacyInvoice(invoice));

		assertNotNull("Invoice ID should not be Null", invoice.getId());
        assertNotNull("Invoice should not be Null:", api.getInvoiceWS(invoice.getId()));

        api.deleteInvoice(invoice.getId());
        try {
            api.getInvoiceWS(invoice.getId());
            fail("Invoice should not have been deleted");
        } catch(Exception e) {
            //ok
        }

		//cleanup
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());

	    /*creates an invoice for Mordor company and tries to delete it with PrancingPony api */

	    user = buildUser(MORDOR_BASIC_ACCOUNT_TYPE);
	    user.setId(mordorApi.createUser(user));

	    itemType = buildItemType();
	    itemType.setId(mordorApi.createItemCategory(itemType));

	    item = buildItem(itemType.getId(), mordorApi.getCallerCompanyId());
	    item.setId(mordorApi.createItem(item));

	    invoice = buildInvoice(user.getId(), item.getId());
	    invoice.setId(mordorApi.saveLegacyInvoice(invoice));

	    assertNotNull("Invoice ID should not be Null", invoice.getId());
	    assertNotNull("Invoice should not be Null:", mordorApi.getInvoiceWS(invoice.getId()));

        // try to delete an invoice that is not mine
        try {
            api.deleteInvoice(invoice.getId());
            fail("Not my invoice. It should not have been deleted");
        } catch(Exception e) {
            //ok
        }

	    //cleanup Mordor data
	    mordorApi.deleteInvoice(invoice.getId());
	    mordorApi.deleteItem(item.getId());
	    mordorApi.deleteItemCategory(itemType.getId());
	    mordorApi.deleteUser(user.getId());
    }

    @Test
    public void test003CreateInvoice() {
        System.out.println("#test003CreateInvoice");

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
        final Integer userId = api.createUser(user);

        //Update Next invoice date and billing cycle period.
        user = api.getUserWS(userId);
	    user.setPassword(null);
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_MONTH);
        user.setMainSubscription(createUserMainSubscription(day));
        user.setPassword(null);
        
        api.updateUser(user);
        user = api.getUserWS(userId);
	    user.setPassword(null);
        user.setNextInvoiceDate(new Date());
        user.setPassword(null);
        api.updateUser(user);

        // setup order
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());
        order.setProrateFlag(false);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(new BigDecimal("10.00"));
        line.setAmount(new BigDecimal("10.00"));

        order.setOrderLines(new OrderLineWS[] { line });


        //  Test invoicing of one-time and recurring orders


        // create 1st order
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // create 2nd order
        line.setPrice(new BigDecimal("20.00"));
        line.setAmount(new BigDecimal("20.00"));
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // create invoice
        Integer[] invoices = api.createInvoice(userId, false);

        assertEquals("Number of invoices returned", 1, invoices.length);
        InvoiceWS invoice = api.getInvoiceWS(invoices[0]);

        assertNull("Invoice is not delegated.", invoice.getDelegatedInvoiceId());
        assertEquals("Invoice does not have a carried balance.", BigDecimal.ZERO, invoice.getCarriedBalanceAsDecimal());

        Integer[] invoicedOrderIds = invoice.getOrders();
        assertEquals("Number of orders invoiced", 2, invoicedOrderIds.length);
        Arrays.sort(invoicedOrderIds);
        assertEquals("Order 1 invoiced", orderId1, invoicedOrderIds[0]);
        assertEquals("Order 2 invoiced", orderId2, invoicedOrderIds[1]);
        assertEquals("Total is 30.0", new BigDecimal("30.00"), invoice.getTotalAsDecimal());

        // clean up
        api.deleteInvoice(invoices[0]);
        api.deleteOrder(orderId1);
        api.deleteOrder(orderId2);

        //  Test only recurring order can generate invoice.

        // one-time order
        line.setPrice(new BigDecimal("2.00"));
        line.setAmount(new BigDecimal("2.00"));
        orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // try to create invoice, but none should be returned
        invoices = api.createInvoice(userId, true);

        // Note: CXF returns null for empty array
        if (invoices != null) {
            assertEquals("Number of invoices returned", 0, invoices.length);
        }

        // recurring order
        order.setPeriod(ORDER_PERIOD_MONTHLY_ID); // monthly
        line.setPrice(new BigDecimal("3.00"));
        line.setAmount(new BigDecimal("3.00"));
        orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // create invoice
        invoices = api.createInvoice(userId, true);

        assertEquals("Number of invoices returned", 1, invoices.length);
        invoice = api.getInvoiceWS(invoices[0]);
        invoicedOrderIds = invoice.getOrders();
        assertEquals("Number of orders invoiced", 2, invoicedOrderIds.length);
        Arrays.sort(invoicedOrderIds);
        assertEquals("Order 1 invoiced", orderId1, invoicedOrderIds[0]);
        assertEquals("Order 2 invoiced", orderId2, invoicedOrderIds[1]);
        assertEquals("Total is 5.0", new BigDecimal("5.00"), invoice.getTotalAsDecimal());

        // clean up
        api.deleteInvoice(invoices[0]);
        api.deleteOrder(orderId1);
        api.deleteOrder(orderId2);

	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(userId);
    }

    @Test
    public void test004CreateInvoiceFromOrder() {
        System.out.println("#test004CreateInvoiceFromOrder");

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    final Integer userId = api.createUser(user);

        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(new BigDecimal("10.00"));
        line.setAmount(new BigDecimal("10.00"));

        order.setOrderLines(new OrderLineWS[] { line });

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Order 2 created", orderId2);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);        

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total is $10.00", new BigDecimal("10.00"), invoice.getTotalAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        // add second order to invoice
        Integer invoiceId2 = api.createInvoiceFromOrder(orderId2, invoiceId);
        assertEquals("Order added to the same invoice", invoiceId, invoiceId2);

        invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Still only 1 invoice generated", 1, invoiceIds.length);

        invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total is $20.00", new BigDecimal("20.00"), invoice.getTotalAsDecimal());
        assertEquals("2 orders invoiced", 2, invoice.getOrders().length);

        // cleanup
        api.deleteInvoice(invoiceId);
        api.deleteOrder(orderId1);
        api.deleteOrder(orderId2);

	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(userId);
    }

    @Test
    public void test005CreateInvoiceSecurity() {
        System.out.println("#test005CreateInvoiceSecurity");

	    //create mordor user
	    UserWS user = buildUser(MORDOR_BASIC_ACCOUNT_TYPE);
	    user.setId(mordorApi.createUser(user));

        try {
            api.createInvoice(user.getId(), false);
            fail("User belongs to entity 2");
        } catch (SessionInternalError e) {
        }

	    //cleanup
	    mordorApi.deleteUser(user.getUserId());
    }


    /**
     * Tests that when a past due invoice is processed it will generate a new invoice for the
     * current period that contains all previously un-paid balances as the carried balance.
     *
     * Invoices that have been carried still show the original balance for reporting/paper-trail
     * purposes, but will not be re-processed by the system as part of the normal billing process.
     *
     * @throws Exception
     */
    @Test
    public void test006CreateWithCarryOver() {
        System.out.println("#test006CreateWithCarryOver");

	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    InvoiceWS overDueInvoice = buildInvoice(user.getId(), item.getId());
	    overDueInvoice.setDueDate(newDate(2007, 8, 26));
	    overDueInvoice.setCreateDateTime(newDate(2007, 7, 26));
	    overDueInvoice.setCreateTimeStamp(newDate(2007, 7, 26));
	    overDueInvoice.setId(api.saveLegacyInvoice(overDueInvoice));

        final Integer USER_ID = user.getId();          // user has one past-due invoice to be carried forward
        final Integer OVERDUE_INVOICE_ID = overDueInvoice.getId();  // holds a $20 balance

        //Update Next invoice date and billing cycle period.
        user = api.getUserWS(USER_ID);
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_MONTH);
        user.setMainSubscription(createUserMainSubscription(day));
        user.setPassword(null);
        api.updateUser(user);
        user = api.getUserWS(USER_ID);
	    user.setPassword(null);
        user.setNextInvoiceDate(new Date());
        user.setPassword(null);
        api.updateUser(user);
        
        // new order with a single line item
        OrderWS order = new OrderWS();
        order.setUserId(USER_ID);
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());
        order.setProrateFlag(false);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(new BigDecimal("10.00"));
        line.setAmount(new BigDecimal("10.00"));

        order.setOrderLines(new OrderLineWS[] { line });

        // create order
        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // create invoice
        Integer invoiceId = api.createInvoice(USER_ID, false)[0];

        // validate that the overdue invoice has been carried forward to the newly created invoice
        InvoiceWS overdue = api.getInvoiceWS(OVERDUE_INVOICE_ID);

        assertEquals("Status updated to 'unpaid and carried'",
                     ServerConstants.INVOICE_STATUS_UNPAID_AND_CARRIED, overdue.getStatusId());
        assertEquals("Carried invoice will not be re-processed",
                     0, overdue.getToProcess().intValue());
        assertEquals("Overdue invoice holds original balance",
                     new BigDecimal("20.00"), overdue.getBalanceAsDecimal());

        assertEquals("Overdue invoice delegated to the newly created invoice",
                     invoiceId, overdue.getDelegatedInvoiceId());

        // validate that the newly created invoice contains the carried balance
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);

        assertEquals("New invoice balance is equal to the current period charges",
                     new BigDecimal("10.00"), invoice.getBalanceAsDecimal());
        assertEquals("New invoice holds the carried balance equal to the old invoice balance",
                     overdue.getBalanceAsDecimal(), invoice.getCarriedBalanceAsDecimal());
        assertEquals("New invoice total is equal to the current charges plus the carried total",
                     new BigDecimal("30.00"), invoice.getTotalAsDecimal());

	    //cleanup
	    api.deleteInvoice(invoice.getId());
	    api.deleteInvoice(overDueInvoice.getId());
	    api.deleteOrder(orderId);
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }

    @Test
    public void test007GetUserInvoicesByDate() {
        System.out.println("#test007GetUserInvoicesByDate");

	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    InvoiceWS invoiceOne = buildInvoice(user.getId(), item.getId());
	    invoiceOne.setCreateDateTime(newDate(2006, 7, 26));
	    invoiceOne.setId(api.saveLegacyInvoice(invoiceOne));

	    InvoiceWS invoiceTwo = buildInvoice(user.getId(), item.getId());
	    invoiceTwo.setCreateDateTime(newDate(2006, 7, 27));
	    invoiceTwo.setId(api.saveLegacyInvoice(invoiceTwo));

	    InvoiceWS invoiceThree = buildInvoice(user.getId(), item.getId());
	    invoiceThree.setCreateDateTime(newDate(2006, 7, 28));
	    invoiceThree.setId(api.saveLegacyInvoice(invoiceThree));

        // invoice dates: 2006-07-26
        // select the week
        Integer[] result = api.getUserInvoicesByDate(user.getId(), "2006-07-23", "2006-07-29");
        // note: invoice 1 gets deleted
        assertEquals("Number of invoices returned", 3, result.length);
        assertEquals("Invoice Three", invoiceThree.getId().intValue(),  result[0].intValue());
        assertEquals("Invoice Two",   invoiceTwo.getId().intValue(),    result[1].intValue());
        assertEquals("Invoice One",   invoiceOne.getId().intValue(),    result[2].intValue());

        // test since date inclusive
        result = api.getUserInvoicesByDate(user.getId(), "2006-07-26", "2006-07-29");
        assertEquals("Number of invoices returned", 3, result.length);
	    assertEquals("Invoice Three", invoiceThree.getId().intValue(),  result[0].intValue());
	    assertEquals("Invoice Two",   invoiceTwo.getId().intValue(),    result[1].intValue());
	    assertEquals("Invoice One",   invoiceOne.getId().intValue(),    result[2].intValue());

        // test until date inclusive
        result = api.getUserInvoicesByDate(user.getId(), "2006-07-23", "2006-07-28");
        assertEquals("Number of invoices returned", 3, result.length);
	    assertEquals("Invoice Three", invoiceThree.getId().intValue(),  result[0].intValue());
	    assertEquals("Invoice Two",   invoiceTwo.getId().intValue(),    result[1].intValue());
	    assertEquals("Invoice One",   invoiceOne.getId().intValue(),    result[2].intValue());

	    result = api.getUserInvoicesByDate(user.getId(), "2006-07-27", "2006-07-28");
	    assertEquals("Number of invoices returned", 2, result.length);
	    assertEquals("Invoice Three", invoiceThree.getId().intValue(),  result[0].intValue());
	    assertEquals("Invoice Two",   invoiceTwo.getId().intValue(),    result[1].intValue());

        // test date with no invoices
        result = api.getUserInvoicesByDate(user.getId(), "2005-07-23", "2005-07-29");
        // Note: CXF returns null for empty array
        if (result != null) {
            assertEquals("Number of invoices returned", 0, result.length);
        }

	    //clean up
	    api.deleteInvoice(invoiceThree.getId());
	    api.deleteInvoice(invoiceTwo.getId());
	    api.deleteInvoice(invoiceOne.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }

    @Test
    public void test008GetTotalAsDecimal() {
        System.out.println("#test008GetTotalAsDecimal");

	    //prepare
        List<Integer> invoiceIds = new ArrayList<Integer>();
        List<Integer> orderIds = new ArrayList<Integer>();
	    List<Integer> itemIds = new ArrayList<Integer>();
	    UserWS user = null;
	    ItemTypeWS itemType = null;

        itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        itemIds.add(api.createItem(buildItem(itemType.getId(), api.getCallerCompanyId())));
        itemIds.add(api.createItem(buildItem(itemType.getId(), api.getCallerCompanyId())));
        itemIds.add(api.createItem(buildItem(itemType.getId(), api.getCallerCompanyId())));

        user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
        user.setId(api.createUser(user));

        // test BigDecimal behavior
        assertFalse(new BigDecimal("1.1").equals(new BigDecimal("1.10")));
        assertTrue(new BigDecimal("1.1").compareTo(new BigDecimal("1.10")) == 0);

        OrderWS order = createMockOrder(user.getId(), itemIds, new BigDecimal("0.32"));

        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        orderIds.add(api.createOrder(order, orderChanges));
        order = createMockOrder(user.getId(), itemIds, new BigDecimal("0.32"));

        orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        orderIds.add(api.createOrder(order, orderChanges));

        invoiceIds.addAll(Arrays.asList(api.createInvoice(user.getId(), false)));
        assertEquals("There should be one invoice created", 1, invoiceIds.size());

        InvoiceWS invoice = api.getInvoiceWS(invoiceIds.get(0));
        assertEquals("1.9200000000", invoice.getTotal());
        assertEquals(new BigDecimal("1.920"), invoice.getTotalAsDecimal());

	    //cleanup
        for (Integer integer : invoiceIds) {
            api.deleteInvoice(integer);
        }
        System.out.println("Successfully deleted invoices: " + invoiceIds.size());
        for (Integer integer : orderIds) {
            api.deleteOrder(integer);
        }
        System.out.println("Successfully deleted orders: " + orderIds.size());
        for(Integer itemId : itemIds){
            api.deleteItem(itemId);
        }
        System.out.println("Successfully deleted items: " + itemIds.size());
        api.deleteItemCategory(itemType.getId());
        api.deleteUser(user.getId());
    }

    @Test
    public void test009GetPaperInvoicePDF() {
        System.out.println("#test009GetPaperInvoicePDF");

	    //setup
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
	    invoice.setId(api.saveLegacyInvoice(invoice));

	    //test
        Integer[] invoiceIds = api.getLastInvoices(user.getId(), 1);
        assertEquals("Invoice found for user", 1, invoiceIds.length);

        byte[] pdf = api.getPaperInvoicePDF(invoiceIds[0]);
        assertTrue("PDF invoice bytes returned", pdf.length > 0);

	    //cleanup
	    api.deleteInvoice(invoice.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }
    
    @Test
    public void test010OverDueInvoices() {
        System.out.println("#test010OverDueInvoices");

	    final Integer ACTIVE = Integer.valueOf(1);
	    final Integer OVERDUE = Integer.valueOf(2);

	    Date date = new Date();
	    date.setDate(date.getDate()+80);

	    {
	    UserWS userOne = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    userOne.setId(api.createUser(userOne));

		userOne = api.getUserWS(userOne.getId());


        //setting user status as ACTIVE
		userOne.setStatusId(ACTIVE);
        userOne.setPassword(null);
        api.updateUser(userOne);
        System.out.println("user initial status : "+api.getUserWS(userOne.getId()).getStatus());

        //creating order having balance less than min balance to ignore ageing i.e, 0.00
        OrderWS order = setUpOrder(userOne.getId(), new BigDecimal("-2.123450"));
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Order 1 created", orderId1);
        System.out.println("Order 1 created"+orderId1);
        Integer invoiceId1 = api.createInvoiceFromOrder(orderId1, null);
        assertNotNull("Invoice created", invoiceId1);
        System.out.println("Invoice created"+invoiceId1);
        api.triggerAgeing(date);
        System.out.println("user status : "+api.getUserWS(userOne.getId()).getStatus());
        //checking if user status is ACTIVE
        assertEquals("Expected ACTIVE user", ACTIVE, api.getUserWS(userOne.getId()).getStatusId());

		//cleanup
	    api.deleteInvoice(invoiceId1);
	    api.deleteOrder(orderId1);
	    api.deleteUser(userOne.getId());
	    }

	    {
		UserWS userTwo = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
		userTwo.setId(api.createUser(userTwo));

		userTwo = api.getUserWS(userTwo.getId());

        //setting user status as ACTIVE
		userTwo.setStatusId(ACTIVE);
        api.updateUser(userTwo);
        System.out.println("user initial status : "+api.getUserWS(userTwo.getId()).getStatus());

        //creating order having balance equal to  min balalance to ignore ageing i.e, 0.00
        OrderWS order2 = setUpOrder(userTwo.getId(), new BigDecimal("0.00"));
        Integer orderId2 = api.createOrder(order2, OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Order 2 created", orderId2);
        System.out.println("Order 2 created"+orderId2);
        Integer invoiceId2 = api.createInvoiceFromOrder(orderId2, null);
        assertNotNull("Invoice created", invoiceId2);
        System.out.println("Invoice created"+invoiceId2);
        api.triggerAgeing(date);
        System.out.println("user status : "+api.getUserWS(userTwo.getId()).getStatus());
        //checking if user status is ACTIVE
        assertEquals("Expected ACTIVE user", ACTIVE, api.getUserWS(userTwo.getId()).getStatusId());

	    api.deleteInvoice(invoiceId2);
	    api.deleteOrder(orderId2);
	    api.deleteUser(userTwo.getId());
	    }

	    {
		UserWS userThree = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
		userThree.setId(api.createUser(userThree));

		userThree = api.getUserWS(userThree.getId());

        //setting user status as ACTIVE
		userThree.setStatusId(ACTIVE);
        api.updateUser(userThree);
        System.out.println("user status again changed to : " + api.getUserWS(userThree.getId()).getStatus());

        //creating order having balance more than min balalance to ignore ageing i.e, 0.00
        OrderWS order3 = setUpOrder(userThree.getId(), new BigDecimal("21.00"));
        Integer orderId3 = api.createOrder(order3, OrderChangeBL.buildFromOrder(order3, ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Order 3 created", orderId3);
        System.out.println("Order 3 created"+orderId3);
        Integer invoiceId3 = api.createInvoiceFromOrder(orderId3, null);
        assertNotNull("Invoice created", invoiceId3);
        System.out.println("Invoice created"+invoiceId3);
        api.triggerAgeing(date);
        System.out.println("user status : "+api.getUserWS(userThree.getId()).getStatus());
        //checking if user status is OVERDUE
        assertEquals("Expected OVERDUE user", OVERDUE, api.getUserWS(userThree.getId()).getStatusId());

		//cleanup
	    api.deleteInvoice(invoiceId3);
	    api.deleteOrder(orderId3);
	    api.deleteUser(userThree.getId());
	    }
    }

    @Test
    public void test011CreditGeneratedFromNegativeInvoice() throws Exception {
        System.out.println("#test011CreditGeneratedFromNegativeInvoice");

        // user for tests
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
        Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(new BigDecimal("-100.00"));
        line.setAmount(new BigDecimal("-100.00"));

        order.setOrderLines(new OrderLineWS[] { line });

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $0.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        //Validate that a credit payment should have been created.
        PaymentWS paymentWS =  api.getLatestPayment(userId);
        assertEquals("The credit payment amount should be $100.00", new BigDecimal("100.00"), paymentWS.getAmountAsDecimal());
        assertEquals("Payment method should be credit.", ServerConstants.PAYMENT_METHOD_CREDIT, paymentWS.getMethodId());

        // cleanup
        api.deleteInvoice(invoiceId);
        api.deleteOrder(orderId1);
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
        api.deleteUser(userId);
    }

    @Test
    public void test012SaveLegacyInvoice() throws Exception {
        System.out.println("#test012SaveLegacyInvoice");

	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
	    Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

        InvoiceWS invoiceWS = new InvoiceWS();
        invoiceWS.setUserId(userId);
        invoiceWS.setNumber("800");
        invoiceWS.setTotal("4500");
        invoiceWS.setToProcess(1);
        invoiceWS.setBalance("4500");
        invoiceWS.setCurrencyId(1);
        invoiceWS.setDueDate(new Date());
        invoiceWS.setPaymentAttempts(2);
        invoiceWS.setInProcessPayment(1);
        invoiceWS.setCarriedBalance("0");

        InvoiceLineDTO invoiceLineDTO = new InvoiceLineDTO();
        invoiceLineDTO.setAmount("4500");
        invoiceLineDTO.setDescription("line desc");
        invoiceLineDTO.setItemId(1);
        invoiceLineDTO.setPercentage(1);
        invoiceLineDTO.setPrice("4500");
        invoiceLineDTO.setQuantity("1");
        invoiceLineDTO.setSourceUserId(userId);

        invoiceWS.setInvoiceLines(new InvoiceLineDTO[] {invoiceLineDTO});

        invoiceWS.setId(api.saveLegacyInvoice(invoiceWS));

        InvoiceWS lastInvoiceWS = api.getLatestInvoice(userId);

        assertNotNull(lastInvoiceWS);
        assertTrue(lastInvoiceWS.getNumber().equals(invoiceWS.getNumber()));
        assertEquals(lastInvoiceWS.getTotalAsDecimal(), invoiceWS.getTotalAsDecimal());
        assertEquals(lastInvoiceWS.getBalanceAsDecimal(), invoiceWS.getBalanceAsDecimal());
        assertTrue(lastInvoiceWS.getCurrencyId().equals(invoiceWS.getCurrencyId()));
        assertTrue("This invoice is migrated from legacy system.".equals(lastInvoiceWS.getCustomerNotes()));

        InvoiceLineDTO lastInvoiceLineDTO = lastInvoiceWS.getInvoiceLines()[0];
        assertNotNull(lastInvoiceLineDTO);
        assertEquals(lastInvoiceLineDTO.getAmountAsDecimal(), invoiceLineDTO.getAmountAsDecimal());
        assertTrue(lastInvoiceLineDTO.getDescription().equals(invoiceLineDTO.getDescription()));
        assertTrue(lastInvoiceLineDTO.getItemId().equals(invoiceLineDTO.getItemId()));
        assertEquals(lastInvoiceLineDTO.getPriceAsDecimal(), invoiceLineDTO.getPriceAsDecimal());
        assertEquals(lastInvoiceLineDTO.getQuantityAsDecimal(), invoiceLineDTO.getQuantityAsDecimal());
        assertTrue(lastInvoiceLineDTO.getSourceUserId().equals(invoiceLineDTO.getSourceUserId()));

	    api.deleteInvoice(invoiceWS.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }

	private OrderWS createMockOrder(int userId, List<Integer> items, BigDecimal linePrice) {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
		order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
		order.setCurrencyId(1);
		order.setActiveSince(new Date());
		order.setProrateFlag(Boolean.FALSE);

		ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(items.size());
		for (int i = 0; i < items.size(); i++){
			OrderLineWS nextLine = new OrderLineWS();
			nextLine.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
			nextLine.setDescription("Order line: " + i);
			nextLine.setItemId(items.get(i));
			nextLine.setQuantity(1);
			nextLine.setPrice(linePrice);
			nextLine.setAmount(nextLine.getQuantityAsDecimal().multiply(linePrice));

			lines.add(nextLine);
		}
		order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
		return order;
	}

    private OrderWS setUpOrder(Integer userId,BigDecimal price){
        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        Date date = new Date();
        date.setDate(date.getDate()-20);
        order.setActiveSince(date);

        //setup orderLines
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(1);
        line.setQuantity(1);
        line.setPrice(price);
        line.setAmount(price);

        order.setOrderLines(new OrderLineWS[] { line });

        return order;
    }

	public static UserWS buildUser(Integer accountTypeId) {
		UserWS newUser = new UserWS();
		newUser.setUserId(0);
		newUser.setUserName("testInvoiceUser-" + System.currentTimeMillis());
		newUser.setPassword("Admin123@");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(accountTypeId);
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(Integer.valueOf(1));
		
		newUser.setInvoiceChild(false);

		MetaFieldValueWS metaField3 = new MetaFieldValueWS();
		metaField3.setFieldName("contact.email");
		metaField3.setValue(newUser.getUserName() + "@shire.com");
		metaField3.setGroupId(accountTypeId);

		MetaFieldValueWS metaField4 = new MetaFieldValueWS();
		metaField4.setFieldName("contact.first.name");
		metaField4.setValue("Frodo");
		metaField4.setGroupId(accountTypeId);

		MetaFieldValueWS metaField5 = new MetaFieldValueWS();
		metaField5.setFieldName("contact.last.name");
		metaField5.setValue("Baggins");
		metaField5.setGroupId(accountTypeId);

		newUser.setMetaFields(new MetaFieldValueWS[] { metaField3, metaField4, metaField5 });
		return newUser;
	}

	private InvoiceWS buildInvoice(Integer userId, Integer itemId) {
		InvoiceWS invoice = new InvoiceWS();
		invoice.setUserId(userId);
		invoice.setNumber("800");
		invoice.setTotal("20");
		invoice.setToProcess(1);
		invoice.setBalance("20");
		invoice.setCurrencyId(1);
		invoice.setDueDate(new Date());
		invoice.setPaymentAttempts(1);
		invoice.setInProcessPayment(1);
		invoice.setCarriedBalance("0");

		InvoiceLineDTO invoiceLineDTO = new InvoiceLineDTO();
		invoiceLineDTO.setAmount("20");
		invoiceLineDTO.setDescription("line desc");
		invoiceLineDTO.setItemId(itemId);
		invoiceLineDTO.setPercentage(1);
		invoiceLineDTO.setPrice("20");
		invoiceLineDTO.setQuantity("1");
		invoiceLineDTO.setSourceUserId(userId);

		invoice.setInvoiceLines(new InvoiceLineDTO[]{invoiceLineDTO});
		return invoice;
	}

	private ItemTypeWS buildItemType() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("Invoice, Item Type:" + System.currentTimeMillis());
		type.setOrderLineTypeId(1);//items
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}

	private ItemDTOEx buildItem(Integer itemTypeId, Integer priceModelCompanyId){
		ItemDTOEx item = new ItemDTOEx();
		long millis = System.currentTimeMillis();
		String name = String.valueOf(millis) + new Random().nextInt(10000);
		item.setDescription("Invoice, Product:" + name);
		item.setPriceModelCompanyId(priceModelCompanyId);
		item.setPriceManual(0);
		item.setPrices(CreateObjectUtil.setItemPrice(new BigDecimal("10.00"), new DateMidnight(1970, 1, 1).toDate(), priceModelCompanyId, Integer.valueOf(1)));
		item.setNumber("INV-PRD-"+name);
		item.setAssetManagementEnabled(0);
		Integer typeIds[] = new Integer[] {itemTypeId};
		item.setTypes(typeIds);
		return item;
	}

    public static MainSubscriptionWS createUserMainSubscription(int day) {
    	MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
    	mainSubscription.setPeriodId(ORDER_PERIOD_MONTHLY_ID); //monthly
    	mainSubscription.setNextInvoiceDayOfPeriod(day); // 1st of the month
    	return mainSubscription;
    }

	private static Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
		OrderChangeStatusWS[] statuses = api.getOrderChangeStatusesForCompany();
		for (OrderChangeStatusWS status : statuses) {
			if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
				return status.getId();
			}
		}
		//there is no APPLY status in db so create one
		OrderChangeStatusWS apply = new OrderChangeStatusWS();
		String status1Name = "APPLY: " + System.currentTimeMillis();
		OrderChangeStatusWS status1 = new OrderChangeStatusWS();
		status1.setApplyToOrder(ApplyToOrder.YES);
		status1.setDeleted(0);
		status1.setOrder(1);
		status1.addDescription(new InternationalDescriptionWS(ServerConstants.LANGUAGE_ENGLISH_ID, status1Name));
		return api.createOrderChangeStatus(apply);
	}

	private static Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
		OrderPeriodWS[] periods = api.getOrderPeriods();
		for(OrderPeriodWS period : periods){
			if(1 == period.getValue().intValue() &&
					PeriodUnitDTO.MONTH == period.getPeriodUnitId().intValue()){
				return period.getId();
			}
		}
		//there is no monthly order period so create one
		OrderPeriodWS monthly = new OrderPeriodWS();
		monthly.setEntityId(api.getCallerCompanyId());
		monthly.setPeriodUnitId(1);//monthly
		monthly.setValue(1);
		monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(ServerConstants.LANGUAGE_ENGLISH_ID, "INV:MONTHLY")));
		return api.createOrderPeriod(monthly);
	}

	private static Date newDate(int year, int month, int day){
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month-1, day);
		return cal.getTime();
	}
}
