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
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Igor Poteryaev
 */
public class EmailAndPaymentUserReader implements ItemReader<Integer> {

    private static final FormatLogger LOG = new FormatLogger(EmailAndPaymentUserReader.class);

    private List<Integer> ids;

    private StepExecution stepExecution;

    @BeforeStep
    public void beforeStepStepExecution(StepExecution stepExecution) {
        LOG.debug("Entering beforeStepStepExecution()");
        this.stepExecution = stepExecution;
        Integer minValue = this.stepExecution.getExecutionContext().getInt("minValue");
        Integer maxValue = this.stepExecution.getExecutionContext().getInt("maxValue");
        ids = getIdsInRange(minValue, maxValue);
        LOG.debug("Leaving beforeStepStepExecution() - Total # %s ids were found for", ids.size());
    }

    /**
     * returns next values present in a user list.
     */
    @Override
    public synchronized Integer read() {

        LOG.debug("Entering read()");
        if (ids.size() > 0) {
            Integer removed = ids.remove(0);
            LOG.debug("Returning id # %s from the list of total size # %s", removed, ids.size());
            return removed;
        }
        return null;
    }

    /**
     * returns a subset of user ids that lies with in given range
     *
     * @param start : first id of range
     * @param end   : last id of range
     * @return : list of ids that lies within range
     */
    private List<Integer> getIdsInRange(Integer start, Integer end) {
        List<Integer> required = new ArrayList<Integer>();
        @SuppressWarnings("unchecked")
        List<Integer> userIds = (List<Integer>) this.stepExecution
                .getJobExecution()
                .getExecutionContext()
                .get(ServerConstants.JOBCONTEXT_SUCCESSFULL_USERS_LIST_KEY);
        for (Integer id : userIds) {
            if (id >= start && id <= end) {
                required.add(id);
            }
        }
        return required;
    }
}
