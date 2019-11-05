package in.webdata.geb

import in.webdata.geb.pageobject.AgentPage
import geb.spock.GebReportingSpec
import org.joda.time.DateTime

import spock.lang.Stepwise;
import in.webdata.geb.pageobject.AgentPage
import in.webdata.geb.pageobject.CustomerPage
import in.webdata.geb.pageobject.PluginsCategoryPage
import in.webdata.geb.pageobject.EditPrefrencePage
import in.webdata.geb.pageobject.AllPreferencesPage
import in.webdata.geb.pageobject.InvoiceListPage
import in.webdata.geb.pageobject.OrderListPage
import in.webdata.geb.pageobject.AgentCommissionProcessPage
import in.webdata.geb.pageobject.ShowCommissionPage
import in.webdata.geb.pageobject.OrderLineChangePage
import in.webdata.geb.pageobject.OrderReviewPage

@Stepwise
class AgentsSpec extends GebReportingSpec {

	def "Agent Creation " () {
		given: 
			to AgentPage
		expect:
			at AgentPage
		
			
		when: "Create an Agent 'Agent A' "
			def fieldsMap = [agentName: "Agent B", emailId: "text@gmail.com", agentType: "Master", commissionType: "Invoice"]
			AgentPage agentPage = to(AgentPage).clickAddNewAgent()
											   .clickSaveAgent(fieldsMap)
											   
			String message	= $("div.msg-box.successfully > p").text() 								
			def agentId 	= OrderSpec.extractId(message)		
							
		then: "Validate Agent A "
			assert agentId != null
			
			
		
		when: "create customer 'Customer B ' with this agent id "
			fieldsMap.clear()
			fieldsMap = [loginName: 'Customer B', agentId: agentId, emailId:"ravi@yahoo.co.in",
						paymentMethod: "Credit Card1", processingOrder: "1", cardHolderName: "Ashish", cardNumber: "4111111111111152", cardExpiryDate: "02/2020" ]
				CustomerPage customerPage = waitFor {to(CustomerPage)
							    			.clickAddNewCustomer()
											.clickCustomerForm("Prancing Pony", "Direct Customer")
											.clickCustomerFormSubmit(fieldsMap)
										}
							    
		then: "Validate customer has created "
			driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true 
													 		
	}
	
	
	def "Create a product for Agent" () {
		given: 
			go "product/index"
		
		when: "Create a Product category 'Commission Product'"
		
			waitFor { $('#column1 .submit', text: "ADD CATEGORY").click() }
			waitFor { $("input", id: "description") << "Commission Product" }
			$("select", id: "company-select") << "Prancing Pony"
			$(".submit", text: "SAVE CHANGES").click()
			
		then: "Validate above category has created "
			assert driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true
		
			
		when: "create product 'Product Code=C-01'"
		
			$('#column1 .double', text: "Commission Product").click()
			waitFor { $('#column2 .submit', text: "ADD PRODUCT").click() }
			
			$('input', id: "product.number") << "C-01"
			$('select', id: "newDescriptionLanguage").value("English")
			$('a > img').click()
			$('input', id: "product.descriptions[0].content").value("Comm Product")
			$('input', id: "product.standardPartnerPercentageAsDecimal") << "5"
			$('input', id: "product.masterPartnerPercentageAsDecimal") << "10"
			$("select", id: "company-select") << "Prancing Pony"
			$("select", id: "entitySelect") << "Prancing Pony"
			$("select", id: "currencySelect") << "Unites State Dollars"
			$("input", name: "product.rate") << "10"
			waitFor { $("a").find{it.text().contains("ADD PRICE")}.click() }
			
			$('.submit', text: "SAVE CHANGES").click()
			
		then: "Validate that product has created"
			assert driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true
	}
		
	def "Set commission plugin and preference" () {
		given: ""
			to PluginsCategoryPage
		expect:
			at PluginsCategoryPage 
		
		when: "Set plugin id '25'"
			def pluginCategoryId = "25"
			def fieldsMap = [pluginType: 'com.sapienter.jbilling.server.user.partner.task.BasicPartnerCommissionTask', processingOrder: "1005"]
			PluginsCategoryPage	pluginsCategoryPage = to(PluginsCategoryPage).clickPluginCategoryById(pluginCategoryId)
																			 .clickAddNewPlugin()
													                         .clickSavePlugin(fieldsMap)
														
																			 					 
		then:"Validate that plugin has been set"
			assert driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true
		
			
			
		when: "Set preference 61 "
			def preferenceName = "Agent Commission Type"
			EditPrefrencePage allPreference = to(AllPreferencesPage).clickPreferenceByName(preferenceName)
		and: "Preference 61 should be set 'Invoice'"
			assert  preferenceValue.value() == "INVOICE" 
			
			
		then:
			$("#column2 .submit", text: "SAVE CHANGES").click()
			assert $("div.msg-box.successfully > strong").text() == "Done"
		
			
	}
	
	def "The commission balance process" () {
		given : 
			to CustomerPage 
		expect:
			at CustomerPage
			
		when:"Create Order for agent"	
			def currentDate = new DateTime().toDate().format("MM/dd/yyyy")
			def fieldsMap = [orderPeriods: "Monthly", billingTypes: "pre paid", activeSinceDate: currentDate]
			def orderLineMap = [effectiveDate: currentDate]
			
			waitFor { OrderLineChangePage orderLineChangePage = to(CustomerPage)
													.clickCustomer("Customer B")
													.clickCreateOrder()
													.orderDetails(fieldsMap)
													.clickNonAssetProduct("Comm Product") 
													
			OrderReviewPage	orderReviewPage	= orderLineChangePage.updateOrderLine(orderLineMap) 
			OrderListPage orderListPage = orderReviewPage.clickOnSaveOrder() }
					
			String orderSuccessFullmessage = $("div.msg-box.successfully > p").text()
			def orderId = OrderSpec.extractId(orderSuccessFullmessage)
			
			
		then:"Validate Order"
			assert orderId != null
			
		when: "Generate Invoice for above order"
		
			waitFor { InvoiceListPage invoicePage = to(OrderListPage).clickOrder(orderId)
													.clickGenerateInvoice() } 
			
			def successfullMessage = $("div.msg-box.successfully > p").text()											
		then: "validate invoice successfully generated"
			assert successfullMessage != null 

	}
	
	
	def "Generate Commission for Agent a" () {
		given: 
			def price = "US1.00"
			
			to AgentCommissionProcessPage
			def currentDate = new DateTime().toDate().format("MM/dd/yyyy")
			
		expect:
			at 	AgentCommissionProcessPage
		
		when: "Run agent commission process"
			AgentCommissionProcessPage agentCommissionProcessPage = to(AgentCommissionProcessPage)
																		.clickRunAgentProcess([nextRunDate: currentDate, periodValue: "1"])
		then: "validate agent commission successfully run"
			assert  driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true 
		
			
			
		when: "Valdiate Commission "
			ShowCommissionPage showCommissionPage = to(AgentPage)
													.clickAgent("Agent B")
													.clickShowCommission()
													.clickRunDate(currentDate)
									     
		then: "Amount should be '1.00'"
		
				$("#main .even > td").find {
					it.text().replace('$', "").equals(price)
				}.text() != null
		}
		
}
