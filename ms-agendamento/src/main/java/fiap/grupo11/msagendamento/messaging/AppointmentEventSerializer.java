package fiap.grupo11.msagendamento.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.grupo11.msagendamento.entity.Agendamento;
import fiap.grupo11.msagendamento.entity.Usuario;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class AppointmentEventSerializer {

    private final ObjectMapper objectMapper;

    public AppointmentEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Agendamento appointment, Usuario paciente, Usuario medico, String tipoAcao) {
        AppointmentEvent event = new AppointmentEvent(
                appointment.getId(),
                paciente.getNome(),
                paciente.getEmail(),
                medico.getNome(),
                appointment.getScheduledAt().atZone(ZoneOffset.UTC).toLocalDateTime(),
                tipoAcao);
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível serializar o evento do agendamento", exception);
        }
    }
}