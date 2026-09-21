package student;

import rs.ac.bg.etf.sab.operations.TagsOperations;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class bm220465_TagsOperations implements TagsOperations {

    private Integer getOrCreateTagId(Connection conn, String tagName) throws SQLException {
        //return idt ili napravi ako nema
        String select = "SELECT IdT FROM Tag WHERE Name = ?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, tagName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        String insert = "INSERT INTO Tag (Name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tagName);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return null;
    }
    @Override
    public Integer addTag(Integer movieId, String tagName) {
        //Adds a tag to the specified movie.
        try (Connection conn = DB.getInstance().getConnection()) {
            boolean ima;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Movie WHERE IdM = ?")) {
                ps.setInt(1, movieId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    ima = rs.getInt(1) > 0;
                }
            }
            if (!ima) return null;

            Integer tagId = getOrCreateTagId(conn, tagName);
            if (tagId == null) return null;

            String checkLink = "SELECT COUNT(*) FROM MovieTag WHERE MovieId = ? AND TagId = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkLink)) {
                ps.setInt(1, movieId);
                ps.setInt(2, tagId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return null; // vec ima
                    }
                }
            }

            String insertLink = "INSERT INTO MovieTag (MovieId, TagId) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertLink)) {
                ps.setInt(1, movieId);
                ps.setInt(2, tagId);
                ps.executeUpdate();
            }
            return movieId;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Integer removeTag(Integer movieId, String tagName) {
        //Removes a tag from the specified movie.
        String sql = "DELETE mt FROM MovieTag mt JOIN Tag t ON t.IdT = mt.TagId WHERE mt.MovieId = ? AND t.Name = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setString(2, tagName);
            int affected = ps.executeUpdate();
            return affected > 0 ? movieId : null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int removeAllTagsForMovie(Integer movieId) {
        //Removes all tags from the specified movie.
        String sql = "DELETE FROM MovieTag WHERE MovieId = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public boolean hasTag(Integer movieId, String tagName) {
        //Checks whether a specific (movieId, tag) association exists.
        String sql = "SELECT COUNT(*) FROM MovieTag mt JOIN Tag t ON t.IdT = mt.TagId WHERE mt.MovieId = ? AND t.Name = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setString(2, tagName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<String> getTagsForMovie(Integer movieId) {
        //Retrieves all tags for the specified movie.
        List<String> result = new ArrayList<>();
        String sql = "SELECT t.Name FROM Tag t JOIN MovieTag mt ON mt.TagId = t.IdT WHERE mt.MovieId = ? ORDER BY t.Name";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
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
    public List<Integer> getMovieIdsByTag(String tagName) {
        //Retrieves IDs of all movies that have the specified tag.
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT mt.MovieId FROM MovieTag mt JOIN Tag t ON t.IdT = mt.TagId WHERE t.Name = ? ORDER BY mt.MovieId";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tagName);
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
    public List<String> getAllTags() {
        //Retrieves a list of all distinct tags present in the system. --unique je name u bazi
        List<String> result = new ArrayList<>();
        String sql = "SELECT Name FROM Tag ORDER BY Name";
        try (Connection conn = DB.getInstance().getConnection();
             Statement stmt = conn.createStatement();) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

}
