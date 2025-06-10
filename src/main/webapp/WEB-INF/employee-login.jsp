<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head>
  <title>Employee Login</title>
  <style>
    body { font-family: sans-serif; padding: 2em; }
    .container { max-width: 400px; margin: auto; }
    label { display: block; margin: .5em 0 0.2em; }
    input { width: 100%; padding: .4em; box-sizing: border-box; }
    button { margin-top: 1em; padding: .6em; width: 100%; }
    .error { color: red; margin-bottom: 1em; }
  </style>
</head>
<body>
<div class="container">
  <h1>Employee Login</h1>

  <c:if test="${not empty param.error}">
    <div class="error">Invalid email or password.</div>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/_dashboard">
    <input type="hidden" name="action" value="login"/>

    <label>Email:
      <input type="email" name="email" required/>
    </label>

    <label>Password:
      <input type="password" name="password" required/>
    </label>

    <button type="submit">Log In</button>
  </form>
</div>
</body>
</html>
