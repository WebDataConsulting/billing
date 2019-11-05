package in.webdata.geb.pageobject

import geb.Page

class RolesPage extends Page {

	static url = "role/list"
	
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		currentUrl == baseUrl + url
	}
	
	static content = {
		addRole 	(wait: true )  { $("div#column1.column-hold > div.btn-box > a.submit.add > span") }
		selectRole  (wait: true ) { $("a.cell.double > strong") }
	}
	
	AddRolesPage addNewRole() {
		addRole.click()
		
		browser.page(AddRolesPage)
		
		return browser.page
	}
	
	ShowRolesPage clickRole (def roleName) {
		
		selectRole.find {
			it.text().equals(roleName)
		}.click()
		
		browser.page(ShowRolesPage)
		
		return browser.page
	}
}


class AddRolesPage extends Page {
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		$("div#main > div.form-edit > div.heading > strong ").text() == "NEW ROLE"
	}
	
	static content = {
		roleName {$("input", id: "role.title")}
		roleDescription {$("input", id: "role.description")}
		
		saveRole (wait: true ) { $("div#main > div.form-edit > div.buttons > ul > li > a.submit.save > span ") }
	}
	
	RolesPage clickAddNewRole (def fieldsMap) {
		if(fieldsMap['roleName']) 			roleName 		= fieldsMap['roleName']
		if(fieldsMap['roleDescription'])	roleDescription = fieldsMap['roleDescription']
		
		saveRole.click()
		
		browser.page(RolesPage)
		return browser.page
	}
}

class ShowRolesPage extends Page {
	
	static roleName 
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		assert $("div#column2.column-hold > div.column-hold > div.heading > strong").text() == roleName
	}
	
	static content = {
		editRole  (wait: true ) { $("div#column2.column-hold > div.column-hold > div.btn-box > div.row > a.submit.edit > span ") }
		deleteRole (wait: true) { $("div#column2.column-hold > div.column-hold > div.btn-box > div.row > a.submit.delete > span") }
		clickYes(wait: true ) {$("ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only > span.ui-button-text")}
	}
	
	AddRolesPage clickEditRole () {
		editRole.click()
		
		browser.page(AddRolesPage)
		return browser.page
	}
	
	RolesPage clickDeleteRole() {
		deleteRole.click()
		
		clickYes.click()
		
		browser.page(RolesPage)
		return browser.page
	}
	
}
