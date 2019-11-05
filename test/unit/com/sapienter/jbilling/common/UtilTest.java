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

package com.sapienter.jbilling.common;

import junit.framework.TestCase;

/**
 * @author Alexander Aksenov
 * @since 26.02.12
 */
public class UtilTest extends TestCase {

    public void testGetPaymentMethod() {
        String ccNumber = "340000000000001";
        assertEquals("Incorrect payment type, AMEX expected",
                CommonConstants.PAYMENT_METHOD_AMEX, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "370000000000001";
        assertEquals("Incorrect payment type, AMEX expected",
                CommonConstants.PAYMENT_METHOD_AMEX, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "30000000000001";
        assertEquals("Incorrect payment type, Diners Club Carte Blanche expected",
                CommonConstants.PAYMENT_METHOD_DINERS, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "30100000000001";
        assertEquals("Incorrect payment type, Diners Club Carte Blanche expected",
                CommonConstants.PAYMENT_METHOD_DINERS, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "30500000000001";
        assertEquals("Incorrect payment type, Diners Club Carte Blanche expected",
                CommonConstants.PAYMENT_METHOD_DINERS, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "36500000000001";
        assertEquals("Incorrect payment type, Diners Club International expected",
                CommonConstants.PAYMENT_METHOD_DINERS, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "5450000000000111";
        assertEquals("Incorrect payment type, MASTERCARD expected",
                CommonConstants.PAYMENT_METHOD_MASTERCARD, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "5550000000000111";
        assertEquals("Incorrect payment type, MASTERCARD expected",
                CommonConstants.PAYMENT_METHOD_MASTERCARD, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "6011000000000111";
        assertEquals("Incorrect payment type, Discover Card expected",
                CommonConstants.PAYMENT_METHOD_DISCOVER, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6221260000000111";
        assertEquals("Incorrect payment type, Discover Card expected",
                CommonConstants.PAYMENT_METHOD_DISCOVER, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6229250000000111";
        assertEquals("Incorrect payment type, Discover Card expected",
                CommonConstants.PAYMENT_METHOD_DISCOVER, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6228150000000111";
        assertEquals("Incorrect payment type, Discover Card expected",
                CommonConstants.PAYMENT_METHOD_DISCOVER, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6440000000000111";
        assertEquals("Incorrect payment type, Discover Card expected",
                CommonConstants.PAYMENT_METHOD_DISCOVER, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6470000000000111";
        assertEquals("Incorrect payment type, Discover Card expected",
                CommonConstants.PAYMENT_METHOD_DISCOVER, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6490000000000111";
        assertEquals("Incorrect payment type, Discover Card expected",
                CommonConstants.PAYMENT_METHOD_DISCOVER, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6510000000000111";
        assertEquals("Incorrect payment type, Discover Card expected",
                CommonConstants.PAYMENT_METHOD_DISCOVER, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "6370000000000111";
        assertEquals("Incorrect payment type, InstaPayment expected",
                CommonConstants.PAYMENT_METHOD_INSTAL_PAYMENT, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6380000000000111";
        assertEquals("Incorrect payment type, InstaPayment expected",
                CommonConstants.PAYMENT_METHOD_INSTAL_PAYMENT, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6390000000000111";
        assertEquals("Incorrect payment type, InstaPayment expected",
                CommonConstants.PAYMENT_METHOD_INSTAL_PAYMENT, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "3528000000000111";
        assertEquals("Incorrect payment type, JCB expected",
                CommonConstants.PAYMENT_METHOD_JCB, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "3589000000000111";
        assertEquals("Incorrect payment type, JCB expected",
                CommonConstants.PAYMENT_METHOD_JCB, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "3569000000000111";
        assertEquals("Incorrect payment type, JCB expected",
                CommonConstants.PAYMENT_METHOD_JCB, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "6304000000000111";
        assertEquals("Incorrect payment type, MAESTRO (=LASER) expected",
                CommonConstants.PAYMENT_METHOD_MAESTRO, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6706000000000111";
        assertEquals("Incorrect payment type, LASER expected",
                CommonConstants.PAYMENT_METHOD_LASER, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6771000000000111";
        assertEquals("Incorrect payment type, LASER expected",
                CommonConstants.PAYMENT_METHOD_LASER, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6709000000000111";
        assertEquals("Incorrect payment type, LASER expected",
                CommonConstants.PAYMENT_METHOD_LASER, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "5018000000000111";
        assertEquals("Incorrect payment type, MAESTRO expected",
                CommonConstants.PAYMENT_METHOD_MAESTRO, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "5020000000000111";
        assertEquals("Incorrect payment type, MAESTRO expected",
                CommonConstants.PAYMENT_METHOD_MAESTRO, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "5038000000000111";
        assertEquals("Incorrect payment type, MAESTRO expected",
                CommonConstants.PAYMENT_METHOD_MAESTRO, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6304000000000111";
        assertEquals("Incorrect payment type, MAESTRO expected",
                CommonConstants.PAYMENT_METHOD_MAESTRO, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6759000000000111";
        assertEquals("Incorrect payment type, MAESTRO expected",
                CommonConstants.PAYMENT_METHOD_MAESTRO, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6761000000000111";
        assertEquals("Incorrect payment type, MAESTRO expected",
                CommonConstants.PAYMENT_METHOD_MAESTRO, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6762000000000111";
        assertEquals("Incorrect payment type, MAESTRO expected",
                CommonConstants.PAYMENT_METHOD_MAESTRO, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "6763000000000111";
        assertEquals("Incorrect payment type, MAESTRO expected",
                CommonConstants.PAYMENT_METHOD_MAESTRO, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "5100000000000111";
        assertEquals("Incorrect payment type, MasterCard expected",
                CommonConstants.PAYMENT_METHOD_MASTERCARD, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "5200000000000111";
        assertEquals("Incorrect payment type, MasterCard expected",
                CommonConstants.PAYMENT_METHOD_MASTERCARD, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "5300000000000111";
        assertEquals("Incorrect payment type, MasterCard expected",
                CommonConstants.PAYMENT_METHOD_MASTERCARD, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "5400000000000111";
        assertEquals("Incorrect payment type, MasterCard expected",
                CommonConstants.PAYMENT_METHOD_MASTERCARD, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "5500000000000111";
        assertEquals("Incorrect payment type, MasterCard expected",
                CommonConstants.PAYMENT_METHOD_MASTERCARD, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "4200000000000111";
        assertEquals("Incorrect payment type, VISA expected",
                CommonConstants.PAYMENT_METHOD_VISA, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "4026000000000111";
        assertEquals("Incorrect payment type, VISA ELECTRON expected",
                CommonConstants.PAYMENT_METHOD_VISA_ELECTRON, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "4175000000000111";
        assertEquals("Incorrect payment type, VISA ELECTRON expected",
                CommonConstants.PAYMENT_METHOD_VISA_ELECTRON, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "4508000000000111";
        assertEquals("Incorrect payment type, VISA ELECTRON expected",
                CommonConstants.PAYMENT_METHOD_VISA_ELECTRON, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "4844000000000111";
        assertEquals("Incorrect payment type, VISA ELECTRON expected",
                CommonConstants.PAYMENT_METHOD_VISA_ELECTRON, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "4913000000000111";
        assertEquals("Incorrect payment type, VISA ELECTRON expected",
                CommonConstants.PAYMENT_METHOD_VISA_ELECTRON, Util.getPaymentMethod(ccNumber)
        );
        ccNumber = "4917000000000111";
        assertEquals("Incorrect payment type, VISA ELECTRON expected",
                CommonConstants.PAYMENT_METHOD_VISA_ELECTRON, Util.getPaymentMethod(ccNumber)
        );

        ccNumber = "************0111";
        assertEquals("Incorrect payment type, GatewayKey expected",
                CommonConstants.PAYMENT_METHOD_GATEWAY_KEY, Util.getPaymentMethod(ccNumber)
        );
    }
}
