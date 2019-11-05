package com.example.tests;

import java.sql.Driver;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.openqa.selenium.WebDriver;

@RunWith(Suite.class)
@Suite.SuiteClasses({ 
	CompanyLoginTest.class, CreatingChildCompanyTest.class,ChildCompanyInvoiceResellerTest.class,
	ImpersonatingChildCompany.class,SettingUpAccountTypes.class, CreatingPaymentMethod.class, Adding_InfoType.class,ConfigurePaymentMethod.class, ConfigureOrderPeriods.class, ConfiguringCollection.class, CollectionsPlugin.class,ConfigureBillingProcess.class, 
	CreatingCategory.class, CreatingAssetCategory1.class, CreatingAssetCategory1.class,CreatingProduct.class, CreatingProduct_Asset.class, EditingAccountTypePrice.class,
	ImpersonatingChildCompany_Asset.class,
	AddingCustomer.class, AddingCustomerAndSubAccount.class, AddingDiscountType.class,
	CreateOrder.class,
	GeneratingInvoiceManually.class, VerifyInvoice.class,GeneratingAndPayingInvoice.class, 
	CreatingOrderOfDifferentPeriods.class,ApprovingreviewReport.class, 
	MakingPaymentOnGeneratedInvoices.class, 
	RunningCollections.class,
	CreatingReportsAndVerifying.class, 
	DependentProductCreation.class, DependentProductOrder.class,
	AgentCreation.class, CreatingCommissionedProduct.class, ConfiguringPlugInForCommission.class,CreatingAndVerifyingCommission.class,
	//TearDownProcess.class
})
public class TestSuite {
	private static WebDriver driver;

	/*private static String baseUrl;
	
	@BeforeClass
    public static void setUp() {
		  //baseUrl = "http://localhost:8080/jbilling";
		  driver = Instance.getInstance();
		  Instance.initialize(driver);
    }*/

    @AfterClass
    public static void tearDown() {
    	driver.quit();
    }
	
	}
