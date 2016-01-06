package com.sapienter.jbilling.server.asset;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.*;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.junit.BeforeClass;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by vojislav on 10/31/14.
 */
@Test(groups = {"web-services", "asset-assignment"})
public class WSTest {

	// Asset managed category details
	private static final Integer ASSET_MANAGEMENT_ENABLED = Integer.valueOf(1);
	private static final Integer ORDER_LINE_TYPE_ID_ITEM = Integer.valueOf(1);
	// end of asset managed category details

	// Statuses flags
	private static final Integer ASSET_AVAILABLE_STATUS_ENABLED = Integer.valueOf(1);
	private static final Integer ASSET_AVAILABLE_STATUS_DISABLED = Integer.valueOf(0);
	private static final Integer ASSET_DEFAULT_STATUS_ENABLED = Integer.valueOf(1);
	private static final Integer ASSET_DEFAULT_STATUS_DISABLED = Integer.valueOf(0);
	private static final Integer ASSET_ORDERED_SAVED_STATUS_ENABLED = Integer.valueOf(1);
	private static final Integer ASSET_ORDERED_SAVED_STATUS_DISABLED = Integer.valueOf(0);;
	// End of statuses flags

	// Pricing model
	private static final Integer PRICING_MODEL_CATEGORY_ID = Integer.valueOf(1);
	//

	// Config details
	private final static Integer CURRENCY_US = Integer.valueOf(1);
	private final static Integer PRANCING_PONY_COMPANY_ID = Integer.valueOf(1);
	//

	// Order details
	private final static int ORDER_PERIOD_MONTHLY = Integer.valueOf(2);
	private final static int ORDER_CHANGE_STATUS_APPLY_ID = Integer.valueOf(3);
	//

	// Suffix value
	private static final String ENTITY_SUFFIX = String.valueOf(new Date().getTime());

	// The API
	private JbillingAPI api;



	@BeforeClass
	public void initializeApi(){
		try {
			if(api == null){
				System.out.println("Initialize the API.");
				api = JbillingAPIFactory.getAPI();
			}
		} catch (Exception e) {
			e.printStackTrace();
			AssertJUnit.fail("API not available!!!");
		}
	}

	@AfterClass
	public void releaseResources(){
		if(api != null){
			System.out.println("Destroy the API.");
			api = null;
		}
	}


	@Test
	public void test001CreateAssetAssignment() throws Exception{

		System.out.println("test001CreateAssetAssignment");

		// Create and persist user
		UserWS customer = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, CURRENCY_US);
		Integer customerId = customer.getId();

		// Create and persist Asset Managed category
		String categoryName = String.format("AATest001_Category-%s", ENTITY_SUFFIX);
		String assetIdLabel = String.format("AATest001_IdLabel-%s", ENTITY_SUFFIX);
		ItemTypeWS assetManagedCategory = createAssetManagedItemType(categoryName, assetIdLabel, true);
		Integer assetManagedCategoryId = assetManagedCategory.getId();

		// Create and persist asset managed item
		String productName = String.format("AATest001_Product-%s", ENTITY_SUFFIX);
		BigDecimal productPrice = new BigDecimal(100);
		String productCode = String.format("AATS001-%s", ENTITY_SUFFIX);
		ItemDTOEx assetManagedProduct = createAssetManagedItem(
				productName, productPrice, productCode, assetManagedCategoryId, true);
		Integer productId = assetManagedProduct.getId();

		// Find default status id
		Integer defaultStatusId = findDefaultStatusId(assetManagedCategory);
		if(defaultStatusId == null || defaultStatusId < Integer.valueOf(0)){
			AssertJUnit.fail("Default status not found!!!");
		}

		// Create and persist asset
		String assetIdentifierValue = String.format("AATest001_Value-%s", ENTITY_SUFFIX);
		AssetWS asset = createAsset(assetIdentifierValue, productId, defaultStatusId, true);
		Integer assetId = asset.getId();

		// Order Lines
		Integer[] assetsIds = {assetId};
		BigDecimal price = new BigDecimal(100);
		String orderLineDescription = "AATest001_OrderLineDesc";
		OrderLineWS orderLine = createOrderLine(price, assetsIds.length, orderLineDescription, productId, assetsIds);

		// Create and persist order for a customer and order line
		OrderWS order = createMonthlyOrder(customerId, new OrderLineWS[]{orderLine}, true, null);
		Integer orderId = order.getId();
		// At this point we know that there is only one order line
		Integer orderLineId = order.getOrderLines()[0].getId();

        /*
        Scenario 1
        Get assignment details for the persisted asset. At this point there should be one assignment for the persisted asset
        and all the fields for the assignment entity except the endDateTime should contain not null values.
         */

		System.out.println("Scenario 1");
		// Get all assignments for the asset.
		AssetAssignmentWS[] allAssetAssignmentsForAsset = api.getAssetAssignmentsForAsset(assetId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", allAssetAssignmentsForAsset);
		AssertJUnit.assertEquals("There should be one assignment!!!", 1, allAssetAssignmentsForAsset.length);

		// At this point we know that there is one assignment.
		AssetAssignmentWS firstAssignmentForAsset = allAssetAssignmentsForAsset[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", firstAssignmentForAsset);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start date time but don't have end date time.
        */
		verifyAssignmentFields(firstAssignmentForAsset, assetId, orderId, orderLineId, true, false);
		System.out.println("Scenario 1 done.");

        /*
        Scenario 2
        At this point we have proven that there is one assignment for the persisted asset.
        In the placed order there is one order line containing the persisted asset only,
        so the asset assignments for this order should contain the persisted asset assignments only
        ie the one and only assignment tested in the previous scenario.
         */

		System.out.println("Scenario 2");
		// Get all assignments for the order
		AssetAssignmentWS[] allAssetAssignmentsForOrder = api.getAssetAssignmentsForOrder(orderId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", allAssetAssignmentsForOrder);
		AssertJUnit.assertEquals("There should be one assignment!!!", 1, allAssetAssignmentsForOrder.length);

		// At this point we know that there is one assignment.
		AssetAssignmentWS firstAssignmentForOrder = allAssetAssignmentsForOrder[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", firstAssignmentForOrder);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start date time but don't have end date time.
        */
		verifyAssignmentFields(firstAssignmentForOrder, assetId, orderId, orderLineId, true, false);

		// Check if we are dealing with the same assignment
		AssertJUnit.assertEquals("Assignment ids are not the same!!!",
				firstAssignmentForAsset.getId(), firstAssignmentForOrder.getId());
		System.out.println("Scenario 2 done.");

		System.out.println("Deleting all test entities...");
		deleteOrder(orderId);
		deleteAsset(assetId);
		deleteItem(productId);
		deleteCategory(assetManagedCategoryId);
		deleteUser(customerId);
		System.out.println("Delete complete.");

		System.out.println("test001CreateAssetAssignment completed!!!");
	}

	@Test
	public void test002MultipleAssetsAssignments() throws Exception{

		System.out.println("test002MultipleAssetsAssignments");

		// Create and persist user
		UserWS customer = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, CURRENCY_US);
		Integer customerId = customer.getId();

		// Create and persist Asset Managed category
		String categoryName = String.format("AATest002_Category-%s", ENTITY_SUFFIX);
		String assetIdLabel = "AATest002_IdLabel";
		ItemTypeWS assetManagedCategory = createAssetManagedItemType(categoryName, assetIdLabel, true);
		Integer assetManagedCategoryId = assetManagedCategory.getId();

		// Create and persist asset managed item
		String productName = String.format("AATest002_Product-%s", ENTITY_SUFFIX);
		BigDecimal productPrice = new BigDecimal(100);
		String productCode = String.format("AATS002-%s", ENTITY_SUFFIX);
		ItemDTOEx assetManagedProduct = createAssetManagedItem(
				productName, productPrice, productCode, assetManagedCategoryId, true);
		Integer productId = assetManagedProduct.getId();

		// Find default status id
		Integer defaultStatusId = findDefaultStatusId(assetManagedCategory);
		if(defaultStatusId == null || defaultStatusId < Integer.valueOf(0)){
			AssertJUnit.fail("Default status not found!!!");
		}

		// Create and persist assets
		String firstAssetIdentifierValue = String.format("AATest002_Value1-%s", ENTITY_SUFFIX);
		String secondAssetIdentifierValue = String.format("AATest002_Value2-%s", ENTITY_SUFFIX);
		String thirdAssetIdentifierValue = String.format("AATest002_Value3-%s", ENTITY_SUFFIX);
		AssetWS firstAsset = createAsset(firstAssetIdentifierValue, productId, defaultStatusId, true);
		AssetWS secondAsset = createAsset(secondAssetIdentifierValue, productId, defaultStatusId, true);
		AssetWS thirdAsset = createAsset(thirdAssetIdentifierValue, productId, defaultStatusId, true);
		Integer firstAssetId = firstAsset.getId();
		Integer secondAssetId = secondAsset.getId();
		Integer thirdAssetId = thirdAsset.getId();

		// Order Lines
		Integer[] assetsIds = {firstAssetId, secondAssetId, thirdAssetId};
		BigDecimal price = new BigDecimal(300);
		String orderLineDescription = "AATest002_OrderLineDesc";
		OrderLineWS orderLine = createOrderLine(price, assetsIds.length, orderLineDescription, productId, assetsIds);

		// Create and persist order for a customer and order line
		OrderWS order = createMonthlyOrder(customerId, new OrderLineWS[]{orderLine}, true, null);
		Integer orderId = order.getId();
		// At this point we know that there is only one order line
		Integer orderLineId = order.getOrderLines()[0].getId();

        /*
        Scenario 1
        Three assets are placed in an order so they must have active assignment entities, they must have start date time
        but not end date time. Check if all assignments have not null values for all the id fields (order, order line and asset)
        and the start date time. At this point the end date time should be null.
        Also verify that these assignments were assigned for this specific order and order line(all assignments must have the same orderId and orderLineId).
         */

		System.out.println("Scenario 1");
		// Check the assignment entity for each of the assets

		// FIRST ASSET
		// Get all assignments for the first asset.
		AssetAssignmentWS[] firstAssetAllAssignments = api.getAssetAssignmentsForAsset(firstAssetId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", firstAssetAllAssignments);
		AssertJUnit.assertEquals("There should be one assignment!!!", 1, firstAssetAllAssignments.length);

		// At this point we know that there is one assignment.
		AssetAssignmentWS firstAssetFirstAssignment = firstAssetAllAssignments[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", firstAssetFirstAssignment);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start date time but don't have end date time.
        */
		verifyAssignmentFields(firstAssetFirstAssignment, firstAssetId, orderId, orderLineId, true, false);

		// SECOND ASSET
		// Get all assignments for the second asset.
		AssetAssignmentWS[] secondAssetAllAssignments = api.getAssetAssignmentsForAsset(secondAssetId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", secondAssetAllAssignments);
		AssertJUnit.assertEquals("There should be one assignment!!!", 1, secondAssetAllAssignments.length);

		// At this point we know that there is one assignment.
		AssetAssignmentWS secondAssetFirstAssignment = secondAssetAllAssignments[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", secondAssetFirstAssignment);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start date time but don't have end date time.
        */
		verifyAssignmentFields(secondAssetFirstAssignment, secondAssetId, orderId, orderLineId, true, false);


		// THIRD ASSET
		// Get all assignments for the third asset.
		AssetAssignmentWS[] thirdAssetAllAssignments = api.getAssetAssignmentsForAsset(thirdAssetId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", thirdAssetAllAssignments);
		AssertJUnit.assertEquals("There should be one assignment!!!", 1, thirdAssetAllAssignments.length);

		// At this point we know that there is one assignment.
		AssetAssignmentWS thirdAssetFirstAssignment = thirdAssetAllAssignments[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", thirdAssetFirstAssignment);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start date time but don't have end date time.
        */
		verifyAssignmentFields(thirdAssetFirstAssignment, thirdAssetId, orderId, orderLineId, true, false);

		System.out.println("Scenario 1 done");

        /*
        Scenario 2
        At this point we see that there is one assignment for each asset, but for the specific order there should
        be three asset assignments which are the same assignments for each of the assets.
         */

		System.out.println("Scenario 2");

		AssetAssignmentWS[] allOrderAssetAssignments = api.getAssetAssignmentsForOrder(orderId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", allOrderAssetAssignments);
		// Check if there are three assignments.
		AssertJUnit.assertEquals("There should be three assignments!!!", 3, allOrderAssetAssignments.length);
		for (AssetAssignmentWS assignment : allOrderAssetAssignments){
			AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", assignment);

            /*
            (This entity is auto-generated by the system so all required fields must contain values).
            Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
            Also verify that this assignment have start date time but don't have end date time.
            */
			verifyAssignmentFields(assignment, null, orderId, orderLineId, true, false);

			Integer assetId = assignment.getAssetId();
			if(!assetId.equals(firstAssetId) && !assetId.equals(secondAssetId) && !assetId.equals(thirdAssetId)){
				AssertJUnit.fail("This assignment is not related to any persisted asset!!!");
			}
		}

		System.out.println("Scenario 2 done");

		System.out.println("Deleting all test entities...");
		deleteOrder(orderId);
		deleteAsset(thirdAssetId);
		deleteAsset(secondAssetId);
		deleteAsset(firstAssetId);
		deleteItem(productId);
		deleteCategory(assetManagedCategoryId);
		deleteUser(customerId);
		System.out.println("Delete complete.");

		System.out.println("test002MultipleAssetsAssignments completed!!!");

	}

	@Test
	public void test003UpdateAssetAssignment() throws Exception{

		System.out.println("test003UpdateAssetAssignment");

		Date orderChangeStartDate = new Date();
		addDays(orderChangeStartDate, -10); //start with changes in the past from today - 10days

		// Create and persist user
		UserWS customer = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, CURRENCY_US);
		Integer customerId = customer.getId();

		// Create and persist Asset Managed category
		String categoryName = String.format("AATest003_Category-%s", ENTITY_SUFFIX);
		String assetIdLabel = "AATest003_IdLabel";
		ItemTypeWS assetManagedCategory = createAssetManagedItemType(categoryName, assetIdLabel, true);
		Integer assetManagedCategoryId = assetManagedCategory.getId();

		// Create and persist asset managed item
		String productName = String.format("AATest003_Product-%s", ENTITY_SUFFIX);
		BigDecimal productPrice = new BigDecimal(100);
		String productCode = String.format("AATS003-%s", ENTITY_SUFFIX);
		ItemDTOEx assetManagedProduct = createAssetManagedItem(
				productName, productPrice, productCode, assetManagedCategoryId, true);
		Integer productId = assetManagedProduct.getId();

		// Find default status id
		Integer defaultStatusId = findDefaultStatusId(assetManagedCategory);
		if(defaultStatusId == null || defaultStatusId < Integer.valueOf(0)){
			AssertJUnit.fail("Default status not found!!!");
		}

		// Create and persist assets
		String firstAssetIdentifierValue = String.format("AATest003_Value1-%s", ENTITY_SUFFIX);
		String secondAssetIdentifierValue = String.format("AATest003_Value2-%s", ENTITY_SUFFIX);
		AssetWS firstAsset = createAsset(firstAssetIdentifierValue, productId, defaultStatusId, true);
		AssetWS secondAsset = createAsset(secondAssetIdentifierValue, productId, defaultStatusId, true);
		Integer firstAssetId = firstAsset.getId();
		Integer secondAssetId = secondAsset.getId();

		// Order Lines
		Integer[] assetsIds = {firstAssetId, secondAssetId};
		BigDecimal price = new BigDecimal(200);
		String orderLineDescription = "AATest003_OrderLineDesc";
		OrderLineWS orderLine = createOrderLine(price, assetsIds.length, orderLineDescription, productId, assetsIds);

		// Create and persist order for a customer and order line
		OrderWS order = createMonthlyOrder(customerId, new OrderLineWS[]{orderLine}, true, orderChangeStartDate);
		Integer orderId = order.getId();
		// At this point we know that there is only one order line
		Integer orderLineId = order.getOrderLines()[0].getId();


        /*
        Scenario 1
        Update the order by removing one of the assets from that order, and check if the assignment for that asset is
        updated. At this point the assignment for the removed asset must have both, start and end date time.
        Verify the assignments both by asset and by order.
         */

		System.out.println("Scenario 1");
		// Remove the second asset from the order
		Integer[] updatedOrderAssetsIds = new Integer[]{firstAssetId};

		// Calculate the difference in quantities
		Integer quantity = updatedOrderAssetsIds.length - assetsIds.length;

		// Get the order lines
		orderLine = api.getOrderLine(orderLineId);

		// Create the order change
		BigDecimal updatedPrice = new BigDecimal(100);
		addDays(orderChangeStartDate, +1);
		OrderChangeWS orderChange = createOrderChange(
				orderLine, updatedPrice, quantity, updatedOrderAssetsIds, orderChangeStartDate);

		// Update the order
		api.updateOrder(order, new OrderChangeWS[]{orderChange});

		// VERIFY BY ASSETS

		// FIRST ASSET
		// Get all assignments for the first asset.
		AssetAssignmentWS[] firstAssetAllAssignmentsAfterUpdate = api.getAssetAssignmentsForAsset(firstAssetId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", firstAssetAllAssignmentsAfterUpdate);
		AssertJUnit.assertEquals("There should be one assignment!!!", 1, firstAssetAllAssignmentsAfterUpdate.length);

		// At this point we know that there is one assignment.
		AssetAssignmentWS firstAssetFirstAssignmentAfterUpdate = firstAssetAllAssignmentsAfterUpdate[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", firstAssetFirstAssignmentAfterUpdate);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start date time but don't have end date time.
        */
		verifyAssignmentFields(firstAssetFirstAssignmentAfterUpdate, firstAssetId, orderId, orderLineId, true, false);


		// SECOND ASSET
		// Get all assignments for the second asset.
		AssetAssignmentWS[] secondAssetAllAssignmentsAfterUpdate = api.getAssetAssignmentsForAsset(secondAssetId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", secondAssetAllAssignmentsAfterUpdate);
		AssertJUnit.assertEquals("There should be one assignment!!!", 1, secondAssetAllAssignmentsAfterUpdate.length);

		// At this point we know that there is one assignment.
		AssetAssignmentWS secondAssetFirstAssignmentAfterUpdate = secondAssetAllAssignmentsAfterUpdate[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", secondAssetFirstAssignmentAfterUpdate);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start and end date time.
        */
		verifyAssignmentFields(secondAssetFirstAssignmentAfterUpdate, secondAssetId, orderId, orderLineId, true, true);

		// VERIFY BY ORDER
		// Get all the assignments for a order.
		AssetAssignmentWS[] allAssetAssignmentsByOrderAfterUpdate = api.getAssetAssignmentsForOrder(orderId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", allAssetAssignmentsByOrderAfterUpdate);
		AssertJUnit.assertEquals("There should be two assignments!!!", 2, allAssetAssignmentsByOrderAfterUpdate.length);

		for (AssetAssignmentWS orderAssignment : allAssetAssignmentsByOrderAfterUpdate){
			// verify by order and order line ids and start date time
			verifyAssignmentFields(orderAssignment, null, orderId, orderLineId, true, null);
		}

		System.out.println("Scenario 1 done.");

        /*
        Scenario 2
        Update the order again by putting back the removed asset, and verify that the asset has
        two assignment entities, one with start and end date time, and one with start date only.
         */

		System.out.println("Scenario 2");

		// Add the second asset back to the order.
		Integer[] updatedOrderAssetsIds2 = new Integer[]{firstAssetId, secondAssetId};

		// Calculate the difference in quantities
		Integer updatedQuantity = updatedOrderAssetsIds2.length - updatedOrderAssetsIds.length;

		// Create the order change
		BigDecimal updatedPrice2 = new BigDecimal(200);
		addDays(orderChangeStartDate, +1);//the second scenario happens a day latter;
		OrderChangeWS orderChangeUpdated = createOrderChange(
				orderLine, updatedPrice2, updatedQuantity, updatedOrderAssetsIds2, orderChangeStartDate);

		// Update the order
		api.updateOrder(order, new OrderChangeWS[]{orderChangeUpdated});

		// VERIFY BY ASSETS

		// FIRST ASSET
		// Get all assignments for the first asset.
		AssetAssignmentWS[] firstAssetAllAssignmentsAfterUpdate2 = api.getAssetAssignmentsForAsset(firstAssetId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", firstAssetAllAssignmentsAfterUpdate2);
		AssertJUnit.assertEquals("There should be one assignment!!!", 1, firstAssetAllAssignmentsAfterUpdate2.length);

		// At this point we know that there is one assignment.
		AssetAssignmentWS firstAssetFirstAssignmentAfterUpdate2 = firstAssetAllAssignmentsAfterUpdate2[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", firstAssetFirstAssignmentAfterUpdate2);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start date only.
        */
		verifyAssignmentFields(firstAssetFirstAssignmentAfterUpdate2, firstAssetId, orderId, orderLineId, true, false);

		// SECOND ASSET
		// Get all assignments for the second asset.
		AssetAssignmentWS[] secondAssetAllAssignmentsAfterUpdate2 = api.getAssetAssignmentsForAsset(secondAssetId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", secondAssetAllAssignmentsAfterUpdate2);
		AssertJUnit.assertEquals("There should be two assignments!!!", 2, secondAssetAllAssignmentsAfterUpdate2.length);

		// At this point we know that there are two assignments.

		// TEST THE FIRST ASSIGNMENT
		AssetAssignmentWS secondAssetFirstAssignmentAfterUpdate2 = secondAssetAllAssignmentsAfterUpdate2[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", secondAssetFirstAssignmentAfterUpdate2);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start date only.
        */
		verifyAssignmentFields(secondAssetFirstAssignmentAfterUpdate2, secondAssetId, orderId, orderLineId, true, false);

		// TEST THE SECOND ASSIGNMENT
		AssetAssignmentWS secondAssetSecondAssignmentAfterUpdate2 = secondAssetAllAssignmentsAfterUpdate2[1];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", secondAssetSecondAssignmentAfterUpdate2);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start and end date time.
        */
		verifyAssignmentFields(secondAssetSecondAssignmentAfterUpdate2, secondAssetId, orderId, orderLineId, true, true);

		// VERIFY BY ORDER

		// Get all the assignments for a order.
		AssetAssignmentWS[] allAssetAssignmentsByOrderAfterUpdate2 = api.getAssetAssignmentsForOrder(orderId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", allAssetAssignmentsByOrderAfterUpdate2);
		AssertJUnit.assertEquals("There should be three assignments!!!", 3, allAssetAssignmentsByOrderAfterUpdate2.length);

		for (AssetAssignmentWS orderAssignment : allAssetAssignmentsByOrderAfterUpdate2){
			// verify by order and order line ids and start date time
			verifyAssignmentFields(orderAssignment, null, orderId, orderLineId, true, null);
		}

		System.out.println("Scenario 2 done.");

		System.out.println("Deleting all test entities...");
		deleteOrder(orderId);
		deleteAsset(secondAssetId);
		deleteAsset(firstAssetId);
		deleteItem(productId);
		deleteCategory(assetManagedCategoryId);
		deleteUser(customerId);
		System.out.println("Delete complete.");

		System.out.println("test003UpdateAssetAssignment completed!!!");

	}

	@Test
	public void test004SwapAssets() throws Exception{
		System.out.println("test004SwapAssets");

		// Create and persist user
		UserWS customer = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, CURRENCY_US);
		Integer customerId = customer.getId();

		// Create and persist Asset Managed category
		String categoryName = String.format("AATest004_Category-%s", ENTITY_SUFFIX);
		String assetIdLabel = "AATest004_IdLabel";
		ItemTypeWS assetManagedCategory = createAssetManagedItemType(categoryName, assetIdLabel, true);
		Integer assetManagedCategoryId = assetManagedCategory.getId();

		// Create and persist asset managed item
		String productName = String.format("AATest004_Product-%s", ENTITY_SUFFIX);
		BigDecimal productPrice = new BigDecimal(100);
		String productCode = String.format("AATS004-%s", ENTITY_SUFFIX);
		ItemDTOEx assetManagedProduct = createAssetManagedItem(
				productName, productPrice, productCode, assetManagedCategoryId, true);
		Integer productId = assetManagedProduct.getId();

		// Find default status id
		Integer defaultStatusId = findDefaultStatusId(assetManagedCategory);
		if(defaultStatusId == null || defaultStatusId < Integer.valueOf(0)){
			AssertJUnit.fail("Default status not found!!!");
		}

		// Create and persist assets
		String firstAssetIdentifierValue = String.format("AATest004_Value1-%s", ENTITY_SUFFIX);
		String secondAssetIdentifierValue = String.format("AATest004_Value2-%s", ENTITY_SUFFIX);
		String thirdAssetIdentifierValue = String.format("AATest004_Value3-%s", ENTITY_SUFFIX);
		AssetWS firstAsset = createAsset(firstAssetIdentifierValue, productId, defaultStatusId, true);
		AssetWS secondAsset = createAsset(secondAssetIdentifierValue, productId, defaultStatusId, true);
		AssetWS thirdAsset = createAsset(thirdAssetIdentifierValue, productId, defaultStatusId, true);
		Integer firstAssetId = firstAsset.getId();
		Integer secondAssetId = secondAsset.getId();
		Integer thirdAssetId = thirdAsset.getId();

		// Order Lines
		Integer[] assetsIds = {firstAssetId, secondAssetId};
		BigDecimal price = new BigDecimal(200);
		String orderLineDescription = "AATest004_OrderLineDesc";
		OrderLineWS orderLine = createOrderLine(price, assetsIds.length, orderLineDescription, productId, assetsIds);

		// Create and persist order for a customer and order line
		OrderWS order = createMonthlyOrder(customerId, new OrderLineWS[]{orderLine}, true, null);
		Integer orderId = order.getId();
		// At this point we know that there is only one order line
		Integer orderLineId = order.getOrderLines()[0].getId();

        /*
        Scenario 1
        Update the order by swapping the second asset with the third asset and check the asset assignment details for all assets.
         */

		System.out.println("Scenario 1");
		// UPDATE THE ORDER (swap second asset with the third)
		Integer[] updatedAssetsIds = {firstAssetId, thirdAssetId};
		Integer quantity = updatedAssetsIds.length - assetsIds.length;

		orderLine = api.getOrderLine(orderLineId);
		BigDecimal updatedPrice = new BigDecimal(200);
		OrderChangeWS orderChange = createOrderChange(orderLine, updatedPrice, quantity, updatedAssetsIds, null);
		api.updateOrder(order, new OrderChangeWS[]{orderChange});

		// VERIFY BY ASSETS

		// FIRST ASSET
		// Get all assignments for the first asset.
		AssetAssignmentWS[] firstAssetAllAssignmentsAfterSwap = api.getAssetAssignmentsForAsset(firstAssetId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", firstAssetAllAssignmentsAfterSwap);
		AssertJUnit.assertEquals("There should be one assignment!!!", 1, firstAssetAllAssignmentsAfterSwap.length);

		// At this point we know that there is one assignment.
		AssetAssignmentWS firstAssetFirstAssignmentAfterSwap = firstAssetAllAssignmentsAfterSwap[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", firstAssetFirstAssignmentAfterSwap);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start date time only.
        */
		verifyAssignmentFields(firstAssetFirstAssignmentAfterSwap, firstAssetId, orderId, orderLineId, true, false);

		// The second asset should have one assignments with start and end date time.

		// SECOND ASSET
		// Get all assignments for the second asset.
		AssetAssignmentWS[] secondAssetAllAssignmentsAfterSwap = api.getAssetAssignmentsForAsset(secondAssetId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", secondAssetAllAssignmentsAfterSwap);
		AssertJUnit.assertEquals("There should be one assignments!!!", 1, secondAssetAllAssignmentsAfterSwap.length);

		// At this point we know that there is one assignment.
		AssetAssignmentWS secondAssetFirstAssignmentAfterSwap = secondAssetAllAssignmentsAfterSwap[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", secondAssetFirstAssignmentAfterSwap);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start and end date time.
        */
		verifyAssignmentFields(secondAssetFirstAssignmentAfterSwap, secondAssetId, orderId, orderLineId, true, true);

		// THIRD ASSET
		// Get all assignments for the third asset.
		AssetAssignmentWS[] thirdAssetAllAssignmentsAfterSwap = api.getAssetAssignmentsForAsset(thirdAssetId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", thirdAssetAllAssignmentsAfterSwap);
		AssertJUnit.assertEquals("There should be one assignment!!!", 1, thirdAssetAllAssignmentsAfterSwap.length);

		// At this point we know that there is one assignment.
		AssetAssignmentWS thirdAssetFirstAssignmentAfterSwap = thirdAssetAllAssignmentsAfterSwap[0];
		// Free not null check.
		AssertJUnit.assertNotNull("Asset Assignment instance expected!!!", thirdAssetFirstAssignmentAfterSwap);

        /*
        (This entity is auto-generated by the system so all required fields must contain values).
        Check if this assignment is for the persisted asset, and that is assigned for the specific order and order line.
        Also verify that this assignment have start and end date time.
        */
		verifyAssignmentFields(thirdAssetFirstAssignmentAfterSwap, thirdAssetId, orderId, orderLineId, true, false);

		// VERIFY BY ORDER

		// Get all the assignments for a order.
		AssetAssignmentWS[] allAssetAssignmentsByOrderAfterSwap = api.getAssetAssignmentsForOrder(orderId);
		// Check if there are any assignments.
		AssertJUnit.assertNotNull("Asset assignments array instance expected!!!", allAssetAssignmentsByOrderAfterSwap);
		AssertJUnit.assertEquals("There should be three assignments!!!", 3, allAssetAssignmentsByOrderAfterSwap.length);

		for (AssetAssignmentWS orderAssignment : allAssetAssignmentsByOrderAfterSwap){
			// verify by order and order line ids and start date time
			verifyAssignmentFields(orderAssignment, null, orderId, orderLineId, true, null);
		}


		System.out.println("Scenario 1 done.");

		System.out.println("Deleting all test entities...");
		deleteOrder(orderId);
		deleteAsset(thirdAssetId);
		deleteAsset(secondAssetId);
		deleteAsset(firstAssetId);
		deleteItem(productId);
		deleteCategory(assetManagedCategoryId);
		deleteUser(customerId);
		System.out.println("Delete complete.");

		System.out.println("test004SwapAssets completed!!!");

	}

	private void verifyAssignmentFields(AssetAssignmentWS assignment, Integer assetId,
	                                    Integer orderId, Integer orderLineId,
	                                    Boolean hasStartDateTime, Boolean hasEndDateTime){
		// ID
		Integer assignmentId = assignment.getId();
		AssertJUnit.assertNotNull("There must be auto generated id!!!", assignmentId);
		AssertJUnit.assertTrue("Assignment Id must be positive number!!!", assignmentId > Integer.valueOf(0));

		// Asset ID
		Integer assignmentAssetId = assignment.getAssetId();
		verifyAssignmentRelatedId(assignmentAssetId, assetId, "Asset Id");
		// Order ID
		Integer assignmentOrderId = assignment.getOrderId();
		verifyAssignmentRelatedId(assignmentOrderId, orderId, "Order Id");
		// Order Line ID
		Integer assignmentOrderLineId = assignment.getOrderLineId();
		verifyAssignmentRelatedId(assignmentOrderLineId, orderLineId, "Order Line Id");

		if(hasStartDateTime != null){
			// Start Date Time
			Date assignmentStartDate = assignment.getStartDatetime();
			if(hasStartDateTime){
				AssertJUnit.assertNotNull("There must be start date associated with this assignment!!!", assignmentStartDate);
			}
			else {
				AssertJUnit.assertNull("The start date time should be null!!!", assignmentStartDate);
			}
		}
		if(hasEndDateTime != null){
			// End Date Time
			Date assignmentEndDate = assignment.getEndDatetime();
			if(hasEndDateTime){
				AssertJUnit.assertNotNull("There must be end date associated with this assignment!!!", assignmentEndDate);
			}
			else {
				AssertJUnit.assertNull("The end date time should be null!!!", assignmentEndDate);
			}
		}

	}

	private void verifyAssignmentRelatedId(Integer verifiedId, Integer verifiedWithId, String entityIdName){
		AssertJUnit.assertNotNull(String.format("There must be %s associated with this assignment!!!", entityIdName), verifiedId);
		AssertJUnit.assertTrue(String.format("%s must be positive number!!!", entityIdName), verifiedId > Integer.valueOf(0));
		if(verifiedWithId != null){
			AssertJUnit.assertEquals(String.format("This assignment is not for the specified %s!!!", entityIdName), verifiedWithId, verifiedId);
		}
	}


	private Integer findDefaultStatusId(ItemTypeWS itemTypeWS){
		if(itemTypeWS != null) {
			for (AssetStatusDTOEx statusDTOEx : itemTypeWS.getAssetStatuses()) {
				if (Integer.valueOf(1) == statusDTOEx.getIsDefault()) {
					return statusDTOEx.getId();
				}
			}
		}
		// There is no default status found
		return Integer.valueOf(-1);
	}

	private ItemTypeWS createAssetManagedItemType(String description, String assetIdLabel, boolean persist) {

		System.out.println("Creating Category...");
		ItemTypeWS itemTypeWS = new ItemTypeWS(Integer.valueOf(0), description, ORDER_LINE_TYPE_ID_ITEM, ASSET_MANAGEMENT_ENABLED);
		if(assetIdLabel != null){
			itemTypeWS.setAssetIdentifierLabel(assetIdLabel);
		}
		itemTypeWS.setEntityId(PRANCING_PONY_COMPANY_ID);
		itemTypeWS.setEntities(Arrays.asList(PRANCING_PONY_COMPANY_ID));
		// Add Statuses
		Set<AssetStatusDTOEx> statuses = new HashSet<AssetStatusDTOEx>();
		statuses.add(createAssetStatus("In Stock", true, true, false, false));
		statuses.add(createAssetStatus("Sold", false, false, true, false));
		statuses.add(createAssetStatus("Reserved", false, false, false, true));
		itemTypeWS.setAssetStatuses(statuses);

		if(persist){
			Integer itemTypeWsId =  api.createItemCategory(itemTypeWS);
			itemTypeWS = api.getItemCategoryById(itemTypeWsId);
		}
		System.out.println(String.format("Category created with id:%d", itemTypeWS.getId()));
		return itemTypeWS;
	}

	private AssetStatusDTOEx createAssetStatus(String description, boolean isAvailable,
	                                           boolean isDefault, boolean isOrderSaved,
	                                           boolean isReserved) {

		AssetStatusDTOEx assetStatusDTOEx = new AssetStatusDTOEx();
		assetStatusDTOEx.setDescription(description);
		assetStatusDTOEx.setIsAvailable(isAvailable ? Integer.valueOf(1) : Integer.valueOf(0));
		assetStatusDTOEx.setIsDefault(isDefault ? Integer.valueOf(1) : Integer.valueOf(0));
		assetStatusDTOEx.setIsOrderSaved(isOrderSaved ? Integer.valueOf(1) : Integer.valueOf(0));
		return assetStatusDTOEx;
	}

	private ItemDTOEx createAssetManagedItem(
			String description, BigDecimal price, String code, Integer categoryId, boolean persist){

		System.out.println("Creating Item...");
		ItemDTOEx item = new ItemDTOEx();
		item.setEntityId(PRANCING_PONY_COMPANY_ID);
		item.setEntities(Arrays.asList(PRANCING_PONY_COMPANY_ID));
		item.setDescription(description);
		item.setPriceModelCompanyId(PRICING_MODEL_CATEGORY_ID);
		item.setPrice(price);
		item.setNumber(code);
		item.setAssetManagementEnabled(ASSET_MANAGEMENT_ENABLED);
		item.setTypes(new Integer[]{categoryId});

		if(persist){
			item.setId(api.createItem(item));
		}

		System.out.println(String.format("Item created with id:%d", item.getId()));
		return item;
	}

	private AssetWS createAsset(String identifierValue, Integer itemId, Integer defaultStatusId, boolean persist) {

		System.out.println("Creating asset...");
		AssetWS asset = new AssetWS();
		asset.setIdentifier(identifierValue);
		asset.setItemId(itemId);
		asset.setEntityId(PRANCING_PONY_COMPANY_ID);
		asset.setEntities(Arrays.asList(PRANCING_PONY_COMPANY_ID));
		asset.setAssetStatusId(defaultStatusId);
		asset.setDeleted(Integer.valueOf(0));

		if(persist){
			Integer assetId = api.createAsset(asset);
			asset = api.getAsset(assetId);

		}
		System.out.println(String.format("Asset created with id:%d",asset.getId()));
		return asset;
	}

	private OrderWS createMonthlyOrder(Integer customerId, OrderLineWS[] orderLines, boolean persist, Date changesStartDate){

		System.out.println("Creating order...");
		OrderWS order = new OrderWS();
		order.setUserId(customerId);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(ORDER_PERIOD_MONTHLY);
		order.setCurrencyId(CURRENCY_US);
		order.setActiveSince(new Date());
		order.setOrderLines(orderLines);

		if(persist) {
			OrderChangeWS changes[] = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
			if (null != changesStartDate) {
				for (OrderChangeWS change : changes) {
					change.setStartDate(changesStartDate);
				}
                if ( changesStartDate.before(order.getActiveSince()) ) {
                    order.setActiveSince(changesStartDate);
                }
			}
			Integer orderId = api.createOrder(order, changes);
			order = api.getOrder(orderId);
		}
		System.out.println(String.format("Order created with:%d", order.getId()));
		return order;
	}

	private OrderLineWS createOrderLine(BigDecimal price, Integer quantity,
	                                    String description, Integer itemId,
	                                    Integer[] assetsIds){

		OrderLineWS line = new OrderLineWS();
		line.setPrice(price);
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(quantity);
		line.setAmount(price.multiply(new BigDecimal(quantity)));
		line.setDescription(description);
		line.setItemId(itemId);
		line.setAssetIds(assetsIds);
		return line;
	}

	private OrderChangeWS createOrderChange(
			OrderLineWS orderLineWS, BigDecimal price, Integer quantity,
			Integer[] assetsIds, Date startDate){
		OrderChangeWS orderChangeWS = new OrderChangeWS();
		orderChangeWS.setQuantity(new BigDecimal(quantity));
		orderChangeWS.setPrice(price);
		orderChangeWS.setItemId(orderLineWS.getItemId());
		orderChangeWS.setOrderId(orderLineWS.getOrderId());
		orderChangeWS.setOrderLineId(orderLineWS.getId());
		orderChangeWS.setAssetIds(assetsIds);
		orderChangeWS.setStartDate(null != startDate ? startDate : Util.truncateDate(new Date()));
		orderChangeWS.setUserAssignedStatusId(ORDER_CHANGE_STATUS_APPLY_ID);
        orderChangeWS.setOrderChangeTypeId(com.sapienter.jbilling.common.Constants.ORDER_CHANGE_TYPE_DEFAULT);

        return orderChangeWS;
	}

	private void deleteOrder(Integer orderId){
		System.out.println(String.format("Deleting Order with id: %d", orderId));
		api.deleteOrder(orderId);
		System.out.println("Done.");
	}

	private void deleteAsset(Integer assetId){
		System.out.println(String.format("Deleting Asset with id: %d", assetId));
		api.deleteAsset(assetId);
		System.out.println("Done.");
	}

	private void deleteItem(Integer itemId){
		System.out.println(String.format("Deleting Item with id: %d", itemId));
		api.deleteItem(itemId);
		System.out.println("Done.");
	}

	private void deleteCategory(Integer categoryId){
		System.out.println(String.format("Deleting ItemType with id: %d", categoryId));
		api.deleteItemCategory(categoryId);
		System.out.println("Done.");
	}

	private void deleteUser(Integer userId){
		System.out.println(String.format("Deleting User with id: %d", userId));
		api.deleteUser(userId);
		System.out.println("Done.");
	}

	public static Date addDays(Date date, int days) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.DATE, days);
		date.setTime(c.getTime().getTime());
		return date;
	}

}