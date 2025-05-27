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

@WebServlet(name = "BrowseServlet", urlPatterns = "/browse")
public class BrowseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private MovieListService movieListService;

    @Override
    public void init() throws ServletException {
        try {
            movieListService = new MovieListService();
        } catch (MovieListService.ServletException e) {
            throw new ServletException("BrowseServlet: Failed to initialize MovieListService", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=unauthorized_browse");
            return;
        }

        String genre = request.getParameter("genre");
        String letter = request.getParameter("letter");

        // Log browse action
        if (genre != null && !genre.isEmpty()) {
            System.out.println("BrowseServlet: Browsing by Genre: " + genre);
        } else if (letter != null && !letter.isEmpty()) {
            System.out.println("BrowseServlet: Browsing by Letter: " + letter);
        } else {
        }

        try {
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

            List<Movie> movies = movieListService.findMovies(request); // Pass the whole request
            int totalRecords = movieListService.getTotalMovieCount(request); // Pass the whole request

            int totalPages = (int) Math.ceil((double) totalRecords / recordsPerPage);
            if (totalPages == 0 && totalRecords > 0) totalPages = 1;
            if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;

            request.setAttribute("movies", movies);
            request.setAttribute("totalRecords", totalRecords);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("recordsPerPage", recordsPerPage);
            request.setAttribute("totalPages", totalPages);

            request.setAttribute("queryString", request.getParameter("q")); // null if not present
            request.setAttribute("genreName", genre);
            request.setAttribute("letterInitial", letter);
            request.setAttribute("sortBy", request.getParameter("sortBy")); // null if not present, service handles default

            System.out.println("BrowseServlet: Forwarding to movie-list.jsp with " + movies.size() + " movies.");
            RequestDispatcher dispatcher = request.getRequestDispatcher("/movie-list.jsp");
            dispatcher.forward(request, response);

        } catch (Exception e) {
            System.err.println("BrowseServlet Error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing browse request.");
        }
    }
}