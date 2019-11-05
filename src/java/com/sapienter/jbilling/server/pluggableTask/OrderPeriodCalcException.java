package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.SessionInternalError;

/**
 * This exception gets thrown when there is an error while calculating order periods for orders.
 *
 * @author Gerhard Maree
 * @since 27/09/13
 */
public class OrderPeriodCalcException extends SessionInternalError {

    public OrderPeriodCalcException() {
    }

    public OrderPeriodCalcException(String message) {
        super(message);
    }

    public OrderPeriodCalcException(String message, Throwable cause) {
        super(message, cause);
    }

}
