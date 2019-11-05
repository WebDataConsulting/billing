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

/*
 * Created on Jan 15, 2005
 *
 */
package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.util.audit.EventLogger;

/**
 * @author Emil
 */
public interface UserSQL {

    String findActiveWithOpenInvoices =
            "SELECT a.id " +
                    "FROM base_user a, customer c " +
                    "WHERE a.status_id = (select id from generic_status " +
                    "    WHERE dtype = 'user_status' AND status_value = 1) " +
                    "AND c.exclude_aging = 0 " +
                    "AND a.deleted = 0 " +
                    "AND a.entity_id = ? " +
                    "AND a.id = c.user_id " +
                    "AND exists (" +
                    "    select 1 " +
                    "      from invoice i" +
                    "     where i.status_id != 26 " + // 26 is paid. It should return upaid and carried
                    "       and i.user_id = a.id " +
                    "       and i.is_review = 0  " +
                    "       and i.deleted = 0 " +
                    "    )";

    String findUserTransitions =
            "SELECT el.id, el.old_str, el.create_datetime, el.old_num, el.foreign_id" +
                    " FROM event_log el" +
                    " WHERE el.module_id = " + EventLogger.MODULE_USER_MAINTENANCE +
                    " AND el.message_id = " + EventLogger.SUBSCRIPTION_STATUS_CHANGE + " AND el.entity_id = ?";

    String findUserTransitionsByIdSuffix =
            " AND el.id > ?";

    String findUserTransitionsByDateSuffix =
            " AND el.create_datetime >= ?";

    String findUserTransitionsUpperDateSuffix =
            " AND el.create_datetime <= ?";

    String findUsedPasswords =
            "SELECT el.old_str" +
                    " FROM event_log el" +
                    " WHERE el.module_id = " + EventLogger.MODULE_USER_MAINTENANCE +
                    " AND el.message_id = " + EventLogger.PASSWORD_CHANGE +
                    " AND el.create_datetime >= ?" +
                    " AND el.foreign_id = ?";

    String lastPasswordChange =
            "SELECT max(create_datetime)" +
                    " FROM event_log el" +
                    " WHERE el.module_id = " + EventLogger.MODULE_USER_MAINTENANCE +
                    " AND el.message_id = " + EventLogger.PASSWORD_CHANGE +
                    " AND el.foreign_id = ?";

    String findInStatus =
            "SELECT id " +
                    "  FROM base_user a " +
                    " WHERE a.status_id = (select id from user_status " +
                    "    WHERE id = ?) " +
                    "   AND a.entity_id = ?" +
                    "   AND a.deleted = 0" +
                    " ORDER BY 1";

    String findNotInStatus =
            "SELECT id " +
                    "  FROM base_user a " +
                    " WHERE a.status_id <> (select id from user_status " +
                    "    WHERE id = ?) " +
                    "   AND a.entity_id = ?" +
                    "   AND a.deleted = 0" +
                    " ORDER BY 1";

    String findByCustomField =
            "SELECT a.id " +
                    "  FROM base_user a, contact c, contact_field cf " +
                    " WHERE c.user_id = a.id " +
                    "    AND c.id = cf.contact_id " +
                    "    AND cf.type_id = ? " +
                    "   AND a.entity_id = ?" +
                    "   AND cf.content = ?" +
                    "   AND a.deleted = 0" +
                    " ORDER BY 1";

    String findByCustomFieldLike =
            "SELECT a.id " +
                    "  FROM base_user a, contact c, contact_field cf " +
                    " WHERE c.user_id = a.id " +
                    "    AND c.id = cf.contact_id " +
                    "    AND cf.type_id = ? " +
                    "   AND a.entity_id = ?" +
                    "   AND cf.content like ?" +
                    "   AND a.deleted = 0" +
                    " ORDER BY 1";


    String findByCreditCard =
            "SELECT a.id " +
                    "  FROM base_user a, user_credit_card_map m, credit_card c " +
                    " WHERE c.cc_number_plain = ? " +
                    "   AND a.id = m.user_id " +
                    "   AND c.id = m.credit_card_id " +
                    "   AND a.entity_id = ?" +
                    "   AND a.deleted = 0" +
                    "   AND c.deleted = 0" +
                    " ORDER BY 1";

    String findByEmail =
            "SELECT a.id " +
                    "  FROM base_user a, user_role_map m, contact c " +
                    " WHERE m.role_id = " + CommonConstants.TYPE_CUSTOMER +
                    "   AND a.id = m.user_id " +
                    "   AND a.id = c.user_id " +
                    "   AND c.email = ?" +
                    "   AND a.deleted = 0" +
                    "   AND c.deleted = 0" +
                    " ORDER BY 1";

    String getEntityId =
            "SELECT entity_id " +
                    "  FROM base_user " +
                    " WHERE id = ?";

    String findOthersByEmail =
            "SELECT a.id " +
                    "  FROM base_user a, contact c " +
                    " WHERE a.id = c.user_id " +
                    "   AND lower(c.email) = ?" +
                    "   AND a.id <> ?" +
                    "   AND a.entity_id = ?" +
                    "   AND a.deleted = 0" +
                    "   AND c.deleted = 0" +
                    " ORDER BY 1";

}
