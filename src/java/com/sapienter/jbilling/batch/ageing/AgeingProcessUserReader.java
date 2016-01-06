package com.sapienter.jbilling.batch.ageing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.util.Constants;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

;


public class AgeingProcessUserReader implements ItemReader<Integer> {

    private final FormatLogger logger = new FormatLogger(AgeingProcessUserReader.class);

    private Integer minValue;
    private Integer maxValue;

    private List<Integer> ids;

    private StepExecution stepExecution;

    @BeforeStep
    public void beforeStepStepExecution(StepExecution stepExecution) {
        logger.debug("Entering beforeStepStepExecution()");
        this.stepExecution = stepExecution;
        ids = getIdsInRange(minValue, maxValue);
        logger.debug("Leaving beforeStepStepExecution() - Total # %s ids were found for", ids.size());
    }

    /**
     * Removes first element from list and give it to processor
     */
    @Override
    public Integer read() {

        logger.debug("Entering read()");
        if (ids.size() > 0) {
            Integer removed = ids.remove(0);
            logger.debug("Returning id # " + removed + " from the list of total size # " + ids.size());
            return removed;
        }
        return null;
    }

    /**
     * Sets first user id of partition
     *
     * @param billingDate :	date on which billing is being done.
     */
    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    /**
     * Sets last user id of partition
     *
     * @param maxValue :	last id of the user in partition
     */
    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
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
        List<Integer> userIds = (List<Integer>) this.stepExecution.getJobExecution().getExecutionContext().get(Constants.JOBCONTEXT_USERS_LIST_KEY);
        Iterator<Integer> iterator = userIds.iterator();
        while (iterator.hasNext()) {
            Integer id = iterator.next();
            if (id >= start && id <= end) {
                required.add(id);
            }
        }
        return required;
    }
}
