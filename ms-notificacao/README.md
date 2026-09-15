# ms-notificacao

Microserviço responsável por consumir eventos de agendamento (RabbitMQ) e enviar notificações por e-mail.

## Objetivo
Consumir mensagens de eventos de consulta (criação/edição) e disparar e-mails aos pacientes usando JavaMail. Não expõe API HTTP: a entrada é via fila RabbitMQ.

## Arquitetura e componentes
A arquitetura é orientada a eventos e construída para ser leve, resiliente e facilmente implantável em contêineres.

Componentes principais:
- ConsultaNotificacaoListener: consumidor AMQP anotado com @RabbitListener. Recebe mensagens da fila configurada, desserializa para ConsultaNotificacaoDTO e inicia o processamento. Trata mensagens nulas e lança AmqpRejectAndDontRequeueException para mensagens invalidas (evita requeue infinito).

- ConsultaNotificacaoDTO: record que modela o payload esperado. Usa validações (jakarta.validation) e um método estático validar(...) para garantir integridade dos dados no processamento.

- NotificacaoService: orquestra o fluxo de notificação. Valida o DTO, registra telemetria via logs e delega o envio para EmailSender. Usa Spring Retry (@Retryable e @Recover) para lidar com falhas transitórias e centralizar política de retry configurável.

- EmailSender / EmailService: interface + implementação que integram com JavaMailSender. EmailService encapsula transformação em SimpleMailMessage e lança NotificacaoTransientException em falhas transitórias para acionar retry.

- EmailTemplateService: responsável por montar assunto e corpo do e-mail (separação de responsabilidades facilita testes e personalização).

- MessagingConfig: configura exchanges, filas, bindings e DLQ (dead-letter). Também registra MessageConverter (Jackson) para (de)serialização JSON de forma consistente.

Fluxo e tolerância a falhas:
1. Produtor publica evento no exchange com routing key.
2. RabbitMQ entrega para a fila durável configurada.
3. Listener consome e chama NotificacaoService.
4. Em falhas transitórias (ex.: MailException) EmailService lança NotificacaoTransientException; NotificacaoService é reexecutado conforme política de retry.
5. Se retry excedido, @Recover registra e lança erro definitivo; a mensagem pode ser encaminhada para a DLQ definida pela fila.

Implantação e escalabilidade:
- Multiplas réplicas do serviço podem rodar em paralelo apontando para a mesma fila — RabbitMQ distribui mensagens entre consumidores (competing consumers).
- Configurações de prefetch e concorrência do listener podem ser ajustadas conforme carga.
- Containerização via Dockerfile e orquestração via docker-compose ou Kubernetes.

Observabilidade e operações:
- Logs estruturados e mensagens de auditoria (mensagem.recebida, notificacao.inicio, notificacao.sucesso, notificacao.falha-transitoria, notificacao.retry-excedido).
- RabbitMQ management UI oferece inspeção de filas, DLQ e re-publicação de mensagens.

Extensibilidade:
- Separação por interfaces (EmailSender) facilita uso de mocks, provedores alternativos (SMS, push) ou testes de integração.
- Possível adicionar camada de persistência para histórico de notificações ou métricas mais avançadas se necessário.

Fluxo de execução:
1. Mensagem publicada no exchange configurado com routing key.
2. RabbitMQ entrega para a fila (consultas-notificacoes-queue por padrão).
3. ConsultaNotificacaoListener desserializa para ConsultaNotificacaoDTO e chama NotificacaoService.
4. NotificacaoService valida dados e chama EmailSender.
5. EmailService monta a mensagem com EmailTemplateService e envia via JavaMailSender.
6. Em falhas transitórias, Spring Retry reexecuta; em excesso, a mensagem pode ir para DLQ.

## Tecnologias
- Java 21
- Spring Boot 3 (spring-boot-starter-web presente para utilitários, mas não usado como controller)
- Spring AMQP (RabbitMQ)
- Spring Mail (JavaMailSender)
- Spring Retry
- Jackson (serialização/deserialização)
- Maven
- Docker (Dockerfile + docker-compose)

## Modelo de mensagem (JSON)
Campos esperados no payload (exemplo):

{
  "idConsulta": 1,
  "nomePaciente": "Thiago Rocha",
  "emailPaciente": "exemplo@provedor.com",
  "nomeMedico": "Dr. João Pereira",
  "dataHoraConsulta": "2026-09-08T21:30:00",
  "tipoAcao": "CRIADA"
}

Descrição dos campos:
- idConsulta (Long): identificador da consulta (obrigatório)
- nomePaciente (String): nome do paciente (obrigatório)
- emailPaciente (String): e-mail do paciente (obrigatório, formato válido)
- nomeMedico (String): nome do médico (obrigatório)
- dataHoraConsulta (ISO-8601): data e hora da consulta (obrigatório)
- tipoAcao (String): enum com valores suportados: CRIADA, EDITADA

Observação: propriedades adicionais são ignoradas (JsonIgnoreProperties).

## Configuração
Principais propriedades (application.yaml) e variáveis de ambiente:

RabbitMQ (exemplo via env):
- RABBITMQ_HOST (default: localhost)
- RABBITMQ_PORT (default: 5672)
- RABBITMQ_USERNAME (default: guest)
- RABBITMQ_PASSWORD (default: guest)

Mail (usar variáveis de ambiente em produção; NUNCA commitar senhas):
- SPRING_MAIL_HOST (ex: smtp.gmail.com)
- SPRING_MAIL_PORT (ex: 587)
- SPRING_MAIL_USERNAME
- SPRING_MAIL_PASSWORD

App messaging (podem ser sobrescritos em application.yaml):
- app.messaging.exchange (default: agendamento.events)
- app.messaging.queue (default: consultas-notificacoes-queue)
- app.messaging.routing-key (default: agendamento.appointment.changed)
- app.messaging.dlx-exchange / dlq-queue / dlq-routing-key (dead-letter)

Retry:
- app.retry.max-attempts (default: 3)
- app.retry.delay-ms (default: 1000)
- app.retry.multiplier (default: 2)

Server:
- server.port (apenas para configuração do app; não expõe controllers por padrão)

## Executando
Opção 1 - Docker (recomendado para testes locais):
1. Ajustar variáveis de ambiente no `docker-compose.yml` (MAIL e RABBITMQ se necessário).
2. docker-compose up --build
3. O RabbitMQ management ficará em http://localhost:15672 (usuário/senha padrão: guest/guest)

Opção 2 - Local (Maven):
1. Definir variáveis de ambiente necessárias (RABBITMQ_*, SPRING_MAIL_*)
2. ./mvnw spring-boot:run   (ou: mvn spring-boot:run)

Logs do serviço mostram quando uma mensagem é recebida e o resultado do envio de e-mail.

## Como publicar mensagens de teste
Método A — RabbitMQ Management UI:
1. Acessar http://localhost:15672 (guest/guest)
2. Acesse Exchanges → selecione `agendamento.events` (ou crie-a) → Publish message
3. No campo Payload, cole o JSON de exemplo. Use routing key `agendamento.appointment.changed`.

Método B — HTTP API do Management (curl):
Exemplo (escape do JSON necessário):

curl -u guest:guest -H "Content-Type: application/json" -X POST \
  -d '{"properties":{},"routing_key":"agendamento.appointment.changed","payload":"{\"idConsulta\":1,\"nomePaciente\":\"Teste\",\"emailPaciente\":\"teste@exemplo.com\",\"nomeMedico\":\"Dr. X\",\"dataHoraConsulta\":\"2026-09-08T21:30:00\",\"tipoAcao\":\"CRIADA\"}","payload_encoding":"string"}' \
  http://localhost:15672/api/exchanges/%2f/agendamento.events/publish

## Observabilidade e troubleshooting
- Logs (stdout) indicam: mensagem.recebida, notificacao.inicio, notificacao.sucesso, notificacao.falha-transitoria, notificacao.retry-excedido.
- Mensagens inválidas (faltando campos) são rejeitadas e não re-enfileiradas (são lançadas como AmqpRejectAndDontRequeueException).
- Mensagens que excedem retries podem ir para DLQ configurada.

