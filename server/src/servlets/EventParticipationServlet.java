package servlets;

import api.GMController;
import dto.request.ParticipationRequestDTO;
import dto.PurchaseDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.GsonProvider;
import util.ServletUtils;

import java.io.IOException;

@WebServlet("/event/participate")
public class EventParticipationServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setCharacterEncoding("UTF-8");

        String userName = ServletUtils.validateUserSession(request);

        if (userName == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print("User is not logged in.");
            return;
        }

        try {
            ParticipationRequestDTO participation =
                    GsonProvider.GSON.fromJson(
                            request.getReader(),
                            ParticipationRequestDTO.class
                    );

            if (participation == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print("Missing participation data.");
                return;
            }

            GMController controller = ServletUtils.getController(getServletContext());

            PurchaseDTO purchase = controller.participateInEvent(
                            userName,
                            participation.eventId(),
                            participation.optionNumber(),
                            participation.shares(),
                            participation.side(),
                            participation.price()
                    );

            if (purchase == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print("This event is closed - no further bets can be placed.");
                return;
            }

            response.setContentType("application/json");

            response.getWriter().print(GsonProvider.GSON.toJson(purchase));

        } catch (IllegalArgumentException | IllegalStateException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print(ex.getMessage());

        } catch (RuntimeException ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("An unexpected server error occurred.");
        }
    }
}