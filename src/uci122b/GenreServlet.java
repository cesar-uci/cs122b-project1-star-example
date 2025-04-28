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
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "GenreServlet", urlPatterns = "/genres")
public class GenreServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect("login.html");
            return;
        }
        resp.setContentType("application/json");
        try (PrintWriter out = resp.getWriter()) {
            Context init = new InitialContext();
            DataSource ds = (DataSource) init.lookup("java:comp/env/jdbc/moviedb");
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM genres ORDER BY name")) {
                List<String> list = new ArrayList<>();
                while (rs.next()) list.add(rs.getString("name"));
                out.print("[");
                for (int i = 0; i < list.size(); i++) {
                    out.print('"' + list.get(i).replace("\"","\\\"") + '"');
                    if (i < list.size()-1) out.print(",");
                }
                out.print("]");
            }
        } catch (Exception e) {
            throw new IOException("Failed to load genres", e);
        }
    }
}
