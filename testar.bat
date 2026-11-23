@echo off
set PASTA=%~dp0
set JAR=%PASTA%target\testador-api.jar
if not exist "%JAR%" (
  echo Gerando o testador-api.jar pela primeira vez...
  pushd "%PASTA%"
  call mvnw.cmd -q package -DskipTests
  popd
)
java -jar "%JAR%" %*
