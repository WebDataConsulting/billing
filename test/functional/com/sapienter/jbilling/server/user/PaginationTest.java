package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.AssertJUnit.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Test the pagination for user's orders invoices and payments
 *
 * @author Panche Isajeski
 * @since 03/01/2013
 */
@Test(groups = { "web-services", "user" })
public class PaginationTest {

    private JbillingAPI api;

    private static final Integer GANDALF_USER_ID = 2;

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
    }

    @Test
    public void testUserOrdersPagination() {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();

            // Gandalf has 7 orders
            // fetch the first 5
            OrderWS[] ordersPaged = api.getUserOrdersPage(GANDALF_USER_ID, 5, 0);
            assertNotNull(ordersPaged);
            assertEquals("Retrieved paginated orders does not match the expected size 5",
                    ordersPaged.length, 5);

            assertThat(ordersPaged[0].getId(), is(1055));
            assertThat(ordersPaged[1].getId(), is(25));
            assertThat(ordersPaged[2].getId(), is(15));
            assertThat(ordersPaged[3].getId(), is(4));
            assertThat(ordersPaged[4].getId(), is(3));

            // get the next 5
            ordersPaged = api.getUserOrdersPage(GANDALF_USER_ID, 5, 5);
            assertNotNull(ordersPaged);
            assertEquals("Retrieved paginated orders does not match the expected size 2",
                    ordersPaged.length, 2);

            assertThat(ordersPaged[0].getId(), is(2));
            assertThat(ordersPaged[1].getId(), is(1));

            ordersPaged = api.getUserOrdersPage(GANDALF_USER_ID, 5, 10);
            assertTrue(ordersPaged.length == 0);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        }

    }

    @Test
    public void testUserInvoicesPagination() {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();

            // Gandalf has 8 invoices
            InvoiceWS[] invoicesPaged = api.getUserInvoicesPage(GANDALF_USER_ID, 5, 0);
            assertNotNull(invoicesPaged);
            assertEquals("Retrieved paginated invoices does not match the expected size 5",
                    invoicesPaged.length, 5);

            assertThat(invoicesPaged[0].getId(), is(45));
            assertThat(invoicesPaged[1].getId(), is(35));
            assertThat(invoicesPaged[2].getId(), is(15));
            assertThat(invoicesPaged[3].getId(), is(5));
            assertThat(invoicesPaged[4].getId(), is(4));

            invoicesPaged = api.getUserInvoicesPage(GANDALF_USER_ID, 5, 5);
            assertNotNull(invoicesPaged);
            assertEquals("Retrieved paginated invoices does not match the expected size 3",
                    invoicesPaged.length, 3);

            assertThat(invoicesPaged[0].getId(), is(3));
            assertThat(invoicesPaged[1].getId(), is(2));
            assertThat(invoicesPaged[2].getId(), is(1));

            invoicesPaged = api.getUserInvoicesPage(GANDALF_USER_ID, 5, 10);
            assertTrue(invoicesPaged.length == 0);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        }
    }

    @Test
    public void testUserPaymentsPagination() {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();
            // Gandalf has 2 payments
            PaymentWS[] paymentsPaged = api.getUserPaymentsPage(GANDALF_USER_ID, 5, 0);
            assertNotNull(paymentsPaged);
            assertEquals("Retrieved paginated payments does not match the expected size 2",
                    paymentsPaged.length, 2);

            assertThat(paymentsPaged[0].getId(), is(6));
            assertThat(paymentsPaged[1].getId(), is(5));

            paymentsPaged = api.getUserPaymentsPage(GANDALF_USER_ID, 5, 5);
            assertTrue(paymentsPaged.length == 0);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        }
    }
}
