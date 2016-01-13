package in.webdata.geb.pageobject

import geb.Page
import java.util.Map
import org.openqa.selenium.Keys

class CollectionPage extends Page {

	static url = "config/aging"
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
		$("div#column1.column-hold > div.form-edit > div.heading > strong ").text() == "COLLECTIONS"
	}
	
	static content = {
		ageingSteps { $("tr > td.medium2 > input").find{ StringUtil.isEmpty() } }
		forDays {$("tr > td.medium > input.numericOnly").find{ StringUtil.isEmpty()} }
		saveAgeing (wait: true) { $("form#save-ageing > div#ageing.form-hold > div.btn-row > a.submit.save > span") }
	}
	
	CollectionPage clickSaveAgieng (def fieldsMap) {
		if(fieldsMap['ageingSteps']) ageingSteps.click().value(fieldsMap['ageingSteps'])
		if(fieldsMap['forDays']) forDays.click().value(fieldsMap['forDays'])

		if(fieldsMap['ageingNotification']) { 
			def sendNotification = forDays.attr('id').substring(0, forDays.attr('id')?.indexOf('.')).concat(".sendNotification")
			$("input", name: sendNotification).click()
		}
		
		if(fieldsMap['agiengDuePayment']) {
			def paymentDue = forDays.attr('id').substring(0, forDays.attr('id')?.indexOf('.')).concat(".paymentRetry")
			$("input", name: paymentDue).click()
		}
		
		if(fieldsMap['agiengSuspended']) {
			def suspend = forDays.attr('id').substring(0, forDays.attr('id')?.indexOf('.')).concat(".suspended")
			$("input", name: suspend).click()
		}
		
		saveAgeing.click()
		
		return browser.page
	} 
	
}
