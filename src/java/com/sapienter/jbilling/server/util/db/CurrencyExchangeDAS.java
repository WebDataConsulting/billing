
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
package com.sapienter.jbilling.server.util.db;

import org.hibernate.Query;

import java.util.Date;
import java.util.List;


public class CurrencyExchangeDAS extends AbstractDAS<CurrencyExchangeDTO> {

    private static final String findExchangeForDateSQL =
        "SELECT a " +
        "  FROM CurrencyExchangeDTO a " +
        " WHERE a.entityId = :entity " +
        "   AND a.currency.id = :currency " +
        "   AND a.validSince <= :date ORDER BY a.validSince DESC";

    private static final String findExchangeInRangeSQL =
        "SELECT a " +
        "  FROM CurrencyExchangeDTO a " +
        " WHERE a.entityId = :entity " +
        "   AND a.currency.id = :currency " +
        "   AND a.validSince >= :dateFrom " +
        "   AND a.validSince <= :dateTo ORDER BY a.validSince DESC";

    private static final String  findByEntitySQL =
        " SELECT a " +
        "   FROM CurrencyExchangeDTO a " +
        "  WHERE a.entityId = :entity";

    public CurrencyExchangeDTO findExchange(Integer entityId, Integer currencyId) {
        return getExchangeRateForDate(entityId, currencyId, new Date());
    }

    public List<CurrencyExchangeDTO> findByEntity(Integer entityId) {
        Query query = getSession().createQuery(findByEntitySQL);
        query.setParameter("entity", entityId);
        return query.list();
    }

    /**
     * Returns an exchange rate closest to a specified date
     */
    public CurrencyExchangeDTO getExchangeRateForDate(Integer entityId, Integer currencyId, Date forDate) {
        Query query = getSession().createQuery(findExchangeForDateSQL);
        query.setParameter("entity", entityId);
        query.setParameter("currency", currencyId);
        query.setParameter("date", forDate);
        final List<CurrencyExchangeDTO> results = query.list();
        if(results.isEmpty()) {
            return null;
        }
        return  results.get(0);
    }

    /**
     * Returns an exchange rate from specified date range closest to a 'to' parameter
     */
    public CurrencyExchangeDTO getExchangeRateForRange(Integer entityId, Integer currencyId, Date from, Date to) {
        Query query = getSession().createQuery(findExchangeInRangeSQL);
        query.setParameter("entity", entityId);
        query.setParameter("currency", currencyId);
        query.setParameter("dateFrom", from);
        query.setParameter("dateTo", to);
        final List<CurrencyExchangeDTO> results = query.list();
        if(results.isEmpty()) {
            return null;
        }
        return  results.get(0);
    }


}
