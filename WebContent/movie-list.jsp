<%--
  Created by IntelliJ IDEA.
  User: cesar
  Date: 4/13/25
  Time: 10:37 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.*, javax.naming.*, javax.sql.DataSource" %>
<html>
<head>
    <meta charset="UTF-8">
    <title>Movie List</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
<h1>Top 20 Rated Movies</h1>
<ul>
    <%
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            DataSource ds = (DataSource) envCtx.lookup("jdbc/moviedb");
            Connection conn = ds.getConnection();

            String query =
                    "SELECT m.id, m.title, m.year, m.director, r.rating " +
                            "FROM movies m, ratings r " +
                            "WHERE m.id = r.movieId " +
                            "ORDER BY r.rating DESC LIMIT 20";

            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                String id = rs.getString("id");
                String title = rs.getString("title");
                String year = rs.getString("year");
                String director = rs.getString("director");
                String rating = rs.getString("rating");
    %>
    <li>
        <a href="single-movie.jsp?id=<%= id %>"><%= title %></a><br/>
        Year: <%= year %>, Director: <%= director %>, Rating: <%= rating %>
    </li>
    <%
            }

            rs.close();
            statement.close();
            conn.close();
        } catch (Exception e) {
            response.getWriter().print("<p>Error: " + e.getMessage() + "</p>");
        }
    %>
</ul>
</body>
</html>