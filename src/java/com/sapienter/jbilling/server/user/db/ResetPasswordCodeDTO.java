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

import com.sapienter.jbilling.server.util.csv.Exportable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
// No cache, mutable and critical
@Table(name = "reset_password_code")
public class ResetPasswordCodeDTO implements Serializable, Exportable {
    private UserDTO user;
    private Date dateCreated;
    private String token;
    private String newPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_user_id", unique = true)
    public UserDTO getUser () {
        return user;
    }

    public void setUser (UserDTO user) {
        this.user = user;
    }

    @Column(name = "date_created")
    public Date getDateCreated () {
        return dateCreated;
    }

    public void setDateCreated (Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Id
    @Column(name = "token", length = 32)
    public String getToken () {
        return token;
    }

    public void setToken (String token) {
        this.token = token;
    }

    @Column(name = "new_password", length = 40)
    public String getNewPassword () {
        return newPassword;
    }

    public void setNewPassword (String newPassword) {
        this.newPassword = newPassword;
    }

    @Transient
    public String[] getFieldNames () {
        return new String[]{
                "user",
                "dateCreated",
                "token",
                "newPassword",
        };
    }

    @Transient
    public Object[][] getFieldValues () {
        return new Object[][]{
                {
                        (user != null ? user.getUserName() : null),
                        dateCreated,
                        token,
                        newPassword
                }
        };
    }

}
