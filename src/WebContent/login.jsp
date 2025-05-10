<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Fabflix Login</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
  <script
          src="https://www.google.com/recaptcha/enterprise.js?render=6LfBczQrAAAAAJgTzTfbR1L_1Wh8fWenwvwb-kk3">
  </script>
</head>
<body>
<div class="login-wrapper">
  <div class="login-card">
    <h2>Login</h2>
    <form id="loginForm" action="${pageContext.request.contextPath}/login" method="post">
      <label for="email">Email</label>
      <input type="email" id="email" name="email" required />

      <label for="password">Password</label>
      <input type="password" id="password" name="password" required />

      <input type="hidden" name="g-recaptcha-response" id="g-recaptcha-response" />

      <button type="submit" id="loginBtn">Log In</button>
    </form>

    <c:if test="${param.error == 'invalid'}">
      <p class="error">⚠️ Invalid email or password.</p>
    </c:if>
    <c:if test="${param.error == 'bot'}">
      <p class="error">⚠️ Bot verification failed. Please try again.</p>
    </c:if>
  </div>
</div>

<script>
  document.getElementById('loginBtn').addEventListener('click', function(e) {
    e.preventDefault();
    grecaptcha.enterprise.ready(async () => {
      const token = await grecaptcha.enterprise.execute(
              '6LfBczQrAAAAAJgTzTfbR1L_1Wh8fWenwvwb-kk3',
              { action: 'login' }
      );
      document.getElementById('g-recaptcha-response').value = token;
      document.getElementById('loginForm').submit();
    });
  });
</script>
</body>
</html>
