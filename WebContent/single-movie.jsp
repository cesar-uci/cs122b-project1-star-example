<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page import="java.sql.*, javax.naming.*, javax.sql.DataSource, java.util.*, java.net.URLEncoder" %>
<%
    String movieId = request.getParameter("id");
    String fullQS  = request.getQueryString();
    String backQS  = "";
    if (fullQS != null && fullQS.contains("&")) {
        backQS = fullQS.substring(fullQS.indexOf("&") + 1);
    }

    Context initCtx = new InitialContext();
    DataSource ds   = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb");
    Connection conn = ds.getConnection();

    // movie details
    PreparedStatement mvStmt = conn.prepareStatement(
            "SELECT m.title,m.year,m.director,COALESCE(r.rating,0) AS rating " +
                    "FROM movies m LEFT JOIN ratings r ON m.id=r.movieId WHERE m.id=?"
    );
    mvStmt.setString(1, movieId);
    ResultSet mvRs = mvStmt.executeQuery();
    String title="", director="";
    int year=0;
    double rating=0;
    if (mvRs.next()) {
        title    = mvRs.getString("title");
        year     = mvRs.getInt("year");
        director = mvRs.getString("director");
        rating   = mvRs.getDouble("rating");
    }
    mvRs.close(); mvStmt.close();

    PreparedStatement gnStmt = conn.prepareStatement(
            "SELECT g.name FROM genres g " +
                    "JOIN genres_in_movies gm ON g.id=gm.genreId " +
                    "WHERE gm.movieId=? ORDER BY g.name LIMIT 3"
    );
    gnStmt.setString(1, movieId);
    ResultSet gnRs = gnStmt.executeQuery();
    List<String> genres = new ArrayList<>();
    while (gnRs.next()) genres.add(gnRs.getString("name"));
    gnRs.close(); gnStmt.close();

    PreparedStatement stStmt = conn.prepareStatement(
            "SELECT s.id,s.name FROM stars s " +
                    "JOIN stars_in_movies sm ON s.id=sm.starId " +
                    "WHERE sm.movieId=? " +
                    "ORDER BY (SELECT COUNT(*) FROM stars_in_movies sim2 WHERE sim2.starId=s.id) DESC, s.name ASC " +
                    "LIMIT 3"
    );
    stStmt.setString(1, movieId);
    ResultSet stRs = stStmt.executeQuery();
    List<String[]> stars = new ArrayList<>();
    while (stRs.next()) {
        stars.add(new String[]{ stRs.getString("id"), stRs.getString("name") });
    }
    stRs.close(); stStmt.close();

    conn.close();
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title><%= title %></title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="page-bg">
    <div class="container">
        <div class="card">

            <!-- header bar -->
            <div class="header">
                <h1><%= title %></h1>
                <a href="shopping-cart.jsp" class="btn-secondary">Checkout 🛒</a>
            </div>

            <!-- add-to-cart -->
            <form method="POST" action="add-to-cart" style="margin-bottom:1rem;">
                <input type="hidden" name="movieId"    value="<%= movieId %>">
                <input type="hidden" name="movieTitle" value="<%= title %>">
                <input type="hidden" name="price"      value="10.00">
                <button type="submit">Add to Cart ($10.00)</button>
            </form>

            <!-- back link -->
            <p><a class="back-link" href="movie-list.jsp?<%= backQS %>">
                ← Back to Movie List
            </a></p>

            <!-- details -->
            <div class="details">
                <p><strong>Year:</strong> <%= year %>,
                    <strong>Director:</strong> <%= director %>,
                    <strong>Rating:</strong> <%= String.format("%.1f", rating) %>
                </p>
                <p><strong>Genres:</strong>
                    <% for (int i = 0; i < genres.size(); i++) { %>
                    <a href="movie-list.jsp?genre=<%= URLEncoder.encode(genres.get(i),"UTF-8") %>">
                        <%= genres.get(i) %>
                    </a><%= (i < genres.size()-1 ? ", " : "") %>
                    <% } %>
                </p>
                <p><strong>Stars:</strong>
                    <% for (int i = 0; i < stars.size(); i++) {
                        String sid = stars.get(i)[0], sname = stars.get(i)[1];
                    %>
                    <a href="single-star.jsp?id=<%= sid %>&<%= backQS %>">
                        <%= sname %>
                    </a><%= (i < stars.size()-1 ? ", " : "") %>
                    <% } %>
                </p>
            </div>

        </div><!-- .card -->
    </div><!-- .container -->
</div><!-- .page-bg -->
</body>
</html>
