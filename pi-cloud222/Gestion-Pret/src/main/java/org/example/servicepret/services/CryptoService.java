package org.example.servicepret.services;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class CryptoService {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12; //  GCM
    private static final int TAG_SIZE = 128;

    private static SecretKeySpec buildKey(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(keyBytes, "AES");
    }

    //  ENCRYPTION
    public static byte[] encrypt(byte[] data, String base64Key) throws Exception {

        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(ALGO);

        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_SIZE, iv));

        byte[] encrypted = cipher.doFinal(data);

        byte[] result = new byte[iv.length + encrypted.length];

        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

        return result;
    }
    public static byte[] decrypt(byte[] encryptedData, String base64Key) throws Exception {

        Cipher cipher = Cipher.getInstance(ALGO);

        byte[] iv = Arrays.copyOfRange(encryptedData, 0, IV_SIZE);
        byte[] data = Arrays.copyOfRange(encryptedData, IV_SIZE, encryptedData.length);

        GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);

        SecretKeySpec key = buildKey(base64Key);

        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        return cipher.doFinal(data);
    }
}