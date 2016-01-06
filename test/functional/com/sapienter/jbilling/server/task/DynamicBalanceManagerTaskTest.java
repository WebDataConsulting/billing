package com.sapienter.jbilling.server.task;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.payment.PaymentMethodHelper;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.ApiTestCase;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.Constants;

import org.testng.annotations.Test;

import java.lang.Exception;
import java.lang.Integer;
import java.lang.String;
import java.lang.System;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;


@Test(groups = { "integration", "task", "dynamic-balance" })
public class DynamicBalanceManagerTaskTest extends ApiTestCase {

    private static Integer US_DOLLAR_ID;
    private static Integer AUS_DOLLAR_ID;
	private static Integer LANGUAGE_ID;
    private static Integer ORDER_CHANGE_STATUS_APPLY_ID;
	private static Integer PRANCING_PONY_ACCOUNT_TYPE;
	private static Integer CUSTOMER_MAIN_ROLE;

	private static Integer CHEQUE_PAYMENT_TYPE;

	private static int USER_ID;
	private static final int PAYMENT_METHOD_ID = 1;
	
	protected void prepareTestInstance() throws Exception {
		super.prepareTestInstance();
		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
		US_DOLLAR_ID = Integer.valueOf(1);
		AUS_DOLLAR_ID = Integer.valueOf(11);
		LANGUAGE_ID = Integer.valueOf(1);
		PRANCING_PONY_ACCOUNT_TYPE = Integer.valueOf(1);
		CUSTOMER_MAIN_ROLE = Integer.valueOf(5);

		CHEQUE_PAYMENT_TYPE = api.createPaymentMethodType(PaymentMethodHelper.buildChequeTemplateMethod(api));
	}

	protected void afterTestClass() throws Exception {
		api.deletePaymentMethodType(CHEQUE_PAYMENT_TYPE);
	}

	/**
     * Test Scenario: Make a user with currency AUS $ , then make a payment of US$ 100, the dynamic balance of the user should be greater than AUS $ 101 , assuming that US $ is STRONGER than AUS $
     */
    @Test
    public void testDetermineAmountWithPaymentSuccessfulEvent() {

	    System.out.println("Testing determine amount with payment successful event");
	    String username = "user-payment-succ-15-" + System.currentTimeMillis();
	    String balance = "1";
	    // make a new user with ZERO balance and currency as AUS DOLLAR
	    Integer userId = makeUser(AUS_DOLLAR_ID, username, balance);
	    PaymentWS payment = new PaymentWS();
	    payment.setUserId(userId);
	    payment.setMethodId(Constants.PAYMENT_METHOD_CHEQUE);
	    payment.setResultId(Constants.RESULT_ENTERED);
	    payment.setPaymentDate(Calendar.getInstance().getTime());
	    payment.setCurrencyId(US_DOLLAR_ID);
	    payment.setAmount("100");
	    payment.setIsRefund(0);

	    PaymentInformationWS cheque = PaymentMethodHelper.createCheque(
			    CHEQUE_PAYMENT_TYPE, "ws bank", "2232-2323-2323", Calendar.getInstance().getTime());
	    payment.getPaymentInstruments().add(cheque);

	    // check that user's dynamic balance is 100
	    UserWS user = api.getUserWS(userId);
	    user.setPassword(null);
	    System.out.println("User's dynamic balance earlier was " + user.getDynamicBalanceAsDecimal());
	    assertTrue("User's Balance would be ONE", (BigDecimal.ONE.compareTo(user.getDynamicBalanceAsDecimal()) == 0));
	    Integer paymentId = api.createPayment(payment);
	    // update the user
	    api.updateUser(user);
	    user = api.getUserWS(userId);
	    System.out.println("User's dynamic balance now is " + user.getDynamicBalanceAsDecimal());
	    System.out.println("Payment amount " + payment.getAmountAsDecimal());
	    System.out.println("balance " + new BigDecimal(balance));

	    assertTrue("User's balance must be greater than the topped up value, assuming US $ is stronger than AUS $", (user.getDynamicBalanceAsDecimal().compareTo(payment.getAmountAsDecimal().add(new BigDecimal(balance))) > 0));

	    //cleanup
	    api.deletePayment(paymentId);
	    api.deleteUser(userId);
    }


    /**
     * Test Scenario: Make a payment in different currency than user's default currency , then delete the payment, the result should be that user's dynamic balance should be updated according to the user's currency
     *  and not on the payment's currency value.
     */
    @Test
    public void testDetermineAmountWithPaymentDeletedEvent() {
	    System.out.println("Testing determine amount with payment successful event");
	    String username = "user-payment-del-2-" + System.currentTimeMillis();
	    String balance = "1";
	    // make a new user with ZERO balance and currency as AUS DOLLAR
	    Integer userId = makeUser(AUS_DOLLAR_ID, username, balance);
	    PaymentWS payment = new PaymentWS();
	    payment.setUserId(userId);
	    payment.setMethodId(Constants.PAYMENT_METHOD_CHEQUE);
	    payment.setResultId(Constants.RESULT_ENTERED);
	    payment.setPaymentDate(Calendar.getInstance().getTime());
	    payment.setCurrencyId(US_DOLLAR_ID);
	    payment.setAmount("100");
	    payment.setIsRefund(0);

	    PaymentInformationWS cheque = PaymentMethodHelper.createCheque(
			    CHEQUE_PAYMENT_TYPE, "ws bank", "2232-2323-2323", Calendar.getInstance().getTime());
	    payment.getPaymentInstruments().add(cheque);

	    // check that user's dynamic balance is 100
	    UserWS user = api.getUserWS(userId);
	    user.setPassword(null);
	    System.out.println("User's dynamic balance earlier was " + user.getDynamicBalanceAsDecimal());
	    assertTrue("User's Balance would be ONE", (BigDecimal.ONE.compareTo(user.getDynamicBalanceAsDecimal()) == 0));
	    Integer paymentId = api.createPayment(payment);
	    // update the user
	    api.updateUser(user);
	    user = api.getUserWS(userId);
	    user.setPassword(null);
	    System.out.println("User's dynamic balance now is " + user.getDynamicBalanceAsDecimal());
	    System.out.println("Payment amount " + payment.getAmountAsDecimal());
	    System.out.println("balance " + new BigDecimal(balance));
	    assertTrue("User's balance must be greater than the topped up value, assuming US $ is stronger than AUS $", (user.getDynamicBalanceAsDecimal().compareTo(payment.getAmountAsDecimal().add(new BigDecimal(balance))) > 0));

	    // now delete the payment , user's dynamic balance should become equal to initial value
	    api.deletePayment(paymentId);
	    // update the user
	    api.updateUser(user);
	    user = api.getUserWS(userId);

	    System.out.println("User's dynamic balance is now " + user.getDynamicBalanceAsDecimal());
	    assertTrue("User's Dynamic Balance Must Be Back to initial balance ", (user.getDynamicBalanceAsDecimal().compareTo(new BigDecimal(balance)) == 0));

	    //cleanup
	    api.deleteUser(userId);
    }

    @Test
    public void testDynamicBalanceAfterQuantityChange() {
	    String username = "user-order-quant-change-1-" + System.currentTimeMillis();
	    String balance = "0";

	    Integer userId = makeUser(US_DOLLAR_ID, username, balance);
	    OrderWS order = buildOrder(userId);
	    BigDecimal secondProductPrice = api.getItem(2, userId, null).getPriceAsDecimal();

	    Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

	    UserWS user = api.getUserWS(userId);
	    order = api.getOrder(orderId);

	    System.out.println("user dynamic balance: " + user.getDynamicBalanceAsDecimal());
	    System.out.println("order total amount: " + order.getTotalAsDecimal());

	    //Calculate the total amount of the order. This makes the test resiliant to changes of the second product price
	    BigDecimal calculatedOrderTotal = BigDecimal.valueOf(10).add(secondProductPrice);
	    System.out.println("The order total is " + calculatedOrderTotal.toPlainString());
	    assertEquals("The order total", calculatedOrderTotal, order.getTotalAsDecimal());
	    assertEquals("Both should be 30", user.getDynamicBalanceAsDecimal(), order.getTotalAsDecimal().negate());

	    //change the quantity of the first product in the order
	    //and check the order total and the dynamic balance
	    OrderLineWS orderLineWS = findLine(order, 1);
	    OrderChangeWS orderChange = OrderChangeBL.buildFromLine(orderLineWS, null, ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange.setQuantity(BigDecimal.valueOf(2).subtract(orderLineWS.getQuantityAsDecimal()));
	    api.updateOrder(order, new OrderChangeWS[]{orderChange});

	    user = api.getUserWS(userId);
	    order = api.getOrder(orderId);

	    calculatedOrderTotal = BigDecimal.valueOf(2).multiply(BigDecimal.valueOf(10)).add(secondProductPrice);
	    System.out.println("The order total is " + calculatedOrderTotal.toPlainString());
	    assertEquals("The order total", calculatedOrderTotal, order.getTotalAsDecimal());
	    assertEquals("Both should be 40", user.getDynamicBalanceAsDecimal(), order.getTotalAsDecimal().negate());

	    //change the quantities of both products simultaneously
	    //and check the order total amount and the balance of the user
	    orderLineWS = findLine(order, 1);
	    OrderChangeWS change1 = OrderChangeBL.buildFromLine(orderLineWS, null, ORDER_CHANGE_STATUS_APPLY_ID);
	    change1.setQuantity(BigDecimal.valueOf(5).subtract(orderLineWS.getQuantityAsDecimal()));

	    orderLineWS = findLine(order, 2);
	    OrderChangeWS change2 = OrderChangeBL.buildFromLine(orderLineWS, null, ORDER_CHANGE_STATUS_APPLY_ID);
	    change2.setQuantity(BigDecimal.valueOf(5).subtract(orderLineWS.getQuantityAsDecimal()));

	    api.updateOrder(order, new OrderChangeWS[]{change1, change2});

	    user = api.getUserWS(userId);
	    order = api.getOrder(orderId);

	    BigDecimal firstLineTotalAmount = BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(10));
	    BigDecimal secondLineTotalAmount = BigDecimal.valueOf(5).multiply(secondProductPrice);
	    //5*10 + 5*20 = 50+100 = 150
	    calculatedOrderTotal = firstLineTotalAmount.add(secondLineTotalAmount);
	    System.out.println("The order total is " + calculatedOrderTotal.toPlainString());
	    assertEquals("The order total", calculatedOrderTotal, order.getTotalAsDecimal());
	    assertEquals("Both should be 150", user.getDynamicBalanceAsDecimal(), order.getTotalAsDecimal().negate());

	    //change both the quantity and the price of first product
	    //and quantity of the second product and check if the total order amount
	    //and user's dynamic balance will correctly follow

	    orderLineWS = findLine(order, 1);
	    change1 = OrderChangeBL.buildFromLine(orderLineWS, null, ORDER_CHANGE_STATUS_APPLY_ID);
	    change1.setQuantity(BigDecimal.valueOf(10).subtract(orderLineWS.getQuantityAsDecimal()));
	    change1.setPrice(BigDecimal.valueOf(3));

	    orderLineWS = findLine(order, 2);
	    change2 = OrderChangeBL.buildFromLine(orderLineWS, null, ORDER_CHANGE_STATUS_APPLY_ID);
	    change2.setQuantity(BigDecimal.valueOf(10).subtract(orderLineWS.getQuantityAsDecimal()));
	    orderLineWS.setQuantity(10);

	    api.updateOrder(order, new OrderChangeWS[]{change1, change2});

	    user = api.getUserWS(userId);
	    order = api.getOrder(orderId);

	    firstLineTotalAmount = BigDecimal.valueOf(10).multiply(BigDecimal.valueOf(3));
	    secondLineTotalAmount = BigDecimal.valueOf(10).multiply(secondProductPrice);
	    //10*3 + 10*20 = 20+200 = 220
	    calculatedOrderTotal = firstLineTotalAmount.add(secondLineTotalAmount);
	    System.out.println("The order total is " + calculatedOrderTotal.toPlainString());
	    assertEquals("The order total", calculatedOrderTotal, order.getTotalAsDecimal());
	    assertEquals("Both should be 220", user.getDynamicBalanceAsDecimal(), order.getTotalAsDecimal().negate());

	    api.deleteOrder(orderId);

	    user = api.getUserWS(userId);

	    System.out.println("User's dynamic balance after order delete is: " + user.getDynamicBalanceAsDecimal().toPlainString());
	    assertEquals("The Dynamic Balance should be 0", BigDecimal.ZERO, user.getDynamicBalanceAsDecimal());

	    //cleanup
	    api.deleteUser(userId);
    }

    @Test
    public void testDynamicBalanceAfterPriceChange() {

	    System.out.println("Testing dynamic balance after price change on one-time order");

	    String username = "user-order-price-change-1-" + System.currentTimeMillis();
	    String balance = "0";

	    Integer userId = makeUser(US_DOLLAR_ID, username, balance);
	    OrderWS order = buildOrder(userId);
	    BigDecimal secondProductPrice = api.getItem(2, userId, null).getPriceAsDecimal();

	    Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

	    UserWS user = api.getUserWS(userId);
	    order = api.getOrder(orderId);

	    System.out.println("user dynamic balance: " + user.getDynamicBalanceAsDecimal());
	    System.out.println("order total amount: " + order.getTotalAsDecimal());

	    //Calculate the total amount of the order. This makes the test resiliant to changes of the second product price
	    BigDecimal calculatedOrderTotal = BigDecimal.valueOf(10).add(secondProductPrice);
	    System.out.println("The order total is " + calculatedOrderTotal.toPlainString());
	    assertEquals("The order total", calculatedOrderTotal, order.getTotalAsDecimal());
	    assertEquals("Both should be 30", user.getDynamicBalanceAsDecimal(), order.getTotalAsDecimal().negate());

	    //change the quantity of the first product in the order
	    //and check the order total and the dynamic balance
	    OrderLineWS orderLineWS = findLine(order, 1);
	    OrderChangeWS change1 = OrderChangeBL.buildFromLine(orderLineWS, null, ORDER_CHANGE_STATUS_APPLY_ID);
	    change1.setQuantity(BigDecimal.valueOf(2).subtract(orderLineWS.getQuantityAsDecimal()));
	    api.updateOrder(order, new OrderChangeWS[]{change1});

	    user = api.getUserWS(userId);
	    order = api.getOrder(orderId);

	    calculatedOrderTotal = BigDecimal.valueOf(2).multiply(BigDecimal.valueOf(10)).add(secondProductPrice);
	    System.out.println("The order total is " + calculatedOrderTotal.toPlainString());
	    assertEquals("The order total", calculatedOrderTotal, order.getTotalAsDecimal());
	    assertEquals("Both should be 40", user.getDynamicBalanceAsDecimal(), order.getTotalAsDecimal().negate());

	    //change only the price of the first product. DO NOT change quantity
	    orderLineWS = findLine(order, 1);
	    change1 = OrderChangeBL.buildFromLine(orderLineWS, null, ORDER_CHANGE_STATUS_APPLY_ID);
	    change1.setPrice(new BigDecimal(7));
	    api.updateOrder(order, new OrderChangeWS[]{change1});

	    user = api.getUserWS(userId);
	    order = api.getOrder(orderId);

	    calculatedOrderTotal = BigDecimal.valueOf(2).multiply(BigDecimal.valueOf(7)).add(secondProductPrice);
	    System.out.println("The order total is " + calculatedOrderTotal.toPlainString());
	    assertEquals("The order total", calculatedOrderTotal, order.getTotalAsDecimal());
	    assertEquals("Both should be 34", user.getDynamicBalanceAsDecimal(), order.getTotalAsDecimal().negate());

	    api.deleteOrder(orderId);

	    user = api.getUserWS(userId);

	    System.out.println("User's dynamic balance after order delete is: " + user.getDynamicBalanceAsDecimal().toPlainString());
	    assertEquals("The Dynamic Balance should be 0", BigDecimal.ZERO, user.getDynamicBalanceAsDecimal());

	    //cleanup
	    api.deleteUser(userId);
    }

    /**
     * Test Scenario: Make a payment in different currency than user's default currency,then create new order on invoice and verify balance
     */
    @Test
    public void testDetermineAmountWithOrderAddedOnInvoiceEvent() {
        int i;
        System.out.println("Testing determine amount with payment successful event");
        String username = "user-order-on-invoice-15"+Math.random();
        String email = "user-payment-succ@jbilling.com";
        String balance = "1";
        // make a new user with ZERO balance and currency as AUS DOLLAR
        USER_ID = makeUser(AUS_DOLLAR_ID, username, balance);
        PaymentWS payment = new PaymentWS();
        payment.setUserId(USER_ID);
        //payment.setCheque(CHEQUE);
        payment.setMethodId(PAYMENT_METHOD_ID);
        Calendar calendar = Calendar.getInstance();
        payment.setPaymentDate(calendar.getTime());
        payment.setCurrencyId(US_DOLLAR_ID);
        payment.setAmount("100");
        payment.setIsRefund(0);
        // check that user's dynamic balance is 100
        UserWS user =  api.getUserWS(USER_ID);
        System.out.println("User's dynamic balance earlier was "+user.getDynamicBalanceAsDecimal());
        assertTrue("User's Balance would be ONE", (BigDecimal.ONE.compareTo(user.getDynamicBalanceAsDecimal())==0));
        PaymentInformationWS cc = com.sapienter.jbilling.server.user.WSTest.createCreditCard("Frodo Baggins",
                "4111111111111111", new Date());
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
        payment.getPaymentInstruments().add(cc);
        api.createPayment(payment);
        // update the user
        user.setPassword(null);
        api.updateUser(user);
        user = api.getUserWS(USER_ID);
        System.out.println("User's dynamic balance now is "+user.getDynamicBalanceAsDecimal());
        System.out.println("Payment amount "+payment.getAmountAsDecimal());
        System.out.println("balance "+new BigDecimal(balance));
        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(USER_ID);
        newOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        newOrder.setPeriod(Constants.ORDER_PERIOD_ONCE);
        newOrder.setCurrencyId(new Integer(1));
        // notes can only be 200 long... but longer should not fail
        newOrder.setNotes("At the same time the British Crown began bestowing land grants in Nova Scotia on favored subjects to encourage settlement and trade with the mother country. In June 1764, for instance, the Boards of Trade requested the King make massive land grants to such Royal favorites as Thomas Pownall, Richard Oswald, Humphry Bradstreet, John Wentworth, Thomas Thoroton[10] and Lincoln's Inn barrister Levett Blackborne.[11] Two years later, in 1766, at a gathering at the home of Levett Blackborne, an adviser to the Duke of Rutland, Oswald and his friend James Grant were released from their Nova Scotia properties so they could concentrate on their grants in British East Florida.");
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2008, 9, 3);
        newOrder.setActiveSince(cal.getTime());

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[3];
        OrderLineWS line;

        line = new OrderLineWS();
        line.setPrice(new BigDecimal("10.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setAmount(new BigDecimal("10.00"));
        line.setDescription("Fist line");
        line.setItemId(new Integer(1));
        lines[0] = line;

        // this is an item line
        line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setItemId(new Integer(2));
        // take the description from the item
        line.setUseItem(new Boolean(true));
        lines[1] = line;

        // this is an item line
        line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setItemId(new Integer(3));
        line.setUseItem(new Boolean(true));
        lines[2] = line;

        newOrder.setOrderLines(lines);

        BigDecimal oldBalance = user.getDynamicBalanceAsDecimal();
        System.out.println("User's dynamic balance before placing order is "+oldBalance);
        System.out.println("Creating order ... " + newOrder);
        // create another one so we can test get by period.
        Integer invoiceId = api.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        System.out.println("Created invoice " + invoiceId);
        InvoiceWS invoice=api.getInvoiceWS(invoiceId);
        BigDecimal invoiceTotal=new BigDecimal(invoice.getTotal());
        System.out.println("Invoice Total"+invoice.getTotal());
        user = api.getUserWS(USER_ID);
        System.out.println("User's dynamic balance after placing Order Added On Invoice is "+user.getDynamicBalanceAsDecimal());
        assertEquals("User's dynamic balance after Order Added On Invoice",user.getDynamicBalanceAsDecimal(),oldBalance.subtract(invoiceTotal));
        //clean up
        api.deleteUser(USER_ID);
    }
    @Test
    public void testDetermineAmountWithNewPriceEvent() {
        int i;
        System.out.println("Testing Determine Amount With New Price Event");
        String username = "user-new-price-15"+Math.random();
        String email = "user-payment-succ@jbilling.com";
        String balance = "1";
        // make a new user with ZERO balance and currency as AUS DOLLAR
        USER_ID = makeUser(AUS_DOLLAR_ID, username, balance);
        PaymentWS payment = new PaymentWS();
        payment.setUserId(USER_ID);
        //payment.setCheque(CHEQUE);
        payment.setMethodId(PAYMENT_METHOD_ID);
        Calendar calendar = Calendar.getInstance();
        payment.setPaymentDate(calendar.getTime());
        payment.setCurrencyId(US_DOLLAR_ID);
        payment.setAmount("100");
        payment.setIsRefund(0);
        // check that user's dynamic balance is 100
        UserWS user =  api.getUserWS(USER_ID);
	    user.setPassword(null);
        System.out.println("User's dynamic balance earlier was "+user.getDynamicBalanceAsDecimal());
        assertTrue("User's Balance would be ONE", (BigDecimal.ONE.compareTo(user.getDynamicBalanceAsDecimal())==0));
        PaymentInformationWS cc = com.sapienter.jbilling.server.user.WSTest.createCreditCard("Frodo Baggins",
                "4111111111111111", new Date());
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
        payment.getPaymentInstruments().add(cc);
        api.createPayment(payment);
        // update the user
        api.updateUser(user);
        user = api.getUserWS(USER_ID);
        System.out.println("User's dynamic balance now is "+user.getDynamicBalanceAsDecimal());
        System.out.println("Payment amount "+payment.getAmountAsDecimal());
        System.out.println("balance "+new BigDecimal(balance));
        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(USER_ID);
        newOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        newOrder.setPeriod(Constants.ORDER_PERIOD_ONCE);
        newOrder.setCurrencyId(new Integer(1));
        // notes can only be 200 long... but longer should not fail
        newOrder.setNotes("At the same time the British Crown began bestowing land grants in Nova Scotia on favored subjects to encourage settlement and trade with the mother country. In June 1764, for instance, the Boards of Trade requested the King make massive land grants to such Royal favorites as Thomas Pownall, Richard Oswald, Humphry Bradstreet, John Wentworth, Thomas Thoroton[10] and Lincoln's Inn barrister Levett Blackborne.[11] Two years later, in 1766, at a gathering at the home of Levett Blackborne, an adviser to the Duke of Rutland, Oswald and his friend James Grant were released from their Nova Scotia properties so they could concentrate on their grants in British East Florida.");
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2008, 9, 3);
        newOrder.setActiveSince(cal.getTime());

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[3];
        OrderLineWS line;

        line = new OrderLineWS();
        line.setPrice(new BigDecimal("10.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setAmount(new BigDecimal("10.00"));
        line.setDescription("Fist line");
        line.setItemId(new Integer(1));
        lines[0] = line;

        // this is an item line
        line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setItemId(new Integer(2));
        // take the description from the item
        line.setUseItem(new Boolean(true));
        lines[1] = line;

        // this is an item line
        line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setItemId(new Integer(3));
        line.setUseItem(new Boolean(true));
        lines[2] = line;

        newOrder.setOrderLines(lines);

        BigDecimal oldBalance = user.getDynamicBalanceAsDecimal();
        System.out.println("User's dynamic balance before placing order is "+oldBalance);
        System.out.println("Creating order ... " + newOrder);
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        newOrder=api.getOrder(orderId);
        BigDecimal orderTotal = new BigDecimal(newOrder.getTotal());
        System.out.println("Total Order Amount"+orderTotal);
        user = api.getUserWS(USER_ID);
        System.out.println("User's dynamic balance after placing order is "+user.getDynamicBalanceAsDecimal());
        assertEquals("User's dynamic balance after placing order",user.getDynamicBalanceAsDecimal(),oldBalance.subtract(orderTotal));
        newOrder=api.getOrder(orderId);
        lines=newOrder.getOrderLines();
        lines[0].setPrice("20.0");
        newOrder.setOrderLines(lines);

        api.updateOrder(newOrder, new OrderChangeWS[] {OrderChangeBL.buildFromLine(lines[0], null, ORDER_CHANGE_STATUS_APPLY_ID)});
        newOrder=api.getOrder(orderId);
        orderTotal = new BigDecimal(newOrder.getTotal());
        newOrder=api.getOrder(orderId);
        System.out.println("orderTotal="+orderTotal);
        user = api.getUserWS(USER_ID);
        System.out.println("User's dynamic balance New Price Event "+user.getDynamicBalanceAsDecimal());
        assertEquals("User's dynamic balance after New Price Event",user.getDynamicBalanceAsDecimal(),oldBalance.subtract(orderTotal));
        //clean up
        api.deleteOrder(newOrder.getId());
        api.deleteUser(USER_ID);

    }
    /**
     * Test Scenario: Make a payment in different currency than user's default currency , then create new order and then delete and then verify
     */

    @Test
    public void testDetermineAmountWithOrderDeletedEvent() {
        int i;
        System.out.println("Testing determine amount with payment successful event");
        String username = "user-order-deleted-15"+Math.random();
        String email = "user-payment-succ@jbilling.com";
        String balance = "1";
        // make a new user with ZERO balance and currency as AUS DOLLAR
        USER_ID = makeUser(AUS_DOLLAR_ID, username, balance);
        PaymentWS payment = new PaymentWS();
        payment.setUserId(USER_ID);
        //payment.setCheque(CHEQUE);
        payment.setMethodId(PAYMENT_METHOD_ID);
        Calendar calendar = Calendar.getInstance();
        payment.setPaymentDate(calendar.getTime());
        payment.setCurrencyId(US_DOLLAR_ID);
        payment.setAmount("100");
        payment.setIsRefund(0);
        // check that user's dynamic balance is 100
        UserWS user =  api.getUserWS(USER_ID);
        System.out.println("User's dynamic balance earlier was "+user.getDynamicBalanceAsDecimal());
        assertTrue("User's Balance would be ONE", (BigDecimal.ONE.compareTo(user.getDynamicBalanceAsDecimal())==0));
        PaymentInformationWS cc = com.sapienter.jbilling.server.user.WSTest.createCreditCard("Frodo Baggins",
                "4111111111111111", new Date());
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
        payment.getPaymentInstruments().add(cc);
        api.createPayment(payment);
        // update the user
        user.setPassword(null);
        api.updateUser(user);
        user = api.getUserWS(USER_ID);
        System.out.println("User's dynamic balance now is "+user.getDynamicBalanceAsDecimal());
        System.out.println("Payment amount "+payment.getAmountAsDecimal());
        System.out.println("balance "+new BigDecimal(balance));

        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(USER_ID);
        newOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        newOrder.setPeriod(Constants.ORDER_PERIOD_ONCE);
        newOrder.setCurrencyId(new Integer(1));
        // notes can only be 200 long... but longer should not fail
        newOrder.setNotes("At the same time the British Crown began bestowing land grants in Nova Scotia on favored subjects to encourage settlement and trade with the mother country. In June 1764, for instance, the Boards of Trade requested the King make massive land grants to such Royal favorites as Thomas Pownall, Richard Oswald, Humphry Bradstreet, John Wentworth, Thomas Thoroton[10] and Lincoln's Inn barrister Levett Blackborne.[11] Two years later, in 1766, at a gathering at the home of Levett Blackborne, an adviser to the Duke of Rutland, Oswald and his friend James Grant were released from their Nova Scotia properties so they could concentrate on their grants in British East Florida.");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2008, 9, 3);
        newOrder.setActiveSince(cal.getTime());

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[3];
        OrderLineWS line;

        line = new OrderLineWS();
        line.setPrice(new BigDecimal("10.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setAmount(new BigDecimal("10.00"));
        line.setDescription("Fist line");
        line.setItemId(new Integer(1));
        lines[0] = line;

        // this is an item line
        line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setItemId(new Integer(2));
        // take the description from the item
        line.setUseItem(new Boolean(true));
        lines[1] = line;

        // this is an item line
        line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setItemId(new Integer(3));
        line.setUseItem(new Boolean(true));
        lines[2] = line;

        newOrder.setOrderLines(lines);

        BigDecimal oldBalance = user.getDynamicBalanceAsDecimal();
        System.out.println("User's dynamic balance before placing order is "+oldBalance);
        System.out.println("Creating order ... " + newOrder);
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        newOrder=api.getOrder(orderId);
        BigDecimal orderTotal = new BigDecimal(newOrder.getTotal());
        System.out.println("Total Order Amount"+orderTotal);
        user = api.getUserWS(USER_ID);
        System.out.println("User's dynamic balance after placing order is "+user.getDynamicBalanceAsDecimal());
        assertEquals("User's dynamic balance after placing order",user.getDynamicBalanceAsDecimal(),oldBalance.subtract(orderTotal));
        api.deleteOrder(orderId);
        user = api.getUserWS(USER_ID);
        assertEquals("User Dynamic Balance Back to old balance",user.getDynamicBalanceAsDecimal(),oldBalance    );
    }
    /**
     * Test Scenario: Make a payment in different currency than user's default currency , then create new order event and verify amount
     */
    @Test
    public void testDetermineAmountWithNewOrderEvent() {
        int i;
        System.out.println("Testing determine amount with payment successful event");
        String username = "user-new-order-event";
        String email = "user-payment-succ@jbilling.com";
        String balance = "1";
        int user_id;
        // make a new user with ZERO balance and currency as AUS DOLLAR
        user_id = makeUser(AUS_DOLLAR_ID, username, balance);
        PaymentWS payment = new PaymentWS();
        payment.setUserId(user_id);
        //payment.setCheque(CHEQUE);
        payment.setMethodId(PAYMENT_METHOD_ID);
        Calendar calendar = Calendar.getInstance();
        payment.setPaymentDate(calendar.getTime());
        payment.setCurrencyId(US_DOLLAR_ID);
        payment.setAmount("100");
        payment.setIsRefund(0);
        // check that user's dynamic balance is 100
        UserWS user =  api.getUserWS(user_id);
	    user.setPassword(null);
        System.out.println("User's dynamic balance earlier was "+user.getDynamicBalanceAsDecimal());
        assertTrue("User's Balance would be ONE", (BigDecimal.ONE.compareTo(user.getDynamicBalanceAsDecimal())==0));
        PaymentInformationWS cc = com.sapienter.jbilling.server.user.WSTest.createCreditCard("Frodo Baggins",
                "4111111111111111", new Date());
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
        payment.getPaymentInstruments().add(cc);
        api.createPayment(payment);
        // update the user
        api.updateUser(user);
        user = api.getUserWS(user_id);
        System.out.println("User's dynamic balance now is "+user.getDynamicBalanceAsDecimal());
        System.out.println("Payment amount "+payment.getAmountAsDecimal());
        System.out.println("balance "+new BigDecimal(balance));

        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(user_id);
        newOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        newOrder.setPeriod(Constants.ORDER_PERIOD_ONCE);
        newOrder.setCurrencyId(new Integer(1));
        // notes can only be 200 long... but longer should not fail
        newOrder.setNotes("At the same time the British Crown began bestowing land grants in Nova Scotia on favored subjects to encourage settlement and trade with the mother country. In June 1764, for instance, the Boards of Trade requested the King make massive land grants to such Royal favorites as Thomas Pownall, Richard Oswald, Humphry Bradstreet, John Wentworth, Thomas Thoroton[10] and Lincoln's Inn barrister Levett Blackborne.[11] Two years later, in 1766, at a gathering at the home of Levett Blackborne, an adviser to the Duke of Rutland, Oswald and his friend James Grant were released from their Nova Scotia properties so they could concentrate on their grants in British East Florida.");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2008, 9, 3);
        newOrder.setActiveSince(cal.getTime());

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[3];
        OrderLineWS line;

        line = new OrderLineWS();
        line.setPrice(new BigDecimal("10.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setAmount(new BigDecimal("10.00"));
        line.setDescription("Fist line");
        line.setItemId(new Integer(1));
        lines[0] = line;

        // this is an item line
        line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setItemId(new Integer(2));
        // take the description from the item
        line.setUseItem(new Boolean(true));
        lines[1] = line;

        // this is an item line
        line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setItemId(new Integer(3));
        line.setUseItem(new Boolean(true));
        lines[2] = line;

        newOrder.setOrderLines(lines);

        BigDecimal oldBalance = user.getDynamicBalanceAsDecimal();
        System.out.println("User's dynamic balance before placing order is "+oldBalance);
        System.out.println("Creating order ... " + newOrder);
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        newOrder=api.getOrder(orderId);
        BigDecimal orderTotal = new BigDecimal(newOrder.getTotal());
        System.out.println("Total Order Amount"+orderTotal);
        user = api.getUserWS(user_id);
        System.out.println("User's dynamic balance after placing order is "+user.getDynamicBalanceAsDecimal());
        assertEquals("User's dynamic balance after placing order",user.getDynamicBalanceAsDecimal(),oldBalance.subtract(orderTotal));
        //clean up
        api.deleteOrder(orderId);
        api.deleteUser(user_id);
    }

    /**
     * Creates a user by calling the api. The user will have a pre-paid dynamic balance
     * account with the given
     */
    private int makeUser(int currencyId, String username, String balance) {
        System.out.println("Making User");
        UserWS user = new UserWS();

        user.setCurrencyId(currencyId);
        user.setUserName(username);
        user.setPassword("Admin123@");

        user.setMainRoleId(CUSTOMER_MAIN_ROLE);// customer role
	    user.setRole("Customer");
	    user.setLanguageId(LANGUAGE_ID);
	    user.setDynamicBalance(new BigDecimal(balance));

	    user.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);

	    MetaFieldValueWS metaField1 = new MetaFieldValueWS();
	    metaField1.setFieldName("contact.email");
	    metaField1.setValue(user.getUserName() + "@gmail.com");
	    metaField1.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

	    MetaFieldValueWS metaField2 = new MetaFieldValueWS();
	    metaField2.setFieldName("contact.first.name");
	    metaField2.setValue("Dynamic Balance Manager");
	    metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

	    MetaFieldValueWS metaField3 = new MetaFieldValueWS();
	    metaField3.setFieldName("contact.last.name");
	    metaField3.setValue("Something Else");
	    metaField3.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

	    MetaFieldValueWS metaField4 = new MetaFieldValueWS();
	    metaField4.setFieldName("contact.country.code");
	    metaField4.setValue("US");
	    metaField4.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

	    user.setMetaFields(new MetaFieldValueWS[]{
			    metaField1,
			    metaField2,
			    metaField3,
			    metaField4
	    });

	    System.out.println("Creating user");
	    return api.createUser(user);
    }

	private OrderWS buildOrder(Integer userId) {
		OrderWS newOrder = new OrderWS();
		newOrder.setUserId(userId);
		newOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		newOrder.setPeriod(Constants.ORDER_PERIOD_ONCE);
		newOrder.setCurrencyId(US_DOLLAR_ID);
		newOrder.setNotes("Order to test dynamic balance changes when changing line quantities and price");

		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2006, 9, 3);
		newOrder.setActiveSince(cal.getTime());

		// now add some lines
		OrderLineWS lines[] = new OrderLineWS[2];
		OrderLineWS line;

		line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("Fist line");
		line.setItemId(new Integer(1));
		lines[0] = line;

		// this is an item line
		line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setItemId(new Integer(2));
		// take the description from the item
		line.setUseItem(new Boolean(true));
		lines[1] = line;

		newOrder.setOrderLines(lines);

		return newOrder;
	}

    private OrderLineWS findLine(OrderWS order, Integer orderLineId){
        for(OrderLineWS orderLine : order.getOrderLines()){
            if(0 == orderLine.getItemId().compareTo(orderLineId)){
                return orderLine;
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
		status1.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, status1Name));
		return api.createOrderChangeStatus(apply);
	}
}