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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String email = req.getParameter("email");
        String pw = req.getParameter("password");
        if (email == null || pw == null) {
            resp.sendRedirect("login.html?error=invalid");
            return;
        }
        boolean valid = false;
        try {
            Context init = new InitialContext();
            DataSource ds = (DataSource) init.lookup("java:comp/env/jdbc/moviedb");
            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT 1 FROM customers WHERE email=? AND password=?")) {
                stmt.setString(1, email);
                stmt.setString(2, pw);
                try (ResultSet rs = stmt.executeQuery()) {
                    valid = rs.next();
                }
            }
        } catch (Exception e) {
            throw new IOException("Authentication error", e);
        }
        if (valid) {
            HttpSession session = req.getSession(true);
            session.setAttribute("userEmail", email);
            resp.sendRedirect("index.html");
        } else {
            resp.sendRedirect("login.html?error=invalid");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.sendRedirect("login.html");
    }
}
