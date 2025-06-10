package uci122b;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import uci122b.util.JwtUtil;
import uci122b.util.PasswordUtil;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {
    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
            System.out.println("LoginServlet: init() complete.");
        } catch (NamingException e) {
            throw new ServletException("Database connection (JNDI) could not be established.", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("siteKey", RecaptchaConstants.SITE_KEY);
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("DIAGNOSTIC: doPost() started.");

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        JsonObject responseJsonObject = new JsonObject();

        try {
            String email = req.getParameter("email");
            System.out.println("DIAGNOSTIC: Received email: " + email);

            boolean userExists = false;
            String sql = "SELECT email FROM customers WHERE email = ?";

            System.out.println("DIAGNOSTIC: Checking if user exists in database...");
            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    // === DIAGNOSTIC CHANGE ===
                    // If the query returns any row, it means the user exists.
                    // We are NOT checking the password here.
                    if (rs.next()) {
                        System.out.println("DIAGNOSTIC: User '" + email + "' found in database. Granting access.");
                        userExists = true;
                    } else {
                        System.out.println("DIAGNOSTIC: User '" + email + "' not found.");
                    }
                }
            }

            if (userExists) {
                System.out.println("DIAGNOSTIC: Login successful (password check bypassed).");

                HttpSession session = req.getSession(true);
                session.setAttribute("userEmail", email);
                String jwt = JwtUtil.generateToken(email);
                Cookie jwtCookie = new Cookie("token", jwt);
                jwtCookie.setHttpOnly(true);
                jwtCookie.setPath("/");
                jwtCookie.setMaxAge(24 * 60 * 60);
                resp.addCookie(jwtCookie);

                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("message", "Login successful!");
                resp.setStatus(HttpServletResponse.SC_OK);

            } else {
                System.out.println("DIAGNOSTIC: Login failed (user not found).");
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Invalid username or password.");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            resp.getWriter().write(responseJsonObject.toString());

        } catch (Exception e) {
            System.err.println("DIAGNOSTIC: An unexpected error occurred in doPost().");
            e.printStackTrace(System.err);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "A server error occurred: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(responseJsonObject.toString());
        }
    }
}
