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
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.FlowHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.CustomerNoteWS
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CustomerDTO
import com.sapienter.jbilling.server.user.db.CustomerNoteDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.db.UserStatusDAS
import com.sapienter.jbilling.server.user.db.UserStatusDTO
import com.sapienter.jbilling.server.payment.PaymentInformationWS
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO
import com.sapienter.jbilling.server.process.ConfigurationBL
import com.sapienter.jbilling.server.user.db.*
import com.sapienter.jbilling.server.util.ServerConstants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import grails.converters.JSON
import grails.orm.PagedResultList
import grails.plugin.springsecurity.annotation.Secured
import grails.plugin.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.Criteria
import org.hibernate.FetchMode
import org.hibernate.Hibernate
import org.hibernate.criterion.*

import com.sapienter.jbilling.server.process.ConfigurationBL
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.NumberUtils

import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import org.hibernate.criterion.LogicalExpression

import com.sapienter.jbilling.server.user.CustomerNoteWS;
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.ContactWS;

import java.util.Calendar;
import java.util.GregorianCalendar;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;

@Secured(["isAuthenticated()"])
class CustomerController {
	static scope = "prototype"
    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    static versions = [ max: 25 ]

    static final viewColumnsToFields =
            ['userId': 'id',
            'userName': 'userName',
            'company': 'company.description',
            'status': 'userStatus.id']

    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils

    def filterService
    def recentItemService
    def breadcrumbService
    def springSecurityService
    def subAccountService

    def index () {
        list()
    }

    def getList(filters, statuses, params) {

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        List<String> contactFilters = ['contact.firstName','contact.lastName','contact.email','contact.phoneNumber',
                                       'contact.postalCode','contact.organizationName']
		def user_id = session['user_id']
        return UserDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            createAlias("customer", "customer")
			createAlias("company", "company")
			
            and {
                if(!Collections.disjoint(contactFilters,filters.findAll{it.value}*.field)) {
                    createAlias("customer.accountType", "accountType")
                    setFetchMode("accountType", FetchMode.JOIN)
                    createAlias("customer.customerAccountInfoTypeMetaFields", "aitMetaFields")
                    createAlias("aitMetaFields.accountInfoType", "accountInfoType")
                    createAlias("aitMetaFields.metaFieldValue", "mfv")
                    setFetchMode("mfv", FetchMode.JOIN)
                    eqProperty("accountType.preferredNotificationAitId", "accountInfoType.id")
                    createAlias("mfv.field", "metaField")
                    or{
                        filters.each { filter ->
                            if (filter.value && contactFilters.contains(filter.field)) {
                                DetachedCriteria metaFieldTypeSubCrit = DetachedCriteria.forClass(MetaField.class,"metaFieldType")
                                        .setProjection(Projections.property('id'))
                                        .add(Restrictions.sqlRestriction("exists (select * from metafield_type_map where field_usage= ? and metafield_id = {alias}.id)",
                                        filterService.getMetaFieldTypeForFilter(filter.field)?.toString(),
                                        Hibernate.STRING))
                                Criterion metaFieldTypeCrit =Property.forName("metaField.id").in(metaFieldTypeSubCrit)
                                def subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringMFValue")
                                        .setProjection(Projections.property('id'))
                                if(filter.field == 'contact.postalCode'){
                                    subCriteria.add(Restrictions.eq('stringMFValue.value', filter.stringValue))
                                }else {
                                    subCriteria.add(Restrictions.ilike('stringMFValue.value', filter.stringValue, MatchMode.ANYWHERE))
                                }
                                Criterion aitMfv = Property.forName("mfv.id").in(subCriteria)
                                LogicalExpression aitMfvAndType = Restrictions.and(aitMfv,metaFieldTypeCrit)
                                addToCriteria(aitMfvAndType)
                            }
                        }
                    }
                }

                filters.each { filter ->
                    if (filter.value) {
						if(filter.value != null && filter.field == 'as.identifier'){
							createAlias("orders", "or")
							createAlias("or.lines", "ol")
							createAlias("ol.assets", "as")
							addToCriteria( Restrictions.ilike("as.identifier",  filter.stringValue, MatchMode.ANYWHERE) )
						}
                        // handle user status separately from the other constraints
                        // we need to find the UserStatusDTO to compare to
                        if (filter.constraintType == FilterConstraint.STATUS) {
                            eq("userStatus", statuses.find{ it.id == filter.integerValue })

                        } else if (filter.field == 'contact.fields') {
                            String typeId = params['contact.fields.fieldKeyData']?params['contact.fields.fieldKeyData']:filter.fieldKeyData
                            String ccfValue= filter.stringValue
                            log.debug "Contact Field Type ID: ${typeId}, CCF Value: ${ccfValue}"
                            if (typeId && ccfValue) {
                                MetaField type = findMetaFieldType(typeId.toInteger());
                                if (type != null) {
                                    // Using different alias names because "type" and "fieldValue" were used below
                                    createAlias("customer.metaFields", "customerTypeMFValue")
                                    createAlias("customerTypeMFValue.field", "customerTypeMF")
                                    setFetchMode("customerTypeMF", FetchMode.JOIN)
                                    eq("customerTypeMF.id", typeId.toInteger())

                                    switch (type.getDataType()) {
                                        case DataType.STRING:
                                        	def subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue")
                                        					.setProjection(Projections.property('id'))
										    				.add(Restrictions.like('stringValue.value', ccfValue + '%').ignoreCase())

                                        	addToCriteria(Property.forName("customerTypeMFValue.id").in(subCriteria))
                                            break;
                                        case DataType.ENUMERATION:
                                        case DataType.JSON_OBJECT:
                                            addToCriteria(Restrictions.ilike("customerTypeMFValue.value", ccfValue, MatchMode.ANYWHERE))
                                            break;
                                        default:
                                        // todo: now searching as string only, search for other types is impossible
//                                            def fieldValue = type.createValue();
//                                            bindData(fieldValue, ['value': ccfValue])
//                                            addToCriteria(Restrictions.eq("fieldValue.value", fieldValue.getValue()))

                                            addToCriteria(Restrictions.eq("customerTypeMFValue.value", ccfValue))
                                            break;
                                    }

                                }
                            }
                        } else if (filter.field == 'accountTypeFields') {
                            String typeId = params['accountTypeFields.fieldKeyData']?params['accountTypeFields.fieldKeyData']:filter.fieldKeyData
                            String ccfValue = filter.stringValue
                            log.debug "Account Field Type ID: ${typeId}, CCF Value: ${ccfValue}"
                            if (typeId && ccfValue) {
                                MetaField type = findATMetaFieldType(typeId.toInteger());
                                if (type != null) {
                                    createAlias("customer.customerAccountInfoTypeMetaFields", "fieldValue")
                                    createAlias("fieldValue.metaFieldValue", "type")
                                    createAlias("type.field", "field")
                                    setFetchMode("type", FetchMode.JOIN)
                                    eq("field.id", typeId.toInteger())

                                    switch (type.getDataType()) {
                                        case DataType.STRING:
                                            def subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue")
                                                    .setProjection(Projections.property('id'))
                                                    .add(Restrictions.like('stringValue.value', ccfValue + '%').ignoreCase())

                                            addToCriteria(Property.forName("type.id").in(subCriteria))
                                            break;
                                        case DataType.ENUMERATION:
                                        case DataType.JSON_OBJECT:
                                            addToCriteria(Restrictions.ilike("fieldValue.value", ccfValue, MatchMode.ANYWHERE))
                                            break;
                                        default:
                                            addToCriteria(Restrictions.eq("fieldValue.value", ccfValue))
                                            break;
                                    }
                                }
                            }
                        } else if(filter.field == 'u.company.description') {
							addToCriteria( Restrictions.ilike("company.description",  filter.stringValue, MatchMode.ANYWHERE) );
                        } else if(filter.field == 'userCodes.userCode.identifier') {
                            createAlias("customer.userCodeLinks", "userCodes")
                            createAlias("userCodes.userCode", "userCode")
                            addToCriteria( Restrictions.eq("userCode.identifier",  filter.stringValue) )
                        } else if(filter.field == 'invoices'){
							createAlias("invoices", "inv")
							eq("inv.deleted", 0)
						}else if (!contactFilters.contains(filter.field)){
                            addToCriteria(filter.getRestrictions());
                        }
                    } else if (filter.value != null && filter.field == 'deleted') {
						addToCriteria(filter.getRestrictions());
					}
                }

                if(params.userId) {
                    eq('id', params.int('userId'))
                }

                if(params.company) {
                    addToCriteria( Restrictions.ilike("company.description",  params.company, MatchMode.ANYWHERE) );
                }
                if(params.userName) {
                    Criterion USER_NAME = Restrictions.ilike("userName",  params.userName, MatchMode.ANYWHERE);
                    addToCriteria(USER_NAME)
                }
                if (params.status) {
                    eq("userStatus", statuses.find{ it.name == params.status })

                }

                //check that the user is a customer
                isNotNull('customer')
				'in'('company', retrieveCompanies())

                if (SpringSecurityUtils.ifNotGranted("CUSTOMER_17")) {
                    UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)

                    if (loggedInUser.getPartner() != null) {
                        // #7043 - Agents && Commissions - A logged in Partner should only see its customers and the ones of his children.
                        // A child Partner should only see its customers.
                        def partnerIds = []
                        if (loggedInUser.getPartner() != null) {
                            partnerIds << loggedInUser.getPartner().getId()
                            if (loggedInUser.getPartner().getChildren()) {
                                partnerIds += loggedInUser.getPartner().getChildren().id
                            }
                        }
                        createAlias("customer.partner", "partner")
                        'in'('partner.id', partnerIds)
                    } else if (SpringSecurityUtils.ifAnyGranted("CUSTOMER_18")) {
                        // restrict query to sub-account user-ids
                        'in'('id', subAccountService.getSubAccountUserIds())
                    }
                        
                    //not granted to see all customer, restrict by user role
                    UserDTO callerUser= UserDTO.get(user_id as int)
            		if ( callerUser.getRoles().find { it.roleTypeId == ServerConstants.TYPE_PARTNER } ) {
            			eq('customer.partner.id', callerUser.partnersForUserId.id)
            		} else if ( callerUser.getRoles().find { it.roleTypeId == ServerConstants.TYPE_CUSTOMER } ) {
						// limit list to only this customer
						eq('id', user_id)
            		}
                }
            }
            resultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
            // apply sorting
            SortableCriteria.sort(params, delegate)
        }
    }

    def findCustomers (){

        def filters = filterService.getFilters(FilterType.CUSTOMER, params)
        def statuses = new UserStatusDAS().findByEntityId(session['company_id'])
        def users = []

        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        users = getList(filters, statuses, params)

        // if the id exists and is valid and there is no record persisted for that id, write an error message
        if(params.id?.isInteger() && selected == null){
            flash.info = message(code: 'flash.customer.not.found')
        }

            users = getList(filters, statuses, params)
        try {
            def jsonData = getCustomersJsonData(users, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    private def getChildren (GrailsParameterMap params, boolean withPagination)
    {
        def paginationValues = [max: params.max]
        if (withPagination){
            paginationValues.offset = params.offset
        }

        def children = UserDTO.createCriteria().list(paginationValues) {
            and {
                createAlias("company", "company")
                customer {
                    parent {
                        eq('baseUser.id', params.int('id'))
                        order("id", "desc")
                    }
                    if(params.userId) {
                        eq('id', params.int('userId'))
                    }
                    if(params.company) {
                        addToCriteria(Restrictions.ilike("company.description",  params.company, MatchMode.ANYWHERE) );
                    }
                    if(params.userName) {
                        addToCriteria(Restrictions.ilike("userName",  params.userName, MatchMode.ANYWHERE))
                    }
                }
                eq('deleted', 0)
            }
            SortableCriteria.sort(params, delegate)
        }
        children
    }
    /**
     * Get a list of users and render the list page. If the "applyFilters" parameter is given, the
     * partial "_users.gsp" template will be rendered instead of the complete user list.
     */
    def list (){
		
        def filters = filterService.getFilters(FilterType.CUSTOMER, params)
        def statuses = new UserStatusDAS().findByEntityId(session['company_id'])
        def users = []
        UserDTO selected = params.id ? UserDTO.get(params.int("id")) : null
        def userData = [:]

        //validate if this user belongs to one of the given companies then show its details
        selected = retrieveCompaniesIds().contains(selected?.company?.id) ? selected : null
        if (selected) {
            //collect user related data, e.g. contact, latestPayment, revenue etc..
			userData = retrieveUserData(selected)
            selected.accountLocked = new UserBL(selected.id).isAccountLocked()
        }

        def crumbDescription = selected ? UserHelper.getDisplayName(selected, userData.contact) : null
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id, crumbDescription)
        
        def contactFieldTypes = params['contactFieldTypes']

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ServerConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'customersTemplate', model: [statuses: statuses, filters: filters] + userData
            }else {
                render view: 'list', model: [statuses: statuses, filters: filters, customerNotes: retrieveCustomerNotes()] + userData
            }
            return
        }
        // if logged in as a customer, you can only view yourself
            users = getList(filters, statuses, params)

        // Show error message if no customer found.
        if (params.id && params.int("id") && !selected) {
            flash.error = "flash.customer.not.found"
        }

        if (params.applyFilter || params.partial) {
            render template: 'customersTemplate', model: [
                    selected: selected,
                    users: users,
                    statuses: statuses,
                    filters: filters,
                    contactFieldTypes: contactFieldTypes ] + userData
        } else {
            render view: 'list', model: [selected: selected, users: users, statuses: statuses, filters: filters,customerNotes: retrieveCustomerNotes()] + userData

        }
    }
	
    def getListWithSelected(statuses, selected) {
        def idFilter = new Filter(type: FilterType.ALL, constraintType: FilterConstraint.EQ,
                field: 'id', template: 'id', visible: true, integerValue: selected.id)
        getList([idFilter], statuses, params)
    }

	/**
     * Applies the set filters to the user list, and exports it as a CSV for download.
     */
    def csv () {
        def filters = filterService.getFilters(FilterType.CUSTOMER, params)
        def statuses = new UserStatusDAS().findByEntityId(session['company_id'])

        // For when the csv is exported on JQGrid
        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order  = (params.sord != null ? params.sord : params.order)
        params.max = CsvExporter.MAX_RESULTS

        def users = getList(filters, statuses, params)
        renderCsvFor(users)
    }

    /**
     * Applies the set filters to the user list, and exports it as a CSV for download.
     */
    def subaccountsCsv () {
        // For when the csv is exported on JQGrid
        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order  = (params.sord != null ? params.sord : params.order)
        params.max = CsvExporter.MAX_RESULTS

        def users = getChildren(params, false)

        renderCsvFor(users)
    }

    def renderCsvFor(users) {
        if (users.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list'
        } else {
            DownloadHelper.setResponseHeader(response, "users.csv")
            Exporter<UserDTO> exporter = CsvExporter.createExporter(UserDTO.class);
            render text: exporter.export(users), contentType: "text/csv"
        }
    }
/**
     * Show details of the selected user. By default, this action renders the "_show.gsp" template.
     * When rendering for an AJAX request the template defined by the "template" parameter will be rendered.
     */
    def show () {
        def user = UserDTO.get(params.int('id'))
        if (!user) {
            log.debug "redirecting to list"
            redirect(action: 'list')
            return
        }
		//collect user related data, e.g. contact, latestPayment, revenue etc..
		def userData = retrieveUserData(user)
        user.accountLocked = new UserBL(user.id).isAccountLocked()

        recentItemService.addRecentItem(user.userId, RecentItemType.CUSTOMER)
        breadcrumbService.addBreadcrumb(controllerName, 'list', params.template ?: null, user.userId, UserHelper.getDisplayName(user, userData.contact))

        FlowHelper.display(this, true, [template: params.template ?: 'show',
                                        model   : [selected: user, customerNotes: retrieveCustomerNotes()] + userData])
    }

    /**
     * Fetches a list of sub-accounts for the given user id and renders the user list "_table.gsp" template.
     */
    def subaccounts () {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def parent = UserDTO.get(params.int('id'))

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ServerConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            render template: 'customersTemplate', model:[parent: parent]
            return
        }

        def children = getChildren(params, true)

        render template: 'customersTemplate', model: [ users: children, parent: parent ]
    }

    /**
     * JQGrid will call this method to get the list as JSon data
     */
    def findSubaccounts () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def children = getChildren(params, true)

        try {
            def jsonData = getCustomersJsonData(children, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts Users to JSon
     */
    private def Object getCustomersJsonData(users, GrailsParameterMap params) {
        def jsonCells = users
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def numberOfPages = Math.ceil(users.totalCount / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: users.totalCount, total: numberOfPages]

        jsonData
    }

    /**
     * Shows all customers of the given partner id
     */
    def partner () {
        def filter = new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.EQ, field: 'customer.partner.id', template: 'id', visible: true, integerValue: params.id)
        filterService.setFilter(FilterType.CUSTOMER, filter)

        redirect action: 'list'
    }

    /**
     *  Add new note from customer screen
     */
     def saveCustomerNotes () {
        try {
            CustomerNoteWS customerNoteWS=new CustomerNoteWS();
            bindData(customerNoteWS,params.notes);
            webServicesSession.createCustomerNote(customerNoteWS);
            render "success"
        } catch (SessionInternalError e) {
            log.error("Could not save the customer's notes", e)
            viewUtils.resolveException(flash, session.locale, e)
        }
    }


    /**
     * Delete the given user id.
     */
    def delete () {
        if (params.id) {
			
			try {
				webServicesSession.deleteUser(params.int('id'))
				flash.message = 'customer.deleted'
				flash.args = [ params.id ]
				log.debug("Deleted user ${params.id}.")
			} catch (SessionInternalError e) {
				log.error("Could not delete user", e)
				viewUtils.resolveException(flash, session.locale, e)
			}

            // remove the id from the list in session.
            subAccountService.removeSubAccountUserId(params.int('id'))
        }

        // render the partial user list
        params.partial = true
        redirect action: 'list'
    }

    /**
     * Get the user to be edited and show the "edit.gsp" view. If no ID is given this view
     * will allow creation of a new user.
     */
    def edit () {
        def accountTypes
        def accountTypeId
		def companyId

		log.debug("params.accountTypeId:########## ${session['company_id']}")
        accountTypeId = params.accountTypeId && params.accountTypeId?.isInteger() ?
            params.int('accountTypeId') : null
		
		companyId = params.int('user.entityId') == null ||  
					params.int('user.entityId') == 'null' ? 
						session['company_id'] : params.int('user.entityId')
		
        if(!accountTypeId && !params.id){
            accountTypes = AccountTypeDTO.createCriteria().list() {
                eq('company.id', companyId)
                order('id', 'asc')
            };

            //if this is request for new customer creation then check if there
            //are available account types. If not abort customer creation
            if (!params.id && (!accountTypes || accountTypes.size == 0)) {
                flash.error = message(code: 'customer.account.types.not.available')
                redirect controller: 'customer', action: 'list'
                return
            }
        }

        def user
        def parent

        try {
            user = params.id ? webServicesSession.getUserWS(params.int('id')) : new UserWS()
            if (params.id?.isInteger() && user?.deleted==1) {
				log.error("Customer not found or deleted, redirect to list.")
				customerNotFoundErrorRedirect(params.id)
            	return
            }
            
            parent = params.parentId ? webServicesSession.getUserWS(params.int('parentId')) : null

        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)
			customerNotFoundErrorRedirect(params.id)
            return
        }

        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? UserHelper.getDisplayName(user, user.contact) : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription)

        if(user.userId || accountTypeId) {

            //if existing user then set the account type of that user
            accountTypeId = user.userId ? user.accountTypeId : accountTypeId
			companyId = user.userId ? (user.entityId ? user.entityId : UserDTO.get(user.userId)?.company?.id) : companyId
            def accountType = accountTypeId ? AccountTypeDTO.get(accountTypeId) :
                                accountTypes?.size() > 0 ? accountTypes.get(0) : null

            def periodUnits = PeriodUnitDTO.list()
            def orderPeriods = OrderPeriodDTO.createCriteria().list() { eq('company', CompanyDTO.get(companyId)) }
			def templateName = ServerConstants.TEMPLATE_MONTHLY

            if(!user.userId || 0 == user.userId){
                initUserDefaultData(user, accountType)
            }

            def infoTypes = accountType?.informationTypes?.sort { it.displayOrder }
			
			// set dates map, effective dates map
			Map<Integer, ArrayList<Date>> dates;
			Map<Integer, Date> effectiveDatesMap;
			Map<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> accountInfoTypeFieldsMap = new HashMap<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>>();
			
			if(user.userId) {
				dates = user.timelineDatesMap;
				effectiveDatesMap = user.effectiveDateMap;
				accountInfoTypeFieldsMap = user.accountInfoTypeFieldsMap;
				
				// if a new info type was added after creating a user it should be
				// added to maps 
				for(def accountInfoType : infoTypes) {
					if(!dates.containsKey(accountInfoType.id)) {
						ArrayList<Date> date = new ArrayList<Date>()
						date.add(CommonConstants.EPOCH_DATE)
						
						dates.put(accountInfoType.id, date)
						effectiveDatesMap.put(accountInfoType.id, CommonConstants.EPOCH_DATE)
					}
				}
			} else {
				dates = new HashMap<Integer, ArrayList<Date>>()
				effectiveDatesMap = new HashMap<Integer, Date>()
				for(def accountInfoType : infoTypes) {
					ArrayList<Date> date = new ArrayList<Date>()
					date.add(CommonConstants.EPOCH_DATE)
					dates.put(accountInfoType.id, date)
					
					effectiveDatesMap.put(accountInfoType.id, CommonConstants.EPOCH_DATE)
				}
			}

            // #7043 - Agents && Commissions - If Partner or Sub-Partner creates a customer it has to be automatically linked to the
            // Partner or Parent Partner.
            UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
            Integer partnerId
            def partnerIds = []
            if (loggedInUser.getPartner() != null) {
                partnerId = loggedInUser.partner.id
                partnerIds << loggedInUser.partner.user.id
                if (loggedInUser.partner.children) {
                    partnerIds += loggedInUser.partner.children.user.id
                }
            }
            
            def userCompany = CompanyDTO.get(companyId)
            			
            // show only recurring payment methods
            def List<PaymentMethodTypeDTO> paymentMethods = getRecurringPaymentMethods(accountType?.paymentMethodTypes)

            //get all the logged in user's user codes. If the user is a Partner then we have to show the User Codes for the Partner and Sub-Partners if it corresponds.
            def userCodes = []
            if (partnerIds) {
                userCodes = new UserCodeDAS().findActiveForPartner(partnerIds).collect { it.identifier }
            } else {
                userCodes = new UserCodeDAS().findActiveForUser(session['user_id'] as int).collect { it.identifier }
            }
			
			/*
			 * This code use to render the period value template(i.e monthly) as per user main sunscription period unit in case of edit.
			 */
			if (null != user) {
				OrderPeriodWS orderPeriodWs = webServicesSession.getOrderPeriodWS(user.mainSubscription.periodId);
				if (orderPeriodWs.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_WEEK) == 0) {
					templateName = ServerConstants.TEMPLATE_WEEKLY
					user.mainSubscription.weekDaysMap = MainSubscriptionWS.weekDaysMap;
				} else if (orderPeriodWs.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_DAY) == 0) {
					templateName = ServerConstants.TEMPLATE_DAILY
				} else if (orderPeriodWs.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_YEAR) == 0) {
					templateName = ServerConstants.TEMPLATE_YEARLY
					user.mainSubscription.yearMonthsMap= MainSubscriptionWS.yearMonthsMap;
					user.mainSubscription.yearMonthDays= MainSubscriptionWS.yearMonthDays;
					GregorianCalendar calendarInstance = new GregorianCalendar();
					calendarInstance.set(Calendar.DAY_OF_YEAR, user.mainSubscription.nextInvoiceDayOfPeriod);
					calendarInstance.getTime();
					user.mainSubscription.nextInvoiceDayOfPeriod = (calendarInstance.get(Calendar.MONTH) + 1)
					user.mainSubscription.nextInvoiceDayOfPeriodOfYear = (calendarInstance.get(Calendar.DAY_OF_MONTH))
				} else if (orderPeriodWs.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
					templateName = ServerConstants.TEMPLATE_SEMI_MONTHLY
					user.mainSubscription.semiMonthlyDaysMap= MainSubscriptionWS.semiMonthlyDaysMap;
				} else {
					user.mainSubscription.monthDays = MainSubscriptionWS.monthDays;
				}
			}
			
			/*
			 * When user want to use billing cycle period that time populate on hidden feild at gsp level
			 * set Main Subscription period as per Billing Configuration Next Run date and billing period
			 * in orderPeriodSubscriptionUnit hidden field.
			 */
			BillingProcessConfigurationWS billingProcessConfiguration = webServicesSession.getBillingProcessConfiguration();
			GregorianCalendar cal = new GregorianCalendar();
			def orderPeriodSubscriptionUnit = null;
			cal.setTime(billingProcessConfiguration.nextRunDate);
			Integer orderPeriodId = null;
			for (OrderPeriodDTO orderPeriod : orderPeriods) {
				
				if (orderPeriod.periodUnit.id.compareTo(billingProcessConfiguration.periodUnitId) == 0) {
					orderPeriodSubscriptionUnit = orderPeriod.id
					break;
				}
			 }
			
			MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
			if (null != parent && parent.isParent) {
				mainSubscription = parent.getMainSubscription();
				OrderPeriodWS orderPeriodWs = webServicesSession.getOrderPeriodWS(parent.mainSubscription.periodId);
				if (orderPeriodWs.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_WEEK) == 0) {
					templateName = ServerConstants.TEMPLATE_WEEKLY
				} else if (orderPeriodWs.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_DAY) == 0) {
					templateName = ServerConstants.TEMPLATE_DAILY
				} else if (orderPeriodWs.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_YEAR) == 0) {
					templateName = ServerConstants.TEMPLATE_YEARLY
					GregorianCalendar calendarInstance = new GregorianCalendar();
				} else if (orderPeriodWs.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
					templateName = ServerConstants.TEMPLATE_SEMI_MONTHLY
				}
			} else {
				mainSubscription.periodId = ServerConstants.PERIOD_UNIT_MONTH
				mainSubscription.nextInvoiceDayOfPeriod = 1;
				mainSubscription.monthDays = MainSubscriptionWS.monthDays;
			}

            render view: "edit", model: [
                    user: user,
                    parent: parent,
                    company: userCompany,
                    currencies: retrieveCurrenciesByCompanyId(companyId),
                    periodUnits: periodUnits,
                    orderPeriods: orderPeriods,
                    availableFields: retrieveAvailableMetaFieldsByCompanyId(companyId),
					mainSubscription: mainSubscription, 
					orderPeriodSubscriptionUnit: orderPeriodSubscriptionUnit, 
					templateName: templateName,
                    accountType: accountType,
                    accountInformationTypes: infoTypes,
                    pricingDates : dates,
                    effectiveDates : effectiveDatesMap,
                    datesXml : map2xml(dates),
                    effectiveDatesXml : map2xml(effectiveDatesMap),
                    infoFieldsMapXml : map2xml(accountInfoTypeFieldsMap),
                    removedDatesXml : map2xml(new HashMap<Integer, List<Date>>()),
					customerNotes:retrieveCustomerNotes(),                    
					userCodes: userCodes,
                    paymentMethods : paymentMethods,
                    partnerId: partnerId,
                    loggedInUser: loggedInUser,
					customerNotes:retrieveCustomerNotes()
            ]
        } else {
            render view: 'list', model: [
                    accountTypes: accountTypes,
                    parentId: parent?.id,
					companies: retrieveCompanies()
            ]
        }
    }
	
	/**
	 * gets account types for the given company
	 */
	def getAccountTypes (){
		def user = new UserWS()
		UserHelper.bindUser(user, params)
		def accountTypes = AccountTypeDTO.createCriteria().list() {
			eq('company.id', user.entityId)
			order('id', 'asc')
		};

		render template : 'accountTypeDropDown',
			   model : [accountTypes : accountTypes]
	}

	private void customerNotFoundErrorRedirect(customerId) {
		flash.error = 'customer.not.found'
		flash.args = [ customerId as String ]
		redirect controller: 'customer', action: 'list'
	}

    /**
     * Validate and save a user.
     */
    def save () {
        UserWS user = new UserWS()

		def templateName = ServerConstants.TEMPLATE_MONTHLY
		def orderPeriods = OrderPeriodDTO.createCriteria().list() { eq('company', retrieveCompany()) }
		/*
		 * Calculate nextInvoiceDayOfPeriod in case of yearly period unit.
		 */

		OrderPeriodWS orderPeriodWs = webServicesSession.getOrderPeriodWS( params['mainSubscription.periodId']?.toInteger() );
		
		if (orderPeriodWs.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_YEAR) == 0) {
			Integer nextInvoiceDayOfPeriod = 1;
			GregorianCalendar calendar = new GregorianCalendar();
			calendar.set(Calendar.MONTH, params.mainSubscription.nextInvoiceDayOfPeriod.toInteger()-1);
			calendar.set(Calendar.DAY_OF_MONTH, params.mainSubscription.nextInvoiceDayOfPeriodOfYear.toInteger());
			calendar.getTime();
			nextInvoiceDayOfPeriod = calendar.get(Calendar.DAY_OF_YEAR);
			params.mainSubscription.nextInvoiceDayOfPeriod = nextInvoiceDayOfPeriod;
		}
		
		OrderPeriodWS orderPeriod = webServicesSession.getOrderPeriodWS(params.mainSubscription.periodId.toInteger());
		 if (orderPeriod.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_MONTH) == 0) {
			templateName = ServerConstants.TEMPLATE_MONTHLY
		  	params.mainSubscription.monthDays= MainSubscriptionWS.monthDays;
		 } else if (orderPeriod.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_WEEK) == 0) {
			templateName = ServerConstants.TEMPLATE_WEEKLY
			params.mainSubscription.weekDaysMap= MainSubscriptionWS.weekDaysMap;
		 } else if (orderPeriod.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_DAY) == 0) {
			templateName = ServerConstants.TEMPLATE_DAILY
		  	params.mainSubscription.nextInvoiceDayOfPeriod = new Integer(1);
		 } else if (orderPeriod.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_YEAR) == 0) {
			templateName = ServerConstants.TEMPLATE_YEARLY
			params.mainSubscription.yearMonthsMap= MainSubscriptionWS.yearMonthsMap;
			params.mainSubscription.yearMonthDays= MainSubscriptionWS.yearMonthDays;
		 } else if (orderPeriod.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
			templateName = ServerConstants.TEMPLATE_SEMI_MONTHLY
		  	params.mainSubscription.semiMonthlyDaysMap= MainSubscriptionWS.semiMonthlyDaysMap;
		 }
		 
		 /*
		  * when user 'use company billing cycle' if any validation error occur set 
		  * Main Subscription period as per Billing Configuration 
		  * Next Run date and billing period in orderPeriodSubscriptionUnit hidden field.
		  */
		BillingProcessConfigurationWS billingProcessConfiguration = webServicesSession.getBillingProcessConfiguration();
		GregorianCalendar cal = new GregorianCalendar();
		def orderPeriodSubscriptionUnit = null;
		cal.setTime(billingProcessConfiguration.nextRunDate);
		for (OrderPeriodDTO orderPeriodDto : orderPeriods) {
			
			if (orderPeriodDto.periodUnit.id == billingProcessConfiguration.periodUnitId) {
				orderPeriodSubscriptionUnit = orderPeriodDto.id
				break;
			}
		 }
		
        UserHelper.bindUser(user, params)
        UserHelper.bindMetaFields(user, retrieveAvailableMetaFieldsByCompanyId(user.entityId), params)
        UserHelper.bindMetaFields(user, retrieveAvailableAitMetaFields(user.accountTypeId), params)
        try {
            UserHelper.bindPaymentInformations(user ,params.int("modelIndex"), params)
        } catch (SessionInternalError sie) {
            viewUtils.resolveException(flash, session.locale, sie)
            flash.error = flash.errorMessages[0]
            flash.errorMessages = null
        }

        List<CustomerNoteWS> customerNotesWS=[]
        if (params.newNotesTotal){
            for(int i=0;i<params.newNotesTotal.toInteger();i++)
            {    customerNotesWS.add(bindData(new CustomerNoteWS(),params.notes."${i}") as CustomerNoteWS)
            }
        }
        user.setCustomerNotes(customerNotesWS.toArray(new CustomerNoteWS[customerNotesWS.size()]))

        // convert xml to maps
        def timelineDates = params.datesXml
        def effectiveDates = params.effectiveDatesXml
        def removedDates = params.removedDatesXml
        def infoFieldsXml = params.infoFieldsMapXml

        user.timelineDatesMap = xml2map(timelineDates)
        user.effectiveDateMap = xml2map(effectiveDates)
        user.removedDatesMap = xml2map(removedDates)
       
        def oldUser = (user.userId && user.userId != 0) ? webServicesSession.getUserWS(user.userId) : null
       
        UserHelper.bindPassword(user, oldUser, params, flash)
		
		def periodUnits = PeriodUnitDTO.list()

        def accountTypeId = user?.accountTypeId
        def accountType = accountTypeId ? AccountTypeDTO.get(accountTypeId) : null;

        def infoTypes = accountType?.informationTypes?.sort { it.displayOrder }
        if (params.'user.partnerId' && !params.'user.partnerId'.equals("null")) {
            try {
                Integer newPartnerId = Integer.valueOf(params.'user.partnerId')
            } catch (NumberFormatException e) {
                flash.error = 'validation.error.invalid.agentid'
            }
        }

        if (flash.error) {

            render view: 'edit', model: [
                    user: user, company: retrieveCompany(), 
                    availableFields: retrieveAvailableMetaFields(), 
					periodUnits: periodUnits, orderPeriods: orderPeriods, 
					currencies: retrieveCurrencies(), templateName: templateName, 
					orderPeriodSubscriptionUnit: orderPeriodSubscriptionUnit,
					pricingDates : xml2map(timelineDates),
					effectiveDates : xml2map(effectiveDates),
					datesXml : timelineDates,
					effectiveDatesXml : effectiveDates,
					infoFieldsMapXml : infoFieldsXml,
					removedDatesXml : removedDates,
					paymentMethods : getRecurringPaymentMethods(accountType?.paymentMethodTypes)
            ] + loadModelForSaveError(user, user.entityId)
            return
        }
		
		// if child company was selected assign that company else assing parent company
		def companyId = user.entityId

        try {
        
            // save or update
            if (!oldUser) {
                    if (user?.userName.trim()) {
	
                        user.userId = webServicesSession.createUserWithCompanyId(user,companyId)
                        /*if(accountType?.paymentMethodTypes?.size() == 0) {
                            flash.info= "customer.accountType.paymentMethod.associated.info"
                        }*/
                        flash.message = 'customer.created'
                        flash.args = [user.userId as String]
                                                
                        // add the id to the list in session.
                        subAccountService.addSubAccountUserId(user)
                        
                    } else {
                        user.userName = ''
                        flash.error = message(code: 'customer.error.name.blank')

                        render view: "edit", model: [
                                user: user, 
								company: retrieveCompany(), currencies: retrieveCurrencies(), 
								periodUnits: periodUnits, orderPeriods: 
								orderPeriods, availableFields: retrieveAvailableMetaFields(), 
								templateName: templateName, 
								orderPeriodSubscriptionUnit: orderPeriodSubscriptionUnit,
                                parent: null,
                                pricingDates : xml2map(timelineDates),
								effectiveDates : xml2map(effectiveDates),
								datesXml : timelineDates,
								effectiveDatesXml : effectiveDates,
								infoFieldsMapXml : infoFieldsXml,
								removedDatesXml : removedDates,
								paymentMethods : getRecurringPaymentMethods(accountType?.paymentMethodTypes)
                        ] + loadModelForSaveError(user, companyId)
                        return
                    }

            } else {

					 webServicesSession.updateUserWithCompanyId(user, companyId)
                    flash.message = 'customer.updated'
                    flash.args = [user.userId as String]

            }

        } catch (SessionInternalError e) {
            flash.clear()
            viewUtils.resolveException(flash, session.locale, e)
			accountType = AccountTypeDTO.get(accountTypeId)
            render view: 'edit', model: [
                    user: user, 
					company: retrieveCompany(), currencies: retrieveCurrencies(), 
					availableFields: retrieveAvailableMetaFields(), 
					periodUnits: periodUnits, orderPeriods: orderPeriods, 
					templateName: templateName, 
					orderPeriodSubscriptionUnit: orderPeriodSubscriptionUnit,
					pricingDates : xml2map(timelineDates),
					effectiveDates : xml2map(effectiveDates),
					datesXml : timelineDates,
					effectiveDatesXml : effectiveDates,
					infoFieldsMapXml : infoFieldsXml,
					removedDatesXml : removedDates,
					paymentMethods : getRecurringPaymentMethods(accountType?.paymentMethodTypes)
            ] + loadModelForSaveError(user, companyId)
            return
        }

        chain action: 'list', params: [id: user.userId,customerNotes:  retrieveCustomerNotes()]
    }

    private Map loadModelForSaveError(user, companyId) {
        def periodUnits = PeriodUnitDTO.list()
        def orderPeriods = OrderPeriodDTO.createCriteria().list(){eq('company', CompanyDTO.get(user.entityId))}

        UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)

        def accountTypeId = user?.accountTypeId
        def accountType = accountTypeId ? AccountTypeDTO.get(accountTypeId) : null;

        def infoTypes = accountType?.informationTypes?.sort { it.displayOrder }

        //get all the logged in user's user codes.
        def userCodes = new UserCodeDAS().findActiveForUser(session['user_id'] as int).collect { it.identifier }

        [
                periodUnits: periodUnits,
                orderPeriods: orderPeriods,
                accountType: accountType,
                accountInformationTypes: infoTypes,
                company: CompanyDTO.get(companyId),
                currencies: retrieveCurrenciesByCompanyId(companyId),
                availableFields: retrieveAvailableMetaFieldsByCompanyId(companyId),
                loggedInUser: loggedInUser,
                userCodes : userCodes
        ]
    }

	def addDate (){
		Integer aitId = params.aitId as Integer
		
		Map<Integer, ArrayList<Date>> dates = xml2map(params.dates)

		def startDate = new Date().parse(message(code: 'date.format'), params.date)
		ArrayList<Date> datesForGivenAit = dates.get(aitId)
		if(!datesForGivenAit.contains(startDate)) {
			datesForGivenAit.add(startDate)
		}
		datesForGivenAit.sort()
		dates.put(aitId, datesForGivenAit)
		
		render template: '/customer/timeline', model: [startDate : startDate , pricingDates : dates, aitVal : aitId, isNew : params.isNew]
	}
	
	def editDate (){
		Date startDate
		try {
			startDate = new Date().parse(message(code: 'date.format'), params.startDate)
		} catch(Exception e) {
			Map<Integer, ArrayList<Date>> datesMap = xml2map(params.dates)
			ArrayList<Date> aitDates = datesMap.get(params.aitId as Integer)
			
			startDate = findEffectiveDate(aitDates)
		}
		
		def accountInfoType = AccountInformationTypeDTO.get(params.aitId)
		Map<Integer, Map<Date, ArrayList<MetaFieldValueWS>>> accountInfoTypeFieldsMap = xml2map(params.values)
		Map<Date, ArrayList<MetaFieldValueWS>> valuesByDate = accountInfoTypeFieldsMap.get(accountInfoType.id)
		ArrayList<MetaFieldValueWS> values
		if(valuesByDate != null) {
			if(valuesByDate.containsKey(startDate)) {
				// if valeus are present for the date then paint those values
				values = valuesByDate.get(startDate)
			} else {
				// if values are not present then paint the latest ones
				for(Map.Entry<Date, ArrayList<MetaFieldValueWS>> entry : valuesByDate.entrySet()) {
					values = entry.getValue()	
				}
			}
			render template: '/customer/aITMetaFields', model: [ait : accountInfoType, values : values, aitVal : accountInfoType.id]
		}
	}

	def updateDatesXml (){
		Integer aitId = params.aitId as Integer 
		def startDate = new Date().parse(message(code: 'date.format'), params.date)
		Map<Integer, ArrayList<Date>> datesMap = xml2map(params.dates)
		
		ArrayList<Date> dates = datesMap.get(aitId)
		String converted
		if(!dates.contains(startDate)) {
			dates.add(startDate)
			dates.sort()
			datesMap.put(aitId, dates)
			converted = map2xml(datesMap)
		} else {
			converted = params.dates
		}
		render (text: converted, contentType: "text/xml", encoding: "UTF-8")
	}
	
	def refreshTimeLine (){
		Integer aitId = params.aitId as Integer
		Map<Integer, ArrayList<Date>> datesMap = xml2map(params.values)
		ArrayList<Date> aitDates = datesMap.get(aitId)
		
		def startDate
		try {
			startDate = new Date().parse(message(code: 'date.format'), params.startDate)
		} catch(Exception e) {
			startDate = findEffectiveDate(aitDates)
		}
		
		render template: '/customer/timeline', model: [startDate : startDate , pricingDates : datesMap, aitVal : aitId, isNew : params.isNew]

	}
	
	def updateEffectiveDateXml (){
		Map<Integer, Date> dates = xml2map(params.values)
		
		try {
			dates.put(params.aitId as Integer, new Date().parse(message(code: 'date.format'), params.startDate))
		} catch(Exception e) {
			Map<Integer, ArrayList<Date>> timelineDates = xml2map(params.dates)
			ArrayList<Date> aitDates = timelineDates.get(params.aitId as Integer)
			
			dates.put(params.aitId as Integer, findEffectiveDate(aitDates))
		}
		
		String converted = map2xml(dates)
		render (text: converted, contentType: "text/xml", encoding: "UTF-8")
	}
	
	def updateTimeLineDatesXml (){
		Map<Integer, ArrayList<Date>> dates = xml2map(params.dates)
		dates.get(params.aitId as Integer).remove(new Date().parse(message(code: 'date.format'), params.startDate))
		String converted = map2xml(dates)
		render (text: converted, contentType: "text/xml", encoding: "UTF-8")
	}
	
	def updateRemovedDatesXml (){
		Integer aitId = params.aitId as Integer
		def startDate = new Date().parse(message(code: 'date.format'), params.startDate)
		Map<Integer, ArrayList<Date>> dates = xml2map(params.removedDates)
		if(dates.containsKey(aitId)) {
			dates.get(aitId).add(startDate)
		} else {
			ArrayList<Date> date = new ArrayList<Date>()
			date.add(startDate)
			
			dates.put(aitId, date)
		}
		String converted = map2xml(dates)
		render (text: converted, contentType: "text/xml", encoding: "UTF-8")
	}
	
	def addPaymentInstrument (){
		def user = new UserWS()
		UserHelper.bindPaymentInformations(user ,params.int("modelIndex"), params)
		
		def accountType = AccountTypeDTO.get(params.int("accountTypeId"))
		// show only recurring payment methods
		def List<PaymentMethodTypeDTO> paymentMethods = getRecurringPaymentMethods(accountType?.paymentMethodTypes)
		// add a new payment instrument
		PaymentInformationWS paymentInstrument = new PaymentInformationWS()
		paymentInstrument.setPaymentMethodTypeId(paymentMethods?.iterator().next().id)
		
		user.paymentInstruments.add(paymentInstrument)
		
		render template: '/customer/paymentMethods', model: [paymentMethods : paymentMethods , paymentInstruments : user.paymentInstruments , accountTypeId : accountType?.id]
	}

    def refreshPaymentInstrument() {
        int currentIndex = params.int("currentIndex")

        def user = params.int("id") && params.int("id")>0 ? webServicesSession.getUserWS(params.int('id')) : new UserWS()
        bindData(params, user.paymentInstruments)
        List<PaymentInformationWS> paymentInstruments = user.paymentInstruments
        UserHelper.bindPaymentInformations(user, params.int("modelIndex"), params)

        def accountType = AccountTypeDTO.get(params.int("accountTypeId"))
        def paymentMethods = getRecurringPaymentMethods(accountType?.paymentMethodTypes)

        paymentInstruments.eachWithIndex { PaymentInformationWS paymentInformationWS, int index ->
            if (paymentInformationWS.paymentMethodTypeId.equals(user.paymentInstruments.get(index).paymentMethodTypeId)) {
                user.paymentInstruments.set(index, paymentInformationWS)
            }
        }
        render template: '/customer/paymentMethods', model: [paymentMethods: paymentMethods, paymentInstruments: user.paymentInstruments, accountTypeId: accountType?.id, user: user]
    }

	def removePaymentInstrument (){
		def currentIndex = params.int("currentIndex")
		
		def user = new UserWS()
		UserHelper.bindPaymentInformations(user ,params.int("modelIndex"), params)
		
		def accountType = AccountTypeDTO.get(params.int("accountTypeId"))
		def paymentMethods = getRecurringPaymentMethods(accountType?.paymentMethodTypes)
		
		PaymentInformationWS removed = user.paymentInstruments.remove(currentIndex)
		log.debug("user instrument is: ${user.paymentInstruments}")
		// if this was saved in database then we need to remove it from database as well
		if(removed.id != null && removed.id != 0) {
			boolean isRemoved = webServicesSession.removePaymentInstrument(removed.id)
		}
		
		render template: '/customer/paymentMethods', model: [paymentMethods : paymentMethods , paymentInstruments : user.paymentInstruments , accountTypeId : accountType?.id]
	}
	
    def retrieveCurrencies() {
        def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), session['company_id'].toInteger())
        return currencies.findAll { it.inUse }
    }
	
	def retrieveCompany() {
		CompanyDTO.get(session['company_id'])
	}
	
	def retrieveCurrenciesByCompanyId(Integer entityId) {
		def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), entityId)
		return currencies.findAll { it.inUse }
	}
	
	def retrieveCompanies(){
		def parentCompany = CompanyDTO.get(session['company_id'])
		def childs = CompanyDTO.findAllByParent(parentCompany)
		childs.add(parentCompany)
		return childs;
	}
	
	def retrieveCompaniesIds() {
		def ids = new ArrayList<Integer>(0);
		
		for(def child : retrieveCompanies()) {
			ids.add(child.id)
		}
		return ids
	}
	
    def retrieveAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.CUSTOMER);
    }
	
	def retrieveAvailableMetaFieldsByCompanyId(Integer entityId) {
		return MetaFieldBL.getAvailableFieldsList(entityId, EntityType.CUSTOMER);
	}
	
	def retrieveAvailableAccounTypeMetaFields() {
		return MetaFieldBL.getAllAvailableFieldsList(session["company_id"], EntityType.ACCOUNT_TYPE);
	}

    def retrieveAvailableAitMetaFields(Integer accountType){
        return MetaFieldBL.getAvailableAccountTypeFieldsMap(accountType)
    }

    def findMetaFieldType(Integer metaFieldId) {
        for (MetaField field : retrieveAvailableMetaFields()) {
            if (field.id == metaFieldId) {
                return field;
            }
        }
        return null;
    }
	
	def findATMetaFieldType(Integer metaFieldId) {
		for (MetaField field : retrieveAvailableAccounTypeMetaFields()) {
			if (field.id == metaFieldId) {
				return field;
			}
		}
		return null;
	}

	def retrieveUserData(def user) {
		
		if ( null == user ) return [:]
		
		def revenue = webServicesSession.getTotalRevenueByUser(user.userId)
		def latestOrder = webServicesSession.getLatestOrder(user.userId)
		def latestPayment = webServicesSession.getLatestPayment(user.userId)
		def latestInvoice = webServicesSession.getLatestInvoice(user.userId)
		def customerAssets = webServicesSession.getAssetsByUserId(user.userId)
		

		def enableTotalOwnedPayment= false
        def isCurrentCompanyOwning = user.company?.id?.equals(session['company_id']) ? true : false
		
		ConfigurationBL config = new ConfigurationBL(user.getCompany()?.getId());
		if (config?.getEntity()?.getAutoPaymentApplication() == 1 && UserBL.getBalance(user.userId) > 0) {
			enableTotalOwnedPayment=true
		}

		// get all meta fileds, standard + ait timeline
		List<MetaFieldValue> values =  new ArrayList<MetaFieldValue>()
		values.addAll(user?.customer?.metaFields)
		new UserBL().getCustomerEffectiveAitMetaFieldValues(values, user?.customer?.getAitTimelineMetaFieldsMap())

        //find all the user codes linked to this customer
        def userCodes = user?.customer? new UserCodeDAS().findLinkedIdentifiers(UserCodeObjectType.CUSTOMER, user.customer.id) : null

		return [ revenue:revenue,
                latestOrder:latestOrder,
                latestPayment:latestPayment,
                latestInvoice:latestInvoice,
				enableTotalOwnedPayment:enableTotalOwnedPayment,
                isCurrentCompanyOwning:isCurrentCompanyOwning,
				metaFields : values,
                userCodes : userCodes,
				customerAssets: customerAssets
        ]
    }

    def initUserDefaultData(def user, def accountType){

        //default data from account type
        user.mainSubscription = UserBL.convertMainSubscriptionToWS(accountType?.billingCycle)
        user.invoiceDesign = accountType?.invoiceDesign
        user.creditLimitAsDecimal = accountType?.creditLimit
        user.currencyId = accountType?.currencyId
        user.languageId = accountType?.languageId
        user.invoiceDeliveryMethodId = accountType?.invoiceDeliveryMethod?.id
    }
	
	def map2xml(map) {
		XStream converter = new XStream(new DomDriver())
		return converter.toXML(map)
	}
	
	/**
	 * Convert an xml to map
	 * 
	 * @param xmlValue	:	map in form of xml
	 * @return			: 	xml in form of map
	 */
	def xml2map(String xmlValue) {
		XStream converter = new XStream(new DomDriver())
		return converter.fromXML(xmlValue)
	}
	
	/**
	 * Gets a list of dates and return currently effective date
	 * 
	 * @param dates	:	list of dates
	 * @return		:	currently effective date
	 */
	def findEffectiveDate(dates) {
		Date date = new Date();
		Date forDate = null;
		for (Date start : dates) {
			if (start != null && start.after(date))
				break;

			forDate = start;
		}
		return forDate;
	}
	
	/**
	 * Return only those payment method types that are recurring.
	 * 
	 * @param paymentMethodTypes	list of payment method types
	 * @return	recurring payment methods
	 */
	def getRecurringPaymentMethods(paymentMethodTypes) {
		def List<PaymentMethodTypeDTO> paymentMethods = new ArrayList<PaymentMethodTypeDTO>();
		for(PaymentMethodTypeDTO dto : paymentMethodTypes) {
			if(dto.isRecurring) {
				paymentMethods.add(dto)
			}
		}
		return paymentMethods
	}

   
	/*
	 * Ajax function to render main subscription value template as per selected Main Subscription unit. 
	 */
	def updateSubscription () {
		OrderPeriodWS orderPeriod = webServicesSession.getOrderPeriodWS(params.mainSubscription.periodId.toInteger());
		params.mainSubscription.nextInvoiceDayOfPeriod = null;
		def templateName = ServerConstants.TEMPLATE_MONTHLY;
		if (orderPeriod.periodUnitId .compareTo(ServerConstants.PERIOD_UNIT_MONTH) == 0) {
			templateName = ServerConstants.TEMPLATE_MONTHLY
		  	params.mainSubscription.monthDays= MainSubscriptionWS.monthDays;
		} else if (orderPeriod.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_WEEK) == 0) {
			templateName = ServerConstants.TEMPLATE_WEEKLY
			params.mainSubscription.weekDaysMap= MainSubscriptionWS.weekDaysMap;
		} else if (orderPeriod.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_DAY) == 0) {
			templateName = ServerConstants.TEMPLATE_DAILY
		  	params.mainSubscription.nextInvoiceDayOfPeriod = new Integer(1);
		} else if (orderPeriod.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_YEAR) == 0) {
			templateName = ServerConstants.TEMPLATE_YEARLY
			params.mainSubscription.yearMonthsMap= MainSubscriptionWS.yearMonthsMap;
			params.mainSubscription.yearMonthDays= MainSubscriptionWS.yearMonthDays;
		} else if (orderPeriod.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
			templateName = ServerConstants.TEMPLATE_SEMI_MONTHLY
		  	params.mainSubscription.semiMonthlyDaysMap= MainSubscriptionWS.semiMonthlyDaysMap;
		}
		
			render template: '/customer/subscription/' + templateName, model: [ mainSubscription: params.mainSubscription]
	}
	
	/*
	 * When user click on 'Use Company Billin Cycle' Ajax request call this function fetch the billing configuration and 
	 * set the period unit and period value as per Billing Configuration Next run date and billing period and 
	 * render the template as per period unit.
	 */
	def updateSubscriptionOnBillingCycle () {
		
		BillingProcessConfigurationWS billingProcessConfiguration = webServicesSession.getBillingProcessConfiguration();
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(billingProcessConfiguration.nextRunDate)
		MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
		def orderPeriods = OrderPeriodDTO.createCriteria().list() { eq('company', retrieveCompany()) }
		OrderPeriodWS orderPeriod = webServicesSession.getOrderPeriodWS(params.mainSubscription.periodId.toInteger());
		def templateName = ServerConstants.TEMPLATE_MONTHLY
		if (billingProcessConfiguration.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_MONTH) == 0) {
			templateName = ServerConstants.TEMPLATE_MONTHLY
			params.mainSubscription.nextInvoiceDayOfPeriod = cal.get(Calendar.DAY_OF_MONTH);
		  	params.mainSubscription.monthDays= MainSubscriptionWS.monthDays;
		} else if (billingProcessConfiguration.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_WEEK) == 0) {
			templateName = ServerConstants.TEMPLATE_WEEKLY
			params.mainSubscription.nextInvoiceDayOfPeriod = cal.get(Calendar.DAY_OF_WEEK);
			params.mainSubscription.weekDaysMap= MainSubscriptionWS.weekDaysMap;
		} else if (billingProcessConfiguration.periodUnitId .compareTo(ServerConstants.PERIOD_UNIT_DAY) == 0) {
			templateName = ServerConstants.TEMPLATE_DAILY
		  	params.mainSubscription.nextInvoiceDayOfPeriod = new Integer(1);
		} else if (billingProcessConfiguration.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_YEAR) == 0) {
			templateName = ServerConstants.TEMPLATE_YEARLY
			params.mainSubscription.nextInvoiceDayOfPeriod = (cal.get(Calendar.MONTH) + 1)
			params.mainSubscription.nextInvoiceDayOfPeriodOfYear = (cal.get(Calendar.DAY_OF_MONTH))
			params.mainSubscription.yearMonthsMap= MainSubscriptionWS.yearMonthsMap;
			params.mainSubscription.yearMonthDays= MainSubscriptionWS.yearMonthDays;
		} else if (billingProcessConfiguration.periodUnitId.compareTo(ServerConstants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
			templateName = ServerConstants.TEMPLATE_SEMI_MONTHLY
			Integer dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
			if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) == dayOfMonth) {
				dayOfMonth = new Integer(15);
			} else if(cal.get(Calendar.DAY_OF_MONTH) > new Integer(15)) {
				dayOfMonth = (cal.get(Calendar.DAY_OF_MONTH) - new Integer(15))
			}
			params.mainSubscription.nextInvoiceDayOfPeriod = dayOfMonth;
		  	params.mainSubscription.semiMonthlyDaysMap= MainSubscriptionWS.semiMonthlyDaysMap;
		}
			render template: '/customer/subscription/' + templateName, model: [ mainSubscription: params.mainSubscription]
	}

    def retrieveCustomerNotes() {
        if (UserDTO.get(params.int('id'))) {
            def customerNotes = CustomerNoteDTO.createCriteria().list(max: 5, offset: 0) {
                and {
                    eq('customer.id', UserBL.getUserEntity(params.int('id'))?.getCustomer()?.getId())
                    order("creationTime", "desc")
                }
            }
        }
    }
}
