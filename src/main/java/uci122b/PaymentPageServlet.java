package uci122b;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/payment") // This servlet will handle the /payment URL
public class PaymentPageServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        // Authentication Check
        if (session == null || session.getAttribute("userEmail") == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=unauthorized_payment_page");
            return;
        }
        // Forward to the payment.jsp located in WEB-INF
        request.getRequestDispatcher("/WEB-INF/payment.jsp").forward(request, response);
    }
}