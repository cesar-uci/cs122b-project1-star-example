<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.sql.*, javax.naming.*, javax.sql.DataSource, java.util.*, java.net.URLEncoder" %>
<%
    if ("true".equals(request.getParameter("fetchGenres"))) {
        Context initCtx = new InitialContext();
        DataSource ds   = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb");
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM genres ORDER BY name")) {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString("name"));
            response.setContentType("application/json");
            out.print("[");
            for (int i = 0; i < list.size(); i++) {
                out.print("\"" + list.get(i).replace("\"","\\\"") + "\"");
                if (i < list.size() - 1) out.print(",");
            }
            out.print("]");
            return;
        }
    }

    String title    = request.getParameter("title");
    String year     = request.getParameter("year");
    String director = request.getParameter("director");
    String star     = request.getParameter("star");
    String genre    = request.getParameter("genre");
    String letter   = request.getParameter("letter");

    String sortBy  = "rating".equals(request.getParameter("sortBy"))
            ? "rating"
            : "title".equals(request.getParameter("sortBy"))
            ? "title"
            : "rating";
    String sortDir = "asc".equals(request.getParameter("sortDir")) ? "asc" : "desc";

    int pageSize;
    try { pageSize = Integer.parseInt(request.getParameter("pageSize")); }
    catch (Exception e) { pageSize = 10; }
    if (!(pageSize==10||pageSize==25||pageSize==50||pageSize==100))
        pageSize = 10;

    int pageNum;
    try { pageNum = Integer.parseInt(request.getParameter("page")); }
    catch (Exception e) { pageNum = 1; }
    if (pageNum < 1) pageNum = 1;

    Context initCtx = new InitialContext();
    DataSource ds   = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb");
    Connection conn = ds.getConnection();

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
        sql.append("AND m.year = ? "); params.add(Integer.parseInt(year));
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
        } else {
            sql.append("AND m.title LIKE ? "); params.add(letter+"%");
        }
    }

    sql.append("ORDER BY ")
            .append(sortBy.equals("title") ? "m.title" : "rating").append(" ").append(sortDir)
            .append(", ")
            .append(sortBy.equals("title") ? "rating" : "m.title").append(" ").append(sortDir)
            .append(" ");

    int fetchSize = pageSize + 1;
    int offset    = (pageNum - 1) * pageSize;
    sql.append("LIMIT ").append(fetchSize).append(" OFFSET ").append(offset);

    PreparedStatement mainStmt = conn.prepareStatement(sql.toString());
    for (int i = 0; i < params.size(); i++) {
        mainStmt.setObject(i+1, params.get(i));
    }
    ResultSet rs = mainStmt.executeQuery();

    List<Map<String,Object>> movies = new ArrayList<>();
    boolean hasNext = false;
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
    rs.close();

    PreparedStatement genreStmt = conn.prepareStatement(
            "SELECT g.name FROM genres g JOIN genres_in_movies gm ON g.id=gm.genreId "
                    + "WHERE gm.movieId=? ORDER BY g.name ASC LIMIT 3"
    );
    PreparedStatement starStmt = conn.prepareStatement(
            "SELECT s.id, s.name FROM stars s JOIN stars_in_movies sm ON s.id=sm.starId "
                    + "WHERE sm.movieId=? "
                    + "ORDER BY (SELECT COUNT(*) FROM stars_in_movies sim2 WHERE sim2.starId=s.id) DESC, s.name ASC "
                    + "LIMIT 3"
    );

    StringBuilder qs = new StringBuilder();
    if (title    != null) qs.append("title=").append(URLEncoder.encode(title,"UTF-8")).append("&");
    if (year     != null) qs.append("year=").append(year).append("&");
    if (director != null) qs.append("director=").append(URLEncoder.encode(director,"UTF-8")).append("&");
    if (star     != null) qs.append("star=").append(URLEncoder.encode(star,"UTF-8")).append("&");
    if (genre    != null) qs.append("genre=").append(URLEncoder.encode(genre,"UTF-8")).append("&");
    if (letter   != null) qs.append("letter=").append(URLEncoder.encode(letter,"UTF-8")).append("&");
    qs.append("sortBy=").append(sortBy).append("&")
            .append("sortDir=").append(sortDir).append("&")
            .append("pageSize=").append(pageSize).append("&")
            .append("page=").append(pageNum);
    String baseQS = qs.toString();
%>

<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Movie List</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="container">
    <div class="card">

        <div class="header">
            <h1>Movie List</h1>
            <a href="shopping-cart.jsp" class="btn-secondary">Checkout 🛒</a>
        </div>

        <!-- Back to search/browse -->
        <p>
            <a href="index.html" class="back-link">← Back to Search/Browse</a>
        </p>

        <!-- Sort / page-size controls -->
        <div class="controls">
            <form method="get" action="movie-list.jsp" class="controls-form">

                <%-- Persist filters --%>
                <% if (title    != null && !title.isEmpty())    { %>
                <input type="hidden" name="title"    value="<%= title %>"/>
                <% } %>
                <% if (year     != null && !year.isEmpty())     { %>
                <input type="hidden" name="year"     value="<%= year %>"/>
                <% } %>
                <% if (director != null && !director.isEmpty()) { %>
                <input type="hidden" name="director" value="<%= director %>"/>
                <% } %>
                <% if (star     != null && !star.isEmpty())     { %>
                <input type="hidden" name="star"     value="<%= star %>"/>
                <% } %>
                <% if (genre    != null && !genre.isEmpty())    { %>
                <input type="hidden" name="genre"    value="<%= genre %>"/>
                <% } %>
                <% if (letter   != null && !letter.isEmpty())   { %>
                <input type="hidden" name="letter"   value="<%= letter %>"/>
                <% } %>

                <label>
                    Sort by:
                    <select name="sortBy">
                        <option value="title"  <%= "title".equals(sortBy)  ? "selected" : "" %>>Title</option>
                        <option value="rating" <%= "rating".equals(sortBy) ? "selected" : "" %>>Rating</option>
                    </select>
                </label>

                <label>
                    Direction:
                    <select name="sortDir">
                        <option value="asc"  <%= "asc".equals(sortDir)  ? "selected" : "" %>>Asc</option>
                        <option value="desc" <%= "desc".equals(sortDir) ? "selected" : "" %>>Desc</option>
                    </select>
                </label>

                <label>
                    Show:
                    <select name="pageSize">
                        <option value="10"  <%= pageSize==10  ? "selected" : "" %>>10</option>
                        <option value="25"  <%= pageSize==25  ? "selected" : "" %>>25</option>
                        <option value="50"  <%= pageSize==50  ? "selected" : "" %>>50</option>
                        <option value="100" <%= pageSize==100 ? "selected" : "" %>>100</option>
                    </select> per page
                </label>

                <button type="submit">Apply</button>
            </form>
        </div>

        <!-- Movie items -->
        <ul class="movies">
            <% for (Map<String,Object> m : movies) {
                String id      = (String)  m.get("id");
                String mTitle  = (String)  m.get("title");
                int    mYear   = (Integer) m.get("year");
                String mDir    = (String)  m.get("director");
                double mRating = (Double)  m.get("rating");
                double mPrice  = 10.00;

                // fetch genres
                genreStmt.setString(1, id);
                ResultSet grs = genreStmt.executeQuery();
                List<String> gList = new ArrayList<>();
                while (grs.next()) gList.add(grs.getString("name"));
                grs.close();

                // fetch stars
                starStmt.setString(1, id);
                ResultSet srs = starStmt.executeQuery();
                List<String[]> sList = new ArrayList<>();
                while (srs.next()) sList.add(new String[]{ srs.getString("id"), srs.getString("name") });
                srs.close();
            %>
            <li>
                <h2>
                    <a href="single-movie.jsp?id=<%=id%>"><%=mTitle%></a>
                </h2>
                <form method="POST" action="add-to-cart" style="display:inline;">
                    <input type="hidden" name="movieId"    value="<%=id%>"/>
                    <input type="hidden" name="movieTitle" value="<%=mTitle%>"/>
                    <input type="hidden" name="price"      value="<%=String.format("%.2f",mPrice)%>"/>
                    <button type="submit">Add to Cart ($<%=String.format("%.2f",mPrice)%>)</button>
                </form>
                <div class="details">
                    <p>Year: <%=mYear%>, Director: <%=mDir%>, Rating: <%=String.format("%.1f",mRating)%></p>
                    <p>Genres:
                        <% for (int i=0; i<gList.size(); i++) { %>
                        <a href="movie-list.jsp?genre=<%=URLEncoder.encode(gList.get(i),"UTF-8")%>">
                            <%=gList.get(i)%>
                        </a><%= i<gList.size()-1 ? ", " : "" %>
                        <% } %>
                    </p>
                    <p>Stars:
                        <% for (int i=0; i<sList.size(); i++) {
                            String sid = sList.get(i)[0], sn = sList.get(i)[1];
                        %>
                        <a href="single-star.jsp?id=<%=sid%>"><%=sn%></a><%= i<sList.size()-1 ? ", " : "" %>
                        <% } %>
                    </p>
                </div>
            </li>
            <% } %>
        </ul>

        <!-- Pagination -->
        <div class="pagination">
            <% if (pageNum > 1) { %>
            <a href="movie-list.jsp?<%= baseQS.replace("&page="+pageNum, "&page="+(pageNum-1)) %>">
                ← Previous
            </a>
            <% } %>
            <% if (hasNext) { %>
            <a href="movie-list.jsp?<%= baseQS.replace("&page="+pageNum, "&page="+(pageNum+1)) %>">
                Next →
            </a>
            <% } %>
        </div>

    </div><!-- /.card -->
</div><!-- /.container -->

<%
    mainStmt.close();
    genreStmt.close();
    starStmt.close();
    conn.close();
%>
</body>
</html>
