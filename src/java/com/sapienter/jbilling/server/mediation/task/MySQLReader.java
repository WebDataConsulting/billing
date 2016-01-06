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

package com.sapienter.jbilling.server.mediation.task;

import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;

import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.*;

/**
 * MySQLReader provides a driver string and constructs a url string
 * from plug-in parameters for use by JDBCReader.
 */
public class MySQLReader extends JDBCReader {

	protected static final ParameterDescription PARAM_HOST = new ParameterDescription("host", true, STR);
	protected static final ParameterDescription PARAM_PORT = new ParameterDescription("port", true, STR);

    // initializer for pluggable params
	{
		descriptions.add(PARAM_HOST);
		descriptions.add(PARAM_PORT);
	}

    public MySQLReader() {
    }

    @Override
    public String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }

    @Override
    public String getUrl() {
        String host = (String) parameters.get(PARAM_HOST.getName());
        String port = (String) parameters.get(PARAM_PORT.getName());

        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://");

        if (host != null) url.append(host);
        if (port != null) url.append(":").append(port);

        url.append("/").append(getDatabaseName());

        return url.toString();
    }
}
