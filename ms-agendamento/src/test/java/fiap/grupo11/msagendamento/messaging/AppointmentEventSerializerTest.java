package fiap.grupo11.msagendamento.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fiap.grupo11.msagendamento.entity.Agendamento;
import fiap.grupo11.msagendamento.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentEventSerializerTest {

    private final AppointmentEventSerializer serializer = new AppointmentEventSerializer(
            new ObjectMapper().registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

    @Test
    void serializesTheNotificationContractInPortuguese() {
        Long appointmentId = 10L;
        Instant scheduledAt = Instant.parse("2030-04-15T14:00:00Z");
        Agendamento appointment = new Agendamento(
                appointmentId, 3L, 1L, scheduledAt, scheduledAt.plusSeconds(1800));
        Usuario paciente = new Usuario(3L, "paciente", "hash", "PACIENTE",
                "Nome do paciente", "paciente@example.com");
        Usuario medico = new Usuario(1L, "medico", "hash", "MEDICO",
                "Nome do médico", "medico@example.com");

        String payload = serializer.serialize(appointment, paciente, medico, "CRIADA");

        assertThat(payload).contains("\"idConsulta\":" + appointmentId);
        assertThat(payload).contains("\"nomePaciente\":\"Nome do paciente\"");
        assertThat(payload).contains("\"emailPaciente\":\"paciente@example.com\"");
        assertThat(payload).contains("\"nomeMedico\":\"Nome do médico\"");
        assertThat(payload).contains("\"dataHoraConsulta\":\"2030-04-15T14:00:00\"");
        assertThat(payload).contains("\"tipoAcao\":\"CRIADA\"");
        assertThat(payload).doesNotContain("eventId", "eventType", "aggregateId");
    }
}