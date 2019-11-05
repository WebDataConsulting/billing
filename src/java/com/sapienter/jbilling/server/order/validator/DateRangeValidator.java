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

import org.apache.commons.lang.WordUtils;

import com.sapienter.jbilling.common.FormatLogger;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * DateRangeValidator
 *
 * @author Brian Cowdery
 * @since 26/01/11
 */
public class DateRangeValidator implements ConstraintValidator<DateRange, Object> {

    private static final FormatLogger LOG = new FormatLogger(DateRangeValidator.class);

    private String startDateFieldName;
    private String endDateFieldName;

    public void initialize(final DateRange dateRange) {
        startDateFieldName = dateRange.start();
        endDateFieldName = dateRange.end();
    }

    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        try {
            Class klass = object.getClass();

            Date startDate = (Date) getAccessorMethod(klass, startDateFieldName).invoke(object);
            Date endDate = (Date) getAccessorMethod(klass, endDateFieldName).invoke(object);
            
            String className = klass.getSimpleName();
            if(className.equals("ItemDTOEx"))
            	return startDate == null || endDate == null || startDate.before(endDate) || startDate.equals(endDate);
            else
            	return startDate == null || endDate == null || startDate.before(endDate);

        } catch (IllegalAccessException e) {
            LOG.debug("Illegal access to the date range property fields.");
        } catch (NoSuchMethodException e) {
            LOG.debug("Date range property missing JavaBeans getter/setter methods.");
        } catch (InvocationTargetException e) {
            LOG.debug("Date property field cannot be accessed.");
        } catch (ClassCastException e) {
            LOG.debug("Property does not contain a java.util.Date object.");
        }

        return false;
    }

    /**
     * Returns the accessor method for the given property name. This assumes
     * that the property follows normal getter/setter naming conventions so that
     * the method name can be resolved introspectively.
     *
     * @param klass class of the target object
     * @param propertyName property name
     * @return accessor method
     */
    public Method getAccessorMethod(Class klass, String propertyName) throws NoSuchMethodException {
        return klass.getMethod("get" + WordUtils.capitalize(propertyName));
    }
}
