package fiap.grupo11.msagendamento.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.grupo11.msagendamento.entity.Agendamento;
import fiap.grupo11.msagendamento.entity.Usuario;
import fiap.grupo11.msagendamento.repository.AgendamentoRepository;
import fiap.grupo11.msagendamento.repository.OutboxRepository;
import fiap.grupo11.msagendamento.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void clearAppointments() {
        agendamentoRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    @Test
    void validCredentialsReturnBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"medico\",\"password\":\"medico123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void invalidCredentialsReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"medico\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patientCanRegisterWithoutAuthentication() throws Exception {
        String username = "avaliador-" + UUID.randomUUID();
        String email = username + "@example.com";

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(username, "avaliador123", "Paciente Avaliador", email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.nome").value("Paciente Avaliador"))
                .andExpect(jsonPath("$.email").value(email));

        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(usuario.getRole()).isEqualTo("PACIENTE");
        org.assertj.core.api.Assertions.assertThat(usuario.getPasswordHash()).isNotEqualTo("avaliador123");
    }

    @Test
    void duplicateRegistrationReturnsConflict() throws Exception {
        String username = "duplicado-" + UUID.randomUUID();
        String firstBody = registrationBody(username, "avaliador123", "Primeiro Paciente", username + "@example.com");
        String secondBody = registrationBody(username, "outrasenha123", "Segundo Paciente", username + "+outro@example.com");

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Nome de usuário já cadastrado"));
    }

    @Test
    void registrationValidatesRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.nome").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void issuedTokenCanAccessProtectedEndpointWithPatientOwnership() throws Exception {
        Instant scheduledAt = Instant.now().plusSeconds(3600);
        agendamentoRepository.save(new Agendamento(null, 3L, 1L,
                scheduledAt, scheduledAt.plusSeconds(1800)));
        agendamentoRepository.save(new Agendamento(null, 4L, 1L,
                scheduledAt.plusSeconds(3600), scheduledAt.plusSeconds(5400)));

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"paciente\",\"password\":\"paciente123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode token = objectMapper.readTree(response).get("accessToken");

        mockMvc.perform(get("/api/v1/agendamentos/me")
                        .header("Authorization", "Bearer " + token.asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idPaciente").value(3));
    }

    private String registrationBody(String username, String password, String nome, String email) {
        return "{\"username\":\"" + username + "\","
                + "\"password\":\"" + password + "\","
                + "\"nome\":\"" + nome + "\","
                + "\"email\":\"" + email + "\"}";
    }
}