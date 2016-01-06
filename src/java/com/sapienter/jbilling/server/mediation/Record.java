/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.item.PricingField;

import java.util.ArrayList;
import java.util.List;

public class Record {
    private StringBuffer key = new StringBuffer();
    private int position = 1;
    private List<PricingField> fields = new ArrayList<PricingField>();
    
    // Record format errors go here
    private List<String> errors = new ArrayList<String>(1);

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
        fields.add(field);
        if (isKey) {
            key.append(field.getValue().toString());
        }
    }

    public String getKey() {
        return key.toString();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        errors.add(error);
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Record");
        sb.append("{key=").append(key);
        sb.append(", position=").append(position);
        sb.append(", fields=").append(fields);
        sb.append('}');
        return sb.toString();
    }
}
