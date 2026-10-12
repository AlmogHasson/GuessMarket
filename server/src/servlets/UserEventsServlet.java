package servlets;

import api.GMController;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.GsonProvider;
import util.ServletUtils;

import java.io.IOException;

@WebServlet("/user/events")
public class UserEventsServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        String userName = ServletUtils.validateUserSession(request);

        if (userName == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            GMController controller =
                    ServletUtils.getController(getServletContext());

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().print(
                    GsonProvider.GSON.toJson(
                            controller.getUserEvents(userName)
                    )
            );

        } catch (IllegalArgumentException ex) {
            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    ex.getMessage()
            );
        }
    }
}