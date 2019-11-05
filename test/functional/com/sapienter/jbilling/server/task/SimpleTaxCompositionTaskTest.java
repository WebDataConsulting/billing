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

package com.sapienter.jbilling.server.task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
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
 * @author Alexander Aksenov, Vikas Bodani
 * @since 30.04.11
 */
@Test(groups = { "integration", "task", "tax", "simpletax" })
public class SimpleTaxCompositionTaskTest extends ApiTestCase {

    private static final Integer SIMPLE_TAX_PLUGIN_TYPE_ID = 86;

	private static int ORDER_CHANGE_STATUS_APPLY_ID;
	private static int CURRENCY_USD;
	private static int LANGUAGE_ID;
	private static int PRANCING_PONY_ACCOUNT_TYPE;

    private Integer taxPluginId;

	protected void prepareTestInstance() throws Exception {
		super.prepareTestInstance();
		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
		CURRENCY_USD = ServerConstants.PRIMARY_CURRENCY_ID;
		LANGUAGE_ID = ServerConstants.LANGUAGE_ENGLISH_ID;
		PRANCING_PONY_ACCOUNT_TYPE = Integer.valueOf(1);
	}

    @AfterMethod
    private void afterTestMethod() {
        if (taxPluginId != null) {
            api.deletePlugin(taxPluginId);
            taxPluginId = null;
        }
    }

    @Test
    public void testInvoiceWithTaxableItems() throws Exception {
        System.out.println("#testInvoiceWithTaxableItems");

	    //create tax category
	    ItemTypeWS taxCategory = buildTaxCategory();
	    taxCategory.setId(api.createItemCategory(taxCategory));

        // add a new tax item & enable the tax plug-in
        ItemDTOEx taxItem = new ItemDTOEx();
        taxItem.setCurrencyId(CURRENCY_USD);
        taxItem.setPrice(new BigDecimal("10.00"));    // $10 flat fee
        taxItem.setHasDecimals(1);
        taxItem.setDescription("Tax line with flat price for tax item.");
        taxItem.setEntityId(TEST_ENTITY_ID);
        taxItem.setNumber("TAX");
        taxItem.setTypes(new Integer[]{taxCategory.getId()});

        taxItem.setId(api.createItem(taxItem));
        assertNotNull("tax item created", taxItem.getId());

	    taxPluginId = enableTaxPlugin(taxItem.getId(), null);

	    //create item category
	    ItemTypeWS itemCategory = buildItemCategory();
	    itemCategory.setId(api.createItemCategory(itemCategory));

	    //create a dummy item to be used in the order
	    ItemDTOEx item = new ItemDTOEx();
	    item.setCurrencyId(CURRENCY_USD);
	    item.setPrice(new BigDecimal("3.5"));
	    item.setDescription("DUMMY-ITEM");
	    item.setEntityId(TEST_ENTITY_ID);
	    item.setNumber("ITEM-SIMPLE-TAX");
	    item.setTypes(new Integer[]{itemCategory.getId()});
	    item.setId(api.createItem(item));

        // create a user for testing
        UserWS user = new UserWS();
        user.setUserName("simple-tax-01-" + new Date().getTime());
        user.setPassword("Admin123@");
        user.setLanguageId(LANGUAGE_ID);
        user.setCurrencyId(CURRENCY_USD);
        user.setMainRoleId(ServerConstants.TYPE_CUSTOMER);
        user.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(user.getUserName() + "@test.com");
        metaField1.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Simple Tax Test");
        metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue("Flat Rate");
        metaField3.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        user.setMetaFields(new MetaFieldValueWS[]{
		        metaField1,
		        metaField2,
		        metaField3,
        });

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());


        // purchase order with taxable items
        OrderWS order = new OrderWS();
        order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
        order.setPeriod(ServerConstants.ORDER_PERIOD_ONCE);
        order.setCurrencyId(CURRENCY_USD);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(item.getId());
        line.setUseItem(true);
        line.setQuantity(10);
        order.setOrderLines(new OrderLineWS[]{line});

        order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID))); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());


        // generate an invoice and verify the taxes
        Integer invoiceId = api.createInvoiceFromOrder(order.getId(), null);
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);

        assertNotNull("invoice generated", invoice);
        assertEquals("two lines in invoice, lemonade and the tax line", 2, invoice.getInvoiceLines().length);

        boolean foundTaxItem = false;
        boolean foundItem = false;

        for (InvoiceLineDTO invoiceLine : invoice.getInvoiceLines()) {

            // purchased lemonade
            if (invoiceLine.getItemId().equals(item.getId())) {
                assertEquals("item", "DUMMY-ITEM", invoiceLine.getDescription());
                assertEquals("$35", new BigDecimal("35"), invoiceLine.getAmountAsDecimal());
                foundItem = true;
            }

            // tax is a flat fee, not affected by the price of the invoice
            if (invoiceLine.getItemId().equals(taxItem.getId())) {
                assertEquals("tax item", "Tax line with flat price for tax item.", invoiceLine.getDescription());
                assertEquals("tax $10", new BigDecimal("10"), invoiceLine.getAmountAsDecimal());
                foundTaxItem = true;
            }
        }

        assertTrue("found and validated tax", foundTaxItem);
        assertTrue("found and validated item", foundItem);


        // cleanup
	    api.deleteInvoice(invoice.getId());
	    api.deleteOrder(order.getId());
	    api.deleteItem(taxItem.getId());
	    api.deleteItemCategory(taxCategory.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemCategory.getId());
        api.deleteUser(user.getUserId());
    }

    @Test
    public void testInvoiceWithExemptItems() throws Exception {
        System.out.println("#testInvoiceWithExemptItems");

	    //create tax category
	    ItemTypeWS taxCategory = buildTaxCategory();
	    taxCategory.setId(api.createItemCategory(taxCategory));

        // add a new tax item & enable the tax plug-in
        ItemDTOEx taxItem = new ItemDTOEx();
        taxItem.setCurrencyId(CURRENCY_USD);

        
        taxItem.setHasDecimals(1);
        taxItem.setDescription("Tax");
        taxItem.setEntityId(TEST_ENTITY_ID);
        taxItem.setNumber("TAX");
        taxItem.setTypes(new Integer[]{taxCategory.getId()});

        taxItem.setId(api.createItem(taxItem));
        assertNotNull("tax item created", taxItem.getId());

	    //create item category
	    ItemTypeWS itemCategory = buildItemCategory();
	    itemCategory.setId(api.createItemCategory(itemCategory));

	    //create a dummy item to be used in the order
	    ItemDTOEx item = new ItemDTOEx();
	    item.setCurrencyId(CURRENCY_USD);
	    item.setPrice(new BigDecimal("3.5"));
	    item.setDescription("DUMMY-ITEM");
	    item.setEntityId(TEST_ENTITY_ID);
	    item.setNumber("ITEM-SIMPLE-TAX");
	    item.setTypes(new Integer[]{itemCategory.getId()});
	    item.setId(api.createItem(item));

	    //exempt item category
	    ItemTypeWS exemptCategory = buildItemCategory();
	    exemptCategory.setId(api.createItemCategory(exemptCategory));

	    //create a dummy item to be used in the order
	    ItemDTOEx exemptItem = new ItemDTOEx();
	    exemptItem.setCurrencyId(CURRENCY_USD);
	    exemptItem.setPrice(new BigDecimal("25"));
	    exemptItem.setDescription("EXEMPT-DUMMY-ITEM");
	    exemptItem.setEntityId(TEST_ENTITY_ID);
	    exemptItem.setNumber("EXEMPT-ITEM-SIMPLE-TAX");
	    exemptItem.setTypes(new Integer[]{exemptCategory.getId()});
	    exemptItem.setId(api.createItem(exemptItem));

	    taxPluginId = enableTaxPlugin(taxItem.getId(), exemptCategory.getId());

        // create a user for testing
        UserWS user = new UserWS();
        user.setUserName("simple-tax-02-" + new Date().getTime());
        user.setPassword("Admin123@");
        user.setLanguageId(LANGUAGE_ID);
        user.setCurrencyId(CURRENCY_USD);
        user.setMainRoleId(ServerConstants.TYPE_CUSTOMER);
        user.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(user.getUserName() + "@test.com");
        metaField1.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Simple Tax Test");
        metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue("% Tax with Exemptions");
        metaField3.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        user.setMetaFields(new MetaFieldValueWS[]{
		        metaField1,
		        metaField2,
		        metaField3,
        });

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());


        // purchase order with taxable items
        OrderWS order = new OrderWS();
        order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
        order.setPeriod(ServerConstants.ORDER_PERIOD_ONCE);
        order.setCurrencyId(CURRENCY_USD);
        order.setActiveSince(new Date());

        OrderLineWS line1 = new OrderLineWS();
        line1.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line1.setItemId(item.getId());
        line1.setUseItem(true);
        line1.setQuantity(10); // $3.5 x 10 = $35 line

        OrderLineWS line2 = new OrderLineWS();
        line2.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line2.setItemId(exemptItem.getId());
        line2.setUseItem(true);
        line2.setQuantity(2);  // $25 x 2 = $50 line

        order.setOrderLines(new OrderLineWS[]{line1, line2});

        order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID))); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());


        // generate an invoice and verify the taxes
        Integer invoiceId = api.createInvoiceFromOrder(order.getId(), null);
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);

        assertNotNull("invoice generated", invoice);
        assertEquals("three lines in invoice including tax line", 3, invoice.getInvoiceLines().length);

        boolean foundTaxItem = false;
        boolean foundItem = false;
        boolean foundExemptItem = false;

        for (InvoiceLineDTO invoiceLine : invoice.getInvoiceLines()) {

            // purchased lemonade
            if (invoiceLine.getItemId().equals(item.getId())) {
                assertEquals("dummy item", "DUMMY-ITEM", invoiceLine.getDescription());
                assertEquals("dummy $35", new BigDecimal("35"), invoiceLine.getAmountAsDecimal());
                foundItem = true;
            }

            // purchased tax exempt long distance plan
            if (invoiceLine.getItemId().equals(exemptItem.getId())) {
                assertEquals("exempt item", "EXEMPT-DUMMY-ITEM", invoiceLine.getDescription());
                assertEquals("exempt $50", new BigDecimal("50"), invoiceLine.getAmountAsDecimal());
                foundExemptItem = true;
            }

            // tax, %10 of taxable item total ($35 x 0.10 = $3.5)
            // excludes $50 from the tax exempt line
            if (invoiceLine.getItemId().equals(taxItem.getId())) {
                assertEquals("tax item", "Tax", invoiceLine.getDescription());
                assertEquals("tax $3.5", new BigDecimal("3.5"), invoiceLine.getAmountAsDecimal());
                foundTaxItem = true;
            }
        }

        assertTrue("found and validated tax", foundTaxItem);
        assertTrue("found and validated item", foundItem);
        assertTrue("found and validated exempt item", foundExemptItem);

        // cleanup
	    api.deleteInvoice(invoice.getId());
	    api.deleteOrder(order.getId());
	    api.deleteItem(taxItem.getId());
	    api.deleteItemCategory(taxCategory.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemCategory.getId());
	    api.deleteItem(exemptItem.getId());
	    api.deleteItemCategory(exemptCategory.getId());
	    api.deleteUser(user.getUserId());
    }

    @Test
    public void testInvoiceWithNoTaxableItems() throws Exception {
        System.out.println("#testInvoiceWithNoTaxableItems");

	    //create tax category
	    ItemTypeWS taxCategory = buildTaxCategory();
	    taxCategory.setId(api.createItemCategory(taxCategory));

        // add a new tax item & enable the tax plug-in
        ItemDTOEx taxItem = new ItemDTOEx();
        taxItem.setCurrencyId(CURRENCY_USD);

        taxItem.setHasDecimals(1);
        taxItem.setDescription("Tax");
        taxItem.setEntityId(TEST_ENTITY_ID);
        taxItem.setNumber("TAX");
        taxItem.setTypes(new Integer[]{taxCategory.getId()});

        taxItem.setId(api.createItem(taxItem));
        assertNotNull("tax item created", taxItem.getId());

	    //exempt item category
	    ItemTypeWS exemptCategory = buildItemCategory();
	    exemptCategory.setId(api.createItemCategory(exemptCategory));

	    //create a dummy item to be used in the order
	    ItemDTOEx exemptItem = new ItemDTOEx();
	    exemptItem.setCurrencyId(CURRENCY_USD);
	    exemptItem.setPrice(new BigDecimal("25"));
	    exemptItem.setDescription("EXEMPT-DUMMY-ITEM");
	    exemptItem.setEntityId(TEST_ENTITY_ID);
	    exemptItem.setNumber("EXEMPT-ITEM-SIMPLE-TAX");
	    exemptItem.setTypes(new Integer[]{exemptCategory.getId()});
	    exemptItem.setId(api.createItem(exemptItem));

	    taxPluginId = enableTaxPlugin(taxItem.getId(), exemptCategory.getId());


        // create a user for testing
        UserWS user = new UserWS();
        user.setUserName("simple-tax-03-" + new Date().getTime());
        user.setPassword("Admin123@");
        user.setLanguageId(LANGUAGE_ID);
        user.setCurrencyId(CURRENCY_USD);
        user.setMainRoleId(ServerConstants.TYPE_CUSTOMER);
        user.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(user.getUserName() + "@test.com");
        metaField1.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Simple Tax Test");
        metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue("No Taxable Items");
        metaField3.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        user.setMetaFields(new MetaFieldValueWS[]{
		        metaField1,
		        metaField2,
		        metaField3,
        });

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());


        // purchase order without any taxable items
        OrderWS order = new OrderWS();
        order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_POST_PAID);
        order.setPeriod(ServerConstants.ORDER_PERIOD_ONCE);
        order.setCurrencyId(CURRENCY_USD);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(exemptItem.getId());
        line.setUseItem(true);
        line.setQuantity(2);  // $25 x 2 = $50 line

        order.setOrderLines(new OrderLineWS[]{line});

        order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID))); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());


        // generate an invoice and verify the taxes
        Integer invoiceId = api.createInvoiceFromOrder(order.getId(), null);
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);

        assertNotNull("invoice generated", invoice);
        assertEquals("There should have been only one Invoice Line, tax was $0", 1, invoice.getInvoiceLines().length);

        boolean foundTaxItem = false;
        boolean foundExemptItem = false;

        for (InvoiceLineDTO invoiceLine : invoice.getInvoiceLines()) {

            // purchased tax exempt long distance plan
            if (invoiceLine.getItemId().equals(exemptItem.getId())) {
                assertEquals("exempt item", "EXEMPT-DUMMY-ITEM", invoiceLine.getDescription());
                assertEquals("exempt item price $50", new BigDecimal("50"), invoiceLine.getAmountAsDecimal());
                foundExemptItem = true;
            }

            // tax, but no taxable items on order
            // value of tax should be $0
            if (invoiceLine.getItemId().equals(taxItem.getId())) {
                foundTaxItem = true;
            }
        }

        assertFalse("Should not find tax item when tax is zero.", foundTaxItem);
        assertTrue("found and validated exempt item", foundExemptItem);

        // cleanup
	    api.deleteInvoice(invoice.getId());
	    api.deleteOrder(order.getId());
	    api.deleteItem(taxItem.getId());
	    api.deleteItemCategory(taxCategory.getId());
	    api.deleteItem(exemptItem.getId());
	    api.deleteItemCategory(exemptCategory.getId());
        api.deleteUser(user.getUserId());
    }

	private Integer enableTaxPlugin(Integer itemId, Integer exemptCategoryId) {
		PluggableTaskWS plugin = new PluggableTaskWS();
		plugin.setTypeId(SIMPLE_TAX_PLUGIN_TYPE_ID);
		plugin.setProcessingOrder(4);

		// plug-in adds the given tax item to the invoice
		// when the customer purchase an item outside of the exempt category
		Hashtable<String, String> parameters = new Hashtable<String, String>();
		parameters.put("charge_carrying_item_id", itemId.toString());
		if(null != exemptCategoryId){
			parameters.put("item_exempt_category_id", exemptCategoryId.toString());
		}
		plugin.setParameters(parameters);

		return api.createPlugin(plugin);
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

	private ItemTypeWS buildTaxCategory() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("SimpleTax, Tax Item Type:" + System.currentTimeMillis());
		type.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_TAX);
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}

	private ItemTypeWS buildItemCategory() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("SimpleTax, Item Type:" + System.currentTimeMillis());
		type.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}
}
