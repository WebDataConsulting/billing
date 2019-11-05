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

import java.util.List;

import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.util.ServerConstants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.db.LanguageDTO
import com.sapienter.jbilling.server.util.db.InternationalDescription
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.order.OrderPeriodWS
import com.sapienter.jbilling.server.payment.tasks.PaymentSageTask.Params;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO

import grails.converters.JSON

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import grails.plugin.springsecurity.annotation.Secured

/**
 * OrderPeriodController 
 *
 * @author Vikas Bodani
 * @since 09-Mar-2011
 */


@Secured(["isAuthenticated()"])
class OrderPeriodController {

	static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
	static scope = "prototype"
	
    static final viewColumnsToFields =
            ['periodId': 'id',
             'value': 'value']

	def breadcrumbService
	IWebServicesSessionBean webServicesSession
	def viewUtils
	
    def index () {
        redirect action: 'list', params: params
    }

    def list () {

		params.max = params?.max?.toInteger() ?: pagination.max
		params.offset = params?.offset?.toInteger() ?: pagination.offset
		params.sort = params?.sort ?: pagination.sort
		params.order = params?.order ?: pagination.order
		
        def period = OrderPeriodDTO.get(params.int('id'))

        if (params.id?.isInteger() && !period) {
            flash.error = 'orderPeriod.not.found'
            flash.args = [ params.id as String ]
        }
		
		breadcrumbService.addBreadcrumb(controllerName, actionName, period?.getDescription(session['language_id'] as Integer), period?.id)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ServerConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'periodsTemplate', model: [selected: period]
            } else {
                if(chainModel){
                    render view: 'list', model:[selected: period]+chainModel
                } else {
                    render view: 'list', model: [selected: period]
                }
            }
            return
        }

        def periods= getList(params)
        if (params.applyFilter || params.partial) {
            render template: 'periodsTemplate', model: [ periods: periods, selected: period ]
        } else {
            if(chainModel){
                render view: 'list', model:[selected: period, periods: periods]+chainModel
            } else {
                render view: 'list', model: [periods: periods, selected: period]
            }
        }
	}
	
	def getList(params) {
		
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def languageId = session['language_id']

        return OrderPeriodDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            eq('company', new CompanyDTO(session['company_id']))
            if (params.periodId){
                def searchParam = params.periodId
                if (searchParam.isInteger()){
                    eq('id', Integer.valueOf(searchParam));
                } else {
                    searchParam = searchParam.toLowerCase()
                    sqlRestriction(
                            """ exists (
                                            select a.foreign_id
                                            from international_description a
                                            where a.foreign_id = {alias}.id
                                            and a.table_id =
                                             (select b.id from jbilling_table b where b.name = ? )
                                            and a.language_id = ?
                                            and a.psudo_column = 'description'
                                            and lower(a.content) like ?
                                        )
                                    """, [ServerConstants.TABLE_ORDER_PERIOD, languageId, "%" + searchParam + "%"]
                    )
                }
            }
            SortableCriteria.sort(params, delegate)
        }
	}


    def findPeriods () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def periods = getList(params)

        try {
            def jsonData = getAsJsonData(periods, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts * to JSon
     */
    private def Object getAsJsonData(elements, GrailsParameterMap params) {
        def jsonCells = elements
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }


    def show () {
        def period = OrderPeriodDTO.get(params.int('id'))

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, period.id, period.getDescription(session['language_id']))

        render template: 'show', model: [ selected: period ]
    }

    def edit () {

        def period = params.id ? OrderPeriodDTO.get(params.int('id')) : null

        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? period?.getDescription(session['language_id']) : null
        
        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', crumbName, params.int('id'), crumbDescription)

        def periodUnits = PeriodUnitDTO.list()
        
        render template: 'edit', model: [ period: period, periodUnits: periodUnits ]
    }
    
    def listEdit () {
        
        def period = params.id ? OrderPeriodDTO.get(params.int('id')) : null
        
        if (params.id?.isInteger() && !period) {
			redirect action: 'list', params: params
			return
        }
        
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
		def periods = getList(params)
		
        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? period?.getDescription(session['language_id']) : null
        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', crumbName, params.int('id'), crumbDescription)
        
        def periodUnits = PeriodUnitDTO.list()
        
        render view: 'listEdit', model: [periods: periods, period: period, periodUnits: periodUnits]
    }
    
	def save () {
        
        OrderPeriodWS ws= new OrderPeriodWS()
        bindData(ws, params)
		log.debug ws
		if(params.description){
			InternationalDescriptionWS descr=
				new InternationalDescriptionWS(session['language_id'] as Integer, params.description)
	        log.debug descr
			ws.descriptions.add descr
		}
		ws.setEntityId(session['company_id'].toInteger())
        if (validOrderPeriod(ws)) {
            log.debug ws

            try {
                boolean retVal= webServicesSession.updateOrCreateOrderPeriod(ws);
                if (params.isNew=="true")
                {
                    flash.message= 'config.periods.created'
                } else{
                    flash.message= 'config.periods.updated'
                }
            } catch (SessionInternalError e){
                viewUtils.resolveException(flash, session.locale, e);
                chain action: 'list', model:[period:ws, periodUnits: PeriodUnitDTO.list()]
                return
            } catch (Exception e) {
                log.error e.getMessage()
                flash.error = 'config.periods.saving.error'
            }
        }
		redirect (action: 'list')
	}

    private boolean validOrderPeriod(OrderPeriodWS orderPeriod) {
        def periods = webServicesSession.getOrderPeriods()
        for (OrderPeriodWS period: periods) {
            if (period.getEntityId().equals(session['company_id']) && period.getId()!= orderPeriod?.id &&
                    period.getDescription(session['language_id']).content.equals(orderPeriod.getDescription(session['language_id'])?.content)) {
                flash.error ='bean.OrderPeriodWS.validate.duplicate'
                flash.args = [ orderPeriod.getDescription(session['language_id']).content ]
                return false
            }
        }
        return true
    }

	def delete () {
		log.debug "delete called on ${params.id}"
        if (params.id) {
            def period= OrderPeriodDTO.get(params.int('id'))
            if (period) {
                try {
                    boolean retVal= webServicesSession.deleteOrderPeriod(params.id?.toInteger());
                    if (retVal) { 
                        flash.message= 'config.periods.delete.success'
                        flash.args = [ params.id ]
                    } else {
                        flash.info = 'config.periods.delete.failure'
                    }
                } catch (SessionInternalError e){
                    viewUtils.resolveException(flash, session.locale, e);
                } catch (Exception e) {
                    log.error e.getMessage()
                    flash.error = 'config.periods.delete.error'
                }
            }
        }

        // render the period list
        params.applyFilter = true
        params.id = null
        redirect (action: 'list')
	}

}
