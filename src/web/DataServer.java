package web;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.configuration.Configuration;

import utils.FileUtils;
import utils.HttpUtils.HttpException;

public class DataServer extends WebServer {


	/**
	 * Path to the Object directory
	 */
	private String path;


	public DataServer() {
		super();
	}

	public DataServer(String address, String urlPath, int port, int range,String path) {
		super(address, urlPath, port, range);
		this.path = path;
	}

	public DataServer(String path){
		super();
		this.path = path;
	}





	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
		try {
			FileUtils.createDirectory(path);
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid path " + path);
		}
	}



	@Override
	protected void delete(String object) throws HttpException{
	
		try {
			FileUtils.deleteFile(path,object);
			
		} catch (FileNotFoundException e) {
			new HttpException(HTTP_NOT_FOUND);
		} catch (IOException e) {
			new HttpException(HTTP_SERVER_ERROR);
		}
	}

	@Override
	protected byte[] getContent(String object) throws HttpException {
		
		try {
			
			byte[] content = FileUtils.readBytesFromFile(path,object);			
			return content;
			
		} catch (FileNotFoundException e) {
			throw new HttpException(HTTP_NOT_FOUND);
		} catch (IOException e) {
			throw new HttpException(HTTP_SERVER_ERROR);
		}
	}

	@Override
	protected void putContent(String object, byte[] content) throws HttpException{
		
		try {
			FileUtils.writeBytesToFile(path,object, content);
		} catch (FileNotFoundException e) {
			throw new HttpException(HTTP_NOT_FOUND);
		} catch (IOException e) {
			new HttpException(HTTP_SERVER_ERROR);
		}
	}


	@Override
	public void start(Configuration config){

		setPath(config.getString("path", System.getProperty("user.dir")));

		super.start(config);

	}


	public static void main(String[] args){
		new DataServer().start(args);
	}


}
