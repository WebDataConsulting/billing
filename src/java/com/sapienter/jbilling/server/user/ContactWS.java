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

/*
 * Created on Jan 25, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.util.api.validation.EntitySignupValidationGroup;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/** @author Emil */
public class ContactWS implements Serializable {

    private static final long serialVersionUID = 20140605L;

    private Integer id;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    @Size(min = 0, max = 200, message = "validation.error.size,0,200")
    private String organizationName;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    @Size(min = 0, max = 100, message = "validation.error.size,0,100")
    private String address1;
    @Size(min = 0, max = 100, message = "validation.error.size,0,100")
    private String address2;
    @Size(min = 0, max = 50, message = "validation.error.size,0,50")
    private String city;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    @Size(min = 0, max = 30, message = "validation.error.size,0,30")
    private String stateProvince;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    @Size(min = 0, max = 15, message = "validation.error.size,0,15")
    private String postalCode;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    @Size(min = 0, max = 2, message = "validation.error.size,0,2")
    @Pattern(regexp = "^$|[A-Z]{2}", message = "validation.error.contact.countryCode.pattern")
    private String countryCode;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    @Size(min = 0, max = 30, message = "validation.error.size,0,30")
    private String lastName;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    @Size(min = 0, max = 30, message = "validation.error.size,0,30")
    private String firstName;
    @Size(min = 0, max = 30, message = "validation.error.size,0,30")
    private String initial;
    private String title;
    private String phoneCountryCode;
    private String phoneAreaCode;
    @Size(min = 0, max = 20, message = "validation.error.size,0,20")
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    private String phoneNumber;
    private Integer faxCountryCode;
    private Integer faxAreaCode;
    private String faxNumber;
    @NotEmpty(message = "validation.error.notnull", groups = EntitySignupValidationGroup.class)
    //Bug4089 regexp for meeting rfc3696 restriction for email addresses. 
    @Pattern(regexp = "^([a-zA-Z0-9#\\!$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]|(\\\\@|\\\\,|\\\\\\[|\\\\\\]|\\\\ ))+(\\.([a-zA-Z0-9!#\\$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]|(\\\\@|\\\\,|\\\\\\[|\\\\\\]))+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.([A-Za-z]{2,})$", message = "validation.error.email")
    @Size(min = 6, max = 320, message = "validation.error.size,6,320")
    private String email;
    private Date createDate;
    private int deleted;
    private Boolean include;
    private Boolean invoiceAsReseller;

    public ContactWS() {
        super();
    }

    public ContactWS(Integer id, String address1,
            String address2, String city, String stateProvince,
            String postalCode, String countryCode, int deleted) {
            this.id = id;
            this.address1 = address1;
            this.address2 = address2;
            this.city = city;
            this.stateProvince = stateProvince;
            this.postalCode = postalCode;
            this.countryCode = countryCode;
            this.deleted = deleted;
        }

    public ContactWS(Integer id, String organizationName, String address1,
                     String address2, String city, String stateProvince,
                     String postalCode, String countryCode, String lastName,
                     String firstName, String initial, String title,
                     String phoneCountryCode, String phoneAreaCode,
                     String phoneNumber, Integer faxCountryCode, Integer faxAreaCode,
                     String faxNumber, String email, Date createDate, Integer deleted,
                     Boolean include) {
        this.id = id;
        this.organizationName = organizationName;
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.stateProvince = stateProvince;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.lastName = lastName;
        this.firstName = firstName;
        this.initial = initial;
        this.title = title;
        this.phoneCountryCode = phoneCountryCode;
        this.phoneAreaCode = phoneAreaCode;
        this.phoneNumber = phoneNumber;
        this.faxCountryCode = faxCountryCode;
        this.faxAreaCode = faxAreaCode;
        this.faxNumber = faxNumber;
        this.email = email;
        this.createDate = createDate;
        this.deleted = deleted;
        this.include = include;
    }

    public ContactWS(ContactWS other) {
        setId(other.getId());
        setOrganizationName(other.getOrganizationName());
        setAddress1(other.getAddress1());
        setAddress2(other.getAddress2());
        setCity(other.getCity());
        setStateProvince(other.getStateProvince());
        setPostalCode(other.getPostalCode());
        setCountryCode(other.getCountryCode());
        setLastName(other.getLastName());
        setFirstName(other.getFirstName());
        setInitial(other.getInitial());
        setTitle(other.getTitle());
        setPhoneCountryCode(null != other.getPhoneCountryCode() ? other.getPhoneCountryCode() : "");
        setPhoneAreaCode(null != other.getPhoneAreaCode() ? other.getPhoneAreaCode() : "");
        setPhoneNumber(other.getPhoneNumber());
        setFaxCountryCode(other.getFaxCountryCode());
        setFaxAreaCode(other.getFaxAreaCode());
        setFaxNumber(other.getFaxNumber());
        setEmail(other.getEmail());
        setCreateDate(other.getCreateDate());
        setDeleted(other.getDeleted());
        setInclude(other.getInclude());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getInitial() {
        return initial;
    }

    public void setInitial(String initial) {
        this.initial = initial;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPhoneCountryCode() {
        return phoneCountryCode;
    }

    public void setPhoneCountryCode(String phoneCountryCode) {
        this.phoneCountryCode = phoneCountryCode;
    }

    public String getPhoneAreaCode() {
        return phoneAreaCode;
    }

    public void setPhoneAreaCode(String phoneAreaCode) {
        this.phoneAreaCode = phoneAreaCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getFaxCountryCode() {
        return faxCountryCode;
    }

    public void setFaxCountryCode(Integer faxCountryCode) {
        this.faxCountryCode = faxCountryCode;
    }

    public Integer getFaxAreaCode() {
        return faxAreaCode;
    }

    public void setFaxAreaCode(Integer faxAreaCode) {
        this.faxAreaCode = faxAreaCode;
    }

    public String getFaxNumber() {
        return faxNumber;
    }

    public void setFaxNumber(String faxNumber) {
        this.faxNumber = faxNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    public Boolean getInclude() {
        return include == null ? Boolean.FALSE : include;
    }

    public void setInclude(Boolean include) {
        this.include = include;
    }
    
    public Boolean getInvoiceAsReseller() {
		return invoiceAsReseller;
	}

	public void setInvoiceAsReseller(Boolean invoiceAsReseller) {
		this.invoiceAsReseller = invoiceAsReseller;
	}

    @Override
    public String toString() {
        return "ContactWS{"
               + "id=" + id
               + ", title='" + title + '\''
               + ", lastName='" + lastName + '\''
               + ", firstName='" + firstName + '\''
               + ", initial='" + initial + '\''
               + ", organization='" + organizationName + '\''
               + ", address1='" + address1 + '\''
               + ", address2='" + address2 + '\''
               + ", city='" + city + '\''
               + ", stateProvince='" + stateProvince + '\''
               + ", postalCode='" + postalCode + '\''
               + ", countryCode='" + countryCode + '\''
               + ", phone='" + (phoneCountryCode != null ? phoneCountryCode : "")
                             + (phoneAreaCode != null ? phoneAreaCode : "")
                             + (phoneNumber != null ?  phoneNumber : "") + '\''
               + ", fax='" + (faxCountryCode != null ? faxCountryCode : "")
                           + (faxAreaCode != null ? faxAreaCode : "")
                           + (faxNumber != null ? faxNumber : "") + '\''
               + ", email='" + email + '\''
               + ", include='" + include + '\''
               + '}';
    }
}
