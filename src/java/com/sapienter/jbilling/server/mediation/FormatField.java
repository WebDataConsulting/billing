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

public class FormatField {
    private String name;
    private String type;
    private Integer startPosition;
    private Integer length;
    private boolean isKey;
    private String durationFormat;
    
    public void isKeyTrue() {
        this.isKey = true;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FormatField() {
        isKey = false;
    }
    
    public boolean getIsKey() {
        return isKey;
    }
    public String getName() {
        return name;
    }
    public String getType() {
        return type;
    }
    
    public String toString() {
        return "name: " + name + " type: " + type + " isKey: " + isKey + 
                " startPosition " + startPosition + " length " + length;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = Integer.valueOf(length);
    }

    public Integer getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(String startPosition) {
        this.startPosition = Integer.valueOf(startPosition);
    }

    public String getDurationFormat() {
        return durationFormat;
    }

    public void setDurationFormat(String durationFormat) {
        this.durationFormat = durationFormat;
    }
}
