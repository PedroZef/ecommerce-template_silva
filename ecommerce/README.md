# E-commerce Híbrido com Assistente de IA Integrado

Este projeto consiste em um sistema de e-commerce robusto e moderno, integrando rotas web tradicionais baseadas em **Spring MVC + Thymeleaf** e uma **API REST** segura, com suporte a **Inteligência Artificial (IA) Generativa** para processamento de comandos em áudio (voz) e texto.

---

## 🚀 Tecnologias Utilizadas

* **Java 25** (Configuração de última geração)
* **Spring Boot 4.1.0 / 3.x**
* **Spring Data JPA & Hibernate**
* **Spring Security 6** & **JSON Web Tokens (JWT)** para a API REST
* **Spring AI 2.0.0** (Integração com LLM Llama-3.3 e Whisper da Groq compatível com API do OpenAI)
* **Flyway Migration 10.10.0** (Gerenciamento de banco de dados para produção)
* **H2 Database** (Para desenvolvimento rápido em memória) & **MySQL** (Para ambiente de produção)
* **Springdoc OpenAPI 2.8.5** (Swagger para documentação interativa da API)
* **Project Lombok** & **spring-dotenv** (Carregamento seguro de credenciais via `.env`)

---

## 🏛️ Arquitetura e Padrões de Projeto

O projeto adota uma **Arquitetura em Camadas** (Layered Architecture) padrão do Spring Boot com as seguintes separações claras de responsabilidade:

1. **Apresentação / Exposição**:
   * **Páginas Web**: Controladores MVC convencionais em `controller/` que renderizam visualizações dinâmicas via Thymeleaf.
   * **Rotas de API REST**: Controladores anotados com `@RestController` que expõem endpoints JSON documentados sob a raiz `/api/**`.
2. **Lógica de Negócios (Service Layer)**: Concentrada no pacote `service/`, aplicando limites transacionais com a anotação `@Transactional` do Spring.
3. **Persistência (Repository Pattern)**: Uso das interfaces `JpaRepository` do Spring Data JPA no pacote `repository/` para acesso simplificado ao banco.
4. **Modelo de Domínio**: Entidades mapeadas no banco em `model/` (ex: `Cliente`, `Produto`, `Pedido`, `ItemPedido`).
5. **Camada de Transporte (DTOs)**: Uso do padrão Data Transfer Object no pacote `dto/` (como `ProdutoRequestDto`, `ProdutoResponseDto`) para desacoplar as entidades de persistência das requisições externas, protegendo contra falhas de exposição de dados e vulnerabilidades de *Mass Assignment*.
6. **Tratamento Global de Exceções**: Centralizado através de um `@RestControllerAdvice` no arquivo [GlobalExceptionHandler.java](file:///D:/FAT 2025/Terceiro Semestre 2026/Arquitetura de sistemas (2025)/projetos/ecommerce-template_silva/ecommerce/src/main/java/br/com/ecommerce/exception/GlobalExceptionHandler.java), retornando respostas amigáveis e padronizadas para erros de validação e regras de negócio da API.

---

## 🔒 Segurança e Integração JWT

A segurança é gerenciada de forma dupla através do **Spring Security 6**:

* **Sessão Stateful**: Utilizada para os usuários autenticados nas páginas Thymeleaf convencionais.
* **Token Stateless JWT**: Um filtro personalizado em `security/JwtAuthenticationFilter.java` intercepta as chamadas sob o prefixo `/api` e valida os tokens Bearer JWT, permitindo integrações stateless.
* **Segurança de Configuração**: A chave de assinatura secreta é mandatoriamente carregada a partir do ambiente de produção através da propriedade `jwt.secret=${JWT_SECRET}`. No ambiente de desenvolvimento (`dev`), um fallback seguro é fornecido automaticamente pelo arquivo [application.properties](file:///D:/FAT 2025/Terceiro Semestre 2026/Arquitetura de sistemas (2025)/projetos/ecommerce-template_silva/ecommerce/src/main/resources/application.properties).
* **Controle de Privilégios**: O endpoint público de registro de usuários `/api/auth/register` força a criação de usuários com a role padrão `ROLE_USER`, impedindo escaladas de privilégio inseguras de usuários externos para `ROLE_ADMIN`.

---

## 💾 Gerenciamento e Migração de Banco de Dados

* **Ambiente de Desenvolvimento (`dev`)**: Configurado no arquivo [application-dev.properties](file:///D:/FAT 2025/Terceiro Semestre 2026/Arquitetura de sistemas (2025)/projetos/ecommerce-template_silva/ecommerce/src/main/resources/application-dev.properties), onde o banco de dados H2 é recriado a cada inicialização (`ddl-auto=create-drop`) e o Flyway fica desabilitado para agilidade.
* **Ambiente de Produção (`prod`)**: Configurado no arquivo [application-prod.properties](file:///D:/FAT 2025/Terceiro Semestre 2026/Arquitetura de sistemas (2025)/projetos/ecommerce-template_silva/ecommerce/src/main/resources/application-prod.properties). O Hibernate é definido como `ddl-auto=validate` (modo estrito) e o controle incremental de tabelas e dados é gerenciado profissionalmente por meio do **Flyway**.
  * As migrações SQL residem em [db/migration](file:///D:/FAT 2025/Terceiro Semestre 2026/Arquitetura de sistemas (2025)/projetos/ecommerce-template_silva/ecommerce/src/main/resources/db/migration) (ex: `V1__Initial_Schema.sql`).

---

## 🎙️ Assistente de IA Integrado (Groq/Spring AI)

O sistema conta com um assistente virtual capaz de interpretar comandos em linguagem natural por texto ou voz (transcrito com Whisper).

* **Fallback Local Resiliente**: Se a API da LLM estiver indisponível ou a chave de API Groq (`GROQ_API_KEY`) for inválida, o assistente ativa um mecanismo de correspondência local por expressões regulares (regex) contido em `ia/AssistantAgent.java` para processar intenções básicas como cadastrar produtos ou consultar faturamento sem interromper o serviço.

* **Autorização por Papel (ROLE_ADMIN)**: Cada tool registrada em `ia/tools/EcommerceTools.java` consulta o `SecurityContext` antes de executar. Consultas de catálogo (preço/estoque e produtos por categoria) são liberadas a qualquer usuário autenticado, mas ações administrativas — cadastro de produto, atualização de status de pedido, resumo de vendas/faturamento, cadastro e relatório de despesas — **exigem `ROLE_ADMIN`** e retornam mensagem amigável de acesso negado para usuários comuns. A proteção cobre tanto o fluxo com LLM quanto o fallback offline, pois ambos chamam as mesmas tools.

### 🔑 Credenciais de Demonstração (apenas perfil `dev`)

O `DataLoader` (`config/DataLoader.java`) é exclusivo do profile `dev` (`@Profile("dev")`), ou seja, **nunca executa em produção**:

| Perfil | Usuário | Senha | Papel |
|---|---|---|---|
| dev | `admin@admin.com` | `admin123` | `ROLE_ADMIN` |
| dev | `maria.silva@email.com` | `cliente123` | `ROLE_USER` |
| dev | `joao.oliveira@email.com` | `cliente123` | `ROLE_USER` |
| dev | `ana.souza@email.com` | `cliente123` | `ROLE_USER` |

Em produção, crie os usuários administrativos manualmente (via SQL/migration própria) e defina obrigatoriamente `JWT_SECRET`.

---

## 🛠️ Como Executar o Projeto

1. **Configurar Variáveis de Ambiente**:
   Crie/edite o arquivo `.env` na raiz do projeto e configure as credenciais necessárias:

   ```env
   GROQ_API_KEY=sua_chave_groq
   JWT_SECRET=chave_base64_segura_de_no_minimo_256_bits
   ```
2. **Executar o Perfil de Desenvolvimento (H2 in-memory)**:

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   * Acesse o console do H2 em: `http://localhost:8080/h2-console`
   * Acesse a documentação Swagger em: `http://localhost:8080/swagger-ui/index.html`
3. **Executar o Perfil de Produção (MySQL + Flyway)**:

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```
4. **Executar os Testes Automatizados**:

   ```bash
   mvn test
   ```
