package br.com.luizen.core;

public class ValidadorAutenticacao {

    //! PROBLEMA DE SENHA EM CÓDIGO FONTE  
    /*   Como  a arquitetura atual tem objetivo ter um 'core' de dominio, a chave não pode 
    **    ser carregada por variáveis de ambiente, pois cada função serveless teria que 
    **    ter a chave, o que dificultaria a manutenção. 
    **    Opções:
    **    1. Criar uma função serveless para validar a chave, todas outras funções chamariam ela
    **    2. Criar uma função serveless para validar gerando um JWT, a outras recebem e validam com
    **    classe do CORE
    **    3. Deixar regra de validação no Gateway
    **
    **    Acredito que a melhor opção seja a 2, as regras ficam no CORE e não expõe a chave no código.
    */   

    private static final String API_KEY = "7c3f9371e1327c96ff069135205a0b669943c145bcc9833cc3caa9d5847c5c8d";

    public static boolean validar(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        return apiKey.equals(API_KEY);
    }

}
