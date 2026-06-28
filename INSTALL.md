# Atlas AI Install

Atlas AI is designed to run locally on one machine.

## Requirements

- Java 21
- Gradle 8+
- Node.js 20+
- Python 3.11+
- Ollama
- Playwright for Python

## Backend

```bash
gradle :atlas-core:bootRun
```

The backend starts on `http://localhost:8080`.

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
