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

import java.util.ArrayList;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.process.BillingProcessTestCase;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;

import org.springframework.util.Assert;
import org.testng.annotations.Test;

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
 * SemiMonthlyBillingCycleTest Test class for Semi-Monthly Billing Cycle.
 * 
 * @author Maryam Rehman On 11 june 2014
 */
@Test(groups = { "billing-and-discounts", "billing" })
public class SemiMonthlyBillingCycleTest extends BillingProcessTestCase {

    @Test
    public void testSemiMonthlyScenarios () throws Exception {
        logger.info("#testSemiMonthlyNotProRated");

        UserWS userD1 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
        OrderWS orderD1 = orderBuilderFactory.forUser(userD1).daily().build();
        logger.info(S("OrderD1 id: {}", orderD1.getId()));
        logger.info(S("UserD1 id: {}", userD1.getUserId()));

        UserWS userD2 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/16/2009").build();
        OrderWS orderD2 = orderBuilderFactory.forUser(userD2).weekly().activeSince("01/01/2009").build();
        logger.info(S("OrderD2 id: {}", orderD2.getId()));
        logger.info(S("UserD2 id: {}", userD2.getUserId()));

        UserWS userD3 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
        OrderWS orderD3 = orderBuilderFactory.forUser(userD3).semiMonthly().build();
        logger.info(S("OrderD3 id: {}", orderD3.getId()));
        logger.info(S("UserD3 id: {}", userD3.getUserId()));

        UserWS userD4 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
        OrderWS orderD4 = orderBuilderFactory.forUser(userD4).semiMonthly().prePaid().build();
        logger.info(S("OrderD4 id: {}", orderD4.getId()));
        logger.info(S("UserD4 id: {}", userD4.getUserId()));

        UserWS userD5 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
        OrderWS orderD5 = orderBuilderFactory.forUser(userD5).semiMonthly().activeSince("01/04/2009").build();
        logger.info(S("OrderD5 id: {}", orderD5.getId()));
        logger.info(S("UserD5 id: {}", userD5.getUserId()));

        UserWS userD6 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
        OrderWS orderD6 = orderBuilderFactory.forUser(userD6).semiMonthly().prePaid().activeSince("01/04/2009").build();
        logger.info(S("OrderD6 id: {}", orderD6.getId()));
        logger.info(S("UserD6 id: {}", userD6.getUserId()));

        UserWS userD7 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
        OrderWS orderD7 = orderBuilderFactory.forUser(userD7).monthly().activeSince("01/15/2009").build();
        logger.info(S("OrderD7 id: {}", orderD7.getId()));
        logger.info(S("UserD7 id: {}", userD7.getUserId()));

        UserWS userD8 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/16/2009").build();
        OrderWS orderD8 = orderBuilderFactory.forUser(userD8).monthly().prePaid().activeSince("01/15/2009").build();
        logger.info(S("OrderD8 id: {}", orderD8.getId()));
        logger.info(S("UserD8 id: {}", userD8.getUserId()));

        // pro-rated data creation
        // E1
        Integer orderIdE1 = null;
        try {
            UserWS userE1 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
            OrderWS orderE1 = orderBuilderFactory.forUser(userE1).daily().proRate(true).build();
            orderIdE1 = orderE1.getId();
            logger.info(S("OrderE1 id: {}", orderIdE1));
        } catch (SessionInternalError ex) {
            logger.info(S("Order E1 failed {}", ex.getErrorMessages()[0]));
            assertContainsErrorBilling(ex, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        } finally {
            Assert.isNull(orderIdE1, "Order E1 should be null");
        }

        // E2
        Integer orderIdE2 = null;
        try {
            UserWS userE2 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
            OrderWS orderE2 = orderBuilderFactory.forUser(userE2).weekly().proRate(true).build();
            orderIdE2 = orderE2.getId();
            logger.info(S("OrderE2 id: {}", orderIdE2));
        } catch (SessionInternalError ex) {
            logger.info(S("Order E2 failed {}", ex.getErrorMessages()[0]));
            assertContainsErrorBilling(ex, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        } finally {
            Assert.isNull(orderIdE2, "Order E2 should be null");
        }

        // E3
        UserWS userE3 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
        OrderWS orderE3 = orderBuilderFactory.forUser(userE3).semiMonthly().proRate(true).activeSince("12/30/2008")
                .build();
        logger.info(S("OrderE3 id: {}", orderE3.getId()));
        logger.info(S("UserE3 id: {}", userE3.getUserId()));

        // E4
        UserWS userE4 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
        OrderWS orderE4 = orderBuilderFactory.forUser(userE4).semiMonthly().prePaid().proRate(true)
                .activeSince("12/30/2008").build();
        logger.info(S("OrderE4 id: {}", orderE4.getId()));
        logger.info(S("UserE4 id: {}", userE4.getUserId()));

        // E5
        UserWS userE5 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
        OrderWS orderE5 = orderBuilderFactory.forUser(userE5).semiMonthly().proRate(true).activeSince("01/04/2009")
                .build();
        logger.info(S("OrderE5 id: {}", orderE5.getId()));
        logger.info(S("UserE5 id: {}", userE5.getUserId()));

        // E6
        UserWS userE6 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
        OrderWS orderE6 = orderBuilderFactory.forUser(userE6).semiMonthly().prePaid().proRate(true)
                .activeSince("01/04/2009").build();
        logger.info(S("OrderE6 id: {}", orderE6.getId()));
        logger.info(S("UserE6 id: {}", userE6.getUserId()));

        // E7
        Integer orderIdE7 = null;
        try {
            UserWS userE7 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
            OrderWS orderE7 = orderBuilderFactory.forUser(userE7).monthly().proRate(true).activeSince("01/15/2009")
                    .build();
            orderIdE7 = orderE7.getId();
            logger.info(S("OrderE7 id: {}", orderIdE7));
        } catch (SessionInternalError ex) {
            logger.info(S("Order E7 failed {}", ex.getErrorMessages()[0]));
            assertContainsErrorBilling(ex, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        } finally {
            Assert.isNull(orderIdE7, "Order E7 should be null");
        }

        // E8
        Integer orderIdE8 = null;
        try {
            UserWS userE8 = semiMonthlyBilledUserBuilder.nextInvoiceDate("01/01/2009").build();
            OrderWS orderE8 = orderBuilderFactory.forUser(userE8).monthly().proRate(true).activeSince("01/15/2009")
                    .build();
            orderIdE8 = orderE8.getId();
            logger.info(S("OrderE8 id: {}", orderIdE8));
        } catch (SessionInternalError ex) {
            logger.info(S("Order E8 failed {}", ex.getErrorMessages()[0]));
            assertContainsErrorBilling(ex, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        } finally {
            Assert.isNull(orderIdE8, "Order E8 should be null");
        }

        // Started Billing process for January 1st 2009.
        triggerBilling(AsDate("01/01/2009"));

        // Date periodStartDate = new Date();
        // Date periodEndDate = new Date();
        scenarioVerifier.forOrder(orderD1).nbd("01/01/2009").nid("01/16/2009").from("01/01/2014").to("01/01/2014")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderD3).nbd("01/01/2009").nid("01/16/2009").from("01/01/2014").to("01/01/2014")
                .invoiceLines(0).verify();
        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ?? .to("01/16/2009")
        scenarioVerifier.forOrder(orderD4).nbd("01/16/2009").nid("01/16/2009").from("01/01/2009").to("01/15/2009")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderD5).nbd("01/04/2009").nid("01/16/2009").from("01/01/2009").to("01/16/2009")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderD6).nbd("01/19/2009").nid("01/16/2009").from("01/04/2009").to("01/18/2009")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderD7).nbd("01/15/2009").nid("01/16/2009").from("01/04/2009").to("01/18/2009")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderD8).nbd("01/15/2009").nid("01/16/2009").from("12/30/2008").to("12/31/2008")
                .invoiceLines(0).verify();
        // Setting expected results E4
        // old way as it has 1 partial and 1 full order lines
        testE4(orderE4);
        scenarioVerifier.forOrder(orderE5).nbd("01/04/2009").nid("01/16/2009").from("12/30/2008").to("12/31/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderE6).nbd("01/16/2009").nid("01/16/2009").from("01/04/2009").to("01/15/2009")
                .invoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/01/2009"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 15th Jan 2009 for Billing
         * sceanrio D8
         */
        triggerBilling(AsDate("01/15/2009"));

        scenarioVerifier.forOrder(orderD8).nbd("01/15/2009").nid("01/16/2009").from("01/04/2009").to("01/15/2009")
                .invoiceLines(0).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/15/2009"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 16th Jan 2009 for Billing
         * sceanrio D1 - D8 And Pro-Rate Scenario E3 and E6
         */
        triggerBilling(AsDate("01/16/2009"));

        scenarioVerifier.forOrder(orderD1).nbd("01/16/2009").nid("02/01/2009").from("01/01/2009").to("01/01/2009")
                .invoiceLines(15).verify();
        scenarioVerifier.forOrder(orderD2).nbd("01/15/2009").nid("02/01/2009").from("01/01/2009").to("01/07/2009")
                .invoiceLines(2).verify();
        scenarioVerifier.forOrder(orderD3).nbd("01/16/2009").nid("02/01/2009").from("01/01/2009").to("01/15/2009")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderD4).nbd("02/01/2009").nid("02/01/2009").from("01/16/2009").to("01/31/2009")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderD5).nbd("01/04/2009").nid("02/01/2009").from("01/16/2009").to("01/31/2009")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderD6).nbd("02/04/2009").nid("02/01/2009").from("01/19/2009").to("02/03/2009")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderD7).nbd("01/15/2009").nid("02/01/2009").from("01/19/2009").to("02/03/2009")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderD8).nbd("02/15/2009").nid("02/01/2009").from("01/15/2009").to("02/14/2009")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderE3).nbd("01/16/2009").nid("02/01/2009").from("01/01/2009").to("01/15/2009")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderE4).nbd("02/01/2009").nid("02/01/2009").from("01/16/2009").to("01/31/2009")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderE5).nbd("01/16/2009").nid("02/01/2009").from("01/04/2009").to("01/15/2009")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderE6).nbd("02/01/2009").nid("02/01/2009").from("01/16/2009").to("01/31/2009")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/16/2009"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 1st Feb 2009 for Billing
         * sceanrio D1 - D8 And Pro-Rate Scenario E3 and E6
         */
        triggerBilling(AsDate("02/01/2009"));

        scenarioVerifier.forOrder(orderD1).nbd("02/01/2009").nid("02/16/2009").from("01/16/2009").to("01/16/2009")
                .invoiceLines(16).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderD2).nbd("01/29/2009").nid("02/16/2009").from("01/15/2009").to("01/21/2009")
                .invoiceLines(2).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderD3).nbd("02/01/2009").nid("02/16/2009").from("01/16/2009").to("01/31/2009")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderD4).nbd("02/16/2009").nid("02/16/2009").from("02/01/2009").to("02/15/2009")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderD5).nbd("01/19/2009").nid("02/16/2009").from("01/04/2009").to("01/18/2009")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderD6).nbd("02/19/2009").nid("02/16/2009").from("02/04/2009").to("02/18/2009")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderD7).nbd("01/15/2009").nid("02/16/2009").from("02/04/2009").to("02/18/2009")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderD8).nbd("03/15/2009").nid("02/16/2009").from("02/15/2009").to("03/14/2009")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderE3).nbd("02/01/2009").nid("02/16/2009").from("01/16/2009").to("01/31/2009")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderE4).nbd("02/16/2009").nid("02/16/2009").from("02/01/2009").to("02/15/2009")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderE5).nbd("02/01/2009").nid("02/16/2009").from("01/16/2009").to("01/31/2009")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderE6).nbd("02/16/2009").nid("02/16/2009").from("02/01/2009").to("02/15/2009")
                .invoiceLines(1).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/01/2009"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 16th Feb 2009 for Billing
         * sceanrio D1 - D8 And Pro-Rate Scenario E3 and E6
         */
        triggerBilling(AsDate("02/16/2009"));

        scenarioVerifier.forOrder(orderD2).nbd("02/12/2009").nid("03/01/2009").from("01/29/2009").to("02/04/2009")
                .invoiceLines(2).dueInvoiceLines(2).verify();
        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ?? .from("02/14/2009").to("02/15/2009")
        scenarioVerifier.forOrder(orderD3).nbd("02/16/2009").nid("03/01/2009").from("02/01/2009").to("02/15/2009")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderD4).nbd("03/01/2009").nid("03/01/2009").from("02/16/2009").to("02/28/2009")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderD5).nbd("02/04/2009").nid("03/01/2009").from("01/19/2009").to("02/03/2009")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ?? .to("03/01/2009")
        scenarioVerifier.forOrder(orderD6).nbd("03/04/2009").nid("03/01/2009").from("02/19/2009").to("03/03/2009")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderD7).nbd("02/15/2009").nid("03/01/2009").from("01/15/2009").to("02/14/2009")
                .invoiceLines(1).verify();
        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ?? .from("01/15/2009").to("02/14/2009")
        scenarioVerifier.forOrder(orderD8).nbd("03/15/2009").nid("03/01/2009").from("02/15/2009").to("03/14/2009")
                .invoiceLines(0).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderE3).nbd("02/16/2009").nid("03/01/2009").from("02/01/2009").to("02/15/2009")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderE4).nbd("03/01/2009").nid("03/01/2009").from("02/16/2009").to("02/28/2009")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderE5).nbd("02/16/2009").nid("03/01/2009").from("02/01/2009").to("02/15/2009")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderE6).nbd("03/01/2009").nid("03/01/2009").from("02/16/2009").to("02/28/2009")
                .invoiceLines(1).dueInvoiceLines(3).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/16/2009"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 1st March 2009 for Billing
         * sceanrio D1 - D8 And Pro-Rate Scenario E3 and E6
         */
        triggerBilling(AsDate("03/01/2009"));

        scenarioVerifier.forOrder(orderD2).nbd("02/26/2009").nid("03/16/2009").from("02/12/2009").to("02/18/2009")
                .invoiceLines(2).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderD3).nbd("03/01/2009").nid("03/16/2009").from("02/16/2009").to("02/28/2009")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderD4).nbd("03/16/2009").nid("03/16/2009").from("03/01/2009").to("03/15/2009")
                .invoiceLines(1).dueInvoiceLines(4).verify();
        scenarioVerifier.forOrder(orderD5).nbd("02/19/2009").nid("03/16/2009").from("02/04/2009").to("02/18/2009")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderD6).nbd("03/19/2009").nid("03/16/2009").from("03/04/2009").to("03/18/2009")
                .invoiceLines(1).dueInvoiceLines(4).verify();
        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ??  .from("03/04/2009").to("03/18/2009")
        scenarioVerifier.forOrder(orderD7).nbd("02/15/2009").nid("03/16/2009").from("01/15/2009").to("02/14/2009")
                .invoiceLines(0).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderD8).nbd("04/15/2009").nid("03/16/2009").from("03/15/2009").to("04/14/2009")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderE3).nbd("03/01/2009").nid("03/16/2009").from("02/16/2009").to("02/28/2009")
                .invoiceLines(1).dueInvoiceLines(4).verify();
        scenarioVerifier.forOrder(orderE4).nbd("03/16/2009").nid("03/16/2009").from("03/01/2009").to("03/15/2009")
                .invoiceLines(1).dueInvoiceLines(4).verify();
        scenarioVerifier.forOrder(orderE5).nbd("03/01/2009").nid("03/16/2009").from("02/16/2009").to("02/28/2009")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderE6).nbd("03/16/2009").nid("03/16/2009").from("03/01/2009").to("03/15/2009")
                .invoiceLines(1).dueInvoiceLines(4).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("03/01/2009"));
    }

    private void testE4 (OrderWS orderE4) {
        for (InvoiceWS invoice : api.getAllInvoicesForUser(orderE4.getUserId())) {
            if (!invoice.getCreateDateTime().equals(AsDate("01/01/2009"))) {
                continue; // skip other invoices
            }
            logger.info(S("Generated Invoice Id :{}", invoice.getId()));
            logger.info(S("Total Invoice lines  :{}", invoice.getInvoiceLines().length));
            assertEqualsBilling("Invoice Lines should be 2 ", 2, invoice.getInvoiceLines().length);
            api.getOrderProcessesByInvoice(invoice.getId());
            for (com.sapienter.jbilling.server.entity.InvoiceLineDTO line : invoice.getInvoiceLines()) {
                logger.info(S("Getting all invoice lines : {}", line));
            }
            //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ??  to 01/01/2009
            com.sapienter.jbilling.server.entity.InvoiceLineDTO line = invoice.getInvoiceLines()[0];
            assertEqualsBilling("1st description for InvoiceWSE4At6Jan should be: ",
                    S("Order line: {} Period from 12/30/2008 to 12/31/2008", line.getItemId()),
                    line.getDescription());

            line = invoice.getInvoiceLines()[1];
            assertEqualsBilling("2nd description for InvoiceWSE4At6Jan should be: ",
                    S("Order line: {} Period from 01/01/2009 to 01/15/2009", line.getItemId()),
                    line.getDescription());
        }
        assertNextBillableDay(api.getOrder(orderE4.getId()), AsDate("01/16/2009"));
        assertNextInvoiceDate(api.getUserWS(orderE4.getUserId()), AsDate("01/16/2009"));
    }
}
