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
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.pluggableTask.PluggableTask
import com.sapienter.jbilling.server.pluggableTask.admin.*
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.ServerConstants
import grails.converters.JSON
import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.ObjectNotFoundException
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions
import org.springframework.security.access.annotation.Secured

@Secured(["isAuthenticated()"])
class PluginController {
	
	static scope = "prototype"
	static pagination = [ max: 10, offset: 0 ]
	static final viewColumnsToFields =
			['categoryId': 'id',
			 'pluginId': 'id',
			 'type': 'pluginType.className',
			 'order': 'processingOrder',
			 'description': 'description']
	
    // all automatically injected by Grails. Thanks.
    IWebServicesSessionBean webServicesSession
    PluginAwareResourceBundleMessageSource messageSource
    PluggableTaskDAS pluggableTaskDAS
    ViewUtils viewUtils
    BreadcrumbService breadcrumbService;
    RecentItemService recentItemService
    
    def index () {
        list();
    }

    /*
     * Lists all the categories. The same for every company
     */
    def list () {
        if (params.id) {
            showListAndPlugin(params.id as Integer);
            return
        }

		breadcrumbService.addBreadcrumb("plugin", "list", null, null);
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], CommonConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid) {
            render (view: "list")
            return
        }

        params.max = Integer.MAX_VALUE // There is no pagination in the old look
        def categories = getPluginCategories(params)
        log.info "Categories found= ${categories?.totalCount}"
        render (view:"list", model:[categories:categories])
	}

    def findCategories () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def categories = getPluginCategories(params)

        try {
            render getAsJsonData(categories, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    def getPluginCategories(params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def languageId = session['language_id']

        return PluggableTaskTypeCategoryDTO.createCriteria().list(
                max: params.max,
                offset: params.offset
        ) {
            if (params.categoryId) {
                eq('id', params.getInt('categoryId'));
            }
            if (params.description){
                def searchParam = params.description
                or {
                    addToCriteria(Restrictions.ilike("interfaceName", searchParam, MatchMode.ANYWHERE));
                    searchParam = searchParam.toLowerCase()
                    sqlRestriction(
                            """ exists (
                                            select a.foreign_id
                                            from international_description a
                                            where a.foreign_id = {alias}.id
                                            and a.table_id =
                                             (select b.id from jbilling_table b where b.name =? )
                                            and a.language_id = ?
                                            and a.psudo_column = 'description'
                                            and lower(a.content) like ?
                                        )
                                    """, [ServerConstants.TABLE_PLUGGABLE_TASK_TYPE_CATEGORY, languageId, "%" + searchParam + "%"]
                    )
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    /**
     * Converts * to JSon
     */
    private def Object getAsJsonData(elements, GrailsParameterMap params) {
        def jsonCells = elements
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords = jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    /*
     * This action lists all the plug-ins that belong to a Company and to 
     * the selected Category
     */
    def plugins () {
        Integer languageId = session.language_id;
        Integer entityId = session.company_id;
        log.info "entityId= ${entityId}"
        log.info "selected ${params["id"]}"
        if (params["id"]) {
            Integer categoryId = Integer.valueOf(params["id"]);
            log.info "Category Id selected= ${categoryId}"
            
            breadcrumbService.addBreadcrumb("plugin", "plugins", null, categoryId);

            // add the category id to the session, so the 'create' button can know
            // which category to create for
            session.selected_category_id = categoryId;

            def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], CommonConstants.PREFERENCE_USE_JQGRID);
            //If JQGrid is showing, the data will be retrieved when the template renders
            if (usingJQGrid){
                if (params.template == 'show') {
                    render template: "pluginsTemplate", model:[selectedCategoryId: categoryId]
                } else {
                    render (view:"list", model:[selectedCategoryId: categoryId])
                }
                return
            }

            def lstByCateg = getPlugins(params, params.getInt('id'))
            log.info "number of plug-ins= ${lstByCateg.size()}";
            // show the list of the plug-ins
            if (params.template == 'show') {
                render template: "pluginsTemplate", model:[plugins:lstByCateg]
            } else {
                params.max = Integer.MAX_VALUE // There is no pagination in the old look
                def categories = getPluginCategories(params)
                render (view:"list", model:[categories:categories, plugins:lstByCateg])
            }
        } else {
            log.error "No Category selected?"
        }
    }

    def findPlugins () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def plugins = getPlugins(params, params.getInt('id'))

        try {
            render getAsJsonData(plugins, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    def getPlugins(params, categoryId) {
        params.max = pagination.max
        // This fixes an issue when retrieving the plugins from the database
        // If more than max appear, they will be left out.
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def languageId = session['language_id']

        return PluggableTaskDTO.createCriteria().list(
                //max: params.max,
                offset: params.offset
        ) {
            eq('entityId', session['company_id'])
            createAlias('type', 'pluginType')
            createAlias('pluginType.category', 'pluginCategory')
            eq('pluginCategory.id', categoryId);
            if (params.pluginId) {
                eq('id', params.getInt('pluginId'));
            }
            if (params.type){
                def searchParam = params.type
                or {
                    addToCriteria(Restrictions.ilike("pluginType.className", searchParam, MatchMode.ANYWHERE));
                    searchParam = searchParam.toLowerCase()
                    sqlRestriction(
                            """ exists (
                                            select a.foreign_id
                                            from international_description a
                                            where a.foreign_id = {alias}.type_id
                                            and a.table_id =
                                             (select b.id from jbilling_table b where b.name = ? )
                                            and a.language_id = ?
                                            and a.psudo_column = 'title'
                                            and lower(a.content) like ?
                                        )
                                    """, [ServerConstants.TABLE_PLUGGABLE_TASK_TYPE, languageId, "%" + searchParam + "%"]
                    )
                }
            }
            resultTransformer org.hibernate.Criteria.DISTINCT_ROOT_ENTITY
            SortableCriteria.sort(params, delegate)
        }
    }

    def show () {
        Integer taskId = params.id.toInteger();

            breadcrumbService.addBreadcrumb("plugin", "show", null, taskId);
            PluggableTaskDTO dto = pluggableTaskDAS.findNow(taskId);
            if ( null == dto ) {
                pluginNotFoundErrorRedirect(taskId)
                return
            }
            
            if (params.template == 'show') {
                render template: "show", model: [plugin: dto]
            } else {
                // its being called by the breadcrumbs
                showListAndPlugin(taskId);
            }
    }
    
    def showForm () {

        // find out the category name
        PluggableTaskTypeCategoryDTO category =  
			new PluggableTaskTypeCategoryDAS().find(session.selected_category_id);

        List<PluggableTaskTypeDTO> typesList = new PluggableTaskTypeDAS().findAllByCategory(category.getId());

        if (typesList?.isEmpty()){
            flash.error = messageSource.getMessage("no.plugin.types",null, session.locale)
            redirect (action: 'list')
            return
        }

        // show the form with the description
        render (view:"form", model:
                [description: category?.getDescription(session.language_id), isEdit: false,
                 types:typesList, parametersDesc : getDescriptions(typesList.first()?.getId())])
    }
    
    /*
     * This is called when a new type is picked from the drop down list of plug-in types (classes)
     * and the parameters need to be re-rendered
     */
    def getTypeParametersDescriptions () {
        log.info "Getting parameters for plug-in type " + params.typeId;
        
        render template:"formParameters", model:[parametersDesc : getDescriptions(params.typeId as Integer) ]
    }
    
    private List<ParameterDescription> getDescriptions(Integer typeId) {
        PluggableTaskTypeDTO type = new PluggableTaskTypeDAS().find(typeId);
        // create a new class to extract the parameters descriptions
        PluggableTask thisTask = PluggableTaskManager.getInstance(type.getClassName(), 
            type.getCategory().getInterfaceName());
        return thisTask.getParameterDescriptions();
    }

    def save () {
        // Create a new object from the form
        PluggableTaskWS newTask = new PluggableTaskWS();
        bindData(newTask, params);

        bindPluginParameters(newTask, params, false)
        
        // save
        Locale locale = session.locale;
        try {
            log.info "now saving ${newTask} by ${session.user_id}";
            Integer pluginId;
            if (newTask.getId() == null || newTask.getId() == 0) {
                pluginId = webServicesSession.createPlugin(newTask);
            	pluggableTaskDAS.invalidateCache(); // or the list won't have the new plug-in
            
            	// the message
            	flash.message = messageSource.getMessage("plugins.create.new_plugin_saved", [pluginId].toArray(), locale);
            } else { 
                // it is an update
                webServicesSession.updatePlugin(newTask);
            	flash.message = messageSource.getMessage("plugins.create.plugin_updated", [newTask.getId()].toArray(), locale);
                pluginId = newTask.getId();
            }

            if ( com.sapienter.jbilling.common.Util.getSysPropBooleanTrue(ServerConstants.PROPERTY_RUN_API_ONLY_BUT_NO_BATCH) ) {
                flash.warn = "plugins.plugin.no.batch"
            }
            
            // forward to the list of plug-in types and the new plug-in selected
            showListAndPlugin(pluginId);
        } catch(SessionInternalError e) {
            // process the exception so the error messages from validation are
            // put in the flash
            viewUtils.resolveException(flash, locale, e);
            // mmm... this can fail if the this is a new plug-in, started after a recent item click?
            PluggableTaskTypeCategoryDTO category =  
					new PluggableTaskTypeCategoryDAS().find( session.selected_category_id );
            
            // render the form again, with all the data
            render(view: "form", model:
                    [description: category?.getDescription(session.language_id),
                            types: new PluggableTaskTypeDAS().findAllByCategory(category.getId()),
                            pluginws: newTask, isEdit: Boolean.valueOf(params.isEdit),
                            parametersDesc: getDescriptions(newTask.getTypeId())])
        }
    }

    def addPluginParameter (){

        PluggableTaskWS newTask = new PluggableTaskWS();
        bindData(newTask, params);
        bindPluginParameters(newTask, params, false)

        render template: 'formParameters', model: [pluginws: newTask,
                parametersDesc : getDescriptions(newTask.getTypeId())]
    }

    def removePluginParameter (){

        PluggableTaskWS newTask = new PluggableTaskWS();
        bindData(newTask, params);
        bindPluginParameters(newTask, params, true)

        render template: 'formParameters', model: [pluginws: newTask,
                parametersDesc : getDescriptions(newTask.getTypeId())]
    }

    def bindPluginParameters(newTask, params, excluded) {

        Integer parameterIndex = params.parameterIndexField.toInteger()
        for(String key: params.keySet()) { // manually bind the plug-in parameters
            def value = params.get(key)
            if (key.startsWith("plg-parm-") && value) {
                newTask.parameters.put(key.substring(9), value);
            }
        }

        def dynamicParameterMap = new TreeMap<Integer, GrailsParameterMap>()
        params.plgDynamic.each{ k, v ->
            if (v instanceof Map)
                dynamicParameterMap.put(k, v)
        }

        dynamicParameterMap.each{ i, parameterMap ->
            if (parameterMap instanceof Map && parameterMap.name) {

                if ((excluded && i.toInteger() != parameterIndex)
                        || (i.toInteger() < parameterIndex)) {
                    newTask.parameters.put(parameterMap.name, parameterMap.value)
                }
            }
        }
    }
    
    private void showListAndPlugin(Integer pluginId) {
        
		try {
			PluggableTaskDTO dto = pluginId ? new PluggableTaskDAS().findNow(pluginId) : null;

			if (pluginId && dto == null) {
				pluginNotFoundErrorRedirect(pluginId)
			} else {
                def categoryId = dto?.getType()?.getCategory().getId()
                breadcrumbService.addBreadcrumb("plugin", "show", null, pluginId);
				render (view: "showListAndPlugin", model:
				[plugin: dto,
					selectedCategoryId: categoryId,
					plugins: pluggableTaskDAS.findByEntityCategory(session.company_id, categoryId),
					parametersDesc: getDescriptions(dto.getType().getId())]);
			}
		} catch (ObjectNotFoundException ex) {
			println"Caught exception"
			flash.error = g.message(code: "validation.error.invalid.pluginid", args: [pluginId])
			redirect(action: 'list')
		}
	}

    def cancel () {
		
        flash.message = messageSource.getMessage("plugins.edit.canceled", null, session.locale);
        if ((params.plugin_id as Integer) > 0 ) {
            // go the the list with the plug selected
            showListAndPlugin(params.plugin_id as Integer);
        } else {
            // it was creating a new one
            redirect (action:'list')
        }
    }

    def edit () {
        PluggableTaskDTO dto =  pluggableTaskDAS.find(params.id as Integer);
        if (dto != null) {
            breadcrumbService.addBreadcrumb("plugin", "edit", null, dto.getId());
            PluggableTaskTypeCategoryDTO category =  dto.getType().getCategory();
            render (view: "form", model:
                    [description: category.getDescription(session.language_id),
                            types: new PluggableTaskTypeDAS().findAllByCategory(category.getId()),
                            pluginws: PluggableTaskBL.getWS(dto), isEdit: true,
                            parametersDesc: getDescriptions(dto.getType().getId())])
        } else {
            pluginNotFoundErrorRedirect(params.id)
        }
    }
    
    private void pluginNotFoundErrorRedirect(pluginId) {
    	flash.error="plugins.plugin.not.found"
		flash.args = [ pluginId as String ]
        redirect action: 'list'
    }

    def delete () {
        Integer id = null
        try {
            id = params.id as Integer;
        	webServicesSession.deletePlugin(id);
        	pluggableTaskDAS.invalidateCache(); // or the list will still show the deleted plug-in
        	flash.message = messageSource.getMessage("plugins.delete.done",[id].toArray(), session.locale);
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            showListAndPlugin(id)
            return
        } catch (Exception e){
            viewUtils.resolveException(flash, session.locale, e)
        }
        redirect (action:'list')
    }
	
}
