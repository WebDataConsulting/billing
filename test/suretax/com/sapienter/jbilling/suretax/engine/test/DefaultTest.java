package com.sapienter.jbilling.suretax.engine.test;

import java.util.Hashtable;

import junit.framework.TestCase;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.task.SureTaxCompositionTask;
import com.sapienter.jbilling.server.process.task.SuretaxDeleteInvoiceTask;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

public abstract class DefaultTest extends TestCase {
	private static final Integer SURETAX_COMPOSITION_TASK_TYPE_ID = 105;
	private static final Integer SURETAX_COMPOSITION_CANCEL_TASK_TYPE_ID = 106;
	public static final String PLUGIN_PARAM_SURETAX_CLIENT_NUMBER = "000000350";
	public static final String PLUGIN_PARAM_SURETAX_VALIDATION_KEY = "a4be5e95-1034-4b73-a9d5-23ba4b4ca2e7";
	public static final String PLUGIN_PARAM_DATA_YEAR = "2012";
	public static final String PLUGIN_PARAM_DATA_MONTH = "7";
	public static final String SURETAX_TESTAPI_POST_URL = "https://testapi.taxrating.net/Services/V01/SureTax.asmx/PostRequest";
	public static final String SURETAX_TESTAPI_CANCEL_POST_URL = "https://testapi.taxrating.net/Services/V01/SureTax.asmx/CancelPostRequest ";
	public static final String PLUGIN_PARAM_SURETAX_ROLLBACK_ON_ERROR = "1";
	public JbillingAPI api = null;
	Integer suretaxPluginId;
	Integer suretaxDeletePluginId;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		api = JbillingAPIFactory.getAPI();

		// Setting up the suretax plugin
		PluggableTaskWS sureTaxPlugin = new PluggableTaskWS();
		sureTaxPlugin.setTypeId(SURETAX_COMPOSITION_TASK_TYPE_ID);
		sureTaxPlugin.setProcessingOrder(3);

		// These are mandatory parameters that need to be set in the plug-in
		Hashtable<String, String> parameters = new Hashtable<String, String>();
		parameters.put(SureTaxCompositionTask.SURETAX_REQUEST_URL,
				SURETAX_TESTAPI_POST_URL);
		parameters.put(SureTaxCompositionTask.CLIENT_NUMBER,
				PLUGIN_PARAM_SURETAX_CLIENT_NUMBER);
		parameters.put(SureTaxCompositionTask.VALIDATION_KEY,
				PLUGIN_PARAM_SURETAX_VALIDATION_KEY);
		parameters.put(SureTaxCompositionTask.DATA_YEAR,
				PLUGIN_PARAM_DATA_YEAR);
		parameters.put(SureTaxCompositionTask.DATA_MONTH,
				PLUGIN_PARAM_DATA_MONTH);
		// Set Optional parameters
		if (getOptionalParametersForSuretaxPlugin() != null) {
			for (String key : getOptionalParametersForSuretaxPlugin().keySet()) {
				parameters.put(key, getOptionalParametersForSuretaxPlugin()
						.get(key));
			}
		}
		sureTaxPlugin.setParameters(parameters);
		try {
			suretaxPluginId = api.createPlugin(sureTaxPlugin);
		} catch (Exception e) {
			if (e instanceof SessionInternalError
					&& e.getMessage().contains("Validation of new plug-in")) {
				// do nothing
				System.out.println("Error in creating sureTaxPlugin: " + e.getMessage());
			} else {
				System.out.println("Exception in creating sureTaxPlugin");
				throw new RuntimeException(e);
			}
		}

		// Setting up the suretax delete plugin
		PluggableTaskWS sureTaxDeletePlugin = new PluggableTaskWS();
		sureTaxDeletePlugin.setTypeId(SURETAX_COMPOSITION_CANCEL_TASK_TYPE_ID);
		sureTaxDeletePlugin.setProcessingOrder(8);

		// These are mandatory parameters that need to be set in the plug-in
		parameters = new Hashtable<String, String>();
		parameters.put(SuretaxDeleteInvoiceTask.SURETAX_DELETE_REQUEST_URL,
				SURETAX_TESTAPI_CANCEL_POST_URL);
		parameters.put(SuretaxDeleteInvoiceTask.CLIENT_NUMBER,
				PLUGIN_PARAM_SURETAX_CLIENT_NUMBER);
		parameters.put(SuretaxDeleteInvoiceTask.VALIDATION_KEY,
				PLUGIN_PARAM_SURETAX_VALIDATION_KEY);
		// Set Optional parameters
		if (getOptionalParametersForSuretaxDeletePlugin() != null) {
			for (String key : getOptionalParametersForSuretaxDeletePlugin()
					.keySet()) {
				parameters.put(key,
						getOptionalParametersForSuretaxDeletePlugin().get(key));
			}
		}
		sureTaxDeletePlugin.setParameters(parameters);
		try {
			suretaxDeletePluginId = api.createPlugin(sureTaxDeletePlugin);
		} catch (Exception e) {
			if (e instanceof SessionInternalError
					&& e.getMessage().contains("Validation of new plug-in")) {
				// do nothing
				System.out.println("Error in creating sureTaxDeletePlugin: " + e.getMessage());
			} else {
				System.out.println("Exception in creating sureTaxDeletePlugin");
				throw new RuntimeException(e);
			}
		}

	}

	protected abstract Hashtable<String, String> getOptionalParametersForSuretaxPlugin();

	protected abstract Hashtable<String, String> getOptionalParametersForSuretaxDeletePlugin();

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (suretaxPluginId != null) {
			api.deletePlugin(suretaxPluginId);
		}
		if (suretaxDeletePluginId != null) {
			api.deletePlugin(suretaxDeletePluginId);
		}
	}
}
