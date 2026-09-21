IF DB_ID('bm220465') IS NULL
    CREATE DATABASE bm220465;
GO

USE bm220465;
GO

-- DROP TABLE IF EXISTS

IF OBJECT_ID('TR_BLOCK_EXTREME_RATING', 'TR') IS NOT NULL DROP TRIGGER TR_BLOCK_EXTREME_RATING;
IF OBJECT_ID('TR_UPDATE_MOVIE_TREND', 'TR') IS NOT NULL DROP TRIGGER TR_UPDATE_MOVIE_TREND;
IF OBJECT_ID('SP_REWARD_USER_CHECK', 'P') IS NOT NULL DROP PROCEDURE SP_REWARD_USER_CHECK;
IF OBJECT_ID('MovieTag', 'U') IS NOT NULL DROP TABLE MovieTag;
IF OBJECT_ID('MovieGenre', 'U') IS NOT NULL DROP TABLE MovieGenre;
IF OBJECT_ID('WatchList', 'U') IS NOT NULL DROP TABLE WatchList;
IF OBJECT_ID('Rating', 'U') IS NOT NULL DROP TABLE Rating;
IF OBJECT_ID('Movie', 'U') IS NOT NULL DROP TABLE Movie;
IF OBJECT_ID('Genre', 'U') IS NOT NULL DROP TABLE Genre;
IF OBJECT_ID('Tag', 'U') IS NOT NULL DROP TABLE Tag;
IF OBJECT_ID('Users', 'U') IS NOT NULL DROP TABLE Users;
GO

-- CREATE TABLES

CREATE TABLE Users (
    IdU INT IDENTITY(1,1) NOT NULL,
    Username NVARCHAR(100) NOT NULL,
    RewardCount INT NOT NULL DEFAULT 0,
    CONSTRAINT PK_Users PRIMARY KEY (IdU),
    CONSTRAINT UQ_Users_Username UNIQUE (Username)
);
GO

CREATE TABLE Genre (
    IdG INT IDENTITY(1,1) NOT NULL,
    Name NVARCHAR(100) NOT NULL,
    CONSTRAINT PK_Genre PRIMARY KEY (IdG),
    CONSTRAINT UQ_Genre_Name UNIQUE (Name)
);
GO

CREATE TABLE Tag (
    IdT INT IDENTITY(1,1) NOT NULL,
    Name NVARCHAR(100) NOT NULL,
    CONSTRAINT PK_Tag PRIMARY KEY (IdT),
    CONSTRAINT UQ_Tag_Name UNIQUE (Name)
);
GO

CREATE TABLE Movie (
    IdM INT IDENTITY(1,1) NOT NULL,
    Title NVARCHAR(100) NOT NULL,
    Director NVARCHAR(100) NOT NULL,
    Status NVARCHAR(50) NULL, -- 'Trending', 'Rising', 'Falling', 'Classic', NULL
    CONSTRAINT PK_Movie PRIMARY KEY (IdM)
);
GO

CREATE TABLE MovieGenre (
    MovieId INT NOT NULL,
    GenreId INT NOT NULL,
    CONSTRAINT PK_MovieGenre PRIMARY KEY (MovieId, GenreId),
    CONSTRAINT FK_MovieGenre_Movie FOREIGN KEY (MovieId)
        REFERENCES Movie(IdM) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_MovieGenre_Genre FOREIGN KEY (GenreId)
        REFERENCES Genre(IdG) ON UPDATE CASCADE ON DELETE CASCADE
);
GO

CREATE TABLE MovieTag (
    MovieId INT NOT NULL,
    TagId INT NOT NULL,
    CONSTRAINT PK_MovieTag PRIMARY KEY (MovieId, TagId),
    CONSTRAINT FK_MovieTag_Movie FOREIGN KEY (MovieId)
        REFERENCES Movie(IdM) ON UPDATE CASCADE ON DELETE NO ACTION,
    CONSTRAINT FK_MovieTag_Tag FOREIGN KEY (TagId)
        REFERENCES Tag(IdT) ON UPDATE CASCADE ON DELETE NO ACTION
);
GO

CREATE TABLE WatchList (
    UserId INT NOT NULL,
    MovieId INT NOT NULL,
    CONSTRAINT PK_WatchList PRIMARY KEY (UserId, MovieId),
    CONSTRAINT FK_WatchList_Users FOREIGN KEY (UserId)
        REFERENCES Users(IdU) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_WatchList_Movie FOREIGN KEY (MovieId)
        REFERENCES Movie(IdM) ON UPDATE CASCADE ON DELETE CASCADE
);
GO

CREATE TABLE Rating (
    IdR INT IDENTITY(1,1) NOT NULL,
    UserId INT NOT NULL,
    MovieId INT NOT NULL,
    RatingValue INT NOT NULL,
    CreatedAt DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT PK_Rating PRIMARY KEY (IdR),
    CONSTRAINT UQ_User_Movie_Rating UNIQUE (UserId, MovieId), -- sme 1om da oceni
    CONSTRAINT CK_RatingValue CHECK (RatingValue BETWEEN 1 AND 10),
    CONSTRAINT FK_Rating_Users FOREIGN KEY (UserId)
        REFERENCES Users(IdU) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT FK_Rating_Movie FOREIGN KEY (MovieId)
        REFERENCES Movie(IdM) ON UPDATE CASCADE ON DELETE CASCADE
);
GO



-- TRIGGERi


-- blok novu extremnu ocenu (1,10) u zanru ako korisnik vec ima >3 ex a manje od 3 neutr (6,7,8) ocene u tom zanru
CREATE TRIGGER TR_BLOCK_EXTREME_RATING
ON Rating
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN MovieGenre mg ON mg.MovieId = i.MovieId
        WHERE i.RatingValue IN (1, 10)
        AND (
            SELECT COUNT(*)
            FROM Rating r
            JOIN MovieGenre mg2 ON mg2.MovieId = r.MovieId
            WHERE r.UserId = i.UserId
              AND mg2.GenreId = mg.GenreId
              AND r.RatingValue IN (1, 10)
              AND r.IdR <> i.IdR
        ) > 3
        AND (
            SELECT COUNT(*)
            FROM Rating r
            JOIN MovieGenre mg2 ON mg2.MovieId = r.MovieId
            WHERE r.UserId = i.UserId
              AND mg2.GenreId = mg.GenreId
              AND r.RatingValue IN (6, 7, 8)
        ) < 3
    )
    BEGIN
        RAISERROR('Blokirano - lose ponasanje - previse extremnih ocena bez dovoljno neutralnih ocena u ovom zanru', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END
END
GO


-- status filma rising>falling>classic>trending (tr je ako je dobio u top 3 max ocena, rising je ocena vise od prosecne fall obrnuto, classic je vise od tri ocene i prosek>=8)
CREATE TRIGGER TR_UPDATE_MOVIE_TREND
ON Rating
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @ThirdBest INT; -- treca najveca vrednost novih ocena (7 ako npr 10,9,7)

    SELECT @ThirdBest = MIN(NewRatings30days)
    FROM (
        SELECT DISTINCT TOP 3 NewRatings30days
        FROM (
            SELECT COUNT(*) AS NewRatings30days
            FROM Rating
            WHERE CreatedAt >= DATEADD(DAY, -30, GETDATE())
            GROUP BY MovieId
        ) Counts
        WHERE NewRatings30days > 0
        ORDER BY NewRatings30days DESC
    ) Top3;

    ;WITH Stats AS (
        SELECT
            m.IdM AS MovieId,
            (SELECT AVG(CAST(RatingValue AS DECIMAL(10,3))) FROM Rating WHERE MovieId = m.IdM) AS AvgRating,
            (SELECT COUNT(*) FROM Rating WHERE MovieId = m.IdM) AS RatingCount,
            (SELECT AVG(CAST(RatingValue AS DECIMAL(10,3)))
                FROM (
                    SELECT TOP 5 RatingValue
                    FROM Rating
                    WHERE MovieId = m.IdM
                    ORDER BY CreatedAt DESC
                ) AS Last5
            ) AS AvgLast5,
            (SELECT COUNT(*) FROM Rating
                WHERE MovieId = m.IdM AND CreatedAt >= DATEADD(DAY, -30, GETDATE())
            ) AS NewRatings30days
        FROM Movie m
    )
    UPDATE m
    SET m.Status =
        CASE
            WHEN s.RatingCount > 0 AND s.AvgLast5 >= s.AvgRating + 1              THEN 'Rising'
            WHEN s.RatingCount > 0 AND s.AvgLast5 <= s.AvgRating - 1              THEN 'Falling'
            WHEN s.RatingCount >= 3 AND s.AvgRating >= 8                          THEN 'Classic'
            WHEN s.NewRatings30days > 0 AND s.NewRatings30days >= @ThirdBest      THEN 'Trending'
            ELSE NULL
        END
    FROM Movie m
    JOIN Stats s ON s.MovieId = m.IdM;
END
GO


-- PROCEDURE


-- callable posle upisa ocene - fav zanr avg ocena>=8 - nagrada za ocenjivanje slabije ocenjenog posle 10 ocena
CREATE PROCEDURE SP_REWARD_USER_CHECK_
    @UserId INT,
    @MovieId INT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @RatedCount INT;
    SELECT @RatedCount = COUNT(*) FROM Rating WHERE UserId = @UserId;

    IF @RatedCount < 10
        RETURN;

    IF EXISTS (
        SELECT 1
        FROM MovieGenre mg
        WHERE mg.MovieId = @MovieId
        AND (
            SELECT AVG(CAST(r.RatingValue AS DECIMAL(10,3)))
            FROM Rating r
            JOIN MovieGenre mg2 ON mg2.MovieId = r.MovieId
            WHERE r.UserId = @UserId AND mg2.GenreId = mg.GenreId
        ) >= 8
        AND ISNULL((
            SELECT AVG(CAST(RatingValue AS DECIMAL(10,3)))
            FROM Rating
            WHERE MovieId = @MovieId AND UserId <> @UserId
        ), 0) < 6
    )
    BEGIN
        UPDATE Users 
        SET RewardCount = RewardCount + 1 
        WHERE IdU = @UserId;
    END
END
GO



-- INICIJALNO PUNJENJE PODACIMA


USE bm220465;
GO

-- 1. KORISNICI
-- Milica: 10 ocena, 11 razlicitih tagova -> status "radoznao"
-- Aca: 10 ocena, 9 razlicitih tagova -> status "fokusiran"
-- Ognjen: manje od 10 ocena -> status "nedefinisan"
-- TestUser: koristi se za demo TR_BLOCK_EXTREME_RATING
-- Jovana, Nikola, Teodora, Stefan, Filip, Sara: dodati da bi Rising/Falling
INSERT INTO Users (Username, RewardCount) VALUES 
('Milica', 0),
('Aca', 0),
('Ognjen', 0),
('TestUser', 0),
('Jovana', 0),
('Nikola', 0),
('Teodora', 0),
('Stefan', 0),
('Filip', 0),
('Sara', 0);

-- 2. ZANROVI
INSERT INTO Genre (Name) VALUES 
('Sci-Fi'),
('Action'),
('Drama'),
('Crime'),
('Comedy');

-- 3. TAGOVI
INSERT INTO Tag (Name) VALUES 
('mind-bending'),    -- Tag 1
('space'),           -- Tag 2
('cyberpunk'),       -- Tag 3
('time-travel'),     -- Tag 4
('mafia'),           -- Tag 5
('detective'),       -- Tag 6
('superhero'),       -- Tag 7
('dystopia'),        -- Tag 8
('ai'),              -- Tag 9
('philosophical'),   -- Tag 10
('dark-comedy');     -- Tag 11

-- 4. FILMOVI
INSERT INTO Movie (Title, Director, Status) VALUES 
('Inception', 'Christopher Nolan', NULL),        -- IdM = 1
('The Matrix', 'Lana & Lilly Wachowski', NULL),  -- IdM = 2  -> ocekivano: Trending
('Interstellar', 'Christopher Nolan', NULL),     -- IdM = 3  -> ocekivano: Classic
('The Godfather', 'Francis Ford Coppola', NULL), -- IdM = 4  -> ocekivano: Trending
('Pulp Fiction', 'Quentin Tarantino', NULL),     -- IdM = 5
('The Dark Knight', 'Christopher Nolan', NULL),  -- IdM = 6
('Blade Runner 2049', 'Denis Villeneuve', NULL), -- IdM = 7
('Fight Club', 'David Fincher', NULL),           -- IdM = 8
('Oppenheimer', 'Christopher Nolan', NULL),      -- IdM = 9
('Tenet', 'Christopher Nolan', NULL),            -- IdM = 10 -> ocekivano: Trending
('Slabi Film', 'Nepoznat Reziser', NULL);        -- IdM = 11 (za SP_REWARD test)

-- 5. POVEZIVANJE FILMOVA I ZANROVA (MovieGenre)
INSERT INTO MovieGenre (MovieId, GenreId) VALUES 
(1, 1), (1, 2),
(2, 1), (2, 2),
(3, 1), (3, 3),
(4, 4), (4, 3),
(5, 4), (5, 5),
(6, 2), (6, 4),
(7, 1), (7, 3),
(8, 3),
(9, 3),
(10, 1), (10, 2),
(11, 1);

-- 6. POVEZIVANJE FILMOVA I TAGOVA (MovieTag)
INSERT INTO MovieTag (MovieId, TagId) VALUES 
(1, 1), (1, 4),
(2, 1), (2, 3), (2, 9),
(3, 2), (3, 10),
(4, 5),
(5, 5), (5, 11),
(6, 7), (6, 6),
(7, 3), (7, 8), (7, 9),
(8, 10),
(9, 10),
(10, 1), (10, 4),
(11, 8);

-- 7. WATCHLIST
INSERT INTO WatchList (UserId, MovieId) VALUES 
(1, 7), -- Milica ima Blade Runner 2049 u listi (namerno ga NE ocenjuje)
(1, 8), -- Milica ima Fight Club u listi (ipak je i ocenjuje - pazi na ovu nekonzistentnost)
(2, 1); -- Aca ima Inception u listi

-- 8. OCENE (Rating) - sa variranjem datuma

-- Ocene drugih korisnika za "Slabi Film" (IdM 11) -> drze mu globalni prosek ispod 6
-- (stare ocene, van 30-dnevnog prozora da ne ulaze u Trending racunicu)
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES 
(2, 11, 4, DATEADD(DAY, -60, GETDATE())),
(3, 11, 5, DATEADD(DAY, -55, GETDATE()));

-- Interstellar (movie 3) -> Classic (avg 9.5, count 4), sve STARE ocene (van 30d)
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES
(1, 3, 9, DATEADD(DAY, -80, GETDATE())),
(2, 3, 9, DATEADD(DAY, -75, GETDATE())),
(3, 3, 10, DATEADD(DAY, -70, GETDATE()));

-- The Matrix (movie 2) -> Trending (3 nove ocene u poslednjih 30 dana)
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES
(1, 2, 10, DATEADD(DAY, -20, GETDATE())),
(2, 2, 8, DATEADD(DAY, -22, GETDATE())),
(3, 2, 10, DATEADD(DAY, -15, GETDATE()));

-- The Godfather (movie 4) -> Trending (1 nova ocena, dovoljno da udje u top 3 praga)
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES
(1, 4, 8, DATEADD(DAY, -70, GETDATE())),
(2, 4, 10, DATEADD(DAY, -65, GETDATE())),
(3, 4, 7, DATEADD(DAY, -5, GETDATE()));

-- Tenet (movie 10) -> Trending (2 nove ocene)
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES
(1, 10, 7, DATEADD(DAY, -12, GETDATE())),
(2, 10, 8, DATEADD(DAY, -8, GETDATE()));

-- Ostali filmovi - filler, stare ocene, ne uticu na trend racunicu
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES
(1, 1, 9, DATEADD(DAY, -70, GETDATE())),   -- Milica - Inception
(1, 5, 7, DATEADD(DAY, -90, GETDATE())),   -- Milica - Pulp Fiction (neutralna)
(1, 6, 8, DATEADD(DAY, -85, GETDATE())),   -- Milica - The Dark Knight
(1, 8, 8, DATEADD(DAY, -95, GETDATE())),   -- Milica - Fight Club (deo "stare" grupe za Falling demo)
(1, 9, 5, DATEADD(DAY, -100, GETDATE())),  -- Milica - Oppenheimer (deo "stare" grupe za Rising demo)
(2, 5, 9, DATEADD(DAY, -90, GETDATE())),   -- Aca - Pulp Fiction
(2, 7, 8, DATEADD(DAY, -95, GETDATE())),   -- Aca - Blade Runner 2049
(2, 8, 9, DATEADD(DAY, -100, GETDATE())),  -- Aca - Fight Club (deo "stare" grupe za Falling demo)
(2, 9, 4, DATEADD(DAY, -105, GETDATE())),  -- Aca - Oppenheimer (deo "stare" grupe za Rising demo)
(2, 1, 7, DATEADD(DAY, -60, GETDATE()));   -- Aca - Inception (ZAMENA za The Dark Knight, radi "fokusiran")

-- Ognjen (UserId = 3) - jos 2 ocene da ostane ispod 10 (status "nedefinisan")
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES 
(3, 1, 10, DATEADD(DAY, -50, GETDATE())),
(3, 6, 9, DATEADD(DAY, -45, GETDATE()));

-- Milica - 10. ocena (Slabi Film), da dostigne 10 ocena / 11 tagova ("radoznao")
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES
(1, 11, 8, DATEADD(DAY, -3, GETDATE()));

-- U pravoj aplikaciji ovo bi Java kod pozvao automatski posle addRating().
-- Ovde ga zovemo rucno jer je ovo cist SQL insert.
EXEC SP_REWARD_USER_CHECK_ @UserId = 1, @MovieId = 11;
GO


-- 10. DEMO: FALLING (Fight Club, movie 8)
-- "Stare" ocene su vec upisane gore (Milica=8, Aca=9).
-- Dodajemo Ognjena u istu staru grupu, pa 5 NOVIJIH (ali i dalje van
-- 30-dnevnog prozora, da se ne kosi sa Trending racunicom) niskih ocena.
-- Ukupno 8 ocena: stare[8,9,8] prosek visok, poslednjih 5[3,2,4,3,2] nizak
-- -> ocekivano: Falling (poslednjih5 <= ukupanProsek - 1)
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES
(3, 8, 8, DATEADD(DAY, -90, GETDATE())),   -- Ognjen - Fight Club (stara)
(5, 8, 3, DATEADD(DAY, -45, GETDATE())),   -- Jovana
(6, 8, 2, DATEADD(DAY, -40, GETDATE())),   -- Nikola
(7, 8, 4, DATEADD(DAY, -38, GETDATE())),   -- Teodora
(8, 8, 3, DATEADD(DAY, -35, GETDATE())),   -- Stefan
(9, 8, 2, DATEADD(DAY, -32, GETDATE()));   -- Filip
GO

-- 11. DEMO: RISING (Oppenheimer, movie 9)
-- "Stare" ocene vec upisane gore (Milica=5, Aca=4).
-- Dodajemo Ognjena u staru grupu, pa 5 novijih (van 30d) visokih ocena.
-- Ukupno 8 ocena: stare[5,4,5] prosek nizak, poslednjih 5[9,8,9,8,9] visok
-- -> ocekivano: Rising (poslednjih5 >= ukupanProsek + 1)
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES
(3, 9, 5, DATEADD(DAY, -95, GETDATE())),   -- Ognjen - Oppenheimer (stara)
(5, 9, 9, DATEADD(DAY, -45, GETDATE())),   -- Jovana
(6, 9, 8, DATEADD(DAY, -40, GETDATE())),   -- Nikola
(7, 9, 9, DATEADD(DAY, -38, GETDATE())),   -- Teodora
(8, 9, 8, DATEADD(DAY, -35, GETDATE())),   -- Stefan
(10, 9, 9, DATEADD(DAY, -32, GETDATE()));  -- Sara
GO

-- 12. Dodatne ocene za raznovrsnost (stare, ne uticu na Trending)
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES
(10, 7, 7, DATEADD(DAY, -80, GETDATE())),  -- Sara - Blade Runner 2049
(9, 5, 8, DATEADD(DAY, -85, GETDATE())),   -- Filip - Pulp Fiction
(5, 6, 9, DATEADD(DAY, -90, GETDATE())),   -- Jovana - The Dark Knight
(6, 11, 3, DATEADD(DAY, -70, GETDATE())),  -- Nikola - Slabi Film (drzi mu prosek nizak)
(7, 10, 6, DATEADD(DAY, -60, GETDATE())),  -- Teodora - Tenet
(8, 3, 8, DATEADD(DAY, -60, GETDATE()));   -- Stefan - Interstellar
GO


-- 9. DEMO: TR_BLOCK_EXTREME_RATING (TestUser, zanr Sci-Fi)
-- Filmovi u Sci-Fi zanru: Inception(1), Matrix(2), Interstellar(3),
-- Blade Runner 2049(7), Tenet(10), Slabi Film(11)

-- Korak 1: 4 ekstremne ocene, 0 neutralnih -> DOZVOLJENO (uslov je STROGO VISE od 3)
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES
(4, 1, 10, DATEADD(DAY, -60, GETDATE())),
(4, 2, 1, DATEADD(DAY, -55, GETDATE())),
(4, 3, 10, DATEADD(DAY, -50, GETDATE())),
(4, 7, 1, DATEADD(DAY, -45, GETDATE()));
GO

-- Korak 2: 5. ekstremna ocena u Sci-Fi zanru -> OCEKIVANA GRESKA (pokreni odvojeno!)
INSERT INTO Rating (UserId, MovieId, RatingValue, CreatedAt) VALUES
(4, 10, 10, DATEADD(DAY, -5, GETDATE()));
GO
-- Proveri: SELECT COUNT(*) FROM Rating WHERE UserId = 4;  -- treba da ostane 4, ne 5

-- EXEC SP_REWARD_USER_CHECK_ @UserId = 1, @MovieId = 11;


-- PRAZNJENJE


USE bm220465;
GO

DELETE FROM WatchList;
DELETE FROM Rating;
DELETE FROM MovieTag;
DELETE FROM MovieGenre;
DELETE FROM Movie;
DELETE FROM Genre;
DELETE FROM Tag;
DELETE FROM Users;
GO

DBCC CHECKIDENT ('Movie', RESEED, 0);
DBCC CHECKIDENT ('Genre', RESEED, 0);
DBCC CHECKIDENT ('Tag', RESEED, 0);
DBCC CHECKIDENT ('Users', RESEED, 0);
DBCC CHECKIDENT ('Rating', RESEED, 0);
GO
