package com.sapienter.jbilling.server.util.hibernate;

import com.sapienter.jbilling.server.util.db.StringList;
import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.CharacterStream;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterStreamImpl;
import org.hibernate.type.descriptor.java.DataHelper;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Gerhard
 * @since 31/01/14
 */
public class StringListTypeDescriptor extends AbstractTypeDescriptor<StringList> {
    public static final StringListTypeDescriptor INSTANCE = new StringListTypeDescriptor();

    public StringListTypeDescriptor() {
        super(StringList.class);
    }

    public String toString(StringList value) {
        return convertToString(value);
    }

    public StringList fromString(String string) {
        return convertToStringList(string);
    }

    @SuppressWarnings({"unchecked"})
    public <X> X unwrap(StringList value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (String.class.isAssignableFrom(type)) {
            return (X) convertToString(value);
        }
        if (Reader.class.isAssignableFrom(type)) {
            return (X) new StringReader(convertToString(value));
        }
        if (CharacterStream.class.isAssignableFrom(type)) {
            return (X) new CharacterStreamImpl(convertToString(value));
        }
        if (Clob.class.isAssignableFrom(type)) {
            return (X) options.getLobCreator().createClob(convertToString(value));
        }
        if (DataHelper.isNClob(type)) {
            return (X) options.getLobCreator().createNClob(convertToString(value));
        }

        throw unknownUnwrap(type);
    }

    public <X> StringList wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        String stringListStr = null;
        if (String.class.isInstance(value)) {
            stringListStr = (String) value;
        }
        if (Reader.class.isInstance(value)) {
            stringListStr = DataHelper.extractString((Reader) value);
        }
        if (Clob.class.isInstance(value) || DataHelper.isNClob(value.getClass())) {
            try {
                stringListStr = DataHelper.extractString(((Clob) value).getCharacterStream());
            } catch (SQLException e) {
                throw new HibernateException("Unable to access lob stream", e);
            }
        }

        if(stringListStr != null) {
            return convertToStringList(stringListStr);
        }
        throw unknownWrap(value.getClass());
    }

    public static String convertToString(StringList sl) {
        StringBuilder b = new StringBuilder();
        for(String v: sl.getValue()) {
            if(b.length() > 0) {
                b.append(',');
            }
            b.append('\"');
            b.append(v);
            b.append('\"');

        }
        return b.toString();
    }

    public static StringList convertToStringList(String s) {
        StringList sl = new StringList();
        List<String> value = new ArrayList<String>();
        if(s != null && s.length() > 0) {
            String[] values = s.split("\",\"");
            if(values.length == 1) {
                value.add(values[values.length-1].substring(1, values[values.length-1].length()-1));
            } else if(values.length > 0) {
                //trim leading " off first entry
                value.add(values[0].substring(1));
                for(int i=1; i<values.length-1; i++) {
                    value.add(values[i]);
                }
                if(values.length > 1) {
                    //trim leading " off first entry
                    value.add(values[values.length-1].substring(0, values[values.length-1].length()-1));
                }
            }
        }

        sl.setValue(value);
        return sl;
    }
}

