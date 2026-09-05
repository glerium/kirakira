@echo off
setlocal
set "APP_HOME=%~dp0.."
set "JAR=%APP_HOME%\target\kirakira-1.0-SNAPSHOT.jar"
set "CONFIG=%APP_HOME%\runtime\application.properties"

if not exist "%CONFIG%" (
  echo Missing private runtime configuration: %CONFIG%
  echo Copy src\main\resources\application.example.properties to that path and fill in the values.
  exit /b 1
)

java -jar "%JAR%" "--spring.config.additional-location=file:%CONFIG%"
