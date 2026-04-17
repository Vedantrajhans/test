CREATE TABLE producers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE promoters (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
);

INSERT INTO producers (name, email)
VALUES
    ('Live Nation', 'contact@livenation.com'),
    ('AEG Presents', 'hello@aegpresents.com');

INSERT INTO promoters (name, email)
VALUES
    ('John Doe Promotions', 'john@promotions.com'),
    ('City Events', 'events@city.com');
