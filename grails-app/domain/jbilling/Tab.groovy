/*
 * jBilling - The Enterprise Open Source Billing System
 * Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde
 * 
 * This file is part of jbilling.
 * 
 * jbilling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jbilling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
You may download the latest source from webdataconsulting.github.io.

 */
package jbilling

/**
 * Objects of this class represents an horizontal menu item at the top of the screen (customers,products...)
 *
 * Either accessUrl or requiredRole must be supplied. If both are supplied the requiredRole takes precedence
 */
class Tab {

    //Message to display on the tab
    String messageCode;
    //User will be redirected to this controller when he clicks the link
    String controllerName;
    //User must have access to the URL
    String accessUrl;
    //User must have this role
    String requiredRole;
    //default order the tab is displayed in
    Integer defaultOrder;

    @Override
    public String toString() {
        return "Tab{" +
                "id=" + id +
                ", messageCode='" + messageCode + '\'' +
                ", controllerName='" + controllerName + '\'' +
                ", accessUrl='" + accessUrl + '\'' +
                ", requiredRole='" + requiredRole + '\'' +
                '}';
    }
}
