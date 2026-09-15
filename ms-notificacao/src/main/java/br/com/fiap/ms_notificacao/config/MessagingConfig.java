package br.com.fiap.ms_notificacao.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    @Value("${app.messaging.exchange:agendamento.events}")
    private String exchangeName;

    @Value("${app.messaging.queue:consultas-notificacoes-queue}")
    private String queueName;

    @Value("${app.messaging.routing-key:agendamento.appointment.changed}")
    private String routingKey;

    @Value("${app.messaging.dlx-exchange:agendamento.events.dlx}")
    private String deadLetterExchangeName;

    @Value("${app.messaging.dlq-queue:consultas-notificacoes-dlq}")
    private String deadLetterQueueName;

    @Value("${app.messaging.dlq-routing-key:agendamento.appointment.changed.dlq}")
    private String deadLetterRoutingKey;

    @Bean
    public DirectExchange appointmentEventsExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(deadLetterExchangeName, true, false);
    }

    @Bean
    public Queue notificacaoQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", deadLetterExchangeName)
                .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueueName).build();
    }

    @Bean
    public Binding binding(Queue notificacaoQueue, DirectExchange appointmentEventsExchange) {
        return BindingBuilder.bind(notificacaoQueue).to(appointmentEventsExchange).with(routingKey);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(deadLetterRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}