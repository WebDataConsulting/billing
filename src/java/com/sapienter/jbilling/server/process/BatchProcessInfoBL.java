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

package com.sapienter.jbilling.server.process;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.process.db.BatchProcessInfoDAS;
import com.sapienter.jbilling.server.process.db.BatchProcessInfoDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;

import java.util.Arrays;
import java.util.List;

public class BatchProcessInfoBL  extends ResultList {
    private BatchProcessInfoDAS processInfoDas = null;
    private BillingProcessDAS billingProcessDas = null;
    private BillingProcessDTO billingProcess = null;
    private BatchProcessInfoDTO processInfo = null;
    
    private static final FormatLogger LOG = new FormatLogger(BillingProcessRunBL.class);
    
    public BatchProcessInfoBL(Integer processInfoId) {
        init();
        set(processInfoId);
    }
    
    public BatchProcessInfoBL() {
        init();
    }
    
    private void init() {
       billingProcessDas = new BillingProcessDAS();
       processInfoDas = new BatchProcessInfoDAS();
    }

    public BatchProcessInfoDTO getEntity() {
        return processInfo;
    }
    
    public void set(Integer id) {
        processInfo = processInfoDas.find(id);
    }
    
    public BatchProcessInfoDTO create(Integer billingProcessId, Integer jobExecutionId,
            Integer totalFailedUsers, Integer totalSuccessfulUsers) {
    	billingProcess = billingProcessDas.find(billingProcessId);
    	
    	processInfo = processInfoDas.create(billingProcess, jobExecutionId, totalFailedUsers, totalSuccessfulUsers);
    	return processInfo;
    }

    public List<BatchProcessInfoDTO> findByBillingProcessId (Integer billingProcess) {
    	List<BatchProcessInfoDTO> list = processInfoDas.getEntitiesByBillingProcessId(billingProcess);
    	if(list.size()>0) {
    		return list;
    	}
    	BatchProcessInfoDTO dto = new BatchProcessInfoDTO();
    	dto.setTotalFailedUsers(0);
    	dto.setTotalSuccessfulUsers(0);
    	//dto.setBillingProcess(billingProcessDas.find(billingProcess));
    	dto.setJobExecutionId(0);
    	
    	return Arrays.asList(dto);
    }
}
