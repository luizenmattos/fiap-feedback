package br.com.luizen.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ValidadorAutenticacao {

    // Para trocar a chave: echo -n "nova-chave" | sha256sum
    private static final String HASH_ESPERADO = "2084c8f57f806e1e13511f560bde9efd29897b20acfa44caf2a2281033904652";

    public static boolean validar(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        return sha256(apiKey).equals(HASH_ESPERADO);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 não disponível", e);
        }
    }
}
