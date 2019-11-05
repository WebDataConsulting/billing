package in.webdata.geb

import in.webdata.geb.pageobject.CustomerPage
import in.webdata.geb.pageobject.ShowCustomerPage
import in.webdata.geb.pageobject.OrderListPage
import in.webdata.geb.pageobject.OrderReviewPage
import in.webdata.geb.pageobject.OrderLineChangePage
import in.webdata.geb.pageobject.OrderReviewPage
import geb.spock.GebReportingSpec

import java.util.Calendar
import java.util.StringTokenizer;

import org.joda.time.DateTime

import spock.lang.Stepwise;

@Stepwise
class OrderSpec  extends GebReportingSpec {
	
	def "Create an order "() {
		
		given: "at customer page"
			to CustomerPage
			
		when: "Select customer for create order"
			def activeSince = new DateTime().minusMonths(1).toDate().format("MM/dd/yyyy");
			def activeUntil = new DateTime().plusMonths(2).toDate().format("MM/dd/yyyy");
			def fieldsMap   = [orderPeriods: "Monthly", billingTypes: "pre paid", activeSinceDate: activeSince, activeuntil: activeUntil]
			OrderLineChangePage orderLinePage = waitFor { to(CustomerPage)
												.clickCustomer("Ashish")
												.clickCreateOrder()
												.orderDetails(fieldsMap)
												.clickAssetProduct("SIM-card1")
												.clickAsset("SIM-101")
												}
			OrderReviewPage	orderReviewPage	    = orderLinePage.updateOrderLine([assetName: "SIM-101"])
			
			 OrderListPage	orderListPage2	    =waitFor { orderReviewPage.clickOnSaveOrder() }
			String orderSuccessFullmessage      = $("div.msg-box.successfully > p").text()
			def orderId = extractId(orderSuccessFullmessage)
			
		then: "Validate that its successfully created"
			driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true
		
		when: "Edit this order and remove an asset "
			def orderListPage = to(OrderListPage).clickOrder(orderId) 
														.clickEditOrder()
														.goOrderReviewPage()
														.clickProductDescription("SIM-card1")
														.clickChange()  
														
				orderReviewPage	=  orderLinePage.updateOrderLine([isAsset: false]) 
				
				orderListPage 	    =  orderReviewPage.clickOnSaveOrder() 
				
		then: "Order Updated successfully"
			assert $("div.msg-box.successfully > strong").text() ==  "Done"
		
		/*when: "Valdate asset assignment history "
			go "product/index"
			waitFor { $('#column1 .double > strong', text: "Asset Category1").click() }
			waitFor { $('#column2 .double > strong', text: "SIM-card1").click() }
			waitFor { $('.submit', text: "SHOW ASSETS").click() }
			waitFor { $('#column1 .double > strong', text: "SIM-101").click() }
			
		then:
			assert  $("div.heading").find {it.text() == "ASSIGNMENT HISTORY"}.text() != null 
			assert  $("#column2 .innerContent", text: orderId).text() != null*/
			
	}
	
	def "Basic order creation - order changes " () {
		given: "at customer page"
			to CustomerPage
			def orderId
		expect: 
			at 	CustomerPage
		
		when: "Create a new customer ''Customer A" 
			def fieldsMap = [loginName: 'Customer A', emailId: 'customerA@abc.com', 
							paymentMethod: "Credit Card1", processingOrder: "1", cardHolderName: "Ashish", cardNumber: "4111111111111152", cardExpiryDate: "02/2020" ]
			CustomerPage customerPage = to(CustomerPage)
										.clickAddNewCustomer()
										.clickCustomerForm("Prancing Pony", "Direct Customer")
										.clickCustomerFormSubmit(fieldsMap)
		then: ""
			driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true
		
		when: "Create a new order for this customer"
			fieldsMap.clear()
			fieldsMap = [orderPeriods: "Monthly", billingTypes: "pre paid", activeSinceDate: "01/01/2002"]
			def orderLinemap = [effectiveDate: "01/01/2002", quantity: "2"]
			OrderLineChangePage orderLinePage = to(CustomerPage)
												.clickCustomer("Customer A")
												.clickCreateOrder()
												.orderDetails(fieldsMap)
												.clickNonAssetProduct("Long Distance Call - Included")
										
			OrderReviewPage	orderReviewPage	 =	orderLinePage.updateOrderLine(orderLinemap)
			OrderListPage orderListPage      =  orderReviewPage.clickOnSaveOrder()
			String orderSuccessFullmessage   = $("div.msg-box.successfully > p").text() 
			println "order message **********" +orderSuccessFullmessage
			
		then: "Validate that order is created "
			assert driver.getPageSource().contains(orderSuccessFullmessage) == true
			
		when: "Edit above created order "
			orderId = extractId(orderSuccessFullmessage)
			orderLinemap.clear()
			orderLinemap  = [effectiveDate: "01/01/2002", quantity: "2"]
			orderLinePage = to(OrderListPage).clickOrder(orderId)
										  .clickEditOrder()
										  .goOrderReviewPage()
										  .clickProductDescription("Long Distance Call - Included")
										  .clickChange()
			orderReviewPage	         =	orderLinePage.updateOrderLine(orderLinemap)
			orderListPage            =  orderReviewPage.clickOnSaveOrder()
			orderSuccessFullmessage  = $("div.msg-box.successfully > p").text()
			
		then: "validate Order successfully updated"
			
			assert driver.getPageSource().contains(orderSuccessFullmessage) == true
	}
	
	
	static def extractId(String message) {
		StringTokenizer token = new StringTokenizer(message);
		String id;
		while(token.hasMoreTokens()) {
			String tokens = token.nextToken();
			if(tokens.matches("\\d.*\\d")) {
				id = tokens.replace(",", "");
				break;
			}
		}
		return id
	}
}




