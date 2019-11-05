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

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.security.JBCrypto;
import com.sapienter.jbilling.server.util.csv.Exportable;

import javax.persistence.*;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

@Entity
// No cache, mutable and critical
@TableGenerator(
        name = "reset_password_code_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "user_password",
        allocationSize = 100)
@Table(name = "user_password_map")
public class UserPasswordDTO implements Serializable, Exportable {
    private Integer id;
    private UserDTO user;
    private Date dateCreated;
    private String password;
    private String encryptedPassword;

    public UserPasswordDTO() {
    }
    
    public UserPasswordDTO(UserDTO user, String encryptedPassword) {
    	this.user = user;
    	this.dateCreated = new Date();
    	this.encryptedPassword = encryptedPassword;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "reset_password_code_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_user_id")
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

    @Pattern(regexp= CommonConstants.PASSWORD_PATTERN_4_UNIQUE_CLASSES, message="validation.error.password.size,8,40")
    @Transient
    public String getPassword () {
        return password;
    }

    public void setPassword (String password,Integer userMainRoleId) {
        this.password = password;
        Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(userMainRoleId);
        setEncryptedPassword(JBCrypto.encodePassword(passwordEncoderId, this.password));

    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(name = "new_password", length = 40)
    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    private void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    @Transient
    public String[] getFieldNames () {
        return new String[]{
                "user",
                "dateCreated",
                "encryptedPassword",
        };
    }

    @Transient
    public Object[][] getFieldValues () {
        return new Object[][]{
                {
                        (user != null ? user.getUserName() : null),
                        dateCreated,
                        encryptedPassword
                }
        };
    }
}
