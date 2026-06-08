ALTER TABLE alertas
    ADD COLUMN IF NOT EXISTS codigo VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS uk_alertas_codigo
    ON alertas (codigo)
    WHERE codigo IS NOT NULL;
