package net.jlxip.lanclipboard;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoFunctions {
	public static final String HASH_ALGORITHM = "SHA-256";
	public static String hash(String password, String SALT) {
		password += SALT;
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance(HASH_ALGORITHM);
			md.update(password.getBytes());
		} catch (Exception e) {}	// This shouldn't be called.
		
		String Hpassword = String.format("%064x", new java.math.BigInteger(1, md.digest()));
		Hpassword = Hpassword.substring(0, 16);
		
		return Hpassword;
	}
	
	public static final int SALT_LENGTH = 16;
	public static String generateRandomSalt() {
		final Random r = new SecureRandom();
		byte[] Bsalt = new byte[SALT_LENGTH];
		r.nextBytes(Bsalt);
		return new String(Bsalt);
	}
	
	
	// AES
	// Main source: https://goo.gl/c9FsFY
	public static final String PASSWORD_CONFIRMATION_STRING = "CORRECT";
	public static byte[] AESencrypt(String key, String initVector, byte[] value) {
		try {
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes());
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

			return cipher.doFinal(value);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}
	
	public static byte[] AESdecrypt(String key, String initVector, byte[] encrypted) {
		try {
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes());
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

			return cipher.doFinal(encrypted);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}
}
