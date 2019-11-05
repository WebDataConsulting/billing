package in.webdata.geb.pageobject

import geb.Page

class OrderPeriodsPage extends Page {

	static url = "orderPeriod/list"
	
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		addOrderPeriod 		(wait: true ) { $("div#column1.column-hold > div.btn-box > a.submit.add > span") }
		selectOrderPeriod   (wait: true ) { $("a.cell.double > strong") }
	}
	
	AddOrderPeriodsPage clickAddNewOrderPeriod() {
		addOrderPeriod.click()
		
		browser.page(AddOrderPeriodsPage)
		return browser.page
	}
	
	ShowOrderPeriodsPage clickOrderPeriod (def orderPeriodName) {
		
		selectOrderPeriod.find {
			it.text().equalsIgnoreCase(orderPeriodName)
		}.click()
		
		browser.page(ShowOrderPeriodsPage)
		return browser.page
	}
}


class AddOrderPeriodsPage extends Page {
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		$("div#column2.column-hold > div.column-hold > div.heading > strong").text() == "NEW ORDER PERIOD"
	}
	
	static content = {
		orderPeriodDescription {$("input", id: "description")}
		orderPeriodUnit  	   {$("input", id: "periodUnitId")}
		orderPeriodValue	   {$("input", id: "value")}
		
		saveOrderPeriod (wait: true) { $("div.btn-box.buttons > ul > li > a.submit.save > span ") }
	}
	
	OrderPeriodsPage clickSaveOrderChange (def fieldsMap) {
		if(fieldsMap['orderPeriodDescription']) orderPeriodDescription 	= fieldsMap['orderPeriodDescription']
		if(fieldsMap['orderPeriodUnit']) 		orderPeriodUnit 		= fieldsMap['orderPeriodUnit']
		if(fieldsMap['orderPeriodValue']) 		orderPeriodValue 		= fieldsMap['orderPeriodValue']
		
		saveOrderPeriod.click()
			
		browser.page(OrderPeriodsPage)
		return browser.page
	}
	
}

class ShowOrderPeriodsPage extends Page {
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
	}

	
	static content = {
		orderPeriodName {$("div#column2.column-hold > div.column-hold > div.heading > strong ")}
		editOrderPeriod (wait: true ) { $("div#column2.column-hold > div.column-hold > div.btn-box > div.row > a.submit.add > span ") }
		deleteOrderPeriod (wait: true) { $("div#column2.column-hold > div.column-hold > div.btn-box > div.row > a.submit.delete > span") }
		clickYes (wait: true ) {$("ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only > span.ui-button-text")}
	}
	
	AddOrderPeriodsPage clickEditOrderPeriod () {
		editOrderPeriod.click()
		
		browser.page(AddOrderPeriodsPage)
		return browser.page
	}
	
	OrderPeriodsPage clickDeleteOrderPeriod() {
		deleteOrderPeriod.click()
		clickYes.click()
		
		browser.page(OrderPeriodsPage)
		return browser.page
	}
	
	
}

