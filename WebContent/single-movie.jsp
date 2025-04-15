<%--
  Created by IntelliJ IDEA.
  User: cesar
  Date: 4/13/25
  Time: 10:38 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.*, javax.naming.*, javax.sql.DataSource" %>
<html>
<head>
    <meta charset="UTF-8">
    <title>Single Movie</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
<%
    String movieId = request.getParameter("id");

    try {
        Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:comp/env");
        DataSource ds = (DataSource) envCtx.lookup("jdbc/moviedb");
        Connection conn = ds.getConnection();

        String query =
                "SELECT m.title, m.year, m.director, r.rating " +
                        "FROM movies m, ratings r " +
                        "WHERE m.id = ? AND m.id = r.movieId";

        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, movieId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            response.getWriter().println("<h1>" + rs.getString("title") + "</h1>");
            response.getWriter().println("<p>Year: " + rs.getString("year") + "</p>");
            response.getWriter().println("<p>Director: " + rs.getString("director") + "</p>");
            response.getWriter().println("<p>Rating: " + rs.getString("rating") + "</p>");
        }

        rs.close();
        ps.close();

        // Fetch stars
        response.getWriter().println("<p>Stars:</p><ul>");
        ps = conn.prepareStatement(
                "SELECT s.id, s.name FROM stars s, stars_in_movies sm " +
                        "WHERE sm.movieId = ? AND sm.starId = s.id");
        ps.setString(1, movieId);
        rs = ps.executeQuery();
        while (rs.next()) {
            response.getWriter().println("<li><a href='single-star.jsp?id=" + rs.getString("id") + "'>" +
                    rs.getString("name") + "</a></li>");
        }
        response.getWriter().println("</ul>");

        rs.close();
        ps.close();
        conn.close();
    } catch (Exception e) {
        response.getWriter().println("<p>Error: " + e.getMessage() + "</p>");
    }
%>
<a href="movie-list.jsp">Back to Movie List</a>
</body>
</html>
