<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page import="java.util.Map, uci122b.AddToCartServlet, uci122b.AddToCartServlet.CartItem" %>
<%
  // logged-in
  if (session == null || session.getAttribute("userEmail") == null) {
    response.sendRedirect(request.getContextPath() + "/login");     return;
  }
  @SuppressWarnings("unchecked")
  Map<String,CartItem> cart =
          (Map<String,CartItem>) session.getAttribute(AddToCartServlet.CART_ATTR);
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Your Cart</title>
  <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="container">
  <div class="card">
    <!-- header -->
    <div class="header">
      <a href="movie-list.jsp">&larr; Continue Shopping</a>
      <h1>Your Cart</h1>
      <div><!-- spacer --></div>
    </div>

    <%
      if (cart == null || cart.isEmpty()) {
    %>
    <p style="text-align:center; color:#718096;">Your shopping cart is empty.</p>
    <%
    } else {
      double total = 0;
    %>
    <table class="cart-table">
      <thead>
      <tr>
        <th>Title</th>
        <th>Price</th>
        <th>Qty</th>
        <th>Subtotal</th>
        <th>Action</th>
      </tr>
      </thead>
      <tbody>
      <%
        for (CartItem item : cart.values()) {
          double sub = item.getPrice() * item.getQuantity();
          total += sub;
      %>
      <tr>
        <td><%=item.getTitle()%></td>
        <td>$<%=String.format("%.2f", item.getPrice())%></td>
        <td>
          <form action="update-cart" method="post" class="inline-form">
            <input type="hidden" name="movieId" value="<%=item.getMovieId()%>"/>
            <input type="number"
                   name="quantity"
                   min="0"
                   value="<%=item.getQuantity()%>"
                   class="qty-input"/>
            <button type="submit" class="btn btn-secondary">Update</button>
          </form>
        </td>
        <td>$<%=String.format("%.2f", sub)%></td>
        <td>
          <form action="remove-from-cart" method="post" class="inline-form">
            <input type="hidden" name="movieId" value="<%=item.getMovieId()%>"/>
            <button type="submit" class="btn btn-secondary">Delete</button>
          </form>
        </td>
      </tr>
      <%
        }
      %>
      </tbody>
      <tfoot>
      <tr>
        <td colspan="3" class="text-right">Total:</td>
        <td colspan="2">$<%=String.format("%.2f", total)%></td>
      </tr>
      </tfoot>
    </table>

    <div class="text-right">
      <form method="GET" action="${pageContext.request.contextPath}/payment"> <%-- MODIFIED to servlet URL --%>
        <button type="submit" class="btn btn-primary">Proceed to Payment</button>
      </form>
    </div>
    <%
      }
    %>
  </div>
</div>
</body>
</html>
