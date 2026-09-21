package student;

import rs.ac.bg.etf.sab.operations.RatingsOperations;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class bm220465_RatingsOperations implements RatingsOperations {

    @Override
    public boolean addRating(Integer userId, Integer movieId, Integer rating) {
        //Adds a new rating for a movie by a specific user.
        String sql = "INSERT INTO Rating (UserId, MovieId, RatingValue) VALUES (?, ?, ?)";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, movieId);
            ps.setInt(3, rating);
            ps.executeUpdate();

            //sp
            String call = "{call SP_REWARD_USER_CHECK_(?, ?)}";
            try (CallableStatement cs = conn.prepareCall(call)) {
                cs.setInt(1, userId);
                cs.setInt(2, movieId);
                cs.execute();
            }
            return true;
        } catch (SQLException e) {
            //ovde ce trigger baciti gresku
//            System.out.println("trigger threw exception");
            return false;
        }
    }

    @Override
    public List<Integer> getRatedMoviesByUser(Integer userId) {
        //Retrieves all movies rated by a specific user.
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT MovieId FROM Rating WHERE UserId = ? ORDER BY MovieId";
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
    public Integer getRating(Integer userId, Integer movieId) {
        //Retrieves the score of a rating for a specific movie by a specific user.
        String sql = "SELECT RatingValue FROM Rating WHERE UserId = ? AND MovieId = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Integer> getUsersWhoRatedMovie(Integer movieId) {
        //Retrieves all users who rated a specific movie.
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT UserId FROM Rating WHERE MovieId = ? ORDER BY UserId";
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
    public boolean removeRating(Integer userId, Integer movieId) {
        //Removes a rating for a specific movie given by a specific user.
        String sql = "DELETE FROM Rating WHERE UserId = ? AND MovieId = ?";
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
    public boolean updateRating(Integer userId, Integer movieId, Integer newRating) {
        //Updates the score of an existing rating.
        String sql = "UPDATE Rating SET RatingValue = ? WHERE UserId = ? AND MovieId = ?";
        try (Connection conn = DB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newRating);
            ps.setInt(2, userId);
            ps.setInt(3, movieId);
            int affected = ps.executeUpdate();

            return affected > 0;
        } catch (SQLException e) {
            //potencijalno iz triggera greska
            return false;
        }
    }

}
