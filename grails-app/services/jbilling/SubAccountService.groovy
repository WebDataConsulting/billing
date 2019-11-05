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

import com.sapienter.jbilling.server.customer.CustomerBL
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.db.CustomerDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpSession

class SubAccountService implements Serializable {

    public static final String SESSION_DESCENDANT_IDS = "customer_desendant_ids"

    static transactional = true

    /**
     * Returns a list of userIds of the subAccounts for the current logged-in user.
     * This list also includes the logged-in userId.
     */
    public List<Integer> getSubAccountUserIds() {
        CustomerDTO customer = CustomerDTO.findByBaseUser(UserDTO.get(session['user_id']))

        List<Integer> descendants = new CustomerBL().getDescendants(customer);
        descendants.add(session['user_id'] as Integer);

        return descendants;
    }

    /**
     * Adds the userId passed as parameter to the subAccountIds for the current logged-in user
     * @param user
     * @return
     */
    def addSubAccountUserId(UserWS user) {
        def CustomerDTO customer = CustomerDTO.findByBaseUser(UserDTO.get(session['user_id']))
        def descendantsId = getSubAccountUserIds()
        if (descendantsId.contains(user.parentId)) {
            //saved in session
            descendantsId << user.userId
        }
    }

    /**
     * Removes the userId passed as parameter to the subAccountIds for the current logged-in user
     * @param user
     * @return
     */
    def removeSubAccountUserId(int userId) {
        def CustomerDTO customer = CustomerDTO.findByBaseUser(UserDTO.get(session['user_id']))
        def descendantsId = getSubAccountUserIds()
        if (descendantsId.contains(userId)) {
            //saved in session
            descendantsId - userId
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
