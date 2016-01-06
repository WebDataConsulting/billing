package com.sapienter.jbilling.test;

import java.util.Date;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

@ContextConfiguration(classes = UnitTestConfig.class, loader = AnnotationConfigContextLoader.class)
public class UnitTestCase extends AbstractTestNGSpringContextTests {

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
}
