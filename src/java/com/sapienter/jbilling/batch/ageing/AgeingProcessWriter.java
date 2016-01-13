package com.sapienter.jbilling.batch.ageing;

import com.sapienter.jbilling.common.FormatLogger;
import org.springframework.batch.item.ItemWriter;

import java.util.List;



/**
 * @author Khobab
 */
public class AgeingProcessWriter implements ItemWriter<Integer> {

    private static final FormatLogger logger = new FormatLogger(AgeingProcessWriter.class);

    @Override
    public void write(List<? extends Integer> userList) throws Exception {
        for (Integer userId : userList) {
            logger.debug("UserId # " + userId + " +++ Read, Processed & Written Successfully!");
        }
    }
}
