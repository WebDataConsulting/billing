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

import com.sapienter.jbilling.server.BigDecimalTestCase;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyExchangeDAS;
import com.sapienter.jbilling.server.util.db.CurrencyExchangeDTO;

import java.math.BigDecimal;
import java.util.Date;

import static org.easymock.classextension.EasyMock.*;

/**
 * @author Brian Cowdery
 * @since 29-04-2010
 */
public class CurrencyBLTest extends BigDecimalTestCase {

    private static final Integer ENTITY_ID = 1;

    // mocks
    private CurrencyDAS mockCurrencyDas = createMock(CurrencyDAS.class);
    private CurrencyExchangeDAS mockExchangeDas = createMock(CurrencyExchangeDAS.class);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        reset(mockCurrencyDas, mockExchangeDas);
    }

    /**
     * Construct a mock CurrencyExchangeDTO for return from the CurrencyExchangeDAS
     * @param rate exchange rate
     * @return mock
     */
    private static CurrencyExchangeDTO _mockCurrencyExchangeDTO(String rate) {
        CurrencyExchangeDTO dto = new CurrencyExchangeDTO();
        dto.setRate(new BigDecimal(rate));
        return dto;
    }

    public void testConvert() throws Exception {
        final Date date = new Date();
        expect(mockExchangeDas.getExchangeRateForDate(ENTITY_ID, 200, date)).andReturn(_mockCurrencyExchangeDTO("0.98")).once();
        expect(mockExchangeDas.getExchangeRateForDate(ENTITY_ID, 201, date)).andReturn(_mockCurrencyExchangeDTO("1.20")).once();
        replay(mockCurrencyDas, mockExchangeDas);

        // convert $20.00 CAD to AUD - approximated conversion rates ;)
        CurrencyBL bl = new CurrencyBL(mockCurrencyDas, mockExchangeDas);
        BigDecimal amount = bl.convert(200, 201, new BigDecimal("20.00"), date, ENTITY_ID);

        verify(mockCurrencyDas, mockExchangeDas);

        assertEquals(new BigDecimal("24.48979"), amount);
    }

    public void testConvertRepeatingDecimal() throws Exception {
        final Date date = new Date();
        expect(mockExchangeDas.getExchangeRateForDate(ENTITY_ID, 200, date)).andReturn(_mockCurrencyExchangeDTO("3.00")).once();
        expect(mockExchangeDas.getExchangeRateForDate(ENTITY_ID, 201, date)).andReturn(_mockCurrencyExchangeDTO("1.00")).once();
        replay(mockCurrencyDas, mockExchangeDas);

        /*
            Pivot calculation will result in a value of 3.333333~ repeating, which will
            cause an ArithmeticException if not handled correctly.

            10.00 / 3.00 = 3.333333~
         */
        CurrencyBL bl = new CurrencyBL(mockCurrencyDas, mockExchangeDas);
        BigDecimal amount = bl.convert(200, 201, new BigDecimal("10.00"), date, ENTITY_ID);

        verify(mockCurrencyDas, mockExchangeDas);

        assertEquals(new BigDecimal("3.33"), amount);
    }


}
