/*
 * jBilling - The Enterprise Open Source Billing System
 * Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde
 * 
 * This file is part of jbilling.
 * 
 * jbilling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jbilling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */

package com.sapienter.jbilling.server.payment;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import org.joda.time.DateMidnight;
/**
 * jUnit Test cases for jBilling's refund functionality
 * @author Vikas Bodani
 * @since 04/01/12
 */
@Test(groups = { "web-services", "payment" })
public class RefundTest {

	private static Integer CURRENCY_ID;
	private static Integer CURRENCY_GBP_ID;
	private static Integer ACCOUNT_TYPE;
	private static Integer LANGUAGE_ID;
	private static Integer ORDER_CHANGE_STATUS_APPLY;

    private static JbillingAPI api;

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
	    CURRENCY_ID = ServerConstants.PRIMARY_CURRENCY_ID;
	    CURRENCY_GBP_ID = Integer.valueOf(5);//Pound
	    LANGUAGE_ID = ServerConstants.LANGUAGE_ENGLISH_ID;
	    ACCOUNT_TYPE = Integer.valueOf(1);
	    ORDER_CHANGE_STATUS_APPLY = getOrCreateOrderChangeStatusApply(api);
    }

    /**
     * 1. Simplest test scenario - A refund affects linked payments balance.
     */
    @Test
    public void testRefundPayment() {
        System.out.println("testRefundPayment().");

        //create user
        UserWS user= createUser();
        assertTrue(user.getUserId() > 0);
        System.out.println("User created successfully " + user.getUserId());

        //make payment
        Integer paymentId= createPayment(api, "100.00", false, user.getUserId(), null);
        System.out.println("Created payment " + paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check payment balance = payment amount
        PaymentWS payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(payment.getAmountAsDecimal(), payment.getBalanceAsDecimal());

        assertTrue(payment.getInvoiceIds().length == 0 );

        //create refund for above payment, refund amount = payment amount
        Integer refundId= createPayment(api, "100.00", true, user.getUserId(), paymentId);
        System.out.println("Created refund " + refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

	    //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
	    api.deleteUser(user.getId());
    }

    /**
     * 2. A refund should bring the User's balance to its Original value before payment
     */
    public void testRefundUserBalanceUnchanged() {
	    System.out.println("testRefundUserBalanceUnchanged()");

		//create user
	    UserWS user = createUser();
	    assertTrue(user.getUserId() > 0);
	    System.out.println("User created successfully " + user.getUserId());

	    user = api.getUserWS(user.getUserId());
	    assertEquals(user.getOwingBalanceAsDecimal(), BigDecimal.ZERO);

	    //make payment
	    Integer paymentId = createPayment(api, "100.00", false, user.getUserId(), null);
	    System.out.println("Created payment " + paymentId);
	    assertNotNull("Didn't get the payment id", paymentId);

	    //check payment balance = payment amount
	    PaymentWS payment = api.getPayment(paymentId);
	    assertNotNull("Payment should be created", payment);
	    assertEquals(payment.getAmountAsDecimal(), payment.getBalanceAsDecimal());

	    //check user's balance
	    user = api.getUserWS(user.getUserId());
	    BigDecimal userBalance = user.getOwingBalanceAsDecimal();
	    assertNotNull(userBalance);
	    assertTrue("User Balance should have been negetive", BigDecimal.ZERO.compareTo(userBalance) > 0);

	    assertTrue(payment.getInvoiceIds().length == 0);

	    //create refund for above payment, refund amount = payment amount
	    Integer refundId = createPayment(api, "100.00", true, user.getUserId(), paymentId);
	    System.out.println("Created refund " + refundId);
	    assertNotNull("Didn't get the payment id", refundId);

	    //check user's balance = 0
	    user = api.getUserWS(user.getUserId());
	    assertNotNull(user);
	    assertEquals(BigDecimal.ZERO, user.getOwingBalanceAsDecimal());

	    //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
	    api.deleteUser(user.getId());
    }

    /**
     * 3. A refund must link to a Payment ID (negetive)
     * because a refund is only issued against a surplus
     */
    @Test
    public void testRefundFailWhenNoPaymentLinked() {
        System.out.println("testRefundFailWhenNoPaymentLinked()");

        //create user
        UserWS user= createUser();
        assertTrue(user.getUserId() > 0);
        System.out.println("User created successfully " + user.getUserId());

        //create refund with no payment set
        try {
            createPayment(api, "100.00", true, user.getUserId(), null);
			fail("Refund can not be created without link to Payment ID");
        } catch (SessionInternalError e) {}

	    //cleanup
	    api.deleteUser(user.getId());
    }

    /**
     * 4. Test payment balance unchanged when linked payment has zero balance and linked invoices,
     * but invoice balance increased from previous balance
     */
    @Test
    public void testRefundPaymentWithInvoiceLinked() {
       System.out.println("testRefundPaymentWithInvoiceLinked()");

        //CREATE USER
        UserWS user= createUser();
        assertTrue(user.getUserId() > 0);
        System.out.println("User created successfully " + user.getUserId());

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId());
	    item.setId(api.createItem(item));

        //CREATE ORDER & INVOICE
        Integer invoiceId= createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull(invoiceId);

        //check invoice balance greater then zero
        InvoiceWS invoice= api.getLatestInvoice(user.getUserId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0 );

	    //check if an order was created
	    Integer orderId = invoice.getOrders()[0];
		assertNotNull("Order should've been create", orderId);

        //MAKE PAYMENT
        Integer paymentId= createPayment(api, "100.00", false, user.getUserId(), null);
        System.out.println("Created payment " + paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check invoice balance is zero
        invoice= api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertEquals(invoice.getBalanceAsDecimal(), BigDecimal.ZERO );

        //check payment balance = zero since invoice paid
        PaymentWS payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //payment has linked invoices
        assertTrue(payment.getInvoiceIds().length > 0 );

        //CREATE REFUND for above payment, refund amount = payment amount
        Integer refundId= null;
        try {
            createPayment(api, "100.00", true, user.getUserId(), paymentId);
            fail("Cannot refund a linked payment.");
        } catch (Exception e) {
            System.out.println("Is SessionInternalError: " + (e instanceof SessionInternalError));
        }

        for(Integer invIds : payment.getInvoiceIds()){
          api.removePaymentLink(invIds,paymentId);
        }

        System.out.println("Succesfully unlnked payment from Invoice");
        refundId= createPayment(api, "100.00", true, user.getUserId(), paymentId);
        System.out.println("Created refund " + refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //check invoice balance is greater than zero
        invoice= api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0);

        //invoice balance is equal to its total
        assertEquals(invoice.getBalanceAsDecimal(), invoice.getTotalAsDecimal());

        System.out.println("Invoice balance is " + invoice.getBalance());

	    //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
	    api.deleteInvoice(invoiceId);
	    api.deleteOrder(orderId);
		api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }

    /**
     * Refund a payment that is linked to one invoice, paying it in full, but
     * having some balance left. Result: payment balance is Refund amount less amount used to pay invoice originally.
     * Invoice balance is equal to its total (used to be zero).
     */
    @Test
    public void testRefundWithPaymentBalance() {
        System.out.println("testRefundWithPaymentBalance()");

        //CREATE USER
        UserWS user= createUser();
        assertTrue(user.getUserId() > 0);
        System.out.println("User created successfully " + user.getUserId());

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId());
	    item.setId(api.createItem(item));

        //CREATE ORDER & INVOICE
        Integer invoiceId= createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull(invoiceId);

        //check invoice balance greater then zero
        InvoiceWS invoice= api.getLatestInvoice(user.getUserId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0 );

        //MAKE PAYMENT
        Integer paymentId= createPayment(api, "200.00", false, user.getUserId(), null);
        System.out.println("Created payment " + paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check invoice balance is zero
        invoice= api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertEquals(invoice.getBalanceAsDecimal(), BigDecimal.ZERO );

        //check payment balance > zero since balance left after invoice paid
        PaymentWS payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertTrue(payment.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0 );
        assertEquals(new BigDecimal("100.00"), payment.getBalanceAsDecimal());

        //payment has linked invoices
        assertTrue(payment.getInvoiceIds().length > 0 );

        //CREATE REFUND for above payment, refund amount = payment amount
        Integer refundId= createPayment(api, "100.00", true, user.getUserId(), paymentId);
        System.out.println("Created refund " + refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //check invoice balance is greater than zero
        invoice= api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertEquals(invoice.getBalanceAsDecimal(), BigDecimal.ZERO );

        System.out.println("Invoice balance is " + invoice.getBalance());

	    //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
	    api.deleteInvoice(invoiceId);
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }

    /**
     * Refund a payment that is linked to many invoices, paying some partially,
     * some in full (uses the whole balance of the payment). Result: payment
     * balance remains zero. Invoice balance for each invoice = balance + amount
     * paid by the payment.
     */
    @Test
    public void testFailedRefundPaymentLinkedManyInvoices() {
        System.out.println("testFailedRefundPaymentLinkedManyInvoices()");

        //CREATE USER
        UserWS user= createUser();
        assertTrue(user.getUserId() > 0);
        System.out.println("User created successfully " + user.getUserId());

        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        ItemDTOEx item = buildItem(itemType.getId());
        item.setId(api.createItem(item));

        //CREATE ORDER & INVOICE 1
        Integer invoiceId1= createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull("Invoice1 should be created", invoiceId1);

	    Integer orderId1 = api.getInvoiceWS(invoiceId1).getOrders()[0];
	    assertNotNull("Order1 should be created", orderId1);

        //2
        Integer invoiceId2= createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull("Invoice2 should be created", invoiceId2);

	    Integer orderId2 = api.getInvoiceWS(invoiceId2).getOrders()[0];
	    assertNotNull("Order2 should be created", orderId2);

        //3
        Integer invoiceId3= createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull("Invoice2 should be created", invoiceId3);

	    Integer orderId3 = api.getInvoiceWS(invoiceId3).getOrders()[0];
	    assertNotNull("Order3 should be created", orderId3);

        //check invoice balance greater then zero
        InvoiceWS invoice= api.getLatestInvoice(user.getUserId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0 );

        //MAKE PAYMENT
        Integer paymentId= createPayment(api, "300.00", false, user.getUserId(), null);
        System.out.println("Created payment " + paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check invoice balance is zero
        invoice= api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertEquals(invoice.getBalanceAsDecimal(), BigDecimal.ZERO);

        //check payment balance = zero since invoice paid
        PaymentWS payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //payment has linked invoices
        assertTrue(payment.getInvoiceIds().length == 3 );

        //CREATE REFUND for above payment, refund amount = payment amount
        try{
            createPayment(api, "300.00", true, user.getUserId(), paymentId);
            fail("Refund should not have succeeded");
        } catch(Exception e) {
            System.out.println("Exception thrown: " + e.getClass().getSimpleName());
        }

        System.out.println("Invoice balance is.. " + invoice.getBalance());

	    //cleanup
	    api.deletePayment(paymentId);
	    api.deleteInvoice(invoiceId1);
	    api.deleteOrder(orderId1);
	    api.deleteInvoice(invoiceId2);
	    api.deleteOrder(orderId2);
	    api.deleteInvoice(invoiceId3);
	    api.deleteOrder(orderId3);
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }

    /**
     * Refund a payment that is linked to many invoices, paying some partially,
     * some in full (uses the whole balance of the payment). Unlinking of the payments is done
     * before they are refunded
     */
    @Test
    public void testSuccessRefundPaymentLinkedManyInvoices() {
        System.out.println("testSuccessRefundPaymentLinkedManyInvoices()");

        //CREATE USER
        UserWS user= createUser();
        assertTrue(user.getUserId() > 0);
        System.out.println("User created successfully " + user.getUserId());

        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        ItemDTOEx item = buildItem(itemType.getId());
        item.setId(api.createItem(item));

        //CREATE ORDER & INVOICE 1
        Integer invoiceId1= createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull(invoiceId1);

        //2
        Integer invoiceId2= createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull(invoiceId2);

        //3
        Integer invoiceId3= createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull(invoiceId3);

        //check invoice balance greater then zero
        InvoiceWS invoice= api.getLatestInvoice(user.getUserId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0 );

        //MAKE PAYMENT
        Integer paymentId= createPayment(api, "300.00", false, user.getUserId(), null);
        System.out.println("Created payment " + paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check invoice balance is zero
        invoice= api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertEquals(invoice.getBalanceAsDecimal(), BigDecimal.ZERO );

        //check payment balance = zero since invoice paid
        PaymentWS payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //payment has linked invoices
        assertTrue(payment.getInvoiceIds().length == 3 );

        api.removeAllPaymentLinks(paymentId);

        Integer refundId = createPayment(api, "300.00", true, user.getUserId(), paymentId);

        System.out.println("Created refund " + refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //check invoice balance is greater than zero
        invoice= api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0);

        System.out.println("Invoice balance is " + invoice.getBalance());

	    //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
	    api.deleteInvoice(invoiceId1);
	    api.deleteInvoice(invoiceId2);
	    api.deleteInvoice(invoiceId3);
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());

    }

    /*
     * Deleting a Payment that has been refunded must fail.
     */
    @Test
    public void testDeletePaymentThatHasRefund() {
        System.out.println("testDeletePaymentThatHasRefund()");

        //create user
        UserWS user= createUser();
        assertTrue(user.getUserId() > 0);
        System.out.println("User created successfully " + user.getUserId());

        //make payment
        Integer paymentId= createPayment(api, "100.00", false, user.getUserId(), null);
        System.out.println("Created payment " + paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check payment balance = payment amount
        PaymentWS payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(payment.getAmountAsDecimal(), payment.getBalanceAsDecimal());

        assertTrue(payment.getInvoiceIds().length == 0 );

        //create refund for above payment, refund amount = payment amount
        Integer refundId= createPayment(api, "100.00", true, user.getUserId(), paymentId);
        System.out.println("Created refund " + refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        try {
            api.deletePayment(paymentId);
            fail("A refund can not be deleted");
        } catch (Exception e) {
            //expected
        }

	    //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
	    api.deleteUser(user.getId());
    }

    /**
     * A payment that has been refunded can not be updated.
     */
    @Test
    public void testUpdatePaymentThatHasRefund() {
        System.out.println("testUpdatePaymentThatHasRefund()");

        //create user
        UserWS user= createUser();
        assertTrue(user.getUserId() > 0);
        System.out.println("User created successfully " + user.getUserId());

        //make payment
        Integer paymentId= createPayment(api, "100.00", false, user.getUserId(), null);
        System.out.println("Created payment " + paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check payment balance = payment amount
        PaymentWS payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(payment.getAmountAsDecimal(), payment.getBalanceAsDecimal());

        assertTrue(payment.getInvoiceIds().length == 0 );

        //create refund for above payment, refund amount = payment amount
        Integer refundId= createPayment(api, "100.00", true, user.getUserId(), paymentId);
        System.out.println("Created refund " + refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment= api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        try {
            payment.setAmount("150.00");
            api.updatePayment(payment);
            fail("A refunded payment can not be updated");
        } catch (Exception e) {
            //expected
        }

	    //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
	    api.deleteUser(user.getId());
    }

	/**
	 * Cannot delete payment that has been refunded (negetive)
	 */
	@Test
	public void testNegativeAmountRefundPayment() {
		System.out.println("testNegativeAmountRefundPayment().");

		//create user
		UserWS user = createUser();
		assertTrue(user.getUserId() > 0);
		System.out.println("User created successfully " + user.getUserId());

		//make payment
		Integer paymentId = createPayment(api, "100.00", false, user.getUserId(), null);
		System.out.println("Created payment " + paymentId);
		assertNotNull("Didn't get the payment id", paymentId);

		//make refund payment with negative amount
		try {
			createPayment(api, "-100.00", true, user.getUserId(), paymentId);
			fail("Should not be able to create a refund with negative amount");
		} catch (SessionInternalError e) {
		}

		//clean up
		api.deletePayment(paymentId);
		api.deleteUser(user.getUserId());
	}

	@Test
    public void testPartialRefund() {
        System.out.println("*** testPartialRefund ***");

        //create user
        UserWS user= createUser();
        assertTrue(user.getUserId() > 0);
        System.out.println("User created successfully " + user.getUserId());

        user= api.getUserWS(user.getUserId());

        // Create a payment
        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal("30.00"));
        payment.setIsRefund(new Integer(0));
        payment.setMethodId(ServerConstants.PAYMENT_METHOD_VISA);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setCurrencyId(CURRENCY_GBP_ID);
        payment.setUserId(user.getUserId());

        // Add the token for this payment
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 5); // dummy data, to pass validation
        PaymentInformationWS cc = com.sapienter.jbilling.server.user.WSTest.createCreditCard("Joe Bloggs",
                "4111111111111152", cal.getTime());
        payment.getPaymentInstruments().add(cc);

        // Process
        System.out.println("Processing token payment...");
        PaymentAuthorizationDTOEx authInfo = api.processPayment(payment, null);
        assertNotNull("Payment result not null", authInfo);

        Integer paymentId = authInfo.getPaymentId();

        assertTrue("Payment Authorization result should be successful",
                authInfo.getResult().booleanValue());

        //remove paymentInvoices links.
        payment = api.getPayment(authInfo.getPaymentId());
        System.out.println("Balance after payment " + payment.getBalanceAsDecimal());

        // Create a refund
        PaymentWS payment2 = new PaymentWS();
        payment2.setAmount(new BigDecimal("200.00")); //Invalid amount
        payment2.setIsRefund(new Integer(1));
        payment2.setMethodId(ServerConstants.PAYMENT_METHOD_VISA);
        payment2.setPaymentDate(Calendar.getInstance().getTime());
        payment2.setCurrencyId(CURRENCY_GBP_ID);
        payment2.setUserId(user.getUserId()); // Existing user Frank Thompson
        payment2.setPaymentId(paymentId);

        cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 5); // dummy data, to pass validation
        // Add the token for this payment
        PaymentInformationWS cc2 = com.sapienter.jbilling.server.user.WSTest.createCreditCard("Joe Bloggs",
                "4111111111111152", cal.getTime());

        payment2.getPaymentInstruments().add(cc2);

        // Process invalid refund
        System.out.println("Processing token payment...");
        PaymentAuthorizationDTOEx authInfo2;
        try {
            authInfo2 = api.processPayment(payment2, null);
            fail("An exception should be thrown");
        } catch (SessionInternalError e) {
            assertEquals("There should be only one exception", 1, e.getErrorMessages().length);
            assertEquals("An exception should be thrown, the amount of the refund is greater thant the payment",
                    "PaymentWS,paymentId,validation.error.apply.without.payment.or.different.linked.payment.amount", e.getErrorMessages()[0]);
        }
        //now set a valid amount.
        payment2.setAmount(new BigDecimal("20.00"));
        authInfo2 = api.processPayment(payment2, null);
        assertNotNull("Payment result not null", authInfo2);

        assertTrue("Payment Authorization result should be successful",
                authInfo2.getResult().booleanValue());

        PaymentWS originalPayment= api.getPayment(paymentId);

        assertEquals("The original payments balance should have reduced to 10.00",
                BigDecimal.TEN, originalPayment.getBalanceAsDecimal());

	    //cleanup
//		api.deletePayment(authInfo2.getPaymentId());//refund
//	    api.deletePayment(authInfo.getPaymentId());//original payment
	    api.deleteUser(user.getId());
    }

	private ItemTypeWS buildItemType() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("Refund, Item Type:" + System.currentTimeMillis());
		type.setOrderLineTypeId(1);//items
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}

	private ItemDTOEx buildItem(Integer itemTypeId){
		ItemDTOEx item = new ItemDTOEx();
		long millis = System.currentTimeMillis();
		String name = String.valueOf(millis) + new Random().nextInt(10000);
		item.setDescription("Invoice, Product:" + name);
		item.setPriceModelCompanyId(api.getCallerCompanyId());
		item.setPriceManual(0);
		item.setPrices(CreateObjectUtil.setItemPrice(new BigDecimal("10"), new DateMidnight(1970, 1, 1).toDate(), Integer.valueOf(1), CURRENCY_ID));
		item.setNumber("RFN-PRD-"+name);
		item.setAssetManagementEnabled(0);
		Integer typeIds[] = new Integer[] {itemTypeId};
		item.setTypes(typeIds);
		return item;
	}

	//Helper method to create user
    private static UserWS createUser() {
        System.out.println("createUser called");
        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName("refund-test-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("Admin123@");
        newUser.setLanguageId(LANGUAGE_ID);
        newUser.setMainRoleId(new Integer(5));
        newUser.setAccountTypeId(ACCOUNT_TYPE);
        newUser.setParentId(null); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(CURRENCY_ID);
        newUser.setInvoiceChild(new Boolean(false));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor


        //contact info
        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(ACCOUNT_TYPE);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("Frodo");
        metaField4.setGroupId(ACCOUNT_TYPE);

        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("Baggins");
        metaField5.setGroupId(ACCOUNT_TYPE);

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

        newUser.getPaymentInstruments().add(
		        com.sapienter.jbilling.server.user.WSTest.createCreditCard(
				        "Frodo Baggins",
        		        "4111111111111111",
				        expiry.getTime()));

        System.out.println("Creating user ...");
        newUser.setUserId(api.createUser(newUser));

        return newUser;
    }

    //Helper method to create payment
    private static Integer createPayment(JbillingAPI api, String amount, boolean isRefund,
                                         Integer userId, Integer linkedPaymentId) {
	    PaymentWS payment = new PaymentWS();
	    payment.setAmount(new BigDecimal(amount));
	    payment.setIsRefund(isRefund ? new Integer(1) : new Integer(0));
	    payment.setMethodId(ServerConstants.PAYMENT_METHOD_CHEQUE);
	    payment.setPaymentDate(Calendar.getInstance().getTime());
	    payment.setResultId(ServerConstants.RESULT_ENTERED);
	    payment.setCurrencyId(CURRENCY_ID);
	    payment.setUserId(userId);
	    payment.setPaymentNotes("Notes");
	    payment.setPaymentPeriod(new Integer(1));
	    payment.setPaymentId(linkedPaymentId);

	    PaymentInformationWS cheque = com.sapienter.jbilling.server.user.WSTest
			    .createCheque("ws bank", "2232-2323-2323", Calendar.getInstance().getTime());
	    payment.getPaymentInstruments().add(cheque);

	    System.out.println("Creating " + (isRefund ? " refund." : " payment."));
	    Integer ret = api.createPayment(payment);
	    return ret;
    }

    //Helper method to create order and invoice
    private static Integer createOrderAndInvoice(JbillingAPI api, Integer userId, Integer itemId) {
        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(userId);
        newOrder.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        newOrder.setPeriod(ServerConstants.ORDER_PERIOD_ONCE);
        newOrder.setCurrencyId(CURRENCY_ID);
        newOrder.setNotes("Lorem ipsum text.");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2008, 1, 1);
        newOrder.setActiveSince(cal.getTime());

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line;

        line = new OrderLineWS();
        line.setPrice(new BigDecimal("100.00"));
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setAmount(new BigDecimal("100.00"));
        line.setDescription("Fist line");
        line.setItemId(itemId);
        lines[0] = line;
        newOrder.setOrderLines(lines);
        System.out.println("Creating order ... ");
        return api.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, ORDER_CHANGE_STATUS_APPLY));
    }

	private static Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
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
		status1.addDescription(new InternationalDescriptionWS(LANGUAGE_ID, status1Name));
		return api.createOrderChangeStatus(apply);
	}
}
