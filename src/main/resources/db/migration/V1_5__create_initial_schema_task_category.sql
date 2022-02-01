CREATE TABLE IF NOT EXISTS task_category
(
    id        BIGINT AUTO_INCREMENT NOT NULL,
    name      VARCHAR(255)          NOT NULL,
    has_actor BIT(1)                NULL,
    CONSTRAINT pk_task_category PRIMARY KEY (id)
);

CREATE INDEX IDX_TASK_CATEGORY_ON_NAME ON task_category (name);