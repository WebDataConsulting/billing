package com.sapienter.jbilling.test;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * User: Nikhil
 * Date: 10/15/12
 */
public class JBillingTestCase extends TestCase {

	/**
	 * Assert for jBilling standard scale comparision of BigDecimal values
	 * @param message
	 * @param expected
	 * @param actual
	 */
    protected static void assertEquals(String message, BigDecimal expected, BigDecimal actual) {
        assertEquals(message,
                (Object) (expected == null ? null : expected.setScale(2, RoundingMode.HALF_UP)),
                (Object) (actual == null ? null : actual.setScale(2, RoundingMode.HALF_UP)));
    }
    
    /**
     *  Useful for pausing the Test Case execution while waiting for the Billing Process
     */
    protected static void pause(long t) {
        //LOG.debug("TestExternalProvisioningMDB: pausing for " + t + " ms...");

        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
        }
    }

}