package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.metafields.validation.RangeValidationRuleModel;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Created by vojislav on 14.1.15.
 */
@Test(groups = {"web-services", "util"}, testName = "enumerations.WSTest")
public class WSTest {

    private static final Integer ROOT_ENTITY_ID = Integer.valueOf(1);
    private static final Integer PERSISTED_ENUMERATION_ID = Integer.valueOf(15);
    private static final String PERSISTED_ENUMERATION_NAME = "Sales Type Code";
    private static final String PERSISTED_ENUMERATION_VALUE_1 = "R";
    private static final String PERSISTED_ENUMERATION_VALUE_2 = "B";
    private static final Integer MAX = Integer.valueOf(10);
    private static final Integer OFFSET = Integer.valueOf(0);

    private JbillingAPI api;

    @BeforeClass
    public void initializeApi(){
        try {
            api = JbillingAPIFactory.getAPI();
        }catch (Exception e) {
            fail("API initialization failed!!");
        }
    }

    @AfterClass
    public void tearDown(){
        if(null != api){
            api = null;
        }
    }

    @Test
    public void test001GetEnumeration(){
        System.out.println("#test001GetEnumeration");

        // Get the loaded enumeration
        EnumerationWS persistedEnumerationWS = api.getEnumeration(PERSISTED_ENUMERATION_ID);

        // Validate the enumeration and its properties
        validatePersistedEnumeration(persistedEnumerationWS);
        // Compare "identification" values

        // ID
        assertEquals("Ids representing the same enumeration entity can not differ!!", PERSISTED_ENUMERATION_ID, persistedEnumerationWS.getId());
        // Name
        assertNotNull("Persisted Enumeration must have a name!!", persistedEnumerationWS.getName());
        assertEquals("Names representing the same enumeration can not differ!!", PERSISTED_ENUMERATION_NAME, persistedEnumerationWS.getName());
        // Entity ID
        assertEquals("Same enumeration entity can not be in different companies!!!", ROOT_ENTITY_ID, persistedEnumerationWS.getEntityId());

        // In this case we know that there are 2 values.
        List<EnumerationValueWS> WSvalues = persistedEnumerationWS.getValues();
        // Validate list
        assertNotNull("Values list can not be null!", WSvalues);
        assertTrue("The size of the values list can not be 0!!", WSvalues.size() > Integer.valueOf(0));
        assertEquals("The size of value list should be 2!!", Integer.valueOf(2), Integer.valueOf(WSvalues.size()));
        // Validate first value
        validateEnumerationValueWS(WSvalues.get(0));
        // Verify first enumeration value
        assertEquals("Value differs!!", PERSISTED_ENUMERATION_VALUE_1, WSvalues.get(0).getValue());
        // Validate second value
        validateEnumerationValueWS(WSvalues.get(1));
        // Verify second enumeration value
        assertEquals("Value differs!!", PERSISTED_ENUMERATION_VALUE_2, WSvalues.get(1).getValue());
        System.out.println("#test001GetEnumeration done!");
    }

    @Test
    public void test002GetEnumerationByName(){
        System.out.println("#test002GetEnumerationByName");

        // Get the loaded enumeration
        EnumerationWS persistedEnumerationWS = api.getEnumerationByName(PERSISTED_ENUMERATION_NAME);

        // Validate the enumeration and its properties
        validatePersistedEnumeration(persistedEnumerationWS);
        // Compare "identification" values

        // ID
        assertEquals("Ids representing the same enumeration entity can not differ!!", PERSISTED_ENUMERATION_ID, persistedEnumerationWS.getId());
        // Name
        assertNotNull("Persisted Enumeration must have a name!!", persistedEnumerationWS.getName());
        assertEquals("Names representing the same enumeration can not differ!!", PERSISTED_ENUMERATION_NAME, persistedEnumerationWS.getName());
        // Entity ID
        assertEquals("Same enumeration entity can not be in different companies!!!", ROOT_ENTITY_ID, persistedEnumerationWS.getEntityId());

        // In this case we know that there are 2 values.
        List<EnumerationValueWS> WSvalues = persistedEnumerationWS.getValues();
        // Validate list
        assertNotNull("Values list can not be null!", WSvalues);
        assertTrue("The size of the values list can not be 0!!", WSvalues.size() > Integer.valueOf(0));
        assertEquals("The size of value list should be 2!!", Integer.valueOf(2), Integer.valueOf(WSvalues.size()));
        // Validate first value
        validateEnumerationValueWS(WSvalues.get(0));
        // Verify first enumeration value
        assertEquals("Value differs!!", PERSISTED_ENUMERATION_VALUE_1, WSvalues.get(0).getValue());
        // Validate second value
        validateEnumerationValueWS(WSvalues.get(1));
        // Verify second enumeration value
        assertEquals("Value differs!!", PERSISTED_ENUMERATION_VALUE_2, WSvalues.get(1).getValue());
        System.out.println("#test002GetEnumerationByName done!");
    }

    @Test
    public void test003GetAllEnumerations(){
        System.out.println("#test003GetAllEnumerations");

        // Get all initial enumerations
        List<EnumerationWS> allEnumerations = api.getAllEnumerations(MAX, OFFSET);
        // Validate the size
        assertNotNull("There should be persisted enumerations!!", allEnumerations);
        assertEquals("Size of the enumeration list not 10!", Integer.valueOf(10), Integer.valueOf(allEnumerations.size()));

        for (EnumerationWS enumerationWS : allEnumerations){
            // Validate each persisted enumeration.
            validatePersistedEnumeration(enumerationWS);
        }
        System.out.println("#test003GetAllEnumerations done!");
    }

    @Test
    public void test004GetAllEnumerationsCount(){
        System.out.println("#test004GetAllEnumerationsCount");

        // Get the count of all persisted enumerations
        Long count = api.getAllEnumerationsCount();
        assertNotNull("There should be a number for a persisted enumerations!", count);
        assertTrue("Number of the persisted enumerations is 0!", count > Long.valueOf(0L));

        System.out.println("#test004GetAllEnumerationsCount done.");
    }

    @Test
    public void test005CreateDeleteEnumeration() {
        System.out.println("#test005CreateDeleteEnumeration");

        // Get the initial number of enumerations
        Long enumerationsCountInitial = api.getAllEnumerationsCount();
        // Validate the number, there should be persisted enumerations.
        assertNotNull("There should be persisted enumerations!!", enumerationsCountInitial);
        assertTrue("Number of the persisted enumerations is 0!", enumerationsCountInitial > Long.valueOf(0L));

        // Create new Enumeration with two test values
        EnumerationWS enumerationWS = createEnumeration("Test");
        enumerationWS.addValue("Test1");
        enumerationWS.addValue("Test2");
        // Persist the entity
        Integer enumerationId = null;
        try {
            enumerationId = api.createUpdateEnumeration(enumerationWS);
        } catch (SessionInternalError se){
            fail(String.format("Error while persisting Enumeration entity!! %s", se));
        }

        // Validate the id of the persisted enumeration
        assertTrue("The ID is not a valid number!!", enumerationId > Integer.valueOf(0));
        enumerationWS.setId(enumerationId);

        // Get the count of all persisted enumerations
        Long enumerationsCountAfterPersist = api.getAllEnumerationsCount();

        // Check if the initial number of enumerations increased.
        assertEquals("Initial number of enumerations should increment!", Long.valueOf(enumerationsCountInitial + 1L), enumerationsCountAfterPersist);

        // Get the newly created enumeration.
        EnumerationWS persistedEnumerationWS = api.getEnumeration(enumerationId);
        // Validate
        validatePersistedEnumeration(persistedEnumerationWS);
        // Compare it with the manually created enumeration
        compareEnumerationsProperties(enumerationWS, persistedEnumerationWS);

        // Get the values
        List<EnumerationValueWS> values = persistedEnumerationWS.getValues();
        // In this case we know that there are two values.
        assertEquals("The size of the values list should be 2!!", Integer.valueOf(2), Integer.valueOf(values.size()));
        // First value, validate and compare
        validateEnumerationValueWS(values.get(0));
        assertEquals("The values are not the same, but it should be!!", "Test1", values.get(0).getValue());
        // Second value, validate and compare
        validateEnumerationValueWS(values.get(1));
        assertEquals("The values are not the same, but it should be!!", "Test2", values.get(1).getValue());

        // Delete the enumeration for house keeping purposes
        try{
            api.deleteEnumeration(enumerationId);
        } catch (SessionInternalError se){
            fail(String.format("Error while deleting enumeration with %d id!!", enumerationId));
        }
        // Check if the deletion was successful.
        // Get the count of all persisted enumerations
        Long enumerationsCountAfterDelete = api.getAllEnumerationsCount();
        // Check if the current number of enumerations is the same as the initial count.
        assertEquals("The number of Enumerations should be the same as the initial number!!", enumerationsCountInitial, enumerationsCountAfterDelete);
        System.out.println("#test005CreateDeleteEnumeration done!!");
    }

    @Test
    public void test006DeleteEnumerationUsedInMetaField(){

        System.out.println("#test006DeleteEnumerationUsedInMetaField");
        // Get the initial number of enumerations
        Long enumerationsCountInitial = api.getAllEnumerationsCount();
        assertNotNull("There should be persisted enumerations!!", enumerationsCountInitial);
        assertTrue("Number of the persisted enumerations is 0!", enumerationsCountInitial > Long.valueOf(0L));

        // Create Enumeration
        EnumerationWS enumerationWS = createEnumeration("MetaTest");
        // Add value
        enumerationWS.addValue("5");

        // Create MetaField that is going to use this enumeration
        MetaFieldWS metafieldWS = new MetaFieldWS();
        metafieldWS.setDataType(DataType.ENUMERATION);
        metafieldWS.setEntityType(EntityType.CUSTOMER);
        metafieldWS.setName(enumerationWS.getName());
        MetaFieldBL.createValue(metafieldWS,"TestMetaFieldValue");
        metafieldWS.setPrimary(true);

        ValidationRuleWS rule = new ValidationRuleWS();
        rule.setRuleType(ValidationRuleType.RANGE.name());
        rule.addRuleAttribute(RangeValidationRuleModel.VALIDATION_MIN_RANGE_FIELD, "1.0");
        rule.addRuleAttribute(RangeValidationRuleModel.VALIDATION_MAX_RANGE_FIELD, "10.0");
        rule.addErrorMessage(Constants.LANGUAGE_ENGLISH_ID, "Please enter a value between 1 and 10");
        metafieldWS.setValidationRule(rule);


        // Persist meta field and enumeration
        Integer metaId = api.createMetaField(metafieldWS);
        Integer enumerationId = null;

        try {
            enumerationId = api.createUpdateEnumeration(enumerationWS);
        } catch (SessionInternalError se){
            fail(String.format("Error while persisting Enumeration entity!! %s", se));
        }

        // Validate the id of the persisted Enumeration
        assertTrue("The ID is not a valid number!!", enumerationId > Integer.valueOf(0));
        enumerationWS.setId(enumerationId);

        // Get the newly created enumeration.
        EnumerationWS persistedEnumerationWS = api.getEnumeration(enumerationId);
        // Validate
        validatePersistedEnumeration(persistedEnumerationWS);
        // Compare it with the manually created enumeration
        compareEnumerationsProperties(enumerationWS, persistedEnumerationWS);

        // Get the number of enumerations after persist
        Long enumerationsCountAfterPersist = api.getAllEnumerationsCount();
        // Ensure that the number increased.
        assertEquals("Initial number of enumerations should increment!", Long.valueOf(enumerationsCountInitial + 1L), enumerationsCountAfterPersist);


        // Try to delete the enumeration used in meta field, it should fail(throws a exception).
        boolean success = false;
        try {
            success = api.deleteEnumeration(enumerationId);
            fail(String.format("Enumeration: %d, used in meta field deleted!", enumerationId));
        } catch (SessionInternalError se){
            assertEquals("Success: true!!", false, success);
        }

        // Verify that the number of enumerations remains the same
        Long enumerationsCountAfterDelete = api.getAllEnumerationsCount();
        assertEquals("Current number of enumerations should remain the same!", enumerationsCountAfterPersist, enumerationsCountAfterDelete);

        // Delete the meta field
        api.deleteMetaField(metaId);

        // Try to delete enumeration again, this time successful
        try {
            success = api.deleteEnumeration(enumerationId);
            assertEquals("Success false!", true, success);
        } catch (SessionInternalError se){
            fail("Enumeration can not be deleted!");
        }
        // Verify that the number of enumerations is the same as initial.
        Long enumerationsCountAfterDelete2 = api.getAllEnumerationsCount();
        assertEquals("Current number of enumerations should remain the same!", enumerationsCountInitial, enumerationsCountAfterDelete2);
        System.out.println("#test006DeleteEnumerationUsedInMetaField done!");
    }

    @Test
    public void test007UpdateEnumeration(){
        System.out.println("#test007UpdateEnumeration");

        // Get initial number of persisted Enumerations
        Long enumerationsCountInitial = api.getAllEnumerationsCount();
        // Validate the number
        assertNotNull("There should be persisted enumerations!!", enumerationsCountInitial);
        assertTrue("Number of the persisted enumerations is 0!", enumerationsCountInitial > Long.valueOf(0L));

        // Create new Enumeration with two test values
        EnumerationWS enumerationWS = createEnumeration("Test");
        enumerationWS.addValue("Test1");
        enumerationWS.addValue("Test2");
        // Persist the entity
        Integer enumerationId = null;
        try {
            enumerationId = api.createUpdateEnumeration(enumerationWS);
        } catch (SessionInternalError se){
            fail(String.format("Error while persisting Enumeration entity!! %s", se));
        }
        // Validate the id of the persisted enumeration
        assertTrue("The ID is not a valid number!!", enumerationId > Integer.valueOf(0));
        enumerationWS.setId(enumerationId);

        // Get the newly created enumeration.
        EnumerationWS persistedEnumerationWS = api.getEnumeration(enumerationId);
        // Validate
        validatePersistedEnumeration(persistedEnumerationWS);
        // Compare it with the manually created enumeration
        compareEnumerationsProperties(enumerationWS, persistedEnumerationWS);

        // Get the current number of persisted enumerations.
        Long enumerationsCountAfterPersist = api.getAllEnumerationsCount();
        // Check if the initial number of enumerations increased.
        assertEquals("Initial number of enumerations should increment!", Long.valueOf(enumerationsCountInitial + 1L), enumerationsCountAfterPersist);


        // Validate values
        List<EnumerationValueWS> wsValues = persistedEnumerationWS.getValues();
        // In this case we know that there are 2 values.
        assertNotNull("Values list can not be null!", wsValues);
        assertEquals("The size of the values list should be 2!!", Integer.valueOf(2), Integer.valueOf(wsValues.size()));
        // Validate first value
        validateEnumerationValueWS(wsValues.get(0));
        assertEquals("The values are not the same, but it should be!!", "Test1", wsValues.get(0).getValue());

        // Validate second value
        validateEnumerationValueWS(wsValues.get(1));
        assertEquals("The values are not the same, but it should be!!", "Test2", wsValues.get(1).getValue());

        // Update one of the values of the Enumeration and add new one, and update.
        wsValues.get(0).setValue(wsValues.get(0).getValue() + " updated");
        persistedEnumerationWS.addValue("Test3");


        // Save changes
        Integer enumerationId2 = null;
        try {
            enumerationId2 = api.createUpdateEnumeration(persistedEnumerationWS);
        } catch (SessionInternalError se){
            fail(String.format("Error while updating Enumeration entity!! %s", se));
        }
        // The ID here represents the same entity as enumerationId, validate this.
        assertEquals("The enumeration ids must be the same, they represent the same entity!", enumerationId, enumerationId2);

        // Get the current number of persisted enumerations after update.
        Long enumerationsCountAfterUpdate = api.getAllEnumerationsCount();
        // Check if number of persisted enumerations remains the same before and after the update.
        assertEquals("Number of Enumerations differs!", enumerationsCountAfterPersist, enumerationsCountAfterUpdate);

        // Get the updated record.
        EnumerationWS updatedEnumeration = api.getEnumeration(enumerationId2);
        // Validate
        validatePersistedEnumeration(updatedEnumeration);
        // Compare it with the previous persisted Enumeration, they should be the same
        compareEnumerationsProperties(persistedEnumerationWS, updatedEnumeration);

        // Get the values
        List<EnumerationValueWS> updatedValuesWS = updatedEnumeration.getValues();
        // In this case we know that there are 3 values.
        assertNotNull("Values list can not be null!", updatedValuesWS);
        assertEquals("The size of the values list should be 3!!", Integer.valueOf(3), Integer.valueOf(updatedValuesWS.size()));
        // First value
        validateEnumerationValueWS(updatedValuesWS.get(0));
        assertEquals("The values are not the same, but it should be!!", "Test1 updated", updatedValuesWS.get(0).getValue());
        // Second value
        validateEnumerationValueWS(updatedValuesWS.get(1));
        assertEquals("The values are not the same, but it should be!!",  "Test2", updatedValuesWS.get(1).getValue());
        // Third value
        validateEnumerationValueWS(updatedValuesWS.get(2));
        assertEquals("The values are not the same, but it should be!!", "Test3", updatedValuesWS.get(2).getValue());

        // Update the name and persist again
        updatedEnumeration.setName("NewName");
        Integer enumerationId3 = null;

        try {
            enumerationId3 = api.createUpdateEnumeration(updatedEnumeration);
        } catch (SessionInternalError sie){
            fail(String.format("Error while updating Enumeration entity!! %s", sie));
        }

        // The ID here represents the same entity as enumerationId, validate this.
        assertEquals("The enumeration ids must be the same, they represent the same entity!", enumerationId, enumerationId3);

        // Get the current number of persisted enumerations after update.
        Long enumerationsCountAfterUpdate2 = api.getAllEnumerationsCount();
        // Check if number of persisted enumerations remains the same before and after the update.
        assertEquals("Number of Enumerations differs!", enumerationsCountAfterPersist, enumerationsCountAfterUpdate2);

        // Get the updated record.
        EnumerationWS updatedEnumeration2 = api.getEnumeration(enumerationId2);
        // Validate
        validatePersistedEnumeration(updatedEnumeration2);
        // Check if the name changed!
        assertEquals("Updated names differs!!", "NewName", updatedEnumeration2.getName());

        // Get the values
        List<EnumerationValueWS> updatedValuesWS2 = updatedEnumeration2.getValues();
        // In this case we know that there are 3 values.
        assertEquals("The size of the values list should be 3!!", Integer.valueOf(3), Integer.valueOf(updatedValuesWS2.size()));
        // First value
        validateEnumerationValueWS(updatedValuesWS2.get(0));
        assertEquals("The values are not the same, but it should be!!", "Test1 updated", updatedValuesWS2.get(0).getValue());
        // Second value
        validateEnumerationValueWS(updatedValuesWS2.get(1));
        assertEquals("The values are not the same, but it should be!!",  "Test2", updatedValuesWS2.get(1).getValue());
        // Third value
        validateEnumerationValueWS(updatedValuesWS2.get(2));
        assertEquals("The values are not the same, but it should be!!", "Test3", updatedValuesWS2.get(2).getValue());

        // Delete the enumeration for house keeping purposes
        try{
            api.deleteEnumeration(enumerationId);
        } catch (SessionInternalError se){
            fail(String.format("Error while deleting enumeration with %d id!!", enumerationId));
        }

        // Get the current number of persisted enumerations after delete.
        Long enumerationsCountAfterDelete = api.getAllEnumerationsCount();
        assertEquals("Number of Enumerations differs!", enumerationsCountInitial, enumerationsCountAfterDelete);

        System.out.println("#test007UpdateEnumeration done!!");
    }

    @Test
    public void test008UpdateEnumerationWithDuplicateName(){
        System.out.println("#test008UpdateEnumerationWithDuplicateName");

        // Get initial number of persisted Enumerations
        Long enumerationsCountInitial = api.getAllEnumerationsCount();
        // Validate the number
        assertNotNull("There should be persisted enumerations!!", enumerationsCountInitial);
        assertTrue("Number of the persisted enumerations is 0!", enumerationsCountInitial > Long.valueOf(0L));

        // Create a Enumeration
        EnumerationWS firstEnumeration = createEnumeration("SameName");
        // Add one value to it.
        firstEnumeration.addValue("SomeValue");

        // Persist the enumeration
        Integer id = null;
        try {
            id = api.createUpdateEnumeration(firstEnumeration);
        } catch (SessionInternalError se){
            fail(String.format("Error while persisting Enumeration entity!! %s", se));
        }
        // Validate the id of the first enumeration
        assertTrue("The ID is not a valid number!!", id > Integer.valueOf(0));
        firstEnumeration.setId(id);

        // Get the current number of persisted enumerations after update.
        Long enumerationsCountAfterPersist = api.getAllEnumerationsCount();
        // Check if number of persisted enumerations increased.
        assertEquals("Number of Enumerations differs!", Long.valueOf(enumerationsCountInitial + 1L), enumerationsCountAfterPersist);

        // Get the persisted enumeration
        EnumerationWS persistedEnumerationWS = api.getEnumeration(id);
        // Validate
        validatePersistedEnumeration(persistedEnumerationWS);
        // Compare against manually created enumeration above
        compareEnumerationsProperties(firstEnumeration, persistedEnumerationWS);

        // Create another enumeration with the same name, but different value
        EnumerationWS secondEnumeration = createEnumeration("SameName");
        // Add one different value
        secondEnumeration.addValue(new EnumerationValueWS("AnotherValue"));

        // Persist the enumeration, this should fail(throw exception), enumerations with the same name, not allowed.
        Integer id2 = null;
        try {
            id2 = api.createUpdateEnumeration(secondEnumeration);
            fail("#test008UpdateEnumerationWithDuplicateName failed!!");
        } catch (SessionInternalError se){
            // Delete the enumerations for house keeping purposes
            // Get the current number of persisted enumerations after delete.
            Long enumerationsCountAfterDuplicate = api.getAllEnumerationsCount();
            // Ensure that the number of enumerations is the same before and after the unsuccessful save.
            assertEquals("Number of Enumerations differs!", enumerationsCountAfterPersist, enumerationsCountAfterDuplicate);
        }

        try{
            api.deleteEnumeration(id);
        } catch (SessionInternalError se1){
            fail(String.format("Error while deleting enumeration with id %d!!", id));
        }
        // Get the current number of persisted enumerations after delete.
        Long enumerationsCountAfterDelete = api.getAllEnumerationsCount();
        // Ensure that the number of Enumerations is the same as initial
        assertEquals("Number of Enumerations differs!", enumerationsCountInitial, enumerationsCountAfterDelete);
        System.out.println("#test008UpdateEnumerationWithDuplicateName done!");
    }

    @Test
    public void test009CreateUpdateInvalidEnumeration(){
        System.out.println("#test009CreateUpdateInvalidEnumeration");

        // Get initial number of persisted Enumerations
        Long enumerationsCountInitial = api.getAllEnumerationsCount();
        // Validate the number
        assertNotNull("There should be persisted enumerations!!", enumerationsCountInitial);
        assertTrue("Number of the persisted enumerations is 0!", enumerationsCountInitial > Long.valueOf(0L));

        // Create enumeration with empty name
        EnumerationWS enumeration = createEnumeration("");
        enumeration.addValue("TestValue");

        Integer enumerationId = null;
        // Try to persist
        try{
            enumerationId = api.createUpdateEnumeration(enumeration);
            fail("Can not create enumeration with empty name!!");
        } catch (SessionInternalError sie){
            assertNull("Id of the unsuccessfully persisted enumeration should be null!!", enumerationId);
        }

        // Get the current number of persisted enumerations after failed persis.
        Long enumerationsCountAfterUnsuccessfulPersist = api.getAllEnumerationsCount();
        // Check if number of persisted enumerations remains the same.
        assertEquals("Number of Enumerations differs!", enumerationsCountInitial, enumerationsCountAfterUnsuccessfulPersist);

        // Create enumeration with no values
        enumeration = createEnumeration("InvalidEnumeration");

        // Try to persist
        try{
            enumerationId = api.createUpdateEnumeration(enumeration);
            fail("Can not create enumeration with no values!!");
        } catch (SessionInternalError sie){
            assertNull("Id of the unsuccessfully persisted enumeration should be null!!", enumerationId);
        }

        // Get the current number of persisted enumerations after failed persis.
        Long enumerationsCountAfterUnsuccessfulPersist2 = api.getAllEnumerationsCount();
        // Check if number of persisted enumerations remains the same.
        assertEquals("Number of Enumerations differs!", enumerationsCountInitial, enumerationsCountAfterUnsuccessfulPersist2);

        // Create enumeration with empty value
        enumeration = createEnumeration("InvalidEnumeration");
        enumeration.addValue("");
        enumeration.addValue("TestValue");

        // Try to persist
        try{
            enumerationId = api.createUpdateEnumeration(enumeration);
            fail("Can not create enumeration with empty value!!");
        } catch (SessionInternalError sie){
            assertNull("Id of the unsuccessfully persisted enumeration should be null!!", enumerationId);
        }

        // Get the current number of persisted enumerations after failed persis.
        Long enumerationsCountAfterUnsuccessfulPersist3 = api.getAllEnumerationsCount();
        // Check if number of persisted enumerations remains the same.
        assertEquals("Number of Enumerations differs!", enumerationsCountInitial, enumerationsCountAfterUnsuccessfulPersist3);

        // Create enumeration with duplicate values
        enumeration = createEnumeration("InvalidEnumeration");
        enumeration.addValue("TestValue");
        enumeration.addValue("TestValue");

        // Try to persist
        try{
            enumerationId = api.createUpdateEnumeration(enumeration);
            fail("Can not create enumeration with duplicate values!!");
        } catch (SessionInternalError sie){
            assertNull("Id of the unsuccessfully persisted enumeration should be null!!", enumerationId);
        }

        // Get the current number of persisted enumerations after failed persis.
        Long enumerationsCountAfterUnsuccessfulPersist4 = api.getAllEnumerationsCount();
        // Check if number of persisted enumerations remains the same.
        assertEquals("Number of Enumerations differs!", enumerationsCountInitial, enumerationsCountAfterUnsuccessfulPersist4);


        System.out.println("#test009CreateUpdateInvalidEnumeration done!");
    }

    /**
     * Validates the persisted {@link com.sapienter.jbilling.server.util.EnumerationWS} entity
     * and its "identification" properties.
     * Simple not null and size validation.
     *
     * @param persistedEnumeration entity, which properties are examined.
     */
    private void validatePersistedEnumeration(EnumerationWS persistedEnumeration){
        assertNotNull("Enumeration entity should not be null!!", persistedEnumeration);
        // ID
        assertNotNull("The ID value can not be null!", persistedEnumeration.getId());
        assertTrue("The ID is not a valid number!!", persistedEnumeration.getId() > Integer.valueOf(0));
        // Entity ID
        assertNotNull("The Entity ID value can not be null!", persistedEnumeration.getEntityId());
        assertTrue("The ID is not a valid number!!", persistedEnumeration.getEntityId() > Integer.valueOf(0));
        // Name
        assertNotNull("Persisted Enumeration must have a name!!", persistedEnumeration.getName());
        assertTrue("The name can not be empty!", persistedEnumeration.getName().length() > Integer.valueOf(0));
    }

    /**
     * Validates if the "identification" properties of the two Enumerations
     * are the same.
     *
     * @param expectedEnumWS first comparison object.
     * @param actualEnumWS first comparison object.
     */
    private void compareEnumerationsProperties(EnumerationWS expectedEnumWS, EnumerationWS actualEnumWS){
        if(null == expectedEnumWS || null == actualEnumWS){
            fail("Can not compare null objects!!");
        }
        // ID
        assertEquals("Ids representing the same enumeration entity can not differ!!", expectedEnumWS.getId(), actualEnumWS.getId());
        // Name
        assertEquals("Names representing the same enumeration can not differ!!", expectedEnumWS.getName(), actualEnumWS.getName());
        // Entity ID
        assertEquals("Same enumeration entity can not be in different companies!!!", expectedEnumWS.getEntityId(), actualEnumWS.getEntityId());

    }

    /**
     * Used for validating the properties of {@link com.sapienter.jbilling.server.util.EnumerationValueWS} object.
     *
     * @param value object that is going to be validated.
     */
    private void validateEnumerationValueWS(EnumerationValueWS value){
        assertNotNull("The EnumerationValueWS can not be null!", value);
        // Id
        assertNotNull("The ID value can not be null!", value.getId());
        assertTrue("The ID is not a valid number!!", value.getId() > Integer.valueOf(0));
        // Value
        assertNotNull("Enumeration value can not be null!", value.getValue());
        assertTrue("The value can not be empty!", value.getValue().length() > Integer.valueOf(0));
    }

    /**
     * Creates test {@link com.sapienter.jbilling.server.util.EnumerationWS} object.
     *
     * @param name of the enumeration
     * @return enumeration object
     */
    public static EnumerationWS createEnumeration(String name){

        EnumerationWS enumerationWS = new EnumerationWS(name);
        enumerationWS.setEntityId(ROOT_ENTITY_ID);
        return enumerationWS;
    }

}
