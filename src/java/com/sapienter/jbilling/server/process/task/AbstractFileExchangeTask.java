package com.sapienter.jbilling.server.process.task;/*
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

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

/**
 * Created by marcomanzi on 3/3/14.
 */
public abstract class AbstractFileExchangeTask extends PluggableTask implements IFileExchangeTask {

    protected static final String PARAM_DOWNLOAD = "download";

    protected static final ParameterDescription PARAM_DOWNLOAD_DESC =
            new ParameterDescription(PARAM_DOWNLOAD, true, ParameterDescription.Type.BOOLEAN);

    {
        descriptions.add(PARAM_DOWNLOAD_DESC);
    }

    protected String getParameterValueFor(String parameterName) throws PluggableTaskException {
        String parameter = getParameter(parameterName, "");
        //check that we have the parameter
        if (parameter.trim().isEmpty()) {
            throw new PluggableTaskException("Parameter " + parameterName + " not specified");
        }
        return parameter;
    }

    protected boolean existParameter(String parameterName) {
        String parameter = getParameter(parameterName, "");
        return !parameter.trim().isEmpty();
    }

    public boolean isDownloadTask() throws PluggableTaskException {
        return Boolean.parseBoolean(getParameterValueFor(PARAM_DOWNLOAD));
    }
}
