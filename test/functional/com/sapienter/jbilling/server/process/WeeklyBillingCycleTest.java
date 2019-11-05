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

import static org.testng.AssertJUnit.assertEquals;

import java.util.Date;

import org.testng.annotations.Test;

import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.process.BillingProcessTestCase;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import java.util.*;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;
import java.lang.System;

/**
 * WeeklyBillingCycleTest Test class for Weekly Billing Cycle.
 * 
 * @author Usman Malik On 06 May 2014
 */
@Test(groups = { "billing-and-discounts", "billing" })
public class WeeklyBillingCycleTest extends BillingProcessTestCase {

    @Test
    public void testWeeklyBillingCycle () throws Exception {
        logger.info("#testWeeklyBillingCycle");

        // ------------------
        // ** C2 Scenario User and Order **
        // Create the user for C2 scenario and updated its next invoice date to
        // 06-Jan
        UserWS userC2 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderC2 = orderBuilderFactory.forUser(userC2).weekly().activeSince("01/01/2014").build();
        logger.info(S("OrderC2 id: {}", orderC2.getId()));
        logger.info(S("UserC2 id: {}", userC2.getUserId()));

        // ------------------
        // ** C1 Scenario User and Order **
        // Create the user for C1 scenario and updated its next invoice date to
        // 06-Jan
        UserWS userC1 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderC1 = orderBuilderFactory.forUser(userC1).daily().activeSince("01/01/2014").build();
        logger.info(S("OrderC1 id: {}", orderC1.getId()));
        logger.info(S("UserC1 id: {}", userC1.getUserId()));

        // ------------------
        // ** C3 Scenario User and Order **
        // Create the user for C3 scenario and updated its next invoice date to
        // 06-Jan
        UserWS userC3 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderC3 = orderBuilderFactory.forUser(userC3).weekly().prePaid().activeSince("01/01/2014").build();
        logger.info(S("OrderC3 id: {}", orderC3.getId()));
        logger.info(S("UserC3 id: {}", userC3.getUserId()));

        // ------------------
        // ** C4 Scenario User and Order **
        // Create the user for C4 scenario and updated its next invoice date to
        // 06-Jan
        UserWS userC4 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderC4 = orderBuilderFactory.forUser(userC4).weekly().prePaid().activeSince("01/08/2014").build();
        logger.info(S("OrderC4 id: {}", orderC4.getId()));
        logger.info(S("UserC4 id: {}", userC4.getUserId()));

        // ------------------
        // ** C5 Scenario User and Order **
        // Create the user for C5 scenario and updated its next invoice date to
        // 06-Jan
        UserWS userC5 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderC5 = orderBuilderFactory.forUser(userC5).weekly().activeSince("01/08/2014").build();
        logger.info(S("OrderC5 id: {}", orderC5.getId()));
        logger.info(S("UserC5 id: {}", userC5.getUserId()));

        // ------------------
        // ** C6 Scenario User and Order **
        // Create the user for C6 scenario and updated its next invoice date to
        // 06-Jan
        UserWS userC6 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderC6 = orderBuilderFactory.forUser(userC6).semiMonthly().activeSince("01/01/2014").build();
        logger.info(S("OrderC6 id: {}", orderC6.getId()));
        logger.info(S("UserC6 id: {}", userC6.getUserId()));

        // ------------------
        // ** C7 Scenario User and Order **
        // Create the user for C7 scenario and updated its next invoice date to
        // 06-Jan
        UserWS userC7 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderC7 = orderBuilderFactory.forUser(userC7).semiMonthly().prePaid().activeSince("01/01/2014").build();
        logger.info(S("OrderC7 id: {}", orderC7.getId()));
        logger.info(S("UserC7 id: {}", userC7.getUserId()));

        // ------------------
        // ** C8 Scenario User and Order **
        // Create the user for C8 scenario and updated its next invoice date to
        // 06-Jan
        UserWS userC8 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderC8 = orderBuilderFactory.forUser(userC8).monthly().activeSince("01/15/2014").build();
        logger.info(S("OrderC8 id: {}", orderC8.getId()));
        logger.info(S("UserC8 id: {}", userC8.getUserId()));

        // ------------------
        // ** C9 Scenario User and Order **
        // Create the user for C9 scenario and updated its next invoice date to
        // 06-Jan
        UserWS userC9 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderC9 = orderBuilderFactory.forUser(userC9).monthly().prePaid().activeSince("01/15/2014").build();
        logger.info(S("OrderC9 id: {}", orderC9.getId()));
        logger.info(S("UserC9 id: {}", userC9.getUserId()));

        // create pro rated scenarios

        int orderFailed = 0;
        try {
            UserWS userF1 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
            OrderWS orderF1 = orderBuilderFactory.forUser(userF1).daily().proRate(true).activeSince("01/01/2014")
                    .build();
            logger.info(S("OrderF1 id: {}", orderF1.getId()));
        } catch (SessionInternalError ex) {
            orderFailed = 1;
            logger.info(S("Order F1 failed {}", ex.getErrorMessages()[0]));
            assertContainsErrorBilling(ex, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        }
        assertEqualsBilling("Order F1 should not be saved", 1, orderFailed);
        orderFailed = 0;

        // F2
        UserWS userF2 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderF2 = orderBuilderFactory.forUser(userF2).weekly().proRate(true).activeSince("01/01/2014").build();
        logger.info(S("OrderF2 id: {}", orderF2.getId()));
        logger.info(S("UserF2 id: {}", userF2.getUserId()));

        // F3
        UserWS userF3 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderF3 = orderBuilderFactory.forUser(userF3).weekly().prePaid().proRate(true)
                .activeSince("01/01/2014").build();
        logger.info(S("OrderF3 id: {}", orderF3.getId()));
        logger.info(S("UserF3 id: {}", userF3.getUserId()));

        // F4
        UserWS userF4 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderF4 = orderBuilderFactory.forUser(userF4).weekly().prePaid().proRate(true)
                .activeSince("01/08/2014").build();
        logger.info(S("OrderF4 id: {}", orderF4.getId()));
        logger.info(S("UserF4 id: {}", userF4.getUserId()));

        // F5
        UserWS userF5 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
        OrderWS orderF5 = orderBuilderFactory.forUser(userF5).weekly().proRate(true).activeSince("01/08/2014").build();
        logger.info(S("OrderF5 id: {}", orderF5.getId()));
        logger.info(S("UserF5 id: {}", userF5.getUserId()));

        // F6
        try {
            UserWS userF6 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
            OrderWS orderF6 = orderBuilderFactory.forUser(userF6).semiMonthly().proRate(true).activeSince("01/01/2014")
                    .build();
            logger.info(S("OrderF6 id: {}", orderF6.getId()));
        } catch (SessionInternalError ex) {
            orderFailed = 1;
            logger.info(S("Order F6 failed {}", ex.getMessage()));
            assertContainsErrorBilling(ex, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        }
        assertEqualsBilling("Order F6 should not be saved", 1, orderFailed);
        orderFailed = 0;

        // F5A
        try {
            UserWS userF5A = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
            OrderWS orderF5A = orderBuilderFactory.forUser(userF5A).semiMonthly().prePaid().proRate(true)
                    .activeSince("01/01/2014").build();
            logger.info(S("OrderF5A id: {}", orderF5A.getId()));
        } catch (SessionInternalError ex) {
            orderFailed = 1;
            logger.info(S("Order F5A failed {}", ex.getMessage()));
            assertContainsErrorBilling(ex, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        }
        assertEqualsBilling("Order F5A should not be saved", 1, orderFailed);
        orderFailed = 0;

        // F7
        try {
            UserWS userF7 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
            OrderWS orderF7 = orderBuilderFactory.forUser(userF7).monthly().proRate(true).activeSince("01/15/2014")
                    .build();
            logger.info(S("OrderF7 id: {}", orderF7.getId()));
        } catch (SessionInternalError ex) {
            orderFailed = 1;
            logger.info(S("Order F7 failed {}", ex.getMessage()));
            assertContainsErrorBilling(ex, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        }
        assertEqualsBilling("Order F7 should not be saved", 1, orderFailed);
        orderFailed = 0;

        // F8
        try {
            UserWS userF8 = weeklyBilledUserBuilder.nextInvoiceDate("01/06/2014").build();
            OrderWS orderF8 = orderBuilderFactory.forUser(userF8).monthly().prePaid().proRate(true)
                    .activeSince("01/15/2014").build();
            logger.info(S("OrderF8 id: {}", orderF8.getId()));
        } catch (SessionInternalError ex) {
            orderFailed = 1;
            logger.info(S("Order F8 failed {}", ex.getMessage()));
            assertContainsErrorBilling(ex, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        }
        assertEqualsBilling("Order F8 should not be saved", 1, orderFailed);

        /**
         * Not Pro-Rate Scenario Billing Run date = 6th Jan 2014 for Billing
         * sceanrio C1 - C9 And Pro-Rate Scenario F2 and F5
         */
        triggerBilling(AsDate("01/06/2014"));

        scenarioVerifier.forOrder(orderC1).nbd("01/06/2014").nid("01/13/2014").from("01/01/2014").to("01/01/2014")
                .invoiceLines(5).verify();
        scenarioVerifier.forOrder(orderC2).nbd("01/01/2014").nid("01/13/2014").from("01/01/2014").to("01/07/2014")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderC3).nbd("01/15/2014").nid("01/13/2014").from("01/01/2014").to("01/07/2014")
                .invoiceLines(2).verify();
        scenarioVerifier.forOrder(orderC4).nbd("01/15/2014").nid("01/13/2014").from("01/08/2014").to("01/14/2014")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC5).nbd("01/08/2014").nid("01/13/2014").from("01/01/2014").to("01/07/2014")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderC6).nbd("01/01/2014").nid("01/13/2014").from("01/01/2014").to("01/16/2014")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderC7).nbd("01/16/2014").nid("01/13/2014").from("01/01/2014").to("01/15/2014")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC8).nbd("01/15/2014").nid("01/13/2014").from("01/01/2014").to("01/31/2014")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderC9).nbd("01/15/2014").nid("01/13/2014").from("01/01/2014").to("01/31/2014")
                .invoiceLines(0).verify();

        // // Pro rated scenarios asserts
        scenarioVerifier.forOrder(orderF2).nbd("01/06/2014").nid("01/13/2014").from("01/01/2014").to("01/05/2014")
                .invoiceLines(1).verify();
        testF3(orderF3);
        scenarioVerifier.forOrder(orderF4).nbd("01/13/2014").nid("01/13/2014").from("01/08/2014").to("01/12/2014")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderF5).nbd("01/08/2014").nid("01/13/2014").from("01/08/2014").to("01/12/2014")
                .invoiceLines(0).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/06/2014"));
        // End 6th Jan Billing Run

        /**
         * Not Pro-Rate Scenario Billing Run date =13th Jan 2014 for Billing
         * sceanrio C1 - C9 And Pro-Rate Scenario F2 and F5
         */
        triggerBilling(AsDate("01/13/2014"));

        scenarioVerifier.forOrder(orderC1).nbd("01/13/2014").nid("01/20/2014").from("01/06/2014").to("01/06/2014")
                .invoiceLines(7).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC2).nbd("01/08/2014").nid("01/20/2014").from("01/01/2014").to("01/07/2014")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC3).nbd("01/22/2014").nid("01/20/2014").from("01/15/2014").to("01/21/2014")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC4).nbd("01/22/2014").nid("01/20/2014").from("01/15/2014").to("01/21/2014")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC5).nbd("01/08/2014").nid("01/20/2014").from("01/01/2014").to("01/07/2014")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderC6).nbd("01/01/2014").nid("01/20/2014").from("01/01/2014").to("01/16/2014")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderC7).nbd("02/01/2014").nid("01/20/2014").from("01/16/2014").to("01/31/2014")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC8).nbd("01/15/2014").nid("01/20/2014").from("01/01/2014").to("01/31/2014")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderC9).nbd("02/15/2014").nid("01/20/2014").from("01/15/2014").to("02/14/2014")
                .invoiceLines(1).verify();

        // Pro rated scenarios asserts
        scenarioVerifier.forOrder(orderF2).nbd("01/13/2014").nid("01/20/2014").from("01/06/2014").to("01/12/2014")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderF3).nbd("01/20/2014").nid("01/20/2014").from("01/13/2014").to("01/19/2014")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderF4).nbd("01/20/2014").nid("01/20/2014").from("01/13/2014").to("01/19/2014")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderF5).nbd("01/13/2014").nid("01/20/2014").from("01/08/2014").to("01/12/2014")
                .invoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/13/2014"));
        // End 13th Jan 2014 Billing run.

        /**
         * Not Pro-Rate Scenario Billing Run date =20th Jan 2014 for Billing
         * sceanrio C1 - C9 And Pro-Rate Scenario F2 and F5
         */
        triggerBilling(AsDate("01/20/2014"));

        scenarioVerifier.forOrder(orderC2).nbd("01/15/2014").nid("01/27/2014").from("01/08/2014").to("01/14/2014")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC3).nbd("01/29/2014").nid("01/27/2014").from("01/22/2014").to("01/28/2014")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderC4).nbd("01/29/2014").nid("01/27/2014").from("01/22/2014").to("01/28/2014")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderC5).nbd("01/15/2014").nid("01/27/2014").from("01/08/2014").to("01/14/2014")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC6).nbd("01/16/2014").nid("01/27/2014").from("01/01/2014").to("01/15/2014")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC7).nbd("02/01/2014").nid("01/27/2014").from("01/16/2014").to("01/31/2014")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC8).nbd("01/15/2014").nid("01/27/2014").from("01/01/2014").to("01/31/2014")
                .invoiceLines(0).verify();
        // StartDateC9At20Jan.set(BILLING_PROCESS_YEAR, Calendar.JANUARY, 15);
        // EndDateC9At20Jan.set(BILLING_PROCESS_YEAR, Calendar.JANUARY, 14);
        scenarioVerifier.forOrder(orderC9).nbd("02/15/2014").nid("01/27/2014").from("01/15/2014").to("02/14/2014")
                .invoiceLines(0).dueInvoiceLines(1).verify();

        // Pro rated scenarios asserts
        scenarioVerifier.forOrder(orderF2).nbd("01/20/2014").nid("01/27/2014").from("01/13/2014").to("01/19/2014")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderF3).nbd("01/27/2014").nid("01/27/2014").from("01/20/2014").to("01/26/2014")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderF4).nbd("01/27/2014").nid("01/27/2014").from("01/20/2014").to("01/26/2014")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderF5).nbd("01/20/2014").nid("01/27/2014").from("01/13/2014").to("01/19/2014")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/20/2014"));
        // End of Billing 20th Jan 2014

        /**
         * Not Pro-Rate Scenario Billing Run date =27th Jan 2014 for Billing
         * sceanrio C2 - C9 And Pro-Rate Scenario F2 and F5
         */
        triggerBilling(AsDate("01/27/2014"));

        scenarioVerifier.forOrder(orderC2).nbd("01/22/2014").nid("02/03/2014").from("01/15/2014").to("01/21/2014")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderC3).nbd("02/05/2014").nid("02/03/2014").from("01/29/2014").to("02/04/2014")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderC4).nbd("02/05/2014").nid("02/03/2014").from("01/29/2014").to("02/04/2014")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderC5).nbd("01/22/2014").nid("02/03/2014").from("01/15/2014").to("01/21/2014")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC6).nbd("01/16/2014").nid("02/03/2014").from("01/01/2014").to("01/15/2014")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderC7).nbd("02/16/2014").nid("02/03/2014").from("02/01/2014").to("02/15/2014")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderC8).nbd("01/15/2014").nid("02/03/2014").from("01/01/2014").to("01/31/2014")
                .invoiceLines(0).verify();
        // StartDateC9At27Jan.set(BILLING_PROCESS_YEAR, Calendar.JANUARY, 15);
        // EndDateC9At27Jan.set(BILLING_PROCESS_YEAR, Calendar.JANUARY, 14);
        scenarioVerifier.forOrder(orderC9).nbd("02/15/2014").nid("02/03/2014").from("01/15/2014").to("02/14/2014")
                .invoiceLines(1).verify();

        // Pro rated scenarios asserts
        scenarioVerifier.forOrder(orderF2).nbd("01/27/2014").nid("02/03/2014").from("01/20/2014").to("01/26/2014")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderF3).nbd("02/03/2014").nid("02/03/2014").from("01/27/2014").to("02/02/2014")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderF4).nbd("02/03/2014").nid("02/03/2014").from("01/27/2014").to("02/02/2014")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderF5).nbd("01/27/2014").nid("02/03/2014").from("01/20/2014").to("01/26/2014")
                .invoiceLines(1).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/27/2014"));
        // End of Billing 27th Jan 2014

        /**
         * Not Pro-Rate Scenario Billing Run date =3rd Feb 2014 for Billing
         * sceanrio C2 - C7 And Pro-Rate Scenario F2 and F5
         */
        triggerBilling(AsDate("02/03/2014"));

        scenarioVerifier.forOrder(orderC2).nbd("01/29/2014").nid("02/10/2014").from("01/22/2014").to("01/28/2014")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderC3).nbd("02/12/2014").nid("02/10/2014").from("02/05/2014").to("02/11/2014")
                .invoiceLines(1).dueInvoiceLines(4).verify();
        scenarioVerifier.forOrder(orderC4).nbd("02/12/2014").nid("02/10/2014").from("02/05/2014").to("02/11/2014")
                .invoiceLines(1).dueInvoiceLines(4).verify();
        scenarioVerifier.forOrder(orderC5).nbd("01/29/2014").nid("02/10/2014").from("01/22/2014").to("01/28/2014")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderC6).nbd("02/01/2014").nid("02/10/2014").from("01/16/2014").to("01/31/2014")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ?? .from("01/16/2014").to("01/31/2014")
        scenarioVerifier.forOrder(orderC7).nbd("02/16/2014").nid("02/10/2014").from("02/01/2014").to("02/15/2014")
                .invoiceLines(1).dueInvoiceLines(2).verify();

        // Pro rated scenarios asserts
        scenarioVerifier.forOrder(orderF2).nbd("02/03/2014").nid("02/10/2014").from("01/27/2014").to("02/02/2014")
                .invoiceLines(1).dueInvoiceLines(4).verify();
        scenarioVerifier.forOrder(orderF3).nbd("02/10/2014").nid("02/10/2014").from("02/03/2014").to("02/09/2014")
                .invoiceLines(1).dueInvoiceLines(4).verify();
        scenarioVerifier.forOrder(orderF4).nbd("02/10/2014").nid("02/10/2014").from("02/03/2014").to("02/09/2014")
                .invoiceLines(1).dueInvoiceLines(4).verify();
        scenarioVerifier.forOrder(orderF5).nbd("02/03/2014").nid("02/10/2014").from("01/27/2014").to("02/02/2014")
                .invoiceLines(1).dueInvoiceLines(3).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/03/2014"));
        // End of Billing 3rd Feb 2014

        /**
         * Not Pro-Rate Scenario Billing Run date =10th Feb 2014 for Billing
         * sceanrio C9
         */
        triggerBilling(AsDate("02/10/2014"));

        // StartDateC9At10Feb.set(BILLING_PROCESS_YEAR, Calendar.JANUARY, 15);
        // EndDateC9At10Feb.set(BILLING_PROCESS_YEAR, Calendar.JANUARY, 14);
        scenarioVerifier.forOrder(orderC9).nbd("03/15/2014").nid("02/17/2014").from("01/15/2014").to("02/14/2014")
                .invoiceLines(0).skipPreviousInvoice().verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/10/2014"));
        // End of 10th Feb 2014 Billing Run.

        /**
         * Not Pro-Rate Scenario Billing Run date =17th Feb 2014 for Billing
         * sceanrio C8
         */
        triggerBilling(AsDate("02/17/2014"));

        scenarioVerifier.forOrder(orderC8).nbd("02/15/2014").nid("02/24/2014").from("01/15/2014").to("02/14/2014")
                .invoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/17/2014"));
        // End of 17th Feb 2014 Billing Run.
    }

    /**
     * @param orderF3
     */
    private void testF3 (OrderWS orderF3) {

        for (InvoiceWS invoice : api.getAllInvoicesForUser(orderF3.getUserId())) {
            if (!invoice.getCreateDateTime().equals(AsDate("01/06/2014"))) {
                continue; // skip other invoices
            }
            logger.info(S("Generated Invoice Id :{}", invoice.getId()));
            logger.info(S("Total Invoice lines  :{}", invoice.getInvoiceLines().length));
            assertEqualsBilling("Invoice Lines should be 2 ", 2, invoice.getInvoiceLines().length);
            api.getOrderProcessesByInvoice(invoice.getId());
            for (com.sapienter.jbilling.server.entity.InvoiceLineDTO line : invoice.getInvoiceLines()) {
                logger.info(S("Getting all invoice lines : {}", line));
            }
            com.sapienter.jbilling.server.entity.InvoiceLineDTO line = invoice.getInvoiceLines()[0];
            assertEqualsBilling("1st description for InvoiceWSF3At6Jan should be: ",
                    S("Order line: {} Period from 01/01/2014 to 01/05/2014", line.getItemId()),
                   line.getDescription());

            line = invoice.getInvoiceLines()[1];
            assertEqualsBilling("2nd description for InvoiceWSF3At6Jan should be: ",
                    S("Order line: {} Period from 01/06/2014 to 01/12/2014", line.getItemId()),
                    line.getDescription());
        }
        assertNextBillableDay(api.getOrder(orderF3.getId()), AsDate("01/13/2014"));
        assertNextInvoiceDate(api.getUserWS(orderF3.getUserId()), AsDate("01/13/2014"));
    }
}
