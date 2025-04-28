<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="uci122b.AddToCartServlet.CartItem, java.util.*" %>
<%
  @SuppressWarnings("unchecked")
  Collection<CartItem> items =
          (Collection<CartItem>) request.getAttribute("purchasedItems");
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Order Confirmation</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>
<h1>Order Confirmation</h1>
<p>Thank you for your purchase! Here’s what you bought today:</p>

<table border="1" cellpadding="5" cellspacing="0">
  <tr><th>Title</th><th>Quantity</th></tr>
  <% for (CartItem it : items) { %>
  <tr>
    <td><%= it.getTitle() %></td>
    <td><%= it.getQuantity() %></td>
  </tr>
  <% } %>
</table>

<p><a href="movie-list.jsp">Continue Shopping</a></p>
</body>
</html>
