package com.sapienter.jbilling.server.accountType;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.accountType.builder.AccountInformationTypeBuilder;
import com.sapienter.jbilling.server.accountType.builder.AccountTypeBuilder;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafield.builder.ValidationRuleBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.metafields.validation.ScriptValidationRuleModel;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.test.ApiTestCase;
import org.junit.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.testng.AssertJUnit.*;

@Test(groups = {"web-services", "accountType"})
public class WSTest extends ApiTestCase {

	private AccountTypeWS refAccountType;

	@AfterMethod
	public void afterTestCleanup() throws Exception {
		if (null != refAccountType) {
			if (null != refAccountType.getId() && refAccountType.getId().intValue() > 0) {
				api.deleteAccountType(refAccountType.getId());
			}
			refAccountType = null;
		}
	}

	@Test
	public void test001CreateRetrieveValidAccountType() {
		//create one vanilla account type
		refAccountType = new AccountTypeBuilder().create(api);
		Assert.assertNotNull("Account Type should be successfully created", refAccountType.getId());

		//get the created account type from server
		AccountTypeWS retAccountType = api.getAccountType(refAccountType.getId());
		assertNotNull("The Account Type was not retrieved successfully", retAccountType);

		//assert equality between the initial account type and retrieved account type
		matchAccountType(refAccountType, retAccountType);
	}


	@Test(expectedExceptions = SessionInternalError.class)
	public void test002CreateInvalidAccountType() {
		//build invalid account type
		AccountTypeWS accountType = new AccountTypeBuilder()
				.currencyId(Integer.valueOf(0))//this currency id does not exist
				.build();

		//try creating this account type, expecting an exception from this
		api.createAccountType(accountType);
	}

	@Test
	public void test003GetNonExistentAccountType() {
		Integer nonExistentAccountTypeID = Integer.valueOf(1000000);
		refAccountType = api.getAccountType(nonExistentAccountTypeID);
		assertNull("AccountType should not exist in the system", refAccountType);
	}

	@Test
	public void test004UpdateAccountType() {
		//create a valid account type with valid credit limit
		refAccountType = new AccountTypeBuilder()
				.creditLimit(new BigDecimal("1000"))
				.create(api, true);

		//modify some arbitrary fields and update
		refAccountType.setCreditNotificationLimit1("825");
		refAccountType.getDescriptions().clear();
		refAccountType.setName("Updated Account Type", Constants.LANGUAGE_ENGLISH_ID);
		boolean testFlag = api.updateAccountType(refAccountType);

		assertTrue("Update of Account Type Failed", testFlag);

		//verify that the update is success
		matchAccountType(refAccountType, api.getAccountType(refAccountType.getId()));
	}

	@Test
	public void test005DeleteAccountType() {
		//create a vanilla account type
		refAccountType = new AccountTypeBuilder().create(api);

		//remove the created account type
		boolean testFlag = api.deleteAccountType(refAccountType.getId());
		assertTrue("Deletion of Account Type Success", testFlag);

		//check if the account type still can be retrieved
		assertNull("Account Type should not exist", api.getAccountType(refAccountType.getId()));

		//cleanup
		refAccountType = null;
	}

	@Test(expectedExceptions = SessionInternalError.class)
	public void test006CreateInvalidAccountType() {

		//create an account type with invalid invoice delivery ID
		refAccountType = new AccountTypeBuilder()
				.invoiceDeliveryMethod(999999)
				.build();

		//try to create the account type, we expect exception here
		api.createAccountType(refAccountType);
	}

	@Test(expectedExceptions = SessionInternalError.class,
			expectedExceptionsMessageRegExp = "(?s).*AccountInformationTypeWS,accountTypeId,may not be null.*")
	public void test007InvalidAccountInformationTypeNoAccountType() {
		//create a valid meta field
		MetaFieldWS fnMetaField = new MetaFieldBuilder()
				.dataType(DataType.STRING)
				.entityType(EntityType.ACCOUNT_TYPE)
				.name("First Name")
				.build();

		//build account information type without account type
		AccountInformationTypeWS ait = new AccountInformationTypeBuilder((AccountTypeWS) null)
				.addMetaField(fnMetaField)
				.build();

		//can't save ait with no data. Exception is expected
		api.createAccountInformationType(ait);
	}

	@Test(expectedExceptions = SessionInternalError.class,
			expectedExceptionsMessageRegExp = "(?s).*AccountInformationTypeWS,metaFields,validation.error.notnull.*")
	public void test008InvalidAccountInformationTypeNoMetaFields() {
		//create account type
		refAccountType = new AccountTypeBuilder().create(api);

		//build account information type without account type
		AccountInformationTypeWS ait = new AccountInformationTypeBuilder(refAccountType).build();

		//can't save ait with no data. Exception is expected
		api.createAccountInformationType(ait);
	}

	@Test(expectedExceptions = SessionInternalError.class,
			expectedExceptionsMessageRegExp = "(?s).*AccountInformationTypeWS,metaFields.name,validation.error.notnull.*")
	public void test009AccountInformationTypeWithInvalidMetaFields() {
		//create account type
		refAccountType = new AccountTypeBuilder().create(api);

		//build incomplete meta field
		MetaFieldWS metaField = new MetaFieldBuilder()
				.dataType(DataType.INTEGER)
				.entityType(EntityType.ACCOUNT_TYPE)
				.build();

		//build account information type with incomplete meta field
		AccountInformationTypeWS ait = new AccountInformationTypeBuilder(refAccountType)
				.addMetaField(metaField)
				.build();

		api.createAccountInformationType(ait);
	}

	@Test(expectedExceptions = SessionInternalError.class,
			expectedExceptionsMessageRegExp = "(?s).*AccountInformationTypeWS,metaFields.validationRule.errorMessages,validation.error.empty.error.message.*")
	public void test010AccountInformationTypeWithInvalidMetaFields() {
		//create account type
		refAccountType = new AccountTypeBuilder().create(api);

		//create invalid meta field,
		//validation rule missing error message
		MetaFieldWS ageMetaField = new MetaFieldBuilder()
				.dataType(DataType.INTEGER)
				.entityType(EntityType.ACCOUNT_TYPE)
				.name("Age")
				.validationRule(new ValidationRuleBuilder()
                        .ruleType(ValidationRuleType.SCRIPT)
                        .addRuleAttribute(ScriptValidationRuleModel.VALIDATION_SCRIPT_FIELD, "_this >18")
                        .build())
				.build();

		// build ait with invalid meta field
		AccountInformationTypeWS ait = new AccountInformationTypeBuilder(refAccountType)
				.name("Contact Information")
				.addMetaField(ageMetaField)
				.build();

		//expecting an exception due to invalid ait
		api.createAccountInformationType(ait);
	}

	@Test(expectedExceptions = SessionInternalError.class,
			expectedExceptionsMessageRegExp = "(?s).*AccountInformationTypeWS,metaFields.validationRule.errorMessages,validation.error.empty.error.message.*")
	public void test011AccountInformationTypeWithInvalidMetaFields() {
		//create account type
		refAccountType = new AccountTypeBuilder().create(api);

		//create a valid meta field
		MetaFieldWS fnMetaField = new MetaFieldBuilder()
				.dataType(DataType.STRING)
				.entityType(EntityType.ACCOUNT_TYPE)
				.name("First Name")
				.build();

		// create invalid meta field
		// missing error message for validation rule
		MetaFieldWS ageMetaField = new MetaFieldBuilder()
				.dataType(DataType.INTEGER)
				.entityType(EntityType.ACCOUNT_TYPE)
				.name("Age")
				.validationRule(new ValidationRuleBuilder()
						.ruleType(ValidationRuleType.SCRIPT)
						.addRuleAttribute(ScriptValidationRuleModel.VALIDATION_SCRIPT_FIELD, "_this >18")
						.build())
				.build();

		// build ait with one valid and one invalid
		AccountInformationTypeWS ait = new AccountInformationTypeBuilder(refAccountType)
				.name("Contact Information")
				.addMetaField(fnMetaField)
				.addMetaField(ageMetaField)
				.build();

		//expecting an exception due to invalid ait
		api.createAccountInformationType(ait);
	}

    @Test
    public void test012CreateAccountInformationType() {
        //create account type
        refAccountType = new AccountTypeBuilder().create(api);

        AccountInformationTypeWS ait = buildValidAitWithMetaFields(refAccountType);

        ait.setId(api.createAccountInformationType(ait));
        assertNotNull("Can't save account information type", ait.getId());

        api.deleteAccountInformationType(ait.getId());
    }

	@Test
	public void test013CreateAccountInformationTypeCheckStructure() {
		//create account type
		refAccountType = new AccountTypeBuilder().create(api);

		AccountInformationTypeWS refAit = buildValidAitWithMetaFields(refAccountType);
        refAit.setId(api.createAccountInformationType(refAit));

		//verify that account type has the correct structure
		AccountInformationTypeWS retAit = api.getAccountInformationType(refAit.getId());
		matchAccountInformationType(refAit, retAit);

		api.deleteAccountInformationType(refAit.getId());
	}

    @Test
    public void test014CreateAccountInformationTypeCheckAit() {
        //create account type
        refAccountType = new AccountTypeBuilder().create(api);

        AccountInformationTypeWS ait = buildValidAitWithMetaFields(refAccountType);
        ait.setId(api.createAccountInformationType(ait));
        AccountTypeWS retAccountType = api.getAccountType(refAccountType.getId());

        //verify that there is only one ait created
        assertEquals("AITs not found for ", 1, retAccountType.getInformationTypeIds().length);
        Integer aitId = retAccountType.getInformationTypeIds()[0];

        api.deleteAccountInformationType(aitId);
    }

    @Test
	public void test015UpdateAccountInformationType() {
		//create account type
		refAccountType = new AccountTypeBuilder().create(api);
		AccountInformationTypeWS ait = buildValidAitWithMetaFields(refAccountType);
		ait.setId(api.createAccountInformationType(ait));

		//get the create ait on server side
		AccountTypeWS retAccountType = api.getAccountType(refAccountType.getId());
		Integer aitId = retAccountType.getInformationTypeIds()[0];
		AccountInformationTypeWS retAit = api.getAccountInformationType(aitId);
		MetaFieldWS oldMetaField = retAit.getMetaFields()[0];

		//valid meta field for last name
		MetaFieldWS lnMetaField = new MetaFieldBuilder()
				.dataType(DataType.STRING)
				.entityType(EntityType.ACCOUNT_TYPE)
				.name("Last Name")
				.validationRule(new ValidationRuleBuilder()
						.ruleType(ValidationRuleType.SCRIPT)
						.addRuleAttribute(ScriptValidationRuleModel.VALIDATION_SCRIPT_FIELD, "_this != null")
						.addErrorMessage("validation.invalid.last.name")
						.build())
				.fieldUsage(MetaFieldType.LAST_NAME)
				.build();

		// initialize the meta fields by excluding age but include last name meta field
		ait.setMetaFields(new MetaFieldWS[]{oldMetaField, lnMetaField});

		//update ait with new information
		api.updateAccountInformationType(ait);

		//get newly updated ait and compare
		retAit = api.getAccountInformationType(aitId);
		matchAccountInformationType(ait, retAit);

		//cleanup
		api.deleteAccountInformationType(aitId);
	}

	@Test
	public void test016DeleteAccountInformationType() {
		//create account type
		refAccountType = new AccountTypeBuilder().create(api);

		AccountInformationTypeWS ait = buildValidAitWithMetaFields(refAccountType);
		ait.setId(api.createAccountInformationType(ait));
		assertNotNull("Account Information Type should be created", ait.getId());
		assertNotNull("Account Information Type should be created", api.getAccountInformationType(ait.getId()));

		api.deleteAccountInformationType(ait.getId());

		assertNull("Account Information Type should be deleted", api.getAccountInformationType(ait.getId()));
		assertNull("Account Information Type array should be null", api.getAccountType(refAccountType.getId()).getInformationTypeIds());
	}

	@Test
	public void test017DeleteAccountTypeWithMultipleAit() {
		//create local account type
		AccountTypeWS accountType = new AccountTypeBuilder().create(api);
		assertNotNull("Account Type Should be created", accountType.getId());

		//create the first Ait
		AccountInformationTypeWS aitOne = buildValidAitWithMetaFields(accountType);
		aitOne.setName("First Contact");
		aitOne.setId(api.createAccountInformationType(aitOne));
		assertNotNull("Can't save account information type", aitOne.getId());

		//create the second Ait
		AccountInformationTypeWS aitTwo = buildValidAitWithMetaFields(accountType);
		aitTwo.setName("Second Contact");
		aitTwo.setId(api.createAccountInformationType(aitTwo));
		assertNotNull("Can't save account information type", aitTwo.getId());

		//by deleting the account type we are deleting the two AIT
		api.deleteAccountType(accountType.getId());

		assertNull("Account Type should be deleted: ", api.getAccountType(accountType.getId()));
		assertNull("Account Information Type (one) should be deleted: ", api.getAccountInformationType(aitOne.getId()));
		assertNull("Account Information Type (tw) should be deleted: ", api.getAccountInformationType(aitTwo.getId()));
	}

	@Test(expectedExceptions = SessionInternalError.class,
			expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,descriptions,validation.error.is.required.*")
	public void test018CreateAccountTypeWithInvalidDescription(){
		refAccountType = new AccountTypeBuilder().noDescriptions().build();
		api.createAccountType(refAccountType);
	}

    @Test(expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,descriptions,validation.error.notnull.*")
    public void test019CreateAccountTypeWithInvalidDescription2(){
        refAccountType = new AccountTypeBuilder().build();
        refAccountType.setDescriptions(new ArrayList<>());
        api.createAccountType(refAccountType);
    }

    @Test(expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,mainSubscription,validation.error.is.required.*")
    public void test020CreateAccountTypeWithInvalidMainSubscription(){
        refAccountType = new AccountTypeBuilder().build();
        refAccountType.setMainSubscription(null);
        api.createAccountType(refAccountType);
    }

    @Test(expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,mainSubscription.nextInvoiceDayOfPeriod,validation.error.is.required.*")
    public void test021CreateAccountTypeWithInvalidMainSubscription2(){
        refAccountType = new AccountTypeBuilder().mainSubscription(new MainSubscriptionWS(AccountTypeBuilder.MONTHLY_PERIOD, null)).build();
        api.createAccountType(refAccountType);
    }

    @Test(expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,mainSubscription.nextInvoiceDayOfPeriod,validation.error.min,1.*")
    public void test022CreateAccountTypeWithInvalidMainSubscription3(){
        refAccountType = new AccountTypeBuilder().mainSubscription(new MainSubscriptionWS(AccountTypeBuilder.MONTHLY_PERIOD, Integer.valueOf(0))).build();
        api.createAccountType(refAccountType);
    }

    @Test(expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,mainSubscription.periodId,validation.error.is.required.*")
    public void test023CreateAccountTypeWithInvalidMainSubscription4(){
        refAccountType = new AccountTypeBuilder().mainSubscription(new MainSubscriptionWS(null, Integer.valueOf(1))).build();
        api.createAccountType(refAccountType);
    }

    @Test(expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,descriptions,validation.error.is.required.*")
    public void test024UpdateAccountTypeWithInvalidDescription(){
        //create a valid account type with valid credit limit
        refAccountType = new AccountTypeBuilder()
                .creditLimit(new BigDecimal("1000"))
                .create(api, true);

        refAccountType.setDescriptions(null);
        api.updateAccountType(refAccountType);
    }

    @Test(expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,descriptions,validation.error.notnull.*")
    public void test025UpdateAccountTypeWithInvalidDescription2(){
        //create a valid account type with valid credit limit
        refAccountType = new AccountTypeBuilder()
                .creditLimit(new BigDecimal("1000"))
                .create(api, true);

        refAccountType.setDescriptions(new ArrayList<>());
        api.updateAccountType(refAccountType);
    }

    @Test(expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,mainSubscription,validation.error.is.required.*")
    public void test026UpdateAccountTypeWithInvalidMainSubscription(){
        //create a valid account type with valid credit limit
        refAccountType = new AccountTypeBuilder()
                .creditLimit(new BigDecimal("1000"))
                .create(api, true);

        refAccountType.setMainSubscription(null);
        api.updateAccountType(refAccountType);
    }

    @Test(expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,mainSubscription.nextInvoiceDayOfPeriod,validation.error.is.required.*")
    public void test027UpdateAccountTypeWithInvalidMainSubscription2(){
        //create a valid account type with valid credit limit
        refAccountType = new AccountTypeBuilder()
                .creditLimit(new BigDecimal("1000"))
                .create(api, true);

        refAccountType.setMainSubscription(new MainSubscriptionWS(AccountTypeBuilder.MONTHLY_PERIOD, null));
        api.updateAccountType(refAccountType);
    }

    @Test(expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,mainSubscription.nextInvoiceDayOfPeriod,validation.error.min,1.*")
    public void test028UpdateAccountTypeWithInvalidMainSubscription3(){
        //create a valid account type with valid credit limit
        refAccountType = new AccountTypeBuilder()
                .creditLimit(new BigDecimal("1000"))
                .create(api, true);

        refAccountType.setMainSubscription(new MainSubscriptionWS(AccountTypeBuilder.MONTHLY_PERIOD, Integer.valueOf(0)));
        api.updateAccountType(refAccountType);
    }

    @Test(expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*AccountTypeWS,mainSubscription.periodId,validation.error.is.required.*")
    public void test029UpdateAccountTypeWithInvalidMainSubscription4(){
        //create a valid account type with valid credit limit
        refAccountType = new AccountTypeBuilder()
                .creditLimit(new BigDecimal("1000"))
                .create(api, true);

        refAccountType.setMainSubscription(new MainSubscriptionWS(null, Integer.valueOf(1)));
        api.updateAccountType(refAccountType);
    }

	private AccountInformationTypeWS buildValidAitWithMetaFields(AccountTypeWS accountType) {
		//create a valid meta field
		MetaFieldWS fnMetaField = new MetaFieldBuilder()
				.dataType(DataType.STRING)
				.entityType(EntityType.ACCOUNT_TYPE)
				.name("First Name")
				.build();

		//create invalid meta field
		MetaFieldWS ageMetaField = new MetaFieldBuilder()
				.dataType(DataType.INTEGER)
				.entityType(EntityType.ACCOUNT_TYPE)
				.name("Age")
				.validationRule(new ValidationRuleBuilder()
						.ruleType(ValidationRuleType.SCRIPT)
						.addRuleAttribute(ScriptValidationRuleModel.VALIDATION_SCRIPT_FIELD, "_this >18")
						.addErrorMessage("validation.invalid.age")
						.build())
				.build();

		//create ait
		AccountInformationTypeWS ait = new AccountInformationTypeBuilder(accountType)
				.name("Contact Information")
				.addMetaField(fnMetaField)
				.addMetaField(ageMetaField)
				.build();

		return ait;
	}

	private void matchAccountInformationType(AccountInformationTypeWS ait, AccountInformationTypeWS aitRetrieved) {
		assertEquals(ait.getName(), aitRetrieved.getName());
		assertEquals(ait.getDisplayOrder().intValue(), aitRetrieved.getDisplayOrder().intValue());
		assertEquals(ait.getMetaFields().length, aitRetrieved.getMetaFields().length);

		for (MetaFieldWS mf : ait.getMetaFields()) {
			boolean foundMatch = false;

			for (MetaFieldWS retrievedMf : aitRetrieved.getMetaFields()) {
				if (mf.getName().equals(retrievedMf.getName())) {
					matchMetaField(mf, retrievedMf);
					foundMatch = true;
					break;
				}
			}

			assertTrue("Matching metafield not found: " + mf.getName(), foundMatch);
		}
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

	private void matchAccountType(AccountTypeWS accountType, AccountTypeWS accountTypeRetrieved) {
		assertEquals(accountType.getDescription(Constants.LANGUAGE_ENGLISH_ID).getContent(),
				accountTypeRetrieved.getDescription(Constants.LANGUAGE_ENGLISH_ID).getContent());
		assertEquals(accountType.getInvoiceDesign(), accountTypeRetrieved.getInvoiceDesign());
		assertEquals(accountType.getLanguageId(), accountTypeRetrieved.getLanguageId());
		assertEquals(accountType.getCurrencyId(), accountTypeRetrieved.getCurrencyId());
		assertEquals(accountType.getMainSubscription(), accountTypeRetrieved.getMainSubscription());
		assertEquals(accountType.getCreditLimitAsDecimal().compareTo(accountTypeRetrieved.getCreditLimitAsDecimal()), 0);
		assertEquals(accountType.getCreditNotificationLimit1AsDecimal().compareTo(accountTypeRetrieved.getCreditNotificationLimit1AsDecimal()), 0);
		assertEquals(accountType.getCreditNotificationLimit2AsDecimal().compareTo(accountTypeRetrieved.getCreditNotificationLimit2AsDecimal()), 0);
		assertNotNull("Should be populated by server", accountTypeRetrieved.getDateCreated());
	}

	private void matchValidationRule(ValidationRuleWS validationRule, ValidationRuleWS retrievedRule) {
		assertTrue(validationRule != null && retrievedRule != null);
		assertEquals(validationRule.getRuleType(), retrievedRule.getRuleType());
		assertEquals(validationRule.getErrorMessages().size(), retrievedRule.getErrorMessages().size());
		assertEquals(validationRule.getRuleAttributes(), retrievedRule.getRuleAttributes());
	}

}
