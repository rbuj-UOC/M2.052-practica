package utils;


import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;

/**
 * Makes Http requests
 * 
 * @author Pablo Chacin
 *
 */
public class HttpUtils {

	private static int HTTP_OK = 200;

	public static class HttpException extends Exception{

		protected int code;
		
		public HttpException(int code) {
			super("HTTP return code "+ code);
			this.code = code;
		}

		public HttpException(int code,String message, Throwable cause) {
			super(message, cause);
			this.code = code;
		}

		public HttpException(int code,String message) {
			super(message);
			this.code = code;
		}

		public HttpException(int code,Throwable cause) {
			super(cause);
			this.code = code;
		}
		
		public int getCode() {
			return code;
		}
	}
	
	public static void delete(String host,String url) throws IOException, HttpException {
		
		delete(host+url);
	}
	
	public static void delete(String url) throws IOException,HttpException {

		HttpURLConnection serverConnection;
		URL serverUrl = new URL(url);
		serverConnection = (HttpURLConnection)serverUrl.openConnection();

		serverConnection.setRequestMethod("DELETE");

		int rc = serverConnection.getResponseCode();
		if (rc != HTTP_OK) {
			throw new HttpException(rc);
		}

	}


	public static byte[] get(String host,String url) throws IOException, HttpException {
		return get(host+url);
	}
	
	public static byte[] get(String url) throws IOException, HttpException  {

		byte[] content = new byte[0];
		
		HttpURLConnection serverConnection;

		URL serverUrl = new URL(url);
		serverConnection = (HttpURLConnection)serverUrl.openConnection();
		serverConnection.setRequestMethod("GET");   
		String type=serverConnection.getContentType();
		String header=serverConnection.getHeaderField("Server");
		int length=Integer.parseInt(serverConnection.getHeaderField("Content-length"));

		//IF OK, read content
		if(serverConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
			content = new byte[length];
			serverConnection.getInputStream().read(content, 0, length);
		}

		int rc = serverConnection.getResponseCode();
		if(rc != HTTP_OK) {
			throw new HttpException(rc);
		}
		
		return content;

	}


	public static int put(String host,String url, byte[] content) throws IOException {
		return put(host+url,content);
	}
	
	
	public static int put(String url, byte[] content) throws IOException {

		HttpURLConnection serverConnection;

		URL serverUrl = new URL(url);
		serverConnection = (HttpURLConnection)serverUrl.openConnection();
		serverConnection.setDoOutput(true);


		serverConnection.setRequestMethod("PUT");   
		serverConnection.setRequestProperty("Content-type", "application/octet-stream");
		serverConnection.setRequestProperty("Content-length", String.valueOf(content.length));
		serverConnection.getOutputStream().write(content);

		serverConnection.getOutputStream().flush();
		serverConnection.getOutputStream().close();

		return serverConnection.getResponseCode();

	}

}
