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

package com.sapienter.jbilling.server.user.partner.db;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Query;

import java.util.List;

public class PartnerDAS extends AbstractDAS<PartnerDTO> {

    private static final FormatLogger LOG = new FormatLogger(PartnerDAS.class);

    private static final String FIND_CHILD_LIST_SQL =
            "SELECT u.id " +
                    "FROM UserDTO u " +
                    "WHERE u.deleted=0 and u.partner.parent.id = :parentID";
    private static final String FIND_PARTNERS_BY_COMPANY =
            "SELECT u.partner " +
                    "FROM UserDTO u " +
                    "WHERE u.deleted=0 " +
                    "and u.company.id = :companyID";

    public List<Integer> findChildList(Integer parentID) {
        Query query = getSession().createQuery(FIND_CHILD_LIST_SQL);
        query.setParameter("parentID", parentID);
        return query.list();
    }

    public List<PartnerDTO> findPartnersByCompany(Integer entityID) {
        Query query = getSession().createQuery(FIND_PARTNERS_BY_COMPANY)
                .setParameter("companyID", entityID);
        return query.list();
    }

}
