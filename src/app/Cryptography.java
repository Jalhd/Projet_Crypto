package app;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
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
	
	public Cryptography(String inputFile,String outputFile, String password,int cipherMode) throws IllegalBlockSizeException, BadPaddingException, IOException {
		this.init(password);
		this.filesToBytes(inputFile,cipherMode);	
		if(cipherMode == Cipher.ENCRYPT_MODE) {
			this.cipher(outputFile);	
		}
		else if(cipherMode == Cipher.DECRYPT_MODE){
			this.decipher(outputFile);
		}
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
	
	public void init(String password) {     
		try {			
			KeyManager km = new KeyManager();		
			this.key = km.keyGen(password,"monSaltMagnifique");
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		}   
	}
	
	public void cipher(String outputFile) throws IllegalBlockSizeException, BadPaddingException, IOException {	
		try {
			this.cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, this.key);	
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] tmp = new byte[16];
			for(int i=0;i<(this.inputData.length/16)-1;i++) {
				if(i == 0) {
					tmp = this.cipher.doFinal(Arrays.copyOfRange(this.inputData, 0, 16));
				}
				else {
					tmp = this.cipher.doFinal(xor(Arrays.copyOfRange(this.inputData, i*16 , (i+1)*16),tmp));
				}
				baos.write(tmp);
			}
			if(this.inputData.length%16 != 0) {
				ctsCipher(baos, tmp);
			}
			this.inputData = baos.toByteArray();
		    try (FileOutputStream stream = new FileOutputStream(outputFile)) {
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


	
	public void decipher(String outputFile) {
		try {
			this.decipher = Cipher.getInstance("AES/ECB/NoPadding");
			decipher.init(Cipher.DECRYPT_MODE, this.key);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] tmp = new byte[16];
			byte[] previousCipheredBlock = new byte[16];
			for(int i=0;i<(this.inputData.length/16)-1;i++) {
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
		    try (FileOutputStream stream = new FileOutputStream(outputFile)) {
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
	
	public byte[] xor(byte[] array1,byte[] array2) {
		byte[] result = new byte[16];
		for(int i=0;i<array1.length;i++) {
			result[i] = (byte) (array1[i] ^ array2[i]);
		}
		return result;
	}
		
	public static void main(String[] args) throws IllegalBlockSizeException, BadPaddingException, IOException {
		CommandParser cp = new CommandParser(args);
		Cryptography c = new Cryptography(cp.input,cp.output,cp.password,cp.cipherMode);
	}
	
}
