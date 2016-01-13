package com.sapienter.jbilling.batch;

import com.sapienter.jbilling.common.FormatLogger;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;



/**
 * @author Khobab
 */
public class ExceptionSkipPolicy implements SkipPolicy {

    private static final FormatLogger LOG = new FormatLogger(ExceptionSkipPolicy.class);

    @Override
    public boolean shouldSkip(Throwable exception, int skipCpunt)
            throws SkipLimitExceededException {
        LOG.error("Skipping processing of user, exception:", exception);
        return true;
    }
}
