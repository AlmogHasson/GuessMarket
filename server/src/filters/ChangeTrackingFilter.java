package filters;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.ServletUtils;

import java.io.IOException;

/** Bumps the data version after every successful POST, so no servlet has to remember to. */
@WebFilter("/*")
public class ChangeTrackingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        chain.doFilter(req, res); // let the servlet do its work first

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if ("POST".equals(request.getMethod()) && response.getStatus() < 400) {
            ServletUtils.getDataVersion(req.getServletContext()).incrementAndGet();
        }
    }
}