package student;

import rs.ac.bg.etf.sab.operations.GenresOperations;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class bm220465_GenresOperations implements GenresOperations {

    @Override
    public Integer addGenre(String name){
        try(
                Connection conn = DB.getInstance().getConnection();
                PreparedStatement stm = conn.prepareStatement("INSERT INTO Genre(Name) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
                ){
            stm.setString(1,name);
            int affected = stm.executeUpdate();
            if(affected==0)return null;
            try(ResultSet keys = stm.getGeneratedKeys()){
                if(keys.next()){
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean doesGenreExist(String name){
        //Checks whether a genre with the given name exists.
        try(
                Connection conn = DB.getInstance().getConnection();
                PreparedStatement stm = conn.prepareStatement("SELECT COUNT(*) FROM Genre WHERE Name=?");
        ){
            stm.setString(1,name);
            try(ResultSet rs = stm.executeQuery();){
                if(rs.next()){
                    return rs.getInt(1)>0;
                }
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Integer> getAllGenreIds(){
        //Returns IDs of all genres.
        List<Integer> lista = new ArrayList<Integer>();
        try(
                Connection conn = DB.getInstance().getConnection();
                PreparedStatement stm = conn.prepareStatement("SELECT IdG FROM Genre ORDER BY IdG");
        ){
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                lista.add(rs.getInt(1));
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public Integer getGenreId(String name){
        //Retrieves the ID of a genre by its name.
        try(
                Connection conn = DB.getInstance().getConnection();
                PreparedStatement stm = conn.prepareStatement("SELECT IdG FROM Genre WHERE Name=?");
        ){
            stm.setString(1,name);
            ResultSet rs = stm.executeQuery();
            if(rs.next()){
                return rs.getInt(1);
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Integer removeGenre(Integer id){
        //Removes a genre by its ID.
        try(
                Connection conn = DB.getInstance().getConnection();
                PreparedStatement stm = conn.prepareStatement("DELETE FROM Genre WHERE IdG=?");
        ){
            stm.setInt(1,id);
            int affected = stm.executeUpdate();

            return affected>0 ? id : null;
        }catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Integer updateGenre(Integer id, String newName){
        //Updates the name of an existing genre.
        try(
                Connection conn = DB.getInstance().getConnection();
                PreparedStatement stm = conn.prepareStatement("UPDATE Genre SET Name=? WHERE IdG=?");
        ){
            stm.setString(1,newName);
            stm.setInt(2,id);
            int affected = stm.executeUpdate();

            return affected>0 ? id : null;
        }catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

}
