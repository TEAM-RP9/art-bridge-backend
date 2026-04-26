CREATE TABLE media (
    id              BIGSERIAL PRIMARY KEY,
    object_key      TEXT NOT NULL UNIQUE,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    owner_user_id   BIGINT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_media_owner_user ON media(owner_user_id);