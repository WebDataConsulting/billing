package com.sapienter.jbilling.server.util.search;

import com.sapienter.jbilling.common.SessionInternalError;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Basic implementation of a filter. It contains a fields name and value
 * can be one of several basic java types.
 *
 * @author Gerhard Maree
 * @since 28/06/13
 */
public class BasicFilter implements Filter {

    /** Field name */
    private String field;
    /** Constraint operator to use in search */
    private FilterConstraint constraint;
    /** Value to filter by */
    private String stringValue;
    private Integer integerValue;
    private Boolean booleanValue;
    private Date dateValue;
    private BigDecimal decimalValue;
    private List<String> stringListValue;
    private List<Integer> intListValue;

    public BasicFilter() {

    }

    public BasicFilter(String field, FilterConstraint constraint, Object value) {
        this.field = field;
        this.constraint = constraint;
        if(value instanceof String) {
            stringValue = (String)value;
        } else if(value instanceof Integer) {
            integerValue = (Integer)value;
        } else if(value instanceof BigDecimal) {
            decimalValue = (BigDecimal)value;
        } else if(value instanceof Boolean) {
            booleanValue = (Boolean)value;
        } else if(value instanceof Date) {
            dateValue = (Date)value;
        } else if(value instanceof List) {
            if(!((List) value).isEmpty() && ((List) value).get(0) instanceof Integer) {
                intListValue = (List<Integer>)value;
            } else {
                stringListValue = (List<String>)value;
            }
        } else {
            throw new SessionInternalError("Unknown type '"+(value ==null ? "null" : value.getClass())+"' for field "+field+" value " + value);
        }
    }

    public String getField() {
        return field;
    }

    public FilterConstraint getConstraint() {
        return constraint;
    }

    public Object getValue() {
        if(stringValue != null) {
            return stringValue;
        } else if(integerValue != null) {
            return integerValue;
        } else if(booleanValue != null) {
            return booleanValue;
        } else if(decimalValue != null) {
            return decimalValue;
        } else if(dateValue != null) {
            return dateValue;
        } else if(stringListValue != null) {
            return stringListValue;
        } else if(intListValue != null) {
            return intListValue;
        }
        return null;
    }

    public void setConstraint(FilterConstraint constraint) {
        this.constraint = constraint;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public Date getDateValue() {
        return dateValue;
    }

    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }

    public BigDecimal getDecimalValue() {
        return decimalValue;
    }

    public void setDecimalValue(BigDecimal decimalValue) {
        this.decimalValue = decimalValue;
    }

    public List<String> getStringListValue() {
        return stringListValue;
    }

    public void setStringListValue(List<String> stringListValue) {
        this.stringListValue = stringListValue;
    }

    public List<Integer> getIntListValue() {
        return intListValue;
    }

    public void setIntListValue(List<Integer> intListValue) {
        this.intListValue = intListValue;
    }

    @Override
    public String toString() {
        return "BasicFilter{" +
                "field='" + field + '\'' +
                ", constraint=" + constraint +
                ", stringValue='" + stringValue + '\'' +
                ", integerValue=" + integerValue +
                ", booleanValue=" + booleanValue +
                ", dateValue=" + dateValue +
                ", decimalValue=" + decimalValue +
                ", stringListValue=" + stringListValue +
                ", intListValue=" + intListValue +
                '}';
    }
}
