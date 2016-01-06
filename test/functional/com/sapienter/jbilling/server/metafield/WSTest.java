package com.sapienter.jbilling.server.metafield;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldGroupWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.metafields.validation.RangeValidationRuleModel;
import com.sapienter.jbilling.server.metafields.validation.RegExValidationRuleModel;
import com.sapienter.jbilling.server.metafields.validation.ScriptValidationRuleModel;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import static org.testng.AssertJUnit.*;

import com.sapienter.jbilling.test.ApiTestCase;
import org.testng.annotations.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Test(groups = { "web-services", "meta-fields" }, sequential = true)
public class WSTest extends ApiTestCase {

	private static final Integer SYSTEM_CURRENCY_ID = Constants.PRIMARY_CURRENCY_ID;
	private static Integer testItemTypeId;

    @BeforeClass
    public void setup(){
        if (testItemTypeId == null){
            testItemTypeId = createCategory(api,true,null);
        }
    }

    @AfterClass
    public void cleanUp() {
        if (testItemTypeId != null){
            api.deleteItemCategory(testItemTypeId);
            testItemTypeId = null;
        }
    }

	@Test
	public void test001CreateInvalidMetaField() {
		System.out.println("testCreateInvalidMetaField");

		try {
			/*
			 * Create
			 */
			MetaFieldWS metafieldWS = new MetaFieldWS();
			metafieldWS.setName("SKU2");
			metafieldWS.setEntityType(EntityType.PRODUCT);

            ValidationRuleWS rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.SCRIPT.name());
            rule.addRuleAttribute(ScriptValidationRuleModel.VALIDATION_SCRIPT_FIELD, "_this < 200 && _this > 100");
            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Please enter a SKU value between 100 and 200");

			metafieldWS.setValidationRule(rule);
			metafieldWS.setPrimary(true);
			metafieldWS.setDataType(DataType.INTEGER);
			System.out.println("Creating metafield ..." + metafieldWS);
			Integer result = api.createMetaField(metafieldWS);
			assertNotNull("The metafield was not created", result);


			System.out.println("Checking metafield ..." + result);
			metafieldWS.setId(result);
			assertNotNull("Metafield has not been created", result);
			System.out.println("Preparing item ...");
			ItemDTOEx item = createProduct();
			MetaFieldValueWS mfValue = new MetaFieldValueWS();
			mfValue.setValue(new Integer(50));
			mfValue.setDataType(metafieldWS.getDataType());
			mfValue.setFieldName(metafieldWS.getName());
			MetaFieldValueWS[] metaFields = new MetaFieldValueWS[] { mfValue };
			//item.setMetaFields(metaFields);
			// This test case was changed and meta fields now provided in a map
			item.getMetaFieldsMap().put(1, metaFields);
			try {
				System.out.println("Creating item ...");
				Integer itemId = api.createItem(item);
				fail("Item should not be created");
			} catch (Exception e) {

				assertNotNull("Exception caught:" + e, e);
			}
			// item.setId(itemId);

			item = createProduct();
			System.out.println("Created item ..." + item);
			mfValue = new MetaFieldValueWS();
			mfValue.setValue(new Integer(150));
			mfValue.setDataType(metafieldWS.getDataType());
			mfValue.setFieldName(metafieldWS.getName());
			metaFields = new MetaFieldValueWS[] { mfValue };
			item.setMetaFields(metaFields);
			Integer itemId = api.createItem(item);

			assertNotNull("The metafield was not created", result);
			api.deleteMetaField(metafieldWS.getId());

            api.deleteItem(itemId);

			System.out.println("Done!");
		} catch (Exception e) {
			fail("Exception caught:" + e);
		}
	}

	@Test
	public void test002CreateMetaFieldWithScriptValidation() {
		System.out.println("test002CreateMetaFieldWithValidation");

		try {
			/*
			 * Create
			 */
			MetaFieldWS metafieldWS = new MetaFieldWS();
			metafieldWS.setName("IP Address2");
			metafieldWS.setEntityType(EntityType.PRODUCT);

            ValidationRuleWS rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.SCRIPT.name());
            rule.addRuleAttribute(ScriptValidationRuleModel.VALIDATION_SCRIPT_FIELD,
                    "_this ==~ /^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$/");
            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Please enter a valid IP address");

			metafieldWS.setValidationRule(rule);
			metafieldWS.setPrimary(true);
			metafieldWS.setDataType(DataType.STRING);
			System.out.println("Creating metafield ..." + metafieldWS);
			Integer result = api.createMetaField(metafieldWS);
			System.out.println("Checking metafield ..." + result);
			assertNotNull("The metafield was not created", result);
			metafieldWS = api.getMetaField(result);
			assertNotNull("Metafield has not been created", metafieldWS);

			System.out.println("Creating item ...");
			ItemDTOEx item = createProduct();
			// new MetaFieldB
			MetaFieldValueWS mfValue = new MetaFieldValueWS();
			mfValue.setValue("10.10.10");
			mfValue.setDataType(metafieldWS.getDataType());
			mfValue.setFieldName(metafieldWS.getName());

			MetaFieldValueWS[] metaFields = new MetaFieldValueWS[] { mfValue };
			//item.setMetaFields(metaFields);
			item.getMetaFieldsMap().put(1, metaFields);
			
			try {

				Integer itemId = api.createItem(item);
                fail(" item should not been created :( "
						+ itemId);
			} catch (Exception e) {
				assertNotNull("Exception caught:" + e, e);
			}
			item = createProduct();

			mfValue = new MetaFieldValueWS();
			mfValue.setDataType(metafieldWS.getDataType());
			mfValue.setFieldName(metafieldWS.getName());

			mfValue.setValue("192.168.1.1");
			metaFields = new MetaFieldValueWS[] { mfValue };
			item.setMetaFields(metaFields);
			Integer itemId = api.createItem(item);

			assertNotNull("The metafield was not created", result);
			api.deleteMetaField(metafieldWS.getId());

            api.deleteItem(itemId);

			System.out.println("Done!");

		} catch (Exception e) {
			fail("Exception caught:" + e);
		}
	}

	@Test
	public void test003CreateMetaFieldWithScriptNumberValidation() {
		System.out.println("test003CreateMetaFieldWithValidation");

		try {
			MetaFieldWS metafieldWS = new MetaFieldWS();
			metafieldWS.setName("Serial Number ");
			metafieldWS.setEntityType(EntityType.PRODUCT);

            ValidationRuleWS rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.SCRIPT.name());
            rule.addRuleAttribute(ScriptValidationRuleModel.VALIDATION_SCRIPT_FIELD, "_this ==~ /[1-9][0-9]*|0/");
            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Please enter a valid Serial Number");

            metafieldWS.setValidationRule(rule);

			metafieldWS.setDataType(DataType.STRING);
			metafieldWS.setPrimary(true);
			System.out.println("Creating metafield ..." + metafieldWS);
			Integer result = api.createMetaField(metafieldWS);
			System.out.println("Created metafield:" + result);
			assertNotNull("The metafield was not created", result);
			System.out.println("Getting metafield ..." + result);
			metafieldWS = api.getMetaField(result);
			assertNotNull("Metafield has not been created", metafieldWS);

			System.out.println("creating item...");
			ItemDTOEx item = createProduct();
			// new MetaFieldB
			MetaFieldValueWS mfValue = new MetaFieldValueWS();
			mfValue.setDataType(metafieldWS.getDataType());
			mfValue.setFieldName(metafieldWS.getName());

			mfValue.setValue("123.12");
			MetaFieldValueWS[] metaFields = new MetaFieldValueWS[] { mfValue };
			//item.setMetaFields(metaFields);
			item.getMetaFieldsMap().put(1, metaFields);
			try {

				Integer itemId = api.createItem(item);
				fail("Item should not be created..." + item);
			} catch (Exception e) {

				assertNotNull("Exception caught:" + e, e);
			}

			item = createProduct();

			mfValue = new MetaFieldValueWS();
			mfValue.setDataType(metafieldWS.getDataType());
			mfValue.setFieldName(metafieldWS.getName());

			mfValue.setValue("123456");
			metaFields = new MetaFieldValueWS[] { mfValue };
			item.setMetaFields(metaFields);
			Integer itemId = api.createItem(item);

			assertNotNull("The metafield was not created", itemId);
			api.deleteMetaField(metafieldWS.getId());

            api.deleteItem(itemId);

			System.out.println("Done!");

		} catch (Exception e) {
			fail("Exception caught:" + e);
		}
	}

    @Test
    public void test004MetaFieldWithEmailValidation() {
        System.out.println("test004MetaFieldWithEmailValidation");

        try {
            MetaFieldWS metafieldWS = new MetaFieldWS();
            metafieldWS.setName("Primary Email");
            metafieldWS.setEntityType(EntityType.PRODUCT);

            ValidationRuleWS rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.EMAIL.name());
            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Please enter a valid Email Address");

            metafieldWS.setValidationRule(rule);

            metafieldWS.setDataType(DataType.STRING);
            metafieldWS.setPrimary(true);
            System.out.println("Creating metafield ..." + metafieldWS);
            Integer result = api.createMetaField(metafieldWS);
            System.out.println("Created metafield:" + result);
            assertNotNull("The metafield was not created", result);

            System.out.println("Getting metafield ..." + result);
            metafieldWS = api.getMetaField(result);
            assertNotNull("Metafield has not been created", metafieldWS);

            System.out.println("creating item...");
            ItemDTOEx item = createProduct();

            MetaFieldValueWS mfValue = new MetaFieldValueWS();
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            mfValue.setValue("admin.jbilling.com");
            MetaFieldValueWS[] metaFields = new MetaFieldValueWS[] { mfValue };
            //item.setMetaFields(metaFields);
            item.getMetaFieldsMap().put(1, metaFields);
            try {
                Integer itemId = api.createItem(item);
                fail("Item should not be created..." + item);
            } catch (Exception e) {
                assertNotNull("Exception caught:" + e, e);
            }

            item = createProduct();

            mfValue = new MetaFieldValueWS();
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            mfValue.setValue("admin@jbilling.com");
            metaFields = new MetaFieldValueWS[] { mfValue };
            item.setMetaFields(metaFields);
            Integer itemId = api.createItem(item);

            assertNotNull("The metafield was not created", itemId);
            api.deleteMetaField(metafieldWS.getId());

            api.deleteItem(itemId);

            System.out.println("Done!");

        } catch (Exception e) {
            fail("Exception caught:" + e);
        }
    }

    @Test
    public void test005MetaFieldWithRangeValidation() {

        System.out.println("test005MetaFieldWithRangeValidation");

        Integer itemId = null;
        MetaFieldWS metafieldWS = null;

        try {
            metafieldWS = new MetaFieldWS();
            metafieldWS.setName("Range Number ");
            metafieldWS.setEntityType(EntityType.PRODUCT);

            // 1. Test range validation min/max 2 < value < 10
            ValidationRuleWS rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.RANGE.name());
            rule.addRuleAttribute(RangeValidationRuleModel.VALIDATION_MIN_RANGE_FIELD, "2.0");
            rule.addRuleAttribute(RangeValidationRuleModel.VALIDATION_MAX_RANGE_FIELD, "10.0");

            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Please enter a value between 2 and 10");

            metafieldWS.setValidationRule(rule);

            metafieldWS.setDataType(DataType.DECIMAL);
            metafieldWS.setPrimary(true);
            System.out.println("Creating metafield ..." + metafieldWS);
            Integer result = api.createMetaField(metafieldWS);
            System.out.println("Created metafield:" + result);
            assertNotNull("The metafield was not created", result);

            System.out.println("Getting metafield ..." + result);
            metafieldWS = api.getMetaField(result);
            assertNotNull("Metafield has not been created", metafieldWS);

            System.out.println("creating item 1...");
            ItemDTOEx item = createProduct();

            MetaFieldValueWS mfValue = new MetaFieldValueWS();
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            mfValue.setValue(BigDecimal.valueOf(1.0));
            MetaFieldValueWS[] metaFields = new MetaFieldValueWS[] { mfValue };
            //item.setMetaFields(metaFields);
            item.getMetaFieldsMap().put(1, metaFields);
            try {

                itemId = api.createItem(item);
                fail("Item should not be created..." + item);
            } catch (Exception e) {

                assertNotNull("Exception caught:" + e, e);
            }

            item = createProduct();

            mfValue = new MetaFieldValueWS();
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            mfValue.setValue(BigDecimal.valueOf(5.0));
            metaFields = new MetaFieldValueWS[] { mfValue };
            item.setMetaFields(metaFields);
            itemId = api.createItem(item);

            assertNotNull("The metafield was not created", itemId);
            api.deleteItem(itemId);

            // 2. Test range validation min > 2
            metafieldWS = api.getMetaField(result);
            rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.RANGE.name());
            rule.addRuleAttribute(RangeValidationRuleModel.VALIDATION_MIN_RANGE_FIELD, "2.0");

            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Please enter a value greater than 2");
            metafieldWS.setValidationRule(rule);

            api.updateMetaField(metafieldWS);

            // create product with invalid/valid metafield value
            System.out.println("creating item 2...");
            item = createProduct();

            mfValue = new MetaFieldValueWS();
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            mfValue.setValue(BigDecimal.valueOf(1.0));
            metaFields = new MetaFieldValueWS[] { mfValue };
            //item.setMetaFields(metaFields);
            item.getMetaFieldsMap().put(1, metaFields);
            try {

                itemId = api.createItem(item);
                fail("Item should not be created..." + item);
            } catch (Exception e) {

                assertNotNull("Exception caught:" + e, e);
            }

            item = createProduct();

            mfValue = new MetaFieldValueWS();
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            mfValue.setValue(BigDecimal.valueOf(15.0));
            metaFields = new MetaFieldValueWS[] { mfValue };
            item.setMetaFields(metaFields);
            itemId = api.createItem(item);

            assertNotNull("The metafield was not created", itemId);
            api.deleteItem(itemId);

            // 3. Test range validation max < 10
            metafieldWS = api.getMetaField(result);
            rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.RANGE.name());
            rule.addRuleAttribute(RangeValidationRuleModel.VALIDATION_MAX_RANGE_FIELD, "10.0");

            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Please enter a value less than 10");
            metafieldWS.setValidationRule(rule);

            api.updateMetaField(metafieldWS);

            // create product with invalid/valid metafield value
            System.out.println("creating item 3...");
            item = createProduct();

            mfValue = new MetaFieldValueWS();
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            mfValue.setValue(BigDecimal.valueOf(22.0));
            metaFields = new MetaFieldValueWS[] { mfValue };
            //item.setMetaFields(metaFields);
            item.getMetaFieldsMap().put(1, metaFields);
            try {

                itemId = api.createItem(item);
                fail("Item should not be created..." + item);
            } catch (Exception e) {

                assertNotNull("Exception caught:" + e, e);
            }

            item = createProduct();

            mfValue = new MetaFieldValueWS();
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            mfValue.setValue(BigDecimal.valueOf(1.0));
            metaFields = new MetaFieldValueWS[] { mfValue };
            item.setMetaFields(metaFields);
            itemId = api.createItem(item);

            assertNotNull("The metafield was not created", itemId);
            api.deleteItem(itemId);


            // 4. verify no validation ranges
            metafieldWS = api.getMetaField(result);
            rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.RANGE.name());

            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Please enter a value: ");
            metafieldWS.setValidationRule(rule);

            api.updateMetaField(metafieldWS);

            // create product with invalid/valid metafield value
            System.out.println("creating item 4...");
            item = createProduct();

            mfValue = new MetaFieldValueWS();
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            mfValue.setValue(BigDecimal.valueOf(1.0));
            metaFields = new MetaFieldValueWS[] { mfValue };
            item.setMetaFields(metaFields);
            itemId = api.createItem(item);

            assertNotNull("The metafield was not created", itemId);

            System.out.println("Done!");

        } catch (Exception e) {
            fail("Exception caught:" + e);
        } finally {
            if (itemId != null && api != null) {
                api.deleteItem(itemId);
            }

            if (metafieldWS != null && metafieldWS.getId() != 0 && api != null) {
                api.deleteMetaField(metafieldWS.getId());
            }
        }
    }

    @Test
    public void test006MetaFieldWithRegExValidation() {
        System.out.println("test006MetaFieldWithRegExValidation");

        Integer itemId = null;
        MetaFieldWS metafieldWS = null;

        try {

            metafieldWS = new MetaFieldWS();
            metafieldWS.setName("Password");
            metafieldWS.setEntityType(EntityType.PRODUCT);

            ValidationRuleWS rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.REGEX.name());
            rule.addRuleAttribute(RegExValidationRuleModel.VALIDATION_REG_EX_FIELD, "^[a-z0-9_-]{6,18}$");
            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Please enter a valid password: ");

            metafieldWS.setValidationRule(rule);

            metafieldWS.setDataType(DataType.STRING);
            metafieldWS.setPrimary(true);
            System.out.println("Creating metafield ..." + metafieldWS);
            Integer result = api.createMetaField(metafieldWS);
            System.out.println("Created metafield:" + result);
            assertNotNull("The metafield was not created", result);

            System.out.println("Getting metafield ..." + result);
            metafieldWS = api.getMetaField(result);
            assertNotNull("Metafield has not been created", metafieldWS);

            System.out.println("creating item...");
            ItemDTOEx item = createProduct();

            MetaFieldValueWS mfValue = new MetaFieldValueWS();
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            mfValue.setValue("mypa$$w0rd");
            MetaFieldValueWS[] metaFields = new MetaFieldValueWS[] { mfValue };
            //item.setMetaFields(metaFields);
            item.getMetaFieldsMap().put(1, metaFields);
            try {

                itemId = api.createItem(item);
                fail("Item should not be created..." + item);
            } catch (Exception e) {

                assertNotNull("Exception caught:" + e, e);
            }

            item = createProduct();

            mfValue = new MetaFieldValueWS();
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            mfValue.setValue("myp4ssw0rd");
            metaFields = new MetaFieldValueWS[] { mfValue };
            item.setMetaFields(metaFields);
            itemId = api.createItem(item);

            assertNotNull("The metafield was not created", itemId);

            System.out.println("Done!");

        } catch (Exception e) {
            fail("Exception caught:" + e);
        } finally {
            if (itemId != null && api != null) {
                api.deleteItem(itemId);
            }

            if (metafieldWS != null && metafieldWS.getId() != 0 && api != null) {
                api.deleteMetaField(metafieldWS.getId());
            }
        }
    }

    @Test
    public void test007MetaFieldCRUDValidation() {
        System.out.println("test007MetaFieldCRUDValidation");

        MetaFieldWS retrievedMetafieldWS = null;

        try {

            MetaFieldWS metafieldWS = new MetaFieldWS();
            metafieldWS.setName("Billing Email");
            metafieldWS.setEntityType(EntityType.PRODUCT);

            metafieldWS.setDataType(DataType.STRING);
            metafieldWS.setPrimary(true);
            System.out.println("Creating metafield ..." + metafieldWS);
            Integer result = api.createMetaField(metafieldWS);
            System.out.println("Created metafield:" + result);
            assertNotNull("The metafield was not created", result);

            System.out.println("Getting metafield ..." + result);
            retrievedMetafieldWS = api.getMetaField(result);
            assertNotNull("Metafield has not been created", metafieldWS);

            matchMetaField(metafieldWS, retrievedMetafieldWS);

            ValidationRuleWS rule = new ValidationRuleWS();
            rule.setRuleType(ValidationRuleType.EMAIL.name());
            rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID,
                    "Please enter a valid billing email: ");

            retrievedMetafieldWS.setValidationRule(rule);

            api.updateMetaField(retrievedMetafieldWS);
            System.out.println("Updated metafield:" + retrievedMetafieldWS);

            System.out.println("Getting metafield ..." + result);
            MetaFieldWS secondRetrievedMetafieldWS = api.getMetaField(result);
            assertNotNull("Metafield has not been updated", secondRetrievedMetafieldWS);

            matchMetaField(retrievedMetafieldWS, secondRetrievedMetafieldWS);

            secondRetrievedMetafieldWS.setValidationRule(null);
            api.updateMetaField(secondRetrievedMetafieldWS);
            System.out.println("Updated metafield:" + retrievedMetafieldWS);

            System.out.println("Getting metafield ..." + result);
            MetaFieldWS thirdRetrievedMetafieldWS = api.getMetaField(result);
            assertNotNull("Metafield has not been updated", thirdRetrievedMetafieldWS);

            matchMetaField(secondRetrievedMetafieldWS, thirdRetrievedMetafieldWS);

            System.out.println("Done!");

        } catch (Exception e) {
            fail("Exception caught:" + e);
        } finally {

            if (retrievedMetafieldWS != null && retrievedMetafieldWS.getId() != 0 && api != null) {
                api.deleteMetaField(retrievedMetafieldWS.getId());
            }
        }
    }

    @Test
    public void test008MetaFieldGroupCRUD() throws Exception {
        MetaFieldGroupWS ws = new MetaFieldGroupWS();
        ws.setDisplayOrder(1);
        ws.setEntityId(1);
        ws.setEntityType(EntityType.ASSET);
        ws.setName("name01");

        MetaFieldWS mf = new MetaFieldWS();
        mf.setDataType(DataType.STRING);
        mf.setDisplayOrder(1);
        mf.setEntityId(1);
        mf.setEntityType(EntityType.ASSET);
        mf.setMandatory(false);
        mf.setName("mfname");
        mf.setPrimary(true);
        mf.setId(api.createMetaField(mf));

        ws.setMetaFields(new MetaFieldWS[]{mf});
        ws.setId(api.createMetaFieldGroup(ws) );

        Integer gId = ws.getId();

        ws = api.getMetaFieldGroup(gId);
        assertEquals(new Integer(1), ws.getDisplayOrder());
        assertEquals(new Integer(1), ws.getEntityId());
        assertEquals(EntityType.ASSET, ws.getEntityType());
        assertEquals("name01", ws.getDescription());

        ws.setDisplayOrder(2);
        api.updateMetaFieldGroup(ws);

        ws = api.getMetaFieldGroup(gId);
        assertEquals(new Integer(2), ws.getDisplayOrder());
        assertEquals(new Integer(1), ws.getEntityId());
        assertEquals(EntityType.ASSET, ws.getEntityType());
        assertEquals("name01", ws.getDescription());

        api.deleteMetaFieldGroup(gId);
        api.deleteMetaField(mf.getId());
    }

    @Test
    public void test009getMetaFieldsAndGroupsForEntityType() throws Exception {
        MetaFieldGroupWS ws = new MetaFieldGroupWS();
        ws.setDisplayOrder(1);
        ws.setEntityId(1);
        ws.setEntityType(EntityType.ASSET);
        ws.setName("name01");

        MetaFieldWS mf = new MetaFieldWS();
        mf.setDataType(DataType.STRING);
        mf.setDisplayOrder(1);
        mf.setEntityId(1);
        mf.setEntityType(EntityType.ASSET);
        mf.setMandatory(false);
        mf.setName("mfname");
        mf.setPrimary(true);
        mf.setId(api.createMetaField(mf));

        ws.setMetaFields(new MetaFieldWS[]{mf});
        ws.setId(api.createMetaFieldGroup(ws) );

        MetaFieldGroupWS[] groups = api.getMetaFieldGroupsForEntity(EntityType.ASSET.name());
        boolean found = false;
        for(MetaFieldGroupWS groupWS : groups) {
            if(ws.getDescription().equals(groupWS.getDescription()) && ws.getId() == groupWS.getId()) {
                found = true;
                break;
            }
        }
        assertTrue("MetaField Group not found", found);

        MetaFieldWS[] metaFields = api.getMetaFieldsForEntity(EntityType.ASSET.name());
        found = false;
        for(MetaFieldWS metaFieldWS : metaFields) {
            if(mf.getName().equals(metaFieldWS.getName()) && mf.getId() == metaFieldWS.getId()) {
                found = true;
                break;
            }
        }
        assertTrue("MetaField not found", found);
        api.deleteMetaFieldGroup(ws.getId());
        api.deleteMetaField(mf.getId());
    }

    @Test
    public void test010createMetaFieldsForCategory() throws Exception {
        try{

            MetaFieldWS metafieldWS = createMetaField("TestRootMetaField2", 1, EntityType.PRODUCT_CATEGORY);
            System.out.println("Creating metafield ..." + metafieldWS);
            Integer result = api.createMetaField(metafieldWS);
            System.out.println("Created metafield ..." + result);
            metafieldWS = api.getMetaField(result);
            assertNotNull("Metafield has not been created", metafieldWS);

            System.out.println("Creating Category ...");
            ItemTypeWS itemType = createCategory(true);
            // new MetaFieldB
            MetaFieldValueWS mfValue = new MetaFieldValueWS();
            mfValue.setValue("Test value");
            mfValue.setDataType(metafieldWS.getDataType());
            mfValue.setFieldName(metafieldWS.getName());

            MetaFieldValueWS[] metaFields = new MetaFieldValueWS[] { mfValue };
            //item.setMetaFields(metaFields);
            itemType.setMetaFields(metaFields);

            Integer itemTypeId = null;
            try {
                itemTypeId = api.createItemCategory(itemType);
            } catch (Exception e) {
                fail("Failed to create Category: " + e);
            }
            assertNotNull(itemTypeId);

            api.deleteMetaField(metafieldWS.getId());

            api.deleteItemCategory(itemTypeId);

        }catch(Exception e){
            fail("Failed: "+e);
        }
    }

    @Test
    public void test011createGlobalCategoryWithMetaFieldsForRootAndChildCompanies() throws Exception {
        try{
            /*
            * create root company meta-field
            * */
            MetaFieldWS metaFieldWSRoot = createMetaField("TestRootMetaField1 NonGlobalCategory", 1, EntityType.PRODUCT_CATEGORY);
            System.out.println("Creating root meta-field ..." + metaFieldWSRoot);
            Integer resultRoot = api.createMetaField(metaFieldWSRoot);
            System.out.println("Created root meta-field ..." + resultRoot);
            metaFieldWSRoot = api.getMetaField(resultRoot);
            assertNotNull("Metafield has not been created", metaFieldWSRoot);

            /*
            * create child company meta-field
            * */
            MetaFieldWS metaFieldWSChild = createMetaField("TestChildMetaField2 NonGlobalCategory", 3, EntityType.PRODUCT_CATEGORY);
            System.out.println("Creating child meta-field ..." + metaFieldWSChild);
            Integer resultChild = api.createMetaField(metaFieldWSChild);
            System.out.println("Created child meta-field ..." + resultChild);
            metaFieldWSChild = api.getMetaField(resultChild);
            assertNotNull("Metafield has not been created", metaFieldWSChild);

            System.out.println("Creating global Category ...");
            ItemTypeWS itemTypeGlobal = createCategory(true, 1);

            MetaFieldValueWS mfGlobalValue = getMetaFieldValue("Test root value", metaFieldWSRoot.getDataType(), metaFieldWSRoot.getName());

            // add root meta-field value to the category
            MetaFieldValueWS[] globalMetaFields = new MetaFieldValueWS[] { mfGlobalValue };

            itemTypeGlobal.setMetaFields(globalMetaFields);
            Integer itemTypeGlobalId = null;
            try {
                itemTypeGlobalId = api.createItemCategory(itemTypeGlobal);
            } catch (Exception e) {
                fail("Failed to create Category: " + e);
            }
            itemTypeGlobal = getItemCategory(itemTypeGlobal.getDescription(),  Arrays.asList(api.getAllItemCategoriesByEntityId(1)));
            assertNotNull(itemTypeGlobalId);
            assertNotNull(itemTypeGlobal);

            MetaFieldValueWS mfChildValue = getMetaFieldValue("Test child value", metaFieldWSChild.getDataType(), metaFieldWSChild.getName());

            // add child meta-field value along with the previous root one
            MetaFieldValueWS[] metaFields = new MetaFieldValueWS[] { mfGlobalValue, mfChildValue };

            itemTypeGlobal.setMetaFields(metaFields);
            try {
                api.updateItemCategory(itemTypeGlobal);
            } catch (Exception e) {
                fail("Failed to update Category: " + e);
            }
            itemTypeGlobal = getItemCategory(itemTypeGlobal.getDescription(), Arrays.asList(api.getAllItemCategoriesByEntityId(1)));

            assertNotNull(itemTypeGlobalId);
            assertNotNull(itemTypeGlobal);

            api.deleteMetaField(metaFieldWSRoot.getId());
            api.deleteMetaField(metaFieldWSChild.getId());

            api.deleteItemCategory(itemTypeGlobalId);

        }catch(Exception e){
            fail("Failed: "+e);
        }
    }

    @Test
    public void test012createNonGlobalCategoryWithMetaFieldsForRootAndChildCompanies() throws Exception {
        try{
            /*
            * create root company meta-field
            * */
            MetaFieldWS metaFieldWSRoot = createMetaField("TestRootMetaField2", 1, EntityType.PRODUCT_CATEGORY);
            System.out.println("Creating root meta-field ..." + metaFieldWSRoot);
            Integer resultRoot = api.createMetaField(metaFieldWSRoot);
            System.out.println("Created root meta-field ..." + resultRoot);
            metaFieldWSRoot = api.getMetaField(resultRoot);
            assertNotNull("Metafield has not been created", metaFieldWSRoot);

            /*
            * create child company meta-field
            * */
            MetaFieldWS metaFieldWSChild = createMetaField("TestChildMetaField1", 3, EntityType.PRODUCT_CATEGORY);
            System.out.println("Creating child meta-field ..." + metaFieldWSChild);
            Integer resultChild = api.createMetaField(metaFieldWSChild);
            System.out.println("Created child meta-field ..." + resultChild);
            metaFieldWSChild = api.getMetaField(resultChild);
            assertNotNull("Metafield has not been created", metaFieldWSChild);

            System.out.println("Creating global Category ...");
            ItemTypeWS itemTypeNonGlobal = createCategory(false, 1);
            itemTypeNonGlobal.getEntities().add(1);
            itemTypeNonGlobal.getEntities().add(3);

            MetaFieldValueWS mfGlobalValue = getMetaFieldValue("Test root value", metaFieldWSRoot.getDataType(), metaFieldWSRoot.getName());

            // add root meta-field value to the category
            MetaFieldValueWS[] globalMetaFields = new MetaFieldValueWS[] { mfGlobalValue };

            itemTypeNonGlobal.setMetaFields(globalMetaFields);
            Integer itemTypeGlobalId = null;
            try {
                itemTypeGlobalId = api.createItemCategory(itemTypeNonGlobal);
            } catch (Exception e) {
                fail("Failed to create Category: " + e);
            }
            itemTypeNonGlobal = getItemCategory(itemTypeNonGlobal.getDescription(), Arrays.asList(api.getAllItemCategoriesByEntityId(1)));
            assertNotNull(itemTypeGlobalId);
            assertNotNull(itemTypeNonGlobal);

            MetaFieldValueWS mfChildValue = getMetaFieldValue("Test child value", metaFieldWSChild.getDataType(), metaFieldWSChild.getName());

            // add child meta-field value along with the previous root one
            MetaFieldValueWS[] metaFields = new MetaFieldValueWS[] { mfGlobalValue, mfChildValue };

            itemTypeNonGlobal.setMetaFields(metaFields);
            try {
                api.updateItemCategory(itemTypeNonGlobal);
            } catch (Exception e) {
                fail("Failed to update Category: " + e);
            }
            itemTypeNonGlobal = getItemCategory(itemTypeNonGlobal.getDescription(), Arrays.asList(api.getAllItemCategoriesByEntityId(1)));

            assertNotNull(itemTypeGlobalId);
            assertNotNull(itemTypeNonGlobal);

            api.deleteMetaField(metaFieldWSRoot.getId());
            api.deleteMetaField(metaFieldWSChild.getId());

            api.deleteItemCategory(itemTypeGlobalId);

        }catch(Exception e){
            fail("Failed: "+e);
        }
    }

    @Test
    public void test013createGlobalItemWithMetaFieldsForRootAndChildCompanies() throws Exception {
        try{
            /*
            * create root company meta-field
            * */
            MetaFieldWS metaFieldWSRoot = createMetaField("TestRootMetaField2", 1, EntityType.PRODUCT);
            System.out.println("Creating root meta-field ..." + metaFieldWSRoot);
            Integer resultRoot = api.createMetaField(metaFieldWSRoot);
            System.out.println("Created root meta-field ..." + resultRoot);
            metaFieldWSRoot = api.getMetaField(resultRoot);
            assertNotNull("Metafield has not been created", metaFieldWSRoot);

            /*
            * create child company meta-field
            * */
            MetaFieldWS metaFieldWSChild = createMetaField("TestChildMetaField1", 3, EntityType.PRODUCT);
            System.out.println("Creating child meta-field ..." + metaFieldWSChild);
            Integer resultChild = api.createMetaField(metaFieldWSChild);
            System.out.println("Created child meta-field ..." + resultChild);
            metaFieldWSChild = api.getMetaField(resultChild);
            assertNotNull("Metafield has not been created", metaFieldWSChild);

            System.out.println("Creating global product ...");
            ItemDTOEx item = createProduct();
            item.setGlobal(true);

            MetaFieldValueWS mfGlobalValue = getMetaFieldValue("Test root value", metaFieldWSRoot.getDataType(), metaFieldWSRoot.getName());

            // add root meta-field value to the category
            MetaFieldValueWS[] globalMetaFields = new MetaFieldValueWS[] { mfGlobalValue };

            item.setMetaFields(globalMetaFields);
            Integer itemId = null;
            try {
                itemId = api.createItem(item);
            } catch (Exception e) {
                fail("Failed to create Category: " + e);
            }
            item = getItem(item.getNumber(), Arrays.asList(api.getAllItemsByEntityId(1)));
            assertNotNull(itemId);
            assertNotNull(item);

            MetaFieldValueWS mfChildValue = getMetaFieldValue("Test child value", metaFieldWSChild.getDataType(), metaFieldWSChild.getName());

            // add child meta-field value along with the previous root one
            MetaFieldValueWS[] metaFields = new MetaFieldValueWS[] { mfGlobalValue, mfChildValue };

            item.setMetaFields(metaFields);
            try {
                api.updateItem(item);
            } catch (Exception e) {
                fail("Failed to update Category: " + e);
            }
            item = getItem(item.getNumber(), Arrays.asList(api.getAllItemsByEntityId(1)));
            assertNotNull(itemId);
            assertNotNull(item);

            api.deleteMetaField(metaFieldWSRoot.getId());
            api.deleteMetaField(metaFieldWSChild.getId());

            api.deleteItem(itemId);

        }catch(Exception e){
            fail("Failed: "+e);
        }
    }

    @Test
    public void test014createNonGlobalItemWithMetaFieldsForRootAndChildCompanies() throws Exception {
        try{
            /*
            * create root company meta-field
            * */
            MetaFieldWS metaFieldWSRoot = createMetaField("TestRootMetaField2", 1, EntityType.PRODUCT);
            System.out.println("Creating root meta-field ..." + metaFieldWSRoot);
            Integer resultRoot = api.createMetaField(metaFieldWSRoot);
            System.out.println("Created root meta-field ..." + resultRoot);
            metaFieldWSRoot = api.getMetaField(resultRoot);
            assertNotNull("Metafield has not been created", metaFieldWSRoot);

            /*
            * create child company meta-field
            * */
            MetaFieldWS metaFieldWSChild = createMetaField("TestChildMetaField1", 3, EntityType.PRODUCT);
            System.out.println("Creating child meta-field ..." + metaFieldWSChild);
            Integer resultChild = api.createMetaField(metaFieldWSChild);
            System.out.println("Created child meta-field ..." + resultChild);
            metaFieldWSChild = api.getMetaField(resultChild);
            assertNotNull("Metafield has not been created", metaFieldWSChild);

            System.out.println("Creating global product ...");
            ItemDTOEx item = createProduct();
            item.setGlobal(false);
            item.setEntityId(1);
            item.getEntities().add(1);
            item.getEntities().add(3);

            MetaFieldValueWS mfGlobalValue = getMetaFieldValue("Test root value", metaFieldWSRoot.getDataType(), metaFieldWSRoot.getName());

            // add root meta-field value to the category
            MetaFieldValueWS[] globalMetaFields = new MetaFieldValueWS[] { mfGlobalValue };

            item.setMetaFields(globalMetaFields);
            Integer itemId = null;
            try {
                itemId = api.createItem(item);
            } catch (Exception e) {
                fail("Failed to create Product: " + e);
            }
            item = getItem(item.getNumber(), Arrays.asList(api.getAllItemsByEntityId(1)));
            assertNotNull(itemId);
            assertNotNull(item);

            MetaFieldValueWS mfChildValue = getMetaFieldValue("Test child value", metaFieldWSChild.getDataType(), metaFieldWSChild.getName());

            // add child meta-field value along with the previous root one
            MetaFieldValueWS[] metaFields = new MetaFieldValueWS[] { mfGlobalValue, mfChildValue };

            item.setMetaFields(metaFields);
            try {
                api.updateItem(item);
            } catch (Exception e) {
                fail("Failed to update Product: " + e);
            }
            item = getItem(item.getNumber(), Arrays.asList(api.getAllItemsByEntityId(1)));
            assertNotNull(itemId);
            assertNotNull(item);

            api.deleteMetaField(metaFieldWSRoot.getId());
            api.deleteMetaField(metaFieldWSChild.getId());

            api.deleteItem(itemId);

        }catch(Exception e){
            fail("Failed: "+e);
        }
    }

	private ItemDTOEx createProduct() {
		ItemDTOEx item = new ItemDTOEx();
		item.setCurrencyId(SYSTEM_CURRENCY_ID);
		item.setPrice("10");
		item.setDescription("Test Item for meta field validation3");
		item.setEntityId(1);
		item.setNumber("Number" + System.currentTimeMillis());
		item.setTypes(new Integer[] {testItemTypeId});
		return item;
	}

    private ItemTypeWS createCategory(String desc, Boolean global, Integer entityId) {
        ItemTypeWS itemType = new ItemTypeWS();

        itemType.setEntityId((entityId != null) ? entityId : 1);
        if(desc != null && !desc.isEmpty()){
            itemType.setDescription(desc);
        }else{
            itemType.setDescription("Test RootMetaField");
        }
        itemType.setGlobal(global);
        itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);

		return itemType;
	}

    private ItemTypeWS createCategory(Boolean global, Integer entityId) {
        return createCategory( "", global, entityId);
    }

    private ItemTypeWS createCategory(Boolean global) {
        return createCategory("", global, null);
    }

    private Integer createCategory(JbillingAPI api, Boolean global, Integer entityId) {
        ItemTypeWS itemTypeWS =  createCategory("Test Category",global, entityId);
        return api.createItemCategory(itemTypeWS);
    }

    private MetaFieldWS createMetaField(String name, Integer entityId, EntityType type){
        MetaFieldWS metaFieldWSRoot = new MetaFieldWS();

        metaFieldWSRoot.setName(name);
        metaFieldWSRoot.setEntityType(type);
        metaFieldWSRoot.setPrimary(true);
        metaFieldWSRoot.setDataType(DataType.STRING);
        metaFieldWSRoot.setEntityId(entityId);

        return metaFieldWSRoot;
    }

    private ItemTypeWS getItemCategory(String desc, List<ItemTypeWS> categories){

        for(ItemTypeWS itemTypeWS: categories){
            if (itemTypeWS.getDescription().equals(desc)){
                return itemTypeWS;
            }
        }
        return null;
    }

    private ItemDTOEx getItem(String number, List<ItemDTOEx> items){

        for(ItemDTOEx item: items){
            if (item.getNumber().equals(number)){
                return item;
            }
        }
        return null;
    }

    private MetaFieldValueWS getMetaFieldValue(Object value, DataType type, String name){
        MetaFieldValueWS mfChildValue = new MetaFieldValueWS();

        mfChildValue.setValue(value);
        mfChildValue.setDataType(type);
        mfChildValue.setFieldName(name);

        return mfChildValue;
    }

    private void matchMetaField(MetaFieldWS mf, MetaFieldWS retrievedMf) {

        assertEquals(mf.getName(), retrievedMf.getName());

        if (mf.getFieldUsage() != null && retrievedMf.getFieldUsage() != null) {
            assertEquals(mf.getFieldUsage(), retrievedMf.getFieldUsage());
        } else if (mf.getFieldUsage() == null ^ retrievedMf.getFieldUsage() == null) {
            fail("Field usage is: " + mf.getFieldUsage() + " and retrieved field usage is: " + retrievedMf.getFieldUsage());
        }

        if (mf.getValidationRule() != null && retrievedMf.getValidationRule() != null) {
            matchValidationRule(mf.getValidationRule(), retrievedMf.getValidationRule());
        } else if (mf.getValidationRule() == null ^ retrievedMf.getValidationRule() == null) {
            fail("Validation rule is: " + mf.getValidationRule() + " and retrieved validation rule is: " + retrievedMf.getValidationRule());
        }
        assertEquals(mf.getDataType(), retrievedMf.getDataType());
        assertEquals(mf.getDefaultValue(), retrievedMf.getDefaultValue());
        assertEquals(mf.getDisplayOrder(), retrievedMf.getDisplayOrder());
    }

    private void matchValidationRule(ValidationRuleWS validationRule, ValidationRuleWS retrievedRule) {
        assertTrue(validationRule != null && retrievedRule != null);
        assertEquals(validationRule.getRuleType(), retrievedRule.getRuleType());
        assertEquals(validationRule.getErrorMessages().size(), retrievedRule.getErrorMessages().size());
        assertEquals(validationRule.getRuleAttributes(), retrievedRule.getRuleAttributes());
    }
}
