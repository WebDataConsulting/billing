package com.sapienter.jbilling.batch.billing;

import com.sapienter.jbilling.common.FormatLogger;
import org.springframework.batch.item.ItemWriter;

import java.util.List;



/**
 * @author Khobab
 */
public class BillingProcessWriter implements ItemWriter<Integer> {

    private static final FormatLogger logger = new FormatLogger(BillingProcessWriter.class);

    @Override
    public void write(List<? extends Integer> list) throws Exception {
        for (Integer user : list) {
            logger.debug("User # " + user + "was successfully processed");
        }
    }
}
