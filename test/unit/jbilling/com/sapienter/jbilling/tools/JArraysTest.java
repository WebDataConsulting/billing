package jbilling.com.sapienter.jbilling.tools;/*
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

import com.sapienter.jbilling.tools.JArrays;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by marcomanzi on 6/20/14.
 */
public class JArraysTest {

    @Test
    public void toListNullArray() {
        assertEquals(new ArrayList<JArraysTest>(), JArrays.toArrayList(null));
    }

    @Test
    public void toListEmptyArray() {
        assertEquals(new ArrayList<JArraysTest>(), JArrays.toArrayList(new JArraysTest[0]));
    }

    @Test
    public void toListArrayWithElement() {
        String[] array = new String []  {"1", "2", "3"};
        assertEquals(new ArrayList<String>(Arrays.asList(array)), JArrays.toArrayList(array));
    }
}
