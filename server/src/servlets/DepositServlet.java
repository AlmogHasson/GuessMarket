package servlets;

import api.GMController;
import dto.UserDTO;
import dto.request.DepositRequestDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.GsonProvider;
import util.ServletUtils;

import java.io.IOException;

@WebServlet("/user/deposit")
public class DepositServlet extends HttpServlet {

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

            DepositRequestDTO depositRequest = GsonProvider.GSON.fromJson(
                            request.getReader(),
                            DepositRequestDTO.class
                    );

            if (depositRequest == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print("Missing deposit data.");
                return;
            }

            GMController controller = ServletUtils.getController(getServletContext());

            UserDTO updatedUser = controller.deposit(userName, depositRequest.amount());

            response.setContentType("application/json");

            response.getWriter().print(GsonProvider.GSON.toJson(updatedUser));

        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print(ex.getMessage());

        } catch (RuntimeException ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("An unexpected server error occurred.");
        }
    }
}