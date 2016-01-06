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

import grails.plugin.springsecurity.annotation.Secured

@Secured(["isAuthenticated()"])
class TabConfigController {
	static scope = "prototype"

    def index () {
        redirect action: "show"
    }

    def show () {
        def tabConfiguration = session[TabConfigurationService.SESSION_USER_TABS]
        [tabConfigurationTabs: tabConfiguration.tabConfigurationTabs]
    }

    def save () {
        TabConfiguration tabConfiguration = session[TabConfigurationService.SESSION_USER_TABS]
        //we have to refresh the user's tabs if he is not new, otherwise hibernate complains about stale objects
        //if we have previously added or removed items to the TabConfiguration
        boolean isPrevSaved = (tabConfiguration.id != null);
        if(isPrevSaved) tabConfiguration.refresh()
        tabConfiguration.tabConfigurationTabs.clear()
        tabConfiguration.save(flush: true)
        if (isPrevSaved) {
            TabConfigurationTab.where {tabConfiguration == tabConfiguration}.deleteAll()
        }

        def idx = 0
        for (configId in params["visible-order"].tokenize(",")) {
            def tabConfigTab = new TabConfigurationTab(
                    [   tab: Tab.get(configId as Long),
                        visible: true,
                        displayOrder: idx++
                    ]
            )
            tabConfiguration.addToTabConfigurationTabs( tabConfigTab ).save(flush: true)
        }
        for (configId in params["hidden-order"].tokenize(",")) {
            def tabConfigTab = new TabConfigurationTab(
                    [   tab: Tab.get(configId as Long),
                        visible: false,
                        displayOrder: idx++
                    ]
            )
            tabConfiguration.addToTabConfigurationTabs( tabConfigTab ).save(flush: true)
        }
        tabConfiguration.save(flush: true)
        session[TabConfigurationService.SESSION_USER_TABS] = tabConfiguration
        redirect action: "show"
    }
}
