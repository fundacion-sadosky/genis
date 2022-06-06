Genis
======

Proyecto de datos genéticos para la Fundación Sadosky

## Requerimientos

- JDK 8
- PostgreSQL 9.4.4
- MongoDB 2.6
- OpenLDAP
- Sbt 0.13.8

En la base de datos de SQL deben existir dos databases `pdgdb` el cual es creado por defecto y crear a mano otra, llamada `pdgLogDb`.

Toda la configuración de las conexiones a estos servicios externos se encuentra en el archivo de configuración `pdg-core/conf/application.conf`.

## Instalación
Para poder correr la aplicación es necesario:
- Posicionarse en la raíz de la aplicación
- Correr activator o sbt
- Ejecutar el comando `run`
- Entrar desde el navegador a http://localhost:9000/
- Para apagar la aplicación en la consola apretar `Ctrl + D`
