# Integração `ms-agendamento` e `ms-notificacao`

Este repositório reúne os dois serviços e a infraestrutura compartilhada para execução local. O
`ms-agendamento` mantém os agendamentos e publica eventos no RabbitMQ; o `ms-notificacao` consome
esses eventos e processa o envio de e-mail.

## Estrutura

```text
.
├── docker-compose.yml
├── .env.example
├── postman/
├── ms-agendamento/
└── ms-notificacao/
```

Os diretórios de serviço preservam os fontes, testes e Dockerfiles dos projetos originais. O
`docker-compose.yml` da raiz será a única entrada para executar a integração, usando um PostgreSQL
dedicado ao agendamento e um RabbitMQ compartilhado.

## Configuração

1. Copie `.env.example` para `.env`.
2. Ajuste as credenciais de banco, RabbitMQ, JWT e Gmail SMTP para o ambiente local.
3. Não versione `.env` nem credenciais reais de SMTP.

Para enviar e-mails pelo Gmail, mantenha `SPRING_MAIL_HOST=smtp.gmail.com` e `SPRING_MAIL_PORT=587`.
Use o endereço da conta em `SPRING_MAIL_USERNAME` e uma senha de aplicativo do Google em
`SPRING_MAIL_PASSWORD`; a senha normal da conta não é aceita pelo Gmail. A senha de aplicativo
é criada na conta Google após ativar a verificação em duas etapas.

No PowerShell:

```powershell
Copy-Item .env.example .env
```

## Execução

Na raiz do repositório, execute:

```powershell
docker compose up --build
```

As APIs serão expostas em `http://localhost:8080` (`ms-agendamento`) e
`http://localhost:8081` (`ms-notificacao`). PostgreSQL e RabbitMQ permanecem acessíveis pelos
nomes de serviço da rede Compose, evitando configurações específicas de `localhost` entre os
contêineres.

Para encerrar os serviços:

```powershell
docker compose down
```

Para remover também o volume local do banco durante uma limpeza de desenvolvimento:

```powershell
docker compose down -v
```

## Mensageria

Os dois serviços usam a mesma exchange `agendamento.events` e a routing key
`agendamento.appointment.changed`. A fila e a DLQ pertencem ao `ms-notificacao`; o payload
publicado pelo agendamento deve ser consumível diretamente por `ConsultaNotificacaoDTO`.

## Testes da API com Postman

A collection `postman/ms-agendamento.postman_collection.json` contém os testes da API do
`ms-agendamento`, incluindo:

- logins dos usuários `enfermeiro`, `medico` e `paciente`;
- cadastro público de um paciente do avaliador, com nome, e-mail e senha configuráveis;
- criação, consulta, listagem e atualização de agendamentos;
- validação de campos obrigatórios e período inválido;
- autenticação ausente ou inválida;
- autorização por papel e tentativa de acesso não permitido;
- agendamento inexistente e conflito de horário.

Importe a collection no Postman e execute as pastas na ordem indicada. Antes da primeira execução,
edite as variáveis `cadastroNome` e `cadastroEmail` com os dados do avaliador; `cadastroUsername` e
`cadastroPassword` também podem ser alterados. O request `Cadastrar paciente do avaliador` cria
somente usuários com papel `PACIENTE`, e os scripts salvam o `idPacienteCadastro`, os tokens JWT e o
ID do agendamento para reutilização no fluxo. Os horários futuros são gerados automaticamente para
permitir novas execuções sem editar o JSON; em uma nova execução, use outro username/e-mail ou
limpe o volume do PostgreSQL. A variável `baseUrl` usa `http://localhost:8080`; altere-a caso a
porta `AGENDAMENTO_API_PORT` do `.env` seja diferente.