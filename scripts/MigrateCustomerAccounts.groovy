import groovy.sql.Sql

/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

includeTargets << grailsScript("Init")
includeTargets << new File("${basedir}/scripts/Liquibase.groovy")

target(migrate: "The description of the script goes here!") {

    depends(parseArguments)
    def db = getDatabaseParameters(argsMap)

    def sql = Sql.newInstance(db.url, db.username, db.password, db.driver)
    def nextAccountTypeId = sql.firstRow("select max(id)+1 as id from account_type").id
    nextAccountTypeId = nextAccountTypeId ? nextAccountTypeId : 1

    def accountTypeTableId = sql.firstRow("select id from jbilling_table where name = 'account_type'").id
    def contactTypeTableId  = sql.firstRow("select id from jbilling_table where name = 'contact_type'").id
    def userTableId = sql.firstRow("select id from jbilling_table where name = 'base_user'").id

    println "next row id for account type table :" + nextAccountTypeId
    println "account type table id number: " + accountTypeTableId

    sql.eachRow("select * from entity") { company ->

        def companyId = company.id

        println 'Processing company with id:' + companyId

        def query = "select id from order_period " +
                "where unit_id = 1 " +
                    "and value = 1 " +
                    "and entity_id = ${companyId}"
        def orderPeriodId = sql.firstRow(query).id

        if (!orderPeriodId) {
            println 'No monthly order period for company: ' + companyId + '. Fallback to order period with ID 1.'
            orderPeriodId = 1
        }

        query = "insert into account_type " +
                "(id, optlock, entity_id, main_subscript_order_period_id, next_invoice_day_of_period) values " +
                "(${nextAccountTypeId}, 1, ${companyId}, ${orderPeriodId}, 1)"
        sql.execute(query)

        sql.eachRow("select * from language") { lang ->
            query = "insert into international_description " +
                    "(table_id, foreign_id, psudo_column, language_id, content) values " +
                    "(${accountTypeTableId}, ${nextAccountTypeId}, 'description', ${lang.id}, 'Personal')"
            sql.execute(query)
        }

        query = "update customer c set account_type_id=${nextAccountTypeId} where " +
                "exists (select * from customer c1, base_user u " +
                    "where u.entity_id=${companyId} " +
                    "and c1.user_id = u.id " +
                    "and c1.id = c.id)"

        result = sql.execute(query)


        query = "select id from contact_type where entity_id = " +
                "${companyId} order by id asc"

        def aitDisplayOrder = 1
        sql.eachRow(query) { contactType ->

            def aitId = generateAitForContactType(
                    sql, companyId, contactType.id,
                    nextAccountTypeId, contactTypeTableId,
                    aitDisplayOrder, 1 == aitDisplayOrder)

            //if AIT was successfuly generated for this contact type
            //then migrate the users from that contact type to the new AIT
            if(aitId){
                println "Successfully generate AIT(${aitId}) " +
                        "for CONTACT TYPE(${contactType.id})"
                migrateUsersForContactType(sql, contactType.id,
                        aitId, userTableId, companyId)
            } else {
                println "Failed to generate AIT " +
                        "for CONTACT TYPE(${contactType.id})"
            }

            aitDisplayOrder++
        }

        nextAccountTypeId++
    }

    updateSequence(sql, 'account_type')
    updateSequence(sql, 'meta_field_name')
    updateSequence(sql, 'meta_field_value')
}

def generateAitForContactType(def sql, def companyId, def contactTypeId,
                              def accountTypeId, def contactTypeTableId,
                              def aitDisplayOrder, def firstAit){

    def query = "select * from international_description" +
            " where table_id = ${contactTypeTableId} " +
            "   and foreign_id = ${contactTypeId} " +
            "   and psudo_column = 'description'"
    def aitName = sql.firstRow(query)?.content
    aitName = aitName ? aitName : "Contact"

    query = "insert into meta_field_group " +
            "(id, entity_id, display_order, optlock, entity_type, discriminator, name, account_type_id) values " +
            "(coalesce((select max(mfg.id)+1 from meta_field_group mfg), 1), ${companyId}, ${aitDisplayOrder}, 1, " +
            "'ACCOUNT_TYPE','ACCOUNT_TYPE', '${aitName}', ${accountTypeId})"
    sql.execute(query)

    query = "select max(id) as id from meta_field_group"
    def aitId = sql.firstRow(query).id
	
    //only the first AIT will have mandatory email address
    insertMetaField(sql, 'contact.email', 'STRING', false, firstAit,
                    1, companyId, false, 'EMAIL',
            "_this.value ==~ ''[_A-Za-z0-9-]+(\\\\\\\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\\\\\\\.[A-Za-z0-9]+)*(\\\\\\\\.[A-Za-z]{2,})''", aitId)

    insertValidationError(sql, 1, 'Email Not Valid')

    insertMetaField(sql, 'contact.organization', 'STRING', false, false,
                    2, companyId, false, 'ORGANIZATION', null, aitId)
    insertMetaField(sql, 'contact.address1', 'STRING', false, false,
                    3, companyId, false, 'ADDRESS1', null, aitId)
    insertMetaField(sql, 'contact.address2', 'STRING', false, false,
                    4, companyId, false, 'ADDRESS2', null, aitId)
    insertMetaField(sql, 'contact.city', 'STRING', false, false,
                    5, companyId, false, 'CITY', null, aitId)
    insertMetaField(sql, 'contact.state.province', 'STRING', false, false,
                    6, companyId, false, 'STATE_PROVINCE', null, aitId)
    insertMetaField(sql, 'contact.postal.code', 'STRING', false, false,
                    7, companyId, false, 'POSTAL_CODE', null, aitId)
    insertMetaField(sql, 'contact.country.code', 'STRING', false, false,
                    8, companyId, false, 'COUNTRY_CODE', null, aitId)

    insertMetaField(sql, 'contact.first.name', 'STRING', false, false,
                    9, companyId, false, 'FIRST_NAME', null, aitId)
    insertMetaField(sql, 'contact.last.name', 'STRING', false, false,
                    10, companyId, false, 'LAST_NAME', null, aitId)
    insertMetaField(sql, 'contact.initial', 'STRING', false, false,
                    11, companyId, false, 'INITIAL', null, aitId)
    insertMetaField(sql, 'contact.title', 'STRING', false, false,
                    12, companyId, false, 'TITLE', null, aitId)

    insertMetaField(sql, 'contact.phone.country.code', 'INTEGER', false, false,
                    13, companyId, false, 'PHONE_COUNTRY_CODE', null, aitId)
    insertMetaField(sql, 'contact.phone.area.code', 'INTEGER', false, false,
                    14, companyId, false, 'PHONE_AREA_CODE', null, aitId)
    insertMetaField(sql, 'contact.phone.number', 'STRING', false, false,
                    15, companyId, false, 'PHONE_NUMBER', null, aitId)

    insertMetaField(sql, 'contact.fax.country.code', 'INTEGER', false, false,
                    16, companyId, false, 'FAX_COUNTRY_CODE', null, aitId)
    insertMetaField(sql, 'contact.fax.area.code', 'INTEGER', false, false,
                    17, companyId, false, 'FAX_AREA_CODE', null, aitId)
    insertMetaField(sql, 'contact.fax.number', 'STRING', false, false,
                    18, companyId, false, 'FAX_NUMBER', null, aitId)

    return aitId
}

def insertMetaField(def sql, def name, def data_type,
                    def disabled, def mandatory,
                    def display_order, def company_id,
                    def primary, def field_usage,
                    def validation_rule, def aitId){


    //insert the meta field
    def query = "insert into meta_field_name " +
            " (id, name, entity_type, data_type, is_disabled, is_mandatory," +
            " display_order, optlock, entity_id, is_primary, validation_rule) values " +
            " ( coalesce((select max(mfn.id)+1 from meta_field_name mfn), 1), " +
            " '${name}', 'ACCOUNT_TYPE', '${data_type}', ${disabled}, ${mandatory}, " +
            " ${display_order}, 1, ${company_id}, ${primary}, '${validation_rule}')"

    sql.execute(query)

    //insert the meta field type
    query = "insert into metafield_type_map (metafield_id, field_usage) values" +
            " ((select max(mfn.id) from meta_field_name mfn), '${field_usage}')"
    sql.execute(query)

    //connect the meta field with the AIT
    query = "insert into metafield_group_meta_field_map " +
            " (metafield_group_id, meta_field_value_id) values" +
            " (${aitId}, (select max(mfn.id) from meta_field_name mfn))"
    sql.execute(query)
}

def insertValidationError(def sql, def languageId, def content) {
    def query = "insert into international_description " +
            " (table_id, foreign_id, psudo_column, language_id, content) values" +
            " ( (select jt.id from jbilling_table jt where name = 'meta_field_name')," +
            " (select max(mfn.id) from meta_field_name mfn), " +
            " 'errorMessage', ${languageId}, '${content}')"
    sql.execute(query)
}

def migrateUsersForContactType(
        def sql, def contactTypeId, def aitId,
        def userTableId, def companyId){

    def fields = [
            'organization'  : 'string',
            'address1'      : 'string',
            'address2'      : 'string',
            'city'          : 'string',
            'state.province': 'string',
            'postal.code'   : 'string',
            'country.code'  : 'string',

            'first.name'    : 'string',
            'last.name'     : 'string',
            'initial'       : 'string',
            'title'         : 'string',

            'phone.country.code' : 'integer',
            'phone.area.code'    : 'integer',
            'phone.number'       : 'string',

            'fax.country.code' : 'integer',
            'fax.area.code'    : 'integer',
            'fax.number'       : 'string',

            'email'         : 'string'
    ]

    def query = "select bu.id as user_id, c.id as customer_id from " +
            " base_user bu, customer c where c.user_id = bu.id"

    sql.eachRow(query) { it ->
        def userId = it.user_id as Integer
        def customerId = it.customer_id as Integer

        def contactQuery = "select " +
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
        "   and cm.type_id = ${contactTypeId} " +
        "   and cm.foreign_id = ${userId} " +
        "   and cm.table_id = ${userTableId}";

        def connectQuery = "insert into customer_meta_field_map " +
                " (customer_id, meta_field_value_id) values " +
                " (${customerId}, (select max(mfv.id) from meta_field_value mfv))"

        sql.eachRow(contactQuery) { contactResults ->
            def contactMapId = contactResults.getProperty("contact.map.id")
            def contactId    = contactResults.getProperty("contact.id")

            fields.each { key, value ->
                def fieldName = "contact.${key}"
                def fieldValue = contactResults.getProperty(key)

                if(fieldValue){
                    def metaFieldId = getMetaFieldId(sql, fieldName, aitId, companyId)

                    if(metaFieldId){
                        def insertQuery = "insert into meta_field_value " +
                                " (id, meta_field_name_id, dtype, ${value}_value) values " +
                                " (coalesce((select max(mfv.id)+1 from meta_field_value mfv), 1)," +
                                " ${metaFieldId}, '${value}', ${value == 'integer' ? fieldValue : '\'' + fieldValue + '\''})"

                        sql.execute(insertQuery)
                        sql.execute(connectQuery)
                    }
                }
            }

            sql.execute("delete from contact_map where id = ${contactMapId}")
            sql.execute("delete from contact where id = ${contactId}")

            //DMLs
            sql.execute("alter table contact_map alter column type_id drop not null")
        }
    }
}

def getMetaFieldId(def sql, def fieldName, def aitGroupId, def companyId){
    def query = "select mfn.id from meta_field_name mfn" +
            " where name = '${fieldName}' " +
            "   and entity_id = ${companyId}" +
            "   and exists (select * from metafield_group_meta_field_map mgmfm " +
            "       where mgmfm.metafield_group_id = ${aitGroupId}" +
            "           and mgmfm.meta_field_value_id = mfn.id)"
    return sql.firstRow(query)?.id
}

def updateSequence(def sql, def tableName){
    def query = "update jbilling_seqs set next_id = " +
            " (select max(id)+1 from ${tableName})" +
            " where name = '${tableName}'"
    sql.execute(query)
}

setDefaultTarget(migrate)
