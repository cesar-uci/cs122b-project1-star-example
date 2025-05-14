package uci122b;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void destroy() { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Whitelist public/customer endpoints:
        if (
                path.equals("/login")
                        || path.equals("/login.jsp")
                        || path.equals("/")
                        || path.startsWith("/search")
                        || path.startsWith("/browse")
                        || path.startsWith("/genres")
                        || path.startsWith("/single-movie")
                        || path.startsWith("/css/")
                        || path.startsWith("/images/")
                        || path.startsWith("/js/")
                        // **NEW** Allow DashboardServlet to handle employee login & dashboard
                        || path.equals("/_dashboard")
        ) {
            chain.doFilter(request, response);
            return;
        }

        // Now enforce customer login:
        HttpSession session = req.getSession(false);
        boolean customerLoggedIn = (session != null && session.getAttribute("userEmail") != null);

        // Any other path that isn't /_dashboard and not whitelisted:
        if (!customerLoggedIn) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        chain.doFilter(request, response);
    }
}
