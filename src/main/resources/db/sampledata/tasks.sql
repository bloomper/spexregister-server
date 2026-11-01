-- Task categories
INSERT INTO task_category (uuid, name, actor_present, created_by, created_at, version)
VALUES (UUID(), 'Kommitté', 0, 'system', CURRENT_TIME, 0);
SET
    @task_category_kommitte = LAST_INSERT_ID();

INSERT INTO task_category (uuid, name, actor_present, created_by, created_at, version)
VALUES (UUID(), 'Orkester', 0, 'system', CURRENT_TIME, 0);
SET
    @task_category_orkester = LAST_INSERT_ID();

INSERT INTO task_category (uuid, name, actor_present, created_by, created_at, version)
VALUES (UUID(), 'Ensemble', 1, 'system', CURRENT_TIME, 0);
SET
    @task_category_ensemble = LAST_INSERT_ID();

INSERT INTO task_category (uuid, name, actor_present, created_by, created_at, version)
VALUES (UUID(), 'Bandet', 0, 'system', CURRENT_TIME, 0);
SET
    @task_category_bandet = LAST_INSERT_ID();

INSERT INTO task_category (uuid, name, actor_present, created_by, created_at, version)
VALUES (UUID(), 'Symphonin', 0, 'system', CURRENT_TIME, 0);
SET
    @task_category_symphonin = LAST_INSERT_ID();

INSERT INTO task_category (uuid, name, actor_present, created_by, created_at, version)
VALUES (UUID(), 'Annat', 0, 'system', CURRENT_TIME, 0);
SET
    @task_category_annat = LAST_INSERT_ID();

-- Tasks
INSERT INTO task (uuid, name, category_id, created_by, created_at, version)
VALUES (UUID(), 'Affisch', @task_category_annat, 'system', CURRENT_TIME, 0),
       (UUID(), 'Althorn', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Altsaxofon', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Altsaxofon', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Arkitekt', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Arrangör', @task_category_annat, 'system', CURRENT_TIME, 0),
       (UUID(), 'Barchef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Bas', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Bas', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Bas', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Basklarinett', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Bastuba', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Blockflöjt', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Casseur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Communicateur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Cello', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Cello', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Cello', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Cello 1', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Cello 2', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Chanteuse', @task_category_annat, 'system', CURRENT_TIME, 0),
       (UUID(), 'Conducteur', @task_category_annat, 'system', CURRENT_TIME, 0),
       (UUID(), 'Costumeur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Costumeurchef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'CR-chef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Dekoratör', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Designer', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Directeur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Directeursassistent', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Documentateur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Dragspel', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Elbas', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Elfiol', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Elgitarr', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Ensemblist', @task_category_ensemble, 'system', CURRENT_TIME, 0),
       (UUID(), 'Exspextor', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Fagott', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Festdirecteur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Festnisse', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Festroddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Fiol', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Fiol', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Flöjt', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Flöjt', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Fotografiker', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Författare', @task_category_annat, 'system', CURRENT_TIME, 0),
       (UUID(), 'Gitarr', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Gitarr', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Gitarr', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Gückeljoursledare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Horn', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Info-chef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Inspextant', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Inspextor', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Inspextris', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Inspicient', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Inspicientassistent', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Kapellmästare', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Kapellmästare', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Klarinett', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Klarinett', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Klarinett', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Kommittéledamot', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Kommunikationsansvarig', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Konstnärlig ledare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Kontrabas', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Koreograf', @task_category_annat, 'system', CURRENT_TIME, 0),
       (UUID(), 'Livestreamer', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Ljudmästare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Ljusmästare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Ljudroddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Ljusroddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Mandolin', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Mediamästare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Mediaansvarig', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Musikproducent', @task_category_annat, 'system', CURRENT_TIME, 0),
       (UUID(), 'Oboe', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Oboe', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Oboeflöjt', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Okänt', @task_category_annat, 'system', CURRENT_TIME, 0),
       (UUID(), 'Okänt', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Okänt', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Okänt', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Organisateur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Orkester', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'PR-chef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'PR/Info-stab', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'PR-roddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'PR-strateg', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Panblåsare', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Pianör', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Pianör', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Pianör', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Producent', @task_category_annat, 'system', CURRENT_TIME, 0),
       (UUID(), 'Produktionschef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Regisseur', @task_category_annat, 'system', CURRENT_TIME, 0),
       (UUID(), 'Saxofon', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Saxofon', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Saxofon', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Scen', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Scenmästare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Scennisse', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Scenograf', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Scenroddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Sekreterare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Skivkommitté', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Skräddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Slagverk', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Slagverk', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Sopransaxofon', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Spexmästare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Sufflör', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Sömmerska', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Tekniknisse', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Teknikroddare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Tenorsaxofon', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Trombon', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Trombon', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Trummor', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Trummor', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Trummor', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Trumpet', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Trumpet', @task_category_bandet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Trumpet', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Turnéchef', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Turnédirecteur', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Tvärflöjt', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Tvärflöjt', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Upplysare', @task_category_kommitte, 'system', CURRENT_TIME, 0),
       (UUID(), 'Valthorn', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Valthorn', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Viola', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Viola', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Violacello', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Violin', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Violin 1', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Violin 1', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Violin 2', @task_category_orkester, 'system', CURRENT_TIME, 0),
       (UUID(), 'Violin 2', @task_category_symphonin, 'system', CURRENT_TIME, 0),
       (UUID(), 'Vistextförfattare', @task_category_annat, 'system', CURRENT_TIME, 0);
