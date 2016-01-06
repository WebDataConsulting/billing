package com.sapienter.jbilling.server.util.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to export a List<Map<String, Object>> via the Exportable interface
 * @author Gerhard
 * @since 11/12/13
 */
public class ExportableMap<T> implements Exportable {

    List<Map<String,T>> content;
    String[] fieldNames;

    public ExportableMap(List<Map<String,T>> content) {
        this.content = content;
    }
    
    @Override
    public String[] getFieldNames() {
        if(fieldNames != null) {
            return fieldNames;
        }

        if(content == null || content.isEmpty()) {
            return new String[0];
        }
        ArrayList nameList = new ArrayList(content.get(0).keySet());
        fieldNames = (String[])nameList.toArray(new String[nameList.size()]);

        return fieldNames;
    }

    @Override
    public Object[][] getFieldValues() {
        String[] fields = getFieldNames();
        Object[][] values = new Object[content.size()][fields.length];
        
        int rowIdx = 0;
        for(Map<String,T> row : content) {
            int colIdx=0;
            for(String field: fields) {
                values[rowIdx][colIdx++] = row.get(field);    
            }
            rowIdx++;
        }
        
        return values;
    }

    public List<Map<String,T>> getContent() {
        return content;
    }

    public void setContent(List<Map<String,T>> content) {
        this.content = content;
    }
}
