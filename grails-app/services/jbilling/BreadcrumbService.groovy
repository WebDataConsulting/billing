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

import org.hibernate.Query
import org.springframework.beans.factory.InitializingBean
import org.springframework.web.context.request.RequestContextHolder

import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpSession

/**
 * BreadcrumbService
 *
 * @author Brian Cowdery
 * @since  14-12-2010
 */
class BreadcrumbService implements InitializingBean, Serializable {

    public static final String SESSION_BREADCRUMBS = "breadcrumbs"
    public static final Integer MAX_ITEMS = 7
    public static final Integer MAX_IN_STORE = MAX_ITEMS + 5

    static scope = "session"

    def void afterPropertiesSet() {
        load()
    }

    def void load() {
        if (session['user_id'])
            session[SESSION_BREADCRUMBS] = getBreadcrumbs()
    }

    /**
     * Returns a list of recorded breadcrumbs for the currently logged in user.
     *
     * @return list of recorded breadcrumbs.
     */
    def Object getBreadcrumbs(int limit = MAX_ITEMS) {
        def userId = session["user_id"]
        return Breadcrumb.withSession { session ->
            Query query = session.createQuery("from Breadcrumb b where b.userId = :userId order by b.id desc")
            query.setInteger("userId", userId)
            query.setReadOnly(true) //do not track entity changes
            query.setMaxResults(limit)
            query.list()
        }
    }

    /**
     * It will delete breadcrumbs that belong to some user and have
     * less then or equal id to a given parameter. This method is used
     * to delete extra breadcrumbs that are no longer needed in database
     *
     * @param userId - the user id to which the breadcrumb belongs
     * @param firstInvalidId - the first invalid id
     * @return the number of deleted breadcrumbs
     */
    def int deleteExtra(Integer userId, Long firstInvalidId) {
        return Breadcrumb.withSession { session ->
            Query query = session.createQuery("delete Breadcrumb b where b.userId = :userId and b.id <= :id")
            query.setInteger("userId", userId)
            query.setLong("id", firstInvalidId)
            query.setReadOnly(true) //do not track entity changes
            query.executeUpdate();
        }
    }

    /**
     * Returns the last recorded breadcrumb for the currently logged in user.
     *
     * @return last recorded breadcrumb.
     */
    def Object getLastBreadcrumb() {
        return Breadcrumb.findByUserId(session['user_id'], [sort:'id', order:'desc'])
    }

    /**
     * Add a new breadcrumb to the breadcrumb list for the currently logged in user and
     * update the session list.
     *
     * The resulting breadcrumb link is generated using the 'g:link' grails tag. The same
     * parameter requirements for g:link apply here as well. A breadcrumb MUST have a controller,
     * but action and ID are optional. the name parameter is used to control the translated breadcrumb
     * message and is optional.
     *
     * @param controller breadcrumb controller (required)
     * @param action breadcrumb action, may be null
     * @param name breadcrumb message key name, may be null
     * @param objectId breadcrumb entity id, may be null.
     */
    def void addBreadcrumb(String controller, String action, String name, Integer objectId) {
		name= StringUtils.abbreviate(name, 255);
        addBreadcrumb(new Breadcrumb(controller: controller, action: action, name: name, objectId: objectId))
    }

    def void addBreadcrumb(String controller, String action, String name, Integer objectId, String description) {
		name= StringUtils.abbreviate(name, 255);
		description= StringUtils.abbreviate(name, 255);
        addBreadcrumb(new Breadcrumb(controller: controller, action: action, name: name, objectId: objectId, description: description))
    }

    /**
     * Add a new breadcrumb to the recent breadcrumb list for the currently logged in user and
     * update the session list.
     *
     * @param crumb breadcrumb to add
     */
    def void addBreadcrumb(Breadcrumb crumb) {
        def crumbs = getBreadcrumbs(MAX_IN_STORE)
        def lastItem = !crumbs.isEmpty() ? crumbs.getAt(0) : null

		// truncate string if its more than 255 words
		crumb.description = StringUtils.left(crumb.description, 255)
		
        // add breadcrumb only if it is different from the last crumb added
        try {
            if (!lastItem || !lastItem.equals(crumb)) {
                def userId = session['user_id']
                crumb.userId = userId
                crumb = crumb.save()

                crumbs.add(0, crumb)//ad as first, newest

                if (crumbs.size() > MAX_ITEMS) {
                    def remove = crumbs.subList(MAX_ITEMS, crumbs.size())
                    if (remove.size() >= (MAX_IN_STORE - MAX_ITEMS)) {
                        //we have reached the max breadcrumbs that
                        //we want to store in database per user
                        //so we will delete the extra elements
                        def firstInvalidId = remove.getAt(0).id
                        def deleteCount = deleteExtra(userId, firstInvalidId)
                        log.debug("Deleted: ${deleteCount} breadcrumbs for user id: ${userId}");
                    }
                    //removes the elements from original list as well
                    remove.clear()
                }

                session[SESSION_BREADCRUMBS] = crumbs
            }

        } catch (Throwable t) {
            log.error("Exception caught adding breadcrumb", t)
            session.error = 'breadcrumb.failed'
        }
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
