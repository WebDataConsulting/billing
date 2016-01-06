package com.sapienter.jbilling.server.metafields.db;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Vladimir Carevski
 */
public abstract class GroupCustomizedEntity extends CustomizedEntity {

    public List<MetaFieldValue> getGroupMetaFieldValues(MetaFieldGroup metaFieldGroup) {
        return getMetaFieldValuesPerGroup().get(metaFieldGroup);
    }

    public Map<MetaFieldGroup, List<MetaFieldValue>> getMetaFieldValuesPerGroup() {

        Map<MetaFieldGroup, List<MetaFieldValue>> values =
                new HashMap<MetaFieldGroup, List<MetaFieldValue>>();
        List<MetaFieldValue> metaFieldValues = getMetaFields();

        for (MetaFieldValue value : metaFieldValues) {
            for (MetaFieldGroup group : value.getField().getMetaFieldGroups()) {
                List<MetaFieldValue> listOfValues = values.get(group);
                if (null == listOfValues) {
                    listOfValues = new LinkedList<MetaFieldValue>();
                    values.put(group, listOfValues);
                }

                listOfValues.add(value);
            }
        }

        return values;
    }

}
