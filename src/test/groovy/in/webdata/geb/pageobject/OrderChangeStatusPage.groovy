package in.webdata.geb.pageobject

import geb.Page


class OrderChangeStatusPage extends Page {
	
	static url = "config/orderChangeStatuses"
	
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
		$("div.form-edit > div.heading > strong") == "ORDER CHANGE STATUSES"
	}
	
	static content = {
		langId {$("input", id: "languageId")}
		addLanguage (wait: true ) { $("div.btn-box > a.submit.add > span") }
		selectlang  {$("a.cell.double > strong")}
	}	

}
