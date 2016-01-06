package com.sapienter.jbilling.server.util.credentials;

import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.test.ApiTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Javier Rivero
 * @since 17/04/15.
 */
public class PasswordServiceTest extends ApiTestCase {
    protected static final Integer CUSTOMER_MAIN_ROLE = Integer.valueOf(5);
    protected static final Integer CUSTOMER_ACTIVE = Integer.valueOf(1);
    protected static final Integer CURRENCY_USD = Integer.valueOf(1);
    protected static final Integer LANGUAGE_US = Integer.valueOf(1);
    protected String random = String.valueOf(new Random().nextInt(100));

    private UserWS customer;

    @BeforeMethod
    protected void setup() {
        //create User
        this.customer = CreateObjectUtil.createCustomer(
                CURRENCY_USD, "testRateOrderApi-New-" + random, "newPa$$word1",
                LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
                CreateObjectUtil.createCustomerContact("test@gmail.com"));
        Integer customerId = api.createUser(this.customer);
        this.customer.setUserId(customerId);
        assertNotNull("Customer/User ID should not be null", customerId);
    }

    @AfterMethod
    protected void tearDown() {
        if (this.customer != null) {
            api.deleteUser(this.customer.getUserId());
        }
    }

    @Test
    public void passwordServiceTest(){

    }

}
