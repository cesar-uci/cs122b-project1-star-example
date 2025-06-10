package uci122b.service;

import uci122b.Movie;
import uci122b.util.SearchUtil;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

public class MovieListService {
    private DataSource dataSource;

    // Custom Exception class for clarity
    public static class ServletException extends Exception {
        public ServletException(String message, Throwable cause) { super(message, cause); }
        public ServletException(String message) { super(message); }
    }

    // Constructor with detailed logging
    public MovieListService() throws ServletException {
        System.out.println("DIAGNOSTIC: MovieListService constructor called.");
        try {
            System.out.println("DIAGNOSTIC: Attempting JNDI lookup for 'java:comp/env/jdbc/moviedb_slave'");
            Context initCtx = new InitialContext();
            this.dataSource = (DataSource) initCtx.lookup("java:comp/env/jdbc/moviedb_slave");
            System.out.println("DIAGNOSTIC: JNDI lookup SUCCEEDED. DataSource is ready.");
        } catch (NamingException e) {
            System.err.println("DIAGNOSTIC: JNDI lookup FAILED for 'jdbc/moviedb_slave'. This is the root cause of the error.");
            e.printStackTrace(System.err);
            throw new ServletException("DataSource lookup failed in MovieListService for jdbc/moviedb_slave", e);
        }
    }

    public List<Movie> findMovies(HttpServletRequest request) {
        List<Movie> movies = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        String userQuery = request.getParameter("q");
        String genreName = request.getParameter("genre");
        String titleInitial = request.getParameter("letter");

        int page = 1;
        if (request.getParameter("page") != null) {
            try { page = Integer.parseInt(request.getParameter("page")); } catch (NumberFormatException e) { page = 1; }
        }
        if (page < 1) page = 1; // Ensure page is at least 1

        int recordsPerPage = 10; // Default
        if (request.getParameter("limit") != null) {
            try { recordsPerPage = Integer.parseInt(request.getParameter("limit")); } catch (NumberFormatException e) { recordsPerPage = 10;}
        }
        if (!List.of(10, 25, 50, 100).contains(recordsPerPage)) recordsPerPage = 10; // Enforce valid limits

        int offset = (page - 1) * recordsPerPage;

        String sortBy = request.getParameter("sortBy");
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "title_asc"; // Default sort
        }

        try {
            conn = dataSource.getConnection(); // From SLAVE pool

            StringBuilder sqlSelectFrom = new StringBuilder(
                    "SELECT DISTINCT m.id, m.title, m.year, m.director, r.rating " +
                            "FROM movies m LEFT JOIN ratings r ON m.id = r.movieId "
            );
            StringBuilder sqlWhere = new StringBuilder();
            List<String> whereConditions = new ArrayList<>();
            List<Object> queryParams = new ArrayList<>();

            // For genre filtering, we need to add JOINs
            if (genreName != null && !genreName.trim().isEmpty()) {
                sqlSelectFrom.append("JOIN genres_in_movies gim ON m.id = gim.movieId ");
                sqlSelectFrom.append("JOIN genres g ON gim.genreId = g.id ");
                whereConditions.add("g.name = ?");
                queryParams.add(genreName);
            }

            if (userQuery != null && !userQuery.trim().isEmpty()) {
                String trimmedQuery = userQuery.trim();
                System.out.println("MovieListService: Processing main search query: '" + trimmedQuery + "'");

                String[] tokens = trimmedQuery.split("\\s+");
                StringBuilder ftQueryBuilder = new StringBuilder();
                for (String token : tokens) {
                    if (!token.isEmpty()) {
                        ftQueryBuilder.append("+").append(token.replaceAll("[+-><()~*\"@]", "")).append("* ");
                    }
                }
                String ftQueryString = ftQueryBuilder.toString().trim();
                String likePattern = "%" + trimmedQuery + "%";
                int edthThreshold = SearchUtil.calculateEditDistanceThreshold(trimmedQuery);

                StringBuilder searchConditionGroup = new StringBuilder("(");
                List<String> searchSubConditions = new ArrayList<>();

                if (!ftQueryString.isEmpty()) {
                    searchSubConditions.add("MATCH(m.title) AGAINST (? IN BOOLEAN MODE)");
                    queryParams.add(ftQueryString);
                }
                searchSubConditions.add("m.title LIKE ?");
                queryParams.add(likePattern);
                searchSubConditions.add("edth(LOWER(m.title), LOWER(?), ?) = 1");
                queryParams.add(trimmedQuery);
                queryParams.add(edthThreshold);

                searchConditionGroup.append(String.join(" OR ", searchSubConditions)).append(")");
                whereConditions.add(searchConditionGroup.toString());
            } else if (titleInitial != null && !titleInitial.trim().isEmpty()) { // only apply letter if not a search query
                if (titleInitial.equals("*")) {
                    whereConditions.add("m.title REGEXP '^[^a-zA-Z0-9]'");
                } else {
                    whereConditions.add("m.title LIKE ?");
                    queryParams.add(titleInitial + "%");
                }
            }

            if (!whereConditions.isEmpty()) {
                sqlWhere.append(" WHERE ").append(String.join(" AND ", whereConditions));
            }

            String orderByClause = " ORDER BY ";
            switch (sortBy.toLowerCase()) {
                case "title_asc":
                    orderByClause += "m.title ASC, r.rating DESC";
                    break;
                case "title_desc":
                    orderByClause += "m.title DESC, r.rating DESC";
                    break;
                case "rating_asc":
                    orderByClause += "r.rating ASC, m.title ASC";
                    break;
                case "rating_desc":
                    orderByClause += "r.rating DESC, m.title ASC";
                    break;
                default: // Default sort
                    orderByClause += "m.title ASC, r.rating DESC";
            }

            String finalSql = sqlSelectFrom.toString() + sqlWhere.toString() + orderByClause + " LIMIT ? OFFSET ?";
            queryParams.add(recordsPerPage);
            queryParams.add(offset);

            pstmt = conn.prepareStatement(finalSql);

            int paramIndex = 1;
            for (Object param : queryParams) {
                if (param instanceof String) pstmt.setString(paramIndex++, (String) param);
                else if (param instanceof Integer) pstmt.setInt(paramIndex++, (Integer) param);
            }

            System.out.println("MovieListService: Executing SQL on SLAVE DB: " + pstmt.toString());
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("id");
                String title = rs.getString("title");
                int year = rs.getInt("year");
                String director = rs.getString("director");
                float rating = rs.getFloat("rating");
                if (rs.wasNull()) {
                    rating = -1.0f; // Indicate no rating or handle as NULL in Movie object
                }
                movies.add(new Movie(id, title, year, director, rating));
            }

        } catch (SQLException e) {
            System.err.println("MovieListService SQL Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (pstmt != null) pstmt.close(); } catch (SQLException ignored) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }
        return movies;
    }

    public int getTotalMovieCount(HttpServletRequest request) {
        int totalRecords = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        String userQuery = request.getParameter("q");
        String genreName = request.getParameter("genre");
        String titleInitial = request.getParameter("letter");

        try {
            conn = dataSource.getConnection(); // From SLAVE pool

            StringBuilder sqlCount = new StringBuilder("SELECT COUNT(DISTINCT m.id) FROM movies m ");
            List<String> whereConditions = new ArrayList<>();
            List<Object> queryParams = new ArrayList<>();

            if (genreName != null && !genreName.trim().isEmpty()) {
                sqlCount.append("JOIN genres_in_movies gim ON m.id = gim.movieId ");
                sqlCount.append("JOIN genres g ON gim.genreId = g.id ");
                whereConditions.add("g.name = ?");
                queryParams.add(genreName);
            }

            if (userQuery != null && !userQuery.trim().isEmpty()) {
                String trimmedQuery = userQuery.trim();
                String[] tokens = trimmedQuery.split("\\s+");
                StringBuilder ftQueryBuilder = new StringBuilder();
                for (String token : tokens) {
                    if (!token.isEmpty()) ftQueryBuilder.append("+").append(token.replaceAll("[+-><()~*\"@]", "")).append("* ");
                }
                String ftQueryString = ftQueryBuilder.toString().trim();
                String likePattern = "%" + trimmedQuery + "%";
                int edthThreshold = SearchUtil.calculateEditDistanceThreshold(trimmedQuery);

                StringBuilder searchConditionGroup = new StringBuilder("(");
                List<String> searchSubConditions = new ArrayList<>();
                if (!ftQueryString.isEmpty()) {
                    searchSubConditions.add("MATCH(m.title) AGAINST (? IN BOOLEAN MODE)");
                    queryParams.add(ftQueryString);
                }
                searchSubConditions.add("m.title LIKE ?");
                queryParams.add(likePattern);
                searchSubConditions.add("edth(LOWER(m.title), LOWER(?), ?) = 1"); // <<<< SEE LOWER() HERE
                queryParams.add(trimmedQuery);
                queryParams.add(edthThreshold);
                searchConditionGroup.append(String.join(" OR ", searchSubConditions)).append(")");
                whereConditions.add(searchConditionGroup.toString());

            } else if (titleInitial != null && !titleInitial.trim().isEmpty()) {
                if (titleInitial.equals("*")) {
                    whereConditions.add("m.title REGEXP '^[^a-zA-Z0-9]'");
                } else {
                    whereConditions.add("m.title LIKE ?");
                    queryParams.add(titleInitial + "%");
                }
            }

            if (!whereConditions.isEmpty()) {
                sqlCount.append(" WHERE ").append(String.join(" AND ", whereConditions));
            }

            pstmt = conn.prepareStatement(sqlCount.toString());
            int paramIndex = 1;
            for (Object param : queryParams) {
                if (param instanceof String) pstmt.setString(paramIndex++, (String) param);
                else if (param instanceof Integer) pstmt.setInt(paramIndex++, (Integer) param);
            }

            System.out.println("MovieListService: Executing Count SQL on SLAVE DB: " + pstmt.toString());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                totalRecords = rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("MovieListService SQL Error (getTotalMovieCount): " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (pstmt != null) pstmt.close(); } catch (SQLException ignored) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }
        return totalRecords;
    }
}
