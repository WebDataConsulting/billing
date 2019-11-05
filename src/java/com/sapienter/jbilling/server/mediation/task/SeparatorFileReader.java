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

import java.util.ArrayList;
import java.util.List;

import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import org.apache.commons.lang.StringUtils;

public class SeparatorFileReader extends AbstractFileReader {
    
    private String fieldSeparator;

    public SeparatorFileReader() {
    }
    
    public static final ParameterDescription PARAMETER_SEPARATOR = 
    	new ParameterDescription("separator", false, ParameterDescription.Type.STR);
    
    
    //initializer for pluggable params
    { 
    	descriptions.add(PARAMETER_SEPARATOR);
    }

    
    
    @Override
    public boolean validate(List<String> messages) {
        boolean retValue = super.validate(messages); 
        
        // optionals
        fieldSeparator = (StringUtils.isBlank(parameters.get(PARAMETER_SEPARATOR.getName()))
                          ? "," : parameters.get(PARAMETER_SEPARATOR.getName()));
       
        return retValue;
    }
    
    @Override
    protected String[] splitFields(String line) {
        return line.split(fieldSeparator, -1);
    }
}
