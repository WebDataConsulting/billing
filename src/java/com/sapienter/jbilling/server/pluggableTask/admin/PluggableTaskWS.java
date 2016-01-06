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

package com.sapienter.jbilling.server.pluggableTask.admin;

import com.sapienter.jbilling.server.security.WSSecured;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


public class PluggableTaskWS implements java.io.Serializable, WSSecured {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Integer id;

    @NotNull(message="validation.error.notnull")
    @Min(value = 1, message = "validation.error.min,1")
    private Integer processingOrder;

    @Size(min=0, max = 1000, message = "validation.error.size,1,1000")
    private String notes;

    @NotNull(message="validation.error.notnull")
    private Integer typeId;
    private Map<String, String> parameters = new HashMap<String, String>();
    private int versionNumber;
    private Integer owningId;
    
    public PluggableTaskWS() {
    }
    
	public void setNotes(String notes) {
		this.notes = notes;
	}


	public String getNotes() {
		return notes;
	}


    public Integer getId() {
        return id;
    }


    public void setId(Integer id) {
        this.id = id;
    }


    public Integer getProcessingOrder() {
        return processingOrder;
    }


    public void setProcessingOrder(Integer processingOrder) {
        this.processingOrder = processingOrder;
    }


    public Integer getTypeId() {
        return typeId;
    }


    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }


    public Map<String, String> getParameters() {
        return parameters;
    }


    public void setParameters(Hashtable<String, String> parameters) {
        this.parameters = parameters;
    }
    
    public int getVersionNumber() {
        return versionNumber;
    }
    
    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluggableTaskWS{");
        sb.append("id=").append(id);
        sb.append(", processingOrder=").append(processingOrder);
        sb.append(", notes='").append(notes).append('\'');
        sb.append(", typeId=").append(typeId);
        sb.append(", parameters=").append(parameters);
        sb.append(", versionNumber=").append(versionNumber);
        sb.append(", owningId=").append(owningId);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public Integer getOwningEntityId() {
        return owningId;
    }
    public void setOwningEntityId(Integer owningId){
    	this.owningId = owningId;
    }
    
    @Override
    public Integer getOwningUserId() {
        return null;
    }
}
