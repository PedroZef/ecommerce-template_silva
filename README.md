# E-commerce Template Silva

Este é um projeto base (template) para um sistema de E-commerce construído com **Java** e **Spring Boot**.

## 🚀 Passo a Passo para Executar o Projeto

Siga as instruções abaixo para configurar, compilar e rodar o projeto em sua máquina local.

### 📋 Pré-requisitos

Antes de começar, certifique-se de ter as seguintes ferramentas instaladas:

- **Java Development Kit (JDK)** (versão 21 ou superior é recomendada)
- Maven

---

### 🛠️ Passo 1: Acessar a pasta do projeto

Abra o terminal e navegue até a pasta principal da aplicação (onde se encontra o código fonte e o arquivo `pom.xml`):

```bash
cd ecommerce
```

### ☕ Passo 2: Executar a aplicação (via Maven Wrapper)

O projeto já inclui o **Maven Wrapper** (`mvnw` e `mvnw.cmd`), o que significa que você não precisa ter o Maven instalado manualmente na sua máquina.

Para iniciar a aplicação, execute o comando correspondente ao seu sistema operacional:

**No Windows:**

```cmd
.\mvnw.cmd spring-boot:run
```

**No Linux ou Mac:**

```bash
.\mvnw spring-boot:run
```

*Aguarde o download das dependências (na primeira vez) e a inicialização do Spring Boot.*

### 🌐 Passo 3: Acessar no Navegador

Após a inicialização com sucesso, a aplicação estará rodando localmente. Abra o seu navegador e acesse:

👉 **[http://localhost:8080](http://localhost:8080)**

Você deverá ver a página inicial configurada no `index.html` através do `HomeController`.

---

### 🐳 Passo Extra: Executando Infraestrutura com Docker

Se o projeto utiliza um banco de dados ou outros serviços auxiliares, você pode iniciá-los utilizando o Docker Compose.
A partir da raiz do repositório (uma pasta acima de `ecommerce`), execute:

```bash
docker-compose up -d
```

*(O parâmetro `-d` roda os contêineres em segundo plano).*

---

## 📁 Estrutura Básica do Projeto

- `src/main/java/.../controller/` - Controladores da aplicação (ex: `HomeController.java`).
- `src/main/resources/templates/` - Arquivos de visualização (HTML, como o `index.html`).
- `src/main/resources/application.properties` - Configurações principais do Spring Boot.
- `pom.xml` - Gerenciamento de dependências do Maven.

## 🧪 Rodando os Testes

Para garantir que tudo está funcionando corretamente, você pode rodar os testes automatizados da aplicação:

**Windows:**

```cmd
mvnw.cmd test
```

**Linux/Mac:**

```bash
./mvnw test
```

- Ainda em desevolvimento
