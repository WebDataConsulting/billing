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
import com.sapienter.jbilling.common.SessionInternalError
import org.joda.time.format.DateTimeFormat
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.report.db.ReportDTO
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.report.db.ReportTypeDTO
import com.sapienter.jbilling.server.report.ReportBL

import com.sapienter.jbilling.server.report.ReportExportFormat
import com.sapienter.jbilling.client.util.ClientConstants;
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.server.report.db.ReportParameterDTO

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions


import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

/**
 * ReportController 
 *
 * @author Brian Cowdery
 * @since 07/03/11
 */
@Secured(["isAuthenticated()"])
class ReportController {

    static scope = "prototype"
    static pagination = [ max: 10, offset: 0 ]
    static final viewColumnsToFields =
            ['reportId': 'id']
	
    def viewUtils
    def filterService
    def breadcrumbService

    def index () {
        list()
    }

    def getReportTypes() {
        params.max = pagination.max
        // This fixes an issue when retrieving the types from the database
        //If more than max appear, they will be left out.
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        return ReportTypeDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            resultTransformer org.hibernate.Criteria.DISTINCT_ROOT_ENTITY
            SortableCriteria.sort(params, delegate)
        }
    }

    def getReports(Integer typeId) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
		def company_id = session['company_id']
        return ReportDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {

            if (typeId) {
                eq('type.id', typeId)
            }

            entities {
                eq('id', company_id)
            }
            if(params.name) {
                or{
                    addToCriteria(Restrictions.ilike("fileName",  params.name, MatchMode.ANYWHERE))
                    addToCriteria(Restrictions.ilike("name", params.name, MatchMode.ANYWHERE));
                }
            }

            SortableCriteria.sort(params, delegate)
        }
    }

    def list () {
        def type = ReportTypeDTO.get(params.int('id'))
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, params.int('id'), type?.getDescription(session['language_id']))

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ClientConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            render view: 'list', model: [ selectedTypeId: type?.id ]
            return
        }

        def types = getReportTypes()
        def reports = params.id ? getReports(params.int('id')) : null

        render view: 'list', model: [ types: types, reports: reports, selectedTypeId: type?.id ]
    }

    def findTypes () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def types = getReportTypes()

        try {
            render getAsJsonData(types, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    def findReports (){
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def type = params.int('id')
        def reports = getReports(type) // If type is null, then search for all

        try {
            render getAsJsonData(reports, params) as JSON

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

    def reports () {
        def typeId = params.int('id')
        def type = ReportTypeDTO.get(params.int('id'))
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, typeId, type?.getDescription(session['language_id']))

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ClientConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            render template: 'reportsTemplate', model: [selectedTypeId: typeId]
            return
        }
        def reports = typeId ? getReports(typeId) : null
        render template: 'reportsTemplate', model: [ reports: reports, selectedTypeId: typeId ]
    }

    def allReports () {
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ClientConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            render template: 'reportsTemplate', model: []
            return
        }
        def reports = getReports(null)
        render template: 'reportsTemplate', model: [ reports: reports ]
    }

    def show () {
        ReportDTO report = ReportDTO.get(params.int('id'))
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, report?.id, report ? message(code: report.name) : null)

        if (params.template) {
            // render requested template, usually "_show.gsp"
            render template: params.template, model: [ selected: report ]

        } else {
            // render default "list" view - needed so a breadcrumb can link to a reports by id
            def typeId = report?.type?.id
            def types = getReportTypes()
            def reports = getReports(typeId)

            render view: 'list', model: [ types: types, reports: reports, selected: report, selectedTypeId: typeId ]
        }
    }

    /**
     * Runs the given report using the entered report parameters. If no format is selected, the report
     * will be rendered as HTML. If an export format is selected, then the generated file will be sent
     * to the browser.
     */
    def run () {
        def report = ReportDTO.get(params.int('id'))
        bindParameters(report, params)
        def runner = new ReportBL(report, session['locale'], session['company_id'])
		def typeId = report?.type?.id
		def types = getReportTypes()
		def reports = getReports(typeId)
		
		if(params.end_date && params.start_date) {
			Date startDate = DateTimeFormat.forPattern(message(code: 'datepicker.format')).parseDateTime(params.start_date).toDate()
			Date endDate = DateTimeFormat.forPattern(message(code: 'datepicker.format')).parseDateTime(params.end_date).toDate()
			if(endDate.compareTo(startDate)<0) {
				flash.error = message(code: 'report.start_date.before.end_date.valid')
				render view: 'list', model: [ types: types, reports: reports, selected: report, selectedTypeId: typeId ]
				return
			}
		}
		
		if(report.type.name == 'order'){
		    if (null == params.item_id || params.item_id.isEmpty() || !params.item_id.isInteger()) {
		        flash.error = message(code: 'report.item.invalid')
		        render view: 'list', model: [types: types, reports: reports, selected: report, selectedTypeId: typeId]
		        return
		    }
		
		    if (null == new ItemDAS().findNow(params.item_id as Integer)) {
		        flash.error = message(code: 'item.not.exists')
		        render view: 'list', model: [ types: types, reports: reports, selected: report, selectedTypeId: typeId ]
		        return
		    }
		}
		
		if(report.type.name  == 'user') {
			if(params.file_name == 'total_invoiced_per_customer_over_years') {
				if(StringUtils.isEmpty((params.start_year).trim()) || StringUtils.isEmpty((params.end_year).trim())) {
					flash.error = message(code: 'report.start.end.year.not.blank')
					render view: 'list', model: [types: types, reports: reports, selected: report, selectedTypeId: typeId]
					return
				}
			}
		}

        if (params.format) {
            // export to selected format
            def format = ReportExportFormat.valueOf(params.format)
            def export = runner.export(format)
            DownloadHelper.sendFile(response, export?.fileName, export?.contentType, export?.bytes)

        } else {
            // render as HTML
            def imageUrl = createLink(controller: 'report', action: 'images', params: [name: '']).toString()
            runner.renderHtml(response, session, imageUrl)
        }
    }

    /**
     * Returns image data generated by the jasper report HTML rendering.
     *
     * Rendering a jasper report to HTML produces a map of images that is stored in the session. This action
     * retrieves images by name and returns the bytes to the browser. The jasper report HTML contains <code>img</code>
     * tags that look to this action as their source.
     */
    def images () {
        Map images = session[ReportBL.SESSION_IMAGE_MAP]
        response.outputStream << images.get(params.name)
    }

    def bindParameters(report, params) {
        params.each { name, value ->
            ReportParameterDTO<?> parameter = report.getParameter(name)
            if (parameter) {

                bindData(parameter, ['value': value])
            }
        }
		
		try {
			report.childEntities = new ArrayList<Integer>()
			// bind childs to list
			params.list('childs').each { child ->
				report.childEntities.add(Integer.parseInt(child))
			}
		} catch(Exception e) {
			//string is null, 
		}
    }
}
