CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    family_id UUID NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX refresh_token_family_id_idx ON refresh_token(family_id);
CREATE INDEX refresh_token_usuario_id_idx ON refresh_token(usuario_id);
