package com.sapienter.jbilling.server.util.search;

import java.io.Serializable;

/**
 * Filter interface. A filter consists of a field name, a constraint and the value to filter by.
 *
 * @author Gerhard
 * @since 28/06/13
 */
public interface Filter extends Serializable {
    public static enum FilterConstraint {
        EQ, LIKE, LT, GT, LE, GE, IN, ILIKE
    }
    public String getField();
    public FilterConstraint getConstraint();
    public Object getValue();
}
