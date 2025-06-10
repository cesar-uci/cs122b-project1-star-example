<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fabflix Login</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"/>
    <script src="https://www.google.com/recaptcha/api.js" async defer></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
</head>
<body>
<div class="login-wrapper">
    <div class="login-card">
        <h1>Fabflix Login</h1>
        <div id="login_error_message" class="error" style="display:none;"></div>
        <% if ("logout_success".equals(request.getParameter("message"))) { %>
        <p style="color: green; text-align: center; margin-bottom: 1rem;">You have been logged out successfully.</p>
        <% } %>
        <form id="login_form" method="post">
            <div class="form-group">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" required/>
            </div>
            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" required/>
            </div>
            <div class="form-group" style="margin-top: 1.5rem; margin-bottom: 1.5rem; display: flex; justify-content: center;">
                <div class="g-recaptcha" data-sitekey="6LdLYTYrAAAAAOgviHSBcxtO3582k5L5IYxx56Jo"></div>
            </div>
            <button type="submit" class="btn-primary">Log In</button>
        </form>
    </div>
</div>
<script>
    function handleLoginFormSubmit(event) {
        event.preventDefault();
        $('#login_error_message').hide();
        $.ajax({
            method: "POST",
            url: "${pageContext.request.contextPath}/login",
            data: $('#login_form').serialize(),
            success: function(response) {
                window.location.replace("index.html");
            },
            error: function(jqXHR) {
                let errorMessage = "An unknown error occurred.";
                if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
                    errorMessage = jqXHR.responseJSON.message;
                }
                $('#login_error_message').text(errorMessage).show();
            }
        });
    }
    $(document).ready(function() {
        $('#login_form').on('submit', handleLoginFormSubmit);
    });
</script>
</body>
</html>
