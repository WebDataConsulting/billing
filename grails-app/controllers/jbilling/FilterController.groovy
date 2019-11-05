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
import grails.transaction.Transactional

/**
 * FilterController
 *
 * @author Brian Cowdery
 * @since  03-12-2010
 */
@Secured(["isAuthenticated()"])
class FilterController {
	static scope = "prototype"
    def filterService

    /**
     * Add a hidden filter to the filter pane.
     */
    def add () {
        def filters = filterService.showFilter(params.name)
        render template: "/layouts/includes/filters", model: [ filters: filters ]
    }

    /**
     * Remove a filter from the filter pane.
     */
    def remove () {
        def filters = filterService.removeFilter(params.name)
        render template: "/layouts/includes/filters", model: [ filters: filters ]
    }

    /**
     * Load a saved filter set and replace the current filters in the filter pane.
     */
    def load () {
        def filters = filterService.loadFilters(params.int("id"))
        render template: "/layouts/includes/filters", model: [ filters: filters ]
    }



    /**
     * Render the filter pane
     */
    def filters () {
        def filters = filterService.getCurrentFilters()
        render template: "/layouts/includes/filters", model: [ filters: filters ]
    }

    /**
     * Render a list of filter sets to be edited (from the save dialog)
     */
    def filtersets () {
        def filters = filterService.getCurrentFilters()
        def filtersets = FilterSet.findAllByUserId(session['user_id'])

        render template: "filtersets", model: [ filtersets: filtersets, filters: filters ]
    }

    @Transactional(readOnly = false)
    def edit () {
        def filters = filterService.getCurrentFilters()
        def filterset = FilterSet.get(params.int('id'))

        render template: "edit", model: [ selected: filterset, filters: filters ]
    }

    @Transactional(readOnly = false)
    def save () {
        def filterset = params.id ? FilterSet.get(params.int('id')) : new FilterSet(params)
        filterset.userId = session['user_id']

        def filters = filterService.getCurrentFilters()
        filterset.filters?.removeAll(filters);

        filters.each {
            filterset.addToFilters(new Filter(it))
        }

        filterset.save(flush: true)

        def filtersets = FilterSet.findAllByUserId(session['user_id'])
        render template: "filtersets", model: [ filtersets: filtersets, selected: filterset  ]
    }

    @Transactional(readOnly = false)
    def delete () {
        FilterSet.get(params.int('id'))?.delete(flush: true)

        log.debug("deleted filter set ${params.id}")

        def filtersets = FilterSet.findAllByUserId(session['user_id'])
        render template: "filtersets", model: [ filtersets: filtersets ]
    }
}
