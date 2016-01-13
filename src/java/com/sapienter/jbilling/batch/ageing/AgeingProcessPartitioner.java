package com.sapienter.jbilling.batch.ageing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.process.AgeingBL;
import org.hibernate.ScrollableResults;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;



/**
 * @author Khobab
 */
public class AgeingProcessPartitioner implements InitializingBean, DisposableBean, Partitioner {

    private static final FormatLogger logger = new FormatLogger(AgeingProcessPartitioner.class);

    private Date ageingDate;
    private Integer entityId;

    private List<Integer> ids;

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
     * This method starts when all the properties of the class are set,
     * gets user ids from database according to entity id and ageing date
     * and saves them in a list to be used by reader
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        logger.debug("Entering afterPropertiesSet()");
        ScrollableResults results = new AgeingBL().getUsersForAgeing(entityId, ageingDate);
        ids = new ArrayList<Integer>();
        //put the items of scrollableresults in a list
        if (results != null) {
            while (results.next()) {
                ids.add((Integer) results.get(0));
            }
        }
        Collections.sort(ids);
        logger.debug("Leaving afterPropertiesSet() - Total # " + ids.size() + " ids were found for the entityId # " + entityId);
    }

    @Override
    public void destroy() throws Exception {
        while (ids.size() > 0) {
            ids.remove(0);
        }
        ids = null;
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
     * Sets ageing date parameter
     *
     * @param billingDate :	date on which billing is being done.
     */
    public void setAgeingDate(Date ageingDate) {
        this.ageingDate = ageingDate;
    }
}