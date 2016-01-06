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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.server.user.db.UserDTO;

@Entity
@TableGenerator(
        name = "process_run_GEN", 
        table = "jbilling_seqs", 
        pkColumnName = "name", 
        valueColumnName = "next_id", 
        pkColumnValue = "billing_process_failed_user", 
        allocationSize = 100)
@Table(name = "billing_process_failed_user")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class BillingProcessFailedUserDTO implements java.io.Serializable {

    private int id;
    private BatchProcessInfoDTO batchProcessDTO;
    private UserDTO userDTO;
    private int versionNum;

    public BillingProcessFailedUserDTO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "process_run_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_process_id")
    public BatchProcessInfoDTO getBatchProcess() {
        return this.batchProcessDTO;
    }

    public void setBatchProcess(BatchProcessInfoDTO batchProcessDTO) {
        this.batchProcessDTO = batchProcessDTO;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getUser() {
		return userDTO;
	}

	public void setUser(UserDTO user) {
		this.userDTO = user;
	}

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }
    
    public String listToString(List<Integer> list) {
    	if(list.size()>0) {
    		String ret = list.remove(0).toString();
    		for(Integer item : list) {
    			ret.concat(",");
    			ret.concat(item.toString());
    		}
    	
    		return ret;
    	}
    	return null;
    }

    public String toString() {
        StringBuffer ret = new StringBuffer(" BatchProcessInfoDTO: id: " + id + " batchProcess: " + batchProcessDTO.getId() + " user: " + userDTO.getId());

        return ret.toString();
    }
}
