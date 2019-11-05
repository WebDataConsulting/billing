/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2014 Enterprise jBilling Software Ltd. and Emiliano Conde

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

import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.order.OrderChangeTypeWS
import com.sapienter.jbilling.server.order.db.OrderChangeTypeDTO
import grails.plugin.springsecurity.annotation.Secured

import java.util.regex.Pattern

/**
 * @author Alexander Aksenov
 * @since 22.02.14
 */
@Secured(['isAuthenticated()'])
class OrderChangeTypeController {

    def viewUtils
    def breadcrumbService
    def webServicesSession
	def productService
    def index (){
        redirect action: list, params: params
    }

    def list (){
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        getList(params)
    }

    def getList(params) {

        def orderChangeType = OrderChangeTypeDTO.get(params.int('id'))

        if (params.id?.isInteger() && !orderChangeType) {
            flash.error = 'orderChangeType.not.found'
            flash.args = [params.id as String]
        }

        def orderChangeTypes = webServicesSession.getOrderChangeTypesForCompany() as List

        if (params.applyFilter) {
            render template: 'orderChangeTypes', model: [orderChangeTypes: orderChangeTypes, selected: orderChangeType]
        } else {
            if (chainModel) {
                def cp = chainModel
                render view: 'listOrderChangeTypes', model: [selected: orderChangeType, orderChangeTypes: orderChangeTypes] + chainModel
            } else {
                render view: 'listOrderChangeTypes', model: [orderChangeTypes: orderChangeTypes, selected: orderChangeType]
            }
        }
    }

    def show (){
        def orderChangeType = webServicesSession.getOrderChangeTypeById(params.int('id'))

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, orderChangeType.id, orderChangeType.name)

        render template: 'showOrderChangeType', model: [selected: orderChangeType]
    }

    def edit (){
        def orderChangeType = params.id ? webServicesSession.getOrderChangeTypeById(params.int('id')) : new OrderChangeTypeWS()

        def actionType = params.id ? 'edit' : 'create'
        def crumbDescription = params.id ? orderChangeType?.name : null

        breadcrumbService.addBreadcrumb(controllerName, actionType, null, params.int('id') ?: null, crumbDescription)

        def itemTypes = productService.getItemTypes(session['company_id'], null)
        render view: 'editOrderChangeType', model: [orderChangeType: orderChangeType, itemTypes: itemTypes]
    }

    def save (){

        OrderChangeTypeWS ws = new OrderChangeTypeWS()
        bindData(ws, params)

         //BIND THE META FIELDS
        def metaFieldIdxs = []
        def pattern = Pattern.compile(/metaField(\d+).id/)
        //get all the ids in an array
        params.each{
            def m = pattern.matcher(it.key)
            if( m.matches()) {
                metaFieldIdxs << m.group(1)
            }
        }

        ws.orderChangeTypeMetaFields = new HashSet<MetaFieldWS>(metaFieldIdxs.size());
        int index = 0;
        //get the meta field values for each id
        metaFieldIdxs.each {
            MetaFieldWS metaField = MetaFieldBindHelper.bindMetaFieldName(params, it)
            metaField.primary = false
            metaField.entityType = EntityType.ORDER_CHANGE
            metaField.entityId = session['company_id']
            ws.orderChangeTypeMetaFields << metaField;
            index++;
        }


        log.debug ws
        ws.setEntityId(session['company_id'].toInteger())
        log.debug ws
        if (ws.name) {
            ws.name = ws.name.trim()
        }

        try {
            boolean retVal = webServicesSession.createUpdateOrderChangeType(ws);
            if (params.isNew == "true") {
                flash.message = 'config.orderChangeTypes.created'
            } else {
                flash.message = 'config.orderChangeTypes.updated'
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'editOrderChangeType', model: [orderChangeType: ws, itemTypes: productService.getItemTypes(session['company_id'], null)]
            return
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'config.orderChangeTypes.saving.error'
        }
        redirect(action: 'list')
    }

    def delete (){
        log.debug "delete called on ${params.id}"
        if (params.id) {
            def orderChangeType = webServicesSession.getOrderChangeTypeById(params.int('id'))
            if (orderChangeType) {
                try {
                    webServicesSession.deleteOrderChangeType(params.id?.toInteger());
                    flash.message = 'config.orderChangeTypes.delete.success'
                    flash.args = [params.id]
                } catch (SessionInternalError e) {
                    viewUtils.resolveException(flash, session.locale, e);
                } catch (Exception e) {
                    log.error e.getMessage()
                    flash.error = 'config.orderChangeTypes.delete.error'
                }
            }
        }

        // render the list
        params.applyFilter = true
        params.id = null
        getList(params)
    }

    /**
     * Use the meta field specified by 'mfId' to act as template for order change meta field
     *
     * @param mfId - MetaField id
     */
    def populateOrderChangeMetaFieldForEdit (){
        MetaFieldWS metaField;
        if(params.mfId && params.mfId != 'null') {
            metaField = MetaFieldBL.getWS(MetaField.read(params.int('mfId')))
            metaField.id = 0
        } else {
            metaField = null
        }
        render template: 'editOrderChangeMetaField', model: [ metaField: metaField, metaFieldIdx: params.startIdx ?: 0, moveMetaFields: true ]
    }
}
