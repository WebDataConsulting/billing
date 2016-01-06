package com.sapienter.jbilling.server.util;

import java.io.Serializable;

/**
 * @author Gerhard
 * @since 11/12/13
 */
public class NameValueString implements Serializable {
    private String name;
    private String value;

    public NameValueString() {
    }

    public NameValueString(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "NameValueString{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
