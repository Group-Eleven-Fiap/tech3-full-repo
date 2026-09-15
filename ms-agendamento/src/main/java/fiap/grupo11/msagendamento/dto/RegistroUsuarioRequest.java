package fiap.grupo11.msagendamento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroUsuarioRequest(
        @NotBlank(message = "username é obrigatório")
        @Size(min = 3, max = 100, message = "username deve ter entre 3 e 100 caracteres")
        String username,
        @NotBlank(message = "password é obrigatório")
        @Size(min = 8, max = 72, message = "password deve ter entre 8 e 72 caracteres")
        String password,
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String nome,
        @NotBlank(message = "email é obrigatório")
        @Email(message = "email deve ser válido")
        @Size(max = 254, message = "email deve ter no máximo 254 caracteres")
        String email) {
}