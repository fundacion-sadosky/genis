#!/bin/bash

# Script para ejecutar GENIS con configuración de desarrollo
# Uso: ./run-genis-dev.sh [foreground|background|stop]

GENIS_HOME="/home/cdiaz/Descargas/genis"
JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"
CONFIG_FILE="./application-dev.conf"
LOGGER_FILE="./logger-dev.xml"
LOG_FILE="genis.log"

# Colores para salida
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para mostrar mensaje de error
error() {
    echo -e "${RED}❌ Error: $1${NC}"
    exit 1
}

# Función para mostrar mensaje de éxito
success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Función para mostrar mensaje de información
info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# Verificar que GENIS_HOME existe
if [ ! -d "$GENIS_HOME" ]; then
    error "Directorio de GENIS no encontrado: $GENIS_HOME"
fi

cd "$GENIS_HOME" || error "No se pudo navegar a $GENIS_HOME"

# Verificar archivos de configuración
if [ ! -f "$CONFIG_FILE" ]; then
    error "Archivo de configuración no encontrado: $CONFIG_FILE"
fi

if [ ! -f "$LOGGER_FILE" ]; then
    info "Archivo de logger no encontrado, se puede crear con: cp logger-dev-template.xml logger-dev.xml"
fi

# Verificar Java
if [ ! -x "$JAVA_HOME/bin/java" ]; then
    error "Java 8 no encontrado en: $JAVA_HOME"
fi

JAVA_VERSION=$($JAVA_HOME/bin/java -version 2>&1 | grep version | cut -d'"' -f2)
info "Usando Java: $JAVA_VERSION desde $JAVA_HOME"

# Acción a realizar
ACTION="${1:-foreground}"

case "$ACTION" in
    foreground)
        info "Iniciando GENIS en primer plano..."
        info "Accede a http://localhost:9000 en tu navegador"
        info "Presiona Ctrl+C para detener"
        
        $JAVA_HOME/bin/java \
            -server \
            -Xms512M \
            -Xmx10g \
            -Xss1M \
            -XX:+CMSClassUnloadingEnabled \
            -Dconfig.file=$CONFIG_FILE \
            -Dlogger.file=$LOGGER_FILE \
            -Dhttp.port=9000 \
            -Dhttps.port=9443 \
            -Dplay.server.pidfile.path=/dev/null \
            -jar target/universal/stage/lib/genis.genis-*.jar
        ;;
        
    background)
        info "Iniciando GENIS en segundo plano..."
        
        # Verificar si ya hay una instancia corriendo
        if pgrep -f "java.*genis" > /dev/null; then
            error "GENIS ya está corriendo. Detén con: $0 stop"
        fi
        
        nohup $JAVA_HOME/bin/java \
            -server \
            -Xms512M \
            -Xmx10g \
            -Xss1M \
            -XX:+CMSClassUnloadingEnabled \
            -Dconfig.file=$CONFIG_FILE \
            -Dlogger.file=$LOGGER_FILE \
            -Dhttp.port=9000 \
            -Dhttps.port=9443 \
            -Dplay.server.pidfile.path=/dev/null \
            -jar target/universal/stage/lib/genis.genis-*.jar > $LOG_FILE 2>&1 &
            
        sleep 2
        
        if pgrep -f "java.*genis" > /dev/null; then
            success "GENIS iniciado en segundo plano (PID: $(pgrep -f 'java.*genis'))"
            info "Logs en: $GENIS_HOME/$LOG_FILE"
            info "Accede a http://localhost:9000"
            info "Para ver logs: tail -f $LOG_FILE"
            info "Para detener: $0 stop"
        else
            error "GENIS no se inició correctamente. Ver logs en $LOG_FILE"
        fi
        ;;
        
    stop)
        if pgrep -f "java.*genis" > /dev/null; then
            info "Deteniendo GENIS..."
            pkill -f "java.*genis"
            sleep 2
            if ! pgrep -f "java.*genis" > /dev/null; then
                success "GENIS detenido correctamente"
            else
                error "No se pudo detener GENIS, intenta: sudo pkill -9 -f 'java.*genis'"
            fi
        else
            info "GENIS no está corriendo"
        fi
        ;;
        
    logs)
        if [ -f "$LOG_FILE" ]; then
            tail -f $LOG_FILE
        else
            error "Archivo de logs no encontrado: $LOG_FILE"
        fi
        ;;
        
    status)
        if pgrep -f "java.*genis" > /dev/null; then
            PID=$(pgrep -f "java.*genis")
            success "GENIS está corriendo (PID: $PID)"
            info "Accesible en http://localhost:9000"
        else
            info "GENIS no está corriendo"
        fi
        ;;
        
    sbt)
        info "Iniciando GENIS con sbt run..."
        info "Esto es más lento pero útil para desarrollo"
        info "Presiona Ctrl+C para detener"
        
        sbt run \
            --java-home $JAVA_HOME \
            -Xms512M \
            -Xmx10g \
            -Xss1M \
            -XX:+CMSClassUnloadingEnabled \
            -Dconfig.file=$CONFIG_FILE \
            -Dlogger.file=$LOGGER_FILE \
            -Dhttp.port=9000 \
            -Dhttps.port=9443
        ;;
        
    *)
        echo "Script para ejecutar GENIS"
        echo ""
        echo "Uso: $0 [comando]"
        echo ""
        echo "Comandos disponibles:"
        echo "  foreground  - Ejecutar GENIS en primer plano (por defecto)"
        echo "  background  - Ejecutar GENIS en segundo plano"
        echo "  stop        - Detener GENIS"
        echo "  logs        - Ver logs en tiempo real"
        echo "  status      - Ver estado de GENIS"
        echo "  sbt         - Ejecutar con 'sbt run' (más lento, para desarrollo)"
        echo ""
        echo "Ejemplos:"
        echo "  $0                 # Ejecuta en primer plano"
        echo "  $0 background      # Ejecuta en segundo plano"
        echo "  $0 stop            # Detiene GENIS"
        echo "  $0 logs            # Ve logs en tiempo real"
        echo ""
        echo "Configuración:"
        echo "  GENIS_HOME: $GENIS_HOME"
        echo "  JAVA_HOME:  $JAVA_HOME"
        echo "  CONFIG:     $CONFIG_FILE"
        exit 1
        ;;
esac
