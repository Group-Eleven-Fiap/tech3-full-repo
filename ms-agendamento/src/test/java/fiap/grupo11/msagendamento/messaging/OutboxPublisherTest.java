package fiap.grupo11.msagendamento.messaging;

import fiap.grupo11.msagendamento.entity.OutboxEvent;
import fiap.grupo11.msagendamento.entity.OutboxStatus;
import fiap.grupo11.msagendamento.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void successfulPublicationMarksTheEventDelivered() {
        OutboxEvent event = event();
        when(repository.findReadyToPublish(any(), any())).thenReturn(List.of(event));
        OutboxPublisher publisher = publisher();

        publisher.publishPending();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DELIVERED);
        assertThat(event.getDeliveredAt()).isNotNull();
        verify(rabbitTemplate).convertAndSend(
                eq("agendamento.events"), eq("agendamento.appointment.changed"), eq("{}"),
                any(MessagePostProcessor.class));
        verify(repository).save(event);
    }

    @Test
    void brokerFailureLeavesTheEventPendingAndSchedulesRetry() {
        OutboxEvent event = event();
        when(repository.findReadyToPublish(any(), any())).thenReturn(List.of(event));
        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitTemplate).convertAndSend(
                        eq("agendamento.events"), eq("agendamento.appointment.changed"), eq("{}"),
                        any(MessagePostProcessor.class));
        OutboxPublisher publisher = publisher();

        publisher.publishPending();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
        verify(repository).save(event);
    }

    @Test
    void publicationMarksThePayloadAsJson() {
        OutboxEvent event = event();
        when(repository.findReadyToPublish(any(), any())).thenReturn(List.of(event));
        OutboxPublisher publisher = publisher();

        publisher.publishPending();

        var postProcessor = org.mockito.ArgumentCaptor
                .forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq("agendamento.events"), eq("agendamento.appointment.changed"), eq("{}"), postProcessor.capture());

        Message message = postProcessor.getValue().postProcessMessage(
                new Message("{}".getBytes(), new org.springframework.amqp.core.MessageProperties()));
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
    }

    private OutboxPublisher publisher() {
        return new OutboxPublisher(repository, rabbitTemplate,
                "agendamento.events", "agendamento.appointment.changed", 30);
    }

    private OutboxEvent event() {
        return new OutboxEvent(1L, 10L, "APPOINTMENT_CREATED", "{}");
    }
}