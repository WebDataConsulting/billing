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

package com.sapienter.jbilling.server.pluggableTask.admin;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.List;

public class PluggableTaskBL<T> {
    private static final FormatLogger LOG = new FormatLogger(PluggableTaskBL.class);
    private EventLogger eLogger = null;

    private PluggableTaskDAS das = null;
    private PluggableTaskParameterDAS dasParameter = null;
    private PluggableTaskDTO pluggableTask = null;
    
    public PluggableTaskBL(Integer pluggableTaskId) {
        init();
        set(pluggableTaskId);
    }
    
    public PluggableTaskBL() {
        init();
    }
    
    private void init() {
        eLogger = EventLogger.getInstance();        
        das = (PluggableTaskDAS) Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);
        dasParameter = new PluggableTaskParameterDAS();
    }

    public void set(Integer id) {
        pluggableTask = das.find(id);
    }
    
    public void set(Integer entityId, Integer typeId) {
        pluggableTask = das.findByEntityType(entityId, typeId);
    }

    public void set(PluggableTaskDTO task) {
        pluggableTask = task;
    }

    public PluggableTaskDTO getDTO() {
        return pluggableTask;
    }
    
    public int create(Integer executorId, PluggableTaskDTO dto) {
        validate(dto);
        LOG.debug("Creating a new pluggable task row %s", dto);
        pluggableTask = das.save(dto);
        eLogger.audit(executorId, null, ServerConstants.TABLE_PLUGGABLE_TASK,
                pluggableTask.getId(), EventLogger.MODULE_TASK_MAINTENANCE,
                EventLogger.ROW_CREATED, null, null, null);
        das.invalidateCache();
        return pluggableTask.getId();
    }
    
    public static final PluggableTaskWS getWS(PluggableTaskDTO dto){
    	
		PluggableTaskWS ws = new PluggableTaskWS();
		ws.setNotes(dto.getNotes());
		ws.setId(dto.getId());
		ws.setProcessingOrder(dto.getProcessingOrder());
		ws.setTypeId(dto.getType().getId());
		for (PluggableTaskParameterDTO param : dto.getParameters()) {
			ws.getParameters().put(param.getName(), param.getValue());
		}
		ws.setVersionNumber(dto.getVersionNum());
		ws.setOwningEntityId(getOwningEntityId(ws));
		return ws;
    }
    private static final Integer getOwningEntityId(PluggableTaskWS ws) {
    	
    if (ws.getId() == null) {
        return null;
    }
    	return new PluggableTaskBL(ws.getId()).getDTO().getEntityId();
    }
    
    public static final PluggableTaskTypeWS getPluggableTaskTypeWS(PluggableTaskTypeDTO dto){
    	PluggableTaskTypeWS ws = new PluggableTaskTypeWS();
		ws.setId(dto.getId());
		ws.setClassName(dto.getClassName());
		ws.setMinParameters(dto.getMinParameters());
		ws.setCategoryId(dto.getCategory().getId());
		return ws;
	}
    
    public static final PluggableTaskTypeCategoryWS getPluggableTaskTypeCategoryWS(PluggableTaskTypeCategoryDTO dto){
		
    	PluggableTaskTypeCategoryWS ws = new PluggableTaskTypeCategoryWS();
    	ws.setId(dto.getId());
		ws.setInterfaceName(dto.getInterfaceName());
		return ws;
	}

    
    public void createParameter(Integer taskId, 
            PluggableTaskParameterDTO dto) {
        PluggableTaskDTO task = das.find(taskId);
        dto.setTask(task);
        task.addParameter(dasParameter.save(dto));
    }

    public void update(Integer executorId, PluggableTaskDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new SessionInternalError("task to update can't be null");
        }
        validate(dto);

        List<PluggableTaskParameterDTO> parameterDTOList = dasParameter.findAllByTask(dto);
        for (PluggableTaskParameterDTO param: dto.getParameters()) {
            parameterDTOList.remove(dasParameter.find(param.getId()));
            param.expandValue();
        }

        for (PluggableTaskParameterDTO param: parameterDTOList){
            dasParameter.delete(param);
        }

        LOG.debug("updating %s", dto);
        pluggableTask = das.save(dto);
        
        eLogger.audit(executorId, null

                , ServerConstants.TABLE_PLUGGABLE_TASK,
                dto.getId(), EventLogger.MODULE_TASK_MAINTENANCE,
                EventLogger.ROW_UPDATED, null, null, null);

        das.invalidateCache(); // 3rd level cache

        pluggableTask.populateParamValues();
    }
    
    public void delete(Integer executor) {
    	checkInUseBeforeDelete(pluggableTask);
        eLogger.audit(executor, null, ServerConstants.TABLE_PLUGGABLE_TASK,
                pluggableTask.getId(), EventLogger.MODULE_TASK_MAINTENANCE,
                EventLogger.ROW_DELETED, null, null, null);
        das.delete(pluggableTask);

    }

    /**
     * This function has been added to perform 'before delete' check such that if a particular 
     * plugin is in use, system can give a proper error message and not allow deletion.
     * Currently, this is added for not allowing deletion of Mediation tasks, if in use.
     * @param pluggableTaskDto
     */
    private void checkInUseBeforeDelete(PluggableTaskDTO pluggableTaskDto) throws SessionInternalError {
    	
    	if (pluggableTaskDto != null && pluggableTaskDto.getType() != null) {
    		
    		boolean inUse = false;
        	String message = "";
	    	String interfaceName = pluggableTaskDto.getType().getCategory().getInterfaceName();
	    	LOG.debug("interfaceName: %s", interfaceName);

	    	LOG.debug("inUse Flag: %s", inUse);
			if (inUse) {
				throw new SessionInternalError("Plugin is in use and cannot be deleted.", 
						new String[]{message + pluggableTaskDto.getType().getClassName()});
			}
    	}
    }
    
    public void deleteParameter(Integer executor, Integer id) {
        eLogger.audit(executor, null, ServerConstants.TABLE_PLUGGABLE_TASK_PARAMETER,
                id, EventLogger.MODULE_TASK_MAINTENANCE,
                EventLogger.ROW_DELETED, null, null, null);
        PluggableTaskParameterDTO toDelete = dasParameter.find(id);
        toDelete.getTask().removeParameter(toDelete);

        dasParameter.delete(toDelete);
    }


    public void updateParameters(PluggableTaskDTO dto) {

        // update the parameters from the dto
        for (PluggableTaskParameterDTO parameter: dto.getParameters()) {
            updateParameter(parameter); 
        }
    }
    
    private void updateParameter(PluggableTaskParameterDTO dto) {
        dto.expandValue();
        dasParameter.save(dto);

    }
    
    public T instantiateTask()
            throws PluggableTaskException {

        PluggableTaskDTO localTask = getDTO();
        String fqn = localTask.getType().getClassName();
        T result;
        try {
            Class taskClazz = Class.forName(fqn);
                    //.asSubclass(result.getClass());
            result = (T) taskClazz.newInstance();
        } catch (ClassCastException e) {
            throw new PluggableTaskException("Task id: " + pluggableTask.getId()
                    + ": implementation class does not implements PaymentTask:"
                    + fqn, e);
        } catch (InstantiationException e) {
            throw new PluggableTaskException("Task id: " + pluggableTask.getId()
                    + ": Can not instantiate : " + fqn, e);
        } catch (IllegalAccessException e) {
            throw new PluggableTaskException("Task id: " + pluggableTask.getId()
                    + ": Can not find public constructor for : " + fqn, e);
        } catch (ClassNotFoundException e) {
            throw new PluggableTaskException("Task id: " + pluggableTask.getId()
                    + ": Unknown class: " + fqn, e);
        }

        if (result instanceof PluggableTask) {
            PluggableTask pluggable = (PluggableTask) result;
            pluggable.initializeParamters(localTask);
        } else {
            throw new PluggableTaskException("Plug-in has to extend PluggableTask " + 
                    pluggableTask.getId());
        }
        return result;
    }
    
    private void validate(PluggableTaskDTO task) {
        List<ParameterDescription> missingParameters = new ArrayList<ParameterDescription>();
        try {
            // start by getting an instance of this type
            PluggableTask instance = (PluggableTask) PluggableTaskManager.getInstance(
                    task.getType().getClassName(), task.getType().getCategory().getInterfaceName());
            
            // loop through the descriptions of parameters
            for (ParameterDescription param: instance.getParameterDescriptions()) {
                if (param.isRequired()) {
                    if(task.getParameters()== null || task.getParameters().size() == 0) {
                        missingParameters.add(param);
                    } else {
                        boolean found = false;
                        for (PluggableTaskParameterDTO parameter:task.getParameters()) {
                            if (parameter.getName().equals(param.getName()) && parameter.getStrValue() != null &&
                                    parameter.getStrValue().trim().length() > 0) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            missingParameters.add(param);
                        }
                    }
                }
            }
        } catch (PluggableTaskException e) {
            LOG.error("Getting instance of plug-in for validation", e);
            throw new SessionInternalError("Validating plug-in");
        }
        
        if (missingParameters.size() > 0) {
            SessionInternalError exception = new SessionInternalError("Validation of new plug-in");
            String messages[] = new String[missingParameters.size()];
            int f=0;
            for (ParameterDescription param: missingParameters) {
                messages[f] = "PluggableTaskWS,parameter,plugins.error.required_parameter," + param.getName();
                f++;
            }
            exception.setErrorMessages(messages);
            throw exception;
        }
        
        // now validate that the processing order is not already taken
        boolean nonUniqueResult= false;
    	try {
    	    PluggableTaskDTO samePlugin = das.findByEntityCategoryOrder(task.getEntityId(), task.getType().getCategory().getId(), 
                    task.getProcessingOrder());
    	    if (samePlugin != null && !samePlugin.getId().equals(task.getId())) {
    	        nonUniqueResult=true;
    	    }
    	} catch (Exception e) {
    	    nonUniqueResult=true;
    	}
        if (nonUniqueResult) {
            SessionInternalError exception = new SessionInternalError("Invalid processing order of new plug-in");
            exception.setErrorMessages(new String[] {
                    "PluggableTaskWS,processingOrder,plugins.error.same_order," + task.getProcessingOrder()});
            throw exception;
        }
        // Now validation of date fields, end and start date
        // Had to determine if the type of plugin falls under IScheduledTask category
        if(task.getType().getCategory().getInterfaceName().equals(ServerConstants.I_SCHEDULED_TASK)) {
            LOG.debug("This is a scheduled type parameter");
            validateDateParameters(task);
        }
    }

    private void validateDateParameters(PluggableTaskDTO task) {
        List<PluggableTaskParameterDTO> dateParameters = new ArrayList<PluggableTaskParameterDTO>();
        try {
            for (PluggableTaskParameterDTO parameter:task.getParameters()) {
                LOG.debug("Parameter passed is %s",parameter.toString());
                // some hard-coding here :(
                if ((parameter.getName().equals(ServerConstants.PARAM_START_TIME) || parameter.getName().equals(ServerConstants.PARAM_END_TIME)) &&
                        !(Util.canParseDate(parameter.getStrValue(), DateTimeFormat.forPattern(ServerConstants.DATE_TIME_FORMAT))) ) {
                    LOG.debug("This is a date field which cannot be parsed %s",parameter.getValue());
                    dateParameters.add(parameter);
                }
            }
        } catch (Exception e) {
            LOG.error("Getting instance of plug-in for validation", e);
            throw new SessionInternalError("Validating plug-in");
        }

        if (dateParameters.size() > 0) {
            SessionInternalError exception = new SessionInternalError("Validation of new plug-in");
            String messages[] = new String[dateParameters.size()];
            int f=0;
            for (PluggableTaskParameterDTO param: dateParameters) {
                // some hard-coding here :(
                messages[f] = "PluggableTaskWS,parameter,plugins.error.date_incorrect_format," + param.getName() + "," + ServerConstants.DATE_TIME_FORMAT;
                f++;
            }
            exception.setErrorMessages(messages);
            throw exception;
        }

    }
}
