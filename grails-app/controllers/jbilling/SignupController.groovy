/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
You may download the latest source from webdataconsulting.github.io.

 */

package jbilling

import com.sapienter.jbilling.client.EntityDefaults
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.user.UserDTOEx
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.SubscriberStatusDAS
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.db.UserStatusDAS
import com.sapienter.jbilling.server.user.permisson.db.RoleDAS
import com.sapienter.jbilling.server.user.RoleBL
import com.sapienter.jbilling.server.user.contact.db.ContactMapDTO
import com.sapienter.jbilling.server.util.credentials.PasswordService
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.user.ContactWS
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO
import com.sapienter.jbilling.server.util.ServerConstants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.api.validation.EntitySignupValidationGroup
import com.sapienter.jbilling.server.util.db.CurrencyDTO
import com.sapienter.jbilling.server.util.db.JbillingTable
import com.sapienter.jbilling.server.util.db.LanguageDTO
import com.sapienter.jbilling.server.metafields.*
import com.sapienter.jbilling.server.security.JBCrypto

import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.sql.Sql

import javax.validation.groups.Default

import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.NumberUtils

import java.sql.ResultSet

/**
 * SignupController 
 *
 * @author Brian Cowdery
 * @since 10/03/11
 */
class SignupController {
	
	static scope = "singleton"
	
	IWebServicesSessionBean webServicesSession
	
    def webServicesValidationAdvice
    def messageSource
    def viewUtils
    def springSecurityService
    def securityContextLogoutHandler
    def productService
    def PasswordService passwordService;

    def dataSource

	static transactional = true
	
    def index () {
        if (!allowSignup()) {
            render(status: 401, text: 'Unauthorized')
        }
    }

    private boolean allowSignup() {
        boolean retValu= true
        def sql= new Sql(dataSource)
        sql.query("SELECT jb_allow_signup FROM jb_host_master") { ResultSet rs ->
            if (rs.next()) {
                log.info "Allow company signup ${rs.getBoolean(1)}"
                retValu= rs.getBoolean(1)
            }
        }
        sql.close()
        retValu
    }

    def save () {

        if (!allowSignup()) {
            log.error "Sign ups disabled on this jBilling host."
            render(status: 401, text: 'Unauthorized')
            return
        }

		boolean reseller = params['contact.invoiceAsReseller']
        // validate required fields
        try {
            ContactWS contact = new ContactWS()
            bindData(contact, params, 'contact')

	    	if (!StringUtils.isEmpty(contact.phoneCountryCode?.toString())) {
				if (!NumberUtils.isDigits(contact.phoneCountryCode?.toString())) {
					flash.error = 'validation.error.phoneCountryCode.should.be.numeric'
		            render view: 'index'
		            return
				}
	    	}
			
				if (!StringUtils.isEmpty(contact.phoneAreaCode.toString())) {
					if (!NumberUtils.isDigits(contact.phoneAreaCode.toString())) {
						flash.error = 'validation.error.phoneAreaCode.should.be.numeric'
						render view: 'index'
						return
					}
				}

				UserWS user = new UserWS()
				bindData(user, params, 'user')

				user.contact = contact
				webServicesValidationAdvice.validateObject(user, Default.class, EntitySignupValidationGroup.class)

			} catch (SessionInternalError e) {
            	viewUtils.resolveException(flash, session?.locale ?: new Locale("en"), e)
				render view: 'index'
				return
			}
        
			if (params['user.password'] != params.verifiedPassword) {
				flash.error = 'passwords.dont.match'
				render view: 'index'
				return
			}

			if(CompanyDTO.findByDescription(params['contact.organizationName'])) {
				// show a error message and return
				flash.error = 'company.already.exists'
				flash.args = [params.contact.organizationName]
				render view: 'index'
				return
			}
			/*
				Create the new entity, root user and basic contact information
			*/
		 
			// create company
			def language = LanguageDTO.get(params.languageId)
			def currency = CurrencyDTO.get(params.currencyId)
			def company = createCompany(language, currency)
			def companyContact = createCompanyContact(company)
		
			// create root user and contact information
			def user = createUser(language, currency, company)
			def userContact = createUserContact(user)

			// set all entity defaults
			new EntityDefaults(company, user, language, messageSource).init()

            passwordService.createPassword(user)

            //create  Internal type ProductCategory for new Company
            productService.createInternalTypeCategory(company)

			//In case of creation of child entity and Invoice as reseller
			if(springSecurityService.isLoggedIn()){
				//Get current logged in user id
				def loggedInUser = UserDTO.get(springSecurityService.principal.userId)
				//set child company's parent company
				company.parent = loggedInUser.company
			
				boolean rollback = false
				if(reseller){
					UserWS resellerUser= new UserWS()
				
					bindResellerUser(resellerUser,params, loggedInUser.company)
					mockMetaFields(resellerUser,params, loggedInUser.company)
					resellerUser.setCreateCredentials(true)

					log.debug resellerUser
					
					try {
						def resellerId = webServicesSession.createUserWithCompanyId(resellerUser, company.parent.id)
						resellerUser.id = resellerId
						createUserContact(resellerUser)
						
						// mark company as reseller
						company.invoiceAsReseller = reseller
						company.reseller = UserDTO.get(resellerId)
						company.save()
					}
					catch (Exception e) {
						// Roll back transaction if any error occurs
						status.setRollbackOnly()
						rollback = true
					}
					
				}
				
				// if logged in, delete the remember me cookie and log the user out
				// the user should always be shown the login page after signup
				if (rollback) {
					flash.error = 'customer.account.types.not.available'
					render view:'index'
				} else {
					company.save()
					flash.message = 'signup.successful'
					flash.args = [ companyContact.organizationName, user.userName ]
					//response.deleteCookie(SpringSecurityUtils.securityConfig.rememberMe.cookieName)
					//redirect uri: SpringSecurityUtils.securityConfig.logout.filterProcessesUrl
					redirect controller: 'home'
				}
			} else {
            	flash.message = 'signup.successful'
				flash.args = [ companyContact.organizationName, user.userName ]
				
				redirect controller: 'login', action: 'auth', params: [ userName: user.userName, companyId: company.id ]
			}
		}

    /**
     * Create a new company for the given language and currency.
     *
     * @param language
     * @param currency
     * @return created company
     */
    def createCompany(language, currency) {
        def company = new CompanyDTO(
                description: StringUtils.left(params['contact.organizationName'], 100),
                createDatetime: new Date(),
                language: language,
                currency: currency,
                deleted: 0
        ).save()

        // we create logo for the new company from etalon logo file
        new File(companyLogoPath(company.id)).bytes = new File(companyLogoPath(1)).bytes

        return company
    }

    private String companyLogoPath (company_id) {
        return Util.getSysProp("base_dir") + "${File.separator}logos${File.separator}entity-${company_id}.jpg"
    }

    /**
     * Create new root user for the given company, currency, and language.
     *
     * @param language
     * @param currency
     * @param company
     * @return created root user
     */
    def createUser(language, currency, company) {
		UserStatusDAS userStatusDAS = new UserStatusDAS()
        def user = new UserDTO()
        bindData(user, params, 'user')
        def methodId = JBCrypto.getPasswordEncoderId(null);
        user.encryptionScheme = methodId
        user.deleted = 0
        user.userStatus = userStatusDAS.find(UserDTOEx.STATUS_ACTIVE)
        user.subscriberStatus = new SubscriberStatusDAS().find(UserDTOEx.SUBSCRIBER_ACTIVE)
        user.language = language
        user.currency = currency
        user.company = company
        log.debug("Company @@@@@@@@@@@@@@@@: ${company}")
        user.createDatetime = new Date()

		createDefaultRoles(language, company)

		// get root role		
        def rootRole = new RoleDAS().findByRoleTypeIdAndCompanyId(
			ServerConstants.TYPE_ROOT, company.id)

        user.roles.add(rootRole);
        user = user.save(flush:true)

        return user
    }
	
	/**
	 * 	Creates default roles taken from another company
	 * 
	 * @param language
	 * @param currency
	 * @param company
	 * @return
	 */
	def createDefaultRoles(language, company) {

		def defaultRoleList = [ ServerConstants.TYPE_ROOT, ServerConstants.TYPE_CLERK, ServerConstants.TYPE_CUSTOMER, ServerConstants.TYPE_PARTNER ];

		def roleService = new RoleBL();

		defaultRoleList.each() {

			def role = new RoleDAS().findByRoleTypeIdAndCompanyId(
					it as Integer, null)

			// check the initial role ( companyId = null )
			if (!role) {
				// if not initial role set use the latest company role settings available
				def defaultCompanyId = CompanyDTO.createCriteria().get {
					projections {
						min("id")
					}
				}
				role = new RoleDAS().findByRoleTypeIdAndCompanyId(
						it as Integer, defaultCompanyId as Integer)
			}
			
			if (!role) {
				return;
			}

			def newRole = new RoleDTO();
			newRole.company = company;
			newRole.roleTypeId = it

			roleService.create(newRole)
            roleService.setDescription(language.id, role.getDescription(language.id)?:role.getDescription())
            roleService.setTitle(language.id, role.getTitle(language.id)?:role.getTitle(1))

		}
	}
	
	
    /**
     * Create a new primary contact for the given user.
     *
     * @param user
     * @return created user contact
     */
    def createUserContact(user) {
        def userContact = new ContactDTO()
        bindData(userContact, params, 'contact')
        userContact.deleted = 0
        userContact.createDate = new Date()
        userContact.userId = user.id
        userContact.include = 1
        userContact.save()

        // map contact to the user table
        // map contact to the primary contact type
        new ContactMapDTO(
                jbillingTable: JbillingTable.findByName(ServerConstants.TABLE_BASE_USER),
                contact: userContact,
                foreignId: user.id
        ).save(flush:true)

        return userContact
    }

    /**
     * Create a new contact for the company.
     *
     * @param company
     * @return created company contact
     */
    def createCompanyContact(company) {
        def entityContact = new ContactDTO()
        bindData(entityContact, params, 'contact')
        entityContact.deleted = 0
        entityContact.createDate = new Date()
        entityContact.save()

        // map contact to the entity table
        // map contact to the base entity contact type
        new ContactMapDTO(
                jbillingTable: JbillingTable.findByName(ServerConstants.TABLE_ENTITY),
                contact: entityContact,
                foreignId: company.id
        ).save()

        return entityContact
    }
	
	/**
	 * binds param fields to reseller user
	 * 
	 * @param user	: reseller userws
	 * @return
	 */
	def bindResellerUser(user,params, company) {
		user.setCompanyName(company.description)
		user.setUserName(params['contact.organizationName'])
		user.setPassword(ServerConstants.RESELLER_PASSWORD);
		user.setMainRoleId(ServerConstants.TYPE_CUSTOMER);
		user.setLanguageId(company.language.id)
		def accountTypes= com.sapienter.jbilling.server.user.db.AccountTypeDTO.findAllByCompany(company,[sort: "id"]);
		//use first one if exists
		if (accountTypes?.size > 0 ) {
			user.setAccountTypeId(accountTypes.get(0)?.id);
		}
		user.setStatusId(UserDTOEx.STATUS_ACTIVE)
		user.setCurrencyId(company.currency.id)
	}
	/**
	 * Mocks meta field values for the reseller user
	 * 
	 * @param user	:	reseller userws
	 */
	def mockMetaFields(user, params, company) {
		def metaFields = [] 
		
		log.debug "Company ID ${company.id}"
		log.debug "Account Type ID ${user.accountTypeId}"
		
		def metaFieldsDefined= []
		metaFieldsDefined << MetaFieldBL.getAvailableFieldsList(company.id as Integer, EntityType.CUSTOMER as EntityType[])?.findAll {
			it.isMandatory()
		};
		
		Map<Integer, List<MetaField>> allAITMFsByID= MetaFieldBL.getAvailableAccountTypeFieldsMap(user.accountTypeId as Integer)
		allAITMFsByID.each { k, v ->
			for (m in v) {
				if (m.isMandatory()) {
					metaFieldsDefined.add(m);
				}
			}
		}
		
		//use first one if exists
		for (MetaField mf in metaFieldsDefined) {
			if(mf.isMandatory()) {
				def metaField1 = new MetaFieldValueWS();
				metaField1.setFieldName(mf.getName()); //same as the meta field name
				//metaField1.setGroupId(accInfoType.getId());//same as account info type meta field group id
				if (mf.getDefaultValue()) {
					metaField1.setValue(mf.getDefaultValue())
				} else {

					switch(mf.getDataType()) {
						case DataType.STRING:
						case DataType.STATIC_TEXT:
						case DataType.TEXT_AREA:
						case DataType.ENUMERATION:
							metaField1.setValue("-");
							break;
						case DataType.JSON_OBJECT:
							metaField1.setValue("{}");
							break;
						case DataType.INTEGER:
							metaField1.setValue(Integer.valueOf(0));
							break;
						case DataType.DECIMAL:
							metaField1.setValue(BigDecimal.ZERO);
							break;
						case DataType.BOOLEAN:
							metaField1.setValue(false);
							break;
						case DataType.DATE:
							metaField1.setValue(new Date())
							break;
						case DataType.LIST:
							metaField1.setValue([])
							break;
					}

					def emailType = mf.getFieldUsage()
					if (emailType == MetaFieldType.EMAIL ) {
						log.debug params['contact.email']
						log.debug mf.name
						//metaField1.setStringValue(String.valueOf(params['contact.email']))
						metaField1.setValue(String.valueOf(params['contact.email']))
					}

					def orgName = mf.getFieldUsage()
					if (orgName == MetaFieldType.ORGANIZATION ) {
						log.debug company.description
						//metaField1.setStringValue(company.description)
						metaField1.setValue(company.description)
					}
				}
				log.debug "Mandatory Meta field name ${metaField1.getFieldName()}, value: ${metaField1.getValue()}"
				metaFields << metaField1
			}
		}
		
		user.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]))
	}
}
