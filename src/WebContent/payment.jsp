<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page import="java.util.Map, uci122b.AddToCartServlet, uci122b.AddToCartServlet.CartItem" %>
<%
  if (session == null || session.getAttribute("userEmail") == null) {
    resp.sendRedirect(req.getContextPath() + "/login");
    return;
  }

  @SuppressWarnings("unchecked")
  Map<String,CartItem> cart =
          (Map<String,CartItem>) session.getAttribute(AddToCartServlet.CART_ATTR);

  double total = 0;
  if (cart != null) {
    for (CartItem i : cart.values()) {
      total += i.getPrice() * i.getQuantity();
    }
  }

  String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Payment</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="container">
  <div class="payment-card">

    <div class="header">
      <a href="shopping-cart.jsp" class="back-link">← Back to Cart</a>
      <h1>Payment</h1>
    </div>

    <p>Your total is <strong>$<%=String.format("%.2f", total)%></strong></p>

    <% if (error != null) { %>
    <p class="error"><%= error %></p>
    <% } %>

    <form action="place-order" method="post" class="payment-form">
      <input type="hidden" name="total" value="<%= total %>"/>

      <div class="form-group">
        <label for="firstName">First Name</label>
        <input id="firstName" class="input-field" type="text" name="firstName" required>
      </div>

      <div class="form-group">
        <label for="lastName">Last Name</label>
        <input id="lastName" class="input-field" type="text" name="lastName" required>
      </div>

      <div class="form-group">
        <label for="ccNumber">Credit Card Number</label>
        <input id="ccNumber" class="input-field" type="text" name="ccNumber" required>
      </div>

      <div class="form-group">
        <label for="expiration">Expiration Date</label>
        <input id="expiration" class="input-field" type="date" name="expiration" required>
      </div>

      <button type="submit" class="btn-primary">Place Order</button>
    </form>

  </div>
</div>
</body>
</html>
