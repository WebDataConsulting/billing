package in.webdata.geb.pageobject

import geb.Page

class UsersPage extends Page {

	static url = "user/list"
	
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
	
		currentUrl == baseUrl + url
	}
	
	static content = {
		addUser 	(wait: true ) { $("div#column1.column-hold > div.btn-box > a.submit.add > span") }
		selectUser  	(wait: true ) { $("a.cell.double > strong") }
	}
	
	AddUserPage clickAddNewUser() {
		addUser.click()
		
		browser.page(AddUserPage)
		return browser.page
	}
	
	ShowUserPage clickUser (def userName) {
		
		selectUser.find {
			it.text().equals(userName)
		}.click()
		
		browser.page(ShowUserPage)
		return browser.page
	}
}

class AddUserPage extends Page {
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
	
		$("div#main > div.form-edit > div.heading > strong ").text() == "NEW USER"
	}
	
	static content = {
		userName 	{$("input", id: "user.userName")}
		createCred 	{$("input", id: "user.createCredentials")}
		userStatus 	{$("input", id: "user.statusId")}
		userLang 	{$("input", id: "user.languageId")}
		userRole 	{$("input", id: "user.mainRoleId")}
		acctLock 	{$("input", id: "user.isAccountLocked")}
		isAcctExp 	{$("input", id: "user.accountExpired")}
		organizationName 	{$("input", id: "contact.organizationName")}
		firstName 	{$("input", id: "contact.firstName")}
		lastName 	{$("input", id: "contact.lastName")}
		countryCode {$("input", id: "contact.phoneCountryCode")}
		areaCode 	{$("input", id: "contact.phoneAreaCode")}
		phoneNo 	{$("input", id: "contact.phoneNumber")}
		email 		{$("input", id: "contact.email")}
		address1 	{$("input", id: "contact.address1")}
		address2 	{$("input", id: "contact.address2")}
		city 		{$("input", id: "contact.city")}
		state 		{$("input", id: "contact.stateProvince")}
		zipCode 	{$("input", id: "contact.postalCode")}
		country 	{$("input", id: "contact.countryCode")}
		
		includeInNotification {$("input", id: "contact.include")}
		
		saveUser (wait: true ) { $("form#user-edit-form > fieldset > div.buttons > ul > li > a.submit.save > span ") }
	}
	
	UsersPage clickSaveUser (def fieldsMap) {
		if(fieldsMap['userName']) 		  userName    	= fieldsMap['userName']
		if(fieldsMap['organizationName']) organizationName 	= fieldsMap['organizationName']
		if(fieldsMap['email']) 			  email 	= fieldsMap['email']
		
		saveUser.click()
		
		browser.page(UsersPage)
		return browser.page
	}
}

class ShowUserPage extends Page {

	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
			
	}
	
	static content = {
		editUser  (wait: true ) { $("div#column2.column-hold > div.column-hold > div.btn-box > div.row > a.submit.edit > span ") }
		userCode  (wait: true ) { $("div#column2.column-hold > div.column-hold > div.btn-box > div.row > a.submit.show > span ") }
	}
	
	AddUserPage clickEditUser () {
		editUser.click()
		
		browser.page(AddUserPage)
		return browser.page
	}
	
	UserCodeListPage clickUserCode () {
		userCode.click()
		
		browser.page(UserCodeListPage)
		return browser.page
	}
	
}

class UserCodeListPage extends Page {
	
		
		
		static at = {
			waitFor {js.('document.readyState') == 'complete'}
				
			$("div#column1.column-hold > div.heading > strong") == "USER CODES FOR ADMIN"
		}
		
		static content = {
			filterBy 	{ $("input", id: "filterBy") }
			active      { $("input", id: "active") }
			
			selectUserCode  (wait: true ) { $("a.cell.double > strong") }
			addUserCode (wait: true )  { $("div#column1.column-hold >div.btn-box > a.submit.add > span") }
		}
		
		AddUserCodePage clickAddUserCode() {
			addUserCode.click()
			
			browser.page(AddUserCodePage)
			return browser.page
		}
		
		ShowUserCodePage clickUserCode (def userCode) {
			
			selectUserCode.find {
				it.text().equals(userCode)
			}.click()
			
			browser.page(ShowUserCodePage)
			return browser.page
		}
		
}



class AddUserCodePage extends Page {
	
		
		
		static at = {
			waitFor {js.('document.readyState') == 'complete'}
			
			//remian
		}
		
		static content = {
			
			userCode    			{ $("input", id: "identifier") }
			extRef     			{ $("input", id: "externalReference") }
			type        			{ $("input", id: "type") }
			typeDesc    			{ $("input", id: "typeDescription") }
			startDate   			{ $("input", id: "validFrom") }
			endDate     			{ $("input", id: "validTo") }
			
			saveUserCode (wait: true ) 	{ $("form#save-userCode-form > div.buttons > ul > li > a.submit.save > span ") }
		}
		
		UserCodeListPage clickSaveUserCode (def userCodeDescription) {
			userCode = userCodeDescription
			
			saveUserCode.click()
			
			browser.page(UserCodeListPage)
			
			return browser.page
		}
		
}


class ShowUserCodePage extends Page {
	
		def userCode 
		
		static at = {
			waitFor {js.('document.readyState') == 'complete'}
				
			assert $("div#column2.column-hold > div.column-hold > div.heading > strong").text() == userCode
		}
		
		static content = {
			editUserCode  (wait: true ) { $("div#column2.column-hold > div.column-hold > div.btn-box > a.submit.edit > span ") }
			//deactivate    (wait: true ) { $("div.btn-box > a.submit.delete > span ") }        not working yet 
		}
		
		AddUserCodePage clickEditUserCode () {
			editUserCode.click()
			
			browser.page(AddUserCodePage)
			return browser.page
		}
}		
