#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

mkdir -p workspace workspace/outputs prompts

echo "Atlas AI local startup check"
java -version
python3 --version

if command -v ollama >/dev/null 2>&1; then
  ollama list || true
else
  echo "Ollama not found. Install Ollama to enable local model execution."
fi

if [ -x "./gradlew" ]; then
  ./gradlew :atlas-core:bootRun
else
  echo "Gradle wrapper is not present. Install Gradle or add the wrapper, then run: gradle :atlas-core:bootRun"
fi
