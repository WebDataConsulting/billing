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
 * Created on Sep 16, 2004
 *
 */
package com.sapienter.jbilling.server.user;

import java.io.Serializable;
import java.util.Date;
import java.util.Hashtable;

import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.server.user.contact.db.ContactDTO;

/**
 * @author Emil
 */
public class ContactDTOEx extends ContactDTO implements Serializable  {

    private Integer type = null; // the contact type

    /**
     * 
     */
    public ContactDTOEx() {
        super();
    }

    /**
     * @param id
     * @param organizationName
     * @param address1
     * @param address2
     * @param city
     * @param stateProvince
     * @param postalCode
     * @param countryCode
     * @param lastName
     * @param firstName
     * @param initial
     * @param title
     * @param phoneCountryCode
     * @param phoneAreaCode
     * @param phoneNumber
     * @param faxCountryCode
     * @param faxAreaCode
     * @param faxNumber
     * @param email
     * @param createDate
     * @param deleted
     */
    public ContactDTOEx(Integer id, String organizationName, String address1,
            String address2, String city, String stateProvince,
            String postalCode, String countryCode, String lastName,
            String firstName, String initial, String title,
            Integer phoneCountryCode, Integer phoneAreaCode,
            String phoneNumber, Integer faxCountryCode, Integer faxAreaCode,
            String faxNumber, String email, Date createDate, Integer deleted,
            Integer notify) {
        super(id,organizationName, address1, address2, city, stateProvince, postalCode, countryCode, 
                lastName, firstName, initial, title, phoneCountryCode, phoneAreaCode, 
                phoneNumber, faxCountryCode, faxAreaCode, faxNumber, email, createDate, deleted, 
                notify, null, null);
    }

    /**
     * @param otherValue
     */
    public ContactDTOEx(ContactDTO otherValue) {
        super(otherValue);
    }
    
    public ContactDTOEx(ContactWS ws) {
        setId(ws.getId());
        setOrganizationName(ws.getOrganizationName());
        setAddress1(ws.getAddress1());
        setAddress2(ws.getAddress2());
        setCity(ws.getCity());
        setStateProvince(ws.getStateProvince());
        setPostalCode(ws.getPostalCode());
        setCountryCode(ws.getCountryCode());
        setLastName(ws.getLastName());
        setFirstName(ws.getFirstName());
        setInitial(ws.getInitial());
        setTitle(ws.getTitle());
		if (!StringUtils.isEmpty(ws.getPhoneCountryCode())) {
			setPhoneCountryCode(new Integer(ws.getPhoneCountryCode()));
		}
		if (!StringUtils.isEmpty(ws.getPhoneAreaCode())) {
			setPhoneAreaCode(new Integer(ws.getPhoneAreaCode()));
		}
        setPhoneNumber(ws.getPhoneNumber());
        setFaxCountryCode(ws.getFaxCountryCode());
        setFaxAreaCode(ws.getFaxAreaCode());
        setFaxNumber(ws.getFaxNumber());
        setEmail(ws.getEmail());
        setCreateDate(ws.getCreateDate());
        setDeleted(ws.getDeleted());
        setInclude( ( ws.getInclude() ? 1 : 0 ) );

        // contacts from ws are always included in notifications
        //setInclude(new Integer(1));

    }

    public Integer getType() {
        return type;
    }
    
    public void setType(Integer type) {
        this.type = type;
    }

	@Override
	public String toString() {
		return String.format("ContactDTOEx [type=%s, toString()=%s]", type,
				super.toString());
	}
    
}
