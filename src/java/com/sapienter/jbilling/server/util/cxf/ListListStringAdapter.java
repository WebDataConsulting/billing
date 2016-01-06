package com.sapienter.jbilling.server.util.cxf;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Allow CXF to marshall {@code List<List<String>> } objects.
 *
 * @author Gerhard
 * @since 14/01/14
 */
public class ListListStringAdapter extends XmlAdapter<ListListString, List<List<String>>> {

    @Override
    public List<List<String>> unmarshal(ListListString b) throws Exception {
        List<List<String>> valueObject = new ArrayList<List<String>>(b.entries.size());
        for(ListListString.StringArray bEntry : b.entries) {
            valueObject.add(bEntry.getArray());
        }
        return valueObject;
    }

    @Override
    public ListListString marshal(List<List<String>> v) throws Exception {
        ListListString boundObject = new ListListString();
        for(List<String> vEntry : v) {
            ListListString.StringArray entry = new ListListString.StringArray();
            entry.setArray(vEntry);
            boundObject.entries.add(entry);
        }
        return boundObject;
    }
}
