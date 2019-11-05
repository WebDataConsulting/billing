/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.InvalidArgumentException;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.mediation.db.*;
import com.sapienter.jbilling.server.mediation.task.IMediationErrorHandler;
import com.sapienter.jbilling.server.mediation.task.IMediationProcess;
import com.sapienter.jbilling.server.mediation.task.IMediationReader;
import com.sapienter.jbilling.server.mediation.task.MediationResult;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.*;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 *
 * @author emilc
 **/
@Transactional( propagation = Propagation.REQUIRED )
public class MediationSessionBean implements IMediationSessionBean {

    private static final FormatLogger LOG = new FormatLogger(MediationSessionBean.class);

    private static final ConcurrentMap<Integer, Boolean> MEDIATION_RUNNING= new ConcurrentHashMap<Integer, Boolean>();

    /**
     * Triggers all the mediation process for all configurations.
     *
     * @param entityId entity id
     * @see MediationSessionBean#triggerMediationByConfiguration(Integer, Integer)
     */
    public void trigger(Integer entityId) {
        LOG.debug("Running mediation trigger for entity %s", entityId);

        /*MEDIATION_RUNNING.putIfAbsent(entityId, false);
        if (!MEDIATION_RUNNING.replace(entityId, false, true)) {
            LOG.warn("Failed to trigger billing process at %s, another process is already running.", new Date());
            return;
        }*/

        StopWatch watch = new StopWatch("trigger watch");
        watch.start();

        // local instance of this bean to invoke transactional methods
        IMediationSessionBean local = Context.getBean(Context.Name.MEDIATION_SESSION);

        List<String> errorMessages = new ArrayList<String>();

        // process each mediation configuration for this entity
        for (MediationConfiguration cfg : local.getAllConfigurations(entityId)) {
            try {
                Integer retVal= local.triggerMediationByConfiguration(cfg.getId(), entityId);
                LOG.debug("Mediation trigger for configuration %s, returned %s", cfg.getId(), retVal);
            } catch (Exception ex) {
                LOG.error("Exception occurred triggering mediation configuration %s", cfg.getId(), ex);
                errorMessages.add(ex.getMessage());
            }
        }

        // throw a SessionInternalError of errors were returned from the configuration run (possible plugin errors)
        if (!errorMessages.isEmpty()) {
            StringBuilder builder = new StringBuilder("Errors during mediation triggering: \n");
            for (String message : errorMessages) {
                builder.append(message).append("\n");
            }
            throw new SessionInternalError(builder.toString());
        }

        watch.stop();
        LOG.debug("Mediation process finished running. Duration (ms): %s", watch.getTotalTimeMillis());
    }

    /**
     * Triggers the mediation process for a specific configuration.
     *
     * Only one mediation process can be run for a configuration at a time. Multiple configurations can be
     * run, asynchronously but we do not allow the same configuration to overlap.
     *
     * @param configId configuration id to run
     * @param entityId entity id
     * @return running mediation process id
     */
    public Integer triggerMediationByConfiguration(final Integer configId, final Integer entityId) {
        LOG.debug("Running mediation trigger for entity " + entityId + " and configuration " + configId);

        // get the local bean & DAS early on to prevent delays recording process start
        IMediationSessionBean local = Context.getBean(Context.Name.MEDIATION_SESSION);
        MediationProcessDAS mediationDAS = new MediationProcessDAS();

        // get the mediation configuration to run
        MediationConfiguration cfg = getMediationConfiguration(configId);
        if (cfg == null || !cfg.getEntityId().equals(entityId)) {
            LOG.error("Mediation configuration %s does not exists!", configId);
            return null;
        }

        /*
            There can only be one process running for this entity, check that there is
            no other mediation process running for this entity before continuing.
         */
        if (mediationDAS.isConfigurationProcessing(cfg.getId())) {
            LOG.debug("Entity %s already has a running mediation process for configuration %s, skipping run", entityId, configId);
            return null;
        }

        // fetch mediation processing plug in (usually a rules based processor)
        final IMediationProcess processTask;
        try {
            PluggableTaskManager<IMediationProcess> taskManager
                    = new PluggableTaskManager<IMediationProcess>(entityId, ServerConstants.PLUGGABLE_TASK_MEDIATION_PROCESS);
            processTask = taskManager.getNextClass();
        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Could not retrieve mediation process plug-in.", e);
        }

        if (processTask == null) {
            LOG.debug("Entity " + entityId + " does not have a mediation process plug-in");
            return null;
        }


        /*
            Double check that we're still the only mediation process running for configuration before we create
            a new MediationProcess record. The mediation processing plug-in may have a long
            instantiation time which leaves a window for another overlapping process to be created.
         */
        if (mediationDAS.isConfigurationProcessing(cfg.getId())) {
            LOG.debug("Entity %s already has an existing mediation process for configuration %s, skipping run", entityId, configId);
            return null;
        }

        final Integer processId = local.createProcessRecord(cfg).getId(); // create process record and mark start time
        final Integer executorId = new EntityBL().getRootUser(entityId); // root user of this entity to be used for order updates

        // run in separate thread
        Thread mediationThread = new Thread(new Runnable() {
            IMediationSessionBean local = (IMediationSessionBean) Context.getBean(Context.Name.MEDIATION_SESSION);
            public void run() {
                local.performMediation(processTask, configId, processId, executorId, entityId);
            }
        });
        mediationThread.start();

        return processId;
    }

    /**
     * Perform the actual mediation by instantiating the reader plug-in for the configuration, reading in the
     * records and processing them with the given {@link IMediationProcess} task.
     *
     * @param processTask process task plug-in to use for processing records
     * @param configurationId mediation configuration id to run
     * @param processId process id to attach to
     * @param executorId user id to use for database updates
     * @param entityId entity id
     */
    public void performMediation(IMediationProcess processTask, Integer configurationId, Integer processId, Integer executorId, Integer entityId) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("Start Mediation for Configuration " + configurationId);

        MediationConfiguration cfg = new MediationConfigurationDAS().find(configurationId);
        MediationProcess process = new MediationProcessDAS().findNow(processId);

        // fetch mediation reader plug-in
        IMediationReader reader;
        try {
            PluggableTaskBL<IMediationReader> readerTask = new PluggableTaskBL<IMediationReader>();
            readerTask.set(cfg.getPluggableTask());
            reader = readerTask.instantiateTask();
        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Could not instantiate mediation reader plug-in.", e);
        }

        // local instance of this bean to invoke transactional methods
        IMediationSessionBean local = Context.getBean(Context.Name.MEDIATION_SESSION);

        List<String> errorMessages = new ArrayList<String>();
        try {
            // read records and normalize using the MediationProcess plug-in
            if (reader.validate(errorMessages)) {
                /*
                    Catch exceptions and log errors instead of re-throwing as SessionInternalError
                    so that the remaining mediation configurations can be run, and so that this
                    process can be "completed" by setting the end date.
                 */
                try {
                    for (List<Record> thisGroup : reader) {
                        LOG.debug("Now processing %s records.", thisGroup.size());
                        local.normalizeRecordGroup(processTask, executorId, process, thisGroup, entityId, cfg);
                    }
                } catch (TaskException e) {
                    LOG.error("Exception occurred processing mediation records.", e);
                } catch (Throwable t) {
                    LOG.error("Unhandled exception occurred during mediation.", t);
                }
            }
        } finally {
            // process should be "ended' anyway
            // mark process end date
            local.updateProcessRecord(process, new Date());
            LOG.debug("Configuration '%s' finished at %s", cfg.getName(), process.getEndDatetime());
        }

        // throw a SessionInternalError of errors were returned from the reader plug-in
        if (!errorMessages.isEmpty()) {
            StringBuilder builder = new StringBuilder("Invalid reader plug-in configuration \n");
            for (String message : errorMessages) {
                builder.append("ERROR: ")
                    .append(message)
                    .append("\n");
            }
            throw new SessionInternalError(builder.toString());
        }
        stopWatch.stop();
        LOG.debug(stopWatch.prettyPrint());
    }


    /**
     * Create a new MediationProcess for the given configuration, marking the
     * start time of the process and initializing the affected order count.
     *
     * @param cfg mediation configuration
     * @return new MediationProcess record
     */
    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public MediationProcess createProcessRecord(MediationConfiguration cfg) {
        MediationProcessDAS processDAS = new MediationProcessDAS();
        MediationProcess process = new MediationProcess();
        process.setConfiguration(cfg);
        process.setStartDatetime(Calendar.getInstance().getTime());
        process.setOrdersAffected(0);
        process = processDAS.save(process);
        processDAS.flush();
        processDAS.detach(process);
        return process;
    }

    /**
     * Updated the end time of the given MediationProcess, effectively marking
     * the process as completed.
     *
     * @param process MediationProcess to update
     * @param enddate end time to set
     * @return updated MediationProcess record
     */
    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public MediationProcess updateProcessRecord(MediationProcess process, Date enddate) {
        process = new MediationProcessDAS().findNow(process.getId());
        process.setEndDatetime(enddate);
        return process;
    }

    /**
     * Returns true if a running MediationProcess exists for the given entity id. A
     * process is considered to be running if it does not have an end time.
     *
     * @param entityId entity id to check
     * @return true if a process is running for the given entity, false if not
     */
    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public boolean isMediationProcessRunning(Integer entityId) {
        return new MediationProcessDAS().isProcessing(entityId);
    }

    public ProcessStatusWS getMediationProcessStatus(Integer entityId) {
        MediationProcessDAS processDAS = new MediationProcessDAS();
        MediationProcess process = processDAS.getLatestMediationProcess(entityId);
        if (process == null) {
            return null;
        } else {
            ProcessStatusWS result = new ProcessStatusWS();
            result.setStart(process.getStartDatetime());
            result.setEnd(process.getEndDatetime());
            result.setProcessId(process.getId());
            if (process.getEndDatetime() == null) {
                result.setState(ProcessStatusWS.State.RUNNING);
            } else if (processDAS.isMediationProcessHasFailedRecords(process.getId())) {
                result.setState(ProcessStatusWS.State.FAILED);
            } else {
                result.setState(ProcessStatusWS.State.FINISHED);
            }
            return result;
        }
    }

    public List<MediationProcess> getAll(Integer entityId) {
        MediationProcessDAS processDAS = new MediationProcessDAS();
        List<MediationProcess> result = processDAS.findAllByEntity(entityId);
        processDAS.touch(result);
        return result;

    }

    /**
     * Returns a list of all MediationConfiguration's for the given entity id.
     *
     * @param entityId entity id
     * @return list of mediation configurations for entity, empty list if none found
     */
    public List<MediationConfiguration> getAllConfigurations(Integer entityId) {
        MediationConfigurationDAS cfgDAS = new MediationConfigurationDAS();
        return cfgDAS.findAllByEntity(entityId);
    }

    protected MediationConfiguration getMediationConfiguration(Integer configurationId) {
        MediationConfigurationDAS cfgDAS = new MediationConfigurationDAS();
        return cfgDAS.findNow(configurationId);
    }

    public void createConfiguration(MediationConfiguration cfg) {
        MediationConfigurationDAS cfgDAS = new MediationConfigurationDAS();

        cfg.setCreateDatetime(Calendar.getInstance().getTime());
        cfgDAS.save(cfg);

    }

    public List<MediationConfiguration> updateAllConfiguration(Integer executorId, List<MediationConfiguration> configurations)
            throws InvalidArgumentException {
        MediationConfigurationDAS cfgDAS = new MediationConfigurationDAS();
        List<MediationConfiguration> retValue = new ArrayList<MediationConfiguration>();
        try {

            for (MediationConfiguration cfg : configurations) {
                // if the configuration is new, the task needs to be loaded
                if (cfg.getPluggableTask().getEntityId() == null) {
                    PluggableTaskDAS pt = (PluggableTaskDAS) Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);
                    PluggableTaskDTO task = pt.find(cfg.getPluggableTask().getId());
                    if (task != null && task.getEntityId().equals(cfg.getEntityId())) {
                        cfg.setPluggableTask(task);
                    } else {
                        throw new InvalidArgumentException("Task not found or " +
                                "entity of pluggable task is not the same when " +
                                "creating a new mediation configuration", 1);
                    }
                }
                retValue.add(cfgDAS.save(cfg));
            }
            return retValue;
        } catch (EntityNotFoundException e1) {
            throw new InvalidArgumentException("Wrong data saving mediation configuration", 1, e1);
        } catch (InvalidArgumentException e2) {
            throw new InvalidArgumentException(e2);
        } catch (Exception e) {
            throw new SessionInternalError("Exception updating mediation configurations ", MediationSessionBean.class, e);
        }
    }

    public void delete(Integer executorId, Integer cfgId) {
        MediationConfigurationDAS cfgDAS = new MediationConfigurationDAS();

        cfgDAS.delete(cfgDAS.find(cfgId));
        EventLogger.getInstance().audit(executorId, null,
                                        ServerConstants.TABLE_MEDIATION_CFG, cfgId,
                                        EventLogger.MODULE_MEDIATION, EventLogger.ROW_DELETED, null,
                                        null, null);
    }

    /**
     * Calculation number of records for each of the existing mediation record statuses
     *
     * @param entityId EntityId for searching mediationRecords
     * @return map of mediation status as a key and long value as a number of records whit given status
     */
    public Map<MediationRecordStatusDTO, Long> getNumberOfRecordsByStatuses(Integer entityId) {
        MediationRecordDAS recordDas = new MediationRecordDAS();
        MediationRecordStatusDAS recordStatusDas = new MediationRecordStatusDAS();
        Map<MediationRecordStatusDTO, Long> resultMap = new HashMap<MediationRecordStatusDTO, Long>();
        List<MediationRecordStatusDTO> statuses = recordStatusDas.findAll();

        //propagate proxy objects for using out of the transaction
        recordStatusDas.touch(statuses);
        for (MediationRecordStatusDTO status : statuses) {
            Long recordsCount = recordDas.countMediationRecordsByEntityIdAndStatus(entityId, status);
            resultMap.put(status, recordsCount);
        }
        return resultMap;
    }

    public boolean hasBeenProcessed(MediationProcess process, Record record) {
        MediationRecordDAS recordDas = new MediationRecordDAS();

        // validate that this group has not been already processed
        if (recordDas.processed(record.getKey())) {
            LOG.debug("Detected duplicated of record: %s", record.getKey());
            return true;
        }
        LOG.debug("Detected record as a new event: %s", record.getKey());

        // assign to record DONE_AND_BILLABLE status as default before processing
        // after actual processing it will be updated
        MediationRecordStatusDTO status = new MediationRecordStatusDAS().find(ServerConstants.MEDIATION_RECORD_STATUS_DONE_AND_BILLABLE);
        MediationRecordDTO dbRecord = new MediationRecordDTO(record.getKey(),
                                                             Calendar.getInstance().getTime(),
                                                             process,
                                                             status);
        recordDas.save(dbRecord);
        recordDas.flush();

        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void normalizeRecordGroup(IMediationProcess processTask, Integer executorId,
                                     MediationProcess mediationProcess, List<Record> thisGroup, Integer entityId,
                                     MediationConfiguration cfg) throws TaskException {

        LOG.debug("Normalizing %s records ...", thisGroup.size());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("Pre-processing");

        // validate that these records have not been already processed
        thisGroup= thisGroup.parallelStream().filter(it -> !hasBeenProcessed(mediationProcess, it)).collect(Collectors.toList());

        if (thisGroup.size() == 0) {
            return; // it could be that they all have been processed already
        }

        ArrayList<MediationResult> results = new ArrayList<MediationResult>(0);

        // call the plug-in to resolve these records
        stopWatch.stop();
        LOG.debug(stopWatch.prettyPrint());
        stopWatch.start("Processing");
        processTask.process(thisGroup, results, cfg.getName());
        stopWatch.stop();
        LOG.debug(stopWatch.prettyPrint());

        stopWatch.start("Post-Processing");
        LOG.debug("Processing " + thisGroup.size()
                + " records took: " + stopWatch.getLastTaskTimeMillis() + "ms,"
                + " or " + new Double(thisGroup.size()) / stopWatch.getLastTaskTimeMillis() * 1000D + " records/sec");

        // this process came from a different transaction (persistent context)
        MediationProcess process = new MediationProcessDAS().findNow(mediationProcess.getId());
        new MediationProcessDAS().reattachUnmodified(process);

        // go over the results
        for (MediationResult result : results) {
            if (!result.isDone()) {
                // this is an error, the rules failed somewhere because the
                // 'done' flag is still false.
                LOG.debug("Record result is not done");

                // errors presented, status of record should be updated
                assignStatusToMediationRecord(result.getRecordKey(),
                                              new MediationRecordStatusDAS().find(ServerConstants.MEDIATION_RECORD_STATUS_ERROR_DETECTED));

                // call error handler for mediation errors
                handleMediationErrors(findRecordByKey(thisGroup, result.getRecordKey()),
                                      resolveMediationResultErrors(result),
                                      entityId, cfg);

            } else if (!result.getErrors().isEmpty()) {
                // There are some user-detected errors
                LOG.debug("Record result is done with errors");

                //done, but errors assigned by rules. status of record should be updated
                assignStatusToMediationRecord(result.getRecordKey(),
                                              new MediationRecordStatusDAS().find(ServerConstants.MEDIATION_RECORD_STATUS_ERROR_DECLARED));
                // call error handler for rules errors
                handleMediationErrors(findRecordByKey(thisGroup, result.getRecordKey()),
                                      result.getErrors(),
                                      entityId, cfg);
            } else {
                // this record was process without any errors
                LOG.debug("Record result is done");

                if (result.getLines() == null || result.getLines().isEmpty()) {
                    //record was processed, but order lines was not affected
                    //now record has status DONE_AND_BILLABLE, it should be changed
                    assignStatusToMediationRecord(result.getRecordKey(),
                                                  new MediationRecordStatusDAS().find(ServerConstants.MEDIATION_RECORD_STATUS_DONE_AND_NOT_BILLABLE));
                    //not needed to update order affected or lines in this case

                } else {
                    //record has status DONE_AND_BILLABLE, only needed to save processed lines
                    process.setOrdersAffected(process.getOrdersAffected() + result.getLines().size());
                    
                    // relate this order with this process
                    MediationOrderMap map = new MediationOrderMap();
                    map.setMediationProcessId(process.getId());
                    map.setOrderId(result.getCurrentOrder().getId());

                    MediationMapDAS mapDas = new MediationMapDAS();
                    mapDas.save(map);
                    // add the record lines
                    // todo: could be problematic if asynchronous mediation processes are running.
                    // a better approach is to link MediationResult to the record by the unique ID -- future enhancement
                    saveEventRecordLines(result.getDiffLines(), new MediationRecordDAS().findNewestByKey(result.getRecordKey()),
                                         result.getEventDate(),
                                         result.getDescription());
                }
            }
        }
        stopWatch.stop();
        LOG.debug(stopWatch.prettyPrint());
    }

    public void saveEventRecordLines(List<OrderLineDTO> newLines, MediationRecordDTO record, Date eventDate,
                                     String description) {

        MediationRecordLineDAS mediationRecordLineDas = new MediationRecordLineDAS();

        for (OrderLineDTO line : newLines) {
            MediationRecordLineDTO recordLine = new MediationRecordLineDTO();

            recordLine.setEventDate(eventDate);
            OrderLineDTO dbLine = new OrderLineDAS().find(line.getId());
            recordLine.setOrderLine(dbLine);
            recordLine.setAmount(line.getAmount());
            recordLine.setQuantity(line.getQuantity());
            recordLine.setRecord(record);
            recordLine.setDescription(description);

            recordLine = mediationRecordLineDas.save(recordLine);
            // no need to link to the parent record. The association is completed already
           // record.addLine(recordLine);
        }
    }

    public List<MediationRecordLineDTO> getMediationRecordLinesForOrder(Integer orderId) {
        List<MediationRecordLineDTO> events = new MediationRecordLineDAS().findByOrder(orderId);
        for (MediationRecordLineDTO line : events) {
            line.toString(); //as a touch
        }
        return events;
    }

    public List<MediationRecordDTO> getMediationRecordsByMediationProcess(Integer mediationProcessId) {
        return new MediationRecordDAS().findByProcess(mediationProcessId);
    }

    private void assignStatusToMediationRecord(String key, MediationRecordStatusDTO status) {
        MediationRecordDAS recordDas = new MediationRecordDAS();
        MediationRecordDTO recordDto = recordDas.findNewestByKey(key);
        if (recordDto != null) {
            recordDto.setRecordStatus(status);
            recordDas.save(recordDto);
        } else {
            LOG.debug("Mediation record with key= %s not found", key);
        }
    }

    private List<String> resolveMediationResultErrors(MediationResult result) {
        List<String> errors = new LinkedList<String>();
        if (result.getLines() == null || result.getLines().isEmpty()) {
            errors.add("JB-NO_LINE");
        }
        if (result.getDiffLines() == null || result.getDiffLines().isEmpty()) {
            errors.add("JB-NO_DIFF");
        }
        if (result.getCurrentOrder() == null) {
            errors.add("JB-NO_ORDER");
        }
        if (result.getUserId() == null) {
            errors.add("JB-NO_USER");
        }
        if (result.getCurrencyId() == null) {
            errors.add("JB-NO_CURRENCY");
        }
        if (result.getEventDate() == null) {
            errors.add("JB-NO_DATE");
        }
        errors.addAll(result.getErrors());
        return errors;
    }

    private Record findRecordByKey(List<Record> records, String key) {
        for (Record r : records) {
            if (r.getKey().equals(key)) {
                return r;
            }
        }
        return null;
    }

    private void handleMediationErrors(Record record,
                                       List<String> errors,
                                       Integer entityId,
                                       MediationConfiguration cfg) {
        if (record == null) return;
        StopWatch watch = new StopWatch("saving errors watch");
        watch.start();
        LOG.debug("Saving mediation result errors: %d", errors.size());

        try {
            PluggableTaskManager<IMediationErrorHandler> tm = new PluggableTaskManager<IMediationErrorHandler>(entityId,
                    ServerConstants.PLUGGABLE_TASK_MEDIATION_ERROR_HANDLER);
            IMediationErrorHandler errorHandler;
            // iterate through all error handlers for current entityId
            // and process errors
            while ((errorHandler = tm.getNextClass()) != null) {
                try {
                    errorHandler.process(record, errors, new Date(), cfg);
                } catch (TaskException e) {
                    // exception catched for opportunity of processing errors by other handlers
                    // and continue mediation process for other records
                    // TO-DO: check requirements about error handling in that case
                    LOG.error(e);
                }
            }

        } catch (PluggableTaskException e) {
            LOG.error(e);
            // it's possible plugin configuration exception
            // TO-DO: check requirements about error handling
            // may be rethrow exception
        }

        watch.stop();
        LOG.debug("Saving mediation result errors done. Duration (mls): %s", watch.getTotalTimeMillis());
    }
}
