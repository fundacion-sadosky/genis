#!/bin/bash
# Script para ejecutar GENis en background de forma persistente

cd /home/cdiaz/Descargas/genis

# Matar procesos previos
pkill -9 -f "sbt run" 2>/dev/null
pkill -9 java 2>/dev/null

# Esperar
sleep 3

# Ejecutar en tmux para mantenerlo vivo
if ! command -v tmux &> /dev/null; then
    # Si no existe tmux, usar screen o directamente nohup
    nohup sbt "run -Dplay.server.http.port=8888" > genis-server.log 2>&1 &
    echo "Servidor iniciado (nohup) en puerto 8888"
    echo "PID: $!"
    echo "Log: $(pwd)/genis-server.log"
else
    # Usar tmux si está disponible
    tmux new-session -d -s genis "cd /home/cdiaz/Descargas/genis && sbt run -Dplay.server.http.port=8888"
    echo "Servidor iniciado en sesión tmux 'genis'"
    echo "Para ver: tmux attach-session -t genis"
    echo "Para salir: Ctrl+B, D"
fi

# Esperar a que inicie
echo "Esperando a que el servidor inicie..."
sleep 40

# Verificar
if lsof -i :8888 >/dev/null 2>&1; then
    echo "✓ Servidor corriendo en puerto 8888"
    echo "Accede en: http://localhost:8888/login"
elif lsof -i :9000 >/dev/null 2>&1; then
    echo "✓ Servidor corriendo en puerto 9000"
    echo "Accede en: http://localhost:9000/login"
else
    echo "✗ Servidor no detectado. Verificar log:"
    tail -100 genis-server.log 2>/dev/null || echo "Log no encontrado"
fi
