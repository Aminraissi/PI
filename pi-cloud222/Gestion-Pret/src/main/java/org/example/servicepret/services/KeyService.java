package org.example.servicepret.services;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

public class KeyService {

    //  génération clé AES 128
    public static String generateDataKey() throws Exception {

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);

        SecretKey key = keyGen.generateKey();

        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}