<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="uci122b.AddToCartServlet.CartItem, java.util.*" %>
<%
  @SuppressWarnings("unchecked")
  Collection<CartItem> items =
          (Collection<CartItem>) request.getAttribute("purchasedItems");
  if (items == null) {
    items = Collections.emptyList();
  }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Order Confirmation</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"> <%-- CORRECTED --%>
</head>
<body>
<div class="page-bg">
  <div class="container">
    <div class="card">

      <div class="header">
        <h1>Thank you for your order!</h1>
        <a href="${pageContext.request.contextPath}/movie-list.jsp" class="btn-secondary">← Continue Shopping</a> <%-- CORRECTED --%>
      </div>

      <p>Here’s what you purchased today:</p>

      <table class="cart-table">
        <thead>
        <tr><th>Title</th><th>Quantity</th></tr>
        </thead>
        <tbody>
        <% if (items.isEmpty()) { %>
        <tr>
          <td colspan="2">(No items found.)</td>
        </tr>
        <% } else {
          for (CartItem it : items) { %>
        <tr>
          <td><%= it.getTitle() %></td>
          <td><%= it.getQuantity() %></td>
        </tr>
        <%   }
        } %>
        </tbody>
      </table>

    </div>
  </div>
</div>
</body>
</html>