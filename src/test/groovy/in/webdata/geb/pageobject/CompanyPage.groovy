package in.webdata.geb.pageobject

import geb.Page


class CompanyPage extends Page {
	
	static url = "config/company"
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
		$("div#column1.column-hold > div.form-edit > div.heading > strong ").text() == "COMPANY"
	}
	
	static content = {
		companyName { $("input", id: "description") }
		address1 {$("input", id: "address1")}
		address2 {$("input", id: "address2")}
		city {$("input", id: "city")}
		state {$("input", id: "stateProvince")}
		zipCode {$("input", id: "postalCode")}
		country {$("input", id: "countryCode")}
		defaultCurrency {$("input", id: "currencyId")}
		language {$("input", id: "languageId")}
		saveCompany (wait: true) { $("form#save-company-form > div.btn-box > a.submit.save > span") }
	}
	
	CompanyPage clickUpdateCompany (def fieldsMap) {
		
		if(fieldsMap['companyName']) 	companyName 	= fieldsMap['companyName']
		if(fieldsMap['address1']) 		address1 		= fieldsMap['address1']
		if(fieldsMap['address2']) 		address2 		= fieldsMap['address2']
		if(fieldsMap['city']) 			city 			= fieldsMap['city']
		if(fieldsMap['state']) 			state 			= fieldsMap['state']
		if(fieldsMap['zipCode']) 		zipCode 		= fieldsMap['zipCode']
		if(fieldsMap['country']) 		country 		= fieldsMap['country']
		if(fieldsMap['language']) 		language  		= fieldsMap['language']
		if(fieldsMap['defaultCurrency']) defaultCurrency = fieldsMap['defaultCurrency']
		
		saveCompany.click()
		
		return browser.page
	}
	
}
