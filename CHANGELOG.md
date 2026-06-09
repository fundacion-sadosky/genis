# Changelog

## [v5.1.13] - 2026-03-30

_Si está actualizando el sistema por favor lea: [`UPGRADING.md`](https://github.com/fundacion-sadosky/genis/blob/main/UPGRADING.md)._

### Added

- **Desktop Search**: es posible ingresar un perfil para buscar coincidencias
  sin que quede almacenado en la base de datos. El perfil se carga por archivo
  marcando un checkbox que indica búsqueda de escritorio. Se respetan las normas
  de admisibilidad, se listan los matches encontrados y se puede exportar el
  reporte con el LR por defecto. Nada queda almacenado en la base.
- **Réplica a instancia superior desde Aceptación Masiva**: es posible replicar
  perfiles hacia la instancia superior directamente desde la pantalla de
  Aceptación Masiva (paso 2), seleccionando perfiles individuales o el lote
  completo.
- **Exportar e importar la configuración del sistema**: es posible exportar e
  importar la configuración completa del sistema (marcadores, kits, categorías
  y sus tablas auxiliares). El botón de importación se deshabilita cuando ya
  existen perfiles en el sistema.
- **Cambio de categoría para perfiles de evidencia**: ahora también es posible
  cambiar la categoría de perfiles de tipo evidencia, además de los indubitados.
- **Notificación de baja de perfil en instancia superior**: cuando se da de baja
  un perfil en la instancia superior, se envía una notificación a la instancia
  inferior indicando la instancia que originó la baja. El perfil queda marcado
  como No Replicado y puede volver a replicarse.
- **Confirmación de coincidencia habilitada solo una vez**: el botón de confirmar
  o descartar una coincidencia se deshabilita cuando la misma ya no está en
  estado pendiente, evitando confirmaciones duplicadas.
- **Herramienta de reporte en Aceptación Masiva**: se incorpora la herramienta
  de generación de reportes en la pantalla de Aceptación Masiva, con más datos
  en los reportes y filtros de fechas en los PDFs.
- **Paginación en Aceptación Masiva**: se agrega paginación en los pasos 1 y 2
  de la Aceptación Masiva para mejorar el manejo de lotes grandes.
- Se registra el usuario correcto en la trazabilidad al aceptar o rechazar una
  coincidencia.
- Se modificaron los permisos para recibir notificaciones después de importar un
  perfil desde una instancia inferior. Los usuarios que reciban la notificación
  deben tener permiso para importar perfiles de una instancia inferior y recibir
  notificaciones por interconexión de instancias.

### Fixed

- Se corrigió un error en la visualización de la tabla de perfiles en el paso 2
  de Aceptación Masiva.
- Se corrigió un error al cargar perfiles con campos vacíos en Aceptación Masiva.
  ([#128](https://github.com/fundacion-sadosky/genis/issues/128)).
- Se mejoró la visualización de mensajes de error en el paso 1 de Aceptación
  Masiva.
  ([#129](https://github.com/fundacion-sadosky/genis/issues/129)).
- Se corrigieron los archivos de muestra en Aceptación Masiva.
  ([#130](https://github.com/fundacion-sadosky/genis/issues/130)).
- Se corrigió un error que producía notificaciones duplicadas al actualizar desde
  una versión anterior.
  ([#141](https://github.com/fundacion-sadosky/genis/issues/141)).
- Se corrigió un error en la generación de reportes.
  ([#162](https://github.com/fundacion-sadosky/genis/issues/162)).
- Se deshabilita la opción de replicar a instancia superior cuando el perfil fue
  utilizado en una búsqueda de Desktop Search.
  ([#183](https://github.com/fundacion-sadosky/genis/issues/183)).
- Se corrigió el auto-refresh del inbox al recibir notificaciones SSE.
  ([#184](https://github.com/fundacion-sadosky/genis/issues/184)).
- Se corrigió un error que impedía replicar el cambio de categoría de un perfil
  que tenía una coincidencia local con un perfil ya replicado.
  ([#182](https://github.com/fundacion-sadosky/genis/issues/182)).
- Se corrigió un error que impedía replicar perfiles que tenían coincidencias ya
  replicadas.
  ([#195](https://github.com/fundacion-sadosky/genis/issues/195)).
- Se optimizaron las consultas de reportes y se muestra un indicador
  "Procesando..." mientras se genera el reporte.
  ([#237](https://github.com/fundacion-sadosky/genis/issues/237)).
- Se corrigió el límite máximo de alelos por marcador (trisomyThreshold + 1).
  ([#242](https://github.com/fundacion-sadosky/genis/issues/242)).
- Se corrigió el formato de fechas de datetime a date en varias pantallas.
  ([#245](https://github.com/fundacion-sadosky/genis/issues/245)).
- Se corrigió un error en la pantalla de Aceptación Masiva al acceder desde una
  notificación.
  ([#247](https://github.com/fundacion-sadosky/genis/issues/247)).
- Se corrigió un error que no mantenía el mensaje de error al aceptar un perfil
  en el paso 2 de Aceptación Masiva.
  ([#251](https://github.com/fundacion-sadosky/genis/issues/251)).
- Se corrigió un error por el cual, cuando dos instancias tenían un asignee con
  el mismo nombre, se confirmaban o descartaban las coincidencias de ambas.
  ([#252](https://github.com/fundacion-sadosky/genis/issues/252)).
- Los perfiles descartados como coincidencia ya no se muestran coloreados como
  perfiles con match en el listado de perfiles.

[v5.2.0]: https://github.com/fundacion-sadosky/genis/releases/tag/v5.2.0

## v[5.1.12] - 2024-11-14

_Si está actualizando el sistema por favor lea:  [`UPGRADING.md`](https://github.com/fundacion-sadosky/genis/blob/main/UPGRADING.md)._

### Added

- Los perfiles con conicidencias se muestran en color diferente en el listade perfiles.
  ([#100](https://github.com/fundacion-sadosky/genis/issues/100)).
- Ahora es posible cambiar la categoría de un perfil indubitado.
  El cambio relanza las comparaciones basadas en la nueva categoría.
  El cambio de categoría se puede replicar en la instancia superior.
  ([#95](https://github.com/fundacion-sadosky/genis/issues/95)).

### Fixed

- Se corrigió un error se que producía cuando se intentaban ver todos los perfiles
  en la visualización de coincidencias en el módulo forense.
  ([#64](https://github.com/fundacion-sadosky/genis/issues/64)).
- Se corrigió un error que mostraba texto estra en la tabla de comparación de alelos.
  ([#65](https://github.com/fundacion-sadosky/genis/issues/65)).
- Se corrigió un error en el módulo MPI dado por pedigrís que no tienen una persona
  faltante/desconocida.
  ([#70](https://github.com/fundacion-sadosky/genis/issues/70)).
- Se corrigió un error que permitía la creación de perfiles que no tienen un persona
  faltante/desconocida.
  ([#72](https://github.com/fundacion-sadosky/genis/issues/72)).
- Se corrigió un error en la verificación de consistencia de un pedigrí que no
  tiene una persona faltante/desconocida.
  ([#77](https://github.com/fundacion-sadosky/genis/issues/77)).
- Se corrigió un error de tipos en el test de PedigreeMatchesService.
  ([#93](https://github.com/fundacion-sadosky/genis/issues/93)).
- Se corrigió un error en la comparación de perfiles.
  ([#108](https://github.com/fundacion-sadosky/genis/issues/108)).
- Se corrigió un error en el cálculo de LR corregido por N.
  ([#109](https://github.com/fundacion-sadosky/genis/issues/109)).
- Se corrigió un error en el cálculo de LR promedio que se da cuando unos de los LRs es 0.0.
  ([#110](https://github.com/fundacion-sadosky/genis/issues/110)).

[v5.1.12]: https://github.com/fundacion-sadosky/genis/releases/tag/v5.1.12

## [v5.1.11] - 2023-10-25

### Fixed

- Se mejoró el proceso de instalación en entornos de desarrollo y producción
  usando contenedores de Docker.
  ([#53](https://github.com/fundacion-sadosky/genis/issues/53)).
- Se corrigió un error en el módulo DVI por el cual se generaba un error,
  cuando se hace la agrupación de resultados.
  ([#56](https://github.com/fundacion-sadosky/genis/issues/56)).
- Se corrigió un error en el módulo MPI que producía coincidencias inespecíficas.
  ([#57](https://github.com/fundacion-sadosky/genis/issues/57)).
- Se modificó la carga de análisis mitocondriales para que acepte inserciones
  usando tanto la notación que comienza con un in signo menos ('-'), como la que
  que no lo requiere.
  ([#59](https://github.com/fundacion-sadosky/genis/issues/59)).

[v5.1.11]: https://github.com/fundacion-sadosky/genis/releases/tag/v5.1.11

## [v5.1.10] - 2023-10-05

### Changed

- La carga masiva acepta de datos mitocondriales para MPI acepta archivos que
  tienen un rango definido pero no tienen mutaciones. Esto implica que la
  secuencia es identica a la secuencia de referencia
  ([#34](https://github.com/fundacion-sadosky/genis/issues/34)).
- La notación de regiones homopoliméricas en análsis mitocondrial requiere que
  comience con un signo '-'
  ([#34, #38](https://github.com/fundacion-sadosky/genis/issues/34)).
- Se mejora la utlización de servicios mediante contendores con docker y se
  documenta el proceso de instalación para entornos de desarrollo y producción
  ([#53](https://github.com/fundacion-sadosky/genis/issues/53)). 

### Fixed

- En familias complejas, el cálculo de LR podía producir un NaN, debido a
  errores de presición. Ahora el resultado es el correcto
  ([#43](https://github.com/fundacion-sadosky/genis/issues/43)).
- Se corrigió un error en el apareamiento de análisis mitocondriales cuando
  una mutación en un individuo cae fuera del rango definido en la otra 
  persona ([#34, #39](https://github.com/fundacion-sadosky/genis/issues/34)).
- Se corrigió un error en el cual el apareamiento de una análisis mitocondrial
  con una cambio ambiguo no era contrastado contra la secuencia de referencia si
  la posición de esa mutación no está dentro de un rango definido en la otra
  persona ([#34](https://github.com/fundacion-sadosky/genis/issues/34)).
- Se corrigió un error en el cual se podía dar una coincidencia entre dos
  individuos de referencia en un caso de MPI
  ([#37](https://github.com/fundacion-sadosky/genis/issues/37)).
- Se corrigió un error en el cual el cual el boton para subir frecuencias estaba
  activo cuando no había un archivo de frecuencias seleccionado para subir
  ([#30](https://github.com/fundacion-sadosky/genis/issues/30)).
- Se corrigió un error que se producía al mientras se inicia la aplicación
  debido a que se intentaba acceder a una ruta que requiere un usuario
  auntenticado antes de mostrar la pantalla de ingreso.
  ([#35](https://github.com/fundacion-sadosky/genis/issues/35)).

[v5.1.10]: https://github.com/fundacion-sadosky/genis/releases/tag/v5.1.10

## [v5.1.9] - 2023-06-23

### Changed
	
- Se permite la carga de datos parciales de datos de filiación asociados al perfil ([#19](https://github.com/fundacion-sadosky/genis/issues/19))

### Added

- Agregamos filtro de búsqueda por categoría de perfil ([#5](https://github.com/fundacion-sadosky/genis/issues/5))
- Al enviar un perfil a una instancia superior analizamos los marcadores considerando los identificadores y los alias ([#13](https://github.com/fundacion-sadosky/genis/issues/13))
- Guardamos en sesión los datos del fitro de búsqueda de perfiles y la página actual de resultados para no perderlos al ingresar a la información de un perfil en particular ([#11](https://github.com/fundacion-sadosky/genis/issues/11))

### Fixed

- Paginación en búsqueda de perfiles ([#5](https://github.com/fundacion-sadosky/genis/issues/5))
- Visualización de escenarios guardados ([#18](https://github.com/fundacion-sadosky/genis/issues/18))

[v5.1.9]: https://github.com/fundacion-sadosky/genis/releases/tag/v5.1.9
