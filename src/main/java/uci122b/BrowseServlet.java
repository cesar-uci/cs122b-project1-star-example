package uci122b;

import jakarta.servlet.RequestDispatcher; // Make sure this is imported
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
// javax.naming.* and javax.sql.* remain javax.*
// No need for database interaction here if movie-list.jsp handles it
// import javax.naming.Context;
// import javax.naming.InitialContext;
// import javax.sql.DataSource;
import java.io.IOException;
// No need for List, Movie, Connection, PreparedStatement, ResultSet if forwarding parameters
// import java.sql.Connection;
// import java.sql.PreparedStatement;
// import java.sql.ResultSet;
// import java.util.ArrayList;
// import java.util.List;

@WebServlet(name = "BrowseServlet", urlPatterns = "/browse")
public class BrowseServlet extends HttpServlet {

    // DataSource (ds) is not needed if this servlet only forwards
    // and movie-list.jsp handles its own DB connection.
    /*
    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (Exception e) {
            throw new ServletException("DataSource lookup failed in BrowseServlet", e);
        }
    }
    */

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("userEmail") == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=unauthorized_browse");
            return;
        }

        String genre = req.getParameter("genre");
        String letter = req.getParameter("letter");

        boolean isBrowseByGenre = (genre != null && !genre.trim().isEmpty());
        boolean isBrowseByLetter = (letter != null && !letter.trim().isEmpty());

        if (!isBrowseByGenre && !isBrowseByLetter) {
            resp.sendRedirect(req.getContextPath() + "/index.html");
            return;
        }

        if (isBrowseByGenre) {
            System.out.println("Browsing by Genre: " + genre);
        } else if (isBrowseByLetter) {
            System.out.println("Browsing by Letter: " + letter);
        }

        RequestDispatcher dispatcher = req.getRequestDispatcher("/movie-list.jsp");
        dispatcher.forward(req, resp);
    }
}
