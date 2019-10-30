package com.eShopool.AndroidApp.Library;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @Author: Roylion
 * @Description: AES algorithm package
 * @Date: Created in 9:46 2018/8/9
 */
public class AESUtil {

    /**
     * Encryption Algorithm.
     */
    private static final String ENCRY_ALGORITHM = "AES";

    /**
     * Encryption algorithm / encryption mode / padding type.
     * This example uses AES encryption, ECB encryption mode, PKCS5Padding padding.
     */
    private static final String CIPHER_MODE = "AES/ECB/PKCS5Padding";

    /**
     * Set iv offset.
     * This example uses ECB encryption mode, no need to set the iv offset.
     */
    private static final String IV_ = null;

    /**
     * Set the cryptographic character set
     * This example uses the UTF-8 character set.
     */
    private static final String CHARACTER = "UTF-8";

    /**
     * Set the encryption password processing length.
     * Less than this length is 0;
     */
    private static final int PWD_SIZE = 16;

    /**
     * Password processing method
     * If there is a problem with encryption and decryption.
     * Please check this method first, and the password length is not enough to make up "0", resulting in inconsistent passwords.
     * @param password Pending password.
     * @return
     * @throws UnsupportedEncodingException
     */
    private static byte[] pwdHandler(String password) throws UnsupportedEncodingException {
        byte[] data = null;
        if (password == null) {
            password = "";
        }
        StringBuffer sb = new StringBuffer(PWD_SIZE);
        sb.append(password);
        while (sb.length() < PWD_SIZE) {
            sb.append("0");
        }
        if (sb.length() > PWD_SIZE) {
            sb.setLength(PWD_SIZE);
        }

        data = sb.toString().getBytes("UTF-8");

        return data;
    }

    /**
     * Original encryption.
     * @param clearTextBytes Plaintext byte array, byte array to be encrypted.
     * @param pwdBytes Encrypted password byte array.
     * @return Returns the encrypted ciphertext byte array, the encryption error returns null.
     */
    public static byte[] encrypt(byte[] clearTextBytes, byte[] pwdBytes) {
        try {
            // 1 Obtain an encryption key.
            SecretKeySpec keySpec = new SecretKeySpec(pwdBytes, ENCRY_ALGORITHM);

            // 2 Get a Cipher instance
            Cipher cipher = Cipher.getInstance(CIPHER_MODE);

            // 3 Initialize the Cipher instance. Set the execution mode and encryption key.
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            // 4 Execution.
            byte[] cipherTextBytes = cipher.doFinal(clearTextBytes);

            // 5 Return ciphertext character set
            return cipherTextBytes;

        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Original decryption.
     * @param cipherTextBytes Ciphertext byte array, byte array to be decrypted.
     * @param pwdBytes Decrypt password byte array.
     * @return Returns the decrypted plaintext byte array, the decryption error returns null.
     */
    public static byte[] decrypt(byte[] cipherTextBytes, byte[] pwdBytes) {

        try {
            // 1 Get the decryption key.
            SecretKeySpec keySpec = new SecretKeySpec(pwdBytes, ENCRY_ALGORITHM);

            // 2 Get a Cipher instance.
            Cipher cipher = Cipher.getInstance(CIPHER_MODE);

            // 3 Initialize the Cipher instance. Set the execution mode and encryption key.
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            // 4 Execution.
            byte[] clearTextBytes = cipher.doFinal(cipherTextBytes);

            // 5 Return plain text character set.
            return clearTextBytes;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Decrypt error, return null.
        return null;
    }

    /**
     * HEX decryption.
     * @param cipherText Ciphertext, with decrypted content.
     * @param password Password, decrypted password.
     * @return Returns the plaintext, the content obtained after decryption. The decryption error returns null.
     */
    public static String decryptHex(String cipherText, String password) {
        try {
            // 1 Convert HEX output ciphertext to ciphertext byte array.
            byte[] cipherTextBytes = hex2byte(cipherText);

            // 2 Decrypt the ciphertext byte array to get the plaintext byte array.
            byte[] clearTextBytes = decrypt(cipherTextBytes, pwdHandler(password));

            // 3 Return plaintext string according to CHARACTER transcode.
            return new String(clearTextBytes, CHARACTER);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Decrypt error, return null.
        return null;
    }

    /* Convert byte array to hex string  */
    public static String byte2hex(byte[] bytes) {
        StringBuffer sb = new StringBuffer(bytes.length * 2);
        String tmp = "";
        for (int n = 0; n < bytes.length; n++) {
            // Integer to hexadecimal representation.
            tmp = (java.lang.Integer.toHexString(bytes[n] & 0XFF));
            if (tmp.length() == 1) {
                sb.append("0");
            }
            sb.append(tmp);
        }
        return sb.toString().toUpperCase(); // Turn to Uppercase.
    }

    /* Convert hex string to byte array */
    private static byte[] hex2byte(String str) {
        if (str == null || str.length() < 2) {
            return new byte[0];
        }
        str = str.toLowerCase();
        int l = str.length() / 2;
        byte[] result = new byte[l];
        for (int i = 0; i < l; ++i) {
            String tmp = str.substring(2 * i, 2 * i + 2);
            result[i] = (byte) (Integer.parseInt(tmp, 16) & 0xFF);
        }
        return result;
    }


}