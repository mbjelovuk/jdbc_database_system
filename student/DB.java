package student;

import java.sql.Connection;
import java.sql.*;

public class DB {
    private static String server = "localhost";
    private static String instance_sql = "\\SQLEXPRESS";
    private static String database = "bm220465";
    private static final String URL = "jdbc:sqlserver://localhost:59731;databaseName=" + database + ";encrypt=true;trustServerCertificate=true;loginTimeout=10;";
//    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=" + database + ";encrypt=true;trustServerCertificate=true;loginTimeout=10;";


    //ako ovo ne radi trebalo bi da radi sa sa 123
    //potencijalni problemi sa sql server tcp i to
    private static final String USER = "milica";
    private static final String PASSWORD = "123";

//    private Connection connection;
    private static DB db = null;

    public static DB getInstance() {
        if(db==null){
            db = new DB();
        }
        return db;
    }

    private DB() {
        //ne moze ovako jer ofc try-w-resources zatvara conn svaki put smrc
//        try {
//            connection = DriverManager.getConnection(URL, USER, PASSWORD);
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
////            e.printStackTrace();
//        }
    }
//    public Connection getConnection() {
//        return connection;
//    }
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}


//za svaki sl
//<?xml version="1.0" encoding="UTF-8"?>
//<project xmlns="http://maven.apache.org/POM/4.0.0"
//xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
//xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
//    <modelVersion>4.0.0</modelVersion>
//
//    <groupId>org.example</groupId>
//    <artifactId>SAB_proj</artifactId>
//    <version>1.0-SNAPSHOT</version>
//
//    <properties>
//        <maven.compiler.source>21</maven.compiler.source>
//        <maven.compiler.target>21</maven.compiler.target>
//        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
//    </properties>
//
//    <dependencies>
//        <dependency>
//            <groupId>rs.ac.bg.etf.sab</groupId>
//            <artifactId>sab-test</artifactId>
//            <version>1.0</version>
//            <scope>system</scope>
//<systemPath>${project.basedir}/libs/SAB_projekat_2026_javni_test.jar</systemPath>
//        </dependency>
//        <dependency>
//            <groupId>com.microsoft.sqlserver</groupId>
//            <artifactId>mssql-jdbc</artifactId>
//            <version>12.6.1.jre11</version>
//        </dependency>
//        <dependency>
//            <groupId>junit</groupId>
//            <artifactId>junit</artifactId>
//            <version>4.12</version>
//        </dependency>
//    </dependencies>
//
//</project>
