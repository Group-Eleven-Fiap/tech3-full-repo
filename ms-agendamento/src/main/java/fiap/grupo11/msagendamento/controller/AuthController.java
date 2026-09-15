package fiap.grupo11.msagendamento.controller;

import fiap.grupo11.msagendamento.config.DemoUser;
import fiap.grupo11.msagendamento.dto.LoginRequest;
import fiap.grupo11.msagendamento.dto.LoginResponse;
import fiap.grupo11.msagendamento.dto.RegistroUsuarioRequest;
import fiap.grupo11.msagendamento.dto.RegistroUsuarioResponse;
import fiap.grupo11.msagendamento.entity.Usuario;
import fiap.grupo11.msagendamento.exception.UsuarioConflictException;
import fiap.grupo11.msagendamento.repository.UsuarioRepository;
import fiap.grupo11.msagendamento.service.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenService jwtTokenService,
                          UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        return jwtTokenService.issue((DemoUser) Objects.requireNonNull(authentication.getPrincipal()));
    }

    @PostMapping("/registro")
    public ResponseEntity<RegistroUsuarioResponse> register(@Valid @RequestBody RegistroUsuarioRequest request) {
        String username = request.username().trim();
        String nome = request.nome().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByUsername(username)) {
            throw new UsuarioConflictException("Nome de usuário já cadastrado");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new UsuarioConflictException("E-mail já cadastrado");
        }

        try {
            Usuario usuario = usuarioRepository.saveAndFlush(new Usuario(
                    null,
                    username,
                    passwordEncoder.encode(request.password()),
                    "PACIENTE",
                    nome,
                    email));
            return ResponseEntity.status(HttpStatus.CREATED).body(new RegistroUsuarioResponse(
                    usuario.getId(), usuario.getUsername(), usuario.getNome(), usuario.getEmail()));
        } catch (DataIntegrityViolationException exception) {
            throw new UsuarioConflictException("Username ou e-mail já cadastrado");
        }
    }
}