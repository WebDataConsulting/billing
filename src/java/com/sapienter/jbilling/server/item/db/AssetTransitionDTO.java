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
package com.sapienter.jbilling.server.item.db;

import com.sapienter.jbilling.server.user.db.UserDTO;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * AssetTransitionDTO objects record status changes of AssetDTOs. Instances are immutable.
 *
 * @author Gerhard
 * @since 15/04/13
 */
@Entity
@Table(name = "asset_transition")
@TableGenerator(
        name = "asset_transition_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "asset_transition",
        allocationSize = 100
)
@NamedQueries({
        @NamedQuery(name = "AssetTransitionDTO.findForAsset",
                query = "select at from AssetTransitionDTO at where at.asset.id= :asset_id order by createDatetime desc, id desc")
})
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class AssetTransitionDTO implements Serializable {

    private int id;
    private Date createDatetime;
    private AssetStatusDTO previousStatus;
    private AssetStatusDTO newStatus;
    private AssetDTO asset;
    /** user the asset is assign to (through OrderLine linked to the asset) */
    private UserDTO assignedTo;
    /** user who made the change */
    private UserDTO user;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "asset_transition_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "create_datetime", nullable = false, length = 29)
    public Date getCreateDatetime() {
        return this.createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_status_id")
    public AssetStatusDTO getPreviousStatus() {
        return this.previousStatus;
    }

    public void setPreviousStatus(AssetStatusDTO previousStatus) {
        this.previousStatus = previousStatus;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_status_id")
    public AssetStatusDTO getNewStatus() {
        return this.newStatus;
    }

    public void setNewStatus(AssetStatusDTO newStatus) {
        this.newStatus = newStatus;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    public AssetDTO getAsset() {
        return this.asset;
    }

    public void setAsset(AssetDTO asset) {
        this.asset = asset;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    public UserDTO getAssignedTo() {
        return this.assignedTo;
    }

    public void setAssignedTo(UserDTO  assignedTo) {
        this.assignedTo = assignedTo;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getUser() {
        return this.user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

}