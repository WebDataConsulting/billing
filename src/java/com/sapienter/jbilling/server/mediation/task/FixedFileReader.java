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

package com.sapienter.jbilling.server.mediation.task;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.mediation.FormatField;

import java.util.ArrayList;
import java.util.List;

public class FixedFileReader extends AbstractFileReader {

    private static final String BLANK = "";

    @Override
    protected String[] splitFields(String line) {

        List<String> fields = new ArrayList<String>();

        for (FormatField formatField : format.getFields()) {
            if (formatField.getStartPosition() == null
                || formatField.getLength() == null
                || formatField.getStartPosition() <= 0
                || formatField.getLength() <= 0) {

                throw new SessionInternalError("Position and length must be positive integers: '" + formatField + "'");
            }

            int start = formatField.getStartPosition() - 1;
            int end = start + formatField.getLength();

            // field end exceeds line length
            if (end > line.length())
                end = line.length();
                                    
            // parse field, or return a blank string if field start exceeds line length
            fields.add(start > line.length() ? BLANK : line.substring(start, end));
        }

        return fields.toArray(new String[fields.size()]);        
    }
}
