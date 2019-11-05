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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.Asserts;
import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormat;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.testng.AssertJUnit.*;


/**
 * @author Emil
 */
@Test(groups = { "web-services", "user" }, testName = "user.WSTest")
public class WSTest {

	private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	private final static int CC_PM_ID = 1;
	private final static int ACH_PM_ID = 2;
	private final static int CHEQUE_PM_ID = 3;
    private static final Integer ROOT_ENTITY_ID = 1;

	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static String CC_MF_TYPE = "cc.type";
	
	private final static String ACH_MF_ROUTING_NUMBER = "ach.routing.number";
	private final static String ACH_MF_BANK_NAME = "ach.bank.name";
	private final static String ACH_MF_CUSTOMER_NAME = "ach.customer.name";
	private final static String ACH_MF_ACCOUNT_NUMBER = "ach.account.number";
	private final static String ACH_MF_ACCOUNT_TYPE = "ach.account.type";
	
	private final static String CHEQUE_MF_BANK_NAME = "cheque.bank.name";
	private final static String CHEQUE_MF_DATE = "cheque.date";
	private final static String CHEQUE_MF_NUMBER = "cheque.number";

	@Test
	public void test001GetUser() throws Exception {
		System.out.println("#test001GetUser");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS userCreated = null;
		try {
            userCreated = createUser(true, null, null);
            System.out.println("Getting user " + userCreated.getId());
			UserWS ret = api.getUserWS(new Integer(userCreated.getId()));
			assertEquals(userCreated.getId(), ret.getUserId());
			try {
				System.out.println("Getting invalid user 13");
				ret = api.getUserWS(new Integer(13));
				fail("Shouldn't be able to access user 13");
			} catch (Exception e) {}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (userCreated != null) api.deleteUser(userCreated.getId());
        }
    }

	@Test
	public void test002MultipleUserWithSameUserNameCreation() throws Exception {
		System.out.println("#test002MultipleUserWithSameUserNameCreation");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS parentUser = null;
        UserWS user = null;
        try {
            String usernameForTest = "sameUserName" + new Date().getTime();
            parentUser = createParent(api);
            user = createUser(true, true, parentUser.getId(), null, false);
			user.setUserName(usernameForTest);
			try {
				System.out.println("Creating the first user...");
				user.setUserId(api.createUser(user));
				System.out
						.println("No exception is thrown because the username is not in use.");
			} catch (SessionInternalError e) {
				System.out.println(e.getMessage());
				fail("No error should ocurr");
			}

			System.out
					.println("Creating the second user with the same username as the first one...");
			UserWS user2 = createUser(true, true, parentUser.getId(), null, false);
			user2.setUserName(usernameForTest);
			try {
                api.createUser(user2);
			} catch (SessionInternalError e) {
				System.out
						.println("A SessionInternalError occurs because the username is already in use.");
				assertEquals("One error should ocurr", 1,
						e.getErrorMessages().length);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (user != null) api.deleteUser(user.getId());
            if (parentUser != null) api.deleteUser(parentUser.getId());
        }
    }

	public void testUserWithParentIdSameAsOwnUserId() {
		try {
			JbillingAPI api = JbillingAPIFactory.getAPI();

			// create our test user
			UserWS user = createUser(true, true, null, null, false);
			try {
				System.out.println("Creating the test user...");
				user = api.getUserWS(api.createUser(user));
				System.out.println("No exception is thrown.");
			} catch (SessionInternalError e) {
				System.out.println(e.getMessage());
				fail("No error should ocurr");
			}

			// Set the parent id same as user's own id.
			user.setParentId(user.getUserId());

			try {
				// invoke update of user after making parent id and user id as
				// same.
                user.setPassword(null);
				api.updateUser(user);
			} catch (SessionInternalError e) {
				System.out
						.println("A SessionInternalError occurs because the parent id set is the same as user's own id.");
				assertEquals("One error should ocurr", 1,
						e.getErrorMessages().length);
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		}
	}


//	  public void testOwingBalance() { try { JbillingAPI api =
//	  JbillingAPIFactory.getAPI();
//
//	  System.out.println("Getting balance of user 2"); UserWS ret =
//	  api.getUserWS(new Integer(2));
//	  assertEquals("Balance of Gandlaf starts at 1377287.98", new
//	  BigDecimal("1377287.98"), ret.getOwingBalanceAsDecimal());
//	  System.out.println("Gandalf's balance: " + ret.getOwingBalance());
//
//	  } catch (Exception e) { e.printStackTrace(); fail("Exception caught:" +
//	  e); } }
//

	@Test
	public void test003CreateUpdateDeleteUser() throws IOException, JbillingAPIException {
		System.out.println("#test003CreateUpdateDeleteUser");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS parentCreated = null;
        UserWS newUser = null;
        
        try {

			// check that the validation works
			UserWS badUser = createUser(true, true, null, null, false);
			// create: the user id has to be 0
			badUser.setUserId(99);
			try {
				api.createUser(badUser);
			} catch (SessionInternalError e) {
				assertEquals("One error", 1, e.getErrorMessages().length);
				assertEquals("Error message",
						"UserWS,id,validation.error.max,0",
						e.getErrorMessages()[0]);
			}

			// now add the wrong user name
			badUser.setUserName("123");
			try {
				api.createUser(badUser);
			} catch (SessionInternalError e) {
				assertEquals("Two errors", 2, e.getErrorMessages().length);
				assertTrue(
						"Error message",
						"UserWS,userName,validation.error.size,5,50"
								.compareTo(e.getErrorMessages()[0]) == 0
								|| "UserWS,userName,validation.error.size,5,50"
										.compareTo(e.getErrorMessages()[1]) == 0);
			}

			// update: the user id has to be more 0
			badUser.setUserId(0);
			badUser.setUserName("12345"); // bring it back to at least 5 length
			try {
	            badUser.setPassword(null);
	            api.updateUser(badUser);
			} catch (SessionInternalError e) {
				assertEquals("One error", 1, e.getErrorMessages().length);
				assertEquals("Error message",
						"UserWS,id,validation.error.min,1",
						e.getErrorMessages()[0]);
			}

			// now add the wrong user name
			badUser.setUserName("123");
			badUser.setUserId(1); // reset so we can test the name validator
			try {
				badUser.setPassword(null);
				api.updateUser(badUser);
			} catch (SessionInternalError e) {
				assertEquals("Two errors", 1, e.getErrorMessages().length);
				assertTrue("Error message",
						"UserWS,userName,validation.error.size,5,50".equals(e
								.getErrorMessages()[0]));
			}

			System.out.println("Validation tested");

			// Create - This passes the password validation routine.
            parentCreated = createParent(api);

            newUser = createUser(true,parentCreated.getId(), null);
			Integer newUserId = newUser.getUserId();
			String newUserName = newUser.getUserName();
			assertNotNull("The user was not created", newUserId);

			System.out.println("Getting the id of the new user: " + newUserName);
			Integer ret = api.getUserId(newUserName);
			assertEquals("Id of new user found", newUserId, ret);

			// verify the created user
			System.out.println("Getting created user " + newUserId);
			UserWS retUser = api.getUserWS(newUserId);
			PaymentInformationWS instrument = retUser.getPaymentInstruments().iterator().next();

			assertEquals("created username", retUser.getUserName(),
					newUser.getUserName());
			assertEquals("create user parent id", new Integer(parentCreated.getId()),
					retUser.getParentId());
			System.out.println("My user: " + retUser);

			assertEquals(
					"created credit card name",
					"Frodo Baggins", getMetaField(instrument.getMetaFields(), CC_MF_CARDHOLDER_NAME).getStringValue());

            //  Make a create mega call

            System.out.println("Making mega call");
            retUser.setUserName("MU"
                    + Long.toHexString(System.currentTimeMillis()));
            // need to reset the password, it came encrypted
            // let's use a long one

            // 2014-11-04 Igor Poteryaev. commented out
            // can't change password here, because of constraint for new passwords only once per day
            // retUser.setPassword("0fu3js8wl1;a$e2w)xRQ");

            // the new user shouldn't be a child
            retUser.setParentId(null);

            // need an order for it
            OrderWS newOrder = getOrder();

            retUser.setUserId(0);
            retUser.setPassword("P@ssword1");
            CreateResponseWS mcRet = api.create(retUser, newOrder,
                    OrderChangeBL.buildFromOrder(newOrder, ORDER_CHANGE_STATUS_APPLY_ID));

            System.out.println("Validating new invoice");
            // validate that the results are reasonable
            assertNotNull("Mega call result can't be null", mcRet);
            assertNotNull("Mega call invoice result can't be null",
                    mcRet.getInvoiceId());
            // there should be a successfull payment
            assertEquals("Payment result OK", true, mcRet.getPaymentResult()
                    .getResult().booleanValue());
            assertEquals("Processor code", "fake-code-default", mcRet
                    .getPaymentResult().getCode1());
            // get the invoice
            InvoiceWS retInvoice = api.getInvoiceWS(mcRet.getInvoiceId());
            assertNotNull("New invoice not present", retInvoice);
            com.sapienter.jbilling.test.Asserts.assertEquals("Balance of invoice should be zero, is paid",
                    new BigDecimal("0.00"), retInvoice.getBalanceAsDecimal());
            com.sapienter.jbilling.test.Asserts.assertEquals("Total of invoice should be total of order",
                    new BigDecimal("20.00"), retInvoice.getTotalAsDecimal());
            assertEquals("New invoice paid", retInvoice.getToProcess(),
                    new Integer(0));

            // TO-DO test that the invoice total is equal to the order total

            // Update

            // now update the created user
            System.out.println("Updating user - Pass 1 - Should succeed");
            retUser = api.getUserWS(newUserId);
            retUser.setCreditLimit(new BigDecimal("112233.0"));
            System.out.println("Updating user...");
            updateMetaField(retUser.getPaymentInstruments().iterator().next()
                    .getMetaFields(), CC_MF_NUMBER, "4111111111111152");
            retUser.setPassword(null);
            api.updateUser(retUser);

            // and ask for it to verify the modification
            System.out.println("Getting updated user ");
            retUser = api.getUserWS(newUserId);
            assertNotNull("Didn't get updated user", retUser);

            assertEquals(
                    "Credit card updated",
                    "4111111111111152",
                    getMetaField(
                            retUser.getPaymentInstruments().iterator().next()
                                    .getMetaFields(), CC_MF_NUMBER)
                            .getStringValue());
            com.sapienter.jbilling.test.Asserts.assertEquals("credit limit updated", new BigDecimal("112233.00"),
                    retUser.getCreditLimitAsDecimal());

            // credit card is no longer implemented
            // retUser.setCreditCard(null);
            // call the update
            retUser.setPassword(null); // should not change the password
            api.updateUser(retUser);
            // fetch the user
            UserWS updatedUser = api.getUserWS(newUserId);
            // credit card functionality has been swapped by payment instrument
            // assertEquals("Credit card should stay the same",
            // "4111111111111152",
            // updatedUser.getCreditCard().getNumber());

            System.out.println("Update result:" + updatedUser);

            // update credit card details
            System.out.println("Removing first payment method");
            // credit card functionality is no longer available this way, you
            // have to remove a payment information manually
            // api.updateCreditCard(newUserId, null);
            api.removePaymentInstrument(updatedUser.getPaymentInstruments()
                    .iterator().next().getId());
            // get updated user with removed payment instrument
            updatedUser = api.getUserWS(newUserId);
            assertEquals("Credit card removed", (int) new Integer(0), (int) updatedUser.getPaymentInstruments().size());

            System.out.println("Creating credit card");
            String ccName = "New ccName";
            String ccNumber = "4012888888881881";
            Date ccExpiry = Util.truncateDate(Calendar.getInstance().getTime());

            PaymentInformationWS newCC = createCreditCard(ccName, ccNumber,
                    ccExpiry);
            updatedUser.getPaymentInstruments().add(newCC);
            updatedUser.setPassword(null);
            api.updateUser(updatedUser);

            // check updated cc details
            retUser = api.getUserWS(newUserId);
            PaymentInformationWS retCc = retUser.getPaymentInstruments()
                    .iterator().next();
            assertEquals("new cc name", ccName,
                    getMetaField(retCc.getMetaFields(), CC_MF_CARDHOLDER_NAME)
                            .getStringValue());
            assertEquals("updated cc number", ccNumber,
                    getMetaField(retCc.getMetaFields(), CC_MF_NUMBER)
                            .getStringValue());
            assertEquals("updated cc expiry", DateTimeFormat.forPattern(
							ServerConstants.CC_DATE_FORMAT).print(ccExpiry.getTime()),
                    getMetaField(retCc.getMetaFields(), CC_MF_EXPIRY_DATE)
                            .getStringValue());

            // set the credit card ID so that we update the existing card with
            // the API call
            newCC.setId(retCc.getId());

            // following functionality is not part of design anymore

            // try and update the card details ignoring the credit card number
            // System.out.println("Updating credit card");
            // cc.setName("Updated ccName");
            // cc.setNumber(null);
            // api.updateCreditCard(newUserId, cc);
            // retUser = api.getUserWS(newUserId);
            // assertEquals("updated cc name", "Updated ccName",
            // retUser.getCreditCard().getName());
            // assertNotNull("cc number still there",
            // retUser.getCreditCard().getNumber());

            // try to update cc of user from different company
            // System.out.println("Attempting to update cc of a user from "
            // + "a different company");
            // try {
            // api.updateCreditCard(new Integer(13), cc);
            // fail("Shouldn't be able to update cc of user 13");
            // } catch (Exception e) {
            // }


            // Delete

            // now delete this new guy
            System.out.println("Deleting user..." + newUserId);
            api.deleteUser(newUserId);

            // try to fetch the deleted user
            System.out.println("Getting deleted user " + newUserId);
            updatedUser = api.getUserWS(newUserId);
            assertEquals(updatedUser.getDeleted(), 1);

            // verify I can't delete users from another company
            try {
                System.out.println("Deleting user base user ... 13");
                api.getUserWS(new Integer(13));
                fail("Shouldn't be able to access user 13");
            } catch (Exception e) {
            }

            System.out.println("Done");

        } catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (newUser != null) api.deleteUser(newUser.getId());
            if (parentCreated != null) api.deleteUser(parentCreated.getId());
        }
    }

	@Test
	public void test004CreditCardUpdates() throws Exception {
		System.out.println("#test004CreditCardUpdates");
		JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS parentCreated = null;
        UserWS user = null;
        
//		  Note, a more direct test would be to write a unit test for the
//		  CreditCardDTO class itself, but our current testing framework doesn't
//		  support this style. Instead, test CreditCardBL which should the
//		  standard service interface for all credit card interaction.
//		 
//
//		
//		  After implementation of #6215 - account payment information now we
//		  can update each payment information individually and there is not
//		  updating of most recent credit card as now user can enter more than
//		  one credit cards
        try {
        	
            parentCreated = createParent(api);

            user = createUser(true, parentCreated.getId(), null);
            user = api.getUserWS(user.getUserId());

            // Visa
            updateMetaField(user.getPaymentInstruments().iterator().next()
                    .getMetaFields(), CC_MF_NUMBER, "4556737877253135");
            user.setPassword(null);
            api.updateUser(user);

            user = api.getUserWS(user.getUserId());
            PaymentInformationWS card = user.getPaymentInstruments().iterator()
                    .next();
            System.out.println("Updated card " + card.getId());
            assertEquals("card type Visa", ServerConstants.PAYMENT_METHOD_VISA,
                    getMetaField(card.getMetaFields(), CC_MF_TYPE).getIntegerValue());

            // Mastercard
            updateMetaField(card.getMetaFields(), CC_MF_NUMBER, "5111111111111985");
            System.out.println("Updating credit card " + card.getId()
                    + " With a Mastercard number");
            user.setPassword(null);
            api.updateUser(user);

            user = api.getUserWS(user.getUserId());
            card = user.getPaymentInstruments().iterator().next();
            System.out.println("Updated card " + card.getId());
            assertEquals("card type Mastercard",
                    ServerConstants.PAYMENT_METHOD_MASTERCARD,
                    getMetaField(card.getMetaFields(), CC_MF_TYPE).getIntegerValue());

            // American Express
            updateMetaField(card.getMetaFields(), CC_MF_NUMBER, "378949830612125");
            System.out.println("Updating credit card " + card.getId()
                    + " With an American Express number");
            user.setPassword(null);
            api.updateUser(user);

            user = api.getUserWS(user.getUserId());
            card = user.getPaymentInstruments().iterator().next();
            System.out.println("Updated card " + card.getId());
            assertEquals("card type American Express",
                    ServerConstants.PAYMENT_METHOD_AMEX,
                    getMetaField(card.getMetaFields(), CC_MF_TYPE).getIntegerValue());

            // Diners Club
            updateMetaField(card.getMetaFields(), CC_MF_NUMBER, "3611111111111985");
            System.out.println("Updating credit card " + card.getId()
                    + " With a Diners Club number");
            user.setPassword(null);
            api.updateUser(user);

            user = api.getUserWS(user.getUserId());
            card = user.getPaymentInstruments().iterator().next();
            System.out.println("Updated card " + card.getId());
            assertEquals("card type Diners", ServerConstants.PAYMENT_METHOD_DINERS,
                    getMetaField(card.getMetaFields(), CC_MF_TYPE).getIntegerValue());

            // Discovery
            updateMetaField(card.getMetaFields(), CC_MF_NUMBER, "6011874982335947");
            System.out.println("Updating credit card " + card.getId()
                    + " With a Discovery card number");
            user.setPassword(null);
            api.updateUser(user);

            user = api.getUserWS(user.getUserId());
            card = user.getPaymentInstruments().iterator().next();
            System.out.println("Updated card " + card.getId());
            assertEquals("card type Discovery", ServerConstants.PAYMENT_METHOD_DISCOVER,
                    getMetaField(card.getMetaFields(), CC_MF_TYPE).getIntegerValue());
        }
        finally {
            // cleanup
            if (user != null) api.deleteUser(user.getId());
            if (parentCreated != null) api.deleteUser(parentCreated.getId());
        }

	}

    private UserWS createParent(JbillingAPI api) throws JbillingAPIException, IOException {
        UserWS parentCreated = createUser(true, null, null);
        parentCreated.setIsParent(true);
        parentCreated.setPassword(null);
        api.updateUser(parentCreated);
        assertNotNull("The parent user was not created", parentCreated);
        return parentCreated;
    }

    @Test
	public void test005LanguageId() throws Exception {
		System.out.println("#test005LanguageId");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer newUserId = null;
        try {
            UserWS newUser = new UserWS();
			newUser.setUserName("language-test" + new Date().getTime());
			newUser.setPassword("As$fasdf1");
			newUser.setLanguageId(new Integer(2)); // French
			newUser.setMainRoleId(new Integer(5));
			newUser.setAccountTypeId(Integer.valueOf(1));
			newUser.setIsParent(new Boolean(true));
			newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);

			MetaFieldValueWS metaField1 = new MetaFieldValueWS();
			metaField1.setFieldName("contact.email");
			metaField1.setValue(newUser.getUserName() + "@shire.com");
			metaField1.setGroupId(1);

			newUser.setMetaFields(new MetaFieldValueWS[] { metaField1 });

			System.out.println("Creating user ...");
			// do the creation
			newUserId = api.createUser(newUser);

			// get user
			UserWS createdUser = api.getUserWS(newUserId);
			assertEquals("Language id", 2, createdUser.getLanguageId()
					.intValue());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            // clean up
            if (newUserId != null) api.deleteUser(newUserId);
        }
    }

    //TODO: Test commented because I can't understand what it's testing and how
/*	@Test
	public void test006UserTransitions() throws Exception {
		System.out.println("#test006UserTransitions");
        JbillingAPI api = JbillingAPIFactory.getAPI();

        try {
            Date beforeTransitionDates = new Date();
            System.out.println("Getting complete list of user transitions");
            UserWS user = createUser(true, null, null);
            user.setStatusId(6);
            api.updateUser(user);
            UserTransitionResponseWS[] ret = api.getUserTransitions(beforeTransitionDates, new Date());

			if (ret == null)
				fail("Transition list should not be empty!");
			assertEquals(6, ret.length);

			// Check the ids of the returned transitions
			assertEquals(ret[0].getId().intValue(), 1);
			assertEquals(ret[1].getId().intValue(), 2);
			// Check the value of returned data
			assertEquals(ret[0].getUserId().intValue(), 2);
			assertEquals(ret[0].getFromStatusId().intValue(), 2);
			assertEquals(ret[0].getToStatusId().intValue(), 1);
			assertEquals(ret[1].getUserId().intValue(), 2);
			assertEquals(ret[1].getFromStatusId().intValue(), 2);
			assertEquals(ret[1].getToStatusId().intValue(), 1);

			// save an ID for later
			Integer myId = ret[4].getId();

			System.out
					.println("Getting first partial list of user transitions");
			ret = api.getUserTransitions(new Date(2000 - 1900, 0, 0), new Date(
					2007 - 1900, 0, 1));
			if (ret == null)
				fail("Transition list should not be empty!");
			assertEquals(ret.length, 1);

			assertEquals(ret[0].getId().intValue(), 1);
			assertEquals(ret[0].getUserId().intValue(), 2);
			assertEquals(ret[0].getFromStatusId().intValue(), 2);
			assertEquals(ret[0].getToStatusId().intValue(), 1);

			System.out
					.println("Getting second partial list of user transitions");
			ret = api.getUserTransitions(null, null);
			if (ret == null)
				fail("Transition list should not be empty!");
			assertEquals(5, ret.length);

			assertEquals(ret[0].getId().intValue(), 2);
			assertEquals(ret[0].getUserId().intValue(), 2);
			assertEquals(ret[0].getFromStatusId().intValue(), 2);
			assertEquals(ret[0].getToStatusId().intValue(), 1);

			System.out.println("Getting list after id");
			ret = api.getUserTransitionsAfterId(myId);
			if (ret == null)
				fail("Transition list should not be empty!");
			assertEquals("Only one transition after id " + myId, 1, ret.length);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		}
	}*/



//	    Parent 1 10752
//	          |
//	           +----+ ---------+-------+
//	           |    |          |       |
//	   10753 iCh1  Ch2 10754  Ch6     iCh7
//	          /\    |                  |
//	         /  \   |                 Ch8
//	      Ch3 iCh4 Ch5
//	    10755 10756 10757
//
//	   Ch3->Ch1
//	   Ch4->Ch4
//	   Ch1->Ch1
//	   Ch5->P1
//	   Ch2->P1
//	   Ch6->P1
//	   Ch7-> Ch7 (its own one time order)
//	   Ch8: no applicable orders

	@Test
	public void test007ParentChild() throws Exception {
	   System.out.println("#test007ParentChild");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        //  Create - This passes the password validation routine.
        Integer parentId = null;
        List<Integer> childrenToRemove = new ArrayList<Integer>();
        Integer child8Id = null;
        Integer child5Id = null;
        Integer child4Id = null;
        Integer child3Id = null;
        Integer child7Id = null;
        Integer child6Id = null;
        Integer child2Id = null;
        Integer child1Id = null;
	   try {
           UserWS newUser = new UserWS();
	       newUser.setUserName("parent1" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setLanguageId(new Integer(1));
	       newUser.setMainRoleId(new Integer(5));
	       newUser.setAccountTypeId(Integer.valueOf(1));
	       newUser.setIsParent(new Boolean(true));
	       newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);

	       MetaFieldValueWS metaField1 = new MetaFieldValueWS();
	       metaField1.setFieldName("contact.email");
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       metaField1.setGroupId(1);

	       newUser.setMetaFields(new MetaFieldValueWS[]{metaField1});

	       System.out.println("Creating parent user ...");
	       // do the creation
	       parentId = api.createUser(newUser);
	       assertNotNull("The user was not created", parentId);

	       // verify the created user
	       System.out.println("Getting created user ");
	       UserWS retUser = api.getUserWS(parentId);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("create user is parent", new Boolean(true), retUser.getIsParent());

	       System.out.println("Creating child1 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(true));
	       newUser.setParentId(parentId);
	       newUser.setUserName("child1" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.TRUE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child1Id = api.createUser(newUser);
           childrenToRemove.add(child1Id);
	       //test
	       System.out.println("Getting created user ");
	       retUser = api.getUserWS(child1Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", parentId, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.TRUE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(parentId);
	       Integer[] childIds = retUser.getChildIds();
	       assertEquals("1 child", 1, childIds.length);
	       assertEquals("created user child", child1Id, childIds[0]);

	       System.out.println("Creating child2 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(true));
	       newUser.setParentId(parentId);
	       newUser.setUserName("child2" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.FALSE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child2Id = api.createUser(newUser);
           childrenToRemove.add(child2Id);
	       //test
	       System.out.println("Getting created user ");
	       retUser = api.getUserWS(child2Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", parentId, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.FALSE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(parentId);
	       childIds = retUser.getChildIds();
	       assertEquals("2 child", 2, childIds.length);
	       assertEquals("created user child", child2Id,
	               childIds[0].equals(child2Id) ? childIds[0] : childIds[1]);

	       System.out.println("Creating child6 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(true));
	       newUser.setParentId(parentId);
	       newUser.setUserName("child6" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.FALSE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child6Id = api.createUser(newUser);
           childrenToRemove.add(child6Id);
	       //test
	       System.out.println("Getting created user ");
	       retUser = api.getUserWS(child6Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", parentId, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.FALSE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(parentId);
	       childIds = retUser.getChildIds();
	       assertEquals("3 child", 3, childIds.length);
	       assertEquals("created user child", child6Id,
	               childIds[0].equals(child6Id) ? childIds[0] :
	                       childIds[1].equals(child6Id) ? childIds[1] : childIds[2]);

	       System.out.println("Creating child7 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(true));
	       newUser.setParentId(parentId);
	       newUser.setUserName("child7" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.TRUE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child7Id = api.createUser(newUser);
           childrenToRemove.add(child7Id);
	       //test
	       System.out.println("Getting created user ");
	       retUser = api.getUserWS(child7Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", parentId, retUser.getParentId());
	       assertEquals("created invoice child", Boolean.TRUE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(parentId);
	       childIds = retUser.getChildIds();
	       assertEquals("4 child", 4, childIds.length);

	       System.out.println("Creating child8 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(true));
	       newUser.setParentId(child7Id);
	       newUser.setUserName("child8" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.FALSE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child8Id = api.createUser(newUser);
           childrenToRemove.add(child8Id);
	       //test
	       System.out.println("Getting created user ");
	       retUser = api.getUserWS(child8Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", child7Id, retUser.getParentId());
	       assertEquals("created invoice child", Boolean.FALSE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(child7Id);
	       childIds = retUser.getChildIds();
	       assertEquals("1 child", 1, childIds.length);

	       System.out.println("Creating child3 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(false));
	       newUser.setParentId(child1Id);
	       newUser.setUserName("child3" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.FALSE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child3Id = api.createUser(newUser);
           childrenToRemove.add(child3Id);
	       //test
	       System.out.println("Getting created user ");
	       retUser = api.getUserWS(child3Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", child1Id, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.FALSE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(child1Id);
	       childIds = retUser.getChildIds();
	       assertEquals("1 child", 1, childIds.length);
	       assertEquals("created user child", child3Id, childIds[0]);

	       System.out.println("Creating child4 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(false));
	       newUser.setParentId(child1Id);
	       newUser.setUserName("child4" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.TRUE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child4Id = api.createUser(newUser);
           childrenToRemove.add(child4Id);
	       //test
	       System.out.println("Getting created user ");
	       retUser = api.getUserWS(child4Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", child1Id, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.TRUE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(child1Id);
	       childIds = retUser.getChildIds();
	       assertEquals("2 child for child1", 2, childIds.length);
	       assertEquals("created user child", child4Id, childIds[0].equals(child4Id) ? childIds[0] : childIds[1]);

	       System.out.println("Creating child5 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(false));
	       newUser.setParentId(child2Id);
	       newUser.setUserName("child5" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.FALSE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child5Id = api.createUser(newUser);
           childrenToRemove.add(child5Id);
	       //test
	       System.out.println("Getting created user ");
	       retUser = api.getUserWS(child5Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", child2Id, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.FALSE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(child2Id);
	       childIds = retUser.getChildIds();
	       assertEquals("1 child for child2", 1, childIds.length);
	       assertEquals("created user child", child5Id, childIds[0]);

	       // create an order for all these users
	       System.out.println("Creating orders for all users");
	       OrderWS order = getOrder();
	       order.setUserId(parentId);
	       api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	       order = getOrder();
	       order.setUserId(child1Id);
	       api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	       order = getOrder();
	       order.setUserId(child2Id);
	       api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	       order = getOrder();
	       order.setUserId(child3Id);
	       api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	       order = getOrder();
	       order.setUserId(child4Id);
	       api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	       order = getOrder();
	       order.setUserId(child5Id);
	       api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	       order = getOrder();
	       order.setUserId(child6Id);
	       api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	       order = getOrder();
	       order.setUserId(child7Id);
	       api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	       // run the billing process for each user, validating the results
	       System.out.println("Invoicing and validating...");
	       // parent1
	       Integer[] invoices = api.createInvoice(parentId, false);
	       assertNotNull("invoices cant be null", invoices);
	       assertEquals("there should be one invoice", 1, invoices.length);
	       InvoiceWS invoice = api.getInvoiceWS(invoices[0]);
           com.sapienter.jbilling.test.Asserts.assertEquals("invoice should be 80$", new BigDecimal("80.00"), invoice.getTotalAsDecimal());
	       // child1
	       invoices = api.createInvoice(child1Id, false);
	       assertNotNull("invoices cant be null", invoices);
	       assertEquals("there should be one invoice", 1, invoices.length);
	       invoice = api.getInvoiceWS(invoices[0]);
           com.sapienter.jbilling.test.Asserts.assertEquals("invoice should be 40$", new BigDecimal("40.00"), invoice.getTotalAsDecimal());
	       // child2
	       invoices = api.createInvoice(child2Id, false);
	       // CXF returns null for empty arrays
	       if (invoices != null) {
	           assertEquals("there should be no invoice", 0, invoices.length);
	       }
	       // child3
	       invoices = api.createInvoice(child3Id, false);
	       if (invoices != null) {
	           assertEquals("there should be no invoice", 0, invoices.length);
	       }
	       // child4
	       invoices = api.createInvoice(child4Id, false);
	       assertNotNull("invoices cant be null", invoices);
	       assertEquals("there should be one invoice", 1, invoices.length);
	       invoice = api.getInvoiceWS(invoices[0]);
           com.sapienter.jbilling.test.Asserts.assertEquals("invoice should be 20$", new BigDecimal("20.00"), invoice.getTotalAsDecimal());
	       // child5
	       invoices = api.createInvoice(child5Id, false);
	       if (invoices != null) {
	           assertEquals("there should be no invoice", 0, invoices.length);
	       }
	       // child6
	       invoices = api.createInvoice(child6Id, false);
	       if (invoices != null) {
	           assertEquals("there should be one invoice", 0, invoices.length);
	       }
	       // child7 (for bug that would ignore an order from a parent if the
	       // child does not have any applicable)
	       invoices = api.createInvoice(child7Id, false);
	       assertNotNull("invoices cant be null", invoices);
	       assertEquals("there should be one invoice", 1, invoices.length);
	       invoice = api.getInvoiceWS(invoices[0]);
           com.sapienter.jbilling.test.Asserts.assertEquals("invoice should be 20$", new BigDecimal("20.00"), invoice.getTotalAsDecimal());


	   } catch (Exception e) {
	       e.printStackTrace();
	       fail("Exception caught:" + e);
	   } finally {
           // clean up
           deleteWithCheckUser(child8Id, api);
           deleteWithCheckUser(child5Id, api);
           deleteWithCheckUser(child4Id, api);
           deleteWithCheckUser(child3Id, api);
           deleteWithCheckUser(child7Id, api);
           deleteWithCheckUser(child6Id, api);
           deleteWithCheckUser(child2Id, api);
           deleteWithCheckUser(child1Id, api);
           deleteWithCheckUser(parentId, api);
       }

	}

    private void deleteWithCheckUser(Integer userId, JbillingAPI api) {
        if (userId != null) api.deleteUser(userId);
    }
    // todo: Returns 8 records as there are duplicate entries in the
	// user_credit_card_map. Appears to be a bug, fix later!

//	  public void testGetByCC() { // note: this method getUsersByCreditCard
//	  seems to have a bug. It does // not reutrn Gandlaf if there is not an
//	  updateUser call before try { JbillingAPI api =
//	  JbillingAPIFactory.getAPI(); Integer[] ids =
//	  api.getUsersByCreditCard("1152"); assertNotNull("Four customers with CC",
//	  ids); assertEquals("Four customers with CC", 6, ids.length); // returns
//	  credit cards from both clients? // 5 cards from entity 1, 1 card from
//	  entity 2 assertEquals("Created user with CC", 10792, ids[ids.length -
//	  1].intValue());
//
//	  // get the user
//	  assertNotNull("Getting found user",api.getUserWS(ids[0])); } catch
//	  (Exception e) { e.printStackTrace(); fail("Exception caught:" + e); } }
//

	@Test
	public void test008UserMainSubscription() throws Exception {
		System.out.println("#test008UserMainSubscription");
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// now get the user
        Integer userID = createUser(true, null, null).getId();
        try {
            UserWS user = api.getUserWS(userID);
            user.setPassword(null);
            api.updateUser(user);
            MainSubscriptionWS existingMainSubscription = user.getMainSubscription();
            System.out.println("User's existing main subscription = "
                    + existingMainSubscription);

            MainSubscriptionWS newMainSubscription = new MainSubscriptionWS(2, 1);
            user.setNextInvoiceDate(api.getUserWS(userID).getNextInvoiceDate());
            user.setMainSubscription(newMainSubscription);
            System.out.println("User's new main subscription = "
                    + user.getMainSubscription());

            // update the user
            user.setPassword(null);
            api.updateUser(user);

            // validate that the user does have the new main subscription
            assertEquals("User does not have the correct main subscription",
                    newMainSubscription, api.getUserWS(userID)
                            .getMainSubscription());

            // update the user (restore main sub)
            user.setMainSubscription(existingMainSubscription);
            user.setPassword(null);
            api.updateUser(user);
            assertEquals("User does not have the original main subscription",
                    existingMainSubscription, api.getUserWS(userID)
                            .getMainSubscription());
        } finally {
            if (userID != null) api.deleteUser(userID);
        }
	}

	@Test
	public void test009PendingUnsubscription() {
        //TODO: This test have to be changed to not be based on an existing customer
		System.out.println("#test009PendingUnsubscription");
		try {
			JbillingAPI api = JbillingAPIFactory.getAPI();
			OrderWS order = api.getLatestOrder(1055);
			order.setActiveUntil(new Date(2008 - 1900, 11 - 1, 1)); // sorry
			api.updateOrder(order, null);
			assertEquals("User 1055 should be now in pending unsubscription",
					UserDTOEx.SUBSCRIBER_PENDING_UNSUBSCRIPTION,
					api.getUserWS(1055).getSubscriberStatusId());
        } catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		}
	}

	@Test
	public void test010Currency() throws Exception {
		System.out.println("#test010Currency");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer myId = null;
		try {
			UserWS myUser = createUser(true, null, 11);
			myId = myUser.getUserId();
			System.out.println("Checking currency of new user");
			myUser = api.getUserWS(myId);
			assertEquals("Currency should be A$", 11, myUser.getCurrencyId()
					.intValue());
			myUser.setCurrencyId(1);
			System.out.println("Updating currency to US$");
			myUser.setPassword(null); // otherwise it will try the encrypted
										// password
			api.updateUser(myUser);
			System.out.println("Checking currency ...");
			myUser = api.getUserWS(myId);
			assertEquals("Currency should be US$", 1, myUser.getCurrencyId()
					.intValue());
			System.out.println("Removing");
			api.deleteUser(myId);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (myId != null) api.deleteUser(myId);
        }
	}

	@Test
	public void test011PrePaidBalance() {
		System.out.println("#test011PrePaidBalance");
		try {
			JbillingAPI api = JbillingAPIFactory.getAPI();
			UserWS myUser = createUser(true, null, null);
			Integer myId = myUser.getUserId();

			// update to pre-paid
            myUser.setPassword(null);
			api.updateUser(myUser);

			// get the current balance, it should be null or 0
			System.out.println("Checking initial balance type and dynamic balance");
			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have 0 balance", BigDecimal.ZERO,
                    myUser.getDynamicBalanceAsDecimal());

			// validate. room = 0, price = 7
			System.out.println("Validate with fields...");
			PricingField pf[] = { new PricingField("src", "604"),
					new PricingField("dst", "512") };
			ValidatePurchaseWS result = api.validatePurchase(myId, 2800, PricingField.setPricingFieldsValue(pf));
			assertEquals("validate purchase success 1", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 1",
					Boolean.valueOf(false), result.getAuthorized());

            com.sapienter.jbilling.test.Asserts.assertEquals("validate purchase quantity 1", BigDecimal.ZERO, result.getQuantityAsDecimal());
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have 0 balance", BigDecimal.ZERO, myUser.getDynamicBalanceAsDecimal());

			// add a payment
			PaymentWS payment = new PaymentWS();
			payment.setAmount(new BigDecimal("20.00"));
			payment.setIsRefund(new Integer(0));
			payment.setMethodId(ServerConstants.PAYMENT_METHOD_CHEQUE);
			payment.setPaymentDate(Calendar.getInstance().getTime());
			payment.setResultId(ServerConstants.RESULT_ENTERED);
			payment.setCurrencyId(new Integer(1));
			payment.setUserId(myId);

			payment.getPaymentInstruments().add(createCheque("ws bank", "2232-2323-2323", Calendar.getInstance().getTime()));

			System.out.println("Applying payment");
			api.applyPayment(payment, null);
			// check new balance is 20
			System.out.println("Validating new balance");
			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have 20 balance", new BigDecimal("20"), myUser.getDynamicBalanceAsDecimal());

			// now create a one time order, the balance should decrease
			OrderWS order = getOrder();
			order.setUserId(myId);
			System.out.println("creating one time order");
			Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
			System.out.println("Validating new balance");
			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have 0 balance", BigDecimal.ZERO, myUser.getDynamicBalanceAsDecimal());

			// for the following, use line 2 with item id 2. item id 1 has
			// cancellation fees rules that affect the balance.
			// increase the quantity of the one-time order
			System.out.println("adding quantity to one time order");
			pause(2000); // pause while provisioning status is being updated
			order = api.getOrder(orderId);
			OrderLineWS line = order.getOrderLines()[0].getItemId() == 2 ? order
					.getOrderLines()[0] : order.getOrderLines()[1];
			line.setAmount(new BigDecimal("7").multiply(line.getPriceAsDecimal()));
			OrderChangeWS orderChange = OrderChangeBL.buildFromLine(line, null, ORDER_CHANGE_STATUS_APPLY_ID);
			orderChange.setQuantity(BigDecimal.valueOf(7).subtract(line.getQuantityAsDecimal()));
			line.setQuantity(7);

			BigDecimal delta = new BigDecimal("6.00").multiply(line.getPriceAsDecimal());
			api.updateOrder(order, new OrderChangeWS[] { orderChange });
			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have new balance", delta.negate(), myUser.getDynamicBalanceAsDecimal());

			// decrease the quantity of the one-time order
			System.out.println("remove quantity to one time order");
			order = api.getOrder(orderId);
			line = order.getOrderLines()[0].getItemId() == 2 ? order.getOrderLines()[0] : order.getOrderLines()[1];
			orderChange = OrderChangeBL.buildFromLine(line, null, ORDER_CHANGE_STATUS_APPLY_ID);
			orderChange.setQuantity(BigDecimal.valueOf(1).subtract(line.getQuantityAsDecimal()));
			line.setQuantity(1);
			line.setAmount(line.getQuantityAsDecimal().multiply(order.getOrderLines()[1].getPriceAsDecimal()));
			api.updateOrder(order, new OrderChangeWS[] { orderChange });
			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have new balance", BigDecimal.ZERO, myUser.getDynamicBalanceAsDecimal());

			// delete one line from the one time order
			System.out.println("remove one line from one time order");
			order = api.getOrder(orderId);

			List<OrderChangeWS> orderChanges = new LinkedList<OrderChangeWS>();
			for (OrderLineWS orderLine : order.getOrderLines()) {
				if (orderLine.getItemId() != 1) {
					orderChange = OrderChangeBL.buildFromLine(orderLine, null, ORDER_CHANGE_STATUS_APPLY_ID);
					orderChange.setQuantity(orderLine.getQuantityAsDecimal().negate());
					orderLine.setDeleted(1);
					orderChanges.add(orderChange);
				}
			}

			api.updateOrder(order, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]));

			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have new balance", new BigDecimal("10"), myUser.getDynamicBalanceAsDecimal());

			// delete the order, the balance has to go back to 20
			System.out.println("deleting one time order");
			api.deleteOrder(orderId);
			System.out.println("Validating new balance");
			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have 20 balance", new BigDecimal("20"), myUser.getDynamicBalanceAsDecimal());

			// now create a recurring order with invoice, the balance should
			// decrease
			order = getOrder();
			order.setUserId(myId);
			order.setPeriod(2);

			// make it half a month to test pro-rating
			order.setActiveSince(new DateMidnight(2009, 1, 1).toDate());
			order.setActiveUntil(new DateMidnight(2009, 1, 1).plusDays(15).toDate());
			order.setProrateFlag(true);

            OrderChangeWS[] orderChanges2 = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
            for (OrderChangeWS change: orderChanges2) {
                change.setStartDate(order.getActiveSince());
                change.setEndDate(order.getActiveUntil());
            }

			System.out.println("creating recurring order and invoice");
			api.createOrderAndInvoice(order, orderChanges2);
			System.out.println("Validating new balance");
			myUser = api.getUserWS(myId);

            com.sapienter.jbilling.test.Asserts.assertEquals("user should have 10.32 balance (15 out of 31 days)",
                    new BigDecimal("10.32"),
                    myUser.getDynamicBalanceAsDecimal());

			System.out.println("Removing");
			api.deleteUser(myId);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		}
	}

	@Test
	public void test012CreditLimit() throws Exception {
		System.out.println("#test012CreditLimit");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer myId = null;
        try {
			UserWS myUser = createUser(true, null, null);
			myId = myUser.getUserId();

			// update to pre-paid
			myUser.setCreditLimit(new BigDecimal("1000.0"));
            myUser.setPassword(null);
			api.updateUser(myUser);

			// get the current balance, it should be null or 0
			System.out
					.println("Checking initial balance type and dynamic balance");
			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have 0 balance", BigDecimal.ZERO,
                    myUser.getDynamicBalanceAsDecimal());

			// now create a one time order, the balance should increase
			OrderWS order = getOrder();
			order.setUserId(myId);
			System.out.println("creating one time order");
			Integer orderId = api.createOrder(order, OrderChangeBL
					.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
			System.out.println("Validating new balance");
			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have 20 balance", new BigDecimal("-20.0"),
                    myUser.getDynamicBalanceAsDecimal());

			// delete the order, the balance has to go back to 0
			System.out.println("deleting one time order");
			api.deleteOrder(orderId);
			System.out.println("Validating new balance");
			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have 0 balance", BigDecimal.ZERO,
                    myUser.getDynamicBalanceAsDecimal());

			// now create a recurring order with invoice, the balance should
			// increase
			order = getOrder();
			order.setUserId(myId);
			order.setPeriod(2);
			System.out.println("creating recurring order and invoice");
			Integer invoiceId = api.createOrderAndInvoice(order, OrderChangeBL
					.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
			System.out.println("Validating new balance");
			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have 20 balance", new BigDecimal("-20.0"),
                    myUser.getDynamicBalanceAsDecimal());

			// add a payment. I'd like to call payInvoice but it's not finding
			// the CC
			PaymentWS payment = new PaymentWS();
			payment.setAmount(new BigDecimal("20.00"));
			payment.setIsRefund(new Integer(0));
			payment.setMethodId(ServerConstants.PAYMENT_METHOD_CHEQUE);
			payment.setPaymentDate(Calendar.getInstance().getTime());
			payment.setResultId(ServerConstants.RESULT_ENTERED);
			payment.setCurrencyId(new Integer(1));
			payment.setUserId(myId);

			payment.getPaymentInstruments().add(createCheque("ws bank", "2232-2323-2323", Calendar.getInstance().getTime()));

			System.out.println("Applying payment");
			api.applyPayment(payment, invoiceId);
			// check new balance is 20
			System.out.println("Validating new balance");
			myUser = api.getUserWS(myId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have 0 balance", BigDecimal.ZERO,
                    myUser.getDynamicBalanceAsDecimal());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            System.out.println("Removing");
            if (myId != null) api.deleteUser(myId);
        }
    }

	@Test
	public void test013RulesValidatePurchaseTask() throws Exception {
		System.out.println("#test013RulesValidatePurchaseTask");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer userId = null;
        Integer orderId = null;
        try {
//			  Validate purchase runs using the mediation process when pricing
//			  fields are provided.
//			  
//			  If there are no pricing fields, then the validation becomes a
//			  simple dynamic balance check (Credit limit or pre-paid balance)
//			  to determine if the customer has the funds in their account
//			  necessary to make the purchase.
//			 

			final int LEMONADE_ITEM_ID = 1;
			final int COFFEE_ITEM_ID = 3;


			// create user
            UserWS user = createUser(true, null, null);
			userId = user.getUserId();

			// update to credit limit
			user.setCreditLimit(new BigDecimal("1000.0"));
			user.setMainSubscription(createUserMainSubscription());
            user.setPassword(null);
			api.updateUser(user);

			// lemonade order
			orderId = createOrder(userId, 2);

			// try to get another lemonde
			ValidatePurchaseWS result = api.validatePurchase(userId,
					LEMONADE_ITEM_ID, null);
			assertEquals("validate purchase success 1", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 1",
					Boolean.valueOf(false), result.getAuthorized());
            com.sapienter.jbilling.test.Asserts.assertEquals("validate purchase quantity 1",
                    new BigDecimal("0.00"), result.getQuantityAsDecimal());

			// exception should be thrown
			PricingField pf[] = { new PricingField("fail", "fail") };
			result = api.validatePurchase(userId, LEMONADE_ITEM_ID, PricingField.setPricingFieldsValue(pf));
			assertEquals("validate purchase success 2", Boolean.valueOf(false),
					result.getSuccess());
			assertEquals("validate purchase authorized 2",
					Boolean.valueOf(false), result.getAuthorized());
            com.sapienter.jbilling.test.Asserts.assertEquals("validate purchase quantity 2", BigDecimal.ZERO,
                    result.getQuantityAsDecimal());
			assertEquals("validate purchase message 2",
					"Error: Thrown exception for testing",
					result.getMessage()[0]);

			// coffee quantity available should be 20
			result = api.validatePurchase(userId, COFFEE_ITEM_ID, null);
			assertEquals("validate purchase success 3", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 3",
					Boolean.valueOf(true), result.getAuthorized());
            com.sapienter.jbilling.test.Asserts.assertEquals("validate purchase quantity 3",
                    new BigDecimal("20.0"), result.getQuantityAsDecimal());

            //TODO: createItem
            // add 10 coffees to current order
			OrderLineWS newLine = new OrderLineWS();
			newLine.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
			newLine.setItemId(new Integer(3));
			newLine.setQuantity(new BigDecimal("10.0"));
			newLine.setUseItem(new Boolean(true)); // use pricing from the item

			// update the current order
			OrderWS currentOrderAfter = api.updateCurrentOrder(userId,
					new OrderLineWS[] { newLine }, null, new Date(),
					"Event from WS");

			// quantity available should be 10
			result = api.validatePurchase(userId, COFFEE_ITEM_ID, null);
			assertEquals("validate purchase success 3", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 3",
					Boolean.valueOf(true), result.getAuthorized());
            com.sapienter.jbilling.test.Asserts.assertEquals("validate purchase quantity 3",
                    new BigDecimal("10.0"), result.getQuantityAsDecimal());

			// add another 10 coffees to current order
			currentOrderAfter = api.updateCurrentOrder(userId,
					new OrderLineWS[] { newLine }, null, new Date(),
					"Event from WS");

			// exceeded account credit limit
			// quantity available should be 0
			result = api.validatePurchase(userId, COFFEE_ITEM_ID, null);
			assertEquals("validate purchase success 4", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 4",
					Boolean.valueOf(false), result.getAuthorized());
            com.sapienter.jbilling.test.Asserts.assertEquals("validate purchase quantity 4", BigDecimal.ZERO,
                    result.getQuantityAsDecimal());
			assertEquals("validate purchase message 4",
					"No more than 20 coffees are allowed.",
					result.getMessage()[0]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            // clean up
            pause(2000);
            if (orderId != null) api.deleteOrder(orderId);
            if (userId != null) api.deleteUser(userId);
        }
    }

	@Test
    public void test014UserBalancePurchaseTaskHierarchical() throws Exception {
        System.out.println("#test014UserBalancePurchaseTaskHierarchical");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer childId = null;
        Integer parentId = null;
        Integer orderId = null;
        try {
            // create 2 users, child and parent
            UserWS newUser = new UserWS();
            newUser.setUserName("parent1" + new Date().getTime());
            newUser.setPassword("As$fasdf1");
            newUser.setLanguageId(new Integer(1));
            newUser.setMainRoleId(new Integer(5));
            newUser.setAccountTypeId(Integer.valueOf(1));
            newUser.setIsParent(new Boolean(true));
            newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
            newUser.setCreditLimit(new BigDecimal("2000.0"));

            MetaFieldValueWS metaField1 = new MetaFieldValueWS();
            metaField1.setFieldName("contact.email");
            metaField1.setValue(newUser.getUserName() + "@shire.com");
            metaField1.setGroupId(1);

            newUser.setMetaFields(new MetaFieldValueWS[]{
                    metaField1
            });

            System.out.println("Creating parent user ...");
            // do the creation
            parentId = api.createUser(newUser);

            // now create the child
            newUser.setIsParent(new Boolean(false));
            newUser.setParentId(parentId);
            newUser.setUserName("child1" + new Date().getTime());
            newUser.setPassword("As$fasdf1");
            newUser.setInvoiceChild(Boolean.FALSE);
            newUser.setCreditLimit((String) null);
            metaField1.setValue(newUser.getUserName() + "@shire.com");
            childId = api.createUser(newUser);

            // create an order for the child
            OrderWS order = getOrder();
            order.setUserId(childId);
            System.out.println("creating one time order");
            orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

            // validate the balance of the parent
            System.out.println("Validating new balance");
            UserWS parentUser = api.getUserWS(parentId);
            com.sapienter.jbilling.test.Asserts.assertEquals("user should have -20 balance", new BigDecimal("20.0").negate(), parentUser.getDynamicBalanceAsDecimal());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        } finally {
            // clean up
            if (childId != null) api.deleteUser(childId);
            if (parentId != null) api.deleteUser(parentId);
            if (orderId != null) api.deleteOrder(orderId);
        }
    }

	@Test
	public void test015ValidateMultiPurchase() throws Exception {
		System.out.println("#test015ValidateMultiPurchase");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS myUser = null;
		try {
			myUser = createUser(true, null, null);
			Integer myId = myUser.getUserId();

			// update to credit limit
			myUser.setCreditLimit(new BigDecimal("1000.0"));
            myUser.setPassword(null);
			api.updateUser(myUser);

			// validate with items only
			ValidatePurchaseWS result = api.validateMultiPurchase(myId,
					new Integer[] { 2800, 2, 251 }, null);
			assertEquals("validate purchase success 1", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 1",
					Boolean.valueOf(true), result.getAuthorized());
			/*
			 * credit limit = 1000 ; customer dynamic balance = 0; total of items price = 20+15+6.5 = 41.50
			 * so resultant quantity = (1000+0)/41.50= 24.096385542
			 * 
			 */
            com.sapienter.jbilling.test.Asserts.assertEquals("validate purchase quantity 1",
                    new BigDecimal("24.10"), result.getQuantityAsDecimal());
            
			System.out.println("Removing");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (myUser != null) api.deleteUser(myUser.getId());
        }
    }

	@Test
	public void test016PenaltyTaskOrder() throws Exception {
		System.out.println("#test016PenaltyTaskOrder");
		JbillingAPI api = JbillingAPIFactory.getAPI();

        UserWS createdUser = null;
        OrderWS order = null;
        try {
            createdUser = createUser(true, null, null);

            Integer USER_ID = 53;
            final Integer ORDER_ID = 35;
            final Integer PENALTY_ITEM_ID = 270;

            // pluggable BasicPenaltyTask is configured for ageing_step 6
            // test that other status changes will not add a new order item
            UserWS user = api.getUserWS(USER_ID);
            user.setPassword(null);
            user.setStatusId(2);
            api.updateUser(user);

            assertEquals("Status was changed", 2, api.getUserWS(USER_ID)
                    .getStatusId().intValue());
            assertEquals("No new order was created", ORDER_ID,
                    api.getLatestOrder(USER_ID).getId());

            // new order will be created with the penalty item when status id = 6
            user.setStatusId(6);
            user.setPassword(null);
            api.updateUser(user);

            assertEquals("Status was changed", 6, api.getUserWS(USER_ID)
                    .getStatusId().intValue());

            order = api.getLatestOrder(USER_ID);
            assertFalse("New order was created, id does not equal original",
                    ORDER_ID.equals(order.getId()));
            assertEquals("New order has one item", 1, order.getOrderLines().length);

            OrderLineWS line = order.getOrderLines()[0];
            assertEquals("New order contains penalty item", PENALTY_ITEM_ID,
                    line.getItemId());
            com.sapienter.jbilling.test.Asserts.assertEquals(
                    "Order penalty value is the item price (not a percentage)",
                    new BigDecimal("10.00"), line.getAmountAsDecimal());

        }
        finally {
            // delete order and invoice
            if (order != null) api.deleteOrder(order.getId());
            if (createdUser != null) api.deleteUser(createdUser.getId());
        }
	}

	@Test
	public void test017AutoRecharge() throws Exception {
		System.out.println("#test017AutoRecharge");

		JbillingAPI api = JbillingAPIFactory.getAPI();

		UserWS user = createUser(true, null, null);

		user.setAutoRecharge(new BigDecimal("25.00")); // automatically charge
														// this user $25 when
														// the balance drops
														// below the threshold
        user.setPassword(null);
		// company (entity id 1) recharge threshold is set to $5
		api.updateUser(user);
		user = api.getUserWS(user.getUserId());

        com.sapienter.jbilling.test.Asserts.assertEquals("Automatic recharge value updated",
                new BigDecimal("25.00"), user.getAutoRechargeAsDecimal());

		// create an order for $10,
		OrderWS order = new OrderWS();
		order.setUserId(user.getUserId());
		order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(new Integer(1));
		order.setCurrencyId(new Integer(1));
		order.setActiveSince(new Date());
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2008, 9, 3);

		OrderLineWS lines[] = new OrderLineWS[1];
		OrderLineWS line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("Fist line");
		line.setItemId(new Integer(1));
		lines[0] = line;

		order.setOrderLines(lines);
		Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(
				order, ORDER_CHANGE_STATUS_APPLY_ID)); // should emit a
														// NewOrderEvent that
														// will be handled by
														// the
														// DynamicBalanceManagerTask
		// where the user's dynamic balance will be updated to reflect the
		// charges

		// user's balance should be 0 - 10 + 25 = 15 (initial balance, minus
		// order, plus auto-recharge).
		UserWS updated = api.getUserWS(user.getUserId());
        com.sapienter.jbilling.test.Asserts.assertEquals("balance updated with auto-recharge payment",
                new BigDecimal("15.00"), updated.getDynamicBalanceAsDecimal());

		// cleanup
		api.deleteOrder(orderId);
		api.deleteUser(user.getUserId());
	}

	@Test
	public void test018UpdateCurrentOrderNewQuantityEvents() throws IOException, JbillingAPIException {
		System.out.println("#test018UpdateCurrentOrderNewQuantityEvents");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        // create user
        UserWS user = null;
		try {
			user = createUser(true, null, null);
			Integer userId = user.getUserId();

			// update to credit limit
			user.setCreditLimit(new BigDecimal("1000.0"));
			user.setMainSubscription(createUserMainSubscription());
            user.setPassword(null);
			api.updateUser(user);

            //TODO: Create the item to add to this order
			// add 10 coffees to current order
			OrderLineWS newLine = new OrderLineWS();
			newLine.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
			newLine.setItemId(new Integer(3));
			newLine.setQuantity(new BigDecimal("10.0"));
			// take the price and description from the item
			newLine.setUseItem(new Boolean(true));

			// update the current order
			OrderWS currentOrderAfter = api.updateCurrentOrder(userId,
					new OrderLineWS[] { newLine }, null, new Date(),
					"Event from WS");

			// check dynamic balance increased (credit limit type)
			user = api.getUserWS(userId);
            com.sapienter.jbilling.test.Asserts.assertEquals("dynamic balance", new BigDecimal("-150.0"),
                    user.getDynamicBalanceAsDecimal());

			// add another 10 coffees to current order
			currentOrderAfter = api.updateCurrentOrder(userId,
					new OrderLineWS[] { newLine }, null, new Date(),
					"Event from WS");

			// check dynamic balance increased (credit limit type)
			user = api.getUserWS(userId);
            com.sapienter.jbilling.test.Asserts.assertEquals("dynamic balance", new BigDecimal("-300.0"),
                    user.getDynamicBalanceAsDecimal());

			// update current order using pricing fields
			PricingField duration = new PricingField("duration", 5); // 5 min
			PricingField disposition = new PricingField("disposition",
					"ANSWERED");
			PricingField dst = new PricingField("dst", "12345678");
			currentOrderAfter = api.updateCurrentOrder(userId, null,
					PricingField.setPricingFieldsValue(new PricingField[] { duration, disposition, dst }),
					new Date(), "Event from WS");

			// check dynamic balance increased (credit limit type)
			// 300 + (5 minutes * 5.0 price)
			user = api.getUserWS(userId);
            com.sapienter.jbilling.test.Asserts.assertEquals("dynamic balance", new BigDecimal("-325.0"),
					user.getDynamicBalanceAsDecimal());

			// clean up
			api.deleteUser(userId);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            // clean up
            if (user != null) api.deleteUser(user.getId());
        }
    }

	@Test
	public void test019UserACHCreation() throws Exception {
        System.out.println("#test019UserACHCreation");

        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS newUser = null;
        try {
            newUser = new UserWS();
            newUser.setUserName("testUserName-"
                    + Calendar.getInstance().getTimeInMillis());
            newUser.setPassword("P@ssword1");
            newUser.setLanguageId(new Integer(1));
            newUser.setMainRoleId(new Integer(5));
            newUser.setAccountTypeId(Integer.valueOf(1));
            newUser.setParentId(null);
            newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
            newUser.setCurrencyId(null);

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

            newUser.setMetaFields(new MetaFieldValueWS[] { metaField1, metaField2,
                    metaField3, metaField4, metaField5 });

            // add a credit card
            Calendar expiry = Calendar.getInstance();
            expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
            PaymentInformationWS cc = createCreditCard("Frodo Baggins", "4111111111111152", expiry.getTime());
            newUser.getPaymentInstruments().add(cc);

            PaymentInformationWS ach = createACH("Frodo Baggins", "Shire Financial Bank", "123456789", "123456789", Integer.valueOf(1));
            newUser.getPaymentInstruments().add(ach);

            System.out.println("Creating user with ACH record...");
            newUser.setUserId(api.createUser(newUser));

            UserWS saved = api.getUserWS(newUser.getUserId());
            List<PaymentInformationWS> achs = getAch(saved.getPaymentInstruments());
            ach = achs.size() > 0 ? achs.iterator().next() : null;

            assertNotNull("Returned UserWS should not be null", saved);
            assertNotNull("Returned ACH record should not be null", ach);
            assertEquals("ABA Routing field does not match", "123456789", getMetaField(ach.getMetaFields(), ACH_MF_ROUTING_NUMBER).getStringValue());
            assertEquals("Account Name field does not match", "Frodo Baggins",
                    getMetaField(ach.getMetaFields(), ACH_MF_CUSTOMER_NAME).getStringValue());
            Integer accountTypeId = getMetaField(ach.getMetaFields(), ACH_MF_ACCOUNT_TYPE).getStringValue().equalsIgnoreCase(ServerConstants.ACH_CHECKING) ?
                    Integer.valueOf(1) : Integer.valueOf(2);
            assertEquals("Account Type field does not match", Integer.valueOf(1),
                    accountTypeId);
            assertEquals("Bank Account field does not match", "123456789", getMetaField(ach.getMetaFields(), ACH_MF_ACCOUNT_NUMBER).getStringValue());
            assertEquals("Bank Name field does not match", "Shire Financial Bank",
                    getMetaField(ach.getMetaFields(), ACH_MF_BANK_NAME).getStringValue());

            System.out.println("Passed ACH record creation test");

            updateMetaField(ach.getMetaFields(), ACH_MF_ACCOUNT_NUMBER, "987654321");

            saved.setPassword(null);
            api.updateUser(saved);

            saved = api.getUserWS(newUser.getUserId());
            ach = getAch(saved.getPaymentInstruments()).iterator().next();
            assertNotNull("Returned UserWS should not be null", saved);
            assertNotNull("Returned ACH record should not be null", ach);
            assertEquals("Bank Account field does not match", "987654321", getMetaField(ach.getMetaFields(), ACH_MF_ACCOUNT_NUMBER).getStringValue());

            System.out.println("Passed ACH record update test");
// #6315 - credit card and ach payment methods removed.
//		assertNull("Auto payment should be null",
//				api.getAutoPaymentType(newUser.getUserId()));
//
//		api.setAutoPaymentType(newUser.getUserId(),
//				CommonConstants.AUTO_PAYMENT_TYPE_ACH, true);
//
//		assertNotNull("Auto payment should not be null",
//				api.getAutoPaymentType(newUser.getUserId()));
//		assertEquals("Auto payment type should be set to ACH",
//				CommonConstants.AUTO_PAYMENT_TYPE_ACH,
//				api.getAutoPaymentType(newUser.getUserId()));
        }
        finally {
            if (newUser != null) api.deleteUser(newUser.getUserId());
        }

	}

	@Test
	public void test020UpdateInvoiceChild() throws Exception {
		System.out.println("#test020UpdateInvoiceChild");

        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS user = null;
        try {
            System.out.println("Parent user parent(43)");
            user = createUser(true, 43, null);
            // userId
            Integer userId = user.getUserId();

            boolean flag = user.getInvoiceChild();
            // set the field
            user.setInvoiceChild(!user.getInvoiceChild());

            // Save
            user.setPassword(null);
            api.updateUser(user);

            // get user again
            user = api.getUserWS(userId);
            assertEquals("Successfully updated invoiceChild: ", new Boolean(!flag),
                    user.getInvoiceChild());

            System.out.println("Testing " + !flag + " equals "
                    + user.getInvoiceChild());

            // cleanup
        }
        finally {
            if (user != null) api.deleteUser(user.getUserId());
        }
	}

	@Test
	public void test021UserExists() throws Exception {
        System.out.println("#test021UserExists");
        UserWS userCreated = null;
        JbillingAPI api = JbillingAPIFactory.getAPI();
        try {
            userCreated = createUser(true, null, null);

            // by user name
            assertFalse(api.userExistsWithName("USER_THAT_DOESNT_EXIST"));
            assertTrue(api.userExistsWithName(userCreated.getUserName()));

            // by id
            assertFalse(api.userExistsWithId(Integer.MAX_VALUE));
            assertTrue(api.userExistsWithId(userCreated.getId()));
        }
        finally {
            if (userCreated != null) api.deleteUser(userCreated.getId());
        }
	}

	@Test
	public void test022GetUserByEmail() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS userCreated = null;
        try {

            PreferenceWS uniqueEmailPref = api
                    .getPreference(ServerConstants.PREFERENCE_FORCE_UNIQUE_EMAILS);
            uniqueEmailPref.setValue("0");
            api.updatePreference(uniqueEmailPref);

            userCreated = createUser(true, null, null);
            try {
                System.out.println("Getting valid user by email");
                Integer userId = api.getUserIdByEmail(userCreated.getUserName() + "@shire.com");
                fail("Shouldn't be able to access user by email");
            } catch (Exception e) {
            	e.printStackTrace();
            }

            uniqueEmailPref = api
                    .getPreference(ServerConstants.PREFERENCE_FORCE_UNIQUE_EMAILS);
            uniqueEmailPref.setValue("1");
            api.updatePreference(uniqueEmailPref);

            try {

                System.out.println("Getting valid user by email");
                Integer userId = api.getUserIdByEmail(userCreated.getUserName() + "@shire.com");
                assertEquals("Returned user with ID", new Integer(userCreated.getId()), userId);
            } catch (Exception e) {
            	e.printStackTrace();
                fail("Shouldn't be able to access user by email");
            }

            // return the preference to it's original state
            uniqueEmailPref.setValue("0");
            api.updatePreference(uniqueEmailPref);
        }
        finally {
            if (userCreated != null) api.deleteUser(userCreated.getId());
        }


	}

	@Test
	public void test024CreditCardNumberFormat() throws Exception {
		System.out.println("#test024CreditCardNumberFormat");
		JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS user = null;
        UserWS parentCreated = null;

        try {
            parentCreated = createParent(api);
            user = createUser(true, 43, null);
            user = api.getUserWS(user.getUserId());

            PaymentInformationWS card = user.getPaymentInstruments().iterator()
                    .next();
            updateMetaField(card.getMetaFields(), CC_MF_NUMBER, "&&&&&");
            // fetch card after each update to ensure that we're
            // always updating the most recent credit card
            // Visa
            try {
                user.setPassword(null);
                api.updateUser(user);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e,
                        "MetaFieldValue,value,Payment card number is not valid");
            }

            Integer cardType = getMetaField(
                    user.getPaymentInstruments().iterator().next().getMetaFields(),
                    CC_MF_TYPE).getIntegerValue();
            assertEquals("card type Visa", ServerConstants.PAYMENT_METHOD_VISA, cardType);
        } catch (Exception e) { throw  e; }
        finally {
            // cleanup
            if (parentCreated != null) api.deleteUser(parentCreated.getId());
            if (user != null) api.deleteUser(user.getUserId());
        }

	}

	@Test
    public void test025UserCodeCreate() throws Exception {
        System.out.println("#test025UserCodeCreate");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS user = null;
        try {
            user = createUser(true, true, null, null, true);

            UserCodeWS uc = new UserCodeWS();
            uc.setIdentifier(user.getUserName() + "0002");
            uc.setTypeDescription("ProgramDesc");
            uc.setType("ProgramType");
            uc.setExternalReference("translationId");
            uc.setValidFrom(new Date());
            uc.setUserId(user.getUserId());

            try {
                api.createUserCode(uc);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserCodeWS,identifier,validation.identifier.pattern.fail");
            }

            uc.setIdentifier(user.getUserName() + "00002");
            uc.setId(1);
            try {
                api.createUserCode(uc);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserCodeWS,id,validation.error.max,0");
            }

            uc.setId(0);
            uc.setId(api.createUserCode(uc));

            UserCodeWS uc2 = new UserCodeWS();
            uc2.setIdentifier(user.getUserName() + "00002");
            uc2.setTypeDescription("ProgramDesc");
            uc2.setType("ProgramType");
            uc2.setExternalReference("translationId");
            uc2.setValidFrom(new Date());
            uc2.setUserId(user.getUserId());
            try {
                api.createUserCode(uc2);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserCodeWS,identifier,userCode.validation.duplicate.identifier");
            }
        }
        finally {
            if (user != null) api.deleteUser(user.getUserId());
        }


    }

    @Test
    public void test026UserCodeUpdate() throws Exception {

        System.out.println("#test026UserCodeUpdate");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS user = null;
        Integer userCode = null;
        Integer userCode2 = null;

        try {
            user = createUser(true, true, null, null, true);

            UserCodeWS uc = new UserCodeWS();
            uc.setIdentifier(user.getUserName() + "00002");
            uc.setTypeDescription("ProgramDesc");
            uc.setType("ProgramType");
            uc.setExternalReference("translationId");
            uc.setValidFrom(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24* 3));
            uc.setValidTo(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24* 2));
            uc.setUserId(user.getUserId());

            try {
                userCode = api.createUserCode(uc);
                uc.setId(userCode);
            } catch (SessionInternalError e) {
                e.printStackTrace();
                System.out.println(Arrays.asList(e.getErrorMessages()));
                fail();
            }


            try {
                uc.setType("Another type");
                api.updateUserCode(uc);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserCodeWS,identifier,userCode.validation.update.expired");
            } catch (Exception e) {
                System.out.println("Testing UserCodeWS,identifier,userCode.validation.update.expired");
                e.printStackTrace();
                throw e;
            }


            UserCodeWS uc2 = new UserCodeWS();
            uc2.setIdentifier(user.getUserName() + "00003");
            uc2.setTypeDescription("ProgramDesc");
            uc2.setType("ProgramType");
            uc2.setExternalReference("translationId");
            uc2.setValidFrom(new Date());
            uc2.setUserId(user.getUserId());
            userCode2 = api.createUserCode(uc2);
            uc2.setId(userCode2);

            try {
                uc2.setIdentifier(user.getUserName() + "00002");
                api.updateUserCode(uc2);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserCodeWS,identifier,userCode.validation.duplicate.identifier");
            } catch (Exception e) {
                System.out.println("Testing UserCodeWS,identifier,userCode.validation.duplicate.identifier");
                e.printStackTrace();
                throw e;
            }
        }
        finally {
            if (user != null) api.deleteUser(user.getId());
        }
    }

    @Test
    public void test027UserCodeLinks() throws Exception {
        System.out.println("#test027UserCodeLinks");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS user = null;
        UserWS user2 = null;

        try {
            user = createUser(true, true, null, null, true);
            String uc1 = user.getUserName() + "00002";
            String uc2 = user.getUserName() + "00003";

            UserCodeWS uc = new UserCodeWS();
            uc.setIdentifier(uc1);
            uc.setTypeDescription("ProgramDesc");
            uc.setType("ProgramType");
            uc.setExternalReference("translationId");
            uc.setValidFrom(new Date());
            uc.setUserId(user.getUserId());
            api.createUserCode(uc);

            uc.setIdentifier(uc2);
            api.createUserCode(uc);

            user2 = createUser(true, true, null, null, false);
            user2.setEntityId(ROOT_ENTITY_ID);
            user2.setUserCodeLink("aaaa");

            try {
                user2.setId(api.createUser(user2));
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserWS,linkedUserCodes,validation.error.userCode.not.exist,aaaa");
            }

            user2.setUserCodeLink(uc1);
            user2.setId(api.createUser(user2));

            user2 = api.getUserWS(user2.getId());

            Integer[] ids = api.getCustomersLinkedToUser(user.getUserId());
            assertEquals(1, ids.length);
            assertEquals(user2.getCustomerId().intValue(), ids[0].intValue());

            ids = api.getCustomersByUserCode(uc1);
            assertEquals(1, ids.length);
            assertEquals(user2.getCustomerId().intValue(), ids[0].intValue());

            user2 = api.getUserWS(user2.getId());
            assertEquals(uc1, user2.getUserCodeLink());

            user2.setUserCodeLink(uc2);
            api.updateUser(user2);

            user2 = api.getUserWS(user2.getId());
            assertEquals(uc2, user2.getUserCodeLink());
        }
        finally {
            if (user != null) api.deleteUser(user.getUserId());
            if (user2 != null) api.deleteUser(user2.getUserId());
        }
    }

    
    @Test
    public void test028ParentChildBillingCycleTest() throws Exception {
        System.out.println("#test025ParentChildBillingCycleTest");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS parentUser = null;
        UserWS childUser = null;
        UserWS childUser2 = null;
        
        try {
            parentUser = createUser(true, null, null);
	        parentUser.setPassword(null);
            parentUser.setIsParent(true);
            parentUser.setPassword(null);
            api.updateUser(parentUser);

            parentUser = api.getUserWS(parentUser.getUserId());

            childUser = createUser(true, parentUser.getUserId(), null);
	        childUser.setPassword(null);
            childUser.setInvoiceChild(false);
            childUser.setNextInvoiceDate(parentUser.getNextInvoiceDate());
            childUser.setPassword(null);
            api.updateUser(childUser);

            // Scenario 1 - While creating/editing sub-account, if 'Invoice if Child' is unchecked, then billing cycle,
            // invoice generation day and next invoice date fields of the child account cannot be different than parent account.

            parentUser = api.getUserWS(parentUser.getUserId());
            Date nextInvoiceDateOfParent =  parentUser.getNextInvoiceDate();
            Integer invoiceGenerationDayOfParent = parentUser.getMainSubscription().getNextInvoiceDayOfPeriod();

            childUser = api.getUserWS(childUser.getUserId());
            Date nextInvoiceDateOfChild =  childUser.getNextInvoiceDate();
            Integer invoiceGenerationDayOfChild = childUser.getMainSubscription().getNextInvoiceDayOfPeriod();

            assertEquals("Both next invoice day generated should be equal", nextInvoiceDateOfChild, nextInvoiceDateOfParent);
            assertEquals("Both invoice generation day should be equal", invoiceGenerationDayOfChild,invoiceGenerationDayOfParent );


            // Scenario 2 - While creating/editing sub-account, if 'Invoice if Child' is checked, then billing cycle,
            // invoice generation day and next invoice date fields of the child account can be different than parent account.

            childUser = api.getUserWS(childUser.getUserId());
	        childUser.setPassword(null);
            childUser.setInvoiceChild(true);
            MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
            mainSubscription.setPeriodId(2); //monthly
            mainSubscription.setNextInvoiceDayOfPeriod(27); // 27th of the month
            childUser.setMainSubscription(mainSubscription);
            childUser.setPassword(null);
            api.updateUser(childUser);

            childUser = api.getUserWS(childUser.getUserId());
            nextInvoiceDateOfChild =  childUser.getNextInvoiceDate();
            invoiceGenerationDayOfChild = childUser.getMainSubscription().getNextInvoiceDayOfPeriod();

            System.out.println("nextInvoiceDateOfChild != nextInvoiceDateOfParent ::::::::::::"+ nextInvoiceDateOfChild +" NOT EQUAL "+ nextInvoiceDateOfParent);
            System.out.println("invoiceGenerationDayOfChild != invoiceGenerationDayOfParent ::::::::::::"+ invoiceGenerationDayOfChild +" NOT EQUAL "+ invoiceGenerationDayOfParent);

            assertEquals(nextInvoiceDateOfChild.compareTo(nextInvoiceDateOfParent),1 );
            assertEquals( invoiceGenerationDayOfChild.compareTo(invoiceGenerationDayOfParent),1 );

            //Scenario 3. When a parent account is edited and updated for billing cycle fields,
            //post update it should also update the billing fields of all its sub accounts with 'invoice if child' flag unchecked.

            GregorianCalendar cal = new GregorianCalendar();
            cal.clear();
            cal.set(2010, GregorianCalendar.JANUARY, 01, 0, 0, 0);

            childUser2 = createUser(true, parentUser.getUserId(), null);
	        childUser2.setPassword(null);
            childUser2.setInvoiceChild(false);
            childUser2.setNextInvoiceDate(parentUser.getNextInvoiceDate());

            childUser2.setPassword(null);
            api.updateUser(childUser2);

            parentUser = api.getUserWS(parentUser.getUserId());
	        parentUser.setPassword(null);
            MainSubscriptionWS mainSubscription1 = new MainSubscriptionWS();
            mainSubscription1.setPeriodId(3); //weekly
            mainSubscription1.setNextInvoiceDayOfPeriod(1); //Monday

            parentUser.setNextInvoiceDate(cal.getTime());
            parentUser.setMainSubscription(mainSubscription1);
            parentUser.setPassword(null);
            api.updateUser(parentUser);

            nextInvoiceDateOfParent =  parentUser.getNextInvoiceDate();
            invoiceGenerationDayOfParent = parentUser.getMainSubscription().getNextInvoiceDayOfPeriod();

            System.out.println("nextInvoiceDateOfChild != nextInvoiceDateOfParent ::::::::::::"+ nextInvoiceDateOfChild +" NOT EQUAL "+ nextInvoiceDateOfParent);
            System.out.println("invoiceGenerationDayOfChild != invoiceGenerationDayOfParent ::::::::::::"+ invoiceGenerationDayOfChild +" NOT EQUAL "+ invoiceGenerationDayOfParent);
            //For Child(Invoice id child = true) and parent
            assertEquals(nextInvoiceDateOfChild.compareTo(nextInvoiceDateOfParent),1 );
            assertEquals( invoiceGenerationDayOfChild.compareTo(invoiceGenerationDayOfParent),1 );

            childUser2 = api.getUserWS(childUser2.getUserId());
            Date nextInvoiceDateOfChild1 =  childUser2.getNextInvoiceDate();
            Integer invoiceGenerationDayOfChild1 = childUser2.getMainSubscription().getNextInvoiceDayOfPeriod();

            System.out.println("nextInvoiceDateOfChild1 == nextInvoiceDateOfParent ::::::::::::"+ nextInvoiceDateOfChild1 +" EQUAL "+ nextInvoiceDateOfParent);
            System.out.println("invoiceGenerationDayOfChild1 != invoiceGenerationDayOfParent ::::::::::::"+ invoiceGenerationDayOfChild1 +" EQUAL "+ invoiceGenerationDayOfParent);
            //For Child(Invoice id child = false) and parent
            assertEquals(nextInvoiceDateOfChild1.compareTo(nextInvoiceDateOfParent),0 );
            assertEquals( invoiceGenerationDayOfChild1.compareTo(invoiceGenerationDayOfParent),0 );

            // Scenario 4. When a parent account is edited and updated for billing cycle fields,
            // post update it should NOT update the billing fields of all its sub accounts with 'invoice if child' flag CHECKED.

            cal.clear();
            cal.set(2010, GregorianCalendar.JANUARY, 02, 0, 0, 0);
            parentUser = api.getUserWS(parentUser.getUserId());
	        parentUser.setPassword(null);
            MainSubscriptionWS mainSubscription2 = new MainSubscriptionWS();
            mainSubscription2.setPeriodId(3); //Weekly
            mainSubscription2.setNextInvoiceDayOfPeriod(2); //Tuesday
            parentUser.setMainSubscription(mainSubscription2);
            parentUser.setNextInvoiceDate(cal.getTime());
            parentUser.setPassword(null);
            api.updateUser(parentUser);

            parentUser = api.getUserWS(parentUser.getUserId());
            Integer parentBillingPeriodId = parentUser.getMainSubscription().getPeriodId();
            childUser = api.getUserWS(childUser.getUserId());
            Integer childBillingPeriodId = childUser.getMainSubscription().getPeriodId();

            System.out.println("parentBillingPeriodId != childBillingPeriodId ::::::::::::"+ parentBillingPeriodId +" NOT EQUAL "+ childBillingPeriodId);
            assertEquals( parentBillingPeriodId.compareTo(childBillingPeriodId),1 );
        }
        finally {
            if (childUser != null) api.deleteUser(childUser.getId());
            if (childUser2 != null) api.deleteUser(childUser2.getId());
            if (parentUser != null) api.deleteUser(parentUser.getId());
        }
    }
    
    @Test
    public void test029ParentChildBillingCycleValidationTest() throws Exception {

        System.out.println("test026ParentChildBillingCycleValidationTest");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS parentUser = null;
        UserWS childUser = null;
        int customerUpdateFailed = 0;
        try{
            //User created
            parentUser = createUser(true, null, null);
            parentUser.setIsParent(true);
            parentUser.setNextInvoiceDate(new DateMidnight(2010, 9, 1).toDate());
            parentUser.setPassword(null);
            api.updateUser(parentUser);

            childUser = createUser(true, parentUser.getUserId(), null);

            childUser.setInvoiceChild(false);

            childUser = api.getUserWS(childUser.getUserId());
            childUser.setNextInvoiceDate(new DateMidnight(2010, 1, 1).toDate());

            childUser.setPassword(null);
            api.updateUser(childUser);
            Integer childUserId = childUser.getUserId();
            System.out.println("childUserId: " + childUserId);
        }catch(SessionInternalError ex){
            customerUpdateFailed = 1;
            System.out.println("User failed "+ex.getErrorMessages()[0]);
            System.out.println(":::::::::::::::::::::::::::: "+ex);
        } finally {
            if (childUser != null) api.deleteUser(childUser.getId());
            if (parentUser != null) api.deleteUser(parentUser.getId());
        }
    }

    @Test
    public void test030ParentChildBillingCycleCheckTest() throws Exception {
        System.out.println("test027ParentChildBillingCycleCheckTest");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS parentUser = null;
        UserWS childUser1 = null;
        UserWS childUser2 = null;
        UserWS childUser3 = null;
        try {
            parentUser = createUser(true, null, null);
            parentUser.setIsParent(true);
            parentUser.setNextInvoiceDate(new DateMidnight(2010, 1, 1).toDate());
            api.updateUser(parentUser);

            childUser1 = createUser(true, parentUser.getUserId(), null);
            childUser1.setInvoiceChild(false);
            childUser1.setNextInvoiceDate(new DateMidnight(2010, 1, 1).toDate());
            api.updateUser(childUser1);
            childUser1 = api.getUserWS(childUser1.getUserId());

            childUser2 = createUser(true, parentUser.getUserId(), null);
            childUser2.setInvoiceChild(false);
            childUser2.setNextInvoiceDate(new DateMidnight(2010, 1, 1).toDate());
            api.updateUser(childUser2);
            childUser2 = api.getUserWS(childUser2.getUserId());

            childUser3 = createUser(true, parentUser.getUserId(), null);
            childUser3.setInvoiceChild(false);
            childUser3.setNextInvoiceDate(new DateMidnight(2010, 1, 1).toDate());
            api.updateUser(childUser3);
            childUser3 = api.getUserWS(childUser3.getUserId());

            parentUser = api.getUserWS(parentUser.getUserId());

            // change billing cycle of parent
            MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
            mainSubscription.setPeriodId(2); //monthly
            mainSubscription.setNextInvoiceDayOfPeriod(10);
            parentUser.setMainSubscription(mainSubscription);
            parentUser.setNextInvoiceDate(new DateMidnight(2010, 1, 10).toDate());// billing cycle monthly 10
            api.updateUser(parentUser);

            parentUser = api.getUserWS(parentUser.getUserId());

            Date nextInvoiceDateOfParent = parentUser.getNextInvoiceDate();

            nextInvoiceDateOfParent = parentUser.getNextInvoiceDate();

            childUser1 = api.getUserWS(childUser1.getUserId());
            childUser2 = api.getUserWS(childUser2.getUserId());
            childUser3 = api.getUserWS(childUser3.getUserId());

            Date nextInvoiceDateOfChild1 =  childUser1.getNextInvoiceDate();
            Date nextInvoiceDateOfChild2 =  childUser2.getNextInvoiceDate();
            Date nextInvoiceDateOfChild3 =  childUser3.getNextInvoiceDate();

            Integer invoiceGenerationDayOfParent = parentUser.getMainSubscription().getNextInvoiceDayOfPeriod();
            Integer invoiceGenerationDayOfChild1 = childUser1.getMainSubscription().getNextInvoiceDayOfPeriod();
            Integer invoiceGenerationDayOfChild2 = childUser2.getMainSubscription().getNextInvoiceDayOfPeriod();
            Integer invoiceGenerationDayOfChild3 = childUser3.getMainSubscription().getNextInvoiceDayOfPeriod();

            Integer periodIdOfParent = parentUser.getMainSubscription().getPeriodId();
            Integer periodIdOfChild1 = childUser1.getMainSubscription().getPeriodId();
            Integer periodIdOfChild2 = childUser2.getMainSubscription().getPeriodId();
            Integer periodIdOfChild3 = childUser3.getMainSubscription().getPeriodId();

            assertEquals("Both PeriodId of child1 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);
            assertEquals("Both PeriodId of child2 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);
            assertEquals("Both PeriodId of child3 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);

            assertEquals("Both next invoice day of child1 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);
            assertEquals("Both next invoice day of child2 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);
            assertEquals("Both next invoice day of child3 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);

            assertEquals("Both next invoice date of child1 and parent should be equal", nextInvoiceDateOfChild1, nextInvoiceDateOfParent);
            assertEquals("Both next invoice date child2 and parent should be equal", nextInvoiceDateOfChild2, nextInvoiceDateOfParent);
            assertEquals("Both next invoice date child3 and parent should be equal", nextInvoiceDateOfChild3, nextInvoiceDateOfParent);
        }
        finally {
            if (childUser1 != null) api.deleteUser(childUser1.getId());
            if (childUser2 != null) api.deleteUser(childUser2.getId());
            if (childUser3 != null) api.deleteUser(childUser3.getId());
            if (parentUser != null) api.deleteUser(parentUser.getId());
        }
    }

    public static UserWS createUser(boolean goodCC, Integer parentId,
			Integer currencyId) throws JbillingAPIException, IOException {
		return createUser(true, goodCC, parentId, currencyId, true);
	}

	public static UserWS createUser(boolean setPassword, boolean goodCC, Integer parentId,
			Integer currencyId, boolean doCreate) throws JbillingAPIException,
			IOException {
		JbillingAPI api = JbillingAPIFactory.getAPI();

		
		// Create - This passes the password validation routine.
		 
		UserWS newUser = new UserWS();
		newUser.setUserId(0); // it is validated
		newUser.setUserName("testUserName-"
				+ Calendar.getInstance().getTimeInMillis());
		if (setPassword) {
            newUser.setPassword("P@ssword1");
        }
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(Integer.valueOf(1));
		newUser.setParentId(parentId); // this parent exists
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(currencyId);
		newUser.setInvoiceChild(false);

		if (parentId != null) {
		    UserWS parent = api.getUserWS(parentId);
		    MainSubscriptionWS parentSubscription = parent.getMainSubscription();
		    newUser.setMainSubscription(
		            new MainSubscriptionWS(parentSubscription.getPeriodId(), parentSubscription.getNextInvoiceDayOfPeriod()));
		    newUser.setNextInvoiceDate(parent.getNextInvoiceDate());
		}
		
		System.out.println("User properties set");
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

		newUser.setMetaFields(new MetaFieldValueWS[] { metaField1, metaField2,
				metaField3, metaField4, metaField5 });

		System.out.println("Meta field values set");

		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		PaymentInformationWS cc = createCreditCard("Frodo Baggins",
				goodCC ? "4111111111111152" : "4111111111111111",
				expiry.getTime());

		newUser.getPaymentInstruments().add(cc);

		if (doCreate) {
			System.out.println("Creating user ...");
			newUser = api.getUserWS(api.createUser(newUser));
	        if (parentId != null) {
	            UserWS parent = api.getUserWS(parentId);
	            newUser.setNextInvoiceDate(parent.getNextInvoiceDate());
	            api.updateUser(newUser);
	            newUser = api.getUserWS(newUser.getId());
	        }
			newUser.setPassword(null);

		}
		System.out.println("User created with id:" + newUser.getUserId());
		return newUser;
	}

	public static OrderWS getOrder() {
		// need an order for it
		OrderWS newOrder = new OrderWS();
		newOrder.setUserId(new Integer(-1)); // it does not matter, the user
												// will be created
		newOrder.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
		newOrder.setPeriod(new Integer(1)); // once
		newOrder.setCurrencyId(new Integer(1));
		newOrder.setActiveSince(new Date());

		// now add some lines
		OrderLineWS lines[] = new OrderLineWS[2];
		OrderLineWS line;

		line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("First line");
		line.setItemId(new Integer(1));
		lines[0] = line;

		line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("Second line");
		line.setItemId(new Integer(2));
		lines[1] = line;

		newOrder.setOrderLines(lines);

		return newOrder;
	}

	public static PaymentInformationWS createCreditCard(String cardHolderName,
			String cardNumber, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(CC_PM_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(ServerConstants.PAYMENT_METHOD_VISA);
		
		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true,
				DataType.STRING, 1, cardHolderName);
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.STRING, 2,
				cardNumber);
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.STRING, 3, DateTimeFormat.forPattern(
						ServerConstants.CC_DATE_FORMAT).print(date.getTime()));
		// have to pass meta field card type for it to be set
		addMetaField(metaFields, CC_MF_TYPE, true, false,
				DataType.INTEGER, 4, new Integer(0));
		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}

	public static PaymentInformationWS createACH(String customerName,
			String bankName, String routingNumber, String accountNumber, Integer accountType) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(ACH_PM_ID);
        cc.setPaymentMethodId(ServerConstants.PAYMENT_METHOD_ACH);
		cc.setProcessingOrder(new Integer(2));

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, ACH_MF_ROUTING_NUMBER, false, true,
				DataType.STRING, 1, routingNumber);
		addMetaField(metaFields, ACH_MF_CUSTOMER_NAME, false, true,
				DataType.STRING, 2, customerName);
		addMetaField(metaFields, ACH_MF_ACCOUNT_NUMBER, false, true,
				DataType.STRING, 3, accountNumber);
		addMetaField(metaFields, ACH_MF_BANK_NAME, false, true,
				DataType.STRING, 4, bankName);
		addMetaField(metaFields, ACH_MF_ACCOUNT_TYPE, false, true,
				DataType.ENUMERATION, 5, accountType == 1 ? ServerConstants.ACH_CHECKING : ServerConstants.ACH_SAVING);

		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}
	
	public static PaymentInformationWS createCheque(String bankName, String chequeNumber, Date date) {
		PaymentInformationWS cheque = new PaymentInformationWS();
		cheque.setPaymentMethodTypeId(CHEQUE_PM_ID);
        cheque.setPaymentMethodId(ServerConstants.PAYMENT_METHOD_CHEQUE);
		cheque.setProcessingOrder(new Integer(3));

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CHEQUE_MF_BANK_NAME, false, true,
				DataType.STRING, 1, bankName);
		addMetaField(metaFields, CHEQUE_MF_NUMBER, false, true,
				DataType.STRING, 2, chequeNumber);
		addMetaField(metaFields, CHEQUE_MF_DATE, false, true,
				DataType.DATE, 3, date);
		cheque.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cheque;
	}
	
	private static void addMetaField(List<MetaFieldValueWS> metaFields,
			String fieldName, boolean disabled, boolean mandatory,
			DataType dataType, Integer displayOrder, Object value) {
		MetaFieldValueWS ws = new MetaFieldValueWS();
		ws.setFieldName(fieldName);
		ws.setDisabled(disabled);
		ws.setMandatory(mandatory);
		ws.setDataType(dataType);
		ws.setDisplayOrder(displayOrder);
		ws.setValue(value);

		metaFields.add(ws);
	}

	private static Integer createOrder(Integer userId, Integer itemId)
			throws JbillingAPIException, IOException {
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// create an order for this user
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(2); // monthly
		order.setCurrencyId(1); // USD

		// a main subscription order
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2009, 1, 1);
		order.setActiveSince(cal.getTime());

		// order lines
		OrderLineWS[] lines = new OrderLineWS[2];
		lines[0] = new OrderLineWS();
		lines[0].setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		lines[0].setQuantity(1);
		lines[0].setItemId(itemId);
		// take the price and description from the item
		lines[0].setUseItem(true);

		lines[1] = new OrderLineWS();
		lines[1].setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		lines[1].setQuantity(3);
		lines[1].setItemId(1); // lemonade
		// take the price and description from the item
		lines[1].setUseItem(true);

		// attach lines to order
		order.setOrderLines(lines);

		// create the order
		return api.createOrder(order, OrderChangeBL.buildFromOrder(order,
				ORDER_CHANGE_STATUS_APPLY_ID));
	}

	public static MainSubscriptionWS createUserMainSubscription() {
		MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
		mainSubscription.setPeriodId(2); // monthly
		mainSubscription.setNextInvoiceDayOfPeriod(1); // 1st of the month
		return mainSubscription;
	}
	
	public static MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields,
			String fieldName) {
		for (MetaFieldValueWS ws : metaFields) {
			if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
				return ws;
			}
		}
		return null;
	}

	@Test(enabled=false)
	public static void updateMetaField(MetaFieldValueWS[] metaFields,
			String fieldName, Object value) {
		for (MetaFieldValueWS ws : metaFields) {
			if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
				ws.setValue(value);
			}
		}
	}
	
	private List<PaymentInformationWS> getAch(List<PaymentInformationWS> instruments) {
		List<PaymentInformationWS> found = new ArrayList<PaymentInformationWS>();
		for(PaymentInformationWS instrument : instruments) {
			if(instrument.getPaymentMethodTypeId() == ACH_PM_ID) {
				found.add(instrument);
			}
		}
		return found;
	}
	
	private List<PaymentInformationWS> getCreditCard(List<PaymentInformationWS> instruments) {
		List<PaymentInformationWS> found = new ArrayList<PaymentInformationWS>();
		for(PaymentInformationWS instrument : instruments) {
			if(instrument.getPaymentMethodTypeId() == CC_PM_ID) {
				found.add(instrument);
			}
		}
		return found;
	}
	
	private void pause(long t) {
		System.out.println("pausing for " + t + " ms...");
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
		}
	}
}
