package com.sapienter.jbilling.server.util.search;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Generic implementation of search criteria.
 * The user can specify filters to use for querying, as well as the offset from the start of the result set,
 * the maximum number of objects to return and sorting parameters.
 *
 * @author Gerhard
 * @since 28/06/13
 */
public class SearchCriteria implements Serializable {
    public static enum SortDirection { ASC, DESC }

    /** index of first result in entire result list */
    private int offset = 0;
    /** max nr of entries to return */
    private int max;
    /** column to sort by */
    private String sort;
    /** Sort direction */
    private SortDirection direction = SortDirection.ASC;
    /** total nr results found. only populate in results if the value is < 0 */
    private int total = -1;
    /** filter to use in the sort */
    private BasicFilter[] filters;

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public SortDirection getDirection() {
        return direction;
    }

    public void setDirection(SortDirection direction) {
        this.direction = direction;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public BasicFilter[] getFilters() {
        return filters;
    }

    public void setFilters(BasicFilter[] filters) {
        this.filters = filters;
    }

    @Override
    public String toString() {
        return "SearchCriteria{" +
                "offset=" + offset +
                ", max=" + max +
                ", sort='" + sort + '\'' +
                ", direction=" + direction +
                ", total=" + total +
                ", filters=" + (filters == null ? null : Arrays.asList(filters)) +
                '}';
    }
}