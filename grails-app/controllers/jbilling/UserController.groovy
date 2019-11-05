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

import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.client.user.UserHelper
import com.sapienter.jbilling.client.util.FlowHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.client.authentication.exception.LicenseExpiredException
import com.sapienter.jbilling.client.authentication.exception.LicenseInvalidException
import com.sapienter.jbilling.client.authentication.exception.LicenseMissingException
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.UserCodeWS
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.UserCodeDTO
import com.sapienter.jbilling.server.user.db.UserCodeLinkDAS
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO
import com.sapienter.jbilling.server.util.ServerConstants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.db.EnumerationDTO

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.plugin.springsecurity.SpringSecurityUtils

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.apache.commons.lang.ArrayUtils

import org.hibernate.FetchMode as FM
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter

@Secured(["isAuthenticated()"])
class UserController {
	static scope = "prototype"
    static pagination = [ max: 10, offset: 0 ]

    static final viewColumnsToFields =
            ['userId': 'id',
             'userName': 'contact.lastName, contact.firstName',
             'organization': 'contact.organizationName']

    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils

    def breadcrumbService
    def recentItemService
    def springSecurityService
    def securitySession
    def subAccountService
    def userService

    def index () {
        redirect action: 'list', params: params
    }

    def getList(params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
		def company_id = session['company_id']
        return UserDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            and {
                or {
                    isEmpty('roles')
                    roles {
                        ne('roleTypeId', CommonConstants.TYPE_CUSTOMER)
                        ne('roleTypeId', CommonConstants.TYPE_PARTNER)
                    }
                }

                eq('company', new CompanyDTO(company_id))
                eq('deleted', 0)
                createAlias('contact', 'contact')
                if(params.userId) {
                    eq('id', params.int('userId'))
                }
                if(params.organization) {
                    addToCriteria(Restrictions.ilike("contact.organizationName",  params.organization, MatchMode.ANYWHERE) );
                }
                if(params.userName) {
                    or{
                        addToCriteria(Restrictions.ilike("userName",  params.userName, MatchMode.ANYWHERE))
                        addToCriteria(Restrictions.ilike("contact.firstName", params.userName, MatchMode.ANYWHERE))
                        addToCriteria(Restrictions.ilike("contact.lastName", params.userName, MatchMode.ANYWHERE))
                    }
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    def list () {
        def currentUser = springSecurityService.principal
        UserWS user
        def selected = params.id ? UserDTO.get(params.int("id")) : null
        def contact = selected ? ContactDTO.findByUserId(selected.id) : null
		
        def crumbDescription = selected ? UserHelper.getDisplayName(selected, contact) : null
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id, crumbDescription)
        user = params.id ? webServicesSession.getUserWS(params.int('id')) : new UserWS()
		if (selected?.deleted == 1) {
            flash.message = 'user.edit.deleted'
            flash.args = [ params.id ]
        }

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], CommonConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'usersTemplate', model: [selected: selected, currentUser: currentUser, contact: contact]
            }else {
                [selected: selected, currentUser: currentUser, contact: contact]
            }
            return
        }

        def users = getList(params)
        //Check if account is locked so that it can be shown on UI appropriately
        if( selected && user?.id) {
            selected.setAccountLocked(user?.isAccountLocked)
        }

        if (params.applyFilter || params.partial) {
            render template: 'usersTemplate', model: [users: users, selected: selected, currentUser: currentUser, contact: contact]
        } else {
            [users: users, selected: selected, currentUser: currentUser, contact: contact]
        }
    }

    /**
     * JQGrid will call this method to get the list as JSon data
     */
    def findUsers () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def users = getList(params)

        try {
            def jsonData = getUsersJsonData(users, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts Users to JSon
     */
    private def Object getUsersJsonData(users, GrailsParameterMap params) {
        def jsonCells = users
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    /* Source of alternate flows
       - MyAccountController
     */
    @Secured(["isFullyAuthenticated()"])
    def show () {
        def currentUser = springSecurityService.principal
        def user

        //load the user by id or user code
        if(params['id']) {
            user = UserDTO.get(params.int('id'))
        } else {
            user = new UserBL().findUserCodeForIdentifier(params['userCode'], session['company_id']).user
        }
        def contact = user ? ContactDTO.findByUserId(user.id) : null
        user.accountLocked = new UserBL(user.id).isAccountLocked()

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, user.userId, UserHelper.getDisplayName(user, contact))

        if (flash.altView || params.partial){
            FlowHelper.display(this, true, [ template: '/user/show',
                                             model: [ selected: user, currentUser: currentUser, contact: contact ]])
        }else{
            redirect(controller: "myAccount", action: "index")
        }
    }

    /*
     Source of alternate flows
      - MyAccountController
     */
    def edit () {
        UserWS user
        def contacts

        try {
            user = params.id ? webServicesSession.getUserWS(params.int('id')) : new UserWS()
            if (user.deleted==1)
            {
                redirect controller: 'user', action: 'list'
                return
            }
            contacts = params.id ? webServicesSession.getUserContactsWS(user.userId) : null

        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)

            flash.error = 'user.not.found'
            flash.args = [params.id as String]

            redirect controller: 'user', action: 'list'
            return
        }
		
		def company_id = session['company_id']
        def company = CompanyDTO.createCriteria().get {
            eq("id", company_id)
            fetchMode('contactFieldTypes', FM.JOIN)
        }
        UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
		FlowHelper.display(this, true, [ view: '/user/edit',
			model: [ user: user, contacts: contacts, company: company, roles: loadRoles(), loggedInUser: loggedInUser ]])

    }



    /* Source of alternate flows
        - /myAccount/editUser
     */
    /**
     * Validate and save a user.
     */
    def save () {
        UserWS user = new UserWS()
        UserHelper.bindUser(user, params)
        def contacts = []

		def userId= params['user']['userId'] as Integer

		log.debug "Save called for user ${userId}"

		def oldUser = userId ? webServicesSession.getUserWS(userId) : null

		def company_id = session['company_id']
        def company = CompanyDTO.createCriteria().get {
            eq("id", company_id)
            fetchMode('contactFieldTypes', FM.JOIN)
        }

		//edit my account fields permission
		if ( !oldUser || SpringSecurityUtils.ifAllGranted('ROLE_SUPER_USER') || SpringSecurityUtils.ifAllGranted('MY_ACCOUNT_162') ) {
			UserHelper.bindUser(user, params)
			UserHelper.bindContacts(user, contacts, company, params)
		} else {
			user= oldUser
			contacts= userId ? webServicesSession.getUserContactsWS(userId) : null
		}
        

		//change password permission
		if ( !oldUser || SpringSecurityUtils.ifAllGranted('ROLE_SUPER_USER') || SpringSecurityUtils.ifAllGranted('MY_ACCOUNT_161') ) {
			UserHelper.bindPassword(user, oldUser, params, flash)
		} else {
			user.password= null
		}
		UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
        if (flash.error) {
			user = new UserWS()
			UserHelper.bindUser(user, params)
			contacts = []
			UserHelper.bindContacts(user, contacts, company, params)
            render view: 'edit', model: [user: user, contacts: contacts, company: company, loggedInUser: loggedInUser, roles: loadRoles()]
            return
        }

        try {
            // save or update
            if (!oldUser) {
                log.debug("creating user ${user}")

                user.userId = webServicesSession.createUser(user)

                flash.message = 'user.created'
                flash.args = [user.userId as String]

            } else {
                log.debug("saving changes to user ${user.userId}")

                webServicesSession.updateUser(user)

                flash.message = 'user.updated'
                flash.args = [user.userId as String]
            }

            // save secondary contacts
            if (user.userId) {
                contacts.each {
                    webServicesSession.updateUserContact(user.userId, it);
                }
            }

        } catch (SessionInternalError e) {
            flash.clear()
            viewUtils.resolveException(flash, session.locale, e)
			contacts = userId ? webServicesSession.getUserContactsWS(userId) : null
            if(!contacts && !userId){
                contacts = [user.getContact()]
            }
            render view: 'edit', model: [user: user, contacts: contacts, company: company, loggedInUser: loggedInUser, roles: loadRoles()]
            return
        }

		if ( SpringSecurityUtils.ifAnyGranted("MENU_99") || SpringSecurityUtils.ifAnyGranted("ROLE_SUPER_USER") ) {
			chain action: 'list', params: [id: user.userId]
		} else {
			chain action: 'edit', params: [id: user.userId]
		}
    }

    def delete () {
        try {
            if (params.id) {
                webServicesSession.deleteUser(params.int('id'))
                log.debug("Deleted user ${params.id}.")
            }

            flash.message = 'user.deleted'
            flash.args = [params.id as String]

            // render the partial user list
            params.applyFilter = true
        } catch (SessionInternalError e) {
            flash.error = message(code: "user.validation.cannot.delete.itself")
            redirect(controller: 'user', action: 'list', params: params)
            return
        }
        redirect(action: 'list')
    }

    @Secured('isAuthenticated()')
    def reload () {
        log.debug("reloading session attributes for user ${springSecurityService.principal.username}")

        securitySession.setAttributes(request, response, springSecurityService.principal)
        reloadURL(null)

    }

    def reloadURL(String errorMessage) {
        breadcrumbService.load()
        recentItemService.load()

        def breadcrumb = breadcrumbService.getLastBreadcrumb()
        if (breadcrumb) {
            // show last page viewed
            redirect(controller: breadcrumb.controller, action: breadcrumb.action, id: breadcrumb.objectId)
            if(errorMessage) {
                flash.errorAuthToFail = errorMessage
            }
        } else {
            // show default page
            redirect(controller: 'customer')
        }
    }

	def getAdminByCompany () {
		def users = UserDTO.findAllByCompany(CompanyDTO.get(params.entityId), [max: 1, sort: "id", order: "asc"])
		if(users.size() > 0) {
			render(contentType: "text/json") {name = users?.get(0).userName + ";" + params.entityId}
		}
	}
	
	def getUserByCompany (){
		def user = UserDTO.get(session["user_id"]);
		if(user) {
			render(contentType: "text/json") {name = user?.userName + ";" + params.entityId}
		}
	}

    private List loadRoles() {
        return RoleDTO.createCriteria().list() {
            eq('company', new CompanyDTO(session['company_id']))
			ne('roleTypeId', CommonConstants.TYPE_CUSTOMER)
			ne('roleTypeId', CommonConstants.TYPE_PARTNER)
            order('id', 'asc')
        }
    }

    /**
     * Display the list of user codes
     */
    def userCodeList () {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order


        UserDTO user = UserDTO.get(params.int('id'))

        //add breadcrumb
        breadcrumbService.addBreadcrumb(controllerName, 'userCodeList', null, params.int('id'), user?.userName)

        if(params['filterBy'] == null) params.active = 'on'

        params.put("userId", params.id)
        params.put("showActive", 'on' == params.active ? 1 : 0)

        def userCodes
        try {
            userCodes = userService.getFilteredUserCodes(params)
        } catch (SessionInternalError e) {
            userCodes = []
            viewUtils.resolveException(flash, session.locale, e);
        }

        def model = [userCodes: userCodes, user: user] + isLoggedInUserPartnerModel()

        if (params.applyFilter || params.partial) {
            render(template: 'userCodeList', model: model)
        } else {
            render(view: 'userCodeListView', model: model)
        }
    }

    def userCodeShow () {
        if (!params.id) {
            flash.error = 'userCode.not.selected'
            flash.args = [ params.id  as String]

            redirect controller: 'user', action: 'userCodeList'
            return
        }

        //load the user code
        def userCode = UserCodeDTO.get(params.id)

        if (params.id) {
            breadcrumbService.addBreadcrumb(controllerName, actionName, 'show', params.int('id'), userCode.identifier)
        }

        //if we must show a template
        if(params.template) {
            render template: 'userCodeShow', model: [ userCode : userCode, user: userCode.user ]

            //else show the user code list
        } else {

            def userCodes = UserCodeDTO.createCriteria().list(
                    max:    params.max
            ) { eq("user.id", userCode.user.id)
            }

            render view: 'userCodeListView', model: [userCodes: userCodes, selectedUserCode: userCode, user: userCode.user]
        }
    }

    /**
     * Deactivate the selected user code. Set the expiry date to today.
     */
    def userCodeDeactivate () {
        def userCode = params.id ? UserCodeDTO.get(params.int('id')) : null

        if (!userCode) {
            flash.error = 'userCode.not.found'
            flash.args = [ params.id  as String]

            redirect controller: 'user', action: 'userCodeList'
            return
        }

        UserCodeWS userCodeWs = UserBL.convertUserCodeToWS(userCode)
        userCodeWs.validTo = new Date()

        try {
            //if the user has access update the user code
                webServicesSession.updateUserCode(userCodeWs)
        } catch (SessionInternalError e) {
            //got an exception, show the edit page again
            viewUtils.resolveException(flash, session.locale, e);
        }

        redirect( action: 'userCodeList', id: params.userId )
    }

    def userCodeEdit () {
        def userCode = params.id ? UserCodeDTO.get(params.int('id')) : new UserCodeDTO()

        if (params.id && !userCode) {
            flash.error = 'userCode.not.found'
            flash.args = [ params.id  as String]

            redirect controller: 'user', action: 'userCodeList'
            return
        }

        //if id is not provided we must be adding
        if (!params.id && !params.boolean('add')) {
            flash.error = 'userCode.not.selected'
            flash.args = [ params.id  as String]

            redirect controller: 'user', action: 'userCodeList'
            return
        }

        //if we are adding we must know which user it belongs to
        if (params.boolean('add') && !params.userId) {
            flash.error = 'userCode.user.not.selected'
            flash.args = [ params.userId  as String]

            redirect controller: 'user', action: 'userCodeList'
            return
        }

        if (params.boolean('add')) {
            UserDTO user = UserDTO.get(params.userId)
            userCode.user= user
            userCode.validFrom = new Date()
        }

        EnumerationDTO typesEnum = EnumerationDTO.createCriteria().get() {
            eq('name', 'User Code Type')
            eq('entity.id', session['company_id'])
        }



        //if we are editing we can create a breadcrumb
        if (params.id) {
            breadcrumbService.addBreadcrumb(controllerName, actionName, 'update', params.int('id'), userCode.identifier)
        }

        boolean isEditable = true;
        if(userCode.id) {
            isEditable = new UserCodeLinkDAS().countLinkedObjects(userCode.id) == 0
        }
        [ userCode : userCode, types: typesEnum?.values?.collect {it.value} , user: userCode.user, isEditable: isEditable ]
    }

    def userCodeSave () {
        def userCode = new UserCodeWS()

        //bind the parameters to the user code
        bindData(userCode, params, [exclude: ['id', 'identifier']])

        userCode.id = !params.id?.equals('') ? params.int('id') : 0
        userCode.identifier = params.userName + params.identifier.trim()
        userCode.userId = params.int('userId')

        try {
            if (userCode.id) {
                //if the user has access update the user code
                    webServicesSession.updateUserCode(userCode)
            } else {
                //if the user has permission add the user code
                    webServicesSession.createUserCode(userCode)
            }
        } catch (SessionInternalError e) {
            //got an exception, show the edit page again
            viewUtils.resolveException(flash, session.locale, e);
            def dto = new UserBL().converUserCodeToDTO(userCode);
            dto.discard()
            dto.validTo = null

            EnumerationDTO typesEnum = EnumerationDTO.createCriteria().get() {
                eq('name', 'User Code Type')
                eq('entity.id', session['company_id'])
            }

            boolean isEditable = true;
            if(userCode.id) {
                isEditable = new UserCodeLinkDAS().countLinkedObjects(userCode.id) == 0
            }
            render view: 'userCodeEdit', model: [ userCode : dto, types: typesEnum?.values?.collect {it.value}, user: UserDTO.get(userCode.userId), isEditable: isEditable] + isLoggedInUserPartnerModel()
            return
        }

        redirect action: 'userCodeList', id: userCode.userId
    }

    def isLoggedInUserPartnerModel() {
        // #7043 - Agents && Commissions - If a Partner is logged in we have to change the layout to panels to avoid showing the configuration menus.
        UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)

        def model = [isPartner: loggedInUser.partner ? true : false]
        model
    }
	
	private List<CompanyDTO> getAccessibleCompanies(UserWS user) {
		List<CompanyDTO> accessibleCompanies= new ArrayList<CompanyDTO>();
		if( !ArrayUtils.isEmpty(user.accessibleEntityIds) ) {
			accessibleCompanies = CompanyDTO.createCriteria().list(){
				'in'('id', user.accessibleEntityIds)
			}
		}
		accessibleCompanies
	}

    def failToSwitch() {

        String msg = ''
        def exception = session[AbstractAuthenticationProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY]
        if (exception) {
            if (exception instanceof AccountExpiredException) {
                msg = g.message(code: "springSecurity.errors.login.expired")
            }
            else if (exception instanceof CredentialsExpiredException) {
                msg = g.message(code: "springSecurity.errors.login.passwordExpired")
            }
            else if (exception instanceof DisabledException) {
                msg = g.message(code: "springSecurity.errors.login.disabled")
            }
            else if (exception instanceof LockedException) {
                msg = g.message(code: "springSecurity.errors.login.locked")
            }
            else if (exception instanceof LicenseMissingException) {
                msg = 'auth.fail.license.missing.exception'
            }
            else if (exception instanceof LicenseInvalidException) {
                msg = 'auth.fail.license.invalid.exception'
            }
            else if (exception instanceof LicenseExpiredException) {
                msg = 'auth.fail.license.expired.exception'
            }
        }
        reloadURL(msg)
    }
}
