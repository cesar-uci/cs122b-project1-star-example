<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page import="java.util.*, java.net.URLEncoder" %>
<%-- No direct database imports should be needed here; servlet handles data --%>

<%
    // Data is expected to be set as request attributes by SingleStarServlet
    String starName = (String) request.getAttribute("starName");
    Integer starBirthYear = (Integer) request.getAttribute("starBirthYear"); // Can be null if not set or 0 from DB
    List<Map<String, String>> moviesByStar = (List<Map<String, String>>) request.getAttribute("moviesByStar");

    // starId is also passed as an attribute by the servlet for consistency
    String starId = (String) request.getAttribute("starId");

    // For "Back to Movie List" link state preservation - passed by servlet
    String backQS = (String) request.getAttribute("backQS");
    if (backQS == null) {
        backQS = ""; // Default to empty if not set by servlet
    }

    // HTML Escaping utility class (defined within the JSP for simplicity here)
    // In a larger application, this would be a separate utility class.
    class SingleStarPageHtmlescaper {
        public String escape(String text) {
            if (text == null) return "";
            return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    }
    SingleStarPageHtmlescaper escaper = new SingleStarPageHtmlescaper();
%>

<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= starName != null ? escaper.escape(starName) : "Star Details" %></title>
    <%-- Corrected CSS path using pageContext.request.contextPath --%>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="page-bg">
    <header class="app-header">
        <h1><%= starName != null ? escaper.escape(starName) : "Star Details" %></h1>
        <div>
            <a href="${pageContext.request.contextPath}/shopping-cart.jsp" class="btn-secondary" style="margin-right: 10px;">Checkout 🛒</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-secondary">Logout</a>
        </div>
    </header>

    <main class="container card">
        <p>
            <%-- Corrected link path and added context path --%>
            <a class="back-link" href="${pageContext.request.contextPath}/movie-list.jsp?<%= escaper.escape(backQS) %>">
                &larr; Back to Movie List
            </a>
        </p>

        <% if (starName != null) { %>
        <div class="details">
            <p><strong>Name:</strong> <%= escaper.escape(starName) %></p>
            <p><strong>Birth Year:</strong> <%= (starBirthYear != null && starBirthYear != 0) ? starBirthYear.toString() : "N/A" %></p>

            <h3>Movies Acted In:</h3>
            <% if (moviesByStar != null && !moviesByStar.isEmpty()) { %>
            <ul class="movies"> <%-- Reusing 'movies' class for styling consistency --%>
                <% for (Map<String, String> movie : moviesByStar) { %>
                <li>
                    <%-- Link to SingleMovieServlet, ensuring movieId parameter is used --%>
                    <a href="${pageContext.request.contextPath}/single-movie?movieId=<%= escaper.escape(movie.get("id")) %>&<%= escaper.escape(backQS) %>">
                        <%= escaper.escape(movie.get("title")) %>
                    </a>
                    (Year: <%= escaper.escape(movie.get("year")) %>)
                </li>
                <% } %>
            </ul>
            <% } else { %>
            <p>No movies found for this star.</p>
            <% } %>
        </div>
        <% } else { %>
        <p>Star information not found. Please ensure you navigated here from a valid link.</p>
        <p>Debug Info: starId attribute was: <%= escaper.escape(starId) %></p>
        <% } %>
    </main>
</div>
</body>
</html>
