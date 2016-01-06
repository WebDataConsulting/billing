package jbilling

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.web.ControllerUnitTestMixin

/**
 * @author Gerhard
 * @since 28/03/13
 */
@TestFor(TabConfigurationService)
@Mock([Tab, TabConfiguration, TabConfigurationTab])
class TabConfigurationServiceTests {

    Tab tab1,tab2,tab3
    def sessionParams = [:]

    public void setUp() {
        tab1 = new Tab([controllerName: "controller1", messageCode: "controller1"]).save(validate: false, flush: true)
        tab2 = new Tab([controllerName: "controller2", messageCode: "controller2"]).save(validate: false, flush: true)
        tab3 = new Tab([controllerName: "controller3", messageCode: "controller3"]).save(validate: false, flush: true)

        def list = Tab.list()

        list = Tab.list([order: "id", cache:true])

        TabConfigurationService.metaClass.getHttpSession = {
            return sessionParams
        }

        sessionParams["user_id"] = "1"

    }

    /**
     * Check if we can load the default tabs
     */
    void test01LoadDefault() {
        service.load()
        TabConfiguration tabConfig = sessionParams["user_tabs"]

        assertEquals(3, tabConfig.tabConfigurationTabs.size())

        def tabsFound = [] as Set
        tabConfig.tabConfigurationTabs.each {
            if(it.tab.id ==tab1.id) {
                tabsFound += 0
                assertEquals(0, it.displayOrder)
                assertTrue(it.visible)
            } else if(it.tab.id ==tab2.id) {
                tabsFound += 1
                assertEquals(1, it.displayOrder)
                assertTrue(it.visible)
            } else if(it.tab.id ==tab3.id) {
                tabsFound += 2
                assertEquals(2, it.displayOrder)
                assertTrue(it.visible)
            }
        }
        assertEquals("Not all 3 tabs are saved as part of the TabConfiguration ["+tabsFound+"] "+tabConfig, [0,1,2] as Set, tabsFound)
    }

    /**
     * Check if new tabs gets added to user profile automatically
     */
    void test02LoadAddNewTabs() {
        TabConfiguration tabConfig = new TabConfiguration([userId:1])
        tabConfig.addToTabConfigurationTabs(new TabConfigurationTab(tab: tab3, visible: true, displayOrder: 0, tabConfiguration: tabConfig))
        tabConfig.save()

        service.load()
        tabConfig = sessionParams["user_tabs"]

        assertEquals(3, tabConfig.tabConfigurationTabs.size())

        def tabsFound = [] as Set
        tabConfig.tabConfigurationTabs.each {
            if(it.tab.id ==tab1.id) {
                tabsFound += 0
                assertEquals(1, it.displayOrder)
                assertFalse(it.visible)
            } else if(it.tab.id ==tab2.id) {
                tabsFound += 1
                assertEquals(2, it.displayOrder)
                assertFalse(it.visible)
            } else if(it.tab.id ==tab3.id) {
                tabsFound += 2
                assertEquals(0, it.displayOrder)
                assertTrue(it.visible)
            }
        }
        assertEquals("Not all 3 tabs are saved as part of the TabConfiguration ["+tabsFound+"] "+tabConfig, [1,2,0] as Set, tabsFound)
    }

}
