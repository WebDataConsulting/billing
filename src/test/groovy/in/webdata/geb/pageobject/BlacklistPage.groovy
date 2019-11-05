package in.webdata.geb.pageobject

import geb.Page

class BlacklistPage extends Page {

	static url = "blacklist/list"
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		csvFile       { $("input" , name: "csv")}
		addRecords    { $("input" , id: "csvUpload.add") }
		modifyRecords { $("input" , id: "csvUpload.modify") }
		filterBy      { $("input" , id: "filterBy") } 

		updateButton (wait: true) { $("form#save-blacklist-form > div.btn-row > a.submit.save > span") }
	}
	
	BlacklistPage clickUpdate(def fieldsMap) {
		
		if(fieldsMap['csvFile']) 		csvFile 	  = fieldsMap['csvFile']
		if(fieldsMap['addRecords']) 	addRecords 	  = fieldsMap['addRecords']
		if(fieldsMap['modifyRecords']) 	modifyRecords = fieldsMap['modifyRecords']
		
		updateButton.click()
		
		return browser.page
		
	}
	
}
