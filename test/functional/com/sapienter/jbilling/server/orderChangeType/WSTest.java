/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.orderChangeType;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderChangeTypeWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.AssertJUnit.*;

/**
 * @author: Alexander Aksenov
 * @since: 21.02.14
 */

@Test(groups = {"web-services", "orderChangeType"})
public class WSTest {

    @Test
    public void test01GetOrderChangeTypes() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        List<OrderChangeTypeWS> currentTypes = Arrays.asList(api.getOrderChangeTypesForCompany());
        assertNotNull("Types should be presented", currentTypes);
        assertFalse("Types should be presented", currentTypes.isEmpty());
        OrderChangeTypeWS defaultType = findTypeById(currentTypes, 1);
        assertNotNull("DEFAULT type should be presented", defaultType);
    }

    public void test02CreateUpdateDeleteOrderChangeType() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        MetaFieldWS[] orderChangeMetaFields = api.getMetaFieldsForEntity("ORDER_CHANGE");
        Set<MetaFieldWS> changeTypeMetaFields = new HashSet<MetaFieldWS>();
        if (orderChangeMetaFields != null && orderChangeMetaFields.length > 0) {
            orderChangeMetaFields[0].setId(0);
            changeTypeMetaFields.add(orderChangeMetaFields[0]);
        }

        OrderChangeTypeWS type1 = new OrderChangeTypeWS();
        type1.setName("Test " + new Date().getTime());
        type1.setDefaultType(false);
        type1.setAllowOrderStatusChange(true);
        type1.setItemTypes(Arrays.asList(1, 2));
        type1.setOrderChangeTypeMetaFields(changeTypeMetaFields);

        Integer typeId = api.createUpdateOrderChangeType(type1);
        assertNotNull("Type should be created", typeId);
        List<OrderChangeTypeWS> types = Arrays.asList(api.getOrderChangeTypesForCompany());
        OrderChangeTypeWS type1Result = findTypeById(types, typeId);
        assertNotNull("Type should be created", type1);
        assertEquals("Incorrect value for name field", type1.getName(), type1Result.getName());
        assertEquals("Incorrect value for defaultType field", type1.isDefaultType(), type1Result.isDefaultType());
        assertEquals("Incorrect value for allowedOrderStatusChange field", type1.isAllowOrderStatusChange(), type1Result.isAllowOrderStatusChange());
        assertEquals("Incorrect value for itemTypes", type1.getItemTypes().size(), type1Result.getItemTypes().size());
        assertEquals("Incorrect value for metaFields", type1.getOrderChangeTypeMetaFields().size(), type1Result.getOrderChangeTypeMetaFields().size());

        // try to create status with the same name
        OrderChangeTypeWS type2 = new OrderChangeTypeWS();
        type2.setName(type1.getName());
        type2.setDefaultType(true);
        type2.setAllowOrderStatusChange(false);
        type2.setItemTypes(Arrays.asList(1));
        type2.setOrderChangeTypeMetaFields(new HashSet<MetaFieldWS>());
        Integer type2Id;
        try {
            type2Id = api.createUpdateOrderChangeType(type2);
            fail("Type with same name should not be created");
        } catch (SessionInternalError ex) {
            assertEquals("Incorrect error", "OrderChangeTypeWS,name,OrderChangeTypeWS.validation.error.name.not.unique," + type2.getName(), ex.getErrorMessages()[0]);
        }
        type2.setName("Test2 " + new Date().getTime());
        type2Id = api.createUpdateOrderChangeType(type2);
        assertNotNull("Type should be created", type2Id);
        types =  Arrays.asList(api.getOrderChangeTypesForCompany());
        type1Result = findTypeById(types, typeId);
        OrderChangeTypeWS type2Result = findTypeById(types, type2Id);

        assertNotNull("Type should be created", type2);
        assertNotNull("Type should be created", type1);
        assertEquals("Incorrect value for name field", type2.getName(), type2Result.getName());
        assertEquals("Incorrect value for defaultType field", type2.isDefaultType(), type2Result.isDefaultType());
        assertEquals("Incorrect value for allowedOrderStatusChange field", type2.isAllowOrderStatusChange(), type2Result.isAllowOrderStatusChange());
        assertEquals("Incorrect value for itemTypes (should be cleared because of default flag)", 0, type2Result.getItemTypes().size());
        assertEquals("Incorrect value for metaFields", type2.getOrderChangeTypeMetaFields().size(), type2Result.getOrderChangeTypeMetaFields().size());

        type1Result.setAllowOrderStatusChange(!type1.isAllowOrderStatusChange());
        api.createUpdateOrderChangeType(type1Result);
        types = Arrays.asList(api.getOrderChangeTypesForCompany());
        type1Result = findTypeById(types, typeId);

        assertEquals("Incorrect value for allowOrderStatusChange field", !type1.isAllowOrderStatusChange(), type1Result.isAllowOrderStatusChange());

        api.deleteOrderChangeType(type2Id);
        types = Arrays.asList(api.getOrderChangeTypesForCompany());
        type2 = findTypeById(types, type2Id);
        assertNull("Type should be deleted", type2);
    }

    private OrderChangeTypeWS findTypeById(List<OrderChangeTypeWS> types, Integer targetId) {
        for (OrderChangeTypeWS ws : types) {
            if (ws.getId().equals(targetId)) {
                return ws;
            }
        }
        return null;
    }
}

