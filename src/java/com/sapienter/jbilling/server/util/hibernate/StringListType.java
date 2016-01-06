package com.sapienter.jbilling.server.util.hibernate;

import com.sapienter.jbilling.server.util.db.StringList;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * @author Gerhard
 * @since 31/01/14
 */
public class StringListType extends AbstractSingleColumnStandardBasicType<StringList> implements DiscriminatorType<StringList> {

    public static final StringListType INSTANCE = new StringListType();

    public StringListType() {
        super( VarcharTypeDescriptor.INSTANCE, StringListTypeDescriptor.INSTANCE );
    }

    public String getName() {
        return "stringList";
    }

    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }

    public String objectToSQLString(StringList value, Dialect dialect) throws Exception {
        return '\'' + StringListTypeDescriptor.convertToString(value) + '\'';
    }

    public StringList stringToObject(String xml) throws Exception {
        return StringListTypeDescriptor.convertToStringList(xml);
    }

    public String toString(String value) {
        return value;
    }
}

