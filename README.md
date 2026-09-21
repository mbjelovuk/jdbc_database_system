# ssms_jdbc_database_system

# Movie Recommendation & Rating System

**Course:** Softverski alati baza podataka  (Software Tools for Databases) 
**Year:** 2025/2026  

MS SQL based system for movies, users, ratings, genres, tags and watchlists.

### Main features
- Extreme rating blocking (`TR_BLOCK_EXTREME`)
- Automatic movie trend status (`TR_UPDATE_MOVIE_TREND`)
- User rewards for rating underrated movies (`SP_REWARD_USER_CHECK_`)
- Recommendations from favorite genres
- Thematic specializations & user description (curious / focused / undefined)

### Entities
| Table       | Key columns                  |
|-------------|------------------------------|
| Users       | IdU, Username, RewardCount   |
| Movie       | IdM, Title, Director, Status |
| Genre       | IdG, Name                    |
| Tag         | IdT, Name                    |
| Rating      | UserId, MovieId, RatingValue |
| WatchList   | UserId, MovieId              |
| MovieGenre  | MovieId, GenreId             |
| MovieTag    | MovieId, TagId               |

### Implementation classes
Package `student`:
- `bm220465_GeneralOperations`
- `bm220465_UsersOperations`
- `bm220465_MoviesOperations`
- `bm220465_GenresOperations`
- `bm220465_TagsOperations`
- `bm220465_RatingsOperations`
- `bm220465_WatchlistsOperations`
- `DB` (connection helper)

### Technical notes
- JDBC + MS SQL Server
- Primary keys are `IDENTITY`
- Decimals use `DECIMAL(10,3)`
- Connection is handled through singleton `DB.getInstance()`

### Required database objects
| Type      | Name                       | Purpose                          |
|-----------|----------------------------|----------------------------------|
| Trigger   | `TR_BLOCK_EXTREME`         | Blocks extreme ratings           |
| Trigger   | `TR_UPDATE_MOVIE_TREND`    | Updates movie trend status       |
| Procedure | `SP_REWARD_USER_CHECK_`    | Awards rewards                   |
