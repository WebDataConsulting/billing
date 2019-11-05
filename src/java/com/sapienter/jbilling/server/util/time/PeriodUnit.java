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

package com.sapienter.jbilling.server.util.time;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.PersistentEnum;

/**
 * Utility class for working with period units. Can be persisted by Hibernate.
 *
 * @author Igor Poteryaev <igor.poteryaev@jbilling.com>
 * @since 2015-04-10
 *
 */
public enum PeriodUnit implements PersistentEnum {

    MONTHLY(1) {
        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {
            return temporal.plusMonths(amount);
        }
    },
    WEEKLY(2) {
        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {
            return temporal.plusWeeks(amount);
        }
    },
    DAYLY(3) {
        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {
            return temporal.plusDays(amount);
        }
    },
    ANNUAL(4) {
        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {
            return temporal.plusYears(amount);
        }
    },
    SEMI_MONTHLY(5) {
        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {

            int initialDay = temporal.getDayOfMonth();
            boolean inLastHalfOfMonth = initialDay > 15;
            LocalDate result = temporal.plusMonths(amount / 2);

            if (amount % 2 != 0) {
                if (inLastHalfOfMonth && (amount > 0)) {
                    result = result.plusMonths(1);
                } else if (!inLastHalfOfMonth && (amount < 0)) {
                    result = result.minusMonths(1);
                }
                result = (inLastHalfOfMonth) ? result.withDayOfMonth(initialDay - 15) : result.withDayOfMonth(Math.min(
                        result.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth(), initialDay + 15));
            }
            return result;
        }

        @Override
        public void validate (LocalDate temporal) {
            int dayOfMonth = temporal.getDayOfMonth();
            if (dayOfMonth == 31 || dayOfMonth == 30) {
                throw new IllegalArgumentException("Day of month in [" + temporal + "] can't exceed 29");
            }
            if (dayOfMonth == 15) {
                throw new IllegalArgumentException("Day of month in [" + temporal + "] can't be equal to 15");
            }
        }
    },
    SEMI_MONTHLY_EOM(6) {

        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {

            boolean endOfMonth = isTheLastDayOfMonth(temporal);
            LocalDate result = temporal.plusMonths(amount / 2);

            if (amount % 2 == 0) {
                if (endOfMonth) {
                    result = result.with(TemporalAdjusters.lastDayOfMonth());
                }
            } else {
                if (endOfMonth && (amount > 0)) {
                    result = result.plusMonths(1);
                } else if (!endOfMonth && (amount < 0)) {
                    result = result.minusMonths(1);
                }
                result = (endOfMonth) ? result.withDayOfMonth(15) : result.with(TemporalAdjusters.lastDayOfMonth());
            }
            return result;
        }

        private boolean isTheLastDayOfMonth (LocalDate temporal) {
            return temporal.equals(temporal.with(TemporalAdjusters.lastDayOfMonth()));
        }

        @Override
        public void validate (LocalDate temporal) {
            if ((temporal.getDayOfMonth() == 15) || isTheLastDayOfMonth(temporal)) {
                return;
            }
            throw new IllegalArgumentException("Day of month in [" + temporal + "] should be 15 or the end of month");
        }
    }; // , BI_WEEKLY(7), QUATERLY(8), SEMI_ANNUAL(9);

    private final int id;

    PeriodUnit (int id) {
        this.id = id;
    }

    @Override
    public int getId () {
        return id;
    }

    public LocalDate addTo (LocalDate temporal, long amount) {
        validate(temporal);
        return _addTo(temporal, amount);
    }

    protected abstract LocalDate _addTo (LocalDate temporal, long amount);

    public void validate (LocalDate temporal) throws IllegalArgumentException {
    }

    /**
     * Finds the type of PeriodUnit from its start day of month and period unit identifier
     * 
     * @param dayOfMonth     period start day of month
     * @param periodUnitId   period unit identifier
     * @return 
     */
    public static PeriodUnit valueOfPeriodUnit (int dayOfMonth, int periodUnitId) {

        if (periodUnitId == ServerConstants.PERIOD_UNIT_MONTH) {
            return PeriodUnit.MONTHLY;
        } else if (periodUnitId == ServerConstants.PERIOD_UNIT_WEEK) {
            return PeriodUnit.WEEKLY;
        } else if (periodUnitId == ServerConstants.PERIOD_UNIT_DAY) {
            return PeriodUnit.DAYLY;
        } else if (periodUnitId == ServerConstants.PERIOD_UNIT_YEAR) {
            return PeriodUnit.ANNUAL;
        } else if (periodUnitId == ServerConstants.PERIOD_UNIT_SEMI_MONTHLY) {
            return (dayOfMonth == 15) ? PeriodUnit.SEMI_MONTHLY_EOM : PeriodUnit.SEMI_MONTHLY;
        }

        throw new IllegalArgumentException("Unsupported PeriodUnitDTO id[" + periodUnitId + "]");
    }
}
