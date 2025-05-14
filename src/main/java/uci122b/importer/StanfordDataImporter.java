
package uci122b.importer;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.sql.*;
import java.util.*;

public class StanfordDataImporter {
    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Usage: java StanfordDataImporter <jdbcUrl> <dbUser> <dbPass> <mainsXml> <castsXml>");
            System.exit(1);
        }
        String jdbcUrl   = args[0];
        String dbUser    = args[1];
        String dbPass    = args[2];
        String mainsFile = args[3];
        String castsFile = args[4];

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
            conn.setAutoCommit(true);

            // In‐memory caches to avoid repeated SELECTs (Optimization #1)
            Map<String,Integer> genreCache = loadGenreCache(conn);
            Map<String,String> fidToMovieId = new HashMap<>();

            // 1) Parse and import movies + genres
            parseMovies(conn, mainsFile, genreCache, fidToMovieId);

            // 2) Parse and import stars + stars_in_movies
            parseCasts(conn, castsFile, fidToMovieId);

            System.out.println("Import complete.");
        }
    }

    private static Map<String,Integer> loadGenreCache(Connection conn) throws SQLException {
        Map<String,Integer> cache = new HashMap<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id,name FROM genres")) {
            while (rs.next()) {
                cache.put(rs.getString("name"), rs.getInt("id"));
            }
        }
        return cache;
    }

    private static void parseMovies(Connection conn,
                                    String xmlPath,
                                    Map<String,Integer> genreCache,
                                    Map<String,String> fidToMovieId)
            throws Exception {
        // Prepare reused statements
        PreparedStatement selMovie = conn.prepareStatement(
                "SELECT id FROM movies WHERE title=? AND year=? AND director=?");
        PreparedStatement insMovie = conn.prepareStatement(
                "INSERT INTO movies(id,title,year,director) VALUES(?,?,?,?)");
        PreparedStatement selGenre = conn.prepareStatement(
                "SELECT id FROM genres WHERE name=?");
        PreparedStatement insGenre = conn.prepareStatement(
                "INSERT INTO genres(name) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
        PreparedStatement insG2M = conn.prepareStatement(
                "INSERT INTO genres_in_movies(genreId,movieId) VALUES(?,?)");
        Statement   maxMovieId = conn.createStatement();

        // DOM parsing
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.parse(new File(xmlPath));
        NodeList directors = doc.getElementsByTagName("directorfilms");
        for (int i = 0; i < directors.getLength(); i++) {
            Element df = (Element) directors.item(i);
            String director = df.getElementsByTagName("dirname").item(0).getTextContent().trim();

            NodeList films = df.getElementsByTagName("film");
            for (int j = 0; j < films.getLength(); j++) {
                Element film = (Element) films.item(j);
                String fid   = film.getAttribute("fid");
                String title = film.getElementsByTagName("t").item(0).getTextContent().trim();
                int    year  = Integer.parseInt(film.getElementsByTagName("year").item(0).getTextContent().trim());

                // 1) Check if movie exists
                selMovie.setString(1, title);
                selMovie.setInt(2, year);
                selMovie.setString(3, director);
                ResultSet rm = selMovie.executeQuery();

                String myMovieId;
                if (rm.next()) {
                    myMovieId = rm.getString(1);
                } else {
                    // 2) Generate new movie ID
                    ResultSet mx = maxMovieId.executeQuery("SELECT MAX(id) FROM movies WHERE id LIKE 'm%'");
                    mx.next();
                    String maxId = mx.getString(1);
                    int nextNum = Integer.parseInt(maxId.substring(1)) + 1;
                    myMovieId = String.format("m%09d", nextNum);

                    // 3) Insert movie
                    insMovie.setString(1, myMovieId);
                    insMovie.setString(2, title);
                    insMovie.setInt(3, year);
                    insMovie.setString(4, director);
                    insMovie.executeUpdate();
                }
                rm.close();

                // Remember mapping from file‐fid → our movieId
                fidToMovieId.put(fid, myMovieId);

                // 4) Handle genres
                NodeList cats = film.getElementsByTagName("cat");
                for (int k = 0; k < cats.getLength(); k++) {
                    String gname = cats.item(k).getTextContent().trim();
                    Integer gid = genreCache.get(gname);
                    if (gid == null) {
                        // Insert new genre
                        insGenre.setString(1, gname);
                        insGenre.executeUpdate();
                        try (ResultSet keys = insGenre.getGeneratedKeys()) {
                            keys.next();
                            gid = keys.getInt(1);
                        }
                        genreCache.put(gname, gid);
                    }
                    // Link table
                    insG2M.setInt(1, gid);
                    insG2M.setString(2, myMovieId);
                    insG2M.addBatch();
                }
                insG2M.executeBatch();
            }
        }

        // cleanup
        selMovie.close();
        insMovie.close();
        selGenre.close();
        insGenre.close();
        insG2M.close();
        maxMovieId.close();
    }

    private static void parseCasts(Connection conn,
                                   String xmlPath,
                                   Map<String,String> fidToMovieId)
            throws Exception {
        // Prepare caching & statements
        Map<String,String> starCache = new HashMap<>();
        PreparedStatement selStar    = conn.prepareStatement(
                "SELECT id FROM stars WHERE name=?");
        PreparedStatement insStar    = conn.prepareStatement(
                "INSERT INTO stars(id,name) VALUES(?,?)");
        Statement   maxStarId        = conn.createStatement();
        PreparedStatement insS2M     = conn.prepareStatement(
                "INSERT INTO stars_in_movies(starId,movieId) VALUES(?,?)");

        // DOM parse
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.parse(new File(xmlPath));
        NodeList ms = doc.getElementsByTagName("m");
        for (int i = 0; i < ms.getLength(); i++) {
            Element m = (Element) ms.item(i);
            String fid      = m.getElementsByTagName("f").item(0).getTextContent().trim();
            String starName = m.getElementsByTagName("a").item(0).getTextContent().trim();

            String movieId = fidToMovieId.get(fid);
            if (movieId == null) {
                continue;
            }

            String starId = starCache.get(starName);
            if (starId == null) {
                selStar.setString(1, starName);
                ResultSet rs = selStar.executeQuery();
                if (rs.next()) {
                    starId = rs.getString(1);
                } else {
                    // generate new starId
                    ResultSet mx = maxStarId.executeQuery("SELECT MAX(id) FROM stars WHERE id LIKE 's%'");
                    mx.next();
                    int nextNum = Integer.parseInt(mx.getString(1).substring(1)) + 1;
                    starId = String.format("s%09d", nextNum);
                    insStar.setString(1, starId);
                    insStar.setString(2, starName);
                    insStar.executeUpdate();
                }
                rs.close();
                starCache.put(starName, starId);
            }

            insS2M.setString(1, starId);
            insS2M.setString(2, movieId);
            insS2M.addBatch();
        }
        insS2M.executeBatch();

        selStar.close();
        insStar.close();
        maxStarId.close();
        insS2M.close();
    }
}

