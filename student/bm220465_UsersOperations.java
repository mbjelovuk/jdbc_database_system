package student;

import rs.ac.bg.etf.sab.operations.UsersOperations;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class bm220465_UsersOperations implements UsersOperations {

    @Override
    public Integer addUser(String username) {
        //Adds a new user with the given username.
        if (doesUserExist(username)) {
            return null;
        }
        String sql = "INSERT INTO Users (Username, RewardCount) VALUES (?, 0)";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            int affected = ps.executeUpdate();
            if (affected == 0) return null;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Integer updateUser(Integer id, String newUsername) {
        //Updates the username of an existing user identified by their unique identifier.
        String sql = "UPDATE Users SET Username = ? WHERE IdU = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setInt(2, id);
            int affected = ps.executeUpdate();

            return affected > 0 ? id : null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Integer removeUser(Integer id) {
        //Removes a user with the specified unique identifier.
        String sql = "DELETE FROM Users WHERE IdU = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();

            return affected > 0 ? id : null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean doesUserExist(String username) {
        //Checks whether a user with the specified username exists.
        String sql = "SELECT IdU FROM Users WHERE Username = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Integer getUserId(String username) {
        //Retrieves the unique identifier (ID) of a user based on their username.
        String sql = "SELECT IdU FROM Users WHERE Username = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Integer> getAllUserIds() {
        //Retrieves a list of unique identifiers (IDs) for all users.
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT IdU FROM Users ORDER BY IdU";
        try (Connection conn = DB.getInstance().getConnection();
             Statement stmt = conn.createStatement();) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                result.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public List<Integer> getRecommendedMoviesFromFavoriteGenres(Integer userId) {
        //Retrieves recommended movies for the given user based on their favorite genres.
        List<Integer> result = new ArrayList<>();
        String sql =
                "WITH FavoriteGenres AS ( " +
                        "    SELECT mg.GenreId " +
                        "    FROM Rating r " +
                        "    JOIN MovieGenre mg ON mg.MovieId = r.MovieId " +
                        "    WHERE r.UserId = ? " +
                        "    GROUP BY mg.GenreId " +
                        "    HAVING AVG(CAST(r.RatingValue AS DECIMAL(10,3))) >= 8 " +
                        "), " +
                        "Candidates AS ( " +
                        "    SELECT DISTINCT m.IdM " +
                        "    FROM Movie m " +
                        "    JOIN MovieGenre mg ON mg.MovieId = m.IdM " +
                        "    WHERE mg.GenreId IN (SELECT GenreId FROM FavoriteGenres) " +
                        "      AND m.IdM NOT IN (SELECT MovieId FROM Rating WHERE UserId = ?) " +
                        "      AND m.IdM NOT IN (SELECT MovieId FROM WatchList WHERE UserId = ?) " +
                        "), " +
                        "Stats AS ( " +
                        "    SELECT c.IdM, " +
                        "           (SELECT COUNT(*) FROM Rating WHERE MovieId = c.IdM) AS RatingCount, " +
                        "           (SELECT AVG(CAST(RatingValue AS DECIMAL(10,3))) FROM Rating WHERE MovieId = c.IdM) AS AvgRating " +
                        "    FROM Candidates c " +
                        ") " +
                        "SELECT IdM FROM Stats " +
                        "WHERE (RatingCount >= 4 AND AvgRating >= 7.5) " +
                        "   OR (RatingCount < 4 AND AvgRating >= 9) " +
                        "ORDER BY AvgRating DESC, IdM ASC";

        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public Integer getRewards(Integer userId) {
        //Returns the number of rewards earned by the specified user.
        String sql = "SELECT RewardCount FROM Users WHERE IdU = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<String> getThematicSpecializations(Integer userId) {
        //Retrieves the list of thematic specializations (tags) for the specified user.
        List<String> result = new ArrayList<>();
        String sql =
                "SELECT t.Name FROM Rating r " +
                        "JOIN MovieTag mt ON mt.MovieId = r.MovieId " +
                        "JOIN Tag t ON t.IdT = mt.TagId " +
                        "WHERE r.UserId = ? AND r.RatingValue >= 8 " +
                        "GROUP BY t.Name " +
                        "HAVING COUNT(*) >= 2 " +
                        "ORDER BY t.Name";

        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String getUserDescription(Integer userId) {
        //Retrieves a descriptive status of the specified user based on their rating behavior.
        String rating = "SELECT COUNT(*) FROM Rating WHERE UserId = ?";
        String tags = "SELECT COUNT(DISTINCT t.IdT) FROM Rating r JOIN MovieTag mt ON mt.MovieId = r.MovieId JOIN Tag t ON t.IdT = mt.TagId WHERE r.UserId = ?";
        try (Connection conn = DB.getInstance().getConnection()) {
            int ratingcnt = 0;
            try (PreparedStatement ps = conn.prepareStatement(rating)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) ratingcnt = rs.getInt(1);
                }
            }
            if (ratingcnt < 10) {
                return "undefined";
            }
            int tagcnt = 0;
            try (PreparedStatement ps = conn.prepareStatement(tags)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) tagcnt = rs.getInt(1);
                }
            }
            return tagcnt >= 10 ? "curious" : "focused";
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

}
