#!/usr/bin/env bash
# ==========================================================
#  ArtCrate - arranca Eureka, los 6 microservicios y el gateway (Linux / macOS / Git Bash).
#  Orden: Eureka -> usuarios, catalogo, inventario, carrito, pedidos, pagos -> gateway,
#  con una pausa entre cada uno.
#
#  Uso:
#    ./run-all.sh            arranca todo (compila antes si faltan los .jar)
#    ./run-all.sh --build    compila siempre antes de arrancar
#
#  Cada servicio corre en segundo plano:
#    - su salida va a   logs/<servicio>.log      (ver en vivo:  tail -f logs/carrito-service.log)
#    - su PID queda en  .pids/<servicio>.pid
#  Para detener todo:    ./stop-all.sh
#
#  Variables opcionales (segundos y memoria), por ejemplo:
#    ESPERA_ENTRE=5 JAVA_OPTS="-Xmx256m" ./run-all.sh
# ==========================================================

cd "$(dirname "${BASH_SOURCE[0]}")" || exit 1

ESPERA_TRAS_EUREKA=${ESPERA_TRAS_EUREKA:-10}   # pausa extra cuando Eureka ya abrió su puerto
ESPERA_ENTRE=${ESPERA_ENTRE:-3}                # pausa entre un servicio y el siguiente
ESPERA_ANTES_GATEWAY=${ESPERA_ANTES_GATEWAY:-5} # pausa antes del gateway
MAX_ESPERA_PUERTO=${MAX_ESPERA_PUERTO:-90}     # cuánto esperar (como máximo) a que un servicio abra su puerto
JAVA_OPTS=${JAVA_OPTS:-}                       # opciones de la JVM, por ejemplo -Xmx256m para ahorrar memoria

# nombre-del-módulo:puerto, EN EL ORDEN DE ARRANQUE
SERVICIOS_EUREKA="eureka-server:8761"
SERVICIOS=(
  "usuarios-service:8081"
  "catalogo-service:8082"
  "inventario-service:8083"
  "carrito-service:8084"
  "pedidos-service:8085"
  "pagos-service:8086"
)
GATEWAY="gateway-service:8080"

mkdir -p logs .pids

# ---------- funciones de apoyo ----------

error() { echo "ERROR: $*" >&2; exit 1; }

# ¿Hay algo escuchando en este puerto? (usa /dev/tcp de bash: no necesita curl ni nc)
puerto_abierto() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

# ¿El proceso con ese PID sigue vivo?
proceso_vivo() {
  [ -n "$1" ] && kill -0 "$1" 2>/dev/null
}

# Espera a que un servicio abra su puerto. Falla si el proceso muere o si pasa demasiado tiempo.
esperar_puerto() {
  local nombre=$1 puerto=$2 pid=$3 segundos=0
  while ! puerto_abierto "$puerto"; do
    if ! proceso_vivo "$pid"; then
      echo "  ✗ $nombre se detuvo al arrancar. Últimas líneas de logs/$nombre.log:" >&2
      tail -n 15 "logs/$nombre.log" >&2
      return 1
    fi
    if [ "$segundos" -ge "$MAX_ESPERA_PUERTO" ]; then
      echo "  ✗ $nombre no abrió el puerto $puerto en ${MAX_ESPERA_PUERTO}s (revisa logs/$nombre.log)" >&2
      return 1
    fi
    sleep 1
    segundos=$((segundos + 1))
  done
  echo "  ✓ $nombre listo en el puerto $puerto (${segundos}s)"
}

# Lanza un módulo en segundo plano; deja su PID en .pids/ y su salida en logs/
LAST_PID=""
arrancar() {
  local nombre=$1 puerto=$2
  LAST_PID=""
  if puerto_abierto "$puerto"; then
    echo "  • $nombre: el puerto $puerto ya está en uso, se omite (¿ya estaba corriendo?)"
    return 2
  fi
  echo "→ Iniciando $nombre (puerto $puerto)..."
  # Se ejecuta desde la carpeta del módulo: así la base H2 queda en <módulo>/data, igual que con mvn spring-boot:run
  # setsid (si existe) desprende el proceso de la terminal, para que siga vivo aunque se cierre.
  local lanzador=()
  command -v setsid >/dev/null 2>&1 && lanzador=(setsid)
  (
    cd "$nombre" || exit 1
    # JAVA_OPTS va sin comillas a propósito: puede traer varias opciones separadas por espacios
    # shellcheck disable=SC2086
    nohup "${lanzador[@]}" java $JAVA_OPTS -jar "target/$nombre-1.0.0.jar" > "../logs/$nombre.log" 2>&1 &
    echo $! > "../.pids/$nombre.pid"
  )
  LAST_PID=$(cat ".pids/$nombre.pid" 2>/dev/null)
  return 0
}

# ---------- comprobaciones previas ----------

command -v java >/dev/null 2>&1 || error "no se encontró 'java'. En Arch Linux: sudo pacman -S jdk21-openjdk"

# Versión mayor de Java (21.0.3 -> 21)
version_java=$(java -version 2>&1 | awk -F'"' '/version/ {print $2}' | cut -d. -f1)
if [ -n "$version_java" ] && [ "$version_java" -lt 21 ] 2>/dev/null; then
  error "se necesita Java 21 o superior y tienes Java $version_java. En Arch Linux: sudo pacman -S jdk21-openjdk y luego 'archlinux-java status' / 'sudo archlinux-java set java-21-openjdk'"
fi

# ¿Hace falta compilar? (si se pidió --build o falta algún .jar)
compilar=0
[ "${1:-}" = "--build" ] && compilar=1
for entrada in "$SERVICIOS_EUREKA" "${SERVICIOS[@]}" "$GATEWAY"; do
  nombre=${entrada%%:*}
  [ -f "$nombre/target/$nombre-1.0.0.jar" ] || compilar=1
done

if [ "$compilar" -eq 1 ]; then
  command -v mvn >/dev/null 2>&1 || error "no se encontró 'mvn' (Maven). En Arch Linux: sudo pacman -S maven"
  echo "Compilando los 8 módulos (la primera vez Maven descarga dependencias y puede demorar)..."
  mvn -B -q clean package || error "falló la compilación"
  echo "Compilación lista."
  echo
fi

# ---------- arranque ----------

trap 'echo; echo "Arranque interrumpido. Los servicios ya lanzados siguen corriendo: usa ./stop-all.sh para detenerlos."; exit 130' INT

echo "Iniciando ArtCrate..."
echo

# 1) Eureka primero
nombre=${SERVICIOS_EUREKA%%:*}; puerto=${SERVICIOS_EUREKA##*:}
arrancar "$nombre" "$puerto"; estado=$?
if [ "$estado" -eq 0 ]; then
  esperar_puerto "$nombre" "$puerto" "$LAST_PID" || { echo "Se cancela el arranque." >&2; exit 1; }
fi
echo "  (esperando ${ESPERA_TRAS_EUREKA}s para que Eureka quede listo...)"
sleep "$ESPERA_TRAS_EUREKA"

# 2) Los 6 microservicios, con una pausa entre cada uno
for entrada in "${SERVICIOS[@]}"; do
  nombre=${entrada%%:*}; puerto=${entrada##*:}
  arrancar "$nombre" "$puerto"; estado=$?
  if [ "$estado" -eq 0 ]; then
    sleep "$ESPERA_ENTRE"
  fi
done

# 3) El gateway al final
echo "  (esperando ${ESPERA_ANTES_GATEWAY}s antes del gateway...)"
sleep "$ESPERA_ANTES_GATEWAY"
nombre=${GATEWAY%%:*}; puerto=${GATEWAY##*:}
arrancar "$nombre" "$puerto"; estado=$?

# 4) Se espera a que todos estén escuchando
echo
echo "Esperando a que todos los servicios abran su puerto..."
fallos=0
for entrada in "${SERVICIOS[@]}" "$GATEWAY"; do
  nombre=${entrada%%:*}; puerto=${entrada##*:}
  pid=$(cat ".pids/$nombre.pid" 2>/dev/null)
  esperar_puerto "$nombre" "$puerto" "$pid" || fallos=$((fallos + 1))
done

# 5) Si hay curl, se muestra cuántos servicios quedaron registrados en Eureka
if command -v curl >/dev/null 2>&1; then
  registrados=0
  for _ in $(seq 1 30); do
    registrados=$(curl -s http://localhost:8761/eureka/apps 2>/dev/null | grep -o '<status>UP</status>' | wc -l)
    [ "$registrados" -ge 7 ] && break
    sleep 2
  done
  echo "Servicios registrados en Eureka: $registrados de 7"
fi

echo
if [ "$fallos" -gt 0 ]; then
  echo "⚠ $fallos servicio(s) no arrancaron bien. Revisa los archivos de la carpeta logs/." >&2
else
  echo "ArtCrate está lista."
fi
echo "  - API (front y Postman):  http://localhost:8080"
echo "  - Panel de Eureka:        http://localhost:8761"
echo "  - Swagger de cada servicio: http://localhost:PUERTO/swagger-ui.html  (8081 a 8086)"
echo "  - Ver los logs:           tail -f logs/<servicio>.log"
echo "  - Detener todo:           ./stop-all.sh"
[ "$fallos" -eq 0 ]
