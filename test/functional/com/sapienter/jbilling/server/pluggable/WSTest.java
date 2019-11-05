package com.sapienter.jbilling.server.pluggable;

import com.sapienter.jbilling.test.ApiTestCase;
import org.testng.annotations.Test;

import junit.framework.TestCase;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Test(groups = {"web-services", "pluggable"})
public class WSTest extends ApiTestCase {
	
	@Test
	public void testGetPluginTypeWSById() throws Exception{
		
		String className = "com.sapienter.jbilling.server.pluggableTask.BasicCompositionTask";
		Integer categoryId = 4;
		
		PluggableTaskTypeWS ws = api.getPluginTypeWS(4);
		
		assertEquals(ws.getClassName(),className);
		assertEquals(ws.getCategoryId(),categoryId);
		
		ws = api.getPluginTypeWS(5);
		assertFalse(ws.getClassName().equals(className));
		assertFalse(ws.getCategoryId() == categoryId);
	}
	
	@Test
	public void testGetPluginTypeWSByClassName() throws Exception{
		
		String className = "com.sapienter.jbilling.server.pluggableTask.BasicCompositionTask";
		Integer categoryId = 4;
		Integer id = 4;
		
		PluggableTaskTypeWS ws = api.getPluginTypeWSByClassName(className);
		
		assertEquals(ws.getClassName(),className);
		assertEquals(ws.getCategoryId(),categoryId);
		assertEquals(ws.getId(),id);
		
		String secondClassName = "com.sapienter.jbilling.server.pluggableTask.BasicPenaltyTask";
		ws = api.getPluginTypeWSByClassName(secondClassName);
		
		assertFalse(ws.getClassName().equals(className));
		assertFalse(ws.getClassName().equals(className));
		assertFalse(ws.getId() == id);
	}
	
	@Test
	public void testGetPluginTypeCategoryById() throws Exception{
		
		Integer id = 4;
		String interfaceName = "com.sapienter.jbilling.server.pluggableTask.InvoiceCompositionTask";
		
		PluggableTaskTypeCategoryWS ws = api.getPluginTypeCategory(id);
		
		assertEquals(ws.getInterfaceName(),interfaceName);
		assertEquals(ws.getId(),id);
		
		Integer secondId = 5;
		ws = api.getPluginTypeCategory(secondId);
		
		assertFalse(ws.getInterfaceName().equals(interfaceName));
		assertFalse(ws.getId() == id);
	}
	
	@Test
	public void testGetPluginTypeCategoryByInterfaceName() throws Exception{

		Integer id = 4;
		String interfaceName = "com.sapienter.jbilling.server.pluggableTask.InvoiceCompositionTask";
		
		PluggableTaskTypeCategoryWS ws = api.getPluginTypeCategoryByInterfaceName(interfaceName);
		assertEquals(ws.getInterfaceName(),interfaceName);
		assertEquals(ws.getId(),id);
		
		String secondInterfaceName = "com.sapienter.jbilling.server.pluggableTask.NotificationTask";
		ws = api.getPluginTypeCategoryByInterfaceName(secondInterfaceName);
		assertFalse(ws.getInterfaceName().equals(interfaceName));
		assertFalse(ws.getId() == id);
	}
} 