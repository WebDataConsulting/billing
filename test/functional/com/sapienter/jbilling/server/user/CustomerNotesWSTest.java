package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.CustomerNoteWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Calendar;

import static org.testng.AssertJUnit.*;

@Test(groups = { "web-services", "notes" })
public class CustomerNotesWSTest {

    @Test
    public void test001CreationAndRetrievalCustomerWithMultipleNotes() throws Exception{
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer userId = null;
        try {
            System.out.println("Test001 Creation And Retrieval Customer With Multiple Notes");

            // user for tests
            UserWS user = createUserWith2Notes(true, null, null); // 2 Notes  are added while creating new user

            userId = user.getUserId();
            UserWS ret = api.getUserWS(userId);
            assertEquals(user.getUserId(), ret.getUserId());
            //If note Id Present that means not has been successfully created
            assertNotSame(0,ret.getCustomerNotes()[0].getNoteId());
            assertNotSame(0,ret.getCustomerNotes()[1].getNoteId());
            //Verify Note Data
            assertEquals("Note Title Verification 1",user.getCustomerNotes()[0].getNoteTitle(),ret.getCustomerNotes()[0].getNoteTitle());
            assertEquals("Note Title Verification 2",user.getCustomerNotes()[1].getNoteTitle(),ret.getCustomerNotes()[1].getNoteTitle());
        } finally {
            if (userId != null) api.deleteUser(userId);
        }
    }
    @Test
    public void test002AddNotesToOldUser() throws Exception{
        JbillingAPI api = JbillingAPIFactory.getAPI();

        System.out.println("Test 002 Add Notes To Old User");
        Integer userId = null;
        try {
            // user for tests
            UserWS user = createUserWith2Notes(true, null, null); // 2 Notes are added while creating new user
            userId = user.getUserId();
            UserWS ret = api.getUserWS(userId);
            int noOfNotesBeforeUpdate=ret.getCustomerNotes().length;
            System.out.println("User Id Test Case 2"+userId);
            //NOTE 3
            CustomerNoteWS customerNoteWS3 = new CustomerNoteWS();
            customerNoteWS3.setNoteTitle("Test Notes 3");
            customerNoteWS3.setNoteContent("Test Note 3");
            customerNoteWS3.setUserId(74);
            //NOTE 4
            CustomerNoteWS customerNoteWS4 = new CustomerNoteWS();
            customerNoteWS4.setNoteTitle("Test Notes 4");
            customerNoteWS4.setNoteContent("Test Note Content 4");
            customerNoteWS4.setUserId(74);
            //Multiple Notes
            ret.setCustomerNotes(new CustomerNoteWS[]{customerNoteWS3, customerNoteWS4});
            ret.setPassword(null);
            api.updateUser(ret);
            //Getting user after adding Notes
            ret = api.getUserWS(userId);
            assertEquals(user.getUserId(), ret.getUserId());
            //Check note count increase
            assertEquals(true,noOfNotesBeforeUpdate<ret.getCustomerNotes().length);
        } finally {
            if (userId != null) api.deleteUser(userId);
        }
    }

    @Test
    public void test003TestAttemptToDeleteNotes()throws Exception
    {   JbillingAPI api = JbillingAPIFactory.getAPI();

        System.out.println("Test 003 Test Attempt To Delete Notes");
        Integer userId = null;
        try {
            // user for tests
            UserWS user = createUserWith2Notes(true, null, null); // 2 Notes are added while creating new user
            userId = user.getUserId();
            UserWS ret = api.getUserWS(userId);
            int noOfNotesBeforeDelete=ret.getCustomerNotes().length;
            ret.setCustomerNotes(new CustomerNoteWS[]{});
            ret.setPassword(null);
            api.updateUser(ret);
            UserWS ret2=api.getUserWS(userId);
            //Check no of notes before and after delete attempt
            assertEquals(noOfNotesBeforeDelete,ret2.getCustomerNotes().length);
            assertEquals(user.getUserId(), ret.getUserId());
        } finally {
            if (userId != null) api.deleteUser(userId);
        }
    }

    @Test
    public void test004UpdateNotesIgnore()throws Exception
    {   JbillingAPI api = JbillingAPIFactory.getAPI();

        System.out.println("Test 004 Server Ignore Notes Update");
        Integer userId = null;
        try{
            // user for tests
            UserWS user = createUserWith2Notes(true, null, null); // 2 Notes are added while creating new user
            userId = user.getUserId();
            UserWS ret = api.getUserWS(userId);
            int noOfNotesBeforeDelete=ret.getCustomerNotes().length;
            UserWS ret2=api.getUserWS(userId);
            CustomerNoteWS[] customerNoteWSes = new CustomerNoteWS[] { ret2.getCustomerNotes()[0], ret2.getCustomerNotes()[1] };
            customerNoteWSes[0].setNoteTitle("################################");
            ret2.setCustomerNotes(customerNoteWSes);
            ret2.setPassword(null);
            api.updateUser(ret2);
            ret = api.getUserWS(userId);
            //Check that server ignore Note Update
            assertNotSame(ret2.getCustomerNotes()[0].getNoteTitle(),ret.getCustomerNotes()[0].getNoteTitle());
            assertEquals(user.getUserId(), ret.getUserId());
        } finally {
            if (userId != null) api.deleteUser(userId);
        }
    }

    @Test
    public void test005UpdateDeleteNotesIgnore()throws Exception
    {   JbillingAPI api = JbillingAPIFactory.getAPI();

        System.out.println("Test 004 Add Notes To Old User");
        Integer userId = null;
        try {
            // user for tests
            UserWS user = createUserWith2Notes(true, null, null); // 2 Notes are added while creating new user
            userId = user.getUserId();
            UserWS ret = api.getUserWS(userId);
            UserWS ret2=api.getUserWS(userId);
	        ret2.setPassword(null);
            //ret2.getCustomerNotes()[0].
            CustomerNoteWS[] customerNoteWSes = new CustomerNoteWS[] { ret2.getCustomerNotes()[0], ret2.getCustomerNotes()[1] };
            customerNoteWSes[0].setNoteTitle("################################");
            ret2.setCustomerNotes(customerNoteWSes);
            ret2.setPassword(null);
            
            api.updateUser(ret2);
            ret = api.getUserWS(userId);
            //Check that server ignore Note Update
            assertNotSame(ret2.getCustomerNotes()[0].getNoteTitle(),ret.getCustomerNotes()[0].getNoteTitle());
            assertEquals(user.getUserId(), ret.getUserId());
            int noOfNotesBeforeDelete=ret.getCustomerNotes().length;
            ret.setCustomerNotes(new CustomerNoteWS[]{});
            ret.setPassword(null);
            api.updateUser(ret);
            ret2=api.getUserWS(userId);
            //Check no of notes before and after delete attempt
            assertEquals(noOfNotesBeforeDelete,ret2.getCustomerNotes().length);
        } finally {
            if (userId != null) api.deleteUser(userId);
        }
    }

    public static UserWS createUserWith2Notes(boolean goodCC, Integer parentId, Integer currencyId) throws JbillingAPIException, IOException {
        return createUserWith2Notes(goodCC, parentId, currencyId, true);
    }

    public static UserWS createUserWith2Notes(boolean goodCC, Integer parentId, Integer currencyId, boolean doCreate) throws JbillingAPIException, IOException {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        /*
        * Create - This passes the password validation routine.
        */
        UserWS newUser = new UserWS();

        newUser.setUserId(0); // it is validated
        newUser.setUserName("testUserName-" + Calendar.getInstance().getTimeInMillis());
        // Changed the password value as per the complexity rule.
        newUser.setPassword("P@ssword1");
        newUser.setLanguageId(new Integer(1));
        newUser.setMainRoleId(new Integer(5));
        newUser.setAccountTypeId(Integer.valueOf(1));
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setInvoiceChild(new Boolean(false));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(1);
        
        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("Frodo");
        metaField4.setGroupId(1);
        
        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("Baggins");
        metaField5.setGroupId(1);

        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4,
                metaField5
        });

        // valid credit card must have a future expiry date to be valid for payment processing
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
        
        // add a credit card
        PaymentInformationWS cc = com.sapienter.jbilling.server.user.WSTest.
        		createCreditCard("Frodo Baggins", goodCC ? "4111111111111152" : "4111111111111111",
				expiry.getTime());
        
        newUser.getPaymentInstruments().add(cc);
        //NOTE 1
        CustomerNoteWS customerNoteWS1 = new CustomerNoteWS();
        customerNoteWS1.setNoteTitle("Test Notes1222");
        customerNoteWS1.setNoteContent("Test Note Content2222");
        customerNoteWS1.setUserId(74);
        customerNoteWS1.setEntityId(1);
        //NOTE 2
        CustomerNoteWS customerNoteWS2 = new CustomerNoteWS();
        customerNoteWS2.setNoteTitle("Test Notes2222");
        customerNoteWS2.setNoteContent("Test Note Content2222");
        customerNoteWS2.setUserId(74);
        customerNoteWS2.setEntityId(1);
        //Multiple Notes
        newUser.setCustomerNotes(new CustomerNoteWS[]{customerNoteWS1, customerNoteWS2});
        System.out.println("Creating New User");
        if (doCreate) {
            System.out.println("Creating user ...");
            newUser.setUserId(api.createUser(newUser));
        }
        return newUser;
    }
}
