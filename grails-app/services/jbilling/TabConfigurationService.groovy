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

import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpSession

/**
 * TabConfigurationService.
 * Used to access the Tab configuration for the current user.
 *
 * @author Gerhard Maree
 * @since  25-03-2013
 */
class TabConfigurationService {

    public static final String SESSION_USER_TABS = "user_tabs"

    /**
     * Load the tab configuration for the currently logged in user.
     */
    def void load() {
        if (httpSession['user_id'] && !httpSession[SESSION_USER_TABS])
            httpSession[SESSION_USER_TABS] = getTabConfiguration()
    }

    /**
     * Returns the tab configuration for the currently logged in user.
     *
     * @return TabConfiguration.
     */
    def TabConfiguration getTabConfiguration() {
        def userId = httpSession["user_id"]
		def list =  TabConfiguration.withCriteria() {
            eq("userId", userId)
        }
        TabConfiguration tabConfiguration = list.isEmpty() ? null : list.get(0)
        return tabConfiguration ? checkForNewTabs(tabConfiguration): createDefaultConfiguration()
    }

    /**
     * Check if tabs have been added to the system.
     * @param tabConfiguration
     * @return
     */
    def TabConfiguration checkForNewTabs(TabConfiguration tabConfiguration) {
        def tabs = Tab.list([sort:"id", cache:true])
        if (tabs.size() == tabConfiguration.tabConfigurationTabs.size()) return tabConfiguration

        def tabsToAdd = tabs - tabConfiguration.tabConfigurationTabs?.tab
        def idx = tabConfiguration.tabConfigurationTabs.size()
        tabsToAdd.each {
            tabConfiguration.addToTabConfigurationTabs(new TabConfigurationTab([displayOrder: idx++, visible:false, tab: it]))
        }
        return tabConfiguration
    }

    /**
     * The default configuration is all tabs ordered by id.
     * @return
     */
    def TabConfiguration createDefaultConfiguration() {
        TabConfiguration tabConfiguration = new TabConfiguration([userId: httpSession["user_id"]])

        def tabs = Tab.list([sort:"defaultOrder", cache:true])

        def idx=0
        tabs.each {
            tabConfiguration.addToTabConfigurationTabs(new TabConfigurationTab([displayOrder: idx++, visible:true, tab: it]))
        }
        return tabConfiguration
    }

    /**
     * Returns the HTTP session
     *
     * @return http session
     */
    def HttpSession getHttpSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }

}
