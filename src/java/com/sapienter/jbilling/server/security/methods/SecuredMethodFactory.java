package com.sapienter.jbilling.server.security.methods;

import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: bcowdery
 * Date: 14/05/12
 * Time: 10:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class SecuredMethodFactory {

    private static final Class WS_INTERFACE = IWebServicesSessionBean.class;

    private static final Map<String, SecuredMethodSignature> SECURED_METHODS = new HashMap<String, SecuredMethodSignature>();

    public static void add(String methodName, int idArgIndex, SecuredMethodType type) {
        for (Method method : WS_INTERFACE.getMethods()) {
            if (method.getName().equals(methodName)) {
                SECURED_METHODS.put(methodName, new SecuredMethodSignature(method, idArgIndex, type));
                return;
            }
        }

        throw new IllegalArgumentException("Method '" + methodName + "' does not exist on " + WS_INTERFACE);
    }

    public static SecuredMethodSignature getSignature(Method method) {
        return getSignatureByMethodName(method.getName());
    }

    public static SecuredMethodSignature getSignatureByMethodName(String methodName) {
        return SECURED_METHODS.get(methodName);
    }

}
