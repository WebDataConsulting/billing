package com.sapienter.jbilling.server.util;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Created by vojislav on 14.1.15.
 */
public class EnumerationValueWS implements Serializable {

    private Integer id;
    @Size(min = 1, max = 50, message = "enumeration.value.missing")
    private String value;

    public EnumerationValueWS() {}

    public EnumerationValueWS(Integer id) {
        this(id, null);
    }

    public EnumerationValueWS(String value){
        this(null, value);
    }

    public EnumerationValueWS(Integer id, String value) {
        setId(id);
        setValue(value);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "EnumerationValueWS{" +
                "id=" + id +
                ", value='" + value + '\'' +
                '}';
    }
}
