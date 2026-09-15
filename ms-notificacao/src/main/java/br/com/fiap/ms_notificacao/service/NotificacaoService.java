package br.com.fiap.ms_notificacao.service;

import br.com.fiap.ms_notificacao.dto.ConsultaNotificacaoDTO;
import br.com.fiap.ms_notificacao.exception.NotificacaoTransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final EmailSender emailSender;
    private final NotificacaoLogger notificacaoLogger;

    public NotificacaoService(EmailSender emailSender, NotificacaoLogger notificacaoLogger) {
        this.emailSender = emailSender;
        this.notificacaoLogger = notificacaoLogger;
    }

    public void processarLembreteConsulta(ConsultaNotificacaoDTO dto) {
        ConsultaNotificacaoDTO.validar(dto);

        log.info(
            "notificacao.inicio consultaId={} tipoAcao={} paciente={} email={}",
            dto.idConsulta(),
            dto.tipoAcao(),
            dto.nomePaciente(),
            dto.emailPaciente()
        );

        try {
            enviar(dto);
            log.info(
                "notificacao.sucesso consultaId={} tipoAcao={} paciente={}",
                dto.idConsulta(),
                dto.tipoAcao(),
                dto.nomePaciente()
            );
            notificacaoLogger.print(dto);
        } catch (IllegalArgumentException e) {
            log.warn(
                "notificacao.dados-invalidos consultaId={} tipoAcao={} motivo={}",
                dto.idConsulta(),
                dto.tipoAcao(),
                e.getMessage()
            );
            throw e;
        } catch (Exception e) {
            log.error(
                "notificacao.falha-definitiva consultaId={} tipoAcao={} motivo={}",
                dto.idConsulta(),
                dto.tipoAcao(),
                e.getMessage(),
                e
            );
            throw new IllegalStateException("Não foi possível processar a notificação da consulta " + dto.idConsulta(), e);
        }
    }

    @Retryable(
        retryFor = NotificacaoTransientException.class,
        maxAttemptsExpression = "${app.retry.max-attempts:3}",
        backoff = @Backoff(delayExpression = "${app.retry.delay-ms:1000}", multiplierExpression = "${app.retry.multiplier:2}")
    )
    public void enviar(ConsultaNotificacaoDTO dto) {
        try {
            emailSender.enviar(dto);
        } catch (NotificacaoTransientException e) {
            log.warn(
                "notificacao.falha-transitoria consultaId={} tipoAcao={} motivo={}",
                dto.idConsulta(),
                dto.tipoAcao(),
                e.getMessage()
            );
            throw e;
        }
    }

    @Recover
    public void recover(NotificacaoTransientException e, ConsultaNotificacaoDTO dto) {
        log.error(
            "notificacao.retry-excedido consultaId={} tipoAcao={} paciente={} motivo={}",
            dto.idConsulta(),
            dto.tipoAcao(),
            dto.nomePaciente(),
            e.getMessage()
        );
        throw new IllegalStateException("Retry excedido ao processar a notificação da consulta " + dto.idConsulta(), e);
    }

}