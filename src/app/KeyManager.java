package app;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyManager {
	
	public KeyManager() {
		
	}
	
	public Key keyGen(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
			if(checkPassword(password) && password.length() >= 22) {
				SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
				KeySpec sp = new PBEKeySpec(password.toCharArray(),salt.getBytes(),5000,128);	
				SecretKey tmp = skf.generateSecret(sp);
				Key k = new SecretKeySpec(tmp.getEncoded(), "AES");
				return k;
			}else {
				System.err.println("Password too short or invalid characters");
				System.exit(1);
			}
			return null;
	}
	
	public SecretKeySpec  hmacGen(Key key) {
			SecretKeySpec secretKey = new SecretKeySpec(key.getEncoded(), "HmacSHA256"); 
			return secretKey ;		
	}
	
	public boolean checkPassword(String password) {
		return password.matches("[a-zA-Z0-9]+");
	}
}
