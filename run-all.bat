@echo off
rem ==========================================================
rem  ArtCrate - abre cada microservicio en su propia ventana.
rem  Requisitos: Java 21 y Maven instalados (comandos "java" y "mvn").
rem  Para detener un servicio: cierra su ventana o presiona Ctrl+C.
rem ==========================================================
cd /d "%~dp0"

echo Iniciando los 6 servicios de ArtCrate...
echo (la primera vez Maven descarga dependencias y puede demorar unos minutos)
echo.

start "usuarios-service (8081)"   cmd /k "cd usuarios-service   && mvn spring-boot:run"
timeout /t 2 /nobreak >nul
start "catalogo-service (8082)"   cmd /k "cd catalogo-service   && mvn spring-boot:run"
timeout /t 2 /nobreak >nul
start "inventario-service (8083)" cmd /k "cd inventario-service && mvn spring-boot:run"
timeout /t 2 /nobreak >nul
start "carrito-service (8084)"    cmd /k "cd carrito-service    && mvn spring-boot:run"
timeout /t 2 /nobreak >nul
start "pedidos-service (8085)"    cmd /k "cd pedidos-service    && mvn spring-boot:run"
timeout /t 2 /nobreak >nul
start "pagos-service (8086)"      cmd /k "cd pagos-service      && mvn spring-boot:run"

echo.
echo Listo: se abrieron 6 ventanas. Cuando todas digan "Started ...Application", la API esta lista.
echo Swagger de cada servicio: http://localhost:PUERTO/swagger-ui.html  (puertos 8081 a 8086)
echo.
pause
