-- Created by Redgate Data Modeler (https://datamodeler.redgate-platform.com)
-- Last modification date: 2026-03-08 20:56:17.616

-- tables
-- Table: artwork
CREATE TABLE artwork (
    id int  NOT NULL,
    user_id bigint  NOT NULL,
    title varchar(255)  NOT NULL,
    description text  NOT NULL,
    image_url text  NOT NULL,
    medium varchar(255)  NOT NULL,
    style varchar(255)  NOT NULL,
    creation_year smallint  NOT NULL,
    views_count bigint  NOT NULL DEFAULT 0,
    likes_count bigint  NOT NULL DEFAULT 0,
    created_at timestamp  NOT NULL,
    updated_at timestamp  NOT NULL,
    CONSTRAINT artwork_pk PRIMARY KEY (id)
);

CREATE INDEX idx_artworks_filter on artwork (medium ASC,style ASC,creation_year ASC);

CREATE INDEX idx_artworks_user_id on artwork (user_id ASC);

-- Table: profile
CREATE TABLE profile (
    id bigint  NOT NULL,
    user_id bigserial  NOT NULL,
    first_name varchar(100)  NOT NULL,
    last_name varchar(100)  NOT NULL,
    bio text  NOT NULL,
    location varchar(255)  NOT NULL,
    website varchar  NOT NULL,
    profile_image_url text  NOT NULL,
    updated_at timestamp  NOT NULL,
    dob int  NOT NULL,
    CONSTRAINT profile_pk PRIMARY KEY (id)
);

-- Table: user
CREATE TABLE "user" (
    id bigint  NOT NULL,
    google_id varchar(255)  NOT NULL,
    email varchar(255)  NOT NULL,
    username varchar(50)  NOT NULL,
    created_at timestamp  NOT NULL,
    updated_at timestamp  NOT NULL,
    CONSTRAINT id PRIMARY KEY (id)
);

-- foreign keys
-- Reference: profile_user (table: profile)
ALTER TABLE profile ADD CONSTRAINT profile_user
    FOREIGN KEY (user_id)
    REFERENCES "user" (id)  
    NOT DEFERRABLE 
    INITIALLY IMMEDIATE
;

-- Reference: user_artwork (table: artwork)
ALTER TABLE artwork ADD CONSTRAINT user_artwork
    FOREIGN KEY (user_id)
    REFERENCES "user" (id)  
    NOT DEFERRABLE 
    INITIALLY IMMEDIATE
;

-- End of file.

