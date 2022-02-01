CREATE TABLE IF NOT EXISTS task_activity
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    task_id     BIGINT                NOT NULL,
    activity_id BIGINT                NULL,
    CONSTRAINT pk_task_activity PRIMARY KEY (id)
);

ALTER TABLE task_activity
    ADD CONSTRAINT FK_TASK_ACTIVITY_ON_ACTIVITY FOREIGN KEY (activity_id) REFERENCES activity (id);

ALTER TABLE task_activity
    ADD CONSTRAINT FK_TASK_ACTIVITY_ON_TASK FOREIGN KEY (task_id) REFERENCES task (id);
