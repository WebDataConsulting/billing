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

package com.sapienter.jbilling.common;

import com.sapienter.jbilling.server.util.CalendarUtils;
import junit.framework.TestCase;
import org.joda.time.DateMidnight;
import org.joda.time.Period;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: Panche.Isajeski
 * @since: 12/06/12
 */
public class CalendarNearestTargetDateTest extends TestCase {

    public void testEndOfMonthTargetDateInPast() {

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.clear();

        calendar.set(2006, Calendar.NOVEMBER, 30);

        Date sourceDate = calendar.getTime();

        calendar.set(2012, Calendar.NOVEMBER, 05);

        Date targetDate = calendar.getTime();

        Date nearestDate = CalendarUtils.findNearestTargetDateInPast(sourceDate, targetDate, 31, 1, 1);

        calendar.set(2012, Calendar.OCTOBER, 31);
        assertEquals("nearest target in past for end of month not matching", calendar.getTime(), nearestDate);

    }

    public void testFindNearestTargetDateInPast() {

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.clear();

        calendar.set(2006, Calendar.AUGUST, 31);

        Date sourceDate = calendar.getTime();

        calendar.set(2012, Calendar.MAY, 10);

        Date targetDate = calendar.getTime();

        Date nearestDate = CalendarUtils.findNearestTargetDateInPast(sourceDate, targetDate, 31, 1, 1);

        calendar.set(2012, Calendar.APRIL, 30);
        assertEquals("nearest target in past not matching", calendar.getTime(), nearestDate);

    }

    public void testFebruaryLeapYearInPast() {

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.clear();

        calendar.set(2006, Calendar.AUGUST, 31);

        Date sourceDate = calendar.getTime();

        calendar.set(2012, Calendar.MARCH, 10);

        Date targetDate = calendar.getTime();

        Date nearestDate = CalendarUtils.findNearestTargetDateInPast(sourceDate, targetDate, 30, 1, 1);

        calendar.set(2012, Calendar.FEBRUARY, 29);
        assertEquals("leap year february date in past not matching", calendar.getTime(), nearestDate);

    }

    public void testFebruaryNonLeapYearInPast() {

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.clear();

        calendar.set(2006, Calendar.AUGUST, 31);

        Date sourceDate = calendar.getTime();

        calendar.set(2011, Calendar.MARCH, 10);

        Date targetDate = calendar.getTime();

        Date nearestDate = CalendarUtils.findNearestTargetDateInPast(sourceDate, targetDate, 30, 1, 1);

        calendar.set(2011, Calendar.FEBRUARY, 28);
        assertEquals("non leap year february date in past not matching", calendar.getTime(), nearestDate);

    }

    public void testFindNearestTargetDateInPastNegative() {

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.clear();

        calendar.set(2011, Calendar.OCTOBER, 15);

        Date sourceDate = calendar.getTime();

        calendar.set(2011, Calendar.JULY, 10);

        Date targetDate = calendar.getTime();

        Date nearestDate = CalendarUtils.findNearestTargetDateInPast(sourceDate, targetDate, 15, 1, 1);

        calendar.set(2011, Calendar.JUNE, 15);
        assertEquals("nearest negative target date in past not matching", calendar.getTime(), nearestDate);

    }

    public void testFindNearestTargetDateInFuture() {


        Calendar calendar = GregorianCalendar.getInstance();
        calendar.clear();

        calendar.set(2006, Calendar.OCTOBER, 31);

        Date sourceDate = calendar.getTime();

        calendar.set(2012, Calendar.APRIL, 15);

        Date targetDate = calendar.getTime();

        Date nearestDate = CalendarUtils.findNearestTargetDateInFuture(sourceDate, targetDate, 31, 1, 3);

        calendar.set(2012, Calendar.APRIL, 30);
        assertEquals("nearest target in future not matching", calendar.getTime(), nearestDate);

    }

    public void testEndOfMonthTargetDateInFuture() {

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.clear();

        calendar.set(2011, Calendar.NOVEMBER, 30);

        Date sourceDate = calendar.getTime();

        calendar.set(2012, Calendar.MAY, 05);

        Date targetDate = calendar.getTime();

        Date nearestDate = CalendarUtils.findNearestTargetDateInFuture(sourceDate, targetDate, 31, 1, 1);

        calendar.set(2012, Calendar.MAY, 31);
        assertEquals("nearest target in future for end of month not matching", calendar.getTime(), nearestDate);

    }

    public void testFebruaryLeapYearInFuture() {

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.clear();

        calendar.set(2006, Calendar.SEPTEMBER, 30);

        Date sourceDate = calendar.getTime();

        calendar.set(2012, Calendar.FEBRUARY, 10);

        Date targetDate = calendar.getTime();

        Date nearestDate = CalendarUtils.findNearestTargetDateInFuture(sourceDate, targetDate, 31, 1, 1);

        calendar.set(2012, Calendar.FEBRUARY, 29);
        assertEquals("leap year february date in future not matching", calendar.getTime(), nearestDate);

    }

    public void testFebruaryNonLeapYearInFuture() {

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.clear();

        calendar.set(2006, Calendar.AUGUST, 31);

        Date sourceDate = calendar.getTime();

        calendar.set(2011, Calendar.FEBRUARY, 10);

        Date targetDate = calendar.getTime();

        Date nearestDate = CalendarUtils.findNearestTargetDateInFuture(sourceDate, targetDate, 31, 1, 1);

        calendar.set(2011, Calendar.FEBRUARY, 28);
        assertEquals("non leap year february date in future not matching", calendar.getTime(), nearestDate);

    }
}
