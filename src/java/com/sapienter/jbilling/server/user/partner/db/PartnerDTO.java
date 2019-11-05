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
package com.sapienter.jbilling.server.user.partner.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.PartnerCommissionType;
import com.sapienter.jbilling.server.user.partner.PartnerType;
import com.sapienter.jbilling.server.util.csv.Exportable;

@SuppressWarnings("serial")
@Entity
@TableGenerator(
        name = "partner_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "partner",
        allocationSize = 10
)
@Table(name = "partner")
public class PartnerDTO extends CustomizedEntity implements java.io.Serializable, Exportable {
    private int id;

    private UserDTO baseUserByUserId;
    private BigDecimal totalPayments;
    private BigDecimal totalRefunds;
    private BigDecimal totalPayouts;
    private BigDecimal duePayout;
    private PartnerType type;
    private PartnerDTO parent;
    private Set<PartnerDTO> children = new HashSet<PartnerDTO>(0);
    private List<CommissionDTO> commissions;
    private List<PartnerCommissionExceptionDTO> commissionExceptions;
    private List<PartnerReferralCommissionDTO> referralCommissions;
    private List<PartnerReferralCommissionDTO> referrerCommissions;
    private PartnerCommissionType commissionType;

    private Set<PartnerPayout> partnerPayouts = new HashSet<PartnerPayout>(0);
    private Set<CustomerDTO> customers = new HashSet<CustomerDTO>(0);
    private int versionNum;

    public PartnerDTO() {
        super();
        this.commissions = new ArrayList<CommissionDTO>();
        this.commissionExceptions = new ArrayList<PartnerCommissionExceptionDTO>();
        this.referralCommissions = new ArrayList<PartnerReferralCommissionDTO>();
        this.referrerCommissions = new ArrayList<PartnerReferralCommissionDTO>();
    }

    public PartnerDTO(int id) {
        this();
        this.id = id;
    }

    public PartnerDTO(int id, BigDecimal totalPayments, BigDecimal totalRefunds, BigDecimal totalPayouts) {
        this(id);
        this.totalPayments = totalPayments;
        this.totalRefunds = totalRefunds;
        this.totalPayouts = totalPayouts;
    }

    public PartnerDTO(int id, UserDTO baseUserByUserId, BigDecimal totalPayments, BigDecimal totalRefunds,
                      BigDecimal totalPayouts, BigDecimal duePayout, Set<PartnerPayout> partnerPayouts,
                      Set<CustomerDTO> customers) {
        this(id);
        this.baseUserByUserId = baseUserByUserId;
        this.totalPayments = totalPayments;
        this.totalRefunds = totalRefunds;
        this.totalPayouts = totalPayouts;
        this.duePayout = duePayout;
        this.partnerPayouts = partnerPayouts;
        this.customers = customers;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "partner_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getBaseUser() {
        return this.baseUserByUserId;
    }

    public void setBaseUser(UserDTO baseUserByUserId) {
        this.baseUserByUserId = baseUserByUserId;
    }

    @Column(name = "total_payments", nullable = false, precision = 17, scale = 17)
    public BigDecimal getTotalPayments() {
        return this.totalPayments;
    }

    public void setTotalPayments(BigDecimal totalPayments) {
        this.totalPayments = totalPayments;
    }

    @Column(name = "total_refunds", nullable = false, precision = 17, scale = 17)
    public BigDecimal getTotalRefunds() {
        return this.totalRefunds;
    }

    public void setTotalRefunds(BigDecimal totalRefunds) {
        this.totalRefunds = totalRefunds;
    }

    @Column(name = "total_payouts", nullable = false, precision = 17, scale = 17)
    public BigDecimal getTotalPayouts() {
        return this.totalPayouts;
    }

    public void setTotalPayouts(BigDecimal totalPayouts) {
        this.totalPayouts = totalPayouts;
    }

    @Column(name = "due_payout", precision = 17, scale = 17)
    public BigDecimal getDuePayout() {
        return this.duePayout;
    }

    public void setDuePayout(BigDecimal duePayout) {
        this.duePayout = duePayout;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "partner")
    public Set<PartnerPayout> getPartnerPayouts() {
        return this.partnerPayouts;
    }

    public void setPartnerPayouts(Set<PartnerPayout> partnerPayouts) {
        this.partnerPayouts = partnerPayouts;
    }

    public void addPartnerPayout (PartnerPayout partnerPayout){this.partnerPayouts.add(partnerPayout);}

    public void removePartnerPayout (PartnerPayout partnerPayout){this.partnerPayouts.remove(partnerPayout);}

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "partner")
    public Set<CustomerDTO> getCustomers() {
        return this.customers;
    }

    public void setCustomers(Set<CustomerDTO> customers) {
        this.customers = customers;
    }

    public void addCustomer (CustomerDTO customer){this.customers.add(customer);}

    public void removeCustomer (CustomerDTO customer){this.customers.remove(customer);}

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "partner_meta_field_map",
            joinColumns = @JoinColumn(name = "partner_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public Set<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

    public void addMetaField (MetaFieldValue MetaFieldsList){this.getMetaFieldsList().add(MetaFieldsList);}

    public void removeMetaField (MetaFieldValue MetaFieldsList){this.getMetaFieldsList().remove(MetaFieldsList);}

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    public PartnerType getType () {
        return type;
    }

    public void setType (PartnerType type) {
        this.type = type;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    public PartnerDTO getParent() {
        return this.parent;
    }

    public void setParent(PartnerDTO parent) {
        this.parent = parent;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
    public Set<PartnerDTO> getChildren() {
        return children;
    }

    public void setChildren(Set<PartnerDTO> children) {
        this.children = children;
    }

    public void addChildren (PartnerDTO children ){this.children.add(children);}

    public void removeChildren (PartnerDTO children ){this.children.remove(children);}

    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.AGENT };
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="partner", orphanRemoval=true)
    @OrderBy(clause="id")
    public List<CommissionDTO> getCommissions() {
        return commissions;
    }

    public void setCommissions (List<CommissionDTO> commissions) {
        this.commissions = commissions;
    }

    public void addCommission (CommissionDTO commission ){this.commissions.add(commission);}

    public void removeCommission (CommissionDTO commission ){this.commissions.remove(commission);}

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="partner", orphanRemoval=true)
    @OrderBy(clause="id")
    public List<PartnerCommissionExceptionDTO> getCommissionExceptions () {
        return commissionExceptions;
    }

    public void setCommissionExceptions (List<PartnerCommissionExceptionDTO> commissionExceptions) {
        this.commissionExceptions = commissionExceptions;
    }

    public void addCommissionException ( PartnerCommissionExceptionDTO commissionException ){this.commissionExceptions.add(commissionException);}

    public void removeCommissionException ( PartnerCommissionExceptionDTO commissionException ){this.commissionExceptions.remove(commissionException);}

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="referral", orphanRemoval=true)
    @OrderBy(clause="id")
    public List<PartnerReferralCommissionDTO> getReferralCommissions () {
        return referralCommissions;
    }

    public void setReferralCommissions (List<PartnerReferralCommissionDTO> referralCommissions) {
        this.referralCommissions = referralCommissions;
    }

    public void addReferralCommission ( PartnerReferralCommissionDTO referralCommission ){this.referralCommissions.add(referralCommission);}

    public void removeReferralCommission( PartnerReferralCommissionDTO referralCommission ){this.referralCommissions.remove(referralCommission);}

    
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="referrer", orphanRemoval=true)
    @OrderBy(clause="id")
    public List<PartnerReferralCommissionDTO> getReferrerCommissions () {
        return referrerCommissions;
    }

    public void setReferrerCommissions (List<PartnerReferralCommissionDTO> referrerCommissions) {
        this.referrerCommissions = referrerCommissions;
    }

    public void addReferrerCommission ( PartnerReferralCommissionDTO referrerCommission ){this.referrerCommissions.add(referrerCommission);}

    public void removeReferrerCommission ( PartnerReferralCommissionDTO referrerCommission ){this.referrerCommissions.remove(referrerCommission);}

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = true)
    public PartnerCommissionType getCommissionType () {
        return commissionType;
    }

    public void setCommissionType (PartnerCommissionType commissionType) {
        this.commissionType = commissionType;
    }

    @Version
    @Column(name = "OPTLOCK")

    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    /* 
     * Inherited from DTOEx
     */
    @Transient
    public UserDTO getUser() {
        return getBaseUser();
    }

    public void touch() {
        for (PartnerPayout payout : getPartnerPayouts()) {
            payout.touch();
        }
    }

    @Transient
    @Override
    public String[] getFieldNames() {
        String names[] = new String[] {
                "id",
                "userName",
                "firstName",
                "lastName",
                "totalPayments",
                "totalRefunds",
                "totalPayouts",
                "type",
                "parentUserName",
        };    
        List<String> list = new ArrayList<>(Arrays.asList(names));
        
        for(String name : metaFieldsNames) {
        	list.add(name);
        }
        
        return list.toArray(new String[list.size()]);
   }

    @Transient
    @Override
    public Object[][] getFieldValues() {
        List<Object[]> values = new ArrayList<Object[]>();
        values.add(
                new Object[] {
                        id,
                        (getBaseUser().getUserName() != null ? getBaseUser().getUserName() : null),
                        (getBaseUser().getContact() != null ? getBaseUser().getContact().getFirstName() : null),
                        (getBaseUser().getContact() != null ? getBaseUser().getContact().getLastName() : null),
                        totalPayments,
                        totalRefunds,
                        totalPayouts,
                        type,
                        (getParent() != null ? getParent().getBaseUser().getUserName() : null)
                }
        );

        Object obj[] = new Object[metaFieldsNames.size()];
        int index = 0;
        for(String name : metaFieldsNames) {
            MetaFieldValue value = getMetaField(name);
            if(value != null) {
                obj[index++] = value.getValue();
            }
            else {
                obj[index++] = null;
            }
        }

        values.add(obj);

        return values.toArray(new Object[values.size()][]);
    }
}
