package com.sapienter.jbilling.server.migrations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.statement.SqlStatement;

public class MigrateSharedEntities extends AbstractCustomSqlChange {

    @Override
    public String getConfirmationMessage() {
        return "Migrated all item types and items.";
    }

    @Override
    public SqlStatement[] doGenerateStatements(Database database)
            throws CustomChangeException {
        List<SqlStatement> statements = new LinkedList<SqlStatement>();

        //Item
        try {
            String itemQuery = "select id as \"id\", entity_id as \"entity_id\" from item where entity_id is not null order by id asc";
            PreparedStatement items = connection.prepareStatement(itemQuery);
            ResultSet rs = items.executeQuery();
            while (rs.next()) {
                Integer itemId = rs.getInt("id");
                Integer entityId = rs.getInt("entity_id");
                statements.addAll(generateMigrateMapAndPrice(itemId, entityId));
            }
        } catch (SQLException e) {
            System.out.println("SQL Exception: " + e.getMessage());
            throw new CustomChangeException("SQL Exception", e);
        }

        //Item Types
        try {
            String itemTypeQuery = "select id as \"id\", entity_id as \"entity_id\" from item_type where entity_id is not null order by id asc";
            PreparedStatement itemTypes = connection.prepareStatement(itemTypeQuery);
            ResultSet rs = itemTypes.executeQuery();
            while (rs.next()) {
                Integer itemTypeId = rs.getInt("id");
                Integer entityId = rs.getInt("entity_id");
                statements.addAll(generateMigrateItemTypes(itemTypeId, entityId));
            }
        } catch (SQLException e) {
            System.out.println("SQL Exception: " + e.getMessage());
            throw new CustomChangeException("SQL Exception", e);
        }

        SqlStatement[] sqls = statements.toArray(new SqlStatement[statements.size()]);
        System.out.println(sqls);
        return sqls;
    }

    private List<SqlStatement> generateMigrateItemTypes(Integer itemTypeId, Integer entityId) {

        List<SqlStatement> statements = new LinkedList<SqlStatement>();

        statements.add(buildInsertStatement("item_type_entity_map")
                .addColumnValue("item_type_id", itemTypeId)
                .addColumnValue("entity_id", entityId));

        return statements;
    }

    private List<SqlStatement> generateMigrateMapAndPrice(Integer itemId, Integer entityId) {

        List<SqlStatement> statements = new LinkedList<SqlStatement>();

        statements.add(buildInsertStatement("item_entity_map")
                .addColumnValue("item_id", itemId)
                .addColumnValue("entity_id", entityId));

        return statements;
    }
}
