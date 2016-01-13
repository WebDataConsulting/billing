package in.webdata.geb.pageobject

import geb.Page


class BillingProcessPage extends Page {
	
	static url = "billingconfiguration/index"
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		nextRunDate { $("input", id: "nextRunDate")}
		generateReview { $("input", id: "generateReport") }
		daysToReview { $("input", id: "daysForReport")}
		lastDayOfMonth { $("input", id: "lastDayOfMonth") }
		billingPeriod { $("input", id: "periodUnitId")}
		duaDate { $("input", id: "dueDateValue") }
		dueDateUnitId {$("input", id: "dueDateUnitId")}
		recquireRecurring {$("input", id: "onlyRecurring")}
		customerNextInvoiceDate {$("input", id: "invoiceDateProcess")}
		maxOrderPeriodForInvoice {$("input", id: "maximumPeriods")} 
		applyToInvoice {$("input", id: "autoPaymentApplication")}
		alwaysEnableProrate {$("input", id: "billing.proratingType.alwaysProrating")}
		neverEnableProrate {$("input", id: "billing.proratingType.neverProrating")}
		manualEnableProrate {$("input", id: "billing.proratingType.manuallyProrating")}
		
		saveBilling (wait: true) { $("form#save-billing-form > div.btn-box > a.submit.save > span") }
		runBilling (wait: true) { $("form#save-billing-form > div.btn-box > a.submit > span") }
	}
	
	BillingProcessPage clickSaveBillingProcess (def fieldsMap) {
		if(fieldsMap['nextRunDate'])   nextRunDate    = fieldsMap['nextRunDate']
		if(fieldsMap['billingPeriod']) billingPeriod  = fieldsMap['billingPeriod']
		if(fieldsMap['duaDate'])   duaDate    = fieldsMap['duaDate']
		
		saveBilling.click()
		
		return browser.page
	}
	
	BillingProcessPage clickRunBillingProcess (def fieldsMap) { 
		runBilling.click()
		return browser.page
	}

}
