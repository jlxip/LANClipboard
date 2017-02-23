package net.jlxip.lanclipboard;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;

public class CryptoFunctions {
	private static final String RSA = "RSA";
	public static KeyPair generateKeys() {
		KeyPairGenerator kpg = null;
		try {
			kpg = KeyPairGenerator.getInstance(RSA);
		} catch (NoSuchAlgorithmException e) {
			// This shouldn't be called.
		}
		
		KeyPair pair = kpg.generateKeyPair();
		
		return pair;
	}
	
	public static SealedObject encrypt(PublicKey pub, String password) {
		Cipher c = null;
		try {
			c = Cipher.getInstance(RSA);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			// This shouldn't be called.
		}
		
		try {
			c.init(Cipher.ENCRYPT_MODE, pub);
		} catch (InvalidKeyException e) {
			System.out.println("Invalid public key!");
			e.printStackTrace();
		}
		
		SealedObject encrypted = null;
		try {
			encrypted = new SealedObject(password, c);
		} catch (IllegalBlockSizeException | IOException e) {
			System.out.println("There was an error encrypting");
			e.printStackTrace();
		}
		
		return encrypted;
	}
	
	public static String decrypt(PrivateKey priv, SealedObject encrypted) {
		Cipher c = null;
		try {
			c = Cipher.getInstance(RSA);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			// This shouldn't be called.
		}
		
		try {
			c.init(Cipher.DECRYPT_MODE, priv);
		} catch (InvalidKeyException e) {
			System.out.println("Invalid private key!");
			e.printStackTrace();
		}
		
		String password = null;
		try {
			password = (String)encrypted.getObject(c);
		} catch (ClassNotFoundException | IllegalBlockSizeException | BadPaddingException | IOException e) {
			e.printStackTrace();
		}
		
		return password;
	}
}
