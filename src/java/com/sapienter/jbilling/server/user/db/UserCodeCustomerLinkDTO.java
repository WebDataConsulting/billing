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
package com.sapienter.jbilling.server.user.db;


import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Represents a link of a UserCodeDTO to Customer in jBilling.
 */
@Entity
@DiscriminatorValue("CUSTOMER")
public class UserCodeCustomerLinkDTO extends UserCodeLinkDTO {

    private CustomerDTO customer;


    public UserCodeCustomerLinkDTO() {

    }

    public UserCodeCustomerLinkDTO(UserCodeDTO userCode, CustomerDTO customer) {
        super(userCode);
        this.customer = customer;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_id")
    public CustomerDTO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDTO customer) {
        this.customer = customer;
    }

}
