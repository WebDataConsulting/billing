package in.webdata.geb

import geb.spock.GebReportingSpec
import in.webdata.geb.pageobject.CustomerPage
import in.webdata.geb.pageobject.OrderListPage
import in.webdata.geb.pageobject.InvoiceListPage
import in.webdata.geb.pageobject.PaymentPage
import in.webdata.geb.pageobject.ReportTypePage
import in.webdata.geb.pageobject.OrderLineChangePage
import in.webdata.geb.pageobject.OrderReviewPage
import in.webdata.geb.pageobject.RunReportPage
import org.joda.time.DateTime

class ReportSpec extends GebReportingSpec {
	
	def " Create customer for 'TestCustomer3' "() {
		when : "create customer 'TestCustomer3'"
			def fieldsMap = [loginName: 'TestCustomer3', emailId: "testcustomer3@gmail.com",
							paymentMethod: "Credit Card1", processingOrder: "1", cardHolderName: "Ashish", cardNumber: "4111111111111152", cardExpiryDate: "02/2020" ]
			CustomerPage customerPage = to(CustomerPage)
										.clickAddNewCustomer()
										.clickCustomerForm("Prancing Pony", "Direct Customer")
										.clickCustomerFormSubmit(fieldsMap)
										
		then: "page should be customer page "
			driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true 
	
		when: "create order for above customer"
			fieldsMap = [orderPeriods: "Monthly", billingTypes: "pre paid"]
			waitFor{ OrderLineChangePage orderLineChangePage = to(CustomerPage)
													.clickCustomer("TestCustomer3")
													.clickCreateOrder()
													.orderDetails(fieldsMap)
													.clickNonAssetProduct("Product Code1 Description")
													
			OrderReviewPage orderReviewPage = orderLineChangePage.updateOrderLine([:])
			OrderListPage orderListPage = orderReviewPage.clickOnSaveOrder()    }
			
			def orderSuccessFullmessage = $("div.msg-box.successfully > p").text() 
			def orderId = OrderSpec.extractId(orderSuccessFullmessage)
			
		then: "Validate that its successfully created"
			assert driver.getPageSource().contains($("div.msg-box.successfully > p").text()) ==  true 
		
		when: "Generate Invoice for above order"
			InvoiceListPage invoicePage = to(OrderListPage).clickOrder(orderId)
													.clickGenerateInvoice()	
													
		then: "validate invoice successfully generated"
		
		when: "Make payment for above generated invocie "
			fieldsMap = [invoices: true, processingOrder: "1", cardHolderName: "Rohit", cardNumber: "4111111111111152", cardExpiryDate: "12/2020"]
			waitFor{ PaymentPage paymentPage = to(CustomerPage).clickCustomer("TestCustomer3")
									  				  .clickMakePayment()
									                  .clickReviewPayment(fieldsMap)
													  .clickConfirmPayment()   }
													  
			 def successfullMessage =  $("div.msg-box.info > p").text() 	
			 		
		then: "Validate Payment "
			assert successfullMessage != null
										  						
	}
	
	def "Validate Invocie Reports " () {
		given: 
			to ReportTypePage
		expect: 
			at ReportTypePage
		
		when: "Select Invoice Report" 
			def currentDate = new DateTime().toDate().format("MM/dd/yyyy")
			def fieldsMap = [startDate: currentDate, endDate: currentDate, reportFormat: "View as HTML", periodBreakDown: "Day"]
			waitFor{ RunReportPage reportTypePage = to(ReportTypePage).clickReportType("Invoice Reports")
											.clickReport("Total Amount Invoiced")
											.clickRunReport(fieldsMap)
											
			$("a.submit.edit", text: "RUN REPORT").click()
															}
			
		then: "Validate Html report"
			
	}
}
