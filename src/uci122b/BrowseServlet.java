package uci122b;


import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "BrowseServlet", urlPatterns = "/browse")
public class BrowseServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect("login.html");
            return;
        }
        String letter = req.getParameter("letter");
        String genre  = req.getParameter("genre");

        resp.setContentType("text/html");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><head><title>Browse Results</title></head><body>");
            out.println("<h1>Browse Results</h1>");
            out.println("<table border=1><tr><th>Title</th><th>Year</th><th>Director</th><th>Rating</th></tr>");

            Context init = new InitialContext();
            DataSource ds = (DataSource) init.lookup("java:comp/env/jdbc/moviedb");
            String sql;
            if (genre != null) {
                sql = "SELECT m.title,m.year,m.director,r.rating " +
                        "FROM movies m JOIN genres_in_movies gm ON m.id=gm.movieId " +
                        "JOIN genres g ON gm.genreId=g.id " +
                        "LEFT JOIN ratings r ON m.id=r.movieId " +
                        "WHERE g.name = ?";
            } else {
                sql = "SELECT m.title,m.year,m.director,r.rating " +
                        "FROM movies m LEFT JOIN ratings r ON m.id=r.movieId " +
                        "WHERE m.title LIKE ?";
                letter = "*".equals(letter)? "[^A-Za-z0-9]%": letter+"%";
            }

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, genre!=null? genre: letter);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        out.printf("<tr><td>%s</td><td>%d</td><td>%s</td><td>%.1f</td></tr>",
                                rs.getString("title"), rs.getInt("year"),
                                rs.getString("director"), rs.getDouble("rating"));
                    }
                }
            }

            out.println("</table></body></html>");
        } catch (Exception e) {
            throw new IOException("Browse failed", e);
        }
    }
}
