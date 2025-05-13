package uci122b;

// javax.naming.* and javax.sql.* remain javax.*
import javax.naming.InitialContext;
import javax.naming.NamingException;
// Changed javax.* to jakarta.*
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            ds = (DataSource)new InitialContext()
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
        if (email == null || email.trim().isEmpty() || pw == null || pw.isEmpty()) {
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
        // In a real application, you would be comparing hashed passwords.
        // For Project 3, you'll implement password encryption. This code assumes plain text or already encrypted comparison.
        String sql = "SELECT password FROM customers WHERE email=?";

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    dbPassword = rs.getString("password");
                    // If passwords are plain text in DB (as in P2 before P3 encryption task)
                    if (dbPassword != null && dbPassword.equals(pw)) {
                        validCredentials = true;
                    }
                    // If passwords ARE encrypted in DB (after P3 Task 4)
                    // else if (dbPassword != null && PasswordUtils.verifyPassword(pw, dbPassword)) { // Assuming you have a PasswordUtils class
                    //     validCredentials = true;
                    // }
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
            // Potentially fetch and set other user details in the session

            //                             ****** THE CRITICAL CHANGE IS HERE ******
            resp.sendRedirect(req.getContextPath() + "/index.html"); // Redirect to your main HTML page
            //                             ******************************************

        } else {
            System.out.println("Login failed: Invalid credentials for email: " + email);
            redirectToLogin(req, resp, "error=invalid");
        }
    }

    private boolean verifyToken(String token, String action) throws IOException {
        // Define your score threshold (can be adjusted)
        final double SCORE_THRESHOLD = 0.5;

        try {
            RecaptchaVerifyUtils.RecaptchaResponse response = RecaptchaVerifyUtils.verify(token);

            System.out.println("reCAPTCHA siteverify response: success=" + response.success +
                    ", score=" + response.score + ", action=" + response.action);

            if (!response.success) {
                System.err.println("reCAPTCHA verification failed: Success flag is false.");
                return false;
            }
            // Basic check: if success is true, consider it human.
            // More advanced checks for score and action can be added if needed.
            return true;

        } catch (Exception e) {
            System.err.println("Error during reCAPTCHA verification using RecaptchaVerifyUtils: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void redirectToLogin(HttpServletRequest req, HttpServletResponse resp, String params) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/login?" + params);
    }
}