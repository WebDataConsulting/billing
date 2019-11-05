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

package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.*;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.api.SpringAPI;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateMidnight;
import org.testng.annotations.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.testng.AssertJUnit.*;

/**
 * @author Emil
 */
@Test(groups = { "web-services", "item" })
public class WSTest {

	private static final Integer PRANCING_PONY = Integer.valueOf(1);
	private static final Integer ENABLED = Integer.valueOf(1);
	private static final Integer DISABLED = Integer.valueOf(0);
	private static final Integer US_DOLLAR = Integer.valueOf(1);
	private static final Integer AU_DOLLAR = Integer.valueOf(11);
	private static final Integer ENGLISH_LANGUAGE = Integer.valueOf(1);
	private static final Integer FRENCH_LANGUAGE = Integer.valueOf(2);
	private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	private static Integer TEST_USER_ID;
	private static Integer TEST_ITEM_TYPE_ID;
	private static Integer TEST_ASSET_ITEM_TYPE_ID;
	private static Integer STATUS_DEFAULT_ID;
	private static Integer STATUS_AVAILABLE_ID;
	private static Integer STATUS_ORDER_SAVED_ID;
	private static Integer STATUS_RESERVED_ID;
	private static Integer TEST_ITEM_ID_WITH_ASSET_MANAGEMENT;
	protected static final Integer CURRENCY_USD = Integer.valueOf(1);
	private String ASSET_STATUS_AVAILABLE = "AVAILABLE";
	private String ASSET_STATUS_ORDERED = "ORDERED";
	private String ASSET_STATUS_DEFAULT = "DEFAULT";
	private String ASSET_STATUS_INTERNAL = "INTERNAL";
	
	private static JbillingAPI api;
	private static JbillingAPI childApi;


	@BeforeClass
	public void initializeTests() throws IOException, JbillingAPIException {
		if(null == api){
			api = JbillingAPIFactory.getAPI();
		}
		if(null == childApi){
			childApi = new SpringAPI(RemoteContext.Name.API_CHILD_CLIENT);
		}

		// Create And Persist User
		UserWS customer = null;
		try {
			customer = com.sapienter.jbilling.server.user.WSTest.createUser(true, true, null, US_DOLLAR, true);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error creating customer!!!");
		}

		TEST_USER_ID = customer.getUserId();

		// Create Item Type
		ItemTypeWS itemType = createItemType(false, false);
		// Persist
		TEST_ITEM_TYPE_ID = api.createItemCategory(itemType);

		// Create Asset Managed Item Type
		ItemTypeWS assetItemType = createItemType(true, false);
		TEST_ASSET_ITEM_TYPE_ID = api.createItemCategory(assetItemType);

		// Get Default Asset Status Id
		Integer[] statusesIds = getAssetStatusesIds(TEST_ASSET_ITEM_TYPE_ID);
		STATUS_DEFAULT_ID = statusesIds[0];
		STATUS_AVAILABLE_ID = statusesIds[1];
		STATUS_ORDER_SAVED_ID = statusesIds[2];
		STATUS_RESERVED_ID = statusesIds[3];
	}

	@AfterClass
	public void tearDown() throws Exception {

		if(null != TEST_ITEM_TYPE_ID){
			api.deleteItemCategory(TEST_ITEM_TYPE_ID);
			TEST_ITEM_TYPE_ID = null;
		}

		if(null != TEST_ASSET_ITEM_TYPE_ID){
			api.deleteItemCategory(TEST_ASSET_ITEM_TYPE_ID);
			TEST_ASSET_ITEM_TYPE_ID = null;
		}

		if(null != STATUS_DEFAULT_ID){
			STATUS_DEFAULT_ID = null;
		}
		if(null != STATUS_AVAILABLE_ID){
			STATUS_AVAILABLE_ID = null;
		}
		if(null != STATUS_ORDER_SAVED_ID){
			STATUS_ORDER_SAVED_ID = null;
		}
		if(null != STATUS_RESERVED_ID){
			STATUS_RESERVED_ID = null;
		}

		if(null != TEST_USER_ID){
			api.deleteUser(TEST_USER_ID);
			TEST_USER_ID = null;
		}

		if(null != api){
			api = null;
		}
		if(null != childApi){
			childApi = null;
		}
	}

	@BeforeMethod(groups = "asset")
	public void initializeAssetManagementTest() {

		// Create Asset Managed Item
		ItemDTOEx assetProduct = createItem(true, false, TEST_ASSET_ITEM_TYPE_ID);

		// Persist Item
		TEST_ITEM_ID_WITH_ASSET_MANAGEMENT = api.createItem(assetProduct);

	}

	@AfterMethod(groups = "asset")
	public void cleanupAssetManagementTest() {
		if(null != TEST_ITEM_ID_WITH_ASSET_MANAGEMENT){
			api.deleteItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
			TEST_ITEM_ID_WITH_ASSET_MANAGEMENT = null;
		}
	}

	@Test
	public void test001Create() {

		// Create new Item
		ItemDTOEx newItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		System.out.println("Creating item ..." + newItem);
		// Persist
		Integer itemId = api.createItem(newItem);
		assertNotNull("The item was not created", itemId);
		newItem.setId(itemId);

		// Check and compare
		ItemDTOEx persistedItem = api.getItem(itemId, TEST_USER_ID, null);
		assertNotNull(String.format("No items persisted for with: %d", itemId), persistedItem);

		// Check for match between manually created and persisted Items
		matchItems(newItem, persistedItem);

		// Clean up
		api.deleteItem(itemId);
	}

	@Test
	public void test002CreateMultipleDescriptions() {

		// Create And Persist Item with multiple descriptions
		ItemDTOEx newItem = createItemWithMultipleDescriptions(TEST_ITEM_TYPE_ID);

		// Get the persisted Item
		ItemDTOEx persistedItem = api.getItem(newItem.getId(), null, null);

		// Check for match between manually created and persisted Items
		matchItems(newItem, persistedItem);

		// Clean up
		api.deleteItem(persistedItem.getId());

	}

	@Test
	public void test003ModifyMultipleDescriptions() {

        /* Create And Persist Item with multiple descriptions */
		ItemDTOEx newItem = createItemWithMultipleDescriptions(TEST_ITEM_TYPE_ID);

		// test remove one description (english) and update
		newItem = api.getItem(newItem.getId(), null, null);
		List<InternationalDescriptionWS> descriptions = newItem.getDescriptions();
		assertNotNull(String.format("Item %d should have descriptions!!!", newItem.getId()), descriptions);
        assertEquals(String.format("There should be two descriptions for the %d Item!!", newItem.getId()), Integer.valueOf(2), Integer.valueOf(descriptions.size()));
        InternationalDescriptionWS englishDescription = descriptions.get(0);
		assertNotNull(String.format("Item %d should have english description!!!", newItem.getId()), englishDescription);

		// set english description as deleted
		englishDescription.setDeleted(true);
		api.updateItem(newItem);

		// Get the persisted Item and check the descriptions count
		newItem = api.getItem(newItem.getId(), null, null);
		descriptions = newItem.getDescriptions();
		assertNotNull(String.format("Item %d should have descriptions!!!", newItem.getId()), descriptions);
		assertEquals(String.format("There should be one description for the %d Item!!", newItem.getId()), Integer.valueOf(1), Integer.valueOf(descriptions.size()));

		// test modify content
		descriptions.get(0).setContent("newItemDescription-fr");
		api.updateItem(newItem);
		newItem = api.getItem(newItem.getId(), null, null);
		String frDescription = getDescription(newItem.getDescriptions(), FRENCH_LANGUAGE);
		assertEquals("newItemDescription-fr", frDescription);

		// Clean up
		api.deleteItem(newItem.getId());
	}

	private ItemDTOEx createItemWithMultipleDescriptions(Integer... types) {

		ItemDTOEx newItem = createItem(false, false, types);

		List<InternationalDescriptionWS> descriptions = new java.util.ArrayList<InternationalDescriptionWS>();
		InternationalDescriptionWS enDesc = new InternationalDescriptionWS(ENGLISH_LANGUAGE, "itemDescription-en");
		InternationalDescriptionWS frDesc = new InternationalDescriptionWS(FRENCH_LANGUAGE, "itemDescription-fr");
		descriptions.add(enDesc);
		descriptions.add(frDesc);

		newItem.setDescriptions(descriptions);

		System.out.println("Creating item ..." + newItem);
		Integer itemId = api.createItem(newItem);
		assertNotNull("The item was not created", itemId);
		System.out.println("Done!");
		newItem.setId(itemId);

		return newItem;
	}

	private String getDescription(List<InternationalDescriptionWS> descriptions, int langId) {
		for (InternationalDescriptionWS description : descriptions) {
			if (description.getLanguageId() == langId) {
				return description.getContent();
			}
		}
		return "";
	}

	@Test
	public void test006GetAllItems() {

		System.out.println("Getting all items");
		ItemDTOEx[] items =  api.getAllItems();
		assertNotNull("The items were not retrieved", items);
		int initialItemsCount = items.length;

		// First Item
		ItemDTOEx firstItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		Integer firstItemId = api.createItem(firstItem);
		firstItem.setId(firstItemId);

		// Second Item
		ItemDTOEx secondItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		Integer secondItemId = api.createItem(secondItem);
		secondItem.setId(secondItemId);

		// Get the current number of persisted Item entities
		items = api.getAllItems();
		assertNotNull("The items were not retrieved", items);
		int currentItemsCount = items.length;

		// Check of the initial number of persisted entities increased by two
		assertEquals("The number of persisted Item entities is not increasing!!!", Integer.valueOf(initialItemsCount + 2), Integer.valueOf(currentItemsCount));

		// Now check if the newest persisted entities are in the 'allItems array'
		int firstPersistedItemIndex = -1;
		int secondPersistedItemIndex = -1;
		ItemDTOEx[] allItems = api.getAllItems();
		for (int i = 0; i < allItems.length; i++){
			if(allItems[i].getId().equals(firstItemId)){
				firstPersistedItemIndex = i;
			}
			else if(allItems[i].getId().equals(secondItemId)){
				secondPersistedItemIndex = i;
			}
		}
		if(firstPersistedItemIndex > 0 && secondPersistedItemIndex > 0){
			matchItems(firstItem, allItems[firstPersistedItemIndex]);
			matchItems(secondItem, allItems[secondPersistedItemIndex]);
		}
		else {
			fail(String.format("The index of first persisted item is %d, the index of second persisted item is %d. " +
					"\nThey should be positive numbers if found in the persisted array!!", firstPersistedItemIndex, secondPersistedItemIndex));
		}

		// Clean up
		api.deleteItem(firstItemId);
		api.deleteItem(secondItemId);

	}

	@Test
	public void test007UpdateItem() {

		// Get the initial number of persisted items
		int initialItemsCount = api.getAllItems().length;

		// Create new Item
		ItemDTOEx newItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		System.out.println("Creating item ..." + newItem);

		// Persist Item
		Integer itemId = api.createItem(newItem);

		// Get the current number of persisted items
		int itemsCountAfterPersist = api.getAllItems().length;

		// Check if the initial count of items increased
		assertEquals("Initial count of items not increased!!", Integer.valueOf(initialItemsCount + 1), Integer.valueOf(itemsCountAfterPersist));

		// Get the persisted Item
		System.out.println("Getting item");
		ItemDTOEx item = api.getItem(itemId, TEST_USER_ID, null);
		System.out.println("After persist item: " + item);
		// Update some of the properties
		System.out.println("Changing properties");
		item.setDescription("Another description");
		item.setNumber("NMR-01");
		item.setPrices(CreateObjectUtil.setItemPrice(new BigDecimal("1.00"), new DateMidnight(1970, 1, 1).toDate(), PRANCING_PONY, Integer.valueOf(1)));

		// Persist changes
		System.out.println("Updating item");
		api.updateItem(item);

		// Get the current number of persisted items
		int itemsCountAfterUpdate = api.getAllItems().length;

		// Check if the current count of items remains the same as before update
		assertEquals("Initial count of items not increased!!", Integer.valueOf(itemsCountAfterPersist), Integer.valueOf(itemsCountAfterUpdate));

		// Get the updated item and compare it with the manually created one to verify changes
		ItemDTOEx itemChanged = api.getItem(itemId, TEST_USER_ID, null);
		System.out.println("After update item: " + itemChanged);
		matchItems(item, itemChanged);

		// Clean up
		api.deleteItem(itemId);
	}

	@Test
	public void test008CurrencyConvert() {

		// Create And Persist User who uses USD currency
		UserWS customer = null;
		try {
			customer = com.sapienter.jbilling.server.user.WSTest.createUser(true, true, null, US_DOLLAR, true);
		} catch (Exception e) {
			fail("Error creating customer!!!");
		}

		// Create Item with AUD currency
		ItemDTOEx item = createItem(false, false, TEST_ITEM_TYPE_ID);
		item.setPrices(CreateObjectUtil.setItemPrice(new BigDecimal("15.00"), new DateMidnight(1970, 1, 1).toDate(), PRANCING_PONY, AU_DOLLAR));

		// Persist Item
		Integer itemId = api.createItem(item);

		// Persisted item has price in AUD - fetch item using a USD customer
		item = api.getItem(itemId, customer.getId(), new PricingField[] {} );

		// price automatically converted to user currency when item is fetched
		assertEquals("Price in USD", 1, item.getCurrencyId().intValue());
        com.sapienter.jbilling.test.Asserts.assertEquals("Converted price AUD->USD", BigDecimal.TEN, item.getPriceAsDecimal());
		System.out.println("Item default price: " + item.getPriceAsDecimal());
		// verify that default item price is in AUD
		item = api.getItem(itemId);
        assertEquals("Default price in AUD", 11, item.getPrices().get(0).getCurrencyId().intValue());
        com.sapienter.jbilling.test.Asserts.assertEquals("Default price in AUD", new BigDecimal("15.00"), item.getPrices().get(0).getPriceAsDecimal());

		// Clean up
		api.deleteItem(itemId);
		api.deleteUser(customer.getId());
	}

	@Test
	public void test009GetAllItemCategories() {

		ItemTypeWS[] types = api.getAllItemCategories();
		assertNotNull("Some result should be received!!", types);

		// Get initial number of categories
		int initialCategoriesCount = types.length;

		// Create two new categories
		// First category
		// Create
		ItemTypeWS firstCategory = createItemType(false, false);
		// Persist
		Integer firstItemCategoryId = api.createItemCategory(firstCategory);
		firstCategory.setId(firstItemCategoryId);

		// Second category
		// Create
		ItemTypeWS secondCategory = createItemType(false, false);
		// Persist
		Integer secondItemCategoryId = api.createItemCategory(secondCategory);
		secondCategory.setId(secondItemCategoryId);

		// Check if the initial number of persisted item types increased by two
		types = api.getAllItemCategories();
		int categoriesCountAfterPersist = types.length;

        assertEquals("The initial number of persisted item types is not increasing!!", Integer.valueOf(initialCategoriesCount + 2), Integer.valueOf(categoriesCountAfterPersist));

		// Check if the returned array of item types contains previously persisted item types.
		int firstItemTypeIndex = -1;
		int secondItemTypeIndex = -1;

		for (int i = 0; i < categoriesCountAfterPersist; i++){
			if(types[i].getId().equals(firstItemCategoryId)){
				firstItemTypeIndex = i;
			}
			else if(types[i].getId().equals(secondItemCategoryId)){
				secondItemTypeIndex = i;
			}
		}

		if(firstItemTypeIndex > 0 && secondItemTypeIndex > 0){
			matchItemTypes(firstCategory, types[firstItemTypeIndex], false);
			matchItemTypes(secondCategory, types[secondItemTypeIndex], false);
		}
		else {
			fail(String.format("The index of first persisted category is %d, the index of second persisted category is %d. " +
					"\nThey should be positive numbers if found in the persisted array!!", firstItemTypeIndex, secondItemTypeIndex));
		}

		// Clean up
		api.deleteItemCategory(firstItemCategoryId);
		api.deleteItemCategory(secondItemCategoryId);

	}

	@Test
	public void test010CreateItemCategory() {

		String description = "Ice creams (WS test)";

		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription(description);
		itemType.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);

		System.out.println("Creating item category '" + description + "'...");
		Integer itemCategoryId = api.createItemCategory(itemType);
		assertNotNull(itemCategoryId);
		System.out.println("Done.");

		System.out.println("Getting all item categories...");
		ItemTypeWS[] types = api.getAllItemCategories();

		boolean addedFound = false;
		for (int i = 0; i < types.length; ++i) {
			if (description.equals(types[i].getDescription())) {
				System.out.println("Test category was found. Creation was completed successfully.");
				addedFound = true;
				break;
			}
		}
		assertTrue("Ice cream not found.", addedFound);

		//Test the creation of a category with the same description as another one.
		System.out.println("Going to create a category with the same description.");

		try {
			itemCategoryId = api.createItemCategory(itemType);
			fail("It should have thrown a SessionInternalError exception.");
		} catch (SessionInternalError sessionInternalError) {
			System.out.println("Exception caught. The category was not created because another one already existed with the same description.");
		} finally {
			// Clean up
			api.deleteItemCategory(itemCategoryId);
		}

		//Test the creation of a category with the same (case insensitive) description as another one.
		System.out.println("Going to create a category with the same description but ignoring differences between uppercase and lowercase letters.");
		// Create and save the original itemType in uppercase letters.
		ItemTypeWS itemTypeOriginal = new ItemTypeWS();
		itemTypeOriginal.setDescription("FROZEN FOOD (TEST)");
		itemTypeOriginal.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		Integer itemCategory2Id = api.createItemCategory(itemTypeOriginal);
		assertNotNull(itemCategory2Id);
		// Create the duplicate with the same description but in lowercase letters.
		ItemTypeWS itemTypeDuplicate = new ItemTypeWS();
		itemTypeDuplicate.setDescription("frozen food (TEST)");
		itemTypeDuplicate.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		try {
			api.createItemCategory(itemTypeDuplicate);
			fail("It should have thrown a SessionInternalError exception.");
		} catch (SessionInternalError sessionInternalError) {
			System.out.println("Exception caught. The category was not created because another one already existed with the same description, ignoring differences between lowercase and upercase letters.).");
		} finally {
			// Clean up
			api.deleteItemCategory(itemCategory2Id);
		}
	}

	@Test
	public void test011UpdateItemCategory() {

		String originalDescription;
		String description = "Updated Description";

		// Create new category
		ItemTypeWS itemCategory = createItemType(false, false);
		// Persist
		Integer itemCategoryId = api.createItemCategory(itemCategory);

		System.out.println("Getting all item categories...");
		ItemTypeWS[] types = api.getAllItemCategories();

		if(types.length <= 0){
			fail("No categories persisted!!!");
		}

		// Get recently persisted item type (Order by ID asc)
		itemCategory = types[types.length - 1];

		System.out.println("Changing description...");
		originalDescription = itemCategory.getDescription();
		itemCategory.setDescription(description);
		api.updateItemCategory(itemCategory);

		System.out.println("Getting all item categories...");
		types = api.getAllItemCategories();
		System.out.println("Verifying description has changed...");
		for (int i = 0; i < types.length; ++i) {
			if (itemCategoryId.equals(types[i].getId())) {
                assertEquals(description, types[i].getDescription());

				System.out.println("Restoring description...");
				types[i].setDescription(originalDescription);
				api.updateItemCategory(types[i]);
				break;
			}
		}

		// Create second category
		ItemTypeWS secondCategory = createItemType(false, false);
		// Persist
		Integer itemCategory2Id = api.createItemCategory(secondCategory);

		//Test the update of a category description to match one from another description.
		System.out.println("Getting all item categories...");
		types = api.getAllItemCategories();

		if(types.length <= 0){
			fail("No categories persisted!!!");
		}

		// Get recently persisted item type (Order by ID asc)
		secondCategory = types[types.length - 1];
		// use used description
		secondCategory.setDescription(originalDescription);

		try {
			api.updateItemCategory(secondCategory);
			fail("It should have thrown a SessionInternalError exception.");
		} catch (SessionInternalError sessionInternalError) {
			System.out.println("Exception caught. The category was not updated because another one already existed with the same description.");
		} finally {
			// Clean up
			api.deleteItemCategory(itemCategoryId);
			api.deleteItemCategory(itemCategory2Id);
		}

		//Test the update of a category description to match one from another description but changing a lowercase by an uppercase letter.
		System.out.println("Going to create a category with the same description but ignoring differences between uppercase and lowercase letters.");

		// Create and save the original itemType with uppercase letters.
		ItemTypeWS itemTypeOriginal = new ItemTypeWS();
		itemTypeOriginal.setDescription("FRUITS AND VEGETABLES (TEST)");
		itemTypeOriginal.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		Integer itemCategory3Id = api.createItemCategory(itemTypeOriginal);
		// Create a second itemType.
		ItemTypeWS itemTypeOther= new ItemTypeWS();
		itemTypeOther.setDescription("vegetables and fruits(TEST)");
		itemTypeOther.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		Integer itemCategory4Id = api.createItemCategory(itemTypeOther);
		// Change the description by the same as the original itemType but in lowercase letters.
		itemTypeOther.setDescription("fruits and vegetables (TEST)");
		try {
			api.updateItemCategory(itemTypeOther);
			fail("It should have thrown a SessionInternalError exception.");
		} catch (SessionInternalError sessionInternalError) {
			System.out.println("Exception caught. The category was not created because another one already existed with the same description, ignoring differences between lowercase and upercase letters.).");
		} finally {
			// Clean up
			api.deleteItemCategory(itemCategory3Id);
			api.deleteItemCategory(itemCategory4Id);
		}
	}

	@Test
	public void test012GetItemsByCategory() {

		// First Item
		ItemDTOEx firstItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		Integer firstItemId = api.createItem(firstItem);
		firstItem.setId(firstItemId);

		// Second Item
		ItemDTOEx secondItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		Integer secondItemId = api.createItem(secondItem);
		secondItem.setId(secondItemId);

		// Get the items for a category
		ItemDTOEx[] items = api.getItemByCategory(TEST_ITEM_TYPE_ID);
		assertNotNull("There should be items received!!", items);
        assertEquals(String.format("Number of items for %d category is not 2!!!", TEST_ITEM_TYPE_ID), 2, items.length);

		// Find and Match
		matchItems(secondItem, items[0]);
		matchItems(firstItem, items[1]);

		// Clean up
		api.deleteItem(firstItemId);
		api.deleteItem(secondItemId);
	}

	@Test( groups = { "web-services", "asset"})
	public void test013CreateAsset() throws Exception {

		// Create asset
		AssetWS asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);

		// Set Asset Identifier as empty and try to persist
		asset.setIdentifier("");
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsAnyError(error, Arrays.asList(new String[]
					{"AssetWS,identifier,validation.error.null.asset.identifier",
							"AssetWS,identifier,validation.error.size,1,200"}));
		}

		// Create Asset without status and try to persist
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, null);
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,assetStatusId,validation.error.null.asset.status");
		}

		// Create Asset without item and try to persist
		asset = getAssetWS(null, STATUS_DEFAULT_ID);
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,itemId,validation.error.null.item");
		}

		// Create Non asset managed item
		ItemDTOEx item = createItem(false, false, TEST_ITEM_TYPE_ID);
		Integer itemId = api.createItem(item);

		// Create asset of non asset managed product
		asset = getAssetWS(itemId, STATUS_DEFAULT_ID);
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,itemId,asset.validation.item.not.assetmanagement");
		}

		// Create asset with empty meta field value and try to persist
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		MetaFieldValueWS[] assetMetaFieldsValues = asset.getMetaFields();
		assertNotNull(String.format("There should be meta fields for %s!!!", asset.getIdentifier()), asset.getMetaFields());
        assertEquals("There should be one meta field value!!", Integer.valueOf(1), Integer.valueOf(assetMetaFieldsValues.length));

		// Set to empty
		asset.setMetaFields(new MetaFieldValueWS[0]);
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "MetaFieldValue,value,value.cannot.be.null,Regulatory Code");
		}

		// Create Valid Asset
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID);

		// Persist
		Integer assetId = api.createAsset(asset);

		// Get the persisted Asset
		AssetWS savedAsset = api.getAsset(assetId);
		JBillingTestUtils.assertPropertiesEqual(asset, savedAsset, new String[] {"id", "createDatetime", "status", "orderLineId", "metaFields", "provisioningCommands"});
        assertEquals(1, asset.getMetaFields().length);
        assertEquals(asset.getMetaFields()[0].getFieldName(), "Regulatory Code");
		assertTrue(asset.getMetaFields()[0].getListValueAsList().contains("01"));
		assertTrue(asset.getMetaFields()[0].getListValueAsList().contains("02"));

		// Try to create duplicate and persist
		try {
			api.createAsset(getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID));
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,identifier,asset.validation.duplicate.identifier");
		}

		// Clean up
		api.deleteAsset(assetId);
		api.deleteItem(itemId);
	}

	@Test(groups = { "web-services", "asset"})
	public void test014UpdateAsset() throws Exception {

		// Create Asset
		AssetWS asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID);
		// Persist Asset
		Integer assetId = api.createAsset(asset);

		// Get saved asset
		AssetWS savedAsset = api.getAsset(assetId);

		// Create Second Asset
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID);
		asset.setIdentifier("ID2");

		// Persist Second Asset
		Integer asset2Id = api.createAsset(asset);

		// Get second saved asset
		AssetWS savedAsset2 = api.getAsset(asset2Id);

		// Set the identifier as duplicate
		savedAsset2.setIdentifier(savedAsset.getIdentifier());
		try {
			api.updateAsset(savedAsset2);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,identifier,asset.validation.duplicate.identifier");
		}

		savedAsset2 = api.getAsset(asset2Id);
		savedAsset2.setAssetStatusId(STATUS_ORDER_SAVED_ID);
		try {
			api.updateAsset(savedAsset2);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,assetStatus,asset.validation.status.change.toordersaved");
		}

		// Clean up
		api.deleteAsset(asset2Id);
		api.deleteAsset(assetId);
	}


	@Test(groups = { "web-services", "asset"})
	public void test015createCategoryWithStatuses() throws Exception {

		ItemTypeWS type = createItemType(true, false);

		AssetStatusDTOEx status = new AssetStatusDTOEx();
		status.setDescription("One");
		status.setIsAvailable(DISABLED);
		status.setIsDefault(ENABLED);
		status.setIsOrderSaved(DISABLED);
		type.getAssetStatuses().add(status);

		try {
			api.createItemCategory(type);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemTypeWS,statuses,validation.error.category.status.default.one");
		}

		status.setIsDefault(DISABLED);
		status.setIsOrderSaved(ENABLED);
		try {
			api.createItemCategory(type);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemTypeWS,statuses,validation.error.category.status.order.saved.one");
		}
	}


	@Test(groups = { "web-services", "asset"})
	public void test016UpdateItemWithAsset() {

		// Create Second Asset Managed Category
		ItemTypeWS assetCategory2 = createItemType(true, false);
		// Persist
		Integer assetCategory2Id = api.createItemCategory(assetCategory2);

		// Create Asset Managed Items
		ItemDTOEx assetItem = createItem(true, false, TEST_ASSET_ITEM_TYPE_ID);
		// Persist
		Integer assetItemId = api.createItem(assetItem);

		// Get item an update item types
		ItemDTOEx item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[] {TEST_ASSET_ITEM_TYPE_ID, assetCategory2Id});

		try {
			api.updateItem(item);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemDTOEx,types,product.validation.multiple.assetmanagement.types.error");
		}

		System.out.println("#test016UpdateItemWithAsset. Type without asset management");
		item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[] {TEST_ITEM_TYPE_ID});

		try {
			api.updateItem(item);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemDTOEx,types,product.validation.no.assetmanagement.type.error");
		}

		System.out.println("#test016UpdateItemWithAsset. Change asset management type");
		item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[] {assetCategory2Id});
		api.updateItem(item);

		AssetWS asset = getAssetWS(assetItemId, STATUS_AVAILABLE_ID);
		Integer assetId = api.createAsset(asset);

		item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[] {TEST_ASSET_ITEM_TYPE_ID});
		try {
			api.updateItem(item);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemDTOEx,types,product.validation.assetmanagement.changed.error");
		}

		System.out.println("#test016UpdateItemWithAsset. No asset manager");
		item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[0]);
		try {
			api.updateItem(item);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemDTOEx,types,validation.error.missing.type");
		}

		// Clean up
		api.deleteAsset(assetId);
		api.deleteItem(assetItemId);
		api.deleteItemCategory(assetCategory2Id);
	}

	@Test(groups = { "web-services", "asset"})
	public void test017UpdateItemTypeAssetManagement() {

		ItemTypeWS assetCategory = null;
		ItemTypeWS[] typeWSs = api.getAllItemCategories();
		for(ItemTypeWS itemTypeWS : typeWSs) {
			if(itemTypeWS.getId().intValue() == TEST_ASSET_ITEM_TYPE_ID) {
				assetCategory = itemTypeWS;
				break;
			}
		}

		if(null == assetCategory){
			fail(String.format("Can not find persisted test asset category %d", TEST_ASSET_ITEM_TYPE_ID));
		}

		assetCategory.setAllowAssetManagement(DISABLED);
		System.out.println("#test017UpdateItemTypeAssetManagement. Can not change type's asset man enabled as product has asset management enabled");
		try {
			api.updateItemCategory(assetCategory);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemTypeWS,allowAssetManagement,product.category.validation.product.assetmanagement.enabled");
		}

		// Create Category WO asset management
		ItemTypeWS categoryWOAssetManagement = createItemType(false, false);
		// Persist
		Integer categoryWOAssetManagementId = api.createItemCategory(categoryWOAssetManagement);

		// Create Asset Managed Items
		ItemDTOEx assetItem = createItem(true, false, TEST_ASSET_ITEM_TYPE_ID);
		// Persist
		Integer assetItemId = api.createItem(assetItem);

		// Create Asset
		AssetWS asset = getAssetWS(assetItemId, STATUS_AVAILABLE_ID);
		Integer assetId = api.createAsset(asset);

		ItemDTOEx item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[] {TEST_ASSET_ITEM_TYPE_ID, categoryWOAssetManagementId});
		api.updateItem(item);

		typeWSs = api.getAllItemCategories();
		for(ItemTypeWS itemTypeWS : typeWSs) {
			if(itemTypeWS.getId().intValue() == categoryWOAssetManagementId) {
				categoryWOAssetManagement = itemTypeWS;
				break;
			}
		}

		categoryWOAssetManagement.setAllowAssetManagement(ENABLED);
		addAssetStatuses(categoryWOAssetManagement);

		System.out.println("#test017UpdateItemTypeAssetManagement. Product will have 2 categories with asset management");
		try {
			api.updateItemCategory(categoryWOAssetManagement);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemTypeWS,allowAssetManagement,product.category.validation.multiple.linked.assetmanagement.types.error");
		}

		// Clean up
		api.deleteAsset(assetId);
		api.deleteItem(assetItemId);
		api.deleteItemCategory(categoryWOAssetManagementId);
	}

	@Test(groups = { "web-services", "asset"})
	public void test018GetAssetsForCategory() {

		Integer[] ids = api.getAssetsForCategory(TEST_ASSET_ITEM_TYPE_ID);
        assertEquals("Ids: "+Arrays.asList(ids), 0, ids.length);

		ids = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
        assertEquals("Ids: "+Arrays.asList(ids), 0, ids.length);

		// Create Asset
		AssetWS asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID);
		Integer assetId = api.createAsset(asset);

		ids = api.getAssetsForCategory(TEST_ASSET_ITEM_TYPE_ID);
        assertEquals("Ids: "+Arrays.asList(ids), 1, ids.length);
        assertEquals("Ids: " +Arrays.asList(ids), assetId, ids[0]);

		ids = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
        assertEquals("Ids: "+Arrays.asList(ids), 1, ids.length);
        assertEquals("Ids: " +Arrays.asList(ids), assetId, ids[0]);

		// Create second asset for asset product
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID);
		asset.setIdentifier("Modified");
		Integer secondAssetId = api.createAsset(asset);

		List idList = Arrays.asList(api.getAssetsForCategory(TEST_ASSET_ITEM_TYPE_ID));
        assertEquals("Ids: " + idList, 2, idList.size());
        assertTrue("Ids: " + idList, idList.contains(assetId));
		assertTrue("Ids: "+idList,idList.contains(secondAssetId));

		idList = Arrays.asList(api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT));
		assertEquals(2, idList.size());
		assertTrue("Ids: " + idList, idList.contains(assetId));
		assertTrue("Ids: " + idList, idList.contains(secondAssetId));

		// Clean up
		api.deleteAsset(assetId);
		api.deleteAsset(secondAssetId);

		ids = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
		assertEquals("Ids: " + Arrays.asList(ids), 0, ids.length);

		ids = api.getAssetsForCategory(TEST_ASSET_ITEM_TYPE_ID);
		assertEquals("Ids: "+Arrays.asList(ids), 0, ids.length);
	}

	@Test(groups = { "web-services", "asset"})
	public void test019AssetTransition() {

		AssetWS asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		Integer assetId = api.createAsset(asset);

		AssetWS savedAsset = api.getAsset(assetId);
		AssetTransitionDTOEx[] transitions = api.getAssetTransitions(assetId);
		assertEquals(1, transitions.length);
		assertEquals(STATUS_DEFAULT_ID, transitions[0].getNewStatusId());
		assertNotNull(transitions[0].getCreateDatetime());
		assertNotNull(transitions[0].getUserId());
		assertNull(transitions[0].getPreviousStatusId());
		assertNull(transitions[0].getAssignedToId());

		savedAsset.setIdentifier("test019AssetTransition");
		api.updateAsset(savedAsset);
		savedAsset = api.getAsset(assetId);

		transitions = api.getAssetTransitions(assetId);
		assertEquals(1, transitions.length);
		assertEquals(STATUS_DEFAULT_ID, transitions[0].getNewStatusId());

		savedAsset.setAssetStatusId(STATUS_AVAILABLE_ID);
		api.updateAsset(savedAsset);
		transitions = api.getAssetTransitions(assetId);
		assertEquals(2, transitions.length);
		int cnt = 0;
		for(AssetTransitionDTOEx ex: transitions) {
			if(ex.getNewStatusId().equals(STATUS_DEFAULT_ID)) {
				cnt += 1;
			} else if(ex.getNewStatusId().equals(STATUS_AVAILABLE_ID)) {
				cnt += 10;
				assertEquals(STATUS_DEFAULT_ID, ex.getPreviousStatusId());
				assertNotNull(ex.getCreateDatetime());
				assertNotNull(ex.getUserId());
				assertNull(ex.getAssignedToId());
			}
		}
		assertEquals("Not all statuses found ["+cnt+"]", 11, cnt);

		// Clean up
		api.deleteAsset(assetId);
	}

	@Test(groups = { "web-services", "asset"}  )
	public void test020BatchUpload() throws Exception {

		File sourceFile = File.createTempFile("testAsset", ".csv");
		writeToFile(sourceFile,
				"identifier,notes,INT1,Regulatory Code\n" +
						"Id1,Note1,1,01\",\"02\n" +
						"Id2,Note2,,01\n" +
						"Id3,Note3,,05\n"
		);
		File errorFile = File.createTempFile("testAssetError", ".csv");

		System.out.println("Source file: "+sourceFile);
		System.out.println("Error file: "+errorFile);
		api.startImportAssetJob(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, "identifier", "notes", "Global", "Entities", sourceFile.getAbsolutePath(), errorFile.getAbsolutePath());
		Thread.sleep(2000);

		String errors = FileUtils.readFileToString(errorFile);
		assertEquals("Errors was: "+errors, "", errors);

		Integer[] assetIds = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
		int assetsFoundCount = 0;
		for(Integer assetId : assetIds) {
			AssetWS asset = api.getAsset(assetId);
			if("Id1".equals(asset.getIdentifier())) {
				assetsFoundCount += 1;
				assertEquals("Note1", asset.getNotes());
				int mfCnt = 0;
				for(MetaFieldValueWS fieldWS : asset.getMetaFields()) {
					if("INT1".equals(fieldWS.getFieldName())) {
						mfCnt += 1;
						assertEquals(1, (int)fieldWS.getIntegerValue());
					} else if("Regulatory Code".equals(fieldWS.getFieldName())) {
						mfCnt += 10;
						assertTrue(fieldWS.getListValueAsList().contains("01"));
						assertTrue(fieldWS.getListValueAsList().contains("02"));
					}
				}
				assertEquals("Not all metafields found ", 11, mfCnt);
			} else if("Id2".equals(asset.getIdentifier())) {
				assetsFoundCount += 10;
				assertEquals("Note2", asset.getNotes());
				int mfCnt = 0;
				for(MetaFieldValueWS fieldWS : asset.getMetaFields()) {
					if("INT1".equals(fieldWS.getFieldName())) {
						mfCnt += 1;
						assertEquals(5, (int)fieldWS.getIntegerValue());
					} else if("Regulatory Code".equals(fieldWS.getFieldName())) {
						mfCnt += 10;
						assertTrue(fieldWS.getListValueAsList().contains("01"));
					}
				}
				assertEquals("Not all metafields found ", 11, mfCnt);
			} else if("Id3".equals(asset.getIdentifier())) {
				assetsFoundCount += 100;
				assertEquals("Note3", asset.getNotes());
				int mfCnt = 0;
				for(MetaFieldValueWS fieldWS : asset.getMetaFields()) {
					if("INT1".equals(fieldWS.getFieldName())) {
						mfCnt += 1;
						assertEquals(5, (int)fieldWS.getIntegerValue());
					} else if("Regulatory Code".equals(fieldWS.getFieldName())) {
						mfCnt += 10;
						assertTrue(fieldWS.getListValueAsList().contains("05"));
					}
				}
				assertEquals("Not all metafields found ", 11, mfCnt);
			}
		}
		assertEquals("Assets found "+assetsFoundCount, 111, assetsFoundCount);

		writeToFile(sourceFile, "identifier,notes,Regulatory Code\n" +
				",Note,01\n" +
				"Id4,Note4,01\n" +
				"Id4,Note41,01\n"
		);

		api.startImportAssetJob(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, "identifier","notes", "Global", "Entities", sourceFile.getAbsolutePath(), errorFile.getAbsolutePath());
		Thread.sleep(2000);

		assetIds = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
		assetsFoundCount = 0;
		for(Integer assetId : assetIds) {
			AssetWS asset = api.getAsset(assetId);
			if("Id4".equals(asset.getIdentifier())) {
				assetsFoundCount += 1;
				assertEquals("Note4", asset.getNotes());
			} else if("Id1".equals(asset.getIdentifier())) {
				assertEquals("Note1", asset.getNotes());
			}
			if(asset.getIdentifier().startsWith("Id") && asset.getIdentifier().length() == 3) {
				api.deleteAsset(assetId);
			}
		}
		assertEquals("Assets found "+assetsFoundCount, 1, assetsFoundCount);

		errors = FileUtils.readFileToString(errorFile);
		System.out.println(errors);
		assertTrue(errors.contains("Id4,Note41,01,An asset with the identifier already exists"));
		assertTrue(errors.contains(",Note,01,The identifier must be between 1 and 200 characters long"));

		// Clean up
		FileUtils.deleteQuietly(errorFile);
		FileUtils.deleteQuietly(sourceFile);
	}

	@Test(groups = { "web-services", "asset"})
	public void test021UpdateItemTypeAssetManagementMetaFields() throws Exception {

		// Create Asset Managed Category
		ItemTypeWS assetCategory = createItemType(true, false);
		// Persist
		Integer assetCategoryId = api.createItemCategory(assetCategory);

		// Statuses
		Integer[] statusesIds = getAssetStatusesIds(assetCategoryId);
		Integer statusDefaultId = statusesIds[0];

		ItemTypeWS[] typeWSs = api.getAllItemCategories();
		for(ItemTypeWS itemTypeWS : typeWSs) {
			if(itemTypeWS.getId().intValue() == assetCategoryId) {
				assetCategory = itemTypeWS;
				break;
			}
		}

		MetaFieldWS mf = new MetaFieldWS();
		BeanUtils.copyProperties(mf, assetCategory.getAssetMetaFields().iterator().next());
		mf.setName("Regulatory Code");
		mf.setId(0);

		assetCategory.getAssetMetaFields().add(mf);

		System.out.println("#test021UpdateItemTypeAssetManagementMetaFields. Two MetaFields with same name");
		try {
			api.updateItemCategory(assetCategory);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "MetaFieldWS,name,metaField.validation.name.unique,Regulatory Code");
		}

		// Create Asset Managed Item
		ItemDTOEx item = createItem(true, false, assetCategoryId);
		Integer itemId = api.createItem(item);

		AssetWS asset = getAssetWS(itemId, statusDefaultId);
		Integer assetId = api.createAsset(asset);

		AssetWS savedAsset = api.getAsset(assetId);

		typeWSs = api.getAllItemCategories();
		for(ItemTypeWS itemTypeWS : typeWSs) {
			if(itemTypeWS.getId().intValue() == assetCategoryId) {
				assetCategory = itemTypeWS;
				break;
			}
		}

		String name = null;
		for(MetaFieldWS metaField: assetCategory.getAssetMetaFields()) {
			if(!metaField.getDataType().equals(DataType.STRING)) {
				metaField.setDataType(DataType.STRING);
				if(metaField.getDefaultValue() != null) {
					metaField.getDefaultValue().setDataType(DataType.STRING);
					metaField.getDefaultValue().setValue("str");
				}
				name = metaField.getName();
				break;
			}
		}

		System.out.println("#test021UpdateItemTypeAssetManagementMetaFields. Can not change data type if meta field is in use");
		try {
			api.updateItemCategory(assetCategory);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "MetaFieldWS,dataType,metaField.validation.type.change.not.allowed,"+name);
		}


		typeWSs = api.getAllItemCategories();
		for(ItemTypeWS itemTypeWS : typeWSs) {
			if(itemTypeWS.getId().intValue() == assetCategoryId) {
				assetCategory = itemTypeWS;
				break;
			}
		}

		assertEquals(2, assetCategory.getAssetMetaFields().size());

		System.out.println("#test021UpdateItemTypeAssetManagementMetaFields. Remove meta fields");
		assetCategory.setAssetMetaFields(new HashSet(0));
		try {
			api.updateItemCategory(assetCategory);

		} catch (SessionInternalError error) {
			error.printStackTrace();
			fail(error.getMessage());
		}

		savedAsset = api.getAsset(assetId);

		assertEquals(0, savedAsset.getMetaFields().length);

		// Clean up
		api.deleteAsset(assetId);
		api.deleteItem(itemId);
		api.deleteItemCategory(assetCategoryId);
	}

	@Test
	public void test022CreateItemWithOrderLineMetaFields() {

		// Create Item
		ItemDTOEx item = createItem(false, false, TEST_ITEM_TYPE_ID);

		// Add MetaField Order Line
		MetaFieldWS metaField = new MetaFieldWS();
		metaField.setDataType(DataType.STRING);
		metaField.setDisabled(false);
		metaField.setDisplayOrder(1);
		metaField.setEntityId(PRANCING_PONY);
		metaField.setEntityType(EntityType.ORDER_LINE);
		metaField.setMandatory(false);
		metaField.setPrimary(false);
		metaField.setName("Item WS-022 orderLinesMetaField_1");
		item.setOrderLineMetaFields(new MetaFieldWS[]{metaField});

		System.out.println("Creating item ..." + item);
		// Persist
		Integer itemId = api.createItem(item);
		assertNotNull("The item was not created", itemId);

		ItemDTOEx itemDtoEx = api.getItem(itemId, TEST_USER_ID, null);
		assertNotNull("Item orderLineMetaFields not fount (empty)", itemDtoEx.getOrderLineMetaFields());
		assertEquals("Item orderLineMetaFields size is incorrect", 1, itemDtoEx.getOrderLineMetaFields().length);
		assertEquals("Item orderLineMetaField is incorrect", metaField.getName(), itemDtoEx.getOrderLineMetaFields()[0].getName());

		itemDtoEx.getOrderLineMetaFields()[0].setName("Item WS-022 metaFieldChangedName");
		api.updateItem(itemDtoEx);

		itemDtoEx = api.getItem(itemId, TEST_USER_ID, null);
		assertNotNull("Item orderLineMetaFields not fount (empty)", itemDtoEx.getOrderLineMetaFields());
		assertEquals("Item orderLineMetaFields size is incorrect", 1, itemDtoEx.getOrderLineMetaFields().length);
		assertEquals("Item orderLineMetaField is incorrect", "Item WS-022 metaFieldChangedName", itemDtoEx.getOrderLineMetaFields()[0].getName());

		itemDtoEx.setOrderLineMetaFields(new MetaFieldWS[]{});

		api.updateItem(itemDtoEx);
		itemDtoEx = api.getItem(itemId, TEST_USER_ID, null);
		assertTrue("Item orderLineMetaFields should be empty", itemDtoEx.getOrderLineMetaFields() == null || itemDtoEx.getOrderLineMetaFields().length == 0);

		// Clean up
		api.deleteItem(itemId);
	}

	@Test(groups = {"web-services", "asset"})
	public void test023findItemsByMetaFields() {

		// Create Two Assets
		AssetWS firstAsset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		Integer firstAssetId = api.createAsset(firstAsset);

		AssetWS secondAsset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		secondAsset.setIdentifier("ASSET2");
		secondAsset.getMetaFields()[0].setListValue(new String[]{"03"});
		Integer secondAssetId = api.createAsset(secondAsset);

		System.out.println("id EQ");
		SearchCriteria criteria = new SearchCriteria();
		criteria.setFilters(new BasicFilter[]{ new BasicFilter("id", Filter.FilterConstraint.EQ, firstAssetId)});
		AssetSearchResult result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assertEquals(1, result.getObjects().length);

		System.out.println("id GE && ID LT");
		criteria.setFilters(new BasicFilter[]{ new BasicFilter("id", Filter.FilterConstraint.GE, firstAssetId),
				new BasicFilter("id", Filter.FilterConstraint.LT, secondAssetId + 1)});
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		AssetWS[] assets = result.getObjects();
		assertEquals(2, assets.length);
		for(AssetWS asset: assets) {
			assertTrue(asset.getId() >= firstAssetId && asset.getId() < secondAssetId + 1);
		}

		System.out.println("Regulatory Code EQ");
		criteria.setFilters(new BasicFilter[]{ new BasicFilter("Regulatory Code", Filter.FilterConstraint.EQ, "01")});
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assertEquals(1, result.getObjects().length);
		assertEquals(firstAssetId, result.getObjects()[0].getId());

		System.out.println("Regulatory Code EQ");
		criteria.setFilters(new BasicFilter[]{ new BasicFilter("Regulatory Code", Filter.FilterConstraint.EQ, "03")});
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assertEquals(1, result.getObjects().length);
		assertEquals(secondAssetId, result.getObjects()[0].getId());

		System.out.println("identifier ILIKE");
		criteria.setFilters(new BasicFilter[]{ new BasicFilter("identifier", Filter.FilterConstraint.LIKE, "ASSET")});
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assets = result.getObjects();
		assertEquals(2, assets.length);
		for(AssetWS asset: assets) {
			assertTrue(asset.getIdentifier().toLowerCase().indexOf("asset")>=0);
		}

		System.out.println("max");
		criteria.setMax(1);
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assets = result.getObjects();
		assertEquals(2, result.getTotal());
		assertEquals(1, assets.length);
		for(AssetWS asset: assets) {
			assertTrue(asset.getIdentifier().toLowerCase().indexOf("asset")>=0);
		}

		System.out.println("sort asc");
		criteria.setFilters(new BasicFilter[]{ new BasicFilter("identifier", Filter.FilterConstraint.LIKE, "ASSET")});
		criteria.setMax(0);
		criteria.setSort("identifier");
		criteria.setDirection(SearchCriteria.SortDirection.ASC);
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assets = result.getObjects();
		assertEquals("ASSET1", assets[0].getIdentifier());
		assertEquals("ASSET2", assets[1].getIdentifier());

		System.out.println("sort desc");
		criteria.setDirection(SearchCriteria.SortDirection.DESC);
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assets = result.getObjects();
		assertEquals("ASSET2", assets[0].getIdentifier());
		assertEquals("ASSET1", assets[1].getIdentifier());

		// Clean up
		api.deleteAsset(firstAssetId);
		api.deleteAsset(secondAssetId);
	}

	@Test
	public void test024DeleteItemTypeWithStatuses() {
		System.out.println("#test024DeleteItemTypeWithStatuses");

		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("TmpAsstMgmgt4");
		type.setAssetIdentifierLabel("Lbl3");
		type.setOrderLineTypeId(1);
		type.setAllowAssetManagement(1);
		addAssetStatuses(type);
		int id = api.createItemCategory(type);
		api.deleteItemCategory(id);
	}


	@Test(groups = { "web-services", "asset"}  )
	public void test025AssetGroupBatchUpload() throws Exception {

		File sourceFile = File.createTempFile("testAsset", ".csv");
		writeToFile(sourceFile,
				"identifier,notes,INT1,Regulatory Code\n" +
						"Id11,Note1,1,01\",\"02\n" +
						"Id12,Note2,,01\n" +
						"Id13,Note3,,05\n"
		);
		File errorFile = File.createTempFile("testAssetError", ".csv");

		System.out.println("Source file: "+sourceFile);
		System.out.println("Error file: "+errorFile);
		api.startImportAssetJob(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, "identifier", "notes", "Global", "Entities", sourceFile.getAbsolutePath(), errorFile.getAbsolutePath());
		Thread.sleep(2000);

		String errors = FileUtils.readFileToString(errorFile);
		System.out.println(errors);

		System.out.println("Initial assets uploaded");
		writeToFile(sourceFile,
				"identifier,Regulatory Code,Asset1,AssetProduct1,Asset3,AssetProduct3\n" +
						"Id14,01,Id11,"+TEST_ITEM_ID_WITH_ASSET_MANAGEMENT+",Id12,"+TEST_ITEM_ID_WITH_ASSET_MANAGEMENT+"\n" +
						"Id15,01,Id13,"+TEST_ITEM_ID_WITH_ASSET_MANAGEMENT+",Id12,"+TEST_ITEM_ID_WITH_ASSET_MANAGEMENT+"\n" +
						"Id16,01,Id13,"+TEST_ITEM_ID_WITH_ASSET_MANAGEMENT+",,\n"
		);

		api.startImportAssetJob(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, "identifier", "notes", "Global", "Entities", sourceFile.getAbsolutePath(), errorFile.getAbsolutePath());
		//Time interval should much enough for mysql when run test cases in group
		Thread.sleep(8000);

		errors = FileUtils.readFileToString(errorFile);
		System.out.println(errors);
		assertTrue("Errors contained: "+errors, errors.contains("The asset [Id12] is already part of an asset group"));


		Integer[] assetIds = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
		List assetsToDelete = Arrays.asList(new String[] {"Id14","Id16"});
		for(Integer assetId : assetIds) {
			AssetWS asset = api.getAsset(assetId);
			if(assetsToDelete.contains(asset.getIdentifier())) {
				api.deleteAsset(assetId);
			}
		}
		assetsToDelete = Arrays.asList(new String[] {"Id11","Id12","Id13"});
		for(Integer assetId : assetIds) {
			AssetWS asset = api.getAsset(assetId);
			if(assetsToDelete.contains(asset.getIdentifier())) {
				api.deleteAsset(assetId);
			}
		}
		// Clean up
		FileUtils.deleteQuietly(errorFile);
		FileUtils.deleteQuietly(sourceFile);
	}


	@Test(groups = { "web-services", "asset"} )
	public void test026AssetGroupCreate() {

		AssetWS asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("GID1");
		Integer assetId = api.createAsset(asset);
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("GID2");
		Integer assetId2 = api.createAsset(asset);
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("GID3");
		Integer assetId3 = api.createAsset(asset);

		asset=getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("Group1");
		asset.setContainedAssetIds(new Integer[] {assetId, assetId2});
		Integer groupId = api.createAsset(asset);

		System.out.println("Create Asset Group");
		asset = api.getAsset(groupId);
		List assetIds = Arrays.asList(asset.getContainedAssetIds());
		assertTrue("Contained assets doesn not include id "+ assetId +" "+assetIds, assetIds.contains(assetId));
		assertTrue("Contained assets doesn not include id "+ assetId2 +" "+assetIds, assetIds.contains(assetId2));

		asset = api.getAsset(assetId);
		assertEquals(ServerConstants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());
		asset = api.getAsset(assetId2);
		assertEquals(ServerConstants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());

		System.out.println("Assign asset to group which already belongs to another group");
		asset=getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("Group2");
		asset.setContainedAssetIds(new Integer[] {assetId});
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {          //The asset [ASSET1] is already part of an asset group.
			JBillingTestUtils.assertContainsError(error,  "AssetWS,containedAssets,asset.validation.group.linked,GID1");
		}

		System.out.println("Change status of asset belonging to a group");
		asset = api.getAsset(assetId);
		asset.setAssetStatusId(STATUS_AVAILABLE_ID);
		try {
			api.updateAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,assetStatus,asset.validation.status.change.internal");
		}

		System.out.println("Remove Asset from Group");
		asset = api.getAsset(groupId);
		asset.setContainedAssetIds(new Integer[] {assetId, assetId3});
		api.updateAsset(asset);

		asset = api.getAsset(assetId2);
		//it must have the default status
		assertEquals(STATUS_DEFAULT_ID, asset.getAssetStatusId());
		asset = api.getAsset(assetId3);
		assertEquals(ServerConstants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());

		System.out.println("Add group to group");
		asset=getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("Group2");
		asset.setContainedAssetIds(new Integer[] {groupId});
		Integer groupId2 = api.createAsset(asset);

		asset = api.getAsset(assetId3);
		assertEquals(ServerConstants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());
		asset = api.getAsset(assetId);
		assertEquals(ServerConstants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());
		asset = api.getAsset(groupId);
		assertEquals(ServerConstants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());

		System.out.println("Delete group");
		api.deleteAsset(groupId2);
		asset = api.getAsset(assetId3);
		assertEquals(ServerConstants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());
		asset = api.getAsset(assetId);
		assertEquals(ServerConstants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());
		asset = api.getAsset(groupId);
		assertEquals(STATUS_DEFAULT_ID, asset.getAssetStatusId());

		System.out.println("Delete member of group");
		try {
			api.deleteAsset(assetId);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,assetStatus,asset.validation.status.change.internal");
		}

		System.out.println("Delete group 2");
		api.deleteAsset(groupId);
		asset = api.getAsset(assetId3);
		assertEquals(STATUS_DEFAULT_ID, asset.getAssetStatusId());
		asset = api.getAsset(assetId);
		assertEquals(STATUS_DEFAULT_ID, asset.getAssetStatusId());

		api.deleteAsset(assetId3);
		api.deleteAsset(assetId2);
		api.deleteAsset(assetId);
	}

	@Test
	public void test023GetChildProductCategoryAndProduct() {

		// get original items
		Integer totalItemTypes = api.getAllItemCategoriesByEntityId(PRANCING_PONY).length;
		Integer totalItems = api.getAllItemsByEntityId(PRANCING_PONY).length;

		//Create an item type for child entity
		long rand = System.currentTimeMillis();
		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription("Root Category"+rand);
		itemType.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		Integer itemTypeId = childApi.createItemCategory(itemType);

		//Create a item for child entity
		ItemDTOEx newItem = new ItemDTOEx();
		newItem.setDescription("A reseller item" + rand);
		newItem.setPriceModelCompanyId(new Integer(1));
		newItem.setPrices(CreateObjectUtil.setItemPrice(new BigDecimal("30.00"), new DateMidnight(1970, 1, 1).toDate(), PRANCING_PONY, Integer.valueOf(1)));
		newItem.setNumber("RP-1");

		Set<Integer> childEntities= new HashSet<Integer>(1);
		childEntities.add(childApi.getCallerCompanyId());
		newItem.setEntities(childEntities);

		Integer types[] = new Integer[1];
		types[0] = itemTypeId;
		newItem.setTypes(types);
		Integer itemId = childApi.createItem(newItem);

		//verify item type
		List<ItemTypeWS> itemsReceived = Arrays.asList(api.getAllItemCategoriesByEntityId(PRANCING_PONY));
		assertEquals(totalItemTypes + 1, itemsReceived.size());

		boolean found = false;
		for(ItemTypeWS itemTypeRx: itemsReceived){
			if(itemTypeRx.getDescription().equals("Root Category"+rand)){
				assertEquals(ServerConstants.ORDER_LINE_TYPE_ITEM, itemTypeRx.getOrderLineTypeId().intValue());
				found=true;
				break;
			}
		}
		assertEquals("Recently created item type not found",true,found);

		//verify item
		List<ItemDTOEx> receivedItems = Arrays.asList(api.getAllItemsByEntityId(PRANCING_PONY));
		assertEquals(totalItems + 1, receivedItems.size());

		found = false;
		for(ItemDTOEx item: receivedItems){
			if ( ("A reseller item" + rand).equals(item.getDescription()) ) {
				assertEquals("RP-1", item.getNumber());
				found = true;
				break;
			}
		}
		assertEquals("Recently created item not found",true,found);

		// tear down
		childApi.deleteItem(itemId);
		childApi.deleteItemCategory(itemTypeId);
	}

	@Test
	public void test024CreateItemDependenciesWithQuantities() {

		// Create Item
		ItemDTOEx item = createItem(false, false, TEST_ITEM_TYPE_ID);

		// Create Dependent item
		ItemDTOEx dependentItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		// Persist
		Integer dependentItemId = api.createItem(dependentItem);

		ItemDependencyDTOEx dep1 = new ItemDependencyDTOEx();
		dep1.setDependentId(dependentItemId);   //Currency test item
		dep1.setMinimum(-1);
		dep1.setType(ItemDependencyType.ITEM);
		item.setDependencies(new ItemDependencyDTOEx[]{dep1});

		System.out.println("Creating item ..." + item);
		try {
			api.createItem(item);
			fail("Exception expected");
		} catch (SessionInternalError e) {
			JBillingTestUtils.assertContainsAnyError(e, Arrays.asList("ItemDTOEx,dependencies.minimum,validation.error.min,0", "ItemDTOEx,dependencies.minimum,validation.error.notnull") );
		}

		dep1.setMinimum(3);
		dep1.setMaximum(2);
		try {
			api.createItem(item);
			fail("Exception expected");
		} catch (SessionInternalError e) {
			JBillingTestUtils.assertContainsError(e, "ItemDTOEx,dependencies,product.validation.dependencies.max.lessthan.min" );
		}

		dep1.setMinimum(2);
		dep1.setMaximum(3);

		// Persist
		Integer itemId = api.createItem(item);

        // tear down
        ItemDTOEx persistedItem = api.getItem(itemId, TEST_USER_ID, null);
        persistedItem.setDependencies(null);
        api.updateItem(persistedItem);
        api.deleteItem(itemId);
		api.deleteItem(dependentItemId);
	}

	@Test
	public void test026FindCategoryById() throws Exception{
		System.out.println("#test026FindCategoryById");
		// Get an API instance
		JbillingAPI api = JbillingAPIFactory.getAPI();
		// Find category
		final Integer DRINK_PASSES = Integer.valueOf(1);
		ItemTypeWS itemTypeWS = api.getItemCategoryById(DRINK_PASSES);

		// Free not null check
		assertNotNull(itemTypeWS);

		// Check the id
		Integer categoryId = itemTypeWS.getId();
		assertNotNull("There must be id for each entity!!!", categoryId);
		assertTrue("Entity Id must be positive number!!!", categoryId > Integer.valueOf(0));

		// Check the description
		String description = itemTypeWS.getDescription();
		assertNotNull("This category must have description!!!", description);
		assertEquals("Not a provided description!!!", description.toUpperCase(), "Drink passes".toUpperCase());

		// Check the order line type (ITEM = 1)
		Integer orderLineTypeId = itemTypeWS.getOrderLineTypeId();
		assertNotNull("This category must have order line type!!!", orderLineTypeId);
		assertEquals("Category not of type ITEM!!!", orderLineTypeId, Integer.valueOf(1));

		// Check the company Id (entity)
		Integer entityId = itemTypeWS.getEntityId();
		assertNotNull("This category must have entityId!!!", entityId);
		assertEquals("Category not in Prancing Pony!!!", orderLineTypeId, Integer.valueOf(1));

		System.out.println("test026FindCategoryById completed.");
	}

	protected static AssetWS getAssetWS(Integer itemId, Integer statusId) {
		AssetWS asset = new AssetWS();
		asset.setEntityId(PRANCING_PONY);
		asset.setIdentifier("ASSET1");
		asset.setItemId(itemId);
		asset.setNotes("NOTE1");
		asset.setAssetStatusId(statusId);
		asset.setDeleted(DISABLED);
		MetaFieldValueWS mf = new MetaFieldValueWS();
		mf.setFieldName("Regulatory Code");
		mf.setDataType(DataType.LIST);
		mf.setListValue(new String[] {"01", "02"});
		asset.setMetaFields(new MetaFieldValueWS[]{mf});
		return asset;
	}

	private void writeToFile(File file, String content) throws IOException {
		FileWriter fw = new FileWriter(file);
		fw.write(content);
		fw.close();
	}

	protected static ItemTypeWS createItemType(boolean allowAssetManagement, boolean global){
		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription("TestCategory: " + System.currentTimeMillis());
		itemType.setEntityId(PRANCING_PONY);
		if(global) {
			itemType.setGlobal(global);
		} else {
			itemType.setEntities(new HashSet<Integer>(PRANCING_PONY));
		}
		itemType.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		if(allowAssetManagement) {
			itemType.setAllowAssetManagement(ENABLED);
			itemType.setAssetIdentifierLabel("Test Asset Label");
			addAssetStatuses(itemType);
			itemType.setAssetMetaFields(createAssetMetaField());
		}
		return itemType;
	}

	protected static Set<MetaFieldWS> createAssetMetaField() {
		Set<MetaFieldWS> metaFields = new HashSet<MetaFieldWS>();

		// First Meta Filed
		MetaFieldWS metaField = new MetaFieldWS();
		metaField.setDataType(DataType.LIST);
		metaField.setName("Regulatory Code");
		metaField.setDisabled(false);
		metaField.setDisplayOrder(1);
		metaField.setEntityId(PRANCING_PONY);
		metaField.setEntityType(EntityType.ASSET);
		metaField.setMandatory(true);
		metaField.setPrimary(false);

		metaFields.add(metaField);

		// Second Meta Field
		metaField = new MetaFieldWS();
		metaField.setDataType(DataType.INTEGER);
		metaField.setName("INT1");
		metaField.setDisabled(false);
		metaField.setDisplayOrder(2);
		metaField.setEntityId(1);
		metaField.setEntityType(EntityType.ASSET);
		metaField.setMandatory(false);
		metaField.setPrimary(false);

		MetaFieldValueWS valueWS = new MetaFieldValueWS();
		valueWS.setIntegerValue(5);
		valueWS.setFieldName("INT1");
		valueWS.setDataType(DataType.INTEGER);

		metaField.setDefaultValue(valueWS);

		metaFields.add(metaField);

		return metaFields;
	}

	protected static void addAssetStatuses(ItemTypeWS itemType){
		if(null == itemType) {
			fail("Can not add statuses on null object!!");
		}
		if (!ENABLED.equals(itemType.getAllowAssetManagement())){
			fail("Can not add statuses on category that is not asset managed!!!");
		}

		// Default
		AssetStatusDTOEx status = new AssetStatusDTOEx();
		status.setDescription("One");
		status.setIsAvailable(DISABLED);
		status.setIsDefault(ENABLED);
		status.setIsOrderSaved(DISABLED);
		itemType.getAssetStatuses().add(status);

		// Available
		status = new AssetStatusDTOEx();
		status.setDescription("Two");
		status.setIsAvailable(ENABLED);
		status.setIsDefault(DISABLED);
		status.setIsOrderSaved(DISABLED);
		itemType.getAssetStatuses().add(status);

		// Order Saved
		status = new AssetStatusDTOEx();
		status.setDescription("Three");
		status.setIsAvailable(DISABLED);
		status.setIsDefault(DISABLED);
		status.setIsOrderSaved(ENABLED);
		itemType.getAssetStatuses().add(status);

		// Reserved
		status = new AssetStatusDTOEx();
		status.setDescription("Four");
		status.setIsAvailable(DISABLED);
		status.setIsDefault(DISABLED);
		status.setIsOrderSaved(DISABLED);
		itemType.getAssetStatuses().add(status);

	}

	protected static ItemDTOEx createItem(boolean allowAssetManagement, boolean global, Integer... types){
		ItemDTOEx item = new ItemDTOEx();
		item.setDescription("TestItem: " + System.currentTimeMillis());
		item.setNumber("TestWS-" + System.currentTimeMillis());
		item.setTypes(types);
		item.setPriceManual(0);
		if(allowAssetManagement){
			item.setAssetManagementEnabled(ENABLED);
		}
		item.setExcludedTypes(new Integer[]{});
		item.setHasDecimals(DISABLED);
		if(global) {
			item.setGlobal(global);
		} else {
			item.setGlobal(false);
		}
		item.setDeleted(DISABLED);
		item.setEntityId(PRANCING_PONY);
		Set<Integer> entities = new HashSet<Integer>();
		entities.add(PRANCING_PONY);
		item.setEntities(entities);
		return item;
	}

	private void matchItems(ItemDTOEx expected, ItemDTOEx actual) {
		// ID
		assertEquals("The ID of two same items differs!!", expected.getId(), actual.getId());
		// Code
		assertEquals("The Number of two same items differs!!", expected.getNumber(), actual.getNumber());
		// Excluded Types
		if(null != expected.getExcludedTypes() && null != actual.getExcludedTypes()){
			if(expected.getExcludedTypes().length != actual.getExcludedTypes().length){
				fail("The number of excluded types differs!!!");
			}
			for (int i = 0; i < expected.getExcludedTypes().length; i++){
				assertEquals(String.format("Excluded Types at %d position Differs!!", i), expected.getExcludedTypes()[i], actual.getExcludedTypes()[i]);
			}
		}
		else if(null == expected.getExcludedTypes() ^ null == actual.getExcludedTypes()){
			fail(String.format("Expected Excluded Types are %s\nActual Excluded Types are %s",
					expected.getExcludedTypes(), actual.getExcludedTypes()));
		}
		// Has Decimals
		assertEquals("Decimals flag of two same items differs!!", expected.getHasDecimals(), actual.getHasDecimals());
		// Standard Availability
		assertEquals("Is Standard Availability flag of two same Items do not match!!", expected.isStandardAvailability(), actual.isStandardAvailability());
		// Global Flag
		assertEquals("Global flag of two same Items do not match!!", expected.isGlobal(), actual.isGlobal());
		// Deleted
		assertEquals("Deleted flag of new item can not be different than 0!!", Integer.valueOf(0), actual.getDeleted());
		// Asset Management Flag
		assertEquals("Asset Management flag of two same items differs!!", expected.getAssetManagementEnabled(), actual.getAssetManagementEnabled());
		// Entity ID
		assertEquals("Entity ID of two same items differs!!", expected.getEntityId(), actual.getEntityId());
		// Entities
		if(null != expected.getEntities() && null != actual.getEntities()){
			if(expected.getEntities().size() != actual.getEntities().size()){
				fail("The number of Entities differs!!!");
			}

            for (Integer i: expected.getEntities()) {
                assertTrue(String.format("Entities in actual should not differs!!"), actual.getEntities().contains(i));
            }
		}
		else if(null == expected.getEntities() ^ null == actual.getEntities()){
			fail(String.format("Expected Entities are %s\nActual Entities are %s",
					expected.getEntities(), actual.getEntities()));
		}
		// Description
		assertEquals("Description of two same items do not match!!", expected.getDescription(), actual.getDescription());
		// Types (ItemTypes)
		if(null != expected.getTypes() && null != actual.getTypes()){
			if(expected.getTypes().length != actual.getTypes().length){
				fail("The number of types differs!!!");
			}
			for (int i = 0; i < expected.getTypes().length; i++){
				assertEquals(String.format("Types at %d position Differs!!", i), expected.getTypes()[i], actual.getTypes()[i]);
			}
		}
		else if(null == expected.getTypes() ^ null == actual.getTypes()){
			fail(String.format("Expected Types are %s\nActual Types are %s",
					expected.getTypes(), actual.getTypes()));
		}
		// International Descriptions
		if(null != expected.getDescriptions() && null != actual.getDescriptions()){
			if(expected.getDescriptions().size() != actual.getDescriptions().size()){
				fail("The number of descriptions differs!!!");
			}
			for (int i = 0; i < expected.getDescriptions().size(); i++){
				matchInternationalDescriptions(expected.getDescriptions().get(i), actual.getDescriptions().get(i));
			}
		}
		else if(null == expected.getDescriptions() ^ null == actual.getDescriptions()){
			fail(String.format("Expected Descriptions are %s\nActual Descriptions are %s",
					expected.getDescriptions(), actual.getDescriptions()));
		}
	}

	private void matchInternationalDescriptions(InternationalDescriptionWS expected, InternationalDescriptionWS actual){
		assertEquals("Pseudo Column of two same international descriptions do not match!!", expected.getPsudoColumn(), actual.getPsudoColumn());
		assertEquals("Language ID of two same international descriptions do not match!!", expected.getLanguageId(), actual.getLanguageId());
		assertEquals("Content of two same international descriptions do not match!!", expected.getContent(), actual.getContent());
		assertEquals("Delete flag of two same international descriptions do not match!!", expected.isDeleted(), actual.isDeleted());
	}

	private void matchItemTypes(ItemTypeWS expected, ItemTypeWS actual, boolean assetManagedCategories){
		assertEquals("The ID of two same categories differs!!", expected.getId(), actual.getId());
		assertEquals("The description of two same categories differs!!", expected.getDescription(), actual.getDescription());
		assertEquals("The Order Line Type ID of two same categories differs!!", expected.getOrderLineTypeId(), actual.getOrderLineTypeId());
		assertEquals("The Parent Type ID of two same categories differs!!", expected.getParentItemTypeId(), actual.getParentItemTypeId());
		assertEquals("The global flag of two same categories differs!!", expected.isGlobal(), actual.isGlobal());
		assertEquals("The internal flag of two same categories differs!!", expected.isInternal(), actual.isInternal());
		assertEquals("The Entity ID of two same categories differs!!", expected.getEntityId(), actual.getEntityId());

        for (Integer i: expected.getEntities()) {
            assertTrue(String.format("The Child Entity ID of two same categories differs!!"), actual.getEntities().contains(i));
        }

		if(assetManagedCategories) {
			assertEquals("Asset management not enabled in two same categories!!", expected.getAllowAssetManagement(), actual.getAllowAssetManagement());
			assertEquals("Asset Identifier Label not the same for two same categories!!", expected.getAssetIdentifierLabel(), actual.getAssetIdentifierLabel());
			for (Iterator<AssetStatusDTOEx> i1 = expected.getAssetStatuses().iterator(), i2 = actual.getAssetStatuses().iterator();
			     i1.hasNext() && i2.hasNext();){
				matchAssetStatuses(i1.next(), i2.next());
			}
		}
		if(null != expected.getAssetMetaFields() && null != actual.getAssetMetaFields()){
			assertEquals("The number of meta fields differs!!", expected.getAssetMetaFields().size(), actual.getAssetMetaFields().size());
			for (Iterator<MetaFieldWS> i1 = expected.getAssetMetaFields().iterator(), i2 = actual.getAssetMetaFields().iterator();
			     i1.hasNext() && i2.hasNext();){
				matchMetaFields(i1.next(), i2.next());
			}
		}
		assertEquals("The One Per Customer flag of two same categories differs!!", expected.isOnePerCustomer(), actual.isOnePerCustomer());
		assertEquals("The One Per Order flag of two same categories differs!!", expected.isOnePerOrder(), actual.isOnePerOrder());
	}

	private void matchAssetStatuses(AssetStatusDTOEx expected, AssetStatusDTOEx actual){
		assertEquals("The ID of two same asset statuses differs!!", expected.getId(), actual.getId());
		assertEquals("The description of two same asset statuses differs!!", expected.getDescription(), actual.getDescription());
		assertEquals("Is default flag of two same asset statuses differs!!", expected.getIsDefault(), actual.getIsDefault());
		assertEquals("Is available flag of two same asset statuses differs!!", expected.getIsAvailable(), actual.getIsAvailable());
		assertEquals("Is orderSaved flag of two same asset statuses differs!!", expected.getIsOrderSaved(), actual.getIsOrderSaved());
		assertEquals("Is internal flag of two same asset statuses differs!!", expected.getIsInternal(), actual.getIsInternal());
	}

	private void matchMetaFields(MetaFieldWS expected, MetaFieldWS actual){
		assertEquals("Meta Field names differs!!!", expected.getName(), actual.getName());

		if (expected.getFieldUsage() != null && actual.getFieldUsage() != null) {
			assertEquals(expected.getFieldUsage(), actual.getFieldUsage());
		} else if (expected.getFieldUsage() == null ^ actual.getFieldUsage() == null) {
			fail("Field usage is: " + expected.getFieldUsage() + " and retrieved field usage is: " + actual.getFieldUsage());
		}

		if (expected.getValidationRule() != null && actual.getValidationRule() != null) {
			matchValidationRule(expected.getValidationRule(), actual.getValidationRule());
		} else if (expected.getValidationRule() == null ^ actual.getValidationRule() == null) {
			fail("Validation rule is: " + expected.getValidationRule() + " and retrieved validation rule is: " + actual.getValidationRule());
		}
		assertEquals(expected.getDataType(), actual.getDataType());
		assertEquals(expected.getDefaultValue(), actual.getDefaultValue());
		assertEquals(expected.getDisplayOrder(), actual.getDisplayOrder());
	}

	private void matchValidationRule(ValidationRuleWS expected, ValidationRuleWS actual) {
		assertTrue("Can not validate null objects!!", expected != null && actual != null);
		assertEquals("Validation rule types differs!!", expected.getRuleType(), actual.getRuleType());
		assertEquals("Error messages length differs!!", expected.getErrorMessages().size(), actual.getErrorMessages().size());
		assertEquals("Rule Attributes differs!!", expected.getRuleAttributes(), actual.getRuleAttributes());
	}

	private Integer[] getAssetStatusesIds(Integer itemTypeId){
		if(null == itemTypeId){
			fail("Can not search for a status in Item Type with null id!!");
		}

		ItemTypeWS assetCategory = null;
		// Find the category with the itemTypeId
		ItemTypeWS[] itemTypes = api.getAllItemCategoriesByEntityId(PRANCING_PONY);
		for (ItemTypeWS itemType : itemTypes){
			if(itemType.getId().equals(itemTypeId)){
				assetCategory = itemType;
			}
		}
		Integer[] statuses = new Integer[4];
		// If category not found return 0
		if(null != assetCategory){
			for (AssetStatusDTOEx status : assetCategory.getAssetStatuses()){
				if(1 == status.getIsDefault()){
					statuses[0] = status.getId();
				}
				else if(1 == status.getIsAvailable()){
					statuses[1] = status.getId();
				}
				else if(1 == status.getIsOrderSaved()){
					statuses[2] = status.getId();
				}
			}
		}
		assertEquals("Not all asset statuses found!!!", Integer.valueOf(4), Integer.valueOf(statuses.length));
		return statuses;
	}

	@Test
	public void test027AssetReservation() throws Exception {

        /* ASSET CREATION */
		Integer categoryID = this.createCategoryWithAssetManagement();
		Integer itemID = this.createItem(categoryID);
		Integer assetID = this.createAsset(itemID, categoryID, ASSET_STATUS_AVAILABLE);
		AssetWS assetAvailable = api.getAsset(assetID);
		assertEquals("The asset status is AVAILABLE.",assetAvailable.getAssetStatusId(),this.getStatus(categoryID,ASSET_STATUS_AVAILABLE));

        /* ASSET RESERVATION */
		api.reserveAsset(assetAvailable.getId(),2);
		UserWS user = CreateObjectUtil.createUser(true, null, null);
		OrderWS order = CreateObjectUtil.createOrderObject(
				user.getId(), CURRENCY_USD, ServerConstants.ORDER_BILLING_PRE_PAID,
				ServerConstants.ORDER_PERIOD_ONCE, new DateMidnight(2013, 01, 21).toDate());

		OrderLineWS[] lines = new OrderLineWS[1];
		OrderLineWS line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("Fist line");
		line.setItemId(itemID);
		line.setAssetIds(new Integer[] {assetID});
		lines[0] = line;

		order.setOrderLines(lines);

		try {
			api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
			fail("Already reserved for different customer");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "OrderLineWS,assetIds,validation.asset.status.reserved");
		}

        /* ASSET RELEASE */
		api.releaseAsset(assetAvailable.getId(),2);
		Integer orderId = null;
		try {
			orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
			assertNotNull("Order is created", orderId);
		} finally {
			if(user!=null) api.deleteUser(user.getId());
			if(orderId!=null) api.deleteOrder(orderId);
		}
	}

	private Integer createAsset(Integer itemID, Integer categoryID, String statusDesc) throws IOException, JbillingAPIException {
		AssetWS asset = new AssetWS();
		asset.setItemId(itemID);
		asset.setAssetStatusId(this.getStatus(categoryID, statusDesc));
		asset.setIdentifier("asset-identifier-for-asset-reservation-" + Math.random() * 10000);
		asset.setEntityId(1);
		asset.setDeleted(0);
		return api.createAsset(asset);
	}
	
	private Integer getStatus(Integer categoryID, String statusDesc) throws IOException, JbillingAPIException {
		AssetStatusDTOEx statusDTOex = null;
		for(ItemTypeWS itemType : api.getAllItemCategories()) {
			if(categoryID.equals(itemType.getId())) {
				for(AssetStatusDTOEx status : itemType.getAssetStatuses()) {
					if(statusDesc.equals(ASSET_STATUS_AVAILABLE)  && status.getIsAvailable()==AssetStatusBL.ASSET_STATUS_TRUE) {
						statusDTOex = new AssetStatusDTOEx();
						statusDTOex.setId(status.getId());
						break;
					}
					else if(statusDesc.equals(ASSET_STATUS_DEFAULT)  && status.getIsDefault()==AssetStatusBL.ASSET_STATUS_TRUE) {
						statusDTOex = new AssetStatusDTOEx();
						statusDTOex.setId(status.getId());
						break;
					}
					else if(statusDesc.equals(ASSET_STATUS_ORDERED)  && status.getIsOrderSaved()==AssetStatusBL.ASSET_STATUS_TRUE) {
						statusDTOex = new AssetStatusDTOEx();
						statusDTOex.setId(status.getId());
						break;
					}
					else if(statusDesc.equals(ASSET_STATUS_INTERNAL)  && status.getIsInternal()==AssetStatusBL.ASSET_STATUS_TRUE) {
						statusDTOex = new AssetStatusDTOEx();
						statusDTOex.setId(status.getId());
						break;
					}
				}
			}
			if(statusDTOex!=null) break;
		}
		return statusDTOex.getId();
	}
	
	private Integer createItem(Integer categoryID) throws IOException, JbillingAPIException {
		ItemDTOEx item = new ItemDTOEx();
		item.setGlobal(true);
		item.setPriceManual(0);
		item.setDescription("item-test-for-asset-reservation");
		item.setNumber("ITFAR-023");
		item.setReservationDuration(10);
		item.setAssetManagementEnabled(1);
		item.setTypes(new Integer[]{categoryID});
		return api.createItem(item);
	}

	private Integer createCategoryWithAssetManagement() throws IOException, JbillingAPIException {
		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription("Asset Category " + Math.random() * 10000);
		itemType.setOrderLineTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
		itemType.setAllowAssetManagement(1);
		itemType.setGlobal(true);
		itemType.setAssetStatuses(this.createAssetStatuses());
		itemType.setDescription("category-test-for-asset-reservation-" + Math.random() * 10000);
		return api.createItemCategory(itemType);
	}

	private Set<AssetStatusDTOEx> createAssetStatuses() {
		Set<AssetStatusDTOEx> assetStatusList = new HashSet<AssetStatusDTOEx>();

		AssetStatusDTOEx status = new AssetStatusDTOEx();
		status.setIsAvailable(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsDefault(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsOrderSaved(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsInternal(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setDescription("asset-reserved");
		assetStatusList.add(status);

		status = new AssetStatusDTOEx();
		status.setIsAvailable(AssetStatusBL.ASSET_STATUS_TRUE);
		status.setIsDefault(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsOrderSaved(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsInternal(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setDescription("asset-available-A");
		assetStatusList.add(status);

		status = new AssetStatusDTOEx();
		status.setIsAvailable(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsDefault(AssetStatusBL.ASSET_STATUS_TRUE);
		status.setIsOrderSaved(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsInternal(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setDescription("asset-default");
		assetStatusList.add(status);

		status = new AssetStatusDTOEx();
		status.setIsAvailable(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsDefault(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsOrderSaved(AssetStatusBL.ASSET_STATUS_TRUE);
		status.setIsInternal(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setDescription("asset-order-saved");
		assetStatusList.add(status);

		return assetStatusList;
	}
	
}
