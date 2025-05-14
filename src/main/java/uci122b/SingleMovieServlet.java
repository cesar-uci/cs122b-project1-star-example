package uci122b;

// Changed javax.* to jakarta.*
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// javax.naming.* and javax.sql.* often remain javax.*
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
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String movieId = req.getParameter("movieId");

        // Defensive check for movieId
        if (movieId == null || movieId.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing movieId parameter");
            return;
        }

        String rawQS = req.getQueryString();
        String backQS = "";
        if (rawQS != null) {
            // More robust way to remove movieId parameter
            backQS = rawQS.replaceAll("&?movieId=[^&]*", "").replaceAll("^&", "");
            if (backQS.startsWith("movieId")) { // Handle case where movieId is the only param
                backQS = "";
            }
        }
        req.setAttribute("backQS", backQS);

        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/moviedb");
            try (Connection conn = ds.getConnection()) {
                // Movie info
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

                // If movie not found, send 404
                if (!movieFound) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Movie with ID " + movieId + " not found.");
                    return;
                }


                // Rating
                String rsql = "SELECT rating FROM ratings WHERE movieId=?";
                try (PreparedStatement rs2 = conn.prepareStatement(rsql)) {
                    rs2.setString(1, movieId);
                    try (ResultSet rrs = rs2.executeQuery()) {
                        if (rrs.next()) {
                            req.setAttribute("rating", rrs.getFloat("rating"));
                        } else {
                            req.setAttribute("rating", 0.0f); // Use 0.0f for float
                        }
                    }
                }

                // Genres
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

                // Stars (sorted)
                List<Star> stars = new ArrayList<>();
                // Added star count query (example, adjust as needed)
                String stsql = "SELECT s.id, s.name, " +
                        "(SELECT count(*) FROM stars_in_movies sim2 WHERE sim2.starId = s.id) as movie_count " +
                        "FROM stars s " +
                        "JOIN stars_in_movies sm ON s.id=sm.starId " +
                        "WHERE sm.movieId=? " +
                        "ORDER BY movie_count DESC, s.name ASC"; // Example sort: star movie count desc, then name asc
                try (PreparedStatement ss = conn.prepareStatement(stsql)) {
                    ss.setString(1, movieId);
                    try (ResultSet srs = ss.executeQuery()) {
                        while (srs.next()) {
                            // Assuming Star constructor takes (id, name)
                            stars.add(new Star(srs.getString("id"), srs.getString("name")));
                        }
                    }
                }
                req.setAttribute("stars", stars);
            }
        } catch (Exception e) {
            // Log the error and provide a user-friendly error page
            System.err.println("Error fetching movie details for ID " + movieId + ": " + e.getMessage());
            e.printStackTrace(); // Log stack trace to server logs
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error loading movie details.");
            return; // Stop further processing
        }

        req.getRequestDispatcher("/WEB-INF/single-movie.jsp").forward(req, resp); // Assuming JSP is in WEB-INF
    }
}