package com.sapienter.jbilling.common;

/**
 * Created with IntelliJ IDEA.
 * User: aristokrates
 * Date: 1/23/13
 * Time: 11:40 AM
 * To change this template use File | Settings | File Templates.
 */

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "errorMessages"
})
@XmlRootElement(name = "SessionJAXBInternalError")
public class SessionJAXBInternalError {

    @XmlElement(required = true)
    private String errorMessages[] = null;

    public String[] getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(String[] errorMessages) {
        this.errorMessages = errorMessages;
    }
}


