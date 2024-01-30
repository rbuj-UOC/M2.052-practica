package web;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.configuration.Configuration;

import utils.HttpUtils.HttpException;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Redirects the requests to a randomly selected server
 * 
 * @author Pablo Chacin
 *
 */
public class ProxyServer extends WebServer {

	
	/**
	 * List of server addresses (<hostname>:<port>)
	 */
	protected String[] servers;
	
	/**
	 * Random generator used to balance requests among servers
	 */
	protected Random rnd;
	 
	
	/**
	 * Constructor with full arguments
	 * @param address
	 * @param urlPath
	 * @param port
	 * @param range
	 * @param servers
	 */
	public ProxyServer(String address, String urlPath, int port, int range,String[] servers) {
		super(address, urlPath, port, range);
		this.servers = servers;
		this.rnd = new Random();
	}


	/**
	 * Constructor with proxy specific arguments
	 * @param servers
	 */
	public ProxyServer(String[] servers) {
		this(null,null,0,0,servers);
	}


	public ProxyServer(){
	  this(null,null,0,0,null);
	}
	

	@Override
	public void start(Configuration configuration) {
		
		setServers(configuration.getStringArray("servers"));
		
		super.start(configuration);
		

	}


	
	
	public String[] getServers() {
		return servers;
	}


	public void setServers(String[] servers) {
		this.servers = servers;
	}


	@Override
	public void handle(HttpExchange exchange) throws IOException {

		//get the request path
		String path = exchange.getRequestURI().getPath();
		
		// get the request method
		String method = exchange.getRequestMethod();		
		
		//select a random target Server port in the range
		//serverPort , serverPort+portRange-1
		
		String server = servers[rnd.nextInt(servers.length)];
				
		HttpURLConnection serverConnection;
		try{
		     URL serverUrl = new URL("http://" +server + path);
		     serverConnection = (HttpURLConnection)serverUrl.openConnection();
		    		     
		    
			// get the headers
			Headers requestHeaders = exchange.getRequestHeaders();
				
			//Retrieve the name of the attributes as an String array
			String[] attributes = requestHeaders.keySet().toArray(new String[0]);
				
			//iterate along the array of attribute names to retrieve their values
			for(int i = 0; i<attributes.length;i++) {
					
				//retrieve the value of the attribute. Notice here that the value of an attribute 
				//is actually a list of String
				List<String> values = requestHeaders.get(attributes[i]);
					
				for(String v: values){
					//put the attribute and its value in the response
					if(attributes[i] != null)
						serverConnection.addRequestProperty(attributes[i], v);
				}
		     
			}
			
			//copy input content to output 
			List<String> contentlength = exchange.getRequestHeaders().get("Content-length");
			
			int size = -1;
			if(contentlength != null){
				size = Integer.parseInt(contentlength.get(0));
			}
			
			if(size > 0){
				serverConnection.setDoOutput(true);					
			}
						
			
			//execute request
		     serverConnection.setRequestMethod(method);

		     if(size > 0){
					byte[] content = new byte[size];
					exchange.getRequestBody().read(content);
		    	    serverConnection.getOutputStream().write(content);
		    	    serverConnection.getOutputStream().flush();
		    	    serverConnection.getOutputStream().close();
		     }
		     
			//get response
		     int rc = serverConnection.getResponseCode();
		     
		     
			//copy headers
		    Map<String,List<String>> responseFields = serverConnection.getHeaderFields(); 
		    for(Map.Entry<String, List<String>> f: responseFields.entrySet()){
		    	if(f.getKey() != null)
		    		exchange.getResponseHeaders().put(f.getKey(), f.getValue());
		    	
		    }
		    
		     //copy content
		     
		     int length = serverConnection.getContentLength();
		     if(length > 0){

			     exchange.sendResponseHeaders(rc, length);
		    	 byte[] content = new byte[length];
		    	 serverConnection.getInputStream().read(content);
		    	 exchange.getResponseBody().write(content);

		     }
		     else{
			     exchange.sendResponseHeaders(rc, -1);
		     }
		    		     
					     
			
		 }catch(Exception e) {
		     System.err.println("Exception connecting to server at "+ server + " with path"+path);
		     e.printStackTrace(System.err);
		 }
		
		 exchange.close();
	}
	
	
	public static void main(String[] args){
		new ProxyServer().start(args);
	}


	@Override
	protected void delete(String object) throws HttpException {
		
	}


	@Override
	protected byte[] getContent(String object) throws HttpException {
		return null;
	}


	@Override
	protected void putContent(String object, byte[] content)
			throws HttpException {
		
	}

}
