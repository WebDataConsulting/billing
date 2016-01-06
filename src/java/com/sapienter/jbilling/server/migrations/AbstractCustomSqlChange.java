package com.sapienter.jbilling.server.migrations;

import java.sql.Connection;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.DeleteStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.UpdateStatement;

public abstract class AbstractCustomSqlChange implements CustomSqlChange {

    protected String     catalogName;
    protected String     schemaName;
    protected Connection connection;

    abstract public String getConfirmationMessage ();

    @Override
    public void setFileOpener (ResourceAccessor arg0) {
    }

    @Override
    public void setUp () throws SetupException {
    }

    @Override
    public ValidationErrors validate (Database database) {
        return null;
    }

    @Override
    public SqlStatement[] generateStatements (Database database) throws CustomChangeException {
        connection = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
        if (database instanceof liquibase.database.core.MySQLDatabase) {
            catalogName = ((liquibase.database.core.MySQLDatabase) database).getDefaultSchemaName();
        }
        if (database instanceof liquibase.database.core.PostgresDatabase) {
            schemaName = "public";
        }
        return doGenerateStatements(database);
    }

    abstract public SqlStatement[] doGenerateStatements (Database database) throws CustomChangeException;

    InsertStatement buildInsertStatement (String tableName) {
        return new InsertStatement(catalogName, schemaName, tableName);
    }

    DeleteStatement buildDeleteStatement (String tableName) {
        return new DeleteStatement(catalogName, schemaName, tableName);
    }

    UpdateStatement buildUpdateStatement (String tableName) {
        return new UpdateStatement(catalogName, schemaName, tableName);
    }
}
