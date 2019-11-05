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

import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper

import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.ServerConstants;

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.common.SessionInternalError

import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import com.sapienter.jbilling.server.util.db.EnumerationDTO
import com.sapienter.jbilling.server.metafields.DataType

/**
 * @author Alexander Aksenov
 * @since 20.10.11
 */
@Secured(['isAuthenticated()'])
class MetaFieldsController {

    static scope = "prototype"

    def viewUtils
    def webServicesValidationAdvice
    def breadcrumbService
    IWebServicesSessionBean webServicesSession
    GrailsConventionGroovyPageLocator groovyPageLocator

    def index () {
        redirect(action: 'listCategories')
    }

    def listCategories () {

        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ServerConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (!usingJQGrid){
           ArrayList<EntityType> categorylist = Arrays.asList(EntityType.values())
		   		categorylist.remove(EntityType.PAYMENT_METHOD_TEMPLATE)
				categorylist.remove(EntityType.PAYMENT_METHOD_TYPE)
                categorylist.remove(EntityType.ACCOUNT_TYPE)
            [lst: categorylist]
        }
    }

    def findCategories () {
        def categories = Arrays.asList(EntityType.values());

        try {
            def jsonData = getAsJsonData(categories, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    def list () {

        EntityType entityType

        MetaField selectedField = null;
        if (params.id && params.get('id').isInteger()) {

            selectedField= MetaField.findById(params.get('id'))
            entityType= selectedField?.entityType
        } else {
            if (params.id) {
                entityType= EntityType.valueOf(params.get('id').toString())
            }
        }

        // if id is present and object not found, give an error message to the user along with the list
        if (params.id && params.get('id').isInteger() && selectedField == null) {
            flash.error = 'metaField.not.found'
            flash.args = [params.id]
        }

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ServerConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.template) {
                render template: 'listMetaFieldsTemplate', model: [selectedCategory: entityType?.name(), selected: new MetaField()]
            } else {
                if (params.selectedId) {
                    selectedField = MetaField.findById(params.int("selectedId"))
                }
                render(view: 'listCategories', model: [selectedCategory: entityType?.name(), selected: selectedField])
            }
            return
        }

        def lstByCateg = MetaFieldBL.getAvailableFieldsList(session['company_id'], entityType);
        if (params.template)
            render template: 'listMetaFieldsTemplate', model: [lstByCategory: lstByCateg, selected: new MetaField()]
        else {
            if (params.selectedId) {
                selectedField = MetaField.findById(params.int("selectedId"))
            }
            render(view: 'listCategories', model: [selectedCategory: entityType?.name(), lst: Arrays.asList(EntityType.values()), lstByCategory: lstByCateg, selected: selectedField])
        }
    }

    def findMetaFields () {
        EntityType entityType
        if (params.id && params.get('id').isInteger()) {
            //Get the EntityType from the selected meta field
            entityType = MetaField.findById(params.get('id'))?.entityType
        } else {
            if (params.id) {
                entityType = EntityType.valueOf(params.get('id').toString())
            }
        }
        def metaFields = MetaFieldBL.getAvailableFieldsList(session['company_id'], entityType)

        try {
            def jsonData = getAsJsonData(metaFields, params)

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

    def show () {
        def metaField = MetaField.get(params.int('id'))

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, metaField.id, metaField.name)

        render template: 'show', model: [selected: metaField]
    }

    def edit () {

        def metaField = params.id ? webServicesSession.getMetaField(params.int("id")) : new MetaFieldWS()

        if (metaField == null) {
            redirect action: 'list', params: params
            return
        }
        def descriptions=[]

		long inUse = 0
		if(metaField?.id) {
			inUse= new MetaFieldDAS().getTotalFieldCount(metaField?.id)
		}

        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? metaField?.getName() : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription)

        [metaField: metaField,descriptions:descriptions, inUse : inUse]
    }

    def save () {

        def metaField;

        try {
            metaField = MetaFieldBindHelper.bindMetaFieldName(params, false)
        } catch (SessionInternalError e) {
            metaField = MetaFieldBindHelper.bindMetaFieldName(params, true)
            viewUtils.resolveException(flash, session.locale, e)
            render view: 'edit', model: [ metaField : metaField ]
            return
        }

		if(metaField?.name?.trim()==''){
			flash.error = 'bean.MetaFieldValueWS.fieldName.empty'
			render view: 'edit', model: [ metaField: metaField ]
			return
		}
        if(metaField?.isDisabled() && metaField?.isMandatory()){
           flash.error = 'metaField.mandatory.disable'
           render view: 'edit',model:  [ metaField: metaField ]
           return
        }
		
        if (!MetaFieldBindHelper.validateErrorMessage(metaField?.validationRule?.errorMessages)){
            flash.error = 'validation.error.invalid.error.messages'
            flash.args = [Arrays.asList(MetaFieldBindHelper.BLACK_LIST_PATTERN)]
            render view: 'edit', model: [ metaField: metaField ]
            return
        }

        def existingMetaField = new MetaFieldDAS().getFieldByName(session['company_id'], metaField.entityType as EntityType[], metaField.name, true);

        if (existingMetaField != null && existingMetaField.id != metaField.id) {
            flash.error = 'metaField.name.exists'
            render view: 'edit', model: [ metaField: metaField ]
            return
        }

        if (metaField.getDataType().equals(DataType.SCRIPT) && (metaField.getFilename())) {
            def script = groovyPageLocator.findTemplate("/metaFields/${metaField.dataType.name().toLowerCase()}/${metaField.filename}")
            if (!script) {
                flash.error = g.message(code: 'metafield.validation.script.not.exist', args: [metaField.getFilename()])
                render view: 'edit', model: [metaField: metaField]
                return
            }
        }
        // set the field to be primary field
        metaField.primary = true
        
        // validate and create/update
        try {
            if (!metaField.id || metaField.id == 0) {
                log.debug("saving new metaField ${metaField}")
                def id = webServicesSession.createMetaField(metaField)
                metaField.id = id
                flash.message = 'metaField.created'
                flash.args = [metaField.id]

            } else {
                log.debug("updating meta field ${metaField.id}")

                webServicesSession.updateMetaField(metaField)

                flash.message = 'metaField.updated'
                flash.args = [metaField.id]
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render view: 'edit', model: [ metaField : metaField ]
            return
        }

        redirect(action: "list", id: metaField.entityType.name(), params: ["selectedId": metaField.id])
    }

    def delete () {
        MetaField metaField = null

        if (params.id) {
            metaField = MetaField.findById(params.int("id"))
        }

        try {
            long useCnt= new MetaFieldDAS().getTotalFieldCount(metaField.id)
            log.debug "Meta field values: $useCnt exist"
            if ( useCnt> 0){
                log.debug("Can not delete metafield ${metaField.getId()}, it is in use.")
                flash.error = 'Can not delete metafield '+metaField.getId()+', it is in use.'
            } else {
                new MetaFieldBL().deleteIfNotParticipant(params.int('id'))
                flash.message = 'metaField.deleted'
            }
        } catch (SessionInternalError e) {
            e.printStackTrace()
            viewUtils.resolveException(flash, session.locale, e)
            redirect action: 'list', params: params
            return
        }

        flash.args = [params.id]
        redirect action: "list", id: metaField.entityType
    }

    def updateValidationModel () {
        def validationRule = MetaFieldBindHelper.bindValidationRule(params)

        render template: '/metaFields/validation/validation',
                model: [ validationRule: validationRule, parentId: params.parentId, metaFieldIdx: params.metaFieldIdx, enabled:validationRule?true:false ]
    }

}
