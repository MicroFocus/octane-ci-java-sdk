package com.hp.octane.integrations.services.tasking;

import com.hp.octane.integrations.CIPluginServices;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaskingTestPluginServicesTest extends CIPluginServices {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	static final String TEST_SERVER_URL = "http://some.non.existing.url:8888";
	static final String TEST_SERVER_TYPE = "dummy_server";
	static final String TEST_SERVER_VERSION = "1.0";
	static final Long TEST_SENDING_TIME = System.currentTimeMillis();
	static final String TEST_PLUGIN_VERSION = "1.0";

	static boolean getJobsAPIReturnNull = false;
	static boolean runAPINotImplemented = false;
	static boolean runAPIThrowsException = false;

	@Override
	public CIServerInfo getServerInfo() {
		return dtoFactory.newDTO(CIServerInfo.class)
				.setUrl(TEST_SERVER_URL)
				.setType(TEST_SERVER_TYPE)
				.setVersion(TEST_SERVER_VERSION)
				.setSendingTime(TEST_SENDING_TIME);
	}

	@Override
	public CIPluginInfo getPluginInfo() {
		return dtoFactory.newDTO(CIPluginInfo.class)
				.setVersion(TEST_PLUGIN_VERSION);
	}

	@Override
	public CIJobsList getJobsList(boolean includeParameters, Long workspaceId) {
		return getJobsAPIReturnNull ? null :
				dtoFactory.newDTO(CIJobsList.class)
						.setJobs(getJobsListInternal(includeParameters));
	}

	@Override
	public PipelineNode getPipeline(String rootJobId) {
		PipelineNode result = null;
		if ("job-a".equals(rootJobId)) {
			return dtoFactory.newDTO(PipelineNode.class)
					.setName("Job A")
					.setJobCiId("job-a");
		}
		return result;
	}

	@Override
	public void runPipeline(String jobId, String originalBody) {
		if (runAPINotImplemented) {
			super.runPipeline(jobId, originalBody);
		}
		if (runAPIThrowsException) {
			throw new RuntimeException("runtime exception");
		}
	}

	private PipelineNode[] getJobsListInternal(boolean withParams) {
		List<PipelineNode> result = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			PipelineNode tmpNode = dtoFactory
					.newDTO(PipelineNode.class)
					.setName("Job " + i)
					.setJobCiId("job-" + i);
			if (withParams) {
				tmpNode.setParameters(Arrays.asList(
						dtoFactory.newDTO(CIParameter.class).setName("param S").setType(CIParameterType.STRING).setValue("some value"),
						dtoFactory.newDTO(CIParameter.class).setName("param N").setType(CIParameterType.NUMBER).setValue(15514567),
						dtoFactory.newDTO(CIParameter.class).setName("param B").setType(CIParameterType.BOOLEAN).setValue(true)
				));
			}
			result.add(tmpNode);
		}
		return result.toArray(new PipelineNode[0]);
	}
}
