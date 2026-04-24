package com.ib.urireg;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

public class TestIt {

	private static final String SecretKeySpec = "my_super_secret_key_ho_ho_ho абвгде";
	private static final String SALT = "ssshhhhh_абвгде";


	public static void main(String[] args) {


		System.out.println("2---------------------------------------");



		String originalString = "The quick brown fox jumps over the едно магаре.";

	    String encryptedString = encrypt(originalString);


	    String decryptedString = decrypt(encryptedString);

	    System.out.println(originalString);
	    System.out.println(encryptedString);
	    System.out.println(decryptedString);

	}


	public static String encrypt(String strToEncrypt) {
	    try {
	      byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	      IvParameterSpec ivspec = new IvParameterSpec(iv);

	      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
	      KeySpec spec = new PBEKeySpec(SecretKeySpec.toCharArray(), SALT.getBytes(), 2048, 256);
	      SecretKey tmp = factory.generateSecret(spec);


//	      return toHex(tmp.getEncoded());

	      //-----------------------------------------------------------------
	      SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

	     // SecretKey key = KeyGenerator.getInstance("AES").generateKey();

	      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // AES/CBC/PKCS5PADDING //DES/CBC/PKCS5Padding
	      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);

	      return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));

	    } catch (Exception e) {
	      System.out.println("Error while encrypting: " + e.toString());
	    }
	    return null;
	  }


	 public static String decrypt(String strToDecrypt) {
	    try {
	      byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	      IvParameterSpec ivspec = new IvParameterSpec(iv);

	      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
	      KeySpec spec = new PBEKeySpec(SecretKeySpec.toCharArray(), SALT.getBytes(), 2048, 256);
	      SecretKey tmp = factory.generateSecret(spec);
	      SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

	    //  SecretKey key = KeyGenerator.getInstance("AES").generateKey();

	      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
	      cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
	      return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
	    } catch (Exception e) {
	      System.out.println("Error while decrypting: " + e.toString());
	    }
	    return null;
	  }





}
