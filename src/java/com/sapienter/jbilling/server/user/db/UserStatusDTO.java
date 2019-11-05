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
package com.sapienter.jbilling.server.user.db;


import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@TableGenerator(name = "user_status_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "user_status",
        allocationSize = 100)
@Table(name = "user_status")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UserStatusDTO extends AbstractDescription implements java.io.Serializable {

    protected int id;
    private int canLogin;
    private AgeingEntityStepDTO ageingEntityStep;
    private Set<UserDTO> baseUsers = new HashSet<UserDTO>(0);

    public UserStatusDTO() {
    }

    public UserStatusDTO(int canLogin, AgeingEntityStepDTO ageingEntityStep) {
        this.canLogin = canLogin;
        this.ageingEntityStep = ageingEntityStep;
    }

    public UserStatusDTO(int canLogin, AgeingEntityStepDTO ageingEntityStep, Set<UserDTO> baseUsers) {
        this.canLogin = canLogin;
        this.ageingEntityStep = ageingEntityStep;
        this.baseUsers = baseUsers;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "user_status_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    @Transient
    protected String getTable() {
        return ServerConstants.TABLE_USER_STATUS;
    }
    
    @Column(name="can_login", nullable=false)
    public int getCanLogin() {
        return this.canLogin;
    }
    
    public void setCanLogin(int canLogin) {
        this.canLogin = canLogin;
    }
    @OneToOne(mappedBy = "userStatus", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public AgeingEntityStepDTO getAgeingEntityStep() {
        return this.ageingEntityStep;
    }

    public void setAgeingEntityStep(AgeingEntityStepDTO ageingEntityStep) {
        this.ageingEntityStep = ageingEntityStep;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "userStatus")
    public Set<UserDTO> getBaseUsers() {
        return this.baseUsers;
    }

    public void setBaseUsers(Set<UserDTO> baseUsers) {
        this.baseUsers = baseUsers;
    }

    public void addBaseUser(UserDTO baseUser){this.baseUsers.add(baseUser);}

    public void removeBaseUser(UserDTO baseUser){this.baseUsers.remove(baseUser);}

    @Transient
    public boolean isSuspended() {
        return ageingEntityStep == null || ageingEntityStep.getSuspend() > 0;
    }
}


