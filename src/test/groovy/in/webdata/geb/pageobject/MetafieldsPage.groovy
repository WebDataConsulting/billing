package in.webdata.geb.pageobject

import geb.Page


class MetafieldsCategoriesPage extends Page {
	
	static url = "metaFields/listCategories"
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		selectMetafieldCategory(wait: true) {$("div.table-box > table > tbody > tr > td > a.cell > strong ")}
	}
	
	MetafieldListPage clickMetafieldFCategory(def metafieldCategoryName) {
		
		selectMetafieldCategory.find {
			it.text().equalsIgnoreCase(metafieldCategoryName)
		}.click()
		
		browser.page(MetafieldListPage)
		return browser.page
	}
}


class MetafieldListPage  extends Page {
	
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		$("div#column2.column-hold > div.table-box > table#roles > thead > tr > th ").text() == "META FIELDS"
	}
	
	static content = {
		addMetafield (wait: true ) { $("div.btn-box > a.submit.add > span") }
		selectMetaField(wait: true) {$("div#column2.column-hold > div.table-box > table#roles > tbody > tr > td > a.cell.double > strong ")}
	}
	
	AddMetafieldPage clickAddNewMetafield() {
		addMetafield.click()
		
		browser.page(AddMetafieldPage)
		return browser.page
		
	}
	
	/*AddMetafieldsGroupPage clickAddMetafieldGroup() {
		addMetafield.click()
		
		browser.page(AddMetafieldsGroupPage)
		return browser.page
		
	}*/
	
	ShowMetafieldPage clickMetafield(def metafieldName) {
		
		selectMetaField.find {
			it.text().equalsIgnoreCase(metafieldName)
		}.click()
		
		browser.page(ShowMetafieldPage)
		return browser.page
	}

}


class ShowMetafieldPage  extends Page {
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		// remaining
	}
	
	static content = {
		//mfName {$("")}	
		metafieldheading (wait: true ) {$("div#column2.column-hold > div.heading > strong ")}
		editMetafield (wait: true ) { $("div#column2.column-hold > div.btn-box > div.row > a.submit.edit > span") }
		deleteMetafield (wait: true) {$("div#column2.column-hold > div.btn-box > div.row > a.submit.delete > span ")}
		clickYes(wait: true ) {$("ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only > span.ui-button-text")}
	}
	
	
	AddMetafieldPage clickEditMetafield() {
		editMetafield.click()
		
		browser.page(AddMetafieldPage)
		
		return browser.page
	}
	
	
	MetafieldsCategoriesPage clickDeleteMF() {
		deleteMetafield.click()
		
		clickYes.click()
		
		browser.page(MetafieldsCategoriesPage)
		return browser.page
	}
}
