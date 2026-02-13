# PostgreSQL 16.6 - Docker Compose

Configuración de PostgreSQL 16.6 con pgAdmin para el proyecto GENis.

## Características

- **PostgreSQL 16.6 Alpine**: Versión LTS estable y moderna
- **pgAdmin 4**: Interfaz web para administración de base de datos
- **Health Check**: Monitoreo de salud del servicio PostgreSQL
- **Network Bridge**: Red dedicada para comunicación entre servicios

## Credenciales por defecto

### PostgreSQL
- **Usuario**: genis
- **Password**: genis123
- **Database**: genis
- **Puerto**: 5433 (mapeado al 5432 interno)

### pgAdmin
- **Email**: admin@genis.local
- **Password**: admin
- **URL**: http://localhost:5050

## Uso

### Iniciar servicios

```bash
cd utils/docker/pgsql16
docker-compose up -d
```

### Detener servicios

```bash
docker-compose down
```

### Ver logs

```bash
docker-compose logs -f postgres
docker-compose logs -f pgadmin
```

### Resetear datos (⚠️ Elimina todos los datos)

```bash
docker-compose down -v
rm -rf data/*
```

## Conexión desde la aplicación

```conf
# application.conf
db.default.url = "jdbc:postgresql://localhost:5433/genis"
db.default.driver = "org.postgresql.Driver"
db.default.username = "genis"
db.default.password = "genis123"
```

## Acceso a pgAdmin

1. Abrir http://localhost:5050
2. Login con las credenciales de pgAdmin
3. Agregar servidor:
   - **Name**: GENis PostgreSQL
   - **Host**: postgres (nombre del servicio en la red Docker)
   - **Port**: 5432
   - **Username**: genis
   - **Password**: genis123

## Notas

- El puerto **5433** se usa para evitar conflictos con instalaciones locales de PostgreSQL
- Los datos se persisten en la carpeta `./data`
- El contenedor incluye health check para verificar disponibilidad
- Se usa Alpine Linux para reducir el tamaño de la imagen
