package jbilling

import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.EnumerationBL
import com.sapienter.jbilling.server.util.EnumerationValueWS
import com.sapienter.jbilling.server.util.EnumerationWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.db.EnumerationDAS
import com.sapienter.jbilling.server.util.db.EnumerationDTO
import com.sapienter.jbilling.server.util.db.EnumerationValueDTO

import grails.converters.JSON
import grails.gorm.PagedResultList
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions

import grails.plugin.springsecurity.annotation.Secured


@Secured(["isAuthenticated()"])
class EnumerationsController {

    static scope = "prototype"
    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    static final viewColumnsToFields = ['enumId': 'id']

    IWebServicesSessionBean webServicesSession
    def webServicesValidationAdvice
    def viewUtils
    def recentItemService
    def breadcrumbService

    def index () {
        list()
    }

    def list () {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        params.total = params?.total ?: webServicesSession.getAllEnumerationsCount()

		def company_id = session['company_id'] as Integer

        def selected = params.id ? webServicesSession.getEnumeration(params.int('id')) : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int("id"))

        // if id is present and object not found, give an error message to the user along with the list
        if (params.id?.isInteger() && selected == null) {
            flash.error = 'enumeration.not.found'
            flash.args = [params.id]
        }

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(company_id, Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'enumerationsTemplate', model: [selected: selected]
            }else {
                render view: 'list', model: [selected: selected]
            }
            return
        }

        def enums = getList(params)

        if (params.applyFilter || params.partial) {
            render template: 'enumerationsTemplate', model: [enumerations: enums, selected: selected]
        } else {
            render view: 'list', model: [enumerations: enums, selected: selected]
        }
    }

    def findEnums () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def enums = getList(params)

        try {
            def jsonData = getEnumerationsJsonData(enums, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts Enumerations to JSon
     */
    private def Object getEnumerationsJsonData(enums, GrailsParameterMap params) {
        def jsonCells = enums
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    private def getList(GrailsParameterMap params){
        def max = params.int('max')
        def offset = params.int('offset')
        return webServicesSession.getAllEnumerations(max, offset)
    }

    /**
     * Shows details of the selected Enumeration.
     */
    def show () {
        def enumerationId = params.int('id')
        def selected = enumerationId ? webServicesSession.getEnumeration(enumerationId): null
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, enumerationId)
        render template: 'show', model: [selected: selected]
    }

    def delete () {
        def enumerationId = params.int('id')
        if (enumerationId) {
            try {
                def enumer = EnumerationDTO.get(params.int('id'))
                log.debug "found enumeration ${enumer}"
                if (new MetaFieldDAS().getFieldCountByDataTypeAndName(DataType.ENUMERATION, enumer.getName(),session['company_id']) > 0
                        || new MetaFieldDAS().getFieldCountByDataTypeAndName(DataType.LIST, enumer.getName(),session['company_id']) > 0) {
                    log.debug('Can not delete enumeration ' + enumer.getName() + ', it is in use.')
                    flash.error = 'Can not delete enumeration ' + enumer.getName() + ', it is in use.'
                } else {
                    webServicesSession.deleteEnumeration(enumerationId)
                    flash.message = 'enumeration.deleted'
                    flash.args = enumerationId
                }
            } catch (SessionInternalError se){
                viewUtils.resolveException(flash, session.locale, se)
                chain action: 'list', params: [id: enumerationId]
                return
            }
        }
        // render the list
        chain action: 'list'
    }

    def edit () {
        def enumerationId = params.int('id')
        def enumeration = enumerationId ? webServicesSession.getEnumeration(enumerationId) : new EnumerationWS()
        def crumbName = enumerationId ? 'update' : 'create'
        def crumbDescription = enumerationId ? enumeration?.getName() : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, enumerationId, crumbDescription)

        render view: 'edit', model: [enumeration: enumeration]
    }

    def save () {
        def enumeration = new EnumerationWS();
        bindData(enumeration, params, [include: ['id', 'entityId', 'name']])
        // Workaround to get the enumeration values from a parameters and bind data to the EnumerationWS values list
        enumeration.values = params.findAll {it.value instanceof GrailsParameterMap}.sort{it.key}.collect {
            def value = new EnumerationValueWS()
            bindData(value, it.value)
        }

        // validate JSR-303 annotations
        try {
            webServicesValidationAdvice.validateObject(enumeration)
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render view: 'edit', model: [enumeration: enumeration]
            return
        }

        // save or update
        try{
            enumeration.id = webServicesSession.createUpdateEnumeration(enumeration)
            flash.message = 'enumeration.saved'
            flash.args = [enumeration.id]
        } catch (SessionInternalError sie){
            viewUtils.resolveException(flash, session.locale, sie)
            render view: 'edit', model: [enumeration: enumeration]
            return
        }
       chain action: 'list', params: [ id: enumeration.id ]
	}

}
