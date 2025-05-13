<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page import="java.sql.*, javax.naming.*, javax.sql.DataSource, java.util.*, java.net.URLEncoder" %>
<%@ page import="uci122b.Star" %>
<%
    // These attributes should be set by SingleMovieServlet.java
    String movieTitle = (String) request.getAttribute("title");
    Integer movieYear = (Integer) request.getAttribute("year");
    String movieDirector = (String) request.getAttribute("director");
    Float movieRating = (Float) request.getAttribute("rating");
    List<String> genreList = (List<String>) request.getAttribute("genres");
    List<Star> starList = (List<Star>) request.getAttribute("stars");

    String movieIdFromAttribute = (String) request.getAttribute("movieId"); // Prefer getting from attribute
    String movieId = movieIdFromAttribute != null ? movieIdFromAttribute : request.getParameter("movieId");


    // For "Back to Movie List" link state preservation
    String backQS = (String) request.getAttribute("backQS");
    if (backQS == null) {
        backQS = ""; // Initialize to empty string if not provided by servlet
        // Fallback logic can be more complex if needed, but servlet should ideally prepare this.
        // For simplicity, if servlet doesn't provide it, the link might be less stateful.
        // A minimal fallback:
        String sortByParam = request.getParameter("sortBy") != null ? request.getParameter("sortBy") : "rating";
        String sortDirParam = request.getParameter("sortDir") != null ? request.getParameter("sortDir") : "desc";
        String pageSizeParam = request.getParameter("pageSize") != null ? request.getParameter("pageSize") : "10";
        String pageParam = request.getParameter("page") != null ? request.getParameter("page") : "1";

        StringBuilder fallbackQS = new StringBuilder();
        try {
            fallbackQS.append("sortBy=").append(URLEncoder.encode(sortByParam, "UTF-8"));
            fallbackQS.append("&sortDir=").append(URLEncoder.encode(sortDirParam, "UTF-8"));
            fallbackQS.append("&pageSize=").append(URLEncoder.encode(pageSizeParam, "UTF-8"));
            fallbackQS.append("&page=").append(URLEncoder.encode(pageParam, "UTF-8"));

            // Append original filter parameters if they were part of the URL to single-movie
            // These should ideally be part of the backQS constructed by the servlet
            String[] paramsToCarry = {"title", "year", "director", "star", "genre", "letter"};
            for (String paramName : paramsToCarry) {
                String paramValue = request.getParameter(paramName); // Check original request for these
                if (paramValue != null && !paramValue.isEmpty()) {
                    fallbackQS.append("&").append(paramName).append("=").append(URLEncoder.encode(paramValue, "UTF-8"));
                }
            }
            backQS = fallbackQS.toString();
        } catch (java.io.UnsupportedEncodingException e) {
            // Should not happen with UTF-8
            e.printStackTrace();
        }
    }

    // Define the Htmlescape class (non-static method)
    class SingleMovieHtmlescaper { // Renamed slightly to avoid confusion if old class is cached
        public String escape(String text) {
            if (text == null) return "";
            return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    }
    SingleMovieHtmlescaper escaper = new SingleMovieHtmlescaper(); // Create an instance
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= movieTitle != null ? escaper.escape(movieTitle) : "Movie Details" %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="page-bg">
    <header class="app-header">
        <h1><%= movieTitle != null ? escaper.escape(movieTitle) : "Movie Details" %></h1>
        <div>
            <a href="${pageContext.request.contextPath}/shopping-cart.jsp" class="btn-secondary" style="margin-right: 10px;">Checkout 🛒</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-secondary">Logout</a>
        </div>
    </header>

    <main class="container card">
        <p>
            <a class="back-link" href="${pageContext.request.contextPath}/movie-list.jsp?<%= escaper.escape(backQS) %>">
                &larr; Back to Movie List
            </a>
        </p>

        <% if (movieTitle != null) { %>
        <form method="POST" action="${pageContext.request.contextPath}/add-to-cart" style="margin-bottom:1rem;">
            <input type="hidden" name="movieId"    value="<%= escaper.escape(movieId) %>">
            <input type="hidden" name="movieTitle" value="<%= escaper.escape(movieTitle) %>">
            <input type="hidden" name="price"      value="9.99"> <%-- Adjust price as needed --%>
            <button type="submit" class="btn-primary">Add to Cart ($9.99)</button>
        </form>

        <div class="details">
            <p><strong>Year:</strong> <%= movieYear != null ? movieYear : "N/A" %></p>
            <p><strong>Director:</strong> <%= movieDirector != null ? escaper.escape(movieDirector) : "N/A" %></p>
            <p><strong>Rating:</strong> <%= movieRating != null ? String.format("%.1f", movieRating) : "N/A" %></p>
            <p><strong>Genres:</strong>
                <% if (genreList != null && !genreList.isEmpty()) {
                    for (int i = 0; i < genreList.size(); i++) {
                        String currentGenre = genreList.get(i);
                %>
                <a href="${pageContext.request.contextPath}/browse?genre=<%= URLEncoder.encode(currentGenre, "UTF-8") %>">
                    <%= escaper.escape(currentGenre) %>
                </a><%= (i < genreList.size() - 1 ? ", " : "") %>
                <%  }
                } else { %>
                N/A
                <% } %>
            </p>
            <p><strong>Stars:</strong>
                <% if (starList != null && !starList.isEmpty()) {
                    for (int i = 0; i < starList.size(); i++) {
                        Star currentStar = starList.get(i);
                %>
                <a href="${pageContext.request.contextPath}/single-star?starId=<%= escaper.escape(currentStar.getId()) %>&<%= escaper.escape(backQS) %>">
                    <%= escaper.escape(currentStar.getName()) %>
                </a><%= (i < starList.size() - 1 ? ", " : "") %>
                <%  }
                } else { %>
                N/A
                <% } %>
            </p>
        </div>
        <% } else { %>
        <p>Movie information not found. Please ensure you navigated here from a valid movie link.</p>
        <p>Debug Info: movieId parameter was: <%= escaper.escape(request.getParameter("movieId")) %>, movieTitle attribute was: <%= escaper.escape(movieTitle) %></p>
        <% } %>
    </main> <%-- Closing main tag --%>
</div> <%-- Closing page-bg tag --%>
</body>
</html>
