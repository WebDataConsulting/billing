package in.webdata.geb

import spock.lang.Stepwise;
import geb.spock.GebReportingSpec

import in.webdata.geb.pageobject.OrderLineChangePage
import in.webdata.geb.pageobject.OrderReviewPage
import in.webdata.geb.pageobject.OrderListPage
import in.webdata.geb.pageobject.CustomerPage
@Stepwise
public class OrderHierarchiesSpec extends GebReportingSpec {
	
	
	def "Order Hierarchies Validation" () {
		given: 
			go "product/index"
		
		when: "Create a Product category 'Commission Product'"
			$('#column1 .submit', text: "ADD CATEGORY").click()
			waitFor { $("select", id: "company-select") << "Prancing Pony" }
			$("input", id: "description") << "Dependant Products" 
			$(".submit", text: "SAVE CHANGES").click()
			
		then: "Validate above category has created "
			assert  driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true 
		
		when: "create product 'Product Code=D-01'"
			$('#column1 .double', text: "Dependant Products").click()
			waitFor { $('#column2 .submit', text: "ADD PRODUCT").click() }
			$('input', id: "product.number") << "D-01"
			$('select', id: "newDescriptionLanguage").value("English")
			$('a > img').click()
			$('input', id: "product.descriptions[0].content").value("Installation Fee")
			$("select", id: "company-select") << "Prancing Pony"
			$("select", id: "entitySelect") << "Prancing Pony"
			$("select", id: "currencySelect") << "Unites State Dollars"
			$("input", name: "product.rate") << "50"
			waitFor { $("a").find{it.text().contains("ADD PRICE")}.click() }
			
			$('.submit', text: "SAVE CHANGES").click()
			
		then: "Validate that product has created"
			assert  driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true
			
		when: "create product 'Product Code=D-02'"
			go "product/index"
			
			$('#column1 .double > strong', text: "Dependant Products").click()
			waitFor { $('#column2 .submit', text: "ADD PRODUCT").click() }
			$('input', id: "product.number") << "D-02"
			$('select', id: "newDescriptionLanguage").value("English")
		    $('a > img').click()
			$('input', id: "product.descriptions[0].content").value("Date Service") 
			$("select", id: "company-select") << "Prancing Pony"
			$("select", id: "entitySelect") << "Prancing Pony"
			$("select", id: "currencySelect") << "Unites State Dollars"
			$("input", name: "product.rate") << "10"
			waitFor { $("a").find{it.text().contains("ADD PRICE")}.click() }

			waitFor { $('#dependency .btn-open', text: "DEPENDENCIES").click() }
			waitFor { $('select', id: "product.dependencyItemTypes").find("option").find { it.text() == "Dependant Products"}.click(); }
			 waitFor {$('select', id: "product.dependencyItems" ).find("option").find {it.text().endsWith("Installation Fee") }.click() }
			
			waitFor{$('.short-width3 > a').click() }
			waitFor { $('.submit', text: "SAVE CHANGES").click() }
			
		then: "Validate that product has created"
			assert  driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true	
	}
	
	def "Validate Dependency product in order" () {
		
		when:"Create order with dependent product"
			def fieldsMap = [orderPeriods: "Monthly", billingTypes: "pre paid"]
			OrderLineChangePage orderLineChangePage =  waitFor { to(CustomerPage)
														.clickCustomer("Customer A")
														.clickCreateOrder()
														.orderDetails(fieldsMap)
														.clickNonAssetProduct("Date Service")
							} 
														 
												
			
			//driver.navigate().refresh()
			waitFor { $('a#ui-id-4') .click() }
			waitFor { $('a#ui-id-5') .click() }
			
			waitFor { $('.submit.add', text:"DEPENDENCY").click() }
			$("table",id:startsWith("dependencies-products-change")).find("tr").find("td").find("strong").click();
			waitFor{ $(".ui-button-text", text: "Current order").click() }
			
			OrderReviewPage orderReviewPage = orderLineChangePage.updateOrderLine([:])
			
		then: "Validate on review that both product added in order "
			assert  $("#line span.description", text: "Installation fee") != null
			assert  $("#line span.description", text: "Date Service") != null
			
		when: "Save Order"
			OrderListPage orderListPage = orderReviewPage.clickOnSaveOrder()
			
		then: "Validate Order"
			assert  driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true	
	}
}
