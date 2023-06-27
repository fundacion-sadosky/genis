# UPGRADING
La versión 5.1.9 de GENis introduce modificaciones en los scripts de datos utilizados por el framework play para actualizar el esquema de datos. Para actualizar a ésta versión se precisan realizar algunos pasos extraordinarios a fin de prevenir la pérdida de información.

## Pasos necesarios para actualizar a la versión 5.1.9
* Actualizar el sistema en la forma habitual a la versión 5.1.8 para asegurarnos que el esquema de datos quede acorde a esa versión.
* Realizar backups de las bases de datos por precaución, suponiendo que las bases se denominan *genisdb* y *genislogdb*:
`sudo -u genissqladmin pg_dump genisdb | gzip > "./genisdb.sql.gz"`
`sudo -u genissqladmin pg_dump genislogdb | gzip > "./genislogdb.sql.gz"`
* Borrar la tabla utilizada por el framework Play para determinarl el estado de los scripts de evolutions y recrearla con los datos adecuados:
`sudo -u genissqladmin psql -d genisdb -c "DROP TABLE public.play_evolutions;"`
`sudo -u genissqladmin psql -d genisdb < ./genis-evolutions.sql`[^1]
* Continuar con la actualización en la forma habitual

## Pasos habituales de actualización

Para actualizar GENis se debe pisar la carpeta */usr/share/genis* con la nueva versión pero previamente se debe realizar un backup de los archivos de configuración bajo */conf* para reutilizarlos en la nueva versión si no se modificaron o utilizarlos como referencia para la configurar la nueva versión.

* Detener GENis:
    * `sudo kill -9 \`cat /usr/share/genis/RUNNING_PID\`;`
    * `sudo rm -rf /usr/share/genis/RUNNING_PID;`
* Guardar una copia de los datos de configuración bajo */usr/share/genis/conf*
* Descomprimir la nueva versión bajo */user/share*: `unzip genis-5.1.9.zip` y renombrar la carpeta a *genis* reemplazando la vieja versión
* Dar permiso de ejecución a genis: `chmod +x /usr/share/genis/bin/genis`
* Adecuar los archivos de configuración de la nueva instalación acorde a los datos guardados previamente
* Ingresar a la aplicación desde el browser para que se ejecuten los scripts de evolutions de la nueva versión

[^1]:[genis-evolutions.sql]((https://github.com/fundacion-sadosky/genis/blob/main/utils/upgrade/genis-evolutions.pdf)).
