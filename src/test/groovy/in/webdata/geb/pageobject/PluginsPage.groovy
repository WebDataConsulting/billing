package in.webdata.geb.pageobject

import java.util.Map;

import geb.Page

class PluginsCategoryPage extends Page {
	
	static url = "plugin/index"
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		selectPluginCategoryById(wait: true) {$("#column1 a ")}
		selectPluginCategoryByClass(wait: true) {$("#column1 a > strong ")}
	}
	
	PluginsListPage clickPluginCategoryByClass(def pluginCategoryClass) {
			selectPluginCategory.find {
				it.text().equals(pluginCategoryClass)
			}.click()
			
		browser.page(PluginsListPage)
		return browser.page
	}
	
	PluginsListPage clickPluginCategoryById(def pluginCategoryId) {
			selectPluginCategoryById.find {
				it.text().equals(pluginCategoryId)
			}.click()
			
		browser.page(PluginsListPage)
		return browser.page
	}

}

class PluginsListPage  extends Page {
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
	}
	
	static content = {
		addPlugin (wait: true ) { $("#column2 .submit.add") }
		selectPlugin(wait: true) {$("table > tbody > tr > td > a > em.tiny ")}
	}
	
	AddPluginPage clickAddNewPlugin () {
		
		addPlugin.click() 
		
		browser.page(AddPluginPage)
		return browser.page
	}
	
	ShowPluginPage clickPlugin(def pluginClass) {
			selectPlugin.find {
				it.equals(pluginClass)
			}.click()
			
		browser.page(ShowPluginPage)
		return browser.page
	}

}

class AddPluginPage extends Page {
	
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		// remaining 
	}
	
	static content = {
		pluginType  { $("select", id: "typeId") }
		processingOrder { $("input", id: "processingOrder") }
		pluginNotes { $("input", id: "notes") }
		 
		savePlugin (wait: true ) { $("#plugin-form .submit.save ") }
	}
	
	PluginsCategoryPage clickSavePlugin (def fieldsMap) {
			if(fieldsMap['pluginType']) 	 pluginType       = fieldsMap['pluginType']
			if(fieldsMap['processingOrder']) processingOrder  = fieldsMap['processingOrder']
			
			savePlugin.click()
			
		browser.page(PluginsCategoryPage)
		return browser.page
	}
}

class ShowPluginPage  extends Page {
	
	static pluginName
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		assert $("div#column2.column-hold > div.column-hold > div.heading > strong").text() == pluginName
	}
	
	static content = {
		editPlugin (wait: true ) { $("div#column2.column-hold > div.btn-box > a.submit > span") }
		deletePlugin (wait: true) {$("div#column2.column-hold > div.btn-box > a.submit.delete > span ")}
		clickYes(wait: true ) {$("ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only > span.ui-button-text")}
	}
	
	
	AddPluginPage clickEditPlugin() {
		editPlugin.click() 
		
		browser.page(AddPluginPage)
		return browser.page
	}
	
	
	PluginsCategoryPage clickDeletePlugin() {
			deletePlugin.click()
			clickYes.click()
			
		browser.page(PluginsCategoryPage)
		return browser.page
	}
}	
