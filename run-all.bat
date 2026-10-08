@echo off
rem ==========================================================
rem  ArtCrate - abre cada servicio en su propia ventana.
rem  Orden: Eureka primero, luego los 6 microservicios y al final el gateway.
rem  Requisitos: Java 21 y Maven instalados (comandos "java" y "mvn").
rem  Para detener un servicio: cierra su ventana o presiona Ctrl+C.
rem ==========================================================
cd /d "%~dp0"

echo Iniciando ArtCrate...
echo (la primera vez Maven descarga dependencias y puede demorar unos minutos)
echo.

echo [1/3] Eureka (registro de servicios) en el puerto 8761...
start "eureka-server (8761)" cmd /k "cd eureka-server && mvn spring-boot:run"

echo Esperando 20 segundos a que Eureka quede listo...
call :esperar 20

echo [2/3] Los 6 microservicios...
start "usuarios-service (8081)"   cmd /k "cd usuarios-service   && mvn spring-boot:run"
call :esperar 2
start "catalogo-service (8082)"   cmd /k "cd catalogo-service   && mvn spring-boot:run"
call :esperar 2
start "inventario-service (8083)" cmd /k "cd inventario-service && mvn spring-boot:run"
call :esperar 2
start "carrito-service (8084)"    cmd /k "cd carrito-service    && mvn spring-boot:run"
call :esperar 2
start "pedidos-service (8085)"    cmd /k "cd pedidos-service    && mvn spring-boot:run"
call :esperar 2
start "pagos-service (8086)"      cmd /k "cd pagos-service      && mvn spring-boot:run"

echo [3/3] API Gateway en el puerto 8080...
call :esperar 2
start "gateway-service (8080)"    cmd /k "cd gateway-service    && mvn spring-boot:run"

echo.
echo Listo: se abrieron 8 ventanas. Cuando todas digan "Started ...Application", ArtCrate esta lista.
echo   - API (usar desde el front y Postman):  http://localhost:8080
echo   - Panel de Eureka (servicios):          http://localhost:8761
echo   - Swagger de cada servicio:             http://localhost:PUERTO/swagger-ui.html  (8081 a 8086)
echo.
pause
goto :eof

rem ----- Espera N segundos. Usa "timeout"; si no se puede (por ejemplo, sin consola interactiva), usa "ping" -----
:esperar
set /a espera=%1+1
timeout /t %1 /nobreak >nul 2>&1 || ping -n %espera% 127.0.0.1 >nul
exit /b
