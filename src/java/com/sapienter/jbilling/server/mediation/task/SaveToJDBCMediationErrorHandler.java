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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.Record;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;

/**
 * This plug-in saves mediation errors to a JDBC database by generating insert statements
 * matching the set of {@link PricingField} objects from the mediation process. This effectively
 * preserves the original CDR record along with error states for later review.
 *
 * This class requires that a database table be created with columns matching the field
 * names from the mediation format XML definition. jBilling does not create this table, it must
 * be created by the end user when installing and configuring this plug-in.
 * 
 * Plug-in parameters:
 *
 *      url                 mandatory parameter, url for JDBC connection to database,
 *                              i.e. jdbc:postgresql://localhost:5432/jbilling_crucible
 *
 *      driver              JDBC driver class for connection to DB, defaults to 'org.postgresql.Driver'
 *      username            username for database, defaults to 'SA'
 *      password            password for database, defaults to a blank string ("")
 *      table_name          table name for saving records, defaults to 'mediation_errors'
 *      errors_column       column name for saving error codes, defaults to 'error_message'
 *      retry_column        column name for saving flag of reprocessing, defaults to 'should_retry'
 *      mediation_cfg_id    id of mediation configuration for filtering errors handling (if param presented)
 *
 * @author Alexander Aksenov
 * @since 31.01.2010
 */
public class SaveToJDBCMediationErrorHandler extends PluggableTask
        implements IMediationErrorHandler {
	
	private static final FormatLogger log = new FormatLogger(SaveToJDBCMediationErrorHandler.class);

    // plug-in parameters
    // mandatory parameter, url with host, port, database, etc
    protected static final ParameterDescription PARAM_DATABASE_URL = 
    	new ParameterDescription("url", true, ParameterDescription.Type.STR);
    // optional, may be used default values
    protected static final ParameterDescription PARAM_DRIVER = 
    	new ParameterDescription("driver", false, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_DATABASE_USERNAME = 
    	new ParameterDescription("username", false, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_DATABASE_PASSWORD = 
    	new ParameterDescription("password", false, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_TABLE_NAME = 
    	new ParameterDescription("table_name", false, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_ERRORS_COLUMN_NAME = 
    	new ParameterDescription("errors_column", false, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_RETRY_COLUMN_NAME = 
    	new ParameterDescription("retry_column", false, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_JBILLING_TIMESTAMP_COLUMN_NAME = 
    	new ParameterDescription("timestamp_column", false, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_MEDIATION_CONFIGURATION_ID = 
    	new ParameterDescription("mediation_cfg_id", false, ParameterDescription.Type.STR);

    // defaults
    public static final String DRIVER_DEFAULT = "org.postgresql.Driver";
    public static final String DATABASE_USERNAME_DEFAULT = "SA";
    public static final String DATABASE_PASSWORD_DEFAULT = "";

    public static final String TABLE_NAME_DEFAULT = "mediation_errors";
    public static final String ERRORS_COLUMN_NAME_DEFAULT = "error_message";
    public static final String RETRY_COLUMN_NAME_DEFAULT = "should_retry";
    public static final String JBILLING_TIMESTAMP_COLUMN_NAME_DEFAULT = "jbilling_timestamp";

    private Boolean mysql;
    
    //initializer for pluggable params
    { 
    	descriptions.add(PARAM_DATABASE_URL);
        descriptions.add(PARAM_DRIVER);
        descriptions.add(PARAM_DATABASE_USERNAME);
        descriptions.add(PARAM_DATABASE_PASSWORD);
        descriptions.add(PARAM_TABLE_NAME);
        descriptions.add(PARAM_ERRORS_COLUMN_NAME);
        descriptions.add(PARAM_RETRY_COLUMN_NAME);
        descriptions.add(PARAM_JBILLING_TIMESTAMP_COLUMN_NAME);
        descriptions.add(PARAM_MEDIATION_CONFIGURATION_ID);
    }


    public void process(Record record, List<String> errors, Date processingTime, MediationConfiguration mediationConfiguration) throws TaskException {
        if (mediationConfiguration != null && getParameter(PARAM_MEDIATION_CONFIGURATION_ID.getName(), (String) null) != null) {
            try {
                Integer configId = Integer.parseInt(getParameter(PARAM_MEDIATION_CONFIGURATION_ID.getName(), ""));
                if (!mediationConfiguration.getId().equals(configId)) {
                    return;
                }
            } catch (NumberFormatException ex) {
                log.error("Error during plug-in parameters parsing, check the configuration", ex);
            }
        }
        log.debug("Perform saving errors to database ");

        Connection connection = null;
        try {
            connection = getConnection();

            String errorColumn = getParameter(PARAM_ERRORS_COLUMN_NAME.getName(), ERRORS_COLUMN_NAME_DEFAULT);
            String retryColumn = getParameter(PARAM_RETRY_COLUMN_NAME.getName(), RETRY_COLUMN_NAME_DEFAULT);
            String timestampColumn = getParameter(PARAM_JBILLING_TIMESTAMP_COLUMN_NAME.getName(), JBILLING_TIMESTAMP_COLUMN_NAME_DEFAULT); 

            List<String> columnNames = new LinkedList<String>();

            // remove extra error columns from incoming pricing fields.
            // if we're re-reading errors from the error table, then we'll end up with duplicate columns
            List<PricingField> fields = record.getFields();
            for (Iterator<PricingField> it = fields.iterator(); it.hasNext();) {
                PricingField field = it.next();
                if (field.getName().equals(errorColumn)) it.remove();
                if (field.getName().equals(retryColumn)) it.remove();
                if (field.getName().equals(timestampColumn)) it.remove();
            }

            for (PricingField field : fields) {
        	// the word 'end' or 'start' is a reserved word in many databases so it can't be used. 
        	// Change it to 'end_time'
        	String fieldName = field.getName();
        	if (fieldName.equals("end") || fieldName.equals("start")) {
        	    fieldName = fieldName + "_time";
        	}
                columnNames.add(escapedKeywordsColumnName(fieldName));
            }

            columnNames.add(errorColumn);
            columnNames.add(retryColumn);

            StringBuilder query = new StringBuilder("insert into ");
            query.append(getParameter(PARAM_TABLE_NAME.getName(), TABLE_NAME_DEFAULT));
            query.append("(");
            query.append(com.sapienter.jbilling.server.util.Util.join(columnNames, ", "));
            query.append(") values (");
            query.append(com.sapienter.jbilling.server.util.Util.join(Collections.nCopies(columnNames.size(), "?"), ", "));
            query.append(")");

            PreparedStatement preparedStatement = connection.prepareStatement(query.toString());

            int index = 1;
            for (PricingField field : fields) {                
                switch (field.getType()) {
                    case STRING:
                        preparedStatement.setString(index, field.getStrValue());
                        break;
                    case INTEGER:
                        preparedStatement.setInt(index, field.getIntValue());
                        break;
                    case DECIMAL:
                        preparedStatement.setDouble(index, field.getDoubleValue());
                        break;
                    case DATE:
                        if (field.getDateValue() != null) {
                            preparedStatement.setTimestamp(index, new Timestamp(field.getDateValue().getTime()));
                        } else {
                            preparedStatement.setNull(index, Types.TIMESTAMP);
                        }
                        break;
                    case BOOLEAN:
                        preparedStatement.setBoolean(index, field.getBooleanValue());
                        break;
                }
                index++;
            }
            // errors column
            preparedStatement.setString(index, com.sapienter.jbilling.server.util.Util.join(errors, " "));
            index++;
            // retry column
            preparedStatement.setBoolean(index, false);

            // save data
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            log.error("Saving errors to database failed", e);
            throw new TaskException(e);
        } catch (ClassNotFoundException e) {
            log.error("Saving errors to database failed, incorrect configuration", e);
            throw new TaskException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }


    protected Connection getConnection() throws SQLException, ClassNotFoundException, TaskException {
        String driver = getParameter(PARAM_DRIVER.getName(), DRIVER_DEFAULT);
        Object url = parameters.get(PARAM_DATABASE_URL.getName());
        if (url == null) {
            throw new TaskException("Error, expected mandatory parameter databae_url");
        }
        String username = getParameter(PARAM_DATABASE_USERNAME.getName(), DATABASE_USERNAME_DEFAULT);
        String password = getParameter(PARAM_DATABASE_PASSWORD.getName(), DATABASE_PASSWORD_DEFAULT);

        // create connection
        Class.forName(driver); // load driver
        return DriverManager.getConnection((String) url, username, password);
    }

    protected String escapedKeywordsColumnName(String columnName) {
        String escape =  isMySQL() ? "`" : "\""; // escape mysql column names with backtick
        return escape + columnName + escape;
    }

    /**
     * returns true if the driver is a MySQL database driver, false if not.
     * @return true if MySQL
     */
    private boolean isMySQL() {
        if (mysql == null)
            mysql = getParameter(PARAM_DRIVER.getName(), DRIVER_DEFAULT).contains("mysql");
        return mysql;
    } 
}
