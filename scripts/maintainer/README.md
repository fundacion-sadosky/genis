Contiene scripts que se ejecutaran durante las fases de instalacion de un paquete .deb

Se debe tener presente que dichos scripts pueden ejecutarse mas de una vez en las sucesivas actualizaciones del software. Por lo tanto se debe chequear que los script sean o bien idempotentes o bien verifiquen si se ejecutaron previamente. Mas informacion en:

https://wiki.debian.org/MaintainerScripts

Actualemnnte solo contiene scripts para ejecutar en la fase de post instalacion, los cuales automatizaran las configuraciones de genis y el software asociado. En caso de  necesitar agregar hooks para otras fases se debera instruir al build.sbt para que los incluya

Al armar el paquete se confeccionara un unico script con la concatencacion de todos los cripts en orden lexicografico. Por lo cual se deduce que las variables/funciones definidas seran visivble y posiblemente sobreescritas por cada script 


