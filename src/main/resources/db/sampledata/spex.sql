-- Spex categories
INSERT INTO spex_category (name, first_year, created_by, created_at)
VALUES ('Chalmersspexet', '1948', 'system', CURRENT_TIME);
SET
@spex_category_chalmersspexet = LAST_INSERT_ID();

INSERT INTO spex_category (name, first_year, created_by, created_at)
VALUES ('Bobspexet', '2003', 'system', CURRENT_TIME);
SET
@spex_category_bobspexet = LAST_INSERT_ID();

INSERT INTO spex_category (name, first_year, created_by, created_at)
VALUES ('Veraspexet', '2003', 'system', CURRENT_TIME);
SET
@spex_category_veraspexet = LAST_INSERT_ID();

INSERT INTO spex_category (name, first_year, created_by, created_at)
VALUES ('Jubileumsspex', '1948', 'system', CURRENT_TIME);
SET
@spex_category_jubileumsspex = LAST_INSERT_ID();

-- Spex details
INSERT INTO spex_details (title, category_id, created_by, created_at)
VALUES ('Bojan', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Erik XIV', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Caesarion', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Scheherazade', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Anna', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Henrik 8', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Gustav E:son Vasa', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Napoleon', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Statyerna', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Lucrezia', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Katarina II', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Starke August', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Klodvig', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Don Pedro', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Charles II', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Nebukadnessar', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Sven Duva', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Montezuma', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Alexander', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Richard III', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Margareta', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('George Washington', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Noak', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Turandot', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Fredrik den Store', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Sherlock Holmes', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Lionardo da Vinci', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Ludvig XIV', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Nils Dacke', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Dr Livingstone', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Nero', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Tutankhamon', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Ludwig van Beethoven', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('John Ericsson', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Filip II', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Lasse-Maja', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Olof Skötkonung', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Victoria', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Montgomery', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Svartskägg', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Christina', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Klondike', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Gutenberg', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Krösus', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Stradivarius', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Ivan den förskräcklige', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Snorre', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Nobel', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Ali Baba', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Sköna Hélena', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Nostradamus', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Mose', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Marco Polo', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Dracula', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Carl von Linné', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Aristoteles', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('H. C. Andersen', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Elisabeth I', @spex_category_chalmersspexet, 'system', CURRENT_TIME),
       ('Gauss', @spex_category_chalmersspexet, 'system', CURRENT_TIME),

       ('Gagarin', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Heliga Birgitta', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Gustav II Adolf', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Casanova', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Hannibal', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Picasso', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Newton', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Kristian Tyrann', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Marie Curie', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Christofer Columbus', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Bellman', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Herakles', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Rasputin', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Kleopatra', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Magnus Ladulås', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Geronimo', @spex_category_bobspexet, 'system', CURRENT_TIME),
       ('Marie Antoinette', @spex_category_bobspexet, 'system', CURRENT_TIME),

       ('Mata Hari', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Phileas Fogg', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Arthur', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Amelia Earhart', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Frankenstein', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Wyatt Earp & Doc Holiday', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Taj Mahal', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Lucia', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Karl XII', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Bröderna Lumière', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Zheng', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Jeanne d''Arc', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Anne Bonny', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Al Capone', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Michelangelo', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Ada Lovelace', @spex_category_veraspexet, 'system', CURRENT_TIME),
       ('Karin Månsdotter', @spex_category_veraspexet, 'system', CURRENT_TIME),

       ('25-årsjubileet', @spex_category_jubileumsspex, 'system', CURRENT_TIME),
       ('Knappt ett Chalmersspex', @spex_category_jubileumsspex, 'system', CURRENT_TIME),
       ('50-årskavalkad', @spex_category_jubileumsspex, 'system', CURRENT_TIME),
       ('Tender Bar', @spex_category_jubileumsspex, 'system', CURRENT_TIME),
       ('75-årsjubileum', @spex_category_jubileumsspex, 'system', CURRENT_TIME);

-- Spex
INSERT INTO spex (year, details_id, created_by, created_at)
VALUES ('1948', (SELECT id FROM spex_details WHERE title = 'Bojan'), 'system', CURRENT_TIME),
       ('1949', (SELECT id FROM spex_details WHERE title = 'Erik XIV'), 'system', CURRENT_TIME),
       ('1950', (SELECT id FROM spex_details WHERE title = 'Caesarion'), 'system', CURRENT_TIME),
       ('1951', (SELECT id FROM spex_details WHERE title = 'Scheherazade'), 'system', CURRENT_TIME),
       ('1952', (SELECT id FROM spex_details WHERE title = 'Anna'), 'system', CURRENT_TIME),
       ('1953', (SELECT id FROM spex_details WHERE title = 'Henrik 8'), 'system', CURRENT_TIME),
       ('1954', (SELECT id FROM spex_details WHERE title = 'Gustav E:son Vasa'), 'system', CURRENT_TIME),
       ('1955', (SELECT id FROM spex_details WHERE title = 'Napoleon'), 'system', CURRENT_TIME),
       ('1956', (SELECT id FROM spex_details WHERE title = 'Statyerna'), 'system', CURRENT_TIME),
       ('1957', (SELECT id FROM spex_details WHERE title = 'Lucrezia'), 'system', CURRENT_TIME),
       ('1958', (SELECT id FROM spex_details WHERE title = 'Katarina II'), 'system', CURRENT_TIME),
       ('1959', (SELECT id FROM spex_details WHERE title = 'Starke August'), 'system', CURRENT_TIME),
       ('1960', (SELECT id FROM spex_details WHERE title = 'Klodvig'), 'system', CURRENT_TIME),
       ('1961', (SELECT id FROM spex_details WHERE title = 'Don Pedro'), 'system', CURRENT_TIME),
       ('1962', (SELECT id FROM spex_details WHERE title = 'Charles II'), 'system', CURRENT_TIME),
       ('1963', (SELECT id FROM spex_details WHERE title = 'Nebukadnessar'), 'system', CURRENT_TIME),
       ('1964', (SELECT id FROM spex_details WHERE title = 'Sven Duva'), 'system', CURRENT_TIME),
       ('1965', (SELECT id FROM spex_details WHERE title = 'Montezuma'), 'system', CURRENT_TIME),
       ('1966', (SELECT id FROM spex_details WHERE title = 'Alexander'), 'system', CURRENT_TIME),
       ('1967', (SELECT id FROM spex_details WHERE title = 'Richard III'), 'system', CURRENT_TIME),
       ('1968', (SELECT id FROM spex_details WHERE title = 'Margareta'), 'system', CURRENT_TIME),
       ('1969', (SELECT id FROM spex_details WHERE title = 'George Washington'), 'system', CURRENT_TIME),
       ('1970', (SELECT id FROM spex_details WHERE title = 'Noak'), 'system', CURRENT_TIME),
       ('1971', (SELECT id FROM spex_details WHERE title = 'Turandot'), 'system', CURRENT_TIME),
       ('1972', (SELECT id FROM spex_details WHERE title = 'Fredrik den Store'), 'system', CURRENT_TIME),
       ('1973', (SELECT id FROM spex_details WHERE title = 'Sherlock Holmes'), 'system', CURRENT_TIME),
       ('1974', (SELECT id FROM spex_details WHERE title = 'Lionardo da Vinci'), 'system', CURRENT_TIME),
       ('1975', (SELECT id FROM spex_details WHERE title = 'Ludvig XIV'), 'system', CURRENT_TIME),
       ('1976', (SELECT id FROM spex_details WHERE title = 'Nils Dacke'), 'system', CURRENT_TIME),
       ('1977', (SELECT id FROM spex_details WHERE title = 'Dr Livingstone'), 'system', CURRENT_TIME),
       ('1978', (SELECT id FROM spex_details WHERE title = 'Nero'), 'system', CURRENT_TIME),
       ('1979', (SELECT id FROM spex_details WHERE title = 'Tutankhamon'), 'system', CURRENT_TIME),
       ('1980', (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven'), 'system', CURRENT_TIME),
       ('1981', (SELECT id FROM spex_details WHERE title = 'John Ericsson'), 'system', CURRENT_TIME),
       ('1982', (SELECT id FROM spex_details WHERE title = 'Filip II'), 'system', CURRENT_TIME),
       ('1983', (SELECT id FROM spex_details WHERE title = 'Lasse-Maja'), 'system', CURRENT_TIME),
       ('1984', (SELECT id FROM spex_details WHERE title = 'Olof Skötkonung'), 'system', CURRENT_TIME),
       ('1985', (SELECT id FROM spex_details WHERE title = 'Victoria'), 'system', CURRENT_TIME),
       ('1986', (SELECT id FROM spex_details WHERE title = 'Montgomery'), 'system', CURRENT_TIME),
       ('1987', (SELECT id FROM spex_details WHERE title = 'Svartskägg'), 'system', CURRENT_TIME),
       ('1988', (SELECT id FROM spex_details WHERE title = 'Christina'), 'system', CURRENT_TIME),
       ('1989', (SELECT id FROM spex_details WHERE title = 'Klondike'), 'system', CURRENT_TIME),
       ('1990', (SELECT id FROM spex_details WHERE title = 'Gutenberg'), 'system', CURRENT_TIME),
       ('1991', (SELECT id FROM spex_details WHERE title = 'Krösus'), 'system', CURRENT_TIME),
       ('1992', (SELECT id FROM spex_details WHERE title = 'Stradivarius'), 'system', CURRENT_TIME),
       ('1993', (SELECT id FROM spex_details WHERE title = 'Ivan den förskräcklige'), 'system', CURRENT_TIME),
       ('1994', (SELECT id FROM spex_details WHERE title = 'Snorre'), 'system', CURRENT_TIME),
       ('1995', (SELECT id FROM spex_details WHERE title = 'Nobel'), 'system', CURRENT_TIME),
       ('1996', (SELECT id FROM spex_details WHERE title = 'Ali Baba'), 'system', CURRENT_TIME),
       ('1997', (SELECT id FROM spex_details WHERE title = 'Sköna Hélena'), 'system', CURRENT_TIME),
       ('1998', (SELECT id FROM spex_details WHERE title = 'Nostradamus'), 'system', CURRENT_TIME),
       ('1999', (SELECT id FROM spex_details WHERE title = 'Mose'), 'system', CURRENT_TIME),
       ('2001', (SELECT id FROM spex_details WHERE title = 'Marco Polo'), 'system', CURRENT_TIME),
       ('2002', (SELECT id FROM spex_details WHERE title = 'Dracula'), 'system', CURRENT_TIME),
       ('2020', (SELECT id FROM spex_details WHERE title = 'Carl von Linné'), 'system', CURRENT_TIME),
       ('2021', (SELECT id FROM spex_details WHERE title = 'Aristoteles'), 'system', CURRENT_TIME),
       ('2022', (SELECT id FROM spex_details WHERE title = 'H. C. Andersen'), 'system', CURRENT_TIME),
       ('2023', (SELECT id FROM spex_details WHERE title = 'Elisabeth I'), 'system', CURRENT_TIME),
       ('2024', (SELECT id FROM spex_details WHERE title = 'Gauss'), 'system', CURRENT_TIME),

       ('2003', (SELECT id FROM spex_details WHERE title = 'Gagarin'), 'system', CURRENT_TIME),
       ('2004', (SELECT id FROM spex_details WHERE title = 'Heliga Birgitta'), 'system', CURRENT_TIME),
       ('2005', (SELECT id FROM spex_details WHERE title = 'Gustav II Adolf'), 'system', CURRENT_TIME),
       ('2006', (SELECT id FROM spex_details WHERE title = 'Casanova'), 'system', CURRENT_TIME),
       ('2007', (SELECT id FROM spex_details WHERE title = 'Hannibal'), 'system', CURRENT_TIME),
       ('2008', (SELECT id FROM spex_details WHERE title = 'Picasso'), 'system', CURRENT_TIME),
       ('2009', (SELECT id FROM spex_details WHERE title = 'Newton'), 'system', CURRENT_TIME),
       ('2010', (SELECT id FROM spex_details WHERE title = 'Kristian Tyrann'), 'system', CURRENT_TIME),
       ('2011', (SELECT id FROM spex_details WHERE title = 'Marie Curie'), 'system', CURRENT_TIME),
       ('2012', (SELECT id FROM spex_details WHERE title = 'Christofer Columbus'), 'system', CURRENT_TIME),
       ('2013', (SELECT id FROM spex_details WHERE title = 'Bellman'), 'system', CURRENT_TIME),
       ('2014', (SELECT id FROM spex_details WHERE title = 'Herakles'), 'system', CURRENT_TIME),
       ('2015', (SELECT id FROM spex_details WHERE title = 'Rasputin'), 'system', CURRENT_TIME),
       ('2016', (SELECT id FROM spex_details WHERE title = 'Kleopatra'), 'system', CURRENT_TIME),
       ('2017', (SELECT id FROM spex_details WHERE title = 'Magnus Ladulås'), 'system', CURRENT_TIME),
       ('2018', (SELECT id FROM spex_details WHERE title = 'Geronimo'), 'system', CURRENT_TIME),
       ('2019', (SELECT id FROM spex_details WHERE title = 'Marie Antoinette'), 'system', CURRENT_TIME),

       ('2003', (SELECT id FROM spex_details WHERE title = 'Mata Hari'), 'system', CURRENT_TIME),
       ('2004', (SELECT id FROM spex_details WHERE title = 'Phileas Fogg'), 'system', CURRENT_TIME),
       ('2005', (SELECT id FROM spex_details WHERE title = 'Arthur'), 'system', CURRENT_TIME),
       ('2006', (SELECT id FROM spex_details WHERE title = 'Amelia Earhart'), 'system', CURRENT_TIME),
       ('2007', (SELECT id FROM spex_details WHERE title = 'Frankenstein'), 'system', CURRENT_TIME),
       ('2008', (SELECT id FROM spex_details WHERE title = 'Wyatt Earp & Doc Holiday'), 'system', CURRENT_TIME),
       ('2009', (SELECT id FROM spex_details WHERE title = 'Taj Mahal'), 'system', CURRENT_TIME),
       ('2010', (SELECT id FROM spex_details WHERE title = 'Lucia'), 'system', CURRENT_TIME),
       ('2011', (SELECT id FROM spex_details WHERE title = 'Karl XII'), 'system', CURRENT_TIME),
       ('2012', (SELECT id FROM spex_details WHERE title = 'Bröderna Lumière'), 'system', CURRENT_TIME),
       ('2013', (SELECT id FROM spex_details WHERE title = 'Zheng'), 'system', CURRENT_TIME),
       ('2014', (SELECT id FROM spex_details WHERE title = 'Jeanne d''Arc'), 'system', CURRENT_TIME),
       ('2015', (SELECT id FROM spex_details WHERE title = 'Anne Bonny'), 'system', CURRENT_TIME),
       ('2016', (SELECT id FROM spex_details WHERE title = 'Al Capone'), 'system', CURRENT_TIME),
       ('2017', (SELECT id FROM spex_details WHERE title = 'Michelangelo'), 'system', CURRENT_TIME),
       ('2018', (SELECT id FROM spex_details WHERE title = 'Ada Lovelace'), 'system', CURRENT_TIME),
       ('2019', (SELECT id FROM spex_details WHERE title = 'Karin Månsdotter'), 'system', CURRENT_TIME),

       ('1973', (SELECT id FROM spex_details WHERE title = '25-årsjubileet'), 'system', CURRENT_TIME),
       ('1979', (SELECT id FROM spex_details WHERE title = 'Knappt ett Chalmersspex'), 'system', CURRENT_TIME),
       ('1998', (SELECT id FROM spex_details WHERE title = '50-årskavalkad'), 'system', CURRENT_TIME),
       ('2008', (SELECT id FROM spex_details WHERE title = 'Tender Bar'), 'system', CURRENT_TIME),
       ('2023', (SELECT id FROM spex_details WHERE title = '75-årsjubileum'), 'system', CURRENT_TIME);

-- Revivals
CREATE
TEMPORARY TABLE temp_spex AS
SELECT id, year, details_id
FROM spex;

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1968', (SELECT id FROM spex_details WHERE title = 'Henrik 8'),
        (SELECT id FROM temp_spex WHERE year = '1954' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Henrik 8')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1971', (SELECT id FROM spex_details WHERE title = 'Montezuma'),
        (SELECT id FROM temp_spex WHERE year = '1966' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Montezuma')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1972', (SELECT id FROM spex_details WHERE title = 'Alexander'),
        (SELECT id FROM temp_spex WHERE year = '1967' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Alexander')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1973', (SELECT id FROM spex_details WHERE title = 'Richard III'),
        (SELECT id FROM temp_spex WHERE year = '1968' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Richard III')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1974', (SELECT id FROM spex_details WHERE title = 'Nebukadnessar'),
        (SELECT id FROM temp_spex WHERE year = '1964' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Nebukadnessar')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1975', (SELECT id FROM spex_details WHERE title = 'Margareta'),
        (SELECT id FROM temp_spex WHERE year = '1969' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Margareta')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1976', (SELECT id FROM spex_details WHERE title = 'Charles II'),
        (SELECT id FROM temp_spex WHERE year = '1963' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Charles II')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1977', (SELECT id FROM spex_details WHERE title = 'Katarina II'),
        (SELECT id FROM temp_spex WHERE year = '1959' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Katarina II')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1978', (SELECT id FROM spex_details WHERE title = 'Caesarion'),
        (SELECT id FROM temp_spex WHERE year = '1950' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Caesarion')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1980', (SELECT id FROM spex_details WHERE title = 'George Washington'),
        (SELECT id FROM temp_spex WHERE year = '1970' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'George Washington')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1981', (SELECT id FROM spex_details WHERE title = 'Don Pedro'),
        (SELECT id FROM temp_spex WHERE year = '1962' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Don Pedro')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1982', (SELECT id FROM spex_details WHERE title = 'Lionardo da Vinci'),
        (SELECT id FROM temp_spex WHERE year = '1975' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Lionardo da Vinci')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1983', (SELECT id FROM spex_details WHERE title = 'Anna'),
        (SELECT id FROM temp_spex WHERE year = '1952' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Anna')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1985', (SELECT id FROM spex_details WHERE title = 'Napoleon'),
        (SELECT id FROM temp_spex WHERE year = '1956' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Napoleon')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1986', (SELECT id FROM spex_details WHERE title = 'Noak'),
        (SELECT id FROM temp_spex WHERE year = '1971' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Noak')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1987', (SELECT id FROM spex_details WHERE title = 'Nero'),
        (SELECT id FROM temp_spex WHERE year = '1979' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Nero')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1988', (SELECT id FROM spex_details WHERE title = 'Gustav E:son Vasa'),
        (SELECT id FROM temp_spex WHERE year = '1955' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Gustav E:son Vasa')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1989', (SELECT id FROM spex_details WHERE title = 'Turandot'),
        (SELECT id FROM temp_spex WHERE year = '1972' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Turandot')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1989', (SELECT id FROM spex_details WHERE title = 'Katarina II'),
        (SELECT id FROM temp_spex WHERE year = '1959' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Katarina II')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1990', (SELECT id FROM spex_details WHERE title = 'Nils Dacke'),
        (SELECT id FROM temp_spex WHERE year = '1977' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Nils Dacke')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1991', (SELECT id FROM spex_details WHERE title = 'Sherlock Holmes'),
        (SELECT id FROM temp_spex WHERE year = '1974' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Sherlock Holmes')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1992', (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven'),
        (SELECT id FROM temp_spex WHERE year = '1981' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1993', (SELECT id FROM spex_details WHERE title = 'Sven Duva'),
        (SELECT id FROM temp_spex WHERE year = '1965' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Sven Duva')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1994', (SELECT id FROM spex_details WHERE title = 'Lasse-Maja'),
        (SELECT id FROM temp_spex WHERE year = '1984' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Lasse-Maja')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1995', (SELECT id FROM spex_details WHERE title = 'Dr Livingstone'),
        (SELECT id FROM temp_spex WHERE year = '1978' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Dr Livingstone')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1996', (SELECT id FROM spex_details WHERE title = 'Olof Skötkonung'),
        (SELECT id FROM temp_spex WHERE year = '1948' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Olof Skötkonung')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1997', (SELECT id FROM spex_details WHERE title = 'Tutankhamon'),
        (SELECT id FROM temp_spex WHERE year = '1980' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Tutankhamon')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1998', (SELECT id FROM spex_details WHERE title = 'Klondike'),
        (SELECT id FROM temp_spex WHERE year = '1990' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Klondike')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1998', (SELECT id FROM spex_details WHERE title = 'Henrik 8'),
        (SELECT id FROM temp_spex WHERE year = '1964' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Henrik 8')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1998', (SELECT id FROM spex_details WHERE title = 'George Washington'),
        (SELECT id FROM temp_spex WHERE year = '1970' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'George Washington')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1998', (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven'),
        (SELECT id FROM temp_spex WHERE year = '1981' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('1999', (SELECT id FROM spex_details WHERE title = 'John Ericsson'),
        (SELECT id FROM temp_spex WHERE year = '1982' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'John Ericsson')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2000', (SELECT id FROM spex_details WHERE title = 'Bojan'),
        (SELECT id FROM temp_spex WHERE year = '1948' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Bojan')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2001', (SELECT id FROM spex_details WHERE title = 'Ludvig XIV'),
        (SELECT id FROM temp_spex WHERE year = '1976' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Ludvig XIV')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2002', (SELECT id FROM spex_details WHERE title = 'Anna'),
        (SELECT id FROM temp_spex WHERE year = '1952' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Anna')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2005', (SELECT id FROM spex_details WHERE title = 'Montgomery'),
        (SELECT id FROM temp_spex WHERE year = '1987' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Montgomery')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2006', (SELECT id FROM spex_details WHERE title = 'Stradivarius'),
        (SELECT id FROM temp_spex WHERE year = '2006' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Stradivarius')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2007', (SELECT id FROM spex_details WHERE title = 'Filip II'),
        (SELECT id FROM temp_spex WHERE year = '1983' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Filip II')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2008', (SELECT id FROM spex_details WHERE title = 'Caesarion'),
        (SELECT id FROM temp_spex WHERE year = '1950' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Caesarion')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2009', (SELECT id FROM spex_details WHERE title = 'Svartskägg'),
        (SELECT id FROM temp_spex WHERE year = '1988' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Svartskägg')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2010', (SELECT id FROM spex_details WHERE title = 'Caesarion'),
        (SELECT id FROM temp_spex WHERE year = '1950' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Caesarion')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2012', (SELECT id FROM spex_details WHERE title = 'Katarina II'),
        (SELECT id FROM temp_spex WHERE year = '1959' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Katarina II')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2013', (SELECT id FROM spex_details WHERE title = 'Gagarin'),
        (SELECT id FROM temp_spex WHERE year = '2003' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Gagarin')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2013', (SELECT id FROM spex_details WHERE title = 'Mata Hari'),
        (SELECT id FROM temp_spex WHERE year = '2003' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Mata Hari')), 'system', CURRENT_TIME);

INSERT INTO spex (year, details_id, parent_id, created_by, created_at)
VALUES ('2013', (SELECT id FROM spex_details WHERE title = 'Lasse-Maja'),
        (SELECT id FROM temp_spex WHERE year = '1984' AND
        details_id = (SELECT id FROM spex_details WHERE title = 'Lasse-Maja')), 'system', CURRENT_TIME);

DROP
TEMPORARY TABLE temp_spex;
