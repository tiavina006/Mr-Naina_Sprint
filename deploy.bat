@echo off
setlocal

set "TOMCAT_HOME=%~1"
if "%TOMCAT_HOME%"=="" set "TOMCAT_HOME=D:\apache-tomcat-9.0.122"

if not exist "%TOMCAT_HOME%\bin\startup.bat" (
    echo Tomcat introuvable dans "%TOMCAT_HOME%".
    echo Usage: deploy.bat "C:\chemin\vers\tomcat"
    exit /b 1
)

echo Construction du WAR...
call mvn -pl test-app -am clean package
if errorlevel 1 (
    echo Echec de la construction.
    exit /b 1
)

echo Copie du WAR vers Tomcat...
copy /Y "test-app\target\sprint-test-app.war" "%TOMCAT_HOME%\webapps\sprint-test-app.war" >nul
if errorlevel 1 (
    echo Echec de la copie vers Tomcat.
    exit /b 1
)

set "CATALINA_HOME=%TOMCAT_HOME%"
echo Demarrage de Tomcat...
call "%TOMCAT_HOME%\bin\startup.bat"
echo Application disponible sur http://localhost:8080/sprint-test-app/employe/liste

endlocal