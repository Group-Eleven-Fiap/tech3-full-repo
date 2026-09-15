package fiap.grupo11.msagendamento.exception;

public class UsuarioNotFoundException extends RuntimeException {

    public UsuarioNotFoundException(Long id, String papel) {
        super("Usuário " + papel + " não encontrado: " + id);
    }
}