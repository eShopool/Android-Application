package com.eShopool.AndroidApp.Library;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to achieve RSA encryption.
 */
public class RSAUtil {
    /**
     * This method is used to recover the public key from the base64 encoded public key.
     * @param key_base64 key string.
     * @return public key.
     * @throws Exception exception.
     */
    public static PublicKey getPulbickey(String key_base64) throws Exception{
        byte[] pb = Base64.getDecoder().decode(key_base64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pb);
        KeyFactory keyfactory = KeyFactory.getInstance("RSA");
        return keyfactory.generatePublic(keySpec);
    }


    /**
     * This method is used to perform public key cryptography.
     * @param key key.
     * @param source the message that needs to be encrypted.
     * @return message in byte format.
     * @throws Exception exception.
     */
    public static byte[] encrypt(Key key,byte[] source) throws Exception{
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] ciphertext = cipher.doFinal(source);
        return ciphertext;
    }

    /**
     * This method is used to get crypto text.
     * @param publicKey public key.
     * @param text the message.
     * @return decrypted string.
     */
    public static String getCryptoText(String publicKey, String text) {
        byte[] cryptograph = new byte[0];
        try {
            cryptograph = encrypt(getPulbickey(publicKey),text.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(Base64.getEncoder().encode(cryptograph));
    }

}
