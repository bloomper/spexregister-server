CREATE TABLE IF NOT EXISTS actor
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_by       VARCHAR(50)           NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(50)           NULL,
    last_modified_at DATETIME              NULL,
    role             VARCHAR(255)          NULL,
    vocal_id         VARCHAR(255)          NOT NULL,
    task_activity_id BIGINT                NULL,
    CONSTRAINT PK_ACTOR PRIMARY KEY (id)
);

ALTER TABLE actor
    ADD CONSTRAINT FK_ACTOR_ON_TASK_ACTIVITY FOREIGN KEY (task_activity_id) REFERENCES task_activity (id);

ALTER TABLE actor
    ADD CONSTRAINT FK_ACTOR_ON_TYPE FOREIGN KEY (vocal_id) REFERENCES type (id);
