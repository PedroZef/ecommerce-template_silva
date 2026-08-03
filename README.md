# E-commerce Template Silva

Template base para um sistema de **E-commerce** construído com **Java 25**, **Spring Boot 4.1.0**, **Spring MVC + Thymeleaf**, **API REST + JWT** e **Assistente de IA Generativa** (texto e voz).

> ⚠️ **Status:** Projeto educacional/template em desenvolvimento contínuo. Já conta com boas práticas como DTOs, tratamento global de erros, transações ACID, Swagger/OpenAPI e autorização por papéis — inclusive nas ferramentas do Assistente IA.

---

## 🚀 Passo a Passo para Executar o Projeto

### 📋 Pré-requisitos

- **JDK 21+** (Java 25 recomendado)
- **Maven** (ou use o Maven Wrapper `mvnw`/`mvnw.cmd`, que já vem no projeto)

---

### 🛠️ Passo 1: Acessar a pasta do projeto

```bash
cd ecommerce
```

### ☕ Passo 2: Configurar variáveis de ambiente

Crie um arquivo `.env` na raiz de `ecommerce/` (ele é ignorado pelo Git):

```env
# Chave para o chat via OpenRouter (modelo google/gemini-2.5-flash)
OPENROUTER_API_KEY=sua_chave_openrouter

# Chave para transcrição de voz via Groq (Whisper large-v3)
GROQ_API_KEY=sua_chave_groq

# Chave de assinatura JWT em Base64 (mínimo 256 bits) - OBRIGATÓRIA em produção
JWT_SECRET=base64_segura_de_no_minimo_256_bits
```

> 🔑 **Atenção:** Em produção (`prod`), se `JWT_SECRET` não for definida, a aplicação falhará ao iniciar (fail-fast). Em dev, existe um fallback apenas para facilitar o desenvolvimento.

### ☕ Passo 3: Executar a aplicação

**No Windows:**

```cmd
.\mvnw.cmd spring-boot:run
```

**No Linux ou Mac:**

```bash
./mvnw spring-boot:run
```

Por padrão, o perfil **`dev`** é ativado (banco **H2 em memória** + carga automática de dados de demonstração). Acesse:

- **Aplicação:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **Console H2:** http://localhost:8080/h2-console (`jdbc:h2:mem:testdb`, usuário `sa`, senha vazia)

**Perfil de produção (MySQL + Flyway):**

```bash
docker-compose up -d    # sobe o MySQL na porta 3306 (na raiz do repositório)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

> No perfil `prod` o Hibernate roda com `ddl-auto=validate` e as tabelas são gerenciadas pelo **Flyway** (`src/main/resources/db/migration`).

### 🧪 Rodando os Testes

```cmd
.\mvnw.cmd test
```

---

## 🏛️ Estrutura do Projeto

```
ecommerce/
├── pom.xml                         # Dependências Maven
├── src/main/java/br/com/ecommerce/
│   ├── config/                     # SecurityConfig, DataLoader (dev), AiConfig, OpenApiConfig
│   ├── controller/                 # Controllers MVC (páginas) + RestControllers (API /api/**)
│   ├── dto/                        # DTOs de entrada/saída da API REST
│   ├── exception/                  # @RestControllerAdvice + exceções de negócio
│   ├── ia/                         # Assistente IA (AssistantAgent + EcommerceTools)
│   ├── model/                      # Entidades JPA (Produto, Pedido, Cliente, ...)
│   ├── repository/                 # Interfaces Spring Data JPA
│   ├── security/                   # JWT (filtro, serviço, DTOs) e AuthController
│   └── service/                    # Camada de negócio (@Transactional)
└── src/main/resources/
    ├── application[-dev|-prod].properties
    ├── db/migration/               # Migrations Flyway (produção)
    ├── static/                     # CSS e JS
    └── templates/                  # Páginas Thymeleaf
```

---

## ✨ Funcionalidades

| Área | Descrição |
|---|---|
| **Páginas Web (MVC)** | Login, Dashboard (métricas + gráficos Chart.js), CRUD de Produtos/Categorias/Clientes, Simulador ACID (checkout) e Histórico de Pedidos. |
| **API REST** | `/api/auth/**` (login/registro JWT), `/api/produtos` (público leitura / ADMIN escrita), `/api/pedidos` (ADMIN), `/api/assistant/**` (autenticado). |
| **Transações ACID** | Checkout atômico: valida estoque, deduz estoque e grava pedido na mesma transação com rollback automático em caso de falha. |
| **Assistente IA** | Comandos por **texto** (OpenRouter/Gemini) e **voz** (Groq/Whisper), com **fallback offline** por expressões regulares. |
| **Segurança** | Spring Security (sessão nas páginas + JWT na API), BCrypt, CSRF nas páginas, validações Bean Validation, segredos via `.env`. |

---

## 🎙️ Assistente IA — Matriz de Autorização

As ferramentas (tools) do assistente consultam o **papel do usuário autenticado** antes de executar. Comandos administrativos são **negados com mensagem amigável** para usuários comuns (`ROLE_USER`):

| Tool do Assistente | Ação | Permissão |
|---|---|---|
| `obterEstoqueEPrecoProduto` | Consultar preço/estoque de produto | ✅ Qualquer usuário autenticado |
| `listarProdutosPorCategoria` | Listar produtos de uma categoria | ✅ Qualquer usuário autenticado |
| `cadastrarProduto` | Criar produto (e categoria, se preciso) | 🔒 Somente `ROLE_ADMIN` |
| `atualizarStatusPedido` | Alterar status de um pedido | 🔒 Somente `ROLE_ADMIN` |
| `obterResumoVendas` | Faturamento e resumo de vendas | 🔒 Somente `ROLE_ADMIN` |
| `cadastrarDespesa` | Registrar despesa/gasto | 🔒 Somente `ROLE_ADMIN` |
| `obterRelatorioDespesas` | Relatório de despesas | 🔒 Somente `ROLE_ADMIN` |

**Como funciona:** a verificação é feita dentro de `ia/tools/EcommerceTools.java` via `SecurityContextHolder` (o mesmo contexto de autenticação da requisição HTTP). A proteção vale tanto para o fluxo com LLM quanto para o **fallback offline** (`ia/AssistantAgent.java`), que chama as mesmas tools. O prompt de sistema do assistente também orienta o modelo a explicar a negativa educadamente.

> 🔒 **Nota de segurança:** o registro público (`/api/auth/register`) cria usuários somente com `ROLE_USER`; a elevação para `ROLE_ADMIN` é controlada pelo banco/administrador. Nenhuma action administrativa do assistente é executável sem esse papel.

---

## 🔐 Segurança e JWT

- **Dupla autenticação:** sessão (páginas Thymeleaf) + Bearer JWT (API REST, via `JwtAuthenticationFilter`).
- **Regras por rota** em `config/SecurityConfig.java` (estáticos públicos, `/api/produtos` leitura pública, admin para escrita, etc.).
- **Dados de demonstração apenas em `dev`:** o `DataLoader` é anotado com `@Profile("dev")` e **não roda em produção** (evita credenciais padrão em ambientes reais).
- **Fail-fast do JWT:** fora do perfil `dev`, o `JwtService` rejeita o fallback padrão da chave — sem `JWT_SECRET` no ambiente, a aplicação **não inicia** (`IllegalStateException` com instruções).
- **Registro seguro:** `RegisterDto` exige senha forte (`@Size(min=8)` + letra maiúscula, minúscula, número e caractere especial) e e-mail válido (`@Email`), sempre com `@Valid`.
- **Sem open-redirect:** rotas de retorno (ex.: `ThemeController`) só aceitam URLs relativas de mesma origem; redirecionam para `/` caso contrário.
- **Checkout sem duplicidade:** o botão "Finalizar Checkout" é desabilitado no primeiro clique (previne pedidos duplicados).
- **Dependências do front locais:** Chart.js é servido localmente (`/js/chart.umd.min.js`, versão fixada) — sem CDN externo no runtime.
- **Erros padronizados:** `GlobalExceptionHandler` devolve JSON consistente (`timestamp`, `status`, `error`, `message`).

---

## 📊 Transações ACID (Destaque Pedagógico)

O método `criarPedido()` em `service/PedidoService.java` é `@Transactional`:

1. Valida itens e forma de pagamento;
2. Busca cada produto, registra preço histórico e confere estoque;
3. Deduz o estoque;
4. Grava pedido + itens em cascata.

Se **qualquer** item exceder o estoque, uma `EstoqueInsuficienteException` (runtime) dispara o **rollback total** — nada é gravado e os estoques parciais voltam ao valor original. Esse comportamento é coberto por testes de integração (`src/test/.../EcommerceApplicationTests.java`).

> 🔒 **Isolamento contra concorrência:** a leitura do produto em `criarPedido()` usa **`PESSIMISTIC_WRITE`** (`SELECT ... FOR UPDATE` via `ProdutoRepository.findByIdForUpdate`). Dois checkouts simultâneos ficam serializados no banco: o segundo só enxerga o estoque **após** o commit do primeiro, eliminando a condição de corrida (*overselling*). Um teste dedicado dispara 2 checkouts concorrentes com estoque insuficiente para ambos e valida que **apenas 1 pedido é aprovado** e o estoque nunca fica negativo.
