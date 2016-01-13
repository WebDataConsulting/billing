package in.webdata.geb.pageobject

import geb.Page
import geb.driver.CachingDriverFactory
import in.webdata.geb.pageobject.InvoiceListPage

class OrderDetailsPage extends Page {
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		$('#ui-id-6').text() == "DETAILS"
	}
	
	static content = {
			orderPeriods(wait:true) {$("select", id: "orderPeriod")} 
			billingTypes {$("select", id: "billingTypeId")}
			activeSinceDate {$("input", id: "activeSince")}
			activeUntilDate {$("input", id: "activeUntil")}
			
			clickProdctTab(wait: true) {$('#ui-id-8')}
			
	}
	
	OrderProductPage orderDetails(def fieldsMap) {
		if(fieldsMap['orderPeriods']) orderPeriods	= fieldsMap['orderPeriods']
		if(fieldsMap['billingTypes']) billingTypes	= fieldsMap['billingTypes']
		if(fieldsMap['activeSinceDate']) activeSinceDate	= fieldsMap['activeSinceDate']
		if(fieldsMap['activeUntilDate']) activeUntilDate	= fieldsMap['activeUntilDate']
		
		clickProdctTab.click()
		browser.page(OrderProductPage)
		return browser.page
	}
	
	OrderReviewPage goOrderReviewPage() {
		browser.page(OrderReviewPage)
		return browser.page
	}
	
}


class OrderProductPage extends Page {
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		$('#ui-id-8').text() == "PRODUCTS"
	}
	
	static content = {
			orderPoduct(wait: true ) {$("a.cell.double > strong")} 
	}
	
	OrderAssetPage clickAssetProduct(def productName) {
		
		orderPoduct.find {
					it.text().equals(productName)
			}.click()
		browser.page(OrderAssetPage)
		return browser.page
	}
	
	OrderLineChangePage clickNonAssetProduct(def productName) {
		
		orderPoduct.find {
				it.text().equals(productName)
			}.click()
		browser.page(OrderLineChangePage)
		return browser.page
	}
}

class OrderAssetPage extends Page {
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		$('span > #ui-id-1 > .ui-dialog-title').text() == "CHOOSE ASSETS"
	}
	
	static content = {
		clickOnAsset (wait: true) {$("tr > td.narrow > a.cell.double")}
		addSelected {$("div.btn-box.row > a.submit.row > span")}
		selectedAsset(wait: true){$("ul.cloud > li > strong")} 
		addToOrder(wait: true) { $("div#buttons-id-add.btn-box > a.submit.add > span")}
	 }
	OrderLineChangePage clickAsset(def assetName) {
		
		 clickOnAsset.find("strong").find { 
			 	it.text().equals(assetName)
		 		}.click()
		 
		assert selectedAsset.find {
			it.text().equals(assetName)
		}.text() != null 
		
		
		addToOrder.find{
			it.text().equalsIgnoreCase("ADD TO ORDER")
		}.click();
		browser.page(OrderLineChangePage)
		return browser.page
	}
	
}

class OrderLineChangePage extends Page {
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		$("a", id: "ui-id-5").text() == "LINE CHANGES"
	}
	
	static content = {
		
		effectiveDate {$("input").find { it.attr('name').startsWith('change--') && it.attr('name').endsWith('startDate')}}
		quantity {$("input").find { it.attr('name').startsWith('change--') && it.attr('name').endsWith('quantityAsDecimal')}}
		isAsset {$("input").find { it.attr('name').startsWith('change--') && it.attr('name').endsWith('.status')}}
		assets{$("div.row > label.lb")}
		updateLineChanges(wait: true) {$("a.submit.save > span", text: "UPDATE")}
	}
	
	OrderReviewPage updateOrderLine(def fieldsMap) {
		waitFor{
			assert  $("a", id: "ui-id-5").text() == "LINE CHANGES"
		
			if(fieldsMap['effectiveDate']) effectiveDate = fieldsMap['effectiveDate']
			if(fieldsMap['quantity']) quantity = fieldsMap['quantity']
			if(fieldsMap['isAsset'] == false) { 
				isAsset.value('false')   
			}	 
			
			if(null != fieldsMap['assetName']) {
				assert assets.find {
							it.text().equals(fieldsMap['assetName'])
				}.text() != null
			}
			
			
			updateLineChanges.click() }
			
		browser.page(OrderReviewPage)
		return browser.page
	}
}


class OrderReviewPage extends Page {
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		$("a", id: "ui-id-4").text() == "REVIEW"
	}
	
	static content = {
		changeOrder(wait:true) {$(".submit", text: "CHANGE")}
		productDescription (wait: true){$(".description")}
		saveOrder(wait : true) {$("div.btn-box.order-btn-box> a.submit.save", text: "SAVE CHANGES")}
	}
	
	OrderReviewPage clickProductDescription(def product) {
		productDescription.find {
			it.text().equals(product)
		}.click()
		return browser.page
	}
	
	OrderLineChangePage clickChange() {
		
		changeOrder.click()
		browser.page(OrderLineChangePage)
		return browser.page
	}
	
	OrderListPage clickOnSaveOrder() {
		waitFor{ 
			assert $("a", id: "ui-id-4").text() == "REVIEW"
			assert $("div#review-messages > div.msg-box.successfully").text() == "Updated successfully."
			waitFor{
			saveOrder.click() }
			browser.page(OrderListPage)
			return browser.page
		}
	}
}

class OrderListPage extends Page {
	
	static url = "order/index" 
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		orders{ $("#column1  a.cell")}
	}
	
	ShowOrderPage clickOrder(def orderId) {
		waitFor{
			orders.find {
				it.text().equals(orderId)
			}.click()
	
			browser.page(ShowOrderPage)
			return browser.page
		}
	}
}

class ShowOrderPage extends Page {
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
	}
	
	static content = {
		generateInvoice (wait: true) {$(".submit", text: "GENERATE INVOICE")}
		applyToInvoice (wait: true) {$(".submit", text: "APPLY TO INVOICE")}
		editOrder (wait: true) {$(".submit", text: "EDIT THIS ORDER")}
		deleteOrder (wait: true) {$(".submit", text: "DELETE")}
	}
	
	OrderDetailsPage clickEditOrder() {
		editOrder.click() 
		
		browser.page(OrderDetailsPage)
		return browser.page
	}
	
	InvoiceListPage clickGenerateInvoice() {
		waitFor{
		generateInvoice.click() }
		
		browser.page(InvoiceListPage)
		return browser.page
	}
	
}



