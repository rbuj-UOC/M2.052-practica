package test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import utils.FileUtils;
import utils.HttpUtils;




/**
 * Allows the exchange of objects to a "remote" data store by means of a DataStoreStub.   
 * Keeps tracks of the execution, issuing messages (when in verbose mode) and
 * collecting statistics.
 *
 * Object's content can be directed to/from files or streams (stdin, stdout).
 * 
 * @author Pablo Chacin
 *
 */
public class HttpTest {

	/**
	 * An implementation of OutputStream that ignores content.
	 * 
	 */
	private class NullOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			//ignore content

		}

	}

	/**
	 * number of bytes of content sent to the server
	 */
	private long bytesSent = 0;

	/**
	 * Number of bytes of content received from the server
	 */
	private long bytesReceived = 0;

	/**
	 * Number of requests executed
	 */
	private int requests = 0;

	/**
	 * Accumulate execution time for all requests
	 */
	private long executionTime;


	/**
	 * display progress messages or not
	 */
	protected boolean verbose = true;

	/**
	 * Constructor. Creates an instance of a DataStore client using a DataStoreClientStub
	 * 
	 * @param stub the implementation of the DataStoreClient interface
	 * @param server the server to connect to
	 * @param localDataStore a DataStore used to store working files
	 * @param verbose indicates if a trace of the commands must be issue to standard outout
	 */
	public HttpTest() {

	}



	/**
	 * 
	 * @return the average response time, with 3 decimals precision
	 */
	public double getAverageResponse() {
		Double avg = 0.0;
		if(requests > 0) {
			avg = (double)executionTime / (double)requests;
		}

		return Math.round(avg*1000.0)/1000.0;
	}

	/**
	 * 
	 * @return the number of requests served
	 */
	public int getRequests() {
		return requests;
	}

	/**
	 * 
	 * @return time spent on serving the requests
	 */
	public long getExecutionTime() {
		return executionTime;
	}

	/**
	 * 
	 * @return the number of bytes of content received from the server
	 */
	public long getBytesReceived() {
		return bytesReceived;
	}

	/**
	 * 
	 * @return the number of bytes of content sent to the Server
	 */
	public long getBytesSent() {
		return bytesSent;
	}


	/**
	 * Deletes an object from the server
	 * 
	 * @param object
	 */
	public void doDelete(String url) {

		if(verbose)
			System.out.print("Deleting " + url + " . . .  ");

		try {

			long init = System.currentTimeMillis();

			HttpUtils.delete(url);

			//calculate time of execution
			long time = System.currentTimeMillis() - init;

			if(verbose)
				System.out.printf("Execution time "+time +" milseconds");

			//account execution time
			requests += 1;
			executionTime += time;

		} catch (Exception e) {
			System.err.println("Exception deleting object [" + url + ": "+e.getMessage());

		}

	}



	/**
	 * Gets the content of an object from the server and puts content to a output
	 * stream
	 * 
	 * @param object
	 * @param  contentOutput OutputStream to write the content to
	 */
	public void doGet(String url,OutputStream contentOutput) {

		if(verbose)
			System.out.print("Getting " + url + " . . .  ");

		try {

			long init = System.currentTimeMillis();

			//TODO: get from the server. By now, get from the "server" data store
			byte[] content = HttpUtils.get(url);	

			//calculate time of execution
			long time = System.currentTimeMillis() - init;

			//copy content to output
			contentOutput.write(content);
			contentOutput.flush();

			if(verbose)
				System.out.println("Size = " + content.length + " bytes, execution time "+time +" milseconds");

			//Account for the bytes received and the execution time
			requests += 1;
			executionTime += time;
			bytesReceived += content.length;

		} catch (Exception e) {
			System.err.println(" Exception getting content from " + url + ": "+e.getMessage());

		}

	}

	/**
	 * Gets an object. If in verbose mode, sends content to standard output, 
	 * otherwise ignores content
	 * 
	 * @param object
	 */
	public void doGet(String object){
		doGet(object, new NullOutputStream());
	}

	/**
	 * Gets an object and redirects content to the given file
	 * 
	 * @param file a String with the file content
	 */
	public void doGet(String object,String contentFile){

		try {
			OutputStream contentOutput = new FileOutputStream(contentFile);
			doGet(object,contentOutput);
		} catch (FileNotFoundException e) {
			System.err.println("Exception getting object [" +object + ": " + 
					"Can't open content file: " + contentFile );
		}
	}

	
	/**
	 * Puts the content of a local object to the server
	 * 
	 * @param object name of the local object
	 * @param content byte array with the content
	 */
	public void doPut(String url,byte[] content) {

		if(verbose)
			System.out.print("Putting " + url + " . . .  ");

		try {			

			//TODO: put to the server. By now, just store in the "server" data store.
			long init = System.currentTimeMillis();

			int rc = HttpUtils.put(url,content);

			//	calculate time of execution
			long time = System.currentTimeMillis() - init;

			if(verbose)
				System.out.println("\nReturn code " + rc +". Size = " + content.length + " bytes, execution time "+time +" milseconds");

			//Account for the bytes sent
			requests += 1;
			executionTime += time;
			bytesSent+= content.length;

		} catch (Exception e) {
			System.err.println(" Exception putting conent to [" + url + ": "+e.getMessage());
		}

	}

	/**
	 * Puts the content from an input Stream to the server
	 * 
	 * @param object
	 * @param contentStream
	 */
	public void doPut(String url,InputStream contentStream){

		byte[] content;
		try {
			content = FileUtils.readBytesFromStream(contentStream);
			doPut(url,content);
		} catch (IOException e) {
			System.err.println(" Exception accessing content for [" + url+ ": "+e.getMessage());
		}


	}

	/**
	 * Reads the content from a file and puts to the server
	 * @param object
	 * @param contentFile
	 */
	public void doPut(String object,String contentFile){

		try {
			doPut(object,new FileInputStream(contentFile));
		} catch (FileNotFoundException e) {
			System.err.println(" Exception putting object [" + object + ": " +
					" file " + contentFile + "not found");
		}

	}

	


	/**
	 * Sets the verbosity of the client
	 * 
	 * @param isVerbose
	 */
	public void setVerbose(boolean isVerbose) {
		this.verbose = isVerbose;
	}



	/**
	 * Process the command line arguments and initiates the execution.
	 * 
	 * @param args an String array with the arguments from the command line.
	 */
	public void start(String[] args) throws Exception{

		Map<String,String>arguments = new HashMap<String,String>();
		
		try{
			utils.CmdLineArgs.getArguments(arguments, args);
		}catch(IllegalArgumentException e) {
			System.err.println("Syntax error in the arguments " + e.getMessage());
			System.exit(1);
		}


	
		//Check server argument (mandatory for execution

		String url = arguments.get("u");
		if(url == null) {
			System.err.println("Error. Url must be specified");
			System.exit(1);
		}


		//check the command to execute
		String command = arguments.get("m");

		if(command == null){
			System.err.println("Error. No command specified ");
		}
		
		
	
		if(arguments.keySet().contains("v")){
			setVerbose(true);
		}
		
		
		//check the command to execute
		String file = arguments.get("f");

		executeCommand(url,command,file);

	}

	/**
	 * Executes a command on a client
	 * @param command
	 */
	protected void executeCommand(String url,String command,String file) throws Exception{

		if(command.equalsIgnoreCase("GET")){

			if(file != null){
				doGet(url,new FileOutputStream(file)); 

			}
			else{				    
				doGet(url,System.out);
			}

		}else if (command.equalsIgnoreCase("PUT")){

			//check content from file
			if(file != null){
				doPut(url,new FileInputStream(file)); 
			} 
			else{
				System.out.println("Introduce content ending with with Ctrl-D.");
				doPut(url,System.in);
			}

		}else if(command.equalsIgnoreCase("DELETE")){
			doDelete(url);	

		}else {

			System.err.println("Error. Invalid command specified: " + command);
		}
	}

	protected void executeBurst(){
		throw new UnsupportedOperationException();


	}

	/**
	 * Main method. Creates a Driver and initiates its execution.
	 * 
	 * @param args a String array with the command line arguments
	 */
	public static void main(String[] args) {
		try {
			new HttpTest().start(args);
		} catch (Exception e) {
			System.out.println("Exception startin driver");
			e.printStackTrace(System.err);
		}
	}
}



