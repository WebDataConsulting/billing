package com.sapienter.jbilling.server.util.search;

import com.sapienter.jbilling.server.util.cxf.ListListStringAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;

/**
 * Extension of SearchResult to facilitate XML marshalling for CXF
 *
 * @author Gerhard
 * @since 14/01/14
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class SearchResultString extends SearchResult<String> {

    @XmlJavaTypeAdapter(ListListStringAdapter.class)
    public List<List<String>> getStringRows() {
        return rows;
    }

    public void setStringRows(List<List<String>> rows) {
        this.rows = rows;
    }
}
