package in.webdata.geb.pageobject

import geb.Page


class OrderStatusPage extends Page {
	
	static url = "orderStatus/list"
	
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		addOrderStatus 	   (wait: true ) { $("div#column1.column-hold > div.btn-box > a.submit.add > span") }
		selectOrderStatus  (wait: true ) { $("a.cell.double ") }
	}
	
	AddOrderStatusPage clickAddNewOrderStatus() {
		addOrderStatus.click()
		
		browser.page(AddOrderStatusPage)
		return browser.page
	}
	
	ShowOrderStatusPage clickOrderStatus (def orderStatusName) {
		
		selectOrderStatus.find {
			it.text().equalsIgnoreCase(orderStatusName)
		}.click()
		
		browser.page(ShowOrderStatusPage)
		return browser.page
	}
}


class AddOrderStatusPage extends Page {

	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		$("div#column2.column-hold > div.column-hold > div.heading > strong ").text() == "NEW ORDER STATUS"
	}
	
	static content = {
		ordStatusFlag {$("input", id: "orderStatusFlag")}
		ordStatusName {$("input", id: "description")}
		
		saveOrderStatus (wait: true ) { $("div#column1.column-hold  > div.column-hold > div.btn-box.buttons > ul > li > a.submit.save > span ") }
	}
	
	OrderStatusPage clickSaveOrderStatus (def fieldsMap) {
		if(fieldsMap['ordStatusFlag']) ordStatusFlag 	= fieldsMap['ordStatusFlag']
		if(fieldsMap['ordStatusName']) ordStatusName 	= fieldsMap['ordStatusName']
		
		saveOrderStatus.click()
		
		browser.page(OrderStatusPage)
		return browser.page
	}
		
}

class ShowOrderStatusPage extends Page {
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		$("div#column2.column-hold > div.column-hold > div.heading > strong > em ").text().startsWith("ORDER STATUS") == true
	}
	
	static content = {
		editOrderStatus  (wait: true ) { $("div#column2.column-hold > div.column-hold > div.btn-box > div.row > a.submit.add > span ") }
		deleteOrderStatus (wait: true) { $("div#column2.column-hold > div.column-hold > div.btn-box > div.row > a.submit.delete > span") }
		clickYes(wait: true ) {$("ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only > span.ui-button-text")}
	}
	
	AddOrderStatusPage clickEditOrderStatus () {
		editOrderStatus.click()
		
		browser.page(AddOrderStatusPage)
		return browser.page
	}
	
	OrderStatusPage clickDeleteOrderStatus() {
		deleteOrderStatus.click()
		
		clickYes.click()
		
		browser.page(OrderStatusPage)
		return browser.page
	}
	
}
