package com.sapienter.jbilling.server.util;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;

/**
 * This is a utility class that can be used in debugging scenarios to
 * log or just find out more information about the current active transaction
 * and/or hibernate session bound to the current thread.
 *
 * The TransactionInfoUtil#getTransactionStatus can also be used in a Log4J
 * filter to add a +/- sing to every log signaling if there is current active
 * transaction or not. This could be useful debugging information. For more
 * details: http://java.dzone.com/articles/monitoring-declarative-transac
 *
 */
public class TransactionInfoUtil {

    private final static String TSM_CLASSNAME = "org.springframework.transaction.support.TransactionSynchronizationManager";

    public static String getTransactionStatus(boolean verbose) {
        String status = null;

        try {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                Class tsmClass = contextClassLoader.loadClass(TSM_CLASSNAME);
                Boolean isActive = (Boolean) tsmClass.getMethod("isActualTransactionActive").invoke(null);
                if (!verbose) {
                    status = (isActive) ? "[+] " : "[-] ";
                } else {
                    String transactionName = (String) tsmClass.getMethod("getCurrentTransactionName").invoke(null);
                    status = (isActive) ? "[" + transactionName + "] " : "[no transaction] ";
                }
            } else {
                status = (verbose) ? "[ccl unavailable] " : "[x ]";
            }
        } catch (Exception e) {
            status = (verbose) ? "[spring unavailable] " : "[x ]";
        }
        return status;
    }

    /**
     * Retrieves the current session bound to the running thread and generates info string.
     * Never creates a new session if one does not exists. Throws exception is a session does
     * not exists and this is called outside of the transaction boundaries.
     *
     * @return - information about the current running session
     */
    public static String getCurrentSessionInfo() {
        SessionFactory sessionFactory = Context.getBean(Context.Name.HIBERNATE_SESSION);
        StringBuilder builder = new StringBuilder();
        Session session = sessionFactory.getCurrentSession();
        builder.append("hash=").append(session.hashCode()).append("; ");
        builder.append("connected=").append(session.isConnected()).append("; ");
        builder.append("open=").append(session.isOpen()).append("; ");
        builder.append("dirty=").append(session.isDirty()).append("; ");
        builder.append("flush_mode=").append(session.getFlushMode().toString());
        return builder.toString();
    }

}