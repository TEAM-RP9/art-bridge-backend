ALTER TABLE "user" ADD COLUMN password_hash VARCHAR(72);
ALTER TABLE "user" ADD CONSTRAINT user_email_unique UNIQUE (email);
