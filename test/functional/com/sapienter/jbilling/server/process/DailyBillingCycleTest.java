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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import com.sapienter.jbilling.server.process.BillingProcessTestCase;
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
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderProcessWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.testng.annotations.Test;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import static org.testng.AssertJUnit.*;
import java.lang.System;

/**
 * DailyBillingCycleTest Test class for Daily Billing Cycle.
 * 
 * @author Sagar Dond On 15 june 2014
 */
@Test(groups = { "billing-and-discounts", "billing" })
public class DailyBillingCycleTest extends BillingProcessTestCase  {

    @Test
    public void testDailyBillingCycle () throws Exception {
        logger.info("#testDailyBillingCycle");

        // ** B1 Scenario User and Order **
        // Create the user for B1 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB1 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2008").build();
        OrderWS orderB1 = orderBuilderFactory.forUser(userB1).weekly().build();
        logger.info(S("OrderB1 id: {}", orderB1.getId()));
        logger.info(S("UserB1 id: {}", userB1.getUserId()));

        // ------------------
        // ** B2 Scenario User and Order **
        // Create the user for B2 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB2 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2008").build();
        OrderWS orderB2 = orderBuilderFactory.forUser(userB2).weekly().prePaid().build();
        logger.info(S("OrderB2 id: {}", orderB2.getId()));
        logger.info(S("UserB2 id: {}", userB2.getUserId()));

        // ------------------
        // ** B3 Scenario User and Order **
        // Create the user for B3 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB3 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2008").build();
        OrderWS orderB3 = orderBuilderFactory.forUser(userB3).semiMonthly().build();
        logger.info(S("OrderB3 id: {}", orderB3.getId()));
        logger.info(S("UserB3 id: {}", userB3.getUserId()));

        // ------------------
        // ** B4 Scenario User and Order **
        // Create the user for B4 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB4 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2008").build();
        OrderWS orderB4 = orderBuilderFactory.forUser(userB4).semiMonthly().prePaid().build();
        logger.info(S("OrderB4 id: {}", orderB4.getId()));
        logger.info(S("UserB4 id: {}", userB4.getUserId()));

        // ------------------
        // ** B5 Scenario User and Order **
        // Create the user for B5 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB5 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2008").build();
        OrderWS orderB5 = orderBuilderFactory.forUser(userB5).monthly().activeSince("01/15/2008").build();
        logger.info(S("OrderB5 id: {}", orderB5.getId()));
        logger.info(S("UserB5 id: {}", userB5.getUserId()));

        // ------------------
        // ** B6 Scenario User and Order **
        // Create the user for B1 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB6 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2008").build();
        OrderWS orderB6 = orderBuilderFactory.forUser(userB6).monthly().prePaid().activeSince("01/15/2008").build();
        logger.info(S("OrderB6 id: {}", orderB6.getId()));
        logger.info(S("UserB6 id: {}", userB6.getUserId()));

        // create pro rated scenarios

        // ------------------
        // ** A1 Scenario User and Order **
        // Create the user for A1 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userA1 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2008").build();
        OrderWS orderA1 = orderBuilderFactory.forUser(userA1).daily().proRate(true).build();
        logger.info(S("OrderA1 id: {}", orderA1.getId()));
        logger.info(S("UserA1 id: {}", userA1.getUserId()));

        // ------------------
        // ** A2 Scenario User and Order **
        // Create the user for A2 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userA2 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2008").build();
        OrderWS orderA2 = orderBuilderFactory.forUser(userA2).daily().prePaid().proRate(true).build();
        logger.info(S("OrderA2 id: {}", orderA2.getId()));
        logger.info(S("UserA2 id: {}", userA2.getUserId()));

        /**
         * Not Pro-Rate Scenario Billing Run date = 1st Jan 2008 for Billing sceanrio B1 - B6 And Pro-Rate Scenario A1
         * and A2
         */
        triggerBilling(AsDate("01/01/2008"));

        // Not Pro rated scenarios asserts
        scenarioVerifier.forOrder(orderB1).nbd("01/01/2008").nid("01/02/2008").from("01/01/2008").to("01/07/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/08/2008").nid("01/02/2008").from("01/01/2008").to("01/07/2008")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB3).nbd("01/01/2008").nid("01/02/2008").from("01/01/2008").to("01/15/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB4).nbd("01/16/2008").nid("01/02/2008").from("01/01/2008").to("01/15/2008")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB5).nbd("01/15/2008").nid("01/02/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB6).nbd("01/15/2008").nid("01/02/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(0).verify();

        // Pro rated scenarios asserts
        scenarioVerifier.forOrder(orderA1).nbd("01/01/2008").nid("01/02/2008").from("01/01/2008").to("01/01/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderA2).nbd("01/02/2008").nid("01/02/2008").from("01/01/2008").to("01/01/2008")
                .invoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/01/2008"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 2th Jan 2008 for Billing sceanrio B1 - B6 And Pro-Rate Scenario A1
         * and A2
         */
        triggerBilling(AsDate("01/02/2008"));

        scenarioVerifier.forOrder(orderB1).nbd("01/01/2008").nid("01/03/2008").from("01/01/2008").to("01/07/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/08/2008").nid("01/03/2008").from("01/07/2008").to("01/07/2008")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB3).nbd("01/01/2008").nid("01/03/2008").from("01/01/2008").to("01/15/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB4).nbd("01/16/2008").nid("01/03/2008").from("01/01/2008").to("01/15/2008")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB5).nbd("01/15/2008").nid("01/03/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB6).nbd("01/15/2008").nid("01/03/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(0).verify();

        // Prorated
        scenarioVerifier.forOrder(orderA1).nbd("01/02/2008").nid("01/03/2008").from("01/01/2008").to("01/01/2008")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderA2).nbd("01/03/2008").nid("01/03/2008").from("01/02/2008").to("01/02/2008")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/02/2008"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 3th Jan 2008 for Billing sceanrio B1 - B6 And Pro-Rate Scenario A1
         * and A2
         */
        triggerBilling(AsDate("01/03/2008"));

        scenarioVerifier.forOrder(orderB1).nbd("01/01/2008").nid("01/04/2008").from("01/01/2008").to("01/07/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/08/2008").nid("01/04/2008").from("01/01/2008").to("01/07/2008")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB3).nbd("01/01/2008").nid("01/04/2008").from("01/01/2008").to("01/15/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB4).nbd("01/16/2008").nid("01/04/2008").from("01/01/2008").to("01/15/2008")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB5).nbd("01/15/2008").nid("01/04/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB6).nbd("01/15/2008").nid("01/04/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(0).verify();

        // Prorated
        scenarioVerifier.forOrder(orderA1).nbd("01/03/2008").nid("01/04/2008").from("01/02/2008").to("01/02/2008")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderA2).nbd("01/04/2008").nid("01/04/2008").from("01/03/2008").to("01/03/2008")
                .invoiceLines(1).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/03/2008"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 4th Jan 2008 for Billing sceanrio B1 - B6 And Pro-Rate Scenario A1
         * and A2
         */
        triggerBilling(AsDate("01/04/2008"));

        scenarioVerifier.forOrder(orderB1).nbd("01/01/2008").nid("01/05/2008").from("01/01/2008").to("01/07/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/08/2008").nid("01/05/2008").from("01/01/2008").to("01/07/2008")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB3).nbd("01/01/2008").nid("01/05/2008").from("01/01/2008").to("01/15/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB4).nbd("01/16/2008").nid("01/05/2008").from("01/01/2008").to("01/15/2008")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB5).nbd("01/15/2008").nid("01/05/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB6).nbd("01/15/2008").nid("01/05/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(0).verify();

        /**
         * Pro-Rate Scenario Billing Run date = 4th Jan 2008 for Billing sceanrio A1 and A2
         */
        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ?? .to("01/04/2008")
        scenarioVerifier.forOrder(orderA1).nbd("01/04/2008").nid("01/05/2008").from("01/03/2008").to("01/03/2008")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderA2).nbd("01/05/2008").nid("01/05/2008").from("01/04/2008").to("01/04/2008")
                .invoiceLines(1).dueInvoiceLines(3).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/04/2008"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 8th Jan 2008 for Billing sceanrio B1 and B2 Before billing run
         * update next invoice date of User to 8th Jan
         */
        userB1 = api.getUserWS(userB1.getUserId());
	    userB1.setPassword(null);
        userB1.setNextInvoiceDate(AsDate("01/08/2008"));
        api.updateUser(userB1);

        userB2 = api.getUserWS(userB2.getUserId());
	    userB2.setPassword(null);
        userB2.setNextInvoiceDate(AsDate("01/08/2008"));
        api.updateUser(userB2);

        triggerBilling(AsDate("01/08/2008"));

        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ?? .nbd("01/01/2008")
        scenarioVerifier.forOrder(orderB1).nbd("01/08/2008").nid("01/09/2008").from("01/01/2008").to("01/07/2008")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/15/2008").nid("01/09/2008").from("01/08/2008").to("01/14/2008")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/08/2008"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 9th Jan 2008 for Billing sceanrio B1 and B2
         */
        triggerBilling(AsDate("01/09/2008"));

        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ?? .nbd("01/01/2008")
        scenarioVerifier.forOrder(orderB1).nbd("01/08/2008").nid("01/10/2008").from("01/01/2008").to("01/07/2008")
                .invoiceLines(1).skipPreviousInvoice().verify();
        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ??  .nid("01/15/2008").from("01/10/2008")
        scenarioVerifier.forOrder(orderB2).nbd("01/15/2008").nid("01/10/2008").from("01/08/2008").to("01/14/2008")
                .invoiceLines(1).skipPreviousInvoice().verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/09/2008"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 15th Jan 2008 for Billing sceanrio B1, B2, B5 and B6 Before billing
         * run update next invoice date of User to 15th Jan
         */

        userB1 = api.getUserWS(userB1.getUserId());
	    userB1.setPassword(null);
        userB1.setNextInvoiceDate(AsDate("01/15/2008"));
        api.updateUser(userB1);

        userB2 = api.getUserWS(userB2.getUserId());
	    userB2.setPassword(null);
        userB2.setNextInvoiceDate(AsDate("01/15/2008"));
        api.updateUser(userB2);

        userB5 = api.getUserWS(userB5.getUserId());
	    userB5.setPassword(null);
        userB5.setNextInvoiceDate(AsDate("01/15/2008"));
        api.updateUser(userB5);

        userB6 = api.getUserWS(userB6.getUserId());
	    userB6.setPassword(null);
        userB6.setNextInvoiceDate(AsDate("01/15/2008"));
        api.updateUser(userB6);

        triggerBilling(AsDate("01/15/2008"));

        scenarioVerifier.forOrder(orderB1).nbd("01/15/2008").nid("01/16/2008").from("01/08/2008").to("01/14/2008")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/22/2008").nid("01/16/2008").from("01/15/2008").to("01/21/2008")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderB5).nbd("01/15/2008").nid("01/16/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB6).nbd("02/15/2008").nid("01/16/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/15/2008"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 16th Jan 2008 for Billing sceanrio B3 and B4
         */
        userB3 = api.getUserWS(userB3.getUserId());
	    userB3.setPassword(null);
        userB3.setNextInvoiceDate(AsDate("01/16/2008"));
        api.updateUser(userB3);

        userB4 = api.getUserWS(userB4.getUserId());
	    userB4.setPassword(null);
        userB4.setNextInvoiceDate(AsDate("01/16/2008"));
        api.updateUser(userB4);

        triggerBilling(AsDate("01/16/2008"));

        scenarioVerifier.forOrder(orderB3).nbd("01/16/2008").nid("01/17/2008").from("01/01/2008").to("01/15/2008")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB4).nbd("02/01/2008").nid("01/17/2008").from("01/16/2008").to("01/31/2008")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/16/2008"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 17th Jan 2008 for Billing sceanrio B3 and B4
         */
        triggerBilling(AsDate("01/17/2008"));

        scenarioVerifier.forOrder(orderB3).nbd("01/16/2008").nid("01/18/2008").from("01/01/2008").to("01/15/2008")
                .invoiceLines(0).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB4).nbd("02/01/2008").nid("01/18/2008").from("01/16/2008").to("01/31/2008")
                .invoiceLines(0).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/17/2008"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 31st Jan 2008 for Billing sceanrio B3 and B4
         */
        userB3 = api.getUserWS(userB3.getUserId());
	    userB3.setPassword(null);
        userB3.setNextInvoiceDate(AsDate("01/31/2008"));
        api.updateUser(userB3);

        userB4 = api.getUserWS(userB4.getUserId());
	    userB4.setPassword(null);
        userB4.setNextInvoiceDate(AsDate("01/31/2008"));
        api.updateUser(userB4);

        triggerBilling(AsDate("01/31/2008"));

        scenarioVerifier.forOrder(orderB3).nbd("01/16/2008").nid("02/01/2008").from("01/01/2008").to("01/15/2008")
                .invoiceLines(0).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB4).nbd("02/01/2008").nid("02/01/2008").from("01/16/2008").to("01/31/2008")
                .invoiceLines(0).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/31/2008"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 1st Feb 2008 for Billing sceanrio B3 and B4
         */
        triggerBilling(AsDate("02/01/2008"));

        scenarioVerifier.forOrder(orderB3).nbd("02/01/2008").nid("02/02/2008").from("01/16/2008").to("01/31/2008")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB4).nbd("02/16/2008").nid("02/02/2008").from("02/01/2008").to("02/15/2008")
                .invoiceLines(1).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/01/2008"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 15th Feb 2008 for Billing sceanrio B5 and B6 Update Next Invoice
         * Date to 15th Jan before Billing run
         */
        userB5 = api.getUserWS(userB5.getUserId());
	    userB5.setPassword(null);
        userB5.setNextInvoiceDate(AsDate("02/15/2008"));
        api.updateUser(userB5);

        userB6 = api.getUserWS(userB6.getUserId());
	    userB6.setPassword(null);
        userB6.setNextInvoiceDate(AsDate("02/15/2008"));
        api.updateUser(userB6);

        triggerBilling(AsDate("02/15/2008"));

        scenarioVerifier.forOrder(orderB5).nbd("02/15/2008").nid("02/16/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB6).nbd("03/15/2008").nid("02/16/2008").from("02/15/2008").to("03/14/2008")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/15/2008"));

        /**
         * Not Pro-Rate Billing Run date = 16th Feb 2008 for Billing sceanrio B5 and B6
         */
        triggerBilling(AsDate("02/16/2008"));

        scenarioVerifier.forOrder(orderB5).nbd("02/15/2008").nid("02/17/2008").from("01/15/2008").to("02/14/2008")
                .invoiceLines(0).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB6).nbd("03/15/2008").nid("02/17/2008").from("02/15/2008").to("03/14/2008")
                .invoiceLines(0).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/16/2008"));
    }
}
