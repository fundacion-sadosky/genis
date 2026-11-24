## ---------------------------------------------------------------------------
## lrval :: paquete de intercambio del escenario de busqueda masiva
##
## Espejo de genis.R para el bloque 2. Mismo vocabulario de archivos que el
## paquete de casos --un lector escrito contra el bloque 1 tiene que servir
## aca-- a otra escala y con otras claves: familia y POI en vez de caso e
## hipotesis.
##
## Dos cosas que no son detalles. La primera: los 200 pedigries se describen
## una vez por tipo, no una vez por familia, porque las 200 familias comparten
## 20 topologias. La segunda: el mapa de que POI es el desaparecido de que
## familia NO se exporta. El sistema evaluado devuelve LR y no decisiones, asi
## que no habria filtracion, pero el experimento tiene que quedar ciego por
## construccion y no por confianza.
## ---------------------------------------------------------------------------

#' Genotipos de una matriz de nomina, en formato largo
#' @keywords internal
.geno_largo <- function(g, clave, loci) {
  do.call(rbind, lapply(loci, function(nm) {
    pz <- strsplit(g[, nm], "/", fixed = TRUE)
    data.frame(setNames(list(rownames(g)), clave), locus = nm,
               alelo_1 = vapply(pz, `[`, "", 1L),
               alelo_2 = vapply(pz, `[`, "", 2L), stringsAsFactors = FALSE)
  }))
}

#' Panel de frecuencias de un pedigri con marcadores, en formato largo
#'
#' Vive aparte de `.panel_largo()` porque es la mitad barata: el exportador
#' necesita SOLO esta, y armar la otra --las matrices celda por celda, unas
#' 50 mil filas sobre el panel de 24 loci-- para descartarla seria trabajo puro.
#' @keywords internal
.frec_largo <- function(src) {
  loci <- pedtools::name(src)
  do.call(rbind, lapply(seq_along(loci), function(j) {
    af <- pedtools::afreq(pedtools::getMarkers(src, j)[[1]])
    data.frame(locus = loci[j], alelo = names(af), frecuencia = as.numeric(af),
               stringsAsFactors = FALSE)
  }))
}

#' Panel y matrices de mutacion de un pedigri con marcadores
#' @keywords internal
.panel_largo <- function(src) {
  loci <- pedtools::name(src)
  mut <- do.call(rbind, lapply(seq_along(loci), function(j) {
    mm <- pedtools::mutmod(pedtools::getMarkers(src, j)[[1]])
    if (is.null(mm)) return(NULL)
    do.call(rbind, lapply(c("male", "female"), function(sx) {
      Mx  <- as.matrix(mm[[sx]])
      idx <- which(Mx > 0, arr.ind = TRUE)
      data.frame(locus = loci[j],
                 sexo_progenitor = if (sx == "male") "M" else "F",
                 alelo_origen = rownames(Mx)[idx[, 1]],
                 alelo_destino = colnames(Mx)[idx[, 2]],
                 probabilidad = Mx[idx], stringsAsFactors = FALSE)
    }))
  }))
  list(frecuencias = .frec_largo(src), mutacion = mut)
}

#' Declaracion del modelo de mutacion, una fila por locus y sexo
#'
#' Lo que viaja es la PARAMETRIZACION, no la matriz: GENis no puede recibir una
#' matriz, solo construirla desde nombre + parametros, y eso es lo que hace el
#' arnes (`ModeloMutacionGenis.desdeDeclarados`).
#'
#' La columna alelos NO arma el universo de alelos del grafo --ese sale de
#' frecuencias.csv (`MapeoMasiva.universo`, igual que `MapeoGenis.universo`
#' en el bloque 1)--. Entra en el alfabeto nombre -> codigo del arnes
#' (`MapeoMasiva.alelos` -> `MapeoGenis.mapearAlelos`), donde hoy no aporta
#' nada porque por construccion coincide con el panel. Viaja igual porque es la
#' escalera declarada del modelo, y `check_export_masiva()` la contrasta contra
#' el panel: si algun dia dejaran de coincidir, el paquete lo dice.
#' @keywords internal
.modelos_largo <- function(freqs, mut) {
  do.call(rbind, lapply(names(freqs), function(nm) data.frame(
    locus = nm, sexo_progenitor = c("M", "F"), modelo = mut$modelo,
    tasa = c(unname(mut$tasa[["M"]]), unname(mut$tasa[["F"]])),
    tasa2 = mut$tasa2, rango = mut$rango,
    alelos = paste(names(freqs[[nm]]), collapse = " "),
    pasos = mut$pasos, stringsAsFactors = FALSE)))
}

#' Reconstruye las matrices desde una declaracion de modelos.csv
#'
#' Espejo de .panel_largo()$mutacion --mismas columnas, solo celdas positivas--
#' pero armado desde los PARAMETROS declarados, que es el unico camino que
#' tiene el receptor del paquete. Los alelos y las frecuencias salen del panel,
#' no de la columna alelos: esa se verifica aparte.
#' @keywords internal
.modelos_a_matrices <- function(mo, freqs) {
  do.call(rbind, lapply(unique(mo$locus), function(nm) {
    fi <- mo[mo$locus == nm, ]
    spec <- list(modelo = fi$modelo[1],
                 tasa   = stats::setNames(as.numeric(fi$tasa), fi$sexo_progenitor),
                 tasa2  = as.numeric(fi$tasa2[1]),
                 rango  = as.numeric(fi$rango[1]),
                 pasos  = as.integer(fi$pasos[1]))
    mm <- build_mutmods(freqs[nm], spec)[[nm]]
    do.call(rbind, lapply(c("male", "female"), function(sx) {
      Mx  <- as.matrix(mm[[sx]])
      idx <- which(Mx > 0, arr.ind = TRUE)
      data.frame(locus = nm,
                 sexo_progenitor = if (sx == "male") "M" else "F",
                 alelo_origen = rownames(Mx)[idx[, 1]],
                 alelo_destino = colnames(Mx)[idx[, 2]],
                 probabilidad = Mx[idx], stringsAsFactors = FALSE)
    }))
  }))
}

#' Topologias y roles, una fila por individuo y tipo de pedigri
#' @keywords internal
.tipos_largo <- function(tipos) {
  list(
    tipos = do.call(rbind, lapply(tipos, function(tp) data.frame(
      tipo = tp, etiqueta = PEDIGREE_TYPES[[tp]]$etiqueta,
      grado = PEDIGREE_TYPES[[tp]]$grado, stringsAsFactors = FALSE))),
    pedigries = do.call(rbind, lapply(tipos, function(tp) {
      t <- PEDIGREE_TYPES[[tp]]
      d <- as.data.frame(t$ped)[, c("id", "fid", "mid", "sex")]
      data.frame(tipo = tp, individuo = d$id,
                 padre = ifelse(d$fid == "0", "", d$fid),
                 madre = ifelse(d$mid == "0", "", d$mid),
                 sexo  = ifelse(d$sex == 1, "M", "F"),
                 rol = ifelse(d$id == "MP", "bajo_prueba",
                       ifelse(d$id %in% t$typed, "tipificado", "no_tipificado")),
                 stringsAsFactors = FALSE)
    })))
}

#' Exporta el escenario masivo como paquete de texto plano
#'
#' @param fams familias de `simulate_mp_families()`
#' @param pool nomina de `build_poi_pool()`
#' @param freqs base de frecuencias
#' @param dir directorio de salida
#' @param escenario etiqueta que va al LEEME ("sin mutacion" / "con mutacion")
#' @param mut parametrizacion del modelo de mutacion (MUTACION_MASIVA) o
#'   NULL en el escenario sin mutacion
#' @export
export_masiva <- function(fams, pool, freqs, dir, escenario, mut = NULL) {
  dir.create(dir, recursive = TRUE, showWarnings = FALSE)
  loci  <- names(freqs)
  tipos <- sort(unique(vapply(fams, `[[`, "", "tipo")))
  tl    <- .tipos_largo(tipos)
  frec  <- .frec_largo(fams[[1]]$ped)
  w <- function(d, f) utils::write.csv(d, file.path(dir, f), row.names = FALSE, na = "")

  w(tl$tipos, "tipos.csv")
  w(tl$pedigries, "pedigries.csv")
  w(data.frame(familia = vapply(fams, `[[`, "", "id"),
               tipo = vapply(fams, `[[`, "", "tipo"), stringsAsFactors = FALSE),
    "familias.csv")
  ## perfiles: solo los parientes tipificados. ped_to_datamatrix() deja afuera
  ## al desaparecido, que no tiene genotipo, y esa es justamente la garantia.
  w(do.call(rbind, lapply(fams, function(f) {
      dm <- ped_to_datamatrix(f$ped)
      do.call(rbind, lapply(seq_along(loci), function(j) data.frame(
        familia = f$id, individuo = rownames(dm), locus = loci[j],
        alelo_1 = dm[, 2 * j - 1], alelo_2 = dm[, 2 * j], stringsAsFactors = FALSE)))
    })), "perfiles.csv")
  w(.geno_largo(pool$geno, "poi", loci), "poi.csv")
  w(frec, "frecuencias.csv")
  if (!is.null(mut)) w(.modelos_largo(freqs, mut), "modelos.csv")
  w(data.frame(familia = character(0), poi = character(0), log10LR = character(0),
               estado = character(0), observaciones = character(0),
               stringsAsFactors = FALSE), "resultados.csv")

  writeLines(c(
    sprintf("Escenario de busqueda masiva - %s", escenario),
    sprintf("%d familias x %d POI = %s comparaciones, %d loci",
            length(fams), nrow(pool$geno),
            formatC(length(fams) * nrow(pool$geno), format = "d", big.mark = ".",
                    decimal.mark = ","),
            length(loci)),
    "",
    "1. Los LR se informan en log10 (con 24 loci llegan a 10^22).",
    "2. Una exclusion se informa con el centinela -Inf, no con un numero muy",
    "   negativo: -999 es un LR de 10^-999, que no es lo mismo.",
    "3. Se piden TODAS las celdas, no solo las que superan un umbral: el",
    "   percentil 99,9 de las comparaciones no emparentadas se calcula sobre la",
    "   nomina completa.",
    "4. La unidad de entrega es la familia completa: una familia aporta sus",
    sprintf("   %d comparaciones o no aporta ninguna.", nrow(pool$geno)),
    if (is.null(mut)) NULL else
      "5. El modelo de mutacion viaja declarado en modelos.csv (modelo, tasa por",
    if (is.null(mut)) NULL else
      "   sexo, tasa2, rango, escalera y pasos), no como matriz: se reconstruye,",
    if (is.null(mut)) NULL else
      "   no se copia.",
    "",
    "El experimento es ciego: el mapa de que POI es el desaparecido de que",
    "familia no viaja en el paquete. El individuo con rol 'bajo_prueba' nunca",
    "lleva perfil.",
    "",
    "Se devuelve resultados_genis_masiva.csv (familia,poi,log10LR,estado,",
    "observaciones) junto con una copia de MANIFIESTO.txt. No se sobreescribe",
    "ningun archivo de este paquete."),
    file.path(dir, "LEEME.txt"))

  ## En el directorio puede haber un mutacion.csv de otra exportacion, y este
  ## exportador no lo escribe ni lo pisa. Si se quedara, el manifiesto lo
  ## inventariaria y el paquete entregado llevaria la declaracion de modelos.csv
  ## y una matriz suelta al lado, las dos firmadas, cuando el modelo viaja en
  ## uno o en el otro, nunca en los dos. Mismo remedio y mismo motivo que en el
  ## exportador del bloque 1 (`genis.R`).
  viejo <- file.path(dir, "mutacion.csv")
  if (file.exists(viejo)) unlink(viejo)

  write_manifest(dir)
  invisible(dir)
}

#' Verificaciones comunes al catalogo de tipos, al panel de frecuencias y al
#' modelo de mutacion
#'
#' `tipos.csv`, `pedigries.csv` y `frecuencias.csv` salen de la misma fuente en
#' memoria (`.tipos_largo()` / `.frec_largo()`), asi que su verificacion alinea
#' por clave y no por texto. `modelos.csv` es distinto: no es un volcado de la
#' matriz sino su DECLARACION, y se verifica reconstruyendo la matriz desde los
#' parametros --el unico camino que tiene el receptor-- y contrastandola contra
#' la que corrieron los motores.
#'
#' `pasos` es la excepcion, y conviene decirlo: la reconstruccion no lo ve.
#' `pedmut` no tiene parametro de truncamiento, asi que una declaracion con
#' `pasos` equivocado devuelve de este lado una matriz identica y la fila
#' "modelo de mutacion declarado" da PASA igual. El sistema receptor si trunca
#' (`cantSaltos`) y armaria otra matriz. Esa fila certifica que modelo, tasas
#' por sexo, tasa2 y rango alcanzan para reconstruir lo que corrieron los
#' motores; no certifica `pasos`. Lo que si se puede verificar con el paquete
#' solo va en la fila de la escalera: que `pasos` sea uno solo --`cantSaltos`
#' es un numero por modelo, no uno por locus-- y que alcance para que ninguna
#' transicion entera quede cortada, que es la propiedad de la que depende el
#' diseno (Stepwise sin truncar). Eso es `pasos >= ladder_span(alelos)`: la
#' amplitud se mide en repeticiones enteras --la cola de microvariantes no es
#' una transicion que el modelo defina, y GENis no la cuenta-- y la igualdad no
#' trunca, porque el corte de GENis es `pasos <= cantSaltos`
#' (`MutationService.stepDistance`). Es el mismo criterio y la misma funcion con la
#' que el exportador del bloque 1 declara `pasos` (`genis.R`).
#'
#' @param dir directorio del paquete
#' @param tipos tipos de pedigri esperados
#' @param freqs base de frecuencias (lista locus -> frecuencias)
#' @param mut_ref matrices que corrieron los motores (salida de
#'   `.panel_largo(ped)$mutacion`), usadas como CONTRASTE de la reconstruccion,
#'   o `NULL` si el escenario no lleva mutacion
#' @keywords internal
.check_panel_masiva <- function(dir, tipos, freqs, mut_ref) {
  rd <- function(f) utils::read.csv(file.path(dir, f), colClasses = "character")
  loci <- names(freqs)
  v <- function(nombre, cond) data.frame(verificacion = nombre,
    veredicto = if (isTRUE(cond)) "PASA" else "FALLA", stringsAsFactors = FALSE)

  ti <- rd("tipos.csv")
  pg <- rd("pedigries.csv")
  fr <- utils::read.csv(file.path(dir, "frecuencias.csv"))

  ## catalogo de tipos y estructura/roles de los pedigries: contra
  ## PEDIGREE_TYPES (via .tipos_largo(), el mismo traductor que usa el
  ## exportador), la fuente en memoria, alineados por clave y no por orden
  ## de archivo.
  tl_ref <- .tipos_largo(tipos)
  idx_ti <- match(tl_ref$tipos$tipo, ti$tipo)
  tipos_ok <- nrow(ti) == nrow(tl_ref$tipos) && !anyNA(idx_ti) &&
    identical(ti$etiqueta[idx_ti], tl_ref$tipos$etiqueta) &&
    identical(ti$grado[idx_ti], tl_ref$tipos$grado)

  clave_pg     <- paste(pg$tipo, pg$individuo)
  clave_pg_ref <- paste(tl_ref$pedigries$tipo, tl_ref$pedigries$individuo)
  idx_pg <- match(clave_pg_ref, clave_pg)
  pedigries_ok <- nrow(pg) == nrow(tl_ref$pedigries) && !anyNA(idx_pg) &&
    identical(pg$padre[idx_pg], tl_ref$pedigries$padre) &&
    identical(pg$madre[idx_pg], tl_ref$pedigries$madre) &&
    identical(pg$sexo[idx_pg],  tl_ref$pedigries$sexo) &&
    identical(pg$rol[idx_pg],   tl_ref$pedigries$rol)

  ## panel de frecuencias: alineado por la clave locus+alelo via match(), no
  ## por la posicion de la fila en el archivo.
  clave_fr    <- paste(fr$locus, fr$alelo)
  esperado_fr <- unlist(lapply(loci, function(nm) setNames(as.numeric(freqs[[nm]]),
                 paste(nm, names(freqs[[nm]])))))
  idx_fr <- match(names(esperado_fr), clave_fr)
  frec_ok <- nrow(fr) == length(esperado_fr) && !anyNA(idx_fr) &&
    isTRUE(all.equal(unname(fr$frecuencia[idx_fr]), unname(esperado_fr), tolerance = 1e-12))

  ## El modelo viaja declarado, no como matriz. La comprobacion que importa
  ## no es "el volcado coincide con el volcado" sino "la declaracion ALCANZA
  ## para reconstruir lo que corrieron los motores", que es lo que va a hacer
  ## GENis con su propia formula.
  archivo_mod <- file.path(dir, "modelos.csv")
  ## La matriz suelta es un modo de fallo de las DOS condiciones, no solo de la
  ## sin mutacion: otra exportacion pudo dejar mutacion.csv al lado de
  ## modelos.csv y el manifiesto lo firma igual, con lo que el paquete llevaria
  ## la declaracion y una matriz suelta a la vez. Se mira aca arriba, fuera
  ## de las ramas, y se cuenta en la fila del modelo --que es de lo que habla--
  ## y no en la de la escalera, cuyo nombre no diria lo que se rompio.
  sin_matriz <- !file.exists(file.path(dir, "mutacion.csv"))
  if (is.null(mut_ref)) {
    mod_ok <- sin_matriz && !file.exists(archivo_mod)
    ## sin modelo declarado no hay escalera que contrastar: la fila pasa por
    ## vacio y todo lo que hay que decir lo dice la fila del modelo.
    esc_ok <- TRUE
  } else if (!file.exists(archivo_mod)) {
    mod_ok <- esc_ok <- FALSE
  } else {
    mo  <- rd("modelos.csv")
    rec <- .modelos_a_matrices(mo, freqs)
    clave_re <- paste(rec$locus, rec$sexo_progenitor, rec$alelo_origen, rec$alelo_destino)
    clave_mr <- paste(mut_ref$locus, mut_ref$sexo_progenitor, mut_ref$alelo_origen,
                      mut_ref$alelo_destino)
    idx_re <- match(clave_mr, clave_re)
    mod_ok <- sin_matriz && nrow(rec) == nrow(mut_ref) && !anyNA(idx_re) &&
      isTRUE(all.equal(rec$probabilidad[idx_re], mut_ref$probabilidad, tolerance = 1e-12))
    ## la escalera y su tope de saltos se verifican juntos: son la misma
    ## propiedad mirada de los dos lados. Ver el caveat de la cabecera sobre
    ## por que pasos no puede verificarse por reconstruccion.
    pasos <- suppressWarnings(as.numeric(mo$pasos))
    esc_ok <- nrow(mo) == 2 * length(loci) && setequal(mo$locus, loci) &&
      all(vapply(seq_len(nrow(mo)), function(i)
        identical(strsplit(mo$alelos[i], " ", fixed = TRUE)[[1]],
                  names(freqs[[mo$locus[i]]])), TRUE)) &&
      !anyNA(pasos) && length(unique(pasos)) == 1 &&
      all(vapply(seq_len(nrow(mo)), function(i)
        pasos[i] >= ladder_span(strsplit(mo$alelos[i], " ", fixed = TRUE)[[1]]), TRUE))
  }

  rbind(
    v("catalogo de tipos de pedigri", tipos_ok),
    v("estructura y roles de los pedigries", pedigries_ok),
    v("panel de frecuencias", frec_ok),
    v("modelo de mutacion declarado", mod_ok),
    v("escalera alelica declarada", esc_ok),
    v("manifiesto", all(verify_manifest(dir)$veredicto == "OK")))
}

#' Verifica que el paquete exportado sea fiel al experimento que se corrio
#'
#' Relee los CSV y los compara contra los objetos en memoria. No recalcula
#' ningun LR: eso es de la reimportacion (`import_masiva()`). Pero si el
#' paquete no es fiel, cualquier discrepancia posterior del sistema evaluado
#' es inatribuible.
#'
#' Cada comprobacion esta pensada para que un modo de fallo concreto la haga
#' fallar, no solo para que un paquete bien escrito la pase: contar filas
#' ademas de compararlas (una familia entera ausente no puede pasar en
#' vacio), y comparar por clave en vez de por posicion (un `all.equal()` con
#' `check.attributes = FALSE` sobre vectores con nombre ignora el nombre y
#' compara solo el orden, lo que dejaria pasar dos loci con las etiquetas
#' intercambiadas).
#' @export
check_export_masiva <- function(dir, fams, pool, freqs) {
  loci  <- names(freqs)
  tipos <- sort(unique(vapply(fams, `[[`, "", "tipo")))
  v <- function(nombre, cond) data.frame(verificacion = nombre,
    veredicto = if (isTRUE(cond)) "PASA" else "FALLA", stringsAsFactors = FALSE)

  fa <- utils::read.csv(file.path(dir, "familias.csv"), colClasses = "character")
  pe <- utils::read.csv(file.path(dir, "perfiles.csv"), colClasses = "character")
  po <- utils::read.csv(file.path(dir, "poi.csv"), colClasses = "character")

  ## nomina: se reconstruye la matriz de genotipos y se compara con pool$geno
  g2 <- matrix(NA_character_, nrow = nrow(pool$geno), ncol = length(loci),
               dimnames = list(rownames(pool$geno), loci))
  g2[cbind(match(po$poi, rownames(g2)), match(po$locus, loci))] <-
    paste(po$alelo_1, po$alelo_2, sep = "/")

  ## perfiles: la cantidad de filas que corresponde por familia (un renglon
  ## por pariente tipificado y locus), no solo que las presentes coincidan.
  ## Contar antes de comparar es lo que le impide a una familia entera
  ## ausente pasar en vacio: vapply(seq_len(0), ...) da logical(0), y
  ## all(logical(0)) es TRUE en R.
  perf_ok <- all(vapply(fams, function(f) {
    dm <- ped_to_datamatrix(f$ped)
    q  <- pe[pe$familia == f$id, ]
    clave_q  <- paste(q$individuo, q$locus)
    clave_ok <- as.vector(outer(rownames(dm), loci, paste))
    if (nrow(q) != length(clave_ok) || !setequal(clave_q, clave_ok)) return(FALSE)
    all(vapply(seq_len(nrow(q)), function(i)
      identical(q$alelo_1[i], unname(dm[q$individuo[i], paste0(q$locus[i], ".1")])) &&
      identical(q$alelo_2[i], unname(dm[q$individuo[i], paste0(q$locus[i], ".2")])), TRUE))
  }, TRUE))

  mut_ref <- .panel_largo(fams[[1]]$ped)$mutacion

  rbind(
    v("familias y tipos", identical(fa$familia, unname(vapply(fams, `[[`, "", "id"))) &&
        identical(fa$tipo, unname(vapply(fams, `[[`, "", "tipo")))),
    v("nomina de POI, genotipo por genotipo", identical(g2, pool$geno)),
    v("perfiles de los parientes tipificados", perf_ok),
    v("el desaparecido no lleva perfil", !any(pe$individuo == "MP")),
    .check_panel_masiva(dir, tipos, freqs, mut_ref))
}

