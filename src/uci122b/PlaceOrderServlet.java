package uci122b;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.time.YearMonth;
import java.time.LocalDate;
import java.util.Map;

@WebServlet(name = "PlaceOrderServlet", urlPatterns = {"/place-order"})
public class PlaceOrderServlet extends HttpServlet {
    public static final String CART_ATTR = AddToCartServlet.CART_ATTR;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect("login.html");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, AddToCartServlet.CartItem> cart =
                (Map<String, AddToCartServlet.CartItem>) session.getAttribute(CART_ATTR);
        if (cart == null || cart.isEmpty()) {
            resp.sendRedirect("shopping-cart.jsp");
            return;
        }

        String firstName = req.getParameter("firstName");
        String lastName  = req.getParameter("lastName");
        String ccNumber  = req.getParameter("ccNumber");
        String expMonth  = req.getParameter("expiration");

        YearMonth ym = YearMonth.parse(expMonth);
        LocalDate ld = ym.atEndOfMonth();
        java.sql.Date expiration = java.sql.Date.valueOf(ld);

        String userEmail = (String) session.getAttribute("userEmail");

        try {
            Context initCtx = new InitialContext();
            DataSource ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb");

            try (Connection conn = ds.getConnection()) {
                String cardSql =
                        "SELECT id FROM creditcards " +
                                "WHERE id=? AND firstName=? AND lastName=? AND expiration=?";
                try (PreparedStatement cardStmt = conn.prepareStatement(cardSql)) {
                    cardStmt.setString(1, ccNumber);
                    cardStmt.setString(2, firstName);
                    cardStmt.setString(3, lastName);
                    cardStmt.setDate  (4, expiration);

                    try (ResultSet rc = cardStmt.executeQuery()) {
                        if (!rc.next()) {
                            req.setAttribute("error", "Invalid payment information");
                            req.getRequestDispatcher("payment.jsp").forward(req, resp);
                            return;
                        }
                    }
                }

                int customerId;
                String custSql = "SELECT id FROM customers WHERE email=?";
                try (PreparedStatement custStmt = conn.prepareStatement(custSql)) {
                    custStmt.setString(1, userEmail);
                    try (ResultSet rc = custStmt.executeQuery()) {
                        if (!rc.next()) {
                            throw new ServletException("No such customer!");
                        }
                        customerId = rc.getInt("id");
                    }
                }

                String saleSql =
                        "INSERT INTO sales(customerId, movieId, saleDate) " +
                                "VALUES (?, ?, CURRENT_DATE())";
                try (PreparedStatement saleStmt = conn.prepareStatement(saleSql)) {
                    for (AddToCartServlet.CartItem item : cart.values()) {
                        String movieId = item.getMovieId();
                        int qty = item.getQuantity();
                        for (int i = 0; i < qty; i++) {
                            saleStmt.setInt   (1, customerId);
                            saleStmt.setString(2, movieId);
                            saleStmt.addBatch();
                        }
                    }
                    saleStmt.executeBatch();
                }

                session.removeAttribute(CART_ATTR);

                req.getRequestDispatcher("confirmation.jsp").forward(req, resp);
            }

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
