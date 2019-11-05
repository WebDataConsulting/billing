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

package com.sapienter.jbilling.server.payment;

import java.math.BigDecimal;
import java.util.*;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import com.sapienter.jbilling.server.util.CreateObjectUtil;

/**
 * @author Emil
 */
@Test(groups = {"web-services", "payment"})
public class WSTest {

	private static Integer STATUS_SUSPENDED;

	private static Integer CC_PAYMENT_TYPE;
	private static Integer ACH_PAYMENT_TYPE;
	private static Integer CHEQUE_PAYMENT_TYPE;

	private static Integer CURRENCY_USD;
	private static Integer CURRENCY_AUD;
	private static Integer PRANCING_PONY_ACCOUNT_TYPE;
	private static Integer MORDOR_ACCOUNT_TYPE;
	private static Integer LANGUAGE_ID;
	private static Integer ORDER_CHANGE_STATUS_APPLY;
	private static Integer PAYMENT_PERIOD;
	private static Integer ORDER_PERIOD_ONCE;

	private static JbillingAPI api;
	private static JbillingAPI mordorApi;

	@BeforeClass
	protected void setUp() throws Exception {
		api = JbillingAPIFactory.getAPI();
		mordorApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_MORDOR.name());
		CURRENCY_USD = ServerConstants.PRIMARY_CURRENCY_ID;
		CURRENCY_AUD = Integer.valueOf(11);
		LANGUAGE_ID = ServerConstants.LANGUAGE_ENGLISH_ID;
		PRANCING_PONY_ACCOUNT_TYPE = Integer.valueOf(1);
		MORDOR_ACCOUNT_TYPE = Integer.valueOf(2);
		PAYMENT_PERIOD = Integer.valueOf(1);
		ORDER_PERIOD_ONCE = Integer.valueOf(1);
		ORDER_CHANGE_STATUS_APPLY = getOrCreateOrderChangeStatusApply(api);
		STATUS_SUSPENDED = getOrCreateSuspendedStatus(api);

		CC_PAYMENT_TYPE = api.createPaymentMethodType(PaymentMethodHelper.buildCCTemplateMethod(api));
		ACH_PAYMENT_TYPE = api.createPaymentMethodType(PaymentMethodHelper.buildACHTemplateMethod(api));
		CHEQUE_PAYMENT_TYPE = api.createPaymentMethodType(PaymentMethodHelper.buildChequeTemplateMethod(api));
	}

	@AfterClass
	protected void tearDown() {
		//TODO: should we be able to (soft) delete payment method type if all customers are soft deleted???
		api.deletePaymentMethodType(CC_PAYMENT_TYPE);
		api.deletePaymentMethodType(ACH_PAYMENT_TYPE);
		api.deletePaymentMethodType(CHEQUE_PAYMENT_TYPE);
	}

	/**
	 * Tests payment apply and retrieve.
	 */
	@Test
	public void testApplyGet() {
		//setup
		UserWS mordorUser = buildUser(MORDOR_ACCOUNT_TYPE);
		mordorUser.setId(mordorApi.createUser(mordorUser));

		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
		invoice.setId(api.saveLegacyInvoice(invoice));

		//testing
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("15.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(ServerConstants.PAYMENT_METHOD_CHEQUE);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setResultId(ServerConstants.RESULT_ENTERED);
		payment.setCurrencyId(CURRENCY_USD);
		payment.setUserId(user.getId());
		payment.setPaymentNotes("Notes");
		payment.setPaymentPeriod(PAYMENT_PERIOD);

		PaymentInformationWS cheque = PaymentMethodHelper.createCheque(
				CHEQUE_PAYMENT_TYPE, "ws bank", "2232-2323-2323", Calendar.getInstance().getTime());
		payment.getPaymentInstruments().add(cheque);

		System.out.println("Applying payment");
		Integer paymentId = api.applyPayment(payment, invoice.getId());
		System.out.println("Created payemnt " + paymentId);
		assertNotNull("Didn't get the payment id", paymentId);


		//  get

		//verify the created payment
		System.out.println("Getting created payment");
		PaymentWS retPayment = api.getPayment(paymentId);
		assertNotNull("didn't get payment ", retPayment);

		assertEquals("created payment result", retPayment.getResultId(), payment.getResultId());
		System.out.println("Instruments are: " + retPayment.getPaymentInstruments());

		assertEquals("created payment cheque ",
				getMetaField(retPayment.getPaymentInstruments().iterator().next().getMetaFields(),
						PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue(),
				getMetaField(payment.getPaymentInstruments().iterator().next().getMetaFields(),
						PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue());

		assertEquals("created payment user ", retPayment.getUserId(), payment.getUserId());
		assertEquals("notes", retPayment.getPaymentNotes(), payment.getPaymentNotes());
		assertEquals("period", retPayment.getPaymentPeriod(), payment.getPaymentPeriod());


		System.out.println("Validated created payment and paid invoice");
		assertNotNull("payment not related to invoice", retPayment.getInvoiceIds());
		assertTrue("payment not related to invoice", retPayment.getInvoiceIds().length == 1);
		assertEquals("payment not related to invoice", retPayment.getInvoiceIds()[0], invoice.getId());

		InvoiceWS retInvoice = api.getInvoiceWS(retPayment.getInvoiceIds()[0]);
		assertNotNull("New invoice not present", retInvoice);
		assertEquals("Balance of invoice should be total of order", BigDecimal.ZERO, retInvoice.getBalanceAsDecimal());
		assertEquals("Total of invoice should be total of order", new BigDecimal("15"), retInvoice.getTotalAsDecimal());
		assertEquals("New invoice not paid", retInvoice.getToProcess(), new Integer(0));
		assertNotNull("invoice not related to payment", retInvoice.getPayments());
		assertTrue("invoice not related to payment", retInvoice.getPayments().length == 1);
		assertEquals("invoice not related to payment", retInvoice.getPayments()[0].intValue(), retPayment.getId());


		//  get latest

		//verify the created payment
		System.out.println("Getting latest");
		retPayment = api.getLatestPayment(user.getId());
		assertNotNull("didn't get payment ", retPayment);
		assertEquals("latest id", paymentId.intValue(), retPayment.getId());
		assertEquals("created payment result", retPayment.getResultId(), payment.getResultId());

		assertEquals("created payment cheque ",
			getMetaField(retPayment.getPaymentInstruments().iterator().next().getMetaFields(),
				PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue(),
			getMetaField(payment.getPaymentInstruments().iterator().next().getMetaFields(),
				PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue());

		assertEquals("created payment user ", retPayment.getUserId(), payment.getUserId());

		try {
			System.out.println("Getting latest - invalid");
			api.getLatestPayment(mordorUser.getId());
			fail("User belongs to entity Mordor");
		} catch (Exception e) {
		}

		//  get last

		System.out.println("Getting last");
		Integer retPayments[] = api.getLastPayments(user.getId(), Integer.valueOf(2));
		assertNotNull("didn't get payment ", retPayments);
		// fetch the payment


		retPayment = api.getPayment(retPayments[0]);

		assertEquals("created payment result", retPayment.getResultId(), payment.getResultId());

		assertEquals("created payment cheque ",
				getMetaField(retPayment.getPaymentInstruments().iterator().next().getMetaFields(),
						PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue(),
				getMetaField(payment.getPaymentInstruments().iterator().next().getMetaFields(),
						PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue());

		assertEquals("created payment user ", retPayment.getUserId(), payment.getUserId());
		assertTrue("No more than two records", retPayments.length <= 2);

		try {
			System.out.println("Getting last - invalid");
			api.getLastPayments(mordorUser.getId(), Integer.valueOf(2));
			fail("User belongs to entity Mordor");
		} catch (Exception e) {
		}

		//cleanup
		api.deletePayment(paymentId);
		api.deleteInvoice(invoice.getId());
		api.deleteUser(user.getId());
		mordorApi.deleteUser(mordorUser.getId());
	}

	/**
	 * Test for: NameFilter. For now it uses a value already in DB
	 * for the blacklisted Name. In prepare-test db the name Bilbo Baggins
	 * is blacklisted.
	 * <p/>
	 * TODO: Here we only test two blacklist filters and both of them test
	 * against data already present in the db from prepare test. Due to the
	 * lack of control for modifying the black list from outside for now we
	 * do not test the rest of the filters: UserId, Address, Phone Number,
	 * Ip Address.
	 */
	@Test
	public void testBlacklistNameFilter() {
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE, "Bilbo", "Baggins");
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
		invoice.setId(api.saveLegacyInvoice(invoice));

		// get invoice id
		invoice = api.getLatestInvoice(user.getId());
		assertNotNull("Couldn't get last invoice", invoice);
		Integer invoiceId = invoice.getId();
		assertNotNull("Invoice id was null", invoiceId);

		// try paying the invoice
		System.out.println("Trying to pay invoice for blacklisted user ...");
		PaymentAuthorizationDTOEx authInfo = api.payInvoice(invoiceId);
		assertNotNull("Payment result empty", authInfo);

		// check that it was failed by the test blacklist filter
		assertFalse("Payment wasn't failed for user: " + user.getId(), authInfo.getResult().booleanValue());
		assertEquals("Processor response", "Name is blacklisted.", authInfo.getResponseMessage());

		//cleanup
		api.deleteInvoice(invoiceId);
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
	}

	/**
	 * Test for: CreditCardFilter.For now it uses a value already in DB
	 * for the blacklisted cc number. In prepare-test db the cc number
	 * 5555555555554444 is blacklisted.
	 */
	@Test
	public void testBlacklistCreditCardFilter() {
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE, "5555555555554444");
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
		invoice.setId(api.saveLegacyInvoice(invoice));

		// get invoice id
		invoice = api.getLatestInvoice(user.getId());
		assertNotNull("Couldn't get last invoice", invoice);
		Integer invoiceId = invoice.getId();
		assertNotNull("Invoice id was null", invoiceId);

		// try paying the invoice
		System.out.println("Trying to pay invoice for blacklisted user ...");
		PaymentAuthorizationDTOEx authInfo = api.payInvoice(invoiceId);
		assertNotNull("Payment result empty", authInfo);

		// check that it was failed by the test blacklist filter
		assertFalse("Payment wasn't failed for user: " + user.getId(), authInfo.getResult().booleanValue());
		assertEquals("Processor response", "Credit card number is blacklisted.", authInfo.getResponseMessage());

		//cleanup
		api.deleteInvoice(invoiceId);
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
	}

	/**
	 * Removing pre-authorization when the CC number is changed.
	 */
	@Test
	public void testRemoveOnCCChange() {
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		// put a pre-auth record on this user
		OrderWS order = buildOrder(user.getId(), Arrays.asList(item.getId()), new BigDecimal("3.45"));

		PaymentAuthorizationDTOEx auth = api.createOrderPreAuthorize(
				order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));
		Integer orderId = api.getLatestOrder(user.getId()).getId();

		PaymentWS preAuthPayment = api.getPayment(auth.getPaymentId());
		assertThat(preAuthPayment, is(not(nullValue())));
		assertThat(preAuthPayment.getIsPreauth(), is(1));
		assertThat(preAuthPayment.getDeleted(), is(0)); // NOT deleted

		// update the user's credit card, this should remove the old card
		// and delete any associated pre-authorizations
		DateTimeFormatter format = DateTimeFormat.forPattern(ServerConstants.CC_DATE_FORMAT);
		user = api.getUserWS(user.getId());
		com.sapienter.jbilling.server.user.WSTest.updateMetaField(
				user.getPaymentInstruments().iterator().next().getMetaFields(),
				PaymentMethodHelper.CC_MF_EXPIRY_DATE, format.print(new DateMidnight().plusYears(4).withDayOfMonth(1).toDate().getTime()));
		api.updateUser(user);
		System.out.println("User instruments are: " + user.getPaymentInstruments());
		// validate that the pre-auth payment is no longer available
		preAuthPayment = api.getPayment(auth.getPaymentId());
		assertThat(preAuthPayment, is(not(nullValue())));
		assertThat(preAuthPayment.getIsPreauth(), is(1));
		assertThat(preAuthPayment.getDeleted(), is(1)); // is now deleted

		// cleanup
		api.deleteOrder(orderId);
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
	}

	/**
	 * Test for BlacklistUserStatusTask. When a user's status moves to
	 * suspended or higher, the user and all their information is
	 * added to the blacklist.
	 */
	@Test(enabled = false)
	public void testBlacklistUserStatus() {
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE
				, "BlackListFirst", "BlackListSecond", "4916347258194745");
		user.setId(api.createUser(user));

		// expected filter response messages
		String[] messages = new String[3];
		messages[0] = "User id is blacklisted.";
		messages[1] = "Name is blacklisted.";
		messages[2] = "Credit card number is blacklisted.";

//	    TODO: for now we do not test for these three
//        messages[3] = "Address is blacklisted.";
//        messages[4] = "IP address is blacklisted.";
//        messages[5] = "Phone number is blacklisted.";


		// check that a user isn't blacklisted
		user = api.getUserWS(user.getId());
		// CXF returns null
		if (user.getBlacklistMatches() != null) {
			assertTrue("User shouldn't be blacklisted yet",
					user.getBlacklistMatches().length == 0);
		}

		// change their status to suspended
		user.setStatusId(STATUS_SUSPENDED);
		user.setPassword(null);
		api.updateUser(user);

		// check all their records are now blacklisted
		user = api.getUserWS(user.getId());
		assertEquals("User records should be blacklisted.",
				Arrays.toString(messages),
				Arrays.toString(user.getBlacklistMatches()));


		//cleanup
		api.deleteUser(user.getId());
	}

	/**
	 * Tests the PaymentRouterCurrencyTask.
	 */
	@Test
	public void testPaymentRouterCurrencyTask() {
		//prepare
		UserWS userUSD = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		userUSD.setCurrencyId(CURRENCY_USD);
		userUSD.setId(api.createUser(userUSD));

		UserWS userAUD = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		userAUD.setCurrencyId(CURRENCY_AUD);
		userAUD.setId(api.createUser(userAUD));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		//testing
		OrderWS order = buildOrder(userUSD.getId(), Arrays.asList(item.getId()), new BigDecimal("10"));
		order.setCurrencyId(userUSD.getCurrencyId());

		// create the order and invoice it
		System.out.println("Creating and invoicing order ...");
		Integer invoiceIdUSD = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));
		Integer orderIdUSD = api.getLastOrders(userUSD.getId(), 1)[0];

		// try paying the invoice in USD
		System.out.println("Making payment in USD...");
		PaymentAuthorizationDTOEx authInfo = api.payInvoice(invoiceIdUSD);

		assertTrue("USD Payment should be successful", authInfo.getResult().booleanValue());
		assertEquals("Should be processed by 'first_fake_processor'", authInfo.getProcessor(), "first_fake_processor");

		// create a new order in AUD and invoice it
		order.setUserId(userAUD.getId());
		order.setCurrencyId(userAUD.getCurrencyId());

		System.out.println("Creating and invoicing order ...");
		Integer invoiceIdAUD = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));
		Integer orderIdAUD = api.getLastOrders(userAUD.getId(), 1)[0];

		// try paying the invoice in AUD
		System.out.println("Making payment in AUD...");
		authInfo = api.payInvoice(invoiceIdAUD);

		assertTrue("AUD Payment should be successful", authInfo.getResult().booleanValue());
		assertEquals("Should be processed by 'second_fake_processor'", authInfo.getProcessor(), "second_fake_processor");

		// remove invoices and orders
		System.out.println("Deleting invoices and orders.");
		api.deleteInvoice(invoiceIdUSD);
		api.deleteInvoice(invoiceIdAUD);
		api.deleteOrder(orderIdUSD);
		api.deleteOrder(orderIdAUD);
		api.deleteUser(userUSD.getId());
		api.deleteUser(userAUD.getId());
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
	}

	/**
	 * Test payInvoice(invoice) API call.
	 */
	@Test
	public void testPayInvoice() {
		//setup
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		//testing
		System.out.println("Getting an invoice paid, and validating the payment.");
		OrderWS order = buildOrder(user.getId(), Arrays.asList(item.getId()), new BigDecimal("3.45"));
		Integer invoiceId = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));
		Integer orderId = api.getInvoiceWS(invoiceId).getOrders()[0];
		PaymentAuthorizationDTOEx auth = api.payInvoice(invoiceId);
		assertNotNull("auth can not be null", auth);
		PaymentWS payment = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", payment);
		assertNotNull("auth in payment can not be null", payment.getAuthorizationId());

		//cleanup
		api.deletePayment(auth.getPaymentId());
		api.deleteInvoice(invoiceId);
		api.deleteOrder(orderId);
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
	}

	/**
	 * Tests processPayment API call.
	 */
	@Test
	public void testProcessPayment() {
		//setup
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE, "4111111111111111");
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		// first, create two unpaid invoices
		OrderWS order = buildOrder(user.getId(), Arrays.asList(item.getId()), new BigDecimal("10.00"));
		Integer invoiceId1 = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));
		Integer invoiceId2 = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));

		// create the payment
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("5.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(ServerConstants.PAYMENT_METHOD_VISA);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setCurrencyId(CURRENCY_USD);
		payment.setUserId(user.getId());

		//  try a credit card number that fails
		// note that creating a payment with a NEW credit card will save it and associate
		// it with the user who made the payment.
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 5);

		PaymentInformationWS cc = PaymentMethodHelper.createCreditCard(
				CC_PAYMENT_TYPE, "Frodo Baggins", "4111111111111111", cal.getTime());
		cc.setPaymentMethodId(ServerConstants.PAYMENT_METHOD_VISA);
		payment.getPaymentInstruments().add(cc);

		System.out.println("processing payment.");
		PaymentAuthorizationDTOEx authInfo = api.processPayment(payment, null);

		// check payment failed
		assertNotNull("Payment result not null", authInfo);
		assertFalse("Payment Authorization result should be FAILED", authInfo.getResult().booleanValue());

		// check payment has zero balance
		PaymentWS lastPayment = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("correct payment amount", new BigDecimal("5"), lastPayment.getAmountAsDecimal());
		assertEquals("correct payment balance", BigDecimal.ZERO, lastPayment.getBalanceAsDecimal());

		// check invoices still have balance
		InvoiceWS invoice1 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", new BigDecimal("10.0"), invoice1.getBalanceAsDecimal());
		InvoiceWS invoice2 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", new BigDecimal("10.0"), invoice2.getBalanceAsDecimal());

		// do it again, but using the credit card on file
		// which is also 4111111111111111
		payment.getPaymentInstruments().clear();
		System.out.println("processing payment.");
		authInfo = api.processPayment(payment, null);
		// check payment has zero balance
		PaymentWS lastPayment2 = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", lastPayment2);
		assertNotNull("auth in payment can not be null", lastPayment2.getAuthorizationId());
		assertEquals("correct payment amount", new BigDecimal("5"), lastPayment2.getAmountAsDecimal());
		assertEquals("correct payment balance", BigDecimal.ZERO, lastPayment2.getBalanceAsDecimal());
		assertFalse("Payment is not the same as preiouvs", lastPayment2.getId() == lastPayment.getId());

		// check invoices still have balance
		invoice1 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", new BigDecimal("10"), invoice1.getBalanceAsDecimal());
		invoice2 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", new BigDecimal("10"), invoice2.getBalanceAsDecimal());


		//  do a successful payment of $5
		cc = PaymentMethodHelper.createCreditCard(
				CC_PAYMENT_TYPE, "Frodo Baggins", "4111111111111152", cal.getTime());
		cc.setPaymentMethodId(ServerConstants.PAYMENT_METHOD_VISA);
		payment.getPaymentInstruments().add(cc);
		System.out.println("processing payment.");
		authInfo = api.processPayment(payment, null);

		// check payment successful
		assertNotNull("Payment result not null", authInfo);
		assertNotNull("Auth id not null", authInfo.getId());
		assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

		// check payment was made
		lastPayment = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("payment ids match", lastPayment.getId(), authInfo.getPaymentId().intValue());
		assertEquals("correct payment amount", new BigDecimal("5"), lastPayment.getAmountAsDecimal());
		assertEquals("correct payment balance", BigDecimal.ZERO, lastPayment.getBalanceAsDecimal());

		// check invoice 1 was partially paid (balance 5)
		invoice1 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", new BigDecimal("5.0"), invoice1.getBalanceAsDecimal());

		// check invoice 2 wan't paid at all
		invoice2 = api.getInvoiceWS(invoiceId2);
		assertEquals("correct invoice balance", new BigDecimal("10.0"), invoice2.getBalanceAsDecimal());


		//  another payment for $10, this time with the user's credit card

		// update the credit card to the one that is good
		user = api.getUserWS(user.getId());
		com.sapienter.jbilling.server.user.WSTest.updateMetaField(
				user.getPaymentInstruments().iterator().next().getMetaFields(),
				PaymentMethodHelper.CC_MF_NUMBER, "4111111111111152");
		api.updateUser(user);

		// process a payment without an attached credit card
		// should try and use the user's saved credit card
		payment.getPaymentInstruments().clear();
		payment.setAmount(new BigDecimal("10.00"));
		System.out.println("processing payment.");
		authInfo = api.processPayment(payment, null);

		// check payment successful
		assertNotNull("Payment result not null", authInfo);
		assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

		// check payment was made
		lastPayment = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("correct payment amount", new BigDecimal("10"), lastPayment.getAmountAsDecimal());
		assertEquals("correct payment balance", BigDecimal.ZERO, lastPayment.getBalanceAsDecimal());

		// check invoice 1 is fully paid (balance 0)
		invoice1 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", BigDecimal.ZERO, invoice1.getBalanceAsDecimal());

		// check invoice 2 was partially paid (balance 5)
		invoice2 = api.getInvoiceWS(invoiceId2);
		assertEquals("correct invoice balance", new BigDecimal("5"), invoice2.getBalanceAsDecimal());


		// another payment for $10

		payment.getPaymentInstruments().add(cc);
		payment.setAmount(new BigDecimal("10.00"));
		System.out.println("processing payment.");
		authInfo = api.processPayment(payment, null);

		// check payment successful
		assertNotNull("Payment result not null", authInfo);
		assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

		// check payment was made
		lastPayment = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("correct  payment amount", new BigDecimal("10"), lastPayment.getAmountAsDecimal());
		assertEquals("correct  payment balance", new BigDecimal("5"), lastPayment.getBalanceAsDecimal());

		// check invoice 1 balance is unchanged
		invoice1 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", BigDecimal.ZERO, invoice1.getBalanceAsDecimal());

		// check invoice 2 is fully paid (balance 0)
		invoice2 = api.getInvoiceWS(invoiceId2);
		assertEquals("correct invoice balance", BigDecimal.ZERO, invoice2.getBalanceAsDecimal());

		//cleanup
		System.out.println("Deleting invoices and orders.");
		api.deleteInvoice(invoice1.getId());
		api.deleteInvoice(invoice2.getId());
		api.deleteOrder(invoice1.getOrders()[0]);
		api.deleteOrder(invoice2.getOrders()[0]);
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
	}

	/**
	 * Tests failed and successful payment for ACH
	 */
	@Test
	public void testAchFakePayments() {
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);

		//remove payment instruments and add only ACH payment instrument
		user.getPaymentInstruments().clear();
		user.getPaymentInstruments().add(
				PaymentMethodHelper.createACH(ACH_PAYMENT_TYPE, "Frodo Baggins",
						"Shire Financial Bank", "123456789", "123456789", PRANCING_PONY_ACCOUNT_TYPE));

		System.out.println("Creating user with ACH record and no CC...");
		user.setId(api.createUser(user));

		// get ach
		PaymentInformationWS ach = user.getPaymentInstruments().get(0);

		System.out.println("Testing ACH payment with even amount (should pass)");
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("15.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(ServerConstants.PAYMENT_METHOD_ACH);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setResultId(ServerConstants.RESULT_ENTERED);
		payment.setCurrencyId(CURRENCY_USD);
		payment.setUserId(user.getId());
		payment.setPaymentNotes("Notes");
		payment.setPaymentPeriod(PAYMENT_PERIOD);
		payment.getPaymentInstruments().add(ach);

		PaymentAuthorizationDTOEx resultOne = api.processPayment(payment, null);
		assertEquals("ACH payment with even amount should pass",
				ServerConstants.RESULT_OK, api.getPayment(resultOne.getPaymentId()).getResultId());

		System.out.println("Testing ACH payment with odd amount (should fail)");
		payment = new PaymentWS();
		payment.setAmount(new BigDecimal("15.01"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(ServerConstants.PAYMENT_METHOD_ACH);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setResultId(ServerConstants.RESULT_ENTERED);
		payment.setCurrencyId(CURRENCY_USD);
		payment.setUserId(user.getId());
		payment.setPaymentNotes("Notes");
		payment.setPaymentPeriod(PAYMENT_PERIOD);
		payment.getPaymentInstruments().add(ach);

		PaymentAuthorizationDTOEx resultTwo = api.processPayment(payment, null);
		assertEquals("ACH payment with odd amount should fail",
				ServerConstants.RESULT_FAIL, api.getPayment(resultTwo.getPaymentId()).getResultId());

		//cleanup
		api.deletePayment(resultTwo.getPaymentId());
		api.deletePayment(resultOne.getPaymentId());
		api.deleteUser(user.getId());
	}

	/**
	 * Tries to create payment against review invoice. Here,
	 * instead of using the billing process to generate a review
	 * invoice we are creating a review invoice with the help
	 * of saveLegacyInvoice call.
	 */
	@Test
	public void testPayReviewInvoice() {
		//creating new user
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
		invoice.setIsReview(Integer.valueOf(1));
		invoice.setId(api.saveLegacyInvoice(invoice));

		//check if invoice is a review invoice
		System.out.println("Invoice is review : " + invoice.getIsReview());
		assertEquals("Invoice is a review invoice", Integer.valueOf(1), invoice.getIsReview());

		try {
			//pay for a review invoice
			api.payInvoice(invoice.getId());
			fail("We should not be able to issue a payment against review invoice");
		} catch (SessionInternalError e) {
			System.out.println(e.getMessage());
		}

		//clean up
		api.deleteInvoice(invoice.getId());
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
	}

	@Test
	public void testNewGetPaymentsApiMethods() throws Exception{
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// Create a user with balance $1.00
		UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);

		List<PaymentWS> payments = new ArrayList<PaymentWS>();

		for(int i=0; i<5; i++){
			payments.add(createPaymentWS(user.getUserId(), new DateTime().plusMonths(i).toDate(), String.valueOf(i)));
		}

		//get two latest payments except the latest one.
		Integer[] paymentsId = api.getLastPaymentsPage(user.getUserId(), 2, 1) ;

		assertEquals(2, paymentsId.length);

		assertEquals("3", api.getPayment(paymentsId[0]).getPaymentNotes());
		assertEquals("2", api.getPayment(paymentsId[1]).getPaymentNotes());

		//get the payments between next month and four months from now.
		Integer[] paymentsId2 = api.getPaymentsByDate(user.getUserId(), new DateTime().plusDays(1).toDate() , new DateTime().plusMonths(3).plusDays(1).toDate()) ;

		assertEquals(3, paymentsId2.length);

		assertEquals("3", api.getPayment(paymentsId2[0]).getPaymentNotes());
		assertEquals("2", api.getPayment(paymentsId2[1]).getPaymentNotes());
		assertEquals("1", api.getPayment(paymentsId2[2]).getPaymentNotes());

		//Delete orders
		for(PaymentWS payment : payments){
			api.deletePayment(payment.getId()) ;
		}
		//Delete user
		api.deleteUser(user.getUserId());
	}

	public PaymentWS createPaymentWS(Integer userId, Date date, String note) throws Exception{
		JbillingAPI api = JbillingAPIFactory.getAPI();

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("15.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(ServerConstants.PAYMENT_METHOD_CHEQUE);
		payment.setPaymentDate(date);
		payment.setCreateDatetime(date);
		payment.setResultId(ServerConstants.RESULT_ENTERED);
		payment.setCurrencyId(new Integer(1));
		payment.setUserId(userId);
		payment.setPaymentNotes(note);
		payment.setPaymentPeriod(new Integer(1));

		PaymentInformationWS cheque = com.sapienter.jbilling.server.user.WSTest.
				createCheque("ws bank", "2232-2323-2323", date);

		payment.getPaymentInstruments().add(cheque);
		System.out.println("Applying payment");
		Integer ret = api.applyPayment(payment, new Integer(35));
		System.out.println("Created payemnt " + ret);
		assertNotNull("Didn't get the payment id", ret);

		payment.setId(ret);
		return payment;
	}
	
	/**
	 * Testing the saveLegacyPayment API call
	 */
	@Test
	public void testSaveLegacyPayment() {
		//setup
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		user.setId(api.createUser(user));

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("15.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(ServerConstants.PAYMENT_METHOD_CREDIT);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setResultId(ServerConstants.RESULT_ENTERED);
		payment.setCurrencyId(CURRENCY_USD);
		payment.setUserId(user.getId());
		payment.setPaymentNotes("Notes");
		payment.setPaymentPeriod(PAYMENT_PERIOD);

		Integer paymentId = api.saveLegacyPayment(payment);
		assertNotNull("Payment should be saved", paymentId);

		PaymentWS retPayment = api.getPayment(paymentId);
		assertNotNull(retPayment);
		assertEquals(retPayment.getAmountAsDecimal(), payment.getAmountAsDecimal());
		assertEquals(retPayment.getIsRefund(), payment.getIsRefund());
		assertEquals(retPayment.getMethodId(), payment.getMethodId());
		assertEquals(retPayment.getResultId(), payment.getResultId());
		assertEquals(retPayment.getCurrencyId(), payment.getCurrencyId());
		assertEquals(retPayment.getUserId(), payment.getUserId());
		assertEquals(retPayment.getPaymentNotes(), payment.getPaymentNotes() + " This payment is migrated from legacy system.");
		assertEquals(retPayment.getPaymentPeriod(), payment.getPaymentPeriod());

		//cleanup
		api.deletePayment(retPayment.getId());
		api.deleteUser(user.getId());
	}

	private UserWS buildUser(Integer accountType) {
		return buildUser(accountType, "Frodo", "Baggins", "4111111111111152");
	}

	private UserWS buildUser(Integer accountType, String firstName, String lastName) {
		return buildUser(accountType, firstName, lastName, "4111111111111152");
	}

	private UserWS buildUser(Integer accountType, String ccNumber) {
		return buildUser(accountType, "Frodo", "Baggins", ccNumber);
	}

	private UserWS buildUser(Integer accountTypeId, String firstName, String lastName, String ccNumber) {
		UserWS newUser = new UserWS();
		newUser.setUserName("payment-test-" + Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("Admin123@");
		newUser.setLanguageId(LANGUAGE_ID);
		newUser.setMainRoleId(new Integer(5));
		newUser.setAccountTypeId(accountTypeId);
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(CURRENCY_USD);
		
		MetaFieldValueWS metaField1 = new MetaFieldValueWS();
		metaField1.setFieldName("contact.email");
		metaField1.setValue(newUser.getUserName() + "@shire.com");
		metaField1.setGroupId(accountTypeId);

		MetaFieldValueWS metaField2 = new MetaFieldValueWS();
		metaField2.setFieldName("contact.first.name");
		metaField2.setValue(firstName);
		metaField2.setGroupId(accountTypeId);

		MetaFieldValueWS metaField3 = new MetaFieldValueWS();
		metaField3.setFieldName("contact.last.name");
		metaField3.setValue(lastName);
		metaField3.setGroupId(accountTypeId);

		newUser.setMetaFields(new MetaFieldValueWS[]{
				metaField1,
				metaField2,
				metaField3
		});

		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		// add credit card
		newUser.getPaymentInstruments().add(PaymentMethodHelper
				.createCreditCard(CC_PAYMENT_TYPE, "Frodo Baggins", ccNumber, expiry.getTime()));

		return newUser;
	}

	public OrderWS buildOrder(int userId, List<Integer> itemIds, BigDecimal linePrice) {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
		order.setPeriod(ORDER_PERIOD_ONCE); // once
		order.setCurrencyId(CURRENCY_USD);
		order.setActiveSince(new Date());
		order.setProrateFlag(Boolean.FALSE);

		ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(itemIds.size());
		for (int i = 0; i < itemIds.size(); i++) {
			OrderLineWS nextLine = new OrderLineWS();
			nextLine.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
			nextLine.setDescription("Order line: " + i);
			nextLine.setItemId(itemIds.get(i));
			nextLine.setQuantity(1);
			nextLine.setPrice(linePrice);
			nextLine.setAmount(nextLine.getQuantityAsDecimal().multiply(linePrice));

			lines.add(nextLine);
		}
		order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
		return order;
	}

	private InvoiceWS buildInvoice(Integer userId, Integer itemId) {
		InvoiceWS invoice = new InvoiceWS();
		invoice.setUserId(userId);
		invoice.setNumber("800" + System.currentTimeMillis());
		invoice.setTotal("15");
		invoice.setToProcess(1);
		invoice.setBalance("15");
		invoice.setCurrencyId(CURRENCY_USD);
		invoice.setDueDate(new Date());
		invoice.setPaymentAttempts(1);
		invoice.setInProcessPayment(1);
		invoice.setCarriedBalance("0");

		InvoiceLineDTO invoiceLineDTO = new InvoiceLineDTO();
		invoiceLineDTO.setAmount("15");
		invoiceLineDTO.setDescription("line desc");
		invoiceLineDTO.setItemId(itemId);
		invoiceLineDTO.setPercentage(1);
		invoiceLineDTO.setPrice("15");
		invoiceLineDTO.setQuantity("1");
		invoiceLineDTO.setSourceUserId(userId);

		invoice.setInvoiceLines(new InvoiceLineDTO[]{invoiceLineDTO});
		return invoice;
	}

	private ItemTypeWS buildItemType() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("Invoice, Item Type:" + System.currentTimeMillis());
		type.setOrderLineTypeId(1);//items
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}

	private ItemDTOEx buildItem(Integer itemTypeId, Integer priceModelCompanyId) {
		ItemDTOEx item = new ItemDTOEx();
		long millis = System.currentTimeMillis();
		String name = String.valueOf(millis) + new Random().nextInt(10000);
		item.setDescription("Payment, Product:" + name);
		item.setPriceModelCompanyId(priceModelCompanyId);
		item.setPrices(CreateObjectUtil.setItemPrice(new BigDecimal(10.00), new DateMidnight(1970, 1, 1).toDate(), priceModelCompanyId, Integer.valueOf(1)));
		item.setNumber("PYM-PROD-" + name);
		item.setAssetManagementEnabled(0);
		Integer typeIds[] = new Integer[]{itemTypeId};
		item.setTypes(typeIds);
		return item;
	}

	private MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields, String fieldName) {
		for (MetaFieldValueWS ws : metaFields) {
			if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
				return ws;
			}
		}
		return null;
	}

	private Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
		OrderChangeStatusWS[] statuses = api.getOrderChangeStatusesForCompany();
		for (OrderChangeStatusWS status : statuses) {
			if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
				return status.getId();
			}
		}
		//there is no APPLY status in db so create one
		OrderChangeStatusWS apply = new OrderChangeStatusWS();
		String status1Name = "APPLY: " + System.currentTimeMillis();
		OrderChangeStatusWS status1 = new OrderChangeStatusWS();
		status1.setApplyToOrder(ApplyToOrder.YES);
		status1.setDeleted(0);
		status1.setOrder(1);
		status1.addDescription(new InternationalDescriptionWS(ServerConstants.LANGUAGE_ENGLISH_ID, status1Name));
		return api.createOrderChangeStatus(apply);
	}

	private Integer getOrCreateSuspendedStatus(JbillingAPI api) {
		List<AgeingWS> steps = Arrays.asList(api.getAgeingConfiguration(LANGUAGE_ID));

		for (AgeingWS step : steps) {
			if (step.getSuspended().booleanValue()) {
				return step.getStatusId();
			}
		}

		AgeingWS suspendStep = new AgeingWS();
		suspendStep.setSuspended(Boolean.TRUE);
		suspendStep.setDays(Integer.valueOf(180));
		suspendStep.setStatusStr("Ageing Step 180");
		suspendStep.setFailedLoginMessage("You are suspended");
		suspendStep.setWelcomeMessage("Welcome");
		steps.add(suspendStep);
		api.saveAgeingConfiguration(steps.toArray(new AgeingWS[steps.size()]), LANGUAGE_ID);
		return getOrCreateOrderChangeStatusApply(api);
	}

}
