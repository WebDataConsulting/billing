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

package com.sapienter.jbilling.batch.billing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.util.ServerConstants;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Igor Poteryaev
 */
public class EmailAndPaymentPartitioner implements InitializingBean, Partitioner {

    private static final FormatLogger LOG = new FormatLogger(EmailAndPaymentPartitioner.class);

    private List<Integer> ids;

    @Value("#{stepExecution}")
    private StepExecution stepExecution;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        LOG.debug("Entering partition(), where gridSize # %s", gridSize);
        int size = ids.size() - 1;
        int targetSize = size / gridSize + 1;
        LOG.debug("Target size for each step # %s", targetSize);

        Map<String, ExecutionContext> result = new HashMap<String, ExecutionContext>();
        int number = 0;
        int start = 0;
        int end = start + targetSize - 1;

        while (start <= size) {
            ExecutionContext value = new ExecutionContext();
            result.put("email-partition" + number, value);

            if (end >= size) {
                end = size;
            }
            value.putInt("minValue", ids.get(start));
            value.putInt("maxValue", ids.get(end));
            start += targetSize;
            end += targetSize;
            number++;
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.debug("Entering afterPropertiesSet() - stepExecution: %s", this.stepExecution);
        this.ids = (List<Integer>) this.stepExecution
                .getJobExecution()
                .getExecutionContext()
                .get(ServerConstants.JOBCONTEXT_SUCCESSFULL_USERS_LIST_KEY);
        // sorts list in ascending order so that we can partition ids across multiple step executions
        Collections.sort(this.ids);
        LOG.debug("Leaving afterPropertiesSet() - stepExecution: %s, ids.size: ", this.stepExecution, this.ids.size());
    }
}