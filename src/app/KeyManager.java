package app;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyManager {
	
	public KeyManager() {
		
	}
	
	public Key keyGen(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			KeySpec sp = new PBEKeySpec(password.toCharArray(),salt.getBytes(),5000,128);	
			SecretKey tmp = skf.generateSecret(sp);
			Key k = new SecretKeySpec(tmp.getEncoded(), "AES");
			return k;
	}
}
