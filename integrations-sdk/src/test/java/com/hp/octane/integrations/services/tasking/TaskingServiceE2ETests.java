package com.hp.octane.integrations.services.tasking;

import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.OctaneResultAbridged;
import com.hp.octane.integrations.dto.connectivity.OctaneTaskAbridged;
import com.hp.octane.integrations.dto.connectivity.TaskProcessingErrorBody;
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.CIProviderSummaryInfo;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.testhelpers.OctaneSPEndpointSimulator;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static com.hp.octane.integrations.services.tasking.TaskingTestPluginServicesTest.TEST_PLUGIN_VERSION;
import static com.hp.octane.integrations.services.tasking.TaskingTestPluginServicesTest.TEST_SENDING_TIME;
import static com.hp.octane.integrations.services.tasking.TaskingTestPluginServicesTest.TEST_SERVER_TYPE;
import static com.hp.octane.integrations.services.tasking.TaskingTestPluginServicesTest.TEST_SERVER_URL;
import static com.hp.octane.integrations.services.tasking.TaskingTestPluginServicesTest.TEST_SERVER_VERSION;

public class TaskingServiceE2ETests {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String APIPrefix = "/nga/api/v1";
	private static OctaneClient client;

	@BeforeClass
	public static void setupClient() {
		String inId = UUID.randomUUID().toString();
		String sspId = UUID.randomUUID().toString();
		OctaneConfiguration configuration = new OctaneConfiguration(inId, OctaneSPEndpointSimulator.getSimulatorUrl(), sspId);
		client = OctaneSDK.addClient(configuration, TaskingTestPluginServicesTest.class);
	}

	@AfterClass
	public static void removeClient() {
		OctaneSDK.removeClient(client);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeTestA() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		tasksProcessor.execute(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeTestB() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class);
		tasksProcessor.execute(taskAbridged);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeTestC() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setUrl("");
		tasksProcessor.execute(taskAbridged);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeTestD() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setUrl("some_wrong_url");
		tasksProcessor.execute(taskAbridged);
	}

	@Test
	public void testNonExistingAPI() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(UUID.randomUUID().toString())
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/some/non/existing/url");
		OctaneResultAbridged resultAbridged = tasksProcessor.execute(taskAbridged);

		Assert.assertNotNull(resultAbridged);
		Assert.assertEquals(HttpStatus.SC_NOT_FOUND, resultAbridged.getStatus());
		Assert.assertNotNull(resultAbridged.getHeaders());
		Assert.assertTrue(resultAbridged.getHeaders().isEmpty());
		Assert.assertEquals(taskAbridged.getId(), resultAbridged.getId());
		Assert.assertEquals(client.getInstanceId(), resultAbridged.getServiceId());
		Assert.assertNull(resultAbridged.getBody());
	}

	@Test
	public void testStatusAPI() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(UUID.randomUUID().toString())
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/status");
		OctaneResultAbridged resultAbridged = tasksProcessor.execute(taskAbridged);

		runCommonAsserts(resultAbridged, taskAbridged.getId(), HttpStatus.SC_OK);

		CIProviderSummaryInfo status = dtoFactory.dtoFromJson(resultAbridged.getBody(), CIProviderSummaryInfo.class);
		Assert.assertNotNull(status);

		Assert.assertNotNull(status.getSdk());
		Assert.assertNotNull(status.getSdk().getApiVersion());
		Assert.assertEquals(1, (int) status.getSdk().getApiVersion());
		Assert.assertEquals(OctaneSDK.SDK_VERSION, status.getSdk().getSdkVersion());

		Assert.assertNotNull(status.getPlugin());
		Assert.assertEquals(TEST_PLUGIN_VERSION, status.getPlugin().getVersion());

		Assert.assertNotNull(status.getServer());
		Assert.assertEquals(TEST_SERVER_URL, status.getServer().getUrl());
		Assert.assertEquals(TEST_SERVER_TYPE, status.getServer().getType());
		Assert.assertEquals(TEST_SERVER_VERSION, status.getServer().getVersion());
		Assert.assertEquals(TEST_SENDING_TIME, status.getServer().getSendingTime());
	}

	@Test
	public void testJobNoImplementedAPI() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(UUID.randomUUID().toString())
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/jobs");

		TaskingTestPluginServicesTest.getJobsAPIReturnNull = true;
		OctaneResultAbridged resultAbridged = tasksProcessor.execute(taskAbridged);
		TaskingTestPluginServicesTest.getJobsAPIReturnNull = false;

		runCommonAsserts(resultAbridged, taskAbridged.getId(), HttpStatus.SC_NOT_IMPLEMENTED);
	}

	@Test
	public void testJobsWithParamsAPI() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(UUID.randomUUID().toString())
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/jobs");
		OctaneResultAbridged resultAbridged = tasksProcessor.execute(taskAbridged);

		runCommonAsserts(resultAbridged, taskAbridged.getId(), HttpStatus.SC_OK);

		CIJobsList ciJobsList = dtoFactory.dtoFromJson(resultAbridged.getBody(), CIJobsList.class);
		Assert.assertNotNull(ciJobsList);
		Assert.assertNotNull(ciJobsList.getJobs());
		Assert.assertEquals(3, ciJobsList.getJobs().length);
		for (PipelineNode ciJob : ciJobsList.getJobs()) {
			Assert.assertNotNull(ciJob);
			Assert.assertTrue(ciJob.getName().startsWith("Job "));
			Assert.assertTrue(ciJob.getJobCiId().startsWith("job-"));
			Assert.assertNotNull(ciJob.getParameters());
			Assert.assertEquals(3, ciJob.getParameters().size());
			for (CIParameter ciParameter : ciJob.getParameters()) {
				Assert.assertNotNull(ciParameter);
				Assert.assertNotNull(ciParameter.getName());
				Assert.assertNotNull(ciParameter.getType());
				Assert.assertNotNull(ciParameter.getValue());
			}
		}
	}

	@Test
	public void testJobsNoParamsAPI() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(UUID.randomUUID().toString())
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/jobs?parameters=false");
		OctaneResultAbridged resultAbridged = tasksProcessor.execute(taskAbridged);

		runCommonAsserts(resultAbridged, taskAbridged.getId(), HttpStatus.SC_OK);

		CIJobsList ciJobsList = dtoFactory.dtoFromJson(resultAbridged.getBody(), CIJobsList.class);
		Assert.assertNotNull(ciJobsList);
		Assert.assertNotNull(ciJobsList.getJobs());
		Assert.assertEquals(3, ciJobsList.getJobs().length);
		for (PipelineNode ciJob : ciJobsList.getJobs()) {
			Assert.assertNotNull(ciJob);
			Assert.assertTrue(ciJob.getName().startsWith("Job "));
			Assert.assertTrue(ciJob.getJobCiId().startsWith("job-"));
			Assert.assertNotNull(ciJob.getParameters());
			Assert.assertTrue(ciJob.getParameters().isEmpty());
		}
	}

	@Test
	public void testJobNotExistsAPI() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(UUID.randomUUID().toString())
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/jobs/job-not-exists");
		OctaneResultAbridged resultAbridged = tasksProcessor.execute(taskAbridged);

		Assert.assertNotNull(resultAbridged);
		Assert.assertEquals(HttpStatus.SC_NOT_FOUND, resultAbridged.getStatus());
		Assert.assertNotNull(resultAbridged.getHeaders());
		Assert.assertTrue(resultAbridged.getHeaders().isEmpty());
		Assert.assertEquals(taskAbridged.getId(), resultAbridged.getId());
		Assert.assertEquals(client.getInstanceId(), resultAbridged.getServiceId());
		Assert.assertNull(resultAbridged.getBody());
	}

	@Test
	public void testJobSpecificAPI() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(UUID.randomUUID().toString())
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/jobs/job-a");
		OctaneResultAbridged resultAbridged = tasksProcessor.execute(taskAbridged);

		runCommonAsserts(resultAbridged, taskAbridged.getId(), HttpStatus.SC_OK);

		PipelineNode pipeline = dtoFactory.dtoFromJson(resultAbridged.getBody(), PipelineNode.class);
		Assert.assertNotNull(pipeline);
		Assert.assertEquals("job-a", pipeline.getJobCiId());
		Assert.assertEquals("Job A", pipeline.getName());
		Assert.assertNotNull(pipeline.getPhasesInternal());
		Assert.assertTrue(pipeline.getPhasesInternal().isEmpty());
		Assert.assertNotNull(pipeline.getPhasesPostBuild());
		Assert.assertTrue(pipeline.getPhasesPostBuild().isEmpty());
		Assert.assertNotNull(pipeline.getParameters());
		Assert.assertTrue(pipeline.getParameters().isEmpty());
		Assert.assertNull(pipeline.getMultiBranchType());
	}

	@Test
	public void testRunNotImplementedAPI() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(UUID.randomUUID().toString())
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/jobs/job-a/run");

		TaskingTestPluginServicesTest.runAPINotImplemented = true;
		OctaneResultAbridged resultAbridged = tasksProcessor.execute(taskAbridged);
		TaskingTestPluginServicesTest.runAPINotImplemented = false;

		Assert.assertNotNull(resultAbridged);
		Assert.assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, resultAbridged.getStatus());
		Assert.assertEquals(taskAbridged.getId(), resultAbridged.getId());
		Assert.assertEquals(client.getInstanceId(), resultAbridged.getServiceId());
		Assert.assertNull(resultAbridged.getBody());
	}

	@Test
	public void testRunThrowsExceptionAPI() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(UUID.randomUUID().toString())
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/jobs/job-a/run");

		TaskingTestPluginServicesTest.runAPIThrowsException = true;
		OctaneResultAbridged resultAbridged = tasksProcessor.execute(taskAbridged);
		TaskingTestPluginServicesTest.runAPIThrowsException = false;

		runCommonAsserts(resultAbridged, taskAbridged.getId(), HttpStatus.SC_INTERNAL_SERVER_ERROR);

		TaskProcessingErrorBody errorBody = dtoFactory.dtoFromJson(resultAbridged.getBody(), TaskProcessingErrorBody.class);
		Assert.assertNotNull(errorBody);
		Assert.assertNotNull(errorBody.getErrorMessage());
		Assert.assertTrue(errorBody.getErrorMessage().contains("runtime exception"));
	}

	@Test
	public void testRunAPI() {
		TasksProcessor tasksProcessor = client.getTasksProcessor();
		Assert.assertNotNull(tasksProcessor);

		OctaneTaskAbridged taskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
				.setId(UUID.randomUUID().toString())
				.setUrl(OctaneSPEndpointSimulator.getSimulatorUrl() + APIPrefix + "/jobs/job-a/run");
		OctaneResultAbridged resultAbridged = tasksProcessor.execute(taskAbridged);

		Assert.assertNotNull(resultAbridged);
		Assert.assertEquals(HttpStatus.SC_CREATED, resultAbridged.getStatus());
		Assert.assertEquals(taskAbridged.getId(), resultAbridged.getId());
		Assert.assertEquals(client.getInstanceId(), resultAbridged.getServiceId());
		Assert.assertNull(resultAbridged.getBody());
	}

	private void runCommonAsserts(OctaneResultAbridged resultAbridged, String taskId, int expectedStatus) {
		Assert.assertNotNull(resultAbridged);
		Assert.assertEquals(expectedStatus, resultAbridged.getStatus());
		Assert.assertEquals(ContentType.APPLICATION_JSON.getMimeType(), resultAbridged.getHeaders().get(HttpHeaders.CONTENT_TYPE));
		Assert.assertEquals(taskId, resultAbridged.getId());
		Assert.assertEquals(client.getInstanceId(), resultAbridged.getServiceId());
		Assert.assertNotNull(resultAbridged.getBody());
	}
}
