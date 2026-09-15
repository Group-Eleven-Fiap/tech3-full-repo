package br.com.fiap.ms_notificacao.service;

import br.com.fiap.ms_notificacao.dto.ConsultaNotificacaoDTO;
import br.com.fiap.ms_notificacao.exception.NotificacaoConfigurationException;
import br.com.fiap.ms_notificacao.exception.NotificacaoTransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    public EmailService(JavaMailSender mailSender, EmailTemplateService emailTemplateService) {
        this.mailSender = mailSender;
        this.emailTemplateService = emailTemplateService;
    }

    @Override
    public void enviar(ConsultaNotificacaoDTO dto) {
        try {
            var mensagem = emailTemplateService.criarMensagem(dto);
            mailSender.send(mensagem);
        } catch (MailAuthenticationException e) {
            log.error("email.falha-configuracao consultaId={} destinatario={} motivo=autenticacao SMTP recusada; " +
                            "configure SPRING_MAIL_USERNAME e SPRING_MAIL_PASSWORD com uma senha de aplicativo do Gmail",
                    dto.idConsulta(), dto.emailPaciente());
            throw new NotificacaoConfigurationException(
                    "Credenciais SMTP inválidas ou ausentes para o envio da notificação", e);
        } catch (MailException e) {
            throw new NotificacaoTransientException("Falha transitória ao enviar e-mail para " + dto.emailPaciente(), e);
        }
    }
}
