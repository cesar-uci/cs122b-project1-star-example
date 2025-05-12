package uci122b;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            ds = (DataSource)
                    new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {
        // show login.jsp
        req.getRequestDispatcher("/WEB-INF/login.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp)
            throws ServletException, IOException {
        // ── 1) verify reCAPTCHA ─────────────────────────────
        String captcha = req.getParameter("g-recaptcha-response");
        try {
            RecaptchaVerifyUtils.verify(captcha);
        } catch (Exception ex) {
            // bot or error: redirect with ?error=bot
            resp.sendRedirect(req.getContextPath() + "/login?error=bot");
            return;
        }

        // ── 2) check user/pass in DB ────────────────────────
        String email = req.getParameter("email");
        String pw    = req.getParameter("password");
        boolean valid = false;

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM customers WHERE email=? AND password=?"
             )) {
            stmt.setString(1, email);
            stmt.setString(2, pw);
            try (ResultSet rs = stmt.executeQuery()) {
                valid = rs.next();
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        if (valid) {
            // success
            HttpSession session = req.getSession(true);
            session.setAttribute("userEmail", email);
            resp.sendRedirect(req.getContextPath() + "/movie-list.jsp");
        } else {
            resp.sendRedirect(req.getContextPath() + "/login?error=invalid");
        }
    }
}
