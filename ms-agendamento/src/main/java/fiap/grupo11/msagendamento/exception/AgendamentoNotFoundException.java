package fiap.grupo11.msagendamento.exception;

public class AgendamentoNotFoundException extends RuntimeException {

    public AgendamentoNotFoundException(Long id) {
        super("Agendamento não encontrado: " + id);
    }
}