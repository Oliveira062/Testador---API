#!/usr/bin/env sh
set -e
PASTA="$(cd "$(dirname "$0")" && pwd)"
JAR="$PASTA/target/testador-api.jar"
if [ ! -f "$JAR" ]; then
  echo "Gerando o testador-api.jar pela primeira vez..."
  (cd "$PASTA" && ./mvnw -q package -DskipTests)
fi
java -jar "$JAR" "$@"
