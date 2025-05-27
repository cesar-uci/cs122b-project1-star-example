package uci122b;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "SingleMovieServlet", urlPatterns = {"/single-movie"})
public class SingleMovieServlet extends HttpServlet {
    // No DataSource member variable here, it's looked up in doGet

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String movieId = req.getParameter("movieId");

        if (movieId == null || movieId.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing movieId parameter");
            return;
        }

        String rawQS = req.getQueryString();
        String backQS = "";
        if (rawQS != null) {
            backQS = rawQS.replaceAll("&?movieId=[^&]*", "").replaceAll("^&", "");
            if (backQS.startsWith("movieId")) {
                backQS = "";
            }
        }
        req.setAttribute("backQS", backQS);

        DataSource ds_slave; // Define DataSource locally in method
        try {
            Context ctx = new InitialContext();
            // Use the SLAVE for reading single movie details
            ds_slave = (DataSource) ctx.lookup("java:comp/env/jdbc/moviedb_slave");
            System.out.println("SingleMovieServlet: Using jdbc/moviedb_slave");
        } catch (Exception e) {
            System.err.println("SingleMovieServlet: Failed to lookup DataSource (jdbc/moviedb_slave): " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
            return;
        }

        try (Connection conn = ds_slave.getConnection()) { // Use the looked-up ds_slave
            String msql = "SELECT title, year, director FROM movies WHERE id=?";
            boolean movieFound = false;
            try (PreparedStatement ms = conn.prepareStatement(msql)) {
                ms.setString(1, movieId);
                try (ResultSet mrs = ms.executeQuery()) {
                    if (mrs.next()) {
                        req.setAttribute("title", mrs.getString("title"));
                        req.setAttribute("year", mrs.getInt("year"));
                        req.setAttribute("director", mrs.getString("director"));
                        movieFound = true;
                    }
                }
            }

            if (!movieFound) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Movie with ID " + movieId + " not found.");
                return;
            }

            String rsql = "SELECT rating FROM ratings WHERE movieId=?";
            try (PreparedStatement rs2 = conn.prepareStatement(rsql)) {
                rs2.setString(1, movieId);
                try (ResultSet rrs = rs2.executeQuery()) {
                    if (rrs.next()) {
                        req.setAttribute("rating", rrs.getFloat("rating"));
                    } else {
                        req.setAttribute("rating", 0.0f);
                    }
                }
            }

            List<String> genres = new ArrayList<>();
            String gsql = "SELECT g.name FROM genres g "
                    + "JOIN genres_in_movies gm ON g.id=gm.genreId "
                    + "WHERE gm.movieId=?";
            try (PreparedStatement gs = conn.prepareStatement(gsql)) {
                gs.setString(1, movieId);
                try (ResultSet grs = gs.executeQuery()) {
                    while (grs.next()) {
                        genres.add(grs.getString("name"));
                    }
                }
            }
            req.setAttribute("genres", genres);

            List<Star> stars = new ArrayList<>();
            String stsql = "SELECT s.id, s.name, " +
                    "(SELECT count(*) FROM stars_in_movies sim2 WHERE sim2.starId = s.id) as movie_count " +
                    "FROM stars s " +
                    "JOIN stars_in_movies sm ON s.id=sm.starId " +
                    "WHERE sm.movieId=? " +
                    "ORDER BY movie_count DESC, s.name ASC";
            try (PreparedStatement ss = conn.prepareStatement(stsql)) {
                ss.setString(1, movieId);
                try (ResultSet srs = ss.executeQuery()) {
                    while (srs.next()) {
                        stars.add(new Star(srs.getString("id"), srs.getString("name")));
                    }
                }
            }
            req.setAttribute("stars", stars);
        } catch (Exception e) {
            System.err.println("Error fetching movie details for ID " + movieId + ": " + e.getMessage());
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error loading movie details.");
            return;
        }

        req.getRequestDispatcher("/WEB-INF/single-movie.jsp").forward(req, resp);
    }
}