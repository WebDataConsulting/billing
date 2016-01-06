package com.sapienter.jbilling.test;

import com.sapienter.jbilling.common.SessionInternalError;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public class Asserts {

    public static void assertEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertEquals(String message, BigDecimal expected, BigDecimal actual) {
        if (message == null) {
            message = "Expected <" + expected + "> but was <" + actual + ">";
        } else {
            message = message + ". Expected <" + expected + "> but was <" + actual + ">";
        }

        org.testng.AssertJUnit.assertEquals(
                message,
                (Object) (expected == null ? null : expected.setScale(2, RoundingMode.HALF_UP)),
                (Object) (actual == null ? null : actual.setScale(2, RoundingMode.HALF_UP))
        );
    }


    /**
     * Checks that the SessionInternalErrors contains an error with id expected.
     *
     * @param errors
     * @param expected
     * @param errorMessage Null allowed. Message that will be displayed in case expected error is not found
     */
    public static void assertContainsError(SessionInternalError errors, String expected, String errorMessage) {
        if(errors.getErrorMessages() == null) {
            org.testng.AssertJUnit.fail( errorMessage != null ? errorMessage : "Expected error ["+expected+"] not found. " +
                    "Actual [ null ]");
        }
        for(String error:errors.getErrorMessages()) {
            if(error.equals(expected)) return;
        }
        org.testng.AssertJUnit.fail( errorMessage != null ? errorMessage : "Expected error ["+expected+"] not found. " +
                "Actual ["+ Arrays.asList(errors.getErrorMessages()) + "]");
    }

    public static void assertContainsError(SessionInternalError errors, String expected) {
        assertContainsError(errors, expected, null);
    }
}