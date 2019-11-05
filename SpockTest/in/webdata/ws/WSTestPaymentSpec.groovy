package in.webdata.ws

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Calendar;

import spock.lang.Specification

import com.sapienter.jbilling.server.entity.AchDTO;
import com.sapienter.jbilling.server.entity.CreditCardDTO;
import com.sapienter.jbilling.server.entity.PaymentInfoChequeDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx
import com.sapienter.jbilling.server.payment.PaymentWS
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

public class WSTestPaymentSpec extends Specification {

	Integer STATUS_SUSPENDED = 6; // should be presented in DB with appropriate ageingStep 'suspend' flag


	def "testApplyGet"() {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("15.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(Constants.PAYMENT_METHOD_CHEQUE);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setResultId(Constants.RESULT_ENTERED);
		payment.setCurrencyId(new Integer(1));
		payment.setUserId(new Integer(2));
		payment.setPaymentNotes("Notes");
		payment.setPaymentPeriod(new Integer(1));


		PaymentInfoChequeDTO cheque = new PaymentInfoChequeDTO();
		cheque.setBank("ws bank");
		cheque.setDate(Calendar.getInstance().getTime());
		cheque.setNumber("2232-2323-2323");
		payment.setCheque(cheque);

		println("Applying payment");
		Integer ret = api.applyPayment(payment, new Integer(35));
		println("Created payemnt " + ret);

		then:

		null		!=			ret;

		/*
		 * get
		 */
		//verify the created payment

		when:

		println("Getting created payment");
		PaymentWS retPayment = api.getPayment(ret);

		then:

		null		!=			retPayment;
		retPayment.getResultId()		==		 payment.getResultId();
		retPayment.getCheque().getNumber()		==		 payment.getCheque().getNumber();
		retPayment.getUserId()		==		  payment.getUserId();
		retPayment.getPaymentNotes()		==		 payment.getPaymentNotes();

		retPayment.getPaymentPeriod()		==		 payment.getPaymentPeriod();


		println("Validated created payment and paid invoice");
		null		!=			retPayment.getInvoiceIds();
		true		==	(retPayment.getInvoiceIds().length == 1);
		retPayment.getInvoiceIds()[0]		==		 new Integer(35);


		when:

		InvoiceWS retInvoice = api.getInvoiceWS(retPayment.getInvoiceIds()[0]);

		then:
		null		!=			retInvoice;
		BigDecimal.ZERO		==		 retInvoice.getBalanceAsDecimal();
		new BigDecimal("15")		==		 retInvoice.getTotalAsDecimal();
		retInvoice.getToProcess()		==		 new Integer(0);
		null		!=			retInvoice.getPayments();
		false			==		(retInvoice.getPayments().length == 1);
		//  /          retInvoice.getPayments()[0].intValue()		==		 retPayment.getId();

		/*
		 * get latest
		 */
		//verify the created payment

		when:

		println("Getting latest");
		retPayment = api.getLatestPayment(new Integer(2));

		then:

		null		!=			retPayment;
		ret.intValue()		==		 retPayment.getId();
		retPayment.getResultId()		==		 payment.getResultId();
		retPayment.getCheque().getNumber()		==		 payment.getCheque().getNumber();
		retPayment.getUserId()		==		 payment.getUserId();


		when:

		println("Getting latest - invalid");
		retPayment = api.getLatestPayment(new Integer(13));

		then:

		thrown(Exception)
		println("User 13 belongs to entity 301");


		/*
		 * get last
		 */

		when:
		println("Getting last");
		Integer []retPayments = api.getLastPayments(new Integer(2), new Integer(2));

		then:

		null		!=			 retPayments;
		// fetch the payment


		when:

		retPayment = api.getPayment(retPayments[0]);

		then:
		retPayment.getResultId()		==		 payment.getResultId();
		retPayment.getCheque().getNumber()	==	 payment.getCheque().getNumber();
		retPayment.getUserId()		==		 payment.getUserId();
		true					==		retPayments.length <= 2;

		when:

		println("Getting last - invalid");
		retPayments = api.getLastPayments(new Integer(13),new Integer(2));

		then:
		thrown(Exception)
		println("User 13 belongs to entity 301");


		/*
		 * TODO test refunds. There are no refund WS methods.
		 * Using applyPayment with is_refund = 1 DOES NOT work
		 */

	}

	/**
	 * Test for UserIdFilter, NameFilter, AddressFilter, PhoneFilter, 
	 * CreditCardFilter and IpAddressFilter
	 */
	def "testBlacklistFilters"() {

		Integer userId = 1000; // starting user id

		// expected filter response messages
		String[] message = [
			"Name is blacklisted.",
			//todo: now id filter is switched off, should be "User id is blacklisted.",
			"Name is blacklisted.",
			"Address is blacklisted.",
			"Phone number is blacklisted.",
			"Credit card number is blacklisted.",
			"IP address is blacklisted."
		];

		JbillingAPI api = JbillingAPIFactory.getAPI();

		/*
		 * Loop through users 1000-1005, which should fail on a respective 
		 * filter: UserIdFilter, NameFilter, AddressFilter, PhoneFilter, 
		 * CreditCardFilter or IpAddressFilter
		 */
		for(int i = 0; i < 6;  userId++) {
			// create a new order and invoice it

			when:
			OrderWS order = new OrderWS();
			order.setUserId(userId);
			order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
			order.setPeriod(2);
			order.setCurrencyId(new Integer(1));

			// add a line
			OrderLineWS []lines = new OrderLineWS[1];
			OrderLineWS line;
			line = new OrderLineWS();
			line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
			line.setQuantity(new Integer(1));
			line.setItemId(new Integer(1));
			line.setUseItem(new Boolean(true));
			lines[0] = line;

			order.setOrderLines(lines);

			// create the order and invoice it
			println("Creating and invoicing order ...");
			Integer thisInvoiceId = api.createOrderAndInvoice(order);
			InvoiceWS newInvoice = api.getInvoiceWS(thisInvoiceId);
			Integer orderId = newInvoice.getOrders()[0]; // this is the order that was also created

			then:

			null		!=		orderId;

			// get invoice id

			when:

			InvoiceWS invoice = api.getLatestInvoice(userId);

			then:

			null		!=			 invoice;

			when:

			Integer invoiceId = invoice.getId();

			then:
			null		!=			invoiceId;

			// try paying the invoice

			when:

			println("Trying to pay invoice for blacklisted user ...");


			PaymentAuthorizationDTOEx authInfo = api.payInvoice(invoiceId);

			then:

			null		!=			 authInfo;

			// check that it was failed by the test blacklist filter
			false 		==		authInfo.getResult().booleanValue();
			message[i]		==		 authInfo.getResponseMessage();

			// remove invoice and order
			api.deleteInvoice(invoiceId);
			api.deleteOrder(orderId);

			i++;
		}

	}

	def "testRemoveOnCCChange"() {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();

		final Integer userId = 868; // this is a user with a good CC

		// put a pre-auth record on this user
		api.createOrderPreAuthorize(com.sapienter.jbilling.server.order.WSTest.createMockOrder(userId, 3, new BigDecimal("3.45")));
		Integer orderId = api.getLatestOrder(userId).getId();

		// user should a a pre-auth

		then:
		PaymentWS payment = null;

		for (int paymentId:api.getLastPayments(userId, 10)) {

			when:
			payment = api.getPayment(paymentId);
			payment.getIsPreauth() == 1

			then:
			break;
		}

		when:
		payment == null || payment.getIsPreauth() == 0

		then:
		println("Could not find pre-auth payment for user " + userId);


		// change the credit card
		when:

		UserWS user = api.getUserWS(userId);
		user.getCreditCard().setName("Meriadoc Pipin");
		api.updateCreditCard(userId, user.getCreditCard());
		payment = api.getPayment(payment.getId());
		// the payment should not be there any more
		then:

		println("payment is sucessfully set.")

		when:
		payment != null && payment.getDeleted() == 0

		then:
		println("Pre-auth should've been deleted")


		// clean-up
		api.deleteOrder(orderId);
	}

	/**
	 * Test for BlacklistUserStatusTask. When a user's status moves to
	 * suspended or higher, the user and all their information is 
	 * added to the blacklist.
	 */
	def "testBlacklistUserStatus"() {

		when:
		final Integer USER_ID = 1006; // user id for testing

		// expected filter response messages
		String[] messages = new String[5];
		int i = 0;
		//            messages[i++] = "User id is blacklisted."; // todo: checking by id is switched off now for payment opportunity during ageing
		messages[i++] = "Name is blacklisted.";
		messages[i++] = "Credit card number is blacklisted.";
		messages[i++] = "Address is blacklisted.";
		messages[i++] = "IP address is blacklisted.";
		messages[i] = "Phone number is blacklisted.";

		JbillingAPI api = JbillingAPIFactory.getAPI();

		// check that a user isn't blacklisted
		UserWS user = api.getUserWS(USER_ID);

		then:
		println("userWS has been sucessfully set.")
		// CXF returns null
		if (user.getBlacklistMatches() != null) {
			"User shouldn't be blacklisted yet"		==		user.getBlacklistMatches().length == 0;
		}

		// change their status to suspended

		when:

		user.setStatusId(STATUS_SUSPENDED);
		user.setPassword(null);
		api.updateUser(user);

		// check all their records are now blacklisted
		user = api.getUserWS(USER_ID);

		then:

		Arrays.toString(messages)		==		Arrays.toString(user.getBlacklistMatches());

		// clean-up
		user.setStatusId(UserDTOEx.STATUS_ACTIVE);
		user.setPassword(null);
		api.updateUser(user);


	}

	/**
	 * Tests the PaymentRouterCurrencyTask. 
	 */
	def "testPaymentRouterCurrencyTask"() {

		when:

		final Integer USER_USD = 10730;
		final Integer USER_AUD = 10731;

		JbillingAPI api = JbillingAPIFactory.getAPI();

		// create a new order
		OrderWS order = new OrderWS();
		order.setUserId(USER_USD);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(2);
		order.setCurrencyId(new Integer(1));

		// add a line
		OrderLineWS []lines = new OrderLineWS[1];
		OrderLineWS line;
		line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setItemId(new Integer(1));
		line.setUseItem(new Boolean(true));
		lines[0] = line;

		order.setOrderLines(lines);

		// create the order and invoice it
		println("Creating and invoicing order ...");
		Integer invoiceIdUSD = api.createOrderAndInvoice(order);
		Integer orderIdUSD = api.getLastOrders(USER_USD, 1)[0];

		// try paying the invoice in USD
		println("Making payment in USD...");
		PaymentAuthorizationDTOEx authInfo = api.payInvoice(invoiceIdUSD);

		then:

		true		==		authInfo.getResult().booleanValue();
		authInfo.getProcessor()		==		 "first_fake_processor";

		// create a new order in AUD and invoice it

		when:

		order.setUserId(USER_AUD);
		order.setCurrencyId(11);

		println("Creating and invoicing order ...");
		Integer invoiceIdAUD = api.createOrderAndInvoice(order);
		Integer orderIdAUD = api.getLastOrders(USER_AUD, 1)[0];

		// try paying the invoice in AUD
		println("Making payment in AUD...");

		authInfo = api.payInvoice(invoiceIdAUD);

		then:

		true		==		authInfo.getResult().booleanValue();
		authInfo.getProcessor()		==		 "second_fake_processor";

		// remove invoices and orders
		println("Deleting invoices and orders.");
		api.deleteInvoice(invoiceIdUSD);
		api.deleteInvoice(invoiceIdAUD);
		api.deleteOrder(orderIdUSD);
		api.deleteOrder(orderIdAUD);


	}

	def "testPayInvoice"() {

		when:

		final Integer USER = 1072;

		JbillingAPI api = JbillingAPIFactory.getAPI();

		System.out.println("Getting an invoice paid, and validating the payment.");
		OrderWS order = com.sapienter.jbilling.server.order.WSTest.createMockOrder(USER, 3, new BigDecimal("3.45"));
		Integer invoiceId = api.createOrderAndInvoice(order);
		PaymentAuthorizationDTOEx auth = api.payInvoice(invoiceId);

		println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+auth)

		then:

		null		!=		auth;

		when:

		PaymentWS payment  = api.getLatestPayment(USER);

		then:
		null		!=		payment;
		null		!=		payment.getAuthorizationId();

		api.deleteInvoice(invoiceId);
		api.deleteOrder(api.getLatestOrder(USER).getId());

	}

	def "testProcessPayment"(){


		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();
		final Integer USER_ID = new Integer(1071);

		// first, create two unpaid invoices
		OrderWS order = com.sapienter.jbilling.server.order.WSTest.createMockOrder(USER_ID, 1, new BigDecimal("10.00"));
		Integer invoiceId1 = api.createOrderAndInvoice(order);
		Integer invoiceId2 = api.createOrderAndInvoice(order);

		// create the payment
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("5.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(Constants.PAYMENT_METHOD_VISA);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setCurrencyId(new Integer(1));
		payment.setUserId(USER_ID);

		UserWS user = api.getUserWS(USER_ID);
		CreditCardDTO originalCC= user.getCreditCard();


		/*
		 * try a credit card number that fails
		 */
		CreditCardDTO cc = new CreditCardDTO();
		cc.setName("Frodo Baggins");
		cc.setNumber("4111111111111111");
		cc.setType(Constants.PAYMENT_METHOD_VISA);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 5);
		cc.setExpiry(cal.getTime());
		payment.setCreditCard(cc);

		println("processing payment.");
		PaymentAuthorizationDTOEx authInfo = api.processPayment(payment);

		// check payment failed

		then:

		null		!=			authInfo;
		false		==			authInfo.getResult().booleanValue();

		// check payment has zero balance
		when:

		PaymentWS lastPayment = api.getLatestPayment(USER_ID);

		then:

		null		!=		 lastPayment;
		null		!=		lastPayment.getAuthorizationId();
		new BigDecimal("5")		==   	 lastPayment.getAmountAsDecimal();
		BigDecimal.ZERO			==		 lastPayment.getBalanceAsDecimal();

		// check invoices still have balance

		when:

		InvoiceWS invoice1 = api.getInvoiceWS(invoiceId1);

		then:

		new BigDecimal("10.0")		==		 invoice1.getBalanceAsDecimal();

		when:

		InvoiceWS invoice2 = api.getInvoiceWS(invoiceId1);

		then:

		new BigDecimal("10.0")		==		 invoice2.getBalanceAsDecimal();

		// do it again, but using the credit card on file
		// which is also 41111111111111
		when:

		payment.setCreditCard(null);
		System.out.println("processing payment.");
		authInfo = api.processPayment(payment);
		// check payment has zero balance
		PaymentWS lastPayment2 = api.getLatestPayment(USER_ID);

		then:

		null		!=		lastPayment2;
		null		!=		lastPayment2.getAuthorizationId();
		new BigDecimal("5")		==		 lastPayment2.getAmountAsDecimal();
		BigDecimal.ZERO			==		 lastPayment2.getBalanceAsDecimal();
		false					==		(	lastPayment2.getId() == lastPayment.getId());

		// check invoices still have balance
		when:

		invoice1 = api.getInvoiceWS(invoiceId1);

		then:

		new BigDecimal("10")		==		 invoice1.getBalanceAsDecimal();

		when:
		invoice2 = api.getInvoiceWS(invoiceId1);

		then:
		new BigDecimal("10")		==		 invoice2.getBalanceAsDecimal();


		/*
		 * do a successful payment of $5
		 */
		when:
		cc.setNumber("4111111111111152");
		payment.setCreditCard(cc);
		println("processing payment.");
		authInfo = api.processPayment(payment);

		// check payment successful

		then:
		null		!=		authInfo;
		416			==		 authInfo.getId().intValue();
		true		==		authInfo.getResult().booleanValue();

		// check payment was made

		when:

		lastPayment = api.getLatestPayment(USER_ID);

		then:

		null		!=		lastPayment;
		null		!=		lastPayment.getAuthorizationId();
		lastPayment.getId()		==		 authInfo.getPaymentId().intValue();
		new BigDecimal("5")		==		 lastPayment.getAmountAsDecimal();
		BigDecimal.ZERO			==		 lastPayment.getBalanceAsDecimal();

		// check invoice 1 was partially paid (balance 5)
		when:
		invoice1 = api.getInvoiceWS(invoiceId1);

		then:
		new BigDecimal("5.0")		==		 invoice1.getBalanceAsDecimal();

		// check invoice 2 wan't paid at all
		when:

		invoice2 = api.getInvoiceWS(invoiceId2);

		then:

		new BigDecimal("10.0")		==		 invoice2.getBalanceAsDecimal();


		/*
		 * another payment for $10, this time with the user's credit card
		 */
		// update the credit card to the one that is good
		when:
		api.updateCreditCard(USER_ID, cc);
		// now the payment does not have a cc
		payment.setCreditCard(null);

		payment.setAmount(new BigDecimal("10.00"));

		println("processing payment.");

		authInfo = api.processPayment(payment);

		then:
		// check payment successful
		null		!=		authInfo;
		true		==	 authInfo.getResult().booleanValue();

		// check payment was made

		when:
		lastPayment = api.getLatestPayment(USER_ID);

		then:

		null		!=		lastPayment;
		null		!=		lastPayment.getAuthorizationId();
		new BigDecimal("10")		==		 lastPayment.getAmountAsDecimal();
		BigDecimal.ZERO			==		 lastPayment.getBalanceAsDecimal();

		// check invoice 1 is fully paid (balance 0)
		when:
		invoice1 = api.getInvoiceWS(invoiceId1);

		then:

		BigDecimal.ZERO		==		 invoice1.getBalanceAsDecimal();

		// check invoice 2 was partially paid (balance 5)
		when:
		invoice2 = api.getInvoiceWS(invoiceId2);
		then:

		new BigDecimal("5")		==		 invoice2.getBalanceAsDecimal();


		/* 
		 *another payment for $10
		 */

		when:
		payment.setCreditCard(cc);
		payment.setAmount(new BigDecimal("10.00"));
		println("processing payment.");
		authInfo = api.processPayment(payment);

		// check payment successful

		then:
		null		!=		authInfo;
		true		==		authInfo.getResult().booleanValue();

		// check payment was made
		when:
		lastPayment = api.getLatestPayment(USER_ID);
		then:

		null		!=		lastPayment;
		null		!=		lastPayment.getAuthorizationId();
		new BigDecimal("10")		==		 lastPayment.getAmountAsDecimal();
		new BigDecimal("5")			==		 lastPayment.getBalanceAsDecimal();

		// check invoice 1 balance is unchanged
		when:
		invoice1 = api.getInvoiceWS(invoiceId1);
		then:

		BigDecimal.ZERO		==		 invoice1.getBalanceAsDecimal();

		// check invoice 2 is fully paid (balance 0)
		when:
		invoice2 = api.getInvoiceWS(invoiceId2);
		then:
		BigDecimal.ZERO		==		 invoice2.getBalanceAsDecimal();


		// clean up

		api.updateCreditCard(USER_ID, originalCC);
		println("Deleting invoices and orders.");
		api.deleteInvoice(invoice1.getId());
		api.deleteInvoice(invoice2.getId());
		api.deleteOrder(invoice1.getOrders()[0]);
		api.deleteOrder(invoice2.getOrders()[0]);

	}

	def "testAchFakePayments"()  {

		when:

		JbillingAPI api = JbillingAPIFactory.getAPI();
		UserWS newUser = createUser();
		newUser.setCreditCard(null);

		println("Creating user with ACH record and no CC...");
		Integer userId = api.createUser(newUser);

		newUser = api.getUserWS(userId);
		AchDTO ach = newUser.getAch();
		// CreditCardDTO cc = newUser.getCreditCard();

		println("Testing ACH payment with even amount (should pass)");
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("15.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(Constants.PAYMENT_METHOD_ACH);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setResultId(Constants.RESULT_ENTERED);
		payment.setCurrencyId(new Integer(1));
		payment.setUserId(newUser.getUserId());
		payment.setPaymentNotes("Notes");
		payment.setPaymentPeriod(new Integer(1));
		payment.setAch(ach);

		PaymentAuthorizationDTOEx result = api.processPayment(payment);

		then:
		Constants.RESULT_OK			==		 api.getPayment(result.getPaymentId()).getResultId();

		when:

		println("Testing ACH payment with odd amount (should fail)");

		payment = new PaymentWS();

		payment.setAmount(new BigDecimal("15.01"));

		payment.setIsRefund(new Integer(0));

		payment.setMethodId(Constants.PAYMENT_METHOD_ACH);

		payment.setPaymentDate(Calendar.getInstance().getTime());

		payment.setResultId(Constants.RESULT_ENTERED);

		payment.setCurrencyId(new Integer(1));

		payment.setUserId(newUser.getUserId());

		payment.setPaymentNotes("Notes");

		payment.setPaymentPeriod(new Integer(1));

		payment.setAch(ach);


		result = api.processPayment(payment);

		then:
		Constants.RESULT_FAIL		==		 api.getPayment(result.getPaymentId()).getResultId();
	}

	def UserWS createUser() {

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

		return newUser;
	}
}
