package com.sapienter.jbilling.server.util.search;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Search result object containing a total row count, row columns names
 * and the content.
 *
 * CXF can not marshall List<List<T>>. Subclasses must be generated with XmlAdapters to help with marshalling
 * See SearchResultString
 * @author Gerhard
 * @since 27/12/13
 * @see SearchResultString
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SearchResult<T> implements Serializable {
    private int total;
    private List<String> columnNames = new ArrayList<String>();

    protected List<List<T>> rows = new ArrayList<List<T>>();

    @XmlAttribute
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    @XmlElement
    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public List<List<T>> getRows() {
        return rows;
    }

    public void setRows(List<List<T>> rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "total=" + total +
                ", columnNames=" + columnNames +
                ", rows.size=" + (rows == null ? 0 : rows.size()) +
                '}';
    }
}
