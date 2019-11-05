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
import com.sapienter.jbilling.server.user.RoleBL
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.ServerConstants
import grails.converters.JSON
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
/**
 * RoleController 
 *
 * @author Brian Cowdery
 * @since 02/06/11
 */
class RoleController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    static final viewColumnsToFields =
            ['roleId': 'id']
    
	static scope = "prototype"
    def breadcrumbService
    def viewUtils

    def index () {
        redirect action: 'list', params: params
    }

    def getList(params) {
		
		def company_id = session['company_id'] as Integer 
		
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
		params.sort = params?.sort ?: pagination.sort
		params.order = params?.order ?: pagination.order
        def languageId = session['language_id']

        return RoleDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
			eq('company', new CompanyDTO(company_id))
            if ( params.roleId ) {
                def searchParam = params.roleId
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
                                            and a.psudo_column = 'title'
                                            and lower(a.content) like ?
                                        )
                                    """,[ServerConstants.TABLE_ROLE,languageId,searchParam]
                    )
                }
            }
            SortableCriteria.sort(params, delegate)
		}
    }

    def list () {
        
        def selected = params.id ? RoleDTO.get(params.int('id')) : null
		// if id is present and object not found, give an error message to the user along with the list
        if (params.id?.isInteger() && selected == null) {
			flash.error = 'role.not.found'
            flash.args = [params.id]
        }

		breadcrumbService.addBreadcrumb(controllerName, 'list',
			selected?.getTitle(session['language_id']), selected?.id, selected?.getDescription(session['language_id']))
		
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ServerConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'rolesTemplate', model: [selected: selected ]
            }else {
                render view: 'list', model: [selected: selected ]
            }
            return
        }

        def roles = getList(params)
        if (params.applyFilter || params.partial) {
            render template: 'rolesTemplate', model: [ roles: roles, selected: selected ]
        } else {
            render view: 'list', model: [ roles: roles, selected: selected ]
        }
    }

    def findRoles () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def roles = getList(params)

        try {
            def jsonData = getRolesJsonData(roles, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts Roles to JSon
     */
    private def Object getRolesJsonData(roles, GrailsParameterMap params) {
        def jsonCells = roles
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def show () {
        def role = RoleDTO.get(params.int('id'))

        breadcrumbService.addBreadcrumb(controllerName, 'list', role.getTitle(session['language_id']), role.id, role.getDescription(session['language_id']))

        render template: 'show', model: [ selected: role ]
    }

    def edit () {
        def role = chainModel?.role ?: params.id ? RoleDTO.get(params.int('id')) : new RoleDTO()
        
        if (role == null) {
        	redirect action: 'list', params:params
            return
        }
        
        def crumbName = params.id ? role?.getTitle(session['language_id']) : null
        def crumbDescription = params.id ? role?.getDescription(session['language_id']) : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription)

		def roleTitle = chainModel?.roleTitle;
		def roleDescription = chainModel?.roleDescription;
		def validationError = chainModel?.validationError ? true : false;

        [ role: role, roleTitle:roleTitle, roleDescription:roleDescription, validationError:validationError ]
    }

    def save () {
    	
    	def role = new RoleDTO();
    	role.company = CompanyDTO.get(session['company_id'])
	    bindData(role, params, 'role')
    	def roleTitle = params.role.title == null ?: params.role.title.trim();
    	def roleDescription = params.role.description == null ?: params.role.description.trim();
    	def languageId = session['language_id'];
    	
    	try {
	
			def isNonEmptyRoleTitle = params.role.title ? !params.role.title.trim().isEmpty() : false;
			if (isNonEmptyRoleTitle) {
	            def roleService = new RoleBL();
	
	            // save or update
	            if (!role.id || role.id == 0) {
	                log.debug("saving new role ${role}")
	                roleService.validateDuplicateRoleName(roleTitle, languageId, role.company.id)
	                role.id = roleService.create(role)
					roleService.updateRoleType(role.id)
	
	                flash.message = 'role.created'
	                flash.args = [role.id as String]
	
	            } else {
	                log.debug("updating role ${role.id}")
	
	                roleService.set(role.id)
	                
	                if (!roleService.getEntity()?.getDescription(languageId, ServerConstants.PSUDO_COLUMN_TITLE)?.equalsIgnoreCase(roleTitle)) {
	                	roleService.validateDuplicateRoleName(roleTitle, languageId, role.company.id)
	                }
	                
	                roleService.update(role)
	
	                flash.message = 'role.updated'
	                flash.args = [role.id as String]
	            }
	
	            // set/update international descriptions
	            roleService.setTitle(languageId, roleTitle)
	            roleService.setDescription(languageId, roleDescription)
	            chain action: 'list', params: [id: role.id]
	        } else {
				
	            String [] errors = ["RoleDTO,title,role.error.title.empty"]
				throw new SessionInternalError("Description is missing ", errors);            
	        }
        
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            chain action: 'edit', model: [ role:role, roleTitle:roleTitle, roleDescription:roleDescription, validationError:true ]
        }
    }

    def delete () {
        if (params.id) {
        	def roleService = new RoleBL(params.int('id'))
            roleService.deleteDescription(session['language_id'])
			roleService.deleteTitle(session['language_id'])
            roleService.delete()
            log.debug("Deleted role ${params.id}.")
        }

        flash.message = 'role.deleted'
        flash.args = [ params.id ]

        // render the partial role list
        params.applyFilter = true
        params.id = null
        redirect action: 'list'
    }
}
