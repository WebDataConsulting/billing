package com.sapienter.jbilling.server.task;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import com.sapienter.jbilling.server.util.CreateObjectUtil;

@Test(groups = { "integration", "task", "auto-recharge" })
public class AutoRechargeTaskTest {
	
	private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private JbillingAPI api;

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
    }

    @Test
    public void testCustomerSpecificThresholdLimit() throws Exception{
        //create a new user with pre-paid balance type
    	
        UserWS user = CreateObjectUtil.createUser(true, null, null);
        user.setMonthlyLimit("100");
	    user.setCreditLimit("1");

        //Set a threshold to the user for auto-recharge
        user.setRechargeThreshold("1.00");
        user.setAutoRecharge("10.00");
        user.setPassword(null);
        api.updateUser(user);

        //Charge the account with an amount higher than the threshold
        chargeUser(user.getUserId(), new BigDecimal("2.00"));

        //check that the auto-recharge takes place
        assertBigDecimalEquals(new BigDecimal("8.00"), getDynamicBalanceAsDecimal(user));
    }

    @Test
    public void testMonthlyLimitReached() throws Exception{
        //create a new user with pre-paid balance type
        UserWS user = CreateObjectUtil.createUser(true, null, null);

        //Set a threshold to the user to 1$
        user.setRechargeThreshold("1.00");

        //recharge amount of 5$
        user.setAutoRecharge("5.00");

        //monthly limit of 8$
        user.setMonthlyLimit("8.00");

        user.setPassword(null);

        api.updateUser(user);

        //charge the user 3$
        chargeUser(user.getUserId(), new BigDecimal("3.00"));

        //Assert that balance is now 2$
        assertBigDecimalEquals(new BigDecimal("2.00"), getDynamicBalanceAsDecimal(user));

        // Charge the user again for time 3$
        chargeUser(user.getUserId(), new BigDecimal("3.00"));

        //Assert that the auto-recharge has not been done
        assertBigDecimalEquals(new BigDecimal("-1.00"), getDynamicBalanceAsDecimal(user));
    }

    private void chargeUser(Integer userId, BigDecimal amount)throws Exception{
        // setup order
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(1); // once
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(1);
        line.setQuantity(1);
        line.setPrice(amount);
        line.setAmount(amount);

        order.setOrderLines(new OrderLineWS[] { line });

        api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // create invoice
        Integer[] invoices = api.createInvoice(userId, false);

        assertEquals("Number of invoices returned", 1, invoices.length);
    }

    private BigDecimal getDynamicBalanceAsDecimal(UserWS user) {
        return (user != null) ? getDynamicBalanceAsDecimal(new Integer(user.getUserId())) : BigDecimal.ZERO;
    }

    private BigDecimal getDynamicBalanceAsDecimal(Integer userId) {
        return api.getUserWS(userId).getDynamicBalanceAsDecimal();
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertTrue("expected: <" + expected.toPlainString() + "> but was: <" + actual.toPlainString() + ">",
        expected.compareTo(actual) == 0);
    }
}
