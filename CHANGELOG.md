# Changelog

## Unreleased

### Fixed

- Se modificaron los permisos para recibir notificaciones después de 
  importar un perfil desde en una instancia inferior. Los usuarios que reciban
  la notificación deben tener permiso para:
  - Importar perfiles de una instancia inferior.
  - Recibir notificaciones por interconexión de instancias.
- Los superusuarios ya no reciben por defecto notificaciones de importación de
  perfiles. Anteriormente, recibian la notificación pero no podían hacer 
  nada a menos que tengan los permisos para:
  - Importar perfiles de una instancia inferior.
  - Recibir notificaciones por interconexión de instancias.
  En ese caso, además se genera un mensaje de error por la falta de los 
    permisos.

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
