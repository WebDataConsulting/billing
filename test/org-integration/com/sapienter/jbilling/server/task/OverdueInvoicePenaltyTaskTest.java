/*
 * jBilling - The Enterprise Open Source Billing System
 * Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde
 * 
 * This file is part of jbilling.
 * 
 * jbilling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jbilling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */

package com.sapienter.jbilling.server.task;

import java.lang.Exception;
import java.lang.Integer;
import java.lang.System;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
//import com.sapienter.jbilling.server.entity.PaymentInfoChequeDTO;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.user.db.UserDTO;

import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import junit.framework.TestCase;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.payment.PaymentWS;

/**
 * A jUnit test class to test OverdueInvoicePenaltyTask plug-in.
 * 
 * @author Vikas Bodani
 * @since 06-Jun-2012
 * 
 */
public class OverdueInvoicePenaltyTaskTest extends TestCase {


    private static final Integer OVERDUE_INVOICE_PENALTY_TASK_TYPE_ID = 97;

    private final static String PLUGIN_PARAM_ITEM_ID = "tax_item";

    private static final Integer LEMONADE_ITEM_ID = 1100; // taxable item

    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;

    /**
     * Test that an Overdue Invoice (Unpaid Invoice) causes a new Penalty Order to be created for that user,
     * just prior to the next billing run. The penalty order is created using a Penalty Category Item
     * @throws Exception
     */

    /**
     * Test Case -1
     * @throws Exception
     */
    public void testOverdueInvoice() throws Exception {

        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer pluginId = 0;
        boolean isPluginDeleted = false;

        // create a user for testing
        UserWS user = new UserWS();
        user.setUserName("invoice-penaly-01-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(5);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        ContactWS contact = new ContactWS();
        contact.setEmail("test@test.com");
        contact.setFirstName("Rubal");
        contact.setLastName("Sharma");
        user.setContact(contact);

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());

        // purchase order with taxable items
        OrderWS order = new OrderWS();
        order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
        order.setPeriod(1);
        order.setCurrencyId(1);
//        order.setActiveSince(new Date());
        Calendar cal = Calendar.getInstance();
        // I want to set the active since to 07 June 2012 , so the billing process sees it and invoices it
        // set the calendar to 06/07
        cal.set(2010, 5, 7);
        // now set the date given in the Calendar
        order.setActiveSince(cal.getTime());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(LEMONADE_ITEM_ID);
        line.setUseItem(true);
        line.setQuantity(10);
        order.setOrderLines(new OrderLineWS[] { line });
        order.setDueDateUnitId(PeriodUnitDTO.DAY);
        order.setDueDateValue(0);//order due

        order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID))); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());

        // update the billing process
        BillingProcessConfigurationWS billingConfig = api.getBillingProcessConfiguration();

        // set it to 06/08
        cal.set(2010, 5, 8);
        billingConfig.setNextRunDate(cal.getTime());
        // config should not require only recurring orders
        billingConfig.setOnlyRecurring(0);
        // do not generate report
        billingConfig.setGenerateReport(0);
        // month
        // update the billing process configuration
        Integer result = api.createUpdateBillingProcessConfiguration(billingConfig);
        // now trigger the billing, and the invoice should be made of the above order
        // trigger the billing on 1st June 2012
        api.triggerBillingAsync(cal.getTime());

        // Now since the invoice was overdue, the task should run and we should get another penalty order for this customer
        // first pause
        pause(5000);
        // now check the orders of the customer
        OrderWS latestOrder = api.getLatestOrder(user.getUserId());
        // check out its amount
        System.out.println("The latest order's amount is "+latestOrder.getTotalAsDecimal());

        // again trigger the billing, this time we should have a different order
        billingConfig = api.getBillingProcessConfiguration();
        // 06/09
        cal.set(2010, 5, 9);
        billingConfig.setNextRunDate(cal.getTime());
//        billingConfig.setOnlyRecurring(0);
//        billingConfig.setInvoiceDateProcess(1);
        pause(5000);
        api.createUpdateBillingProcessConfiguration(billingConfig);
//         pause(5000);
        // now bill
        api.triggerBillingAsync(cal.getTime());

        pause(5000);
        pause(5000);
        // now see the orders

        // get the last 2 first
        Integer ids[] =   api.getLastOrders(user.getUserId(),2);
        // The second last
        latestOrder = api.getOrder(ids[0]);
        System.out.println("The id being checked is >> "+ids[0]);
        System.out.println("The id being not checked is >> "+ids[1]);
        System.out.println("The latest order's amount is "+latestOrder.getTotalAsDecimal());
         // again get the user
         user = api.getUserWS(user.getUserId());
        // the latest order should have the item containing the above id
        assertEquals("latest order amount should be 1.5 % of 100 = 1.5$", new BigDecimal("1.500"), latestOrder.getTotalAsDecimal());

        }


    /**
     * When an Unpaid, Invoice is paid, any Penalty Order created from OverdueInvoicePenaltyTask
     * must be updated to set an activeUntil date equal to the same value as the Payment Date.
     *
     * This is the test that activeUntil Date of such an order should not be null
     * and also equal to the Payment Date
     * @throws Exception
     */
    // test case -2

    public void testInvoicePayment() throws Exception {

        // LET TEST CASE 1 RUN, wait 30 sec
        pause(30000);
        JbillingAPI api = JbillingAPIFactory.getAPI();
        // first configure an item
        // create a user for testing
        UserWS user = new UserWS();
        user.setUserName("invoice-penaly-01-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(5);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        ContactWS contact = new ContactWS();
        contact.setEmail("test@test.com");
        contact.setFirstName("First Name");
        contact.setLastName("Last Name");
        user.setContact(contact);

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());

        OrderWS order = new OrderWS();
        order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        // 1 is one time, 200 is for monthly
        order.setPeriod(200);
        order.setCurrencyId(1);
        Calendar cal = Calendar.getInstance();
        // I want to set the active since to 1st May 2012 , so the billing process sees it and invoices it
        // set the calendar to 04/01 (1st May)
        cal.set(2011, 4, 1);
        // now set the date given in the Calendar
        order.setActiveSince(cal.getTime());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(LEMONADE_ITEM_ID);
        line.setUseItem(true);
        line.setQuantity(10);
        order.setOrderLines(new OrderLineWS[] { line });
        order.setDueDateUnitId(PeriodUnitDTO.MONTH);

        order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID))); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());

        // call the billing process to run
        BillingProcessConfigurationWS configBackup = api.getBillingProcessConfiguration();
        BillingProcessConfigurationWS config1 = api.getBillingProcessConfiguration();

        // set the calendar to 04/02 (2nd May)
        cal.set(2011, 4, 2);
        config1.setOnlyRecurring(0);
        config1.setNextRunDate(cal.getTime());
        config1.setGenerateReport(0);
        config1.setDueDateUnitId(1);

        api.createUpdateBillingProcessConfiguration(config1);


        // trigger the billing
        api.triggerBilling(cal.getTime());

        // confirm that an invoice is generated
        InvoiceWS latestInvoice = api.getLatestInvoice(user.getUserId());
        assertNotNull("Should not be null", latestInvoice);

        pause(5000);
        // make another billing run so that invoice is overdue and an order is generated

        BillingProcessConfigurationWS config2 = api.getBillingProcessConfiguration();

        // set the calendar to 05/02 (2nd June)
        cal.set(2011, 5, 2);
        config2.setOnlyRecurring(0);
        config2.setNextRunDate(cal.getTime());
        api.createUpdateBillingProcessConfiguration(config2);

        // trigger the billing
        api.triggerBilling(cal.getTime());

        pause(5000);

        // check the order generated
        // get the last 2 first
        Integer ids[] =   api.getLastOrders(user.getUserId(),2);
        // The second last
        OrderWS latestOrder = api.getOrder(ids[0]);
        System.out.println("The id being checked is >> "+ids[0]);
        System.out.println("The id being not checked is >> "+ids[1]);
        System.out.println("The latest order's amount is "+latestOrder.getTotalAsDecimal());
        // check the status
        System.out.println("The order's status is >>>>>> "+latestOrder.getOrderStatusWS().getId());
        System.out.println("The order's active until is >>>>>> "+latestOrder.getActiveUntil());


        // now make a payment for the above invoice, sufficient to pay them all
        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setIsRefund(new Integer(0));
        payment.setMethodId(ServerConstants.PAYMENT_METHOD_CHEQUE);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setResultId(ServerConstants.RESULT_ENTERED);
        payment.setCurrencyId(new Integer(1));
        payment.setUserId(user.getUserId());
        payment.setPaymentNotes("Notes");
        payment.setPaymentPeriod(new Integer(1));

/*
        PaymentInfoChequeDTO cheque = new PaymentInfoChequeDTO();
        cheque.setBank("ws bank");
        cheque.setDate(Calendar.getInstance().getTime());
        cheque.setNumber("2232-2323-2323");
        payment.setCheque(cheque);
*/
        System.out.println("Applying payment");
//        Integer ret = api.applyPayment(payment, new Integer(35));
        Integer ret = api.applyPayment(payment, null);
        System.out.println("Created payemnt " + ret);
        assertNotNull("Didn't get the payment id", ret);


        // get the order again
        pause(1000);
        order = api.getOrder(order.getId());

        System.out.println("The order's status is >>>>>> "+order.getOrderStatusWS().getId());
        System.out.println("The order's active until is >>>>>> "+order.getActiveUntil());


        // the order should have create date as that of the invoice's create date
        //assertEquals("Order's create date should be equal to invoice's create date", latestOrder.getCreateDate(), latestInvoice.getCreateDateTime());

        // order's active until should be equal to Payment date
        //assertEquals("order's active until should be equal to Payment date", latestOrder.getActiveUntil(), payment.getPaymentDate());

    }


    /**
     * A test case that asserts that the Penalty Item Order is created with a Penalty (1.5% in this case)
     * on only the Un-paid amount of the current Invoice and not on any carried balance on that Invoice
     * @throws Exception
     */

    // test case - 3
    public void testPenaltyOnOverdueAmountOnly() throws Exception {

        // let the first 2 test cases execute
        pause(60000);
        // get the api
        JbillingAPI api = JbillingAPIFactory.getAPI();
        // create customer
        // create a user for testing
        UserWS user = new UserWS();
        user.setUserName("invoice-penaly-01-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(5);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        ContactWS contact = new ContactWS();
        contact.setEmail("test@test.com");
        contact.setFirstName("First Name");
        contact.setLastName("Last Name");
        user.setContact(contact);

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());

        // create order
        OrderWS order = new OrderWS();
        order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        // 1 is one time, 200 is for monthly
        order.setPeriod(200);
        order.setCurrencyId(1);
        Calendar cal = Calendar.getInstance();
        // I want to set the active since to 1 Apr 2012 , so the billing process sees it and invoices it
        // set the calendar to 03/01
        cal.set(2012, 3, 1);
        // now set the date given in the Calendar
        order.setActiveSince(cal.getTime());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(LEMONADE_ITEM_ID);
        line.setUseItem(true);
        line.setQuantity(10);
        order.setOrderLines(new OrderLineWS[] { line });
        order.setDueDateUnitId(PeriodUnitDTO.MONTH);

        order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID))); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());


        // now trigger the billing process and generate invoice
        BillingProcessConfigurationWS config2 = api.getBillingProcessConfiguration();
        // set the calendar to 03/02  2 April
        cal.set(2012, 3, 2);

        config2.setOnlyRecurring(0);
        config2.setGenerateReport(0);
        config2.setDueDateUnitId(1);
        config2.setNextRunDate(cal.getTime());

        api.createUpdateBillingProcessConfiguration(config2);

        // trigger the billing
        api.triggerBillingAsync(cal.getTime());

        // pause
        pause(5000);

        // the invoice would be generated, now trigger the billing on 2 May
        BillingProcessConfigurationWS config3 = api.getBillingProcessConfiguration();
        // set the calendar to 03/02  2 May
        cal.set(2012, 4, 2);
        api.createUpdateBillingProcessConfiguration(config3);
        // trigger the billing, this should generate the invoice with carried balance of 200 with vcarried balance of $100
        api.triggerBillingAsync(cal.getTime());
        // pause
        pause(5000);

        // now trigger the billing again, this should generate the order finally
        BillingProcessConfigurationWS config4 = api.getBillingProcessConfiguration();
        // set the calendar to 03/02  2 June
        cal.set(2012, 5, 2);
        api.createUpdateBillingProcessConfiguration(config4);
        // trigger the billing, this should generate the invoice with carried balance of 200 with vcarried balance of $100
        api.triggerBillingAsync(cal.getTime());
        // pause
        pause(5000);

        // get the latest order and check it would be of $3.02 because 201.50 * 1.5 % = 3.02 (Nothing to do with )
        // get the last 3 first
        Integer ids[] =   api.getLastOrders(user.getUserId(),3);
        // The third last
        OrderWS latestOrder = api.getOrder(ids[0]);
        System.out.println("id of 1st is >> "+ids[0]);
        System.out.println("id of 2nd is >> "+ids[1]);
        System.out.println("id of 3rd is >> "+ids[2]);
        assertNotNull(latestOrder);
        assertEquals("3.02 expected", new BigDecimal("3.02"),latestOrder.getTotalAsDecimal());
    }


        /*
        * Enable/disable the SimpleTaxCompositionTask plug-in.
        */

    public Integer enableTaxPlugin(JbillingAPI api, Integer itemId) {
        PluggableTaskWS plugin = new PluggableTaskWS();
        plugin.setTypeId(OVERDUE_INVOICE_PENALTY_TASK_TYPE_ID);
        plugin.setProcessingOrder(3);

        // plug-in adds the given tax item to the invoice
        // when the customer purchase an item outside of the exempt category
        Hashtable<String, String> parameters = new Hashtable<String, String>();
        parameters.put(PLUGIN_PARAM_ITEM_ID, itemId.toString());
        // todo : Not Required
//        parameters.put(PLUGIN_PARAM_AGEING_STEP, "2");
        plugin.setParameters(parameters);

        return api.createPlugin(plugin);
    }

    public void disableTaxPlugin(JbillingAPI api, Integer pluginId) {
        api.deletePlugin(pluginId);
    }

    /*
     * Convenience assertions for BigDecimal comparisons.
     */

    public static void assertEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertEquals(String message, BigDecimal expected,
            BigDecimal actual) {
        assertEquals(
                message,
                (Object) (expected == null ? null : expected.setScale(2,
                        RoundingMode.HALF_UP)), (Object) (actual == null ? null
                        : actual.setScale(2, RoundingMode.HALF_UP)));
    }

    private void pause(long t) {
        System.out.println("pausing for " + t + " ms...");
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
        }
    }

}
