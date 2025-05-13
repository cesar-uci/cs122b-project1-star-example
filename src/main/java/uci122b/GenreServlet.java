package uci122b;

// Changed javax.* to jakarta.*
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.ServletException; // Import ServletException

// javax.naming.* and javax.sql.* remain javax.*
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException; // Import SQLException
import java.util.ArrayList;
import java.util.List;

// Gson for JSON conversion
import com.google.gson.Gson;


@WebServlet(name = "GenreServlet", urlPatterns = "/genres")
public class GenreServlet extends HttpServlet {

    private DataSource ds; // Cache DataSource

    @Override
    public void init() throws ServletException {
        try {
            ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (Exception e) {
            throw new ServletException("DataSource lookup failed in GenreServlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException { // Added ServletException
        HttpSession session = req.getSession(false);

        // Authentication check - should genres be public or require login?
        // Assuming public for this example. If login required, uncomment below.
        /*
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Login required to view genres.");
            return;
        }
        */

        // Set content type to JSON
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8"); // Ensure UTF-8 encoding for JSON


        List<String> genreList = new ArrayList<>();
        String sql = "SELECT name FROM genres ORDER BY name ASC"; // Order genres alphabetically


        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                genreList.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("Database error fetching genres: " + e.getMessage());
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve genre list.");
            return; // Stop processing
        } catch (Exception e) { // Catch other potential exceptions like NamingException
            System.err.println("Error fetching genres: " + e.getMessage());
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
            return;
        }


        // Convert the list to JSON using Gson
        Gson gson = new Gson();
        String jsonResult = gson.toJson(genreList);

        // Write JSON response
        try (PrintWriter out = resp.getWriter()) {
            out.print(jsonResult);
            out.flush(); // Ensure data is sent
        } catch (IOException e) {
            System.err.println("IOException writing JSON response for genres: " + e.getMessage());
            // Hard to send an error response here if writing already failed
        }


        /* Original direct JSON string building (commented out in favor of Gson)
        try (PrintWriter out = resp.getWriter()) {
            out.print("[");
            for (int i = 0; i < genreList.size(); i++) {
                 // Basic escaping for double quotes within genre names
                 String escapedGenre = genreList.get(i).replace("\"", "\\\"");
                out.print("\"" + escapedGenre + "\"");
                if (i < genreList.size() - 1) {
                    out.print(",");
                }
            }
            out.print("]");
            out.flush();
        } catch (Exception e) {
            System.err.println("Failed to load genres: " + e.getMessage());
            e.printStackTrace();
            // Avoid throwing IOException if response might be committed
             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // throw new IOException("Failed to load genres", e);
        }
        */
    }
}