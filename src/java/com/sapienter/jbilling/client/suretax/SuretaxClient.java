package com.sapienter.jbilling.client.suretax;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.sapienter.jbilling.common.FormatLogger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import com.sapienter.jbilling.client.suretax.request.ItemList;
import com.sapienter.jbilling.client.suretax.request.LineItem;
import com.sapienter.jbilling.client.suretax.request.SuretaxCancelRequest;
import com.sapienter.jbilling.client.suretax.request.SuretaxRequest;
import com.sapienter.jbilling.client.suretax.response.SuretaxCancelResponse;
import com.sapienter.jbilling.client.suretax.response.SuretaxResponse;
import com.sapienter.jbilling.server.invoice.db.SuretaxTransactionLogDAS;
import com.sapienter.jbilling.server.invoice.db.SuretaxTransactionLogDTO;

public class SuretaxClient {

	FormatLogger log = new FormatLogger(SuretaxClient.class);

	// public static final String SURETAX_TESTAPI_POST_URL =
	// "https://testapi.taxrating.net/Services/V01/SureTax.asmx/PostRequest";
	// public static final String SURETAX_TESTAPI_CANCEL_POST_URL =
	// "https://testapi.taxrating.net/Services/V01/SureTax.asmx/CancelPostRequest ";
	// public static final String SURETAX_POST_URL =
	// "https://testapi.taxrating.net/Services/V01/SureTax.asmx/PostRequest";
	// public static final String SURETAX_CANCEL_POST_URL =
	// "https://testapi.taxrating.net/Services/V01/SureTax.asmx/CancelPostRequest ";

	public SuretaxResponse getResponse(SuretaxRequest request, String url) {
		try {
			HttpClient client = new HttpClient();
			PostMethod post = new PostMethod(url);

			ObjectMapper mapper = new ObjectMapper();
			String jsonRequestString = mapper.writeValueAsString(request);
			log.debug("Sending suretax json request string: %s",
					  jsonRequestString);

			post.addParameter("request", jsonRequestString);

			int respCode = client.executeMethod(post);
			String response = post.getResponseBodyAsString();
			SAXReader saxReader = new SAXReader();
			Document doc = saxReader.read(new StringReader(response));
			log.debug("Suretax response code: %s, Suretax response json string: %s ", respCode, 
					  doc.getRootElement().getText());
			SuretaxResponse stResponse = mapper.readValue(doc.getRootElement()
					.getText(), SuretaxResponse.class);
			stResponse.setJsonString(doc.getRootElement().getText());

			return stResponse;
		} catch (Exception e) {
			log.error("Exception while making a suretax request", e);
			return null;
		}
	}

	public SuretaxCancelResponse getCancelResponse(
			SuretaxCancelRequest request, String url) {
		try {
			HttpClient client = new HttpClient();
			PostMethod post = new PostMethod(url);

			ObjectMapper mapper = new ObjectMapper();
			String jsonRequestString = mapper.writeValueAsString(request);
			log.debug("Sending suretax json request string: %s",
					  jsonRequestString);
			post.addParameter("requestCancel", jsonRequestString);

			int respCode = client.executeMethod(post);
			String response = post.getResponseBodyAsString();
			SAXReader saxReader = new SAXReader();
			Document doc = saxReader.read(new StringReader(response));
			log.debug("Suretax response code: %s, Suretax response json string: %s", respCode,
					  doc.getRootElement().getText());
			SuretaxCancelResponse stcResponse = mapper.readValue(doc
					.getRootElement().getText(), SuretaxCancelResponse.class);
			return stcResponse;
		} catch (Exception e) {
			log.error("Exception while making a suretax request", e);
			e.printStackTrace();
			return null;
		}
	}
}
