package com.sapienter.jbilling.server.util.cxf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Used by ListListStringAdapter to help CXF to marshall List<List<String>> objects
 * @author Gerhard
 * @since 14/01/14
 */
@XmlType(name = "CxfListListString")
public class ListListString {
    @XmlElement(nillable = false, name = "entry")
    List<StringArray> entries = new ArrayList<StringArray>();

    public List<StringArray> getEntries() {
        return entries;
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "CxfListListStringStringArray")
    static class StringArray  {
        @XmlElement(required = true, nillable = false)
        List<String> array = new ArrayList<String>();

        List<String> getArray() {
            return array;
        }

        void setArray(List<String> array) {
            this.array = array;
        }
    }
}
