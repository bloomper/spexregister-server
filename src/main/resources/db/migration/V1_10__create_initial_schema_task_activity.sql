CREATE TABLE IF NOT EXISTS task_activity
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_by       VARCHAR(50)           NOT NULL,
    created_at       DATETIME              NOT NULL,
    last_modified_by VARCHAR(50)           NULL,
    last_modified_at DATETIME              NULL,
    activity_id      BIGINT                NOT NULL,
    task_id          BIGINT                NOT NULL,
    CONSTRAINT PK_TASK_ACTIVITY PRIMARY KEY (id)
);

ALTER TABLE task_activity
    ADD CONSTRAINT FK_TASK_ACTIVITY_ON_ACTIVITY FOREIGN KEY (activity_id) REFERENCES activity (id);

ALTER TABLE task_activity
    ADD CONSTRAINT FK_TASK_ACTIVITY_ON_TASK FOREIGN KEY (task_id) REFERENCES task (id);
