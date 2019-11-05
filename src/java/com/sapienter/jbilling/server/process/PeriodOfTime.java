package com.sapienter.jbilling.server.process;

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

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.io.Serializable;
import java.util.Date;

public class PeriodOfTime implements Serializable {

    private final LocalDate start;
    private final LocalDate end;
    private final int daysInCycle;

    static public final PeriodOfTime OneTimeOrderPeriodOfTime = new PeriodOfTime() {
        @Override
        public Date getStart() {
            return null;
        }

        @Override
        public Date getEnd() {
            return null;
        }

        @Override
        public int getDaysInPeriod() {
            return 0;
        }
    };

    public PeriodOfTime(Date start, Date end, int dayInCycle) {
        this.start = new LocalDate(start.getTime());
        this.end = new LocalDate(end.getTime());
        this.daysInCycle = dayInCycle;
    }

    private PeriodOfTime() {
        this.start = null;
        this.end = null;
        this.daysInCycle = 0;
    }

    public Date getEnd() {
        return end.toDate();
    }

    public LocalDate getDateMidnightEnd() {
        return end;
    }

    public Date getStart() {
        return start.toDate();
    }

    public LocalDate getDateMidnightStart() {
        return start;
    }

    public int getDaysInCycle() {
        return daysInCycle;
    }

    /**
     * Find the number of days between the period start date to the period end date. This means
     * that the start date is counted as a days within the period, but not the end date. For example, January 01 to
     * January 10th includes 9 days total.
     * <p>
     * This method takes into account daylight savings time to ensure that days are counted
     * correctly across DST boundaries.
     *
     * @return number of days between start and end dates
     */
    public int getDaysInPeriod() {
        if (end.isBefore(start) || end.isEqual(start)) {
            return 0;
        }
        return Days.daysBetween(start, end).getDays();
    }

    @Override
    public String toString() {
        return "period starts: " + start + " ends " + end + " days in cycle " + getDaysInCycle();
    }
}
