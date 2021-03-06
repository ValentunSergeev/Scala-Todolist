CREATE TABLE USERS
(
    ID        BIGSERIAL PRIMARY KEY,
    USER_NAME VARCHAR NOT NULL UNIQUE,
    EMAIL     VARCHAR NOT NULL,
    HASH      VARCHAR NOT NULL,
    ROLE      VARCHAR NOT NULL DEFAULT 'Client'
);

CREATE TABLE TODOS
(
    ID       BIGSERIAL PRIMARY KEY,
    NAME     VARCHAR NOT NULL,
    CONTENT  VARCHAR NOT NULL,
    PRIORITY VARCHAR NOT NULL,
    USER_ID  BIGINT  NOT NULL REFERENCES USERS (ID) ON DELETE CASCADE
);

CREATE TABLE JWT
(
    ID           VARCHAR PRIMARY KEY,
    JWT          VARCHAR   NOT NULL,
    IDENTITY     BIGINT    NOT NULL REFERENCES USERS (ID) ON DELETE CASCADE,
    EXPIRY       TIMESTAMP NOT NULL,
    LAST_TOUCHED TIMESTAMP
);
