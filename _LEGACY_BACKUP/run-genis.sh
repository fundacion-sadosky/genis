#!/bin/bash

# Script para ejecutar GENis con Docker y la aplicación compilada

set -e

echo "======================================"
echo "Iniciando GENis"
echo "======================================"
echo ""

# Establecer Java 8
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Verificar que Docker está corriendo
echo "✓ Verificando Docker..."
cd /home/cdiaz/Descargas/genis/utils/docker

# Iniciar contenedores Docker
echo "✓ Iniciando contenedores Docker..."
sudo docker compose up -d > /dev/null 2>&1

# Esperar a que PostgreSQL esté listo
echo "✓ Esperando a que PostgreSQL se inicialice..."
sleep 10

# Iniciar GENis
echo "✓ Iniciando aplicación GENis..."
cd /home/cdiaz/Descargas/genis/target/universal/stage

./bin/genis \
  -Dconfig.file=/tmp/genis-docker.conf \
  -DapplyEvolutions.default=true \
  -DapplyEvolutions.logDb=true \
  -Dplay.server.provider=play.core.server.NettyServerProvider \
  -Dplay.server.netty.transport=jvm \
  -Dhttp.port=9000

echo ""
echo "======================================"
echo "✅ GENis iniciado en http://localhost:9000"
echo "======================================"
