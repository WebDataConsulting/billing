package com.sapienter.jbilling.server.util.search;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class to provide basic functionality of RowMapper.
* @author Gerhard
* @since 27/12/13
*/
public abstract class AbstractRowMapper<T> implements FilterSqlHelper.RowMapper<T> {
    List<String> columnNames = null;

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<T> mapRow(ResultSet resultSet, int i) throws SQLException {
        if(columnNames == null) {
            ResultSetMetaData md = resultSet.getMetaData();
            columnNames = new ArrayList<String>(md.getColumnCount());
            for(int j=1; j<=md.getColumnCount(); j++) {
                columnNames.add(md.getColumnName(j));
            }
        }

        List<T> row = new ArrayList<T>(columnNames.size());
        for(int j=1; j<=columnNames.size(); j++) {
            row.add(mapCell(j, resultSet.getObject(j)));
        }

        return row;
    }

    protected abstract T mapCell(int col, Object value);
}
