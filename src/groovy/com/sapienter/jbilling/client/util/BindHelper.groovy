package com.sapienter.jbilling.client.util

import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod

/**
 * Helper class to help with non-default bindings. Useful when binding form parameters to objects.
 *
 * @author Gerhard
 * @since 17/04/13
 */
class BindHelper {
    BindDynamicMethod bind = new BindDynamicMethod();

    /**
     * Used when binding checkboxes to boolean 1 or 0 values.
     * If src[srcPref+property] exists set the value on target to 1 else 0.
     *
     * @param src           Source of property values
     * @param target        Set values on the target.
     * @param properties    Collection of property names to bind
     * @param srcPref       Prefix when looking for a property value in src
     */
    public static void bindPropertyPresentToInteger(Object src, Object target, Collection properties, String srcPref = null) {
        if (srcPref == null) srcPref = "";
        properties.each {
            target[it] = (src[srcPref+it]  ? 1 : 0)
        }
    }

    /**
     * Safe binding of integers values. Null, '', and 'null' will be bound to 0.
     *
     * @param src           Source of property values
     * @param target        Set values on the target.
     * @param properties    Collection of property names to bind
     * @param srcPref       Prefix when looking for a property value in src
     */
    public static void bindInteger(Object src, Object target, Collection properties, String srcPref = null) {
        if (srcPref == null) srcPref = "";
        properties.each {
            def val = src[srcPref + it]
            if(val == "null" || val == "" || val == null) val = 0
            target[it] = (val as Integer)
        }
    }
}
