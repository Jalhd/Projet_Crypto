package app;

import java.text.ParseException;

import javax.crypto.Cipher;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CommandParser {
	
	public String input;
	public String output;
	public String password;
	public int cipherMode;
	
	public CommandParser(String[] args) {
	       Options options = new Options();
	       
	       Option input = new Option("enc", "<cipher mode>", false, "-enc or -dec");
	       input.setRequired(false);
	       options.addOption(input);
	       
	       input = new Option("dec", "<cipher mode>", false, "-enc or -dec");
	       input.setRequired(false);
	       options.addOption(input);
	       
	       input = new Option("in", "<input file>", true, "file to cipher/decipher");
	       input.setRequired(true);
	       options.addOption(input);
	       
	       input = new Option("out", "<output file>", true, "destination file");
	       input.setRequired(true);
	       options.addOption(input);
	       
	       input = new Option("p", "password", true, "user password");
	       input.setRequired(true);
	       options.addOption(input);
	        
	       CommandLineParser parser = new DefaultParser();
	       HelpFormatter formatter = new HelpFormatter();
	       CommandLine cmd;

	       try {
	           cmd = parser.parse(options, args);
		       this.input = cmd.getOptionValue("in");
		       this.output = cmd.getOptionValue("out");
		       this.password = cmd.getOptionValue("password");
		       if(cmd.hasOption("enc")){
		    	   cipherMode = Cipher.ENCRYPT_MODE;
		       }else if(cmd.hasOption("dec")) {
		    	   cipherMode = Cipher.DECRYPT_MODE;
		       }
		   	       
	       } catch (org.apache.commons.cli.ParseException e) {
	           System.out.println(e.getMessage());
	           formatter.printHelp("utility-name", options);

	           System.exit(1);
	       }
	       
	}
	
}
