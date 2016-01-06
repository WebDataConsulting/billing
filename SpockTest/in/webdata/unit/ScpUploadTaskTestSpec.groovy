package in.webdata.unit;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.pluggableTask2.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask2.admin.PluggableTaskParameterDTO;
import com.sapienter.jbilling.server.pluggableTask2.admin.PluggableTaskTypeDTO;
import com.sapienter.jbilling.server.process.task.ScpUploadTask;

import junit.framework.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import spock.lang.Specification;
import spock.lang.Shared;


public class ScpUploadTaskTestSpec extends Specification {

	private static final String BASE_DIR = Util.getSysProp("base_dir");

	
	@Shared private ScpUploadTask task = new ScpUploadTask();
	
	

		//Not doing in setup() Because setup requires atleast on method to start setup
		def "For Intilization"()  {
		
		setup:
		
			List<PluggableTaskParameterDTO> parameters = new ArrayList<PluggableTaskParameterDTO>();
		
			parameters.add(_mockParameter(ScpUploadTask.PARAM_SCP_USERNAME, ""));
			parameters.add(_mockParameter(ScpUploadTask.PARAM_SCP_PASSWORD, ""));
			parameters.add(_mockParameter(ScpUploadTask.PARAM_SCP_HOST, ""));
			parameters.add(_mockParameter(ScpUploadTask.PARAM_SCP_REMOTE_PATH, ""));


			PluggableTaskDTO dto = new PluggableTaskDTO();
			dto.setEntityId(1);
			dto.setParameters(parameters);

			PluggableTaskTypeDTO type = new PluggableTaskTypeDTO();
			type.setMinParameters(0);
			dto.setType(type);

			task.initializeParamters(dto);

	}

	
		def PluggableTaskParameterDTO _mockParameter(String name, String value) {
			
			println("in _mockParameter")
			PluggableTaskParameterDTO parameter = new PluggableTaskParameterDTO();
			parameter.setName(name);
			parameter.setStrValue(value);
	        return parameter;
	}

		def "testNoop"() {
			true
	}
}
