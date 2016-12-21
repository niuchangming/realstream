package tools;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class AES {
	private static final String DEFAULT_KEY = "eK0oLab*_*Sw11t";
    private static byte[] key ;
    public static SecretKeySpec getKey(String myKey){
    	if(Utils.isBlank(myKey)){
    		myKey = DEFAULT_KEY;
    	}
        MessageDigest sha = null;
        SecretKeySpec secretKey = null;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
        	e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
        	e.printStackTrace();
        }
        return secretKey;
    }
    
    public static String encrypt(String strToEncrypt, String key){
        try{
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey(key));
            return Base64.encodeBase64String(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        }catch (Exception e){
           e.printStackTrace();
        }
        return null;
    }
    
    public static String decrypt(String strToDecrypt, String key){
        try{
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(key));
            return new String(cipher.doFinal(Base64.decodeBase64(strToDecrypt)));
        }catch (Exception e){
        	e.printStackTrace();e.printStackTrace();
        }
        return null;
    }
    
}
