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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * Utility class for Hibernate.
 * Add support of persistence of enum types
 * 
 * @author Igor Poteryaev <igor.poteryaev@jbilling.com>
 * @since  2015-04-10
 *
 */
public abstract class PersistentEnumUserType<T extends PersistentEnum> extends BasicUserType implements UserType {

    @Override
    public boolean isMutable () {
        return false;
    }

    @Override
    public Object nullSafeGet (ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
        int id = rs.getInt(names[0]);
        if (rs.wasNull()) {
            return null;
        }
        for (PersistentEnum value : returnedClass().getEnumConstants()) {
            if (id == value.getId()) {
                return value;
            }
        }
        throw new IllegalStateException("Unknown " + returnedClass().getSimpleName() + " id");
    }

    @Override
    public void nullSafeSet (PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.SMALLINT);
        } else {
            st.setInt(index, ((PersistentEnum) value).getId());
        }
    }

    @Override
    public abstract Class<T> returnedClass ();

    @Override
    public int[] sqlTypes () {
        return new int[] { Types.SMALLINT };
    }
}
