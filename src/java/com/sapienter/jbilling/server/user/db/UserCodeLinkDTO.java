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
 * Represents a link of a UserCodeDTO to another object in jBilling.
 */
@Entity
@TableGenerator(
        name="user_code_link_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="user_code_link",
        allocationSize = 10
        )

@Table(name = "user_code_link")
@NamedQueries({
        @NamedQuery(name = "UserCodeLinkDTO.countLinkedObjects",
                query = "select count(u.id) from UserCodeLinkDTO u where u.userCode.id = :user_code_id"),
        @NamedQuery(name = "UserCodeLinkDTO.findForUserCodeAndObjectType",
                query = "select u.objectId from UserCodeLinkDTO u where u.userCode.identifier = :user_code and objectType = :object_type"),
        @NamedQuery(name = "UserCodeLinkDTO.findForUserIdAndObjectType",
                query = "select u.objectId from UserCodeLinkDTO u where u.userCode.user.id = :user_id and objectType = :object_type")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "object_type")
public class UserCodeLinkDTO implements Serializable {

    private int id;
    private UserCodeDTO userCode;
    /** type of object linked to */
    private UserCodeObjectType objectType;
    private int objectId;


    public UserCodeLinkDTO() {
    }

    public UserCodeLinkDTO(int id) {
        this.id = id;
    }

    public UserCodeLinkDTO(UserCodeDTO userCode) {
        this.userCode = userCode;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "user_code_link_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_code_id")
    public UserCodeDTO getUserCode() {
        return userCode;
    }

    public void setUserCode(UserCodeDTO userCode) {
        this.userCode = userCode;
    }

    @Column(name="object_type", insertable = false, updatable = false)
    public UserCodeObjectType getObjectType() {
        return objectType;
    }

    public void setObjectType(UserCodeObjectType objectType) {
        this.objectType = objectType;
    }

    @Column(name="object_id", insertable = false, updatable = false)
    public int getObjectId() {
        return objectId;
    }

    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }

    public void touch() {
        getUserCode().getIdentifier();
    }

    @Override
    public String toString() {
        return "UserCodeLinkDTO{" +
                "id=" + id +
                ", userCode=" + userCode +
                ", objectType=" + objectType +
                ", objectId=" + objectId +
                '}';
    }
}
