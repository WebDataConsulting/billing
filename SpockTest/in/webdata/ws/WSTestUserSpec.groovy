
package in.webdata.ws

import com.sapienter.jbilling.server.user.ContactWS

import com.sapienter.jbilling.server.user.ContactWS
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.entity.AchDTO;
import com.sapienter.jbilling.server.entity.CreditCardDTO;
import com.sapienter.jbilling.server.entity.PaymentInfoChequeDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.user.ContactWS
import com.sapienter.jbilling.server.user.CreateResponseWS
import com.sapienter.jbilling.server.user.UserTransitionResponseWS
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.ValidatePurchaseWS
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.api.WebServicesConstants;

import junit.framework.TestCase;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.joda.time.DateMidnight;

import spock.lang.Specification;

public class WSTestUserSpec extends Specification {

	def  "testGetUser"() {


		setup:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		println("Getting user 2");

		UserWS ret = api.getUserWS(new Integer(2));

		expect:
		2		==	 ret.getUserId();


		when:
		println("Getting invalid user 13");
		ret = api.getUserWS(new Integer(13));

		then:
		thrown(JbillingAPIException);
		println("Shouldn't be able to access user 13");
	}

	def  "testOwingBalance"() {

		setup:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		println("Getting balance of user 2");

		UserWS ret = api.getUserWS(new Integer(2));

		expect:

		"130.00"	==	 ret.getOwingBalance();

		println("Gandalf's balance: " + ret.getOwingBalance());
	}


	def "testCreateUpdateDeleteUser"() {
		//try {


		/*
		 * Create - This passes the password validation routine.
		 */

		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();
		println("");
		UserWS newUser = createUser(true, 43, null);
		Integer newUserId = newUser.getUserId();
		String newUserName = newUser.getUserName();

		then:
		null	!=	 newUserId;

		println("Getting the id of the new user");

		when:

		Integer ret = api.getUserId(newUserName);

		then:

		newUserId == ret;

		//verify the created user
		when:

		println("Getting created user " + newUserId);

		UserWS retUser = api.getUserWS(newUserId);

		then:

		retUser.getUserName() == newUser.getUserName();

		retUser.getContact().getFirstName()	==	newUser.getContact().getFirstName();

		new Integer(43)				==	 retUser.getParentId();

		Constants.BALANCE_NO_DYNAMIC	==	retUser.getBalanceType();

		println("My user: " + retUser);

		"Frodo Baggins"		==	retUser.getCreditCard().getName();


		/*
		 * Make a create mega call
		 */
		when:

		println("Making mega call");
		retUser.setUserName("MU" + Long.toHexString(System.currentTimeMillis()));
		// need to reset the password, it came encrypted
		// let's use a long one
		retUser.setPassword("0fu3js8wl1;a\$e2w)xRQ");
		// the new user shouldn't be a child
		retUser.setParentId(null);

		// need an order for it
		OrderWS newOrder = getOrder();

		CreateResponseWS mcRet = api.create(retUser,newOrder);

		println("Validating new invoice");
		// validate that the results are reasonable
		then:


		null		!=	 mcRet;
		null		!=	 mcRet.getInvoiceId();
		// there should be a successfull payment
		true			==    mcRet.getPaymentResult().getResult().booleanValue();
		"fake-code-default"		==     mcRet.getPaymentResult().getCode1();
		// get the invoice
		when:

		InvoiceWS retInvoice = api.getInvoiceWS(mcRet.getInvoiceId());

		then:

		null	!=     retInvoice;
		"0.00"		==      retInvoice.getBalance();
		"20.00"		==      retInvoice.getTotal();
		retInvoice.getToProcess()		==      new Integer(0);

		// TO-DO test that the invoice total is equal to the order total

		/*
		 * Update
		 */
		// now update the created user

		when:

		println("Updating user - Pass 1 - Should succeed");

		retUser.setPassword("newPassword1");

		retUser.getCreditCard().setNumber("4111111111111152");

		retUser.setBalanceType(Constants.BALANCE_CREDIT_LIMIT);

		retUser.setCreditLimit(new BigDecimal("112233.0"));

		println("Updating user...");

		api.updateUser(retUser);

		// and ask for it to verify the modification
		println("Getting updated user ");

		retUser = api.getUserWS(newUserId);

		then:

		null		!=      retUser;

		// The password should be the same as in the first step, no update happened.
		retUser.getPassword()		==      "33aa7e0850c4234ff03beb205b9ea728";

		retUser.getContact().getFirstName()		==      newUser.getContact().getFirstName();

		"4111111111111152"		==      retUser.getCreditCard().getNumber();

		Constants.BALANCE_CREDIT_LIMIT 		==     retUser.getBalanceType();

		"112233.00"		==      retUser.getCreditLimit();


		when:

		println("Updating user - Pass 2 - Should fail due to invalid password");

		retUser.setPassword("newPassword");

		println("Updating user...");

		api.updateUser(retUser);
		then:
		def i =2
		println("User was not updated. Password validation worked.")
		thrown(Exception)

		when:
		i	!=2
		then:
		println("User was updated - Password validation not working!")
		// again, for the contact info, and no cc
		when:
		retUser.getContact().setFirstName("New Name");

		retUser.getContact().setLastName("New L.Name");

		retUser.setCreditCard(null);
		// call the update
		retUser.setPassword(null); // should not change the password
		api.updateUser(retUser);
		// fetch the user
		UserWS updatedUser = api.getUserWS(newUserId);

		then:

		retUser.getContact().getFirstName()		==     updatedUser.getContact().getFirstName();

		retUser.getContact().getLastName()		== 	   updatedUser.getContact().getLastName();

		"4111111111111152"						==     updatedUser.getCreditCard().getNumber();

		"33aa7e0850c4234ff03beb205b9ea728"		==     updatedUser.getPassword();

		println("Update result:" + updatedUser);

		when:

		// now update the contact only
		retUser.getContact().setFirstName("New Name2");

		api.updateUserContact(retUser.getUserId(),new Integer(2),retUser.getContact());
		// fetch the user
		updatedUser = api.getUserWS(newUserId);

		then:
		retUser.getContact().getFirstName()		==     updatedUser.getContact().getFirstName();

		// now update with a bogus contact type

		when:
		println("Updating with invalid contact type");
		api.updateUserContact(retUser.getUserId(),new Integer(1),retUser.getContact());

		then:
		println("Should not update with an invalid contact type");
		thrown(Exception)
		//                // good
		//                  println("Type rejected " + e.getMessage());
		//            }

		// update credit card details

		when:

		println("Removing credit card");

		api.updateCreditCard(newUserId, null);

		then:

		null		==     api.getUserWS(newUserId).getCreditCard();

		when:

		println("Creating credit card");

		String ccName = "New ccName";

		String ccNumber = "4012888888881881";

		Date ccExpiry = Util.truncateDate(Calendar.getInstance().getTime());

		CreditCardDTO cc = new CreditCardDTO();

		cc.setName(ccName);

		cc.setNumber(ccNumber);

		cc.setExpiry(ccExpiry);

		api.updateCreditCard(newUserId,cc);


		// check updated cc details
		retUser = api.getUserWS(newUserId);

		CreditCardDTO retCc = retUser.getCreditCard();

		then:

		ccName		==      retCc.getName();

		ccNumber		==      retCc.getNumber();

		ccExpiry		==      retCc.getExpiry();

		when:

		println("Updating credit card");

		cc.setName("Updated ccName");

		cc.setNumber(null);

		api.updateCreditCard(newUserId,cc);

		retUser = api.getUserWS(newUserId);

		then:
		"Updated ccName"		==      retUser.getCreditCard().getName();

		null		!=			        retUser.getCreditCard().getNumber();

		// try to update cc of user from different company

		when:

		println("Attempting to update cc of a user from "
				+ "a different company");

		api.updateCreditCard(new Integer(13),cc);

		then:
		println("Shouldn't be able to update cc of user 13");
		thrown(Exception)

		/*
		 * Delete
		 */
		// now delete this new guy

		when:

		println("Deleting user..." + newUserId);

		api.deleteUser(newUserId);

		// try to fetch the deleted user
		println("Getting deleted user " + newUserId);

		updatedUser = api.getUserWS(newUserId);

		then:

		updatedUser.getDeleted()		==      1;

		// verify I can't delete users from another company
		when:
		println("Deleting user base user ... 13");

		api.getUserWS(new Integer(13));

		then:
		println("Shouldn't be able to access user 13");
		thrown(Exception)



		/*
		 * Get list of active customers
		 */

		when:

		println("Getting active users...");
		Integer[] users = api.getUsersInStatus(UserDTOEx.STATUS_ACTIVE);

		then:
		1156		==      users.length;

		1		==      users[0].intValue();

		/*
		 * Get list of not active customers
		 */

		when:

		println("Getting NOTactive users...");
		users = api.getUsersNotInStatus(UserDTOEx.STATUS_ACTIVE);

		then:

		users.length		==      1;
		for (int f = 0; f < users.length; f++) {
			println("Got user " + users[f]);
		}

		/*
		 * Get list using a custom field
		 */
		when:

		println("Getting by custom field...");
		users = api.getUsersByCustomField(new Integer(1),new String("serial-from-ws"));

		// the one from the megacall is not deleted and has the custom field
		then:

		users.length		==      1123;
		users[1000]		==      mcRet.getUserId();

		println("Done");

	}

	def "testCreditCardUpdates"() {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		/*  Note, a more direct test would be to write a unit test for the CreditCardDTO class itself,
		 but our current testing framework doesn't support this style. Instead, test CreditCardBL
		 which should the standard service interface for all credit card interaction.        
		 */

		UserWS user = createUser(true, 43, null);

		Integer userId = user.getUserId();
		CreditCardDTO card = user.getCreditCard();

		// Visa
		card.setNumber("4111111111111985");
		api.updateCreditCard(user.getUserId(), card);

		user = api.getUserWS(userId);

		then:

		Constants.PAYMENT_METHOD_VISA		==		 user.getCreditCard().getType();

		// Mastercard
		when:

		card.setNumber("5111111111111985");
		api.updateCreditCard(user.getUserId(), card);

		user = api.getUserWS(userId);

		then:

		Constants.PAYMENT_METHOD_MASTERCARD		==		 user.getCreditCard().getType();

		// American Express
		when:
		card.setNumber("3711111111111985");
		api.updateCreditCard(user.getUserId(), card);

		user = api.getUserWS(userId);

		then:
		Constants.PAYMENT_METHOD_AMEX		==		 user.getCreditCard().getType();

		// Diners Club
		when:

		card.setNumber("3811111111111985");
		api.updateCreditCard(user.getUserId(), card);

		user = api.getUserWS(userId);

		then:
		Constants.PAYMENT_METHOD_DINERS		==		 user.getCreditCard().getType();

		// Discovery
		when:
		card.setNumber("6111111111111985");
		api.updateCreditCard(user.getUserId(), card);

		user = api.getUserWS(userId);

		then:

		Constants.PAYMENT_METHOD_DISCOVERY		==		 user.getCreditCard().getType();

		//cleanup
		api.deleteUser(user.getUserId());
	}

	def "testLanguageId"() {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		UserWS newUser = new UserWS();
		newUser.setUserName("language-test");
		newUser.setPassword("asdfasdf1");
		newUser.setLanguageId(new Integer(2)); // French
		newUser.setMainRoleId(new Integer(5));
		newUser.setIsParent(new Boolean(true));
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);

		// add a contact
		ContactWS contact = new ContactWS();
		contact.setEmail("frodo@shire.com");
		newUser.setContact(contact);

		println("Creating user ...");
		// do the creation
		Integer newUserId = api.createUser(newUser);

		// get user
		UserWS createdUser = api.getUserWS(newUserId);

		then:
		2		==		createdUser.getLanguageId().intValue();

		// clean up
		api.deleteUser(newUserId);



	}
	def "testUserTransitions"() {

		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();

		println("Getting complete list of user transitions");


		UserTransitionResponseWS[] ret = api.getUserTransitions(null, null);

		then:

		println("UserTransitionResponseWS object sucessfully created.")

		when:
		ret == null
		then:

		println("Transition list should not be empty!");

		when:
		println("Checking ret length")
		then:
		1 		==		ret.length;

		// Check the ids of the returned transitions

		//			when:
		//			println("Testing the transitions")
		//
		//			then:
		//
		//             ret[0].getId().intValue()		==		 1
		//             ret[1].getId().intValue()		==		 2
		//            // Check the value of returned data
		//             ret[0].getUserId().intValue()	==		 2
		//             ret[0].getFromStatusId().intValue()		==		 2
		//             ret[0].getToStatusId().intValue()		==		  1
		//             ret[1].getUserId().intValue()		==		  2
		//             ret[1].getFromStatusId().intValue()		==		  2
		//             ret[1].getToStatusId().intValue()		==		  1
		//
		// save an ID for later
		when:
		Integer myId = ret[4].getId();

		println("Getting first partial list of user transitions");
		ret =  api.getUserTransitions(new Date(2000 - 1900,0,0),
				new Date(2007 - 1900, 0, 1));
		ret == null

		then:
		thrown(Exception)
		println("Transition list should not be empty!");

		when:
		println("Testing the transitions")

		then:
		ret.length		==		  1
		ret[0].getId().intValue()		==		  1
		ret[0].getUserId().intValue()		==		  2
		ret[0].getFromStatusId().intValue()		==		  2
		ret[0].getToStatusId().intValue()		==		  1

		when:
		println("Getting second partial list of user transitions");
		ret = api.getUserTransitions(null,null);
		ret == null
		then:

		println("Transition list should not be empty!");

		when:
		println("Testing the transitions")
		then:
		5		==		  ret.length
		ret[0].getId().intValue()		==		  2
		ret[0].getUserId().intValue()		==		  2
		ret[0].getFromStatusId().intValue()		==		  2
		ret[0].getToStatusId().intValue()		==		  1

		when:

		println("Getting list after id");
		ret = api.getUserTransitionsAfterId(myId);
		ret == null

		then:
		thrown(Exception)

		when:
		println("Transition list should not be empty!");

		then:
		1			==			ret.length


	}

	def "testAuthentication"() {
		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();

		println("Auth with wrong credentials");

		Integer result = api.authenticate("authuser", "notAGoodOne");


		then:
		WebServicesConstants.AUTH_WRONG_CREDENTIALS		==		 result
		when:
		result = api.authenticate("authuser", "notAGoodOne");


		then:
		WebServicesConstants.AUTH_WRONG_CREDENTIALS		==		 result
		when:
		println("Too many retries");
		result = api.authenticate("authuser", "notAGoodOne");

		then:
		WebServicesConstants.AUTH_LOCKED		==		 result

		// it is locked, but we know the secret password
		when:
		println("Auth for expired");
		result = api.authenticate("authuser", "totalSecret");

		println("\n\n>>>>>>>>>>>>>>>>>>>>>>>>>>"+result)

		then:
		(WebServicesConstants.AUTH_EXPIRED-3)		==		 result

		// update the user's password
		when:
		Integer userId = api.getUserId("authuser");
		UserWS user = api.getUserWS(userId);
		user.setPassword("234qwe");
		api.updateUser(user);

		// try again ...
		println("Auth after password update");
		result = api.authenticate("authuser", "234qwe");

		then:
		WebServicesConstants.AUTH_OK		==		 result





	}

	///*
	//          Parent 1 10752
	//                |
	//         +----+ ---------+-------+
	//         |    |          |       |
	// 10753 iCh1  Ch2 10754  Ch6     iCh7
	//        /\    |                  |
	//       /  \   |                 Ch8
	//    Ch3 iCh4 Ch5
	//  10755 10756 10757
	//
	//Ch3->Ch1
	//Ch4->Ch4
	//Ch1->Ch1
	//Ch5->P1
	//Ch2->P1
	//Ch6->P1
	//Ch7-> Ch7 (its own one time order)
	//Ch8: no applicable orders
	//     */
	def "testParentChild"() {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();
		/*
		 * Create - This passes the password validation routine.
		 */
		UserWS newUser = new UserWS();
		newUser.setUserName("parent1");
		newUser.setPassword("asdfasdf1");
		newUser.setLanguageId(new Integer(1));
		newUser.setMainRoleId(new Integer(5));
		newUser.setIsParent(new Boolean(true));
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);

		// add a contact
		ContactWS contact = new ContactWS();
		contact.setEmail("frodo@shire.com");
		newUser.setContact(contact);

		println("Creating parent user ...");
		// do the creation
		Integer parentId = api.createUser(newUser);

		then:
		null		!=		parentId

		// verify the created user
		when:
		println("Getting created user ");
		UserWS retUser = api.getUserWS(parentId);

		then:

		retUser.getUserName()		==       newUser.getUserName()
		new Boolean(true)		==		 retUser.getIsParent()

		when:
		println("Creating child1 user ...");
		// now create the child
		newUser.setIsParent(new Boolean(true));
		newUser.setParentId(parentId);
		newUser.setUserName("child1");
		newUser.setPassword("asdfasdf1");
		newUser.setInvoiceChild(Boolean.TRUE);
		Integer child1Id = api.createUser(newUser);
		//test
		println("Getting created user ");
		retUser = api.getUserWS(child1Id);

		then:

		retUser.getUserName()		==                  newUser.getUserName();
		parentId		==		 retUser.getParentId();
		Boolean.TRUE		==		 retUser.getInvoiceChild();

		// test parent has child id
		when:
		retUser = api.getUserWS(parentId);
		Integer[] childIds = retUser.getChildIds();

		then:

		1		==		 childIds.length;
		child1Id		==		 childIds[0];

		when:
		println("Creating child2 user ...");
		// now create the child
		newUser.setIsParent(new Boolean(true));
		newUser.setParentId(parentId);
		newUser.setUserName("child2");
		newUser.setPassword("asdfasdf1");
		newUser.setInvoiceChild(Boolean.FALSE);
		Integer child2Id = api.createUser(newUser);
		//test
		println("Getting created user ");
		retUser = api.getUserWS(child2Id);

		then:

		retUser.getUserName()		==                  newUser.getUserName();
		parentId		==		 retUser.getParentId();
		Boolean.FALSE		==		 retUser.getInvoiceChild();

		// test parent has child id
		when:

		retUser = api.getUserWS(parentId);
		childIds = retUser.getChildIds();

		then:

		2		==		 childIds.length;
		child2Id		==
				childIds[0].equals(child2Id) ? childIds[0] : childIds[1];

		when:
		println("Creating child6 user ...");
		// now create the child
		newUser.setIsParent(new Boolean(true));
		newUser.setParentId(parentId);
		newUser.setUserName("child6");
		newUser.setPassword("asdfasdf1");
		newUser.setInvoiceChild(Boolean.FALSE);
		Integer child6Id = api.createUser(newUser);
		//test
		println("Getting created user ");
		retUser = api.getUserWS(child6Id);

		then:

		retUser.getUserName()		==                    newUser.getUserName();
		parentId		==		 retUser.getParentId();
		Boolean.FALSE		==		 retUser.getInvoiceChild();

		// test parent has child id
		when:
		retUser = api.getUserWS(parentId);
		childIds = retUser.getChildIds();

		then:

		3		==		 childIds.length;
		child6Id		==	childIds[0].equals(child6Id) ? childIds[0] :   childIds[1].equals(child6Id) ? childIds[1] : childIds[2];

		when:
		println("Creating child7 user ...");
		// now create the child
		newUser.setIsParent(new Boolean(true));
		newUser.setParentId(parentId);
		newUser.setUserName("child7");
		newUser.setPassword("asdfasdf1");
		newUser.setInvoiceChild(Boolean.TRUE);
		Integer child7Id = api.createUser(newUser);
		//test
		println("Getting created user ");
		retUser = api.getUserWS(child7Id);

		then:

		retUser.getUserName()		==                newUser.getUserName();
		parentId		==		 retUser.getParentId();
		Boolean.TRUE		==		 retUser.getInvoiceChild();

		// test parent has child id
		when:

		retUser = api.getUserWS(parentId);
		childIds = retUser.getChildIds();

		then:
		4		==		 childIds.length;

		when:
		println("Creating child8 user ...");
		// now create the child
		newUser.setIsParent(new Boolean(true));
		newUser.setParentId(child7Id);
		newUser.setUserName("child8");
		newUser.setPassword("asdfasdf1");
		newUser.setInvoiceChild(Boolean.FALSE);
		Integer child8Id = api.createUser(newUser);
		//test
		println("Getting created user ");
		retUser = api.getUserWS(child8Id);

		then:

		retUser.getUserName()		==           newUser.getUserName();
		child7Id		==		 retUser.getParentId();
		Boolean.FALSE		==		 retUser.getInvoiceChild();

		// test parent has child id
		when:

		retUser = api.getUserWS(child7Id);
		childIds = retUser.getChildIds();

		then:

		1		==		 childIds.length;

		when:
		println("Creating child3 user ...");
		// now create the child
		newUser.setIsParent(new Boolean(false));
		newUser.setParentId(child1Id);
		newUser.setUserName("child3");
		newUser.setPassword("asdfasdf1");
		newUser.setInvoiceChild(Boolean.FALSE);
		Integer child3Id = api.createUser(newUser);
		//test
		println("Getting created user ");
		retUser = api.getUserWS(child3Id);

		then:

		retUser.getUserName()		==                    newUser.getUserName();
		child1Id		==		 retUser.getParentId();
		Boolean.FALSE		==		 retUser.getInvoiceChild();

		// test parent has child id
		when:

		retUser = api.getUserWS(child1Id);
		childIds = retUser.getChildIds();

		then:

		1		==		 childIds.length;
		child3Id		==		 childIds[0];

		when:
		println("Creating child4 user ...");
		// now create the child
		newUser.setIsParent(new Boolean(false));
		newUser.setParentId(child1Id);
		newUser.setUserName("child4");
		newUser.setPassword("asdfasdf1");
		newUser.setInvoiceChild(Boolean.TRUE);
		Integer child4Id = api.createUser(newUser);
		//test
		println("Getting created user ");
		retUser = api.getUserWS(child4Id);

		then:

		retUser.getUserName()		==                    newUser.getUserName();
		child1Id		==		 retUser.getParentId();
		Boolean.TRUE		==		 retUser.getInvoiceChild();

		// test parent has child id
		when:
		retUser = api.getUserWS(child1Id);

		childIds = retUser.getChildIds();

		then:
		2		==		 childIds.length;
		child4Id		==		 childIds[0].equals(child4Id) ? childIds[0] : childIds[1];

		when:
		println("Creating child5 user ...");
		// now create the child
		newUser.setIsParent(new Boolean(false));
		newUser.setParentId(child2Id);
		newUser.setUserName("child5");
		newUser.setPassword("asdfasdf1");
		newUser.setInvoiceChild(Boolean.FALSE);
		Integer child5Id = api.createUser(newUser);
		//test
		println("Getting created user ");
		retUser = api.getUserWS(child5Id);

		then:
		retUser.getUserName()		==                  newUser.getUserName();
		child2Id		==		 retUser.getParentId();
		Boolean.FALSE		==		 retUser.getInvoiceChild();

		// test parent has child id
		when:
		retUser = api.getUserWS(child2Id);
		childIds = retUser.getChildIds();

		then:

		1		==		 childIds.length;
		child5Id		==		 childIds[0];

		// test authentication of two of them
		when:
		println("Authenticating new users ");

		then:
		new Integer(0)		==		                 api.authenticate("parent1", "asdfasdf1");
		new Integer(0)		==						 api.authenticate("child1", "asdfasdf1");

		when:
		api.authenticate("child1", "asdfasdf1");

		// create an order for all these users
		println("Creating orders for all users");
		OrderWS order = getOrder();
		order.setUserId(parentId);
		api.createOrder(order);
		order = getOrder();
		order.setUserId(child1Id);
		api.createOrder(order);
		order = getOrder();
		order.setUserId(child2Id);
		api.createOrder(order);
		order = getOrder();
		order.setUserId(child3Id);
		api.createOrder(order);
		order = getOrder();
		order.setUserId(child4Id);
		api.createOrder(order);
		order = getOrder();
		order.setUserId(child5Id);
		api.createOrder(order);
		order = getOrder();
		order.setUserId(child6Id);
		api.createOrder(order);
		order = getOrder();
		order.setUserId(child7Id);
		api.createOrder(order);
		// run the billing process for each user, validating the results
		println("Invoicing and validating...");
		// parent1
		Integer[] invoices = api.createInvoice(parentId, false);

		then:

		null		!=		 invoices;
		1		==		 invoices.length;

		when:
		InvoiceWS invoice = api.getInvoiceWS(invoices[0]);

		then:

		"80.00"		==		 invoice.getTotal();
		// child1
		when:
		invoices = api.createInvoice(child1Id, false);

		then:

		null		!=		 invoices;
		1		==		 invoices.length;

		when:
		invoice = api.getInvoiceWS(invoices[0]);

		then:

		"40.00"		==		 invoice.getTotal();
		// child2

		when:
		invoices = api.createInvoice(child2Id, false);
		// CXF returns null for empty arrays
		invoices != null

		then:
		0		==		 invoices.length;

		// child3
		when:
		invoices = api.createInvoice(child3Id, false);

		invoices != null

		then:

		0		==		 invoices.length;

		// child4
		when:
		invoices = api.createInvoice(child4Id, false);

		then:
		null		!=		 invoices;
		1		==		 invoices.length;

		when:
		invoice = api.getInvoiceWS(invoices[0]);

		then:
		"20.00"		==		 invoice.getTotal();
		// child5
		when:
		invoices = api.createInvoice(child5Id, false);
		invoices != null

		then:
		0		==		 invoices.length;

		// child6
		when:
		invoices = api.createInvoice(child6Id, false);
		invoices != null

		then:
		0		==		 invoices.length;

		// child7 (for bug that would ignore an order from a parent if the
		// child does not have any applicable)
		when:
		invoices = api.createInvoice(child7Id, false);

		then:
		null		!=		 invoices;
		1		==		 invoices.length;

		when:
		invoice = api.getInvoiceWS(invoices[0]);

		then:
		"20.00"		==		 invoice.getTotal();

		// clean up
		and:
		api.deleteUser(parentId);
		api.deleteUser(child1Id);
		api.deleteUser(child2Id);
		api.deleteUser(child3Id);
		api.deleteUser(child4Id);
		api.deleteUser(child5Id);
		api.deleteUser(child6Id);
		api.deleteUser(child7Id);
		api.deleteUser(child8Id);





	}

	def "testGetByCC"() {
		// note: this method getUsersByCreditCard seems to have a bug. It does
		// not reutrn Gandlaf if there is not an updateUser call before
		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();
		Integer[] ids = api.getUsersByCreditCard("1152");


		then:
		null		!=		ids;
		425		==		 ids.length; // returns credit cards from both clients?
		// 5 cards from entity 1, 1 card from entity 2
		10902		==		    ids[ids.length - 1].intValue();

		// get the user
		null		!=		api.getUserWS(ids[0]);



	}

	def 	UserWS createUser(boolean goodCC, Integer parentId, Integer currencyId) {
		JbillingAPI api = JbillingAPIFactory.getAPI();

		/*
		 * Create - This passes the password validation routine.
		 */
		UserWS newUser = new UserWS();
		newUser.setUserName("testUserName-" + Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("asdfasdf1");
		newUser.setLanguageId(new Integer(1));
		newUser.setMainRoleId(new Integer(5));
		newUser.setParentId(parentId); // this parent exists
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(currencyId);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);

		// add a contact
		ContactWS contact = new ContactWS();
		contact.setEmail("frodo@shire.com");
		contact.setFirstName("Frodo");
		contact.setLastName("Baggins");
		String []fields = new String[2];
		fields[0] = "1";
		fields[1] = "2"; // the ID of the CCF for the processor
		String []fieldValues = new String[2];
		fieldValues[0] = "serial-from-ws";
		fieldValues[1] = "FAKE_2"; // the plug-in parameter of the processor
		contact.setFieldNames(fields);
		contact.setFieldValues(fieldValues);
		newUser.setContact(contact);

		// add a credit card
		CreditCardDTO cc = new CreditCardDTO();
		cc.setName("Frodo Baggins");
		cc.setNumber(goodCC ? "4111111111111152" : "4111111111111111");

		// valid credit card must have a future expiry date to be valid for payment processing
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
		cc.setExpiry(expiry.getTime());

		newUser.setCreditCard(cc);

		println("Creating user ...");
		newUser.setUserId(api.createUser(newUser));

		return newUser;
	}
	//
	def Integer createMainSubscriptionOrder(Integer userId,Integer itemId) {
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// create an order for this user
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(2); // monthly
		order.setCurrencyId(1); // USD

		// a main subscription order
		order.setIsCurrent(1);
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2009, 1, 1);
		order.setActiveSince(cal.getTime());

		// order lines
		OrderLineWS[] lines = new OrderLineWS[2];
		lines[0] = new OrderLineWS();
		lines[0].setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		lines[0].setQuantity(1);
		lines[0].setItemId(itemId);
		// take the price and description from the item
		lines[0].setUseItem(true);

		lines[1] = new OrderLineWS();
		lines[1].setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		lines[1].setQuantity(3);
		lines[1].setItemId(1); // lemonade
		// take the price and description from the item
		lines[1].setUseItem(true);

		// attach lines to order
		order.setOrderLines(lines);

		// create the order
		return api.createOrder(order);
	}

	def OrderWS getOrder() {
		// need an order for it
		OrderWS newOrder = new OrderWS();
		newOrder.setUserId(new Integer(-1)); // it does not matter, the user will be created
		newOrder.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		newOrder.setPeriod(new Integer(1)); // once
		newOrder.setCurrencyId(new Integer(1));

		// now add some lines
		OrderLineWS []lines = new OrderLineWS[2];
		OrderLineWS line;

		line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("First line");
		line.setItemId(new Integer(1));
		lines[0] = line;

		line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("Second line");
		line.setItemId(new Integer(2));
		lines[1] = line;

		newOrder.setOrderLines(lines);

		return newOrder;
	}

	def "testPendingUnsubscription"() {

		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();
		OrderWS order = api.getLatestOrder(1055);
		order.setActiveUntil(new Date(2008 - 1900, 11 - 1, 1)); // sorry
		api.updateOrder(order);

		then:
		UserDTOEx.SUBSCRIBER_PENDING_UNSUBSCRIPTION		==		 api.getUserWS(1055).getSubscriberStatusId();

	}

	def "testCurrency"() {
		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();
		UserWS myUser = createUser(true, null, 11);
		Integer myId = myUser.getUserId();
		println("Checking currency of new user");
		myUser = api.getUserWS(myId);

		then:
		11		==		 myUser.getCurrencyId().intValue();

		when:
		myUser.setCurrencyId(1);
		println("Updating currency to US\$");
		myUser.setPassword(null); // otherwise it will try the encrypted password
		api.updateUser(myUser);
		println("Checking currency ...");
		myUser = api.getUserWS(myId);

		then:
		1		==		 myUser.getCurrencyId().intValue();
		println("Removing");
		api.deleteUser(myId);
	}

	def "testPrePaidBalance"() {

		when:
		
		JbillingAPI api = JbillingAPIFactory.getAPI();
		
		UserWS myUser = createUser(true, null, null);
		
		Integer myId = myUser.getUserId();

		// update to pre-paid
		myUser.setBalanceType(Constants.BALANCE_PRE_PAID);
		
		api.updateUser(myUser);

		// get the current balance, it should be null or 0
		println("Checking initial balance type and dynamic balance");
		
		myUser = api.getUserWS(myId);

		then:

		Constants.BALANCE_PRE_PAID		==		 myUser.getBalanceType();
		BigDecimal.ZERO		==		 myUser.getDynamicBalanceAsDecimal();
		

		// validate. room = 0, price = 7

		when:
		
		println("Validate with fields...");
		PricingField []pf=  [ new PricingField("src", "604"),new PricingField("dst", "512")];
		ValidatePurchaseWS result = api.validatePurchase(myId, 2800, pf);

		then:
		
		Boolean.valueOf(true)	==	 result.getSuccess();
		Boolean.valueOf(false)	==	 result.getAuthorized();

		BigDecimal.ZERO		==		 result.getQuantityAsDecimal();


		// add a payment
		when:
		
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("20.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(Constants.PAYMENT_METHOD_CHEQUE);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setResultId(Constants.RESULT_ENTERED);
		payment.setCurrencyId(new Integer(1));
		payment.setUserId(myId);

		PaymentInfoChequeDTO cheque = new PaymentInfoChequeDTO();
		cheque.setBank("ws bank");
		cheque.setDate(Calendar.getInstance().getTime());
		cheque.setNumber("2232-2323-2323");
		payment.setCheque(cheque);

		println("Applying payment");
		api.applyPayment(payment, null);
		// check new balance is 20
		println("Validating new balance");
		myUser = api.getUserWS(myId);

		then:
		
		new BigDecimal("20")		==		myUser.getDynamicBalanceAsDecimal();

		// validate. room = 20, price = 7
		
		when:
		
		println("Validate with fields...");
		
		result = api.validatePurchase(myId, 2800, pf);
		
		then:
		
		Boolean.valueOf(true)		==		 result.getSuccess();
		Boolean.valueOf(true)	==		 result.getAuthorized();
		new BigDecimal("2.8571428571")		==		 result.getQuantityAsDecimal();

		// validate without item id (mediation should set item)
		// duration field needed for rule to fire
		when:

		PricingField[] pf2 = [ new PricingField("src", "604"),
			new PricingField("dst", "512"),
			new PricingField("duration", 1),
			new PricingField("userfield", myUser.getUserName()),
			new PricingField("start", new Date()) ];
		println("Validate with fields and without itemId...");
		result = api.validatePurchase(myId, null, pf2);

		then:
		
		Boolean.valueOf(true)		==		  result.getSuccess();
		Boolean.valueOf(true)		==		  result.getAuthorized();
		new BigDecimal("2.8571428571")		==		  result.getQuantityAsDecimal();

		// now create a one time order, the balance should decrease
		when:
		
		OrderWS order = getOrder();
		order.setUserId(myId);
		println("creating one time order");
		Integer orderId = api.createOrder(order);
		println("Validating new balance");
		myUser = api.getUserWS(myId);

		then:
		
		BigDecimal.ZERO		==		  myUser.getDynamicBalanceAsDecimal();

		// for the following, use line 2 with item id 2. item id 1 has
		// cancellation fees rules that affect the balance.
		// increase the quantity of the one-time order

		when:
		
		println("adding quantity to one time order");
		pause(2000); // pause while provisioning status is being updated
		order = api.getOrder(orderId);
		OrderLineWS line = order.getOrderLines()[0].getItemId() == 2 ? order.getOrderLines()[0] : order.getOrderLines()[1];
		line.setQuantity(7);
		line.setAmount(line.getQuantityAsDecimal().multiply(line.getPriceAsDecimal()));

		BigDecimal delta = new BigDecimal("6.00").multiply(line.getPriceAsDecimal());
		api.updateOrder(order);
		myUser = api.getUserWS(myId);

		then:
		
		delta.negate()		==		 myUser.getDynamicBalanceAsDecimal();

		// decrease the quantity of the one-time order
		when:
		
		println("remove quantity to one time order");

		order = api.getOrder(orderId);
		line = order.getOrderLines()[0].getItemId() == 2 ? order.getOrderLines()[0] : order.getOrderLines()[1];
		line.setQuantity(1);
		line.setAmount(line.getQuantityAsDecimal().multiply(order.getOrderLines()[1].getPriceAsDecimal()));
		api.updateOrder(order);
		myUser = api.getUserWS(myId);

		then:
		
		BigDecimal.ZERO		==		 myUser.getDynamicBalanceAsDecimal();

		// delete one line from the one time order

		when:
		
		println("remove one line from one time order");
		order = api.getOrder(orderId);
		line = order.getOrderLines()[0].getItemId() == 1 ? order.getOrderLines()[0] : order.getOrderLines()[1];
		
		def 	ar 		=	new OrderLineWS[1];
		
		ar[0]			=	line;
				
		order.setOrderLines(ar);
		api.updateOrder(order);
		myUser = api.getUserWS(myId);

		then:

		new BigDecimal("10")		==		 myUser.getDynamicBalanceAsDecimal();

		// validate. room = 10, price = 10
		when:
	
			println("Validate with fields...");
		result = api.validatePurchase(myId, 1, null); // lemonade!
		
		then:
		
		Boolean.valueOf(true)		==		 result.getSuccess();
		Boolean.valueOf(true)		==		 result.getAuthorized();
		Constants.BIGDECIMAL_ONE		==		 result.getQuantityAsDecimal();


		// delete the order, the balance has to go back to 20
		when:
		
		println("deleting one time order");
		api.deleteOrder(orderId);
		println("Validating new balance");
		myUser = api.getUserWS(myId);

		then:
		
		new BigDecimal("20")		==		 myUser.getDynamicBalanceAsDecimal();

		// now create a recurring order with invoice, the balance should decrease

		when:
		
		order = getOrder();
		order.setUserId(myId);
		order.setPeriod(2);

		// make it half a month to test pro-rating
		order.setActiveSince(new DateMidnight(2009,1,1).toDate());
		order.setActiveUntil(new DateMidnight(2009,1,1).plusDays(15).toDate());

			println("creating recurring order and invoice");
            api.createOrderAndInvoice(order);
            println("Validating new balance");
            myUser = api.getUserWS(myId);

			
			
		then:
            new BigDecimal("10.32")		==			 myUser.getDynamicBalanceAsDecimal();

            println("Removing");        
		
		api.deleteUser(myId);

	}

	def "testCreditLimit"() {

		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();
		UserWS myUser = createUser(true, null, null);
		Integer myId = myUser.getUserId();

		// update to pre-paid
		myUser.setBalanceType(Constants.BALANCE_CREDIT_LIMIT);
		myUser.setCreditLimit(new BigDecimal("1000.0"));
		api.updateUser(myUser);

		// validate. room = 1000, price = 7
		println("Validate with fields...");
		PricingField []pf =  [
			new PricingField("src", "604"),
			new PricingField("dst", "512")
		];
		ValidatePurchaseWS result = api.validatePurchase(myId, 2800, pf); // long distance calls for rate card.

		then:
		Boolean.valueOf(true)		==		 result.getSuccess();
		Boolean.valueOf(true)		==		 result.getAuthorized();
		new BigDecimal("142.8571428571")		==		 result.getQuantityAsDecimal();


		// get the current balance, it should be null or 0
		when:
		println("Checking initial balance type and dynamic balance");
		myUser = api.getUserWS(myId);

		then:
		Constants.BALANCE_CREDIT_LIMIT		==		 myUser.getBalanceType();
		BigDecimal.ZERO		==		 myUser.getDynamicBalanceAsDecimal();

		// now create a one time order, the balance should increase
		when:
		OrderWS order = getOrder();
		order.setUserId(myId);
		println("creating one time order");
		Integer orderId = api.createOrder(order);
		println("Validating new balance");
		myUser = api.getUserWS(myId);

		then:
		new BigDecimal("20.0")		==		 myUser.getDynamicBalanceAsDecimal();

		// validate. room = 980, price = 10
		when:
		println("Validate with fields...");
		result = api.validatePurchase(myId, 1, null); // lemonade!
		then:
		Boolean.valueOf(true)		==		 result.getSuccess();
		Boolean.valueOf(true)		==		 result.getAuthorized();
		new BigDecimal("98.0")		==		 result.getQuantityAsDecimal();

		// delete the order, the balance has to go back to 0
		when:
		println("deleting one time order");
		api.deleteOrder(orderId);
		println("Validating new balance");
		myUser = api.getUserWS(myId);

		then:
		BigDecimal.ZERO		==		 myUser.getDynamicBalanceAsDecimal();

		// now create a recurring order with invoice, the balance should increase
		when:
		order = getOrder();
		order.setUserId(myId);
		order.setPeriod(2);
		println("creating recurring order and invoice");
		Integer invoiceId = api.createOrderAndInvoice(order);
		println("Validating new balance");
		myUser = api.getUserWS(myId);

		then:
		new BigDecimal("20.0")		==		 myUser.getDynamicBalanceAsDecimal();

		// validate. room = 980, price = 7
		when:
		println("Validate with fields...");
		result = api.validatePurchase(myId, 1, pf);
		then:
		Boolean.valueOf(true)		==		 result.getSuccess();
		Boolean.valueOf(true)		==		 result.getAuthorized();
		// rules limit max to 3 lemonades
		new BigDecimal("2.0")		==		 result.getQuantityAsDecimal();


		// add a payment. I'd like to call payInvoice but it's not finding the CC
		when:
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("20.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(Constants.PAYMENT_METHOD_CHEQUE);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setResultId(Constants.RESULT_ENTERED);
		payment.setCurrencyId(new Integer(1));
		payment.setUserId(myId);

		PaymentInfoChequeDTO cheque = new PaymentInfoChequeDTO();
		cheque.setBank("ws bank");
		cheque.setDate(Calendar.getInstance().getTime());
		cheque.setNumber("2232-2323-2323");
		payment.setCheque(cheque);

		println("Applying payment");
		api.applyPayment(payment, invoiceId);
		// check new balance is 20
		println("Validating new balance");
		myUser = api.getUserWS(myId);

		then:
		BigDecimal.ZERO		==		 myUser.getDynamicBalanceAsDecimal();

		println("Removing");
		api.deleteUser(myId);

	}

	def "testRulesValidatePurchaseTask"() {
		when:
		// see ValidatePurchaseRules.drl

		JbillingAPI api = JbillingAPIFactory.getAPI();

		// create user
		UserWS user = createUser(true, null, null);
		Integer userId = user.getUserId();
		// update to credit limit
		user.setBalanceType(Constants.BALANCE_CREDIT_LIMIT);
		user.setCreditLimit(new BigDecimal("1000.0"));
		api.updateUser(user);

		// create main subscription order, lemonade plan
		Integer orderId = createMainSubscriptionOrder(userId, 2);

		// validate that the user does have the new main order
		println("Validate that new order is the user's main order");
		then:
		orderId		==		        api.getUserWS(user.getUserId()).getMainOrderId();

		// try to get another lemonde
		when:
		ValidatePurchaseWS result = api.validatePurchase(userId, 1, null);
		Boolean.valueOf(true)		==		 result.getSuccess();
		Boolean.valueOf(false)		==		 result.getAuthorized();
		BigDecimal.ZERO		==		 result.getQuantityAsDecimal();


		then:
		"No more than 3 lemonades are allowed."		==              result.getMessage()[0];


		// exception should be thrown
		when:
		PricingField []pf = [
			new PricingField("fail", "fail")
		];
		result = api.validatePurchase(userId, 1, pf);

		then:
		Boolean.valueOf(false)		==		 result.getSuccess();
		Boolean.valueOf(false)		==		 result.getAuthorized();
		BigDecimal.ZERO		==		 result.getQuantityAsDecimal();

		"Error: java.lang.RuntimeException: Throw exception rule"		==                result.getMessage()[0];


		// coffee quantity available should be 20
		when:
		result = api.validatePurchase(userId, 3, null);

		then:
		Boolean.valueOf(true)		==		 result.getSuccess();
		Boolean.valueOf(true)		==		 result.getAuthorized();
		new BigDecimal("20.0")		==		 result.getQuantityAsDecimal();


		// add 10 coffees to current order
		when:
		OrderLineWS newLine = new OrderLineWS();
		newLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		newLine.setItemId(new Integer(3));
		newLine.setQuantity(new BigDecimal("10.0"));
		// take the price and description from the item
		newLine.setUseItem(new Boolean(true));

		// update the current order

		def 	ar 		=	new OrderLineWS[1];

		ar[0]			=	newLine;
		OrderWS currentOrderAfter = api.updateCurrentOrder(userId,ar, null, new Date(),"Event from WS");

		// quantity available should be 10
		result = api.validatePurchase(userId, 3, null);

		then:
		Boolean.valueOf(true)		==		 result.getSuccess();
		Boolean.valueOf(true)		==		 result.getAuthorized();
		new BigDecimal("10.0")		==		 result.getQuantityAsDecimal();

		// add another 10 coffees to current order

		when:
		currentOrderAfter = api.updateCurrentOrder(userId,ar, null, new Date(),"Event from WS");

		// quantity available should be 0
		result = api.validatePurchase(userId, 3, null);

		then:
		Boolean.valueOf(true)		==		 result.getSuccess();
		Boolean.valueOf(false)		==		 result.getAuthorized();
		BigDecimal.ZERO		==		 result.getQuantityAsDecimal();

		"No more than 20 coffees are allowed."		==     result.getMessage()[0];


		// clean up
		api.deleteOrder(orderId);
		api.deleteUser(userId);

	}


	def "testUserBalancePurchaseTaskHierarchica6l"() {

		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// create 2 users, child and parent
		UserWS newUser = new UserWS();
		newUser.setUserName("parent1");
		newUser.setPassword("asdfasdf1");
		newUser.setLanguageId(new Integer(1));
		newUser.setMainRoleId(new Integer(5));
		newUser.setIsParent(new Boolean(true));
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setBalanceType(Constants.BALANCE_CREDIT_LIMIT);
		newUser.setCreditLimit(new BigDecimal("2000.0"));

		// add a contact
		ContactWS contact = new ContactWS();
		contact.setEmail("frodo@shire.com");
		newUser.setContact(contact);

		println("Creating parent user ...");
		// do the creation
		Integer parentId = api.createUser(newUser);

		// now create the child
		newUser.setIsParent(new Boolean(false));
		newUser.setParentId(parentId);
		newUser.setUserName("child1");
		newUser.setPassword("asdfasdf1");
		newUser.setInvoiceChild(Boolean.FALSE);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
		newUser.setCreditLimit((String) null);
		Integer childId = api.createUser(newUser);

		// validate a purchase for the child
		// validate. room = 2000, price = 7
		println("Validate with fields...");

		PricingField []pf =  [
			new PricingField("src", "604"),
			new PricingField("dst", "512")
		];

		ValidatePurchaseWS result = api.validatePurchase(childId, 2800, pf); // Long Distance for rate card test


		then:

		result			==		null

		thrown(Exception)

		Boolean.valueOf(true)		==		 result.getSuccess();

		Boolean.valueOf(true)		==		  result.getAuthorized();

		new BigDecimal("285.7143")		==		 result.getQuantityAsDecimal();

		// create an order for the child

		when:

		OrderWS order = getOrder();

		order.setUserId(childId);

		println("creating one time order");

		Integer orderId = api.createOrder(order);

		// validate the balance of the parent

		println("Validating new balance");

		UserWS parentUser = api.getUserWS(parentId);

		then:

		new BigDecimal("20.0")		==		  parentUser.getDynamicBalanceAsDecimal();

		// validate another purchase for the child
		// validate. room = 1980, price = 10

		when:

		println("Validate with fields...");

		result = api.validatePurchase(childId, 1, null);

		then:
		Boolean.valueOf(true)		==		  result.getSuccess();
		Boolean.valueOf(true)		==		  result.getAuthorized();
		new BigDecimal("198.0")		==		  result.getQuantityAsDecimal();

		// clean up
		api.deleteUser(parentId);
		api.deleteUser(childId);


	}

	//	    def "testValidateMultiPurchase"() {
	//	        when:
	//	            JbillingAPI api = JbillingAPIFactory.getAPI();
	//	            UserWS myUser = createUser(true, null, null);
	//	            Integer myId = myUser.getUserId();
	//
	//	            // update to credit limit
	//	            myUser.setBalanceType(Constants.BALANCE_CREDIT_LIMIT);
	//	            myUser.setCreditLimit(new BigDecimal("1000.0"));
	//	            api.updateUser(myUser);
	//
	//	            // validate with items only
	//				def ar6		=	new Integer[3];
	//
	//				ar6[0]			=	2800;
	//
	//				ar6[1]			=	2;
	//
	//				ar6[2]			=	251 ;
	//
	//
	//	            ValidatePurchaseWS result = api.validateMultiPurchase(myId, ar6, null);
	//
	//
	//					then:
	//
	//	               Boolean.valueOf(true)		==		 result.getSuccess();
	//
	//				   Boolean.valueOf(true)		==		 result.getAuthorized();
	//
	//					new BigDecimal("28.5714285714")		==		 result.getQuantityAsDecimal();
	//
	//	            // validate with pricing fields
	//
	//				   when:
	//
	//				   PricingField  []pf = new PricingField[2];
	//
	//				   pf[0]	= new PricingField("src", "604");
	//
	//				   pf[1]	=  new PricingField("dst", "512") ;
	//
	//				   println(">>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<+"+pf)
	//
	//				   Integer []ar3		=	new Integer[3];
	//
	//
	//				//new PricingField[][] { pf, pf, pf }
	//
	//				//ar3		=	[2800,2800,2800];
	//				ar3[0]			= 	2800;
	//				ar3[1]			=	2800;
	//				ar3[2]			=	2800;
	//
	//
	//				println(ar3);
	//
	//				println("This is pf >>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+pf);
	//
	//				 [][]ar4		=	 new PricingField[1][2];
	//
	//				ar4[0][0]			=	pf;
	//
	//				ar4[0][1]			=	pf;
	//
	//				ar4[0][2]			=	pf;
	//
	//				println("This is ar4 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+ar4);
	//
	//				//new PricingField[][] { pf, pf, pf }
	//
	//	            result = api.validateMultiPurchase(myId,ar3,ar4);
	//
	//				then:
	//	               Boolean.valueOf(true)		==		 result.getSuccess();
	//	               Boolean.valueOf(true)		==		 result.getAuthorized();
	//	               new BigDecimal("47.6190")	==		 result.getQuantityAsDecimal();
	//
	//	            // validate without item ids (mediation should set item)
	//	            // duration field needed for rule to fire
	//				when:
	//
	//	          def pf2 = PricingField[] [new PricingField("src", "604"), new PricingField("dst", "512"),
	//	                    new PricingField("duration", 1),
	//	             	    new PricingField("userfield", myUser.getUserName()),
	//	                    new PricingField("start", new Date()) ];
	//
	//	              println("Validate with fields and without itemId...");
	//
	////>>>>>>>>>>>>>>>>>>>>>>>>>>Create the local variable here.
	//
	//	            result = api.validateMultiPurchase(myId, null, ar4 );
	//
	//				then:
	//				 Boolean.valueOf(true)		==		 result.getSuccess();
	//	               Boolean.valueOf(true)		==		 result.getAuthorized();
	//	               new BigDecimal("47.6190")		==		 result.getQuantityAsDecimal();
	//
	//
	//	              println("Removing");
	//	            api.deleteUser(myId);
	//
	//}

	// name changed so it is not called in normal test runs
	def XXtestLoad() {

		JbillingAPI api = JbillingAPIFactory.getAPI();
		for (int i = 0; i < 1000; i++) {
			Random rnd = new Random();
			UserWS newUser = createUser(rnd.nextBoolean(), null, null);
			OrderWS newOrder = getOrder();
			// change the quantities for viarety
			newOrder.getOrderLines()[0].setQuantity(rnd.nextInt(100) + 1);
			//newOrder.getLines().first().setUseItem(true);
			newOrder.getOrderLines()[newOrder.getOrderLines().length - 1].setQuantity(rnd.nextInt(100) + 1);
			//newOrder.getLines().last().setUseItem(true);
			newOrder.setUserId(newUser.getUserId());
			api.createOrder(newOrder);
		}



	}


	def "testPenaltyTaskOrder"() {

		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();

		final Integer USER_ID = 53;
		final Integer ORDER_ID = 35;
		final Integer PENALTY_ITEM_ID = 270;

		// pluggable BasicPenaltyTask is configured for ageing_step 6
		// test that other status changes will not add a new order item
		UserWS user = api.getUserWS(USER_ID);
		user.setPassword(null);
		user.setStatusId(2);
		api.updateUser(user);

		then:
		2		==		 api.getUserWS(USER_ID).getStatusId().intValue();
		ORDER_ID		==		 api.getLatestOrder(USER_ID).getId();


		// new order will be created with the penalty item when status id = 6
		when:
		user.setStatusId(6);
		api.updateUser(user);

		then:
		6		==		 api.getUserWS(USER_ID).getStatusId().intValue();

		when:
		OrderWS order = api.getLatestOrder(USER_ID);

		then:

		false 				==		 ORDER_ID.equals(order.getId());

		1		==		 order.getOrderLines().length;

		when:
		OrderLineWS line = order.getOrderLines()[0];

		then:
		PENALTY_ITEM_ID		==		 line.getItemId();
		new BigDecimal("10.00")		==		 line.getAmountAsDecimal();

		// delete order and invoice
		api.deleteOrder(order.getId());
	}

	def "testAutoRecharge"() {

		when:

		println("Starting auto-recharge test.");

		JbillingAPI api = JbillingAPIFactory.getAPI();

		UserWS user = createUser(true, null, null);

		user.setBalanceType(Constants.BALANCE_PRE_PAID);
		user.setAutoRecharge(new BigDecimal("25.00")); // automatically charge this user $25 when the balance drops below the threshold
		// company (entity id 1) recharge threshold is set to $5
		api.updateUser(user);
		user = api.getUserWS(user.getUserId());

		then:

		new BigDecimal("25.00")		==		 user.getAutoRechargeAsDecimal();

		// create an order for $10,

		when:

		OrderWS order = new OrderWS();
		order.setUserId(user.getUserId());
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(new Integer(1));
		order.setCurrencyId(new Integer(1));
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2008, 9, 3);
		order.setCycleStarts(cal.getTime());

		OrderLineWS []lines = new OrderLineWS[1];
		OrderLineWS line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("Fist line");
		line.setItemId(new Integer(1));
		lines[0] = line;

		order.setOrderLines(lines);
		Integer orderId = api.createOrder(order); // should emit a NewOrderEvent that will be handled by the DynamicBalanceManagerTask
		// where the user's dynamic balance will be updated to reflect the charges

		// user's balance should be 0 - 10 + 25 = 15 (initial balance, minus order, plus auto-recharge).
		UserWS updated = api.getUserWS(user.getUserId());

		then:

		new BigDecimal("15.00")		==		 updated.getDynamicBalanceAsDecimal();

		// cleanup
		api.deleteOrder(orderId);
		api.deleteUser(user.getUserId());
	}

	void pause(long t) {
		println("pausing for " + t + " ms...");

		Thread.sleep(t);

	}

	def "testUpdateCurrentOrderNewQuantityEvents"() {
		when:
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// create user
		UserWS user = createUser(true, null, null);
		Integer userId = user.getUserId();

		// update to credit limit
		user.setBalanceType(Constants.BALANCE_CREDIT_LIMIT);
		user.setCreditLimit(new BigDecimal("1000.0"));
		api.updateUser(user);

		// create main subscription order, lemonade plan
		Integer orderId = createMainSubscriptionOrder(userId, 2);

		// add 10 coffees to current order
		OrderLineWS newLine = new OrderLineWS();
		newLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		newLine.setItemId(new Integer(3));
		newLine.setQuantity(new BigDecimal("10.0"));
		// take the price and description from the item
		newLine.setUseItem(new Boolean(true));

		// update the current order
		def ar 		= new OrderLineWS[1];

		ar[0]		=	newLine;

		OrderWS currentOrderAfter = api.updateCurrentOrder(userId,
				ar, null, new Date(),
				"Event from WS");

		// check dynamic balance increased (credit limit type)
		user = api.getUserWS(userId);

		then:
		new BigDecimal("150.0")		==		 user.getDynamicBalanceAsDecimal();

		// add another 10 coffees to current order
		when:
		currentOrderAfter = api.updateCurrentOrder(userId,
				ar, null, new Date(),
				"Event from WS");

		// check dynamic balance increased (credit limit type)
		user = api.getUserWS(userId);

		then:
		new BigDecimal("300.0")		==		 user.getDynamicBalanceAsDecimal();

		// update current order using pricing fields

		when:
		PricingField pf = new PricingField("newPrice", new BigDecimal("5.0"));
		PricingField duration = new PricingField("duration", 5); // 5 min
		PricingField dst = new PricingField("dst", "12345678");
		def ar2 		= new PricingField[3];

		ar2[0]		=	pf;
		ar2[1]		=	duration;
		ar2[2]		=	dst;

		currentOrderAfter = api.updateCurrentOrder(userId, null,
				ar2, new Date(),
				"Event from WS");

		// check dynamic balance increased (credit limit type)
		// 300 + (5 minutes * 5.0 price)

		user = api.getUserWS(userId);

		then:

		new BigDecimal("325.0")		==		user.getDynamicBalanceAsDecimal();


		// clean up
		api.deleteOrder(orderId);
		api.deleteUser(userId);

	}

	def "testUserACHCreation"()  {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();
		UserWS newUser = new UserWS();
		newUser.setUserName("testUserName-" + Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("asdfasdf1");
		newUser.setLanguageId(new Integer(1));
		newUser.setMainRoleId(new Integer(5));
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(null);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);

		// add a contact
		ContactWS contact = new ContactWS();
		contact.setEmail("frodo@shire.com");
		contact.setFirstName("Frodo");
		contact.setLastName("Baggins");
		String []fields = new String[2];
		fields[0] = "1";
		fields[1] = "2"; // the ID of the CCF for the processor
		String []fieldValues = new String[2];
		fieldValues[0] = "serial-from-ws";
		fieldValues[1] = "FAKE_2"; // the plug-in parameter of the processor
		contact.setFieldNames(fields);
		contact.setFieldValues(fieldValues);
		newUser.setContact(contact);

		// add a credit card
		CreditCardDTO cc = new CreditCardDTO();
		cc.setName("Frodo Baggins");
		cc.setNumber("4111111111111152");
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
		cc.setExpiry(expiry.getTime());

		newUser.setCreditCard(cc);

		AchDTO ach = new AchDTO();
		ach.setAbaRouting("123456789");
		ach.setAccountName("Frodo Baggins");
		ach.setAccountType(Integer.valueOf(1));
		ach.setBankAccount("123456789");
		ach.setBankName("Shire Financial Bank");

		newUser.setAch(ach);

		println("Creating user with ACH record...");

		newUser.setUserId(api.createUser(newUser));

		UserWS saved = api.getUserWS(newUser.getUserId());

		then:
		null		!=		saved;
		null		!=		saved.getAch();


		"123456789"		==		 saved.getAch().getAbaRouting();

		"Frodo Baggins"		==		 saved.getAch().getAccountName();

		Integer.valueOf(1)		==		 saved.getAch().getAccountType();

		"123456789"		==		 saved.getAch().getBankAccount();

		"Shire Financial Bank"		==		 saved.getAch().getBankName();


		when:
		println("Passed ACH record creation test");

		ach = saved.getAch();
		ach.setBankAccount("987654321");
		api.updateAch(saved.getUserId(), ach);

		saved = api.getUserWS(newUser.getUserId());

		then:

		null		!=		saved;
		null		!=		saved.getAch();

		"987654321"		==		 saved.getAch().getBankAccount();

		println("Passed ACH record update test");

		null		==     		api.getAutoPaymentType(newUser.getUserId());

		when:
		api.setAutoPaymentType(newUser.getUserId(),
				Constants.AUTO_PAYMENT_TYPE_ACH, true);


		then:
		null		!=       		api.getAutoPaymentType(newUser.getUserId());

		Constants.AUTO_PAYMENT_TYPE_ACH			==			api.getAutoPaymentType(newUser.getUserId());
	}

}