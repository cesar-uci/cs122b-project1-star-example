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
    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            Context init = new InitialContext();
            ds = (DataSource) init.lookup("java:comp/env/jdbc/moviedb");
        } catch (Exception e) {
            throw new ServletException("DataSource lookup failed in GenreServlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {
        // (Optional) session check here if you require login to view genres

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        List<String> genreList = new ArrayList<>();
        String sql = "SELECT name FROM genres ORDER BY name ASC";

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
