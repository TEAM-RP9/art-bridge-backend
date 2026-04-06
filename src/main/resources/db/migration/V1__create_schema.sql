-- Full schema based on DDL/artbridge_create.sql
-- Uses BIGSERIAL for auto-increment primary keys
-- Makes google_id nullable to support password-only users

-- Table: user
CREATE TABLE "user" (
    id BIGSERIAL NOT NULL,
    google_id varchar(255),
    email varchar(255) NOT NULL,
    username varchar(50) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT user_pk PRIMARY KEY (id)
);

-- Table: profile
CREATE TABLE profile (
    id BIGSERIAL NOT NULL,
    user_id bigint NOT NULL,
    first_name varchar(100) NOT NULL,
    last_name varchar(100) NOT NULL,
    bio text NOT NULL,
    location varchar(255) NOT NULL,
    website varchar NOT NULL,
    profile_image_url text NOT NULL,
    updated_at timestamptz NOT NULL,
    dob date NOT NULL,
    CONSTRAINT profile_pk PRIMARY KEY (id)
);

-- Table: artwork
CREATE TABLE artwork (
    id BIGSERIAL NOT NULL,
    user_id bigint NOT NULL,
    title varchar(255) NOT NULL,
    description text NOT NULL,
    image_url text NOT NULL,
    medium varchar(255) NOT NULL,
    style varchar(255) NOT NULL,
    creation_year smallint NOT NULL,
    views_count bigint NOT NULL DEFAULT 0,
    likes_count bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT artwork_pk PRIMARY KEY (id)
);

CREATE INDEX idx_artworks_filter ON artwork (medium, style, creation_year);
CREATE INDEX idx_artworks_user_id ON artwork (user_id);

-- Foreign keys
ALTER TABLE profile ADD CONSTRAINT profile_user
    FOREIGN KEY (user_id) REFERENCES "user" (id);

ALTER TABLE artwork ADD CONSTRAINT user_artwork
    FOREIGN KEY (user_id) REFERENCES "user" (id);
