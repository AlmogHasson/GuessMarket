package servlets;

import api.GMController;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.GsonProvider;
import util.ServletUtils;

import java.io.IOException;

@WebServlet("/user")
public class UserServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userName = ServletUtils.getLoggedInUserName(request);

        if (userName == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        GMController controller = ServletUtils.getController(getServletContext());

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(GsonProvider.GSON.toJson(controller.getOrCreateUser(userName)));
    }
}