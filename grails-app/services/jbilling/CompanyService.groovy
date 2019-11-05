package jbilling

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

import com.sapienter.jbilling.server.user.db.CompanyDTO

import org.hibernate.criterion.CriteriaSpecification

import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpSession

class CompanyService implements Serializable {

	static transactional = false
	
	/**
	 * If the current user's company is the Root Company we return a list with its id PLUS all its children ids.
	 * If the logged in user's company is a child company then we return only its id.
	 */
	def getEntityAndChildEntities() {
		CompanyDTO loggedInUserCompany = CompanyDTO.get( session['company_id'] as Integer )
		def childEntities = []
		childEntities << loggedInUserCompany
		childEntities += CompanyDTO.findAllByParent(loggedInUserCompany)
	}

	def getRootCompanyId() {
		CompanyDTO loggedInUserCompany = CompanyDTO.get( session['company_id'] as Integer )
		loggedInUserCompany.parent == null ? loggedInUserCompany.id : loggedInUserCompany.parent.id
	}
	
	def getHierarchyEntities(Integer companyId) {
		def allEntities = []
		def company= CompanyDTO.get(companyId)
		
		if ( null == company) return allEntities
		
		allEntities << company
		allEntities += company.parent ? CompanyDTO.findAllByParent(company.parent) : CompanyDTO.findAllByParent(company)
	}
	
	def isAvailable(boolean isGlobal, Integer entityId, List<Integer> entities) {
		
		if ( isGlobal && getRootCompanyId() == entityId ) {
			return true
		} 
		
		def hierachyIds = entities?.intersect(getEntityAndChildEntities()*.id) 
		return (hierachyIds)
	}
	
    /**
     * Returns the HTTP session
     *
     * @return http session
     */
    def HttpSession getSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }
	
}
