package com.sapienter.jbilling.server.security.methods;

import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

import java.lang.reflect.Method;

/**
 * Created by IntelliJ IDEA.
 * User: bcowdery
 * Date: 14/05/12
 * Time: 9:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class SecuredMethodSignature {

    private Method method;
    private Integer IdArgIndex;
    private SecuredMethodType type;

    public SecuredMethodSignature(Method method, Integer idArgIndex, SecuredMethodType type) {
        this.method = method;
        this.IdArgIndex = idArgIndex;
        this.type = type;
    }

    public Method getMethod() {
        return method;
    }

    public Integer getIdArgIndex() {
        return IdArgIndex;
    }

    public SecuredMethodType getType() {
        return type;
    }
}


