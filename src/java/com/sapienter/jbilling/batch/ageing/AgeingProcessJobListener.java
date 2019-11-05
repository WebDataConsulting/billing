package com.sapienter.jbilling.batch.ageing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.process.AgeingBL;
import com.sapienter.jbilling.server.process.event.AgeingProcessCompleteEvent;
import com.sapienter.jbilling.server.process.event.AgeingProcessStartEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.util.ServerConstants;
import org.hibernate.ScrollableResults;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;



public class AgeingProcessJobListener implements JobExecutionListener {

    private final FormatLogger logger = new FormatLogger(AgeingProcessJobListener.class);

    @Override
    public void afterJob(JobExecution jobExec) {
        logger.debug("Entering afterJob()");
        // get entity id from job parameters to mark ageing process event as complete
        Integer entityId = Integer.parseInt(jobExec.getJobParameters().getString(ServerConstants.BATCH_JOB_PARAM_ENTITY_ID));
        logger.debug("Marking ageing process as complete for entity id # " + entityId);
        EventManager.process(new AgeingProcessCompleteEvent(entityId));
        logger.debug("Destorying list of ids in AgeingProcessUsersLoader");
        jobExec.getExecutionContext().remove(ServerConstants.JOBCONTEXT_USERS_LIST_KEY);
    }

    @Override
    public void beforeJob(JobExecution jobExec) {
        logger.debug("Entering beforeJob()");
        JobParameters jobParams = jobExec.getJobParameters();

        Integer entityId = Integer.parseInt(jobParams.getString(ServerConstants.BATCH_JOB_PARAM_ENTITY_ID));
        Date ageingDate = jobParams.getDate(ServerConstants.BATCH_JOB_PARAM_AGEING_DATE);
        logger.debug("Starting ageing process event for entity id # " + entityId);
        EventManager.process(new AgeingProcessStartEvent(entityId));
        logger.debug("Load ageing users for entity id # " + entityId + " and ageing date # " + ageingDate);
        jobExec.getExecutionContext().put(ServerConstants.JOBCONTEXT_USERS_LIST_KEY, loadAndSort(entityId, ageingDate));
    }

    /**
     * Finds user ids for company id and billing date using suitable find users method
     *
     * @param entityId
     * @param ageingDate
     * @return
     */
    private List<Integer> loadAndSort(Integer entityId, Date ageingDate) {
        logger.debug("Entering loadAndStore() where entityId # %s and billingDate # %s", entityId, ageingDate);
        ScrollableResults results = new AgeingBL().getUsersForAgeing(entityId, ageingDate);
        List<Integer> ids = new ArrayList<Integer>();
        // put the items of scrollableresults in a list
        if (results != null) {
            while (results.next()) {
                ids.add(results.getInteger(0));
            }
            results.close();
        }
        Collections.sort(ids);
        logger.debug("Found %s users loadAndStore() where entityId # %s and billingDate # %s", ids.size(), entityId, ageingDate);
        return ids;
    }
}
