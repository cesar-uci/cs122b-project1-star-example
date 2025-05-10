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
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String movieId = req.getParameter("movieId");
        String rawQS = req.getQueryString();
        String backQS = "";
        if (rawQS != null) {
            backQS = rawQS.replaceAll("(^|&)?movieId=[^&]*&?", "");
            if (backQS.endsWith("&")) backQS = backQS.substring(0, backQS.length() - 1);
        }
        req.setAttribute("backQS", backQS);

        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/moviedb");
            try (Connection conn = ds.getConnection()) {
                // Movie info
                String msql = "SELECT title, year, director FROM movies WHERE id=?";
                try (PreparedStatement ms = conn.prepareStatement(msql)) {
                    ms.setString(1, movieId);
                    try (ResultSet mrs = ms.executeQuery()) {
                        if (mrs.next()) {
                            req.setAttribute("title", mrs.getString("title"));
                            req.setAttribute("year", mrs.getInt("year"));
                            req.setAttribute("director", mrs.getString("director"));
                        }
                    }
                }

                // Rating
                String rsql = "SELECT rating FROM ratings WHERE movieId=?";
                try (PreparedStatement rs2 = conn.prepareStatement(rsql)) {
                    rs2.setString(1, movieId);
                    try (ResultSet rrs = rs2.executeQuery()) {
                        if (rrs.next()) {
                            req.setAttribute("rating", rrs.getFloat("rating"));
                        } else {
                            req.setAttribute("rating", 0);
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
                String stsql = "SELECT s.id, s.name FROM stars s "
                        + "JOIN stars_in_movies sm ON s.id=sm.starId "
                        + "WHERE sm.movieId=? ORDER BY s.name ASC";
                try (PreparedStatement ss = conn.prepareStatement(stsql)) {
                    ss.setString(1, movieId);
                    try (ResultSet srs = ss.executeQuery()) {
                        while (srs.next()) {
                            stars.add(new Star(srs.getString("id"), srs.getString("name")));
                        }
                    }
                }
                req.setAttribute("stars", stars);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }

        req.getRequestDispatcher("single-movie.jsp").forward(req, resp);
    }
}
