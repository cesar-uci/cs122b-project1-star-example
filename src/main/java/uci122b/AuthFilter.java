package uci122b;

// Changed javax.* to jakarta.*
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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

        // Allow login, index, search/browse, static resources
        // Added check for login.jsp explicitly if it's accessed directly (though should go through servlet ideally)
        // Added check for API endpoints if any are public
        if ( path.equals("/login")
                || path.equals("/login.jsp") // Might need this depending on setup
                || path.equals("/")
                || path.startsWith("/search")
                || path.startsWith("/browse")
                || path.startsWith("/genres") // Assuming genres can be viewed without login
                || path.startsWith("/single-movie") // Assuming movie details can be viewed without login
                || path.startsWith("/css/")
                || path.startsWith("/images/")
                || path.startsWith("/js/")
                || path.startsWith("/api/") // Example: if you have public API endpoints
        ) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        boolean loggedIn = (session != null && session.getAttribute("userEmail") != null);
        if (!loggedIn) {
            // Use res instead of resp (variable name consistency)
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        chain.doFilter(request, response);
    }
}