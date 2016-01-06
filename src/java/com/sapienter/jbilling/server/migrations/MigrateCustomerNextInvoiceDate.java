package com.sapienter.jbilling.server.migrations;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.logging.LogFactory;
import liquibase.logging.Logger;
import liquibase.statement.SqlStatement;

import org.joda.time.DateTime;

/**
 * Created by Fernando G. Morales on 4/1/15.
 */
public class MigrateCustomerNextInvoiceDate extends AbstractCustomSqlChange {

    public static Logger log = LogFactory.getLogger("MigrateCustomerNextInvoiceDate");

    @Override
    public String getConfirmationMessage() {
        return "Customer's next invoice date migrated";
    }

    @Override
    public SqlStatement[] doGenerateStatements(Database database) throws CustomChangeException {

        int userID = 0;
        Statement statement = null;
        String query = "select * from customer cust inner join base_user bu on cust.user_id=bu.id";
        try {
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
            ResultSet result = statement.executeQuery(query);
            if(result!=null && result.isBeforeFirst()) {
                while(result.next()) {
                    userID = result.getInt("user_id");
                    log.info("User ID: " + userID);
                    Date date = result.getDate("create_datetime");
                    log.info("Next Invoice Date value before update: " + date);
                    result.updateDate("next_inovice_date", calculateNextInvoiceDate(date));
                    log.info("Next Invoice Date value after update: " + date);
                    result.updateRow();
                }
            }
        } catch(SQLException e) {
            log.info("Cannot update customer id = " + userID);
        }
        return new SqlStatement[0];
    }

    /**
     * For each customer (test), whose default values for main subscription are as follows:
     * a) period = 1 (monthly)
     * b) invoice billiable day = 1
     * this method calculates the next invoice date, a required param for new customers.
     *
     * @param creationDate
     * @return nextInvoiceDate
     */
    private Date calculateNextInvoiceDate(java.util.Date creationDate) {
        java.util.Date nextInvoiceDate = null;
        DateTime customerCreationDateTime = new DateTime(creationDate);
        //this is set to 1 for all customers by default
        int invoiceDayOfPeriod = 1;
        int customerCreationDay = customerCreationDateTime.getDayOfMonth();
        if(customerCreationDay==invoiceDayOfPeriod) {
            nextInvoiceDate = customerCreationDateTime.toDate();
        }
        else {
            nextInvoiceDate = customerCreationDateTime.plusMonths(1).withDayOfMonth(1).toDate();
        }
        return new Date(nextInvoiceDate.getTime());
    }

}
