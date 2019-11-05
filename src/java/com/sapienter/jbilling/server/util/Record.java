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

import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.item.PricingField;
import java.util.ArrayList;

public class Record {
    private StringBuffer key = new StringBuffer();
    private Date processingDate ;
    private int position = 1;
    private List<PricingField> fields = new ArrayList<PricingField>();
    
    // Record format errors go here
    private List<String> errors = new ArrayList<String>(1);

    private String recordId;

    public Date getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(String processingTime) {
        try{
            long processTime=Long.parseLong(processingTime);
            this.processingDate = new Date(processTime);
        }catch (Exception e){
            //wrong date format inside the hbase

        }
    }

    public Record() {
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
        for (PricingField field : fields) {
            field.setPosition(position);
        }
    }

    public List<PricingField> getFields() {
        return fields;
    }

    public void setFields(List<PricingField> fields) {
        this.fields = fields;
    }

    public void addField(PricingField field, boolean isKey) {
        if (isKey && PricingField.find(fields, field.getName()) == null) {
            key.append(field.getValue().toString());
        }
        PricingField.add(fields, field);
    }

    public String getKey() {
        return key.toString();
    }

    public void setKey(String key) {
        this.key.append(key);
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        errors.add(error);
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Record");
        sb.append("{key=").append(key);
        sb.append(", position=").append(position);
        sb.append(", fields=").append(fields);
        sb.append(", recordId=").append(recordId);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Record record = (Record) o;

        if (position != record.position) return false;
        if (fields != null ? !fields.equals(record.fields) : record.fields != null) return false;
        if (!key.equals(record.key)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + position;
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        return result;
    }
}
