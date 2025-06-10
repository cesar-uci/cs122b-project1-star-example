<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.sql.*, javax.naming.*, javax.sql.DataSource, java.util.*, java.net.URLEncoder, uci122b.Movie" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%!
    public static class Htmlescape {
        public static String escape(String text) {
            if (text == null) return "";
            return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    }
%>

<%
    List<Movie> movies = (List<Movie>) request.getAttribute("movies");
    if (movies == null) {
        movies = new ArrayList<>(); // Initialize to prevent NullPointerExceptions in the loop
    }
    int currentPage = (Integer) (request.getAttribute("currentPage") != null ? request.getAttribute("currentPage") : 1);
    int totalPages = (Integer) (request.getAttribute("totalPages") != null ? request.getAttribute("totalPages") : 0);
    int recordsPerPage = (Integer) (request.getAttribute("recordsPerPage") != null ? request.getAttribute("recordsPerPage") : 10);
    String q_param  = (String) request.getAttribute("queryString");
    String genre    = (String) request.getAttribute("genreName");
    String letter   = (String) request.getAttribute("letterInitial");
    String currentSortByCombined = (String) request.getAttribute("sortBy"); // e.g., "title_asc"
    String formSortBy = "rating"; // Default
    String formSortDir = "desc"; // Default
    if (currentSortByCombined != null && !currentSortByCombined.isEmpty()) {
        String[] sortParts = currentSortByCombined.split("_");
        if (sortParts.length == 2) {
            formSortBy = sortParts[0];
            formSortDir = sortParts[1];
        }
    }
    StringBuilder qsBuilder = new StringBuilder();
    if (q_param  != null && !q_param.isEmpty())  qsBuilder.append("q=").append(URLEncoder.encode(q_param,"UTF-8")).append("&");
    if (genre    != null && !genre.isEmpty()) qsBuilder.append("genre=").append(URLEncoder.encode(genre,"UTF-8")).append("&");
    if (letter   != null && !letter.isEmpty()) qsBuilder.append("letter=").append(URLEncoder.encode(letter,"UTF-8")).append("&");
    qsBuilder.append("sortBy=").append(formSortBy).append("&");
    qsBuilder.append("sortDir=").append(formSortDir).append("&");
    qsBuilder.append("limit=").append(recordsPerPage).append("&");
    String baseQS = qsBuilder.toString();
    boolean hasPreviousPage = currentPage > 1;
    boolean hasNextPage = currentPage < totalPages;
    String errorMessage = (String) request.getAttribute("errorMessage");
    DataSource subQueryDs = null;
    try {
        Context initCtx = new InitialContext();
        subQueryDs = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb_slave");
    } catch (NamingException e) {
        errorMessage = (errorMessage == null ? "" : errorMessage + " ") + "Error: Sub-query DataSource not found. " + e.getMessage();
        System.err.println("movie-list.jsp: JNDI lookup failed for jdbc/moviedb_slave for sub-queries: " + e.getMessage());
    }
%>

<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Movie List</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="page-bg">
    <header class="app-header">
        <h1>Movie List</h1>
        <div>
            <a href="shopping-cart.jsp" class="btn-secondary" style="margin-right: 10px;">Checkout 🛒</a>
            <form method="POST" action="logout" style="display:inline;"> <button type="submit" class="btn-secondary">Logout</button></form>
        </div>
    </header>

    <main class="container card">
        <p>
            <a href="index.html" class="back-link">← Back to Main Page</a>
        </p>
        <% if (errorMessage != null) { %>
        <p class="error"><%= Htmlescape.escape(errorMessage) %></p>
        <% } %>

        <div class="controls">
            <form method="GET" action="${ (q_param != null && !q_param.isEmpty()) ? 'search' : ( (genre != null && !genre.isEmpty()) || (letter != null && !letter.isEmpty()) ? 'browse' : 'search' ) }" class="controls-form">
                <c:if test="${not empty param.q}"> <input type="hidden" name="q" value="${fn:escapeXml(param.q)}"/></c:if>
                <c:if test="${not empty param.genre && empty param.q}"><input type="hidden" name="genre" value="${fn:escapeXml(param.genre)}"/></c:if>
                <c:if test="${not empty param.letter && empty param.q}"><input type="hidden" name="letter" value="${fn:escapeXml(param.letter)}"/></c:if>

                <label for="sortBySelect">Sort by:</label>
                <select name="sortBy" id="sortBySelect">
                    <option value="title"  <%= "title".equals(formSortBy)  ? "selected" : "" %>>Title</option>
                    <option value="rating" <%= "rating".equals(formSortBy) ? "selected" : "" %>>Rating</option>
                </select>

                <label for="sortDirSelect">Direction:</label>
                <select name="sortDir" id="sortDirSelect">
                    <option value="asc"  <%= "asc".equals(formSortDir)  ? "selected" : "" %>>Ascending</option>
                    <option value="desc" <%= "desc".equals(formSortDir) ? "selected" : "" %>>Descending</option>
                </select>

                <label for="pageSizeSelect">Show:</label>
                <select name="limit" id="pageSizeSelect">
                    <option value="10"  <%= recordsPerPage == 10  ? "selected" : "" %>>10</option>
                    <option value="25"  <%= recordsPerPage == 25  ? "selected" : "" %>>25</option>
                    <option value="50"  <%= recordsPerPage == 50  ? "selected" : "" %>>50</option>
                    <option value="100" <%= recordsPerPage == 100 ? "selected" : "" %>>100</option>
                </select> per page

                <button type="submit">Apply</button>
            </form>
        </div>

        <c:choose>
            <c:when test="${empty movies and empty errorMessage}">
                <p>No movies found matching your criteria.</p>
            </c:when>
            <c:when test="${not empty movies}">
                <ul class="movies">
                    <% for (Movie movie : movies) {
                        String id      = movie.getId();
                        String mTitle  = movie.getTitle();
                        int    mYear   = movie.getYear();
                        String mDir    = movie.getDirector();
                        float  mRating = movie.getRating();
                        double mPrice  = 9.99;
                        List<String> gList = new ArrayList<>();
                        List<Map<String, String>> sList = new ArrayList<>();

                        if (subQueryDs != null) {
                            try (Connection subConn = subQueryDs.getConnection()) {
                                // Fetch Genres
                                String genreSql = "SELECT g.name FROM genres g JOIN genres_in_movies gm ON g.id=gm.genreId "
                                        + "WHERE gm.movieId=? ORDER BY g.name ASC LIMIT 3";
                                try (PreparedStatement genreLoopStmt = subConn.prepareStatement(genreSql)) {
                                    genreLoopStmt.setString(1, id);
                                    try (ResultSet genreLoopRs = genreLoopStmt.executeQuery()) {
                                        while (genreLoopRs.next()) {
                                            gList.add(genreLoopRs.getString("name"));
                                        }
                                    }
                                } catch (SQLException e_genre) {
                                    System.err.println("movie-list.jsp: Error fetching genres for movie ID " + id + ": " + e_genre.getMessage());
                                }

                                // Fetch Stars
                                String starSql = "SELECT s.id, s.name FROM stars s JOIN stars_in_movies sm ON s.id=sm.starId "
                                        + "WHERE sm.movieId=? "
                                        + "ORDER BY (SELECT COUNT(*) FROM stars_in_movies sim2 WHERE sim2.starId=s.id) DESC, s.name ASC "
                                        + "LIMIT 3";
                                try (PreparedStatement starLoopStmt = subConn.prepareStatement(starSql)) {
                                    starLoopStmt.setString(1, id);
                                    try (ResultSet starLoopRs = starLoopStmt.executeQuery()) {
                                        while (starLoopRs.next()) {
                                            Map<String, String> starInfo = new HashMap<>();
                                            starInfo.put("id", starLoopRs.getString("id"));
                                            starInfo.put("name", starLoopRs.getString("name"));
                                            sList.add(starInfo);
                                        }
                                    }
                                } catch (SQLException e_star) {
                                    System.err.println("movie-list.jsp: Error fetching stars for movie ID " + id + ": " + e_star.getMessage());
                                }
                            } catch (SQLException e_conn) {
                                System.err.println("movie-list.jsp: Error getting sub-connection for movie ID " + id + ": " + e_conn.getMessage());
                            }
                        }
                    %>
                    <li>
                        <h2>
                            <a href="single-movie?movieId=<%=Htmlescape.escape(id)%>&<%=baseQS.substring(0, Math.max(0, baseQS.length()-1))%>"><%=Htmlescape.escape(mTitle)%></a>
                        </h2>
                        <form method="POST" action="add-to-cart" style="display:inline;">
                            <input type="hidden" name="movieId"    value="<%=Htmlescape.escape(id)%>"/>
                            <input type="hidden" name="movieTitle" value="<%=Htmlescape.escape(mTitle)%>"/>
                            <input type="hidden" name="price"      value="<%=String.format("%.2f",mPrice)%>"/>
                            <button type="submit">Add to Cart ($<%=String.format("%.2f",mPrice)%>)</button>
                        </form>
                        <div class="details">
                            <p>Year: <%=mYear%>, Director: <%=Htmlescape.escape(mDir)%>,
                                Rating: <%=(mRating == -1.0f ? "N/A" : String.format("%.1f", mRating))%></p>
                            <p>Genres:
                                <% if (gList.isEmpty()) { out.print("N/A"); } %>
                                <c:forEach var="genreName" items="<%=gList%>" varStatus="loop">
                                    <a href="browse?genre=<%=URLEncoder.encode((String)pageContext.getAttribute("genreName"),"UTF-8")%>&limit=<%=recordsPerPage%>&sortBy=<%=formSortBy%>&sortDir=<%=formSortDir%>"><%=Htmlescape.escape((String)pageContext.getAttribute("genreName"))%></a><c:if test="${!loop.last}">, </c:if>
                                </c:forEach>
                            </p>
                            <p>Stars:
                                <% if (sList.isEmpty()) { out.print("N/A"); } %>
                                <c:forEach var="starInfo" items="<%=sList%>" varStatus="loop">
                                    <a href="single-star?starId=<%=((Map<String,String>)pageContext.getAttribute("starInfo")).get("id")%>&<%=baseQS.substring(0, Math.max(0, baseQS.length()-1))%>"><%=Htmlescape.escape(((Map<String,String>)pageContext.getAttribute("starInfo")).get("name"))%></a><c:if test="${!loop.last}">, </c:if>
                                </c:forEach>
                            </p>
                        </div>
                    </li>
                    <% } %>
                </ul>
            </c:when>
        </c:choose>

        <div class="pagination">
            <% if (hasPreviousPage) { %>
            <a href="${ (q_param != null && !q_param.isEmpty()) ? 'search' : ( (genre != null && !genre.isEmpty()) || (letter != null && !letter.isEmpty()) ? 'browse' : 'search' ) }?<%= baseQS %>page=<%=currentPage-1 %>">
                &larr; Previous
            </a>
            <% } %>
            &nbsp; <% if (totalPages > 0) { %>
            Page <%=currentPage%> of <%=totalPages%>
            <% } %>
            &nbsp;
            <% if (hasNextPage) { %>
            <a href="${ (q_param != null && !q_param.isEmpty()) ? 'search' : ( (genre != null && !genre.isEmpty()) || (letter != null && !letter.isEmpty()) ? 'browse' : 'search' ) }?<%= baseQS %>page=<%=currentPage+1 %>">
                Next &rarr;
            </a>
            <% } %>
        </div>
    </main>
</div>
</body>
</html>
