package uci122b;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet(name="DashboardServlet", urlPatterns="/_dashboard")
public class DashboardServlet extends HttpServlet {
    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            ds = (DataSource) new InitialContext()
                    .lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException("Datasource lookup failed", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("empEmail") == null) {
            req.getRequestDispatcher("/WEB-INF/employee-login.jsp")
                    .forward(req, resp);
            return;
        }

        Map<String,List<String>> schema = new LinkedHashMap<>();
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData md = conn.getMetaData();

            try (ResultSet tables = md.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    List<String> cols = new ArrayList<>();
                    try (ResultSet columns = md.getColumns(null, null, tableName, "%")) {
                        while (columns.next()) {
                            String name = columns.getString("COLUMN_NAME");
                            String type = columns.getString("TYPE_NAME");
                            cols.add(name + " : " + type);
                        }
                    }
                    schema.put(tableName, cols);
                }
            }
        } catch (SQLException e) {
            throw new ServletException("Failed to fetch metadata", e);
        }

        req.setAttribute("schema", schema);
        req.getRequestDispatcher("/WEB-INF/dashboard.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        String action = req.getParameter("action");

        // --- Employee login POST ---
        if ("login".equals(action)) {
            String email = req.getParameter("email");
            String pw    = req.getParameter("password");
            if (email != null && pw != null) {
                String sql = "SELECT password FROM employees WHERE email=?";
                try (Connection conn = ds.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && uci122b.util.PasswordUtil.check(pw, rs.getString("password"))) {
                            req.getSession(true).setAttribute("empEmail", email);
                        }
                    }
                } catch (SQLException e) {
                    throw new ServletException(e);
                }
            }
            resp.sendRedirect(req.getContextPath() + "/_dashboard");
            return;
        }

        //  must be logged in for all further actions
        if (session == null || session.getAttribute("empEmail") == null) {
            resp.sendRedirect(req.getContextPath() + "/_dashboard");
            return;
        }

        // --- Add Star ---
        if ("addStar".equals(action)) {
            String name = req.getParameter("starName");
            String by   = req.getParameter("birthYear");
            String insert = "INSERT INTO stars (id,name,birthYear) VALUES (?, ?, ?)";

            try (Connection conn = ds.getConnection()) {
                // generate new star ID
                String newId;
                try (Statement s = conn.createStatement();
                     ResultSet rs = s.executeQuery(
                             "SELECT CONCAT('s',LPAD(CAST(SUBSTRING(MAX(id),2)+1 AS UNSIGNED),9,'0')) FROM stars"
                     )) {
                    rs.next();
                    newId = rs.getString(1);
                }

                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setString(1, newId);
                    ps.setString(2, name);
                    if (by == null || by.isEmpty()) {
                        ps.setNull(3, Types.INTEGER);
                    } else {
                        ps.setInt(3, Integer.parseInt(by));
                    }
                    ps.executeUpdate();
                    session.setAttribute("msg", "Star “" + name + "” added as " + newId);
                }
            } catch (Exception e) {
                throw new ServletException(e);
            }

            resp.sendRedirect(req.getContextPath() + "/_dashboard");
            return;
        }

        // --- Call add_movie stored procedure ---
        if ("addMovie".equals(action)) {
            String title   = req.getParameter("title");
            String yearStr = req.getParameter("year");
            String dir     = req.getParameter("director");
            String star    = req.getParameter("star");
            String genre   = req.getParameter("genre");

            try (Connection conn = ds.getConnection();
                 CallableStatement cs = conn.prepareCall("{CALL add_movie(?,?,?,?,?,?)}")) {

                cs.setString(1, title);
                cs.setInt   (2, Integer.parseInt(yearStr));
                cs.setString(3, dir);
                cs.setString(4, star);
                cs.setString(5, genre);
                cs.registerOutParameter(6, Types.VARCHAR);
                cs.execute();

                String status = cs.getString(6);
                session.setAttribute("msg", status);
            } catch (Exception e) {
                throw new ServletException(e);
            }

            resp.sendRedirect(req.getContextPath() + "/_dashboard");
        }
    }
}
