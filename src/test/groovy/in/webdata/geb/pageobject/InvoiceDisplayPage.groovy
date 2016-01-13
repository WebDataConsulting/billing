package in.webdata.geb.pageobject

import geb.Page

class InvoiceDisplayPage extends Page {
	
	static url = "config/invoice"
	
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
		$("div#column1.column-hold > div.form-edit > div.heading > strong ").text() == "INVOICE DISPLAY"
	}
	
	static content = {
		invoiceNumber {$("input" , id: "number")}
		invoicePrefix {$("input" , id: "prefix")}
		invoiceFile   {$("input" , name: "logo")}
		
		saveInvoiceDisplay (wait: true) { $("div#column1.column-hold >  div.form-edit > div.btn-box > a.submit.save > span") }
	}
	
	InvoiceDisplayPage clickSaveInvoiceDispaly (def fieldsMap) {
		if(fieldsMap['invoiceNumber']) invoiceNumber = fieldsMap['invoiceNumber']
		if(fieldsMap['invoicePrefix']) invoicePrefix = fieldsMap['invoicePrefix']
		if(fieldsMap['invoiceFile'])   invoiceFile   = fieldsMap['invoiceFile']
		
		saveInvoiceDisplay.click()
		return browser.page
	}
}
