-- Task categories
INSERT INTO task_category (name, actor_present, created_by, created_at, version)
VALUES ('Kommitté', 0, 'system', CURRENT_TIME, 0);
SET
@task_category_kommitte = LAST_INSERT_ID();

INSERT INTO task_category (name, actor_present, created_by, created_at, version)
VALUES ('Orkester', 0, 'system', CURRENT_TIME, 0);
SET
@task_category_orkester = LAST_INSERT_ID();

INSERT INTO task_category (name, actor_present, created_by, created_at, version)
VALUES ('Ensemble', 1, 'system', CURRENT_TIME, 0);
SET
@task_category_ensemble = LAST_INSERT_ID();

INSERT INTO task_category (name, actor_present, created_by, created_at, version)
VALUES ('Bandet', 0, 'system', CURRENT_TIME, 0);
SET
@task_category_bandet = LAST_INSERT_ID();

INSERT INTO task_category (name, actor_present, created_by, created_at, version)
VALUES ('Symphonin', 0, 'system', CURRENT_TIME, 0);
SET
@task_category_symphonin = LAST_INSERT_ID();

INSERT INTO task_category (name, actor_present, created_by, created_at, version)
VALUES ('Annat', 0, 'system', CURRENT_TIME, 0);
SET
@task_category_annat = LAST_INSERT_ID();

-- Tasks
INSERT INTO task (name, category_id, created_by, created_at, version)
VALUES ('Affisch', @task_category_annat, 'system', CURRENT_TIME, 0),
       ('Altsaxofon', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Altsaxofon', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Arkitekt', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Arrangör', @task_category_annat, 'system', CURRENT_TIME, 0),
       ('Bas', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Bas', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Bas', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Basklarinett', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Bastuba', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Blockflöjt', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Casseur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Communicateur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Cello', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Cello', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Cello 1', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Cello 2', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Chanteuse', @task_category_annat, 'system', CURRENT_TIME, 0),
       ('Conducteur', @task_category_annat, 'system', CURRENT_TIME, 0),
       ('Costumeur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Costumeurchef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('CR-chef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Dekoratör', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Designer', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Directeur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Directeursassistent', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Documentateur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Dragspel', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Elfiol', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Ensemblist', @task_category_ensemble, 'system', CURRENT_TIME, 0),
       ('Exspextor', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Fagott', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Festdirecteur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Festnisse', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Festroddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Fiol', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Flöjt', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Flöjt', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Fotografiker', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Författare', @task_category_annat, 'system', CURRENT_TIME, 0),
       ('Gitarr', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Gitarr', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Gitarr', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Horn', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Info-chef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Inspextant', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Inspextor', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Inspextris', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Inspicient', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Inspicientassistent', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Kapellmästare', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Klarinett', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Klarinett', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Klarinett', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Kommittéledamot', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Kommunikationsansvarig', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Konstnärlig ledare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Kontrabas', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Koreograf', @task_category_annat, 'system', CURRENT_TIME, 0),
       ('Livestreamer', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Ljudmästare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Ljusmästare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Ljudroddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Ljusroddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Mandolin', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Mediamästare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Mediaansvarig', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Musikproducent', @task_category_annat, 'system', CURRENT_TIME, 0),
       ('Oboe', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Oboe', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Oboeflöjt', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Okänt', @task_category_annat, 'system', CURRENT_TIME, 0),
       ('Okänt', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Okänt', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Okänt', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Organisateur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Orkester', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('PR-chef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('PR/Info-stab', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('PR-roddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Panblåsare', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Pianör', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Pianör', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Pianör', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Producent', @task_category_annat, 'system', CURRENT_TIME, 0),
       ('Regisseur', @task_category_annat, 'system', CURRENT_TIME, 0),
       ('Saxofon', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Saxofon', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Scen', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Scenmästare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Scennisse', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Scenograf', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Scenroddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Sekreterare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Skivkommitté', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Skräddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Slagverk', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Sopransaxofon', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Spexmästare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Sufflör', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Sömmerska', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Tekniknisse', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Teknikroddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Tenorsaxofon', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Trombon', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Trombon', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Trummor', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Trummor', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Trummor', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Trumpet', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Trumpet', @task_category_bandet, 'system', CURRENT_TIME, 0),
       ('Trumpet', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Turnéchef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Tvärflöjt', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Tvärflöjt', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Upplysare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       ('Valthorn', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Valthorn', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Viola', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Violacello', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Violin', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Violin 1', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Violin 1', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Violin 2', @task_category_orkester, 'system', CURRENT_TIME, 0),
       ('Violin 2', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       ('Vistextförfattare', @task_category_annat, 'system', CURRENT_TIME, 0);
