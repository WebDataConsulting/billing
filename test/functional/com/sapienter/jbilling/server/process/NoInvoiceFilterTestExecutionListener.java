package com.sapienter.jbilling.server.process;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

public class NoInvoiceFilterTestExecutionListener extends AbstractTestExecutionListener {

    private static final Log logger                       = LogFactory
                                                                  .getLog(NoInvoiceFilterTestExecutionListener.class);

    private Integer          noInvoiceFilterTaskId;
    private JbillingAPI      api;

    private static final int NO_INVOICE_FILTER_TASK_ID    = 14;
    private static final int NO_INVOICE_FILTER_TASK_ORDER = 99;

    @Override
    public void afterTestClass (TestContext testContext) throws Exception {
        disableNoInvoiceFilterTask(noInvoiceFilterTaskId);
        super.afterTestClass(testContext);
    }

    @Override
    public void beforeTestClass (TestContext testContext) throws Exception {
        super.beforeTestClass(testContext);
        api = (JbillingAPI) testContext.getApplicationContext().getBean("api");
        noInvoiceFilterTaskId = enableNoInvoiceFilterTask(NO_INVOICE_FILTER_TASK_ID);
    }

    private Integer enableNoInvoiceFilterTask (Integer pluginTypeId) {
        PluggableTaskWS plugin = new PluggableTaskWS();
        plugin.setTypeId(pluginTypeId);
        plugin.setProcessingOrder(NO_INVOICE_FILTER_TASK_ORDER);
        Integer pluginId = api.createPlugin(plugin);
        logger.debug("Enabling plugin[" + pluginId + "]");
        return pluginId;
    }

    private void disableNoInvoiceFilterTask (Integer pluginId) {
        if (pluginId != null) {
            logger.debug("Disabling plugin[" + pluginId + "]");
            api.deletePlugin(pluginId);
        }
    }
}
