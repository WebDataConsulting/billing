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

/*
 * Created on Dec 18, 2003
 *
 */
package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.JBillingTestUtils;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import java.io.IOException;
import java.lang.System;
import java.lang.Exception;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Arrays;

import org.joda.time.DateTime;

import com.sapienter.jbilling.test.Asserts;

import org.testng.annotations.Test;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Khobab
 */
@Test(groups = { "web-services", "ait-timeline" })
public class AITTimelineTest {

    @Test
    public void test001CreateCustomerWithAitMetaFields_DefaultDate() throws Exception {
        System.out.println("#test001CreateCustomerWithAitMetaFields_DefaultDate");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer userId = null;
        try {
            UserWS newUser = createUser();
            
            System.out.println("Setting Ait fields values for date:" + CommonConstants.EPOCH_DATE);
            MetaFieldValueWS metaField1 = new MetaFieldValueWS();
            metaField1.setFieldName("partner.prompt.fee");
            metaField1.setValue("serial-from-ws");

            MetaFieldValueWS metaField2 = new MetaFieldValueWS();
            metaField2.setFieldName("ccf.payment_processor");
            metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor
            
            String email = newUser.getUserName() + "@shire.com";
            String firstName = "Frodo";
            String lastName = "Baggins";
            
            MetaFieldValueWS metaField3 = new MetaFieldValueWS();
            metaField3.setFieldName("contact.email");
            metaField3.setValue(email);
            metaField3.setGroupId(1);

            MetaFieldValueWS metaField4 = new MetaFieldValueWS();
            metaField4.setFieldName("contact.first.name");
            metaField4.setValue(firstName);
            metaField4.setGroupId(1);

            MetaFieldValueWS metaField5 = new MetaFieldValueWS();
            metaField5.setFieldName("contact.last.name");
            metaField5.setValue(lastName);
            metaField5.setGroupId(1);
            
            newUser.setMetaFields(new MetaFieldValueWS[]{
                    metaField1,
                    metaField2,
                    metaField3,
                    metaField4,
                    metaField5
            });
            
            System.out.println("Setting default date as the only date of timeline");
            ArrayList<Date> timelineDates = new ArrayList<Date>(0);
            timelineDates.add(CommonConstants.EPOCH_DATE);
            newUser.getTimelineDatesMap().put(new Integer(1), timelineDates);
            
            System.out.println("Creating user ...");
            userId = api.createUser(newUser);
            newUser.setUserId(userId);
            
            System.out.println("Getting created user");
            UserWS ret = api.getUserWS(newUser.getUserId());
            
            ArrayList<MetaFieldValueWS> aitMetaFields = ret.getAccountInfoTypeFieldsMap().get(new Integer(1)).get(CommonConstants.EPOCH_DATE);
            
            // check that timeline contains default date
            if(!ret.getAccountInfoTypeFieldsMap().get(new Integer(1)).containsKey(CommonConstants.EPOCH_DATE)) {
            	fail("Default date: " + CommonConstants.EPOCH_DATE + " should be present in timeline");
            }
            
            // Total no of ait meta fields returned should be 18, no of meta fields for ait = 1, only 3 has values
            assertEquals(3, aitMetaFields.size());
            
            // ait meta fields should be 11. Customer Specific: 8 (2 has value). Ait meta field for effective date: 18 (3 has values)
            assertEquals(5, ret.getMetaFields().length);

            // assert that email, first name and last name has same values
            for(MetaFieldValueWS ws : aitMetaFields) {
            	if(ws.getFieldName().equals("contact.email")) {
            		assertEquals("Email should be same", email, ws.getValue());
            	} else if(ws.getFieldName().equals("contact.first.name")) {
            		assertEquals("First name should be same", firstName, ws.getValue());
            	} else if(ws.getFieldName().equals("contact.last.name")) {
            		assertEquals("Last name should be same", lastName, ws.getValue());
            	}
            }
            

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        } finally {
            System.out.println("Deleting created user");
            if (userId != null) api.deleteUser(userId);
        }
    }
    
    @Test
    public void test002CreateCustomerWithAitMetaFields_DefaultAndTodaysDate() throws Exception {
        System.out.println("#test002CreateCustomerWithAitMetaFields_DefaultAndTodaysDate");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer userId = null;
        try {

            UserWS newUser = createUser();
            
            System.out.println("Setting Ait fields values for date:" + CommonConstants.EPOCH_DATE);
            MetaFieldValueWS metaField1 = new MetaFieldValueWS();
            metaField1.setFieldName("partner.prompt.fee");
            metaField1.setValue("serial-from-ws");

            MetaFieldValueWS metaField2 = new MetaFieldValueWS();
            metaField2.setFieldName("ccf.payment_processor");
            metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor
            
            String email = newUser.getUserName() + "@shire.com";
            
            MetaFieldValueWS metaField3 = new MetaFieldValueWS();
            metaField3.setFieldName("contact.email");
            metaField3.setValue(email);
            metaField3.setGroupId(1);
            
            newUser.setMetaFields(new MetaFieldValueWS[]{
                    metaField1,
                    metaField2,
                    metaField3
            });
            
            Date today = new DateTime().toDateMidnight().toDate();
            System.out.println("Setting default date and todays date on timeline");
            ArrayList<Date> timelineDates = new ArrayList<Date>(0);
            timelineDates.add(CommonConstants.EPOCH_DATE);
            timelineDates.add(today);
            newUser.getTimelineDatesMap().put(new Integer(1), timelineDates);
            
            System.out.println("Creating user ...");
            userId = api.createUser(newUser);
            newUser.setUserId(userId);
            
            System.out.println("Getting created user");
            UserWS ret = api.getUserWS(newUser.getUserId());
            
            // check that timeline contains default date and today's date
            if(!ret.getAccountInfoTypeFieldsMap().get(new Integer(1)).containsKey(CommonConstants.EPOCH_DATE)) {
            	fail("Default date: " + CommonConstants.EPOCH_DATE + " should be present in timeline");
            }
            if(!ret.getAccountInfoTypeFieldsMap().get(new Integer(1)).containsKey(today)) {
            	fail("Default date: " + today + " should be present in timeline");
            }
            
            // verify meta fields for default date
            ArrayList<MetaFieldValueWS> aitMetaFields = ret.getAccountInfoTypeFieldsMap().get(new Integer(1)).get(CommonConstants.EPOCH_DATE);
            
            // Total no of ait meta fields returned should be 18, no of meta fields for ait = 1, but only 1 has a value
            assertEquals(1, aitMetaFields.size());
            
            // assert that email, first name and last name has same values
            for(MetaFieldValueWS ws : aitMetaFields) {
            	if(ws.getFieldName().equals("contact.email")) {
            		assertEquals("Email should be same", email, ws.getValue());
            	}
            }
            
            // verify meta fields for todays date
            aitMetaFields = ret.getAccountInfoTypeFieldsMap().get(new Integer(1)).get(today);
            
            // Total no of ait meta fields returned should be 18, no of meta fields for ait = 1  , but only 1 has a value
            assertEquals(1, aitMetaFields.size());
            
            // assert that email, first name and last name has same values
            for(MetaFieldValueWS ws : aitMetaFields) {
            	if(ws.getFieldName().equals("contact.email")) {
            		assertEquals("Email should be same", email, ws.getValue());
            	}
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        } finally {
            System.out.println("Deleting created user");
            api.deleteUser(userId);
        }
    }
    
    @Test
    public void test003CreateCustomerWithAitMetaFields_UpdateTodaysFields() throws Exception {
        System.out.println("#test003CreateCustomerWithAitMetaFields_UpdateTodaysFields");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer userId = null;
        try {

            UserWS newUser = createUser();
            System.out.println("Setting Ait fields values for date:" + CommonConstants.EPOCH_DATE);
            MetaFieldValueWS metaField1 = new MetaFieldValueWS();
            metaField1.setFieldName("partner.prompt.fee");
            metaField1.setValue("serial-from-ws");

            MetaFieldValueWS metaField2 = new MetaFieldValueWS();
            metaField2.setFieldName("ccf.payment_processor");
            metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor
            
            MetaFieldValueWS metaField3 = new MetaFieldValueWS();
            metaField3.setFieldName("contact.email");
            metaField3.setValue("abc@xyz.com");
            metaField3.setGroupId(1);
            
            newUser.setMetaFields(new MetaFieldValueWS[]{
                    metaField1,
                    metaField2,
                    metaField3
            });
            
            Date today = new DateTime().toDateMidnight().toDate();
            System.out.println("Setting default date and todays date on timeline");
            ArrayList<Date> timelineDates = new ArrayList<Date>(0);
            timelineDates.add(CommonConstants.EPOCH_DATE);
            timelineDates.add(today);
            newUser.getTimelineDatesMap().put(new Integer(1), timelineDates);
            
            System.out.println("Creating user ...");
            userId = api.createUser(newUser);
            newUser.setUserId(userId);
            
            System.out.println("Getting created user");
            UserWS ret = api.getUserWS(newUser.getUserId());
            
            // Update todays ait meta field value for the created users
            metaField3 = new MetaFieldValueWS();
            metaField3.setFieldName("contact.email");
            metaField3.setValue("xyz@abc.com");
            metaField3.setGroupId(1);
            
            ret.setMetaFields(new MetaFieldValueWS[]{
                    metaField1,
                    metaField2,
                    metaField3
            });
            
            ret.getEffectiveDateMap().put(1, today);
            api.updateUser(ret);
            
            // get updated user
            ret = api.getUserWS(newUser.getUserId());
            
            // check that timeline contains default date and today's date
            if(!ret.getAccountInfoTypeFieldsMap().get(new Integer(1)).containsKey(CommonConstants.EPOCH_DATE)) {
            	fail("Default date: " + CommonConstants.EPOCH_DATE + " should be present in timeline");
            }
            
            if(!ret.getAccountInfoTypeFieldsMap().get(new Integer(1)).containsKey(today)) {
            	fail("Default date: " + today + " should be present in timeline");
            }
            
            
            // verify meta fields for default date
            ArrayList<MetaFieldValueWS> aitMetaFields = ret.getAccountInfoTypeFieldsMap().get(new Integer(1)).get(CommonConstants.EPOCH_DATE);

            // Total no of ait meta fields returned should be 1, no of meta fields for ait 1 is 18, but only
            //one has a value
            assertEquals(1, aitMetaFields.size());
            
            // assert that email, first name and last name has same values
            for(MetaFieldValueWS ws : aitMetaFields) {
            	if(ws.getFieldName().equals("contact.email")) {
            		assertEquals("Email should be same", "abc@xyz.com", ws.getValue());
            	}
            }
            
            // verify meta fields for todays date
            aitMetaFields = ret.getAccountInfoTypeFieldsMap().get(new Integer(1)).get(today);
            
            // Total no of ait meta fields returned should be 1, no of meta fields for ait 1 is 18, but only
            //one has a value
            assertEquals(1, aitMetaFields.size());
            
            // assert that email, first name and last name has same values
            for(MetaFieldValueWS ws : aitMetaFields) {
            	if(ws.getFieldName().equals("contact.email")) {
            		assertEquals("Email should be same", "xyz@abc.com", ws.getValue());
            	}
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        } finally {
            // Change
            System.out.println("Deleting created user");
            if (userId != null) api.deleteUser(userId);

        }
    }
    
    public static UserWS createUser() throws JbillingAPIException,
			IOException {
		JbillingAPI api = JbillingAPIFactory.getAPI();

		
		// Create - This passes the password validation routine.
		 
		UserWS newUser = new UserWS();
		newUser.setUserId(0); // it is validated
		newUser.setUserName("testUserName-"
				+ Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("P@ssword1");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(Integer.valueOf(1));
		newUser.setParentId(null); // this parent exists
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(new Integer(1));
		newUser.setInvoiceChild(new Boolean(false));

		return newUser;
	}
}
