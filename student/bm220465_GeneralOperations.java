package student;

import rs.ac.bg.etf.sab.operations.GeneralOperations;

import java.sql.*;

public class bm220465_GeneralOperations implements GeneralOperations {
    //bitan redosled log
    private static final String[] TABLES = {
            "WatchList",
            "Rating",
            "MovieTag",
            "MovieGenre",
            "Movie",
            "Genre",
            "Tag",
            "Users"
    };
    @Override
    public void eraseAll(){
        try (
                Connection conn = DB.getInstance().getConnection();
                Statement stm = conn.createStatement();)
        {
            for (String table : TABLES) {
                stm.executeUpdate("DELETE FROM " + table);
            }
            //zbog AI - truncate skripta
            stm.executeUpdate("DBCC CHECKIDENT ('Movie', RESEED, 0)");
            stm.executeUpdate("DBCC CHECKIDENT ('Genre', RESEED, 0)");
            stm.executeUpdate("DBCC CHECKIDENT ('Tag', RESEED, 0)");
            stm.executeUpdate("DBCC CHECKIDENT ('Users', RESEED, 0)");
            stm.executeUpdate("DBCC CHECKIDENT ('Rating', RESEED, 0)");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
