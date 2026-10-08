#!/usr/bin/env bash
# ==========================================================
#  ArtCrate - detiene los servicios que lanzó ./run-all.sh.
#  Orden inverso al arranque: primero el gateway y al final Eureka.
#  Uso:  ./stop-all.sh
# ==========================================================

cd "$(dirname "${BASH_SOURCE[0]}")" || exit 1

ESPERA_CIERRE=${ESPERA_CIERRE:-15}   # segundos que se espera a que un servicio cierre solo antes de forzarlo

# nombre:puerto, en el orden en que se DETIENEN
SERVICIOS=(
  "gateway-service:8080"
  "pagos-service:8086"
  "pedidos-service:8085"
  "carrito-service:8084"
  "inventario-service:8083"
  "catalogo-service:8082"
  "usuarios-service:8081"
  "eureka-server:8761"
)

puerto_abierto() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

proceso_vivo() {
  [ -n "$1" ] && kill -0 "$1" 2>/dev/null
}

detenidos=0
for entrada in "${SERVICIOS[@]}"; do
  nombre=${entrada%%:*}; puerto=${entrada##*:}
  archivo=".pids/$nombre.pid"
  pid=$(cat "$archivo" 2>/dev/null)

  if proceso_vivo "$pid"; then
    echo "→ Deteniendo $nombre (PID $pid)..."
    kill "$pid" 2>/dev/null                       # cierre normal (SIGTERM): Spring cierra ordenadamente
    for _ in $(seq 1 "$ESPERA_CIERRE"); do
      proceso_vivo "$pid" || break
      sleep 1
    done
    if proceso_vivo "$pid"; then
      echo "  no cerró en ${ESPERA_CIERRE}s, se fuerza"
      kill -9 "$pid" 2>/dev/null
    fi
    detenidos=$((detenidos + 1))
  elif puerto_abierto "$puerto"; then
    echo "• $nombre: el puerto $puerto está en uso por otro proceso (no lo inició run-all.sh), no se toca"
  fi
  rm -f "$archivo"
done

echo
if [ "$detenidos" -eq 0 ]; then
  echo "No había servicios de ArtCrate corriendo (según .pids/)."
else
  echo "Listo: $detenidos servicio(s) detenido(s)."
fi
