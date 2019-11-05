package in.webdata.geb.pageobject

import geb.Page

import in.webdata.geb.pageobject.AddMetafieldPage

class OrderChangeTypePage extends Page {

	static url = "orderChangeType/list"
	
	
		static at = {
			waitFor {js.('jQuery.active') == 0}
		
			driver.currentUrl == System.getProperty("baseUrl").concat(url)
			
		}
		
		static content = {
			languageId {$("input", id: "languageId")}
			addNewOrderChangeType (wait: true ) { $("div#column1.column-hold > div.btn-box > a.submit.add > span") }
			selectOrderChangeType (wait: true ) { $("a.cell.double > strong") }
		}
		
		AddOrderChangeTypePage clickAddNewOrderChange() {
			addNewOrderChangeType.click()
			
			browser.page(AddOrderChangeTypePage)
			return browser.page
		}
		
		ShowOrderChangeTypePage clickSelectOrdChngType (def ordChngTypeName) {
			
			selectOrderChangeType.find {
				it.text().equalsIgnoreCase(ordChngTypeName) 
			}.click()
			
			browser.page(ShowOrderChangeTypePage)
			return browser.page
		}
	
}


class AddOrderChangeTypePage extends Page {
	
		static at = {
			waitFor {js.('jQuery.active') == 0}
			
			driver.currentUrl == System.getProperty("baseUrl").concat(url)
			
			$("div#main > div.form-edit > div.heading >  strong").text() == "NEW ORDER CHANGE TYPE"
		}
		
		static content = {
			
			orderChangeName {$("input", id: "name")}
			isSelectAll {$("input", id: "defaultType_checkbox")}
			productCategory {$("input", id: "itemTypes_selector")}  
			allowAssetManagement {$("input", id: "allowOrderStatusChange")}
			
			addMataField (wait: true ) { $("div#orderChangeMetaFields.box-cards.box-cards-open > div.box-card-hold > div.type-metafield-header > div.btn-row > a.submit.add > span") }
			saveOrderChangeType (wait: true ) { $("div#main > div.form-edit > div.buttons > ul > li > a.submit.save > span ") }
		}
		
		AddMetafieldPage clickAddNewMetafield() {
			addMataField.click()
			
			browser.page(AddMetafieldPage)
			return browser.page
		}
		
		OrderChangeTypePage clickSaveOrderChangeType (def fieldsMap) {
			if(fieldsMap['orderChangeName']) 		orderChangeName 		= fieldsMap['orderChangeName']
			if(fieldsMap['productCategory'])		productCategory 		= fieldsMap['productCategory']
			if(fieldsMap['allowAssetManagement']) 	allowAssetManagement  	= fieldsMap['allowAssetManagement']
			if(fieldsMap['isSelectAll']) 			isSelectAll  			= fieldsMap['isSelectAll']
			
			saveOrderChangeType.click()
			
			browser.page(OrderChangeTypePage)
			return browser.page
		}
		
}

class ShowOrderChangeTypePage extends Page {
	
		static at = {
			waitFor {js.('jQuery.active') == 0}
		
			driver.currentUrl == System.getProperty("baseUrl").concat(url)
			
		}
	
	
		
		static content = {
			orderChangeType                     {$("div#column2.column-hold > div.column-hold > div.heading > strong ")}
			editOrderChangeType   (wait: true)  {$("div#column1.column-hold > div.column-hold > div.btn-box > div.row > a.submit.edit > span ") }
			deleteOrderChangeType (wait: true)  {$("div#column1.column-hold > div.column-hold > div.btn-box > div.row > a.submit.delete > span") }
			clickYes              (wait: true)  {$("ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only > span.ui-button-text")}
		}
		
		AddOrderChangeTypePage clickEditOrderChangeType () {
			editOrderChangeType.click()
			
			browser.page(AddOrderChangeTypePage)
			return browser.page
		}
		
		OrderChangeTypePage clcikDeleteOrderChangeType() {
			deleteOrderChangeType.click()
			
			
			clickYes.click()
			
			browser.page(OrderChangeTypePage)
			return browser.page
		}
}		
