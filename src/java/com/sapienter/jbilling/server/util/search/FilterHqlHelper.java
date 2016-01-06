package com.sapienter.jbilling.server.util.search;

/**
 * Helper to build HQL queries from Filters.
 *
 * @author Gerhard
 * @since 28/06/13
 */
public class FilterHqlHelper {

    /**
     * Convert a FilterConstraint to an HQL operator.
     *
     * @param constraint
     * @return
     */
    public static String getHqlOperator(Filter.FilterConstraint constraint) {
        switch (constraint) {
            case EQ: return "=";
            case GE: return ">=";
            case GT: return ">";
            case LE: return "<=";
            case LT: return "<";
            case LIKE: return " like ";
            case IN: return " IN ";
        }
        return null;
    }

    /**
     * If the constraint is 'LIKE' it will append a '%' after the queryParameter.
     *
     * @param constraint
     * @param queryParameter
     * @return
     */
    public static Object likeMatchStart(Filter.FilterConstraint constraint, Object queryParameter) {
        if(queryParameter instanceof String &&
                (constraint == Filter.FilterConstraint.LIKE)) {
            return ((String)queryParameter)+'%';
        } else {
            return queryParameter;
        }
    }

    /**
     * Return the HQL value for the sort direction
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
}
