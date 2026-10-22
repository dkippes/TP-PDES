-- Idempotency key of each purchase, shared with the flying-service sale.
ALTER TABLE compra
    ADD COLUMN operacion_id UUID;

-- No purchase flow existed before this migration; backfill defensively so the column can be required.
UPDATE compra SET operacion_id = gen_random_uuid() WHERE operacion_id IS NULL;

ALTER TABLE compra
    ALTER COLUMN operacion_id SET NOT NULL;

ALTER TABLE compra
    ADD CONSTRAINT compra_operacion_id_unique UNIQUE (operacion_id);
