package jsystem.extensions.report.difido;

import java.io.File;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import il.co.topq.difido.model.execution.MachineNode;
import il.co.topq.difido.model.remote.ExecutionDetails;
import il.co.topq.difido.model.test.TestDetails;

public class DifidoClient {

	private static final String BASE_URI_TEMPLATE = "http://%s:%d/api/";
	private final String baseUri;
	private final HttpClient client;

	public DifidoClient(String host, int port) {
		baseUri = String.format(BASE_URI_TEMPLATE, host, port);
		client = HttpClients.createDefault();
	}

	public int addExecution(ExecutionDetails details) throws Exception {
		final HttpPost method = new HttpPost(baseUri + "executions/");
		if (details != null) {
			final String descriptionJson = new ObjectMapper().writeValueAsString(details);
			method.setEntity(new StringEntity(descriptionJson,ContentType.APPLICATION_JSON));
		}
		final HttpResponse  response = client.execute(method);
		handleResponseCode(response);
		String responseBody = EntityUtils.toString(response.getEntity());
		return Integer.parseInt(responseBody);
	}

	public void endExecution(int executionId) throws Exception {
		final HttpPut method = new HttpPut(baseUri + "executions/" + executionId + "?active=false");
		method.setHeader("Content-Type", "text/plain");
		final HttpResponse response = client.execute(method);
		handleResponseCode(response);
	}

	public int addMachine(int executionId, MachineNode machine) throws Exception {
		HttpPost method = new HttpPost(baseUri + "executions/" + executionId + "/machines/");
		final ObjectMapper mapper = new ObjectMapper();
		final String json = mapper.writeValueAsString(machine);
		method.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
		HttpResponse response = client.execute(method);
		handleResponseCode(response);
		return Integer.parseInt(EntityUtils.toString(response.getEntity()));
	}

	public void updateMachine(int executionId, int machineId, MachineNode machine) throws Exception {
		HttpPut method = new HttpPut(baseUri + "executions/" + executionId + "/machines/" + machineId);
		final ObjectMapper mapper = new ObjectMapper();
		final String json = mapper.writeValueAsString(machine);
		method.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
		HttpResponse response = client.execute(method);
		handleResponseCode(response);
	}

	public void addTestDetails(int executionId, TestDetails testDetails) throws Exception {
		HttpPost method = new HttpPost(baseUri + "executions/" + executionId + "/details");
		final ObjectMapper mapper = new ObjectMapper();
		final String json = mapper.writeValueAsString(testDetails);
		method.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
		final HttpResponse response = client.execute(method);
		handleResponseCode(response);
	}

	public void addFile(final int executionId, final String uid, final File file) throws Exception {
		HttpPost method = new HttpPost(baseUri + "executions/" + executionId + "/details/" + uid + "/file/");
		method.setEntity(new FileEntity(file));
		final HttpResponse response = client.execute(method);
		handleResponseCode(response);
	}

	private void handleResponseCode(HttpResponse response) throws Exception {
		int responseCode = response.getStatusLine().getStatusCode();
		if (responseCode != 200 && responseCode != 204) {
			String responseBody = EntityUtils.toString(response.getEntity());
			throw new Exception("Request was not successful. Response is: " + responseCode + ".\n Response body: "
					+ responseBody);
		}

	}

}
