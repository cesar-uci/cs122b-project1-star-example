<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Fabflix Login</title>
  <%-- Use JSTL Expression Language to build a reliable path to the CSS file --%>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"/>

  <%-- Load reCAPTCHA v2 Checkbox JS API --%>
  <script src="https://www.google.com/recaptcha/api.js" async defer></script>

  <%-- Load jQuery library, required for the AJAX script --%>
  <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
</head>
<body>
<div class="login-wrapper">
  <div class="login-card">
    <h1>Fabflix Login</h1>

    <%-- This div will be used by JavaScript to display any login errors returned from the server --%>
    <div id="login_error_message" class="error" style="display:none;"></div>

    <%-- Display a success message if the user was just logged out --%>
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
        <input type="password" id="password" name="password" required/>
      </div>

      <%-- Google reCAPTCHA Widget --%>
      <div class="form-group" style="margin-top: 1.5rem; margin-bottom: 1.5rem; display: flex; justify-content: center;">
        <div class="g-recaptcha" data-sitekey="6LdLYTYrAAAAAOgviHSBcxtO3582k5L5IYxx56Jo"></div>
      </div>

      <button type="submit" class="btn-primary">Log In</button>
    </form>
  </div>
</div>

<script>
  // This script handles the login form submission using AJAX
  function handleLoginFormSubmit(event) {
    // Prevent the form from doing a traditional submission, which would reload the page
    event.preventDefault();
    console.log("Login form submitted. Preparing AJAX request...");

    // Hide any previous error messages
    $('#login_error_message').hide();

    // Perform the AJAX POST request to the LoginServlet
    $.ajax({
      method: "POST",
      // Use the context path to build a reliable URL to your servlet
      url: "${pageContext.request.contextPath}/login",
      // Serialize the form data into a URL-encoded string (e.g., "email=a@a.com&password=...")
      data: $('#login_form').serialize(),
      success: function(response) {
        // This function is called when the server returns a successful (2xx) status code
        console.log("AJAX request successful. Server response:", response);
        // On success, redirect the user to the main page
        window.location.replace("index.html");
      },
      error: function(jqXHR) {
        // This function is called when the server returns an error (4xx or 5xx) status code
        console.error("AJAX request failed. Status:", jqXHR.status);
        let errorMessage = "An unknown error occurred. Please try again.";
        // The server should send back a JSON object with an error message
        if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
          errorMessage = jqXHR.responseJSON.message;
        }
        // Display the error message in the designated div
        $('#login_error_message').text(errorMessage).show();
      }
    });
  }

  // When the document is fully loaded, attach the submit event handler to the form
  $(document).ready(function() {
    $('#login_form').on('submit', handleLoginFormSubmit);
  });
</script>

</body>
</html>
