package com.sapienter.jbilling.batch.billing;

import com.sapienter.jbilling.common.FormatLogger;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

;

public abstract class StepContextHandler {

    private static final FormatLogger logger = new FormatLogger(JobContextHandler.class);

    private ExecutionContext stepExecutionContext;

    public Integer getIntegerFromContext(String key) {
        if (stepExecutionContext.containsKey(key)) {
            return stepExecutionContext.getInt(key);
        }
        return 0;
    }

    public void addIntegerToContext(String key, Integer value) {
        stepExecutionContext.putInt(key, value);
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getIntegerListFromContext(String key) {
        if (stepExecutionContext.containsKey(key)) {
            return (List<Integer>) stepExecutionContext.get(key);
        }
        return new ArrayList<Integer>();
    }

    public void addIntegerListToContext(String key, List<Integer> list) {
        stepExecutionContext.put(key, list);
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, Integer[]> getMapFromContext(String key) {
        if (stepExecutionContext.containsKey(key)) {
            return (Map<Integer, Integer[]>) stepExecutionContext.get(key);
        }
        return new HashMap<Integer, Integer[]>();
    }

    public void addMapToContext(String key, Map<Integer, Integer[]> map) {
        stepExecutionContext.put(key, map);
    }

    public void setStepExecution(StepExecution stepExecution) {
        logger.debug("entered setStepExecution()");
        this.stepExecutionContext = stepExecution.getExecutionContext();
        logger.debug("leaving setStepExecution");
    }

}
