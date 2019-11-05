package in.webdata.geb.pageobject

import geb.Page


class CurrencyPage extends Page {

	static url = "config/currency"
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
		$("div#currList.form-edit > div.heading > strong ").text() == "CURRENCIES"
	}
	
	static content = {
		defualtCurrency {$("input", id: "defaultCurrencyId")}
		startDate {$("input", id: "startDate")}
		addNewCurrency (wait: true ) { $("div.row > div > a.submit.add > span") }
	}
	
	AddNewCurrencyPage clickAddNewCurrency() {
		addNewCurrency.click()
		
		browser.page(AddNewCurrencyPage)
		
		return browser.page
	}
}


class AddNewCurrencyPage extends Page {
	
		static at = {
			waitFor {js.('document.readyState') == 'complete'}
			
			$("div#column2.column-hold > div.column-hold > div.heading > strong ").text() == "NEW CURRENCY"
		}
		
		static content = {
			currencyName   {$("input", id: "description")}
			currencyCode   {$("input", id: "code")}
			currencySymbol {$("input", id: "symbol")}
			countryCode    {$("input", id: "countryCode")}
			exchangeRate   {$("input", id: "rate")}
			systemRate     {$("input", id: "sysRate")}
			active         {$("input", id: "inUse")}
			saveCurrency (wait: true ) { $("div#column2.column-hold > div.column-hold > div.button-box.buttons > ul > li > a.submit.save > span") }
		}
		
		CurrencyPage clickSaveCurrency (def fieldsMap) {
			
			if(fieldsMap['currencyName'])    currencyName 		= fieldsMap['currencyName']
			if(fieldsMap['currencyCode'])    currencyCode 		= fieldsMap['currencyCode']
			if(fieldsMap['currencySymbol'])  currencySymbol 	= fieldsMap['currencySymbol']
			if(fieldsMap['countryCode'])     countryCode 		= fieldsMap['countryCode']
			if(fieldsMap['exchangeRate'])    exchangeRate 		= fieldsMap['exchangeRate']
			if(fieldsMap['systemRate'])      systemRate 		= fieldsMap['systemRate']
			if(fieldsMap['active'])          active 			= fieldsMap['active']
			
			saveCurrency.click()
			
			browser.page(CurrencyPage)
			
			return browser.page
		}
}		
