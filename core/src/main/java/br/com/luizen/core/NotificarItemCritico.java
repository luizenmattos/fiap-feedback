package br.com.luizen.core;

import br.com.luizen.core.ports.INotificadorEmail;

public class NotificarItemCritico {

    public static void executar(String mensagem, INotificadorEmail notificadorEmail) {
        notificadorEmail.notificarItemCritico(mensagem);
    }
}
