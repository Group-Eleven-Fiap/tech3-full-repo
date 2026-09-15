package br.com.fiap.ms_notificacao.service;

import br.com.fiap.ms_notificacao.dto.ConsultaNotificacaoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class ConsoleNotificacaoLogger implements NotificacaoLogger {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificacaoLogger.class);

    private final EmailTemplateService emailTemplateService;

    public ConsoleNotificacaoLogger(EmailTemplateService emailTemplateService) {
        this.emailTemplateService = emailTemplateService;
    }

    @Override
    public void print(ConsultaNotificacaoDTO dto) {
        if (dto == null) {
            log.info("notificacao.console mensagem nula");
            return;
        }

        try {
            SimpleMailMessage mensagem = emailTemplateService.criarMensagem(dto);

            String destinatarios = mensagem.getTo() != null ? String.join(",", mensagem.getTo()) : dto.emailPaciente();
            String assunto = mensagem.getSubject();
            String corpo = mensagem.getText();

            System.out.println("---- NOTIFICAÇÃO (EMAIL) ----");
            System.out.println("Para: " + destinatarios);
            System.out.println("Assunto: " + (assunto == null ? "(sem assunto)" : assunto));
            System.out.println("Corpo:\n" + (corpo == null ? "" : corpo));
            System.out.println("-----------------------------");

        } catch (Exception e) {
            log.warn("notificacao.console falha ao criar mensagem consultaId={} motivo={}", dto.idConsulta(), e.getMessage());
        }
    }
}