package uci122b;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession; // For authentication check

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration; // For getting all parameter names
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.URLEncoder;

// This is the servlet that should handle requests to "/single-star"
@WebServlet(name = "SingleStarServlet", urlPatterns = "/single-star")
public class SingleStarServlet extends HttpServlet {
    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            Context initCtx = new InitialContext();
            // Ensure your JNDI name in context.xml matches "java:comp/env/jdbc/moviedb"
            this.ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb");
        } catch (Exception e) {
            System.err.println("DataSource lookup failed in SingleStarServlet: " + e.getMessage());
            throw new ServletException("DataSource lookup failed in SingleStarServlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Authentication Check
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=unauthorized_access_to_star_details");
            return;
        }

        String starId = req.getParameter("starId"); // Expecting "starId" from links

        if (starId == null || starId.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty starId parameter.");
            return;
        }
        req.setAttribute("starId", starId); // Pass it to JSP for potential use (e.g. debug)


        // Construct backQS: query string for "Back to Movie List" link
        // This preserves filters, sorting, and pagination from the movie list page
        StringBuilder backQSBuilder = new StringBuilder();
        Enumeration<String> paramNames = req.getParameterNames();
        boolean firstParamAppended = false;
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            if (!paramName.equalsIgnoreCase("starId")) { // Exclude the starId itself
                if (firstParamAppended) {
                    backQSBuilder.append("&");
                }
                String[] paramValues = req.getParameterValues(paramName);
                for (int i = 0; i < paramValues.length; i++) {
                    backQSBuilder.append(URLEncoder.encode(paramName, "UTF-8"))
                            .append("=")
                            .append(URLEncoder.encode(paramValues[i], "UTF-8"));
                    if (i < paramValues.length - 1) { // For multi-valued parameters
                        backQSBuilder.append("&").append(URLEncoder.encode(paramName, "UTF-8")).append("=");
                    }
                }
                firstParamAppended = true;
            }
        }
        req.setAttribute("backQS", backQSBuilder.toString());

        Connection conn = null;
        PreparedStatement starDetailsStmt = null;
        ResultSet rsStarDetails = null;
        PreparedStatement moviesStmt = null;
        ResultSet rsStarMovies = null;

        try {
            conn = ds.getConnection();

            // 1. Get Star Details (Name, Birth Year)
            String starDetailsSql = "SELECT name, birthYear FROM stars WHERE id = ?";
            starDetailsStmt = conn.prepareStatement(starDetailsSql);
            starDetailsStmt.setString(1, starId);
            rsStarDetails = starDetailsStmt.executeQuery();

            if (rsStarDetails.next()) {
                req.setAttribute("starName", rsStarDetails.getString("name"));
                int birthYear = rsStarDetails.getInt("birthYear");
                if (rsStarDetails.wasNull()) { // Check if birthYear was SQL NULL
                    req.setAttribute("starBirthYear", null);
                } else {
                    req.setAttribute("starBirthYear", birthYear);
                }
            } else {
                // Star not found
                System.err.println("SingleStarServlet: Star with ID '" + starId + "' not found.");
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Star not found.");
                return;
            }

            // 2. Get Movies the Star Acted In
            // Sorted by year descending, then by movie title ascending (as per Project 2 requirements)
            String starMoviesSql = "SELECT m.id, m.title, m.year " +
                    "FROM movies m JOIN stars_in_movies sim ON m.id = sim.movieId " +
                    "WHERE sim.starId = ? " +
                    "ORDER BY m.year DESC, m.title ASC";
            moviesStmt = conn.prepareStatement(starMoviesSql);
            moviesStmt.setString(1, starId);
            rsStarMovies = moviesStmt.executeQuery();

            List<Map<String, String>> moviesList = new ArrayList<>();
            while (rsStarMovies.next()) {
                Map<String, String> movieData = new HashMap<>();
                movieData.put("id", rsStarMovies.getString("id"));
                movieData.put("title", rsStarMovies.getString("title"));
                movieData.put("year", Integer.toString(rsStarMovies.getInt("year"))); // Store as String
                moviesList.add(movieData);
            }
            req.setAttribute("moviesByStar", moviesList);

            // 3. Forward to the JSP in WEB-INF
            RequestDispatcher dispatcher = req.getRequestDispatcher("/WEB-INF/single-star.jsp");
            dispatcher.forward(req, resp);

        } catch (SQLException e) {
            System.err.println("SingleStarServlet SQL Error for starId=" + starId + ": " + e.getMessage());
            e.printStackTrace(); // Log the full stack trace to server logs
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "A database error occurred while retrieving star details.");
        } catch (Exception e) {
            System.err.println("SingleStarServlet Generic Error for starId=" + starId + ": " + e.getMessage());
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        } finally {
            // Close resources in reverse order of creation
            try { if (rsStarMovies != null) rsStarMovies.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (moviesStmt != null) moviesStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (rsStarDetails != null) rsStarDetails.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (starDetailsStmt != null) starDetailsStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
