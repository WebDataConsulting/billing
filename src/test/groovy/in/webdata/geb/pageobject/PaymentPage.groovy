package in.webdata.geb.pageobject

import geb.Page

class PaymentPage extends Page {
	
	static url = "payment/index"
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		payments{ $("a.double.cell > strong ")}
		createPayment (wait: true) {$("a", text: "CREATE PAYMENT")}
	}
	
	ShowPaymentPage clickPayment(def paymentId) {
			payments.find {
				it.text().equals(paymentId)
			}.click()
			
		browser.page(ShowPaymentPage)
		return browser.page
	}
	
	CustomerPage clickAddNewPayment() {
		createPayment.click() 
		
		browser.page(CustomerPage)
		return browser.page
	}
}

class ShowPaymentPage extends Page {
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
	}
	
	static content = {
		editPayment (wait: true) {$(".submit", text: "EDIT")}
		deletePayment (wait: true) {$(".submit", text: "DELETE")}
	}
	
	EditPaymentPage clickEditPayment() {
		editPayment.click() 
		
		browser.page(OrderDetailsPage)
		return browser.page
	}
}


class EditPaymentPage extends Page {
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		
	}
	
	static content = {
		paymentCurrency 		{$("select", id: "payment.currencyId")}
		paymentAmount   		{$("input", id: "payment_amountAsDecimal")}
		paymentDate  			{$("input", id: "payment.paymentDate")}
		isRefund 			{$("input", id: "refund_cb")}
		isProcepaymentInRealTime 	{$("input", id: "processNow")}
		paymentMethod 			{$("select", id: endsWith(".paymentMethodTypeId"))}
		processingOrder  		{$("input").find {it.attr('name').startsWith('paymentMethod_') && it.attr('name').endsWith('processingOrder')}}
		cardHolderName   		{$("div.row > div.inp-bg > input.field").find{it.value()==""}}
		cardNumber   	 		{$("div.row > div.inp-bg > input.field").find{it.value()==""}}
		cardExpiryDate   		{$("div.row > div.inp-bg > input.field").find{it.value()==""}}
		invoices 		 	{$("td.innerContent > div.row > input")}	
		
		reviewPayment (wait: true) 	{$("a", text: "REVIEW PAYMENT")}
		
	}
	
	ReviewPaymentPage clickReviewPayment (def fieldsMap) {
		
		if(fieldsMap['invoices']) invoices.click()
		if(fieldsMap['paymnetAmount']) paymentAmount = fieldsMap['paymentAmount']
		if(fieldsMap['paymentMethod']) paymentMethod.find { it.text() == fieldsMap['selectPayment']}.click()
		if(fieldsMap['processingOrder']) processingOrder = fieldsMap['processingOrder']
		if(fieldsMap['cardHolderName']) cardHolderName = fieldsMap['cardHolderName']
		if(fieldsMap['cardNumber']) cardNumber = fieldsMap['cardNumber']
		if(fieldsMap['cardExpiryDate']) cardExpiryDate = fieldsMap['cardExpiryDate']
		reviewPayment.click()
		
		browser.page(ReviewPaymentPage)
		return browser.page
	}
	
}

class ReviewPaymentPage extends Page {
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		}

	static content = {
		makePayment (wait: true) {$("a", text: "MAKE PAYMENT")}
	}
	
	PaymentPage clickConfirmPayment() {
		makePayment.click() 
		
		browser.page(PaymentPage)
		return browser.page
	}	
} 
