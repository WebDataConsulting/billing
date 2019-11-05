package com.sapienter.jbilling.batch.billing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.task.BasicBillingProcessFilterTask;
import com.sapienter.jbilling.server.process.task.IBillingProcessFilterTask;
import com.sapienter.jbilling.server.util.ServerConstants;
import org.hibernate.ScrollableResults;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;



/**
 * @author Khobab
 */
public class BillingProcessPartitioner implements InitializingBean, Partitioner {

    private static final FormatLogger logger = new FormatLogger(BillingProcessPartitioner.class);

    private List<Integer> ids;

    private Integer entityId;
    private Date billingDate;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        logger.debug("Entering partition(), where gridSize # " + gridSize);
        int size = ids.size() - 1;
        int targetSize = size / gridSize + 1;
        logger.debug("Target size for each step # " + targetSize);

        Map<String, ExecutionContext> result = new HashMap<String, ExecutionContext>();
        int number = 0;
        int start = 0;
        int end = start + targetSize - 1;

        while (start <= size) {
            ExecutionContext value = new ExecutionContext();
            result.put("partition" + number, value);

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

    /**
     * This method runs after entityId and billingDate has been set that are received from job parameters.
     * Gets PluggableTaskManager and then finds user id's on the basis of entity id and billing date.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        logger.debug("Entering afterPropertiesSet() - EntityId # " + entityId + " , Date # " + billingDate);

        //currently there is not taskManager for task category 20 in here, so basic filter will be initialized
        @SuppressWarnings("rawtypes")
        PluggableTaskManager taskManager = new PluggableTaskManager(entityId,
                ServerConstants.PLUGGABLE_TASK_BILL_PROCESS_FILTER);
        logger.debug("PluggableTaskManager initialized");

        IBillingProcessFilterTask task = (IBillingProcessFilterTask) taskManager.getNextClass();
        //if one was not configured just use the basic task by default
        if (task == null) {
            logger.debug("No filter was found, initializing basic filter");
            task = new BasicBillingProcessFilterTask();
        }

        logger.debug("Finding user ids and setting them to scrollable results cursor");
        ScrollableResults results = task.findUsersToProcess(entityId, billingDate);
        ids = new ArrayList<Integer>();
        //put the items of scrollableresults in a list
        if (results != null) {
            while (results.next()) {
                ids.add((Integer) results.get(0));
            }
        }
        //sorts list in ascending order so that we can partition ids across multiple step executions
        Collections.sort(ids);
        logger.debug("Leaving afterPropertiesSet() - Total # " + ids.size() + " ids were found for the entityId # " + entityId);
    }

    /**
     * Sets entityId parameter
     *
     * @param entityId :	company id
     */
    public void setEntityId(String entityId) {
        this.entityId = Integer.parseInt(entityId);
    }

    /**
     * Sets billing date parameter
     *
     * @param billingDate :	date on which billing is being done.
     */
    public void setBillingDate(Date billingDate) {
        this.billingDate = billingDate;
    }

}