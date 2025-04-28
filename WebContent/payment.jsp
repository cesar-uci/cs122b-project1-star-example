<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page import="java.util.Map, uci122b.AddToCartServlet, uci122b.AddToCartServlet.CartItem" %>
<%
  if (session == null || session.getAttribute("userEmail") == null) {
    response.sendRedirect("login.html");
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
<div class="container payment-card">
  <div class="header">
    <a href="shopping-cart.jsp" class="back">&larr; Back to Cart</a>
    <h1>Payment</h1>
  </div>

  <p class="total-line">
    Your total is <span class="total-amount">$<%=String.format("%.2f", total)%></span>
  </p>

  <%
    if (error != null) {
  %>
  <p class="error-msg"><%= error %></p>
  <%
    }
  %>

  <form action="place-order" method="post" class="payment-form">
    <!-- server will recompute / validate total if desired -->
    <input type="hidden" name="total" value="<%= total %>"/>

    <label>First Name:
      <input type="text" name="firstName" required class="input-field"/>
    </label>

    <label>Last Name:
      <input type="text" name="lastName" required class="input-field"/>
    </label>

    <label>Credit Card Number:
      <input type="text" name="ccNumber" required class="input-field"/>
    </label>

    <label>Expiration Date:
      <input type="month" name="expiration" required class="input-field"/>
    </label>

    <button type="submit" class="btn-primary">Place Order</button>
  </form>
</div>
</body>
</html>
