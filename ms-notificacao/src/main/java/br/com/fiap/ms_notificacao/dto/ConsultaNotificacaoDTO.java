package br.com.fiap.ms_notificacao.dto;

import br.com.fiap.ms_notificacao.enums.TipoAcao;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConsultaNotificacaoDTO(
        @NotNull(message = "Identificador da consulta é obrigatório.") Long idConsulta,
        @NotBlank(message = "Nome do paciente é obrigatório.") String nomePaciente,
        @NotBlank(message = "E-mail do paciente é obrigatório.") @Email(message = "E-mail do paciente inválido.") String emailPaciente,
        @NotBlank(message = "Nome do médico é obrigatório.") String nomeMedico,
        @NotNull(message = "Data e hora da consulta são obrigatórias.") LocalDateTime dataHoraConsulta,
        @NotNull(message = "Tipo da ação é obrigatório.") TipoAcao tipoAcao
) {

    public static void validar(ConsultaNotificacaoDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dados da consulta não podem ser nulos.");
        }

        if (dto.idConsulta() == null) {
            throw new IllegalArgumentException("Identificador da consulta é obrigatório.");
        }

        if (dto.nomePaciente() == null || dto.nomePaciente().isBlank()) {
            throw new IllegalArgumentException("Nome do paciente é obrigatório.");
        }

        if (dto.emailPaciente() == null || dto.emailPaciente().isBlank()) {
            throw new IllegalArgumentException("E-mail do paciente é obrigatório.");
        }

        if (dto.nomeMedico() == null || dto.nomeMedico().isBlank()) {
            throw new IllegalArgumentException("Nome do médico é obrigatório.");
        }

        if (dto.dataHoraConsulta() == null) {
            throw new IllegalArgumentException("Data da consulta é obrigatória.");
        }

        if (dto.tipoAcao() == null) {
            throw new IllegalArgumentException("Tipo da ação da consulta é obrigatório.");
        }
    }
}