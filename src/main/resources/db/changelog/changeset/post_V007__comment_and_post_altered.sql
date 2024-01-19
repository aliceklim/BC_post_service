ALTER TABLE comment
ADD COLUMN    verified_date     timestamptz,
ADD COLUMN    verified          boolean;

ALTER TABLE post
ADD COLUMN    corrected         boolean DEFAULT false NOT NULL;
ADD COLUMN    views             bigint SET DEFAULT 0,
ADD COLUMN    verified_date     timestamptz,
ALTER COLUMN  verified          SET DEFAULT false,
ALTER COLUMN  verified          SET NOT NULL;
