package in.webdata.geb.pageobject

import geb.Page

class EmailPage extends Page {

	static url = "config/email"
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
		$("div#column1.column-hold > div.form-edit > div.heading > strong ").text() == "EMAIL"
	}
	
	static content = {
		paperInvoice 			  { $("input", id: "selfDeliver") }
		customerInInvoice		  { $("input", id: "customerNotes") }	
		daysForOrderNotification1 { $("input", id: "daysForNotification") }
		daysForOrderNotification2 { $("input", id: "daysForNotification2") }
		daysForOrderNotification3 { $("input", id: "daysForNotification3") }
		invoiceReminder 		  { $("input", id: "useInvoiceReminders") }
		firstReminder 			  { $("input", id: "firstReminder") }	
		nextReminder 			  { $("input", id: "nextReminder") }
		
		createEmail (wait: true) { $("div.btn-box > a.submit.save > span ") }
	}
	
	EmailPage createEmail(def fieldsMap) {
		if(fieldsMap['daysForOrderNotification1'])  daysForOrderNotification1 = fieldsMap['daysForOrderNotification1']
		if(fieldsMap['daysForOrderNotification2'])  daysForOrderNotification2 = fieldsMap['daysForOrderNotification2']
		if(fieldsMap['nextReminder'])  				nextReminder 			  = fieldsMap['nextReminder']
		
		createEmail.click()
		return browser.page
	}
	
}	
	
