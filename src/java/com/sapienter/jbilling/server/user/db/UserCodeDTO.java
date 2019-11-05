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
import java.util.Set;


/**
 * A user code links a unique identifier to a user. This identifier can then be used to
 * link the user to other objects in jBilling.
 *
 */
@Entity
@TableGenerator(
        name="user_code_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="user_code",
        allocationSize = 10
        )

@Table(name = "user_code")
@NamedQueries({
        @NamedQuery(name = "UserCodeDTO.findForUser",
                query = "from UserCodeDTO u where u.user.id = :user_id "),
        @NamedQuery(name = "UserCodeDTO.findActiveForUser",
                query = "from UserCodeDTO u where u.user.id = :user_id" +
                        " and (u.validTo is null or u.validTo > :a_date)" +
                        " and (u.validFrom is null or u.validFrom <= :a_date)"),
        @NamedQuery(name = "UserCodeDTO.findActiveForPartner",
                query = "from UserCodeDTO u where u.user.id in (:user_ids)" +
                        " and (u.validTo is null or u.validTo > :a_date)" +
                        " and (u.validFrom is null or u.validFrom <= :a_date)"),
        @NamedQuery(name = "UserCodeDTO.findLinkedIdentifiers",
                query = "select u.identifier from UserCodeDTO u join u.userCodeLinks l " +
                        "where l.objectId = :object_id and l.objectType = :object_type"),
        @NamedQuery(name = "UserCodeDTO.findForIdentifier",
                query = "from UserCodeDTO u where u.identifier = :identifier and u.user.deleted=0 and u.user.company.id = :companyId")


})
public class UserCodeDTO implements Serializable {

    private int id;
    private UserDTO user;
    /** unique identifier to identify this user */
    private String identifier;
    /** A reference that might be used in an external system */
    private String externalReference;
    /** What type of program does the translationId relate to */
    private String type;
    /** Description of the programType */
    private String typeDescription;
    private Date validFrom;
    private Date validTo;

    private Set<UserCodeLinkDTO> userCodeLinks;

    public UserCodeDTO() {
    }

    public UserCodeDTO(int id) {
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "user_code_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getUser() {
        return user;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "userCode")
    public Set<UserCodeLinkDTO> getUserCodeLinks() {
        return userCodeLinks;
    }

    public void setUserCodeLinks(Set<UserCodeLinkDTO> userCodeLinks) {
        this.userCodeLinks = userCodeLinks;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Column(name = "identifier", nullable = false)
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Column(name = "external_ref")
    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String translationId) {
        this.externalReference = translationId;
    }

    @Column(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String programType) {
        this.type = programType;
    }

    @Column(name = "type_desc")
    public String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(String programDesc) {
        this.typeDescription = programDesc;
    }

    @Column(name = "valid_from")
    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    @Column(name = "valid_to")
    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    @Override
    public String toString() {
        return "UserCodeDTO{" +
                "id=" + id +
                ", user=" + (user == null ? "null" : user.getUserName()) +
                ", identifier='" + identifier + '\'' +
                ", externalReference='" + externalReference + '\'' +
                ", type='" + type + '\'' +
                ", typeDescription='" + typeDescription + '\'' +
                ", validFrom=" + validFrom +
                ", validTo=" + validTo +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserCodeDTO that = (UserCodeDTO) o;

        if (!identifier.equals(that.identifier)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    public boolean hasExpired() {
        return validTo != null && validTo.before(new Date());
    }
}
