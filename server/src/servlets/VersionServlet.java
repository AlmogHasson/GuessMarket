package servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.ServletUtils;

import java.io.IOException;

@WebServlet("/version")
public class VersionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (ServletUtils.getLoggedInUserName(request) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        int version = ServletUtils.getDataVersion(getServletContext()).get();

        response.setContentType("text/plain");
        response.getWriter().print(version);
    }
}