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

/**
 * 
 */
package com.sapienter.jbilling.server.task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.test.ApiTestCase;

import junit.framework.TestCase;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.testng.annotations.*;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * Test class to unit test the functionality of Tax applied to invoices that
 * belong to user from a configured country.
 * 
 * @author Vikas Bodani
 * @since 29-Jul-2011
 * 
 */
@Test(groups = { "integration", "task", "tax", "paymentpenalty" })
public class PaymentTermPenaltyTaskTest extends ApiTestCase {

    private static final int PENALTY_TERM_PLUGIN_TYPE_ID = 91;

	private static int ORDER_CHANGE_STATUS_APPLY_ID;
	private static int CURRENCY_USD;
	private static int LANGUAGE_ID;
	private static int PRANCING_PONY_ACCOUNT_TYPE;

	protected void prepareTestInstance() throws Exception {
		super.prepareTestInstance();
		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
		CURRENCY_USD = ServerConstants.PRIMARY_CURRENCY_ID;
		LANGUAGE_ID = ServerConstants.LANGUAGE_ENGLISH_ID;
		PRANCING_PONY_ACCOUNT_TYPE = Integer.valueOf(1);
	}

    @Test
    public void testPenalty() {
        System.out.println("#testPenalty");

	    ItemTypeWS penaltyCategory = buildPenaltyCategory();
	    penaltyCategory.setId(api.createItemCategory(penaltyCategory));

	    System.out.println("Creating the Penalty Item.");
	    Integer taxItemID = createPenaltyItem(penaltyCategory.getId());
	    assertNotNull("Penalty Item Id  should not be null.", taxItemID);

	    System.out.println("Penalty Item ID: " + taxItemID + "\nAdding a PaymentTermPenaltyTask Plugin.");

	    Integer taxPluginId = enableTaxPlugin(taxItemID);
	    assertNotNull("Plugin id is not null.", taxPluginId);


        System.out.println("Adding a new user with contact and country set to AU.");
        Integer userId = api.createUser(createUserWS("testPenltTsk" + System.currentTimeMillis(), ServerConstants.PERIOD_UNIT_MONTH, 1));//due date 1 month or 30 days
        assertNotNull("Test fail at user creation.", userId);

		ItemTypeWS itemCategory = buildItemCategory();
	    itemCategory.setId(api.createItemCategory(itemCategory));
	    Integer itemId = createItem(itemCategory.getId());

        System.out.println("Plugin ID: " + taxPluginId + "\nCreating order for user 1 & Generate invoice.");
        OrderWS orderWS = createOrderWs(userId, itemId, ServerConstants.PERIOD_UNIT_MONTH, 1);
	    //due date 1 month or 30 days
        Integer invId = api.createOrderAndInvoice(orderWS, OrderChangeBL.buildFromOrder(orderWS, ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Order ID should have value.", invId);

        InvoiceWS invoice = api.getInvoiceWS(invId);
        InvoiceLineDTO[] lines = invoice.getInvoiceLines();
        System.out.println("Invoice ID: " + invId + "\nInspecting invoices lines..");

        for (InvoiceLineDTO line : lines) {
            System.out.println(line.getDescription());
            if (line.getDescription() != null && line.getDescription().startsWith("Tax or Penalty")) {
                System.out.println("Penalty line found. Amount=" + line.getAmount());
                assertTrue(line.getItemId().intValue() == taxItemID.intValue());
                System.out.println("The amount should be equal to $0.5 (1 percent of 50)");
                assertEquals(
                        "The amount should have been $0.5 (1 percent of 50).",
                        new BigDecimal("0.5").compareTo(line.getAmountAsDecimal())==0);
            }
        }

        System.out.println("Successful, testPenalty");

		//cleanup
	    api.deletePlugin(taxPluginId);
        api.deleteInvoice(invId);
        OrderWS order = api.getLatestOrder(userId);
        api.deleteOrder(order.getId());
	    api.deleteItem(itemId);
	    api.deleteItemCategory(itemCategory.getId());
        api.deleteItem(taxItemID);
	    api.deleteItemCategory(penaltyCategory.getId());
        api.deleteUser(userId);
    }

    @Test
    public void testNoPenalty() {
        System.out.println("#testNoPenalty");

	    ItemTypeWS penaltyCategory = buildPenaltyCategory();
	    penaltyCategory.setId(api.createItemCategory(penaltyCategory));

	    System.out.println("Creating the Penalty Item.");
	    Integer taxItemID = createPenaltyItem(penaltyCategory.getId());
	    assertNotNull("Penalty Item Id  should not be null.", taxItemID);

	    System.out.println("Penalty Item ID: " + taxItemID + "\nAdding a PaymentTermPenaltyTask Plugin.");

	    Integer taxPluginId = enableTaxPlugin(taxItemID);
	    assertNotNull("Plugin id is not null.", taxPluginId);

        System.out.println("Adding a new user with contact and country set to non au.");
        Integer userId = api.createUser(createUserWS("testPenltTsk" + System.currentTimeMillis(), ServerConstants.PERIOD_UNIT_DAY, 10));//10 days due date
        assertNotNull("Test fail at user creation.", userId);

        System.out.println("User ID: " + userId + "\nPenalty Item for IN does not exists.");
        System.out.println("Penalty Plugin for AU exists, but not for IN.");
        System.out.println("Creating order for user 2 & Generate invoice.");

	    ItemTypeWS itemCategory = buildItemCategory();
	    itemCategory.setId(api.createItemCategory(itemCategory));
	    Integer itemId = createItem(itemCategory.getId());

        OrderWS orderWS = createOrderWs(userId, itemId, ServerConstants.PERIOD_UNIT_DAY, 10);
        Integer invId = api.createOrderAndInvoice(orderWS, OrderChangeBL.buildFromOrder(orderWS, ORDER_CHANGE_STATUS_APPLY_ID));//order due date 10 days
        assertNotNull("Invoice ID should have value.", invId);

        InvoiceWS invoice = api.getInvoiceWS(invId);
        InvoiceLineDTO[] lines = invoice.getInvoiceLines();
        System.out.println("Invoice ID: " + invId + "\nInspecting invoices lines..");

        assertTrue("There should have been only 1 line.", lines.length == 1);

        for (InvoiceLineDTO line : lines) {
            System.out.println(line.getDescription());
            if (line.getDescription() != null) {
                assertFalse(line.getDescription().startsWith(
                        "Tax or Penalty"));
            }
        }
        System.out.println("Successful, testNoPenalty");

		//cleanup
	    api.deletePlugin(taxPluginId);
        api.deleteInvoice(invId);
        OrderWS order = api.getLatestOrder(userId);
        api.deleteOrder(order.getId());
	    api.deleteItem(taxItemID);
	    api.deleteItemCategory(penaltyCategory.getId());
	    api.deleteItem(itemId);
	    api.deleteItemCategory(itemCategory.getId());
        api.deleteUser(userId);
    }

	private Integer createPenaltyItem(Integer categoryId) {
		ItemDTOEx item = new ItemDTOEx();
		item.setCurrencyId(CURRENCY_USD);
		
		item.setDescription("1 % Penalty Item");
		item.setEntityId(TEST_ENTITY_ID);
		item.setNumber("PYMPEN");
		item.setTypes(new Integer[]{categoryId});
		return api.createItem(item);
	}

	private Integer createItem(Integer categoryId) {
		ItemDTOEx item = new ItemDTOEx();
		item.setCurrencyId(CURRENCY_USD);
		item.setPrice(new BigDecimal("3.5"));
		item.setDescription("ITEM-PEN");
		item.setEntityId(TEST_ENTITY_ID);
		item.setNumber("PEN-ITEM-AU");
		item.setTypes(new Integer[]{categoryId});
		item.setId(api.createItem(item));
		return item.getId();
	}

    private OrderWS createOrderWs(
		    Integer userId, Integer itemId,
		    Integer dueDateUnitId, Integer dueDateUnitValue) {

        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ServerConstants.ORDER_PERIOD_ONCE); // once
        order.setCurrencyId(CURRENCY_USD);
        order.setActiveSince(new Date());
        order.setOrderLines(new OrderLineWS[] { createOrderLineWS(itemId) });
        order.setDueDateUnitId(dueDateUnitId);
        order.setDueDateValue(dueDateUnitValue);
        return order;
    }

    private OrderLineWS createOrderLineWS(Integer itemId) {
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(itemId);
        line.setQuantity(1);
	    line.setUseItem(Boolean.FALSE);
        line.setPrice(new BigDecimal("50"));
        line.setAmount(new BigDecimal("50"));
        return line;
    }

    private UserWS createUserWS(String userName, Integer dueDateUnitId, Integer dueDateUnitValue) {
        UserWS newUser = new UserWS();
        newUser.setUserId(0);
        newUser.setUserName(userName);
        newUser.setPassword("Admin123@");
        newUser.setLanguageId(LANGUAGE_ID);
        newUser.setMainRoleId(ServerConstants.TYPE_CUSTOMER);
        newUser.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);
        newUser.setParentId(null);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(CURRENCY_USD);
        newUser.setDueDateUnitId(dueDateUnitId);
        newUser.setDueDateValue(dueDateUnitValue);

        newUser.setMetaFields(createContactMetaFields(newUser.getUserName()));

        return newUser;
    }

    private MetaFieldValueWS[] createContactMetaFields(String username){
        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(username + "@gmail.com");
        metaField1.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Test");
        metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue("Plugin");
        metaField3.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        return new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
        };
    }

	private ItemTypeWS buildItemCategory() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("Penalty Test Item Cat:" + System.currentTimeMillis());
		type.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}

	private ItemTypeWS buildPenaltyCategory() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("Payment Item Cat:" + System.currentTimeMillis());
		type.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_PENALTY);
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
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

	private Integer enableTaxPlugin(Integer itemId) {
		PluggableTaskWS plugin = new PluggableTaskWS();
		plugin.setTypeId(PENALTY_TERM_PLUGIN_TYPE_ID);
		plugin.setProcessingOrder(4);

		// plug-in adds the given tax item to the invoice
		// when the customers country code is Australia 'AU'
		Hashtable<String, String> parameters = new Hashtable<String, String>();
		parameters.put("charge_carrying_item_id", itemId.toString());
		parameters.put("penalty_after_days", "14");
		plugin.setParameters(parameters);

		return api.createPlugin(plugin);
	}
}
