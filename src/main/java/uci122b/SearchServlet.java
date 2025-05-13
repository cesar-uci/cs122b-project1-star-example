package uci122b;

import jakarta.servlet.RequestDispatcher; // Import RequestDispatcher
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// No need for database imports here if movie-list.jsp handles the query
// import javax.naming.Context;
// import javax.naming.InitialContext;
// import javax.sql.DataSource;
import java.io.IOException;
// No need for PrintWriter, Connection, PreparedStatement, ResultSet, List, Map
// if forwarding parameters to movie-list.jsp

@WebServlet(name = "SearchServlet", urlPatterns = "/search")
public class SearchServlet extends HttpServlet {
    // DataSource (ds) is not needed if this servlet only forwards
    // and movie-list.jsp handles its own DB connection and query building.
    /*
    private DataSource ds;

    @Override-
    public void init() throws ServletException {
        try {
            ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (Exception e) {
            throw new ServletException("DataSource lookup failed in SearchServlet", e);
        }
    }
    */

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=unauthorized_search");
            return;
        }

        // String title = req.getParameter("title");
        // String year = req.getParameter("year");
        // String director = req.getParameter("director");
        // String star = req.getParameter("star");

        // The parameters 'title', 'year', 'director', 'star' are already in the request.
        // movie-list.jsp is designed to pick these up using request.getParameter()
        // and build its own SQL query.

        // Log the search action
        System.out.println("Search submitted with parameters: " + req.getQueryString());


        // Forward the request to movie-list.jsp.
        // movie-list.jsp will use the parameters from the original request.
        RequestDispatcher dispatcher = req.getRequestDispatcher("/movie-list.jsp");
        dispatcher.forward(req, resp);
    }
}
