#!/bin/bash

# =============================================================================
# Script de Setup para GENis 6.0 (Rama de-cero)
# =============================================================================

set -e

echo "╔════════════════════════════════════════════════════════╗"
echo "║        GENis 6.0 - Setup Entorno de Desarrollo        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variables
JAVA_VERSION_REQUIRED="11"
SBT_VERSION_REQUIRED="1.9"
POSTGRES_PORT=5432
LDAP_PORT=389

# ============================================================================
# FUNCIONES AUXILIARES
# ============================================================================

print_step() {
    echo -e "${GREEN}[PASO $1]${NC} $2"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[ADVERTENCIA]${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

# ============================================================================
# VERIFICACIONES PREVIAS
# ============================================================================

print_step "1" "Verificando prerequisitos..."

# Verificar Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[^."]*' | head -1)
    if [ "$JAVA_VERSION" -ge "$JAVA_VERSION_REQUIRED" ]; then
        print_success "Java $JAVA_VERSION encontrado"
    else
        print_error "Java $JAVA_VERSION_REQUIRED o superior es requerido. Encontrado: $JAVA_VERSION"
        exit 1
    fi
else
    print_error "Java no está instalado"
    exit 1
fi

# Verificar SBT
if command -v sbt &> /dev/null; then
    print_success "SBT encontrado"
else
    print_warning "SBT no está en PATH. Se usará version declarada en build.properties"
fi

# Verificar PostgreSQL
if command -v psql &> /dev/null; then
    PSQL_VERSION=$(psql --version | awk '{print $3}')
    print_success "PostgreSQL $PSQL_VERSION encontrado"
else
    print_warning "PostgreSQL CLI no está instalado, pero puede estar corriendo como servicio"
fi

# ============================================================================
# CONFIGURACIÓN DE VARIABLES DE ENTORNO
# ============================================================================

print_step "2" "Creando archivo .env..."

if [ -f ".env" ]; then
    print_warning ".env ya existe. Creando backup..."
    cp .env .env.backup
fi

cat > .env << 'EOF'
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/genisdb
DATABASE_USER=genissqladmin
DATABASE_PASSWORD=genisadmin

# Log Database  
LOG_DATABASE_URL=jdbc:postgresql://localhost:5432/genislogdb
LOG_DATABASE_USER=genissqladmin
LOG_DATABASE_PASSWORD=genisadmin

# LDAP
LDAP_ENABLED=true
LDAP_URL=ldap://localhost:389
LDAP_BASE_DN=dc=genis,dc=local
LDAP_ADMIN_DN=cn=admin,dc=genis,dc=local
LDAP_ADMIN_PASSWORD=admin

# JWT
JWT_SECRET=CAMBIAR_A_VALOR_SEGURO_128_CARACTERES_MINIMO_PARA_PRODUCCION
JWT_EXPIRATION=86400

# Aplicación
APP_ENV=development
APP_PORT=9000
EOF

print_success "Archivo .env creado"

# ============================================================================
# SETUP DE BASE DE DATOS
# ============================================================================

print_step "3" "Configurando PostgreSQL..."

# Crear usuario y bases de datos
PSQL_SETUP=$(cat <<EOF
-- Crear usuario si no existe
DO \$\$ BEGIN
  CREATE ROLE genissqladmin WITH LOGIN PASSWORD 'genisadmin' CREATEDB;
EXCEPTION WHEN OTHERS THEN
  NULL; -- Ignorar si ya existe
END \$\$;

-- Crear bases de datos
CREATE DATABASE IF NOT EXISTS genisdb OWNER genissqladmin;
CREATE DATABASE IF NOT EXISTS genislogdb OWNER genissqladmin;

-- Privilegios
GRANT ALL PRIVILEGES ON DATABASE genisdb TO genissqladmin;
GRANT ALL PRIVILEGES ON DATABASE genislogdb TO genissqladmin;
EOF
)

if sudo -u postgres psql -c "$PSQL_SETUP" 2>/dev/null; then
    print_success "PostgreSQL configurado"
else
    print_warning "No se pudo configurar PostgreSQL. Verificar permisos de sudo"
fi

# ============================================================================
# SETUP DE LDAP
# ============================================================================

print_step "4" "Verificando LDAP..."

if systemctl is-active --quiet slapd; then
    print_success "OpenLDAP (slapd) está corriendo"
else
    print_warning "OpenLDAP (slapd) no está corriendo"
    echo "  Para iniciarlo: sudo systemctl start slapd"
fi

# ============================================================================
# COMPILACIÓN
# ============================================================================

print_step "5" "Compilando proyecto..."

sbt clean 2>&1 | grep -E "(clean|Compiling)" || true
if sbt compile 2>&1 | tail -5 | grep -q "success"; then
    print_success "Compilación exitosa"
else
    print_warning "Hubo problemas en la compilación. Revisar logs"
fi

# ============================================================================
# INFORMACIÓN FINAL
# ============================================================================

echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║            Setup completado exitosamente!             ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

echo "📝 Próximos pasos:"
echo ""
echo "1. Revisar archivo .env y actualizar valores si es necesario"
echo ""
echo "2. Iniciar FerretDB (si lo usarás):"
echo "   docker run -d -p 27017:27017 \\"
echo "     -e FERRETDB_POSTGRESQL_URL=\"postgres://genissqladmin:genisadmin@localhost/ferretdb\" \\"
echo "     ghcr.io/ferretdb/ferretdb:latest"
echo ""
echo "3. Ejecutar la aplicación:"
echo "   sbt run -Dconfig.file=./conf/application-moderno.conf"
echo ""
echo "4. Acceder a:"
echo "   http://localhost:9000"
echo ""
echo "5. Probar autenticación:"
echo "   curl -X POST http://localhost:9000/api/auth/login \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"username\": \"usuario\", \"password\": \"contraseña\"}'"
echo ""

print_success "¡Listo para comenzar!"
