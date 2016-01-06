package in.webdata.unit;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.pluggableTask2.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask2.admin.PluggableTaskParameterDTO;
import com.sapienter.jbilling.server.pluggableTask2.admin.PluggableTaskTypeDTO;
import com.sapienter.jbilling.server.process.task.SftpUploadTask;

import junit.framework.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import spock.lang.Specification


public class SftpUploadTaskTestSpec extends Specification {

    private static final String BASE_DIR = Util.getSysProp("base_dir");    

    // class under test
    private SftpUploadTask task = new SftpUploadTask();

    	def "Intialize"() {

		setup:
	        List<PluggableTaskParameterDTO> parameters = new ArrayList<PluggableTaskParameterDTO>();
	        parameters.add(_mockParameter(SftpUploadTask.PARAM_SFTP_USERNAME, ""));
	        parameters.add(_mockParameter(SftpUploadTask.PARAM_SFTP_PASSWORD, ""));
	        parameters.add(_mockParameter(SftpUploadTask.PARAM_SFTP_HOST, ""));
	        parameters.add(_mockParameter(SftpUploadTask.PARAM_SFTP_REMOTE_PATH, ""));
	
			
	        PluggableTaskDTO dto = new PluggableTaskDTO();
	        dto.setEntityId(1);
	        dto.setParameters(parameters);        
	        
			
					
	        PluggableTaskTypeDTO type = new PluggableTaskTypeDTO();
	        type.setMinParameters(0);
	        dto.setType(type);
	        
	        task.initializeParamters(dto);
	    }

    private PluggableTaskParameterDTO _mockParameter(String name, String value) {
        PluggableTaskParameterDTO parameter = new PluggableTaskParameterDTO();
        parameter.setName(name);
        parameter.setStrValue(value);
		show();
        return parameter;
    }

	public void show() {
		
		System.out.println("Showing this message.");
	}
	
    public void testNoop() {
			true;
    }

}