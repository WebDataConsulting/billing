--- this script will upgrade a database schema from jbilling release 3.x to the the latest jbilling 3.1 release.

-- It is tested on postgreSQL, but it is meant to be ANSI SQL
--
-- MySQL does not support many of the ANSI SQL statements used in this file to upgrade the
-- base schema. If you are using MySQL as your database, you will need to edit this file and
-- comment out the labeled 'postgresql' statements and un-comment the ones labeled 'mysql'


-- Date: 27-Jul-2011
-- Redmine Issue: #1108
-- Description: Subscriber Management - Manage Tax Rates

-- insert new tax plugin to the database
insert into pluggable_task_type (id, category_id, class_name, min_parameters) values (90, 4, 'com.sapienter.jbilling.server.process.task.CountryTaxCompositionTask', 2);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (24,  90, 'title',1, 'Country Tax Invoice Composition Task');
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (24,  90, 'description', 1, 'A pluggable task of the type AbstractChargeTask to apply tax item to the Invoice if the Partner''s country code is matching.');

-- insert new payment term penalty plugin
insert into pluggable_task_type (id, category_id, class_name, min_parameters) values (91, 4, 'com.sapienter.jbilling.server.process.task.PaymentTermPenaltyTask', 2);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (24,  91, 'title',1, 'Payment Terms Penalty Task');
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (24,  91, 'description', 1, 'A pluggable task of the type AbstractChargeTask to apply a Penalty to an Invoice having a due date beyond a configurable days period.');


-- Date: 28-Jul-2011
-- Description: user names can not be less than 5 characters. jB1 and 2 allows for a length of 4 chars
update base_user set user_name = user_name || '1' where id in ( select id from base_user where length(user_name) < 5); -- postgresql


-- Date: 29-Jul-2011
-- Redmine Issue: #1208
-- Description: Sub-account pricing
alter table customer add column use_parent_pricing boolean;
update customer set use_parent_pricing = false where use_parent_pricing is null;
alter table customer alter column use_parent_pricing set not null;

-- remove obsolete TieredPriceModelPricingTask plug-in, functionality moved into PriceModelPricingTask
update pluggable_task set type_id = 79 where type_id = 80;
delete from pluggable_task_type where id = 80;


-- Date: 08-Aug-2011
-- Redmine Issue: #1235
-- Description: Nullable custom contact field values

alter table contact_field alter column content drop not null; -- postgresql
-- alter table contact_field modify (content null); -- oracle
-- alter table customer modify content varchar(100) null default null; -- mysql

-- Date: 09-Aug-2011
-- Redmine Issue: #1233
-- Description: Enumerations
insert into jbilling_table VALUES (105, 'enumeration');
insert into jbilling_seqs VALUES ('enumeration', 1);

CREATE TABLE enumeration (
  id integer NOT NULL,
  entity_id integer NOT NULL,
  name character varying(50) NOT NULL,
  optlock integer NOT NULL,
  CONSTRAINT enumeration_pkey PRIMARY KEY (id)
);

insert into jbilling_table VALUES (106, 'enumeration_values');
insert into jbilling_seqs VALUES ('enumeration_values', 1);

CREATE TABLE enumeration_values (
  id integer NOT NULL,
  enumeration_id integer NOT NULL,
  value character varying(50) NOT NULL,
  optlock integer NOT NULL,
  CONSTRAINT enumeration_values_pkey PRIMARY KEY (id),
  CONSTRAINT enumeration_values_fk_1 FOREIGN KEY (enumeration_id)
      REFERENCES enumeration (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- Date: 11-Aug-2011
-- Redmine Issue: #1234
-- Description: CCF Display In View
ALTER TABLE contact_field_type add column display_in_view smallint default 0;
ALTER TABLE contact_field_type ALTER COLUMN data_type TYPE VARCHAR(50);


-- Date: 14-Aug-2011
-- Description: Add Simple Tax plug-in to DB
-- insert new tax plugin to the database
insert into pluggable_task_type (id, category_id, class_name, min_parameters)
	values (92, 4, 'com.sapienter.jbilling.server.process.task.SimpleTaxCompositionTask', 1);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content)
	values (24,  92, 'title',1, 'Simple Tax Invoice Composition Task');
insert into international_description (table_id, foreign_id, psudo_column, language_id, content)
	values (24,  92, 'description', 1, 'A pluggable task to automatically add taxes to invoices, with the option of exluding some customers and some items (excemptions).');


-- Date: 15-Aug-2011
-- Redmine Issue: #1212
-- Description: Add date and time dimension to pricing models

-- map of item prices with start dates
drop table if exists item_price_timeline;
create table item_price_timeline (
    item_id int not null,
    price_model_id int not null,
    start_date timestamp,
    primary key (item_id, start_date),
    unique (price_model_id)
);
alter table item_price_timeline add constraint item_timeline_price_model_id_FK foreign key (price_model_id) references price_model;
alter table item_price_timeline add constraint item_timeline_item_id_FK foreign key (item_id) references item;

insert into item_price_timeline (item_id, price_model_id, start_date) select id, price_model_id, '1970-01-01 00:00' as start_date from item where price_model_id is not null;
alter table item drop column price_model_id;

-- map of plan item prices with start dates
drop table if exists plan_item_price_timeline;
create table plan_item_price_timeline (
    plan_item_id int not null,
    price_model_id int not null,
    start_date timestamp,
    primary key (plan_item_id, start_date),
    unique (price_model_id)
);


alter table plan_item_price_timeline add constraint plan_item_timeline_price_mode_id_FK foreign key (price_model_id) references price_model;
alter table plan_item_price_timeline add constraint plan_item_timeline_plan_item_id_FK foreign key (plan_item_id) references plan_item;

insert into plan_item_price_timeline (plan_item_id, price_model_id, start_date) select id, price_model_id, '1970-01-01 00:00' as start_date from plan_item where price_model_id is not null;
alter table plan_item drop column price_model_id;


-- Date: 29-Sept-2011
-- Redmine Issue: #1126
-- Description: Historical plan pricing report
insert into report_type (id, name, optlock) values (5, 'plan', 0);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (101, 5, 'description', 1, 'Plan');

insert into report (id, type_id, name, file_name, optlock) values (10, 5, 'plan_history', 'plan_history.jasper', 0);
insert into report_parameter (id, report_id, dtype, name) values (15, 10, 'integer', 'plan_id');
insert into report_parameter (id, report_id, dtype, name) values (16, 10, 'string', 'plan_code');
insert into report_parameter (id, report_id, dtype, name) values (17, 10, 'string', 'plan_description');
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (100, 10, 'description', 1, 'Plan pricing history for all plan products and start dates.');
insert into entity_report_map (select 10 as report_id, e.id as entity_id from entity e);

-- Date: 13-Oct-2011
-- Redmine Issue: #1445
-- Description: Switch Users
insert into permission_type (id, description) values (11, 'User Switching');

insert into permission (id, type_id) values (110, 11);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59, 110, 'description', 1, 'Switch to sub-account');

insert into permission (id, type_id) values (111, 11);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59, 111, 'description', 1, 'Switch to any user');

-- switch all for super users
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 111 as permission_id from role r where r.role_type_id=2 group by r.id;

-- basic view permissions for customers
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 24 as permission_id from role r where r.role_type_id=5 group by r.id; -- view orders
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 30 as permission_id from role r where r.role_type_id=5 group by r.id; -- create payment
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 34 as permission_id from role r where r.role_type_id=5 group by r.id; -- view payments
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 72 as permission_id from role r where r.role_type_id=5 group by r.id; -- view invoices
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 91 as permission_id from role r where r.role_type_id=5 group by r.id; -- invoices menu
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 92 as permission_id from role r where r.role_type_id=5 group by r.id; -- order menu
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 93 as permission_id from role r where r.role_type_id=5 group by r.id; -- payments menu

-- Date: 27-Oct-2011
-- Redmine Issue: #1444
-- Description: Customer sub-account management

-- Customer: view all customers
insert into permission (id, type_id) values (17, 1);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59,17,'description', 1, 'View all customers');

-- Customer: view customer sub-accounts
insert into permission (id, type_id) values (18, 1);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59,18,'description', 1, 'View customer sub-accounts');

-- Order: view customer sub-accounts
insert into permission (id, type_id) values (29, 2);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59,29,'description', 1, 'View customer sub-accounts');

-- Payment: view customer sub-accounts
insert into permission (id, type_id) values (37, 3);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59,37,'description', 1, 'View customer sub-accounts');

-- Invoice: view customer sub-accounts
insert into permission (id, type_id) values (75, 7);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59,75,'description', 1, 'View customer sub-accounts');

-- super users and clerks can view all
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 17 as permission_id from role r where r.role_type_id=2 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 17 as permission_id from role r where r.role_type_id=3 group by r.id;

-- customers can view sub-accounts
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 90 as permission_id from role r where r.role_type_id=5 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 15 as permission_id from role r where r.role_type_id=5 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 18 as permission_id from role r where r.role_type_id=5 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 29 as permission_id from role r where r.role_type_id=5 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 37 as permission_id from role r where r.role_type_id=5 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 75 as permission_id from role r where r.role_type_id=5 group by r.id;


-- Date 21-Oct-2011
-- Redmine Issue: #1445
-- Description: Enabling Partner user
insert into permission (id, type_id) values (100, 9);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59, 100, 'description', 1, 'Show partner menu');

-- move api permissions to 12 to fit with the api access permission
insert into permission_type (id, description) values (12, 'API');
update permission set type_id = 12 where type_id = 10;
delete from permission_type where id = 10;

-- use permission type 10 for partners now that its free
insert into permission_type (id, description) values (10, 'Agent');

insert into permission (id, type_id) values (101, 10);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59, 101, 'description', 1, 'Create agent');

insert into permission (id, type_id) values (102, 10);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59, 102, 'description', 1, 'Edit agent');

insert into permission (id, type_id) values (103, 10);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59, 103, 'description', 1, 'Delete agent');

insert into permission (id, type_id) values (104, 10);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (59, 104, 'description', 1, 'View agent details');

-- permissions for super users
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 100 as permission_id from role r where r.role_type_id=2 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 101 as permission_id from role r where r.role_type_id=2 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 102 as permission_id from role r where r.role_type_id=2 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 103 as permission_id from role r where r.role_type_id=2 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 104 as permission_id from role r where r.role_type_id=2 group by r.id;

-- permissions for clerks
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 100 as permission_id from role r where r.role_type_id=3 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 101 as permission_id from role r where r.role_type_id=3 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 102 as permission_id from role r where r.role_type_id=3 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 103 as permission_id from role r where r.role_type_id=3 group by r.id;
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 104 as permission_id from role r where r.role_type_id=3 group by r.id;

-- new role and basic permissions for partners
insert into role (select distinct e.id + 100 as id, e.id as entity_id, 4 as role_type_id from entity e);
insert into international_description (select 60 as table_id, r.id as foreign_id, 'title' as psudo_column, 1 as language_id, 'Agent' as content from role r where r.role_type_id=4);
insert into international_description (select 60 as table_id, r.id as foreign_id, 'description' as psudo_column, 1 as language_id, 'A agent that will bring customers' as content from role r where r.role_type_id=4);

insert into permission_role_map (role_id, permission_id) select r.id as role_id, 15 as permission_id from role r where r.role_type_id=4 group by r.id; -- view customers
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 10 as permission_id from role r where r.role_type_id=4 group by r.id; -- create customer
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 11 as permission_id from role r where r.role_type_id=4 group by r.id; -- edit customer
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 24 as permission_id from role r where r.role_type_id=4 group by r.id; -- view orders
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 28 as permission_id from role r where r.role_type_id=4 group by r.id; -- view all customer orders
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 20 as permission_id from role r where r.role_type_id=4 group by r.id; -- create orders
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 21 as permission_id from role r where r.role_type_id=4 group by r.id; -- edit orders
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 34 as permission_id from role r where r.role_type_id=4 group by r.id; -- view payments
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 36 as permission_id from role r where r.role_type_id=4 group by r.id; -- view all customer payments
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 30 as permission_id from role r where r.role_type_id=4 group by r.id; -- create payment
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 72 as permission_id from role r where r.role_type_id=4 group by r.id; -- view invoices
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 74 as permission_id from role r where r.role_type_id=4 group by r.id; -- view all customer invoices
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 90 as permission_id from role r where r.role_type_id=4 group by r.id; -- customers menu
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 91 as permission_id from role r where r.role_type_id=4 group by r.id; -- invoices menu
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 92 as permission_id from role r where r.role_type_id=4 group by r.id; -- order menu
insert into permission_role_map (role_id, permission_id) select r.id as role_id, 93 as permission_id from role r where r.role_type_id=4 group by r.id; -- payments menu


-- 10-Jan-2011
-- Redmine Issue: #1165
-- Description: Credit Card that expires in the same month does not process automatically
alter table currency_exchange add column valid_since timestamp not null default Date('1970-01-01');

-- 02-Feb-2012
-- Redmine Issue: #2125
-- Description: New Report: Total Invoiced per Customer.

--Change User Reports to Customer Reports.
update international_description
set content = 'Customer Reports'
where table_id = 101
and foreign_id = 4;

--Add report.
insert into report(id, type_id, name, file_name, optlock) values (11, 4, 'total_invoiced_per_customer', 'total_invoiced_per_customer.jasper', 1);
insert into report_parameter(id, report_id, dtype, name) values(18, 11, 'date', 'start_date');
insert into report_parameter(id, report_id, dtype, name) values(19, 11, 'date', 'end_date');
insert into international_description(table_id, foreign_id, psudo_column, language_id, content) values(100, 11, 'description', 1, 'Total invoiced per customer grouped by product category.');
insert into entity_report_map (select 11 as report_id, e.id as entity_id from entity e);

-- 03-Feb-2012
-- Redmine Issue: #2127
-- Description: New Report: Total Invoiced per Customer, comparison over years.

--Add report.
insert into report(id, type_id, name, file_name, optlock) values (12, 4, 'total_invoiced_per_customer_over_years', 'total_invoiced_per_customer_over_years.jasper', 1);
insert into report_parameter(id, report_id, dtype, name) values(20, 12, 'string', 'start_year');
insert into report_parameter(id, report_id, dtype, name) values(21, 12, 'string', 'end_year');
insert into international_description(table_id, foreign_id, psudo_column, language_id, content) values(100, 12, 'description', 1, 'Total invoiced per customer over years grouped by year.');
insert into entity_report_map (select 12 as report_id, e.id as entity_id from entity e);



-- Date: 12-Oct-2011
-- Redmine Issue: #1425
-- Custom Fields for User, Item, Order, Invoice, Payment

-- new tables
CREATE TABLE meta_field_name
(
    id INTEGER NOT NULL,
    name VARCHAR(100),
    entity_type VARCHAR(25) NOT NULL,
    data_type VARCHAR(25) NOT NULL,
    is_disabled BOOLEAN,
    is_mandatory BOOLEAN,
    display_order INTEGER,
    default_value_id INTEGER,
    OPTLOCK INTEGER NOT NULL,
    PRIMARY KEY (id)
);


-- MySQL or other DB
-- create table meta_field_name (
--   id integer NOT NULL,
--   "name" character varying(100) NOT NULL,
--   entity_type character varying (25) NOT NULL,
--   data_type character varying (25) NOT NULL,
--   is_disabled smallint,
--   is_mandatory smallint,
--   display_order integer,
--   default_value_id integer,
--   CONSTRAINT meta_field_name_pkey PRIMARY KEY (id),
--   UNIQUE(id)
-- );

create table meta_field_value (
    id integer NOT NULL,
    meta_field_name_id integer NOT NULL,
    dtype character varying(10) NOT NULL,
    boolean_value boolean,
    date_value timestamp without time zone,
    decimal_value numeric(22,10),
    integer_value integer,
    string_value character varying(1000),
    PRIMARY KEY (id),
    CONSTRAINT meta_field_value_fk_1 FOREIGN KEY (meta_field_name_id)
      REFERENCES meta_field_name (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);
alter table meta_field_name add constraint meta_field_name_fk_1
      FOREIGN KEY (default_value_id)
      REFERENCES meta_field_value (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;

create table customer_meta_field_map (
    customer_id integer NOT NULL,
    meta_field_value_id integer NOT NULL,
    CONSTRAINT customer_meta_field_map_fk_1 FOREIGN KEY (customer_id)
      REFERENCES customer (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT customer_meta_field_map_fk_2 FOREIGN KEY (meta_field_value_id)
      REFERENCES meta_field_value (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

create table order_meta_field_map (
    order_id integer NOT NULL,
    meta_field_value_id integer NOT NULL,
    CONSTRAINT order_meta_field_map_fk_1 FOREIGN KEY (order_id)
      REFERENCES purchase_order (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT order_meta_field_map_fk_2 FOREIGN KEY (meta_field_value_id)
      REFERENCES meta_field_value (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

create table item_meta_field_map (
    item_id integer NOT NULL,
    meta_field_value_id integer NOT NULL,
    CONSTRAINT item_meta_field_map_fk_1 FOREIGN KEY (item_id)
      REFERENCES item (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT item_meta_field_map_fk_2 FOREIGN KEY (meta_field_value_id)
      REFERENCES meta_field_value (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

create table invoice_meta_field_map (
    invoice_id integer NOT NULL,
    meta_field_value_id integer NOT NULL,
    CONSTRAINT invoice_meta_field_map_fk_1 FOREIGN KEY (invoice_id)
      REFERENCES invoice (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT invoice_meta_field_map_fk_2 FOREIGN KEY (meta_field_value_id)
      REFERENCES meta_field_value (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

create table payment_meta_field_map (
    payment_id integer NOT NULL,
    meta_field_value_id integer NOT NULL,
    CONSTRAINT payment_meta_field_map_fk_1 FOREIGN KEY (payment_id)
      REFERENCES payment (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT payment_meta_field_map_fk_2 FOREIGN KEY (meta_field_value_id)
      REFERENCES meta_field_value (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- move old CCF to new meta fields for USER entity type

INSERT INTO meta_field_name(
            id, "name", entity_type, data_type, is_disabled, is_mandatory,
            display_order, default_value_id, optlock)
select id, prompt_key, 'CUSTOMER',
   case data_type
    when 'string' then 'STRING'
    when 'integer' then 'INTEGER'
    when 'date' then 'DATE'
    when 'boolean' then 'BOOLEAN'
    when 'decimal' then 'DECIMAL'
    else 'STRING'
   END,
   false, false, id, NULL, 0
from contact_field_type;

-- MySQL or other DB
-- INSERT INTO meta_field_name(
--             id, "name", entity_type, data_type, is_disabled, is_mandatory,
--             display_order, default_value_id)
-- select id, prompt_key, 'CUSTOMER',
--    case data_type
--     when 'string' then 'STRING'
--     when 'integer' then 'INTEGER'
--     when 'date' then 'DATE'
--     when 'boolean' then 'BOOLEAN'
--     when 'decimal' then 'DECIMAL'
--     else 'STRING'
--    END,
--    0, 0, id, NULL
-- from contact_field_type;


INSERT INTO meta_field_value(
            id, meta_field_name_id, dtype,
            boolean_value, date_value, decimal_value, integer_value, string_value)
select  cf.id,
    cf.type_id,
    case data_type
        when 'string' then 'string'
        when 'integer' then 'integer'
        when 'date' then 'date'
        when 'boolean' then 'boolean'
        when 'decimal' then 'decimal'
        else 'string'
    END,
    case cft.data_type
        when 'boolean' then
            case cf.content
                when 'true' then TRUE
                else FALSE
            END
        else NULL
    END,
    case cft.data_type
        when 'date' then date(cf.content)
        else NULL
    END,
    case cft.data_type
        when 'decimal' then cast(cf.content as numeric(22,10))
        else NULL
    END,
    case cft.data_type
        when 'integer' then cast(cf.content as integer)
        else NULL
    END,
    case cft.data_type
        when 'integer' then NULL
        when 'date' then NULL
        when 'boolean' then NULL
        when 'decimal' then NULL
        else cf.content
    END

from contact_field cf
  inner join contact_field_type cft on cf.type_id = cft.id
  inner join contact_map cm on cf.contact_id = cm.contact_id, customer cust
where cm.table_id = 10 and cust.user_id = cm.foreign_id;

insert into customer_meta_field_map (customer_id, meta_field_value_id)
select cust.id, cf.id
from contact_field cf inner join contact_map cm on cf.contact_id = cm.contact_id, customer cust
where cm.table_id = 10 and cust.user_id = cm.foreign_id;

-- changes to BlackListDTO for metaFields support
alter table blacklist add meta_field_value_id integer;
alter table blacklist add constraint blacklist_fk_4
      FOREIGN KEY (meta_field_value_id)
      REFERENCES meta_field_value (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;

-- drop old contact custom fields
DROP TABLE contact_field;
DROP TABLE contact_field_type;

insert into jbilling_seqs (name, next_id) values ('meta_field_name', 1);
insert into jbilling_seqs (name, next_id) values ('meta_field_value', 1);


-- Date: 16-Feb-2012
-- Redmine Issue: #1785
-- Description: Rate cards
drop table if exists rate_card;
-- Date: 16-Mar-2012
-- New plug-in for cancellation fees (without using rules)
insert into pluggable_task_type (id, category_id, class_name, min_parameters)
    values(94, 17, 'com.sapienter.jbilling.server.order.task.CancellationFeeTask', 2);
insert into international_description(table_id, foreign_id, psudo_column, language_id, content)
    values(24, 94, 'title', 1, 'Fees for early cancellation of a subscription');
insert into international_description(table_id, foreign_id, psudo_column, language_id, content)
    values(24, 94, 'description', 1, 'This plug-in will create a new order with a fee if a recurring order is cancelled too early');

-- Date 19-Mar-2012
-- Set correct meta field values next_id value
update jbilling_seqs set next_id = coalesce((select round(max(id)/10)+1 from meta_field_value), 1) where name = 'meta_field_value';

-- Date 20-Mar-2012
-- Bugs #2418 Metafields should be company wide instead of system wide
alter table meta_field_name add entity_id integer default 1;
alter table meta_field_name add constraint meta_field_entity_id_FK foreign key (entity_id) references entity (id);

-- Date: 24-March-2012
-- Redmine Issue: #2257
-- Description: rename meta field type ITEM to PRODUCT
update meta_field_name set entity_type = 'PRODUCT' where entity_type = 'ITEM';



-- Date 25-March-2012
-- Redmine Issue: #1549
-- Description: list data type for meta fields
create table list_meta_field_values (
    meta_field_value_id int not null,
    list_value varchar(255)
);

alter table list_meta_field_values
        add constraint list_meta_field_values_fk_1
        foreign key (meta_field_value_id)
        references meta_field_value;



-- Date 25-March-2012
-- Redmine Issue: #1827
-- Description: meta fields for partners
create table partner_meta_field_map (
    partner_id integer NOT NULL,
    meta_field_value_id integer NOT NULL,
    CONSTRAINT partner_meta_field_map_fk_1 FOREIGN KEY (partner_id)
      REFERENCES partner (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT partner_meta_field_map_fk_2 FOREIGN KEY (meta_field_value_id)
      REFERENCES meta_field_value (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

update jbilling_seqs set next_id = coalesce((select round(max(id)/100)+1 from role), 1) where name = 'role';

-- Date: 17-May-2012
-- Redmine Issue: #2747
-- Description: Unable to pay invoice (add new payment info task plugin)
insert into pluggable_task_type (id, category_id, class_name, min_parameters) values (95, 8, 'com.sapienter.jbilling.server.pluggableTask.AlternativePaymentInfoTask', 0);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (24,  95, 'title',1, 'Alternative Payment Info Task');
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (24,  95, 'description', 1, 'A pluggable task of the type Payment Info Task that first checks the preferred payment method than if there is no data for the preferred method it searches for alternative payment methods');

update order_line set use_item = false where use_item is null;
alter table order_line alter column use_item set not null;

alter table price_model alter column strategy_type type varchar(40); -- postgresql
-- alter table price_model modify strategy_type varchar(40); -- mysql

insert into pluggable_task_type values (97, 7, 'com.sapienter.jbilling.server.notification.task.TestNotificationTask',0);

-- 01-Nov-2011
-- Redmine Issue: #1404
-- Description: Credit Card that expires in the same month does not process automatically

update credit_card set cc_expiry = date_trunc('month', cc_expiry) + INTERVAL '1 month' - INTERVAL '1 day'; -- postgresql
-- update credit_card set cc_expiry = last_day(cc_expiry); -- mysql

-- Date 18-May-2012
-- Replace timestamp columns with date for timelines
alter table currency_exchange alter valid_since type date;
alter table item_price_timeline alter start_date type date;
alter table plan_item_price_timeline alter start_date type date;

-- Date 30-May-2012
-- Bugs #2825 - Fix percentage products in Plans.
insert into order_period (id, entity_id, value, unit_id, optlock) values (5, null, null, null, 1);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (17, 5, 'description', 1, 'All Orders');


--Date 30-Nov-2012
--Requirement #3990 - Make orderId persistent to InvoiceLineDTO
ALTER TABLE invoice_line add column order_id integer;
ALTER table invoice_line add constraint "invoice_line_fk_4" foreign key (order_id) references purchase_order(id);


-- 05-Dec-2012
-- Redmine issue #2006
-- Description: Option to force unique emails in the company
insert into preference_type values (51, 0);

insert into international_description (table_id, foreign_id, psudo_column, language_id, content)
values (50, 51, 'description', 1, 'Force Unique Emails');

insert into international_description (table_id, foreign_id, psudo_column, language_id, content)
values (50, 51, 'instruction', 1, 'Set to 1 in order to force unique emails among the users/customers into the company. Set to 0 otherwise.');

-- Date: 28-Dec-2012
-- Redmine Issue: #2488
-- Description: Import feature - BalanceBelowThresholdNotificationTask
insert into pluggable_task_type (id, category_id, class_name, min_parameters) values (92, 17, 'com.sapienter.jbilling.server.user.tasks.BalanceThresholdNotificationTask', 1);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (24,  92, 'title',1, 'User Balance threshold notification task');
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (24,  92, 'description', 1, 'A pluggable task of the type InternalEventsTask to monitor if users pre-paid balance is below a threshold level and send notifications.');

--
insert into notification_category values (5);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (104, 5, 'description', 1, 'Custom Notifications');

INSERT INTO notification_message_type(id, category_id, optlock) VALUES (23, 5, 0);
insert into international_description (table_id, foreign_id, psudo_column, language_id, content) values (52, 23, 'description', 1, 'Balance Below Threshold');

INSERT INTO notification_message (id, type_id, entity_id, language_id, use_flag, optlock) VALUES (200, 23, 10, 1, 1, 0);
INSERT INTO notification_message_section (id, message_id, section, optlock) VALUES (200, 200, 1,0), (201, 200, 2, 0), (202, 200, 3, 0);
INSERT INTO notification_message_line (id, message_section_id, content, optlock)  VALUES (200,200,'Your pre-paid balance is below Water mark.', 0),
 (201, 201, 'Dear $userSalutation,\r\n\r\nYour Pre-paid balance has reached below the threshold mark of $thresholdAmt. Your balance is currently at $dynamicBalance.\r\n\r\nTherefore, you are kindly advised to make sufficient payment into your account as early as possible.\r\n\r\nThanks.', 0);

update jbilling_seqs set next_id = coalesce((select round(max(id)/100)+1 from notification_message), 1) where name = 'notification_message';
update jbilling_seqs set next_id = coalesce((select round(max(id)/100)+1 from notification_message_section), 1) where name = 'notification_message_section';
update jbilling_seqs set next_id = coalesce((select round(max(id)/100)+1 from notification_message_line), 1) where name = 'notification_message_line';

-- 19-Feb-2013
-- Redmine issue #2378
-- Description: Need to show billing period in billing process UI, and need it in plural as well as singular form.
update international_description set content = replace(content,'(s)', '')  where table_id = 6 and language_id = 1 and psudo_column = 'description';
update international_description set content = content || '(s)' where table_id = 6 and language_id = 1 and psudo_column = 'description';

-- 29-Apr-2013
-- Redmine issue #4933 - Invoice Date and Invoice Due Date are Incorrect
update international_description set content='Default blank. This preference has to be a date (In the format yyyy-mm-dd. Example: 2000-01-31), the system will make sure that all your invoices have their dates in an incremental way. Any invoice with a greater ''ID'' will also have a greater (or equal) date. In other words, a new invoice can not have an earlier date than an existing (older) invoice. To use this preference, set it as a string with the date where to start. This preference will not be used if blank'
 where table_id=50 and foreign_id=31 and language_id=1 and psudo_column='instruction';

--update international_description set content='Default blank. This preference has to be a date (In the format yyyy-mm-dd. Example: 2000-01-31), the system will make sure that all your invoices have their dates in an incremental way. Any invoice with a greater \'ID\' will also have a greater (or equal) date. In other words, a new invoice can not have an earlier date than an existing (older) invoice. To use this preference, set it as a string with the date where to start. This preference will not be used if blank'
-- where table_id=50 and foreign_id=31 language_id=1 and psudo_column='instruction'; -- mysql

update preference_type set def_value='' where id=31;

-- 25-Oct-2013
-- Redmine issue #6183 - FR10.9 Credit Card Transactions Report
update international_description set content = 'Discover' where content = 'Discovery' and table_id = 35;
