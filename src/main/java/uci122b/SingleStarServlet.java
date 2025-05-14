package uci122b;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "SingleStarServlet", urlPatterns = "/single-star")
public class SingleStarServlet extends HttpServlet {
    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            Context initCtx = new InitialContext();
            // JNDI lookup must match your context.xml
            ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb");
        } catch (Exception e) {
            throw new ServletException("DataSource lookup failed in SingleStarServlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=unauthorized_access_to_star_details");
            return;
        }

        String starId = req.getParameter("starId");
        if (starId == null || starId.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty starId parameter.");
            return;
        }
        req.setAttribute("starId", starId);

        StringBuilder backQS = new StringBuilder();
        Enumeration<String> names = req.getParameterNames();
        boolean first = true;
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!name.equals("starId")) {
                for (String v : req.getParameterValues(name)) {
                    if (!first) backQS.append("&");
                    first = false;
                    backQS.append(URLEncoder.encode(name, "UTF-8"))
                            .append("=")
                            .append(URLEncoder.encode(v, "UTF-8"));
                }
            }
        }
        req.setAttribute("backQS", backQS.toString());

        String starSql =
                "SELECT name, birthYear " +
                        "FROM stars " +
                        "WHERE id = ?";
        String moviesSql =
                "SELECT m.id, m.title, m.year " +
                        "FROM movies m " +
                        "JOIN stars_in_movies sim ON m.id = sim.movieId " +
                        "WHERE sim.starId = ? " +
                        "ORDER BY m.year DESC, m.title ASC";

        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement ps1 = conn.prepareStatement(starSql)) {
                ps1.setString(1, starId);
                try (ResultSet rs1 = ps1.executeQuery()) {
                    if (rs1.next()) {
                        req.setAttribute("starName", rs1.getString("name"));
                        int dob = rs1.getInt("birthYear");
                        if (rs1.wasNull()) {
                            req.setAttribute("starBirthYear", null);
                        } else {
                            req.setAttribute("starBirthYear", dob);
                        }
                    } else {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Star not found.");
                        return;
                    }
                }
            }

            List<Map<String,String>> moviesList = new ArrayList<>();
            try (PreparedStatement ps2 = conn.prepareStatement(moviesSql)) {
                ps2.setString(1, starId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        Map<String,String> m = new HashMap<>();
                        m.put("id",    rs2.getString("id"));
                        m.put("title", rs2.getString("title"));
                        m.put("year",  Integer.toString(rs2.getInt("year")));
                        moviesList.add(m);
                    }
                }
            }
            req.setAttribute("moviesByStar", moviesList);

            // 6) Forward to JSP
            RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/single-star.jsp");
            rd.forward(req, resp);

        } catch (SQLException e) {
            // log and show 500
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "A database error occurred while retrieving star details.");
        }
    }
}
