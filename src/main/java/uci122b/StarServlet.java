package uci122b;

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
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name = "StarServlet", urlPatterns = "/stars")
public class StarServlet extends HttpServlet {
    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            Context init = new InitialContext();
            ds = (DataSource) init.lookup("java:comp/env/jdbc/moviedb");
        } catch (Exception e) {
            throw new ServletException("DataSource lookup failed in StarServlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {
        // 1) Authentication check
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=unauthorized");
            return;
        }

        resp.setContentType("text/html");
        try (PrintWriter out = resp.getWriter()) {
            // HTML header
            out.println("<!DOCTYPE html><html><head>");
            out.println("<meta charset=\"UTF-8\"><title>Fabflix Stars</title>");
            out.println("<link rel=\"stylesheet\" href=\"" + req.getContextPath() + "/css/style.css\">");
            out.println("</head><body><div class=\"container\">");
            out.println("<div class=\"header\"><h1>MovieDB Stars</h1>"
                    + "<form method=\"post\" action=\"" + req.getContextPath() + "/logout\" style=\"display:inline;\">"
                    + "<button type=\"submit\" class=\"btn-secondary\">Logout</button></form></div>");

            // Database query
            String sql = "SELECT id, name, birthYear FROM stars ORDER BY name ASC LIMIT 20";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                out.println("<div class=\"card\"><h2>Top Stars</h2>");
                out.println("<table class=\"table\"><thead><tr>"
                        + "<th>ID</th><th>Name</th><th>Birth Year</th></tr></thead><tbody>");

                boolean found = false;
                while (rs.next()) {
                    found = true;
                    String id   = rs.getString("id");
                    String name = rs.getString("name");
                    int by      = rs.getInt("birthYear");
                    String bys  = rs.wasNull() ? "N/A" : Integer.toString(by);
                    out.printf("<tr><td>%s</td><td>%s</td><td>%s</td></tr>%n", id, name, bys);
                }
                out.println("</tbody></table>");
                if (!found) {
                    out.println("<p>No stars found in the database.</p>");
                }
                out.println("</div>"); // end card
            } catch (SQLException e) {
                log("Error retrieving stars", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Could not retrieve star list.");
            }

            // HTML footer
            out.println("</div></body></html>");
        }
    }
}
