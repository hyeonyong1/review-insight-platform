@echo off
setlocal
cd /d "%~dp0..\backend-java"

echo [check] Java
where java >nul 2>nul || (
  echo ERROR: java was not found in PATH. Install JDK 17+ or set JAVA_HOME first.
  exit /b 1
)
java -version

echo.
echo [check] Maven
where mvn >nul 2>nul || (
  echo ERROR: mvn was not found in PATH. Install Maven 3.9+ or add MAVEN_HOME\bin to PATH.
  exit /b 1
)
mvn -version

echo.
echo [start] Spring Boot backend
mvn spring-boot:run
