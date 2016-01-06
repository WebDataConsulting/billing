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

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * 
 * @author Igor Poteryaev <igor.poteryaev@jbilling.com>
 * @since  2015-04-10
 *
 */
public abstract class BasicUserType implements UserType {

    @Override
    public Object assemble (Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object deepCopy (Object value) throws HibernateException {
        return value;
    }

    @Override
    public Serializable disassemble (Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public boolean equals (Object x, Object y) throws HibernateException {
        return x == y;
    }

    @Override
    public int hashCode (Object x) throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Object replace (Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}
