package in.webdata.geb

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite.class)
@Suite.SuiteClasses([
	TestGebSpec.class,
	CompanySpec.class,
	CompanySetupConfigurationSpec.class,
	CategoryProductsSpec.class,
	
	CompanyHierarchySpec.class,
	CustomerSpec.class,
	OrderSpec.class,
	
	InvoiceManualSpec.class,
	BillingProcessAndCollectionsSpec.class,
	CollectionSpec.class,
	CreatePaymentSpec.class,
	ReportSpec.class,
	OrderHierarchiesSpec.class,
	AgentsSpec.class,

	
])

class TestSuite {
	
}
