package uci122b;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

@WebServlet(name = "GenreServlet", urlPatterns = "/genres")
public class GenreServlet extends HttpServlet {
    private DataSource ds; // This will now be for the slave

    @Override
    public void init() throws ServletException {
        try {
            Context init = new InitialContext();
            // Use the SLAVE for reading genres
            ds = (DataSource) init.lookup("java:comp/env/jdbc/moviedb_slave");
            System.out.println("GenreServlet: Initialized with jdbc/moviedb_slave");
        } catch (Exception e) {
            System.err.println("GenreServlet: Failed to lookup DataSource (jdbc/moviedb_slave): " + e.getMessage());
            throw new ServletException("DataSource lookup failed in GenreServlet (slave)", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        List<String> genreList = new ArrayList<>();
        String sql = "SELECT name FROM genres ORDER BY name ASC";

        // ds is now the slave connection pool
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                genreList.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            log("Database error fetching genres", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to retrieve genre list.");
            return;
        }

        String json = new Gson().toJson(genreList);
        try (PrintWriter out = resp.getWriter()) {
            out.print(json);
        }
    }
}