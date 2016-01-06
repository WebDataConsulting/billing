package in.webdata.unit

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.process.task.AbstractUploadTask;

import junit.framework.TestCase;

import org.quartz.JobExecutionException;

import java.io.File;
import java.util.Collections;
import java.util.List;

import spock.lang.Specification;

public class AbstractUploadTaskTestSpec extends Specification {
    

	/*
	 * Inmportant Information
	 * 
	 * Please put resources directory under /home/userPcName/Desktop/jB-CE/src/resources
	 * 
	 * 
	 * 
	 * 
	 * */
	
	private static final String BASE_DIR = "/home/abhimanyus/Desktop/jB-CE/src/resources";


    private TestUploadTask task = new TestUploadTask();

    private class TestUploadTask extends AbstractUploadTask {
        public void upload(List<File> files) throws JobExecutionException { }
        public String getTaskName() { return null; }
    }

	  	def "testCollectFilesNonRecursive"() {
        
			  setup:
			
				  	File path = new File(BASE_DIR);
							        
					List<File> files = task.collectFiles(path, "1	", false);

				expect:
			
					1									== files.size();
		 
					"entityNotifications.properties" 	== files.get(0).getName();
    }

		def "testCollectFilesRecursive"() {
			
			setup:
		
				File path = new File(BASE_DIR);

        // Sir please ensure that all the .jasper files from jB-CE/designs are in the jB-CE/src/resources/
		
        List<File> nonRecursive = task.collectFiles(path, ".*\\.jasper", false);
        
        List<File> files = task.collectFiles(path, ".*\\.jasper", true);
        Collections.sort(files);

		expect:
		
		14									 ==		 nonRecursive.size();
        14									 ==		 files.size();
        "simple_invoice.jasper"				 == files.get(0).getName();
        "simple_invoice_b2b.jasper" 		 == files.get(1).getName();
        "simple_invoice_telco.jasper"		 ==  files.get(2).getName();
        "simple_invoice_telco_events.jasper" == files.get(3).getName();
    }

		def "testCollectfilesCompoundRegex"()  {
        
			setup:
			
				File path = new File(BASE_DIR);
			
				List<File> files = task.collectFiles(path, "(.*\\.jasper|.*\\.jpg\$)", true);
		
				Collections.sort(files);
								
			expect:
			
					17 								 == files.size();
		        "simple_invoice.jasper"				 == files.get(3).getName();
		        "simple_invoice_b2b.jasper"			 == files.get(4).getName();
		        "simple_invoice_telco.jasper"		 == files.get(5).getName();
		        "simple_invoice_telco_events.jasper" == files.get(6).getName();
		        "entity-1.jpg"						 == files.get(1).getName();
    }
}
