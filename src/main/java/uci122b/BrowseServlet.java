package uci122b;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uci122b.service.MovieListService;
import java.io.IOException;

@WebServlet(name = "BrowseServlet", urlPatterns = "/browse")
public class BrowseServlet extends HttpServlet {
    private MovieListService movieListService;

    @Override
    public void init() throws ServletException {
        System.out.println("DIAGNOSTIC: BrowseServlet init() started.");
        try {
            System.out.println("DIAGNOSTIC: Creating new MovieListService instance...");
            movieListService = new MovieListService();
            System.out.println("DIAGNOSTIC: MovieListService instance created successfully.");
        } catch (MovieListService.ServletException e) {
            System.err.println("DIAGNOSTIC: ERROR occurred while initializing MovieListService in BrowseServlet.");
            e.printStackTrace(System.err);
            throw new ServletException("BrowseServlet: Failed to initialize MovieListService", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("DIAGNOSTIC: BrowseServlet doGet() called for path: " + request.getRequestURI());
        // Forward to the JSP page. The error happens during init, so this part is less critical for diagnosis.
        request.setAttribute("movies", new java.util.ArrayList<Movie>());
        RequestDispatcher dispatcher = request.getRequestDispatcher("/movie-list.jsp");
        dispatcher.forward(request, response);
    }
}
