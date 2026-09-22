-- Existing buyer-related records cannot be represented after Comprador becomes a JOINED Perfil subtype.
-- This application currently has no user-data preservation requirement.
DELETE FROM valoracion;
DELETE FROM compra;
DELETE FROM usuario;
DELETE FROM comprador;

ALTER TABLE comprador
    DROP COLUMN perfil_id;

ALTER TABLE comprador
    ALTER COLUMN id DROP IDENTITY IF EXISTS;

ALTER TABLE comprador
    ADD CONSTRAINT comprador_id_fkey FOREIGN KEY (id) REFERENCES perfil(id);

ALTER TABLE usuario
    ADD COLUMN password_hash VARCHAR(255) NOT NULL;

ALTER TABLE usuario
    ADD CONSTRAINT usuario_correo_normalized CHECK (correo = lower(correo));
