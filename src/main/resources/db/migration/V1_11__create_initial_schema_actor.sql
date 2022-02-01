CREATE TABLE IF NOT EXISTS actor
(
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    created_by         VARCHAR(50)           NOT NULL,
    created_date       BIGINT                NOT NULL,
    last_modified_by   VARCHAR(50)           NULL,
    last_modified_date BIGINT                NULL,
    `role`             VARCHAR(255)          NULL,
    vocal              VARCHAR(255)          NULL,
    task_activity_id   BIGINT                NULL,
    CONSTRAINT pk_actor PRIMARY KEY (id)
);

ALTER TABLE actor
    ADD CONSTRAINT FK_ACTOR_ON_TASK_ACTIVITY FOREIGN KEY (task_activity_id) REFERENCES task_activity (id);
