package com.sapienter.jbilling.server.item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.*;
import static org.testng.AssertJUnit.*;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.JBillingTestUtils;

@Test(groups = { "web-services", "item" })
public class AssetValidateTest {
	private static final Integer PRANCING_PONY = Integer.valueOf(1);
	private static final Integer Reseller_Organization = Integer.valueOf(3);
	private static final Integer Child_Company = Integer.valueOf(4);
	private static final Integer ENABLED = Integer.valueOf(1);
	private static final Integer DISABLED = Integer.valueOf(0);
	private static Integer TEST_ASSET_ITEM_TYPE_ID;
	private static Integer TEST_ITEM_ID_WITH_ASSET_MANAGEMENT;
	private static Integer STATUS_DEFAULT_ID;
	private static JbillingAPI api;
	private Integer assetIdRoot1;
	private Integer assetIdRoot2;
	private Integer assetIdReseller1;
	private Integer assetIdReseller2;
	private Integer assetIdChild1;
	private Integer assetIdChild2;
	private Integer assetIdGlobal;
	
	@BeforeClass
	public void initializeTests() throws IOException, JbillingAPIException {
		if(null == api){
			api = JbillingAPIFactory.getAPI();
		}
		
		// Create Asset Managed Item Type
		ItemTypeWS assetItemType = WSTest.createItemType(true, true);
		TEST_ASSET_ITEM_TYPE_ID = api.createItemCategory(assetItemType);
		assertNotNull(String.format("Item type id should not be null !!!"), TEST_ASSET_ITEM_TYPE_ID);
		
		// Create Asset Managed Item
		ItemDTOEx assetProduct = WSTest.createItem(true, true, TEST_ASSET_ITEM_TYPE_ID);
	
		// Persist Item
		TEST_ITEM_ID_WITH_ASSET_MANAGEMENT = api.createItem(assetProduct);
		assertNotNull(String.format("Item id should not be null !!!"), TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
		
		// Get Default Asset Status Id
		Integer[] statusesIds = getAssetStatusesIds(TEST_ASSET_ITEM_TYPE_ID);
		STATUS_DEFAULT_ID = statusesIds[0];
		
		AssetWS asset = WSTest.getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("ASSETROOT1");
		List<Integer> entities =new ArrayList<Integer>();
			entities.add(PRANCING_PONY);
		asset.setEntities(entities);
		assetIdRoot1 = api.createAsset(asset);
		
		assertNotNull(String.format("Asset id1 should not be null for PRANCING_PONY !!!"), assetIdRoot1);
		
		asset = WSTest.getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("ASSETROOT2");
		asset.setEntities(entities);
		assetIdRoot2 = api.createAsset(asset);
		
		assertNotNull(String.format("Asset id2 should not be null for PRANCING_PONY !!!"), assetIdRoot2);
		
		asset = WSTest.getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("ASSETRESELLER1");
		List<Integer> entities2 =new ArrayList<Integer>();
			entities2.add(Reseller_Organization);
		asset.setEntities(entities2);
		assetIdReseller1 = api.createAsset(asset);
		
		assertNotNull(String.format("Asset id1 should not be null for Reseller_Organization !!!"), assetIdReseller1);
		
		asset = WSTest.getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("ASSETRESELLER2");
		asset.setEntities(entities2);
		assetIdReseller2 = api.createAsset(asset);
		
		assertNotNull(String.format("Asset id2 can not be null for Reseller_Organization !!!"), assetIdReseller2);
		
		asset = WSTest.getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("ASSETCHILD1");
		List<Integer> entities3 =new ArrayList<Integer>();
			entities3.add(Child_Company);
		asset.setEntities(entities3);
		assetIdChild1 = api.createAsset(asset);
		
		assertNotNull(String.format("Asset id1 should not be null for child_company !!!"), assetIdChild1);
		
		asset = WSTest.getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("ASSETCHILD2");
		asset.setEntities(entities3);
		assetIdChild2 = api.createAsset(asset);
		
		assertNotNull(String.format("Asset id2 should not be null for child_company !!!"), assetIdChild2);
		
		asset = WSTest.getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("ASSETGLOBAL1");
		asset.setGlobal(true);
        asset.setContainedAssetIds(null);
		assetIdGlobal = api.createAsset(asset);
		
		assertNotNull(String.format("Asset id should not be null for global !!!"), assetIdGlobal);
	}
	
	@AfterClass
	public void tearDown() throws Exception {

		if(null != api){
			api = null;
		}
	}
	
	
	
	@Test(groups = { "web-services", "asset"})
	public void test01validateAssetWithRoot(){
	
		AssetWS asset=WSTest.getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("Group1");
		List<Integer> rootEntity = new ArrayList<Integer>();
		rootEntity.add(PRANCING_PONY);	
		asset.setEntities(rootEntity);
		asset.setContainedAssetIds(new Integer[] {assetIdRoot1, assetIdReseller2, assetIdChild2});

		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {          //The asset [ASSET1] is already part of an asset group.
			JBillingTestUtils.assertContainsError(error,  "AssetWS,containedAssets,validation.child.asset.not.add.root.asset");
		}
		
		asset.setContainedAssetIds(null);
		asset.setContainedAssetIds(new Integer[] {assetIdRoot1, assetIdReseller2, assetIdGlobal});
		
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {          //The asset [ASSET1] is already part of an asset group.
			JBillingTestUtils.assertContainsError(error,  "AssetWS,containedAssets,validation.child.asset.not.add.root.asset");
		}
		
		asset.setContainedAssetIds(null);
		asset.setContainedAssetIds(new Integer[] {assetIdRoot1, assetIdGlobal});
		
		Integer assetId = api.createAsset(asset);
		assertNotNull(String.format("Asset must be created with having contained asset with same company !!!"), assetId);
		
		api.deleteAsset(assetId);
	}
	
	@Test(groups = { "web-services", "asset"})
	public void test02validateAssetWithChildReseller(){
	
		AssetWS asset=WSTest.getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		List<Integer> resellerEntity = new ArrayList<Integer>();
			resellerEntity.add(Reseller_Organization);
        asset.setEntityId(api.getCallerCompanyId());
		asset.setEntities(resellerEntity);
		asset.setIdentifier("Group2");
		asset.setContainedAssetIds(new Integer[] {assetIdRoot2, assetIdReseller2, assetIdChild2});
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {          //The asset [ASSET1] is already part of an asset group.
			JBillingTestUtils.assertContainsError(error,  "AssetWS,containedAssets,validation.child.asset.not.add.root.asset");
		}
		asset.setContainedAssetIds(new Integer[] {assetIdRoot2, assetIdReseller2, assetIdGlobal});
		
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {          //The asset [ASSET1] is already part of an asset group.
			JBillingTestUtils.assertContainsError(error,  "AssetWS,containedAssets,validation.child.asset.not.add.root.asset");
		}
		asset.setContainedAssetIds(new Integer[] {assetIdReseller2, assetIdGlobal});
		
		Integer assetId = api.createAsset(asset);
		assertNotNull(String.format("Asset must be created with having contained asset with same company !!!"), assetId);
		
		api.deleteAsset(assetId);
	}
	
	
	@Test(groups = { "web-services", "asset"})
	public void test03validateAssetWithChild(){
	
		AssetWS asset=WSTest.getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		List<Integer> childEntity = new ArrayList<Integer>();
		childEntity.add(Child_Company);	
		asset.setEntities(childEntity);
		asset.setIdentifier("Group3");
		asset.setContainedAssetIds(new Integer[] {assetIdRoot2, assetIdReseller2, assetIdChild1});
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {          //The asset [ASSET1] is already part of an asset group.
			JBillingTestUtils.assertContainsError(error,  "AssetWS,containedAssets,validation.child.asset.not.add.root.asset");
		}
		asset.setContainedAssetIds(new Integer[] {assetIdRoot2, assetIdGlobal, assetIdChild1});
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {          //The asset [ASSET1] is already part of an asset group.
			JBillingTestUtils.assertContainsError(error,  "AssetWS,containedAssets,validation.child.asset.not.add.root.asset");
		}
		asset.setContainedAssetIds(new Integer[] {assetIdGlobal, assetIdChild1});
		
		Integer assetId = api.createAsset(asset);
		assertNotNull(String.format("Asset must be created with having contained asset with same company !!!"), assetId);
		
		api.deleteAsset(assetId);

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
		Integer[] statuses = new Integer[3];
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
		assertEquals("Not all asset statuses found!!!", Integer.valueOf(3), Integer.valueOf(statuses.length));
		return statuses;
	}
	
}
