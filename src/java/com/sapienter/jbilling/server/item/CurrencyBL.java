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

package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.JNDILookup;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.CurrencyWS;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.db.CurrencyExchangeDAS;
import com.sapienter.jbilling.server.util.db.CurrencyExchangeDTO;

import org.springmodules.cache.CachingModel;
import org.springmodules.cache.FlushingModel;
import org.springmodules.cache.provider.CacheProviderFacade;

import javax.naming.NamingException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Emil
 */
public class CurrencyBL {
    private static final FormatLogger LOG = new FormatLogger(CurrencyBL.class);


    private static final Integer SYSTEM_RATE_ENTITY_ID = 0;
    private static final Integer SYSTEM_CURRENCY_ID = 1;
    private static final BigDecimal SYSTEM_CURRENCY_RATE_DEFAULT = new BigDecimal("1.0");

    private CurrencyDAS currencyDas = null;
    private CurrencyExchangeDAS exchangeDas = null;

    // cache management
    private CacheProviderFacade cache;
    private CachingModel cacheModel;
    private FlushingModel flushModel;

    private CurrencyDTO currency = null;

    public CurrencyBL() {
        currencyDas = new CurrencyDAS();
        exchangeDas = new CurrencyExchangeDAS();

        cache = (CacheProviderFacade) Context.getBean(Context.Name.CACHE);
        cacheModel = (CachingModel) Context.getBean(Context.Name.CURRENCY_CACHE_MODEL);
        flushModel = (FlushingModel) Context.getBean(Context.Name.CURRENCY_FLUSH_MODEL);
    }

    public CurrencyBL(Integer currencyId) {
        this();
        set(currencyId);
    }

    public CurrencyBL(CurrencyDAS currencyDas, CurrencyExchangeDAS exchangeDas) {
        this.currencyDas = currencyDas;
        this.exchangeDas = exchangeDas;
    }

    public void set(Integer id) {
        currency = currencyDas.find(id);
    }

    public CurrencyDTO getEntity() {
        return currency;
    }
    	
    public static final CurrencyWS getCurrencyWS(CurrencyDTO dto,boolean defaultCurrency){
    	
		CurrencyWS ws = new CurrencyWS();
		ws.setId(dto.getId());
		ws.setDescription(dto.getDescription());
		ws.setSymbol(dto.getSymbol());
		ws.setCode(dto.getCode());
		ws.setCountryCode(dto.getCountryCode());
		ws.setInUse(dto.getInUse());

		ws.setRate(dto.getRate());
		ws.setSysRate(dto.getSysRate());

		ws.setDefaultCurrency(defaultCurrency);
		return ws;
    }
    
    
    public Integer create(CurrencyDTO dto, Integer entityId) {
        if (dto != null) {
            /*
                Simplify currency creation; Set exchange rates from transient CurrencyDTO#getRate() and
                CurrencyDTO#getSysRate() if no currency exchanges have been mapped.
             */
            if (dto.getCurrencyExchanges().isEmpty()) {
                // set system rate
                CurrencyExchangeDTO sysRate = new CurrencyExchangeDTO();
                sysRate.setEntityId(SYSTEM_RATE_ENTITY_ID);
                sysRate.setRate(dto.getSysRate() != null ? dto.getSysRate() : BigDecimal.ONE);
                sysRate.setValidSince(com.sapienter.jbilling.common.Util.truncateDate(CommonConstants.EPOCH_DATE));
                sysRate.setCurrency(dto);

                dto.addCurrencyExchange(sysRate);
            }

            this.currency = currencyDas.save(dto);

            // add active currencies to the company map
            if (dto.getInUse()) {
                CompanyDTO company = new CompanyDAS().find(entityId);
                company.addCurrency(this.currency);
            }

            invalidateCache();

            return this.currency.getId();
        }

        LOG.error("Cannot save a null CurrencyDTO!");
        return null;
    }

    public void update(CurrencyDTO dto, Integer entityId) throws SessionInternalError {    	    	
        if (currency != null) {
            currency.setSymbol(dto.getSymbol());
            currency.setCode(dto.getCode());
            currency.setCountryCode(dto.getCountryCode());

            // set system rate
            if (dto.getSysRate() != null) {
                final CurrencyExchangeDTO systemExchangeRate = findExchange(SYSTEM_RATE_ENTITY_ID, currency.getId(), new Date());
                systemExchangeRate.setRate(dto.getSysRate());
            }

            // add active currencies to the company map
            CompanyDTO company = new CompanyDAS().find(entityId);
            if (dto.getInUse()) {
                company.addCurrency(currency);
            } else {
                company.removeCurrency(currency);
            }

            invalidateCache();

        } else {
            LOG.error("Cannot update, CurrencyDTO not found or not set!");
        }
    }
    
    public boolean delete() {
        if (currency != null && currency.isDeletable()) {
        	currency.getEntities_1().clear();
        	currencyDas.delete(currency);
            invalidateCache();
        	return true;
        } else {
            LOG.error("Cannot delete, CurrencyDTO not found or not deletable!");
        }
        
        return false;
    }

    public CurrencyExchangeDTO setOrUpdateExchangeRate(BigDecimal amount, Integer entityId, Date dateFrom) {
        final Date truncatedToDay = com.sapienter.jbilling.common.Util.truncateDate(dateFrom);
        CurrencyExchangeDTO exchangeRate = exchangeDas.getExchangeRateForRange(entityId, currency.getId(), truncatedToDay, dateFrom);

        if(amount == null) {
            if(exchangeRate != null) {
                // remove record
                exchangeDas.delete(exchangeRate);
                currency.removeCurrencyExchange(exchangeRate);
            }
            return null;
        }

        if (exchangeRate == null) {
            exchangeRate = new CurrencyExchangeDTO();
            exchangeRate.setEntityId(entityId);
            exchangeRate.setValidSince(truncatedToDay);
            exchangeRate.setCurrency(currency);
            currency.addCurrencyExchange(exchangeRate);
        }

        exchangeRate.setRate(amount);
        invalidateCache();
        return exchangeRate;
    }

    @SuppressWarnings("unchecked")
    public CurrencyDTO[] getCurrencies(Integer languageId, Integer entityId) throws NamingException, SQLException {
        final List<CurrencyDTO> currencies = getCurrenciesToDate(languageId, entityId, new Date());
        return currencies.toArray(new CurrencyDTO[currencies.size()]);
    }
    
    //use it when you need only currency objects
    public List<CurrencyDTO> getCurrenciesWithoutRates(Integer languageId, Integer entityId, boolean inUseOnly) throws NamingException, SQLException {

        String cacheKey ="without rates "+ getCacheKey(languageId, entityId, new Date());
        List<CurrencyDTO> cachedCurrencies = (List<CurrencyDTO>)cache.getFromCache(cacheKey, cacheModel);

        if (cachedCurrencies != null && !cachedCurrencies.isEmpty()) {
            LOG.debug("Cache hit for %s", cacheKey);
            return cachedCurrencies;
        }

        List<CurrencyDTO> currencies = new CurrencyDAS().findAll();

        boolean inUse;
        for (CurrencyDTO currency : currencies) {
            set(currency.getId());
			inUse = entityHasCurrency(entityId, currency.getId());
			if(inUseOnly&&!inUse)
				continue;
            currency.setInUse(inUse);
            currency.setName(this.currency.getDescription(languageId));

            // find system rate
            if (currency.getId() == SYSTEM_CURRENCY_ID.intValue()) {
                currency.setSysRate(SYSTEM_CURRENCY_RATE_DEFAULT);
            } 
        }
        
        cache.putInCache(cacheKey, cacheModel, currencies);
        return currencies;
    }
 

    public List<CurrencyDTO> getCurrenciesToDate(Integer languageId, Integer entityId, Date to) throws NamingException, SQLException {

        String cacheKey = getCacheKey(languageId, entityId, to);
        List<CurrencyDTO> cachedCurrencies = (List<CurrencyDTO>)cache.getFromCache(cacheKey, cacheModel);

        if (cachedCurrencies != null && !cachedCurrencies.isEmpty()) {
            LOG.debug("Cache hit for %s", cacheKey);
            return cachedCurrencies;
        }

        List<CurrencyDTO> currencies = new CurrencyDAS().findAll();

        for (CurrencyDTO currency : currencies) {
            set(currency.getId());
            currency.setName(this.currency.getDescription(languageId));

            // find system rate
            if (currency.getId() == SYSTEM_CURRENCY_ID.intValue()) {
                currency.setSysRate(SYSTEM_CURRENCY_RATE_DEFAULT);
            } else {
                final CurrencyExchangeDTO exchangeRateForDate = findExchange(SYSTEM_RATE_ENTITY_ID, currency.getId(), to);
                currency.setSysRate(exchangeRateForDate.getRate());
            }

            // find entity specific rate
            CurrencyExchangeDTO exchange = exchangeDas.getExchangeRateForDate(entityId, currency.getId(), to);
            if (exchange != null) {
                currency.setRate(exchange.getRate().toString());
            }

            // set in-use flag
            currency.setInUse(entityHasCurrency(entityId, currency.getId()));
        }
        
        cache.putInCache(cacheKey, cacheModel, currencies);

        return currencies;
    }
    
    

    public void setCurrencies(Integer entityId, CurrencyDTO[] currencies) throws NamingException, ParseException {
        EntityBL entity = new EntityBL(entityId);

        // start by wiping out the existing data for this entity
        entity.getEntity().getCurrencies().clear();
        for (CurrencyExchangeDTO exchange : exchangeDas.findByEntity(entityId)) {
            exchangeDas.delete(exchange);
        }

        for (CurrencyDTO currency : currencies) {
            if (currency.getInUse()) {
                set(currency.getId());
                entity.getEntity().addCurrency(new CurrencyDAS().find(this.currency.getId()));

                if (currency.getRate() != null) {
                    CurrencyExchangeDTO exchange = setOrUpdateExchangeRate(currency.getRateAsDecimal(), entityId, new Date());
                    exchangeDas.save(exchange);
                }
            }
        }

        invalidateCache();
    }

    public static List<Date> getUsedTimePoints(Integer entityId) {
        List<Date> result = new ArrayList<Date>();
        final List<CurrencyExchangeDTO> companyExchanges = new CurrencyExchangeDAS().findByEntity(entityId);
        for (CurrencyExchangeDTO exchange : companyExchanges) {
            if (entityId.equals(exchange.getEntityId())) {
                final Date validSince = exchange.getValidSince();
                final Date validSinceRoundedToDate = com.sapienter.jbilling.common.Util.truncateDate(validSince);
                if(!result.contains(validSinceRoundedToDate)) {
                    result.add(validSinceRoundedToDate);
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Removes exchange data for specified date
     */
    public void removeExchangeRatesForDate(Integer entityId, Date date) {
        final Date dayStart = com.sapienter.jbilling.common.Util.truncateDate(date);

        final CurrencyExchangeDAS exchangeDAS = new CurrencyExchangeDAS();
        final List<CurrencyExchangeDTO> companyExchanges = exchangeDAS.findByEntity(entityId);
        for (CurrencyExchangeDTO exchange : companyExchanges) {
            if (entityId.equals(exchange.getEntityId())) {
                final Date validSince = exchange.getValidSince();
                if (dayStart.equals(com.sapienter.jbilling.common.Util.truncateDate(validSince))) {
                    exchangeDAS.delete(exchange);
                }
            }
        }
        /* @todo Konstantin Kulagin this should be in controller	*/
        exchangeDAS.flush();
        invalidateCache();
    }

    public void invalidateCache() {
        LOG.debug("Invalidating currency cache");
        cache.flushCache(flushModel);
    }

    public static Integer getEntityCurrency(Integer entityId) {
        CompanyDTO entity = new CompanyDAS().find(entityId);
        return entity.getCurrencyId();
    }

    public static void setEntityCurrency(Integer entityId, Integer currencyId) {
        CompanyDTO entity = new CompanyDAS().find(entityId);
        entity.setCurrency(new CurrencyDAS().find(currencyId));
    }

    /*
        Currency conversion
     */
    public BigDecimal convert(Integer fromCurrencyId, Integer toCurrencyId, BigDecimal amount, Date toDate, Integer entityId)
            throws SessionInternalError {

        LOG.debug("Converting %s to %s , amount %s ,entity %s", fromCurrencyId, toCurrencyId, amount, entityId);
        if (fromCurrencyId.equals(toCurrencyId)) {
            return amount; // mmm.. no conversion needed
        }
        
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;	// mmm.. conversion doth not make sense
        }

        // make the conversions
        final BigDecimal pivotAmount = convertToPivot(fromCurrencyId, amount, toDate, entityId);
        LOG.debug("Pivot Amount %s", pivotAmount);
        return convertPivotToCurrency(toCurrencyId, pivotAmount, toDate, entityId);
    }

    /**
     * Converts all currencies to Pivot currency i.e. 1
     * @param currencyId
     * @param amount
     * @param toDate
     * @param entityId
     * @return
     * @throws SessionInternalError
     */
    private BigDecimal convertToPivot(Integer currencyId, BigDecimal amount, Date toDate, Integer entityId) throws SessionInternalError {
        if (currencyId.equals(SYSTEM_CURRENCY_ID)) {
        	LOG.debug("this currency is already in the pivot");
            return amount; 
        }

        // make the conversion itself
        CurrencyExchangeDTO exchange = findExchange(entityId, currencyId, toDate);
        return amount.divide(exchange.getRate(), ServerConstants.BIGDECIMAL_SCALE, ServerConstants.BIGDECIMAL_ROUND);
    }

    private BigDecimal convertPivotToCurrency(Integer currencyId, BigDecimal amount, Date toDate,
                                              Integer entityId) throws SessionInternalError {
        if (currencyId.equals(SYSTEM_CURRENCY_ID)) {
            return amount; // this is already in the pivot
        }
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        CurrencyExchangeDTO exchange = findExchange(entityId, currencyId, toDate);

        // make the conversion itself
        return amount.multiply(exchange.getRate()).setScale(ServerConstants.BIGDECIMAL_SCALE, ServerConstants.BIGDECIMAL_ROUND);
    }

    private CurrencyExchangeDTO findExchange(Integer entityId, Integer currencyId, Date toDate) throws SessionInternalError {
    	
        // check for system currency exchange
        if (SYSTEM_CURRENCY_ID.equals(currencyId)) {
        	return new CurrencyExchangeDTO(0, currency, entityId, SYSTEM_CURRENCY_RATE_DEFAULT, new Date());
        }
        LOG.debug("Get exchange rate for %s for entity %s for date %s", currencyId, entityId, toDate);
    	
        CurrencyExchangeDTO exchange = exchangeDas.getExchangeRateForDate(entityId, currencyId, toDate);
        if (exchange == null) {
            // this entity doesn't have this exchange defined
            // 0 is the default, don't try to use null, it won't work
            exchange = exchangeDas.findExchange(SYSTEM_RATE_ENTITY_ID, currencyId);
            if (exchange == null) {
                throw new SessionInternalError("Currency " + currencyId + " doesn't have a default exchange");
            }
        }
        LOG.debug("Exchange found %s", exchange.getId());
        return exchange;
    }

    private String getCacheKey(Integer languageId, Integer entityId, Date date)  {
        return "currency language" + languageId + "entity:" + entityId + "date:"
                + com.sapienter.jbilling.common.Util.truncateDate(date);
    }

    /**
     * Ok, this is cheating, but heck is easy and fast.
     *
     * @param entityId
     * @param currencyId
     * @return
     * @throws SQLException
     * @throws NamingException
     */
    private static boolean entityHasCurrency(Integer entityId, Integer currencyId) throws SQLException, NamingException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            JNDILookup jndi = JNDILookup.getFactory();
            conn = jndi.lookUpDataSource().getConnection();
            stmt = conn.prepareStatement(
                    "select 1 " +
                            "  from currency_entity_map " +
                            " where currency_id = ? " +
                            "   and entity_id = ?");

            stmt.setInt(1, currencyId);
            stmt.setInt(2, entityId);
            result = stmt.executeQuery();
            return result.next();
        } finally {
            Util.closeQuietly(result);
            Util.closeQuietly(stmt);
            Util.closeQuietly(conn);
        }
    }
}
