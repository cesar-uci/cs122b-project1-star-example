<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page import="java.sql.*, javax.naming.*, javax.sql.DataSource, java.util.*" %>
<%
    String starId = request.getParameter("id");
    String fullQS = request.getQueryString();
    String backQS = "";
    if (fullQS != null && fullQS.contains("&")) {
        backQS = fullQS.substring(fullQS.indexOf("&") + 1);
    }

    Context initCtx = new InitialContext();
    DataSource ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb");
    Connection conn = ds.getConnection();

    String name = "", birth = "N/A";
    PreparedStatement sStmt = conn.prepareStatement(
            "SELECT name,birthYear FROM stars WHERE id=?"
    );
    sStmt.setString(1, starId);
    ResultSet sRs = sStmt.executeQuery();
    if (sRs.next()) {
        name = sRs.getString("name");
        int by = sRs.getInt("birthYear");
        if (!sRs.wasNull()) birth = Integer.toString(by);
    }
    sRs.close();
    sStmt.close();

    List<String[]> movies = new ArrayList<>();
    PreparedStatement mStmt = conn.prepareStatement(
            "SELECT m.id,m.title FROM movies m " +
                    "JOIN stars_in_movies sm ON m.id=sm.movieId " +
                    "WHERE sm.starId=? " +
                    "ORDER BY m.year DESC, m.title ASC"
    );
    mStmt.setString(1, starId);
    ResultSet mRs = mStmt.executeQuery();
    while (mRs.next()) {
        movies.add(new String[]{ mRs.getString("id"), mRs.getString("title") });
    }
    mRs.close();
    mStmt.close();

    conn.close();
%>

<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title><%= name %></title>
    <link rel="stylesheet" href="css/style.css">
</head>

<body>
<div class="page-bg">
    <div class="container">
        <div class="card">

            <div class="header">
                <h1><%= name %></h1>
                <a href="shopping-cart.jsp" class="btn-secondary">Checkout 🛒</a>
            </div>

            <p><a class="back-link" href="movie-list.jsp?<%= backQS %>">
                ← Back to Movie List
            </a></p>

            <div class="details">
                <p><strong>Birth Year:</strong> <%= birth %></p>
                <ul class="movies">
                    <% for (String[] mv : movies) { %>
                    <li>
                        <a href="single-movie.jsp?id=<%= mv[0] %>&<%= backQS %>">
                            <%= mv[1] %> (<%= mv[0] %>)
                        </a>
                    </li>
                    <% } %>
                </ul>
            </div>

        </div>
    </div>
</div>
</body>
</html>
