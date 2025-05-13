package uci122b;

// Changed javax.* to jakarta.*
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException; // Import ServletException

import java.io.IOException;
import java.util.Map;

@WebServlet("/update-cart")
public class UpdateCartServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException { // Added ServletException
        HttpSession session = req.getSession(false); // Don't create if it doesn't exist

        // Authentication Check
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You must be logged in to update the cart.");
            return;
        }

        // Get parameters
        String movieId = req.getParameter("movieId");
        String quantityStr = req.getParameter("quantity");

        // Parameter Validation
        if (movieId == null || movieId.trim().isEmpty() ||
                quantityStr == null || quantityStr.trim().isEmpty()) {
            System.err.println("UpdateCartServlet: Missing movieId or quantity parameter.");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing item ID or quantity.");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            // Quantity cannot be negative (0 means remove)
            if (quantity < 0) {
                throw new NumberFormatException("Quantity cannot be negative");
            }
        } catch (NumberFormatException e) {
            System.err.println("UpdateCartServlet: Invalid quantity format: " + quantityStr);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid quantity value.");
            return;
        }

        // Get cart from session
        @SuppressWarnings("unchecked")
        Map<String, AddToCartServlet.CartItem> cart =
                (Map<String, AddToCartServlet.CartItem>) session.getAttribute(AddToCartServlet.CART_ATTR);

        String message = null; // Message to display on redirect

        if (cart != null) {
            AddToCartServlet.CartItem item = cart.get(movieId);

            if (item != null) { // Check if the item exists in the cart
                if (quantity <= 0) {
                    // Remove item if quantity is 0 or less
                    cart.remove(movieId);
                    System.out.println("Removed item from cart: " + item.getTitle());
                    message = "Removed '" + item.getTitle() + "' from your cart.";
                } else {
                    // Update quantity if item exists and quantity > 0
                    item.setQuantity(quantity);
                    System.out.println("Updated quantity for item '" + item.getTitle() + "' to " + quantity);
                    message = "Updated quantity for '" + item.getTitle() + "' to " + quantity + ".";
                }
                // If cart becomes empty after removal, consider removing the cart attribute
                if (cart.isEmpty()) {
                    session.removeAttribute(AddToCartServlet.CART_ATTR);
                    System.out.println("Cart is now empty.");
                } else {
                    session.setAttribute(AddToCartServlet.CART_ATTR, cart); // Update session cart
                }

            } else {
                System.err.println("UpdateCartServlet: Attempted to update item not found in cart (movieId: " + movieId + ")");
                // Optionally set an error message
                message = "Error: Item with ID " + movieId + " not found in your cart.";
                // Don't send a 400 error here, just redirect with message
            }
        } else {
            // Cart doesn't exist in session, which shouldn't happen if trying to update
            System.err.println("UpdateCartServlet: Cart not found in session for update attempt.");
            message = "Error: Your shopping cart was not found.";
        }

        // Set message attribute for display on the shopping cart page
        if (message != null) {
            session.setAttribute("cart_message", message);
        }


        // Redirect back to the shopping cart page
        resp.sendRedirect(req.getContextPath() + "/shopping-cart.jsp"); // Assuming this is your cart page
    }
}