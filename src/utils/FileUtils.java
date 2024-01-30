package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Utility functions to manipulate files
 * 
 * @author Pablo Chacin
 *
 */
public class FileUtils {

	/**
	 * Read the entire content of a stream and returns it as a byte array
	 * 
	 * @param stream
	 * @return a byte array with the content from the stream
	 * @throws IOException
	 */
	public static byte[] readBytesFromStream(InputStream stream) throws IOException {
		
		
		//buffer for content from stream
		StringBuffer contentBuffer = new StringBuffer();

		InputStreamReader contentReader = new InputStreamReader(stream);
		
		//small buffer to read ahead chars
		char[] buffer = new char[128];

		//read and append while not EOF (read == -1)
		int read;

		while( (read = contentReader.read(buffer)) != -1){
			contentBuffer.append(buffer,0,read);
		}
		
		return contentBuffer.toString().getBytes();

	}
	
	
	public static byte[] readBytesFromFile(String path,String file) throws FileNotFoundException, IOException{
		return readBytesFromFile(path + File.separator + file);
	}
	
	public static byte[] readBytesFromFile(String path) throws FileNotFoundException, IOException{

			FileInputStream ios = new FileInputStream(path);
			return readBytesFromStream(ios);
	}
	
	
	public static void writeBytesToFile(String directory,String file,byte[] content) throws FileNotFoundException,IOException{
		
		writeBytesToFile(directory + File.separator + file,content);
		
	}

	public static void writeBytesToFile(String path,byte[] content) throws IOException{
			File file = new File(path);
			FileOutputStream fos = new FileOutputStream(file);
			writeBytesToStream(fos,content);
	}
	
	
	public static void writeBytesToStream(OutputStream out,byte[] content) throws IOException{
		out.write(content);
		out.close();
	}
	
	public static String[] listDirectory(String path){
		File directory = new File(path);		
		return directory.list();
	}
	
	
	public static void createDirectory(String path) throws IOException{
		
		File directory = new File(path);
		if(!directory.exists()) {
			boolean created = directory.mkdir();
			if(!created) {
				throw new IOException("Can't create data store directory ["+path+"]");
			}
		}
	}
	
	
	public static void eraseDirectory(String path) throws IOException{
		File directory = new File(path);
		File[] files = directory.listFiles();
		for(File f: files) {
			boolean deleted = f.delete();
			if(!deleted) {
				throw new IOException("Cannot delete object ["+f.getName()+"]");
			}
		}
	}
	
	

	public static void deleteFile(String path,String file) throws FileNotFoundException, IOException{

		deleteFile(path + File.separator + file);
	}
	
	public static void deleteFile(String path) throws FileNotFoundException, IOException{
		
		File file = new File(path);
		
		if(!file.exists()){
			throw new FileNotFoundException("File " + path + " not found");
		}
		
		if(file.isDirectory()){
			throw new IOException(path + " is a directory");
		}
		
		boolean deleted = file.delete();
		
		if(!deleted){
			throw new IOException("File " + path + " can't be deleted");
		}
		
	}
}
