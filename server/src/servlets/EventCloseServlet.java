package servlets;

import api.GMController;
import dto.request.CloseEventRequestDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.GsonProvider;
import util.ServletUtils;

import java.io.IOException;

@WebServlet("/event/close")
public class EventCloseServlet extends HttpServlet {

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
            CloseEventRequestDTO closeRequest =
                    GsonProvider.GSON.fromJson(
                            request.getReader(),
                            CloseEventRequestDTO.class
                    );

            if (closeRequest == null) {
                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                response.getWriter().print(
                        "Missing close event data."
                );

                return;
            }

            GMController controller =
                    ServletUtils.getController(
                            getServletContext()
                    );

            controller.closeEvent(
                    userName,
                    closeRequest.eventId(),
                    closeRequest.winningOption()
            );

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

        } catch (
                IllegalArgumentException |
                IllegalStateException ex
        ) {
            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            response.getWriter().print(
                    ex.getMessage()
            );
        }
    }
}