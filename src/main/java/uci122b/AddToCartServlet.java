package uci122b;

// Changed javax.* to jakarta.*
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.Serializable; // CartItem should be Serializable if stored in session across nodes
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AddToCartServlet", urlPatterns = {"/add-to-cart"})
public class AddToCartServlet extends HttpServlet {
    public static final String CART_ATTR = "cart"; // Session attribute name for the cart

    public static class CartItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String movieId;
        private final String title;
        private final double price;
        private int quantity;

        // Constructor
        public CartItem(String movieId, String title, double price, int quantity) {
            this.movieId = movieId;
            this.title = title;
            this.price = price;
            this.quantity = quantity;
        }

        // Getters
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

        // Setter for quantity
        public void setQuantity(int quantity) {
            if (quantity < 0) { // Basic validation
                this.quantity = 0;
            } else {
                this.quantity = quantity;
            }
        }

        // Method to increment quantity
        public void incrementQuantity(int amount) {
            if (amount > 0) {
                this.quantity += amount;
            }
        }

        public double getTotalPrice() {
            return this.price * this.quantity;
        }

        // Override equals and hashCode if items might be compared or put in Sets
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CartItem cartItem = (CartItem) o;
            return movieId != null ? movieId.equals(cartItem.movieId) : cartItem.movieId == null;
        }

        @Override
        public int hashCode() {
            return movieId != null ? movieId.hashCode() : 0;
        }
    } // End of CartItem class

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false); // Don't create session if user isn't logged in

        // Authentication Check
        if (session == null || session.getAttribute("userEmail") == null) {
            System.out.println("AddToCartServlet: Unauthorized attempt.");
            // Send an error response or redirect to login
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You must be logged in to add items to the cart.");
            // resp.sendRedirect(req.getContextPath() + "/login?error=unauthorized_cart");
            return;
        }

        // Get parameters
        String movieId = req.getParameter("movieId");
        String movieTitle = req.getParameter("movieTitle");
        String priceStr = req.getParameter("price"); // Get price as string first

        // Basic validation
        if (movieId == null || movieId.trim().isEmpty() ||
                movieTitle == null || movieTitle.trim().isEmpty() ||
                priceStr == null || priceStr.trim().isEmpty()) {
            System.err.println("AddToCartServlet: Missing required parameters (movieId, movieTitle, or price).");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing movie information.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) { // Price shouldn't be negative
                throw new NumberFormatException("Price cannot be negative");
            }
        } catch (NumberFormatException e) {
            System.err.println("AddToCartServlet: Invalid price format: " + priceStr);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid price value.");
            return;
        }


        // Get or create the cart from the session
        @SuppressWarnings("unchecked")
        Map<String, CartItem> cart = (Map<String, CartItem>) session.getAttribute(CART_ATTR);
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute(CART_ATTR, cart); // Add the new cart to the session
        }

        CartItem item = cart.get(movieId);
        if (item == null) {
            item = new CartItem(movieId, movieTitle, price, 1);
            cart.put(movieId, item);
            System.out.println("Added new item to cart: " + movieTitle);
        } else {
            item.incrementQuantity(1);
            System.out.println("Incremented quantity for item: " + movieTitle);
        }

        session.setAttribute("cart_message", "Added '" + movieTitle + "' to your cart.");


        String referer = req.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            System.out.println("Redirecting back to: " + referer);
            resp.sendRedirect(referer);
        } else {
            System.out.println("Redirecting to default movie list.");
            resp.sendRedirect(req.getContextPath() + "/movie-list"); // Assuming /movie-list is the main movie page
        }
    }
}