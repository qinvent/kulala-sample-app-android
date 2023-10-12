package qi.ble.communication.keycore;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {

    /**
     *AES encryption
     * @param info Content to be encrypted
     * @param encryptKey encryption key
     * @return encrypted byte[]
     * @throwsException
     */
    public static byte[] AESgenerator(String info, String encryptKey)   {
        if(encryptKey == null  || encryptKey.equals(""))return null;
        try{
            String iv        = "0102030405060708";
            Cipher cipher    = Cipher.getInstance("AES/CBC/NoPadding");
            int    blockSize = cipher.getBlockSize();

            byte[] dataBytes = info.getBytes();
            int plaintextLength = dataBytes.length;
            if (plaintextLength % blockSize != 0) {
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
            }
            byte[] plaintext = new byte[plaintextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);
            SecretKeySpec   keyspec = new SecretKeySpec(encryptKey.getBytes(), "AES");
            IvParameterSpec ivspec  = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            byte[] encrypted = cipher.doFinal(plaintext);

            return encrypted;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

/**
     * Decrypt AES encrypted string
     * @param content AES encrypted content
     * @param password Password for encryption
     * @return plain text
     */
    public static byte[] decrypt(byte[] content, String password) {
        try {
            String iv        = "0102030405060708";
           //Create AES key producer
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
           //Use the user password as a random number to initialize a 128-bit key producer. SecureRandom is to produce a secure random number sequence, password.getBytes() is the seed
            // As long as the seeds are the same, the sequence is the same, so decryption only requires a password.
            keyGenerator.init(128, new SecureRandom(password.getBytes()));
           // Generate a key based on the user's password
            SecretKey secretKey = keyGenerator.generateKey();
           // Return the key in basic encoding format. Returns null if this key does not support encoding
            byte[] enCodeFormat = secretKey.getEncoded();
            
            //Convert to AES private key
            SecretKeySpec secretKeySpec = new SecretKeySpec(password.getBytes(),"AES");
           //Create cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            IvParameterSpec ivspec  = new IvParameterSpec(iv.getBytes());
           // Initialize the cipher to decryption mode
            cipher.init(Cipher.DECRYPT_MODE,secretKeySpec,ivspec);
           // Encryption

            // Return plain text
            return cipher.doFinal(content);
        } catch (NoSuchAlgorithmException | BadPaddingException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
