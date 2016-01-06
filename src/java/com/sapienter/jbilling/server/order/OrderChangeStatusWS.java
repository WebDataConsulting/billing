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

package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Aksenov
 * @since 05.07.13
 */
public class OrderChangeStatusWS implements Serializable {

    private Integer id;
    private Integer order;
    private Integer entityId;
    private int deleted;
    @NotNull(message = "validation.error.notnull")
    private ApplyToOrder applyToOrder;
    @Size(min = 1, message = "validation.error.notnull")
    private List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>(1);

    public OrderChangeStatusWS() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public ApplyToOrder getApplyToOrder() {
        return applyToOrder;
    }

    public void setApplyToOrder(ApplyToOrder applyToOrder) {
        this.applyToOrder = applyToOrder;
    }

    public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    public void setName(String name,Integer languageId) {
        InternationalDescriptionWS description = new InternationalDescriptionWS(languageId, name);
        addDescription(description);
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    public void addDescription(InternationalDescriptionWS description) {
        this.descriptions.add(description);
    }

    public InternationalDescriptionWS getDescription(Integer languageId) {
        for (InternationalDescriptionWS descriptionWS : this.descriptions) {
            if (descriptionWS.getLanguageId().equals(languageId)) {
                return descriptionWS;
            }
        }
        return null;
    }

   
}
