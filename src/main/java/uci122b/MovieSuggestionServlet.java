package uci122b;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uci122b.util.SearchUtil; // Import the utility class we created

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "MovieSuggestionServlet", urlPatterns = "/api/movie-suggestions")
public class MovieSuggestionServlet extends HttpServlet {
    private DataSource ds; // DataSource for database connections

    @Override
    public void init() throws ServletException {
        try {
            Context initCtx = new InitialContext();
            this.ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb_slave");
            System.out.println("MovieSuggestionServlet: Initialized with JNDI resource jdbc/moviedb_slave");
        } catch (Exception e) {
            System.err.println("MovieSuggestionServlet: Failed to lookup DataSource (jdbc/moviedb_slave): " + e.getMessage());
            throw new ServletException("DataSource lookup failed in MovieSuggestionServlet for jdbc/moviedb_slave", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String query = request.getParameter("query");

        if (query == null || query.trim().length() < 3) {
            out.write("[]"); // Return empty array if query is too short
            out.flush();
            return;
        }

        String trimmedQuery = query.trim();
        JsonArray suggestionsArray = new JsonArray();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        System.out.println("MovieSuggestionServlet: Autocomplete search initiated for: '" + trimmedQuery + "' (after delay)");
        System.out.println("MovieSuggestionServlet: Sending AJAX request to server for: '" + trimmedQuery + "'");

        try {
            conn = ds.getConnection(); // Connection from SLAVE pool

            String[] tokens = trimmedQuery.split("\\s+");
            StringBuilder booleanModeQueryBuilder = new StringBuilder();
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    booleanModeQueryBuilder.append("+").append(token.replaceAll("[+-><()~*\"@]", "")).append("* ");
                }
            }
            String ftQueryString = booleanModeQueryBuilder.toString().trim();

            String likePattern = "%" + trimmedQuery + "%";

            int edthThreshold = SearchUtil.calculateEditDistanceThreshold(trimmedQuery);

            String sql = "SELECT DISTINCT m.id, m.title " +
                    "FROM movies m " +
                    "WHERE ";

            List<String> conditions = new ArrayList<>();
            List<Object> sqlParams = new ArrayList<>();

            if (!ftQueryString.isEmpty()) {
                conditions.add("MATCH(m.title) AGAINST (? IN BOOLEAN MODE)");
                sqlParams.add(ftQueryString);
            }

            conditions.add("m.title LIKE ?");
            sqlParams.add(likePattern);

            conditions.add("edth(LOWER(m.title), LOWER(?), ?) = 1"); // <<<< SEE LOWER() HERE
            sqlParams.add(trimmedQuery); // This is the user's query
            sqlParams.add(edthThreshold);

            sql += String.join(" OR ", conditions);
            sql += " LIMIT 10";

            pstmt = conn.prepareStatement(sql);

            int paramIndex = 1;
            for (Object param : sqlParams) {
                if (param instanceof String) {
                    pstmt.setString(paramIndex++, (String) param);
                } else if (param instanceof Integer) {
                    pstmt.setInt(paramIndex++, (Integer) param);
                }
            }

            System.out.println("MovieSuggestionServlet: Executing SQL on SLAVE DB: " + pstmt.toString()); // For your debugging

            rs = pstmt.executeQuery();
            while (rs.next()) {
                String movieId = rs.getString("id");
                String movieTitle = rs.getString("title");
                JsonObject suggestion = new JsonObject();
                suggestion.addProperty("value", movieTitle); // Text to display
                suggestion.addProperty("data", movieId);     // Movie ID for redirection
                suggestionsArray.add(suggestion);
            }

            System.out.println("MovieSuggestionServlet: Used suggestion list (from server): " + suggestionsArray.toString());
            out.write(suggestionsArray.toString());

        } catch (SQLException e) {
            System.err.println("MovieSuggestionServlet SQL Error (SLAVE DB) for query '" + trimmedQuery + "': " + e.getMessage());
            e.printStackTrace(); // Good for server-side debugging
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // Send a JSON error response to the client if possible
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("error", "SQL Error processing your request: " + e.getMessage().replace("\"", "'")); // Avoid breaking JSON
            out.write(errorJson.toString());
        } catch (Exception e) { // Catch any other unexpected errors
            System.err.println("MovieSuggestionServlet General Error for query '" + trimmedQuery + "': " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("error", "Unexpected error processing your request: " + e.getMessage().replace("\"", "'"));
            out.write(errorJson.toString());
        } finally {
            // Close database resources in a finally block
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (pstmt != null) pstmt.close(); } catch (SQLException ignored) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}