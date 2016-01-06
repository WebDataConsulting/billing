package com.sapienter.jbilling.server.util.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Gerhard
 * @since 31/01/14
 */
public class StringList implements Serializable {
    private List<String> value = new ArrayList<String>();

    public StringList() {

    }

    public StringList(Collection valueEntries) {
        value.addAll(valueEntries);
    }

    public List<String> getValue() {
        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StringList) {
            StringList o2 = (StringList) obj;
            if(value.size() == o2.value.size()) {
                for(int i=0; i<value.size(); i++) {
                    if(!value.get(i).equals(o2.value.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "StringList{" +
                "value=" + value +
                '}';
    }
}
