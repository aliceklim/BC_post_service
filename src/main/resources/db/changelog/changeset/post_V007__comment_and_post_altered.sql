ALTER TABLE comment
ADD COLUMN    verified_date     timestamptz,
ADD COLUMN    verified          boolean DEFAULT false NOT NULL;

ALTER TABLE post
ADD COLUMN    corrected         boolean DEFAULT false NOT NULL,
ADD COLUMN    views             bigint DEFAULT 0,
ADD COLUMN    verified_date     timestamptz,
ADD COLUMN    verified          boolean DEFAULT false NOT NULL;

