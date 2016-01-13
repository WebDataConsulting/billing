package in.webdata.geb

import spock.lang.Stepwise;
import in.webdata.geb.pageobject.CustomerPage

import in.webdata.geb.pageobject.ShowCustomerPage
import geb.spock.GebReportingSpec
import in.webdata.geb.pageobject.CustomerNewFormPage
import in.webdata.geb.pageobject.OrderListPage
import in.webdata.geb.pageobject.OrderReviewPage
import in.webdata.geb.pageobject.OrderLineChangePage

@Stepwise
class CustomerSpec extends GebReportingSpec {

	def "Create a customer 'Ashish'"() {
		
		when : "Create a new customer 'Ashish'"
			def fieldsMap = [loginName: 'Ashish', emailId: 'ashishs@gmail.com', nextInvoiceDayOfPeriodId: '1', 
							paymentMethod: "Credit Card1", processingOrder: "1", cardHolderName: "Ashish", cardNumber: "4111111111111152", cardExpiryDate: "02/2020" ]
			CustomerNewFormPage customerNewFormPage = to(CustomerPage).clickAddNewCustomer().clickCustomerForm("Prancing Pony", "Direct Customer")
				customerNewFormPage = customerNewFormPage.clickCompanyBillingCycle()
				
		then: "Validate billing cycle unit and Billing cycle day is updates according Billing Configuration"
			assert mainSubscriptionPeriodId.find('option', value: mainSubscriptionPeriodId.value()).text() == "Monthly"
			assert nextInvoiceDayOfPeriodId.find('option', value: nextInvoiceDayOfPeriodId.value()).text() == "26"
			
			
			
		when: "Save Customer with paymnet method "
			CustomerPage customerPage = customerNewFormPage.clickCustomerFormSubmit(fieldsMap)
				 
		then: "page should be customer page "
			at CustomerPage
			driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true
	}
	
	def "Create new customer with page Object "() {
		
		when : "create a parent customer 'Brian Smith'"
			def fieldsMap = [loginName: 'Brian Smith', emailId: 'bsmith@abc.com', isParent: true, 
							paymentMethod: "Credit Card1", processingOrder: "1", cardHolderName: "Ashish", cardNumber: "4111111111111152", cardExpiryDate: "02/2020" ]
			CustomerPage customerPage = to(CustomerPage)
										.clickAddNewCustomer()
										.clickCustomerForm("Prancing Pony", "Direct Customer")
										.clickCustomerFormSubmit(fieldsMap)
										
		then: "page should be customer page "
			at CustomerPage
			driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true
	}
	
	def " Create child  customer for 'Brian Smith' "() {
		
		when : "create a child customer 'Sarah Wilson'"
			def fieldsMap = [loginName: 'Sarah Wilson', emailId: 'swilson@abc.com',isParent: false,
							paymentMethod: "Credit Card1", processingOrder: "1", cardHolderName: "Ashish", cardNumber: "4111111111111152", cardExpiryDate: "02/2020" ]
			CustomerPage customerPage = to(CustomerPage)
										.clickCustomer("Brian Smith")
										.clickAddChildCusomer()
										.clickCustomerForm("Prancing Pony", "Direct Customer")
										.clickCustomerFormSubmit(fieldsMap)
										
		then: "page should be customer page "
		
			assert $("div.msg-box.successfully > strong").text() ==  "Done"
			
		
	}
	
	def "Validate Parent and child customer" () {
		
		when: "select above create customer 'Sarah Wilson' "
			ShowCustomerPage showCustomerPage = to(CustomerPage)
												.clickCustomer("Sarah Wilson")
		then: "validate parent id "
			assert parentOrChildId.find {
				it.text().equals("Sarah Wilson")
			}.text() != null
		
		
		when: "select customer 'Brian Smith' "
			showCustomerPage = to(CustomerPage)
							.clickCustomer("Brian Smith")
							
		then: "validate that it has child customer "
			at ShowCustomerPage
			assert  parentOrChildId.find {
				it.text().equals("Brian Smith")
			}.text() != null
		}
	
	
	def "Create a one time  discount " () {
		
		given: 
			go "discount/index"
			
		expect:
			driver.currentUrl == System.getProperty("baseUrl").concat("discount/index")
		
		when: "Validate Discount "
			$(".submit", text: "ADD NEW").click()
			$(".submit", text: "SAVE CHANGES").click()
		then: "A validation should be appeared "
			assert $("div.msg-box.error > strong").text() == "Error"
			
		when: "Fill discount data in fields "
			$('input', id: "discount.code").value("Test Discount1") 
			$('select', id: "newDescriptionLanguage").value("English") 
			$('a > img').click() 
			$('input', id: "discount.descriptions[0].content").value("Test Discount1") 
			$('select', id: "discount.type") << "One_Time_Amount"
			$('input', id: "discount.rate").value("5")  
			
			$('.submit', text: "SAVE CHANGES").click()
			
		then: "Validate that discount created "
			assert $("div.msg-box.successfully > strong").text() ==  "Done" 
	}
	 	
	def "create order with above created discount " () {
		
		when: "Creating Order"
			def fieldsMap = [:] 
			OrderLineChangePage orderLinePage =  waitFor { to(CustomerPage)
												.clickCustomer("Ashish")
												.clickCreateOrder()
												.orderDetails(fieldsMap)
												.clickNonAssetProduct("Long Distance Call - Included");}
			OrderReviewPage	orderReviewPage	=	orderLinePage.updateOrderLine([:]) 
			
		then: "Validate that page on Review Tab"
			at OrderReviewPage
		
		when: "Apply Discount "	
			$(".ui-tabs-anchor", text: "DISCOUNTS").click()
			waitFor { $('select').find("option").find {it.text()=="ONE_TIME_AMOUNT - Test Discount1"}.click() } 
		then: "a validation should appear "
			assert $("li").find { it.text() == "Please provide Discountable Item / Order and Discount on the Discount Line."} != null 
						
		
		when: "add product to Discountable Item/Order"
			$(".ui-tabs-anchor", text: "DISCOUNTS").click()
			waitFor { $('select').find("option").find {it.text()== "-- Order Level Discount --"}.click() }
			waitFor { $('select').find("option").find {it.text()=="ONE_TIME_AMOUNT - Test Discount1"}.click() } 
			          OrderListPage orderListPage =  orderReviewPage.clickOnSaveOrder()		
			
		then: "validate that order is successfully created "
			assert  $("div.msg-box.successfully >strong").text() ==  "Done"
	}
	
}
