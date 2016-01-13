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

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.joda.time.DateMidnight;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.order.WSTest;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;


@Test(groups = { "user" }, testName = "ConcurrentCreateTest")
public class ConcurrentCreateTest {

    public static final Integer USER_COUNT =10;
    public static final Integer ORDER_COUNT =10;

    public static final Map<Integer, List<OrderWS>> userOrderMap = new ConcurrentHashMap<>();

    @Test (threadPoolSize = 4, invocationCount = 10)
    public void test001ConcurrentUsers() {
        System.out.println("#test001ConcurrentUsers");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS userCreated = null;
        try {
            userCreated = createUser(true, null, 1, true);
            assertNotNull(userCreated);
            System.out.println("Getting user " + userCreated.getId());
            UserWS ret = api.getUserWS(new Integer(userCreated.getId()));
            userOrderMap.putIfAbsent(ret.getId(), new ArrayList<OrderWS>());
            assertEquals(userCreated.getId(), ret.getUserId());
            System.out.println("ret.getUserName() = " + ret.getUserName());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        }
    }

    @Test (threadPoolSize = 4, invocationCount = 10)
    public void test002ConcurrentOrders() {
        System.out.println("ConcurrentCreateTest.test001ConcurrentOrders");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS userCreated = null;
        try {
            userOrderMap.forEach( (key, orderList) -> {
                // Create
                OrderWS newOrder = WSTest.buildOneTimePostPaidOrder(key);

                // Add Lines
                OrderLineWS lines[] = new OrderLineWS[3];
                // Set line price
                lines[0] = WSTest.buildOrderLine(2800, 1, BigDecimal.TEN);
                // Use item price
                lines[1] = WSTest.createOrderLine(2800, 1, null);
                // Use item price
                lines[2] = WSTest.createOrderLine(2800, 1, null);

                newOrder.setOrderLines(lines);

                System.out.println("Creating order ... " + newOrder);

                OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
                Integer statusId = null;
                for(OrderChangeStatusWS orderChangeStatus : list){
                    if(orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)){
                        statusId = orderChangeStatus.getId();
                        if ( null != statusId)  break;
                    }
                }
                Integer orderId= api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, statusId));
                assertNotNull(orderId);
                OrderWS createdOrder= api.getOrder(orderId);
                orderList.add(createdOrder)
            });
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        }
    }

    //TODO triggerBilling x 2
    //TODO triggerBilling

    //TODO To be removed from here and instead call CreateObjectUtil.createUser non-static
    public UserWS createUser(boolean goodCC, Integer parentId, Integer currencyId, boolean doCreate) throws JbillingAPIException, IOException {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        /*
        * Create - This passes the password validation routine.
        */
        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        String userName= "testUserName-" + new Random().nextLong();
        System.out.println("Creating user with userName = " + userName);
        newUser.setUserName(userName);
        newUser.setPassword("As$fasdf1");
        newUser.setLanguageId(Integer.valueOf(1));
        newUser.setMainRoleId(Integer.valueOf(5));
        newUser.setAccountTypeId(Integer.valueOf(1));
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setCreditLimit("1");
        newUser.setInvoiceChild(new Boolean(false));

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
        metaField4.setValue("FrodoRecharge");
        metaField4.setGroupId(1);

        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("BagginsRecharge");
        metaField5.setGroupId(1);

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

        // add a credit card
        PaymentInformationWS cc = createCreditCard("Frodo Rech Baggins", goodCC ? "4929974024420784" : "4111111111111111",
                expiry.getTime());

        newUser.getPaymentInstruments().add(cc);

        if (doCreate) {
            System.out.println("Creating user ...");
            Integer userId = api.createUser(newUser);
            newUser = api.getUserWS(userId);
        }

        return newUser;
    }

}