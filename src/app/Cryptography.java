package app;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


public class Cryptography {

	private Cipher cipher;
	private Cipher decipher;
	private byte[] inputData;
	private Key key;
	
	public Cryptography() throws IllegalBlockSizeException, BadPaddingException, IOException {
		
	}

	public void cryptoAlgo(String inputFile, String outputFile, int cipherMode, FileManager fm, String password)
			throws IOException, IllegalBlockSizeException, BadPaddingException {
		int k =0;
		if(cipherMode == Cipher.ENCRYPT_MODE) {
			//The input zip is dezipped into a directory resources
			fm.zipToFiles(inputFile,"resources");
		    //The output zip is created ready to host the ciphered files before the zip 
			fm.createDirectory("ciphered");
			//Iterations throught all the files in resources which will be ciphered one by one and written in the ciphered directory
			//and then the ciphered directory is zipped
			for(byte[] byteArray : fm.filesToListOfBytes("resources") ) {
				this.inputData = byteArray;
				this.cipher(password,fm.getFileNameList().get(k));		
				k++;
			}
			fm.zipDirectory("ciphered",outputFile);
		}
		else if(cipherMode == Cipher.DECRYPT_MODE){
			fm.zipToFiles(inputFile, "ciphered");
			fm.createDirectory("deciphered");
			for(byte[] byteArray : fm.filesToListOfBytes("ciphered") ) {
				this.inputData = byteArray;
				this.decipher(password,fm.getFileNameList().get(k));			
				k++;
			}
			fm.zipDirectory("deciphered",outputFile);
		}
		//Delete temporary pre-zip files
		fm.recursiveDelete(new File("ciphered"));
		fm.recursiveDelete(new File("deciphered"));
		fm.recursiveDelete(new File("resources"));
	}
	
	public void filesToBytes(String file,int cipherMode) {
		Path path = Paths.get(file);
		try {
			if(cipherMode == Cipher.ENCRYPT_MODE) {
				this.inputData = Files.readAllBytes(path);
			}
			else if(cipherMode == Cipher.DECRYPT_MODE){
				this.inputData = Files.readAllBytes(path);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void initKeyFromPassword(String password,String salt) {     
		try {			
			KeyManager km = new KeyManager();		
			this.key = km.keyGen(password,salt);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		}   
	}
	
	public void cipher(String password,String outputFile) throws IllegalBlockSizeException, BadPaddingException, IOException {	
		//The main key is derivated from the password and each file is ciphered with a new key derivated from the main key with a different salt
		this.initKeyFromPassword(password,outputFile);
		try {
			this.cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, this.key);	
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			//This flag determines if we must iterate throught all the blocks or leave the last full block and the last partial block
			int lengthFlag =0;
			if(this.inputData.length%16 != 0) {
				lengthFlag = 1;
			}			
			byte[] tmp = new byte[16];
			for(int i=0;i<(this.inputData.length/16)-lengthFlag;i++) {
				if(i == 0) {
					tmp = this.cipher.doFinal(Arrays.copyOfRange(this.inputData, 0, 16));
				}
				else {
					tmp = this.cipher.doFinal(xor(Arrays.copyOfRange(this.inputData, i*16 , (i+1)*16),tmp));
				}
				baos.write(tmp);
			}
			//Here we cipher the last full block and the last partial block following CTS Algorithm if needed
			if(this.inputData.length%16 != 0) {
				ctsCipher(baos, tmp);
			}
			this.inputData = baos.toByteArray();
		    try (FileOutputStream stream = new FileOutputStream("ciphered"+ File.separator + outputFile)) {
		      stream.write(this.inputData);
			}
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}	
	}

	private void ctsCipher(ByteArrayOutputStream baos, byte[] tmp)
			throws IllegalBlockSizeException, BadPaddingException, IOException {
		int lastBlockLength = this.inputData.length%16;
		tmp = cipher.doFinal(xor(Arrays.copyOfRange(this.inputData, this.inputData.length - lastBlockLength - 16, this.inputData.length - lastBlockLength),tmp));
		byte[] beforeLastBlockCiphered = tmp;
		byte[] lastPlainTextBlock = Arrays.copyOf(Arrays.copyOfRange(this.inputData, this.inputData.length-lastBlockLength, this.inputData.length),16);
		for(int k = lastBlockLength;k<16;k++) {
			lastPlainTextBlock[k] = 0x0;
		}
		tmp=cipher.doFinal(xor(lastPlainTextBlock,beforeLastBlockCiphered));					
		baos.write(tmp);
		baos.write(beforeLastBlockCiphered,0,lastBlockLength);
	}

	
	public void decipher(String password,String outputFile) {
		this.initKeyFromPassword(password,outputFile);
		try {
			this.decipher = Cipher.getInstance("AES/ECB/NoPadding");
			decipher.init(Cipher.DECRYPT_MODE, this.key);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] tmp = new byte[16];
			byte[] previousCipheredBlock = new byte[16];
			int lengthFlag =0;
			if(this.inputData.length%16 != 0) {
				lengthFlag = 1;
			}	
			for(int i=0;i<(this.inputData.length/16)-lengthFlag;i++) {
				if(i == 0) {
					tmp = this.decipher.doFinal(Arrays.copyOfRange(this.inputData, 0, 16));
				}else {			
					tmp = this.decipher.doFinal(Arrays.copyOfRange(this.inputData, i*16 , (i+1)*16));
					tmp = xor(tmp,previousCipheredBlock);
				}
				previousCipheredBlock = Arrays.copyOfRange(this.inputData, i*16, (i+1)*16);
				baos.write(tmp);
			}
			if(this.inputData.length%16 != 0) {	
				ctsDecipher(baos, previousCipheredBlock);			
			}
			
			byte[] deciphered = baos.toByteArray();
		    try (FileOutputStream stream = new FileOutputStream("deciphered"+ File.separator +outputFile)) {
			      stream.write(deciphered);
				}
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | IOException e) {
			e.printStackTrace();
		}	
	}

	private void ctsDecipher(ByteArrayOutputStream baos, byte[] previousCipheredBlock)
			throws IllegalBlockSizeException, BadPaddingException, IOException {
		int lastBlockLength = this.inputData.length%16;
		byte[] blockCipheredTwoTimes = this.decipher.doFinal(Arrays.copyOfRange(this.inputData,this.inputData.length - lastBlockLength - 16, this.inputData.length - lastBlockLength));
		byte[] ctsChunk = Arrays.copyOfRange(blockCipheredTwoTimes,lastBlockLength,16);
		byte[] lastBlockCiphered = Arrays.copyOfRange(this.inputData,this.inputData.length - lastBlockLength , this.inputData.length);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		output.write(lastBlockCiphered);
		output.write(ctsChunk);
		byte[] beforeLastBlockCiphered = decipher.doFinal(output.toByteArray());
		baos.write(xor(beforeLastBlockCiphered,previousCipheredBlock));
		baos.write(xor(blockCipheredTwoTimes,output.toByteArray()),0,lastBlockLength);
	}
	
	public void HMAC(String filepath) {
		Path path = Paths.get(filepath);	
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] data = Files.readAllBytes(path);
		} catch (IOException | NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public byte[] xor(byte[] array1,byte[] array2) {
		byte[] result = new byte[16];
		for(int i=0;i<array1.length;i++) {
			result[i] = (byte) (array1[i] ^ array2[i]);
		}
		return result;
	}
		
	public static void main(String[] args) throws IllegalBlockSizeException, BadPaddingException, IOException {
		CommandParser cp = new CommandParser(args);
		Cryptography c = new Cryptography();
		FileManager fm = new FileManager();
		c.cryptoAlgo(cp.input, cp.output, cp.cipherMode, fm,cp.password);
	}
	
}
