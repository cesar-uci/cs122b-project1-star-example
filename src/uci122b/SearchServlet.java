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

@WebServlet(name = "SearchServlet", urlPatterns = "/search")
public class SearchServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect("login.html");
            return;
        }
        String title    = req.getParameter("title");
        String year     = req.getParameter("year");
        String director = req.getParameter("director");
        String star     = req.getParameter("star");

        resp.setContentType("text/html");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><head><title>Search Results</title></head><body>");
            out.println("<h1>Search Results</h1>");
            out.println("<table border=1><tr><th>Title</th><th>Year</th><th>Director</th><th>Rating</th></tr>");

            Context init = new InitialContext();
            DataSource ds = (DataSource) init.lookup("java:comp/env/jdbc/moviedb");
            try (Connection conn = ds.getConnection()) {
                StringBuilder sql = new StringBuilder(
                        "SELECT m.title,m.year,m.director,r.rating " +
                                "FROM movies m LEFT JOIN ratings r ON m.id=r.movieId " +
                                (star!=null&&!star.isEmpty()?
                                        "JOIN stars_in_movies sim ON m.id=sim.movieId JOIN stars s ON sim.starId=s.id ":"")
                );
                boolean first = true;
                if (title!=null&&!title.isEmpty()) {
                    sql.append(first?" WHERE":" AND").append(" m.title LIKE ?"); first=false;
                }
                if (year!=null&&!year.isEmpty()) {
                    sql.append(first?" WHERE":" AND").append(" m.year = ?"); first=false;
                }
                if (director!=null&&!director.isEmpty()) {
                    sql.append(first?" WHERE":" AND").append(" m.director LIKE ?"); first=false;
                }
                if (star!=null&&!star.isEmpty()) {
                    sql.append(first?" WHERE":" AND").append(" s.name LIKE ?");
                }

                PreparedStatement stmt = conn.prepareStatement(sql.toString());
                int idx=1;
                if (title   !=null&&!title.isEmpty())    stmt.setString(idx++,"%"+title+"%");
                if (year    !=null&&!year.isEmpty())     stmt.setInt   (idx++,Integer.parseInt(year));
                if (director!=null&&!director.isEmpty()) stmt.setString(idx++,"%"+director+"%");
                if (star    !=null&&!star.isEmpty())     stmt.setString(idx++,"%"+star+"%");

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
            throw new IOException("Search failed", e);
        }
    }
}
