<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.sql.*, javax.naming.*, javax.sql.DataSource, java.util.*, java.net.URLEncoder" %>
<%
    // Check if the request is to fetch genres (though this logic is better in a servlet)
    // For Project 2/3, this genre fetching logic should ideally be exclusively in GenreServlet.
    // The index.html should call GenreServlet directly.
    // This block in movie-list.jsp can be removed if index.html calls GenreServlet.
    if ("true".equals(request.getParameter("fetchGenres"))) {
        Context initCtx = new InitialContext();
        DataSource ds   = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb");
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM genres ORDER BY name")) {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString("name"));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8"); // Good practice for JSON
            java.io.PrintWriter jsonOut = response.getWriter(); // Use a different 'out' for JSON
            jsonOut.print("[");
            for (int i = 0; i < list.size(); i++) {
                // Simple JSON escaping for double quotes
                jsonOut.print("\"" + list.get(i).replace("\"", "\\\"") + "\"");
                if (i < list.size() - 1) jsonOut.print(",");
            }
            jsonOut.print("]");
            jsonOut.flush();
            return; // Important to stop further JSP processing
        }
        // Catch and log exceptions appropriately
    }

    String title    = request.getParameter("title");
    String year     = request.getParameter("year");
    String director = request.getParameter("director");
    String star     = request.getParameter("star");
    String genre    = request.getParameter("genre");
    String letter   = request.getParameter("letter");

    // Default sorting: rating descending, then title descending as tie-breaker
    String sortBy  = request.getParameter("sortBy");
    String sortDir = request.getParameter("sortDir");

    if (sortBy == null || (!sortBy.equals("title") && !sortBy.equals("rating"))) {
        sortBy = "rating"; // Default sort by rating
    }
    if (sortDir == null || (!sortDir.equals("asc") && !sortDir.equals("desc"))) {
        sortDir = "desc"; // Default sort direction
    }


    int pageSize;
    try { pageSize = Integer.parseInt(request.getParameter("pageSize")); }
    catch (Exception e) { pageSize = 10; } // Default page size
    if (!(pageSize==10||pageSize==25||pageSize==50||pageSize==100))
        pageSize = 10;

    int pageNum;
    try { pageNum = Integer.parseInt(request.getParameter("page")); }
    catch (Exception e) { pageNum = 1; } // Default page number
    if (pageNum < 1) pageNum = 1;

    // --- Database Connection and Query Setup ---
    // This part remains largely the same, using prepared statements is good.
    // Ensure your SQL dynamically builds based on parameters.

    Context initCtx_page = new InitialContext(); // Renamed to avoid conflict if 'initCtx' used above for JSON
    DataSource ds_page   = (DataSource) initCtx_page.lookup("java:comp/env/jdbc/moviedb");
    Connection conn = null; // Declare outside try to close in finally
    PreparedStatement mainStmt = null;
    PreparedStatement genreStmt = null;
    PreparedStatement starStmt = null;
    ResultSet rs = null;
    ResultSet grs = null;
    ResultSet srs = null;

    List<Map<String,Object>> movies = new ArrayList<>();
    boolean hasNext = false;
    String baseQS = ""; // For pagination links

    try {
        conn = ds_page.getConnection();

        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT m.id, m.title, m.year, m.director, COALESCE(r.rating,0) AS rating "
                        + "FROM movies m LEFT JOIN ratings r ON m.id=r.movieId "
        );
        if (star != null && !star.isEmpty()) {
            sql.append("JOIN stars_in_movies sim ON m.id=sim.movieId ")
                    .append("JOIN stars s ON sim.starId=s.id ");
        }
        if (genre != null && !genre.isEmpty()) {
            sql.append("JOIN genres_in_movies gm ON m.id=gm.movieId ")
                    .append("JOIN genres g ON gm.genreId=g.id ");
        }
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (title != null && !title.isEmpty()) {
            sql.append("AND m.title LIKE ? "); params.add("%"+title+"%");
        }
        if (year != null && !year.isEmpty()) {
            try { // Basic validation for year
                params.add(Integer.parseInt(year));
                sql.append("AND m.year = ? ");
            } catch (NumberFormatException e) { /* ignore invalid year or handle error */ }
        }
        if (director != null && !director.isEmpty()) {
            sql.append("AND m.director LIKE ? "); params.add("%"+director+"%");
        }
        if (star != null && !star.isEmpty()) {
            sql.append("AND s.name LIKE ? "); params.add("%"+star+"%");
        }
        if (genre != null && !genre.isEmpty()) {
            sql.append("AND g.name = ? "); params.add(genre);
        }
        if (letter != null && !letter.isEmpty()) {
            if ("*".equals(letter)) {
                sql.append("AND m.title REGEXP '^[^A-Za-z0-9]' ");
            } else if (letter.matches("^[a-zA-Z0-9]$")){ // Ensure single alphanumeric
                sql.append("AND m.title LIKE ? "); params.add(letter+"%");
            }
        }

        // Sorting Logic from Project 2 requirements
        // Sort by (first_sort_criteria, second_sort_criteria)
        // first_sort_criteria: title OR rating
        // second_sort_criteria: the other one (rating if title, title if rating)
        // Both use sortDir (asc/desc)

        String firstSortColumnDb = sortBy.equals("title") ? "m.title" : "rating";
        String secondSortColumnDb = sortBy.equals("title") ? "rating" : "m.title";

        sql.append("ORDER BY ")
                .append(firstSortColumnDb).append(" ").append(sortDir)
                .append(", ")
                .append(secondSortColumnDb).append(" ").append(sortDir) // Use same direction for tie-breaker based on typical P2 spec
                .append(" ");


        int fetchSize = pageSize + 1; // To check if there's a next page
        int offset    = (pageNum - 1) * pageSize;
        sql.append("LIMIT ? OFFSET ?");
        params.add(fetchSize);
        params.add(offset);


        mainStmt = conn.prepareStatement(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            mainStmt.setObject(i+1, params.get(i));
        }
        rs = mainStmt.executeQuery();


        while (rs.next()) {
            if (movies.size() >= pageSize) { hasNext = true; break; }
            Map<String,Object> m = new HashMap<>();
            m.put("id",       rs.getString("id"));
            m.put("title",    rs.getString("title"));
            m.put("year",     rs.getInt("year"));
            m.put("director", rs.getString("director"));
            m.put("rating",   rs.getDouble("rating"));
            movies.add(m);
        }


        genreStmt = conn.prepareStatement(
                "SELECT g.name FROM genres g JOIN genres_in_movies gm ON g.id=gm.genreId "
                        + "WHERE gm.movieId=? ORDER BY g.name ASC LIMIT 3"
        );
        starStmt = conn.prepareStatement(
                "SELECT s.id, s.name FROM stars s JOIN stars_in_movies sm ON s.id=sm.starId "
                        + "WHERE sm.movieId=? "
                        + "ORDER BY (SELECT COUNT(*) FROM stars_in_movies sim2 WHERE sim2.starId=s.id) DESC, s.name ASC "
                        + "LIMIT 3"
        );

        // Building query string for pagination and sorting links
        // (Project 2 requires maintaining state)
        StringBuilder qsBuilder = new StringBuilder();
        if (title    != null && !title.isEmpty()) qsBuilder.append("title=").append(URLEncoder.encode(title,"UTF-8")).append("&");
        if (year     != null && !year.isEmpty()) qsBuilder.append("year=").append(year).append("&");
        if (director != null && !director.isEmpty()) qsBuilder.append("director=").append(URLEncoder.encode(director,"UTF-8")).append("&");
        if (star     != null && !star.isEmpty()) qsBuilder.append("star=").append(URLEncoder.encode(star,"UTF-8")).append("&");
        if (genre    != null && !genre.isEmpty()) qsBuilder.append("genre=").append(URLEncoder.encode(genre,"UTF-8")).append("&");
        if (letter   != null && !letter.isEmpty()) qsBuilder.append("letter=").append(URLEncoder.encode(letter,"UTF-8")).append("&");

        // These are always present for state preservation
        qsBuilder.append("sortBy=").append(sortBy).append("&")
                .append("sortDir=").append(sortDir).append("&")
                .append("pageSize=").append(pageSize).append("&");
        // Page number will be added specifically for prev/next links

        baseQS = qsBuilder.toString(); // baseQS will not include the 'page' parameter initially

    } catch (Exception e) {
        // Log error and display a user-friendly message
        e.printStackTrace(); // For debugging
        // You might want to set an error message attribute and forward to an error page
        // For now, just printing to the page (not ideal for production)
        out.println("<p>Error loading movies: " + e.getMessage() + "</p>");
        // Ensure resources are closed even if an error occurs before HTML generation
        if (rs != null) try { rs.close(); } catch (SQLException ex) {}
        if (mainStmt != null) try { mainStmt.close(); } catch (SQLException ex) {}
        if (genreStmt != null) try { genreStmt.close(); } catch (SQLException ex) {}
        if (starStmt != null) try { starStmt.close(); } catch (SQLException ex) {}
        if (conn != null) try { conn.close(); } catch (SQLException ex) {}
        return; // Stop if there's a major error
    }

%>

<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Movie List</title>
    <link rel="stylesheet" href="css/style.css"> <%-- Make sure this path is correct --%>
</head>
<body>
<div class="page-bg"> <%-- Added page-bg for consistency with index.html --%>
    <header class="app-header">  <%-- Added header for consistency --%>
        <h1>Movie List</h1>
        <div>
            <a href="shopping-cart.jsp" class="btn-secondary" style="margin-right: 10px;">Checkout 🛒</a>
            <%-- Logout link --%>
            <a href="logout" class="btn-secondary">Logout</a>
        </div>
    </header>

    <main class="container card"> <%-- Added main and container card for consistency --%>

        <p>
            <%-- This link now points to your index.html (the main page) --%>
            <a href="${pageContext.request.contextPath}/index.html" class="back-link">← Back to Main Page</a>
        </p>

        <div class="controls">
            <%-- The form should submit to the current page (movie-list.jsp) to re-apply filters/sorting --%>
            <form method="GET" action="movie-list.jsp" class="controls-form">

                <%-- Persist filter parameters as hidden inputs --%>
                <% if (title    != null && !title.isEmpty())    { %><input type="hidden" name="title"    value="<%= Htmlescape.escape(title) %>"/><% } %>
                <% if (year     != null && !year.isEmpty())     { %><input type="hidden" name="year"     value="<%= Htmlescape.escape(year) %>"/><% } %>
                <% if (director != null && !director.isEmpty()) { %><input type="hidden" name="director" value="<%= Htmlescape.escape(director) %>"/><% } %>
                <% if (star     != null && !star.isEmpty())     { %><input type="hidden" name="star"     value="<%= Htmlescape.escape(star) %>"/><% } %>
                <% if (genre    != null && !genre.isEmpty())    { %><input type="hidden" name="genre"    value="<%= Htmlescape.escape(genre) %>"/><% } %>
                <% if (letter   != null && !letter.isEmpty())   { %><input type="hidden" name="letter"   value="<%= Htmlescape.escape(letter) %>"/><% } %>

                <label for="sortBySelect">Sort by:</label>
                <select name="sortBy" id="sortBySelect">
                    <option value="title"  <%= "title".equals(sortBy)  ? "selected" : "" %>>Title</option>
                    <option value="rating" <%= "rating".equals(sortBy) ? "selected" : "" %>>Rating</option>
                </select>

                <label for="sortDirSelect">Direction:</label>
                <select name="sortDir" id="sortDirSelect">
                    <option value="asc"  <%= "asc".equals(sortDir)  ? "selected" : "" %>>Ascending</option>
                    <option value="desc" <%= "desc".equals(sortDir) ? "selected" : "" %>>Descending</option>
                </select>

                <label for="pageSizeSelect">Show:</label>
                <select name="pageSize" id="pageSizeSelect">
                    <option value="10"  <%= pageSize==10  ? "selected" : "" %>>10</option>
                    <option value="25"  <%= pageSize==25  ? "selected" : "" %>>25</option>
                    <option value="50"  <%= pageSize==50  ? "selected" : "" %>>50</option>
                    <option value="100" <%= pageSize==100 ? "selected" : "" %>>100</option>
                </select> per page

                <button type="submit">Apply</button>
            </form>
        </div>

        <% if (movies.isEmpty()) { %>
        <p>No movies found matching your criteria.</p>
        <% } else { %>
        <ul class="movies">
            <% for (Map<String,Object> m : movies) {
                String id      = (String)  m.get("id");
                String mTitle  = (String)  m.get("title"); // Already HTML escaped if using a proper library
                int    mYear   = (Integer) m.get("year");
                String mDir    = (String)  m.get("director"); // Already HTML escaped
                double mRating = (Double)  m.get("rating");
                // For Project 2, price could be fixed or from DB if you added it.
                // For now, using a placeholder.
                double mPrice  = 9.99; // Example price

                // Fetch genres for this movie
                List<String> gList = new ArrayList<>();
                if (genreStmt != null) { // Ensure statement is initialized
                    genreStmt.setString(1, id);
                    grs = genreStmt.executeQuery();
                    while (grs.next()) gList.add(grs.getString("name"));
                    grs.close();
                }

                // Fetch stars for this movie
                List<Map<String, String>> sList = new ArrayList<>(); // Store as Map for id and name
                if (starStmt != null) { // Ensure statement is initialized
                    starStmt.setString(1, id);
                    srs = starStmt.executeQuery();
                    while (srs.next()) {
                        Map<String, String> starInfo = new HashMap<>();
                        starInfo.put("id", srs.getString("id"));
                        starInfo.put("name", srs.getString("name"));
                        sList.add(starInfo);
                    }
                    srs.close();
                }
            %>
            <li>
                <h2>
                    <%-- Link to single-movie.jsp or SingleMovieServlet --%>
                    <a href="single-movie?movieId=<%=id%>&<%= Htmlescape.escape(baseQS) %>"><%=Htmlescape.escape(mTitle)%></a>
                </h2>
                <%-- Add to Cart Form --%>
                <form method="POST" action="add-to-cart" style="display:inline;">
                    <input type="hidden" name="movieId"    value="<%=id%>"/>
                    <input type="hidden" name="movieTitle" value="<%=Htmlescape.escape(mTitle)%>"/>
                    <input type="hidden" name="price"      value="<%=String.format("%.2f",mPrice)%>"/>
                    <button type="submit" class="btn-add-to-cart">Add to Cart ($<%=String.format("%.2f",mPrice)%>)</button>
                </form>
                <div class="details">
                    <p>Year: <%=mYear%>, Director: <%=Htmlescape.escape(mDir)%>, Rating: <%=String.format("%.1f",mRating)%></p>
                    <p>Genres:
                        <% for (int i=0; i<gList.size(); i++) { %>
                        <%-- Link to movie-list.jsp with genre filter or BrowseServlet --%>
                        <a href="movie-list.jsp?genre=<%=URLEncoder.encode(gList.get(i),"UTF-8")%>&sortBy=<%=sortBy%>&sortDir=<%=sortDir%>&pageSize=<%=pageSize%>">
                            <%=Htmlescape.escape(gList.get(i))%>
                        </a><%= i<gList.size()-1 ? ", " : "" %>
                        <% } %>
                        <% if (gList.isEmpty()) { out.print("N/A"); } %>
                    </p>
                    <p>Stars:
                        <% for (int i=0; i<sList.size(); i++) {
                            String starId = sList.get(i).get("id");
                            String starName = sList.get(i).get("name");
                        %>
                        <%-- Link to single-star.jsp or SingleStarServlet --%>
                        <a href="single-star?starId=<%=starId%>&<%= Htmlescape.escape(baseQS) %>"><%=Htmlescape.escape(starName)%></a><%= i<sList.size()-1 ? ", " : "" %>
                        <% } %>
                        <% if (sList.isEmpty()) { out.print("N/A"); } %>
                    </p>
                </div>
            </li>
            <% } %>
        </ul>
        <% } %>

        <div class="pagination">
            <% if (pageNum > 1) { %>
            <a href="movie-list.jsp?<%= baseQS %>page=<%=pageNum-1 %>">
                &larr; Previous
            </a>
            <% } %>
            <% if (hasNext) { %>
            <a href="movie-list.jsp?<%= baseQS %>page=<%=pageNum+1 %>">
                Next &rarr;
            </a>
            <% } %>
        </div>
    </main> <%-- Closing main --%>
</div> <%-- Closing page-bg --%>

<%
    // Close all JDBC resources in a finally block
    // This 'finally' block should be part of the try-catch that opens the connection.
    // The current structure has the try-catch ending before the HTML.
    // For robustness, the resource closing should be guaranteed.
    // However, to keep changes minimal to your existing structure for now:
    if (rs != null) try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
    if (grs != null) try { grs.close(); } catch (SQLException e) { e.printStackTrace(); }
    if (srs != null) try { srs.close(); } catch (SQLException e) { e.printStackTrace(); }
    if (mainStmt != null) try { mainStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
    if (genreStmt != null) try { genreStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
    if (starStmt != null) try { starStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
    if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
%>
<%!
    // Basic HTML escaping utility class (consider using a library like Apache Commons Lang for more robust escaping)
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
</body>
</html>