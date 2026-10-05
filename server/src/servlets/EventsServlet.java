package servlets;

import api.GMController;
import dto.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.GsonProvider;
import util.ServletUtils;

import java.io.IOException;
import java.util.List;

@WebServlet("/events")
public class EventsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        GMController controller = ServletUtils.getController(getServletContext());

        List<EventSummaryDTO> events = controller.getEvents();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(GsonProvider.GSON.toJson(events));
    }
}