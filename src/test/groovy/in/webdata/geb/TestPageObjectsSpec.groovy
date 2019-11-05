/*package in.webdata.geb

import org.junit.After;

import geb.spock.GebReportingSpec
import in.webdata.geb.pageobject.AccountTypePage
import in.webdata.geb.pageobject.EditAccountTypePage
import in.webdata.geb.pageobject.ShowAccountTypePage
import in.webdata.geb.pageobject.AITDetailsPage
import in.webdata.geb.pageobject.AddMetafieldPage
import in.webdata.geb.pageobject.AccountInformationTypeListPage
import in.webdata.geb.pageobject.AgentCommissionProcessPage
import in.webdata.geb.pageobject.BillingProcessPage
import in.webdata.geb.pageobject.BlacklistPage
import in.webdata.geb.pageobject.CollectionPage
import in.webdata.geb.pageobject.CurrencyPage
import in.webdata.geb.pageobject.AddNewCurrencyPage
import in.webdata.geb.pageobject.EmailPage
import in.webdata.geb.pageobject.InvoiceDisplayPage
import in.webdata.geb.pageobject.LanguagePage
import in.webdata.geb.pageobject.AddLanguagePage
import in.webdata.geb.pageobject.ShowLanguagePage
import in.webdata.geb.pageobject.CompanyPage
import in.webdata.geb.pageobject.MetafieldsCategoriesPage
import in.webdata.geb.pageobject.MetafieldListPage
import in.webdata.geb.pageobject.AddMetafieldPage
import in.webdata.geb.pageobject.ShowMetafieldPage
import in.webdata.geb.pageobject.OrderChangeTypePage
import in.webdata.geb.pageobject.AddOrderChangeTypePage
import in.webdata.geb.pageobject.ShowOrderChangeTypePage
import in.webdata.geb.pageobject.OrderPeriodsPage
import in.webdata.geb.pageobject.AddOrderPeriodsPage
import in.webdata.geb.pageobject.ShowOrderPeriodsPage
import in.webdata.geb.pageobject.ShowOrderStatusPage
import in.webdata.geb.pageobject.AddOrderStatusPage
import in.webdata.geb.pageobject.OrderStatusPage
import in.webdata.geb.pageobject.EnumerationPage
import in.webdata.geb.pageobject.AddEnumerationPage
import in.webdata.geb.pageobject.EnumerationListPage



class TestPageObjectsSpec extends GebReportingSpec {

    //TODO Rohit - To test this method for accurace and extend it for
    // testing content for AccountTypePage object and remaining objects
	
    def "Test Account Type Page Objects"() {
        given:
           to AccountTypePage 
		   
		expect: at AccountTypePage
			
        when : "Test edit Account Type Page " 
			EditAccountTypePage editAccountTypePage = to(AccountTypePage).clickAddNewAccountType()
		then : "" 	
			 at EditAccountTypePage   
		
		when : "Test Show Account Type Page "
			def accountName = "Private"
			
			ShowAccountTypePage showAccountTypePage = to(AccountTypePage).clickAccountType(accountName)
			
		then : ""
			 assert accountTypeDescription.text() == accountName.toUpperCase() 
			 
		when : "Test Ait Details  Page "
			 AITDetailsPage aitDetailsPage = showAccountTypePage.clickAddInformationType()
		 
		then : ""
			 at AITDetailsPage
			 
		when: "Test add metafield page"
		 	 AddMetafieldPage addMetafieldPage = aitDetailsPage.clickAddMetafield()
		then: ""	  
		 	 at AddMetafieldPage
			  
		when : "Test edit account type page for existing account type  Page "
			 def accountTypeId = 3
			 to ShowAccountTypePage
			 EditAccountTypePage existaccountTypePage = showAccountTypePage.clickEditAccountType()
			 
		then : ""
		     //at new EditAccountTypePage(accountTypeId: actTypeId)
			 driver.currentUrl == System.getProperty("baseUrl").concat(url).concat("/").concat(accountTypeId.toString())
			 
		when: "test Ait list page "
	 		to AccountInformationTypeListPage
		then:
			at AccountInformationTypeListPage
					 
	}
	
	def "Test Agent Commission " () {
		given: ""
			to AgentCommissionProcessPage
		expect: 
			at AgentCommissionProcessPage
		
	}
	
	def "Test Billing Process " () {
	 given: ""
		 to BillingProcessPage
	 expect: at BillingProcessPage
	 
 	}
	
	def "Test Black List " () {
	 given: ""
		 to BlacklistPage
	 expect: at BlacklistPage
	 
	 }
	
	def "Test Collection Page " () {
	 given: ""
		 to CollectionPage
	 expect: at CollectionPage
	 
	 }
	
	def "Test company Page " () {
	 given: ""
		 to CompanyPage
	 expect: at CompanyPage
	 
	 }
	
	def "Test currency Page " () {
		given: ""
			to CurrencyPage 
		expect: at CurrencyPage
		
		when: "test add new currecny page"
			AddNewCurrencyPage addNewCurrencyPage = to(CurrencyPage).clickAddNewCurrency() 
		then: ""
			at AddNewCurrencyPage
				
	}
	
	def "Test Email Page " () {
		given: ""
			to EmailPage 
		expect: at EmailPage
		
	}
	
	def "Test Invoice Display  Page " () {
		given: ""
			to InvoiceDisplayPage
		expect: at InvoiceDisplayPage
		
	}
	
	def "Test Language Page " () {
		given: ""
			to LanguagePage 
		expect: at LanguagePage
		
		when: "test add new currecny page"
			 AddLanguagePage addLanguagePage = to(LanguagePage).clickAddLanguage()
		then: ""
			at AddLanguagePage
			
		when: "test add new currecny page"
			def languagedescription = "Portuguese"
			ShowLanguagePage showLanguagePage = to(LanguagePage).clickLanguage(languagedescription)
		then: ""
			assert languageName.text() == languagedescription.toUpperCase()
		when: "test edit language page for existing language "		
			def languageCode = "pt" 
			AddLanguagePage elp = showLanguagePage.clickEditLang()
		then: ""	
			langCode.value() == languageCode
	}
	
	 def "Test Metafield Page " () {
		given: ""
			to MetafieldsCategoriesPage
		expect: at MetafieldsCategoriesPage
		
		when: "Test show metafield category 'CUSTOMER'  "
			MetafieldListPage metafieldList = to(MetafieldsCategoriesPage).clickMetafieldFCategory("CUSTOMER")
		then: 
			at 	MetafieldListPage
		when: "Test add new meta field page "	
			AddMetafieldPage addMetafieldPage = metafieldList.clickAddNewMetafield()
		then: 
			$("div#main > div.form-edit > div.heading > strong").text() == "NEW META FIELD"
			
		when: "retrun tp MetaFieldList page for test show metafields page"
			to MetafieldsCategoriesPage 
			metafieldList = metafieldCategory.clickMetafieldFCategory("CUSTOMER")
		then: 
			at 	MetafieldListPage
		when: "Test Show Meta field Page"
			def  metafieldName = "partner.prompt.fee"
			
			ShowMetafieldPage showMetafieldPage = metafieldList.clickMetafield("partner.prompt.fee")
		then:
			metafieldheading.text() == metafieldName.toUpperCase()
			
		when: "Test edit Meta field Page for existing MF "
			
			addMetafieldPage = showMetafieldPage.clickEditMetafield()
		then:
			$("div#main > div.form-edit > div.heading > strong").text() == "EDIT META FIELD"
			
			
	}
	
	def "Test order change type " () {
		given :
			to OrderChangeTypePage 
		expect:
			at 	OrderChangeTypePage
			
		when: "Test add order change type page "
			AddOrderChangeTypePage addOrderChangeTypePage = to(OrderChangeTypePage).clickAddNewOrderChange()
		then: 
			at AddOrderChangeTypePage
		
		when: "Test show  order change type page "		
				def orderChangeTypeName = "Default"
			ShowOrderChangeTypePage showOrderChangeTypePage = to(OrderChangeTypePage).clickSelectOrdChngType(orderChangeTypeName)
		then: ""
			orderChangeType.text() == orderChangeTypeName.toUpperCase()
			
	}
	
	def "Test order periods pages" () {
		given :
			to OrderPeriodsPage 
		expect:
			at 	OrderPeriodsPage
			
		when: "Test add order change type page "
			AddOrderPeriodsPage addOrderPeriodsPage = to(OrderPeriodsPage).clickAddNewOrderPeriod()
		then:
			at AddOrderPeriodsPage
		
		when: "Test show  order change type page "
				def orderPeriod = "Every 3 months"
			ShowOrderPeriodsPage showOrderPeriodsPage = to(OrderPeriodsPage).clickOrderPeriod(orderPeriod)
		then: ""
			orderPeriodName.text() == orderPeriod.toUpperCase()
			
		when: "Test edit order period with existing orderperiod"	
			addOrderPeriodsPage = showOrderPeriodsPage.clickEditOrderPeriod()
		then: ""
			$("div#column2.column-hold > div.column-hold > div.heading > strong ").text() == "EDIT ORDER PERIOD"
			
	}
	
	def " Test order status pages " () {
		
		given :
			to OrderStatusPage
		expect:
			at 	OrderStatusPage
			
		when: "Test add order status "
			AddOrderStatusPage addOrderStatus = to(OrderStatusPage).clickAddNewOrderStatus()
		then: "validate AddOrderStatus "
			at AddOrderStatusPage
		
		when: "Test ShowOrderStatus Page "
			def orderStatusName = "INVOICE"
			ShowOrderStatusPage showOrderStatusPage = to(OrderStatusPage).clickOrderStatus(orderStatusName) 	
		then: "Valdiate ShowOrderStatus Page "
			at ShowOrderStatusPage
			
		when : "Test EditOrderPeriod  Page"	
			addOrderStatus = showOrderStatusPage.clickEditOrderStatus()
		then: ""
			$("div#column2.column-hold > div.column-hold > div.heading > strong ").text() == "EDIT ORDER STATUS"	
		
	}
	
	def "testing EnumerationPage"() {
		given:
			to  EnumerationPage
			
		expect:
			at EnumerationPage
			
	}
	
	def "testing AddEnumerationPage"() {
		
		given:
			def fieldsMap = [addName: "Amit", enterValues: "1", enterNewValues: "2"]
			EnumerationPage enumPage = to(AddEnumerationPage)
										.clickSaveEnumeration(fieldsMap)
			
		expect:
			assert $("div.msg-box.successfully > strong").text() == "Done"
	
		
	}
	
}
*/