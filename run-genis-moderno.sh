#!/bin/bash

# ============================================================================
# Script para ejecutar GENis 6.0 con configuración moderna
# ============================================================================

set -e

echo "╔════════════════════════════════════════════════════════╗"
echo "║          GENis 6.0 - Startup Script                   ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Cambiar al directorio del proyecto
cd /home/cdiaz/Descargas/genis

# Limpiar PID anterior
rm -f target/universal/stage/RUNNING_PID

# Mensaje informativo
echo "🚀 Iniciando GENis 6.0..."
echo "   Framework: Play 3.0.0"
echo "   Scala: 2.13.12"
echo "   URL: http://localhost:9000"
echo ""

# Ejecutar SBT con configuración moderna
sbt -Dconfig.file=./conf/application-moderno.conf run
