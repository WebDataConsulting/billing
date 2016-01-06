package in.webdata.unit

import java.io.File;

import java.io.IOException;
import java.util.List;
import java.util.HashMap;

import junit.framework.TestCase;

import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.rule.task.AbstractGeneratorTask;
import com.sapienter.jbilling.server.rule.task.VelocityRulesGeneratorTask;
import com.sapienter.jbilling.server.rule.task.test.Bundle;
import com.sapienter.jbilling.server.rule.task.test.Product;

import spock.lang.Specification;

public class VelocityRulesGeneratorTaskTest2 extends Specification {
	 
	 final static VelocityRulesGeneratorTask task = new VelocityRulesGeneratorTask();
     static File outputFile = null;
	

    static {
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        
		parameters.put(AbstractGeneratorTask.PARAM_CONFIG_FILENAME, 
        
			        System.getProperty("user.dir") + 
							"/descriptors/rules/rules-generator-config.xml");

		try {
            outputFile = File.createTempFile("test", "pkg");
            outputFile.deleteOnExit();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
		
        parameters.put(AbstractGeneratorTask.PARAM_OUTPUT_FILENAME, 
                outputFile.getAbsolutePath());
			
        parameters.put(VelocityRulesGeneratorTask.PARAM_TEMPLATE_FILENAME, 
                System.getProperty("user.dir") + 
                			"/descriptors/rules/rules-generator-template-unit-test.vm");
			
        task.setParameters(parameters);
    }



   def "testXMLParsing"()  {
        
	   setup:
	   
		   String xml = 
	            "<bundles> " +
	              "<bundle> " +
	                "<original-product> " +
	                  "<name>Silver Package</name> " +
	                "</original-product> " +
	                "<replacement-product> " +
	                  "<name>Medium speed connection</name> " +
	                "</replacement-product> " +
	                "<replacement-product> " +
	                  "<name>Unlimited emails</name> " +
	                "</replacement-product> " +
	              "</bundle> " +
	              "<bundle> " +
	                "<original-product> " +
	                  "<name>Gold Package</name> " +
	                "</original-product> " +
	                "<replacement-product> " +
	                  "<name>High speed connection</name> " +
	                "</replacement-product> " +
	                "<replacement-product> " +
	                  "<name>Unlimited emails</name> " +
	                "</replacement-product> " +
	              "</bundle> " +
	            "</bundles>";


        task.unmarshal(xml);

        Object data = task.getData();

        List<Bundle> bundles = (List<Bundle>) data;

        Bundle bundle1 = bundles.get(0);

        List<Product> replacementProducts1 = bundle1.getReplacementProducts();
       
        Bundle bundle2 = bundles.get(1);
      
        List<Product> replacementProducts2 = bundle2.getReplacementProducts();
      
		expect: 
			
			true                                 == data instanceof List;
			bundles.size() 						 == 2;
			 "Silver Package"                    == bundle1.getOriginalProduct().getName();
			"Medium speed connection"	         == replacementProducts1.get(0).getName();
		    "Unlimited emails"                   == replacementProducts1.get(1).getName();
			"Gold Package"                       == bundle2.getOriginalProduct().getName();
			"Unlimited emails"                   ==	replacementProducts2.get(1).getName();		
    }

    	def "testRuleGeneration"()  {
		
			when:
			
			    task.process();
				
			then:
				
	            String rules = task.getRules();
				
	            System.out.println(rules);
	        	
				Exception e = thrown()
	                
				String expected = 
						
				"package InternalEventsRulesTask520\n" +
						"\n" +
						"import com.sapienter.jbilling.server.order.OrderLineBL\n" +
						"import com.sapienter.jbilling.server.order.event.OrderToInvoiceEvent\n" +
						"import com.sapienter.jbilling.server.order.db.OrderDTO\n" +
						"import com.sapienter.jbilling.server.order.db.OrderLineDTO\n" +
						"\n" +
						"rule 'Bundle 1'\n" +
						"when\n" +
						"        OrderToInvoiceEvent(userId == 1010)\n" +
						"        \$order : OrderDTO(notes == \"Change me.\")\n" +
						"        \$planLine : OrderLineDTO( itemId == 1) from \$order.lines # Plan\n" +
						"then\n" +
						"        \$order.setNotes(\"Modified by rules created by generateRules API method.\");\n" +
						"        \$order.getLines().remove(\$planLine); # Plan is only for grouping\n" +
						"\n" +
						"        OrderLineBL.addItem(\$order, 1, false); # A product for this plan\n" +
						"        OrderLineBL.addItem(\$order, 1, false); # A product for this plan\n" +
						"        update(\$order);\n" +
						"end\n" +
						"rule 'Bundle 2'\n" +
						"when\n" +
						"        OrderToInvoiceEvent(userId == 1010)\n" +
						"        \$order : OrderDTO(notes == \"Change me.\")\n" +
						"        \$planLine : OrderLineDTO( itemId == 1) from \$order.lines # Plan\n" +
						"then\n" +
						"        \$order.setNotes(\"Modified by rules created by generateRules API method.\");\n" +
						"        \$order.getLines().remove(\$planLine); # Plan is only for grouping\n" +
						"\n" +
						"        OrderLineBL.addItem(\$order, 1, false); # A product for this plan\n" +
						"        OrderLineBL.addItem(\$order, 1, false); # A product for this plan\n" +
						"        update(\$order);\n" +
						"end\n";

			expect:
		
						expected == rules;
    }
}
