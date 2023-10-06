# Changelog

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
