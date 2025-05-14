package uci122b;

// Changed javax.* to jakarta.*
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException; // Import ServletException

import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException { // Added ServletException

        HttpSession session = req.getSession(false); // Get existing session, don't create new one

        if (session != null) {
            String userEmail = (String) session.getAttribute("userEmail"); // Get email for logging before invalidating
            System.out.println("Logging out user: " + (userEmail != null ? userEmail : "Unknown"));
            session.invalidate(); // Invalidate the session, removing all attributes
        } else {
            System.out.println("Logout attempt: No active session found.");
        }

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        resp.setDateHeader("Expires", 0); // Proxies.


        // Redirect to the login page with a success message (optional)
        System.out.println("Redirecting to login page after logout.");
        resp.sendRedirect(req.getContextPath() + "/login?message=logout_success");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        System.out.println("LogoutServlet: GET request received, processing as POST.");
        doPost(req, resp);
    }
}