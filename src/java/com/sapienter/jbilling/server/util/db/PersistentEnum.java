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

package com.sapienter.jbilling.server.util.db;

/**
 * @see http://www.gabiaxel.com/2011/01/better-enum-mapping-with-hibernate.html
 * For simpler grails alternative @see http://blog.tamashumi.com/2013/06/grails-enum-custom-database-value.html
 * TODO: http://www.abnsoft.info/2014/01/14/hibernate-enum-usertype-how-to-use-java-enum-class-hibernate/
 * 
 * @author Igor Poteryaev <igor.poteryaev@jbilling.com>
 * @since  2015-04-10
 */
public interface PersistentEnum {
    int getId ();
}
