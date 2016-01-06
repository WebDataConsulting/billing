/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.security;

import org.junit.Assert;
import org.testng.annotations.Test;

import com.sapienter.jbilling.test.ApiTestCase;

/**
 * Just in case to run some test code.
 * 
 * @author jah
 */

@Test(groups = { "integration" })
public class SecurityTest extends ApiTestCase {

    // user from entity[id:1]
    private static final Integer TEST_USER_ID_ENTITY_1    = Integer.valueOf(1);
    // user from entity[id:2]
    private static final Integer TEST_USER_ID_ENTITY_2    = Integer.valueOf(13);
    // user from entity[id:3] which is a child of entity[id:1]
    private static final Integer TEST_USER_ID_ENTITY_3    = Integer.valueOf(10801); // MAGIC NUMBER !!!

    private static final Integer TEST_ORDER_ID_ENTITY_1   = Integer.valueOf(1);
    private static final Integer TEST_ORDER_ID_ENTITY_2   = Integer.valueOf(5);

    private static final Integer TEST_INVOICE_ID_ENTITY_1 = Integer.valueOf(1);
    private static final Integer TEST_INVOICE_ID_ENTITY_2 = Integer.valueOf(75);

    /**
     * Simplest possible test.
     */
    @Test(enabled = false)
    public void testCallingJbillingApiSecurely () throws Exception {
        logger.info("#testCallingJbillingApiSecurely");
        @SuppressWarnings("unused")
        boolean isRunning = api.isAgeingProcessRunning();
        logger.info("#testCallingJbillingApiSecurely finished.");
    }

    /**
     * Access to same as caller user entity data should be possible. N.B. We use admin user from entity[id:1], who
     * should have access as superuser to any data in this entity.
     */
    @Test(enabled = true)
    public void testAccessToSameEntityData () throws Exception {
        /*
         * TODO: Configure new test user as less powerful one and add some tests for him.
         */
        api.getUserWS(TEST_USER_ID_ENTITY_1);
        api.getOrder(TEST_ORDER_ID_ENTITY_1);
        api.getInvoiceWS(TEST_INVOICE_ID_ENTITY_1);
    }

    /* @formatter:off */
    /**
     * N.B. This test is fragile, because of the definition for user id used.
     * It is defined in descriptors/jbilling-upgrade-3.4.xml changeSet:
     * 
     * <changeSet author="Khobab Chaudhary" context="test" id="requirement #5292 - child_reseller">
     * 
     * as :
     * <insert tableName="base_user">
     *     <column name="id" valueComputed="(select max(t.id)+1 from base_user t)"/>
     *     <column name="entity_id" valueComputed="(select max(e.id) from entity e)"/>
     * 
     * This is definitely fragile.
     */

//    private static final Integer NOT_EXISTING_ID          = Integer.valueOf(-1);
    /* @formatter:on */

    @Test(enabled = true)
    public void testAccessToChildEntityData () throws Exception {
        api.getUserWS(TEST_USER_ID_ENTITY_3);
        /* @formatter:off */
        /*
         * N.B. [2015-05-05] We have no test data for orders and invoices in child company.
         *      If this data will be added, please, extend this test using this template
         *      for test calls:
         */
        /* @formatter:on */
        // api.getOrder(TEST_ORDER_ID_ENTITY_3);
        // api.getInvoiceWS(TEST_INVOICE_ID_ENTITY_3);
    }

    /**
     * Access to data from other entity (not hierarchically related to caller user entity) should be prohibited. Several
     * tests are combined in single method.
     */
    @Test(enabled = true)
    public void testAccessToOtherEntityData () throws Exception {
        try {
            api.getUserWS(TEST_USER_ID_ENTITY_2);
            Assert.fail();
        } catch (SecurityException e) {
        }
        try {
            api.getOrder(TEST_ORDER_ID_ENTITY_2);
            Assert.fail();
        } catch (SecurityException e) {
        }
        try {
            api.getInvoiceWS(TEST_INVOICE_ID_ENTITY_2);
            Assert.fail();
        } catch (SecurityException e) {
        }
    }

    /**
     * TODO: Add test for this corner cases.
     */
    @Test(enabled = false)
    public void testAccessToNotExistingData () throws Exception {
        // api.getUserWS(null);
        // api.getUserWS(NOT_EXISTING_ID);
        // api.getOrder(NOT_EXISTING_ID);
        // api.getOrder(null);
        // api.getInvoiceWS(NOT_EXISTING_ID);
        // api.getInvoiceWS(null);
    }
}
