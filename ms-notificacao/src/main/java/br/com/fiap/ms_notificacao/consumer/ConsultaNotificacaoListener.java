package br.com.fiap.ms_notificacao.consumer;

import br.com.fiap.ms_notificacao.dto.ConsultaNotificacaoDTO;
import br.com.fiap.ms_notificacao.exception.NotificacaoConfigurationException;
import br.com.fiap.ms_notificacao.service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConsultaNotificacaoListener {

    private static final Logger log = LoggerFactory.getLogger(ConsultaNotificacaoListener.class);

    private final NotificacaoService notificacaoService;

    @Value("${app.messaging.queue:consultas-notificacoes-queue}")
    private String queueName;

    public ConsultaNotificacaoListener(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @RabbitListener(queues = "${app.messaging.queue:consultas-notificacoes-queue}")
    public void listener(ConsultaNotificacaoDTO dto) {
        if (dto == null) {
            log.warn("mensagem.vazia fila={}", queueName);
            return;
        }

        log.info("mensagem.recebida fila={} consultaId={} tipoAcao={} paciente={}", queueName, dto.idConsulta(), dto.tipoAcao(), dto.nomePaciente());

        try {
            notificacaoService.processarLembreteConsulta(dto);
        } catch (IllegalArgumentException e) {
            log.warn("mensagem.invalidada fila={} consultaId={} tipoAcao={} motivo={}", queueName, dto.idConsulta(), dto.tipoAcao(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException(
                    "Mensagem inválida para consultaId=" + dto.idConsulta() + ": " + e.getMessage(),
                    e
            );
        } catch (NotificacaoConfigurationException e) {
            log.error("mensagem.configuracao-invalida fila={} consultaId={} tipoAcao={} motivo={}",
                    queueName, dto.idConsulta(), dto.tipoAcao(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException(
                    "Configuração de e-mail inválida para consultaId=" + dto.idConsulta(), e);
        }
    }
}
