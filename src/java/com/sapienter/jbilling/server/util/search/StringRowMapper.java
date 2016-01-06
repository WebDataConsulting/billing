package com.sapienter.jbilling.server.util.search;

/**
 * Map all cells to Strings.
 *
* @author Gerhard
* @since 27/12/13
*/
public class StringRowMapper extends AbstractRowMapper<String> {
    @Override
    protected String mapCell(int col, Object value) {
        if (value != null) {
            return value.toString();
        }
        return "";
    }
}
