package com.sapienter.jbilling.server.util.search;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper to build SQL queries from Filters.
 *
 * @author Gerhard
 * @since 28/06/13
 */
public class FilterSqlHelper {
    private static final FormatLogger LOG = new FormatLogger(FilterSqlHelper.class);

    /**
     * Convert a FilterConstraint to an SQL filter.
     * @return
     */
    public static String getSqlFilter(Filter filter) {
        StringBuilder sql;
        if(filter.getConstraint() == Filter.FilterConstraint.ILIKE) {
            sql = new StringBuilder("lower("+filter.getField()+")");
        } else {
            sql = new StringBuilder(filter.getField());
        }

        switch (filter.getConstraint()) {
            case EQ: sql.append("= ?"); break;
            case GE: sql.append(">= ?"); break;
            case GT: sql.append("> ?"); break;
            case LE: sql.append("<= ?"); break;
            case LT: sql.append("< ?"); break;
            case LIKE:
            case ILIKE: sql.append(" like ?"); break;
            case IN: sql.append(" IN (");
                for(int i=0; i<((List)filter.getValue()).size(); i++) {
                    if(i > 0) {
                        sql.append(',');
                    }
                    sql.append('?');
                }
                sql.append(')');
                break;
        }
        return sql.toString();
    }

    /**
     * Return all the parameters in the filter as an array.
     * The parameter may be altered depending on the constraint.
     *
     * @param filter
     * @return
     */
    public static List getFilterParams(Filter filter) {
        if(filter.getValue() instanceof  List) {
            return (List)filter.getValue();
        }
        List params = new ArrayList(1);
        params.add(likeMatchStart(filter.getConstraint(), filter.getValue()));
        return params;
    }

    /**
     * If the constraint is 'LIKE' it will append a '%' after the queryParameter.
     *
     * @param constraint
     * @param queryParameter
     * @return
     */
    public static Object likeMatchStart(Filter.FilterConstraint constraint, Object queryParameter) {
        if(queryParameter instanceof String) {
            if(constraint == Filter.FilterConstraint.LIKE) {
                return ((String)queryParameter)+'%';
            } else if(constraint == Filter.FilterConstraint.ILIKE) {
                return ((String) queryParameter).toLowerCase()+'%';
            }
        }
        return queryParameter;
    }

    public static String getOrderBy(SearchCriteria criteria) {
        if(criteria.getSort() != null && criteria.getSort().length() > 0) {
            return " ORDER BY " + criteria.getSort() +" "+getSortDirection(criteria.getDirection());
        }
        return "";
    }

    /**
     * Return the SQL value for the sort direction
     *
     * @param direction
     * @return
     */
    public static String getSortDirection(SearchCriteria.SortDirection direction) {
        switch (direction) {
            case ASC: return "asc";
            case DESC: return "desc";
        }
        return "";
    }

    /**
     * Helper function to do a SQL search based on the SearchCriteria provided.
     *
     * @param criteria  - SearchCriteria used to build SQL
     * @param rowMapper - Object that will map a row in the result set
     * @param tableName - table name to query
     * @param jdbcTemplate  - JDBC template to use
     * @return SearchResult populated with rows matching the criteria as well as total nr of lines
     */
    public static <T> void search(SearchCriteria criteria, RowMapper<T> rowMapper, SearchResult<T> result, String tableName, JdbcTemplate jdbcTemplate) {
        LOG.debug(criteria);

        //SQL clauses
        StringBuilder sqlFrom = new StringBuilder("SELECT * FROM "+tableName);
        StringBuilder sqlWhere = new StringBuilder(" WHERE ");

        //parameters for HQL query
        List<Object> parameters = new ArrayList<Object>();

        int filterIdx = 1;
        for(BasicFilter filter : criteria.getFilters()) {
            if(filterIdx++ > 1) {
                sqlWhere.append(" AND ");
            }

            sqlWhere.append(FilterSqlHelper.getSqlFilter(filter));
            parameters.addAll(FilterSqlHelper.getFilterParams(filter));

        }

        String paging = addPaging(criteria, sqlFrom, sqlWhere, jdbcTemplate);

        //construct the search query
        String sqlQuery = sqlFrom.toString();
        if(filterIdx > 1) {
            sqlQuery += sqlWhere.toString();
        }
        sqlQuery += FilterSqlHelper.getOrderBy(criteria) + paging;

        LOG.debug(sqlQuery);
        LOG.debug("Parameters %s", parameters);

        result.setRows(jdbcTemplate.query(sqlQuery, parameters.toArray(), rowMapper));
        result.setColumnNames(rowMapper.getColumnNames());

        //get the total
        if(criteria.getTotal() < 0) {
            String sqlCountQuery = "SELECT count(1) FROM "+tableName;
            if(filterIdx > 1) {
                sqlCountQuery += sqlWhere.toString();
            }

            LOG.debug(sqlCountQuery);
            result.setTotal(jdbcTemplate.queryForInt(sqlCountQuery, parameters.toArray()));
        }
    }

    /**
     * Add paging arguments to the sql query
     * @param criteria
     * @param sqlFrom
     * @param sqlWhere
     * @param jdbcTemplate
     * @return
     */
    private static String addPaging(SearchCriteria criteria, StringBuilder sqlFrom, StringBuilder sqlWhere, JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.setFetchSize(criteria.getMax());
            jdbcTemplate.setMaxRows(criteria.getMax());

            Connection conn = jdbcTemplate.getDataSource().getConnection();
            String db = conn.getMetaData().getDatabaseProductName().toUpperCase();
            conn.close();

            if (db.equals("POSTGRESQL") || db.equals("MYSQL")) {
                return " LIMIT "+criteria.getMax() + " OFFSET " + criteria.getOffset() ;
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
        return "";
    }

    /**
     * Interface used to map rows. All columns are mapped to the same type.
     * @param <T> - all columns will have this type
     */
    public interface RowMapper<T> extends org.springframework.jdbc.core.RowMapper<List<T>> {
        public List<String> getColumnNames();
    }

}
