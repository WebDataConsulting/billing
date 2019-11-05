package jbilling

import java.util.Collections;
import java.util.Locale;

import grails.test.mixin.*;
import grails.plugin.springsecurity.SpringSecurityService

import com.sapienter.jbilling.server.util.api.validation.APIValidator
import com.sapienter.jbilling.client.EntityDefaults
import com.sapienter.jbilling.client.authentication.CompanyUserDetails


import com.sapienter.jbilling.server.util.db.JbillingTable
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.contact.db.ContactMapDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.db.CurrencyDTO
import com.sapienter.jbilling.server.util.db.LanguageDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO

@TestFor(SignupController)
class SignupControllerTests{

	void testSave_NotLoggedIn() {
		
		mockParam()
		mockAPIValidator()
		mockMetaClasses()
		
		mockController(SignupController)
		controller.metaClass.createUser = {userPram, langugaeParam, currencyParam -> mockAdminUser()}
		
		def springSecurityService = new Object()
		springSecurityService.metaClass.isLoggedIn = {-> false}
		controller.springSecurityService = springSecurityService
		
		controller.save()
		assert response.redirectedUrl == "/login/auth?userName=user_reseller&companyId=1"
	}
	
	void testSave_LoggedIn() {
		
		mockParam()
		mockAPIValidator()
		mockMetaClasses()
		
		mockController(SignupController)
		controller.metaClass.createUser = {userPram, langugaeParam, currencyParam -> mockAdminUser()}
		
		def springSecurityService = new SpringSecurityService()
		springSecurityService.metaClass.isLoggedIn = {-> true}
		
		CompanyUserDetails principal = new CompanyUserDetails("admin", "123qwe", true, true, true, true,Collections.EMPTY_LIST, new Locale("English"), 1, 1, 1, 1, 1);

		springSecurityService.metaClass.getPrincipal = {-> principal}
		
		controller.springSecurityService = springSecurityService
		
		//mockResponse(controller.response)
		
		controller.save()
		assert response.redirectedUrl == "/j_spring_security_logout"
	}
	
	def mockParam() {
		
		params['userName'] = "user_reseller"
		params['password'] = "password123"
		params['verifiedPassword'] = "password123"
		params['firstName'] = "Mr."
		params['lastName'] = "Smith"
		params['phoneCountryCode'] = "00"
		params['phoneAreaCode'] = "000"
		params['phoneNumber'] = "00000"
		params['email'] = "abc@xyz.com"
		params['languageId'] = "1"
		params['currencyId'] = "1"
		params['organizationName'] = "Reseller"
		params['invoiceAsReseller'] = "true"
		params['address1'] = "Address 1"
		params['address2'] = "Address 2"
		params['city'] = "City"
		params['stateProvince'] = "Province"
		params['countryCode'] = "PK"
		params['postalCode'] = "00000"
		
		params['user.password'] = "password123"
		params['contact.organizationName'] = "Reseller"
	}
	
	def mockMetaClasses() {
		//mock withTransaction
		CompanyDTO.metaClass.static.withTransaction = { Closure callable ->  callable.call(null) }
		
		CompanyDTO.metaClass.static.findByDescription = {keyword -> null }
		
		LanguageDTO.metaClass.static.get = {keyword -> mockLanguage()}
			
		CurrencyDTO.metaClass.static.get = {keyword -> mockCurrency()}
		
		UserDTO.metaClass.static.get = {keyword -> mockLoggedInUser()}
		
		CompanyDTO.metaClass.static.save = {companyParam -> mockCompany() }
		
		ContactDTO.metaClass.static.save = {contactParam -> }
		
		ContactMapDTO.metaClass.static.save = {contactMapParam -> }
		
		JbillingTable.metaClass.static.findByName = { keyword -> mockTable()}
			
		mockFor(EntityDefaults)
		// mocking constructor
		EntityDefaults.metaClass.constructor = {param1, param2, param3, param4 -> new EntityDefaults()}
		EntityDefaults.metaClass.init = {}

		/*def passwordEncoder = mockFor(JBillingPasswordEncoder)
		 passwordEncoder.demand.encodePassword() {"encodedpassword"}
		 controller.passwordEncoder = passwordEncoder.createMock()
		 
		 UserStatusDTO userStatus = new UserStatusDTO()
		 userStatus.id = 1
		 userStatus.canLogin = 1*/
		 
		 /*def userStatusDAS = mockFor(UserStatusDAS)
		 passwordEncoder.demand.find() {userStatus}*/
	}
	
	def mockAPIValidator() {
		def webServicesValidationAdvice = mockFor(APIValidator)
		webServicesValidationAdvice.demand.validateObject() {}
		controller.webServicesValidationAdvice = webServicesValidationAdvice.createMock()
	}
	
	def mockTable() {
		JbillingTable table = new JbillingTable()
		table.id = 5
		table.name = "entity"
		
		return table
	}
	
	def mockCompany() {
		CompanyDTO company = new CompanyDTO()
		company.id = 1
		company.language = mockLanguage()
		company.currencyDTO = mockCurrency()
		
		return company
		
	}
	
	def mockLanguage() {
		LanguageDTO mockedLanguage = new LanguageDTO()
		mockedLanguage.id = 1
		mockedLanguage.code = "en"
		mockedLanguage.description = "English"
		
		return mockedLanguage
	}
	
	def mockCurrency() {
		CurrencyDTO mockedCurrency = new CurrencyDTO()
		mockedCurrency.id = 1
		mockedCurrency.code = "USD"
		mockedCurrency.countryCode = "US"
		
		return mockedCurrency
	}
	
	def mockAdminUser() {
		def mockedUser = new UserDTO()
		mockedUser.userName = "user_reseller"
		mockedUser.id = 10
		return mockedUser
	}
	
	def mockLoggedInUser() {
		def mockedUser = new UserDTO()
		mockedUser.userName = "admin"
		mockedUser.id = 1
		mockedUser.company = mockCompany()
		return mockedUser
	}
}
