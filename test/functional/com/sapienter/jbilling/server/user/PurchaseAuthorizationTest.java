package com.sapienter.jbilling.server.user;
/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.joda.time.DateMidnight;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
@Test(groups = { "web-services", "purchase-auth" })
public class PurchaseAuthorizationTest {

    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;

@Test
public void testPurchaseAuthorization(){
    System.out.println("#test013RulesValidatePurchaseTask");
    try {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS myUser = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);
        Integer myId = myUser.getUserId();
        // update credit limit
        myUser.setCreditLimit(new BigDecimal("10"));
        myUser.setPassword(null);
        api.updateUser(myUser);
        PaymentWS payment = new PaymentWS();
        payment.setUserId(myId);
        //payment.setCheque(CHEQUE);
        payment.setMethodId(1);
        Calendar calendar = Calendar.getInstance();
        payment.setPaymentDate(calendar.getTime());
        payment.setCurrencyId(1);
        payment.setAmount("10");
        payment.setIsRefund(0);
        // check that user's dynamic balance is 20
        UserWS user =  api.getUserWS(myId);
        System.out.println("User's dynamic balance earlier was "+user.getDynamicBalanceAsDecimal());
        PaymentInformationWS cc = com.sapienter.jbilling.server.user.WSTest.createCreditCard("Frodo Baggins",
                "4111111111111111", new Date());
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
        payment.getPaymentInstruments().add(cc);


        api.createPayment(payment);
        // update the user
        user.setPassword(null);
        api.updateUser(user);
        user = api.getUserWS(myId);
        System.out.println("User's dynamic balance earlier was "+user.getDynamicBalanceAsDecimal());
        ItemDTOEx newItem = new ItemDTOEx();
        newItem.setDescription("TEST Item");
        newItem.setPrice(new BigDecimal("20"));
        newItem.setNumber("WS-001");
        Integer types[] = new Integer[1];
        types[0] = new Integer(1);
        newItem.setTypes(types);
        System.out.println("Creating item ..." + newItem);
        Integer itemId = api.createItem(newItem);
        //credit_limit(10)+balance(10)=order total(20)
        ValidatePurchaseWS result=api.validatePurchase(myId,itemId,null);
        AssertJUnit.assertEquals("validate purchase success 1", Boolean.valueOf(true), result.getSuccess());
        AssertJUnit.assertEquals("validate purchase authorized 1", Boolean.valueOf(true), result.getAuthorized());
        assertEquals("validate purchase quantity 1", new BigDecimal("1.00"), result.getQuantityAsDecimal());
        // now create a one time order, the balance should decrease this one time order is of order total 20
        OrderWS order = com.sapienter.jbilling.server.user.WSTest.getOrder();
        order.setUserId(myId);
        System.out.println("creating one time order");
        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        System.out.println("Validating new balance");
        myUser = api.getUserWS(myId);
        assertEquals("user should have -ve 10 balance", new BigDecimal("10.0").negate(), myUser.getDynamicBalanceAsDecimal());
        //now balance(-10)+credit_limit(10) and order total -10 so the result should fail
        result=api.validatePurchase(myId,itemId,null);
        AssertJUnit.assertEquals("validate purchase success 2", Boolean.valueOf(true), result.getSuccess());
        AssertJUnit.assertEquals("validate purchase authorized 2", Boolean.valueOf(false), result.getAuthorized());
        assertEquals("validate purchase quantity 1", new BigDecimal("0.00"), result.getQuantityAsDecimal());

        api.deleteUser(myId);
    } catch (Exception e) {
        e.printStackTrace();
        fail("Exception caught:" + e);
    }


}

}
