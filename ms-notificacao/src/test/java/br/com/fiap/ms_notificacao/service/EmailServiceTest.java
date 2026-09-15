package br.com.fiap.ms_notificacao.service;

import br.com.fiap.ms_notificacao.dto.ConsultaNotificacaoDTO;
import br.com.fiap.ms_notificacao.enums.TipoAcao;
import br.com.fiap.ms_notificacao.exception.NotificacaoConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class EmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailTemplateService templateService = mock(EmailTemplateService.class);
    private final EmailService emailService = new EmailService(mailSender, templateService);

    @Test
    void logsTheRenderedMessageWithoutSmtpCredentials(CapturedOutput output) {
        ConsultaNotificacaoDTO consulta = new ConsultaNotificacaoDTO(
                10L,
                "Nome do paciente",
                "paciente@example.com",
                "Nome do médico",
                LocalDateTime.of(2030, 4, 15, 14, 0),
                TipoAcao.CRIADA);
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(consulta.emailPaciente());
        mensagem.setSubject("Confirmação de Agendamento");
        mensagem.setText("Olá, Nome do paciente.");
        when(templateService.criarMensagem(consulta)).thenReturn(mensagem);

        emailService.enviar(consulta);

        verify(mailSender).send(any(SimpleMailMessage.class));
        assertThat(output).contains("email.saida");
        assertThat(output).contains("paciente@example.com");
        assertThat(output).contains("Confirmação de Agendamento");
        assertThat(output).contains("Olá, Nome do paciente.");
        assertThat(output).doesNotContain("smtp-secret", "password");
    }

    @Test
    void treatsGmailAuthenticationFailureAsConfigurationError(CapturedOutput output) {
        ConsultaNotificacaoDTO consulta = new ConsultaNotificacaoDTO(
                10L,
                "Nome do paciente",
                "paciente@example.com",
                "Nome do médico",
                LocalDateTime.of(2030, 4, 15, 14, 0),
                TipoAcao.EDITADA);
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(consulta.emailPaciente());
        mensagem.setSubject("Alteração no seu Agendamento");
        mensagem.setText("Olá, Nome do paciente.");
        when(templateService.criarMensagem(consulta)).thenReturn(mensagem);
        doThrow(new org.springframework.mail.MailAuthenticationException("no password specified"))
                .when(mailSender).send(mensagem);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> emailService.enviar(consulta))
                .isInstanceOf(NotificacaoConfigurationException.class)
                .hasMessage("Credenciais SMTP inválidas ou ausentes para o envio da notificação");

        assertThat(output).contains("email.falha-configuracao");
        assertThat(output).doesNotContain("no password specified");
    }
}