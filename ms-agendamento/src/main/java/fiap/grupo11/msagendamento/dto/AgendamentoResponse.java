package fiap.grupo11.msagendamento.dto;

import fiap.grupo11.msagendamento.entity.Agendamento;

import java.time.Instant;

public record AgendamentoResponse(
        Long id,
        Long idPaciente,
        Long idMedico,
        Instant dataHoraInicio,
        Instant dataHoraFim,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public static AgendamentoResponse from(Agendamento appointment) {
        return new AgendamentoResponse(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getProfessionalId(),
                appointment.getScheduledAt(),
                appointment.getEndsAt(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt());
    }
}