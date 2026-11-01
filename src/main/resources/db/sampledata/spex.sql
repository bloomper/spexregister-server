-- Spex categories
INSERT INTO spex_category (uuid, name, first_year, created_by, created_at, version)
VALUES (UUID(), 'Gamla Chalmersspexet', '1948', 'system', CURRENT_TIME, 0);
SET
    @spex_category_gamla_chalmersspexet = LAST_INSERT_ID();

INSERT INTO spex_category (uuid, name, first_year, created_by, created_at, version)
VALUES (UUID(), 'Bobspexet', '2003', 'system', CURRENT_TIME, 0);
SET
    @spex_category_bobspexet = LAST_INSERT_ID();

INSERT INTO spex_category (uuid, name, first_year, created_by, created_at, version)
VALUES (UUID(), 'Veraspexet', '2003', 'system', CURRENT_TIME, 0);
SET
    @spex_category_veraspexet = LAST_INSERT_ID();

INSERT INTO spex_category (uuid, name, first_year, created_by, created_at, version)
VALUES (UUID(), 'Jubileumsspex', '1948', 'system', CURRENT_TIME, 0);
SET
    @spex_category_jubileumsspex = LAST_INSERT_ID();

INSERT INTO spex_category (uuid, name, first_year, created_by, created_at, version)
VALUES (UUID(), 'Nya Chalmersspexet', '2020', 'system', CURRENT_TIME, 0);
SET
    @spex_category_nya_chalmersspexet = LAST_INSERT_ID();

-- Spex details
INSERT INTO spex_details (uuid, title, category_id, created_by, created_at, version)
VALUES (UUID(), 'Bojan', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Erik XIV', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Cæsarion', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Scheherazade', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Anna', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Henrik 8', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Gustav E:son Vasa', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Napoleon', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Statyerna', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Lucrezia', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Katarina II', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Starke August', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Klodvig', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Don Pedro', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Charles II', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Nebukadnessar', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Sven Duva', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Montezuma', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Alexander', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Richard III', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Margareta', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'George Washington', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Noak', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Turandot', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Fredrik den Store', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Sherlock Holmes', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Lionardo da Vinci', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Ludvig XIV', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Nils Dacke', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Dr Livingstone', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Nero', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Tutankhamon', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Ludwig van Beethoven', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'John Ericsson', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Filip II', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Lasse-Maja', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Olof Skötkonung', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Victoria', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Montgomery', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Svartskägg', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Christina', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Klondike', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Gutenberg', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Krösus', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Stradivarius', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Ivan den förskräcklige', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Snorre', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Nobel', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Ali Baba', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Sköna Helena', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Nostradamus', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Mose', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Marco Polo', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Dracula', @spex_category_gamla_chalmersspexet, 'system', CURRENT_TIME, 0),

       (UUID(), 'Gagarin', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Heliga Birgitta', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Gustav II Adolf', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Casanova', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Hannibal', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Picasso', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Newton', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Kristian Tyrann', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Marie Curie', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Christofer Columbus', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Bellman', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Herakles', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Rasputin', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Kleopatra', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Magnus Ladulås', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Geronimo', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Marie Antoinette', @spex_category_bobspexet, 'system', CURRENT_TIME, 0),

       (UUID(), 'Mata Hari', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Phileas Fogg', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Arthur', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Amelia Earhart', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Frankenstein', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Wyatt Earp & Doc Holliday', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Taj Mahal', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Lucia', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Karl XII', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Bröderna Lumière', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Zheng', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Jeanne d''Arc', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Anne Bonny', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Al Capone', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Michelangelo', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Ada Lovelace', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Karin Månsdotter', @spex_category_veraspexet, 'system', CURRENT_TIME, 0),

       (UUID(), 'Carl von Linné', @spex_category_nya_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Aristoteles', @spex_category_nya_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'H. C. Andersen', @spex_category_nya_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Elisabeth I', @spex_category_nya_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Gauss', @spex_category_nya_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Ask och Embla', @spex_category_nya_chalmersspexet, 'system', CURRENT_TIME, 0),
       (UUID(), 'Abraham Lincoln', @spex_category_nya_chalmersspexet, 'system', CURRENT_TIME, 0),

       (UUID(), '25-årsjubileet', @spex_category_jubileumsspex, 'system', CURRENT_TIME, 0),
       (UUID(), 'Knappt ett Chalmersspex', @spex_category_jubileumsspex, 'system', CURRENT_TIME, 0),
       (UUID(), '50-årskavalkad', @spex_category_jubileumsspex, 'system', CURRENT_TIME, 0),
       (UUID(), 'Tender Bar', @spex_category_jubileumsspex, 'system', CURRENT_TIME, 0),
       (UUID(), '75-årsjubileum', @spex_category_jubileumsspex, 'system', CURRENT_TIME, 0);

-- Spex
INSERT INTO spex (uuid, year, details_id, created_by, created_at, version)
VALUES (UUID(), '1948', (SELECT id FROM spex_details WHERE title = 'Bojan'), 'system', CURRENT_TIME, 0),
       (UUID(), '1949', (SELECT id FROM spex_details WHERE title = 'Erik XIV'), 'system', CURRENT_TIME, 0),
       (UUID(), '1950', (SELECT id FROM spex_details WHERE title = 'Cæsarion'), 'system', CURRENT_TIME, 0),
       (UUID(), '1951', (SELECT id FROM spex_details WHERE title = 'Scheherazade'), 'system', CURRENT_TIME, 0),
       (UUID(), '1952', (SELECT id FROM spex_details WHERE title = 'Anna'), 'system', CURRENT_TIME, 0),
       (UUID(), '1953', (SELECT id FROM spex_details WHERE title = 'Cæsarion'), 'system', CURRENT_TIME, 0),
       (UUID(), '1954', (SELECT id FROM spex_details WHERE title = 'Henrik 8'), 'system', CURRENT_TIME, 0),
       (UUID(), '1955', (SELECT id FROM spex_details WHERE title = 'Gustav E:son Vasa'), 'system', CURRENT_TIME, 0),
       (UUID(), '1956', (SELECT id FROM spex_details WHERE title = 'Napoleon'), 'system', CURRENT_TIME, 0),
       (UUID(), '1957', (SELECT id FROM spex_details WHERE title = 'Statyerna'), 'system', CURRENT_TIME, 0),
       (UUID(), '1958', (SELECT id FROM spex_details WHERE title = 'Lucrezia'), 'system', CURRENT_TIME, 0),
       (UUID(), '1959', (SELECT id FROM spex_details WHERE title = 'Katarina II'), 'system', CURRENT_TIME, 0),
       (UUID(), '1960', (SELECT id FROM spex_details WHERE title = 'Starke August'), 'system', CURRENT_TIME, 0),
       (UUID(), '1961', (SELECT id FROM spex_details WHERE title = 'Klodvig'), 'system', CURRENT_TIME, 0),
       (UUID(), '1962', (SELECT id FROM spex_details WHERE title = 'Don Pedro'), 'system', CURRENT_TIME, 0),
       (UUID(), '1963', (SELECT id FROM spex_details WHERE title = 'Charles II'), 'system', CURRENT_TIME, 0),
       (UUID(), '1964', (SELECT id FROM spex_details WHERE title = 'Nebukadnessar'), 'system', CURRENT_TIME, 0),
       (UUID(), '1965', (SELECT id FROM spex_details WHERE title = 'Sven Duva'), 'system', CURRENT_TIME, 0),
       (UUID(), '1966', (SELECT id FROM spex_details WHERE title = 'Montezuma'), 'system', CURRENT_TIME, 0),
       (UUID(), '1967', (SELECT id FROM spex_details WHERE title = 'Alexander'), 'system', CURRENT_TIME, 0),
       (UUID(), '1968', (SELECT id FROM spex_details WHERE title = 'Richard III'), 'system', CURRENT_TIME, 0),
       (UUID(), '1969', (SELECT id FROM spex_details WHERE title = 'Margareta'), 'system', CURRENT_TIME, 0),
       (UUID(), '1970', (SELECT id FROM spex_details WHERE title = 'George Washington'), 'system', CURRENT_TIME, 0),
       (UUID(), '1971', (SELECT id FROM spex_details WHERE title = 'Noak'), 'system', CURRENT_TIME, 0),
       (UUID(), '1972', (SELECT id FROM spex_details WHERE title = 'Turandot'), 'system', CURRENT_TIME, 0),
       (UUID(), '1973', (SELECT id FROM spex_details WHERE title = 'Fredrik den Store'), 'system', CURRENT_TIME, 0),
       (UUID(), '1974', (SELECT id FROM spex_details WHERE title = 'Sherlock Holmes'), 'system', CURRENT_TIME, 0),
       (UUID(), '1975', (SELECT id FROM spex_details WHERE title = 'Lionardo da Vinci'), 'system', CURRENT_TIME, 0),
       (UUID(), '1976', (SELECT id FROM spex_details WHERE title = 'Ludvig XIV'), 'system', CURRENT_TIME, 0),
       (UUID(), '1977', (SELECT id FROM spex_details WHERE title = 'Nils Dacke'), 'system', CURRENT_TIME, 0),
       (UUID(), '1978', (SELECT id FROM spex_details WHERE title = 'Dr Livingstone'), 'system', CURRENT_TIME, 0),
       (UUID(), '1979', (SELECT id FROM spex_details WHERE title = 'Nero'), 'system', CURRENT_TIME, 0),
       (UUID(), '1980', (SELECT id FROM spex_details WHERE title = 'Tutankhamon'), 'system', CURRENT_TIME, 0),
       (UUID(), '1981', (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven'), 'system', CURRENT_TIME, 0),
       (UUID(), '1982', (SELECT id FROM spex_details WHERE title = 'John Ericsson'), 'system', CURRENT_TIME, 0),
       (UUID(), '1983', (SELECT id FROM spex_details WHERE title = 'Filip II'), 'system', CURRENT_TIME, 0),
       (UUID(), '1984', (SELECT id FROM spex_details WHERE title = 'Lasse-Maja'), 'system', CURRENT_TIME, 0),
       (UUID(), '1985', (SELECT id FROM spex_details WHERE title = 'Olof Skötkonung'), 'system', CURRENT_TIME, 0),
       (UUID(), '1986', (SELECT id FROM spex_details WHERE title = 'Victoria'), 'system', CURRENT_TIME, 0),
       (UUID(), '1987', (SELECT id FROM spex_details WHERE title = 'Montgomery'), 'system', CURRENT_TIME, 0),
       (UUID(), '1988', (SELECT id FROM spex_details WHERE title = 'Svartskägg'), 'system', CURRENT_TIME, 0),
       (UUID(), '1989', (SELECT id FROM spex_details WHERE title = 'Christina'), 'system', CURRENT_TIME, 0),
       (UUID(), '1990', (SELECT id FROM spex_details WHERE title = 'Klondike'), 'system', CURRENT_TIME, 0),
       (UUID(), '1991', (SELECT id FROM spex_details WHERE title = 'Gutenberg'), 'system', CURRENT_TIME, 0),
       (UUID(), '1992', (SELECT id FROM spex_details WHERE title = 'Krösus'), 'system', CURRENT_TIME, 0),
       (UUID(), '1993', (SELECT id FROM spex_details WHERE title = 'Stradivarius'), 'system', CURRENT_TIME, 0),
       (UUID(), '1994', (SELECT id FROM spex_details WHERE title = 'Ivan den förskräcklige'), 'system', CURRENT_TIME, 0),
       (UUID(), '1995', (SELECT id FROM spex_details WHERE title = 'Snorre'), 'system', CURRENT_TIME, 0),
       (UUID(), '1996', (SELECT id FROM spex_details WHERE title = 'Nobel'), 'system', CURRENT_TIME, 0),
       (UUID(), '1997', (SELECT id FROM spex_details WHERE title = 'Ali Baba'), 'system', CURRENT_TIME, 0),
       (UUID(), '1998', (SELECT id FROM spex_details WHERE title = 'Sköna Helena'), 'system', CURRENT_TIME, 0),
       (UUID(), '1999', (SELECT id FROM spex_details WHERE title = 'Nostradamus'), 'system', CURRENT_TIME, 0),
       (UUID(), '2000', (SELECT id FROM spex_details WHERE title = 'Mose'), 'system', CURRENT_TIME, 0),
       (UUID(), '2001', (SELECT id FROM spex_details WHERE title = 'Marco Polo'), 'system', CURRENT_TIME, 0),
       (UUID(), '2002', (SELECT id FROM spex_details WHERE title = 'Dracula'), 'system', CURRENT_TIME, 0),

       (UUID(), '2003', (SELECT id FROM spex_details WHERE title = 'Gagarin'), 'system', CURRENT_TIME, 0),
       (UUID(), '2004', (SELECT id FROM spex_details WHERE title = 'Heliga Birgitta'), 'system', CURRENT_TIME, 0),
       (UUID(), '2005', (SELECT id FROM spex_details WHERE title = 'Gustav II Adolf'), 'system', CURRENT_TIME, 0),
       (UUID(), '2006', (SELECT id FROM spex_details WHERE title = 'Casanova'), 'system', CURRENT_TIME, 0),
       (UUID(), '2007', (SELECT id FROM spex_details WHERE title = 'Hannibal'), 'system', CURRENT_TIME, 0),
       (UUID(), '2008', (SELECT id FROM spex_details WHERE title = 'Picasso'), 'system', CURRENT_TIME, 0),
       (UUID(), '2009', (SELECT id FROM spex_details WHERE title = 'Newton'), 'system', CURRENT_TIME, 0),
       (UUID(), '2010', (SELECT id FROM spex_details WHERE title = 'Kristian Tyrann'), 'system', CURRENT_TIME, 0),
       (UUID(), '2011', (SELECT id FROM spex_details WHERE title = 'Marie Curie'), 'system', CURRENT_TIME, 0),
       (UUID(), '2012', (SELECT id FROM spex_details WHERE title = 'Christofer Columbus'), 'system', CURRENT_TIME, 0),
       (UUID(), '2013', (SELECT id FROM spex_details WHERE title = 'Bellman'), 'system', CURRENT_TIME, 0),
       (UUID(), '2014', (SELECT id FROM spex_details WHERE title = 'Herakles'), 'system', CURRENT_TIME, 0),
       (UUID(), '2015', (SELECT id FROM spex_details WHERE title = 'Rasputin'), 'system', CURRENT_TIME, 0),
       (UUID(), '2016', (SELECT id FROM spex_details WHERE title = 'Kleopatra'), 'system', CURRENT_TIME, 0),
       (UUID(), '2017', (SELECT id FROM spex_details WHERE title = 'Magnus Ladulås'), 'system', CURRENT_TIME, 0),
       (UUID(), '2018', (SELECT id FROM spex_details WHERE title = 'Geronimo'), 'system', CURRENT_TIME, 0),
       (UUID(), '2019', (SELECT id FROM spex_details WHERE title = 'Marie Antoinette'), 'system', CURRENT_TIME, 0),

       (UUID(), '2003', (SELECT id FROM spex_details WHERE title = 'Mata Hari'), 'system', CURRENT_TIME, 0),
       (UUID(), '2004', (SELECT id FROM spex_details WHERE title = 'Phileas Fogg'), 'system', CURRENT_TIME, 0),
       (UUID(), '2005', (SELECT id FROM spex_details WHERE title = 'Arthur'), 'system', CURRENT_TIME, 0),
       (UUID(), '2006', (SELECT id FROM spex_details WHERE title = 'Amelia Earhart'), 'system', CURRENT_TIME, 0),
       (UUID(), '2007', (SELECT id FROM spex_details WHERE title = 'Frankenstein'), 'system', CURRENT_TIME, 0),
       (UUID(), '2008', (SELECT id FROM spex_details WHERE title = 'Wyatt Earp & Doc Holliday'), 'system', CURRENT_TIME, 0),
       (UUID(), '2009', (SELECT id FROM spex_details WHERE title = 'Taj Mahal'), 'system', CURRENT_TIME, 0),
       (UUID(), '2010', (SELECT id FROM spex_details WHERE title = 'Lucia'), 'system', CURRENT_TIME, 0),
       (UUID(), '2011', (SELECT id FROM spex_details WHERE title = 'Karl XII'), 'system', CURRENT_TIME, 0),
       (UUID(), '2012', (SELECT id FROM spex_details WHERE title = 'Bröderna Lumière'), 'system', CURRENT_TIME, 0),
       (UUID(), '2013', (SELECT id FROM spex_details WHERE title = 'Zheng'), 'system', CURRENT_TIME, 0),
       (UUID(), '2014', (SELECT id FROM spex_details WHERE title = 'Jeanne d''Arc'), 'system', CURRENT_TIME, 0),
       (UUID(), '2015', (SELECT id FROM spex_details WHERE title = 'Anne Bonny'), 'system', CURRENT_TIME, 0),
       (UUID(), '2016', (SELECT id FROM spex_details WHERE title = 'Al Capone'), 'system', CURRENT_TIME, 0),
       (UUID(), '2017', (SELECT id FROM spex_details WHERE title = 'Michelangelo'), 'system', CURRENT_TIME, 0),
       (UUID(), '2018', (SELECT id FROM spex_details WHERE title = 'Ada Lovelace'), 'system', CURRENT_TIME, 0),
       (UUID(), '2019', (SELECT id FROM spex_details WHERE title = 'Karin Månsdotter'), 'system', CURRENT_TIME, 0),

       (UUID(), '2020', (SELECT id FROM spex_details WHERE title = 'Carl von Linné'), 'system', CURRENT_TIME, 0),
       (UUID(), '2021', (SELECT id FROM spex_details WHERE title = 'Aristoteles'), 'system', CURRENT_TIME, 0),
       (UUID(), '2022', (SELECT id FROM spex_details WHERE title = 'H. C. Andersen'), 'system', CURRENT_TIME, 0),
       (UUID(), '2023', (SELECT id FROM spex_details WHERE title = 'Elisabeth I'), 'system', CURRENT_TIME, 0),
       (UUID(), '2024', (SELECT id FROM spex_details WHERE title = 'Gauss'), 'system', CURRENT_TIME, 0),
       (UUID(), '2025', (SELECT id FROM spex_details WHERE title = 'Ask och Embla'), 'system', CURRENT_TIME, 0),
       (UUID(), '2026', (SELECT id FROM spex_details WHERE title = 'Abraham Lincoln'), 'system', CURRENT_TIME, 0),

       (UUID(), '1973', (SELECT id FROM spex_details WHERE title = '25-årsjubileet'), 'system', CURRENT_TIME, 0),
       (UUID(), '1979', (SELECT id FROM spex_details WHERE title = 'Knappt ett Chalmersspex'), 'system', CURRENT_TIME, 0),
       (UUID(), '1998', (SELECT id FROM spex_details WHERE title = '50-årskavalkad'), 'system', CURRENT_TIME, 0),
       (UUID(), '2008', (SELECT id FROM spex_details WHERE title = 'Tender Bar'), 'system', CURRENT_TIME, 0),
       (UUID(), '2023', (SELECT id FROM spex_details WHERE title = '75-årsjubileum'), 'system', CURRENT_TIME, 0);

-- Revivals
CREATE
    TEMPORARY TABLE temp_spex AS
SELECT id, year, details_id
FROM spex
WHERE parent_id IS NULL;

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1968', (SELECT id FROM spex_details WHERE title = 'Henrik 8'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1954'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Henrik 8')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1971', (SELECT id FROM spex_details WHERE title = 'Montezuma'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1966'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Montezuma')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1972', (SELECT id FROM spex_details WHERE title = 'Alexander'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1967'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Alexander')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1973', (SELECT id FROM spex_details WHERE title = 'Richard III'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1968'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Richard III')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1974', (SELECT id FROM spex_details WHERE title = 'Nebukadnessar'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1964'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Nebukadnessar')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1975', (SELECT id FROM spex_details WHERE title = 'Margareta'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1969'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Margareta')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1976', (SELECT id FROM spex_details WHERE title = 'Charles II'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1963'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Charles II')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1977', (SELECT id FROM spex_details WHERE title = 'Katarina II'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1959'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Katarina II')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1978', (SELECT id FROM spex_details WHERE title = 'Cæsarion'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1950'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Cæsarion')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1980', (SELECT id FROM spex_details WHERE title = 'George Washington'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1970'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'George Washington')), 'system', CURRENT_TIME,
        0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1981', (SELECT id FROM spex_details WHERE title = 'Don Pedro'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1962'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Don Pedro')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1982', (SELECT id FROM spex_details WHERE title = 'Lionardo da Vinci'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1975'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Lionardo da Vinci')), 'system', CURRENT_TIME,
        0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1983', (SELECT id FROM spex_details WHERE title = 'Anna'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1952'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Anna')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1985', (SELECT id FROM spex_details WHERE title = 'Napoleon'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1956'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Napoleon')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1986', (SELECT id FROM spex_details WHERE title = 'Noak'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1971'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Noak')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1987', (SELECT id FROM spex_details WHERE title = 'Nero'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1979'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Nero')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1988', (SELECT id FROM spex_details WHERE title = 'Gustav E:son Vasa'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1955'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Gustav E:son Vasa')), 'system', CURRENT_TIME,
        0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1989', (SELECT id FROM spex_details WHERE title = 'Turandot'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1972'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Turandot')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1990', (SELECT id FROM spex_details WHERE title = 'Nils Dacke'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1977'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Nils Dacke')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1991', (SELECT id FROM spex_details WHERE title = 'Sherlock Holmes'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1974'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Sherlock Holmes')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1992', (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1981'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven')), 'system', CURRENT_TIME,
        0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1993', (SELECT id FROM spex_details WHERE title = 'Sven Duva'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1965'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Sven Duva')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1994', (SELECT id FROM spex_details WHERE title = 'Lasse-Maja'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1984'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Lasse-Maja')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1995', (SELECT id FROM spex_details WHERE title = 'Dr Livingstone'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1978'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Dr Livingstone')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1996', (SELECT id FROM spex_details WHERE title = 'Victoria'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1986'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Victoria')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1997', (SELECT id FROM spex_details WHERE title = 'Tutankhamon'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1980'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Tutankhamon')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1998', (SELECT id FROM spex_details WHERE title = 'Klondike'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1990'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Klondike')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1998', (SELECT id FROM spex_details WHERE title = 'Henrik 8'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1954'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Henrik 8')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1998', (SELECT id FROM spex_details WHERE title = 'George Washington'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1970'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'George Washington')), 'system', CURRENT_TIME,
        0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1998', (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1981'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Ludwig van Beethoven')), 'system', CURRENT_TIME,
        0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '1999', (SELECT id FROM spex_details WHERE title = 'John Ericsson'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1982'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'John Ericsson')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2000', (SELECT id FROM spex_details WHERE title = 'Bojan'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1948'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Bojan')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2001', (SELECT id FROM spex_details WHERE title = 'Ludvig XIV'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1976'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Ludvig XIV')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2002', (SELECT id FROM spex_details WHERE title = 'Anna'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1952'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Anna')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2005', (SELECT id FROM spex_details WHERE title = 'Montgomery'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1987'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Montgomery')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2006', (SELECT id FROM spex_details WHERE title = 'Stradivarius'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1993'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Stradivarius')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2007', (SELECT id FROM spex_details WHERE title = 'Filip II'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1983'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Filip II')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2008', (SELECT id FROM spex_details WHERE title = 'Cæsarion'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1950'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Cæsarion')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2009', (SELECT id FROM spex_details WHERE title = 'Svartskägg'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1988'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Svartskägg')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2010', (SELECT id FROM spex_details WHERE title = 'Cæsarion'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1950'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Cæsarion')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2012', (SELECT id FROM spex_details WHERE title = 'Katarina II'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1959'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Katarina II')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2013', (SELECT id FROM spex_details WHERE title = 'Charles II'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1963'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Charles II')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2013', (SELECT id FROM spex_details WHERE title = 'Gagarin'),
        (SELECT id
         FROM temp_spex
         WHERE year = '2003'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Gagarin')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2013', (SELECT id FROM spex_details WHERE title = 'Mata Hari'),
        (SELECT id
         FROM temp_spex
         WHERE year = '2003'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Mata Hari')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2013', (SELECT id FROM spex_details WHERE title = 'Lasse-Maja'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1984'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Lasse-Maja')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2014', (SELECT id FROM spex_details WHERE title = 'Lasse-Maja'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1984'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Lasse-Maja')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2015', (SELECT id FROM spex_details WHERE title = 'Victoria'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1986'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Victoria')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2018', (SELECT id FROM spex_details WHERE title = 'Richard III'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1968'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Richard III')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2018', (SELECT id FROM spex_details WHERE title = 'Christina'),
        (SELECT id
         FROM temp_spex
         WHERE year = '1989'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Christina')), 'system', CURRENT_TIME, 0);

INSERT INTO spex (uuid, year, details_id, parent_id, created_by, created_at, version)
VALUES (UUID(), '2024', (SELECT id FROM spex_details WHERE title = 'Jeanne d''Arc'),
        (SELECT id
         FROM temp_spex
         WHERE year = '2014'
           AND details_id = (SELECT id FROM spex_details WHERE title = 'Jeanne d''Arc')), 'system', CURRENT_TIME, 0);

DROP
    TEMPORARY TABLE temp_spex;
