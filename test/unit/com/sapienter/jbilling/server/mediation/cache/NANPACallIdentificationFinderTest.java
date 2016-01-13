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

package com.sapienter.jbilling.server.mediation.cache;

import junit.framework.TestCase;

/**
 * NANPACallIdentificationFinderTest
 *
 * @author Brian Cowdery
 * @since 07-12-2010
 */
public class NANPACallIdentificationFinderTest extends TestCase {

    public NANPACallIdentificationFinderTest() {
    }

    public NANPACallIdentificationFinderTest(String name) {
        super(name);
    }

    public void testGetDigits() throws Exception {
        NANPACallIdentificationFinder finder = new NANPACallIdentificationFinder(null, null);
        String number = "12345";

        assertEquals("12345", finder.getDigits(number, 5));
        assertEquals("1234", finder.getDigits(number, 4));
        assertEquals("123", finder.getDigits(number, 3));
        assertEquals("12", finder.getDigits(number, 2));
        assertEquals("1", finder.getDigits(number, 1));
        assertEquals("0", finder.getDigits(number, 0));
        assertEquals("0", finder.getDigits(number, -1));
    }
}
