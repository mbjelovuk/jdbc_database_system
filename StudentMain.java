package rs.ac.bg.etf.sab;

import rs.ac.bg.etf.sab.operations.*;
import rs.ac.bg.etf.sab.tests.TestHandler;
import rs.ac.bg.etf.sab.tests.TestRunner;
import student.*;

public class StudentMain {
    public static void main(String[] args) throws Exception {
        GeneralOperations generalOperations = new bm220465_GeneralOperations();
        GenresOperations genresOperations = new bm220465_GenresOperations();
        MoviesOperations moviesOperations = new bm220465_MoviesOperations();
        RatingsOperations ratingsOperation = new bm220465_RatingsOperations();
        TagsOperations tagsOperations = new bm220465_TagsOperations();
        UsersOperations usersOperations = new bm220465_UsersOperations();
        WatchlistsOperations watchlistsOperations = new bm220465_WatchlistsOperations();

        TestHandler.createInstance(
                genresOperations,
                moviesOperations,
                ratingsOperation,
                tagsOperations,
                usersOperations,
                watchlistsOperations,
                generalOperations);
        TestRunner.runTests();
    }
}