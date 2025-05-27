package uci122b;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import uci122b.service.MovieListService; // Your service class
import uci122b.Movie; // Your Movie class

import java.io.IOException;
import java.util.List;

@WebServlet(name = "SearchServlet", urlPatterns = "/search")
public class SearchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private MovieListService movieListService;

    @Override
    public void init() throws ServletException {
        try {
            // MovieListService constructor looks up "java:comp/env/jdbc/moviedb_slave"
            movieListService = new MovieListService();
        } catch (MovieListService.ServletException e) {
            throw new ServletException("SearchServlet: Failed to initialize MovieListService", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=unauthorized_search");
            return;
        }

        String query = request.getParameter("q");
        System.out.println("SearchServlet: Full-text search submitted with query: " + query);

        try {
            // Get pagination and sorting parameters from request
            // These will be used by MovieListService.findMovies()
            int recordsPerPage = 10; // Default
            if (request.getParameter("limit") != null) {
                try {
                    recordsPerPage = Integer.parseInt(request.getParameter("limit"));
                    if (!List.of(10, 25, 50, 100).contains(recordsPerPage)) recordsPerPage = 10;
                } catch (NumberFormatException e) { /* use default */ }
            }

            int currentPage = 1; // Default
            if (request.getParameter("page") != null) {
                try {
                    currentPage = Integer.parseInt(request.getParameter("page"));
                    if (currentPage < 1) currentPage = 1;
                } catch (NumberFormatException e) { /* use default */ }
            }

            // Fetch movies using the service. MovieListService will handle the fuzzy search logic.
            List<Movie> movies = movieListService.findMovies(request); // Pass the whole request
            int totalRecords = movieListService.getTotalMovieCount(request); // Pass the whole request

            int totalPages = (int) Math.ceil((double) totalRecords / recordsPerPage);
            if (totalPages == 0 && totalRecords > 0) totalPages = 1;
            if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;


            // Set attributes for the JSP page
            request.setAttribute("movies", movies);
            request.setAttribute("totalRecords", totalRecords);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("recordsPerPage", recordsPerPage);
            request.setAttribute("totalPages", totalPages);

            // Pass through existing parameters for links on the page
            request.setAttribute("queryString", query);
            request.setAttribute("genreName", request.getParameter("genre")); // null if not present
            request.setAttribute("letterInitial", request.getParameter("letter")); // null if not present
            request.setAttribute("sortBy", request.getParameter("sortBy")); // null if not present, service handles default

            System.out.println("SearchServlet: Forwarding to movie-list.jsp with " + movies.size() + " movies and query: " + query);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/movie-list.jsp");
            dispatcher.forward(request, response);

        } catch (Exception e) {
            System.err.println("SearchServlet Error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing search request.");
        }
    }
}