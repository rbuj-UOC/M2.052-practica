package utils;

import java.util.Map;

/**
 * Handles command line arguments
 * 
 * @author Pablo Chacin
 *
 */
public class CmdLineArgs {

	/**
	 * Returns a map with the valid arguments taken from the command line.
	 * 
	 * Commands are assumed to be in the form "-k [v]", where "k" is the key and
	 * "v" is an (optional) value. Keys are key sensitive ("-k" is different from "-K")
	 * 
	 * The resulting map has the k,v pairs for parameters. Notice that the trailing "-" is 
	 * removed from parameter's key. 
	 * 
	 * @param argsMap a Map to put the commands. 
	 * @param args arguments from the command line
	 * 
	 * @throws IllegalArgumentException if there is syntax error in the arguments
	 */
	public static void getArguments(Map<String,String> argsMap,String[] args) throws IllegalArgumentException{

		//reads all the arguments and assumes
		for(int i = 0; i < args.length;) {
			String token = args[i];
			
			//Check that token is a valid command key
			if((token.length() >= 2) && (!token.startsWith("-"))){
				throw new IllegalArgumentException("Invalid synthax. Command line option expected: " + token);
			}
			
			//remove trailing "-"
			String cmd = token.substring(1);
		
			
			String value = null;
			
			if((i<args.length -1) && !args[i+1].startsWith("-")){
				value = args[i+1];
				i++;
			}
			argsMap.put(cmd, value);	
			i++;
		}
	}
}
