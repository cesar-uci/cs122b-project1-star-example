<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <title>Fabflix Login</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"/>

  <%-- 1) Load reCAPTCHA v2 Checkbox JS API --%>
  <script src="https://www.google.com/recaptcha/api.js" async defer></script>
</head>
<body>
<div class="login-wrapper">
  <div class="login-card">
    <h2>Login</h2>

    <%-- Error messages from LoginServlet --%>
    <% String error = request.getParameter("error"); %>
    <% if (error != null) { %>
    <p class="error">
      <% if ("recaptcha_missing".equals(error)) { %>
      Please complete the reCAPTCHA.
      <% } else if ("missing_credentials".equals(error)) { %>
      Email or password missing.
      <% } else if ("recaptcha_error".equals(error)) { %>
      reCAPTCHA verification error. Please try again.
      <% } else if ("bot".equals(error)) { %>
      reCAPTCHA verification failed.
      <% } else if ("db_error".equals(error)) { %>
      Database error. Please try again later.
      <% } else if ("invalid".equals(error)) { %>
      ⚠️ Invalid email or password.
      <% } else { %>
      Login failed. Please try again.
      <% } %>
    </p>
    <% } %>
    <%-- JSTL error messages (kept your original logic here) --%>
    <c:if test="${param.error == 'bot'}">
      <p class="error">⚠️ Bot verification failed. Please try again.</p>
    </c:if>
    <c:if test="${param.error == 'invalid'}">
      <p class="error">⚠️ Invalid email or password.</p>
    </c:if>
    <% String message = request.getParameter("message"); %>
    <% if ("logout_success".equals(message)) { %>
    <p class="success-message">You have been logged out successfully.</p>
    <% } %>


    <form id="loginForm"
          action="${pageContext.request.contextPath}/login"
          method="post">

      <label for="email">Email</label>
      <input type="email" id="email" name="email" placeholder="you@example.com" required/>

      <label for="password">Password</label>
      <input type="password" id="password" name="password" required/>

      <%-- reCAPTCHA v2 Checkbox Widget --%>
      <%-- The SITE_KEY is passed from LoginServlet's doGet as 'siteKey' request attribute --%>
      <% String siteKey = (String) request.getAttribute("siteKey"); %>
      <% if (siteKey != null && !siteKey.isEmpty()) { %>
      <div class="form-group" style="margin-top: 1rem; margin-bottom: 1rem; display: flex; justify-content: center;"> {/* Added form-group for potential styling and centering */}
        <div class="g-recaptcha" data-sitekey="<%= siteKey %>"></div>
      </div>
      <% } else { %>
      <p class="error">reCAPTCHA not configured (site key missing).</p>
      <% } %>

      <%-- The g-recaptcha-response field is automatically managed by the v2 API --%>
      <%-- No need for a separate hidden input manually populated by JS for v2 checkbox --%>

      <button type="submit" id="loginBtn">Log In</button> <%-- Changed back to type="submit" --%>
    </form>
  </div>
</div>

<%-- Removed the v3/Enterprise specific JavaScript block --%>

</body>
</html>