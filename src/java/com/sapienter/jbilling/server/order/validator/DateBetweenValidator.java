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

package com.sapienter.jbilling.server.order.validator;


import com.sapienter.jbilling.common.FormatLogger;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Date;

/**
 * DateBetweenValidator
 *
 * @author Juan Vidal
 * @since 03/01/2012
 */
public class DateBetweenValidator implements ConstraintValidator<DateBetween, Date> {

    private static final FormatLogger LOG = new FormatLogger(DateBetweenValidator.class);

    private String startDate;
    private String endDate;

    public void initialize(final DateBetween dateRange) {
        if(dateRange!=null){
            startDate = dateRange.start();
            endDate = dateRange.end();
        }
    }

    public boolean isValid(Date date, ConstraintValidatorContext constraintValidatorContext) {
        try {
            if (date == null || "".equals(date.toString()) ) {
                return true;
            }
            DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy");
            Date startDate = dateTimeFormatter.parseDateTime(this.startDate).toDate();
            Date endDate = dateTimeFormatter.parseDateTime(this.endDate).toDate();

            return startDate.before(date) && endDate.after(date);

        } catch (NullPointerException e) {
            LOG.debug("Date is null.");
        } catch (ClassCastException e) {
            LOG.debug("Property does not contain a java.util.Date object.");
        } catch (IllegalArgumentException e) {
            LOG.debug("Error while parsing the date.");
        }

        return false;
    }
}
