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
import java.sql.ResultSet;
import java.sql.Statement;

@WebServlet("/stars")
public class StarServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        resp.setContentType("text/html");
        try (PrintWriter out = resp.getWriter()) {
            Context init = new InitialContext();
            DataSource ds = (DataSource) init.lookup("java:comp/env/jdbc/moviedb");
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM stars LIMIT 10")) {
                out.println("<html><head><title>Fabflix Stars</title></head><body>");
                out.println("<h1>MovieDB Stars</h1><table border=\"1\">");
                out.println("<tr><th>ID</th><th>Name</th><th>Birth Year</th></tr>");
                while (rs.next()) {
                    out.printf("<tr><td>%s</td><td>%s</td><td>%s</td></tr>",
                            rs.getString("id"), rs.getString("name"), rs.getString("birthYear"));
                }
                out.println("</table></body></html>");
            }

        } catch (Exception e) {
            throw new IOException("Error retrieving stars", e);
        }
    }
}
