package in.webdata.geb.pageobject

import geb.Page

class LanguagePage extends Page {

	static url = "language/list"
	
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		
		addLanguage (wait: true ) { $("div.btn-box > a.submit.add > span") }
		selectlang {$("a.cell.double > strong")}
	}
	
	AddLanguagePage clickAddLanguage() {
		addLanguage.click()
		browser.page(AddLanguagePage)
		
		return browser.page
	}	
	
	ShowLanguagePage clickLanguage(def langCode) {
		selectlang.each  {
			if(it.text().equals(langCode)) { it.click()}
		}
		
		browser.page(ShowLanguagePage)
		
		return browser.page
	}
	
}


class AddLanguagePage extends Page {
	static at = {
			waitFor {js.('document.readyState') == 'complete'}
		
			$("div#column2.column-hold > div.column-hold > div.heading > strong").text() == "NEW LANGUAGE"
	}
	
	static content = {
		languageCode 		{ $("input", id: "code") }
		languageDescription { $("input", id: "description") }
		saveLanguage (wait: true ) { $("div#column2 > div.column-hold > div.btn-box.buttons > ul > li > a.submit.save > span") }
	}
	
	LanguagePage createLang (def fieldsMap) {
		if(fieldsMap['languageCode']) 		 languageCode 		  = fieldsMap['languageCode']
		if(fieldsMap['languageDescription']) languageDescription  = fieldsMap['languageDescription']
		
		saveLanguage.click()
		
		browser.page(LanguagePage)
		return browser.page
	}
	
}

class ShowLanguagePage extends Page {
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
	}
	
	static content = {
		languageName (wait: true){ $("div#column2 > div.column-hold > div.heading > strong")}
		editLanguage (wait: true ) { $("div#column2 > div.column-hold > div.btn-box > div.row > a.submit.add > span") }
	}
	
	AddLanguagePage clickEditLanguage() {
		editLanguage.click()
		
		browser.page(AddLanguagePage)
		return browser.page
	}
}	

