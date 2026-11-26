# Cómo correr la validación, de punta a punta

Esta carpeta valida el **cálculo de LR del motor de pedigrí de GENis**
(`app/pedigree/BayesianNetwork.scala`). El motor forense de mezclas no forma parte de esta
validación.

Tiene **dos mitades que no comparten código**:

| | dónde vive | qué hace |
|---|---|---|
| **La referencia** | `validation/referencia/` (R) | Define los casos, calcula el LR de referencia y emite el veredicto. No llama al motor que se valida |
| **El arnés** | `validation/scala/` (Scala) | Corre los mismos casos sobre el motor de GENis |

Y **dos bloques**, que se validan por separado:

| bloque | qué mide | escala | tiempo |
|---|---|---|---|
| **1: catálogo** | caso por caso, contra valores en forma cerrada de la literatura | 51 casos | unos 6 minutos |
| **2: búsqueda masiva** | familias contra una nómina de personas de interés, celda por celda, contra los motores de referencia | 1.900.000 celdas | alrededor de dos días |

---

## El orquestador

Todo se corre con **un solo ejecutable**, desde cualquier directorio:

```bash
validation/correr --lista        # el menú completo
validation/correr masiva --plan  # qué haría, sin hacerlo
validation/correr masiva         # hacerlo
```

Una sola regla de superficie:

> **con sustantivo se corre ese paso; sin sustantivo, el bloque entero, en orden.**

```
correr bloque1 [referencia|genis|veredicto]
correr masiva  [paquete|sonda|genis|comparar]
correr masiva genis [sin_mutacion|con_mutacion]
correr informe
correr todo
```

`bloque1`, `masiva` y `todo` cierran con el informe; `correr informe` lo genera suelto
sobre lo que haya en disco, en segundos y sin sbt. Ver «El informe» más abajo.

`correr` es el **único** lugar que sabe rutas, escalas, banderas y orden.

**`--plan` no corre ni toca nada.** Imprime los comandos literales que ejecutaría, con las
rutas ya resueltas y el tiempo estimado de cada paso. Conviene usarlo siempre antes de una
corrida larga.

Las otras banderas globales: `--si` (desatendido), `--sin-candado`, `--forzar` (el candado
quedó colgado de un proceso muerto), `--sin-respaldo`. Para `masiva genis`: `--salida DIR`
escribe la entrega de GENis en otro lado y `--reanudar` fuerza seguir sobre particiones de
otra corrida (ver más abajo).

### Lo que hace alrededor de la corrida

- **Plan y confirmación.** Antes de arrancar imprime los pasos. Si alguno cuesta horas,
  pregunta.
- **Candado.** `validation/out/corridas/<bloque>.lock`, con pid y paso actual; se libera
  solo. Guarda esta copia del repositorio, no la máquina: una corrida desde otra copia no
  la ve.
- **Registro de la corrida.** `validation/out/corridas/<fecha>-<bloque>.log`, con el commit
  (sufijado `-dirty` si el árbol no estaba limpio), el comando literal de cada paso, su
  código de salida y su duración.
- **Resguardo de la entrega anterior de GENis.** Si `masiva genis` encuentra en
  `out/masiva/<cond>/` una salida que no es de esta misma corrida, la mueve entera a
  `out/masiva/anteriores/<fecha>-<commit>/<cond>/`, la anota en `anteriores/INDICE.txt` y
  relanza el paso. Nunca borra.

### Requisitos

- **Java 8** y sbt, igual que para compilar el motor (ver el README de la raíz).
- **R** con los paquetes `pedtools`, `pedprobr`, `pedmut`, `forrel`, `ribd`, `Familias`,
  `mispitools` y `pedFamilias`. No se usa `renv`: tienen que estar instalados. Las
  versiones con las que se emitió el veredicto publicado quedan en
  `referencia/out/resultados/entorno.txt`.
- `pdflatex`, sólo para compilar el informe a PDF; sin él queda el `.tex`.

---

## Bloque 1: el catálogo

```bash
validation/correr bloque1        # unos 6 minutos
```

Son tres pasos y el orden importa:

```
  referencia  ──►  GENis  ──►  referencia
  arma cases/      contesta    emite el veredicto
```

| paso | tiempo | qué deja |
|---|---|---|
| `bloque1 referencia` | ~2 min | `validation/cases/`: casos, frecuencias, `modelos.csv`, la grilla de matrices y su `MANIFIESTO.txt` |
| `bloque1 genis` | ~2 min | `validation/out/`: `resultados_genis.csv`, `contraste.txt`, `matrices_genis.csv` |
| `bloque1 veredicto` | ~2 min | `referencia/out/resultados/`: veredicto por caso y motor, diferencias de convención, trazabilidad, `entorno.txt`, y el sello `PROCEDENCIA.txt` que dice qué entrega juzgó |
| el informe | ~10 s | `referencia/out/VEREDICTO.txt`, `INFORME.md`, `INFORME.tex` e `INFORME.pdf` |

La primera vez sbt descarga dependencias y compila; los tiempos de arriba son con eso ya
hecho.

**El veredicto se corre con `--con-genis` y el orquestador ya lo hace.** Corrido a mano sin
esa bandera, `cases/` se reexporta igual y el veredicto deja de incluir a GENis.

### Qué contiene el catálogo

| prefijo | casos | qué cubre |
|---|---|---|
| `PAR` | 17 | parentescos sin mutación: dúos, tríos, abuelos, tíos, hermanos, medios hermanos, primos |
| `SLO` | 5 | ejemplos de Slooten (2011) |
| `MUT` | 10 | modelos de mutación Equal, Stepwise y Stepwise extendido |
| `SIL` | 3 | alelo silente |
| `FRQ` | 10 | frecuencias: microvariantes, piso de frecuencia, alelos que el panel no trae |
| `PRU` | 2 | fundador no tipificado con mutación: pedigrí completo y podado |
| `INV` | 4 | invariantes: individuo o locus sin datos, orden de los alelos |

El alcance es el del motor de pedigrí. Lo que ese motor no modela (corrección por
coancestría θ, drop-in y drop-out, otros modelos de mutación) no tiene casos acá; está
dicho en el README de la raíz.

---

## Bloque 2: la búsqueda masiva

200 familias contra 5.000 personas de interés —un millón de comparaciones— sin modelo de
mutación, y 180 familias contra las mismas 5.000 con modelo de mutación.

```bash
validation/correr masiva --plan   # mirar primero
validation/correr masiva          # alrededor de dos días
```

### Los pasos y lo que cuestan

| paso | lado | tiempo | qué hace |
|---|---|---|---|
| `masiva paquete` | R | ~45 min | respalda, arma el paquete y corre los motores de referencia |
| `masiva sonda` | sbt | ~50 min | mide el costo por tipo de pedigrí antes de comprometer horas; no escribe entrega |
| `masiva genis sin_mutacion` | sbt | ~45 min | GENis corre la condición sin mutación, con su puente |
| `masiva genis con_mutacion` | sbt | ~10 h, más el puente (~30 h) | GENis corre la condición con mutación |
| `masiva comparar` | R | ~2 min | el anticipo (80 pares) y la comparación celda a celda |

Los tiempos son los de la corrida publicada, en una notebook de 8 núcleos y 16 GB. El
orquestador no impide que la máquina se suspenda: si se suspende, la corrida se detiene y
retoma sola al volver.

**Con mutación el veredicto va contra pedsuite.** Familias aplica otra convención para los
fundadores no tipificados (los poda), así que con modelo de mutación sus números no son
comparables celda a celda con los de un motor que los conserva; es la misma diferencia de
convención que muestran `PRU-01` y `PRU-02` en el catálogo. Por eso, **por omisión,
Familias no corre en la condición con mutación**: es el paso más caro de toda la
validación (unas 40 h de reloj, 192 h de núcleo) y no emite veredicto. Para correrlo
igual:

```bash
validation/correr masiva --con-familias-mutacion
```

**La sonda va primero.** Si un tipo de pedigrí da un costo que no cierra con el tiempo
disponible, la salida es achicar **por el eje de familias, nunca por el de personas de
interés**: el costo es por familia, así que bajar la nómina no ahorra nada y en cambio
recorta la cola de la distribución, que es lo que el escenario mide.

**Hilos y memoria.** El arnés corre en una JVM con `-Xmx6g`. Con mutación, varias familias
de hermanos calculándose a la vez no entran en esa memoria, así que
`masiva genis con_mutacion` corre **con 1 hilo por omisión**. Sin mutación usa todos los
núcleos. `--hilos N` manda en las dos condiciones.

**El puente** contrasta los dos caminos de cálculo de GENis (la búsqueda y el cálculo
directo de un pedigrí) sobre una muestra de celdas: `--puente K` pide K celdas por tipo de
pedigrí y decil, y el valor por omisión es 3 (633 celdas con mutación). Con mutación es
serial y es lo más largo de toda la validación, unas 30 horas: cada celda de un tipo con
hermanos cuesta del orden de media hora. Con `--puente 1` lleva la tercera parte.
El cierre de la corrida se escribe **antes** del puente: si se interrumpe, queda
`puente = pendiente` en `condiciones_masiva.txt`, la entrega ya sirve para
`masiva comparar`, y volver a lanzar `masiva genis con_mutacion` corre sólo el puente.

**Las dos condiciones no corren a la misma escala, a propósito.** Sin mutación son 200
familias con el tipo de pedigrí sorteado uniformemente; con mutación son 180 con
asignación estratificada, porque con Stepwise el costo por familia varía en órdenes de
magnitud según el tipo. El `MANIFIESTO.txt` de cada paquete es la fuente de verdad sobre
la escala entregada.

**`masiva genis` se puede interrumpir y reanudar.** La unidad es la familia: se escribe
una parcial por familia y se renombra atómicamente, así que «el archivo existe» significa
«la familia está completa». Volver a lanzar saltea las que ya están, **pero sólo si son de
esta misma corrida**.

### Qué pasa con una entrega anterior de GENis

- La corrida que arranca desde cero deja `out/masiva/<cond>/parcial/PROCEDENCIA.txt` con
  el commit, la versión, el SHA-256 del `MANIFIESTO.txt` del paquete y la hora de arranque.
  Cada reanudación agrega una línea `reanudada`.
- **Se reanuda sólo una corrida incompleta del mismo commit y el mismo paquete.** Cualquier
  otra salida (de otro commit, sin sello, o ya cerrada) hace que el arnés se niegue antes
  de calcular o escribir nada.
- **`correr` mueve esa salida entera** a `out/masiva/anteriores/<fecha>-<commit>/<cond>/` y
  relanza el paso limpio. Cada movimiento queda en `out/masiva/anteriores/INDICE.txt` y en
  el registro de la corrida. Nada se borra; `anteriores/` crece unos 120 MB por corrida
  movida y se poda a mano.
- **`--reanudar`** sirve para el caso «hice un commit a mitad de una corrida de horas»:
  sigue sobre las particiones existentes y deja los dos commits escritos en
  `condiciones_masiva.txt`.
- Quien invoque `sbt "validacion:runMain validation.RunnerMasiva ..."` a mano recibe la
  misma negativa (exit 5 si la salida es de otra corrida, 6 si está completa) y tiene que
  mover la salida.
- `-dirty` no distingue dos árboles con cambios distintos sobre el mismo commit.

### Qué deja

| archivo | qué es |
|---|---|
| `out/masiva/<cond>/resultados_genis_masiva.csv` | la entrega: una fila por (familia, persona de interés) |
| `out/masiva/<cond>/condiciones_masiva.txt` | versión, commit, camino de cálculo, escala, modelo de mutación, y de dónde vienen las particiones (`arrancada`, `reanudada`) |
| `out/masiva/<cond>/parcial/PROCEDENCIA.txt` | el sello de la corrida que escribió las particiones |
| `out/masiva/<cond>/puente_masiva.csv` | el contraste entre los dos caminos de cálculo de GENis |
| `referencia/out/masiva/comparacion/acuerdo_celdas.csv` | el veredicto celda a celda |
| `referencia/out/masiva/comparacion/PROCEDENCIA_<cond>.txt` | el sello del veredicto: qué entrega de GENis juzgó y contra qué paquete |
| `referencia/out/masiva/comparacion/celdas_discrepantes.csv` | sólo lo que no es `PASA`. Si no existe, es porque no hubo ninguna |

### Qué mirar

1. **`excl_solo_obs` y `excl_solo_ref`.** Un desacuerdo categórico, en el que un lado
   excluye y el otro no, importa más que cualquier decimal.
2. **`condiciones_masiva.txt`**: que el camino sea el de búsqueda y que la escala coincida
   con el manifiesto. Un `ki_no_nulos = 0` en la condición con mutación significa que
   corrió sin mutación.
3. **`puente_masiva.csv`**: los dos caminos de GENis tienen que coincidir a precisión de
   máquina.
4. **`rel_max`** se calcula sobre las celdas con magnitud comparable, es decir sin
   aquellas donde los dos lados excluyen. `celdas_magnitud` dice sobre cuántas se calculó.
5. **`arrancada` y `reanudada`** en `condiciones_masiva.txt`: si el commit de `arrancada`
   no es el de `commit`, la entrega mezcla familias calculadas por dos versiones.

El arnés corre la búsqueda sin ver ningún valor de referencia: la matriz de referencia no
viaja en el paquete de entrada, porque revelaría qué persona de interés es el desaparecido
de cada familia.

---

## El informe

```bash
validation/correr informe        # unos 10 segundos
```

Es el último paso de `bloque1`, `masiva` y `todo`, y se puede pedir suelto. **No recalcula
nada**: lee lo que hay en disco y lo presenta en dos niveles.

| archivo | qué es |
|---|---|
| `referencia/out/VEREDICTO.txt` | el nivel corto, que es lo que se imprime por consola al final: procedencia, conteo por veredicto de cada bloque, advertencias |
| `referencia/out/INFORME.md` | el nivel largo: el mismo veredicto, cómo leerlo, la tabla por caso con el LR de GENis y el de referencia, la explicación de cada diferencia de convención y, para la búsqueda masiva, la tabla por tipo de pedigrí y el puente |
| `referencia/out/INFORME.pdf` | el mismo nivel largo en PDF: se genera desde el mismo documento que el Markdown, se escribe como `INFORME.tex` y se compila con `pdflatex` |

### Los veredictos

- **PASA**: la discrepancia relativa entre el LR de GENis y el de referencia es de 1e-9 o
  menor. **REVISAR**: hasta 1e-6. **FALLA**: mayor.
- **CONVENCION**: el caso admite dos convenciones documentadas que dan números distintos, y
  el paquete trae el valor de las dos en forma cerrada (`LR_referencia` y
  `LR_otra_convencion` en `casos.csv`). El motor no da el valor de referencia pero sí,
  dentro de la misma tolerancia, el de la otra convención. No es un error de cálculo; el
  informe explica cada una. Un número que no es ninguno de los dos es FALLA.

Una **FALLA no prevista** hay que mirarla antes de dar por buena la corrida.
`referencia/hallazgos_genis.csv` es la tabla donde se anota, una vez analizada, la
explicación de una FALLA, para que el informe la muestre con su título; hoy no tiene
filas.

### La cadena

Antes de sumar nada, el informe verifica que las piezas de cada bloque sean de la misma
corrida: que el paquete actual sea el que la entrega consumió (identidad del manifiesto
por contenido) y que la entrega en disco sea la que el veredicto selló (SHA-256 del
archivo; en la masiva, además la línea `arrancada`). Las fechas no cuentan. Si un eslabón
está roto, el bloque queda **`SIN VEREDICTO`** y el informe dice qué paso volver a correr.

Lo que no rompe la cadena pero se avisa: commits distintos entre las dos mitades, árbol
`-dirty`, puente pendiente, reanudación sobre otro commit, familias que no entraron al
veredicto.
