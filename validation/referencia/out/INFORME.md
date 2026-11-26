# Informe de la validación del LR por pedigrí de GENis

Generado el 2026-11-26 17:55:10 en el commit e8f4a8a por `validation/correr informe`. No recalcula nada: toda cifra sale de los archivos de la corrida que están en disco.

## Veredicto

```
VEREDICTO DE LA VALIDACIÓN DEL LR POR PEDIGRÍ DE GENis
generado 2026-11-26 17:55:10 en el commit e8f4a8a | tolerancia de aceptación: discrepancia relativa <= 1.00e-09

PROCEDENCIA (la cadena se verifica por contenido: manifiestos y SHA-256, no por fechas)
  bloque 1              paquete 2026-11-24 18:02:09 | entrega de GENis commit 6482f36 | veredicto de la referencia commit 6482f36, 2026-11-24 18:02:13
                        cadena verificada: el veredicto es de esta entrega y la entrega es de este paquete
  masiva sin_mutacion  paquete 2026-11-24 18:45:49 | entrega de GENis commit 9e8887a, arrancada 2026-11-24 19:35:58 en 9e8887a | comparación commit 9e8887a, 2026-11-26 17:48:18
                        cadena verificada: el veredicto es de esta entrega y la entrega es de este paquete
  masiva con_mutacion  paquete 2026-11-24 18:45:52 | entrega de GENis commit 9e8887a, arrancada 2026-11-24 20:19:51 en 9e8887a | comparación commit 9e8887a, 2026-11-26 17:48:46
                        cadena verificada: el veredicto es de esta entrega y la entrega es de este paquete

Bloque 1, el catálogo: 51 casos, GENis calculó 51 | PASA=49  CONVENCION=2  REVISAR=0  FALLA=0  SIN_DATO=0
  diferencias de convención (2): SIL-01 PRU-02
  FALLA no previstas (0): ninguna
  contraste del arnés vs referencia: coinciden los 51 casos calculados
  invariantes 3 PASA | grilla de matrices 165 PASA
  => bloque 1: sin FALLA frente a la referencia; 2 diferencias de convención, explicadas en el informe

Bloque 2, la búsqueda masiva:
  sin_mutacion  1.000.000 celdas contra familias | PASA=1.000.000  REVISAR=0  FALLA=0  SIN_DATO=0
                rel_max 2.46e-14 (tolerancia 1.00e-09) sobre 281.787 celdas con magnitud | exclusiones sólo GENis 0, sólo referencia 0
                puente (los dos caminos de GENis): 384 celdas, diferencia máxima 2.00e-15
  con_mutacion  900.000 celdas contra pedsuite | PASA=900.000  REVISAR=0  FALLA=0  SIN_DATO=0
                rel_max 1.68e-10 (tolerancia 1.00e-09) sobre 724.466 celdas con magnitud | exclusiones sólo GENis 0, sólo referencia 0
                puente (los dos caminos de GENis): 633 celdas, diferencia máxima 4.26e-14
  => bloque 2: 1.900.000 celdas en 2 condiciones, todas PASA, cero desacuerdos categóricos

ADVERTENCIAS
  ninguna

Detalle por caso y por tipo: validation/referencia/out/INFORME.pdf (fuente INFORME.tex; el mismo contenido en INFORME.md)
Este texto: validation/referencia/out/VEREDICTO.txt
```

## Cómo leer este informe

- **Alcance**: se valida el cálculo de LR del motor de pedigrí de GENis (`BayesianNetwork`), el que usa la identificación por parentesco. El motor forense de mezclas no forma parte de esta validación.
- **PASA**: la discrepancia relativa entre el LR de GENis y el de la referencia está dentro de la tolerancia. **REVISAR**: hasta 1e-6. **FALLA**: mayor.
- **CONVENCION**: el caso admite dos convenciones documentadas que dan números distintos, y trae el valor de las dos en forma cerrada. GENis no da el valor de referencia pero sí, dentro de la misma tolerancia, el de la otra convención. No es un error de cálculo: abajo se explica cada una. Un número que no es ninguno de los dos sigue siendo FALLA.
- Una **FALLA no prevista** hay que mirarla antes de dar por buena la corrida. `validation/referencia/hallazgos_genis.csv` es la tabla donde se anota, una vez analizada, la explicación de una FALLA; hoy no tiene filas.
- La **cadena** paquete -> entrega -> veredicto se verifica por contenido (identidad del manifiesto y SHA-256 de la entrega), no por fechas. Si un eslabón está roto, el bloque no recibe veredicto: hay que volver a correr el paso que la cadena nombra.
- El bloque 1 es el catálogo caso por caso, contra valores en forma cerrada. El bloque 2 es la búsqueda masiva: una familia contra toda la nómina de personas de interés, celda por celda, contra los motores de referencia; ahí lo que más importa son los desacuerdos categóricos (`excl_solo_obs`, `excl_solo_ref`), en los que un lado excluye y el otro no.

## Bloque 1: el catálogo de casos

### Procedencia y cadena

| pieza | commit | fecha | identidad (prefijo del hash) |
| --- | --- | --- | --- |
| paquete de casos |  | 2026-11-24 18:02:09 | 15900ccf7ef3 |
| entrega de GENis | 6482f36 |  | 1b40a169b93b |
| veredicto de la referencia | 6482f36 | 2026-11-24 18:02:13 | 1b40a169b93b |

| eslabón | ok | detalle |
| --- | --- | --- |
| paquete -> entrega | cierra | la entrega consumió el paquete actual (manifiesto por contenido) |
| entrega -> veredicto | cierra | el veredicto se emitió sobre la entrega que está en disco (SHA-256) |

### Resumen

- casos del catálogo: 51; calculados por GENis: 51; PASA=49  CONVENCION=2  REVISAR=0  FALLA=0  SIN_DATO=0
- tolerancia de aceptación: discrepancia relativa <= 1.00e-09 (REVISAR hasta 1e-6)
- el veredicto propio del arnés (`contraste.txt`) coincide con el de la referencia en los 51 casos calculados

### Veredicto por caso

| caso | título | LR GENis | LR referencia | disc. rel. | veredicto | estado | nota |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PAR-01 | Dúo paterno: padre 1/2, hijo 1/3 | 2.5 | 2.5 | 3.552713679e-16 | PASA | ok |  |
| PAR-02 | Dúo paterno: padre 1/2, hijo 1/1 | 5 | 5 | 1.776356839e-16 | PASA | ok |  |
| PAR-03 | Trío: padre 1/2, madre 3/4, hijo 1/3 | 5 | 5 | 3.552713679e-16 | PASA | ok |  |
| PAR-04 | Trío: padre 1/1, madre 1/2, hijo 1/1 | 10 | 10 | 1.776356839e-16 | PASA | ok |  |
| PAR-05 | Dúo materno: madre 2/3, hijo 2/4 | 1.25 | 1.25 | 1.776356839e-16 | PASA | ok |  |
| PAR-06 | Hijo desaparecido con ambos padres tipificados | 1.5625 | 1.5625 | 1.421085472e-16 | PASA | ok |  |
| PAR-07 | Hijo desaparecido con un solo padre tipificado | 1.25 | 1.25 | 3.552713679e-16 | PASA | ok |  |
| PAR-08 | Un abuelo paterno tipificado, nieto desaparecido | 1.75 | 1.75 | 3.806478942e-16 | PASA | ok |  |
| PAR-09 | Tío paterno y sobrino desaparecido | 1.75 | 1.75 | 3.806478942e-16 | PASA | ok |  |
| PAR-10 | Un hermano completo | 1.5 | 1.5 | 1.480297366e-16 | PASA | ok |  |
| PAR-11 | Dos hermanos completos, tercer hermano desaparecido | 1.041666667 | 1.041666667 | 6.394884622e-16 | PASA | ok |  |
| PAR-12 | Medio hermano paterno | 1.75 | 1.75 | 3.806478942e-16 | PASA | ok |  |
| PAR-13 | Primos hermanos | 1.375 | 1.375 | 4.844609562e-16 | PASA | ok |  |
| PAR-14 | Bisabuelo y bisnieto desaparecido | 1.375 | 1.375 | 4.844609562e-16 | PASA | ok |  |
| PAR-15 | Dos tías del mismo lado (S1=S2=x/y, UI=x/z) | 1.809701493 | 1.809701493 | 6.134840631e-16 | PASA | ok |  |
| PAR-16 | Perfil idéntico comparado como hermanos | 6.083333333 | 6.083333333 | 2.92003864e-16 | PASA | ok |  |
| PAR-17 | Dúo paterno sin alelo compartido (exclusión, LR=0) | 0 | 0 | 0 | PASA | ok |  |
| SLO-41 | Trío clásico UI=MO=CH=(1,1): LR = 1/p1 | 10 | 10 | 1.776356839e-16 | PASA | ok |  |
| SLO-A1 | Tercer hermano: S1=(x,y), S2=(x,z), UI=(y,z) | 1.041666667 | 1.041666667 | 6.394884622e-16 | PASA | ok |  |
| SLO-A3 | Dos tías: S1=S2=(x,y), UI=(x,z) | 1.809701493 | 1.809701493 | 6.134840631e-16 | PASA | ok |  |
| SLO-INC | Pedigrí incestuoso: el padre del desaparecido es el padre de la madre | 1.25 | 1.25 | 3.552713679e-16 | PASA | ok |  |
| SLO-3G | Tres generaciones (progenitor e hijo del desaparecido), sin mutación | 3.571428571 | 3.571428571 | 2.486899575e-16 | PASA | ok |  |
| MUT-01 | Dúo con mutación de un paso, modelo Equal | 0.006666666667 | 0.006666666667 | 1.301042607e-16 | PASA | ok |  |
| MUT-02 | Dúo con mutación de un paso, modelo Stepwise | 0.005300959582 | 0.005300959582 | 1.636235335e-16 | PASA | ok |  |
| MUT-03 | Dúo con mutación de dos pasos, modelo Stepwise | 0.001893670477 | 0.001893670477 | 5.72540041e-16 | PASA | ok |  |
| MUT-04 | Dúo con mutación entera->microvariante (9 -> 9.3), Stepwise extendido | 0.004451629794 | 0.004451629794 | 1.363889732e-15 | PASA | ok |  |
| MUT-05 | Dúo con mutación microvariante->entera (9.3 -> 10), Stepwise extendido | 8.329880438e-05 | 8.329880438e-05 | 6.50790957e-16 | PASA | ok |  |
| MUT-06 | Dúo con mutación 9.3 -> 10.3 (mismo grupo decimal, un paso) | 0.01256495786 | 0.01256495786 | 6.903021464e-16 | PASA | ok |  |
| MUT-07 | Trío con tasas de mutación distintas por sexo (paterna 10x) | 0.01002555928 | 0.01002555928 | 1.038180571e-15 | PASA | ok |  |
| MUT-08 | Dúo excluyente SIN modelo de mutación (LR debe ser 0) | 0 | 0 | 0 | PASA | ok |  |
| MUT-09 | Trío consistente con modelo de mutación activo | 1.66110883 | 1.66110883 | 1.33672521e-16 | PASA | ok |  |
| MUT-10 | Dúo con tasa de mutación alta (0.10), modelo Equal | 0.1333333333 | 0.1333333333 | 6.245004514e-16 | PASA | ok |  |
| SIL-01 | Dúo con banda única en ambos, leída como a/a o a/silente | 0 | 0.666000666 | Inf | CONVENCION | ok | diferencia de convención |
| SIL-02 | Trío con genotipos explícitos madre 3/silent e hijo 1/silent | 5.263157895 | 5.263157895 | 8.437694987e-16 | PASA | ok |  |
| SIL-03 | Dúo con locus nulo registrado como silent/silent | 20 | 20 | 3.552713679e-16 | PASA | ok |  |
| FRQ-01 | Dúo con microvariante 15.2 | 2.5 | 2.5 | 3.552713679e-16 | PASA | ok |  |
| FRQ-02 | Dúo con microvariante rara sujeta a frecuencia mínima 0.001 | 250.24975 | 250.24975 | 1.135733779e-16 | PASA | ok |  |
| FRQ-02S | Gemelo de FRQ-02 sin piso declarado (alelo raro a 1e-6) | 250000 | 250000 | 6.98491931e-16 | PASA | ok |  |
| FRQ-03 | Dúo sobre un alelo en el piso de frecuencia (5e-4) | 500.249995 | 500.249995 | 1.47719031e-15 | PASA | ok |  |
| FRQ-04 | Trío multi-locus con base argentina (5 loci, incluye THO1 9.3) | 74.94640002 | 74.94640002 | 2.085749307e-15 | PASA | ok |  |
| FRQ-05 | Dúo: el padre alegado trae un alelo que el panel no tiene (piso 0.01) | 1.2625 | 1.2625 | 1.758769148e-16 | PASA | ok |  |
| FRQ-06 | Dúo: el hijo trae un alelo que el panel no tiene (piso 0.01) | 1.2625 | 1.2625 | 7.035076592e-16 | PASA | ok |  |
| FRQ-07 | Dúo: el alelo compartido es uno que el panel no tiene (piso 0.01) | 25.25 | 25.25 | 0 | PASA | ok |  |
| FRQ-08 | Trío con dos alelos que el panel no tiene, uno en cada progenitor (piso 0.01) | 5.1 | 5.1 | 6.966105253e-16 | PASA | ok |  |
| FRQ-09 | Dúo: alelo que el panel no tiene, sobre un panel con otro alelo bajo el piso (0.01) | 25.499975 | 25.499975 | 5.572889666e-16 | PASA | ok |  |
| PRU-01 | Tres generaciones CON mutación, pedigrí completo (sin podar) | 0.7594578942 | 0.7594578942 | 0 | PASA | ok |  |
| PRU-02 | Tres generaciones CON mutación, pedigrí podado (valor de referencia) | 0.7594578942 | 0.7577582827 | 0.002242946862 | CONVENCION | ok | diferencia de convención |
| INV-01 | Dúo paterno de base: padre 1/2, hijo 1/3 | 2.5 | 2.5 | 3.552713679e-16 | PASA | ok |  |
| INV-02 | Un hermano no tipificado no altera el LR del dúo paterno | 2.5 | 2.5 | 3.552713679e-16 | PASA | ok |  |
| INV-03 | El orden de los alelos dentro del genotipo no altera el LR | 2.5 | 2.5 | 3.552713679e-16 | PASA | ok |  |
| INV-04 | Locus no tipificado en el individuo cuestionado (LR debe ser 1) | 1 | 1 | 0 | PASA | ok |  |

### Diferencias de convención

**SIL-01**: Dúo con banda única en ambos, leída como a/a o a/silente

El caso registra una sola banda en cada individuo (1 en el padre, 3 en el hijo), escrita como genotipo 1/1 y 3/3. El valor de referencia sigue la convención de Familias: una banda única puede ser a/a o a/silente, y se suma sobre las dos posibilidades. La otra convención, la de pedsuite, toma el genotipo tal como está escrito: con 1/1 y 3/3 el dúo es una exclusión y el LR es 0.

Medido: GENis da 0; el valor de referencia es 0.666000666 y el de la otra convención, 0, que es el que GENis reproduce dentro de la tolerancia.

**PRU-02**: Tres generaciones CON mutación, pedigrí podado (valor de referencia)

El mismo pedigrí que PRU-01, con el valor del pedigrí podado (Slooten 2011, ec. 4.4), que es la convención de Familias 2.6.4 con modelo de mutación. Un motor que conserva los fundadores no tipificados, como pedsuite, da aquí el valor de PRU-01.

Medido: GENis da 0.7594578942; el valor de referencia es 0.7577582827 y el de la otra convención, 0.7594578942, que es el que GENis reproduce dentro de la tolerancia.

### Otros indicadores

Invariantes entre casos, sobre los LR de GENis:

| invariante | descripción | veredicto | detalle |
| --- | --- | --- | --- |
| CRUCE-01/GENis | Abuelo-nieto, tío-sobrino y medio hermano comparten kappa=(1/2,1/2,0): mismo LR | PASA | abuelo=1.75 tío=1.75 medio_hermano=1.75 |
| CRUCE-02/GENis | Primos hermanos y bisabuelo-bisnieto comparten kappa=(3/4,1/4,0): mismo LR | PASA | primos=1.375 bisabuelo=1.375 |
| CRUCE-03/GENis | Exclusión sin modelo de mutación: LR exactamente 0 | PASA | PAR-17=0 MUT-08=0 |

Grilla de matrices de mutación volcadas por GENis: 165 PASA.

Convención para el fundador no tipificado, medida sobre los casos con mutación: GENis lo **conserva** (la misma en todos los casos: PASA).

Piso de frecuencia: el caso con piso declarado junto a su control sin piso:

| caso_piso | caso_sin_piso | piso | LR_piso | LR_sin_piso | diagnóstico |
| --- | --- | --- | --- | --- | --- |
| FRQ-02 | FRQ-02S | 0.001 | 250.24975 | 250000 | PISO_OK |

Descomposición de la diferencia en los casos con mutación (d_algoritmo mide el cálculo sobre la matriz propia; d_modelo, la matriz; d_semantica, la convención para el fundador no tipificado):

| caso | matriz | semántica | d_total | d_algoritmo | d_modelo | d_semantica | ver_algoritmo | ver_conformidad |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| MUT-01 | volcada | indistinguible | 0 | 0 | 0 | 0 | PASA | PASA |
| MUT-02 | volcada | conserva | 0 | 0 | 0 | 0 | PASA | PASA |
| MUT-03 | volcada | conserva | -4.44e-16 | -4.44e-16 | 0 | 0 | PASA | PASA |
| MUT-04 | volcada | conserva | 4.44e-16 | 4.44e-16 | 0 | 0 | PASA | PASA |
| MUT-05 | volcada | conserva | 8.88e-16 | 8.88e-16 | 0 | 0 | PASA | PASA |
| MUT-06 | volcada | conserva | 4.44e-16 | 4.44e-16 | 0 | 0 | PASA | PASA |
| MUT-07 | volcada | conserva | 4.44e-16 | 4.44e-16 | 0 | 0 | PASA | PASA |
| MUT-09 | volcada | conserva | -8.33e-17 | -8.33e-17 | 0 | 0 | PASA | PASA |
| MUT-10 | volcada | indistinguible | -2.22e-16 | -2.22e-16 | 0 | 0 | PASA | PASA |

## Bloque 2: la búsqueda masiva

### Condición sin_mutacion

#### Procedencia y cadena

| pieza | commit | fecha | identidad (prefijo del hash) |
| --- | --- | --- | --- |
| paquete |  | 2026-11-24 18:45:49 | 2e9af994e0e7 |
| entrega de GENis | 9e8887a | 2026-11-24 19:35:58 en 9e8887a | 56f84a95a3b1 |
| comparación celda a celda | 9e8887a | 2026-11-26 17:48:18 | 56f84a95a3b1 |

| eslabón | ok | detalle |
| --- | --- | --- |
| paquete -> entrega | cierra | la entrega consumió el paquete actual (manifiesto por contenido) |
| entrega -> veredicto | cierra | el veredicto se emitió sobre la entrega que está en disco (misma `arrancada`, mismo SHA-256) |

Condiciones de la corrida de GENis: artefacto = 5.1.15; commit = 9e8887a; arrancada = 2026-11-24 19:35:58 en 9e8887a; camino = busqueda (getGenotypification por familia + findCompatibilityMatches por POI); umbral de reporte = salteado (se entregan todas las celdas); familias = 200; poi = 5000; celdas = 1000000; hilos = 8; puente = 3; puente.semilla = 20260828; puente.tipos_sin_muestra = ninguno; mutacion = sin modelo.

#### Veredicto celda a celda

| motor | celdas | pasa | revisar | falla | sin_dato | celdas_magnitud | rel_max | rel_mediana | excl_ambos | excl_solo_obs | excl_solo_ref |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| familias | 1e+06 | 1e+06 | 0 | 0 | 0 | 2.82e+05 | 2.46e-14 | 2.12e-15 | 7.18e+05 | 0 | 0 |

`rel_max` se calcula sobre las celdas con magnitud comparable (sin las que ambos lados excluyen); tolerancia 1.00e-09. `excl_solo_obs` y `excl_solo_ref` son desacuerdos categóricos: uno excluye y el otro no.

Cobertura: 200 de 200 familias aceptadas, 1.000.000 celdas con dato; verificación del manifiesto independiente: sí.

#### Por tipo de pedigrí

| motor | tipo | n_familias | celdas | pasa | revisar | falla | sin_dato | rel_max | excl_solo_obs | excl_solo_ref |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| familias | abuelo_1 | 5 | 2.5e+04 | 2.5e+04 | 0 | 0 | 0 | 1.13e-14 | 0 | 0 |
| familias | abuelos_2_cruzados | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 1.64e-14 | 0 | 0 |
| familias | abuelos_2_mismo | 7 | 3.5e+04 | 3.5e+04 | 0 | 0 | 0 | 1.23e-14 | 0 | 0 |
| familias | abuelos_3 | 8 | 4e+04 | 4e+04 | 0 | 0 | 0 | 8.24e-15 | 0 | 0 |
| familias | abuelos_4 | 3 | 1.5e+04 | 1.5e+04 | 0 | 0 | 0 | 4.09e-15 | 0 | 0 |
| familias | hermano_1 | 9 | 4.5e+04 | 4.5e+04 | 0 | 0 | 0 | 1.64e-14 | 0 | 0 |
| familias | hermanos_2 | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 4.21e-15 | 0 | 0 |
| familias | hermanos_3 | 12 | 6e+04 | 6e+04 | 0 | 0 | 0 | 8.26e-15 | 0 | 0 |
| familias | madre | 11 | 5.5e+04 | 5.5e+04 | 0 | 0 | 0 | 4.11e-15 | 0 | 0 |
| familias | madre_hermano | 11 | 5.5e+04 | 5.5e+04 | 0 | 0 | 0 | 8.18e-15 | 0 | 0 |
| familias | medio_hermano_1 | 9 | 4.5e+04 | 4.5e+04 | 0 | 0 | 0 | 1.13e-14 | 0 | 0 |
| familias | medios_hermanos_2 | 6 | 3e+04 | 3e+04 | 0 | 0 | 0 | 1.23e-14 | 0 | 0 |
| familias | padre | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 4.07e-15 | 0 | 0 |
| familias | padre_hermano | 15 | 7.5e+04 | 7.5e+04 | 0 | 0 | 0 | 4.22e-15 | 0 | 0 |
| familias | padres | 12 | 6e+04 | 6e+04 | 0 | 0 | 0 | 0 | 0 | 0 |
| familias | padres_hermano | 15 | 7.5e+04 | 7.5e+04 | 0 | 0 | 0 | 8.2e-15 | 0 | 0 |
| familias | tio_1 | 12 | 6e+04 | 6e+04 | 0 | 0 | 0 | 1.13e-14 | 0 | 0 |
| familias | tios_2_cada_lado | 16 | 8e+04 | 8e+04 | 0 | 0 | 0 | 2.46e-14 | 0 | 0 |
| familias | tios_2_maternos | 5 | 2.5e+04 | 2.5e+04 | 0 | 0 | 0 | 1.23e-14 | 0 | 0 |
| familias | tios_2_paternos | 14 | 7e+04 | 7e+04 | 0 | 0 | 0 | 1.64e-14 | 0 | 0 |

No hay celdas discrepantes: `celdas_discrepantes.csv` no existe porque no hubo ninguna.

#### Puente: los dos caminos de cálculo de GENis

Muestra pedida (celdas por tipo de pedigrí y decil): 3. 384 celdas muestreadas, diferencia máxima en log10 2.00e-15. Los dos caminos tienen que coincidir a precisión de máquina.

#### Acuerdo entre los motores de referencia

| motor_a | motor_b | n_pares | max_dif_log10 | veredicto | exigido | medido | commit |
| --- | --- | --- | --- | --- | --- | --- | --- |
| familias | pedsuite | 1e+06 | 7.11e-15 | PASA | TRUE | 2026-11-24 | 9e8887a |

Una fila con `medido` vacío o con otra fecha es una medición preservada de otra corrida, no de esta.

### Condición con_mutacion

#### Procedencia y cadena

| pieza | commit | fecha | identidad (prefijo del hash) |
| --- | --- | --- | --- |
| paquete |  | 2026-11-24 18:45:52 | 63decb4fff0d |
| entrega de GENis | 9e8887a | 2026-11-24 20:19:51 en 9e8887a | b1fee8f28f41 |
| comparación celda a celda | 9e8887a | 2026-11-26 17:48:46 | b1fee8f28f41 |

| eslabón | ok | detalle |
| --- | --- | --- |
| paquete -> entrega | cierra | la entrega consumió el paquete actual (manifiesto por contenido) |
| entrega -> veredicto | cierra | el veredicto se emitió sobre la entrega que está en disco (misma `arrancada`, mismo SHA-256) |

Condiciones de la corrida de GENis: artefacto = 5.1.15; commit = 9e8887a; arrancada = 2026-11-24 20:19:51 en 9e8887a; camino = busqueda (getGenotypification por familia + findCompatibilityMatches por POI); umbral de reporte = salteado (se entregan todas las celdas); familias = 180; poi = 5000; celdas = 900000; hilos = 1; puente = 3; puente.semilla = 20260828; puente.tipos_sin_muestra = ninguno; mutacion = stepwise; mutacion.rango = 0.5; mutacion.pasos = 22; mutacion.ki_no_nulos = 754.

#### Veredicto celda a celda

| motor | celdas | pasa | revisar | falla | sin_dato | celdas_magnitud | rel_max | rel_mediana | excl_ambos | excl_solo_obs | excl_solo_ref |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| pedsuite | 9e+05 | 9e+05 | 0 | 0 | 0 | 7.24e+05 | 1.68e-10 | 4.06e-15 | 1.76e+05 | 0 | 0 |

`rel_max` se calcula sobre las celdas con magnitud comparable (sin las que ambos lados excluyen); tolerancia 1.00e-09. `excl_solo_obs` y `excl_solo_ref` son desacuerdos categóricos: uno excluye y el otro no.

Cobertura: 180 de 180 familias aceptadas, 900.000 celdas con dato; verificación del manifiesto independiente: sí.

#### Por tipo de pedigrí

| motor | tipo | n_familias | celdas | pasa | revisar | falla | sin_dato | rel_max | excl_solo_obs | excl_solo_ref |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| pedsuite | abuelo_1 | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 1.43e-14 | 0 | 0 |
| pedsuite | abuelos_2_cruzados | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 1.65e-14 | 0 | 0 |
| pedsuite | abuelos_2_mismo | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 4.09e-14 | 0 | 0 |
| pedsuite | abuelos_3 | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 4.11e-14 | 0 | 0 |
| pedsuite | abuelos_4 | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 8.19e-14 | 0 | 0 |
| pedsuite | hermano_1 | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 2.04e-14 | 0 | 0 |
| pedsuite | hermanos_2 | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 1.68e-10 | 0 | 0 |
| pedsuite | hermanos_3 | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 6.55e-14 | 0 | 0 |
| pedsuite | madre | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 6.55e-14 | 0 | 0 |
| pedsuite | madre_hermano | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 8.19e-14 | 0 | 0 |
| pedsuite | medio_hermano_1 | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 1.23e-14 | 0 | 0 |
| pedsuite | medios_hermanos_2 | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 2.05e-14 | 0 | 0 |
| pedsuite | padre | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 6.55e-14 | 0 | 0 |
| pedsuite | padre_hermano | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 4.92e-14 | 0 | 0 |
| pedsuite | padres | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 9.83e-14 | 0 | 0 |
| pedsuite | padres_hermano | 10 | 5e+04 | 5e+04 | 0 | 0 | 0 | 9.83e-14 | 0 | 0 |
| pedsuite | tio_1 | 5 | 2.5e+04 | 2.5e+04 | 0 | 0 | 0 | 1.59e-14 | 0 | 0 |
| pedsuite | tios_2_cada_lado | 5 | 2.5e+04 | 2.5e+04 | 0 | 0 | 0 | 3.28e-14 | 0 | 0 |
| pedsuite | tios_2_maternos | 5 | 2.5e+04 | 2.5e+04 | 0 | 0 | 0 | 2.06e-14 | 0 | 0 |
| pedsuite | tios_2_paternos | 5 | 2.5e+04 | 2.5e+04 | 0 | 0 | 0 | 2.05e-14 | 0 | 0 |

No hay celdas discrepantes: `celdas_discrepantes.csv` no existe porque no hubo ninguna.

#### Puente: los dos caminos de cálculo de GENis

Muestra pedida (celdas por tipo de pedigrí y decil): 3. 633 celdas muestreadas, diferencia máxima en log10 4.26e-14. Los dos caminos tienen que coincidir a precisión de máquina.

Con modelo de mutación el veredicto de GENis se emite contra pedsuite, porque Familias aplica otra convención para los fundadores no tipificados (los poda); es la misma diferencia de convención que muestran PRU-01 y PRU-02 en el catálogo.

## Advertencias

Ninguna.

