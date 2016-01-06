package jbilling

import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldGroupWS
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

/**
 * @author Oleg Baskakov
 * @since 25.04.13
 * 
 */

@Secured(['isAuthenticated()'])
class MetaFieldGroupController {

    def viewUtils
    def webServicesValidationAdvice
    def breadcrumbService
	IWebServicesSessionBean webServicesSession
		
	static pagination = [max: 10, offset: 0,  order: 'asc']
	
    def index (){
        listCategories()
    }

    def listCategories (){
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], com.sapienter.jbilling.server.util.Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (!usingJQGrid) {
            ArrayList<EntityType> categorylist = Arrays.asList(EntityType.values());
				categorylist.remove(EntityType.PAYMENT_METHOD_TEMPLATE);
				categorylist.remove(EntityType.PAYMENT_METHOD_TYPE);
            render view: 'listCategories', model : [categories: categorylist]
        }
    }

    def findCategories (){
        def categories = Arrays.asList(EntityType.values());

        try {
            def jsonData = getAsJsonData(categories, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    def list (){

        EntityType entityType
        MetaFieldGroupWS selectedGroup = null;

		def listColumn=params.listColumn?:'column2'

        if (params.selectedId?.isInteger()) {
            selectedGroup = webServicesSession.getMetaFieldGroup(params.int("selectedId"))
            entityType = selectedGroup?.entityType
			listColumn='column1'
        }

        if (params.id && params.get('id').isInteger()) {

            selectedGroup= webServicesSession.getMetaFieldGroup((params.int('id')))
            entityType= selectedGroup?.entityType
        } else {
            if (params.id) {
                entityType= EntityType.valueOf(params.get('id').toString())
            }
        }
		
		// if id is present and object not found, give an error message to the user along with the list
        if (params.selectedId  && selectedGroup == null) {
			flash.error = 'metaFieldGroup.not.found'
            flash.args = [params.selectedId]
        }

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], com.sapienter.jbilling.server.util.Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.template) {
                render template: 'metaFieldGroupsTemplate', model: [selectedCategory: entityType?.name(), entityType:entityType]
            } else {
                render(view: 'listCategories', model: [selectedCategory: entityType?.name(), selected: selectedGroup])
            }
            return
        }
		
		def groups=[]
		if(entityType){

			groups = getMetaFieldGroupsList(entityType, params)
		}

        if (params.template)
            render template: 'metaFieldGroupsTemplate', model: [groups: groups,entityType:entityType,listColumn:listColumn]
        else {
            render(view: 'listCategories', model: [selectedCategory: entityType?.name(), categories: Arrays.asList(EntityType.values()),
													groups: groups, selected: selectedGroup, listColumn:listColumn])
        }
    }

    def findMetaFieldGroups (){
        EntityType entityType
        if (params.selectedId?.isInteger()) {
            entityType = webServicesSession.getMetaFieldGroup(params.int("selectedId"))?.entityType
        } else  if (params.id && params.get('id').isInteger()) {
            entityType = webServicesSession.getMetaFieldGroup(params.int("id"))?.entityType
        } else {
            if (params.id) {
                entityType= EntityType.valueOf(params.get('id').toString())
            }
        }
        def groups = getMetaFieldGroupsList(entityType, params)

        try {
            def jsonData = getAsJsonData(groups, params)

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
		def metaFieldGroup = webServicesSession.getMetaFieldGroup(params.int("id"))
		breadcrumbService.addBreadcrumb(controllerName, 'list', null, metaFieldGroup.id, metaFieldGroup.getDescription())

		render template: 'show', model: [selected: metaFieldGroup]
	}

    def edit (){
        MetaFieldGroupWS metaFieldGroup = params.id ?
            webServicesSession.getMetaFieldGroup(params.int("id")) : new MetaFieldGroupWS()

        if (!metaFieldGroup.entityType) {
            if (params.entityType) {
                metaFieldGroup.entityType = EntityType.valueOf(params.entityType)
            } else {
                flash.error = 'metaFieldGroup.entitytype.not.set'
                redirect action: 'list', params: params
                return

            }
        }
        def metafieldLists = getMetafieldListsForSelects(metaFieldGroup)
        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? metaFieldGroup?.getDescription() : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription)
        render view: 'edit', model: [metaFieldGroup: metaFieldGroup] + metafieldLists
    }

    def clone (){
        MetaFieldGroupWS metaFieldGroup = webServicesSession.getMetaFieldGroup(params.int("id"))
        if (!metaFieldGroup) {
            redirect action: 'list', params: params
            return
        }

        //we will threat this object as a new in save action
        metaFieldGroup.setId(0)

        if (!metaFieldGroup.entityType) {
            if (params.entityType) {
                metaFieldGroup.entityType = EntityType.valueOf(params.entityType)
            } else {
                flash.error = 'metaFieldGroup.entitytype.not.set'
                redirect action: 'list', params: params
                return

            }
        }
        def crumbName = 'cloning'
        def crumbDescription = params.id ? metaFieldGroup?.getDescription() : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription)
        render view: 'edit', model: [metaFieldGroup: metaFieldGroup] + getMetafieldListsForSelects(metaFieldGroup)
    }

    def save (){
        MetaFieldGroupWS metaFieldGroup = new MetaFieldGroupWS()
        //	bindData(metaFieldGroup, params, 'metaFieldGroup')
        metaFieldGroup.setDisplayOrder(params.int("displayOrder"))
        def p = params
        metaFieldGroup.setEntityType(EntityType.valueOf(params["entityType"]))
        metaFieldGroup.setName(params["name"]);
        if (params.get("selected-fields")) {
            def fieldsString = params.get("selected-fields")?.trim()?.split(',')
            def metaField;
            List metaFields = new ArrayList();
            fieldsString?.each {
                try {
                    if (it) {
                        metaField = webServicesSession.getMetaField(Integer.parseInt(it))
                        metaFields.add(metaField)
                    }
                } catch (Exception e) {
                    log.error("Can't add metaField (id:${it}) to metafieldGroup ", e)
                }
            }

            metaFieldGroup.setMetaFields(metaFields.toArray(new MetaFieldWS[metaFields.size()]))

        }

        // validate
        try {
            if (StringUtils.isEmpty(params["name"]?.trim())) {
                String[] errmsgs= new String[1];
                errmsgs[0]= "MetaFieldGroupWS,name,metaFieldGroupWS.error.name.blank"
                throw new SessionInternalError("There is an error in  data.", errmsgs );
            }

            if (!params.id || params?.id.equals("0")) {
                log.debug("saving new metaFieldGroup ${metaFieldGroup}")
                metaFieldGroup.id = webServicesSession.createMetaFieldGroup(metaFieldGroup)

                flash.message = 'metaFieldGroup.created'
                flash.args = [metaFieldGroup.id]

            } else {
                log.debug("updating meta field group ${metaFieldGroup.id}")
                metaFieldGroup.id = params.int('id')
                webServicesSession.updateMetaFieldGroup(metaFieldGroup)

                flash.message = 'metaFieldGroup.updated'
                flash.args = [metaFieldGroup.id]
            }
        } catch (SessionInternalError e) {
            flash.clear()
            viewUtils.resolveException(flash, session.locale, e)
            render view: 'edit', model: [metaFieldGroup: metaFieldGroup] + getMetafieldListsForSelects(metaFieldGroup)
            return
        }

        redirect(action: "list", id: metaFieldGroup.entityType.name(), params: ["selectedId": metaFieldGroup.id])
    }

    def delete (){
        MetaFieldGroupWS metaFieldGroup = null
        try {
            if (!params.id) {
                redirect action: "list"
                return
            }
            metaFieldGroup = webServicesSession.getMetaFieldGroup(params.int("id"))
            if (!metaFieldGroup) {
                redirect action: "list"
                flash.message = 'metaFieldGroup.notfound.error'
                flash.args = [params.id]
                return

            }
            webServicesSession.deleteMetaFieldGroup(metaFieldGroup.id)
            flash.message = 'metaField.deleted'
            flash.args = [params.id]
        } catch (SessionInternalError e) {
            flash.clear()
            viewUtils.resolveException(flash, session.locale, e)
        }

        redirect action: "list", id: metaFieldGroup?.entityType
    }

    private def getMetafieldListsForSelects(MetaFieldGroupWS metaFieldGroup) {
        def companyMetafields = MetaFieldBL.getAvailableFieldsList(session['company_id'], metaFieldGroup.entityType)
        def availableMetafields = []
        def selectedMetafields = []
        if (metaFieldGroup.metaFields) {
            companyMetafields.each { companyMetaField ->
                def contains = metaFieldGroup.metaFields.find {
                    groupMetaField -> groupMetaField.id == companyMetaField.id }
                if (null == contains) {
                    availableMetafields << [value: companyMetaField.id, message: companyMetaField.name]
                }
            }
            metaFieldGroup.metaFields.each {
                selectedMetafields << [value: it.id, message: it.name]
            }

        } else {
            companyMetafields.each { availableMetafields << [value: it.id, message: it.name] }

        }

        [availableMetafields: availableMetafields, selectedMetafields: selectedMetafields]
    }

	def getMetaFieldGroupsList(entityType, params){
		params.max = params?.max?.toInteger() ?: pagination.max
		params.offset = params?.offset?.toInteger() ?: pagination.offset

		return MetaFieldGroup.createCriteria().list(
                max: params.max,
                offset: params.offset
        ) {
            order("displayOrder", "asc")
            eq('entity', new CompanyDTO(session['company_id']))
			eq('entityType', entityType)	
			eq('class',MetaFieldGroup.class)
        }
	}
}
