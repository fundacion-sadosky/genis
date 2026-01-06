# ⚠️ Tener en Cuenta - Limpieza de Proyecto (6 de Enero 2026)

## 🧹 Reestructuración del Proyecto

Se ha realizado una limpieza profunda de la estructura del proyecto para separar el código moderno (Play 3.0, Scala 2.13) del código heredado (Legacy).

### 📁 Nueva Estructura
La raíz del proyecto ahora solo contiene los archivos necesarios para la versión moderna:
- **`app/`**: Código fuente moderno (Controladores V2, Vistas nuevas, Servicios de Autenticación).
- **`conf/`**: Configuración moderna (`application-moderno.conf`, `routes` limpios).
- **`public/`**: Recursos estáticos.
- **`project/` y `build.sbt`**: Configuración de compilación.
- **Documentación**: `INICIO.md`, `README-MODERNO.md`, `QUICK-START.md`.

### 📦 Respaldo Legacy (`_LEGACY_BACKUP/`)
Todos los archivos y carpetas antiguos **NO se han eliminado**, sino que se han movido a la carpeta `_LEGACY_BACKUP/` para mantener el área de trabajo limpia. Esta carpeta incluye:
- `app.viejo/`, `app-2.11/`, `codegen/`
- `test/` (tests antiguos)
- `scripts/`, `utils/`, `lib/`
- Archivos de configuración antiguos (`application.conf` original, `routes.viejo`, etc.)
- Documentación antigua (`README.md` original, `CHANGELOG.md`, etc.)

Si necesitas consultar código antiguo o recuperar algún archivo, búscalo primero en esta carpeta.

## 🚀 Ejecución del Proyecto

**IMPORTANTE**: Debido a que el archivo `application.conf` original (que apuntaba a configuraciones legacy) fue movido al backup, **siempre** debes iniciar la aplicación especificando el archivo de configuración moderno:

```bash
sbt -Dconfig.file=./conf/application-moderno.conf run
```

Si intentas ejecutar solo `sbt run`, es posible que falle o no encuentre la configuración adecuada por defecto si no se ha renombrado el archivo moderno a `application.conf` (actualmente se mantiene explícito como `application-moderno.conf` para evitar confusiones).

## ↩️ Deshacer Cambios

Si por alguna razón esta reestructuración causa problemas bloqueantes y necesitas revertir todo a como estaba:
1. Mueve el contenido de `_LEGACY_BACKUP/` de vuelta a la raíz.
2. `mv _LEGACY_BACKUP/* .`
3. Elimina la carpeta vacía `_LEGACY_BACKUP`.
