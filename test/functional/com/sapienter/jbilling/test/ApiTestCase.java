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

package com.sapienter.jbilling.test;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

/**
 * 
 * 
 * @author Igor Poteryaev <igor.poteryaev@jbilling.com>
 *
 */
@ContextConfiguration(classes = ApiTestConfig.class, loader = AnnotationConfigContextLoader.class)
public class ApiTestCase extends AbstractTestNGSpringContextTests {

    public static final Integer TEST_LANGUAGE_ID = ServerConstants.LANGUAGE_ENGLISH_ID;
    public static final Integer TEST_ENTITY_ID   = 1;

    @Autowired
    protected JbillingAPI       api;

    @Override
    @BeforeClass(alwaysRun = true, dependsOnMethods = "springTestContextBeforeTestClass")
    protected void springTestContextPrepareTestInstance () throws Exception {
        super.springTestContextPrepareTestInstance();
        prepareTestInstance();
    }

    @Override
    @BeforeClass(alwaysRun = true)
    protected void springTestContextBeforeTestClass () throws Exception {
        super.springTestContextBeforeTestClass();
        beforeTestClass();
    }

    @Override
    @AfterClass(alwaysRun = true)
    protected void springTestContextAfterTestClass () throws Exception {
        afterTestClass();
        super.springTestContextAfterTestClass();
    }

    /*
     * methods for subclasses to override
     */
    protected void afterTestClass () throws Exception {
    }

    protected void beforeTestClass () throws Exception {
    }

    protected void prepareTestInstance () throws Exception {
    }

    /*
     * utility methods
     */
    protected static Date AsDate (String dateStr) {
        return TestUtils.AsDate(dateStr);
    }

    protected static Date AsDate (int year, int month, int day) {
        return TestUtils.AsDate(year, month, day);
    }

    protected static String AsString (Date date) {
        return TestUtils.AsString(date);
    }

    /**
     * logback style ("... {} ...") message parameters support. based on log4j LogSF implementation
     */
    protected static String S (final String pattern, final Object argument) {
        return Util.S(pattern, argument);
    }

    /**
     * logback style ("... {} ...") message parameters support. based on log4j LogSF implementation. multi-param
     * version.
     */
    protected static String S (final String pattern, final Object... arguments) {
        return Util.S(pattern, arguments);
    }
}
