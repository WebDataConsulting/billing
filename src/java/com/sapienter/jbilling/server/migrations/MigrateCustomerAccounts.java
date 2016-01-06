package com.sapienter.jbilling.server.migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.RawSqlStatement;

import com.sapienter.jbilling.server.metafields.validation.ScriptValidationRuleModel;

/**
 * @author Vladimir Carevski
 */
public class MigrateCustomerAccounts extends AbstractCustomSqlChange {

    private Integer userTableId = null;
    private Integer contactTypeTableId = null;
    private Integer accountTypeTableId = null;
    private Integer metaFieldTableId = null;

    private Integer nextAccoutTypeId = null;
    private Integer nextAitId = null;

    private String emailValidationRule = null;

    private enum DataType {
        STRING,
        INTEGER
    }

    public String getConfirmationMessage() {
        return "Customer Accounts Migrated";
    }

    @Override
    public SqlStatement[] doGenerateStatements(Database database) throws CustomChangeException {
        List<SqlStatement> statements = new LinkedList<SqlStatement>();
        try {
            //check if contact type table exist if the table
            //does not exist that a migration has already been done
            try {
                connection.prepareStatement(
                        "select count(*) from contact_type")
                        .executeQuery().close();
            } catch (SQLException ex){
                System.out.println("Customer account migration will not be performed");
                return new SqlStatement[0];
            }


            //global init table ids
            userTableId = getTableId(connection, "base_user");
            contactTypeTableId = getTableId(connection, "account_type");
            accountTypeTableId = getTableId(connection, "account_type");
            metaFieldTableId = getTableId(connection, "meta_field_name");

            //global init next ids
            nextAccoutTypeId = getNextId(connection, "account_type");
            nextAitId = getNextId(connection, "meta_field_group");

            initEmailValidationRule();

            //loop through all the companies in the
            //system and do a migration to all of them
            String companiesQuery = "select id as \"id\" from entity order by id asc";
            PreparedStatement companiesStatement = connection.prepareStatement(companiesQuery);
            ResultSet companies = companiesStatement.executeQuery();
            while (companies.next()) {
                Integer companyId = companies.getInt("id");
                System.out.println("Migrating Company ID: " + companyId);
                statements.addAll(migrateCompany(connection, companyId));
                nextAccoutTypeId++;
            }
            companies.close();
        } catch (SQLException e) {
            System.out.println("SQL Exception");
            throw new CustomChangeException("SQL Exception", e);
        }
        System.out.println("Number of statements: " + statements.size());

        //generate update sequence statements
        statements.add(generateUpdateSequence("account_type"));
        statements.add(generateUpdateSequence("meta_field_name"));
        statements.add(generateUpdateSequence("meta_field_value"));
        statements.add(generateUpdateSequence("validation_rule"));

        //convert to array and return
        SqlStatement[] stmts = new SqlStatement[statements.size()];
        for (int i = 0; i < statements.size(); i++) {
            stmts[i] = statements.get(i);
        }
        return stmts;
    }

    private List<SqlStatement> migrateCompany(Connection connection, Integer companyId)
            throws SQLException {
        List<SqlStatement> statements = new LinkedList<SqlStatement>();

        List<SqlStatement> companyStatements = new LinkedList<SqlStatement>();
        companyStatements.addAll(generateAccountTypeStatements(connection, companyId, nextAccoutTypeId));
        companyStatements.add(generateUpdateCustomersStatement(companyId, nextAccoutTypeId));

        String contactTypeQuery = "select id from contact_type where entity_id = ? order by id asc";
        PreparedStatement cTypeStatement = connection.prepareStatement(contactTypeQuery);
        cTypeStatement.setInt(1, companyId);
        ResultSet type = cTypeStatement.executeQuery();
        int displayOrder = 1;

        //for each contact type in the company generate
        //ait with default meta fields defined and migrate
        //users contact information to those new meta fields
        while(type.next()){
            Integer contactTypeId = type.getInt(1);

            if(existCustomerUsingContactType(connection, contactTypeId, companyId)){
                statements.addAll(generateAitStatements(
                        connection, companyId, nextAccoutTypeId, nextAitId,
                        contactTypeId, displayOrder, 1 == displayOrder));

                statements.addAll(generateUserMigrationStatements(
                        connection, companyId, nextAitId, contactTypeId
                ));

                nextAitId++;
                displayOrder++;
            }
        }

        //means there are no migrated customers for this
        //company so do not created anything for this company
        if(statements.isEmpty()){
            return statements;
        } else {
            companyStatements.addAll(statements);
            return companyStatements;
        }
    }

    private boolean existCustomerUsingContactType(
            Connection connection, Integer cTypeId, Integer companyId)
    throws SQLException{
        String query = "select count(*) from " +
                "   base_user bu, customer c, contact co, contact_map cm" +
                "   where c.user_id = bu.id " +
                "       and co.user_id = bu.id " +
                "       and cm.contact_id = co.id " +
                "       and bu.entity_id = ?" +
                "       and cm.type_id = ?";

        PreparedStatement count = connection.prepareStatement(query);
        count.setInt(1, companyId);
        count.setInt(2, cTypeId);
        ResultSet result = count.executeQuery();
        return result.next() ? 0 != result.getInt(1) : false;
    }

    private List<SqlStatement> generateAccountTypeStatements(
            Connection connection, Integer companyId, Integer accountTypeId) throws SQLException {

        List<SqlStatement> statements = new LinkedList<SqlStatement>();

        Integer orderPeriodId = getCompanyMonthlyPeriod(connection, companyId);
        if(null == orderPeriodId) orderPeriodId = Integer.valueOf(1);

        statements.add(buildInsertStatement("account_type")
                .addColumnValue("id", accountTypeId)
                .addColumnValue("optlock", Integer.valueOf(1))
                .addColumnValue("currency_id", Integer.valueOf(1))
                .addColumnValue("entity_id", companyId)
                .addColumnValue("main_subscript_order_period_id", orderPeriodId)
                .addColumnValue("next_invoice_day_of_period", Integer.valueOf(1)));

        String langQuery = "select id from language";
        PreparedStatement langStatement = connection.prepareStatement(langQuery);
        ResultSet language = langStatement.executeQuery();
        while(language.next()){
            Integer langId = language.getInt(1);
            statements.add(buildInsertStatement("international_description")
                    .addColumnValue("table_id", accountTypeTableId)
                    .addColumnValue("foreign_id", accountTypeId)
                    .addColumnValue("psudo_column", "description")
                    .addColumnValue("language_id", langId)
                    .addColumnValue("content", "Basic"));
        }
        language.close();
        return statements;
    }

    /**
     * update all the customers from this company
     * to belong to the newly created account type
     *
     * @param companyId
     * @return
     */
    private SqlStatement generateUpdateCustomersStatement(Integer companyId, Integer accountTypeId){
        String updateCustomerQuery =
                "update customer c set account_type_id = " + accountTypeId.toString() + " where " +
                        " c.id in (select cid from (select c1.id as cid from customer c1, base_user u " +
                        " where u.entity_id = " + companyId.toString() +
                        " and c1.user_id = u.id ) as t) ";
        return new RawSqlStatement(updateCustomerQuery);
    }

    private List<SqlStatement> generateAitStatements(
            Connection connection, Integer companyId, Integer accountTypeId,
            Integer aitId, Integer contactTypeId, Integer displayOrder,
            boolean isFirst)
            throws SQLException{

        List<SqlStatement> statements = new LinkedList<SqlStatement>();

        String desc = getContactTypeDesc(
                connection, contactTypeId);

        statements.add(generateAitStatement(
                companyId, displayOrder, accountTypeId, desc));

        statements.addAll(generateMetaFieldStatements(
                companyId, aitId, isFirst));

        return statements;
    }

    private SqlStatement generateAitStatement(
            Integer companyId, Integer displayOrder,
            Integer accountTypeId, String description){

        return buildInsertStatement("meta_field_group")
                .addColumnValue("id", new StringBuffer("(coalesce((select max(mfg.id)+1 from meta_field_group mfg), 1))"))
                .addColumnValue("entity_id", companyId)
                .addColumnValue("display_order", displayOrder)
                .addColumnValue("optlock", Integer.valueOf(1))
                .addColumnValue("entity_type", "ACCOUNT_TYPE")
                .addColumnValue("discriminator", "ACCOUNT_TYPE")
                .addColumnValue("name", description)
                .addColumnValue("account_type_id", accountTypeId);
    }

    private List<SqlStatement> generateMetaFieldStatements(
            Integer companyId, Integer aitId, boolean firstAit) {
        List<SqlStatement> statements = new LinkedList<SqlStatement>();

        statements.addAll(generateMetaFieldInserts("contact.email", "STRING", false, firstAit,
                1, companyId, false, "EMAIL", emailValidationRule, aitId));

        statements.add(generateValidationError(1, "Email not Valid"));

        statements.addAll(generateMetaFieldInserts("contact.organization", "STRING", false, false,
                2, companyId, false, "ORGANIZATION", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.address1", "STRING", false, false,
                3, companyId, false, "ADDRESS1", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.address2", "STRING", false, false,
                4, companyId, false, "ADDRESS2", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.city", "STRING", false, false,
                5, companyId, false, "CITY", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.state.province", "STRING", false, false,
                6, companyId, false, "STATE_PROVINCE", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.postal.code", "STRING", false, false,
                7, companyId, false, "POSTAL_CODE", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.country.code", "STRING", false, false,
                8, companyId, false, "COUNTRY_CODE", null, aitId));

        statements.addAll(generateMetaFieldInserts("contact.first.name", "STRING", false, false,
                9, companyId, false, "FIRST_NAME", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.last.name", "STRING", false, false,
                10, companyId, false, "LAST_NAME", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.initial", "STRING", false, false,
                11, companyId, false, "INITIAL", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.title", "STRING", false, false,
                12, companyId, false, "TITLE", null, aitId));

        statements.addAll(generateMetaFieldInserts("contact.phone.country.code", "INTEGER", false, false,
                13, companyId, false, "PHONE_COUNTRY_CODE", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.phone.area.code", "INTEGER", false, false,
                14, companyId, false, "PHONE_AREA_CODE", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.phone.number", "STRING", false, false,
                15, companyId, false, "PHONE_NUMBER", null, aitId));

        statements.addAll(generateMetaFieldInserts("contact.fax.country.code", "INTEGER", false, false,
                16, companyId, false, "FAX_COUNTRY_CODE", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.fax.area.code", "INTEGER", false, false,
                17, companyId, false, "FAX_AREA_CODE", null, aitId));
        statements.addAll(generateMetaFieldInserts("contact.fax.number", "STRING", false, false,
                18, companyId, false, "FAX_NUMBER", null, aitId));

        return statements;
    }

    private List<SqlStatement> generateMetaFieldInserts(
            String name, String dataType, boolean disabled,
            boolean mandatory, Integer displayOrder, Integer companyId,
            boolean primary,  String usage, String validation,
            Integer aitId) {

        List<SqlStatement> inserts = new LinkedList<SqlStatement>();

        if (validation != null) {
            inserts.add(buildInsertStatement("validation_rule")
                    .addColumnValue("id", new StringBuffer("(coalesce((select max(vl.id)+1 from validation_rule vl), 1))"))
                    .addColumnValue("rule_type", "SCRIPT")
                    .addColumnValue("enabled", true)
                    .addColumnValue("optlock", 0));

            inserts.add(buildInsertStatement("validation_rule_attributes")
                    .addColumnValue("validation_rule_id", new StringBuffer("(select max(vl.id) from validation_rule vl)"))
                    .addColumnValue("attribute_name", ScriptValidationRuleModel.VALIDATION_SCRIPT_FIELD)
                    .addColumnValue("attribute_value", validation));
        }

        InsertStatement insertMetaField = buildInsertStatement("meta_field_name")
                .addColumnValue("id", new StringBuffer("(coalesce((select max(mfn.id)+1 from meta_field_name mfn), 1))"))
                .addColumnValue("name", name)
                .addColumnValue("entity_type", "ACCOUNT_TYPE")
                .addColumnValue("data_type", dataType)
                .addColumnValue("is_disabled", disabled)
                .addColumnValue("is_mandatory", mandatory)
                .addColumnValue("display_order", displayOrder)
                .addColumnValue("optlock", Integer.valueOf(1))
                .addColumnValue("entity_id", companyId)
                .addColumnValue("is_primary", primary);
        if (validation != null) {
            insertMetaField.addColumnValue("validation_rule_id", new StringBuffer("(select max(vl.id) from validation_rule vl)"));
        }
        inserts.add(insertMetaField);

        inserts.add(buildInsertStatement("metafield_type_map")
                .addColumnValue("metafield_id", new StringBuffer("(select max(mfn.id) from meta_field_name mfn)"))
                .addColumnValue("field_usage", usage));

        inserts.add(buildInsertStatement("metafield_group_meta_field_map")
                .addColumnValue("metafield_group_id", aitId)
                .addColumnValue("meta_field_value_id", new StringBuffer("(select max(mfn.id) from meta_field_name mfn)")));
        return inserts;

    }

    private SqlStatement generateValidationError(Integer langId, String content){
        return buildInsertStatement("international_description")
                .addColumnValue("table_id", metaFieldTableId)
                .addColumnValue("foreign_id", new StringBuffer("(select max(mfn.id) from meta_field_name mfn)"))
                .addColumnValue("psudo_column", "errorMessage")
                .addColumnValue("language_id", langId)
                .addColumnValue("content", content);
    }

    private List<SqlStatement> generateUserMigrationStatements(
            Connection connection, Integer companyId,
            Integer aitId, Integer contactTypeId)
            throws SQLException{
        List<SqlStatement> statements = new LinkedList<SqlStatement>();

        PreparedStatement statement = connection.prepareStatement(buildUserQuery());
        statement.setInt(1, companyId);
        ResultSet users = statement.executeQuery();
        while (users.next()) {

            int userId = users.getInt("user_id");
            int customerId = users.getInt("customer_id");

            PreparedStatement contactQuery = connection.prepareStatement(getContactQuery());
            contactQuery.setInt(1, contactTypeId);
            contactQuery.setInt(2, userId);
            contactQuery.setInt(3, userTableId);

            ResultSet contact = contactQuery.executeQuery();
            while (contact.next()) {
                Integer contactId = contact.getInt("contact.id");
                Integer contactMapId = contact.getInt("contact.map.id");

                statements.addAll(generateMetaFieldStatements(
                        "contact.organization", aitId, contact, customerId,
                        "organization", DataType.STRING, companyId));

                statements.addAll(generateMetaFieldStatements(
                        "contact.address1", aitId, contact, customerId,
                        "address1", DataType.STRING, companyId));

                statements.addAll(generateMetaFieldStatements(
                        "contact.address2", aitId, contact, customerId,
                        "address2", DataType.STRING, companyId));

                statements.addAll(generateMetaFieldStatements(
                        "contact.city", aitId, contact, customerId,
                        "city", DataType.STRING, companyId));

                statements.addAll(generateMetaFieldStatements(
                        "contact.state.province", aitId, contact, customerId,
                        "state.province", DataType.STRING, companyId));
                statements.addAll(generateMetaFieldStatements(
                        "contact.postal.code", aitId, contact, customerId,
                        "postal.code", DataType.STRING, companyId));
                statements.addAll(generateMetaFieldStatements(
                        "contact.country.code", aitId, contact, customerId,
                        "country.code", DataType.STRING, companyId));

                statements.addAll(generateMetaFieldStatements(
                        "contact.first.name", aitId, contact, customerId,
                        "first.name", DataType.STRING, companyId));
                statements.addAll(generateMetaFieldStatements(
                        "contact.last.name", aitId, contact, customerId,
                        "last.name", DataType.STRING, companyId));
                statements.addAll(generateMetaFieldStatements(
                        "contact.initial", aitId, contact, customerId,
                        "initial", DataType.STRING,companyId));
                statements.addAll(generateMetaFieldStatements(
                        "contact.title", aitId, contact, customerId,
                        "title", DataType.STRING, companyId));

                statements.addAll(generateMetaFieldStatements(
                        "contact.phone.country.code", aitId, contact, customerId,
                        "phone.country.code", DataType.INTEGER, companyId));
                statements.addAll(generateMetaFieldStatements(
                        "contact.phone.area.code", aitId, contact, customerId,
                        "phone.area.code", DataType.INTEGER, companyId));
                statements.addAll(generateMetaFieldStatements(
                        "contact.phone.number", aitId, contact, customerId,
                        "phone.number", DataType.STRING, companyId));

                statements.addAll(generateMetaFieldStatements(
                        "contact.fax.country.code", aitId, contact, customerId,
                        "fax.country.code", DataType.INTEGER, companyId));
                statements.addAll(generateMetaFieldStatements(
                        "contact.fax.area.code", aitId, contact, customerId,
                        "fax.area.code", DataType.INTEGER, companyId));
                statements.addAll(generateMetaFieldStatements(
                        "contact.fax.number", aitId, contact, customerId,
                        "fax.number", DataType.STRING, companyId));

                statements.addAll(generateMetaFieldStatements(
                        "contact.email", aitId, contact, customerId,
                        "email", DataType.STRING, companyId));

                statements.addAll(generateDeleteContactStatements(contactId, contactMapId));
            }
            contact.close();

        }
        users.close();
        return statements;
    }

    private List<SqlStatement> generateMetaFieldStatements(
            String fieldName, Integer aitId, ResultSet result, Integer customerId,
            String columnName, DataType type, Integer companyId)
            throws SQLException {

        Object value = type.equals(DataType.INTEGER) ?
                result.getInt(columnName) :
                result.getString(columnName);

        List<SqlStatement> statements = new LinkedList<SqlStatement>();


        //avoid creation of meta fields from NULL columns
        if ((type.equals(DataType.STRING) && null == value) ||
                (type.equals(DataType.INTEGER) &&
                        0 == ((Integer) value).compareTo(Integer.valueOf(0)))) {
            //value for this column was NULL, so no statements
            return statements;
        }

        //purge empty string values
        if (type.equals(DataType.STRING) && ((String)value).trim().isEmpty()){
            return statements;
        }

        statements.add(buildMFInsertStm(fieldName, aitId, companyId, value));
        statements.add(buildMFCustomerStm(customerId));

        return statements;
    }

    private InsertStatement buildMFInsertStm(String fieldName, Integer aitId, Integer companyId, Object value){
        if(null == value){
            throw new IllegalArgumentException("can not be null");
        }
        String type = value instanceof Integer ? "integer" : "string";
        //string buffer is used to trick LB not to quote the string
        return buildInsertStatement("meta_field_value")
                .addColumnValue("meta_field_name_id", getFieldIdQuery(fieldName, aitId, companyId))
                .addColumnValue("dtype", type)
                .addColumnValue(type + "_value", value)
                .addColumnValue("id", new StringBuffer("(select max(mv.id) + 1 from meta_field_value mv)"));
    }

    private List<SqlStatement> generateDeleteContactStatements(Integer contactId, Integer contactMapId)
            throws SQLException {
        List<SqlStatement> statements = new LinkedList<SqlStatement>();

        if(0 != contactMapId.compareTo(Integer.valueOf(0))){
            statements.add(buildDeleteStatement("contact_map")
                    .setWhere("id = ?")
                    .addWhereParameter(contactMapId));
        }

        if(0 != contactId.compareTo(Integer.valueOf(0))){
            statements.add(buildDeleteStatement("contact")
                    .setWhere("id = ?")
                    .addWhereParameter(contactId));
        }
        return statements;
    }

    private InsertStatement buildMFCustomerStm(Integer customerId){
        //string buffer is used to trick LB not to quote the string
        return buildInsertStatement("customer_meta_field_map")
                .addColumnValue("customer_id", customerId)
                .addColumnValue("meta_field_value_id", new StringBuffer("(select max(mv.id) from meta_field_value mv)"));
    }


    private StringBuffer getFieldIdQuery(String name, Integer aitId, Integer companyId){
        return new StringBuffer(
                "(select mfn.id from meta_field_name mfn" +
                        " where name = '"+name+"'" +
                        " and entity_id = " + companyId.toString() +
                        " and exists (" +
                        "       select * from metafield_group_meta_field_map mgmfm " +
                        "       where mgmfm.metafield_group_id = " + aitId.toString() +
                        "       and mgmfm.meta_field_value_id = mfn.id))");
    }

    private Integer getNextId(Connection connection, String tableName)
            throws SQLException {
        String max = "select max(id) from " + tableName;
        ResultSet resultSet = connection.prepareStatement(max).executeQuery();
        Integer nextId = resultSet.next() ? resultSet.getInt(1) + 1  : Integer.valueOf(1);
        resultSet.close();
        return nextId;
    }

    private Integer getCompanyMonthlyPeriod(Connection connection, Integer companyId)
            throws  SQLException{
        String query = "select id from order_period where unit_id = 1 and value = 1 and entity_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, companyId);
        ResultSet result = statement.executeQuery();
        Integer id = result.next() ? result.getInt(1) : null;
        result.close();
        return id;
    }

    private String getContactTypeDesc(Connection connection, Integer contactTypeId)
            throws SQLException {
        String query =
                "select content from international_description " +
                        " where table_id = ? " +
                        "   and foreign_id = ? " +
                        "   and psudo_column = 'description'";

        PreparedStatement cTypeStatement = connection.prepareStatement(query);
        cTypeStatement.setInt(1, contactTypeTableId);
        cTypeStatement.setInt(2, contactTypeId);
        ResultSet desc = cTypeStatement.executeQuery();
        String content = desc.next() ? desc.getString(1) : null;
        content = null != content ? content : "Contact";
        return content;
    }

    private Integer getTableId(Connection connection, String name) throws SQLException {
        String query = "select jt.id as \"table_id\" from jbilling_table jt where jt.name = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, name);
        ResultSet result = statement.executeQuery();
        Integer tableId = result.next() ? Integer.valueOf(result.getInt("table_id")) : null;
        result.close();
        return tableId;
    }

    private SqlStatement generateUpdateSequence(String name){
        return buildUpdateStatement("jbilling_seqs")
                .addNewColumnValue("next_id", new StringBuffer("coalesce((select max(t.id)+1 FROM " + name + " t),1)"))
                .setWhereClause("name = ?")
                .addWhereParameter(name);
    }

    private String buildUserQuery(){
        return "select bu.id as \"user_id\", c.id as \"customer_id\" from " +
                " base_user bu, customer c " +
                " where c.user_id = bu.id" +
                " and bu.entity_id = ?";
    }

    private String getContactQuery(){
        return "select " +
                "   c.id as \"contact.id\", " +
                "   cm.id as \"contact.map.id\", " +
                "   c.organization_name as \"organization\", " +
                "   c.street_addres1 as \"address1\", " +
                "   c.street_addres2 as \"address2\", " +
                "   c.city as \"city\", " +
                "   c.state_province as \"state.province\", " +
                "   c.postal_code as \"postal.code\", " +
                "   c.country_code as \"country.code\", " +
                "   c.last_name as \"last.name\", " +
                "   c.first_name as \"first.name\", " +
                "   c.person_initial as \"initial\", " +
                "   c.person_title as \"title\", " +
                "   c.phone_country_code as \"phone.country.code\", " +
                "   c.phone_area_code as \"phone.area.code\", " +
                "   c.phone_phone_number as \"phone.number\", " +
                "   c.fax_country_code as \"fax.country.code\", " +
                "   c.fax_area_code as \"fax.area.code\", " +
                "   c.fax_phone_number as \"fax.number\", " +
                "   c.email as \"email\" " +
                "from contact_map cm, contact c " +
                "   where cm.contact_id = c.id " +
                "   and cm.type_id = ? " +
                "   and cm.foreign_id = ? " +
                "   and cm.table_id = ? ";
    }

    /**
     * Nasty hack to deal with escaping of backslash on different
     * platforms. For example on Postgres 8.4 we need extra backslahes
     * just to input one backslash, on version 9.1 we do not need the
     * extra backslashes.
     */
    private void initEmailValidationRule() {
//      for postgres 9.1
        emailValidationRule = "_this ==~ /[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})/";
//      for postgres 8.4
//      emailValidationRule = "_this.value ==~ /[_A-Za-z0-9-]+(\\\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\\\.[A-Za-z0-9]+)*(\\\\.[A-Za-z]{2,})/";
    }

}