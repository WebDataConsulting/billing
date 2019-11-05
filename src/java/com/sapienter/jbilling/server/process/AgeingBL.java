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

/*
 * Created on Mar 26, 2004
 */
package com.sapienter.jbilling.server.process;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDAS;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.process.task.BasicAgeingTask;
import com.sapienter.jbilling.server.process.task.IAgeingTask;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import org.hibernate.ScrollableResults;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Emil
 */
public class AgeingBL {
    private static final FormatLogger LOG = new FormatLogger(AgeingBL.class);

    private AgeingEntityStepDAS ageingDas = null;
    private AgeingEntityStepDTO ageing = null;
    private EventLogger eLogger = null;

    private static final ConcurrentMap<Integer, Boolean> running = new ConcurrentHashMap<Integer, Boolean>();

    public AgeingBL(Integer ageingId) {
        init();
        set(ageingId);
    }

    public AgeingBL() {
        init();
    }

    private void init() {
        eLogger = EventLogger.getInstance();        
        ageingDas = new AgeingEntityStepDAS();
    }

    public AgeingEntityStepDTO getEntity() {
        return ageing;
    }
    
    public void set(Integer id) {
        ageing = ageingDas.find(id);
    }

    public ScrollableResults getUsersForAgeing(Integer entityId, Date ageingDate) {
        try {
            PluggableTaskManager<IAgeingTask> taskManager
                    = new PluggableTaskManager<IAgeingTask>(entityId, ServerConstants.PLUGGABLE_TASK_AGEING);

            IAgeingTask task = taskManager.getNextClass();
            // If one was not configured just use the basic task by default
            if (task == null) {
                task = new BasicAgeingTask();
            }
            return task.findUsersToAge(entityId, ageingDate);

        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Ageing task exception while running ageing review.", e);
        }
    }

    public List<InvoiceDTO> reviewUserForAgeing(Integer entityId, Integer userId, Date today) {
        try {
            PluggableTaskManager<IAgeingTask> taskManager
                    = new PluggableTaskManager<IAgeingTask>(entityId, ServerConstants.PLUGGABLE_TASK_AGEING);

            CompanyDTO company = new EntityBL(entityId).getEntity();

            IAgeingTask task = taskManager.getNextClass();
            List<InvoiceDTO> overdueInvoices = new ArrayList<InvoiceDTO>();

            while (task != null) {
                overdueInvoices.addAll(task.reviewUser(entityId, company.getAgeingEntitySteps(), userId, today, null));
                task = taskManager.getNextClass();
            }

            return overdueInvoices;

        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Ageing task exception while running ageing review.", e);
        }
    }

    public void out(UserDTO user, Integer excludedInvoiceId) {
        try {
            PluggableTaskManager<IAgeingTask> taskManager
                    = new PluggableTaskManager<IAgeingTask>(user.getCompany().getId(), ServerConstants.PLUGGABLE_TASK_AGEING);

            IAgeingTask task = taskManager.getNextClass();
            while (task != null) {
                task.removeUser(user, excludedInvoiceId, null);
                task = taskManager.getNextClass();
            }

        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Ageing task exception when removing user from ageing.", e);
        }
    }

    public void setUserStatus(Integer executorId, Integer userId, Integer statusId, Date today) {
        UserDTO user = new UserDAS().find(userId);
        UserStatusDTO userStatus = new UserStatusDAS().find(statusId);

        try {
            PluggableTaskManager<IAgeingTask> taskManager
                    = new PluggableTaskManager<IAgeingTask>(user.getCompany().getId(), ServerConstants.PLUGGABLE_TASK_AGEING);

            IAgeingTask task = taskManager.getNextClass();
            while (task != null) {
                task.setUserStatus(user, userStatus, today, executorId);
                task = taskManager.getNextClass();
            }

        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Ageing task exception when setting user status.", e);
        }
    }

    public AgeingDTOEx[] getOrderedSteps(Integer entityId) {
        return getSteps(entityId, null, null);
    }

    public AgeingDTOEx[] getSteps(Integer entityId,
                                  Integer executorLanguageId, Integer languageId) {

        List<AgeingEntityStepDTO> ageingSteps = ageingDas.findAgeingStepsForEntity(entityId);

        AgeingDTOEx[] result  = ageingSteps.stream()
                .map( it -> convertToDTOEx(it, executorLanguageId, languageId))
                .toArray(AgeingDTOEx[]::new);

        return result;
    }

    private AgeingDTOEx convertToDTOEx(final AgeingEntityStepDTO step, final Integer executorLanguageId, final Integer languageId) {
        AgeingDTOEx newStep = new AgeingDTOEx();
        newStep.setStatusId(step.getUserStatus().getId());
        UserStatusDTO statusRow = new UserStatusDAS().find(newStep.getStatusId());
        if (executorLanguageId != null) {
            newStep.setStatusStr(statusRow.getDescription(executorLanguageId));
        } else {
            newStep.setStatusStr(statusRow.getDescription());
        }
        newStep.setId(step.getId());

        newStep.setDays(step.getDays());
        newStep.setSuspend(step.getSuspend());
        newStep.setRetryPayment(step.getRetryPayment());
        newStep.setSendNotification(step.getSendNotification());

        newStep.setCanLogin(statusRow.getCanLogin());
        newStep.setDays(step.getDays());
        if (languageId != null) {
            newStep.setDescription(step.getDescription(languageId));
        } else {
            newStep.setDescription(step.getDescription());
        }

        newStep.setInUse(ageingDas.isAgeingStepInUse(step.getId()));
        return newStep;
    }

    public void setSteps(Integer entityId, Integer languageId, AgeingDTOEx[] steps) throws NamingException {
        LOG.debug("Setting a total of %s steps", steps.length);

        //validate unique steps
        List<AgeingDTOEx> ageingStepsList = new ArrayList<AgeingDTOEx>(Arrays.asList(steps));
        List<AgeingDTOEx> stepsList;
        List<Integer>  days;
        for (AgeingDTOEx step : steps) {
            stepsList = ageingStepsList;
            stepsList.remove(step);

            days = new ArrayList<>();
            for(AgeingDTOEx stp: stepsList){
                days.add(stp.getDays());
            }
            if(days.contains(step.getDays())){
                LOG.debug("Received non-unique ageing step(s) : %s", step);
                throw new SessionInternalError("There are non-unique ageing step(s)");
            }
        }

        List<AgeingDTOEx> existedAgeingSteps = new ArrayList<AgeingDTOEx>(
                Arrays.asList(getSteps(entityId, languageId, languageId)));

        for (AgeingDTOEx step : steps) {

            LOG.debug("Processing step for persisting: %s", step);

            AgeingDTOEx persistedStep = null;
            UserStatusDTO stepUserStatus = new UserStatusDAS().find(step.getStatusId());
            if (stepUserStatus != null && stepUserStatus.getAgeingEntityStep() != null) {
                for (AgeingDTOEx stp : existedAgeingSteps) {
                    LOG.debug("Matching received step: %s with existing step: %s", stepUserStatus.getAgeingEntityStep().getId(), stp.getId());
                    if (stepUserStatus.getAgeingEntityStep().getId() == stp.getId()) {
                        persistedStep = stp;
                        break;
                    }
                }
            }

            if (persistedStep != null) {
                existedAgeingSteps.remove(persistedStep);
                ageing = ageingDas.find(persistedStep.getId());
                // update
                LOG.debug("Updating ageing step# %s", ageing.getId());
                ageing.setDays(step.getDays());
                ageing.setDescription(step.getStatusStr(), languageId);

                ageing.setSuspend(step.getSuspend());
                UserStatusDAS userDas = new UserStatusDAS();
                UserStatusDTO userStatusDTO = userDas.find(ageing.getUserStatus().getId());
                if (!userStatusDTO.getDescription(languageId).equals(ageing.getDescription(languageId))) {
                    LOG.debug("Updating user status description to: %s", ageing.getDescription(languageId));
                    userStatusDTO.setDescription(ageing.getDescription(languageId), languageId);
                    userDas.save(userStatusDTO);
                }
                ageing.setRetryPayment(step.getRetryPayment());
                ageing.setSendNotification(step.getSendNotification());

            } else {
                LOG.debug("Creating step.");
                ageingDas.create(entityId, step.getStatusStr(),
                        languageId, step.getDays(), step.getSendNotification(),
                        step.getRetryPayment(), step.getSuspend()
                );
            }
        }

        for (AgeingDTOEx ageingStep : existedAgeingSteps) {
            if (ageingStep.getInUse()) {
                throw new SessionInternalError("Ageing entity step is in use and can't be deleted!");
            }
            AgeingEntityStepDTO ageingDto = ageingDas.find(ageingStep.getId());
            UserStatusDAS userStatusDas = new UserStatusDAS();
            userStatusDas.delete(ageingDto.getUserStatus());
            ageingDas.delete(ageingDto);
        }
    }

    public AgeingWS getWS(AgeingDTOEx dto) {
    	if(null == dto) return null;
    	
		AgeingWS ws = new AgeingWS();
		ws.setStatusId(dto.getStatusId());
		ws.setStatusStr(dto.getStatusStr());
		ws.setWelcomeMessage(dto.getWelcomeMessage());
		ws.setFailedLoginMessage(dto.getFailedLoginMessage());
		ws.setSuspended(dto.getSuspend() == 1);
		ws.setPaymentRetry(dto.getRetryPayment() == 1);
		ws.setSendNotification(dto.getSendNotification() == 1);
		ws.setInUse(dto.getInUse());
		ws.setDays(dto.getDays());
		ws.setEntityId((null != dto.getCompany()) ? dto.getCompany().getId()
				: null);

		return ws;
    }

    public AgeingDTOEx getDTOEx(AgeingWS ws) {
        AgeingDTOEx dto= new AgeingDTOEx();
        dto.setStatusId(ws.getStatusId());
        dto.setStatusStr(ws.getStatusStr());
        dto.setSuspend(ws.getSuspended() ? 1 : 0);
        dto.setSendNotification(ws.getSendNotification() ? 1 : 0);
        dto.setRetryPayment(ws.getPaymentRetry() ? 1 : 0);
        dto.setInUse(ws.getInUse());
        dto.setDays(null == ws.getDays() ? 0 : ws.getDays().intValue());
        dto.setWelcomeMessage(ws.getWelcomeMessage());
        dto.setFailedLoginMessage(ws.getFailedLoginMessage());
        return dto;
    }
}
