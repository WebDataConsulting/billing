package com.sapienter.jbilling.common;

/**
* Session internal error messages
 * </p>
 * Provides the error messages returned from the jBIlling API
*
* @author: Panche.Isajeski
* @since: 01/23/13
*/
import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "errorMessages"
})
@XmlRootElement(name = "SessionInternalErrorMessages")
public class SessionInternalErrorMessages implements Serializable {

    @XmlElement(required = true)
    private String errorMessages[] = null;

    public String[] getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(String[] errorMessages) {
        this.errorMessages = errorMessages;
    }
}


