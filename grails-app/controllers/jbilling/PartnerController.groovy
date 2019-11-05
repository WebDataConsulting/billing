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

import com.sapienter.jbilling.client.util.ClientConstants
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.partner.validator.PartnerCommissionsValidator
import com.sapienter.jbilling.server.user.partner.db.CommissionDAS
import com.sapienter.jbilling.server.user.partner.db.CommissionDTO
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDTO
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDAS
import com.sapienter.jbilling.server.user.partner.db.InvoiceCommissionDAS
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import grails.converters.JSON
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions
import grails.plugin.springsecurity.annotation.Secured
import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.client.user.UserHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.partner.PartnerWS
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.db.UserStatusDTO
import com.sapienter.jbilling.server.util.ServerConstants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import grails.plugin.springsecurity.SpringSecurityUtils
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.EntityType


@Secured(["isAuthenticated()"])
class PartnerController {
	static scope = "prototype"
    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]

    static final viewColumnsToFields =
            ['userid': 'id',
             'username': 'baseUser.userName',
             'company': 'company.description',
             'status': 'baseUser.userStatus.id']

    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils

    def filterService
    def recentItemService
    def breadcrumbService
    def springSecurityService

    def index () {
        list()
    }

    def getList(filters, params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        // #7043 - Agents && Commissions - A logged in Partner should see only his children and himself.
        UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
        def partnerIds = []
        if (loggedInUser.getPartner() != null) {
            partnerIds << loggedInUser.getPartner().getUser().getId()
            if (loggedInUser.getPartner().getChildren()) {
                partnerIds += loggedInUser.getPartner().getChildren().user.id
            }
        }

        def statuses = UserStatusDTO.findAll()

        return PartnerDTO.createCriteria().list(
            max:    params.max,
            offset: params.offset
        ) {
            and {
                createAlias('baseUser', 'baseUser')
                createAlias("baseUser.contact", "contact")
                createAlias("baseUser.company","company")

                filters.each { filter ->
                    if (filter.value) {
                        if (filter.constraintType == FilterConstraint.STATUS) {
                            eq("baseUser.userStatus", statuses.find { it.id == filter.integerValue })
                        }else if(filter.field == 'userName'){
                            eq("baseUser.userName", filter.value)
                        }else if(filter.field == 'deleted') {
                            eq('baseUser.deleted', filter.value)
                        }else {
                            addToCriteria(filter.getRestrictions());
                        }
                    }
                }
                eq('baseUser.company', retrieveCompany())
				
                if(partnerIds) {
                    'in'('baseUser.id', partnerIds)
                }
                if(params.userid) {
                    eq('id', params.int('userid'))
                }
                if(params.company) {
                    addToCriteria(Restrictions.ilike("company.description",  params.company, MatchMode.ANYWHERE) );
                }
                if(params.username) {
                    or{
                        eq("baseUser.userName", params.username)
                        addToCriteria(Restrictions.ilike("contact.firstName", params.username, MatchMode.ANYWHERE))
                        addToCriteria(Restrictions.ilike("contact.lastName", params.username, MatchMode.ANYWHERE))
                    }
                }
            }

            // apply sorting
            SortableCriteria.buildSortNoAlias(params, delegate)
        }
    }

    def list () {
        def filters = filterService.getFilters(FilterType.PARTNER, params)

        UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
        def selected = params.id ? PartnerDTO.get(params.int("id")) : null
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, null)

        // if id is present and object not found, give an error message to the user along with the list
        if (params.id?.isInteger() && selected == null) {
            flash.error = 'partner.not.found'
            flash.args = [params.id]
        }

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ClientConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'partnersTemplate', model: [loggedInUser: loggedInUser, filters:filters]
            }else {
                render view: 'list', model: [loggedInUser: loggedInUser, filters:filters]
            }
            return
        }

        def partners = getList(filters, params)
        def contact = selected ? ContactDTO.findByUserId(selected?.baseUser?.id) : null

        if (selected?.baseUser?.id) {
            selected.baseUser.accountLocked = new UserBL(selected.baseUser.id).isAccountLocked()
        }

        if (params.applyFilter || params.partial) {
            render template: 'partnersTemplate', model: [ partners: partners, selected: selected, contact: contact, filters:filters ]
            return 
        } 
        render view: 'list', model: [ partners: partners, selected: selected, contact: contact, filters:filters, loggedInUser: loggedInUser]
    }

    def getListWithSelected(selected) {
        def idFilter = new Filter(type: FilterType.ALL, constraintType: FilterConstraint.EQ,
                field: 'id', template: 'id', visible: true, integerValue: selected.id)
        getList([idFilter], params)
    }

    def findAgents (){
        //This will enable the filters to work properly
        if (params.username){
            params.firstName = params.username
            params.lastName = params.username
        }

        def filters = filterService.getFilters(FilterType.PARTNER, params)

        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0

        def selected = params.id ? PartnerDTO.get(params.int("id")) : null
        def partners = selected ? getListWithSelected(selected) : getList(filters, params)

        try {
            render getAgentsJsonData(partners, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    private def Object getAgentsJsonData(partners, GrailsParameterMap params) {
        def jsonCells = partners
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def numberOfPages = Math.ceil(partners.totalCount / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: partners.totalCount, total: numberOfPages]

        jsonData
    }

    /**
     * Applies the set filters to the order list, and exports it as a CSV for download.
     */
    def csv (){
        def filters = filterService.getFilters(FilterType.PARTNER, params)

        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order = params.sord
        params.max = CsvExporter.MAX_RESULTS

        def selected = params.id ? PartnerDTO.get(params.int("id")) : null
        def partners = selected ? getListWithSelected(selected) : getList(filters, params)

        renderCsvForPartners(partners)
    }

    def renderCsvForPartners(partners) {
        if (partners.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "partners.csv")
            Exporter<PartnerDTO> exporter = CsvExporter.createExporter(PartnerDTO.class);
            render text: exporter.export(partners), contentType: "text/csv"
        }
    }

    def show () {
        def partner = PartnerDTO.get(params.int('id'))
        def contact = partner ? ContactDTO.findByUserId(partner?.baseUser.id) : null

        //Check if account is locked so that it can be shown on UI appropriately
        def user = partner?.baseUser.id ? webServicesSession.getUserWS(partner?.baseUser.id) : new UserWS()
        if(null != user?.id){
            partner.baseUser.accountLocked = user.isAccountLocked
        }

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, partner.id, UserHelper.getDisplayName(partner.baseUser, contact))

        render template: 'show', model: [ selected: partner, contact: contact ]
    }

    /**
     * Shows the commissions runs.
     */
    def showCommissionRuns (){
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        def commissionRuns = getCommissionRuns(PartnerDTO.get(params.int('id')))

        [ commissionRuns: commissionRuns ]
    }

    /**
     * Returns a list of runCommissions that can be paginated
     */
    private def getCommissionRuns (PartnerDTO partner) {
        params.max = (params?.max?.toInteger()) ?: pagination.max
        params.offset = (params?.offset?.toInteger()) ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        return CommissionProcessRunDTO.createCriteria()
                .list(max:params.max, offset:params.offset) {
            eq("entity", this.retrieveCompany())
            if(partner) {
                createAlias('commissions', 'commissions')
                and{
                    eq('commissions.partner', partner)
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    /**
     * Shows the commissions for a single commission run.
     */
    def showCommissions (){
        Integer processRunId = params.int('id')

        def commissionRun = new CommissionProcessRunDAS().find(processRunId)

        def commissions = getCommissions(commissionRun)

        [ commissionRun : commissionRun, commissions: commissions]
    }

    /**
    * Returns a list of commissions that can be paginated
    * @param commissionProcessRun
    */
    private def getCommissions(CommissionProcessRunDTO commissionProcessRun) {
        params.max = (params?.max?.toInteger()) ?: pagination.max
        params.offset = (params?.offset?.toInteger()) ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        Integer agentId = params?.int('agentId')
        return CommissionDTO.createCriteria().list(
            max:    params.max,
            offset: params.offset
            ) {
            createAlias("commissionProcessRun", "_commissionProcessRun")
            createAlias("_commissionProcessRun.entity", "_entity")
                and{
                    eq('commissionProcessRun', commissionProcessRun)
                    eq('_entity.id', session['company_id'])
                    if (agentId) {
                        createAlias("partner", "_partner")
                        and {
                            eq('_partner.id', agentId)
                        }
                    }
                }
                // apply sorting
                SortableCriteria.sort(params, delegate)
            }
    }

    /**
     * Shows the detailed commissions for a partner.
     */
    def showCommissionDetail (){
        Integer commissionId = params.int('commissionId')

        def commission = new CommissionDAS().find(commissionId)

        def invoiceCommissions = new InvoiceCommissionDAS().findByCommission(commission)

        [ commission : commission, invoiceCommissions: invoiceCommissions]
    }

    /**
     * Returns the list of commissions as a csv file
     */
    def commissionCsv (){
        Integer processRunId = params.int('id')

        def commissionRun = new CommissionProcessRunDAS().find(processRunId)

        def commissions = new CommissionDAS().findAllByProcessRun(commissionRun, session['company_id'])

        if (commissions.size() > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'showCommissions'
        } else {
            DownloadHelper.setResponseHeader(response, "commissions.csv")
            Exporter<CommissionDTO> exporter = CsvExporter.createExporter(CommissionDTO.class);
            render text: exporter.export(commissions), contentType: "text/csv"
        }
    }

    def edit () {

        def user
        def partner
        def contacts
        def parentId

        try {
            partner= params.id ? webServicesSession.getPartner(params.int('id')) : new PartnerWS()
            user= (params.id &&  partner) ? webServicesSession.getUserWS(partner?.userId) : new UserWS()
            contacts = params.id ? webServicesSession.getUserContactsWS(user.userId) : null
            
            breadcrumbService.addBreadcrumb(controllerName, 'edit', null, partner.id, 
            	UserHelper.getDisplayName(user, contacts && contacts.length > 0 ? contacts[0] : null))

            // If a parentId comes in the params is because the Add Sub-Agent button was clicked.
            parentId = params.parentId ?: null
        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)
            redirect action: 'list', params:params
            return
        }

        [
                partner: partner,
                user: user,
                contacts: contacts?.flatten(),
                company: retrieveCompany(),
                currencies: retrieveCurrencies(),
                availableFields: retrieveAvailableMetaFields(),
                parentId: parentId,
                loggedInUser: UserDTO.get(springSecurityService.principal.id)
        ]
    }

    /**
     * Validate and Save the Partner User
     */
    def save () {
        def partner = new PartnerWS()
        def user = new UserWS()

        // Bind partner's data
        bindData(partner, params)

        // Retrieve useful information
        UserHelper.bindUser(user, params)

        def availableMetaFields = retrieveAvailableMetaFields()
        UserHelper.bindMetaFields(user, availableMetaFields, params)

        log.debug("bound fields: ${user.getMetaFields()}")

        def contacts = []
        UserHelper.bindContacts(user, contacts, retrieveCompany(), params)

        def oldUser = (user.userId && user.userId != 0) ? webServicesSession.getUserWS(user.userId) : null
        
        UserHelper.bindPassword(user, oldUser, params, flash)

        //Bind commission exceptions.
        UserHelper.bindPartnerCommissionExceptions(partner, params, g.message(code: 'datepicker.format').toString())
        UserHelper.bindPartnerReferralCommissions(partner, params, g.message(code: 'datepicker.format').toString())

        // Validate the Partner Commission data.
        PartnerCommissionsValidator commissionsValidator = new PartnerCommissionsValidator()
        String validationResult = commissionsValidator.validate(partner)

        if (validationResult) {
            flash.error = validationResult
        }

        if( partner.getId()!=null && (partner.getId()==partner.getParentId()) ) {
            flash.error = g.message(code: 'partner.error.parentOfItsOwn')
        }
        else {
            try {
                PartnerWS parent = (params.parentId && params.parentId.isNumber()) ? webServicesSession.getPartner(params.int("parentId")) : null
                if (parent && parent.parentId) {
                    if (partner.getId()==parent.getId()) {
                        flash.error = g.message(code: 'partner.error.parentOfItsOwn')
                    }
                    else {
                        flash.error = g.message(code: 'partner.error.parentIsChild')
                    }
                }
            } catch(Exception e) {
                flash.error = g.message(code: 'partner.error.parentDoesNotExist')
            }
        }

        if (flash.error) {
            render view: 'edit', model: [
                    partner: partner,
                    user: user,
                    contacts: contacts,
                    company: retrieveCompany(),
                    currencies: retrieveCurrencies(),
                    availableFields: availableMetaFields
            ]
            return
        }

        try {
            // save or update
            if (!oldUser) {
                    log.debug("creating partner ${user}")

                    partner.id = webServicesSession.createPartner(user, partner)

                    flash.message = 'partner.created'
                    flash.args = [partner.id]

            } else {
                    log.debug("saving changes to partner ${user.userId} & ${user.customerId}")

                    partner.setUserId(user.getUserId())
                    webServicesSession.updatePartner(user, partner)

                    flash.message = 'partner.updated'
                    flash.args = [partner.id]

            }

            // save secondary contacts
            if (user.userId) {
                contacts.each {
                    webServicesSession.updateUserContact(user.userId, it);
                }
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render view: 'edit', model: [ partner: partner, user: user, contacts: contacts, company: retrieveCompany(), currencies: retrieveCurrencies(), availableFields: availableMetaFields ]
            return
        }

        chain action: 'list', params: [ id: partner.id ]
    }

    def delete () {

        try {
            if (params.id) {
                webServicesSession.deletePartner(params.int('id'))
                log.debug("Deleted partner ${params.id}.")
            }
            flash.message = 'partner.deleted'
            flash.args = [params.id]
        } catch (SessionInternalError e) {
            log.error("Could not delete agent", e)
            viewUtils.resolveException(flash, session.locale, e)
        }
        // render the partial user list
        params.applyFilter = true
        params.id = null
        params.partial = true
        redirect action: 'list'
    }

    def userCodeList () {
        render(view: 'userCodeListView', model: modelAndView.model)
    }

    def retrieveCurrencies() {
        def currencies = webServicesSession.getCurrencies()
        return currencies.findAll { it.inUse }
    }
    
    def retrieveCompany() {
        CompanyDTO.get(session['company_id'])
    }

    def retrieveAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.AGENT);
    }
}
