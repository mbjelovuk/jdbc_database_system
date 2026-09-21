package student;

import rs.ac.bg.etf.sab.operations.MoviesOperations;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import student.bm220465_TagsOperations;

public class bm220465_MoviesOperations implements MoviesOperations {
//    private final bm220465_TagsOperations tagsOps = new bm220465_TagsOperations();

    private boolean existsById(Connection conn, String table, String idColumn, Integer id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + idColumn + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public Integer addGenreToMovie(Integer movieId, Integer genreId){
        //Adds a new genre to the specified movie.
        try(
                Connection conn = DB.getInstance().getConnection();){
            if (!existsById(conn, "Movie", "IdM", movieId)) return null;
            if (!existsById(conn, "Genre", "IdG", genreId)) return null;

            String checkSql = "SELECT COUNT(*) FROM MovieGenre WHERE MovieId = ? AND GenreId = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, movieId);
                ps.setInt(2, genreId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return null; // vec postoji
                    }
                }
            }
            PreparedStatement stm = conn.prepareStatement("INSERT INTO MovieGenre (MovieId,GenreId) VALUES (?,?)");
            stm.setInt(1,movieId);
            stm.setInt(2,genreId);
            int affected = stm.executeUpdate();

            return affected>0 ? movieId : null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Integer addMovie(String title, Integer genreId, String director){
        //Adds a new movie.
        try(Connection conn = DB.getInstance().getConnection();){
            Integer idm = null;
            try(PreparedStatement stm = conn.prepareStatement("INSERT INTO Movie (Title,Director) VALUES (?,?)",Statement.RETURN_GENERATED_KEYS);){
                stm.setString(1,title);
                stm.setString(2,director);
                int affected = stm.executeUpdate();
                if(affected==0) return null;
                try(ResultSet keys = stm.getGeneratedKeys()){
                    if(keys.next()) idm = keys.getInt(1);
                }
            }
            if(idm==null)return null;
            if(genreId!=null){
                try(PreparedStatement ps = conn.prepareStatement("INSERT INTO MovieGenre (MovieId,GenreId) VALUES (?,?)")){
                    ps.setInt(1,idm);
                    ps.setInt(2,genreId);
                    ps.executeUpdate();
                }
            }
            return idm;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Integer> getAllMovieIds() {
        //Retrieves IDs of all movies.
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT IdM FROM Movie ORDER BY IdM";
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
    public List<Integer> getGenreIdsForMovie(Integer movieId) {
        //Retrieves IDs of genres associated with a given movie.
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT GenreId FROM MovieGenre WHERE MovieId = ? ORDER BY GenreId";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
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
    public List<Integer> getMovieIds(String title, String director) {
        //Retrieves a list of IDs for movies that match the given title and director.
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT IdM FROM Movie WHERE Title = COALESCE(?, Title) AND Director = COALESCE(?, Director) ORDER BY IdM";

        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, director);

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
    public List<Integer> getMovieIdsByDirector(String director) {
        //Retrieves a list of IDs for movies directed by the specified director.
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT IdM FROM Movie WHERE Director = ? ORDER BY IdM";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, director);
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
    public List<Integer> getMovieIdsByGenre(Integer genreId) {
        //Retrieves IDs of movies that belong to a given genre.
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT MovieId FROM MovieGenre WHERE GenreId = ? ORDER BY MovieId";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, genreId);
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
    public String getMovieTrend(Integer movieId) {
        //Retrieves the current trend status of a movie.
        String sql = "SELECT Status FROM Movie WHERE IdM = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Integer removeGenreFromMovie(Integer movieId, Integer genreId) {
        //Removes a genre from the specified movie.
        String sql = "DELETE FROM MovieGenre WHERE MovieId = ? AND GenreId = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, genreId);
            int affected = ps.executeUpdate();

            return affected > 0 ? movieId : null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Integer removeMovie(Integer movieId) {
        //Removes a movie by its ID.

//        tagsOps.removeAllTagsForMovie(movieId);

        try (Connection conn = DB.getInstance().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM MovieTag WHERE MovieId = ?")) {
                ps.setInt(1, movieId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Movie WHERE IdM = ?")) {
                ps.setInt(1, movieId);
                int affected = ps.executeUpdate();
                return affected > 0 ? movieId : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Integer updateMovieDirector(Integer movieId, String newDirector) {
        //Updates the director of a movie.
        String sql = "UPDATE Movie SET Director = ? WHERE IdM = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newDirector);
            ps.setInt(2, movieId);
            int affected = ps.executeUpdate();

            return affected > 0 ? movieId : null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Integer updateMovieTitle(Integer movieId, String newTitle) {
        //Updates the title of a movie.
        String sql = "UPDATE Movie SET Title = ? WHERE IdM = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newTitle);
            ps.setInt(2, movieId);
            int affected = ps.executeUpdate();

            return affected > 0 ? movieId : null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

}
