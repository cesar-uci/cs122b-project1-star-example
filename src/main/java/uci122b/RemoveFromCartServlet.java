package uci122b;

// Changed javax.* to jakarta.*
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;

@WebServlet("/remove-from-cart")
public class RemoveFromCartServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String,AddToCartServlet.CartItem> cart =
                (Map<String,AddToCartServlet.CartItem>) session.getAttribute(AddToCartServlet.CART_ATTR);
        if (cart != null) {
            cart.remove(req.getParameter("movieId"));
        }
        resp.sendRedirect("shopping-cart.jsp");
    }
}