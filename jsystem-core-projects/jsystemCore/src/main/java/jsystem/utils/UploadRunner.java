/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package jsystem.utils;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;

import jsystem.framework.FrameworkOptions;
import jsystem.framework.JSystemProperties;



/**
 * use this class to upload log files to http server (instead of ftp server)
 * Object of this class create zip file from log directory and after that send
 * multipart request to http server
 */
public class UploadRunner {

	private String serverUrl = null;

	private String filePath = null;

	private long logIndex;

	private File logDir;

	private HttpPost filePost;

	private static Logger log = Logger.getLogger(UploadRunner.class.getName());

	/**
	 * 
	 * @param logDir
	 *            -log files directory
	 * @param logIndex
	 *            -current log index
	 */
	public UploadRunner(File logDir, long logIndex)

	{
		super();
		this.logDir = logDir;
		this.logIndex = logIndex;
	}

	public void zipFile() throws Exception {

		/**
		 * zip log directory
		 */
		filePath = System.getProperty("user.dir") + File.separator + String.valueOf(logIndex) + ".zip";

		FileUtils.zipDirectory(logDir.getAbsolutePath(), null, filePath);

		/**
		 * get server properties from db.properties file
		 */
		this.serverUrl = "http://" + getServerUrl() + "/reports/upload";
	}

	public void setFilePath() throws Exception {

		/**
		 * zip log directory
		 */
		filePath = System.getProperty("user.dir") + File.separator + String.valueOf(logIndex) + ".zip";

		/**
		 * get server properties from db.properties file
		 */
		this.serverUrl = "http://" + getServerUrl() + "/reports/upload";
	}

	/**
	 * Gets the server URL according to the parameters specified in the JSystem
	 * properties
	 */
	private static String getServerUrl() throws Exception {
		return JSystemProperties.getInstance().getPreferenceOrDefault(FrameworkOptions.REPORTS_PUBLISHER_HOST) + ":"
				+ JSystemProperties.getInstance().getPreferenceOrDefault(FrameworkOptions.REPORTS_PUBLISHER_PORT);
	}

	/**
	 * get reports application url.
	 */
	public static String getReportsApplicationUrl() throws Exception {
		final String host = JSystemProperties.getInstance().getPreferenceOrDefault(FrameworkOptions.REPORTS_PUBLISHER_HOST);
		final String port = JSystemProperties.getInstance().getPreferenceOrDefault(FrameworkOptions.REPORTS_PUBLISHER_PORT);
		return "http://" + host + ":" + port + "/report-service/index.html";
	}

	/**
	 * get server properties from db.properties file
	 */
	public static boolean validateUrl(String url) {
		try {
			URL _url = new URI(url).toURL();
			_url.openConnection().connect();
			return true;
		} catch (Exception e) {
			log.log(Level.FINE, "Failed validating url " + url, e);
			return false;
		}
	}

	/**
	 * use jacarta http client Send to the server(servlet)multipart request
	 * server IP must be writen in db.properties -serverIP="you server ip"
	 * 
	 * @throws Exception
	 */
	public void upload() throws Exception {
		filePost = new HttpPost(serverUrl);

		/**
		 * create multipart request
		 */
		try {
			File targetFile = new File(filePath);
			HttpEntity entity = MultipartEntityBuilder.create()
					.addBinaryBody(targetFile.getName(), targetFile)
					.build();
			filePost.setEntity(entity);
			
			RequestConfig config = RequestConfig.custom().setConnectTimeout(5000).build();
			HttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build();

			/**
			 * send request
			 */
			HttpResponse response = client.execute(filePost);

			/**
			 * upload fail
			 */
			int status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {

				throw new Exception("Publish error : fail upload files to " + serverUrl + " \n\n"
						+ "Unable upload file " + filePath + "\n" + "HTTP Status " + status + "\n");
			}
		}
		/**
		 * release connection-must delete zip file in client log directory
		 */
		finally {
			if (filePost != null) {
				filePost.releaseConnection();
			}
			File file = new File(filePath);
			file.delete();
		}

	}
}
