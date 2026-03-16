# Secto Backend

API Spring Boot da plataforma Sectotech. O projeto expõe endpoints REST sob o prefixo `/api`, usa PostgreSQL com Flyway, autenticação via Keycloak e integrações com OpenAI, AWS S3 e Stripe.

## Stack

- Java 17
- Spring Boot 3.5
- Spring Security OAuth2 Resource Server
- PostgreSQL 15
- Flyway
- Swagger UI via Springdoc
- Keycloak

## Pré-requisitos

- Java 17
- Docker e Docker Compose
- Maven Wrapper do projeto
- FFprobe disponível no PATH ao rodar o backend fora de container

> O serviço `AudioMetadataService` usa o binário `ffprobe` para extrair duração de arquivos de áudio. No container do backend isso já é resolvido pela imagem; em execução local, a ferramenta precisa existir no sistema.

## Infraestrutura local

O arquivo `docker-compose.yml` sobe dois serviços auxiliares:

- PostgreSQL em `localhost:5432`
- Keycloak em `http://localhost:8180`

### Subindo a infraestrutura

```bash
docker compose up -d postgres keycloak
```

Credenciais padrão definidas no compose:

- Banco: `secto_db`
- Usuário do banco: `app`
- Senha do banco: `app`
- Admin do Keycloak: `admin`
- Senha do admin do Keycloak: `admin`

## Configuração de ambiente

Existe um arquivo base chamado `.env.example`. Copie para `.env` e complete as variáveis necessárias.

### Copiar o arquivo de exemplo

```bash
cp .env.example .env
```

PowerShell:

```powershell
Copy-Item .env.example .env
```

### Exemplo de `.env` para desenvolvimento local

```env
SPRING_DATASOURCE_HOST=localhost
SPRING_DATASOURCE_PORT=5432
SPRING_DATASOURCE_DB=secto_db
SPRING_DATASOURCE_USERNAME=app
SPRING_DATASOURCE_PASSWORD=app

APP_FRONTEND_URL=http://localhost:3000

KEYCLOAK_REALM=secto-realm
KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/secto-realm
KEYCLOAK_JWK_SET_URI=http://localhost:8180/realms/secto-realm/protocol/openid-connect/certs
KEYCLOAK_CLIENT_ID=backend-identity-service
KEYCLOAK_CLIENT_SECRET=troque-pelo-secret-do-client
KEYCLOAK_ADMIN_URL=http://localhost:8180/admin
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin

SPRING_AI_OPENAI_API_KEY=
OPENAI_API_KEY=

AWS_S3_ENDPOINT=
AWS_REGION=
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_S3_BUCKET_NAME=

STRIPE_PUBLIC_KEY=
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=
```

### Variáveis mínimas para subir a aplicação

Sem as variáveis abaixo o backend não consegue inicializar corretamente:

- `SPRING_DATASOURCE_HOST`
- `SPRING_DATASOURCE_PORT`
- `SPRING_DATASOURCE_DB`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `KEYCLOAK_ISSUER_URI`
- `KEYCLOAK_JWK_SET_URI`
- `KEYCLOAK_CLIENT_ID`
- `KEYCLOAK_CLIENT_SECRET`
- `KEYCLOAK_ADMIN_URL`
- `KEYCLOAK_REALM`
- `KEYCLOAK_ADMIN_USERNAME`
- `KEYCLOAK_ADMIN_PASSWORD`

As chaves de OpenAI, S3 e Stripe devem ser preenchidas se você for usar os fluxos correspondentes.

## Configuração do Keycloak

O `docker-compose` sobe apenas o servidor do Keycloak. Realm, clients, roles e usuários ainda precisam ser configurados.

Configuração sugerida para desenvolvimento local:

- Realm: `secto-realm`
- Client do backend: `backend-identity-service`
- Client do frontend: defina um client separado e use o mesmo valor no `.env.local` do frontend
- Usuários e roles: crie conforme os perfis exigidos pela aplicação

Para o backend validar tokens, o `issuer` precisa coincidir com o realm configurado. Exemplo:

```env
KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/secto-realm
KEYCLOAK_JWK_SET_URI=http://localhost:8180/realms/secto-realm/protocol/openid-connect/certs
```

## Executando a aplicação

### Com Maven Wrapper

Linux e macOS:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrão, a aplicação sobe em `http://localhost:8080/api`.

## Endpoints úteis em desenvolvimento

- API base: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`
- Healthcheck: `http://localhost:8080/api/actuator/health`

## Segurança e autenticação

- A maioria dos endpoints exige token JWT válido do Keycloak.
- Endpoints públicos liberados na configuração atual incluem Swagger, actuator, webhook de pagamento e `/scripts/test`.
- O backend lê roles de `realm_access.roles` do token.

## CORS

O backend aceita, entre outras, estas origens:

- `http://localhost:*`
- `http://127.0.0.1:*`
- `https://localhost:*`

Isso permite o uso local do frontend sem ajuste adicional de CORS, desde que a autenticação e a URL da API estejam corretas.

## Banco e migrações

- As migrações ficam em `src/main/resources/db/migration`
- O Hibernate está configurado com `ddl-auto=validate`
- O Flyway roda na inicialização da aplicação

## Rodando com Docker

### Build da imagem

```bash
docker build -t secto-backend .
```

### Execução do container

O container do backend também precisa das mesmas variáveis de ambiente do `.env`. Exemplo:

```bash
docker run --rm -p 8080:8080 --env-file .env secto-backend
```

## Fluxo recomendado de desenvolvimento local

1. Suba PostgreSQL e Keycloak com `docker compose up -d postgres keycloak`.
2. Configure realm, clients e usuários no Keycloak.
3. Copie `.env.example` para `.env` e complete as variáveis.
4. Inicie a API com o Maven Wrapper.
5. Inicie o frontend apontando para `http://localhost:8080/api`.

## Problemas comuns

### Erro de conexão com banco

Verifique se o Postgres do compose está de pé e se as variáveis `SPRING_DATASOURCE_*` batem com os valores do container.

### Erro 401 ou 403 mesmo com login no frontend

Revise:

- realm configurado no Keycloak
- `KEYCLOAK_ISSUER_URI`
- secret e client do backend
- roles presentes em `realm_access.roles`

### Falha ao processar áudio

Se estiver rodando o backend fora de Docker, confirme que o comando `ffprobe` está disponível no PATH do sistema.