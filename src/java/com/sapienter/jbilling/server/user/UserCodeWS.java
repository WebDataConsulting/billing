/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.api.validation.CreateValidationGroup;
import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

public class UserCodeWS implements WSSecured, Serializable {

	@Min(value = 1, message = "validation.error.min,1", groups = UpdateValidationGroup.class)
    @Max(value = 0, message = "validation.error.max,0", groups = CreateValidationGroup.class)
    private int id;
    @Min(value = 1, message = "validation.error.min,1")
    private int userId;
    @NotNull(message="validation.error.notnull")
    @Size(min = 0, max = 55, message = "validation.error.size,0,55")
    private String identifier = "";
    @Size(min = 0, max = 50, message = "validation.error.size,0,50")
    private String externalReference = "";
    @Size(min = 0, max = 50, message = "validation.error.size,0,50")
    private String type = "";
    @Size(min = 0, max = 250, message = "validation.error.size,0,250")
    private String typeDescription = "";
    @NotNull(message="validation.error.notnull")
    private Date validFrom;
    private Date validTo;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Integer getOwningEntityId() {
        return null;
    }

    public Integer getOwningUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "UserCodeWS{" +
                "id=" + id +
                ", userId=" + userId +
                ", identifier='" + identifier + '\'' +
                ", externalReference='" + externalReference + '\'' +
                ", type='" + type + '\'' +
                ", typeDescription='" + typeDescription + '\'' +
                ", validFrom=" + validFrom +
                ", validTo=" + validTo +
                '}';
    }
}
