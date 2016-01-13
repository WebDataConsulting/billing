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
 * Created on Apr 15, 2003
 *
 */
package com.sapienter.jbilling.server.pluggableTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


public abstract class PluggableTask {

	private static final FormatLogger LOG = new FormatLogger(PluggableTask.class);
    public static final DateTimeFormatter PARAMETER_DATE_FORMAT = DateTimeFormat.forPattern("yyyyMMdd-HHmm");

    public final List<ParameterDescription> descriptions = new ArrayList<ParameterDescription>();

    protected Map<String, String> parameters = null;
    private Integer entityId = null;
    protected PluggableTaskDTO task = null;

    public List<ParameterDescription> getParameterDescriptions() {
        return descriptions;
    }

    protected Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public Integer getTaskId() {
        return task.getId();
    }

    public void initializeParamters(PluggableTaskDTO task)
            throws PluggableTaskException {
        Collection<PluggableTaskParameterDTO> DBparameters = task.getParameters();
        parameters = new HashMap<String, String>();
        entityId = task.getEntityId();
        this.task = task;
        if (DBparameters.size() < task.getType().getMinParameters()) {
            throw new PluggableTaskException("Type [" + task.getType().getClassName() + "] requires at least " +
                    task.getType().getMinParameters() + " parameters." +
                    DBparameters.size() + " found.");
        }

        if (DBparameters.isEmpty()) {
            return;
        }

        for (PluggableTaskParameterDTO parameter : DBparameters) {
            Object value = parameter.getIntValue();
            if (value == null) {
                value = parameter.getStrValue();
                if (value == null) {
                    value = parameter.getFloatValue();
                }
            }

            // change: all the parameters will be strings in jB3. TODO: drop the int_value, float_value columns
            parameters.put(parameter.getName(), value.toString());
        }
    }

    /**
     * Returns the plug-in parameter value as a String if it exists, or
     * returns the given default value if it doesn't
     *
     * @param key plug-in parameter name
     * @param defaultValue default value if parameter not defined
     * @return parameter value, or default if not defined
     */
    protected String getParameter(String key, String defaultValue) {
        String value = parameters.get(key);
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }

    /**
     * Returns the plug-in parameter value as an Integer if it exists, or
     * returns the given default value if it doesn't
     *
     * @param key plug-in parameter name
     * @param defaultValue default value if parameter not defined
     * @return parameter value, or default if not defined
     */
    protected Integer getParameter(String key, Integer defaultValue) throws PluggableTaskException {
        String value = parameters.get(key);

        try {
            return StringUtils.isNotBlank(value) ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            throw new PluggableTaskException(key + " could not be parsed as an integer!", e);
        }
    }

    /**
     * Returns the plug-in parameter value as a boolean value if it exists, or
     * returns the given default value if it doesn't.
     *
     * "true" and "True" equals Boolean.TRUE, all other values equate to false.
     *
     * @param key plug-in parameter name
     * @param defaultValue default value if parameter not defined
     * @return parameter value, or default if not defined
     */
    protected Boolean getParameter(String key, Boolean defaultValue) {
        String value = parameters.get(key);
        return StringUtils.isNotBlank(value) ? (value).equalsIgnoreCase("true") : defaultValue;
    }

    /**
     * Returns the plug-in parameter value as a Date value if it exists, or
     * returns the given default value if it doesn't.
     *
     * Parameter date strings must be in the format "yyyyMMdd-HHmm"
     *
     * @param key plug-in parameter name
     * @param defaultValue default value if parameter not defined
     * @return parameter value, or default if not defined
     * @throws PluggableTaskException thrown if parameter could not be parsed as a date
     */
    protected Date getParameter(String key, Date defaultValue) throws PluggableTaskException {
        String value = parameters.get(key);

        try {
            return StringUtils.isNotBlank(value) ? PARAMETER_DATE_FORMAT.parseDateTime(value).toDate() : defaultValue;
        } catch (IllegalArgumentException e) {
            throw new PluggableTaskException(key + " could not be parsed as a date!", e);
        }
    }

    public boolean validate() {
        if (getParameterDescriptions() != null) {
            // validate that those required are present
            for (ParameterDescription param: getParameterDescriptions()) {
                if (param.isRequired()) {
                    if(parameters == null || !parameters.containsKey(param.getName())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * The getter and setter methods for the class field parameters
     * is provided only for the sole purpose of injecting a pluggable
     * task via spring configuration for tests that run without the
     * jbilling running.
     * @return
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
