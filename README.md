# `ms-agendamento` + `ms-notificacao`

Repositório com os dois microsserviços, PostgreSQL, RabbitMQ e a integração de envio de e-mails.
O `ms-agendamento` publica eventos e o `ms-notificacao` os consome.

```text
├── docker-compose.yml
├── .env.example
├── ms-agendamento/
├── ms-notificacao/
└── postman/
```

## Configuração e execução

```powershell
docker compose up --build
```

As APIs ficam disponíveis em `http://localhost:8080` (`ms-agendamento`) e `http://localhost:8081`
(`ms-notificacao`).

Para encerrar:

```powershell
docker compose down
```

Para encerrar e apagar o banco local:

```powershell
docker compose down -v
```

## Postman

Importe `postman/ms-agendamento.postman_collection.json` e execute as pastas na ordem indicada.
Antes da primeira execução, ajuste `cadastroNome`, `cadastroEmail`, `cadastroUsername` e
`cadastroPassword`. A collection cadastra um paciente `PACIENTE`, salva o ID e o token JWT e cobre:

- login e cadastro;
- criação, consulta, atualização e listagem de agendamentos;
- consulta do histórico futuro do paciente via GraphQL;
- validações, conflitos, autenticação e autorização.

Os horários são gerados automaticamente. Para executar novamente, use outro username/e-mail ou
rode `docker compose down -v` antes de subir os serviços.