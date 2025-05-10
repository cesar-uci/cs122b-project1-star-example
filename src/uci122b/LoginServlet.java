package uci122b;

import com.google.cloud.recaptchaenterprise.v1.Assessment;
import com.google.cloud.recaptchaenterprise.v1.CreateAssessmentRequest;
import com.google.cloud.recaptchaenterprise.v1.Event;
import com.google.cloud.recaptchaenterprise.v1.RecaptchaEnterpriseServiceClient;
import com.google.cloud.recaptchaenterprise.v1.ProjectName;
import com.google.recaptchaenterprise.v1.RiskAnalysis.ClassificationReason;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.sql.DataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    // Your Google Cloud Project ID
    private static final String PROJECT_ID = "project-1-1746848175261";
    // Site key isn’t used server‑side
    private static final String SECRET_KEY = "6LfBczQrAAAAAKUXLMZbUFFiUxNBawbnbNnH8GV_";
    private static final double SCORE_THRESHOLD = 0.5;

    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            ds = (DataSource) new InitialContext()
                    .lookup("java:comp/env/jdbc/moviedb");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = req.getParameter("g-recaptcha-response");
        if (!verifyEnterpriseToken(token, "login")) {
            resp.sendRedirect("login.jsp?error=bot");
            return;
        }

        String email = req.getParameter("email");
        String pw    = req.getParameter("password");
        boolean valid = false;

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM customers WHERE email=? AND password=?")) {
            stmt.setString(1, email);
            stmt.setString(2, pw);
            try (ResultSet rs = stmt.executeQuery()) {
                valid = rs.next();
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }

        if (valid) {
            HttpSession session = req.getSession(true);
            session.setAttribute("userEmail", email);
            resp.sendRedirect("index.html");
        } else {
            resp.sendRedirect("login.jsp?error=invalid");
        }
    }

    private boolean verifyEnterpriseToken(String token, String action) {
        try (RecaptchaEnterpriseServiceClient client =
                     RecaptchaEnterpriseServiceClient.create()) {

            Event event = Event.newBuilder()
                    .setSiteKey(SECRET_KEY)
                    .setToken(token)
                    .build();

            CreateAssessmentRequest request = CreateAssessmentRequest.newBuilder()
                    .setParent(ProjectName.of(PROJECT_ID).toString())
                    .setAssessment(Assessment.newBuilder().setEvent(event).build())
                    .build();

            Assessment resp = client.createAssessment(request);

            // Check if token is valid and action matches
            if (!resp.getTokenProperties().getValid() ||
                    !resp.getTokenProperties().getAction().equals(action)) {
                return false;
            }

            // Evaluate risk score
            double score = resp.getRiskAnalysis().getScore();
            return score >= SCORE_THRESHOLD;

        } catch (Exception e) {
            // Log the exception in real code
            return false;
        }
    }
}
