package app.one.secondvpnlite.security;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
 
public class AppSecurityManager {
    /*
    //Sample Code
    public static void main(String[] args)  throws Exception{
        String msg = "123456";
        String keyStr = "abcdef";
        String ivStr = "ABCDEF";
        
        byte[] msg_byte = msg.getBytes("UTF-8");
        System.out.println("Before Encrypt: " + msg);
        
        byte[] ans = Decryption.encrypt(ivStr, keyStr, msg.getBytes());
        System.out.println("After Encrypt: " + new String(ans, "UTF-8"));
        
        String ansBase64 = Decryption.encryptStrAndToBase64(ivStr, keyStr, msg);
        System.out.println("After Encrypt & To Base64: " + ansBase64);
        
        byte[] deans = Decryption.decrypt(ivStr, keyStr, ans);
        System.out.println("After Decrypt: " + new String(deans, "UTF-8"));
        
        String deansBase64 = Decryption.decryptStrAndFromBase64(ivStr, keyStr, ansBase64);
        System.out.println("After Decrypt & From Base64: " + deansBase64);}
    */

    public static final String k1  = new String(new byte[]{87,97,115,106,100,101,105,106,115,64,47});
    public static final String k2  = new String(new byte[]{-61,-121,80,-61,-93,111,79,102,50,51,49}) ;
    public static final String k3  = new String(new byte[]{35,36,37,-62,-88,38,42,40,41,95,113});
    public static final String k4  = new String(new byte[]{113,117,38,105,74,111,62});
    public static final String k5  = "ç";

    public static final String iv1 = new String(new byte[]{52,48,48,50,57,56,50});
    public static final String iv2 = new String(new byte[]{68,79,83,84,84,65,83});
    public static final String ivf = new String(new byte[]{77,85,78,100,105,64,-61,-121});

    public static final String a = "a";
    public static final String i= "b";

    public static byte[] encrypt(String ivStr, String keyStr, byte[] bytes) throws Exception{
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(ivStr.getBytes());
        byte[] ivBytes = md.digest();
        
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        sha.update(keyStr.getBytes());
        byte[] keyBytes = sha.digest();
        
        return encrypt(ivBytes, keyBytes, bytes);
    }
    
    static byte[] encrypt(byte[] ivBytes, byte[] keyBytes, byte[] bytes) throws Exception{
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
        return cipher.doFinal(bytes);
    }
        
    public static byte[] decrypt(String ivStr, String keyStr, byte[] bytes) throws Exception{
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(ivStr.getBytes());
        byte[] ivBytes = md.digest();
        
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        sha.update(keyStr.getBytes());
        byte[] keyBytes = sha.digest();
        
        return decrypt(ivBytes, keyBytes, bytes);
    }
    
    static byte[] decrypt(byte[] ivBytes, byte[] keyBytes, byte[] bytes)  throws Exception{
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
        return cipher.doFinal(bytes);
    }
    
    public static String encryptStrAndToBase64(String ivStr, String keyStr, String enStr) throws Exception{
        byte[] bytes = encrypt(keyStr, keyStr, enStr.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.encode(bytes ,Base64.DEFAULT), StandardCharsets.UTF_8);
    }
    
    public static String decryptStrAndFromBase64(String ivStr, String keyStr, String deStr) throws Exception{
        byte[] bytes = decrypt(keyStr, keyStr, Base64.decode(deStr.getBytes(StandardCharsets.UTF_8),Base64.DEFAULT));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}