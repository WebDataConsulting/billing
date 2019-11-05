/*
 * jBilling - The Enterprise Open Source Billing System
 * Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde
 * 
 * This file is part of jbilling.
 * 
 * jbilling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jbilling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
You may download the latest source from webdataconsulting.github.io.

 */
package jbilling



import grails.test.mixin.*

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(TabConfigController)
@Mock([Tab, TabConfiguration, TabConfigurationTab])
class TabConfigControllerTests {

    Tab tab1,tab2,tab3

    public void setUp() {
        tab1 = new Tab([controllerName: "controller1"]).save(validate: false)
        tab2 = new Tab([controllerName: "controller2"]).save(validate: false)
        tab3 = new Tab([controllerName: "controller3"]).save(validate: false)

        TabConfiguration tabConfig = new TabConfiguration([userId:1, tabConfigurationTabs: new TreeSet()])
        tabConfig.tabConfigurationTabs.add(new TabConfigurationTab(tab: tab1, visible: true, displayOrder: 1, tabConfiguration: tabConfig))
        tabConfig.tabConfigurationTabs.add(new TabConfigurationTab(tab: tab2, visible: true, displayOrder: 2, tabConfiguration: tabConfig))
        tabConfig.tabConfigurationTabs.add(new TabConfigurationTab(tab: tab3, visible: true, displayOrder: 3, tabConfiguration: tabConfig))
        tabConfig.save(flush: true)
        session[TabConfigurationService.SESSION_USER_TABS] = tabConfig
    }

    void test01SaveSwapPosition() {
        //swap the first and second item
        params['visible-order'] = ','+tab2.id+','+tab1.id+','+tab3.id
        params['hidden-order'] = ''
//        params['tab-vis-'+tab1.id] = "on"
//        params['tab-pos-'+tab1.id] = "2"
//
//        params['tab-vis-'+tab2.id] = "on"
//        params['tab-pos-'+tab2.id] = "1"
//
//        params['tab-vis-'+tab3.id] = "on"
//        params['tab-pos-'+tab3.id] = "3"

        controller.save()

        TabConfiguration tabConfig = session[TabConfigurationService.SESSION_USER_TABS]
        assertEquals(3, tabConfig.tabConfigurationTabs.size())
        //make sure we don't have double entries in the DB
        assertEquals(3, TabConfiguration.get(1).tabConfigurationTabs.size())

        def tabsFound = [] as Set
        tabConfig.tabConfigurationTabs.each {
            if(it.tab.id ==tab1.id) {
                tabsFound += 1
                assertEquals(1, it.displayOrder)
                assertTrue(it.visible)
            } else if(it.tab.id ==tab2.id) {
                tabsFound += 2
                assertEquals(0, it.displayOrder)
                assertTrue(it.visible)
            } else if(it.tab.id ==tab3.id) {
                tabsFound += 3
                assertEquals(2, it.displayOrder)
                assertTrue(it.visible)
            }
        }
        assertEquals("Not all 3 tabs are saved as part of the TabConfiguration ["+tabsFound+"] "+tabConfig, [1,2,3] as Set, tabsFound)

    }

    void test02SaveMakeInvisible() {
        //make the 3rd tab not visible
        controller.params.clear()
        params['visible-order'] = ','+tab2.id+','+tab1.id
        params['hidden-order'] = ','+tab3.id

//        controller.params['tab-vis-'+tab1.id] = "on"
//        controller.params['tab-pos-'+tab1.id] = "2"
//
//        controller.params['tab-vis-'+tab2.id] = "on"
//        controller.params['tab-pos-'+tab2.id] = "1"
//
//        controller.params['tab-pos-'+tab3.id] = "3"

        controller.save()

        TabConfiguration tabConfig = session[TabConfigurationService.SESSION_USER_TABS]
        assertEquals(3, tabConfig.tabConfigurationTabs.size())

        def thirdTabFound = false
        tabConfig.tabConfigurationTabs.each {
            if(it.tab.id ==tab3.id) {
                thirdTabFound = true
                assertEquals(2, it.displayOrder)
                assertFalse(it.visible)
            }
        }
        assertTrue("Third tab not found "+tabConfig, thirdTabFound)
    }

    /**
     * User doesn't have any configuration saved. Test first time save
     */
    void test03SaveDefault() {
        //def sessionParams = [:]
        session[TabConfigurationService.SESSION_USER_TABS] = null
        session['user_id'] = 2

        def service = new TabConfigurationService();
        TabConfigurationService.metaClass.getHttpSession = {
            return session
        }
        service.load()

        assertNotNull(session[TabConfigurationService.SESSION_USER_TABS])
        assertNull(session[TabConfigurationService.SESSION_USER_TABS].id)
        assertEquals(3, session[TabConfigurationService.SESSION_USER_TABS].tabConfigurationTabs.size())

        //swap the first and second item
        params['visible-order'] = ','+tab2.id+','+tab1.id+','+tab3.id
        params['hidden-order'] = ''

        controller.save()

        TabConfiguration tabConfig = session[TabConfigurationService.SESSION_USER_TABS]
        assertEquals(3, tabConfig.tabConfigurationTabs.size())
        //make sure we don't have double entries in the DB
        assertEquals(3, TabConfiguration.get(1).tabConfigurationTabs.size())

        def tabsFound = [] as Set
        tabConfig.tabConfigurationTabs.each {
            if(it.tab.id ==tab1.id) {
                tabsFound += 1
                assertEquals(1, it.displayOrder)
                assertTrue(it.visible)
            } else if(it.tab.id ==tab2.id) {
                tabsFound += 2
                assertEquals(0, it.displayOrder)
                assertTrue(it.visible)
            } else if(it.tab.id ==tab3.id) {
                tabsFound += 3
                assertEquals(2, it.displayOrder)
                assertTrue(it.visible)
            }
        }
        assertEquals("Not all 3 tabs are saved as part of the TabConfiguration ["+tabsFound+"] "+tabConfig, [1,2,3] as Set, tabsFound)
    }
}
