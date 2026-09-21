USE bm220465;
GO

IF OBJECT_ID('TR_BLOCK_EXTREME_RATING', 'TR') IS NOT NULL DROP TRIGGER TR_BLOCK_EXTREME_RATING;
IF OBJECT_ID('TR_UPDATE_MOVIE_TREND', 'TR') IS NOT NULL DROP TRIGGER TR_UPDATE_MOVIE_TREND;
IF OBJECT_ID('SP_REWARD_USER_CHECK_', 'P') IS NOT NULL DROP PROCEDURE SP_REWARD_USER_CHECK_;
GO


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


-- status filma rising>falling>classic>trending (tr je ako je dobio u top 10% po br novih ocena, rising je ocena vise od prosecne fall obrnuto, classic je vise od tri ocene i prosek>=8)
CREATE TRIGGER TR_UPDATE_MOVIE_TREND
ON Rating
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

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
    ),
    TrendingSet AS (
        SELECT TOP 10 PERCENT WITH TIES MovieId
        FROM Stats
        WHERE NewRatings30days > 0
        ORDER BY NewRatings30days DESC
    )
    UPDATE m
    SET m.Status =
        CASE
            WHEN s.RatingCount > 0 AND s.AvgLast5 >= s.AvgRating + 1   THEN 'Rising'
            WHEN s.RatingCount > 0 AND s.AvgLast5 <= s.AvgRating - 1   THEN 'Falling'
            WHEN s.RatingCount >= 3 AND s.AvgRating >= 8               THEN 'Classic'
            WHEN s.MovieId IN (SELECT MovieId FROM TrendingSet)       THEN 'Trending'
            ELSE NULL
        END
    FROM Movie m
    JOIN Stats s ON s.MovieId = m.IdM;
END
GO


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
