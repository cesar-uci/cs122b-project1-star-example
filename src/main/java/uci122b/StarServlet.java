package uci122b;

// Changed javax.* to jakarta.*
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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

@WebServlet("/stars") // Consider a more descriptive path like "/list-stars"
public class StarServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false); // Check if session exists

        // Authentication Check (moved from original AuthFilter logic for clarity here)
        // AuthFilter should handle this, but double-checking can be useful
        if (session == null || session.getAttribute("userEmail") == null) {
            System.out.println("StarServlet: Unauthorized access attempt.");
            resp.sendRedirect(req.getContextPath() + "/login?error=unauthorized");
            return;
        }

        resp.setContentType("text/html"); // Set content type early

        // Use try-with-resources for PrintWriter as well
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>"); // Add DOCTYPE
            out.println("<html><head>");
            out.println("<meta charset=\"UTF-8\">"); // Specify encoding
            out.println("<title>Fabflix Stars</title>");
            // Link to external CSS (assuming style.css is in /css/ folder)
            out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath() + "/css/style.css\">");
            out.println("</head><body>");

            // Use container div from CSS for centering/styling
            out.println("<div class=\"container\">");
            // Include a header structure
            out.println("<div class=\"header\"><h1>MovieDB Stars</h1>");
            // Example: Add logout button/link
            out.println("<form method=\"post\" action=\"" + req.getContextPath() + "/logout\" style=\"display: inline;\"><button type=\"submit\" class=\"btn-secondary\">Logout</button></form>");
            out.println("</div>"); // end header

            Context init = new InitialContext();
            DataSource ds = (DataSource) init.lookup("java:comp/env/jdbc/moviedb");

            // Use try-with-resources for database resources
            // Fetch more stars and add pagination/sorting later if needed
            String sql = "SELECT id, name, birthYear FROM stars ORDER BY name ASC LIMIT 20"; // Example: Sort by name, limit 20

            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                out.println("<div class=\"card\">"); // Use card styling
                out.println("<h2>Top Stars</h2>");
                out.println("<table class=\"table\">"); // Add CSS class if available
                out.println("<thead><tr><th>ID</th><th>Name</th><th>Birth Year</th></tr></thead>"); // Use thead/tbody
                out.println("<tbody>");

                boolean foundStars = false;
                while (rs.next()) {
                    foundStars = true;
                    String starId = rs.getString("id");
                    String starName = rs.getString("name");
                    // Handle potential null birthYear
                    int birthYearInt = rs.getInt("birthYear");
                    String birthYearStr = rs.wasNull() ? "N/A" : String.valueOf(birthYearInt);

                    out.printf("<tr><td>%s</td><td>%s</td><td>%s</td></tr>%n",
                            starId, starName, birthYearStr);
                }
                out.println("</tbody>");
                out.println("</table>"); // end table

                if (!foundStars) {
                    out.println("<p>No stars found in the database.</p>");
                }
                out.println("</div>"); // end card
            } // End of try-with-resources for DB connection

            out.println("</div>"); // end container
            out.println("</body></html>");

        } catch (Exception e) { // Catch broader exceptions for JNDI/SQL issues
            System.err.println("Error retrieving stars: " + e.getMessage());
            e.printStackTrace();
            // Send a generic error response instead of re-throwing IOException if response is committed
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // Avoid writing to out if already closed or in error state
            PrintWriter errorOut = null;
            try {
                // Try to get a writer again if the original one failed badly
                if (!resp.isCommitted()) {
                    resp.setContentType("text/html"); // Ensure content type is set for error page
                    errorOut = resp.getWriter();
                    errorOut.println("<html><head><title>Error</title></head><body>");
                    errorOut.println("<h1>Error</h1><p>Could not retrieve star list due to a server error.</p>");
                    // errorOut.println("<p>Error details: " + e.getMessage() + "</p>"); // Maybe only in debug mode
                    errorOut.println("</body></html>");
                }
            } catch (IOException ioex) {
                // Ignore if we can't even write the error page
                System.err.println("Failed to write error page response: " + ioex.getMessage());
            }
            // throw new IOException("Error retrieving stars", e); // Avoid this if response might be committed
        }
    }
}