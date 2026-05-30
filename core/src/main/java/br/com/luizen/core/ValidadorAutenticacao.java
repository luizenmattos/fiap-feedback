package br.com.luizen.core;

public class ValidadorAutenticacao {


    public static boolean validar(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        String senhaEsperada = System.getenv("API_KEY");
        return apiKey.equals(senhaEsperada);
    }

}
