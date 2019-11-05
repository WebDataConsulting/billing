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
package com.sapienter.jbilling.server.process.db;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;

import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class BatchProcessInfoDAS extends AbstractDAS<BatchProcessInfoDTO> {

    //private static final FormatLogger LOG = new FormatLogger(ProcessRunDAS.class);

	public BatchProcessInfoDTO create(BillingProcessDTO billingProcessDTO, Integer jobExecutionId,
            Integer totalFailedUsers, Integer totalSuccessfulUsers) {
		BatchProcessInfoDTO dto = new BatchProcessInfoDTO(billingProcessDTO,jobExecutionId,
                totalFailedUsers,totalSuccessfulUsers);
  
        dto = save(dto);
        return dto;
    }
  
    public List<BatchProcessInfoDTO> getEntitiesByBillingProcessId(Integer entityId) {
        final String hql =
            "select a " +
            "  from BatchProcessInfoDTO a " +
            " where a.billingProcess.id = :entity " +
            " order by a.id desc ";
       
        Query query = getSession().createQuery(hql);
        query.setParameter("entity", entityId);
        return (List<BatchProcessInfoDTO>) query.list();
    }
}
