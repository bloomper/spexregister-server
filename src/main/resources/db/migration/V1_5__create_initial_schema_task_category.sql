CREATE TABLE IF NOT EXISTS task_category
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    name              VARCHAR(255)          NOT NULL,
    has_actor         BIT                   NULL,
    created_by        VARCHAR(255)          NOT NULL,
    created_at        DATETIME              NOT NULL,
    last_modified_by  VARCHAR(255)          NULL,
    last_modified_at  DATETIME              NULL,
    CONSTRAINT PK_TASK_CATEGORY PRIMARY KEY (id)
);

CREATE INDEX IDX_TASK_CATEGORY_ON_NAME ON task_category (name);