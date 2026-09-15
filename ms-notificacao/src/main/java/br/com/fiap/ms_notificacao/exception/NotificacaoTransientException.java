package br.com.fiap.ms_notificacao.exception;

public class NotificacaoTransientException extends RuntimeException {

    public NotificacaoTransientException(String message) {
        super(message);
    }

    public NotificacaoTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
