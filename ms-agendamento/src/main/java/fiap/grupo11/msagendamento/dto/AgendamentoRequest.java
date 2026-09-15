package fiap.grupo11.msagendamento.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record AgendamentoRequest(
        @NotNull(message = "idPaciente é obrigatório")
        @Positive(message = "idPaciente deve ser positivo")
        Long idPaciente,
        @NotNull(message = "idMedico é obrigatório")
        @Positive(message = "idMedico deve ser positivo")
        Long idMedico,
        @NotNull(message = "dataHoraInicio é obrigatória")
        @Future(message = "dataHoraInicio deve estar no futuro")
        Instant dataHoraInicio,
        @NotNull(message = "dataHoraFim é obrigatória")
        @Future(message = "dataHoraFim deve estar no futuro")
        Instant dataHoraFim
) {
}