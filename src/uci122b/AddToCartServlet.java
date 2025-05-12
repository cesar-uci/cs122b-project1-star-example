package uci122b;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AddToCartServlet", urlPatterns = {"/add-to-cart"})
public class AddToCartServlet extends HttpServlet {
    public static final String CART_ATTR = "cart";

    public static class CartItem {
        private final String movieId;
        private final String title;
        private final double price;
        private int quantity;

        public CartItem(String movieId, String title, double price, int quantity) {
            this.movieId = movieId;
            this.title = title;
            this.price = price;
            this.quantity = quantity;
        }

        public String getMovieId() {
            return movieId;
        }
        public String getTitle() {
            return title;
        }
        public double getPrice() {
            return price;
        }
        public int getQuantity() {
            return quantity;
        }
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String,CartItem> cart = (Map<String,CartItem>) session.getAttribute(CART_ATTR);
        if (cart == null) {
            cart = new HashMap<>();
        }

        String movieId = req.getParameter("movieId");
        String movieTitle = req.getParameter("movieTitle");
        double price = Double.parseDouble(req.getParameter("price"));

        CartItem item = cart.get(movieId);
        if (item == null) {
            item = new CartItem(movieId, movieTitle, price, 1);
            cart.put(movieId, item);
        } else {
            item.setQuantity(item.getQuantity() + 1);
        }

        session.setAttribute(CART_ATTR, cart);

        resp.sendRedirect("movie-list.jsp");
    }
}
