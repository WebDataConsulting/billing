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
import java.util.Iterator;
import java.util.List;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.server.mediation.Record;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;

/**
 * All readers should extend this class. 
 * All readers need to read a batch of records at a time
 * @author emilc
 */
public abstract class AbstractReader extends PluggableTask implements
        IMediationReader {

    private static final Logger LOG = Logger.getLogger(AbstractReader.class);
    private int batchSize;
    
    public static final ParameterDescription PARAMETER_BATCH_SIZE = 
    	new ParameterDescription("batch_size", false, ParameterDescription.Type.STR);
    
    //initializer for pluggable params
    { 
    	descriptions.add(PARAMETER_BATCH_SIZE);
    }

    public boolean validate(List<String> messages) {
        boolean retValue = true;
        try {
            // the parameter is optional and defaults to 1000 records
            batchSize = getParameter(PARAMETER_BATCH_SIZE.getName(), 100);
            LOG.debug("Batch size for this reader is " + getBatchSize());

        } catch (PluggableTaskException e) {
            retValue = false;
            messages.add(e.getMessage());
        }
        return retValue;
    }

    public abstract Iterator<List<Record>> iterator();

    public int getBatchSize() {
        return batchSize;
    }
}
