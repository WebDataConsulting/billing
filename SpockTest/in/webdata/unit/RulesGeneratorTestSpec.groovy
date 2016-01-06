package in.webdata.unit;

import java.io.*;
import spock.lang.Specification;
import com.sapienter.jbilling.tools.RulesGenerator;
import spock.lang.Shared;

public class RulesGeneratorTestSpec extends Specification {

    @Shared private File templateFile;
    @Shared private File dataFile;
    @Shared private File outputFile;
	char seprator  = ',';

    
    	def setup()  {

        templateFile = File.createTempFile("template", ".vm");
        dataFile = File.createTempFile("data", ".csv");
        outputFile = File.createTempFile("output", ".drl");
		
        templateFile.deleteOnExit();
        dataFile.deleteOnExit();
        outputFile.deleteOnExit();
    }

			def "meth1"() {
				setup:
				println("fg");
			}
			
		def "testRulesGeneration"() {
        
			setup:
					
				BufferedWriter templateWriter = new BufferedWriter(new FileWriter(templateFile));
		        String lineSeparator = System.getProperty("line.separator");
		        if (lineSeparator == null) lineSeparator = "\n";
		       
				 templateWriter.write("when" + lineSeparator +
		                "PricingField( name == \"prefix\", intValue_== \$field_1)" + lineSeparator +
		                "then" + lineSeparator +
		                "setPrice(\$field_2);" + lineSeparator +
		                lineSeparator);
					println(templateWriter)
		        templateWriter.close();
		
		        BufferedWriter dataWriter = new BufferedWriter(new FileWriter(dataFile));
		        dataWriter.write("613999,0.89\n" +
		                "613989,0.99\n" +
		                "613979,1.09");
		        dataWriter.close();
		
				RulesGenerator.generateRules(templateFile, dataFile, seprator, outputFile);
	
				String result = readFileToString(outputFile.getAbsolutePath());
		
			expect:
        
		
                true 			== 	result.contains("when" + lineSeparator +
				                        "PricingField( name == \"prefix\", intValue_== 613999)" + lineSeparator +
				                        	"then" + lineSeparator +
												"setPrice(0.89);" + lineSeparator +
															lineSeparator);
													
				true			==	result.contains("when" + lineSeparator +
                        				"PricingField( name == \"prefix\", intValue_== 613979)" + lineSeparator +
											"then" + lineSeparator +
												"setPrice(1.09);" + lineSeparator +
													lineSeparator);
    }

		def "testCounters"() {
			
		setup:
	        BufferedWriter templateWriter = new BufferedWriter(new FileWriter(templateFile));
	        String lineSeparator = System.getProperty("line.separator");
	        if (lineSeparator == null) lineSeparator = "\n";
	        templateWriter.write("Row \$row_number from \$total_rows rows, columns count \$total_columns" + lineSeparator);
	        templateWriter.close();
	
	        BufferedWriter dataWriter = new BufferedWriter(new FileWriter(dataFile));
	        dataWriter.write("613999,0.89\n" +
	                "613989,0.99, 555\n" +
	                "613979,1.09");
	        dataWriter.close();
	
	        RulesGenerator.generateRules(
	                templateFile,
	                dataFile,
					seprator,
	                outputFile
	        );

        	String result = readFileToString(outputFile.getAbsolutePath());
		 
			expect:
			    true        						== result.contains("from 3 rows");
		        true        						== result.contains("Row 0 from");
		        true        						== result.contains("Row 1 from");
		        true        						== result.contains("Row 2 from");
		        true        						== result.contains("columns count 2");
		        true        						== result.contains("columns count 3");	
	    }

			def "readFileToString"(String filePath) {
        
				StringBuffer fileData = new StringBuffer();
				BufferedReader reader = new BufferedReader(
	                new FileReader(filePath));
				char[] buf = new char[1024];
				int numRead = 0;
				while ((numRead = reader.read(buf)) != -1) {
	            fileData.append(buf, 0, numRead);
				}
				reader.close();
	        return fileData.toString();
    }
}
