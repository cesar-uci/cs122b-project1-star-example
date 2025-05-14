package uci122b;

// javax.naming.* and javax.sql.* remain javax.*
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

// Changed javax.* to jakarta.*
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import uci122b.util.PasswordUtil;   // ← import our bcrypt helper

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            ds = (DataSource) new InitialContext()
                    .lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            System.err.println("Failed to lookup DataSource: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Database connection could not be established.", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("siteKey", RecaptchaConstants.SITE_KEY);
        req.getRequestDispatcher("/WEB-INF/login.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp)
            throws ServletException, IOException {

        String token = req.getParameter("g-recaptcha-response");
        String email = req.getParameter("email");
        String pw    = req.getParameter("password");

        if (token == null || token.isEmpty()) {
            System.err.println("Login attempt failed: Missing reCAPTCHA token. Email: " + email);
            redirectToLogin(req, resp, "error=recaptcha_missing");
            return;
        }
        if (email == null || email.trim().isEmpty()
                || pw == null || pw.isEmpty()) {
            System.err.println("Login attempt failed: Missing email or password. Email: " + email);
            redirectToLogin(req, resp, "error=missing_credentials");
            return;
        }

        boolean isHuman = false;
        try {
            isHuman = verifyToken(token, "login");
        } catch (Exception e) {
            System.err.println("reCAPTCHA verification failed for email " + email + ": " + e.getMessage());
            e.printStackTrace();
            redirectToLogin(req, resp, "error=recaptcha_error");
            return;
        }

        if (!isHuman) {
            System.out.println("Login attempt blocked: reCAPTCHA verification failed. Email: " + email);
            redirectToLogin(req, resp, "error=bot");
            return;
        }

        System.out.println("reCAPTCHA verified successfully for email: " + email);

        boolean validCredentials = false;
        String dbPassword = null;

        String sql = "SELECT password FROM customers WHERE email=?";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    dbPassword = rs.getString("password");
                    // Check against bcrypt hash
                    if (dbPassword != null && PasswordUtil.check(pw, dbPassword)) {
                        validCredentials = true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during login for email " + email + ": " + e.getMessage());
            e.printStackTrace();
            redirectToLogin(req, resp, "error=db_error");
            return;
        }

        if (validCredentials) {
            System.out.println("Login successful for email: " + email);
            HttpSession session = req.getSession(true);
            session.setAttribute("userEmail", email);
            resp.sendRedirect(req.getContextPath() + "/index.html");
        } else {
            System.out.println("Login failed: Invalid credentials for email: " + email);
            redirectToLogin(req, resp, "error=invalid");
        }
    }

    private boolean verifyToken(String token, String action) throws IOException {
        try {
            RecaptchaVerifyUtils.RecaptchaResponse response = RecaptchaVerifyUtils.verify(token);
            System.out.println("reCAPTCHA siteverify response: success=" + response.success +
                    ", score=" + response.score + ", action=" + response.action);
            return response.success;
        } catch (Exception e) {
            System.err.println("Error during reCAPTCHA verification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void redirectToLogin(HttpServletRequest req,
                                 HttpServletResponse resp,
                                 String params)
            throws IOException {
        resp.sendRedirect(req.getContextPath() + "/login?" + params);
    }
}
