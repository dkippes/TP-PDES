-- Las valoraciones historicas conservan sus relaciones y no se asignan a compras arbitrarias.
ALTER TABLE valoracion ADD COLUMN compra_id BIGINT REFERENCES compra(id);
ALTER TABLE valoracion ADD CONSTRAINT uk_valoracion_compra UNIQUE (compra_id);
ALTER TABLE valoracion ADD COLUMN fecha TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE valoracion ALTER COLUMN comentario DROP NOT NULL;
ALTER TABLE valoracion ADD CONSTRAINT ck_valoracion_puntaje CHECK (puntaje BETWEEN 0 AND 10);
CREATE INDEX idx_valoracion_paquete_fecha ON valoracion (paquete_id, fecha DESC, id DESC);
