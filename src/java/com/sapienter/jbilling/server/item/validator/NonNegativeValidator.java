package com.sapienter.jbilling.server.item.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: 10/11/12
 * Time: 6:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class NonNegativeValidator implements ConstraintValidator<NonNegative, String>, Serializable {
    @Override
    public void initialize(NonNegative nonNegative) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isValid(String number, ConstraintValidatorContext constraintValidatorContext) {
        Integer num = Integer.parseInt(number);  
        System.out.println("+++++++++++++++++++++++"+num);
        if(num < 0){
            return false;
        }
        else{
            return false;
        }
    }
}
