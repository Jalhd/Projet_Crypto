package app;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileManager {
	
    private static final int BUFFER_SIZE = 4096;
    public ArrayList<String> fileNameList = new ArrayList<String>();
    
	public FileManager(){
	}
	
	public ArrayList<byte[]> filesToListOfBytes(String filepath) {
		ArrayList<byte[]> allData = new ArrayList<byte[]>();

		  File dir = new File(filepath);
		  File[] directoryListing = dir.listFiles();
		  if (directoryListing != null) {
		    for (File child : directoryListing) {
		      try {
		    	fileNameList.add(child.getName());
				allData.add(Files.readAllBytes(Paths.get(child.getPath())));
			  } catch (IOException e) {
				e.printStackTrace();
			  	}
		    }
		  }
		  return allData;
	}
	
	public void zipToFiles(String zipFilePath, String destDirectory) throws IOException {
	    File destDir = new File(destDirectory);
	    if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                extractFile(zipIn, filePath);
            } else {
               System.out.println("No directory is accepted in the zip");
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
	}
	
    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    public File zipDirectory(String directoryToZip,String zipName) throws IOException {
    	  File zipfile = new File(directoryToZip);
    	  File[] listOfFiles = zipfile.listFiles();
    	    // Create a buffer for reading the files
    	    byte[] buf = new byte[1024];
    	 	   try {
       	        // create the ZIP file
       	        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipName));
       	        // compress the files
       	        for(File filepath : listOfFiles) {
       	            FileInputStream in = new FileInputStream(filepath);
       	            // add ZIP entry to output stream
       	            out.putNextEntry(new ZipEntry(filepath.getName()));
       	            // transfer bytes from the file to the ZIP file
       	            int len;
       	            while((len = in.read(buf)) > 0) {
       	                out.write(buf, 0, len);
       	            }
       	            // complete the entry
       	            out.closeEntry();
       	            in.close();
       	        }
       	        out.close();
       	        return zipfile;
       	    } catch (IOException ex) {
       	        System.err.println(ex.getMessage());
       	    }
    	    return null;         
    }
       
    public void recursiveDelete(File file) {
        if (!file.exists())
            return;
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                //call recursively
                recursiveDelete(f);
            }
        }
        file.delete();
    }
    
    public void createDirectory(String directoryName) {
	    File destDir = new File(directoryName);
	    if (!destDir.exists()) {
            destDir.mkdir();
        }
    }

	public ArrayList<String> getFileNameList() {
		return fileNameList;
	}

	public void setFileNameList(ArrayList<String> fileNameList) {
		this.fileNameList = fileNameList;
	}
}
