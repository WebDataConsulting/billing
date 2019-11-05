package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.SessionInternalError;
import junit.framework.TestCase;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * User: Nikhil
 * Date: 10/15/12
 */
public class JBillingTestUtils extends TestCase {

    public static void assertEquals(String message, BigDecimal expected, BigDecimal actual) {
        assertEquals(message,
                (Object) (expected == null ? null : expected.setScale(2, RoundingMode.HALF_UP)),
                (Object) (actual == null ? null : actual.setScale(2, RoundingMode.HALF_UP)));
    }

    /**
     * Tests that properties of {@code test} equals those in {@code ref}.
     *
     * @param ref - reference object
     * @param test - object to test
     * @param excludes - list of properties (in {@code ref} to exclude from checking
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static void assertPropertiesEqual(Object ref, Object test, String[] excludes) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        //get all the properties in ref object
        BeanUtilsBean bub = BeanUtilsBean.getInstance();
        Map refDescr = bub.describe(ref);

        //remove the properties to exclude
        if(excludes != null) {
            for(String prop: excludes) {
                refDescr.remove(prop);
            }
        }

        //check that the properties are equal
        for(String prop: (Collection<String>)refDescr.keySet()) {
            assertEquals("Property " +prop+" on test obj["+bub.getProperty(test,prop)+"] expected["+bub.getProperty(ref,prop)+"]",
                    bub.getProperty(ref, prop), bub.getProperty(test,prop));
        }
    }

    /**
     * Checks that {@code messageCode} is one of the {@code errorMessages} in {@code error}.
     * @param error
     * @param messageCode
     */
    public static void assertContainsError(SessionInternalError error, String messageCode) {
        for(String msg : error.getErrorMessages()) {
            if(msg.equals(messageCode)) return;
        }
        fail("Code ["+messageCode+"] not found in: "+ Arrays.asList(error.getErrorMessages()));
    }

    /**
     * Checks that at least one of {@code messageCodes} is one of the {@code errorMessages} in {@code error}.
     * @param error
     * @param messageCodes
     */
    public static void assertContainsAnyError(SessionInternalError error, Collection<String> messageCodes) {
        for(String msg : error.getErrorMessages()) {
            if(messageCodes.contains(msg)) return;
        }
        fail("Codes ["+messageCodes+"] not found in: "+ Arrays.asList(error.getErrorMessages()));
    }
}
