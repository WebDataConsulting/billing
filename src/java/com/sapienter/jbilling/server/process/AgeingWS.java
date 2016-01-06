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

import java.io.Serializable;
import javax.validation.constraints.NotNull;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;
import org.hibernate.validator.constraints.NotEmpty;


/**
 * AgeingWS
 * @author Vikas Bodani
 */
public class AgeingWS implements WSSecured, Serializable {
	
	@NotNull(message="validation.error.notnull", groups = {UpdateValidationGroup.class})
	private Integer statusId = null;
    @NotEmpty(message="validation.error.notnull")
    private String statusStr = null;
    @NotNull(message="validation.error.notnull")
    private String welcomeMessage = null;
    @NotNull(message="validation.error.notnull")
    private String failedLoginMessage = null;
    private Boolean inUse = null;
    private Boolean suspended;
    private Boolean paymentRetry;
    private Boolean sendNotification;
    @NotNull(message="validation.error.notnull")
    private Integer days;
    private Integer entityId;
    
    //default constructor
    public AgeingWS(){}
    
	public Integer getStatusId() {
		return statusId;
	}
	public void setStatusId(Integer statusId) {
		this.statusId = statusId;
	}
	public String getStatusStr() {
		return statusStr;
	}
	public void setStatusStr(String statusStr) {
		this.statusStr = statusStr;
	}
	public String getWelcomeMessage() {
		return welcomeMessage;
	}
	public void setWelcomeMessage(String welcomeMessage) {
		this.welcomeMessage = welcomeMessage;
	}
	public String getFailedLoginMessage() {
		return failedLoginMessage;
	}
	public void setFailedLoginMessage(String failedLoginMessage) {
		this.failedLoginMessage = failedLoginMessage;
	}

    public Boolean getInUse() {
        return inUse;
    }

    public void setInUse(Boolean inUse) {
        this.inUse = inUse;
    }

    public Boolean getSuspended() {
        return suspended;
    }

    public void setSuspended(Boolean suspended) {
        this.suspended = suspended;
    }

    public Boolean getPaymentRetry() {
        return paymentRetry;
    }

    public void setPaymentRetry(Boolean paymentRetry) {
        this.paymentRetry = paymentRetry;
    }

    public Boolean getSendNotification() {
        return sendNotification;
    }

    public void setSendNotification(Boolean sendNotification) {
        this.sendNotification = sendNotification;
    }

    public Integer getDays() {
		return days;
	}
	public void setDays(Integer days) {
		this.days = days;
	}
	
	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	public Integer getOwningEntityId() {
        return getEntityId();
    }
    /**
     * Unsupported, web-service security enforced using {@link #getOwningEntityId()}
     * @return null
     */
    public Integer getOwningUserId() {
        return null;
    }

	
	public String toString() {
		return "AgeingWS [statusId=" + statusId + ", statusStr=" + statusStr
				+ ", welcomeMessage=" + welcomeMessage
				+ ", failedLoginMessage=" + failedLoginMessage + ", suspended="
				+ suspended + ", days=" + days + "]";
	}

}
