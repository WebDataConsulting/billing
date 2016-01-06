package com.sapienter.jbilling.server.item.validator;

import javax.validation.Payload;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: 10/11/12
 * Time: 6:25 PM
 * To change this template use File | Settings | File Templates.
 */
public @interface NonNegative {
    String message() default "validation.error.missing.price";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
