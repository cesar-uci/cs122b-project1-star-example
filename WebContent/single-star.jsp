<%--
  Created by IntelliJ IDEA.
  User: cesar
  Date: 4/13/25
  Time: 10:39 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.*, javax.naming.*, javax.sql.DataSource" %>
<html>
<head>
    <meta charset="UTF-8">
    <title>Single Star</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
<%
    String starId = request.getParameter("id");

    try {
        Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:comp/env");
        DataSource ds = (DataSource) envCtx.lookup("jdbc/moviedb");
        Connection conn = ds.getConnection();

        String query = "SELECT name, birthYear FROM stars WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, starId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            response.getWriter().println("<h1>" + rs.getString("name") + "</h1>");
            response.getWriter().println("<p>Birth Year: " + (rs.getString("birthYear") == null ? "N/A" : rs.getString("birthYear")) + "</p>");
        }

        rs.close();
        ps.close();

        response.getWriter().println("<p>Movies:</p><ul>");
        ps = conn.prepareStatement("SELECT m.id, m.title FROM movies m, stars_in_movies sm " + "WHERE sm.starId = ? AND sm.movieId = m.id");
        ps.setString(1, starId);
        rs = ps.executeQuery();

        while (rs.next()) {
            response.getWriter().println("<li><a href='single-movie.jsp?id=" + rs.getString("id") + "'>" +
                    rs.getString("title") + "</a></li>");
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
