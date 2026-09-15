package br.com.fiap.ms_notificacao.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

@Component("notificacaoRabbitErrorHandler")
public class NotificacaoRabbitErrorHandler implements RabbitListenerErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoRabbitErrorHandler.class);

    @Override
    public Object handleError(Message amqpMessage, org.springframework.messaging.Message<?> message,
                             ListenerExecutionFailedException exception) {
        String motivo = exception.getCause() != null && exception.getCause().getMessage() != null
                ? exception.getCause().getMessage()
                : exception.getMessage();

        if (motivo == null || motivo.isBlank()) {
            motivo = exception.getClass().getSimpleName();
        }

        log.warn("Mensagem descartada pelo consumidor RabbitMQ. Motivo: {}", motivo);
        log.debug("Detalhes do erro do RabbitMQ:", exception);
        return null;
    }
}
