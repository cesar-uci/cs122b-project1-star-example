package uci122b;

// Changed javax.* to jakarta.*
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
// javax.naming.* and javax.sql.* remain javax.*
import javax.naming.Context;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate; // Use java.time for dates
import java.util.*;

@WebServlet(name = "PlaceOrderServlet", urlPatterns = {"/place-order"})
public class PlaceOrderServlet extends HttpServlet {

    private DataSource ds; // Cache DataSource

    @Override
    public void init() throws ServletException {
        try {
            ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (Exception e) {
            throw new ServletException("DataSource lookup failed in PlaceOrderServlet", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);

        // Authentication Check
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=unauthorized_order");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, AddToCartServlet.CartItem> cart =
                (Map<String, AddToCartServlet.CartItem>) session.getAttribute(AddToCartServlet.CART_ATTR);

        if (cart == null || cart.isEmpty()) {
            System.out.println("PlaceOrderServlet: Attempt to place order with empty cart.");
            session.setAttribute("cart_message", "Your cart is empty. Please add items before placing an order.");
            resp.sendRedirect(req.getContextPath() + "/shopping-cart.jsp");
            return;
        }

        String firstName = req.getParameter("firstName");
        String lastName = req.getParameter("lastName");
        String ccNumber = req.getParameter("ccNumber");
        String expirationStr = req.getParameter("expiration"); // Expecting YYYY-MM-DD format

        System.out.println("PlaceOrderServlet DEBUG: Received raw parameters - firstName: [" + firstName + "], lastName: [" + lastName + "], ccNumber: [" + ccNumber + "], expirationStr: [" + expirationStr + "]");


        if (firstName == null || firstName.trim().isEmpty() ||
                lastName == null || lastName.trim().isEmpty() ||
                ccNumber == null || ccNumber.trim().isEmpty() ||
                expirationStr == null || expirationStr.trim().isEmpty()) {
            System.err.println("PlaceOrderServlet: Missing payment information.");
            forwardToPaymentWithError(req, resp, "Missing required payment fields.");
            return;
        }

        java.sql.Date expirationSqlDate;
        try {
            // Assuming input is YYYY-MM-DD from the HTML date input
            LocalDate expirationLocalDate = LocalDate.parse(expirationStr);
            // NO FUTURE DATE CHECK - as per your request to match DB data
            expirationSqlDate = java.sql.Date.valueOf(expirationLocalDate);
            System.out.println("PlaceOrderServlet DEBUG: Parsed SQL Expiration Date: [" + expirationSqlDate + "]");
        } catch (Exception e) {
            System.err.println("PlaceOrderServlet: Invalid expiration date format (expecting YYYY-MM-DD): " + expirationStr + " - " + e.getMessage());
            forwardToPaymentWithError(req, resp, "Invalid expiration date. Please use YYYY-MM-DD format.");
            return;
        }

        String userEmail = (String) session.getAttribute("userEmail");
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);

            System.out.println("PlaceOrderServlet DEBUG: Attempting to validate card against DB.");

            String cardSql = "SELECT id FROM creditcards WHERE id=? AND firstName=? AND lastName=? AND expiration=?";
            boolean validCard = false;
            try (PreparedStatement cardStmt = conn.prepareStatement(cardSql)) {
                // Trim inputs for names, and ensure ccNumber matches DB format (with/without spaces)
                String trimmedFirstName = firstName.trim();
                String trimmedLastName = lastName.trim();
                String processedCcNumber = ccNumber.trim(); // Further processing might be needed if DB has spaces and form doesn't or vice-versa

                cardStmt.setString(1, processedCcNumber);
                cardStmt.setString(2, trimmedFirstName);
                cardStmt.setString(3, trimmedLastName);
                cardStmt.setDate(4, expirationSqlDate);

                System.out.println("PlaceOrderServlet DEBUG: Executing SQL: " + cardSql);
                System.out.println("PlaceOrderServlet DEBUG: With SQL Params - id: [" + processedCcNumber + "], firstName: [" + trimmedFirstName + "], lastName: [" + trimmedLastName + "], expiration: [" + expirationSqlDate + "]");

                try (ResultSet rc = cardStmt.executeQuery()) {
                    if (rc.next()) {
                        validCard = true;
                        System.out.println("PlaceOrderServlet DEBUG: Card VALIDATED successfully in DB.");
                    } else {
                        System.out.println("PlaceOrderServlet DEBUG: Card validation FAILED - no matching record found in DB.");
                    }
                }
            }

            if (!validCard) {
                System.out.println("PlaceOrderServlet: Invalid payment information for user " + userEmail +
                        ". Submitted to SQL: CC#=[" + ccNumber.trim() + "], Name=[" + firstName.trim() + " " + lastName.trim() +
                        "], ParsedExpDate=[" + expirationSqlDate + "]");
                if (conn != null) conn.rollback();
                forwardToPaymentWithError(req, resp, "Invalid payment information. Please check your card details and expiration date.");
                return;
            }

            int customerId = -1;
            String custSql = "SELECT id FROM customers WHERE email=?";
            try (PreparedStatement custStmt = conn.prepareStatement(custSql)) {
                custStmt.setString(1, userEmail);
                try (ResultSet rc = custStmt.executeQuery()) {
                    if (!rc.next()) {
                        System.err.println("PlaceOrderServlet: Critical Error! Customer not found for logged-in email: " + userEmail);
                        if (conn != null) conn.rollback();
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not find customer record.");
                        return;
                    }
                    customerId = rc.getInt("id");
                }
            }

            if (customerId == -1) { // Should have been caught above, but as a safeguard
                System.err.println("PlaceOrderServlet: Failed to retrieve customer ID for " + userEmail);
                if (conn != null) conn.rollback();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving customer information.");
                return;
            }

            java.sql.Date saleDate = java.sql.Date.valueOf(LocalDate.now());
            String saleSql = "INSERT INTO sales(customerId, movieId, saleDate) VALUES (?, ?, ?)";
            int totalItemsInserted = 0;
            try (PreparedStatement saleStmt = conn.prepareStatement(saleSql)) {
                for (AddToCartServlet.CartItem item : cart.values()) {
                    String movieId = item.getMovieId();
                    int qty = item.getQuantity();
                    for (int i = 0; i < qty; i++) {
                        saleStmt.setInt(1, customerId);
                        saleStmt.setString(2, movieId);
                        saleStmt.setDate(3, saleDate);
                        saleStmt.addBatch();
                        totalItemsInserted++;
                    }
                }
                int[] results = saleStmt.executeBatch();
                int successfulInserts = 0;
                for (int result : results) {
                    if (result >= 0 || result == Statement.SUCCESS_NO_INFO) {
                        successfulInserts++;
                    }
                }
                if (successfulInserts != totalItemsInserted) {
                    System.err.printf("PlaceOrderServlet: Batch insert mismatch. Expected %d, Successful: %d%n", totalItemsInserted, successfulInserts);
                    if (conn != null) conn.rollback();
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error saving some order items.");
                    return;
                }
                System.out.println("Successfully inserted " + successfulInserts + " sale records for customer ID " + customerId);
            }

            conn.commit();
            System.out.println("Order placed successfully for user: " + userEmail);

            Collection<AddToCartServlet.CartItem> purchasedItems = new ArrayList<>(cart.values());
            req.setAttribute("purchasedItems", purchasedItems);
            req.setAttribute("saleDate", saleDate);

            session.removeAttribute(AddToCartServlet.CART_ATTR);

            // Forward to confirmation page located in WEB-INF
            req.getRequestDispatcher("/WEB-INF/confirmation.jsp").forward(req, resp);

        } catch (SQLException e) {
            System.err.println("SQL Error during order placement for user " + userEmail + ": " + e.getMessage());
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { System.err.println("Error during transaction rollback: " + ex.getMessage()); }
            forwardToPaymentWithError(req, resp, "A database error occurred while processing your order. Please try again.");

        } catch (Exception e) {
            System.err.println("Unexpected Error during order placement for user " + userEmail + ": " + e.getMessage());
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* ignored */ }
            forwardToPaymentWithError(req, resp, "An unexpected error occurred. Please try again.");

        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException ex) {
                System.err.println("Error closing connection: " + ex.getMessage());
            }
        }
    }

    private void forwardToPaymentWithError(HttpServletRequest req, HttpServletResponse resp, String errorMessage)
            throws ServletException, IOException {
        req.setAttribute("error", errorMessage);
        // Path should point to where your payment.jsp is located (inside WEB-INF as per image_6e3483.png)
        req.getRequestDispatcher("/WEB-INF/payment.jsp").forward(req, resp);
    }
}