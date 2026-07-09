# ▶️ How to Run the Application

## Prerequisites

- **Java 21** (JDK 21) — the project uses records and other Java 21 language features and will not compile on older versions.
- **Maven 3.6+** (or use the Maven wrapper if one is present in `src/`).
- Internet access on first run so Maven can download dependencies into `~/.m2/repository`.

Check your Java version:

```bash
java -version
```

If you have multiple JDKs installed, make sure `JAVA_HOME` points at 21, e.g. on Windows:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

```bash
export JAVA_HOME=/path/to/jdk-21
```

---

## Option 1: Run with the helper script

From the `homework-1` folder:

```bash
# Linux / macOS / Git Bash
./demo/run.sh

# Windows (cmd or PowerShell)
demo\run.bat
```

Both scripts `cd` into `src/`, build the project, and start the API on **http://localhost:8080**.

## Option 2: Run manually with Maven

```bash
cd src
mvn spring-boot:run
```

## Option 3: Build a jar and run it directly

```bash
cd src
mvn -DskipTests package
java -jar target/banking-transactions-api-0.0.1-SNAPSHOT.jar
```

This is the most reliable option if your environment has trouble resolving the `spring-boot-maven-plugin`'s `run` goal (some restricted/offline networks fail to fetch its optional `buildpack-platform` dependency — packaging and running the jar directly avoids that path entirely).

---

## Verifying it's running

```bash
curl http://localhost:8080/transactions
# -> []
```

An empty JSON array means the server is up and the in-memory store is empty (expected on every fresh start — there is no persistence between restarts).

## Running the tests

```bash
cd src
mvn test
```

This runs both the JUnit 5 unit tests and the jqwik property-based tests.

## Trying the API

See [`demo/sample-requests.http`](demo/sample-requests.http) for ready-to-run requests (openable directly in VS Code with the "REST Client" extension, or copy the `curl` commands from [`demo/sample-requests.sh`](demo/sample-requests.sh)). [`demo/sample-data.json`](demo/sample-data.json) contains example transaction payloads.

## Stopping the application

Press `Ctrl+C` in the terminal where it's running.
