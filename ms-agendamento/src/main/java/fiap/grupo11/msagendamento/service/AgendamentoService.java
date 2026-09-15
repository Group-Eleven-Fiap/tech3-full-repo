package fiap.grupo11.msagendamento.service;

import fiap.grupo11.msagendamento.dto.AgendamentoRequest;
import fiap.grupo11.msagendamento.dto.AgendamentoResponse;
import fiap.grupo11.msagendamento.entity.Agendamento;
import fiap.grupo11.msagendamento.entity.Usuario;
import fiap.grupo11.msagendamento.exception.AgendamentoConflictException;
import fiap.grupo11.msagendamento.exception.AgendamentoNotFoundException;
import fiap.grupo11.msagendamento.exception.UsuarioNotFoundException;
import fiap.grupo11.msagendamento.entity.OutboxEvent;
import fiap.grupo11.msagendamento.messaging.AppointmentEventSerializer;
import fiap.grupo11.msagendamento.repository.AgendamentoRepository;
import fiap.grupo11.msagendamento.repository.OutboxRepository;
import fiap.grupo11.msagendamento.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AgendamentoService {

    private final AgendamentoRepository repository;
    private final OutboxRepository outboxRepository;
    private final AppointmentEventSerializer eventSerializer;
    private final UsuarioRepository usuarioRepository;

    public AgendamentoService(AgendamentoRepository repository, OutboxRepository outboxRepository,
                              AppointmentEventSerializer eventSerializer, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.eventSerializer = eventSerializer;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public AgendamentoResponse create(AgendamentoRequest request, Authentication authentication) {
        requireRole(authentication, "ROLE_ENFERMEIRO",
                "Somente enfermeiros podem cadastrar agendamentos");
        validatePeriod(request.dataHoraInicio(), request.dataHoraFim());
        if (repository.existsOverlapping(request.idMedico(), request.dataHoraInicio(), request.dataHoraFim())) {
            throw new AgendamentoConflictException();
        }
        Agendamento appointment = new Agendamento(
                null, request.idPaciente(), request.idMedico(), request.dataHoraInicio(), request.dataHoraFim());
        Usuario paciente = findUser(request.idPaciente(), "paciente");
        Usuario medico = findUser(request.idMedico(), "médico");
        Agendamento saved = repository.save(appointment);
        saveEvent(saved, "CRIADA", paciente, medico);
        return AgendamentoResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> list(Authentication authentication) {
        requireProfessional(authentication);
        return repository.findAllByScheduledAtAsc().stream()
                .map(AgendamentoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listMine(Authentication authentication) {
        requireRole(authentication, "ROLE_PACIENTE",
                "Somente pacientes podem listar os próprios agendamentos");
        List<Agendamento> appointments = repository.findByPatientIdOrderByScheduledAtAsc(
                currentUserId(authentication));
        return appointments.stream().map(AgendamentoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse get(Long id, Authentication authentication) {
        Agendamento appointment = find(id);
        if (!isProfessional(authentication) && !appointment.getPatientId().equals(currentUserId(authentication))) {
            throw new AccessDeniedException("O agendamento pertence a outro paciente");
        }
        return AgendamentoResponse.from(appointment);
    }

    @Transactional
    public AgendamentoResponse update(Long id, AgendamentoRequest request, Authentication authentication) {
        requireRole(authentication, "ROLE_MEDICO",
                "Somente médicos podem editar agendamentos");
        Agendamento appointment = find(id);
        Usuario paciente = findUser(request.idPaciente(), "paciente");
        Usuario medico = findUser(request.idMedico(), "médico");
        validatePeriod(request.dataHoraInicio(), request.dataHoraFim());
        if (repository.existsOverlappingForAnotherAppointment(
                id, request.idMedico(), request.dataHoraInicio(), request.dataHoraFim())) {
            throw new AgendamentoConflictException();
        }
        appointment.update(request.idPaciente(), request.idMedico(), request.dataHoraInicio(), request.dataHoraFim());
        Agendamento saved = repository.save(appointment);
        saveEvent(saved, "EDITADA", paciente, medico);
        return AgendamentoResponse.from(saved);
    }

    private void saveEvent(Agendamento appointment, String tipoAcao, Usuario paciente, Usuario medico) {
        OutboxEvent event = outboxRepository.save(new OutboxEvent(null, appointment.getId(), tipoAcao, ""));
        event.setPayload(eventSerializer.serialize(appointment, paciente, medico, tipoAcao));
        outboxRepository.save(event);
    }

    private Usuario findUser(Long id, String papel) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id, papel));
    }

    private Agendamento find(Long id) {
        return repository.findById(id).orElseThrow(() -> new AgendamentoNotFoundException(id));
    }

    private void validatePeriod(Instant scheduledAt, Instant endsAt) {
        if (!endsAt.isAfter(scheduledAt)) {
            throw new IllegalArgumentException("O horário de término deve ser posterior ao horário de início");
        }
    }

    private void requireRole(Authentication authentication, String requiredRole, String message) {
        currentUserId(authentication);
        boolean hasRequiredRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(requiredRole::equals);
        if (!hasRequiredRole) {
            throw new AccessDeniedException(message);
        }
    }

    private void requireProfessional(Authentication authentication) {
        currentUserId(authentication);
        boolean hasProfessionalRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_MEDICO") || authority.equals("ROLE_ENFERMEIRO"));
        if (!hasProfessionalRole) {
            throw new AccessDeniedException("Somente profissionais de saúde podem listar todos os agendamentos");
        }
    }

    private boolean isProfessional(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_MEDICO") || authority.equals("ROLE_ENFERMEIRO"));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("É necessário informar um usuário autenticado");
        }
        if (authentication.getPrincipal() instanceof fiap.grupo11.msagendamento.config.DemoUser user) {
            return user.getUserId();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return parseUserId(jwt.getSubject());
        }
        return parseUserId(authentication.getName());
    }

    private Long parseUserId(String userId) {
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("O usuário autenticado possui um identificador inválido");
        }
    }
}