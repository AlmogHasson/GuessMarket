package servlets;

import api.GMController;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.GsonProvider;
import util.ServletUtils;

import java.io.IOException;


@WebServlet("/event/stats")
public class EventStatsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (ServletUtils.getLoggedInUserName(request) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            int eventId = Integer.parseInt(request.getParameter("id"));
            GMController controller = ServletUtils.getController(getServletContext());

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print(GsonProvider.GSON.toJson(controller.getOrderBookStats(eventId)));

        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid event id.");
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }
}