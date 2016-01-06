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
package com.sapienter.jbilling.server.util;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;

public class PreferenceWS implements Serializable {

    private Integer id;
    private PreferenceTypeWS preferenceType;
    private Integer tableId;
    private Integer foreignId;
    @Size(min=0, max=200, message="validation.error.max,200")
    private String value;
    @Digits(integer=12, fraction=0, message="validation.error.not.a.number.12.integer")
    private String intValue;

    public PreferenceWS() {
    }

    public PreferenceWS(PreferenceTypeWS preferenceType, String value) {
        this.preferenceType = preferenceType;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public PreferenceTypeWS getPreferenceType() {
        return this.preferenceType;
    }

    public void setPreferenceType(PreferenceTypeWS preferenceType) {
        this.preferenceType = preferenceType;
    }

    public Integer getTableId() {
        return tableId;
    }

    public void setTableId(Integer tableId) {
        this.tableId = tableId;
    }
    
    public Integer getForeignId() {
        return this.foreignId;
    }

    public void setForeignId(Integer foreignId) {
        this.foreignId = foreignId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "PreferenceWS{"
               + "id=" + id
               + ", preferenceType=" + preferenceType
               + ", tableId=" + tableId
               + ", foreignId=" + foreignId
               + ", value='" + value + '\''
               + '}';
    }
}
