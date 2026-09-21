package student;

import rs.ac.bg.etf.sab.operations.WatchlistsOperations;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class bm220465_WatchlistsOperations implements WatchlistsOperations {

    @Override
    public boolean addMovieToWatchlist(Integer userId, Integer movieId) {
        //Adds a movie to a user's watchlist.
        if (isMovieInWatchlist(userId, movieId)) {
            return false;
        }
        String sql = "INSERT INTO WatchList (UserId, MovieId) VALUES (?, ?)";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, movieId);
            int affected = ps.executeUpdate();

            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean removeMovieFromWatchlist(Integer userId, Integer movieId) {
        //Removes a movie from a user's watchlist.
        String sql = "DELETE FROM WatchList WHERE UserId = ? AND MovieId = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, movieId);
            int affected = ps.executeUpdate();

            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isMovieInWatchlist(Integer userId, Integer movieId) {
        //Checks if a movie is in a user's watchlist.
        String sql = "SELECT COUNT(*) FROM WatchList WHERE UserId = ? AND MovieId = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Integer> getMoviesInWatchlist(Integer userId) {
        //Retrieves all movies in a user's watchlist.
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT MovieId FROM WatchList WHERE UserId = ? ORDER BY MovieId";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
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
    public List<Integer> getUsersWithMovieInWatchlist(Integer movieId) {
        //Retrieves all users who have a specific movie in their watchlist.
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT UserId FROM WatchList WHERE MovieId = ? ORDER BY UserId";
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

}
