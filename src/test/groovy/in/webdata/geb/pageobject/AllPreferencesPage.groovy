package in.webdata.geb.pageobject

import geb.Page

class AllPreferencesPage extends Page {

	static url = "config/index"
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		selectPreferenceByName (wait: true) {$("#column1 .double > strong ")}
		selectPreferenceById (wait: true) {$("#column1 .double > em ")}
	}
	
	EditPrefrencePage clickPreferenceByName(def preferenceName) {
		
		selectPreferenceByName.find {
			it.text().equals(preferenceName)
		}.click()
		
		browser.page(EditPrefrencePage)
		return browser.page
	}
	
	EditPrefrencePage clickPreferenceById(def preferenceId) {
		
		selectPreferenceById.find {
			it.text().equalsIgnoreCase(preferenceId)
		}.click()
		
		browser.page(EditPrefrencePage)
		return browser.page
	}
}

class EditPrefrencePage extends Page {
	
	static at = {	
		waitFor {js.('document.readyState') == 'complete'}
		
		assert $("#column2 .heading", text: "PREFERENCE").text() != null
	}
		
	static content = {
			preferenceValue (wait: true) { $("input", id: "preference.value") }
			savePreference  (wait: true) {$("#column2 .submit", text: "SAVE CHANGES")}
	}
		
	AllPreferencesPage clickSavePreference(def value) {
		
		preferenceValue = value
			
		savePreference.click()
			
		browser.page(AllPreferencesPage)
		return browser.page
	} 
}
