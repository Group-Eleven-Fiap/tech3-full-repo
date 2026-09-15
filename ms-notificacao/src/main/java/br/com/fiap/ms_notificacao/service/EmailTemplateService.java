package br.com.fiap.ms_notificacao.service;

import br.com.fiap.ms_notificacao.dto.ConsultaNotificacaoDTO;
import br.com.fiap.ms_notificacao.enums.TipoAcao;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailTemplateService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    public SimpleMailMessage criarMensagem(ConsultaNotificacaoDTO dto) {
        ConsultaNotificacaoDTO.validar(dto);

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(dto.emailPaciente());
        mensagem.setSubject(obterAssunto(dto.tipoAcao()));
        mensagem.setText(obterTexto(dto));
        return mensagem;
    }

    private String obterAssunto(TipoAcao tipoAcao) {
        return switch (tipoAcao) {
            case CRIADA -> "Confirmação de Agendamento - Hospital Tech";
            case EDITADA -> "Alteração no seu Agendamento - Hospital Tech";
            default -> throw new IllegalArgumentException("Tipo de ação não suportado: " + tipoAcao);
        };
    }

    private String obterTexto(ConsultaNotificacaoDTO dto) {
        String dataFormatada = dto.dataHoraConsulta().format(FORMATTER);

        return switch (dto.tipoAcao()) {
            case CRIADA ->
                    String.format("Olá, %s.\n\nSua consulta com %s foi agendada com sucesso para o dia %s.\n\nAté logo!", dto.nomePaciente(), dto.nomeMedico(), dataFormatada);
            case EDITADA ->
                    String.format("Olá, %s.\n\nSua consulta com %s foi remarcada/alterada para o dia %s.\n\nAté logo!", dto.nomePaciente(), dto.nomeMedico(), dataFormatada);
            default -> throw new IllegalArgumentException("Tipo de ação não suportado: " + dto.tipoAcao());
        };
    }
}
