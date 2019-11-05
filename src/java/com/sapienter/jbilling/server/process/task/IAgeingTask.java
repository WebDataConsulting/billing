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

package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import org.hibernate.ScrollableResults;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * IAgeingTask
 *
 * @author Brian Cowdery
 * @since 28/04/11
 */
public interface IAgeingTask {

    /**
     * Retrieve all the users that are candidates for the ageing process
     *
     * @param entityId company id
     * @param ageingDate date when the ageing was initiated
     * @return users cursor-candidates for ageing
     */
    public ScrollableResults findUsersToAge(Integer entityId, Date ageingDate);

    /**
     *  Review the user and evaluate if the user has to be aged
     *
     * @param entityId company id
     * @param steps ageing steps defined per company
     * @param userId id if the user to be reviews for ageing
     * @param today today's date
     * @param executorId executor id
     */
    public List<InvoiceDTO> reviewUser(Integer entityId, Set<AgeingEntityStepDTO> steps, Integer userId, Date today, Integer executorId);

    /**
     * Age the user by moving the user one step forward in the ageing process
     *
     * @param steps ageing steps
     * @param user user to age
     * @param overdueInvoice ovedue invoice
     * @param today today's date
     * @param executorId executor id
     * @return the resulting ageing step for the user after ageing
     */
    public AgeingEntityStepDTO ageUser(Set<AgeingEntityStepDTO> steps, UserDTO user, InvoiceDTO overdueInvoice, Date today, Integer executorId);

    /**
     * Removes a user from the ageing process (makes them active).
     *
     * @param user user to make active
     * @param excludedInvoiceId invoice id to ignore when determining if the user CAN be made active
     * @param executorId executor id
     */
    public void removeUser(UserDTO user, Integer excludedInvoiceId, Integer executorId);

    /**
     * Returns true if the user requires ageing.
     *
     * @param user user being reviewed
     * @param overdueInvoice earliest overdue invoice
     * @param today today's date
     * @return true if user requires ageing, false if not
     */
    public boolean isAgeingRequired(UserDTO user, InvoiceDTO overdueInvoice, Integer stepDays, Date today);

    /**
     * Sets the users status.
     *
     * @param user user
     * @param status status to set
     * @param today today's date
     * @param executorId executor id
     */
    public boolean setUserStatus(UserDTO user, UserStatusDTO status, Date today, Integer executorId);

}
