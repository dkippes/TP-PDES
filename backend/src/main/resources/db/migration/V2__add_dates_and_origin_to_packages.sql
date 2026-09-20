ALTER TABLE paquete
    ADD COLUMN origen VARCHAR(255),
    ADD COLUMN fecha_inicial DATE,
    ADD COLUMN fecha_final DATE;

ALTER TABLE compra
    ADD COLUMN fecha DATE;

ALTER TABLE paquete
    ALTER COLUMN origen SET NOT NULL,
    ALTER COLUMN fecha_inicial SET NOT NULL,
    ALTER COLUMN fecha_final SET NOT NULL;

ALTER TABLE compra
    ALTER COLUMN fecha SET NOT NULL;
