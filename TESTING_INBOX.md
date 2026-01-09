# Guía de Testing - Módulo Inbox Modernizado

**Objetivo**: Verificar que el módulo Inbox implementado mantiene 100% de paridad funcional con la versión legacy.

## Configuración Previa

### 1. Verificar que el servidor está corriendo
```bash
# En otra terminal, ejecutar si no está corriendo:
cd /home/cdiaz/Descargas/genis
sbt run
```

### 2. Acceder a la aplicación
- URL: http://localhost:9000/dashboard
- Usuario: `setup`
- Contraseña: `pass`
- TOTP: `ETZK6M66LFH3PHIG`

---

## Test Cases

### TC-01: Cargar Inbox
**Pasos**:
1. Hacer login exitosamente
2. Hacer click en "Inbox" en el menú lateral
3. Esperar a que carguen las notificaciones

**Resultado esperado**:
- ✅ Tabla de notificaciones visible
- ✅ Contador de notificaciones total > 0
- ✅ Filtros muestran conteos correctos

---

### TC-02: Filtro Todo (All)
**Pasos**:
1. Hacer click en botón "Todo" en toolbar
2. Observar tabla de notificaciones

**Resultado esperado**:
- ✅ Muestra TODAS las notificaciones
- ✅ Botón "Todo" resaltado (fondo purple)
- ✅ Contador = todas las notificaciones del usuario

---

### TC-03: Filtro Pendiente
**Pasos**:
1. Hacer click en botón "Pendiente" en toolbar
2. Observar tabla

**Resultado esperado**:
- ✅ Muestra SOLO notificaciones con `pending=true`
- ✅ Contador = notificaciones pendientes
- ✅ Filas tienen fondo azul claro (#f0f4ff)

---

### TC-04: Filtro Marcado
**Pasos**:
1. Hacer click en botón "Marcado" en toolbar
2. Observar tabla

**Resultado esperado**:
- ✅ Muestra SOLO notificaciones con `flagged=true`
- ✅ Contador = notificaciones marcadas
- ✅ Iconos de bandera son amarillos

---

### TC-05: Búsqueda por Título
**Pasos**:
1. Entrar en filtro "Todo"
2. Escribir texto en campo "Buscar por título o descripción..."
3. Esperar 300ms (debounce)

**Resultado esperado**:
- ✅ Tabla filtra por coincidencias en título
- ✅ Paginación se resetea a página 1
- ✅ Muestra mensaje "Sin resultados" si no hay coincidencias

**Ejemplo**:
- Buscar: "matching" → Muestra solo notificaciones cuyo título contiene "matching"

---

### TC-06: Búsqueda por Descripción ⭐ NUEVA
**Pasos**:
1. Entrar en filtro "Todo"
2. Escribir texto que aparezca en descripción (no en título)
3. Esperar 300ms

**Resultado esperado**:
- ✅ Tabla filtra por coincidencias en descripción
- ✅ Encontradas notificaciones aunque el título no contenga el texto
- ✅ Case-insensitive (mayúsculas y minúsculas)

**Ejemplo**:
- Si existe notificación con título="Análisis" y descripción="Sistema genético"
- Buscar: "genético" → Debe mostrar esta notificación

---

### TC-07: Filtro por Tipo
**Pasos**:
1. Hacer dropdown "Todos los tipos"
2. Seleccionar uno (ej: "Matching")
3. Observar tabla

**Resultado esperado**:
- ✅ Muestra SOLO notificaciones con `notificationType="matching"`
- ✅ Badge del tipo tiene color específico
- ✅ Contador se actualiza

**Tipos disponibles**:
- 📊 Matching (azul)
- 📥 Importación (verde)
- 👤 Usuario (amarillo)
- 📋 Perfil (turquesa)
- 👨‍👩‍👧 Pedigree (magenta)
- 🔬 Análisis (púrpura)
- 📄 Reporte (naranja)

---

### TC-08: Búsqueda por Rango de Fechas ⭐ NUEVA
**Pasos**:
1. Hacer click en campo "Desde"
2. Seleccionar una fecha de inicio (ej: 01/01/2025)
3. Hacer click en campo "Hasta"
4. Seleccionar una fecha final (ej: 31/12/2025)
5. Sistema filtra automáticamente

**Resultado esperado**:
- ✅ Solo muestran notificaciones con createdAt entre ambas fechas
- ✅ Rango es inclusivo (desde las 00:00:00 del primer día hasta 23:59:59 del último)
- ✅ Funciona con una sola fecha (ej: solo "Desde")

**Caso de error**:
- Seleccionar Desde=31/12/2025 y Hasta=01/01/2025
- Resultado esperado: ❌ Alerta "La fecha 'Desde' debe ser anterior o igual a 'Hasta'"

---

### TC-09: Combinación de Filtros
**Pasos**:
1. Seleccionar Filtro: "Pendiente"
2. Tipo: "Matching"
3. Rango de fechas: 01/01/2025 - 31/12/2025
4. Búsqueda: "important"

**Resultado esperado**:
- ✅ Aplicados todos los filtros simultáneamente
- ✅ Tabla muestra SOLO notificaciones que cumplen TODAS las condiciones:
  - `pending=true`
  - `notificationType="matching"`
  - `createdAt` entre rangos
  - Título O descripción contienen "important"

---

### TC-10: Botón Limpiar Filtros ⭐ NUEVA
**Pasos**:
1. Aplicar varios filtros (tipo, fecha, búsqueda, etc.)
2. Hacer click en botón "Limpiar" (X gris)
3. Observar formulario

**Resultado esperado**:
- ✅ Todos los inputs se limpian:
  - Campo de búsqueda vacío
  - Tipo resetea a "Todos los tipos"
  - Desde vacío
  - Hasta vacío
- ✅ Tabla vuelve a mostrar TODO (filtro "Todo" seleccionado)
- ✅ Paginación se resetea a página 1

---

### TC-11: Paginación
**Pasos**:
1. Si hay más de 30 notificaciones:
   - Observar números de página abajo
2. Hacer click en página 2
3. Hacer click en página 1

**Resultado esperado**:
- ✅ Tabla muestra siguiente página de 30 items
- ✅ Página activa está resaltada
- ✅ Botón anterior deshabilitado en página 1
- ✅ Botón siguiente deshabilitado en última página

---

### TC-12: Ordenamiento
**Pasos**:
1. Hacer click en encabezado "Fecha"
2. Hacer click nuevamente

**Resultado esperado**:
- Primera vez: ✅ Ordena DESC (más nuevo primero), ⬇️ chevron
- Segunda vez: ✅ Ordena ASC (más viejo primero), ⬆️ chevron

---

### TC-13: Marcar como Flagged
**Pasos**:
1. Hacer click en icono de bandera (🚩) de una notificación
2. Observar color

**Resultado esperado**:
- ✅ Bandera cambia de gris a amarillo
- ✅ Notificación se mueve a filtro "Marcado" si está aplicado
- ✅ Contador de "Marcado" aumenta

---

### TC-14: Desmarcar Flagged
**Pasos**:
1. Hacer click nuevamente en bandera amarilla
2. Observar color

**Resultado esperado**:
- ✅ Bandera cambia de amarillo a gris
- ✅ Notificación desaparece de filtro "Marcado"
- ✅ Contador de "Marcado" disminuye

---

### TC-15: Marcar como Leído y Navegar
**Pasos**:
1. Hacer click en cualquier fila de notificación (título o descripción)
2. Si la notificación tiene `url`, debería navegar
3. Si no tiene `url`, solo marca como leído

**Resultado esperado**:
- ✅ POST a `/api/notifications/{id}/read` ejecutado
- ✅ Notificación marcada como leída (desaparece de "Pendiente")
- ✅ Si tiene URL, navega a ella; sino vuelve a cargar inbox

---

### TC-16: Seleccionar Todo (Select All)
**Pasos**:
1. Hacer click en checkbox del encabezado
2. Observar checkboxes de filas

**Resultado esperado**:
- ✅ Todos los checkboxes de la tabla se marcan
- ✅ Checkbox del encabezado se marca
- ✅ Segunda vez desselecciona todo

---

### TC-17: Eliminar Seleccionadas
**Pasos**:
1. Marcar 2-3 notificaciones con sus checkboxes
2. Hacer click en botón "🗑️ Eliminar"
3. Confirmar en popup

**Resultado esperado**:
- ✅ Popup pregunta: "¿Eliminar X notificación(es)?"
- ✅ Si confirma, DELETE a `/api/notifications/{id}` para cada
- ✅ Tabla se recarga automáticamente
- ✅ Notificaciones desaparecen
- ✅ Contadores se actualizan

**Si cancela**:
- ✅ No se elimina nada

---

### TC-18: Actualizar (Refresh Button)
**Pasos**:
1. Hacer click en botón "🔄 Actualizar"
2. Esperar

**Resultado esperado**:
- ✅ GET a `/api/notifications` se ejecuta
- ✅ Tabla se recarga con últimos datos
- ✅ Spinner muestra mientras carga

---

### TC-19: Auto-refresh
**Pasos**:
1. Dejar inbox abierta por 31+ segundos
2. No hacer ninguna acción

**Resultado esperado**:
- ✅ Cada 30 segundos se ejecuta `loadNotifications()`
- ✅ Nueva notificación (si llega) aparece automáticamente

---

### TC-20: Sin Resultados
**Pasos**:
1. Buscar por texto que NO existe (ej: "xyzabc123")
2. Observar tabla

**Resultado esperado**:
- ✅ Tabla desaparece
- ✅ Mensaje "No se encontraron notificaciones según su búsqueda"
- ✅ Paginación desaparece

---

## Matriz de Verificación

| # | Característica | Legacy | Moderno | Resultado |
|---|---|---|---|---|
| 1 | Listar notificaciones | ✅ | ✅ | |
| 2 | Tabla 5 columnas | ✅ | ✅ | |
| 3 | Paginación 30/página | ✅ | ✅ | |
| 4 | Contador total | ✅ | ✅ | |
| 5 | Filtro Todo | ✅ | ✅ | |
| 6 | Filtro Pendiente | ✅ | ✅ | |
| 7 | Filtro Marcado | ✅ | ✅ | |
| 8 | Buscar por tipo | ✅ | ✅ | |
| 9 | Buscar por título | ✅ | ✅ | |
| 10 | **Buscar por descripción** | ✅ | ✅ NEW | |
| 11 | **Buscar por rango fechas** | ✅ | ✅ NEW | |
| 12 | Flag/Unflag | ✅ | ✅ | |
| 13 | Marcar como leído | ✅ | ✅ | |
| 14 | Eliminar | ✅ | ✅ | |
| 15 | **Limpiar filtros** | ✅ | ✅ NEW | |

---

## Notas de Testing

### Performance
- Esperado: Tabla carga en < 2 segundos
- Búsqueda en vivo con debounce 300ms
- Paginación instantánea

### Navegadores Soportados
- ✅ Chrome/Chromium 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Edge 90+

### Responsive
- ✅ Desktop (1920x1080)
- ✅ Tablet (768x1024)
- ✅ Mobile (375x667)

---

## Reporte de Testing

**Fecha**: ___________  
**Tester**: ___________  
**Navegador**: ___________  

| TC | Resultado | Notas |
|----|-----------|-------|
| 01 | ✅ / ❌ | |
| 02 | ✅ / ❌ | |
| ... | | |
| 20 | ✅ / ❌ | |

**Resumen**: ___________  
**Issues encontrados**: ___________  
**OK para producción**: ✅ / ❌

---

**Aprobado por**: _____________________  
**Fecha aprobación**: _____________________
