#!/bin/bash

# Script para iniciar GENis con la configuración correcta

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
export PLAY_CONF_FILE=/home/cdiaz/Descargas/genis/conf/application.conf

# Cambiar al directorio de la aplicación compilada
cd /home/cdiaz/Descargas/genis/target/universal/stage

# Ejecutar GENis
./bin/genis -Dplay.server.provider=play.core.server.NettyServerProvider -Dplay.server.netty.transport=jvm -Dhttp.port=9000
