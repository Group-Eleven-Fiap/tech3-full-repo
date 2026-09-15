package fiap.grupo11.msagendamento.exception;

public class AgendamentoConflictException extends RuntimeException {

    public AgendamentoConflictException() {
        super("O profissional já possui um agendamento no período solicitado");
    }
}