package net.jlxip.lanclipboard;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

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
		
		
		return Hpassword;
	}
	
	public static final int SALT_LENGTH = 18;
	public static String generateRandomSalt() {
		final Random r = new SecureRandom();
		byte[] Bsalt = new byte[SALT_LENGTH];
		r.nextBytes(Bsalt);
		return new String(Bsalt);
	}
}
