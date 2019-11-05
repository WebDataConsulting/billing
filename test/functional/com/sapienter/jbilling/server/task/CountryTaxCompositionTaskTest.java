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
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

import org.testng.annotations.*;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;

/**
 * Test class to unit test the functionality of Tax applied to invoices that
 * belong to user from a configured country.
 *
 * @author Vikas Bodani
 * @since 29-Jul-2011
 *
 */
@Test(groups = { "integration", "task", "tax", "countrytax" })
public class CountryTaxCompositionTaskTest extends ApiTestCase {

    private static final Integer COUNTRY_TAX_PLUGIN_TYPE_ID = 90;

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
	public void afterTestMethod() {
		if (null != taxPluginId) {
			api.deletePlugin(taxPluginId);
			taxPluginId = null;
		}
	}

	@Test
    public void testCountryTaxAUPercentage() {
        System.out.println("#testCountryTaxAUPercentage");

	    //create tax category
	    ItemTypeWS taxCategory = buildTaxCategory();
	    taxCategory.setId(api.createItemCategory(taxCategory));

        // add a new tax item & enable the tax plug-in
	    ItemDTOEx taxItem = new ItemDTOEx();
	    taxItem.setCurrencyId(CURRENCY_USD);

	    taxItem.setDescription("AU Tax");
	    taxItem.setEntityId(TEST_ENTITY_ID);
	    taxItem.setNumber("TAX-AU");
	    taxItem.setTypes(new Integer[]{taxCategory.getId()});

        taxItem.setId(api.createItem(taxItem));
        assertNotNull("tax item created", taxItem.getId());

        taxPluginId = enableTaxPlugin(taxItem.getId());

	    //create item category
		ItemTypeWS itemCategory = buildItemCategory();
	    itemCategory.setId(api.createItemCategory(itemCategory));

		//create a dummy item to be used in the order
	    ItemDTOEx item = new ItemDTOEx();
	    item.setCurrencyId(CURRENCY_USD);
	    item.setPrice(new BigDecimal("3.5"));
	    item.setDescription("ITEM-AU-Tax");
	    item.setEntityId(TEST_ENTITY_ID);
	    item.setNumber("ITEM-AU");
	    item.setTypes(new Integer[]{itemCategory.getId()});
	    item.setId(api.createItem(item));

        // create a user for testing
        UserWS user = new UserWS();
        user.setUserName("country-tax-01-" + new Date().getTime());
        user.setPassword("Admin123@");
        user.setLanguageId(LANGUAGE_ID);
        user.setCurrencyId(CURRENCY_USD);
        user.setMainRoleId(ServerConstants.TYPE_CUSTOMER);
        user.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(user.getUserName() + "@gmail.com");
        metaField1.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Country Tax Test");
        metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue("Percentage Rate");
        metaField3.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.country.code");
        metaField4.setValue("AU");// country code set to Australia (AU)
        metaField4.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        user.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4
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
        assertEquals("two lines in invoice, item and the tax line", 2, invoice.getInvoiceLines().length);

        boolean foundTaxItem = false;
        boolean foundItem = false;

        for (InvoiceLineDTO invoiceLine : invoice.getInvoiceLines()) {

            // purchased item
            if (invoiceLine.getItemId().equals(item.getId())) {
                assertEquals("ITEM-AU-Tax", "ITEM-AU-Tax", invoiceLine.getDescription());
                assertEquals("item $35", new BigDecimal("35"), invoiceLine.getAmountAsDecimal());
                foundItem = true;
            }

            // tax, %10 of invoice total ($35 x 0.10 = $3.5)
            if (invoiceLine.getItemId().equals(taxItem.getId())) {
                assertEquals("tax item", "AU Tax", invoiceLine.getDescription());
                assertEquals("tax $3.5", new BigDecimal("3.5"), invoiceLine.getAmountAsDecimal());
                foundTaxItem = true;
            }
        }

        assertTrue("found and validated tax", foundTaxItem);
        assertTrue("found and validated item", foundItem);


        // cleanup
	    api.deleteInvoice(invoice.getId());
	    api.deleteOrder(order.getId());
	    api.deleteItem(taxItem.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(taxCategory.getId());
	    api.deleteItemCategory(itemCategory.getId());
        api.deleteUser(user.getUserId());
    }

    @Test
    public void testCountryTaxAUFlatFee() throws Exception {
        System.out.println("#testCountryTaxAUFlatFee");

	    //create tax category
	    ItemTypeWS taxCategory = buildTaxCategory();
	    taxCategory.setId(api.createItemCategory(taxCategory));

        // add a new tax item & enable the tax plug-in
        ItemDTOEx taxItem = new ItemDTOEx();
        taxItem.setCurrencyId(CURRENCY_USD);
	    taxItem.setPrice(BigDecimal.TEN);      // $10 flat fee
        taxItem.setDescription("AU Tax");
        taxItem.setEntityId(TEST_ENTITY_ID);
        taxItem.setNumber("TAX-AU");
        taxItem.setTypes(new Integer[]{taxCategory.getId()});

        taxItem.setId(api.createItem(taxItem));
        assertNotNull("tax item created", taxItem.getId());

        taxPluginId = enableTaxPlugin(taxItem.getId());

	    //create item category
	    ItemTypeWS itemCategory = buildItemCategory();
	    itemCategory.setId(api.createItemCategory(itemCategory));

	    //create a dummy item to be used in the order
	    ItemDTOEx item = new ItemDTOEx();
	    item.setCurrencyId(CURRENCY_USD);
	    item.setPrice(new BigDecimal("3.5"));
	    item.setDescription("ITEM-AU-Tax");
	    item.setEntityId(TEST_ENTITY_ID);
	    item.setNumber("ITEM-AU");
	    item.setTypes(new Integer[]{itemCategory.getId()});
	    item.setId(api.createItem(item));


        // create a user for testing
        UserWS user = new UserWS();
        user.setUserName("country-tax-02-" + new Date().getTime());
        user.setPassword("Admin123@");
        user.setLanguageId(LANGUAGE_ID);
        user.setCurrencyId(CURRENCY_USD);
        user.setMainRoleId(ServerConstants.TYPE_CUSTOMER);
        user.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(user.getUserName() + "@gmail.com");
        metaField1.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Country Tax Test");
        metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue("Flat Fee");
        metaField3.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.country.code");
        metaField4.setValue("AU");// country code set to Australia (AU)
        metaField4.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        user.setMetaFields(new MetaFieldValueWS[]{
		        metaField1,
		        metaField2,
		        metaField3,
		        metaField4
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
        assertEquals("two lines in invoice, dummy item and the tax line", 2, invoice.getInvoiceLines().length);

        boolean foundTaxItem = false;
        boolean foundItemItem = false;

        for (InvoiceLineDTO invoiceLine : invoice.getInvoiceLines()) {

            // purchased item
            if (invoiceLine.getItemId().equals(item.getId())) {
                assertEquals("item", "ITEM-AU-Tax", invoiceLine.getDescription());
                assertEquals("item $35", new BigDecimal("35"), invoiceLine.getAmountAsDecimal());
                foundItemItem = true;
            }

            // tax is a flat fee, not affected by the price of the invoice
            if (invoiceLine.getItemId().equals(taxItem.getId())) {
                assertEquals("tax item", "AU Tax", invoiceLine.getDescription());
                assertEquals("tax $10", new BigDecimal("10"), invoiceLine.getAmountAsDecimal());
                foundTaxItem = true;
            }
        }

        assertTrue("found and validated tax", foundTaxItem);
        assertTrue("found and validated item", foundItemItem);

        // cleanup

        api.deleteItem(taxItem.getId());
        api.deleteItemCategory(taxCategory.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemCategory.getId());
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(order.getId());
        api.deleteUser(user.getUserId());
    }

    @Test
    public void testCountryTaxNonAUCustomer() throws Exception {
        System.out.println("#testCountryTaxNonAUCustomer");

	    //create tax category
	    ItemTypeWS taxCategory = buildTaxCategory();
	    taxCategory.setId(api.createItemCategory(taxCategory));

        // add a new tax item & enable the tax plug-in
        ItemDTOEx taxItem = new ItemDTOEx();
        taxItem.setCurrencyId(CURRENCY_USD);

        taxItem.setDescription("AU Tax");
        taxItem.setEntityId(TEST_ENTITY_ID);
        taxItem.setNumber("TAX-AU");
        taxItem.setTypes(new Integer[]{taxCategory.getId()});

        taxItem.setId(api.createItem(taxItem));
        assertNotNull("tax item created", taxItem.getId());

	    taxPluginId = enableTaxPlugin(taxItem.getId());

	    //create item category
	    ItemTypeWS itemCategory = buildItemCategory();
	    itemCategory.setId(api.createItemCategory(itemCategory));

	    //create a dummy item to be used in the order
	    ItemDTOEx item = new ItemDTOEx();
	    item.setCurrencyId(CURRENCY_USD);
	    item.setPrice(new BigDecimal("3.5"));
	    item.setDescription("ITEM-AU-Tax");
	    item.setEntityId(TEST_ENTITY_ID);
	    item.setNumber("ITEM-AU");
	    item.setTypes(new Integer[]{itemCategory.getId()});
	    item.setId(api.createItem(item));

        // create a user for testing
        UserWS user = new UserWS();
        user.setUserName("country-tax-03-" + new Date().getTime());
        user.setPassword("Admin123@");
        user.setLanguageId(LANGUAGE_ID);
        user.setCurrencyId(CURRENCY_USD);
        user.setMainRoleId(ServerConstants.TYPE_CUSTOMER);
        user.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(user.getUserName() + "@gmail.com");
        metaField1.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Country Tax Test");
        metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue("Non-Australian");
        metaField3.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.country.code");
        metaField4.setValue("CA");// country NOT set to AU,
        metaField4.setGroupId(PRANCING_PONY_ACCOUNT_TYPE); // non-Australian customers shouldn't be taxed

        user.setMetaFields(new MetaFieldValueWS[]{
		        metaField1,
		        metaField2,
		        metaField3,
		        metaField4
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
        assertEquals("one lines in invoice, just item, no tax", 1, invoice.getInvoiceLines().length);

        InvoiceLineDTO invoiceLine = invoice.getInvoiceLines()[0];
        assertEquals("dummy item", item.getId(), invoiceLine.getItemId());
        assertEquals("dummy item", "ITEM-AU-Tax", invoiceLine.getDescription());
        assertEquals("item $35", new BigDecimal("35"), invoiceLine.getAmountAsDecimal());


        // cleanup
	    api.deleteInvoice(invoice.getId());
	    api.deleteOrder(order.getId());
	    api.deleteItem(taxItem.getId());
	    api.deleteItemCategory(taxCategory.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemCategory.getId());
	    api.deleteUser(user.getUserId());
    }

	private Integer enableTaxPlugin(Integer itemId) {
		PluggableTaskWS plugin = new PluggableTaskWS();
		plugin.setTypeId(COUNTRY_TAX_PLUGIN_TYPE_ID);
		plugin.setProcessingOrder(4);

		// plug-in adds the given tax item to the invoice
		// when the customers country code is Australia 'AU'
		Hashtable<String, String> parameters = new Hashtable<String, String>();
		parameters.put("charge_carrying_item_id", itemId.toString());
		parameters.put("tax_country_code", "AU");
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
		type.setDescription("CountryTax, Tax Item Type:" + System.currentTimeMillis());
		type.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_TAX);
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}

	private ItemTypeWS buildItemCategory() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("CountryTax, Item Type:" + System.currentTimeMillis());
		type.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}
}
