package uci122b;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uci122b.util.JwtUtil;

import java.io.IOException;
import java.util.ArrayList;

@WebFilter("/*")
public class AuthFilter implements Filter {

    private final ArrayList<String> allowedURIs = new ArrayList<>();

    @Override
    public void init(FilterConfig fConfig) {
        // CHANGE THIS LINE: from "/login.jsp" to "/login"
        allowedURIs.add("/login");
        allowedURIs.add("/api/login");
        allowedURIs.add("/css/");
        allowedURIs.add("/js/");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

        if (isUrlAllowed(path)) {
            chain.doFilter(request, response);
            return;
        }

        Cookie[] cookies = httpRequest.getCookies();
        String token = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            String userEmail = JwtUtil.validateToken(token);
            if (userEmail != null) {
                chain.doFilter(request, response);
                return;
            }
        }

        if (path.startsWith("/api/")) {
            System.err.println("AuthFilter: Blocked API request to " + path + ". JWT validation failed.");
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication token is missing or invalid.");
        } else {
            // CHANGE THIS LINE: from "/login.jsp" to "/login"
            System.out.println("AuthFilter: No valid token for " + path + ". Redirecting to login page.");
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
        }
    }

    private boolean isUrlAllowed(String path) {
        for (String uri : allowedURIs) {
            if (path.toLowerCase().startsWith(uri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        // destroy() method
    }
}
