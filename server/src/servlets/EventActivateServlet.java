package servlets;

import api.GMController;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.ServletUtils;

import java.io.IOException;

@WebServlet("/event/activate")
public class EventActivateServlet extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setCharacterEncoding("UTF-8");

        String userName =
                ServletUtils.validateUserSession(request);

        if (userName == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print("User is not logged in.");
            return;
        }

        try {
            int eventId =
                    Integer.parseInt(request.getParameter("id"));

            GMController controller =
                    ServletUtils.getController(getServletContext());

            controller.activateEvent(userName, eventId);

            response.setStatus(HttpServletResponse.SC_OK);

        } catch (NumberFormatException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print(
                    "Missing or invalid event id."
            );

        } catch (IllegalArgumentException | IllegalStateException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print(ex.getMessage());
        }
    }
}