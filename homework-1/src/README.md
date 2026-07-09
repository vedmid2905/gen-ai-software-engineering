# Banking Transactions API

Spring Boot 3 / Java 21 in-memory REST API for banking transaction management.

## Prerequisites

- **Java 21** (JDK 21) — records require Java 16+; Spring Boot 3.x requires Java 17+
- **Maven 3.6+**

## Build

```powershell
# Set JAVA_HOME to JDK 21 (required if JAVA_HOME points to an older JDK)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "C:\Program Files\Java\jdk-21\bin;" + $env:PATH

# Compile
mvn compile "-Dmaven.wagon.http.ssl.insecure=true" "-Dmaven.wagon.http.ssl.allowall=true"

# Run tests
mvn test "-Dmaven.wagon.http.ssl.insecure=true" "-Dmaven.wagon.http.ssl.allowall=true"

# Run the application
mvn spring-boot:run "-Dmaven.wagon.http.ssl.insecure=true" "-Dmaven.wagon.http.ssl.allowall=true"
```

The `-Dmaven.wagon.http.ssl.insecure=true` flag is only needed if your JVM truststore doesn't include the Maven Central CA certificate.

## Package Structure

| Package                  | Purpose                                                  |
|--------------------------|----------------------------------------------------------|
| `banking`                | Application entry point (`BankingTransactionsApiApplication`) |
| `banking.controller`     | Spring MVC `@RestController` classes                     |
| `banking.service`        | Business logic services                                  |
| `banking.domain`         | Core domain entity (`Transaction` record)                |
| `banking.store`          | In-memory `ConcurrentHashMap`-backed storage             |
| `banking.validation`     | Input validation component                               |
| `banking.dto`            | Request and response DTO records                         |
| `banking.exception`      | Custom exception classes                                 |

## Endpoints

| Method | Path                              | Description              |
|--------|-----------------------------------|--------------------------|
| POST   | `/transactions`                   | Create a transaction     |
| GET    | `/transactions`                   | List transactions        |
| GET    | `/transactions/{id}`              | Get transaction by ID    |
| GET    | `/accounts/{accountId}/balance`   | Get account balance      |
| GET    | `/accounts/{accountId}/summary`   | Get account summary      |
