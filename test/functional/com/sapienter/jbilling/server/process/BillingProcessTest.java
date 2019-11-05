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

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.payment.PaymentWS;

import jline.internal.Log;
import junit.framework.TestCase;
import org.joda.time.DateMidnight;

import com.sapienter.jbilling.common.Util;
import org.joda.time.LocalDate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetTransitionDTOEx;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

/**
 * Points to testOrders: Orders : - next billable day - to_process - start/end of billing period - invoice has (not)
 * generated - billing process relationship - some amounts of the generated invoice Invoices : - if the invoice has been
 * processed or no - to_process - delegated_invoice_id is updated
 *
 * @author Emil
 */
@Test(groups = { "integration", "process" })
public class BillingProcessTest extends BillingProcessTestCase {

    Date                         processDate                          = null;
    final Integer                entityId                             = 1;
    Integer                      languageId                           = null;
    Date                         runDate                              = null;

    private static final Integer ORDER_PERIOD_PLUGIN_ID               = 6;
    private static final Integer BASIC_ORDER_PERIOD_PLUGIN_TYPE_ID    = 7;   // BasicOrderPeriodTask
    private static final Integer PRO_RATE_ORDER_PERIOD_PLUGIN_TYPE_ID = 37;  // ProRateOrderPeriodTask
    private final static int     ORDER_CHANGE_STATUS_APPLY_ID         = 3;

    @BeforeClass
    protected void setUp () throws Exception {
        languageId = new Integer(1);
        runDate = AsDate(2006, 10, 26);
    }

    @Test(enabled = true)
    public void test001EndOfMonthCorrection () throws Exception {
        System.out.println("#test001EndOfMonthCorrection");

        // set the configuration to something we are sure about
        /*
         * BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
         * 
         * config.setNextRunDate(new DateMidnight(2000, 12, 1).toDate()); config.setRetries(new Integer(1));
         * config.setDaysForRetry(new Integer(5)); config.setGenerateReport(new Integer(1));
         * config.setAutoPaymentApplication(new Integer(1)); config.setDfFm(new Integer(0));
         * config.setDueDateUnitId(CommonConstants.PERIOD_UNIT_MONTH); config.setDueDateValue(new Integer(1));
         * config.setInvoiceDateProcess(new Integer(1)); config.setMaximumPeriods(new Integer(10));
         * config.setOnlyRecurring(new Integer(1)); config.setPeriodUnitId(CommonConstants.PERIOD_UNIT_MONTH);
         * config.setPeriodValue(new Integer(1));
         * 
         * System.out.println("A - Setting config to: " + config); api.createUpdateBillingProcessConfiguration(config);
         */

        // user for tests
        UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);
        OrderWS order = com.sapienter.jbilling.server.order.WSTest.createMockOrder(user.getUserId(), 1, new BigDecimal(
                60));
        order.setActiveSince(AsDate(2000, 11, 30));
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(2); // monthly
        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        Integer orderId = api.createUpdateOrder(order, orderChanges);
        System.out.println("Order id: " + orderId);

        // run the billing process. It should only get this order
        // Date billingDate = new DateMidnight(2000, 12, 1).toDate();
        // api.triggerBilling(billingDate);
        api.createInvoice(user.getUserId(), false);

        System.out.println("User id: " + user.getUserId());

        Integer[] invoiceIds = api.getAllInvoices(user.getUserId());
        System.out.println("Invoice ids: " + Arrays.toString(invoiceIds));

        InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);

        // clean up
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(orderId);
        api.deleteUser(user.getUserId());
    }
    @Test(enabled = true)
    public void test002ViewLimit () throws Exception {
        System.out.println("#test002ViewLimit");

        // set the configuration to something we are sure about

        BillingProcessConfigurationWS configBackup = api.getBillingProcessConfiguration();
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();

        // just change it to a day
        config.setMaximumPeriods(100);
        //config.setInvoiceDateProcess(0);
        //config.setProratingType();

        System.out.println("A - Setting config to: " + config);
        api.createUpdateBillingProcessConfiguration(config);

        // user for tests
        // user is invoices Monthly on 1
        UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);

        // update the user main subscription and next invoice date to match with the order active since date
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(new Date());
        MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
        mainSubscription.setPeriodId(2); // monthly
        mainSubscription.setNextInvoiceDayOfPeriod(gcal.get(Calendar.DAY_OF_MONTH));
        user.setMainSubscription(mainSubscription);
        user.setNextInvoiceDate(gcal.getTime());
        // 2014-11-05 Igor Poteryaev. commented out
        // can't change password here, because of constraint for new passwords only once per day
        // user.setPassword("P@ssword18");
        api.updateUser(user);

        // update the user once again this time for setting back the invoice date to today's date
        user = api.getUserWS(user.getId());
        user.setNextInvoiceDate(gcal.getTime());
        // 2014-11-05 Igor Poteryaev. commented out
        // can't change password here, because of constraint for new passwords only once per day
        // user.setPassword("P@ssword19");
        api.updateUser(user);

        System.out.println("::::: testViewLimit user: " + user);

        OrderWS order = com.sapienter.jbilling.server.order.WSTest.createMockOrder(user.getUserId(), 1, new BigDecimal(
                60));
        // active since a little bit more than a month than the current billing process
        // When calling 'createInvoice' the billing process date is set to today, but the period is
        // taken from the configuration (very odd, almost a bug. To fix it, add a parameter to 'createInvoice' with the
        // date.
        // if null, use today).
        order.setActiveSince(new LocalDate(new Date()).toDateTimeAtStartOfDay().withDayOfMonth(1).minus(10).toDate());
        
        System.out.println("New Date " + new Date());
        System.out.println("WithDayOfMonth 1 " + new DateMidnight(new Date()).withDayOfMonth(1).toDate());
        System.out.println("Minus 10 Days " + new DateMidnight(new Date()).withDayOfMonth(1).minus(10).toDate());
        System.out.println("Active Since " + order.getActiveSince());
        
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(2); // monthly

        List<OrderChangeWS> orderChanges = new LinkedList<OrderChangeWS>();
        for (OrderLineWS line : order.getOrderLines()) {
	        OrderChangeWS orderChange = OrderChangeBL.buildFromLine(line, order, ORDER_CHANGE_STATUS_APPLY_ID);
	        orderChange.setStartDate(Util.truncateDate(order.getActiveSince()));
            orderChanges.add(orderChange);

        }
        Integer orderId = api.createUpdateOrder(order, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]));
        System.out.println("Order id: " + orderId);

        // run the billing process. For this user only
        Integer invoiceIds[] = api.createInvoice(user.getUserId(), false);

        System.out.println("Invoice ids: " + Arrays.toString(invoiceIds));
        InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
        
        Calendar cal= Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        Date viewLimit= cal.getTime();
        System.out.println("View Limit " + viewLimit);
        Calendar activeSince= Calendar.getInstance();
        activeSince.setTime(order.getActiveSince());
        System.out.println("Starting with activeSince " + activeSince.getTime());
        int i = 0;
        while (activeSince.getTime().compareTo(viewLimit) < 0 ) {
        	activeSince.add(Calendar.MONTH, 1);
        	System.out.println("I at " + i + " and activeSince " + activeSince.getTime());
        	i++;
        }
        
        BigDecimal expectedValue = new BigDecimal(60).multiply(new BigDecimal(i));
        System.out.println("Expected value " + expectedValue + " actual Value " + invoice.getBalanceAsDecimal());
                
        // customer is invoices on 1 of the month and its evaluation period is 1 month
        assertEquals("New invoice should be " + i + " months, for a total of " + 60 * i,
                     expectedValue,
                     invoice.getBalanceAsDecimal());

        // clean up
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(orderId);
        api.deleteUser(user.getUserId());
        api.createUpdateBillingProcessConfiguration(configBackup);
    }

    @Test(enabled = true)
    public void test003Retry () throws Exception {
        System.out.println("#test003Retry");

        // set the configuration to something we are sure about
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();

        config.setNextRunDate(runDate);
        config.setRetries(new Integer(1));
        config.setDaysForRetry(new Integer(5));
        config.setGenerateReport(new Integer(0));
        config.setAutoPaymentApplication(new Integer(1));
        config.setDfFm(new Integer(0));
        config.setDueDateUnitId(ServerConstants.PERIOD_UNIT_MONTH);
        config.setDueDateValue(new Integer(1));
        config.setInvoiceDateProcess(new Integer(1));
        config.setMaximumPeriods(new Integer(10));
        config.setOnlyRecurring(new Integer(1));
        System.out.println("B - Setting config to: " + config);
        api.createUpdateBillingProcessConfiguration(config);

        // retries calculate dates using the real date of the run
        // when know of one from the pre-cooked DB
        Date retryDate = AsDate(2000, 12, 19);

        // get the involved process
        BillingProcessWS billingProcess = api.getBillingProcess(2);

        // run trigger
        api.triggerBilling(retryDate);

        // get the process again
        BillingProcessWS billingProcess2 = api.getBillingProcess(2);

        // run trigger 5 days later
        api.triggerBilling(AsDate(2000, 12, 24));

        // get the process again
        // now a retry should be there
        BillingProcessWS billingProcess3 = api.getBillingProcess(2);

        // run trigger 10 days later then retryDate
        api.triggerBilling(AsDate(2000, 12, 29));

        // get the process again
        BillingProcessWS billingProcess4 = api.getBillingProcess(2);

        // wait for the asynchronous payment processing to finish
        Thread.sleep(3000);

        // the billing process has to have a total paid equal to the invoice
        BillingProcessWS process = api.getBillingProcess(2);
        ProcessRunWS run = process.getProcessRuns().get(process.getProcessRuns().size() - 1);
        ProcessRunTotalWS total = run.getProcessRunTotals().get(0);
    }

    @Test(enabled = true)
    public void test004Run () {
        System.out.println("#test004Run");
        try {
            // get the latest process
            Integer processId = api.getLastBillingProcess();
            BillingProcessWS process = api.getBillingProcess(processId);

            // run trigger but too early
            api.triggerBilling(AsDate(2005, 1, 26));

            // get the latest process (after triggered run)
            Integer processId2 = api.getLastBillingProcess();
            BillingProcessWS process2 = api.getBillingProcess(processId2);

            // no new process should have run
            assertEquals("No new process run", process.getId(), process2.getId());

            // no retry should have run
            assertEquals("No new process run (retries)", process.getProcessRuns().size(), process2.getProcessRuns()
                    .size());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception:" + e);
        }
    }

    @Test
    public void test005Review () throws Exception {
        logger.info("#test005Review");

        // enableProRateOrderPeriodTask(api);

        UserWS user121 = api.getUserWS(121);
	    user121.setPassword(null);

        // update the user main subscription and next invoice date to match with the order active since date
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(AsDate(2006, 11, 01));
        MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
        mainSubscription.setPeriodId(2); // monthly
        mainSubscription.setNextInvoiceDayOfPeriod(gcal.get(Calendar.DAY_OF_MONTH));
        user121.setMainSubscription(mainSubscription);
        user121.setNextInvoiceDate(gcal.getTime());
        api.updateUser(user121);

        // update the user once again this time for setting back the invoice date to today's date
        user121 = api.getUserWS(121);
	    user121.setPassword(null);
        user121.setNextInvoiceDate(gcal.getTime());
        api.updateUser(user121);

        OrderWS order103 = api.getOrder(103);
        order103.setProrateFlag(true);
        api.updateOrder(order103, null);

        // get the latest process
        Integer abid = api.getLastBillingProcess();
        BillingProcessWS lastDto = api.getBillingProcess(abid);

        // get the review
        BillingProcessWS reviewDto = api.getReviewBillingProcess();

        // not review should be there
        logger.info("TODO: should there be already a review?");
        // assertNotNull("3 - The test DB should have one review", reviewDto);

        // set the configuration to something we are sure about
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        config.setDaysForReport(5);
        config.setGenerateReport(1);
        // enable Pro-Rating
        config.setProratingType(ProratingType.PRORATING_AUTO_ON.toString());

        logger.info("C - Setting config to: " + config);
        api.createUpdateBillingProcessConfiguration(config);

        // disapprove the review (that just run before this one)
        api.setReviewApproval(false);

        // run trigger, this time it should run and generate a report
        Thread reviewThread = new Thread() {
            @Override
            public void run () {
                api.triggerBilling(runDate);
            }
        };
        logger.info("billing process have run run for date: " + runDate);

        reviewThread.start();

        // trying immediatelly after, should not run
        Thread.sleep(1000); // take it easy
        assertFalse("It should not run, a review is running already", api.triggerBilling(runDate));

        logger.info("Second Billing process have run");
        // now wait until that thread is done
        while (reviewThread.isAlive()) {
            Thread.sleep(1000); // take it easy
        }
        logger.info("Getting latest process");
        // get the latest process
        BillingProcessWS lastDtoB = api.getBillingProcess(api.getLastBillingProcess());

        logger.info("Last Billing Process is: " + lastDtoB);
        // no new process should have run
        assertEquals("4 - No new process", lastDto.getId(), lastDtoB.getId());

        // get the review
        // now review should be there
        logger.info("Getting last review");
        reviewDto = api.getReviewBillingProcess();
        assertNotNull("5 - Review should be there", reviewDto);

        // validate that the review generated an invoice for user 121
        logger.info("Validating invoice delegation");

        Integer[] invoiceIds = api.getAllInvoices(121);
        assertEquals("User 121 should have two invoices", 2, invoiceIds.length);

        InvoiceWS invoice = getReviewInvoice(invoiceIds);

        assertNotNull("Review invoice present", invoice);
        assertEquals("Review invoice has to be total 1288.55", new BigDecimal("1288.55"), invoice.getTotalAsDecimal());
        assertNull("Review invoice not delegated", invoice.getDelegatedInvoiceId());

        Integer reviewInvoiceId = invoice.getId();
        logger.info("Review invoice id: " + reviewInvoiceId);

        invoice = getNonReviewInvoice(invoiceIds);

        assertNull("Overdue invoice not delegated", invoice.getDelegatedInvoiceId());
        assertEquals("Overdue invoice should remain 'unpaid', since this is only a review",
                ServerConstants.INVOICE_STATUS_UNPAID, invoice.getStatusId());

        assertEquals("Overdue invoice balance 15", new BigDecimal("15.0"), invoice.getBalanceAsDecimal());
        Integer overdueInvoiceId = invoice.getId();

        // validate that the review left the order 107600 is still active
        // This is a pro-rated order with only a fraction of a period to
        // invoice.

        OrderWS proRatedOrder = api.getOrder(103);
        assertEquals("Pro-rate order should remain active", OrderStatusFlag.INVOICE, proRatedOrder.getOrderStatusWS()
                .getOrderStatusFlag());

        // disapprove the review
        api.setReviewApproval(false);

        invoiceIds = api.getAllInvoices(121);
        invoice = getNonReviewInvoice(invoiceIds);

        assertNotNull("Overdue invoice still there", invoice);
        assertEquals("Overdue invoice should remain 'unpaid', after disapproval", ServerConstants.INVOICE_STATUS_UNPAID,
                invoice.getStatusId());

        assertEquals("Overdue invoice balance 15", new BigDecimal("15.0"), invoice.getBalanceAsDecimal());

        logger.info("Triggering Billing early");
        // run trigger, but too early (six days, instead of 5)
        api.triggerBilling(AsDate(2006, 10, 20));

        // get the latest process
        // no new process should have run
        lastDtoB = api.getBillingProcess(api.getLastBillingProcess());
        assertEquals("7 - No new process, too early", lastDto.getId(), lastDtoB.getId());

        // get the review
        BillingProcessWS reviewDto2 = api.getReviewBillingProcess();
        assertEquals("8 - No new review run", reviewDto.getId(), reviewDto2.getId());

        // status of the review should still be disapproved
        config = api.getBillingProcessConfiguration();
        assertEquals("9 - Review still disapproved", config.getReviewStatus(),
                ServerConstants.REVIEW_STATUS_DISAPPROVED.intValue());

        // run trigger this time has to generate a review report
        api.triggerBilling(AsDate(2006, 10, 22));

        invoice = api.getInvoiceWS(overdueInvoiceId);

        assertNotNull("Overdue invoice still there", invoice);
        assertEquals("Overdue invoice should remain 'unpaid', after disapproval", ServerConstants.INVOICE_STATUS_UNPAID,
                invoice.getStatusId());

        assertEquals("Overdue invoice balance 15", new BigDecimal("15.0"), invoice.getBalanceAsDecimal());

        try {
            invoice = api.getInvoiceWS(reviewInvoiceId);
            logger.info("Invoice:" + invoice + " for invoiceId: " + reviewInvoiceId);
            fail("Invoice does not exist, should throw a Hibernate exception.");
        } catch (Exception e) {
        }

        // get the latest process
        // no new process should have run
        lastDtoB = api.getBillingProcess(api.getLastBillingProcess());
        assertEquals("10 - No new process, review disapproved", lastDto.getId(), lastDtoB.getId());

        // get the review
        // since the last one was disapproved, a new one has to be created
        reviewDto2 = api.getReviewBillingProcess();
        assertNotSame("11 - New review run", reviewDto.getId(), reviewDto2.getId());

        // status of the review should now be generated
        config = api.getBillingProcessConfiguration();
        assertEquals("12 - Review generated", config.getReviewStatus(), ServerConstants.REVIEW_STATUS_GENERATED.intValue());

        // run trigger, date is good, but the review is not approved
        api.triggerBilling(AsDate(2006, 10, 22));

        // get the review
        // the status is generated, so it should not be a new review
        reviewDto = api.getReviewBillingProcess();
        assertEquals("13 - No new review run", reviewDto.getId(), reviewDto2.getId());

        // run trigger report still not approved, no process then
        api.triggerBilling(AsDate(2006, 10, 22));

        // get the latest process
        // no new process should have run
        lastDtoB = api.getBillingProcess(api.getLastBillingProcess());
        assertEquals("14 - No new process, review not yet approved", lastDto.getId(), lastDtoB.getId());

        // disapprove the review so it should run again
        api.setReviewApproval(false);

        //
        // Run the review and approve it to allow the process to run
        //
        api.triggerBilling(AsDate(2006, 10, 22));

        // get the review
        // since the last one was disapproved, a new one has to be created
        reviewDto2 = api.getReviewBillingProcess();
        assertFalse("14.2 - New review run", reviewDto.getId().equals(reviewDto2.getId()));

        // finally, approve the review. The billing process is next
        api.setReviewApproval(true);

        // enableBasicOrderPeriodTask(api);
    }

    @Test(enabled = false)
    public void test006BillingProcessStatus () throws Exception {
        System.out.println("#test006BillingProcessStatus");

        // no active processes now, all calls was sync
        assertFalse("No active billing processes now!", api.isBillingRunning(entityId));

        ProcessStatusWS completedStatus = api.getBillingProcessStatus();
        assertNotNull("Status should be retrieved", completedStatus);
        assertNotNull("Start date should be filled", completedStatus.getStart());
        assertNotNull("End date should be filled", completedStatus.getEnd());
        assertEquals("Process status should be FINISHED", ProcessStatusWS.State.FINISHED, completedStatus.getState());
    }

    @Test(enabled = false)
    public void test007Process () throws Exception {
        System.out.println("#test007Process");

        // enableProRateOrderPeriodTask(api);

        // get the latest process
        BillingProcessWS lastDto = api.getBillingProcess(api.getLastBillingProcess());

        // get the review, so we can later check that what id had
        // is the same that is generated in the real process
        BillingProcessWS reviewDto = api.getReviewBillingProcess();

        // check that the next billing date is updated
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();

        // enable Pro-Rating
        config.setProratingType(ProratingType.PRORATING_AUTO_ON.toString());
        api.createUpdateBillingProcessConfiguration(config);
        config = api.getBillingProcessConfiguration();
        assertEquals("14.9 - Next billing date starting point", AsDate(2006, 10, 26), config.getNextRunDate());

        Integer userId = api.getUserId("pendunsus1");
        UserWS user54 = api.getUserWS(userId);
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(config.getNextRunDate());
        MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
        mainSubscription.setPeriodId(2); // monthly
        mainSubscription.setNextInvoiceDayOfPeriod(gcal.get(Calendar.DAY_OF_MONTH));
        user54.setMainSubscription(mainSubscription);
        user54.setNextInvoiceDate(gcal.getTime());
        api.updateUser(user54);

        // update the user once again this time for setting back the invoice date to today's date
        user54 = api.getUserWS(userId);
        user54.setNextInvoiceDate(gcal.getTime());
        api.updateUser(user54);

        Integer userId2 = api.getUserId("pendunsus2");
        UserWS user55 = api.getUserWS(userId2);
        gcal.setTime(config.getNextRunDate());
        mainSubscription.setPeriodId(2); // monthly
        mainSubscription.setNextInvoiceDayOfPeriod(gcal.get(Calendar.DAY_OF_MONTH));
        user55.setMainSubscription(mainSubscription);
        user55.setNextInvoiceDate(gcal.getTime());
        api.updateUser(user55);

        // update the user once again this time for setting back the invoice date to today's date
        user55 = api.getUserWS(userId2);
        user55.setNextInvoiceDate(gcal.getTime());
        api.updateUser(user55);

        // run trigger on the run date
        api.triggerBillingAsync(runDate);

        // continually check the process status until the API says that the billing process is no longer running.
        ProcessStatusWS runningStatus = null;
        while (api.isBillingRunning(entityId)) {
            ProcessStatusWS tempStatus = api.getBillingProcessStatus();
            /*
             * if isBillingRunning returns true and getBillingProcessStatus return FINISHED status (a random possibility
             * if billing run is finished in between the 2 api calls), then below assert for RUNNING status was failing.
             * so added a check here to set the runningStatus variable, only if status is RUNNING. This ensures that
             * runningStatus is not set with FINISHED status in radom cases. The purpose of below assert is only to
             * check that billing process has indeed run and the status was set to RUNNING at least once in this loop.
             * Hence added a check of tempStatus.getState() == ProcessStatusWS.State.RUNNING
             */
            if (tempStatus != null && tempStatus.getState() == ProcessStatusWS.State.RUNNING)
                runningStatus = tempStatus;
            Thread.sleep(1000);
        }

        // validate the process status that was recorded while the billing process was running
        System.out.println("found running status for process: " + runningStatus);

        assertNotNull("Process never had a running status", runningStatus);
        assertEquals("Status should have been RUNNING", ProcessStatusWS.State.RUNNING, runningStatus.getState());
        assertNotNull("Status should have start date", runningStatus.getStart());
        runningStatus = api.getBillingProcessStatus();
        assertNotNull("Status end date should be empty while running", runningStatus.getEnd());

        // validate process status after the billing process finished
        ProcessStatusWS completedStatus = api.getBillingProcessStatus();
        System.out.println("completed status for process: " + completedStatus);

        assertNotNull("Process had a status upon completion", completedStatus);
        assertEquals("Status should be FINISHED", ProcessStatusWS.State.FINISHED, completedStatus.getState());
        assertNotNull("Status should have start date", completedStatus.getStart());
        assertNotNull("Status should have end date", completedStatus.getEnd());

        // validate invoice delegation
        InvoiceWS invoice = api.getInvoiceWS(8500);
        assertNotNull("Overdue invoice still there", invoice);
        assertEquals("Overdue invoice is not 'paid'", 0, invoice.getToProcess().intValue());
        assertEquals("Overdue invoice is now 'carried over'", ServerConstants.INVOICE_STATUS_UNPAID_AND_CARRIED,
                invoice.getStatusId());

        assertEquals("Overdue invoice balance remains the same", new BigDecimal("15.0"), invoice.getBalanceAsDecimal());

        assertNotNull("Overdue invoice is now delegated", invoice.getDelegatedInvoiceId());

        // get the latest process
        // this is the one and only new process run
        BillingProcessWS lastDtoB = api.getBillingProcess(api.getLastBillingProcess());
        assertFalse("15 - New Process", lastDto.getId().equals(lastDtoB.getId()));

        // initially, runs should be 1
        assertEquals("16 - Only one run", 1, lastDtoB.getProcessRuns().size());

        // check that the next billing date is updated
        config = api.getBillingProcessConfiguration();
        assertEquals("17 - Next billing date for a month later", AsDate(2006, 11, 26), config.getNextRunDate());

        // verify that the transition from pending unsubscription to unsubscribed worked

        UserWS user = api.getUserWS(userId);

        assertEquals("User should stay on pending unsubscription", UserDTOEx.SUBSCRIBER_PENDING_UNSUBSCRIPTION,
                user.getSubscriberStatusId());

        userId = api.getUserId("pendunsus2");
        user = api.getUserWS(userId);

        assertEquals("User should have changed to unsubscribed", UserDTOEx.SUBSCRIBER_UNSUBSCRIBED,
                user.getSubscriberStatusId());

        // enableBasicOrderPeriodTask(api);
    }

    @Test(enabled = false)
    public void test008GeneratedInvoices () throws Exception {
        System.out.println("#test008GeneratedInvoices");

        Integer[] invoiceIds = api.getBillingProcessGeneratedInvoices(api.getLastBillingProcess());
        assertEquals("Invoices generated", 12, invoiceIds.length);

        // validate each invoice and check that the invoiced total matches the
        // sum of the comprising order totals.
        for (Integer id : invoiceIds) {
            InvoiceWS invoice = api.getInvoiceWS(id);

            // calculate the total value from the source orders
            BigDecimal orderTotal = BigDecimal.ZERO;

            for (OrderProcessWS orderProcess : api.getOrderProcessesByInvoice(id)) {
                OrderWS orderDto = api.getOrder(orderProcess.getOrderId());
                orderTotal = orderTotal.add(orderDto.getTotalAsDecimal());

                // validate the invoice total for non pro-rated invoices
                if (orderProcess.getOrderId() == 102) {
                    BigDecimal invoiceTotal = invoice.getTotalAsDecimal()
                            .subtract(invoice.getCarriedBalanceAsDecimal());
                    assertEquals("sum of orders does not equal total for invoice " + invoice.getId() + " (total: "
                            + invoice.getTotal() + ", carried: " + invoice.getCarriedBalance() + ")", orderTotal,
                            invoiceTotal);
                }
            }

        }

        assertTrue("invoice should not be generated", api.getAllInvoices(1067).length == 0);
    }

    @Test(enabled = false)
    public void test010NextBillingProcess () throws Exception {
        System.out.println("#test010NextBillingProcess");

        // re-run the billing process
        System.out.println("Running the billing process again");
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        config.setProratingType(ProratingType.PRORATING_AUTO_ON.toString());
        api.createUpdateBillingProcessConfiguration(config);
        config = api.getBillingProcessConfiguration();
        api.triggerBilling(config.getNextRunDate());

        // update the user main subscription and next invoice date to match with the order active since date
        UserWS user1067 = api.getUserWS(1067);
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(config.getNextRunDate());
        MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
        mainSubscription.setPeriodId(2); // monthly
        mainSubscription.setNextInvoiceDayOfPeriod(gcal.get(Calendar.DAY_OF_MONTH));
        user1067.setMainSubscription(mainSubscription);
        user1067.setNextInvoiceDate(gcal.getTime());
        api.updateUser(user1067);

        // update the user once again this time for setting back the invoice date to today's date
        user1067 = api.getUserWS(1067);
        user1067.setNextInvoiceDate(gcal.getTime());
        api.updateUser(user1067);

        OrderWS order102 = api.getOrder(102);
        UserWS user120 = api.getUserWS(order102.getUserId());
        gcal.setTime(config.getNextRunDate());
        MainSubscriptionWS mainSubscription120 = new MainSubscriptionWS();
        mainSubscription120.setPeriodId(2); // monthly
        mainSubscription120.setNextInvoiceDayOfPeriod(gcal.get(Calendar.DAY_OF_MONTH));
        user120.setMainSubscription(mainSubscription120);
        user120.setNextInvoiceDate(gcal.getTime());
        api.updateUser(user120);

        // update the user once again this time for setting back the invoice date to today's date
        user120 = api.getUserWS(order102.getUserId());
        user120.setNextInvoiceDate(gcal.getTime());
        api.updateUser(user120);

        // get the review
        // now review should be there
        BillingProcessWS reviewDto = api.getReviewBillingProcess();
        assertNotNull("Review should be there", reviewDto);

        api.setReviewApproval(true);
        // Make sure it is not asking for a review
        config.setGenerateReport(0);
        api.createUpdateBillingProcessConfiguration(config);

        // run trigger on the run date
        api.triggerBillingAsync(config.getNextRunDate());
        Thread.sleep(5000);

        // continually check the process status until the API says that the billing process is no longer running.
        ProcessStatusWS runningStatus = null;
        while (api.isBillingRunning(entityId)) {
            ProcessStatusWS tempStatus = api.getBillingProcessStatus();
            if (tempStatus != null && tempStatus.getState() == ProcessStatusWS.State.RUNNING)
                runningStatus = tempStatus;
            Thread.sleep(5000);
        }

        assertNotNull("Process had a running status", runningStatus);
        assertEquals("Status should be RUNNING", ProcessStatusWS.State.RUNNING, runningStatus.getState());
        assertNotNull("Status should have start date", runningStatus.getStart());
        assertNull("Status end date should be empty while running", runningStatus.getEnd());

        // validate process status after the billing process finished
        ProcessStatusWS completedStatus = api.getBillingProcessStatus();
        System.out.println("completed status for process: " + completedStatus);

        assertNotNull("Process had a status upon completion", completedStatus);
        assertEquals("Status should be FINISHED", ProcessStatusWS.State.FINISHED, completedStatus.getState());
        assertNotNull("Status should have start date", completedStatus.getStart());
        assertNotNull("Status should have end date", completedStatus.getEnd());
        
        config = api.getBillingProcessConfiguration();

        // TODO: Remove the comments!
        assertEquals("17 - Next billing date for a month later", AsDate(2006, 12, 26), config.getNextRunDate());

        Integer[] invoiceIds = api.getBillingProcessGeneratedInvoices(api.getLastBillingProcess());
        assertEquals("Invoices generated", 1000, invoiceIds.length);

        assertTrue("invoice should be generated", api.getAllInvoices(1067).length != 0);

        // enableBasicOrderPeriodTask(api);
    }

    /*
     * VALIDATE ORDERS
     */

    @Test(enabled = false)
    public void test013OrdersProcessedDate () {
        System.out.println("#test013OrdersProcessedDate");

        String dates[] = { "2006-12-01", "2006-12-01", null, // 100 - 102
                "2006-12-01", null, null, // 103 - 105
                "2006-12-01", null, null, // 106 - 108
                null, "2006-12-01", "2006-12-01", // 109 - 111
                "2006-11-15", null, // 112 - 113
        };

        try {
            for (int f = 100; f < dates.length; f++) {
                OrderWS order = api.getOrder(f);

                if (order.getNextBillableDay() != null) {
                    if (dates[f] == null) {
                        assertNull("Order " + order.getId(), order.getNextBillableDay());
                    } else {
                        assertEquals("Order " + order.getId(), parseDate(dates[f]), order.getNextBillableDay());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception:" + e);
        }
    }

    @Test(enabled = false)
    public void test014OrdersFlaggedOut () {
        System.out.println("#test014OrdersFlaggedOut");

        int orders[] = { 102 };

        try {
            for (int f = 0; f < orders.length; f++) {
                OrderWS order = api.getOrder(orders[f]);
                assertEquals("Order " + order.getId(), order.getOrderStatusWS().getOrderStatusFlag(),
                        OrderStatusFlag.FINISHED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception:" + e);
        }
    }

    @Test(enabled = false)
    public void test015OrdersStillIn () {
        System.out.println("#test015OrdersStillIn");

        int orders[] = { 100, 101, 103, 106, 110, 111, 112 };

        try {
            for (int f = 0; f < orders.length; f++) {
                OrderWS order = api.getOrder(orders[f]);
                assertEquals("Order " + order.getId(), order.getOrderStatusWS().getOrderStatusFlag(),
                        OrderStatusFlag.INVOICE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception:" + e);
        }
    }

    @Test(enabled = false)
    public void test016Excluded () {
        System.out.println("#test016Excluded");
        int orders[] = { 109 };
        try {
            for (int f = 0; f < orders.length; f++) {
                OrderWS order = api.getOrder(orders[f]);
                OrderProcessWS[] processes = api.getOrderProcesses(order.getId());

                assertTrue("Order not processed", processes.length==0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception:" + e);
        }
    }

    @Test(enabled = false)
    public void test017NegativeInvoiceToCredit () throws Exception {
        System.out.println("#test017NegativeInvoiceToCredit");

        // user for tests
        UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);
        Integer userId = user.getUserId();

        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(1); // once
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(1);
        line.setQuantity(1);
        line.setPrice(new BigDecimal("-200.00"));
        line.setAmount(new BigDecimal("-200.00"));

        order.setOrderLines(new OrderLineWS[] { line });

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Order 1 created", orderId1);

        // generate review report
        api.createInvoice(user.getUserId(), false);

        // validate generated invoices
        InvoiceWS invoice = api.getLatestInvoice(userId);
        assertNotNull("Invoice generated", invoice);

        assertEquals("Invoice total should be $0.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        // Validate that a credit payment should have been created.
        PaymentWS paymentWS = api.getLatestPayment(userId);
        assertEquals("The credit payment amount should be $200.00", new BigDecimal("200.00"),
                paymentWS.getAmountAsDecimal());
        assertEquals("Payment method should be credit.", ServerConstants.PAYMENT_METHOD_CREDIT, paymentWS.getMethodId());

        // cleanup
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(orderId1);
        api.deleteUser(userId);
    }

    /**
     * Test that the BillingProcess will fail with status "Finished: failed" if an exception occurs and that resolving
     * the failure allows the process to complete successfully in a later run.
     *
     * @throws Exception
     *             testing
     */
    @Test(enabled = false)
    public void test018BillingProcessFailure () throws Exception {
        System.out.println("#test018BillingProcessFailure");

        // order period aligned with the 13th
        Date runDate = AsDate(2007, 12, 13);

        // create testing user and order
        UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);

        // update the user main subscription and next invoice date to match with the order active since date
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(runDate);
        MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
        mainSubscription.setPeriodId(2); // monthly
        mainSubscription.setNextInvoiceDayOfPeriod(gcal.get(Calendar.DAY_OF_MONTH));
        user.setMainSubscription(mainSubscription);
        user.setNextInvoiceDate(gcal.getTime());
        user.setPassword("P@ssword18");
        api.updateUser(user);

        // update the user once again this time for setting back the invoice date to today's date
        user = api.getUserWS(user.getId());
        user.setNextInvoiceDate(gcal.getTime());
        user.setPassword("P@ssword19");
        api.updateUser(user);

        System.out.println("::::: test017BillingProcessFailure user: " + user);

        OrderWS brokenOrder = com.sapienter.jbilling.server.order.WSTest.createMockOrder(user.getUserId(), 1,
                new BigDecimal(10));

        brokenOrder.setActiveSince(runDate);
        brokenOrder.setPeriod(2); // monthly
        brokenOrder.setBillingTypeId(9); // invalid billing type id to trigger failure

        List<OrderChangeWS> orderChanges = new LinkedList<OrderChangeWS>();
        for (OrderLineWS line : brokenOrder.getOrderLines()) {
            orderChanges.add(OrderChangeBL.buildFromLine(line, brokenOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        }
        Integer orderId = api.createUpdateOrder(brokenOrder,
                orderChanges.toArray(new OrderChangeWS[orderChanges.size()]));
        System.out.println("Order id: " + orderId);

        // set the configuration to include the corrupt order
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        config.setNextRunDate(runDate);
        config.setRetries(1);
        config.setDaysForRetry(5);
        config.setGenerateReport(0);
        config.setAutoPaymentApplication(1);
        config.setDfFm(0);
        config.setDueDateUnitId(ServerConstants.PERIOD_UNIT_MONTH);
        config.setDueDateValue(1);
        config.setInvoiceDateProcess(1);
        config.setMaximumPeriods(10);
        config.setOnlyRecurring(1);

        // trigger billing
        // process should finish with status "failed" because of the corrupt order
        System.out.println("D - Setting config to: " + config);
        api.createUpdateBillingProcessConfiguration(config);
        System.out.println("Running proces for : " + runDate);
        api.triggerBilling(runDate);

        Integer billingProcessId = api.getLastBillingProcess();
        BillingProcessWS billingProcess = api.getBillingProcess(billingProcessId);
        ProcessRunWS run = billingProcess.getProcessRuns().get(0);

        assertEquals("Last billing process run should have failed.", "Finished: failed", run.getStatusStr());

        // fix the order by setting billing type ID to a proper value
        OrderWS fixedOrder = api.getOrder(orderId);
        fixedOrder.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
        api.updateOrder(fixedOrder, null);

        // reset the configuration and retry
        // process should finish with status "successful"
        System.out.println("E - Setting config to: " + config);
        api.createUpdateBillingProcessConfiguration(config);
        System.out.println("Running proces for : " + runDate);
        api.triggerBilling(runDate);
        billingProcessId = api.getLastBillingProcess();
        billingProcess = api.getBillingProcess(billingProcessId);
        run = billingProcess.getProcessRuns().get(0);
        System.out.println("Got out of the job");
        assertEquals("Last billing process run should have passed.", "Finished: successful", run.getStatusStr());

        // cleanup
        api.deleteOrder(orderId);
        api.deleteUser(user.getUserId());
    }

    @Test(enabled = false)
    public void test018NegativeInvoiceToCredit () throws Exception {
        System.out.println("#test018NegativeInvoiceToCredit");

        // user for tests
        UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);
        Integer userId = user.getUserId();

        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(1); // once
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(1);
        line.setQuantity(1);
        line.setPrice(new BigDecimal("-200.00"));
        line.setAmount(new BigDecimal("-200.00"));

        order.setOrderLines(new OrderLineWS[] { line });

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Order 1 created", orderId1);

        // generate review report
        api.createInvoice(user.getUserId(), false);

        // validate generated invoices
        InvoiceWS invoice = api.getLatestInvoice(userId);
        assertNotNull("Invoice generated", invoice);

        assertEquals("Invoice total should be $0.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        // Validate that a credit payment should have been created.
        PaymentWS paymentWS = api.getLatestPayment(userId);
        assertEquals("The credit payment amount should be $200.00", new BigDecimal("200.00"),
                paymentWS.getAmountAsDecimal());
        assertEquals("Payment method should be credit.", ServerConstants.PAYMENT_METHOD_CREDIT, paymentWS.getMethodId());

        // cleanup
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(orderId1);
        api.deleteUser(userId);
    }

    @Test(enabled = false)
    public void test020AssetRemovalWhenFinished () throws Exception {
        System.out.println("#test020AssetRemovalWhenFinished");

        // user for tests
        UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);
        OrderWS order = com.sapienter.jbilling.server.order.WSTest.createMockOrder(user.getUserId(), 1, new BigDecimal(
                60));
        order.setActiveSince(AsDate(2000, 11, 30));
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
        order.setActiveUntil(AsDate(2000, 12, 29));
        order.setPeriod(2); // monthly
        OrderLineWS line = order.getOrderLines()[0];
        line.setAssetIds(new Integer[0]);
        line.setItemId(1250);
        line.setAssetIds(new Integer[] { 1 });

        List<OrderChangeWS> orderChanges = new LinkedList<OrderChangeWS>();
        for (OrderLineWS lineWS : order.getOrderLines()) {
            orderChanges.add(OrderChangeBL.buildFromLine(lineWS, order, ORDER_CHANGE_STATUS_APPLY_ID));
        }

        int transitionCount = api.getAssetTransitions(1).length;

        // Create an order and test if the asset is updated.
        Integer orderId = api.createUpdateOrder(order, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]));
        System.out.println("Order id: " + orderId);

        AssetTransitionDTOEx[] assetTransitions = api.getAssetTransitions(1);

        assertEquals("As an order was created, the assets in it must have had their status changed",
                transitionCount + 1, assetTransitions.length);
        assertEquals("Asset Transition Previous Status must be 101", 101, assetTransitions[0].getPreviousStatusId()
                .intValue());
        assertEquals("Asset Transition New Status must be 102", 102, assetTransitions[0].getNewStatusId().intValue());

        AssetWS assetWS = api.getAsset(1);
        assertNotNull("Asset must be linked to an order line", assetWS.getOrderLineId());
        assertEquals("Asset must have status 102", 102, assetWS.getAssetStatusId().intValue());

        // The asset will get updated here because the order is about to finish,
        // so it must change the asset's status back to default
        api.createInvoice(user.getUserId(), false);

        System.out.println("User id: " + user.getUserId());

        assetTransitions = api.getAssetTransitions(1);

        assertEquals(
                "The order will expire after this billing process, the asset status should have had returned to default",
                transitionCount + 2, assetTransitions.length);
        assertEquals("Asset Transition Previous Status must be 102", 102, assetTransitions[0].getPreviousStatusId()
                .intValue());
        assertEquals("Asset Transition New Status must be 101", 101, assetTransitions[0].getNewStatusId().intValue());

        // Delete the order and test if the asset is updated.
        api.deleteOrder(orderId);

        assetWS = api.getAsset(1);
        assertNull("Asset must NOT be linked to an order line", assetWS.getOrderLineId());
        assertEquals("Asset must have status 101", 101, assetWS.getAssetStatusId().intValue());

        // clean up
        Integer[] invoiceIds = api.getAllInvoices(user.getUserId());
        System.out.println("Invoice ids: " + Arrays.toString(invoiceIds));
        InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
        api.deleteInvoice(invoice.getId());
        api.deleteUser(user.getUserId());
    }

    @Test(enabled = false)
    public void testIsBillingRunning () {
        boolean running;

        // test for correct entityId
        running = api.isBillingRunning(entityId);
        System.out.println("Is billing process running for entity " + entityId + " : " + running);
        assertFalse("Is billing process running for entity " + entityId, running);

        // test for incorrect entityId
        Integer testEntityId = 99999;
        running = api.isBillingRunning(testEntityId);
        System.out.println("Is billing process running for entity " + testEntityId + " : " + running);
        assertFalse("Is billing process running for entity " + testEntityId, running);

        // test for null entityId
        testEntityId = null;
        running = api.isBillingRunning(testEntityId);
        System.out.println("Is billing process running for entity " + testEntityId + " : " + running);
        assertFalse("Is billing process running for entity " + testEntityId, running);
    }

    private static Date parseDate (String str) throws Exception {
        if (str == null) {
            return null;
        }

        if (str.length() != 10 || str.charAt(4) != '-' || str.charAt(7) != '-') {
            throw new Exception("Can't parse " + str);
        }

        try {
            int year = Integer.valueOf(str.substring(0, 4)).intValue();
            int month = Integer.valueOf(str.substring(5, 7)).intValue();
            int day = Integer.valueOf(str.substring(8, 10)).intValue();

            Calendar cal = GregorianCalendar.getInstance();
            cal.clear();
            cal.set(year, month - 1, day);

            return cal.getTime();
        } catch (Exception e) {
            throw new Exception("Can't parse " + str);
        }
    }

    private InvoiceWS getReviewInvoice (Integer[] invoiceIds) {
        for (Integer id : invoiceIds) {
            InvoiceWS invoice = api.getInvoiceWS(id);
            if (invoice != null && invoice.getIsReview() == 1)
                return invoice;
        }
        return null;
    }

    private InvoiceWS getNonReviewInvoice (Integer[] invoiceIds) {
        for (Integer id : invoiceIds) {
            InvoiceWS invoice = api.getInvoiceWS(id);
            if (invoice != null && invoice.getIsReview() == 0)
                return invoice;
        }
        return null;
    }

    private void enableBasicOrderPeriodTask (JbillingAPI api) {
        PluggableTaskWS plugin = api.getPluginWS(ORDER_PERIOD_PLUGIN_ID);
        plugin.setTypeId(BASIC_ORDER_PERIOD_PLUGIN_TYPE_ID);

        api.updatePlugin(plugin);
    }

    private void enableProRateOrderPeriodTask (JbillingAPI api) {
        PluggableTaskWS plugin = api.getPluginWS(ORDER_PERIOD_PLUGIN_ID);
        plugin.setTypeId(PRO_RATE_ORDER_PERIOD_PLUGIN_TYPE_ID);

        api.updatePlugin(plugin);
    }
}
