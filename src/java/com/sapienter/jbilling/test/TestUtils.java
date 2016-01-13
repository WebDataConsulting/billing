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

package com.sapienter.jbilling.test;

import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;

public class TestUtils {

    public static <T> List<T> arrayToList (final T[] array) {
        final List<T> newList = new ArrayList<T>(array.length);

        Collections.addAll(newList, array);
        return newList;
    }

    public static <T> List<T> arrayToFixedSizeList (final T[] array) {
        return Arrays.asList(array);
    }

    public static List<InternationalDescriptionWS> buildDescriptions (InternationalDescriptionWS... values) {
        return arrayToList(values);
    }

    public static final String             TEST_DATE_FORMAT = "MM/dd/yyyy";

    private static final DateTimeFormatter DateParser       = DateTimeFormat.forPattern(TEST_DATE_FORMAT);

    /*
     * utility methods
     */
    protected static Date AsDate (String dateStr) {
        return DateParser.parseLocalDate(dateStr).toDateTimeAtStartOfDay().toDate();
    }

    protected static Date AsDate (int year, int month, int day) {
        return new LocalDate(year, month, day).toDateTimeAtStartOfDay().toDate();
    }

    protected static String AsString (Date date) {
        return DateParser.print(new LocalDate(date).toDateTimeAtStartOfDay());
    }
}
