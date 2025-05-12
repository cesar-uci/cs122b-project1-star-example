<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Fabflix Login</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"/>

  <!-- Classic reCAPTCHA v2 -->
  <script src="https://www.google.com/recaptcha/api.js" async defer></script>
</head>
<body>
<div class="login-wrapper">
  <div class="login-card">
    <h2>Login</h2>
    <form id="loginForm"
          action="${pageContext.request.contextPath}/login"
          method="post">
      <label for="email">Email</label>
      <input type="email" id="email"
             name="email" required/>

      <label for="password">Password</label>
      <input type="password" id="password"
             name="password" required/>

      <!-- reCAPTCHA widget -->
      <div class="g-recaptcha"
           data-sitekey="6LfBczQrAAAAAJgTzTfbR1L_1Wh8fWenwvwb-kk3">
      </div>

      <button type="submit">Log In</button>
    </form>

    <c:if test="${param.error == 'bot'}">
      <p class="error">⚠️ Bot check failed. Please try again.</p>
    </c:if>
    <c:if test="${param.error == 'invalid'}">
      <p class="error">⚠️ Invalid email or password.</p>
    </c:if>
  </div>
</div>
</body>
</html>
