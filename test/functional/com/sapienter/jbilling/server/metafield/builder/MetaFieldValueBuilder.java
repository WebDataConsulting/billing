package com.sapienter.jbilling.server.metafield.builder;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;

public class MetaFieldValueBuilder {
    private final String fieldName;
    private Object       value;
    private int          group;

    public MetaFieldValueBuilder (String fieldName) {
        this.fieldName = fieldName;
    }

    public MetaFieldValueBuilder value (Object value) {
        this.value = value;
        return this;
    }

    public MetaFieldValueBuilder group (int group) {
        this.group = group;
        return this;
    }

    public MetaFieldValueWS build () {
        MetaFieldValueWS newMetaField = new MetaFieldValueWS();
        newMetaField.setFieldName(fieldName);
        newMetaField.setValue(value);
        if (group != 0) {
            newMetaField.setGroupId(group);
        }
        return newMetaField;
    }
}