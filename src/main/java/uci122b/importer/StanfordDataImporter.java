package uci122b.importer;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class StanfordDataImporter {
    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println("Usage: java StanfordDataImporter <jdbcUrl> <dbUser> <dbPass> <mainsXml> <castsXml>");
            System.exit(1);
        }
        String jdbcUrl   = args[0];
        String dbUser    = args[1];
        String dbPass    = args[2];
        String mainsPath = args[3];
        String castsPath = args[4];

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
            conn.setAutoCommit(false);
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = f.newDocumentBuilder();

            Document moviesDoc = builder.parse(new File(mainsPath));
            moviesDoc.getDocumentElement().normalize();
            parseMovies(conn, moviesDoc);

            Document castsDoc = builder.parse(new File(castsPath));
            castsDoc.getDocumentElement().normalize();
            parseCasts(conn, castsDoc);

            conn.commit();
            System.out.println("Import finished without fatal errors.");
        }
    }

    private static void parseMovies(Connection conn, Document doc) throws SQLException {
        NodeList dirList = doc.getElementsByTagName("directorfilms");
        if (dirList.getLength() == 0) {
            System.err.println("No <directorfilms> found in mains XML—check your path!");
            return;
        }

        PreparedStatement insertMovie = conn.prepareStatement(
                "INSERT IGNORE INTO movies(id,title,year,director) VALUES(?,?,?,?)"
        );
        PreparedStatement insertGenreLink = conn.prepareStatement(
                "INSERT IGNORE INTO genres_in_movies(genreId,movieId) VALUES(?,?)"
        );
        // cache existing genres
        Set<String> seenGenres = new HashSet<>();
        try (Statement s = conn.createStatement();
             ResultSet r = s.executeQuery("SELECT name FROM genres")) {
            while (r.next()) seenGenres.add(r.getString(1));
        }

        for (int i = 0; i < dirList.getLength(); i++) {
            Element dirEl = (Element) dirList.item(i);
            String director = getText(dirEl, "dirname", "Unknown Director");

            NodeList filmList = dirEl.getElementsByTagName("film");
            for (int j = 0; j < filmList.getLength(); j++) {
                Element filmEl = (Element) filmList.item(j);
                String mId    = filmEl.getAttribute("id");
                String title  = getText(filmEl, "t", null);
                String yearS  = getText(filmEl, "year", null);
                int year      = yearS != null ? Integer.parseInt(yearS) : 0;

                insertMovie.setString(1, mId);
                insertMovie.setString(2, title);
                insertMovie.setInt   (3, year);
                insertMovie.setString(4, director);
                insertMovie.executeUpdate();

                Element catsEl = (Element) filmEl.getElementsByTagName("cats").item(0);
                if (catsEl != null) {
                    NodeList catList = catsEl.getElementsByTagName("cat");
                    for (int k = 0; k < catList.getLength(); k++) {
                        String genre = catList.item(k).getTextContent();
                        if (!seenGenres.contains(genre)) {
                            // create new genre
                            try (PreparedStatement pg = conn.prepareStatement(
                                    "INSERT INTO genres(name) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
                                pg.setString(1, genre);
                                pg.executeUpdate();
                                try (ResultSet gk = pg.getGeneratedKeys()) {
                                    if (gk.next()) seenGenres.add(genre);
                                }
                            }
                        }
                        try (PreparedStatement gid = conn.prepareStatement(
                                "SELECT id FROM genres WHERE name=?")) {
                            gid.setString(1, genre);
                            try (ResultSet gr = gid.executeQuery()) {
                                if (gr.next()) {
                                    insertGenreLink.setInt(1, gr.getInt(1));
                                    insertGenreLink.setString(2, mId);
                                    insertGenreLink.executeUpdate();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void parseCasts(Connection conn, Document doc) throws SQLException {
        NodeList castList = doc.getElementsByTagName("m");
        if (castList.getLength() == 0) {
            System.err.println("No <m> elements in casts XML—check your path!");
            return;
        }

        PreparedStatement findStar = conn.prepareStatement(
                "SELECT id FROM stars WHERE name=?"
        );
        PreparedStatement insertStar = conn.prepareStatement(
                "INSERT INTO stars(id,name) VALUES(?,?)"
        );
        PreparedStatement linkStar  = conn.prepareStatement(
                "INSERT IGNORE INTO stars_in_movies(starId,movieId) VALUES(?,?)"
        );

        for (int i = 0; i < castList.getLength(); i++) {
            Element m = (Element) castList.item(i);
            String mId = getText(m, "f", null);
            String actor = getText(m, "a", null);

            // find or create star
            String starId = null;
            findStar.setString(1, actor);
            try (ResultSet rs = findStar.executeQuery()) {
                if (rs.next()) starId = rs.getString(1);
            }
            if (starId == null) {
                // generate new starId
                try (Statement s = conn.createStatement();
                     ResultSet mx = s.executeQuery(
                             "SELECT CONCAT('s',LPAD(CAST(SUBSTRING(MAX(id),2)+1 AS UNSIGNED),9,'0')) FROM stars"
                     )) {
                    mx.next();
                    starId = mx.getString(1);
                }
                insertStar.setString(1, starId);
                insertStar.setString(2, actor);
                insertStar.executeUpdate();
            }
            // link
            linkStar.setString(1, starId);
            linkStar.setString(2, mId);
            linkStar.executeUpdate();
        }
    }

    private static String getText(Element parent, String tag, String def) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return def;
        String txt = nl.item(0).getTextContent();
        return (txt == null || txt.trim().isEmpty()) ? def : txt.trim();
    }
}
