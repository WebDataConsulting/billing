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
package com.sapienter.jbilling.server.item;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Version of AssetStatusDTO which is safe for external communication.
 *
 * @author Gerhard
 * @since 15/04/13
 * @see com.sapienter.jbilling.server.item.db.AssetStatusDTO
 */
public class AssetStatusDTOEx implements Serializable {

    private int id;
    @Size(min=1,max=50, message="validation.error.size,1,50")
    private String description;
    private int isDefault;
    private int isAvailable;
    private int isOrderSaved;
    private int isInternal;

    public AssetStatusDTOEx() {

    }

    public AssetStatusDTOEx(int id, String description, int aDefault, int available, int orderSaved, int internal) {
        this.id = id;
        this.description = description;
        this.isDefault = aDefault;
        this.isAvailable = available;
        this.isOrderSaved = orderSaved;
        this.isInternal = internal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIsInternal() {
        return isInternal;
    }

    public void setIsInternal(int internal) {
        isInternal = internal;
    }

    public int getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(int aDefault) {
        isDefault = aDefault;
    }

    public int getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(int available) {
        isAvailable = available;
    }

    public int getIsOrderSaved() {
        return isOrderSaved;
    }

    public void setIsOrderSaved(int orderSaved) {
        isOrderSaved = orderSaved;
    }

}