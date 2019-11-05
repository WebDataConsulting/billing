package in.webdata.geb.pageobject

import geb.Page


class PaymentMethodPage extends Page {

	static url = "paymentMethodType/list"
	
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		addPayment 	(wait: true ) { $("div#column1.column-hold > div.btn-box > a.submit.add > span") }
		selectPaymentMethod  (wait: true ) { $("a.cell.double > strong") }
	}
	
	PaymentMethodTempltePage clickAddNewPaymentMethod() {
		addPayment.click()
		
		browser.page(PaymentMethodTempltePage)
		return browser.page
	}
	
	ShowPaymentMethodPage clickPaymnetMethod (def paymentMethodName) {
		
		selectPaymentMethod.find {
			it.text().equals(paymentMethodName)
		}.click()
		
		browser.page(ShowPaymentMethodPage)
		
		return browser.page
	}
}

class PaymentMethodTempltePage extends Page {
	
		static at = {
			waitFor {js.('document.readyState') == 'complete'}
			
			$("div.form-edit > div.heading").text() == "SELECT PAYMENT METHOD TEMPLATE"
		}
		
		static content = {
			paymentTemplate { $( "input", id: "templateId" )}
			
			selectPaymentMethod (wait: true ) { $("div#column1.column-hold > div.form-edit >  div.buttons > div.btn-row > ul > li > a.submit.save > span ") }
		}
		
		EditPaymentMethodPage clickPaymentMethod () {
			selectPaymentMethod.click()
			
			browser.page(EditPaymentMethodPage)
			return browser.page
		}
}

class EditPaymentMethodPage extends Page {
	
		static at = {
			waitFor {js.('document.readyState') == 'complete'}
			
			$("a#ui-id-1").text() == "DETAILS"
		}
		
		static content = {
			paymentMethodName   	{ $( "input", id: "methodName"  )}
			isRecurring  		{ $( "input", id: "isRecurring" )}
			isAllAccount 		{ $( "input", id: "allAccount"  )}
			accountTypes 		{ $( "input", id: "accountTypes")}
			
			addMetafield (wait: true) { $("div#column1.column-hold > div.btn-box.ait-btn-box > a.submit.save > span")}
			savePaymentMethod (wait: true) { $("div#column2.column-hold > div#review-box > div.btn-box.ait-btn-box > a.submit.save > span") }
		}
		
		
		
		AddMetafieldPage clickAddMetafield() {
			addMetafield.click()
			
			browser.page(AddMetafieldPage)
			return browser.page
		}
		
		PaymentMethodPage clickSavePaymentMethod (def fieldsMap) {
			if(fieldsMap['paymentMethodName']) paymentMethodName = fieldsMap['paymentMethodName']
			if(fieldsMap['isRecurring']) 	   isRecurring       = fieldsMap['isRecurring']
			if(fieldsMap['isAllAccount'])      isAllAccount 		 = fieldsMap['isAllAccount']
			if(fieldsMap['accountTypes'])      accountTypes 		 = fieldsMap['accountTypes']
			
			savePaymentMethod.click()
			
			browser.page(PaymentMethodPage)
			return browser.page
		}
}


class ShowPaymentMethodPage extends Page {
	
		static paymentMethod 
		
		static at = {
			waitFor {js.('document.readyState') == 'complete'}
			
			assert $("div#column2.column-hold > div.column-hold > div.heading > strong").text() == paymentMethod
		}
		
		static content = {
			editPaymentMethod  (wait: true ) { $("div.btn-box > div.row > a.submit.edit > span ") }
			deletePaymentMethod (wait: true) { $("div.btn-box > div.row > a.submit.delete > span") }
			clickYes(wait: true) {$("ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only > span.ui-button-text")}
		}
		
		EditPaymentMethodPage clickEditPaymentMethod () {
			editPaymentMethod.click()
			
			browser.page(EditPaymentMethodPage)
			return browser.page
		}
		
		PaymentMethodPage clickDeletePaymentMethod() {
			deletePaymentMethod.click()
			
			clickYes.click()
			
			browser.page(PaymentMethodPage)
			return browser.page
		}
}
