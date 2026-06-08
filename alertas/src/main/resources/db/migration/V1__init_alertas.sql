CREATE TABLE IF NOT EXISTS alertas (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(255) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    detalle VARCHAR(255) NOT NULL,
    codigo VARCHAR(255),
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_lectura TIMESTAMP NULL
);

ALTER TABLE alertas
    ADD COLUMN IF NOT EXISTS codigo VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS uk_alertas_codigo
    ON alertas (codigo)
    WHERE codigo IS NOT NULL;
