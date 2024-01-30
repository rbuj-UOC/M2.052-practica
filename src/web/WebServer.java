package web;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.commons.configuration.Configuration;

import utils.FileUtils;
import utils.HttpUtils.HttpException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import dat.utils.CmdLineArgs;

/**
 * Skeleton for implementing a simple Http Server based on Sun's provided HttpServer class.
 * 
 * It handles the initialization issues for servers. Sub classes must implement the handling
 * of requests
 * 
 * @author Pablo Chacin
 *
 */
public abstract class WebServer implements HttpHandler {

	
	protected static String DEFAULT_ADDRESS = "localhost";
	
	protected static String DEFAULT_PROTOCOL = "http";
	
	protected static String DEFAULT_URL = "/";
	
	protected static int DEFAULT_PORT = 8080;
	
	protected static int DEFAULT_RANGE = 10;
	
	protected static int HTTP_OK = 200;

	protected static int HTTP_SERVER_ERROR = 500;

	protected static int HTTP_NOT_FOUND = 404;
	
	protected static int HTTP_BAD_REQUEST = 400;
	
	protected static int HTTP_NOT_IMPLEMENTED= 501;
	
	protected static int HTTP_NO_CONTENT = -1;
	
	
	/**
	 * Protocol used to access this server
	 */
	protected String protocol;
	
	/**
	 * Binding address
	 */
	protected String address;

	/**
	 * Path used to install the handler
	 */
	protected String urlPath;

	/**
	 * Port used to listen to clients
	 */
	protected int port;

	/**
	 * Range of ports from which select a port
	 */
	protected int range;

	protected int bindingPort;
	
	/**
	 * Server
	 */
	protected HttpServer server;
	
	
	/**
	 * Constructor. Create an instance of the server associated to a port and a path to
	 * the data store.
	 * 
	 * @param address an String with the address to bind to
	 * @param urlPath an String with the path to install the handler
	 * @param port an integer with the port to listen
	 * @param range an integer with the range of ports to try
	 * @param path a String with the path to the server's data store
	 */
	public WebServer(String address,String urlPath,int port,int range) {
		this.address = address;
		this.urlPath = urlPath;
		this.range = range;
		this.port = port;

	}

	public WebServer() {
	}


	public void start(String[] cmdArgs){
		
		Configuration config = CmdLineArgs.getArguments(cmdArgs);
		
		start(config);
	}
	
	/**
	 * Starts the server with command from the command line
	 * 
	 * @param cmdArgs
	 */
	public void start(Configuration configuration){

		try{
			setAddress(configuration.getString("address",DEFAULT_ADDRESS));
			setProtocol(configuration.getString("protocol",DEFAULT_PROTOCOL));
			setUrlPath(configuration.getString("url",DEFAULT_URL));
			setPort(configuration.getInteger("port",DEFAULT_PORT));
			setRange(configuration.getInteger("range",DEFAULT_RANGE));
			
			start();
			
		}catch(Exception e){
			System.err.println("Syntax Error in the Arguments " + e.getMessage());
		}
		
	}
	

	
	public void start(){

			// Create an HttpServer and bind it to the socket
		
			for(int p=port;p<port+range;p++){
				
				try{
					InetSocketAddress addr = new InetSocketAddress(address,p);
					server = HttpServer.create(addr, 0);

					// Sets an Executor that will handler all HTTP requests
					server.setExecutor(Executors.newCachedThreadPool());
					
					//add itself as handler
					server.createContext(urlPath,this);

					// Start the server
					server.start();
										
					bindingPort = p;
					
					System.out.println("Server started at " + getUrl());
					
					return;
				}catch(IOException e){
					continue;
				}
			}
			
			//if for cycle ends, no port was available
			System.out.println("Unable to start server at " +address + 
					           " in port range " + port + "-" + 1);
	}


	
	public String getUrl(){
		return getProtocol()+ "://" + getAddress() + ":" + getBindingPort() + getUrlPath();
	}
	
	protected String getProtocol(){
		return protocol;
	}
	
	protected void setProtocol(String protocol){
		this.protocol = protocol;
	}
	
	public int getBindingPort(){
		return bindingPort;
	}
	
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getUrlPath() {
		return urlPath;
	}

	public void setUrlPath(String urlPath) {
		this.urlPath = urlPath;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public HttpServer getServer() {
		return server;
	}

	public void setServer(HttpServer server) {
		this.server = server;
	}

	/**
	 * Handles a request. Call the method to the corresponding operation
	 */
	public void handle(HttpExchange exchange) throws IOException {

		
		// get the request method
		String method = exchange.getRequestMethod();
	
		try {
		if(method.equalsIgnoreCase("PUT")) {
			
			doPUT(exchange);
			
		} else if(method.equalsIgnoreCase("GET")) {

		
			doGET(exchange);
			

		} else  if(method.equalsIgnoreCase("DELETE")) {

			doDELETE(exchange);

		} else {

			exchange.sendResponseHeaders(HTTP_SERVER_ERROR, HTTP_NO_CONTENT);
		}

		}catch(HttpException e) {
			exchange.sendResponseHeaders(e.getCode(), HTTP_NO_CONTENT);
		}
		
		exchange.close();


	}
	
	/**
	 * Returns the name of the object relative to the path
	 * Associated with this handler
	 * 
	 * @param urlPath the full path of the request
	 * 
	 * @return the relative path to the object
	 */
	protected String getObject(HttpExchange exchange){
		//get the path removing the server's url path
		int prefixLengh = exchange.getHttpContext().getPath().length();
		
		String object = exchange.getRequestURI().getPath().substring(prefixLengh);
		
		return object;
	}
	
	protected void doGET(HttpExchange exchange) throws IOException,HttpException{
		
		String object = getObject(exchange);
		
		byte[] content = getContent(object);
		
		if(content.length> 0){
			exchange.sendResponseHeaders(HTTP_OK,content.length);
			FileUtils.writeBytesToStream(exchange.getResponseBody(), content);
		}
		else{
			exchange.sendResponseHeaders(HTTP_OK,HTTP_NO_CONTENT);
		}
	}
	
	
	protected void doPUT(HttpExchange exchange) throws IOException, HttpException{
		String object = getObject(exchange);
		
		byte[] content;
			content = FileUtils.readBytesFromStream(exchange.getRequestBody());
			putContent(object,content);
			exchange.sendResponseHeaders(HTTP_OK,HTTP_NO_CONTENT);
		

	}
	
	
	
	protected void doDELETE(HttpExchange exchange) throws IOException, HttpException{
		String object = getObject(exchange);
		delete(object);
		exchange.sendResponseHeaders(HTTP_OK,HTTP_NO_CONTENT);
	}
	
	
	/**
	 * Gets the content of the given object and returns it in a byte[].
	 * The method must allocate the space for the content.
	 * 
	 * If the object doesn't exists, must leave it unchanged.
	 * 
	 * @param object logical path to the object
	 * @param content content of the object. 
	 * @return
	 */
	protected abstract byte[] getContent(String object) throws HttpException;
	
	
	protected abstract void  putContent(String object,byte[]content) throws HttpException;
		
	
	protected abstract void delete(String object) throws HttpException;
	
}
