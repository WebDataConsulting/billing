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
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.ServerConstants
import grails.converters.JSON

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import java.util.List;

import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.order.db.OrderStatusDTO
import com.sapienter.jbilling.server.order.OrderStatusWS
import com.sapienter.jbilling.server.order.db.OrderStatusBL

/**
 * OrderStatusController 
 *
 * @author Maruthi
 * @since 17-Jul-2013
 */


@Secured(["isAuthenticated()", "MENU_100"])
class OrderStatusController {

    static pagination = [max: 10, offset: 0]

    static final viewColumnsToFields =
            ['orderStatusId': 'id',
             'description': 'description']

    def breadcrumbService
    IWebServicesSessionBean webServicesSession
    def orderStatusList
    ViewUtils viewUtils

    def index (){
        redirect action: 'list', params: params
    }

    def list (){
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ServerConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (!usingJQGrid){
            orderStatusList = OrderStatusDTO.list().findAll { it?.entity?.id == session['company_id'] }
			
			model: [orderStatusList: orderStatusList, orderStatusWS: params.showEdit ? new OrderStatusWS() : null]
        }
    }

    def findOrderStatuses (){
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        orderStatusList = OrderStatusDTO.list().findAll { it?.entity?.id == session['company_id'] }

        try {
            def jsonData = getAsJsonData(orderStatusList, params)

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
        def totalRecords =  jsonCells ? jsonCells.size() : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def show (){
        def orderStatus = webServicesSession.findOrderStatusById(params.int('id'))
        render template: 'show', model: [selected: orderStatus]
    }


    def edit (){
        def orderStatus = params.id ? OrderStatusDTO.get(params.int('id')) : null
		
	def crumbName = params.id ? 'update' : 'create'
	def crumbDescription = params.id ? orderStatus?.getDescription(session['language_id']) : null
	breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription)
		
        render template: 'edit', model: [orderStatusWS: orderStatus]
    }

    def listEdit (){

        def orderStatus = params.id ? OrderPeriodDTO.get(params.int('id')) : null
        if (params.id?.isInteger() && !orderStatus) {
            redirect action: 'list', params: params
        }
        render view: 'listEdit', model: [orderStatus: orderStatus]
    }

    def save (){

        OrderStatusWS orderStatusWS = new OrderStatusWS();
        bindData(orderStatusWS, params)
        orderStatusWS.setEntity(webServicesSession.getCompany());
        if (params.description) {
            InternationalDescriptionWS descr =
                    new InternationalDescriptionWS(session['language_id'] as Integer, params.description)
            orderStatusWS.descriptions.add descr
        }
        try {
            def id = webServicesSession.createUpdateOrderStatus(orderStatusWS);
            if (params?.isNew?.equals('true')) {
                flash.message = 'order.status.created'
                flash.args = [id]
            } else {
                flash.message = 'order.status.updated'
				flash.args = [id]
            }
            redirect action: 'list'
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            redirect action: 'list', params: [showEdit: true]
        }
    }

    def delete (){
        log.debug "delete order status called on ${params.id}"
        if (params.id) {
            OrderStatusDTO orderStatus = OrderStatusDTO.findByIdAndEntity(params.int('id'), CompanyDTO.get(session['company_id'] as Integer))

            try {
                def orderStatusWS = OrderStatusBL.getOrderStatusWS(orderStatus)
                webServicesSession.deleteOrderStatus(orderStatusWS);
            } catch (Exception e) {
                viewUtils.resolveException(flash, session.locale, e);
                redirect action: 'list'
                return
            }
            flash.message = 'order.status.deleted'

        }
        redirect action: 'list'
    }

}
