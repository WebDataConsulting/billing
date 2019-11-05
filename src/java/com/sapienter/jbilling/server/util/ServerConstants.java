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

package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.CommonConstants;

/**
 * @author emilc
 *
 */
public final class ServerConstants extends CommonConstants {
	
	/*
	 * 
	 * Spring Batch CommonConstants
	 * 
	 */
	public static final String JOBCONTEXT_TOTAL_USERS_FAILED_KEY = "usersFailed";
	public static final String JOBCONTEXT_BILLING_PROCESS_ID_KEY = "billingProcessId";
	public static final String JOBCONTEXT_FAILED_USERS_LIST_KEY = "failedUsersList";
	public static final String JOBCONTEXT_TOTAL_INVOICES_KEY = "totalInvoices";	
	public static final String JOBCONTEXT_SUCCESSFULL_USERS_LIST_KEY = "successfulUsersList";
	public static final String JOBCONTEXT_PROCESS_USER_RESULT_KEY = "userInvoices";
    public static final String JOBCONTEXT_USERS_LIST_KEY = "userIds";
	
	public static final String BATCH_JOB_PARAM_ENTITY_ID = "entityId";
	public static final String BATCH_JOB_PARAM_BILLING_DATE = "billingDate";
	public static final String BATCH_JOB_PARAM_AGEING_DATE = "ageingDate";
	public static final String BATCH_JOB_PARAM_PERIOD_VALUE = "periodValue";
	public static final String BATCH_JOB_PARAM_PERIOD_TYPE = "periodType";
	public static final String BATCH_JOB_PARAM_REVIEW = "review";
	public static final String BATCH_JOB_PARAM_UNIQUE = "unique";
	
    /*
     * DATA BASE CONSTANTS
     * These values are in the database, should be initialized by the
     * InitDataBase program and remain static.
     */
    // the agreed maximum length for a varchar.
    public static final int MAX_VARCHAR_LENGTH = 1000;
    // this should be equal to hibernate.jdbc.batch_size
    public static final int HIBERNATE_BATCH_SIZE = 100;
    // tables
    public static final String TABLE_ITEM = "item";
    public static final String TABLE_PUCHASE_ORDER = "purchase_order";
    public static final String TABLE_ORDER_PROCESSING_RULE = "order_processing_rule";
    public static final String TABLE_ORDER_PERIOD = "order_period";
    public static final String TABLE_ORDER_LINE_TYPE = "order_line_type";
    public static final String TABLE_BILLING_PROCESS = "billing_process";
    public static final String TABLE_BILLING_PROCESS_RUN = "process_run";
    public static final String TABLE_BILLING_PROCESS_RUN_TOTAL = "process_run_total";
    public static final String TABLE_BILLING_PROCESS_RUN_TOTAL_PM = "process_run_total_pm";
    public static final String TABLE_BILLING_PROCESS_CONFIGURATION = "billing_process_configuration";
    public static final String TABLE_INVOICE = "invoice";
    public static final String TABLE_INVOICE_STATUS = "invoice_status";
    public static final String TABLE_INVOICE_LINE= "invoice_line";
    public static final String TABLE_EVENT_LOG = "event_log";
    public static final String TABLE_INTERNATIONAL_DESCRIPTION = "international_description";
    public static final String TABLE_LANGUAGE = "language";
    public static final String TABLE_ENTITY = "entity";
    public static final String TABLE_USER_TYPE = "user_type";
    public static final String TABLE_BASE_USER = "base_user";
    public static final String TABLE_CUSTOMER = "customer";
    public static final String TABLE_PERIOD_UNIT = "period_unit";
    public static final String TABLE_ORDER_BILLING_TYPE = "order_billing_type";
    public static final String TABLE_ORDER_STATUS = "order_status";
    public static final String TABLE_ORDER_LINE = "order_line";
    public static final String TABLE_PLUGGABLE_TASK_TYPE_CATEGORY = "pluggable_task_type_category";
    public static final String TABLE_PLUGGABLE_TASK_TYPE = "pluggable_task_type";
    public static final String TABLE_PLUGGABLE_TASK = "pluggable_task";
    public static final String TABLE_PLUGGABLE_TASK_PARAMETER = "pluggable_task_parameter";
    public static final String TABLE_CONTACT = "contact";
    public static final String TABLE_CONTACT_FIELD = "contact_field";
    public static final String TABLE_CONTACT_FIELD_TYPE = "contact_field_type";
    public static final String TABLE_CONTACT_TYPE = "contact_type";
    public static final String TABLE_CONTACT_MAP = "contact_map";
    public static final String TABLE_INVOICE_LINE_TYPE = "invoice_line_type";
    public static final String TABLE_PAYMENT = "payment";
    public static final String TABLE_PAYMENT_INFO_CHEQUE = "payment_info_cheque";
    public static final String TABLE_PAYMENT_RESULT = "payment_result";
    public static final String TABLE_PAYMENT_METHOD = "payment_method";
    public static final String TABLE_PAYMENT_INVOICE_MAP = "payment_invoice";
    public static final String TABLE_EVENT_LOG_MODULE = "event_log_module";
    public static final String TABLE_EVENT_LOG_MESSAGE = "event_log_message";
    public static final String TABLE_ORDER_PROCESS = "order_process";
    public static final String TABLE_PREFERENCE = "preference";
    public static final String TABLE_PREFERENCE_TYPE = "preference_type";
    public static final String TABLE_NOTIFICATION_MESSAGE = "notification_message";
    public static final String TABLE_NOTIFICATION_MESSAGE_SECTION = "notification_message_section";
    public static final String TABLE_NOTIFICATION_MESSAGE_TYPE = "notification_message_type";
    public static final String TABLE_NOTIFICATION_MESSAGE_LINE = "notification_message_line";
    public static final String TABLE_NOTIFICATION_MESSAGE_ARCHIVE = "notification_message_arch";
    public static final String TABLE_NOTIFICATION_MESSAGE_ARCHIVE_LINE = "notification_message_arch_line";
    public static final String TABLE_REPORT = "report";
    public static final String TABLE_REPORT_TYPE = "report_type";
    public static final String TABLE_ROLE= "role";
    public static final String TABLE_USER_ROLE_MAP= "user_role_map";
    public static final String TABLE_MENU_OPTION = "menu_option";
    public static final String TABLE_COUNTRY = "country";
    public static final String TABLE_PARTNER = "partner";
    public static final String TABLE_PARTNER_RANGE = "partner_range";
    public static final String TABLE_PARTNER_PAYOUT = "partner_payout";
    public static final String TABLE_USER_STATUS = "user_status";
    public static final String TABLE_USER_SUBSCRIBER_STATUS = "subscriber_status";
    public static final String TABLE_ITEM_TYPE = "item_type";
    public static final String TABLE_ITEM_USER_PRICE= "item_user_price";
    public static final String TABLE_PROMOTION= "promotion";
    public static final String TABLE_CREDIT_CARD= "credit_card";
    public static final String TABLE_USER_CREDIT_CARD_MAP= "user_credit_card_map";
    public static final String TABLE_PAYMENT_AUTHORIZATION="payment_authorization";
    public static final String TABLE_ENTITY_PAYMENT_METHOD_MAP = "entity_payment_method_map";
    public static final String TABLE_CURRENCY = "currency";
    public static final String TABLE_CURRENCY_ENTITY_MAP = "currency_entity_map";
    public static final String TABLE_CURRENCY_EXCHANGE= "currency_exchange";
    public static final String TABLE_ITEM_PRICE = "item_price";
    public static final String TABLE_AGEING_ENTITY_STEP = "ageing_entity_step";
    public static final String TABLE_INVOICE_DELIVERY_METHOD = "invoice_delivery_method";
    public static final String TABLE_ENTITY_DELIVERY_METHOD_MAP = "entity_delivery_method_map";
    public static final String TABLE_PAPER_INVOICE_BATCH = "paper_invoice_batch";
    public static final String TABLE_ACH = "ach";
    public static final String TABLE_LIST_ENTITY = "list_entity";
    public static final String TABLE_LIST_FIELD_ENTITY = "list_field_entity";
    public static final String TABLE_MEDIATION_CFG = "mediation_cfg";
    public static final String TABLE_MEDIATION_RECORD_STATUS = "mediation_record_status";
    public static final String TABLE_BLACKLIST = "blacklist";
    public static final String TABLE_GENERIC_STATUS_TYPE = "generic_status_type";
    public static final String TABLE_GENERIC_STATUS = "generic_status";
    public static final String TABLE_PROCESS_RUN_STATUS = "process_run_status";
    public static final String TABLE_NOTIFICATION_CATEGORY = "notification_category";
    public static final String TABLE_ASSET = "asset";
    public static final String TABLE_ASSET_STATUS = "asset_status";
    public static final String TABLE_ASSET_TRANSITION = "asset_transition";

    public static final String TABLE_ENUMERATION = "enumeration";
    public static final String TABLE_ENUMERATION_VALUES = "enumeration_values";
    public static final String TABLE_METAFIELD_GROUP = "meta_field_group";
    public static final String TABLE_METAFIELD= "meta_field_name";
    public static final String TABLE_VALIDATION_RULE= "validation_rule";
    
    public static final String TABLE_DISCOUNT = "discount";
    
    public static final String TABLE_ORDER_CHANGE_STATUS = "order_change_status";
    public static final String TABLE_ORDER_CHANGE = "order_change";
    
    public static final String TABLE_ORDER_CHANGE_PLAN_ITEM = "order_change_plan_item";

    public static final String TABLE_RATE_CARDS = "rate_card";

    public static final String TABLE_PAYMENT_METHOD_TYPE = "payment_method_type";

    // psudo column values from international description
    public static final String PSUDO_COLUMN_TITLE = "title";
    public static final String PSUDO_COLUMN_DESCRIPTION = "description";
    
    // order line types
    public static final int ORDER_LINE_TYPE_ITEM = 1;
    public static final int ORDER_LINE_TYPE_TAX = 2;
    public static final int ORDER_LINE_TYPE_PENALTY = 3;
    public static final int ORDER_LINE_TYPE_DISCOUNT = 4;
    public static final int ORDER_LINE_TYPE_SUBSCRIPTION = 5;
    
    // order periods. This are those NOT related with any single entity
    public static final Integer ORDER_PERIOD_ONCE = new Integer(1);
    
    public static final Integer ORDER_PERIOD_ALL_ORDERS = new Integer(5);

    // period unit types
    public static final Integer PERIOD_UNIT_MONTH = new Integer(1);
    public static final Integer PERIOD_UNIT_WEEK = new Integer(2);
    public static final Integer PERIOD_UNIT_DAY = new Integer(3);
    public static final Integer PERIOD_UNIT_YEAR= new Integer(4);
    public static final Integer PERIOD_UNIT_SEMI_MONTHLY= new Integer(5);
    
    // order billing types
    public static final Integer ORDER_BILLING_PRE_PAID = new Integer(1);
    public static final Integer ORDER_BILLING_POST_PAID = new Integer(2);
    
    // pluggable tasks categories
    public static final Integer PLUGGABLE_TASK_PROCESSING_ORDERS = new Integer(1);
    public static final Integer PLUGGABLE_TASK_ORDER_FILTER = new Integer(2);
    public static final Integer PLUGGABLE_TASK_INVOICE_FILTER = new Integer(3);
    public static final Integer PLUGGABLE_TASK_INVOICE_COMPOSITION = new Integer(4);
    public static final Integer PLUGGABLE_TASK_ORDER_PERIODS = new Integer(5);
    public static final Integer PLUGGABLE_TASK_PAYMENT = new Integer(6);
    public static final Integer PLUGGABLE_TASK_NOTIFICATION = new Integer(7);
    public static final Integer PLUGGABLE_TASK_PAYMENT_INFO = new Integer(8);
    public static final Integer PLUGGABLE_TASK_PENALTY = new Integer(9);
    public static final Integer PLUGGABLE_TASK_PROCESSOR_ALARM = new Integer(10);
    public static final Integer PLUGGABLE_TASK_SUBSCRIPTION_STATUS = new Integer(11);
    public static final Integer PLUGGABLE_TASK_ASYNC_PAYMENT_PARAMS = new Integer(12);
    public static final Integer PLUGGABLE_TASK_ITEM_MANAGER = new Integer(13);
    public static final Integer PLUGGABLE_TASK_ITEM_PRICING = new Integer(14);
    public static final Integer PLUGGABLE_TASK_MEDIATION_READER = Integer.valueOf(15);
    public static final Integer PLUGGABLE_TASK_MEDIATION_PROCESS = Integer.valueOf(16);
    public static final Integer PLUGGABLE_TASK_INTERNAL_EVENT = new Integer(17);
    public static final Integer PLUGGABLE_TASK_VALIDATE_PURCHASE = new Integer(19);
    public static final Integer PLUGGABLE_TASK_BILL_PROCESS_FILTER = new Integer(20);
    public static final Integer PLUGGABLE_TASK_MEDIATION_ERROR_HANDLER = Integer.valueOf(21);
    public static final Integer PLUGGABLE_TASK_SCHEDULED = new Integer(22);
    public static final Integer PLUGGABLE_TASK_RULES_GENERATOR = new Integer(23);
    public static final Integer PLUGGABLE_TASK_AGEING = new Integer(24);
    public static final Integer PLUGGABLE_TASK_PARTNER_COMMISSION = new Integer(25);
    public static final Integer PLUGGABLE_TASK_FILE_EXCHANGE = new Integer(26);

    // pluggable task types (belongs to a category)
    public static final Integer PLUGGABLE_TASK_T_PAPER_INVOICE = new Integer(12);
    
    // invoice line types
    public static final Integer INVOICE_LINE_TYPE_ITEM_RECURRING = new Integer(1);
    public static final Integer INVOICE_LINE_TYPE_TAX = new Integer(2);
    public static final Integer INVOICE_LINE_TYPE_DUE_INVOICE = new Integer(3);
    public static final Integer INVOICE_LINE_TYPE_PENALTY = new Integer(4);
    public static final Integer INVOICE_LINE_TYPE_SUB_ACCOUNT = new Integer(5);
    public static final Integer INVOICE_LINE_TYPE_ITEM_ONETIME = new Integer(6);

    // languages - when the project is a big company, we can do this right ! :p
    public static final Integer LANGUAGE_ENGLISH_ID = new Integer(1);
    public static final String LANGUAGE_ENGLISH_STR = "English";
    public static final Integer LANGUAGE_SPANISH_ID = new Integer(2);
    public static final String LANGUAGE_SPANISH_STR = "Spanish";    

    public static final Integer ORDER_PROCESS_ORIGIN_PROCESS = new Integer(1);
    public static final Integer ORDER_PROCESS_ORIGIN_MANUAL = new Integer(2);

    //Notification Preference Types
    public static final Integer PREFERENCE_TYPE_SELF_DELIVER_PAPER_INVOICES = new Integer(13);
    public static final Integer PREFERENCE_TYPE_INCLUDE_CUSTOMER_NOTES = new Integer(14);
    public static final Integer PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP = new Integer(15);
    public static final Integer PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP2 = new Integer(16);
    public static final Integer PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP3 = new Integer(17);
    public static final Integer PREFERENCE_TYPE_USE_INVOICE_REMINDERS = new Integer(21);
    public static final Integer PREFERENCE_TYPE_NO_OF_DAYS_INVOICE_GEN_1_REMINDER = new Integer(22);
    public static final Integer PREFERENCE_TYPE_NO_OF_DAYS_NEXT_REMINDER = new Integer(23);

    // notification message types
    public static final Integer NOTIFICATION_TYPE_INVOICE_EMAIL = 1;
    public static final Integer NOTIFICATION_TYPE_USER_REACTIVATED = 2;
    public static final Integer NOTIFICATION_TYPE_USER_OVERDUE = 3;
    public static final Integer NOTIFICATION_TYPE_USER_OVERDUE_2 = 4;
    public static final Integer NOTIFICATION_TYPE_USER_OVERDUE_3 = 5;
    public static final Integer NOTIFICATION_TYPE_USER_SUSPENDED = 6;
    public static final Integer NOTIFICATION_TYPE_USER_SUSPENDED_2 = 7;
    public static final Integer NOTIFICATION_TYPE_USER_SUSPENDED_3 = 8;
    public static final Integer NOTIFICATION_TYPE_USER_DELETED = 9;
    public static final Integer NOTIFICATION_TYPE_INVOICE_PAPER = 12;
    public static final Integer NOTIFICATION_TYPE_ORDER_EXPIRE_1 = 13;
    public static final Integer NOTIFICATION_TYPE_ORDER_EXPIRE_2 = 14;
    public static final Integer NOTIFICATION_TYPE_ORDER_EXPIRE_3 = 15;
    public static final Integer NOTIFICATION_TYPE_PAYMENT_SUCCESS = 16;
    public static final Integer NOTIFICATION_TYPE_PAYMENT_FAILED = 17;
    public static final Integer NOTIFICATION_TYPE_INVOICE_REMINDER = 18;
    public static final Integer NOTIFICATION_TYPE_CREDIT_CARD_UPDATE = 19;
    public static final Integer NOTIFICATION_TYPE_LOST_PASSWORD = 20;
    public static final Integer NOTIFICATION_TYPE_INITIAL_CREDENTIALS = 21;
    public static final Integer NOTIFICATION_TYPE_USAGE_POOL_CONSUMPTION = 24;
    
    // contact type
    public static final Integer ENTITY_CONTACT_TYPE = new Integer(1);

    //Jbilling Table Ids
    public static final Integer ENTITY_TABLE_ID = new Integer(5);

    //Internal AssetStatus objects
    public static final Integer ASSET_STATUS_MEMBER_OF_GROUP = 1;

    // primary currency id is assumed to be USD currently
    public static final Integer PRIMARY_CURRENCY_ID = 1;

    // JBilling Interface Name
    public static final String I_SCHEDULED_TASK = "com.sapienter.jbilling.server.process.task.IScheduledTask";
    // AbstractSimpleScheduledTask parameter
    public static final String PARAM_START_TIME = "start_time";
    public static final String PARAM_END_TIME = "end_time";
    public static final String PARAM_REPEAT = "repeat";
    // Scheduled Plugin Date/Time Format
    public static final String DATE_TIME_FORMAT = "yyyyMMdd-HHmm";

    public static final String CC_DATE_FORMAT = "MM/yyyy";

    // Currency Field Names
    public static final String FIELD_CURRENCY = "currency";
    public static final String FIELD_FEE_CURRENCY = "feeCurrency";
    
    // Decimal Point String Constant
    public static final String DOT = ".";
    public static final String DECIMAL_POINT = ".";
    public static final String SINGLE_SPACE = " ";
    public static final String COLON = ":";
    public static final String PIPE = "|";

    //Account Type Table
    public static final String TABLE_ACCOUNT_TYPE = "account_type";
    
    // Reseller Customer CommonConstants
    public static final String RESELLER_PASSWORD = "P@ssword1";
    
    //Route Table
    public static final String TABLE_ROUTE = "route";
    //Route RateCard Table
    public static final String TABLE_ROUTE_RATE_CARD = "route_rate_card";

    public static final String PRICE_FIELD_FILE_NAME = "file_name";

 //
    public static final String PROPERTY_RUN_COMMISION = "process.run_commission";
    public static final String PROPERTY_RUN_API_ONLY_BUT_NO_BATCH = "process.run_api_only_but_no_batch";
    public static final String PROPERTY_RUN_ORDER_UPDATE = "process.run_order_update";
    public static final String BLANK_STRING = "";
    
    // Main Subscription Period Temlate Name
    public static final String TEMPLATE_MONTHLY = "monthly";
    public static final String TEMPLATE_WEEKLY = "weekly";
    public static final String TEMPLATE_DAILY = "daily";
    public static final String TEMPLATE_YEARLY= "yearly";
    public static final String TEMPLATE_SEMI_MONTHLY= "semiMonthly";
    
    public static final Integer SEMI_MONTHLY_END_OF_MONTH= new Integer(15);

    public static final String PLANS_INTERNAL_CATEGORY_NAME = "plans";

    //encryption scheme
    public static final String PASSWORD_ENCRYPTION_SCHEME = "security.password_encrypt_scheme";
	public static final String PROPERTY_LOCKOUT_PASSWORD = "security.lockout_password";

    //Invoice Templates
    public static final String TABLE_INVOICE_TEMPLATE = "invoice_template";
    public static final String TABLE_INVOICE_TEMPLATE_VERSION = "invoice_template_version";
    public static final String TABLE_INVOICE_TEMPLATE_FILE = "invoice_template_file";

    // Enumerations
    public static final Integer ENUMERATION_VALUE_MAX_LENGTH = Integer.valueOf(50);

    public static final String RESERVAED_STATUS = "Reserved";

    /*
        Response error codes
    */
    public static final Integer ERROR_CODE_404 = Integer.valueOf(404);
}
