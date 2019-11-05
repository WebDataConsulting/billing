/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */

package com.sapienter.jbilling.server.pricing.cache;

import com.sapienter.jbilling.common.FormatLogger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.math.BigDecimal;

;

/**
 * MatchType specifies the logic used by {@link RateCardFinder} when determining if a pricing
 *
 * @author Brian Cowdery
 * @since 19-02-2012
 */
public enum MatchType {

    /**
     * Searches for an entry in the rating table where the entry exactly matches the search value
     */
    EXACT {
        public BigDecimal findPrice(JdbcTemplate jdbcTemplate, String query, String searchValue) {
            LOG.debug("Searching for exact match '%s'", searchValue);
            SqlRowSet rs = jdbcTemplate.queryForRowSet(query, searchValue);

            if (rs.next())
                return rs.getBigDecimal("rate");

            return null;
        }
    },

    /**
     * Searches through the rating table looking for an entry using the search value as
     * a prefix. The BEST_MATCH continually shortens the prefix being used in the search
     * to find a match with the largest possible portion of the search string.
     */
    BEST_MATCH {
        public BigDecimal findPrice(JdbcTemplate jdbcTemplate, String query, String searchValue) {
            int length = 10;
            searchValue = getCharacters(searchValue, length);

            while (length >= 0) {
                LOG.debug("Searching for prefix '%s'", searchValue);
                SqlRowSet rs = jdbcTemplate.queryForRowSet(query, searchValue);

                if (rs.next()) {
                    return rs.getBigDecimal("rate");
                } else {
                    length--;
                    searchValue = getCharacters(searchValue, length);
                }
            }

            return null;
        }

        public String getCharacters(String number, int length) {
            if (length <= 0) return "*";
            return number.length() > length ? number.substring(0, length) : number;
        }
    };


    private static final FormatLogger LOG = new FormatLogger(MatchType.class);

    public abstract BigDecimal findPrice(JdbcTemplate jdbcTemplate, String query, String searchValue);
}
