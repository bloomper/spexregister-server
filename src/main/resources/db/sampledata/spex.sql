-- Spex categories
INSERT INTO spex_category (name, first_year, created_by, created_at, version)
VALUES ('Chalmersspexet', '1948', 'system', CURRENT_TIME, 0);
SET
@spex_category_chalmersspexet = LAST_INSERT_ID();

INSERT INTO spex_category (name, first_year, created_by, created_at, version)
VALUES ('Bobspexet', '2003', 'system', CURRENT_TIME, 0);
SET
@spex_category_bobspexet = LAST_INSERT_ID();

INSERT INTO spex_category (name, first_year, created_by, created_at, version)
VALUES ('Veraspexet', '2003', 'system', CURRENT_TIME, 0);
SET
@spex_category_veraspexet = LAST_INSERT_ID();

INSERT INTO spex_category (name, first_year, created_by, created_at, version)
VALUES ('Jubileumsspex', '1948', 'system', CURRENT_TIME, 0);
SET
@spex_category_jubileumsspex = LAST_INSERT_ID();

-- Spex details
INSERT INTO spex_details (title, category_id, created_by, created_at, version)
VALUES ('Bojan', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Erik XIV', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Cæsarion', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Scheherazade', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Anna', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Henrik 8', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Gustav E:son Vasa', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Napoleon', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Statyerna', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Lucrezia', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Katarina II', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Starke August', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Klodvig', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Don Pedro', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Charles II', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Nebukadnessar', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Sven Duva', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Montezuma', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Alexander', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Richard III', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Margareta', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('George Washington', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Noak', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Turandot', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Fredrik den Store', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Sherlock Holmes', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Lionardo da Vinci', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Ludvig XIV', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Nils Dacke', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Dr Livingstone', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Nero', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Tutankhamon', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Ludwig van Beethoven', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('John Ericsson', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Filip II', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Lasse-Maja', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Olof Skötkonung', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Victoria', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Montgomery', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Svartskägg', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Christina', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Klondike', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Gutenberg', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Krösus', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Stradivarius', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Ivan den förskräcklige', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Snorre', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Nobel', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Ali Baba', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Sköna Hélena', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Nostradamus', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Mose', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Marco Polo', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Dracula', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Carl von Linné', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Aristoteles', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('H. C. Andersen', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Elisabeth I', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),
       ('Gauss', @spex_category_chalmersspexet, 'system', CURRENT_TIME, 0),

       ('Gagarin', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Heliga Birgitta', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Gustav II Adolf', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Casanova', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Hannibal', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Picasso', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Newton', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Kristian Tyrann', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Marie Curie', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Christofer Columbus', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Bellman', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Herakles', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Rasputin', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Kleopatra', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Magnus Ladulås', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Geronimo', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       ('Marie Antoinette', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),

       ('Mata Hari', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Phileas Fogg', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Arthur', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Amelia Earhart', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Frankenstein', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Wyatt Earp & Doc Holiday', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Taj Mahal', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Lucia', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Karl XII', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Bröderna Lumière', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Zheng', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Jeanne d''Arc', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Anne Bonny', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Al Capone', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Michelangelo', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Ada Lovelace', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       ('Karin Månsdotter', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),

       ('25-årsjubileet', @spex_category_jubileumsspex, 'system', CURRENT_TIME, 0),
       ('Knappt ett Chalmersspex', @spex_category_jubileumsspex, 'system', CURRENT_TIME, 0),
       ('50-årskavalkad', @spex_category_jubileumsspex, 'system', CURRENT_TIME, 0),
       ('Tender Bar', @spex_category_jubileumsspex, 'system', CURRENT_TIME, 0),
       ('75-årsjubileum', @spex_category_jubileumsspex, 'system', CURRENT_TIME, 0);

-- Spex
INSERT INTO spex (year, details_id, created_by, created_at, version)
VALUES ('1948', (SELECT id FROM spex_details WHERE title = 'Bojan'), 'system', CURRENT_TIME, 0),
       ('1949', (SELECT id FROM spex_details WHERE title = 'Erik XIV'), 'system', CURRENT_TIME, 0),
       ('1950', (SELECT id FROM spex_details WHERE title = 'Cæsarion'), 'system', CURRENT_TIME, 0),
       ('1951', (SELECT id FROM spex_details WHERE title = 'Scheherazade'), 'system', CURRENT_TIME, 0),
       ('1952', (SELECT id FROM spex_details WHERE title = 'Anna'), 'system', CURRENT_TIME, 0),
       ('1953', (SELECT id FROM spex_details WHERE title = 'Cæsarion'), 'system', CURRENT_TIME, 0),
       ('1954', (SELECT id FROM spex_details WHERE title = 'Henrik 8'), 'system', CURRENT_TIME, 0),
       ('1955', (SELECT id FROM spex_details WHERE title = 'Gustav E:son Vasa'), 'system', CURRENT_TIME, 0),
       ('1956', (SELECT id FROM spex_details WHERE title = 'Napoleon'), 'system', CURRENT_TIME, 0),
       ('1957', (SELECT id FROM spex_details WHERE title = 'Statyerna'), 'system', CURRENT_TIME, 0),
       ('1958', (SELECT id FROM spex_details WHERE title = 'Lucrezia'), 'system', CURRENT_TIME, 0),
       ('1959', (SELECT id FROM spex_details WHERE title = 'Katarina II'), 'system', CURRENT_TIME, 0),
       ('1960', (SELECT id FROM spex_details WHERE title = 'Starke August'), 'system', CURRENT_TIME, 0),
       ('1961', (SELECT id FROM spex_details WHERE title = 'Klodvig'), 'system', CURRENT_TIME, 0),
       ('1962', (SELECT id FROM spex_details WHERE title = 'Don Pedro'), 'system', CURRENT_TIME, 0),
       ('1963', (SELECT id FROM spex_details WHERE title = 'Charles II'), 'system', CURRENT_TIME, 0),
       ('1964', (SELECT id FROM spex_details WHERE title = 'Nebukadnessar'), 'system', CURRENT_TIME, 0),
       ('1965', (SELECT id FROM spex_details WHERE title = 'Sven Duva'), 'system', CURRENT_TIME, 0),
       ('1966', (SELECT id FROM spex_details WHERE title = 'Montezuma'), 'system', CURRENT_TIME, 0),
       ('1967', (SELECT id FROM spex_details WHERE title = 'Alexander'), 'system', CURRENT_TIME, 0),
       ('1968', (SELECT id FROM spex_details WHERE title = 'Richard III'), 'system', CURRENT_TIME, 0),
       ('1969', (SELECT id FROM spex_details WHERE title = 'Margareta'), 'system', CURRENT_TIME, 0),
       ('1970', (SELECT id FROM spex_details WHERE title = 'George Washington'), 'system', CURRENT_TIME, 0),
       ('1971', (SELECT id FROM spex_details WHERE title = 'Noak'), 'system', CURRENT_TIME, 0),
       ('1972', (SELECT id FROM spex_details WHERE title = 'Turandot'), 'system', CURRENT_TIME, 0),
       ('1973', (SELECT id FROM spex_details WHERE title = 'Fredrik den Store'), 'system', CURRENT_TIME, 0),
       ('1974', (SELECT id FROM spex_details WHERE title = 'Sherlock Holmes'), 'system', CURRENT_TIME, 0),
       ('1975', (SELECT id FROM spex_details WHERE title = 'Lionardo da Vinci'), 'system', CURRENT_TIME, 0),
       ('1976', (SELECT id FROM spex_details WHERE title = 'Ludvig XIV'), 'system', CURRENT_TIME, 0),
       ('1977', (SELECT id FROM spex_details WHERE title = 'Nils Dacke'), 'system', CURRENT_TIME, 0),
       ('1978', (SELECT id FROM spex_details WHERE title = 'Dr Livingstone'), 'system', CURRENT_TIME, 0),
       ('1979', (SELECT id FROM spex_details WHERE title = 'Nero'), 'system', CURRENT_TIME, 0),
       ('1980', (SELECT id FROM spex_details WHERE title = 'Tutankhamon'), 'system', CURRENT_TIME, 0),
       ('1981', (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven'), 'system', CURRENT_TIME, 0),
       ('1982', (SELECT id FROM spex_details WHERE title = 'John Ericsson'), 'system', CURRENT_TIME, 0),
       ('1983', (SELECT id FROM spex_details WHERE title = 'Filip II'), 'system', CURRENT_TIME, 0),
       ('1984', (SELECT id FROM spex_details WHERE title = 'Lasse-Maja'), 'system', CURRENT_TIME, 0),
       ('1985', (SELECT id FROM spex_details WHERE title = 'Olof Skötkonung'), 'system', CURRENT_TIME, 0),
       ('1986', (SELECT id FROM spex_details WHERE title = 'Victoria'), 'system', CURRENT_TIME, 0),
       ('1987', (SELECT id FROM spex_details WHERE title = 'Montgomery'), 'system', CURRENT_TIME, 0),
       ('1988', (SELECT id FROM spex_details WHERE title = 'Svartskägg'), 'system', CURRENT_TIME, 0),
       ('1989', (SELECT id FROM spex_details WHERE title = 'Christina'), 'system', CURRENT_TIME, 0),
       ('1990', (SELECT id FROM spex_details WHERE title = 'Klondike'), 'system', CURRENT_TIME, 0),
       ('1991', (SELECT id FROM spex_details WHERE title = 'Gutenberg'), 'system', CURRENT_TIME, 0),
       ('1992', (SELECT id FROM spex_details WHERE title = 'Krösus'), 'system', CURRENT_TIME, 0),
       ('1993', (SELECT id FROM spex_details WHERE title = 'Stradivarius'), 'system', CURRENT_TIME, 0),
       ('1994', (SELECT id FROM spex_details WHERE title = 'Ivan den förskräcklige'), 'system', CURRENT_TIME, 0),
       ('1995', (SELECT id FROM spex_details WHERE title = 'Snorre'), 'system', CURRENT_TIME, 0),
       ('1996', (SELECT id FROM spex_details WHERE title = 'Nobel'), 'system', CURRENT_TIME, 0),
       ('1997', (SELECT id FROM spex_details WHERE title = 'Ali Baba'), 'system', CURRENT_TIME, 0),
       ('1998', (SELECT id FROM spex_details WHERE title = 'Sköna Hélena'), 'system', CURRENT_TIME, 0),
       ('1999', (SELECT id FROM spex_details WHERE title = 'Nostradamus'), 'system', CURRENT_TIME, 0),
       ('2000', (SELECT id FROM spex_details WHERE title = 'Mose'), 'system', CURRENT_TIME, 0),
       ('2001', (SELECT id FROM spex_details WHERE title = 'Marco Polo'), 'system', CURRENT_TIME, 0),
       ('2002', (SELECT id FROM spex_details WHERE title = 'Dracula'), 'system', CURRENT_TIME, 0),
       ('2020', (SELECT id FROM spex_details WHERE title = 'Carl von Linné'), 'system', CURRENT_TIME, 0),
       ('2021', (SELECT id FROM spex_details WHERE title = 'Aristoteles'), 'system', CURRENT_TIME, 0),
       ('2022', (SELECT id FROM spex_details WHERE title = 'H. C. Andersen'), 'system', CURRENT_TIME, 0),
       ('2023', (SELECT id FROM spex_details WHERE title = 'Elisabeth I'), 'system', CURRENT_TIME, 0),
       ('2024', (SELECT id FROM spex_details WHERE title = 'Gauss'), 'system', CURRENT_TIME, 0),

       ('2003', (SELECT id FROM spex_details WHERE title = 'Gagarin'), 'system', CURRENT_TIME, 0),
       ('2004', (SELECT id FROM spex_details WHERE title = 'Heliga Birgitta'), 'system', CURRENT_TIME, 0),
       ('2005', (SELECT id FROM spex_details WHERE title = 'Gustav II Adolf'), 'system', CURRENT_TIME, 0),
       ('2006', (SELECT id FROM spex_details WHERE title = 'Casanova'), 'system', CURRENT_TIME, 0),
       ('2007', (SELECT id FROM spex_details WHERE title = 'Hannibal'), 'system', CURRENT_TIME, 0),
       ('2008', (SELECT id FROM spex_details WHERE title = 'Picasso'), 'system', CURRENT_TIME, 0),
       ('2009', (SELECT id FROM spex_details WHERE title = 'Newton'), 'system', CURRENT_TIME, 0),
       ('2010', (SELECT id FROM spex_details WHERE title = 'Kristian Tyrann'), 'system', CURRENT_TIME, 0),
       ('2011', (SELECT id FROM spex_details WHERE title = 'Marie Curie'), 'system', CURRENT_TIME, 0),
       ('2012', (SELECT id FROM spex_details WHERE title = 'Christofer Columbus'), 'system', CURRENT_TIME, 0),
       ('2013', (SELECT id FROM spex_details WHERE title = 'Bellman'), 'system', CURRENT_TIME, 0),
       ('2014', (SELECT id FROM spex_details WHERE title = 'Herakles'), 'system', CURRENT_TIME, 0),
       ('2015', (SELECT id FROM spex_details WHERE title = 'Rasputin'), 'system', CURRENT_TIME, 0),
       ('2016', (SELECT id FROM spex_details WHERE title = 'Kleopatra'), 'system', CURRENT_TIME, 0),
       ('2017', (SELECT id FROM spex_details WHERE title = 'Magnus Ladulås'), 'system', CURRENT_TIME, 0),
       ('2018', (SELECT id FROM spex_details WHERE title = 'Geronimo'), 'system', CURRENT_TIME, 0),
       ('2019', (SELECT id FROM spex_details WHERE title = 'Marie Antoinette'), 'system', CURRENT_TIME, 0),

       ('2003', (SELECT id FROM spex_details WHERE title = 'Mata Hari'), 'system', CURRENT_TIME, 0),
       ('2004', (SELECT id FROM spex_details WHERE title = 'Phileas Fogg'), 'system', CURRENT_TIME, 0),
       ('2005', (SELECT id FROM spex_details WHERE title = 'Arthur'), 'system', CURRENT_TIME, 0),
       ('2006', (SELECT id FROM spex_details WHERE title = 'Amelia Earhart'), 'system', CURRENT_TIME, 0),
       ('2007', (SELECT id FROM spex_details WHERE title = 'Frankenstein'), 'system', CURRENT_TIME, 0),
       ('2008', (SELECT id FROM spex_details WHERE title = 'Wyatt Earp & Doc Holiday'), 'system', CURRENT_TIME, 0),
       ('2009', (SELECT id FROM spex_details WHERE title = 'Taj Mahal'), 'system', CURRENT_TIME, 0),
       ('2010', (SELECT id FROM spex_details WHERE title = 'Lucia'), 'system', CURRENT_TIME, 0),
       ('2011', (SELECT id FROM spex_details WHERE title = 'Karl XII'), 'system', CURRENT_TIME, 0),
       ('2012', (SELECT id FROM spex_details WHERE title = 'Bröderna Lumière'), 'system', CURRENT_TIME, 0),
       ('2013', (SELECT id FROM spex_details WHERE title = 'Zheng'), 'system', CURRENT_TIME, 0),
       ('2014', (SELECT id FROM spex_details WHERE title = 'Jeanne d''Arc'), 'system', CURRENT_TIME, 0),
       ('2015', (SELECT id FROM spex_details WHERE title = 'Anne Bonny'), 'system', CURRENT_TIME, 0),
       ('2016', (SELECT id FROM spex_details WHERE title = 'Al Capone'), 'system', CURRENT_TIME, 0),
       ('2017', (SELECT id FROM spex_details WHERE title = 'Michelangelo'), 'system', CURRENT_TIME, 0),
       ('2018', (SELECT id FROM spex_details WHERE title = 'Ada Lovelace'), 'system', CURRENT_TIME, 0),
       ('2019', (SELECT id FROM spex_details WHERE title = 'Karin Månsdotter'), 'system', CURRENT_TIME, 0),

       ('1973', (SELECT id FROM spex_details WHERE title = '25-årsjubileet'), 'system', CURRENT_TIME, 0),
       ('1979', (SELECT id FROM spex_details WHERE title = 'Knappt ett Chalmersspex'), 'system', CURRENT_TIME, 0),
       ('1998', (SELECT id FROM spex_details WHERE title = '50-årskavalkad'), 'system', CURRENT_TIME, 0),
       ('2008', (SELECT id FROM spex_details WHERE title = 'Tender Bar'), 'system', CURRENT_TIME, 0),
       ('2023', (SELECT id FROM spex_details WHERE title = '75-årsjubileum'), 'system', CURRENT_TIME, 0);

-- Revivals
CREATE
TEMPORARY TABLE temp_spex AS
SELECT id, year, details_id
FROM spex
WHERE parent_id IS NULL;

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1968', (SELECT id FROM spex_details WHERE title = 'Henrik 8'),
        (SELECT id FROM temp_spex WHERE year = '1954' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Henrik 8')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1971', (SELECT id FROM spex_details WHERE title = 'Montezuma'),
        (SELECT id FROM temp_spex WHERE year = '1966' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Montezuma')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1972', (SELECT id FROM spex_details WHERE title = 'Alexander'),
        (SELECT id FROM temp_spex WHERE year = '1967' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Alexander')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1973', (SELECT id FROM spex_details WHERE title = 'Richard III'),
        (SELECT id FROM temp_spex WHERE year = '1968' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Richard III')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1974', (SELECT id FROM spex_details WHERE title = 'Nebukadnessar'),
        (SELECT id FROM temp_spex WHERE year = '1964' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Nebukadnessar')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1975', (SELECT id FROM spex_details WHERE title = 'Margareta'),
        (SELECT id FROM temp_spex WHERE year = '1969' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Margareta')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1976', (SELECT id FROM spex_details WHERE title = 'Charles II'),
        (SELECT id FROM temp_spex WHERE year = '1963' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Charles II')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1977', (SELECT id FROM spex_details WHERE title = 'Katarina II'),
        (SELECT id FROM temp_spex WHERE year = '1959' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Katarina II')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1978', (SELECT id FROM spex_details WHERE title = 'Caesarion'),
        (SELECT id FROM temp_spex WHERE year = '1950' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Caesarion')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1980', (SELECT id FROM spex_details WHERE title = 'George Washington'),
        (SELECT id FROM temp_spex WHERE year = '1970' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'George Washington')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1981', (SELECT id FROM spex_details WHERE title = 'Don Pedro'),
        (SELECT id FROM temp_spex WHERE year = '1962' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Don Pedro')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1982', (SELECT id FROM spex_details WHERE title = 'Lionardo da Vinci'),
        (SELECT id FROM temp_spex WHERE year = '1975' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Lionardo da Vinci')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1983', (SELECT id FROM spex_details WHERE title = 'Anna'),
        (SELECT id FROM temp_spex WHERE year = '1952' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Anna')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1985', (SELECT id FROM spex_details WHERE title = 'Napoleon'),
        (SELECT id FROM temp_spex WHERE year = '1956' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Napoleon')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1986', (SELECT id FROM spex_details WHERE title = 'Noak'),
        (SELECT id FROM temp_spex WHERE year = '1971' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Noak')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1987', (SELECT id FROM spex_details WHERE title = 'Nero'),
        (SELECT id FROM temp_spex WHERE year = '1979' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Nero')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1988', (SELECT id FROM spex_details WHERE title = 'Gustav E:son Vasa'),
        (SELECT id FROM temp_spex WHERE year = '1955' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Gustav E:son Vasa')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1989', (SELECT id FROM spex_details WHERE title = 'Turandot'),
        (SELECT id FROM temp_spex WHERE year = '1972' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Turandot')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1989', (SELECT id FROM spex_details WHERE title = 'Katarina II'),
        (SELECT id FROM temp_spex WHERE year = '1959' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Katarina II')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1990', (SELECT id FROM spex_details WHERE title = 'Nils Dacke'),
        (SELECT id FROM temp_spex WHERE year = '1977' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Nils Dacke')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1991', (SELECT id FROM spex_details WHERE title = 'Sherlock Holmes'),
        (SELECT id FROM temp_spex WHERE year = '1974' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Sherlock Holmes')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1992', (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven'),
        (SELECT id FROM temp_spex WHERE year = '1981' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1993', (SELECT id FROM spex_details WHERE title = 'Sven Duva'),
        (SELECT id FROM temp_spex WHERE year = '1965' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Sven Duva')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1994', (SELECT id FROM spex_details WHERE title = 'Lasse-Maja'),
        (SELECT id FROM temp_spex WHERE year = '1984' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Lasse-Maja')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1995', (SELECT id FROM spex_details WHERE title = 'Dr Livingstone'),
        (SELECT id FROM temp_spex WHERE year = '1978' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Dr Livingstone')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1996', (SELECT id FROM spex_details WHERE title = 'Olof Skötkonung'),
        (SELECT id FROM temp_spex WHERE year = '1985' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Olof Skötkonung')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1997', (SELECT id FROM spex_details WHERE title = 'Tutankhamon'),
        (SELECT id FROM temp_spex WHERE year = '1980' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Tutankhamon')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1998', (SELECT id FROM spex_details WHERE title = 'Klondike'),
        (SELECT id FROM temp_spex WHERE year = '1990' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Klondike')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1998', (SELECT id FROM spex_details WHERE title = 'Henrik 8'),
        (SELECT id FROM temp_spex WHERE year = '1954' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Henrik 8')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1998', (SELECT id FROM spex_details WHERE title = 'George Washington'),
        (SELECT id FROM temp_spex WHERE year = '1970' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'George Washington')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1998', (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven'),
        (SELECT id FROM temp_spex WHERE year = '1981' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('1999', (SELECT id FROM spex_details WHERE title = 'John Ericsson'),
        (SELECT id FROM temp_spex WHERE year = '1982' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'John Ericsson')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2000', (SELECT id FROM spex_details WHERE title = 'Bojan'),
        (SELECT id FROM temp_spex WHERE year = '1948' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Bojan')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2001', (SELECT id FROM spex_details WHERE title = 'Ludvig XIV'),
        (SELECT id FROM temp_spex WHERE year = '1976' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Ludvig XIV')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2002', (SELECT id FROM spex_details WHERE title = 'Anna'),
        (SELECT id FROM temp_spex WHERE year = '1952' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Anna')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2005', (SELECT id FROM spex_details WHERE title = 'Montgomery'),
        (SELECT id FROM temp_spex WHERE year = '1987' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Montgomery')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2006', (SELECT id FROM spex_details WHERE title = 'Stradivarius'),
        (SELECT id FROM temp_spex WHERE year = '1993' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Stradivarius')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2007', (SELECT id FROM spex_details WHERE title = 'Filip II'),
        (SELECT id FROM temp_spex WHERE year = '1983' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Filip II')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2008', (SELECT id FROM spex_details WHERE title = 'Caesarion'),
        (SELECT id FROM temp_spex WHERE year = '1950' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Caesarion')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2009', (SELECT id FROM spex_details WHERE title = 'Svartskägg'),
        (SELECT id FROM temp_spex WHERE year = '1988' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Svartskägg')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2010', (SELECT id FROM spex_details WHERE title = 'Caesarion'),
        (SELECT id FROM temp_spex WHERE year = '1950' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Caesarion')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2012', (SELECT id FROM spex_details WHERE title = 'Katarina II'),
        (SELECT id FROM temp_spex WHERE year = '1959' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Katarina II')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2013', (SELECT id FROM spex_details WHERE title = 'Gagarin'),
        (SELECT id FROM temp_spex WHERE year = '2003' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Gagarin')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2013', (SELECT id FROM spex_details WHERE title = 'Mata Hari'),
        (SELECT id FROM temp_spex WHERE year = '2003' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Mata Hari')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at, version)
VALUES ('2013', (SELECT id FROM spex_details WHERE title = 'Lasse-Maja'),
        (SELECT id FROM temp_spex WHERE year = '1984' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Lasse-Maja')), 'system', CURRENT_TIME, 0);

DROP
TEMPORARY TABLE temp_spex;
