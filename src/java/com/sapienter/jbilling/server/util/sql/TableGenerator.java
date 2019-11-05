/*
 * jBilling - The Enterprise Open Source Billing System
 * Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde
 * 
 * This file is part of jbilling.
 * 
 * jbilling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jbilling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */

package com.sapienter.jbilling.server.util.sql;

;

import com.sapienter.jbilling.common.FormatLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple table generator that takes a list of columns and produces SQL to create
 * and manipulate data in a database table.
 *
 * @author Brian Cowdery
 * @since 15-Feb-2012
 */
public class TableGenerator {
    private static final FormatLogger LOG = new FormatLogger(TableGenerator.class);


    /**
     * Columns of the table schema.
     */
    public static class Column {
        private final String name;
        private final String dataType;
        private final boolean nullable;
        private final boolean primaryKey;

        public Column(String name, String dataType, boolean nullable) {
            this.name = name;
            this.dataType = dataType;
            this.nullable = nullable;
            this.primaryKey = false;
        }

        public Column(String name, String dataType, boolean nullable, boolean primaryKey) {
            this.name = name;
            this.dataType = dataType;
            this.nullable = nullable;
            this.primaryKey = primaryKey;
        }

        public String getName() {
            return name;
        }

        public String getDataType() {
            return dataType;
        }

        public boolean isNullable() {
            return nullable;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        @Override
        public String toString() {
            StringBuilder sql = new StringBuilder();
            sql.append(name)
               .append(' ')
               .append(dataType);

            return sql.toString();
        }
    }



    private List<Column> columns = new ArrayList<Column>();
    private String tableName;

    public TableGenerator() {
    }

    public TableGenerator(String tableName, List<Column> columns) {
        this.tableName = tableName.replaceAll("'", "''");
        this.columns = new ArrayList<Column>(columns); // safety first kids!
    }

    public void addColumn(Column column) {
        this.columns.add(column);
    }

    public void addColumns(List<Column> columns) {
        this.columns.addAll(columns);
    }

    public List<Column> getColumns() {
        return columns;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName.replaceAll("'", "''");
    }


    /**
     * Produces the DDL statements to create a table with the set columns.
     *
     * @return table creation DDL statement
     */
    public String buildCreateTableSQL() {
        StringBuilder ddl = new StringBuilder();
        ddl.append("create table ").append(tableName).append(" (");

        StringBuilder pk = new StringBuilder();
        pk.append("primary key (");

        int primaryKeys = 0;

        for (Iterator<Column> it = columns.iterator(); it.hasNext();) {
            Column column = it.next();
            ddl.append(column);

            if (!column.isNullable())
                ddl.append(" not null");

            if (column.isPrimaryKey()) {
                if (primaryKeys > 0)
                    pk.append(", ");

                pk.append(column.name);
                primaryKeys++;
            }

            if (it.hasNext())
                ddl.append(", ");
        }

        if (primaryKeys > 0) {
            pk.append(')');
            ddl.append(", ").append(pk);
        }

        ddl.append(");");

        LOG.debug("Generated create table SQL [%s]", ddl);
        return ddl.toString();
    }

    /**
     * Produces the DDL statement to drop a table from the schema.
     *
     * @return drop table DDL statement
     */
    public String buildDropTableSQL() {
        String drop = "drop table if exists " + tableName + ";";
        LOG.debug("Generated drop table SQL [%s]", drop);

        return drop;
    }

    /**
     * Produces the DDL statement to rename a a table;.
     *
     * @param newTableName new table name
     * @return alter table DDL statement
     */
    public String buildRenameTableSQL(String newTableName) {
        String alter = "alter table " + tableName + " rename to " + newTableName + ";";
        LOG.debug("Generated rename table SQL [%s]", alter);

        return alter;
    }

    /**
     * Produces a prepared statement SQL template for the set columns.
     *
     * Example:
     * <code>
     *     insert into table_name (col1, col2) values (?, ?);
     * </code>
     *
     * @return prepared statement SQL template
     */
    public String buildInsertPreparedStatementSQL() {
        StringBuilder insert = new StringBuilder();
        insert.append("insert into ").append(tableName).append(" (");

        StringBuilder values = new StringBuilder();
        values.append("values (");

        for (Iterator<Column> it = columns.iterator(); it.hasNext();) {
            Column column = it.next();

            insert.append(column.getName());
            values.append('?');

            if (it.hasNext()) {
                insert.append(", ");
                values.append(", ");
            }
        }

        insert.append(") ");
        values.append(");");

        insert.append(values);

        LOG.debug("Generated insert statement [%s]", insert);
        return insert.toString();
    }

}
