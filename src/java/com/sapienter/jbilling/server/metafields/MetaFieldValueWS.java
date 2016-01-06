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

package com.sapienter.jbilling.server.metafields;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlTransient;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * @author Alexander Aksenov
 * @since 09.10.11
 */
public class MetaFieldValueWS implements Serializable {

    private static final long serialVersionUID = 20130704L;

    @NotNull(message="validation.error.notnull")
    @Size(min = 1, max = 100, message = "validation.error.size,1,100")
    private String fieldName;
    private Integer groupId;
    private boolean disabled;
    private boolean mandatory;
    private DataType dataType;
    private Object defaultValue;
    private Integer displayOrder;

    private Integer id;

    @Size(min = 0, max = 1000, message = "validation.error.size,0,1000")
    private String stringValue;
    private Date dateValue;
    private Boolean booleanValue;
    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    private String decimalValue;
    private Integer integerValue;
    private String[] listValue;

    public MetaFieldValueWS() {
    }

    public MetaFieldValueWS clone() {
    	MetaFieldValueWS ws = new MetaFieldValueWS();
    	ws.setFieldName(this.fieldName);
    	ws.setGroupId(this.groupId);
    	ws.setDisabled(this.disabled);
    	ws.setMandatory(this.mandatory);
    	ws.setDisplayOrder(this.displayOrder);
    	ws.setDataType(this.dataType);
    	ws.setDefaultValue(this.defaultValue);
    	
    	ws.setStringValue(this.stringValue);
    	ws.setDateValue(this.dateValue);
    	ws.setBooleanValue(this.booleanValue);
    	ws.setDecimalValue(this.decimalValue);
    	ws.setIntegerValue(this.integerValue);
    	ws.setListValue(this.listValue);
    	
    	return ws;
    }
    
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @XmlTransient
    public Object getValue() {
        if (getStringValue() != null) {
            return getStringValue();
        } else if (getDateValue() != null) {
            return getDateValue();
        } else if (getBooleanValue() != null) {
            return getBooleanValue();
        } else if (getDecimalValue() != null) {
            return getDecimalValueAsDecimal();
        } else if (getIntegerValue() != null) {
            return getIntegerValue();
        } else if (getListValue() != null) {
            return getListValueAsList();
        }

        return null;
    }

    public void setValue(Object value) {
        setStringValue(null);
        setDateValue(null);
        setBooleanValue(null);
        setDecimalValue(null);
        setIntegerValue(null);

        if (value == null) return;

        if (value instanceof String) {
            setStringValue((String) value);
        } else if (value instanceof Date) {
            setDateValue((Date) value);
        } else if (value instanceof Boolean) {
            setBooleanValue((Boolean) value);
        } else if (value instanceof BigDecimal) {
            setBigDecimalValue((BigDecimal) value);
        } else if (value instanceof Integer) {
            setIntegerValue((Integer) value);
        } else if (value instanceof List) {
            // store List<String> as String[] for WS-compatible mode, perform manual convertion
            setListValue(((List<String>) value).toArray(new String[((List<String>) value).size()]));
        } else if (value instanceof String[]) {
            setListValue((String[]) value);
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        if (defaultValue != null && defaultValue instanceof Collection) {
            // default value is the first in list
            if (((Collection) defaultValue).isEmpty()) {
                this.defaultValue = null;
            } else {
                this.defaultValue = ((Collection) defaultValue).iterator().next();
            }
        } else {
            this.defaultValue = defaultValue;
        }
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Date getDateValue() {
        return dateValue;
    }

    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public String getDecimalValue() {
        return decimalValue;
    }

    public BigDecimal getDecimalValueAsDecimal() {
        return Util.string2decimal(decimalValue);
    }


    public void setDecimalValue(String decimalValue) {
        this.decimalValue = decimalValue;
    }

    public void setBigDecimalValue(BigDecimal decimalValue) {
        this.decimalValue = decimalValue != null ? decimalValue.toPlainString() : null;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    public String[] getListValue() {
        return listValue;
    }

    public void setListValue(String[] listValue) {
        this.listValue = listValue;
    }

    /**
     * Call this method instead of getValue() for metaField with type LIST, because
     * storing data inside MetaFieldValueWS as String[] for WS-complaint mode.
     *
     * @return value as java.util.List for LIST meta field type. null otherwise.
     */
    @XmlTransient
    public List getListValueAsList() {
        if (listValue != null) {
            return new LinkedList<String>(Arrays.asList(listValue));
        } else {
            return null;
        }
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    @Override
    public String toString() {
        return "MetaFieldValueWS{" +
                "id=" + id +
                ", fieldName='" + fieldName + '\'' +
                ", groupId=" + groupId +
                ", dataType=" + dataType +
                ", value=" + getValue() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetaFieldValueWS that = (MetaFieldValueWS) o;

        if (disabled != that.disabled) return false;
        if (mandatory != that.mandatory) return false;
        if (! nullSafeEquals(booleanValue, that.booleanValue)) return false;
        if (dataType != that.dataType) return false;
        if (! nullSafeEquals(dateValue, that.dateValue)) return false;
        if (! nullSafeEquals(decimalValue, that.decimalValue)) return false;
        if (! nullSafeEquals(defaultValue, that.defaultValue)) return false;
        if (! nullSafeEquals(displayOrder, that.displayOrder)) return false;
        if (!fieldName.equals(that.fieldName)) return false;
        if (! nullSafeEquals(groupId, that.groupId)) return false;
        if (! nullSafeEquals(integerValue, that.integerValue)) return false;
        if (!Arrays.equals(listValue, that.listValue)) return false;
        if (! nullSafeEquals(stringValue, that.stringValue)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fieldName.hashCode();
        result = 31 * result + nullSafeHashCode(groupId);
        result = 31 * result + (disabled ? 1 : 0);
        result = 31 * result + (mandatory ? 1 : 0);
        result = 31 * result + nullSafeHashCode(dataType);
        result = 31 * result + nullSafeHashCode(defaultValue);
        result = 31 * result + nullSafeHashCode(displayOrder);
        result = 31 * result + nullSafeHashCode(stringValue);
        result = 31 * result + nullSafeHashCode(dateValue);
        result = 31 * result + nullSafeHashCode(booleanValue);
        result = 31 * result + nullSafeHashCode(decimalValue);
        result = 31 * result + nullSafeHashCode(integerValue);
        result = 31 * result + nullSafeHashCode(listValue);
        return result;
    }
}
