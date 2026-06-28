# Atlas AI Install

Atlas AI is designed to run locally on one machine.

## Requirements

- Java 21
- Node.js 20+
- Python 3.11+
- Ollama
- Playwright for Python

## Backend

```bash
./gradlew :atlas-core:bootRun
```

The backend starts on `http://localhost:8080`.

The Gradle wrapper is configured with a Java toolchain resolver. If Java 21 is not installed locally, Gradle can download a matching JDK into its local cache during the first run.

## IntelliJ

Open the repository root and let IntelliJ import it as a Gradle project.

Recommended settings:

- Project SDK: Java 21
- Gradle JVM: Java 21, or use the project Gradle wrapper/toolchain

If IntelliJ tries to run Atlas with JetBrains Runtime 17, use the Gradle task instead:

```bash
./gradlew :atlas-core:bootRun
```

## Frontend

```bash
cd atlas-ui
npm install
npm run dev
```

The frontend starts on `http://localhost:5173`.

## Browser Automation

```bash
python3 -m pip install playwright
python3 -m playwright install chromium
```

## Local Models

```bash
ollama pull llama3.1
ollama list
```

Configure defaults in `atlas-core/src/main/resources/application.yml`.

## One Command Startup

```bash
./scripts/start-atlas.sh
```
