CREATE TABLE role (
    id BIGSERIAL NOT NULL,
    name varchar(32) NOT NULL,
    CONSTRAINT role_pk PRIMARY KEY (id),
    CONSTRAINT role_name_unique UNIQUE (name)
);

INSERT INTO role (name) VALUES ('USER'), ('ADMIN'), ('ARTIST');

ALTER TABLE "user"
    ADD COLUMN role_id bigint;

UPDATE "user" SET role_id = (SELECT id FROM role WHERE name = 'USER');

ALTER TABLE "user"
    ALTER COLUMN role_id SET NOT NULL;

ALTER TABLE "user" ADD CONSTRAINT user_role
    FOREIGN KEY (role_id) REFERENCES role (id);