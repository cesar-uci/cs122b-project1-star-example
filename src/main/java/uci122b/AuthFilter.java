package uci122b;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("AuthFilter: Initializing (Corrected Operational Version)");
    }

    @Override
    public void destroy() {
        System.out.println("AuthFilter: Destroying (Corrected Operational Version)");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        boolean isWhitelisted = path.equals("/login") ||
                path.equals("/login.jsp") ||
                path.equals("") ||
                path.startsWith("/css") ||
                path.startsWith("/js") ||
                path.startsWith("/api/movie-suggestions") ||
                path.equals("/images");


        isWhitelisted = isWhitelisted ||
                path.startsWith("/search") ||
                path.startsWith("/browse") ||
                path.startsWith("/genres") ||
                path.startsWith("/single-movie") ||
                path.equals("/_dashboard");


        if (isWhitelisted) {
            System.out.println("AuthFilter (Corrected Operational Version) - Whitelisted path: " + path + " - Chaining.");
            chain.doFilter(request, response);
            return;
        }

        System.out.println("AuthFilter (Corrected Operational Version) - Protected path: " + path + " - Checking session.");
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            System.out.println("AuthFilter (Corrected Operational Version) - No valid session. Redirecting to login for path: " + path);
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        System.out.println("AuthFilter (Corrected Operational Version) - Valid session. Allowing path: " + path);
        chain.doFilter(request, response);
    }
}