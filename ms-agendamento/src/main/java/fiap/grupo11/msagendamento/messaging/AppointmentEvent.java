package fiap.grupo11.msagendamento.messaging;

import java.time.LocalDateTime;

public record AppointmentEvent(
        Long idConsulta,
        String nomePaciente,
        String emailPaciente,
        String nomeMedico,
        LocalDateTime dataHoraConsulta,
        String tipoAcao
) {
}