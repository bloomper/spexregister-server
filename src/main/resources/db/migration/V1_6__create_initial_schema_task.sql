CREATE TABLE IF NOT EXISTS task
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_by       VARCHAR(50)           NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(50)           NULL,
    last_modified_at DATETIME              NULL,
    name             VARCHAR(255)          NOT NULL,
    category_id      BIGINT                NOT NULL,
    CONSTRAINT pk_task PRIMARY KEY (id)
);

ALTER TABLE task
    ADD CONSTRAINT FK_TASK_ON_CATEGORY FOREIGN KEY (category_id) REFERENCES task_category (id);

CREATE INDEX IDX_TASK_ON_NAME ON task (name);
