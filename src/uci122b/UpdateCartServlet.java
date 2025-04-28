package uci122b;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;

@WebServlet("/update-cart")
public class UpdateCartServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.sendRedirect("login.html");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String,AddToCartServlet.CartItem> cart =
                (Map<String,AddToCartServlet.CartItem>) session.getAttribute(AddToCartServlet.CART_ATTR);
        if (cart != null) {
            String id       = req.getParameter("movieId");
            int qty         = Integer.parseInt(req.getParameter("quantity"));
            if (qty <= 0) {
                cart.remove(id);
            } else {
                AddToCartServlet.CartItem item = cart.get(id);
                if (item != null) item.setQuantity(qty);
            }
        }
        resp.sendRedirect("shopping-cart.jsp");
    }
}
