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


import com.sapienter.jbilling.server.user.IUserSessionBean
import com.sapienter.jbilling.server.util.Context
import grails.plugin.springsecurity.SpringSecurityUtils

class LogoutController {
	static scope = "singleton"
	/**
	 * Index action. Redirects to the Spring security logout uri.
	 */
	def filterService
	def index () {
        //get bean to update event audit log during log out
        IUserSessionBean iUserSessionBean = (IUserSessionBean) Context.getBean(Context.Name.USER_SESSION)
        iUserSessionBean.logout(session['user_id'])

		def filters = filterService.getCurrentFilters();
        filters?.each{
            if (!it.name.isEmpty())
                it.visible = false
				it.clear()
        }
		redirect uri: SpringSecurityUtils.securityConfig.logout.filterProcessesUrl // '/j_spring_security_logout'
	}
}
