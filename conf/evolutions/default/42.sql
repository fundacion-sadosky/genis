# --- !Ups

-- Tabla de tipos de crimen
CREATE TABLE crime_type (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500)
);

-- Tabla de crímenes (asociados a un tipo)
CREATE TABLE crime_involved (
    id VARCHAR(50) PRIMARY KEY,
    crime_type_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT fk_crime_type FOREIGN KEY (crime_type_id) 
        REFERENCES crime_type(id) ON DELETE CASCADE
);

-- Índice para búsquedas por tipo de crimen
CREATE INDEX idx_crime_involved_type ON crime_involved(crime_type_id);

-- Datos iniciales de ejemplo
INSERT INTO crime_type (id, name, description) VALUES 
    ('violent', 'Crímenes Violentos', 'Crímenes que involucran violencia física'),
    ('property', 'Crímenes contra la Propiedad', 'Crímenes relacionados con daños o robo de propiedad'),
    ('sexual', 'Crímenes Sexuales', 'Crímenes de naturaleza sexual'),
    ('other', 'Otros', 'Otros tipos de crímenes');

INSERT INTO crime_involved (id, crime_type_id, name, description) VALUES 
    ('homicide', 'violent', 'Homicidio', 'Muerte causada por otra persona'),
    ('assault', 'violent', 'Agresión', 'Ataque físico a otra persona'),
    ('robbery', 'property', 'Robo', 'Apropiación de bienes ajenos'),
    ('burglary', 'property', 'Allanamiento', 'Ingreso ilegal a propiedad'),
    ('sexual_assault', 'sexual', 'Agresión Sexual', 'Ataque de naturaleza sexual'),
    ('kidnapping', 'violent', 'Secuestro', 'Privación ilegal de libertad');

# --- !Downs

DROP TABLE IF EXISTS crime_involved;
DROP TABLE IF EXISTS crime_type;
